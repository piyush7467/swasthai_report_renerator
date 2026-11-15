package com.piyush.swasthaireportgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.piyush.swasthaireportgenerator.ui.auth.AuthViewModel
import com.piyush.swasthaireportgenerator.ui.components.ThemeSelectorDialog
import com.piyush.swasthaireportgenerator.ui.navigation.AppNavHost
import com.piyush.swasthaireportgenerator.ui.theme.SwasthAiTheme
import com.piyush.swasthaireportgenerator.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            // Collect theme state reactively using modern non-deprecated collectAsStateWithLifecycle
            val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()
            var showThemeDialog by remember { mutableStateOf(false) }

            SwasthAiTheme(
                themeMode = themeState.mode,
                palette = themeState.palette,
                amoledDark = themeState.amoledDark
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    if (showThemeDialog) {
                        ThemeSelectorDialog(
                            currentMode = themeState.mode,
                            currentPalette = themeState.palette,
                            currentAmoledDark = themeState.amoledDark,
                            onModeSelected = { themeViewModel.setThemeMode(it) },
                            onPaletteSelected = { themeViewModel.setThemePalette(it) },
                            onAmoledDarkChanged = { themeViewModel.setAmoledDark(it) },
                            onDismissRequest = { showThemeDialog = false }
                        )
                    }

                    AppNavHost(
                        navController = navController,
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel,
                        onOpenThemeDialog = { showThemeDialog = true }
                    )
                }
            }
        }
    }
}
