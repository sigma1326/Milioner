package com.ads.milioner.View

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.ViewModel.HomeViewModel
import com.ads.milioner.ads.InterstitialFetcher
import com.ads.milioner.ads.PlacementId
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.inmobi.sdk.InMobiSdk
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.home_fragment.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class HomeFragment : Fragment() {
    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()

    private var interstitialApplication: AppManager? = null
    private var interstitialFetcher: InterstitialFetcher? = null
    private val TAG = AppManager.TAG
    private val mHandler = Handler()
    private val forcedRetry = AtomicInteger(0)
    private var mInterstitialAd: InMobiInterstitial? = null


    companion object {
        fun newInstance() = HomeFragment()
    }

    private lateinit var viewModel: HomeViewModel

    lateinit var disposable: Disposable
    var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disposable = ReactiveNetwork
            .observeInternetConnectivity(AppManager.settings)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                isConnected = it
            }


        initAds()

        forcedRetry.set(0)
        prefetchInterstitial()

//        if (null == mInterstitialAd) {
//            setupInterstitial()
//        } else {
//            mInterstitialAd?.load()
//            mInterstitialAd?.show()
//        }


    }


    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(com.ads.milioner.R.layout.home_fragment, container, false)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        RxView.clicks(layout_show_ads).filter {
            pb_ads.visibility != View.VISIBLE
        }.subscribe {
            pb_ads.visibility = View.VISIBLE

            if (isConnected) {
                showAds()
            } else {
                Toast.makeText(this.context, "دستگاه به اینترنت متصل نیست", Toast.LENGTH_SHORT).show()
                pb_ads.visibility = View.GONE
            }
        }

        RxView.clicks(layout_charge).filter {
            pb_charge.visibility != View.VISIBLE
        }.subscribe {
            pb_charge.visibility = View.VISIBLE
            if (isConnected) {
                charge()
            } else {
                Toast.makeText(this.context, "دستگاه به اینترنت متصل نیست", Toast.LENGTH_SHORT).show()
                pb_charge.visibility = View.GONE
            }
        }


    }

    private fun getTimeStamp(): String {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
    }

    private fun showAds() {
        //todo should not be here
        playAds()
        db.getUser().let {
            network.checkIP(object : ResponseListener {
                override fun onSuccess(message: String) {
                    when (message) {
                        "true" -> {
                            val gid = UUID.randomUUID().toString()
                            val timeStamp = getTimeStamp()
                            val hashString = "$timeStamp$gid$timeStamp${it?.apiKey}"
                            val hash = sha256(hashString)
                            network.ads(it?.token.toString()
                                , gid, hash, timeStamp, hashString
                                , object : ResponseListener {
                                    override fun onSuccess(message: String) {
                                        Log.d(AppManager.TAG, message)
                                        updateState()
                                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onFailure(message: String) {
                                        Log.d(AppManager.TAG, message)
                                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                                        updateState()
                                    }

                                })
                        }
                        "false" -> {
                            Toast.makeText(activity, "لطفا با VPN وصل شوید", Toast.LENGTH_SHORT).show()
                            updateState()
                        }
                        else -> {
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                            updateState()
                        }
                    }

                }

                override fun onFailure(message: String) {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    updateState()
                }
            })
        }
    }

    private fun playAds() {
        pb_ads.visibility = View.VISIBLE
        setupInterstitial()
        mInterstitialAd?.load()
        mInterstitialAd?.show()
    }

    @Throws(NoSuchAlgorithmException::class)
    fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(text.toByteArray())
        val digest = md.digest()
        return android.util.Base64.encodeToString(digest, Base64.DEFAULT)
    }

    private fun charge() {
        db.getUser().let {
            network.charge(it?.token.toString(), object : ResponseListener {
                override fun onSuccess(message: String) {
                    Log.d(AppManager.TAG, message)
                    when (message) {
                        "true" -> Toast.makeText(activity, "شارژ با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
                        "false" -> Toast.makeText(activity, "موجودی شما کافی نمی‌باشد", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                    pb_charge.visibility = View.INVISIBLE
                }

                override fun onFailure(message: String) {
                    Log.d(AppManager.TAG, message)
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    pb_charge.visibility = View.INVISIBLE
                }

            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        viewModel.user = db.getUserLiveData()
        viewModel.user.observe(this, Observer {
            if (it != null && tv_balance != null) {
                tv_balance.text = it.balance.toString()
            }
        })
    }

    private fun initAds() {
        val consent = JSONObject()
        try {
            // Provide correct consent value to sdk which is obtained by User
            consent.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        InMobiSdk.init(activity, PlacementId.siteID, consent)
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
        interstitialApplication = this.activity?.application as AppManager

        interstitialFetcher = object : InterstitialFetcher {
            override fun onFetchSuccess() {
                setupInterstitial()
            }

            override fun onFetchFailure() {
                if (forcedRetry.getAndIncrement() < 2) {
                    mHandler.postDelayed({ interstitialApplication?.fetchInterstitial(interstitialFetcher) }, 5000)
                } else {
//                    adjustButtonVisibility()
                }
            }
        }
    }

    private fun setupInterstitial() {
        mInterstitialAd = InMobiInterstitial(activity, PlacementId.YOUR_PLACEMENT_ID,
            object : InterstitialAdEventListener() {
                override fun onAdLoadSucceeded(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdLoadSucceeded(inMobiInterstitial)
                    Log.d(TAG, "onAdLoadSuccessful")
                    updateState()
                    if (inMobiInterstitial?.isReady!!) {
                        Log.d(TAG, "Ad is Ready")
                        try {
                            mInterstitialAd?.load()
                            mInterstitialAd?.show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    } else {
                        Log.d(TAG, "onAdLoadSuccessful inMobiInterstitial not ready")
                    }
                }

                override fun onAdLoadFailed(
                    inMobiInterstitial: InMobiInterstitial?,
                    inMobiAdRequestStatus: InMobiAdRequestStatus?
                ) {
                    super.onAdLoadFailed(inMobiInterstitial, inMobiAdRequestStatus)
                    Log.d(TAG, "Unable to load interstitial ad (error message: " + inMobiAdRequestStatus!!.message)
                    updateState()
                }

                override fun onAdReceived(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdReceived(inMobiInterstitial)
                    Log.d(TAG, "onAdReceived")
                    updateState()
                }

                override fun onAdClicked(inMobiInterstitial: InMobiInterstitial?, map: Map<Any, Any>?) {
                    super.onAdClicked(inMobiInterstitial, map)
                    Log.d(TAG, "onAdClicked " + map!!.size)
                    updateState()
                }

                override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdWillDisplay(inMobiInterstitial)
                    Log.d(TAG, "onAdWillDisplay " + inMobiInterstitial!!)
                    updateState()
                }

                override fun onAdDisplayed(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdDisplayed(inMobiInterstitial)
                    Log.d(TAG, "onAdDisplayed " + inMobiInterstitial!!)
                    updateState()

                }

                override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdDisplayFailed(inMobiInterstitial)
                    Log.d(TAG, "onAdDisplayFailed " + "FAILED")
                    updateState()
                }

                override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdDismissed(inMobiInterstitial)
                    Log.d(TAG, "onAdDismissed " + inMobiInterstitial!!)
                    updateState()
                }

                override fun onUserLeftApplication(inMobiInterstitial: InMobiInterstitial?) {
                    super.onUserLeftApplication(inMobiInterstitial)
                    Log.d(TAG, "onUserWillLeaveApplication " + inMobiInterstitial!!)
                    updateState()
                }

                override fun onRewardsUnlocked(inMobiInterstitial: InMobiInterstitial?, map: Map<Any, Any>?) {
                    super.onRewardsUnlocked(inMobiInterstitial, map)
                    Log.d(TAG, "onRewardsUnlocked " + map!!.size)
                    updateState()
                }
            })
    }

    private fun updateState() {
        pb_ads.visibility = View.INVISIBLE
    }

    private fun prefetchInterstitial() {
        mInterstitialAd = interstitialApplication?.getInterstitial()
        if (null == mInterstitialAd) {
            interstitialApplication?.fetchInterstitial(interstitialFetcher)
            return
        }

        mInterstitialAd?.setListener(object : InterstitialAdEventListener() {
            override fun onAdLoadSucceeded(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdLoadSucceeded(inMobiInterstitial)
                Log.d(TAG, "onAdLoadSuccessful")
                updateState()
                if (inMobiInterstitial!!.isReady) {
                    Log.d(TAG, "Ad is Ready")
                } else {
                    Log.d(TAG, "onAdLoadSuccessful inMobiInterstitial not ready")
                }
            }

            override fun onAdLoadFailed(
                inMobiInterstitial: InMobiInterstitial?,
                inMobiAdRequestStatus: InMobiAdRequestStatus?
            ) {
                super.onAdLoadFailed(inMobiInterstitial, inMobiAdRequestStatus)
                Log.d(TAG, "Unable to load interstitial ad (error message: " + inMobiAdRequestStatus!!.message)
                updateState()
            }

            override fun onAdReceived(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdReceived(inMobiInterstitial)
                updateState()
                Log.d(TAG, "onAdReceived")
            }

            override fun onAdClicked(inMobiInterstitial: InMobiInterstitial?, map: Map<Any, Any>?) {
                super.onAdClicked(inMobiInterstitial, map)
                Log.d(TAG, "onAdClicked " + map!!.size)
                updateState()
            }

            override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdWillDisplay(inMobiInterstitial)
                Log.d(TAG, "onAdWillDisplay " + inMobiInterstitial!!)
                updateState()
            }

            override fun onAdDisplayed(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdDisplayed(inMobiInterstitial)
                Log.d(TAG, "onAdDisplayed " + inMobiInterstitial!!)
                updateState()
            }

            override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdDisplayFailed(inMobiInterstitial)
                Log.d(TAG, "onAdDisplayFailed " + "FAILED")
                updateState()
            }

            override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdDismissed(inMobiInterstitial)
                Log.d(TAG, "onAdDismissed " + inMobiInterstitial!!)
                updateState()
            }

            override fun onUserLeftApplication(inMobiInterstitial: InMobiInterstitial?) {
                super.onUserLeftApplication(inMobiInterstitial)
                Log.d(TAG, "onUserWillLeaveApplication " + inMobiInterstitial!!)
                updateState()
            }

            override fun onRewardsUnlocked(inMobiInterstitial: InMobiInterstitial?, map: Map<Any, Any>?) {
                super.onRewardsUnlocked(inMobiInterstitial, map)
                Log.d(TAG, "onRewardsUnlocked " + map!!.size)
                updateState()
            }
        })
        mInterstitialAd?.load()
    }

}
