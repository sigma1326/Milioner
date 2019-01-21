package com.ads.milioner.View

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.R
import com.jakewharton.rxbinding2.view.RxView
import com.simorgh.sweetalertdialog.SweetAlertDialog
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = db.getUser()
        user?.token?.let {
            network.refresh(it, object : ResponseListener {
                override fun onSuccess(message: String) {
                    network.me(it, object : ResponseListener {
                        override fun onSuccess(message: String) {
                            Log.d(AppManager.TAG, message)
                        }

                        override fun onFailure(message: String) {
                            Log.d(AppManager.TAG, message)
                        }
                    })
                }

                override fun onFailure(message: String) {
                    this@MainActivity.let {
                        SweetAlertDialog(it, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText(message)
                            .setConfirmText("باشه")
                            .show()
                    }
                }
            })
        }

        if (img_clear_data != null) {
            RxView.clicks(img_clear_data).subscribe {
                this.let {
                    SweetAlertDialog(it, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("پاک کردن اطلاعات برنامه")
                        .setContentText("آیا اطمینان دارید؟")
                        .setConfirmText("باشه")
                        .setCancelText("بی‌خیال")
                        .setConfirmClickListener { sad ->
                            Handler(Looper.getMainLooper()).post {
                                db.clearData()
                                this@MainActivity.finish()
                                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            }
                            sad.dismissWithAnimation()
                        }
                        .show()
                }
            }
        }


    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

}
