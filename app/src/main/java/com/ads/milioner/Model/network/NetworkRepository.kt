package com.ads.milioner.Model.network

import androidx.annotation.Keep
import com.ads.milioner.Model.network.model.ResponseListener

@Keep
interface NetworkRepository {
    fun login(phone: String, code: String, responseListener: ResponseListener)
    fun register(phone: String, responseListener: ResponseListener)
    fun me(token: String, responseListener: ResponseListener)
    fun refresh(token: String, responseListener: ResponseListener)
    fun charge(token: String, phone: String, responseListener: ResponseListener)
    fun checkIP(responseListener: ResponseListener)
    fun checkAds(responseListener: ResponseListener)
    fun ads(token: String, gid: String, hash: String, timeStamp: String, responseListener: ResponseListener)
}