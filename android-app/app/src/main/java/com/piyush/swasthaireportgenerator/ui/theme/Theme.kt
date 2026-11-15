package com.piyush.swasthaireportgenerator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ThemePalette(val displayName: String) {
    SWASTHAI_TEAL("SwasthAI Teal"),
    CLINICAL_BLUE("Clinical Blue"),
    EMERALD_HEALTH("Emerald Health"),
    DYNAMIC("Dynamic Material You")
}

// 1. SwasthAI Teal Schemes
private val SwasthAiLightScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    background = SlateBackground,
    onBackground = SlateOnBackground,
    surface = SlateSurface,
    onSurface = SlateOnSurface,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant
)

private val SwasthAiDarkScheme = darkColorScheme(
    primary = TealDarkPrimary,
    onPrimary = TealDarkOnPrimary,
    primaryContainer = TealDarkPrimaryContainer,
    onPrimaryContainer = TealDarkOnPrimaryContainer,
    secondary = TealDarkSecondary,
    onSecondary = TealDarkOnSecondary,
    secondaryContainer = TealDarkSecondaryContainer,
    onSecondaryContainer = TealDarkOnSecondaryContainer,
    background = DarkSlateBackground,
    onBackground = DarkSlateOnBackground,
    surface = DarkSlateSurface,
    onSurface = DarkSlateOnSurface,
    surfaceVariant = DarkSlateSurfaceVariant,
    onSurfaceVariant = DarkSlateOnSurfaceVariant,
    outline = DarkSlateOutline,
    outlineVariant = DarkSlateOutlineVariant
)

// 2. Clinical Blue Schemes
private val ClinicalBlueLightScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    background = SlateBackground,
    onBackground = SlateOnBackground,
    surface = SlateSurface,
    onSurface = SlateOnSurface,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant
)

private val ClinicalBlueDarkScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = BlueDarkOnPrimary,
    primaryContainer = BlueDarkPrimaryContainer,
    onPrimaryContainer = BlueDarkOnPrimaryContainer,
    secondary = BlueDarkSecondary,
    onSecondary = BlueDarkOnSecondary,
    secondaryContainer = BlueDarkSecondaryContainer,
    onSecondaryContainer = BlueDarkOnSecondaryContainer,
    background = DarkSlateBackground,
    onBackground = DarkSlateOnBackground,
    surface = DarkSlateSurface,
    onSurface = DarkSlateOnSurface,
    surfaceVariant = DarkSlateSurfaceVariant,
    onSurfaceVariant = DarkSlateOnSurfaceVariant,
    outline = DarkSlateOutline,
    outlineVariant = DarkSlateOutlineVariant
)

// 3. Emerald Health Schemes
private val EmeraldHealthLightScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnSecondary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = EmeraldOnSecondaryContainer,
    background = SlateBackground,
    onBackground = SlateOnBackground,
    surface = SlateSurface,
    onSurface = SlateOnSurface,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant
)

private val EmeraldHealthDarkScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
    secondary = EmeraldDarkSecondary,
    onSecondary = EmeraldDarkOnSecondary,
    secondaryContainer = EmeraldDarkSecondaryContainer,
    onSecondaryContainer = EmeraldDarkOnSecondaryContainer,
    background = DarkSlateBackground,
    onBackground = DarkSlateOnBackground,
    surface = DarkSlateSurface,
    onSurface = DarkSlateOnSurface,
    surfaceVariant = DarkSlateSurfaceVariant,
    onSurfaceVariant = DarkSlateOnSurfaceVariant,
    outline = DarkSlateOutline,
    outlineVariant = DarkSlateOutlineVariant
)

@Composable
fun SwasthAiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.SWASTHAI_TEAL,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val baseColorScheme: ColorScheme = when {
        palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == ThemePalette.CLINICAL_BLUE -> {
            if (darkTheme) ClinicalBlueDarkScheme else ClinicalBlueLightScheme
        }
        palette == ThemePalette.EMERALD_HEALTH -> {
            if (darkTheme) EmeraldHealthDarkScheme else EmeraldHealthLightScheme
        }
        else -> {
            if (darkTheme) SwasthAiDarkScheme else SwasthAiLightScheme
        }
    }

    val colorScheme = if (darkTheme && amoledDark) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF101010),
            surfaceVariant = Color(0xFF1E1E1E)
        )
    } else {
        baseColorScheme
    }

    // Modern WindowCompat status bar edge-to-edge styling without deprecated Accompanist
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
