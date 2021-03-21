package com.rakiwow.fitsynckoala.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import javax.inject.Singleton
import com.rakiwow.fitsynckoala.retrofit.StravaService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Singleton
    @Provides
    fun provideJsonBuilder(): Gson {
        return GsonBuilder().create()
    }

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson): Retrofit.Builder {
        return Retrofit.Builder().baseUrl("https://www.strava.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
    }

    @Singleton
    @Provides
    fun provideStravaService(retrofit: Retrofit.Builder): StravaService {
        return retrofit.build().create(StravaService::class.java)
    }
}