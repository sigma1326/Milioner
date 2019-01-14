package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class CheckIPResponse(
    @SerializedName("status") var status: Boolean? = false
    , @SerializedName("ip") var ip: String? = ""
    , @SerializedName("country") var country: Country? = null
)

data class Country(
    @SerializedName("country_code") var countryCode: String? = ""
    , @SerializedName("country_name") var countryName: String? = ""
)