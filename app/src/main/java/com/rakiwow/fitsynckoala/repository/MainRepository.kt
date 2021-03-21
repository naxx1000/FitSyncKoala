package com.rakiwow.fitsynckoala.repository

import com.rakiwow.fitsynckoala.model.ExchangeTokenResult
import com.rakiwow.fitsynckoala.model.RefreshTokenResult
import com.rakiwow.fitsynckoala.retrofit.ExchangeTokenMapper
import com.rakiwow.fitsynckoala.retrofit.RefreshTokenMapper
import com.rakiwow.fitsynckoala.retrofit.StravaService
import com.rakiwow.fitsynckoala.util.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

class MainRepository
@Inject constructor(
    private val service: StravaService,
    private val exchangeTokenMapper: ExchangeTokenMapper,
    private val refreshTokenMapper: RefreshTokenMapper
) {

    suspend fun getExchangeResult(
        clientId: Int, clientSecret: String, code: String
    ): Flow<DataState<ExchangeTokenResult>> = flow {
        emit(DataState.Loading)
        try {
            val networkExchangeTokenResult =
                service.exchangeTokenResult(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    code = code
                )
            val exchangeTokenResult = exchangeTokenMapper.mapFromEntity(networkExchangeTokenResult)
            emit(DataState.Success(exchangeTokenResult))
        } catch (e: HttpException) {
            emit(DataState.Error(e))
        }
    }

    suspend fun refreshTokenResult(
        clientId: Int, clientSecret: String, refreshToken: String
    ): Flow<DataState<RefreshTokenResult>> = flow {
        emit(DataState.Loading)
        try {
            val networkRefreshTokenResult =
                service.refreshTokenResult(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    refreshToken = refreshToken
                )
            val refreshTokenResult = refreshTokenMapper.mapFromEntity(networkRefreshTokenResult)
            emit(DataState.Success(refreshTokenResult))
        } catch (e: HttpException) {
            emit(DataState.Error(e))
        }
    }

    suspend fun createActivity(
        token: String,
        name: String,
        type: String,
        date: Date,
        elapsedTime: Int,
        description: String,
        distance: Float
    ): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading)
        try {
            service.createActivity(
                "Bearer $token",
                name = name,
                type = type,
                date = date,
                elapsedTime = elapsedTime,
                description = description,
                distance = distance
            )
            emit(DataState.Success(true))
        } catch (e: HttpException) {
            emit(DataState.Error(e))
        }
    }
}