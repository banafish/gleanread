package com.gleanread.android.data.appearance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance_preferences")

class AppearancePreferencesRepository(private val context: Context) {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val THEME_COLOR_KEY = stringPreferencesKey("theme_color")
    private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")

    val themeModeFlow: Flow<ThemeMode> = context.appearanceDataStore.data.map { preferences ->
        val modeStr = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val themeColorFlow: Flow<ThemeColor> = context.appearanceDataStore.data.map { preferences ->
        val colorStr = preferences[THEME_COLOR_KEY] ?: ThemeColor.DYNAMIC.name
        try {
            ThemeColor.valueOf(colorStr)
        } catch (e: Exception) {
            ThemeColor.DYNAMIC
        }
    }
    
    val avatarUrlFlow: Flow<String?> = context.appearanceDataStore.data.map { preferences ->
        preferences[AVATAR_URL_KEY]
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appearanceDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setThemeColor(color: ThemeColor) {
        context.appearanceDataStore.edit { preferences ->
            preferences[THEME_COLOR_KEY] = color.name
        }
    }
    
    suspend fun setAvatarUrl(url: String?) {
        context.appearanceDataStore.edit { preferences ->
            if (url == null) {
                preferences.remove(AVATAR_URL_KEY)
            } else {
                preferences[AVATAR_URL_KEY] = url
            }
        }
    }
}
