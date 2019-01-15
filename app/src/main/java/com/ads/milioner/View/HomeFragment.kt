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
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.R
import com.ads.milioner.ViewModel.HomeViewModel
import com.ads.milioner.ads.InterstitialFetcher
import com.ads.milioner.ads.PlacementId
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
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


    }


    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        Log.d(AppManager.TAG, "start")
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        viewModel.user = db.getUserLiveData()
        viewModel.user.observe(activity!!, Observer {
            if (it != null && tv_balance != null) {
                Handler(Looper.getMainLooper()).postDelayed({ tv_balance?.text = it.balance.toString() }, 1500)
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
        pb_ads.visibility = View.VISIBLE
        if (!AppManager.isPlayingAd) {
            setupInterstitial()
            viewModel.mInterstitialAd?.load()
            viewModel.mInterstitialAd?.show()
        }
    }

    private fun charge() {
        db.getUser().let {
            network.charge(it?.token.toString(), object : ResponseListener {
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
