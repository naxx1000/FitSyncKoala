package com.rakiwow.fitsynckoala.di

import com.rakiwow.fitsynckoala.repository.MainRepository
import com.rakiwow.fitsynckoala.retrofit.ExchangeTokenMapper
import com.rakiwow.fitsynckoala.retrofit.RefreshTokenMapper
import com.rakiwow.fitsynckoala.retrofit.StravaService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideMainRepository(
        stravaService: StravaService,
        exchangeTokenMapper: ExchangeTokenMapper,
        refreshTokenMapper: RefreshTokenMapper
    ): MainRepository {
        return MainRepository(stravaService, exchangeTokenMapper, refreshTokenMapper)
    }
}