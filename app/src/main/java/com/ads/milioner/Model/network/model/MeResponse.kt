package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class MeResponse(
    @SerializedName("phone") var phone: String? = ""
    , @SerializedName("money") var money: Long? = 0
    , @SerializedName("api_key") var apiKey: String? = ""
)