package com.ads.milioner.View

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.R
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import org.koin.android.ext.android.inject

class LoginActivity : AppCompatActivity() {
    private val db: DataBaseRepositoryImpl by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = db.getUser()
        if (user == null) {
            setContentView(R.layout.activity_login)
        } else {
            user.phone?.let {
                if (!it.isEmpty() && it.length > 2) {
                    if (it.substring(0, 2) == "98") {
                        startNormalMode()
                    } else {
                        startForeignMode()
                    }
                } else {
                    startForeignMode()
                }
            }
        }
    }

    private fun startForeignMode() {
        this.finish()
        startActivity(Intent(this, MainActivityForeignMode::class.java))
    }

    private fun startNormalMode() {
        this.finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onSupportNavigateUp()
        return findNavController(R.id.login_nav_host_fragment).navigateUp()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }


}
