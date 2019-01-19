package com.ads.milioner.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel;
import com.ads.milioner.Model.database.model.User
import com.ads.milioner.ads.InterstitialFetcher
import com.inmobi.ads.InMobiInterstitial
import java.util.concurrent.atomic.AtomicInteger

class GameViewModel : ViewModel() {
    lateinit var user: LiveData<User>
    var forcedRetry = AtomicInteger(0)
    var mInterstitialAd: InMobiInterstitial? = null
    lateinit var interstitialFetcher: InterstitialFetcher
}
