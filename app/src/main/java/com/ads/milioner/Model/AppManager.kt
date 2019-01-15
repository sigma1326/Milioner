package com.ads.milioner.Model

import android.util.Log
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import com.ads.milioner.Model.database.DataBase
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.ApiService
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.ads.InterstitialFetcher
import com.ads.milioner.ads.PlacementId
import com.facebook.stetho.Stetho
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.strategy.SocketInternetObservingStrategy
import com.google.common.hash.Hashing
import com.inmobi.ads.InMobiAdRequest
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.sdk.InMobiSdk
import io.reactivex.schedulers.Schedulers
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.startKoin
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.chrisjenx.calligraphy.CalligraphyConfig
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class AppManager : MultiDexApplication() {
    private lateinit var mInMobiAdRequest: InMobiAdRequest
    private var mIntQueue: BlockingQueue<InMobiInterstitial> = LinkedBlockingQueue()
    private var mIntFetcherQueue: BlockingQueue<InterstitialFetcher> = LinkedBlockingQueue<InterstitialFetcher>()
    private lateinit var interstitialAdRequestListener: InMobiInterstitial.InterstitialAdRequestListener

    private lateinit var network: NetworkRepositoryImpl
    private lateinit var db: DataBaseRepositoryImpl


    override fun onCreate() {
        super.onCreate()

//        getUncaughtExceptions()

        Stetho.initializeWithDefaults(this)

        //start Koin DI
        startKoin(this, listOf(appModule))


        CalligraphyConfig.initDefault(
            CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Vazir-Medium-FD.ttf")
                .build()
        )

        initADS()
        db = DataBaseRepositoryImpl(
            Room.databaseBuilder(
                this, DataBase::class.java,
                DB_NAME
            ).allowMainThreadQueries()
                .build()
        )
        network = NetworkRepositoryImpl(retrofit.create(ApiService::class.java), db)
    }


    private fun initADS() {
        val consent = JSONObject()
        try {
            // Provide correct consent value to sdk which is obtained by User
            consent.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        InMobiSdk.init(this, PlacementId.siteID, consent)
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
        mInMobiAdRequest = InMobiAdRequest.Builder(PlacementId.YOUR_PLACEMENT_ID)
            .setMonetizationContext(InMobiAdRequest.MonetizationContext.MONETIZATION_CONTEXT_ACTIVITY).build()
        interstitialAdRequestListener =
                InMobiInterstitial.InterstitialAdRequestListener { inMobiAdRequestStatus, inMobiInterstitial ->
                    if (inMobiAdRequestStatus.statusCode == InMobiAdRequestStatus.StatusCode.NO_ERROR && null != inMobiInterstitial) {
                        mIntQueue.offer(inMobiInterstitial)
                        signalIntResult(true)
                    } else {
                        signalIntResult(false)
                    }
                }

        fetchInterstitial(null)
    }

    fun fetchInterstitial(interstitialFetcher: InterstitialFetcher?) {
        if (null != interstitialFetcher) {
            mIntFetcherQueue.offer(interstitialFetcher)
        }
        InMobiInterstitial.requestAd(this, mInMobiAdRequest, interstitialAdRequestListener)
    }

    private fun signalIntResult(result: Boolean) {
        val interstitialFetcher = mIntFetcherQueue.poll()
        if (null != interstitialFetcher) {
            if (result) {
                interstitialFetcher.onFetchSuccess()
            } else {
                interstitialFetcher.onFetchFailure()
            }
        }
    }

    fun getInterstitial(): InMobiInterstitial? {
        return mIntQueue.poll()
    }


    private fun getUncaughtExceptions() {
        // Setup handler for uncaught exceptions.
        try {
            Thread.setDefaultUncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
                Log.e(TAG, "Uncaught Exception thread: " + thread.name + "" + throwable.stackTrace)
                throwable.printStackTrace()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Could not set the Default Uncaught Exception Handler:" + e.stackTrace)
        }
    }


    companion object {
        private const val host = "https://apanaj.fdli.ir"
        const val TAG = "debug"
        private const val DB_NAME = "milioner-db"
        var token = ""
        var phone: String = ""

        val settings = InternetObservingSettings.builder()
            .initialInterval(1000)
            .interval(5000)
            .host("https://google.com")
            .strategy(SocketInternetObservingStrategy())
            .timeout(5000)
            .build()!!

        private val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(host)
            .build()


        val appModule = module {
            //Network
            single { NetworkRepositoryImpl(get(), get()) }

            single { retrofit.create(ApiService::class.java) }

            //DataBase
            single {
                Room.databaseBuilder(
                    androidApplication(), DataBase::class.java,
                    DB_NAME
                ).allowMainThreadQueries()
                    .build()
            }

            single { DataBaseRepositoryImpl(get()) }
        }


    }


    fun deactivateReward() {
        val user = db.getUser()
        if (user != null) {
            user.isRewardActive = false
            db.insertUser(user)
        }
    }

    fun callAdsAPI() {
        val user = db.getUser()
        if (user != null) {
            user.isRewardActive = true
            db.insertUser(user)
        }
        if (user != null) {
            if (user.isRewardActive!!) {
                val gid = UUID.randomUUID().toString()
                val timeStamp = System.currentTimeMillis().toString()
                val hashString = "$timeStamp$gid$timeStamp${user.apiKey}"
                val hash = Hashing.sha256().hashString(hashString, StandardCharsets.UTF_8).toString()

                network.ads(
                    user.token.toString()
                    , gid, hash, timeStamp
                    , object : ResponseListener {
                        override fun onSuccess(message: String) {
                            Log.d(AppManager.TAG, message)
                            deactivateReward()
                        }

                        override fun onFailure(message: String) {
                            Log.d(AppManager.TAG, message)
                        }

                    })
            }
        }
    }

}
