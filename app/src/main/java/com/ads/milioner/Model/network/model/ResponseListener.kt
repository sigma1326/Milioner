package com.ads.milioner.Model.network.model

interface ResponseListener {
    fun onSuccess(message: String)
    fun onFailure(message: String)
}
