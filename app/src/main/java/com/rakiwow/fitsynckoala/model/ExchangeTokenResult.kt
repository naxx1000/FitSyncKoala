package com.rakiwow.fitsynckoala.model

data class ExchangeTokenResult(
    val accessToken: String,
    val athlete: Athlete,
    val expiresAt: Int,
    val expiresIn: Int,
    val refreshToken: String,
    val tokenType: String
)