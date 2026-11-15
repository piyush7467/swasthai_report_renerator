package com.piyush.swasthaireportgenerator.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.piyush.swasthaireportgenerator.ui.theme.ThemeMode
import com.piyush.swasthaireportgenerator.ui.theme.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_THEME_PALETTE = stringPreferencesKey("theme_palette")
        private val KEY_AMOLED_DARK = booleanPreferencesKey("amoled_dark")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { preferences ->
        val modeName = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val themePalette: Flow<ThemePalette> = context.themeDataStore.data.map { preferences ->
        val paletteName = preferences[KEY_THEME_PALETTE] ?: ThemePalette.SWASTHAI_TEAL.name
        try {
            ThemePalette.valueOf(paletteName)
        } catch (e: Exception) {
            ThemePalette.SWASTHAI_TEAL
        }
    }

    val amoledDark: Flow<Boolean> = context.themeDataStore.data.map { preferences ->
        preferences[KEY_AMOLED_DARK] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_PALETTE] = palette.name
        }
    }

    suspend fun setAmoledDark(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_AMOLED_DARK] = enabled
        }
    }
}
