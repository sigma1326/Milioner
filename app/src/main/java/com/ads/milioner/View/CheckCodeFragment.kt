package com.ads.milioner.View

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.R
import com.ads.milioner.ViewModel.CheckCodeViewModel
import com.ads.milioner.util.CustomUtils
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.check_code_fragment.*
import kotlinx.android.synthetic.main.check_code_fragment.view.*
import org.koin.android.ext.android.inject

@SuppressLint("CheckResult")
class CheckCodeFragment : Fragment() {

    private lateinit var viewModel: CheckCodeViewModel
    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()

    var timer: CountDownTimer? = null

    lateinit var disposable: Disposable
    var isConnected = false

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        disposable = ReactiveNetwork
            .observeInternetConnectivity(AppManager.settings)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                isConnected = it
            }

        if (timer == null) {
            timer = object : CountDownTimer(90000, 1000) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    second = (millisUntilFinished / 1000).toInt()
                    if (btn_resend != null) {
                        btn_resend.setTextColor(Color.BLACK)
                        btn_resend.text = "$second Until resend"
                    }
                }

                override fun onFinish() {
                    isDone = true
                    if (btn_resend != null) {
                        btn_resend.setTextColor(Color.parseColor("#70A1FF"))
                        btn_resend.text = activity?.getString(R.string.resend)
                    }

                }
            }
            (timer as CountDownTimer).start()
        }
    }

    var isDone = false
    var second = 90


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.check_code_fragment, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.btn_wrong_number.setOnClickListener {
            CustomUtils.hideKeyboard(activity)
            findNavController().navigateUp()
        }



        tv_hint?.text = "Please Enter Activation Code Sent to  ${AppManager.phone}"


        RxTextView.afterTextChangeEvents(et_code!!)
            .skipInitialValue()
            .subscribe {
                val text = et_code.text.toString()
                if (text.length < 5 || text.isEmpty()) {
                    btn_login.isEnabled = false
                    btn_login.setTextColor(Color.parseColor("#A6A6A6"))
                    btn_login.background = activity?.resources?.getDrawable(R.drawable.btn_disabled_bkg, null)
                } else {
                    btn_login.isEnabled = true
                    btn_login.setTextColor(Color.parseColor("#ffffff"))
                    btn_login.background = activity?.resources?.getDrawable(R.drawable.button_bkg, null)
                }
            }

        view.btn_login.setOnClickListener {
            CustomUtils.hideKeyboard(activity)
            if (progressBar.visibility != View.VISIBLE) {
                progressBar.visibility = View.VISIBLE
                btn_login.text = ""
                if (isConnected) {
                    login()
                } else {
                    updateState("دستگاه به اینترنت متصل نیست", true)
                }
            }
        }

        RxView.clicks(btn_resend).filter {
            isDone && progressBar.visibility == View.GONE
        }.subscribe {
            CustomUtils.hideKeyboard(activity)
            if (progressBar.visibility != View.VISIBLE) {
                progressBar.visibility = View.VISIBLE
                btn_login.text = ""
                if (isConnected) {
                    resend()
                } else {
                    updateState("دستگاه به اینترنت متصل نیست", true)
                }
            }
        }


    }

    private fun resend() {
        network.register(AppManager.phone, object : ResponseListener {
            override fun onSuccess(message: String) {
                Log.d(AppManager.TAG, message)
                updateState(message, false)
                Toast.makeText(activity, "کد با موفقیت ارسال شد", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(message: String) {
                Log.d(AppManager.TAG, message)
                updateState(message, true)
            }

        })
    }

    private fun login() {
        network.login(AppManager.phone, et_code.text.toString(), object : ResponseListener {
            override fun onSuccess(message: String) {
                Log.d(AppManager.TAG, message)
                AppManager.token.let {
                    network.me(it, object : ResponseListener {
                        override fun onSuccess(message: String) {
                            Log.d(AppManager.TAG, message)
                            updateState(message, false)
                            activity?.finish()
                            activity?.startActivity(Intent(activity, MainActivity::class.java))
                        }

                        override fun onFailure(message: String) {
                            Log.d(AppManager.TAG, message)
                            updateState(message, true)
                        }
                    })
                }
            }

            override fun onFailure(message: String) {
                Log.d(AppManager.TAG, message)
                updateState(message, true)
            }

        })
        activity?.finish()
        activity?.startActivity(Intent(this.activity, MainActivity::class.java))
    }

    private fun updateState(message: String, isError: Boolean) {
        try {
            if (tv_error == null) {
                return
            }
            if (isError) {
                tv_error.text = message
                tv_error.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                btn_login.text = activity?.getString(R.string.login)
            } else {
                tv_error.text = ""
                tv_error.visibility = View.GONE
                progressBar.visibility = View.GONE
                btn_login.text = activity?.getString(R.string.login)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CheckCodeViewModel::class.java)
    }

}
