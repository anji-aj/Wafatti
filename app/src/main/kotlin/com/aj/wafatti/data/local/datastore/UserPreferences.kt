package com.aj.wafatti.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wafatti_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val KEY_CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_USER_ID]
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    suspend fun saveCurrentUserId(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENT_USER_ID] = userId
        }
    }

    suspend fun clearCurrentUserId() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_CURRENT_USER_ID)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = enabled
        }
    }
}
