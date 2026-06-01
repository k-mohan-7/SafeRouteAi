package com.saferoute.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("session")

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val userIdKey = intPreferencesKey("user_id")
    private val fullnameKey = stringPreferencesKey("fullname")

    val userId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[userIdKey]
    }

    val fullname: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[fullnameKey]
    }

    suspend fun saveSession(userId: Int, fullname: String) {
        context.dataStore.edit { prefs ->
            prefs[userIdKey] = userId
            prefs[fullnameKey] = fullname
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    fun getUserIdBlocking(): Int? = runBlocking {
        context.dataStore.data.first()[userIdKey]
    }
}
