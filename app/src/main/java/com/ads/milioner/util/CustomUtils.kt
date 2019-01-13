package com.ads.milioner.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager


object CustomUtils {

    fun gpsStatus(context: Context): Boolean {
        val provider =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
        return provider.contains("gps")
    }

    fun requestEnableGPS(context: Context) {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun getDistance(
        startLatitude: Double, startLongitude: Double,
        endLatitude: Double, endLongitude: Double
    ): Float {
        val arr = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, arr)
        return arr[0]
    }

    fun fullScreen(activity: Activity, window: Window) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    fun hideKeyboard(activity: Activity?) {
        val inputMethodManager = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focusView = activity.currentFocus
        if (focusView != null) {
            inputMethodManager.hideSoftInputFromWindow(focusView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
        if (activity.currentFocus != null) {
            activity.currentFocus!!.clearFocus()
        }
    }

    fun rotateSmoothView(view: View) {
        val rotate = RotateAnimation(0F, 360F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotate.duration = 500
        rotate.repeatCount = 0
        view.animation = rotate
    }

    fun getHandler() = Handler(Looper.getMainLooper())

    fun isConnectedToNetwork(activity: Context): Boolean {
        val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isAvailable && networkInfo.isConnected
    }

}

