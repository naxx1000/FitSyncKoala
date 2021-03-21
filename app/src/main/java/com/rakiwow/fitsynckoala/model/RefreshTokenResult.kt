package com.rakiwow.fitsynckoala.model

data class RefreshTokenResult(
    val accessToken: String,
    val expiresAt: Int,
    val expiresIn: Int,
    val refreshToken: String,
    val tokenType: String
)
