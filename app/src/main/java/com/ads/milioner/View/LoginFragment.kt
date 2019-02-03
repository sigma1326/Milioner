package com.ads.milioner.View

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.database.model.User
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.ViewModel.LoginViewModel
import com.ads.milioner.util.CustomUtils
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.mukesh.countrypicker.CountryPicker
import com.transitionseverywhere.TransitionManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.login_fragment.*
import kotlinx.android.synthetic.main.login_fragment.view.*
import org.koin.android.ext.android.inject


class LoginFragment : Fragment() {

    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()


    private lateinit var viewModel: LoginViewModel


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

        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(com.ads.milioner.R.layout.login_fragment, container, false)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TransitionManager.beginDelayedTransition((view as ViewGroup?)!!)

        countryCodeHolder.text = viewModel.countryCode

        RxTextView.afterTextChangeEvents(view.et_code!!)
            .skipInitialValue()
            .subscribe {
                val text = view.et_code.text.toString()
                var max = 10

                if (!view.countryCodeHolder.text.isEmpty()) {
                    max = if (view.countryCodeHolder.text.toString() == "+98") {
                        10
                    } else {
                        5
                    }
                }
                if (text.length < max || text.isEmpty()) {
                    btn_login.isEnabled = false
                    btn_login.setTextColor(Color.parseColor("#A6A6A6"))
                    btn_login.background =
                        activity?.resources?.getDrawable(com.ads.milioner.R.drawable.btn_disabled_bkg, null)
                } else {
                    btn_login.isEnabled = true
                    btn_login.setTextColor(Color.parseColor("#ffffff"))
                    btn_login.background =
                        activity?.resources?.getDrawable(com.ads.milioner.R.drawable.button_bkg, null)
                }
            }

        RxView.clicks(view.countryCodeHolder!!).subscribe {
            val builder = CountryPicker.Builder().with(activity!!)
                .listener {
                    view.countryCodeHolder.text = it.dialCode.toString()
                    viewModel.countryCode = it.dialCode.toString()
                }
            builder.build().showDialog(activity!!)
        }

        view.btn_login.setOnClickListener {
            CustomUtils.hideKeyboard(activity)
            if (countryCodeHolder.text.isEmpty()) {
                updateState("Country Code cannot be empty!", true)
            } else if (progressBar.visibility != View.VISIBLE) {
                progressBar.visibility = View.VISIBLE
                btn_login.text = ""
                if (isConnected) {
                    login()
                } else {
                    updateState("No Internet Connection!", true)
                }
            }

        }
    }

    private fun login() {
        if (countryCodeHolder.text.substring(1) == "98") {
            network.register("${countryCodeHolder.text.substring(1)}${et_code.text}", object : ResponseListener {
                override fun onSuccess(message: String) {
                    Log.d(AppManager.TAG, message)
                    updateState(message, false)

                    try {
                        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
                        settings.edit { clear() }
                        findNavController().navigate(com.ads.milioner.R.id.action_loginFragment_to_checkCodeFragment)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(message: String) {
                    Log.d(AppManager.TAG, message)
                    updateState(message, true)
                }

            })
        } else {
            updateState("", false)
            db.clearData()
            var user = db.getUser()
            if (user == null) {
                user = User(
                    userID = 1,
                    phone = et_code.text.toString(),
                    balance = 0,
                    token = ""
                )
                db.insertUser(user)
                try {
                    val settings = PreferenceManager.getDefaultSharedPreferences(activity)
                    settings.edit { clear() }
                } catch (e: Exception) {
                }

            }
            activity?.finish()
            activity?.startActivity(Intent(activity, MainActivityForeignMode::class.java))
        }


    }

    private fun updateState(message: String, isError: Boolean) {
        if (isError) {
            tv_error.text = message
            tv_error.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            btn_login.text = activity?.getString(com.ads.milioner.R.string.get_activation_code)
        } else {
            tv_error.text = ""
            tv_error.visibility = View.GONE
            progressBar.visibility = View.GONE
            btn_login.text = activity?.getString(com.ads.milioner.R.string.get_activation_code)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

}
