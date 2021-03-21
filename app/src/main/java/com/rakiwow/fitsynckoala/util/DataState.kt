package com.rakiwow.fitsynckoala.util

import retrofit2.HttpException

sealed class DataState<out R> {

    data class Success<out T>(val data: T): DataState<T>()
    data class Error(val exception: HttpException): DataState<Nothing>()
    object Loading: DataState<Nothing>()
}