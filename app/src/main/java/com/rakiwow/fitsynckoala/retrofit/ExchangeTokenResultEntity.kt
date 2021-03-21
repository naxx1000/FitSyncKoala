package com.rakiwow.fitsynckoala.retrofit

import com.rakiwow.fitsynckoala.model.Athlete
import com.google.gson.annotations.SerializedName

data class ExchangeTokenResultEntity(
    @SerializedName("expires_at") val expiresAt: Int,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("athlete") val athele: Athlete
)