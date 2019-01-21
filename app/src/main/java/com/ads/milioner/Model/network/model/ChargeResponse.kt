package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class ChargeResponse(
    @SerializedName("success") var success: Boolean? = false
    , @SerializedName("money") var money: Long? = 0
    , @SerializedName("id") var id: Long? = 0
    , @SerializedName("phone") var phone: String? = ""
)