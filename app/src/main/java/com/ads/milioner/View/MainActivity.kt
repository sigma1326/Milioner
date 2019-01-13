package com.ads.milioner.View

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.network.NetworkRepositoryImpl
import com.ads.milioner.Model.network.model.ResponseListener
import com.ads.milioner.R
import com.jakewharton.rxbinding2.view.RxView
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
            network.me(it, object : ResponseListener {
                override fun onSuccess(message: String) {
                }

                override fun onFailure(message: String) {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            })
        }

        if (img_clear_data != null) {
            RxView.clicks(img_clear_data).subscribe {
                val builder: AlertDialog.Builder
                this.let {
                    builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlertDialog.Builder(it, android.R.style.Theme_Material_Dialog_Alert)
                    } else {
                        AlertDialog.Builder(it)
                    }
                    builder.setTitle("پاک کردن اطلاعات برنامه")
                        .setMessage("آیا اطمینان دارید؟")
                        .setPositiveButton("بله") { dialog, which ->
                            Handler(Looper.getMainLooper()).post {
                                db.clearData()
                                it.startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            }
                        }
                        .setNegativeButton("خیر") { dialog, which -> }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                }
            }
        }


    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }


}
