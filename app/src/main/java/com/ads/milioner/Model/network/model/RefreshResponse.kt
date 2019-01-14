package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class RefreshResponse(
    @SerializedName("token") var token: String? = ""
)