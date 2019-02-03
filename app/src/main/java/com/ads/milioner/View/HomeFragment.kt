package com.ads.milioner.View

import android.animation.Animator
import android.annotation.SuppressLint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.adcolony.sdk.*
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.R
import com.ads.milioner.ViewModel.HomeViewModel
import com.ads.milioner.ads.InterstitialFetcher
import com.ads.milioner.ads.PlacementId
import com.ads.milioner.util.DialogMaker
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
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
import com.robinhood.ticker.TickerUtils
import com.simorgh.sweetalertdialog.SweetAlertDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.home_fragment.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject


class HomeFragment : Fragment() {
    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()

    private var interstitialApplication: AppManager? = null

    private val TAG = AppManager.TAG
    private val mHandler = Handler()

    private lateinit var ad: AdColonyInterstitial
    private lateinit var listener: AdColonyInterstitialListener
    private lateinit var adOptions: AdColonyAdOptions

    private lateinit var mRewardedVideoAd: RewardedVideoAd

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
        AdColony.configure(activity, appOptions, PlacementId.APP_ID, PlacementId.ZONE_ID)


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
                this@HomeFragment.ad = ad!!
                Log.d(TAG, "onRequestFilled")
                this@HomeFragment.ad.show()
                pb_ads.visibility = View.VISIBLE
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
            }

            override fun onExpiring(ad: AdColonyInterstitial?) {
                // Request a new ad if ad is expiring
                super.onExpiring(ad)
                AdColony.requestInterstitial(PlacementId.ZONE_ID, this, adOptions)
                Log.d(TAG, "onExpiring")
                updateState()
            }

            override fun onLeftApplication(ad: AdColonyInterstitial?) {
                super.onLeftApplication(ad)
                updateState()
            }

            override fun onClosed(ad: AdColonyInterstitial?) {
                super.onClosed(ad)
                Log.d(TAG, "onClosed")
                updateState()
                AppManager.isPlayingAd = false
                (activity?.application as AppManager).callAdsAPI()
            }

            override fun onClicked(ad: AdColonyInterstitial?) {
                super.onClicked(ad)
                Log.d(TAG, "onClicked")
                updateState()
            }
        }


        MobileAds.initialize(activity, PlacementId.AD_MOB_APP_ID)


        // Use an activity context to get the rewarded video instance.
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity)
        mRewardedVideoAd.rewardedVideoAdListener = object : RewardedVideoAdListener {
            override fun onRewardedVideoAdClosed() {
                Log.d(TAG, "onRewardedVideoAdClosed")
                updateState()
                AppManager.isPlayingAd = false
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoAdLeftApplication() {
                Log.d(TAG, "onRewardedVideoAdLeftApplication")
                updateState()
                AppManager.isPlayingAd = false
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
                AppManager.isPlayingAd = true
                viewModel.needToReloadAd = true
            }

            override fun onRewardedVideoCompleted() {
                Log.d(TAG, "onRewardedVideoCompleted")
                updateState()
            }

            override fun onRewarded(p0: RewardItem?) {
                Log.d(TAG, "onRewarded")
                updateState()
                AppManager.isPlayingAd = false
                viewModel.needToReloadAd = true
                (activity?.application as AppManager).callAdsAPI()
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


    private fun loadRewardedVideoAd() {
        if (!viewModel.needToReloadAd) {
            if (!mRewardedVideoAd.isLoaded) {
                mRewardedVideoAd.loadAd(PlacementId.AD_MOB_AD_UNIT_ID, AdRequest.Builder().build())
            }
        } else {
            mRewardedVideoAd.loadAd(PlacementId.AD_MOB_AD_UNIT_ID, AdRequest.Builder().build())
        }
    }

    override fun onResume() {
        super.onResume()
        mRewardedVideoAd.resume(activity)

        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        viewModel.init(db)

        viewModel.user.observe(this, Observer {
            if (it != null && tv_balance != null) {
//                Log.d(AppManager.TAG, it.balance.toString())
                Handler(Looper.getMainLooper()).postDelayed({
                    tv_balance?.text = it.balance.toString()
                }, 1500)
            }
        })

        initAds()

        viewModel.forcedRetry.set(0)
//        prefetchInterstitial()

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

    override fun onPause() {
        super.onPause()
        Log.d(AppManager.TAG, "pause")
        mRewardedVideoAd.pause(activity)
    }

    override fun onDestroy() {
        Log.d(AppManager.TAG, "destroy")
        disposable.dispose()
        super.onDestroy()
        mRewardedVideoAd.destroy(activity)
    }

    override fun onStart() {
        super.onStart()
        Log.d(AppManager.TAG, "start")


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


        tv_balance.setCharacterLists(TickerUtils.provideNumberList())
        tv_balance.animationInterpolator = OvershootInterpolator()
        tv_balance.animationDuration = 1000
        tv_balance.typeface = Typeface.createFromAsset(activity?.assets, "fonts/Vazir-Medium-FD.ttf")
        val mp = MediaPlayer.create(activity, R.raw.coin)
        mp.setVolume(100F, 100f)

        tv_balance.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                mp.start()
            }
        })

        RxView.clicks(layout_show_ads).filter {
            pb_ads.visibility != View.VISIBLE
        }.subscribe {
            pb_ads.visibility = View.VISIBLE

            if (isConnected) {
                showAds()
            } else {
                SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("دستگاه به اینترنت متصل نیست")
                    .setConfirmText("باشه")
                    .show()
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
                SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("دستگاه به اینترنت متصل نیست")
                    .setConfirmText("باشه")
                    .show()
                pb_charge.visibility = View.GONE
            }
        }

        RxView.clicks(layout_game)
            .filter {
                pb_charge.visibility != View.VISIBLE && pb_ads.visibility != View.VISIBLE
            }
            .subscribe {
                try {
                    findNavController().navigate(R.id.action_homeFragment_to_gameFragment)
                } catch (e: Exception) {
                }
            }

    }


    private fun showAds() {
        db.getUser().let {
            network.checkIP(object : ResponseListener {
                override fun onSuccess(message: String) {
                    when (message) {
                        "true" -> {
                            pb_ads.visibility = View.VISIBLE
                            loadRewardedVideoAd()
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
        pb_ads.visibility = View.VISIBLE
        if (!AppManager.isPlayingAd) {
            setupInterstitial()
            viewModel.mInterstitialAd?.load()
            viewModel.mInterstitialAd?.show()
        }
    }

    private fun charge() {
        db.getUser()?.let {
            if (it.phone?.length!! > 2) {
                if (it.phone?.substring(0, 2).toString() == "98") {
                    val phone = "0${it.phone?.substring(2)}"
                    if (phone.length > 10) {
                        DialogMaker.phoneInputDialog(
                            this.activity!!,
                            phone,
                            object : DialogMaker.OnPhoneEnteredListener {
                                override fun onPhoneEntered(phone: String, cancel: Boolean) {
                                    pb_charge.visibility = View.INVISIBLE
                                    if (!cancel) {
                                        network.charge(it.token.toString(), phone, object : ResponseListener {
                                            override fun onSuccess(message: String) {
                                                Log.d(AppManager.TAG, message)
                                                when (message) {
                                                    "true" -> {
                                                        SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
                                                            .setTitleText("شارژ با موفقیت انجام شد")
                                                            .setConfirmText("باشه")
                                                            .show()
                                                    }
                                                    "false" -> {
                                                        SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                                            .setTitleText("موجودی شما کافی نمی‌باشد")
                                                            .setConfirmText("باشه")
                                                            .show()
                                                    }
                                                    "null" -> {
                                                        SweetAlertDialog(activity, SweetAlertDialog.NORMAL_TYPE)
                                                            .setTitleText("درخواست شما بررسی می‌شود")
                                                            .setConfirmText("باشه")
                                                            .show()
                                                    }
                                                    else -> {
                                                        SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                                            .setTitleText(message)
                                                            .setConfirmText("باشه")
                                                            .show()
                                                    }
                                                }
                                                pb_charge.visibility = View.INVISIBLE
                                            }

                                            override fun onFailure(message: String) {
                                                Log.d(AppManager.TAG, message)
                                                SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                                    .setTitleText(message)
                                                    .setConfirmText("باشه")
                                                    .show()
                                                pb_charge.visibility = View.INVISIBLE
                                            }

                                        })
                                    }
                                }
                            })
                    }
                }
            }
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
                    pb_ads.visibility = View.VISIBLE
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
        pb_ads.visibility = View.INVISIBLE
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
