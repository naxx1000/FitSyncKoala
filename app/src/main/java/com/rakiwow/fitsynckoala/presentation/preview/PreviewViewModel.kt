package com.rakiwow.fitsynckoala.presentation.preview

import android.app.Application
import androidx.lifecycle.*
import com.rakiwow.fitsynckoala.model.RefreshTokenResult
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
class PreviewViewModel
@Inject constructor(
    private val mainRepository: MainRepository,
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val instanceManager = InstanceManager(application)

    private val _refreshTokenResult: MutableLiveData<Event<DataState<RefreshTokenResult>>> =
        MutableLiveData()
    val refreshTokenResult: LiveData<Event<DataState<RefreshTokenResult>>>
        get() = _refreshTokenResult

    val expiresAtFlow = instanceManager.expiresAtFlow.asLiveData()
    val refreshTokenFlow = instanceManager.refreshTokenFlow.asLiveData()

    fun fetchRefreshTokenResult(clientId: Int, clientSecret: String, refreshToken: String) =
        viewModelScope.launch {
            mainRepository.refreshTokenResult(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            ).onEach { dataState ->
                _refreshTokenResult.value = Event(dataState)
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