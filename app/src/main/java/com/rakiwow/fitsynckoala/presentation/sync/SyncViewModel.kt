package com.rakiwow.fitsynckoala.presentation.sync

import android.app.Application
import androidx.lifecycle.*
import com.rakiwow.fitsynckoala.repository.MainRepository
import com.rakiwow.fitsynckoala.util.DataState
import com.rakiwow.fitsynckoala.util.Event
import com.rakiwow.fitsynckoala.util.InstanceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SyncViewModel
@Inject constructor(
    private val mainRepository: MainRepository,
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val instanceManager = InstanceManager(application)

    private val _createActivityResult: MutableLiveData<Event<DataState<Boolean>>> =
        MutableLiveData()
    val createActivityResult: LiveData<Event<DataState<Boolean>>>
        get() = _createActivityResult

    val accessTokenFlow = instanceManager.accessTokenFlow.asLiveData()
    val expiresAtFlow = instanceManager.expiresAtFlow.asLiveData()
    val expiresInFlow = instanceManager.expiresInFlow.asLiveData()
    val refreshTokenFlow = instanceManager.refreshTokenFlow.asLiveData()
    val lastActivityNameFlow = instanceManager.lastActivityNameFlow.asLiveData()

    private val _activityTimeInSeconds: MutableLiveData<Int> = MutableLiveData()
    val activityTimeInSeconds: LiveData<Int>
        get() = _activityTimeInSeconds

    private val _activityDistanceInMeters: MutableLiveData<Float> = MutableLiveData()
    val activityDistanceInMeters: LiveData<Float>
        get() = _activityDistanceInMeters

    private val _activityKcal: MutableLiveData<Float> = MutableLiveData()
    val activityKcal: LiveData<Float>
        get() = _activityKcal

    fun createActivity(
        name: String,
        type: String,
        date: Date,
        elapsedTime: Int,
        description: String,
        distance: Float
    ) = viewModelScope.launch {
        accessTokenFlow.value?.let { token ->
            mainRepository.createActivity(
                token = token,
                name = name,
                type = type,
                date = date,
                elapsedTime = elapsedTime,
                description = description,
                distance = distance
            ).onEach { dataState ->
                _createActivityResult.value = Event(dataState)
            }.launchIn(viewModelScope)
        }
    }

    fun getFormattedTextFromSeconds(seconds: Int): String {
        val longVal: Long = seconds.toLong()
        val hours = longVal.toInt() / 3600
        var remainder = longVal.toInt() - hours * 3600
        val mins = remainder / 60
        remainder -= mins * 60
        val secs = remainder

        return "$hours hr, $mins min, $secs sec"
    }

    fun setActivityTimeInSeconds(seconds: Int) {
        _activityTimeInSeconds.value = seconds
    }

    fun setActivityDistanceInMeters(meters: Float) {
        _activityDistanceInMeters.value = meters
    }

    fun setActivityKcal(kcal: Float) {
        _activityKcal.value = kcal
    }

    fun getTotalSeconds(hour: Int = 0, min: Int = 0, sec: Int = 0): Int {
        return (hour * 60 * 60) + (min * 60) + sec
    }

    fun getHourMinSecFromSeconds(seconds: Int): Triple<Int, Int, Int> {
        val longVal: Long = seconds.toLong()
        val hours = longVal.toInt() / 3600
        var remainder = longVal.toInt() - hours * 3600
        val mins = remainder / 60
        remainder -= mins * 60
        val secs = remainder

        return Triple(hours, mins, secs)
    }

    fun persistLastActivityName(lastActivityName: String) = viewModelScope.launch {
        instanceManager.storeLastPickedActivityName(lastActivityName)
    }
}