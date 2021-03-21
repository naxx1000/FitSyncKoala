package com.rakiwow.fitsynckoala.presentation.login

import android.app.Application
import androidx.lifecycle.*
import com.rakiwow.fitsynckoala.model.ExchangeTokenResult
import com.rakiwow.fitsynckoala.repository.MainRepository
import com.rakiwow.fitsynckoala.util.DataState
import com.rakiwow.fitsynckoala.util.Event
import com.rakiwow.fitsynckoala.util.InstanceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
@Inject constructor(
    private val mainRepository: MainRepository,
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val instanceManager = InstanceManager(application)

    private val _exchangeTokenResult: MutableLiveData<Event<DataState<ExchangeTokenResult>>> =
        MutableLiveData()
    val exchangeTokenResult: LiveData<Event<DataState<ExchangeTokenResult>>>
        get() = _exchangeTokenResult

    val accessTokenFlow = instanceManager.accessTokenFlow.asLiveData()
    val expiresAtFlow = instanceManager.expiresAtFlow.asLiveData()
    val expiresInFlow = instanceManager.expiresInFlow.asLiveData()
    val refreshTokenFlow = instanceManager.refreshTokenFlow.asLiveData()

    fun fetchExchangeTokenResult(clientId: Int, clientSecret: String, code: String) =
        viewModelScope.launch {
            mainRepository.getExchangeResult(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code
            ).onEach { dataState ->
                _exchangeTokenResult.value = Event(dataState)
            }.launchIn(viewModelScope)
        }

    fun persistInstance(
        accessToken: String,
        expiresAt: Int,
        expiresIn: Int,
        refreshToken: String
    ) = viewModelScope.launch {
        instanceManager.storeInstance(
            accessToken,
            expiresAt,
            expiresIn,
            refreshToken
        )
    }
}