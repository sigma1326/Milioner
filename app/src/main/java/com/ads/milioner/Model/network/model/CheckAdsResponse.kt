package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class CheckAdsResponse(
    @SerializedName("status") var status: Boolean? = false
    , @SerializedName("ip") var ip: String? = ""
    , @SerializedName("country") var country: Country? = null
    , @SerializedName("id") var id: Long? = 0
    , @SerializedName("adProvider") var adProvider: String? = ""
)