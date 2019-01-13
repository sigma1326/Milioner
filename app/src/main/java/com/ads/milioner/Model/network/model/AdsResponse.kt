package com.ads.milioner.Model.network.model

import com.google.gson.annotations.SerializedName

data class AdsResponse(
    @SerializedName("reward") var reward: Long? = 0
    , @SerializedName("money") var money: Long? = 0
)