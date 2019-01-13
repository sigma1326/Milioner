package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("phone") var phone: String? = ""
)