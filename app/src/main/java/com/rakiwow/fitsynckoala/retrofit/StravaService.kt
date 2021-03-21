package com.rakiwow.fitsynckoala.retrofit

import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.*

interface StravaService {

    @POST("api/v3/oauth/token")
    suspend fun exchangeTokenResult(
        @Query("client_id") clientId: Int,
        @Query("client_secret") clientSecret: String,
        @Query("code") code: String,
        @Query("grant_type") grantType: String = "authorization_code"
    ): ExchangeTokenResultEntity

    @POST("api/v3/oauth/token")
    suspend fun refreshTokenResult(
        @Query("client_id") clientId: Int,
        @Query("client_secret") clientSecret: String,
        @Query("grant_type") grantType: String = "refresh_token",
        @Query("refresh_token") refreshToken: String
    ): RefreshTokenResultEntity

    @POST("api/v3/activities")
    suspend fun createActivity(
        @Header("Authorization") authHeader: String,
        @Query("name") name: String,
        @Query("type") type: String,
        @Query("start_date_local") date: Date,
        @Query("elapsed_time") elapsedTime: Int,
        @Query("description") description: String,
        @Query("distance") distance: Float
    )
}