package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class LoginSuccess(
    @SerializedName("token") var token: String? = ""
    , @SerializedName("user") var user: User? = null
)

data class User(
    @SerializedName("phone") var phone: String? = ""
    , @SerializedName("money") var money: Long? = 0
)
