package com.ads.milioner.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.database.model.User
import com.ads.milioner.ads.InterstitialFetcher
import com.inmobi.ads.InMobiInterstitial
import java.util.concurrent.atomic.AtomicInteger

class HomeViewModel : ViewModel() {
    lateinit var user: LiveData<User>
    var forcedRetry = AtomicInteger(0)
    var mInterstitialAd: InMobiInterstitial? = null
    lateinit var interstitialFetcher: InterstitialFetcher

    var needToReloadAd = false

    fun init(db: DataBaseRepositoryImpl) {
        user = db.getUserLiveData()
    }

}
