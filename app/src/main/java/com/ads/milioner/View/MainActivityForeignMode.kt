package com.ads.milioner.View

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.R
import com.jakewharton.rxbinding2.view.RxView
import com.simorgh.sweetalertdialog.SweetAlertDialog
import kotlinx.android.synthetic.main.activity_main_foreign_mode.*
import org.koin.android.ext.android.inject

class MainActivityForeignMode : AppCompatActivity() {

    private val network: NetworkRepositoryImpl by inject()
    private val db: DataBaseRepositoryImpl by inject()


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main_foreign_mode)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
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
    }

    companion object {
        @JvmField
        @Volatile
        var running: Boolean = false
    }
}
