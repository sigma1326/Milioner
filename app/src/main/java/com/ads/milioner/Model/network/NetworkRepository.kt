package com.ads.milioner.Model.network

import com.ads.milioner.Model.network.model.ResponseListener

interface NetworkRepository {
    fun login(phone: String, code: String, responseListener: ResponseListener)
    fun register(phone: String, responseListener: ResponseListener)
    fun me(token: String, responseListener: ResponseListener)
    fun refresh(token: String, responseListener: ResponseListener)
    fun charge(token: String, responseListener: ResponseListener)
    fun checkIP(responseListener: ResponseListener)
    fun ads(token: String, gid: String, hash: String, timeStamp: String, responseListener: ResponseListener)
}