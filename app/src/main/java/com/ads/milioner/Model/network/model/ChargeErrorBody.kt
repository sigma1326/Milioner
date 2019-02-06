package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class ChargeErrorBody(
    @SerializedName("non_field_errors") var Errors: List<String>? = listOf()
)
