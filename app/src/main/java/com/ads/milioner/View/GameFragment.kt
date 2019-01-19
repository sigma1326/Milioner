package com.ads.milioner.View

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
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
import com.ads.milioner.R
import com.ads.milioner.View.MainActivity.Companion.running
import com.ads.milioner.ViewModel.GameViewModel
import com.ads.milioner.ads.InterstitialFetcher
import com.ads.milioner.ads.PlacementId
import com.ads.milioner.game.Tile
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.inmobi.sdk.InMobiSdk
import com.simorgh.sweetalertdialog.SweetAlertDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.game_fragment.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject

class GameFragment : Fragment() {
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


    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()

    private var interstitialApplication: AppManager? = null

    private val TAG = AppManager.TAG
    private val mHandler = Handler()

    lateinit var disposable: Disposable
    var isConnected = false
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
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


        disposable = ReactiveNetwork
            .observeInternetConnectivity(AppManager.settings)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                isConnected = it
            }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.game_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        game_view?.game?.newGame()


        game_view?.setOnGameEndedListener {
            if (isConnected) {
                showAds()
            } else {
                SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("دستگاه به اینترنت متصل نیست")
                    .setConfirmText("باشه")
                    .show()
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("hasState", true)
        save()
    }


    override fun onPause() {
        super.onPause()
        save()
        running = false
    }

    override fun onResume() {
        super.onResume()
        load()
        running = true
    }

    override fun onStart() {
        super.onStart()
        Log.d(AppManager.TAG, "start")
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)
        viewModel.user = db.getUserLiveData()
        viewModel.user.observe(activity!!, Observer {
            if (it != null) {
                Log.d(AppManager.TAG, "money :: " + it.balance.toString())
            }
        })





        initAds()

        viewModel.forcedRetry.set(0)
        prefetchInterstitial()

        db.getUser()?.token?.let {
            network.me(it, object : ResponseListener {
                override fun onSuccess(message: String) {
                    Log.d(AppManager.TAG, message)
                }

                override fun onFailure(message: String) {
                    Log.d(AppManager.TAG, message)
                }
            })
        }
    }

    override fun onStop() {
        super.onStop()
        save()
        running = false
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    private fun save() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
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

        val settings = PreferenceManager.getDefaultSharedPreferences(context)
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



    private fun showAds() {
        db.getUser().let {
            network.checkIP(object : ResponseListener {
                override fun onSuccess(message: String) {
                    when (message) {
                        "true" -> {
                            playAds()
                        }
                        "false" -> {
                            SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("لطفا با VPN وصل شوید")
                                .setConfirmText("باشه")
                                .show()
                            updateState()
                        }
                        else -> {
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                            updateState()
                        }
                    }

                }

                override fun onFailure(message: String) {
                    SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText(message)
                        .setConfirmText("باشه")
                        .show()
                    updateState()
                }
            })
        }
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

        InMobiSdk.init(activity, PlacementId.siteID, consent)
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
        interstitialApplication = this.activity?.application as AppManager

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
        viewModel.mInterstitialAd = InMobiInterstitial(activity, PlacementId.YOUR_PLACEMENT_ID,
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
                    (activity?.application as AppManager).callAdsAPI()
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
                (activity?.application as AppManager).callAdsAPI()
            }
        })
        viewModel.mInterstitialAd?.load()
    }


}
