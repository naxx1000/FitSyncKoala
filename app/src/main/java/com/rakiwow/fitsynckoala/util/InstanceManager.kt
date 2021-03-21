package com.rakiwow.fitsynckoala.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InstanceManager(context: Context) {

    private val dataStore = context.createDataStore(name = "instanceStore")

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("ACCESS_TOKEN")
        val EXPIRES_AT = intPreferencesKey("EXPIRES_AT")
        val EXPIRES_IN = intPreferencesKey("EXPIRES_IN")
        val REFRESH_TOKEN = stringPreferencesKey("REFRESH_TOKEN")
        val LAST_ACTIVITY_NAME = stringPreferencesKey("LAST_ACTIVITY_NAME")
    }

    suspend fun storeInstance(
        accessToken: String,
        expiresAt: Int,
        expiresIn: Int,
        refreshToken: String
    ) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[EXPIRES_AT] = expiresAt
            prefs[EXPIRES_IN] = expiresIn
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun storeLastPickedActivityName(lastActivityName: String) {
        dataStore.edit { prefs ->
            prefs[LAST_ACTIVITY_NAME] = lastActivityName
        }
    }

    val accessTokenFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN] ?: ""
    }

    val expiresAtFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[EXPIRES_AT] ?: 0
    }

    val expiresInFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[EXPIRES_IN] ?: 0
    }

    val refreshTokenFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN] ?: ""
    }

    val lastActivityNameFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[LAST_ACTIVITY_NAME] ?: ""
    }
}