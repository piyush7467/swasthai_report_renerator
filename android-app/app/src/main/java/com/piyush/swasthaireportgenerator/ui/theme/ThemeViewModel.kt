package com.piyush.swasthaireportgenerator.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.local.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.SWASTHAI_TEAL,
    val amoledDark: Boolean = false
)

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val themePreferences = ThemePreferences(application)

    val themeState: StateFlow<ThemeState> = combine(
        themePreferences.themeMode,
        themePreferences.themePalette,
        themePreferences.amoledDark
    ) { mode, palette, amoledDark ->
        ThemeState(mode = mode, palette = palette, amoledDark = amoledDark)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setThemePalette(palette: ThemePalette) {
        viewModelScope.launch {
            themePreferences.setThemePalette(palette)
        }
    }

    fun setAmoledDark(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setAmoledDark(enabled)
        }
    }
}
