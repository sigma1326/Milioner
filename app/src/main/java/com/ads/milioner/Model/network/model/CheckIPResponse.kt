package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class CheckIPResponse(
    @SerializedName("status") var status: Boolean? = false
)