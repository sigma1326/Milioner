package com.ads.milioner.View

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.adcolony.sdk.*
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.R
import com.ads.milioner.ViewModel.GameViewModel
import com.ads.milioner.ads.InterstitialFetcher
import com.ads.milioner.ads.PlacementId
import com.ads.milioner.game.Tile
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.inmobi.sdk.InMobiSdk
import com.jakewharton.rxbinding2.view.RxView
import com.simorgh.sweetalertdialog.SweetAlertDialog
import kotlinx.android.synthetic.main.activity_main_foreign_mode.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject

class MainActivityForeignMode : AppCompatActivity() {

    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()
    private val WIDTH = "width"
    private val HEIGHT = "height"
    private val SCORE = "score"
    private val HIGH_SCORE = "high score temp"
    private val UNDO_SCORE = "undo score"
    private val CAN_UNDO = "can undo"
    private val UNDO_GRID = "undo"
    private val GAME_STATE = "game state"
    private val UNDO_GAME_STATE = "undo game state"
    private val MAX_TILE = "max tile"
    private val TIMER = "timer"


    private var interstitialApplication: AppManager? = null

    private val TAG = AppManager.TAG
    private val mHandler = Handler()


    private lateinit var ad: AdColonyInterstitial
    private lateinit var listener: AdColonyInterstitialListener
    private lateinit var adOptions: AdColonyAdOptions

    private lateinit var viewModel: GameViewModel

    private lateinit var mRewardedVideoAd: RewardedVideoAd

    private fun loadRewardedVideoAd() {
        if (!viewModel.needToReloadAd) {
            if (!mRewardedVideoAd.isLoaded) {
                mRewardedVideoAd.loadAd(PlacementId.AD_MOB_AD_UNIT_ID, AdRequest.Builder().build())
            }
        } else {
            mRewardedVideoAd.loadAd(PlacementId.AD_MOB_AD_UNIT_ID, AdRequest.Builder().build())
        }
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main_foreign_mode)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }


        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        game_view?.hasSaveState = settings.getBoolean("save_state", false)

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load()
            }
        }

        // Elapsed Time Counter
        val t = object : Thread() {

            override fun run() {
                try {
                    while (!isInterrupted) {
                        Thread.sleep(1000)
                        Handler(Looper.getMainLooper()).post {
                            if (running) {
                                game_view?.setElTime()
                            }
                        }
                    }
                } catch (ignored: InterruptedException) {
                    ignored.printStackTrace()
                }

            }
        }
        t.start()



        game_view?.game?.newGame()


        game_view?.setOnGameEndedListener {
            showAds()
        }

        if (img_clear_data != null) {
            RxView.clicks(img_clear_data).subscribe {
                this.let {
                    SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Clear App Data")
                        .setContentText("Are you sure?")
                        .setConfirmText("ok")
                        .setCancelText("cancel")
                        .setConfirmClickListener { sad ->
                            Handler(Looper.getMainLooper()).post {
                                db.clearData()
                                this@MainActivityForeignMode.finish()
                                startActivity(Intent(this@MainActivityForeignMode, LoginActivity::class.java))
                            }
                            sad.dismissWithAnimation()
                        }
                        .show()
                }
            }
        }


        // Your user's consent String. In this case, the user has given consent to store
        // and process personal information.
        val consent = "1"

        // The value passed via setGDPRRequired() will determine the GDPR requirement of
        // the user. If it's set to true, the user is subject to the GDPR laws.

        // Construct optional app options object to be sent with configure
        val appOptions = AdColonyAppOptions()
            .setKeepScreenOn(true)
            .setAppOrientation(0)
            .setGDPRRequired(true)
            .setGDPRConsentString(consent)
            .setMultiWindowEnabled(false)
            .setRequestedAdOrientation(0)
            .setTestModeEnabled(true)

        // Configure AdColony in your launching Activity's onCreate() method so that cached ads can
        // be available as soon as possible
        AdColony.configure(this, appOptions, PlacementId.APP_ID, PlacementId.ZONE_ID)


        // Ad specific options to be sent with request
        adOptions = AdColonyAdOptions()
            .enableConfirmationDialog(false)
            .enableResultsDialog(false)


        // Set up listener for interstitial ad callbacks. You only need to implement the callbacks
        // that you care about. The only required callback is onRequestFilled, as this is the only
        // way to get an ad object.

        listener = object : AdColonyInterstitialListener() {
            override fun onRequestFilled(ad: AdColonyInterstitial?) {
                // Ad passed back in request filled callback, ad can now be shown
                this@MainActivityForeignMode.ad = ad!!
                Log.d(TAG, "onRequestFilled")
                this@MainActivityForeignMode.ad.show()
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onOpened(ad: AdColonyInterstitial?) {
                // Request a new ad if ad is expiring
                super.onOpened(ad)
                Log.d(TAG, "onOpened")
                updateState()
                AppManager.isPlayingAd = true
                viewModel.needToReloadAd = true
            }

            override fun onRequestNotFilled(zone: AdColonyZone?) {
                // Ad request was not filled
                super.onRequestNotFilled(zone)
                Log.d(TAG, "onRequestNotFilled ")
                updateState()
//                playAds()
                viewModel.needToReloadAd = true
            }

            override fun onExpiring(ad: AdColonyInterstitial?) {
                // Request a new ad if ad is expiring
                super.onExpiring(ad)
                AdColony.requestInterstitial(PlacementId.ZONE_ID, this, adOptions)
                Log.d(TAG, "onExpiring")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onLeftApplication(ad: AdColonyInterstitial?) {
                super.onLeftApplication(ad)
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onClosed(ad: AdColonyInterstitial?) {
                super.onClosed(ad)
                Log.d(TAG, "onClosed")
                updateState()
                AppManager.isPlayingAd = false
                viewModel.needToReloadAd = true
            }

            override fun onClicked(ad: AdColonyInterstitial?) {
                super.onClicked(ad)
                Log.d(TAG, "onClicked")
                updateState()
                viewModel.needToReloadAd = true
            }
        }



        MobileAds.initialize(this, PlacementId.AD_MOB_APP_ID)


        // Use an activity context to get the rewarded video instance.
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = object : RewardedVideoAdListener {
            override fun onRewardedVideoAdClosed() {
                Log.d(TAG, "onRewardedVideoAdClosed")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoAdLeftApplication() {
                Log.d(TAG, "onRewardedVideoAdLeftApplication")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoAdLoaded() {
                Log.d(TAG, "onRewardedVideoAdLoaded")
                updateState()
                viewModel.needToReloadAd = true
                if (mRewardedVideoAd.isLoaded) {
                    mRewardedVideoAd.show()
                }
            }

            override fun onRewardedVideoAdOpened() {
                Log.d(TAG, "onRewardedVideoAdOpened")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoCompleted() {
                Log.d(TAG, "onRewardedVideoCompleted")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onRewarded(p0: RewardItem?) {
                Log.d(TAG, "onRewarded")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoStarted() {
                Log.d(TAG, "onRewardedVideoStarted")
                updateState()
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoAdFailedToLoad(p0: Int) {
                AdColony.requestInterstitial(PlacementId.ZONE_ID, listener, adOptions)
                Log.d(TAG, "onRewardedVideoAdFailedToLoad")
                updateState()
                viewModel.needToReloadAd = true
            }

        }
    }


    override fun onPause() {
        super.onPause()
//        save()
        running = false
        mRewardedVideoAd.pause(this)
    }

    override fun onResume() {
        super.onResume()
//        load()
        running = true
        mRewardedVideoAd.resume(this)
    }

    override fun onStart() {
        super.onStart()
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)
        viewModel.user = db.getUserLiveData()
        viewModel.user.observe(this, Observer {
            if (it != null) {
            }
        })


        initAds()

        viewModel.forcedRetry.set(0)
//        prefetchInterstitial()

    }

    override fun onStop() {
        super.onStop()
        save()
        running = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mRewardedVideoAd.destroy(this)
    }


    private fun showAds() {
        loadRewardedVideoAd()
    }

    private fun playAds() {
        if (!AppManager.isPlayingAd) {
            setupInterstitial()
            viewModel.mInterstitialAd?.load()
            viewModel.mInterstitialAd?.show()
        }
    }


    private fun initAds() {
        val consent = JSONObject()
        try {
            // Provide correct consent value to sdk which is obtained by User
            consent.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        InMobiSdk.init(this, PlacementId.siteID, consent)
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
        interstitialApplication = this.application as AppManager

        viewModel.interstitialFetcher = object : InterstitialFetcher {
            override fun onFetchSuccess() {
                setupInterstitial()
            }

            override fun onFetchFailure() {
                if (viewModel.forcedRetry.getAndIncrement() < 2) {
                    mHandler.postDelayed(
                        { interstitialApplication?.fetchInterstitial(viewModel.interstitialFetcher) },
                        5000
                    )
                } else {
                }
            }
        }
    }

    private fun setupInterstitial() {
        viewModel.mInterstitialAd = InMobiInterstitial(this, PlacementId.YOUR_PLACEMENT_ID,
            object : InterstitialAdEventListener() {
                override fun onAdLoadSucceeded(inMobiInterstitial: InMobiInterstitial?) {
                    super.onAdLoadSucceeded(inMobiInterstitial)
                    Log.d(TAG, "onAdLoadSuccessful")
                    updateState()
                    if (inMobiInterstitial?.isReady!!) {
                        Log.d(TAG, "Ad is Ready")
                        try {
                            viewModel.mInterstitialAd?.load()
                            viewModel.mInterstitialAd?.show()
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
                    AppManager.isPlayingAd = true
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
                    AppManager.isPlayingAd = false
                }
            })
    }

    private fun updateState() {
    }

    private fun prefetchInterstitial() {
        viewModel.mInterstitialAd = interstitialApplication?.getInterstitial()
        if (null == viewModel.mInterstitialAd) {
            interstitialApplication?.fetchInterstitial(viewModel.interstitialFetcher)
            return
        }

        viewModel.mInterstitialAd?.setListener(object : InterstitialAdEventListener() {
            override fun onAdLoadSucceeded(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdLoadSucceeded(inMobiInterstitial)
                Log.d(TAG, "onAdLoadSuccessful 1")
                updateState()
                if (inMobiInterstitial!!.isReady) {
                    Log.d(TAG, "Ad is Ready 1")
                } else {
                    Log.d(TAG, "onAdLoadSuccessful inMobiInterstitial not ready 1 ")
                }
            }

            override fun onAdLoadFailed(
                inMobiInterstitial: InMobiInterstitial?,
                inMobiAdRequestStatus: InMobiAdRequestStatus?
            ) {
                super.onAdLoadFailed(inMobiInterstitial, inMobiAdRequestStatus)
                Log.d(TAG, "Unable to load interstitial ad 1 (error message: " + inMobiAdRequestStatus!!.message)
                updateState()
            }

            override fun onAdReceived(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdReceived(inMobiInterstitial)
                updateState()
                Log.d(TAG, "onAdReceived 1 ")
            }

            override fun onAdClicked(inMobiInterstitial: InMobiInterstitial?, map: Map<Any, Any>?) {
                super.onAdClicked(inMobiInterstitial, map)
                Log.d(TAG, "onAdClicked 1" + map!!.size)
                updateState()
            }

            override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdWillDisplay(inMobiInterstitial)
                Log.d(TAG, "onAdWillDisplay 1" + inMobiInterstitial!!)
                updateState()
            }

            override fun onAdDisplayed(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdDisplayed(inMobiInterstitial)
                Log.d(TAG, "onAdDisplayed 1" + inMobiInterstitial!!)
                updateState()
            }

            override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdDisplayFailed(inMobiInterstitial)
                Log.d(TAG, "onAdDisplayFailed 1" + "FAILED")
                updateState()
            }

            override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial?) {
                super.onAdDismissed(inMobiInterstitial)
                Log.d(TAG, "onAdDismissed 1" + inMobiInterstitial!!)
                updateState()
            }

            override fun onUserLeftApplication(inMobiInterstitial: InMobiInterstitial?) {
                super.onUserLeftApplication(inMobiInterstitial)
                Log.d(TAG, "onUserWillLeaveApplication 1" + inMobiInterstitial!!)
                updateState()
            }

            override fun onRewardsUnlocked(inMobiInterstitial: InMobiInterstitial?, map: Map<Any, Any>?) {
                super.onRewardsUnlocked(inMobiInterstitial, map)
                Log.d(TAG, "onRewardsUnlocked 1 " + map!!.size)
                updateState()
                (this@MainActivityForeignMode.application as AppManager).callAdsAPI()
            }
        })
        viewModel.mInterstitialAd?.load()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasState", true)
        save()
    }

    private fun save() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        val field = game_view?.game?.grid?.field
        val undoField = game_view?.game?.grid?.undoField
        editor.putInt(WIDTH, field?.size!!)
        editor.putInt(HEIGHT, field.size)
        for (xx in field.indices) {
            for (yy in 0 until field[0]?.size!!) {
                if (field[xx][yy] != null) {
                    editor.putInt("$xx $yy", field[xx][yy].value)
                } else {
                    editor.putInt("$xx $yy", 0)
                }

                if (undoField!![xx][yy] != null) {
                    editor.putInt("$UNDO_GRID$xx $yy", undoField[xx][yy].value)
                } else {
                    editor.putInt("$UNDO_GRID$xx $yy", 0)
                }
            }
        }
        editor.putLong(SCORE, game_view?.game?.score!!)
        editor.putLong(HIGH_SCORE, game_view?.game?.highScore!!)
        editor.putLong(UNDO_SCORE, game_view?.game?.lastScore!!)
        editor.putBoolean(CAN_UNDO, game_view?.game?.canUndo!!)
        editor.putInt(GAME_STATE, game_view?.game?.gameState!!)
        editor.putInt(UNDO_GAME_STATE, game_view?.game?.lastGameState!!)
        editor.putLong(MAX_TILE, game_view?.game?.maxTile!!)
        editor.putInt(TIMER, game_view?.elTime!!)
        editor.apply()
    }

    private fun load() {
        //Stopping all animations
        game_view?.game?.aGrid?.cancelAnimations()

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        for (xx in game_view?.game?.grid?.field?.indices!!) {
            for (yy in 0 until game_view?.game?.grid?.field!![0].size) {
                val value = settings.getInt("$xx $yy", -1)
                if (value > 0) {
                    game_view?.game?.grid?.field!![xx][yy] = Tile(xx, yy, value)
                } else if (value == 0) {
                    game_view?.game?.grid?.field!![xx][yy] = null
                }

                val undoValue = settings.getInt("$UNDO_GRID$xx $yy", -1)
                if (undoValue > 0) {
                    game_view?.game?.grid!!.undoField[xx][yy] = Tile(xx, yy, undoValue)
                } else if (value == 0) {
                    game_view?.game?.grid!!.undoField[xx][yy] = null
                }
            }
        }

//        game_view?.game?.score = settings.getLong(SCORE, game_view?.game?.score!!)
        game_view?.game?.highScore = settings.getLong(HIGH_SCORE, game_view?.game?.highScore!!)
        game_view?.game?.lastScore = settings.getLong(UNDO_SCORE, game_view?.game?.lastScore!!)
//        game_view?.game?.canUndo = settings.getBoolean(CAN_UNDO, game_view?.game?.canUndo!!)
//        game_view?.game?.gameState = settings.getInt(GAME_STATE, game_view?.game?.gameState!!)
//        game_view?.game?.lastGameState = settings.getInt(UNDO_GAME_STATE, game_view?.game?.lastGameState!!)
//        game_view?.game?.maxTile = settings.getLong(MAX_TILE, game_view?.game?.maxTile!!)
//        game_view?.elTime = settings.getInt(TIMER, game_view?.elTime!!)
    }


    companion object {
        @JvmField
        @Volatile
        var running: Boolean = false
    }
}
