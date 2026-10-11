package com.aj.wafatti.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WafattiDarkColorScheme = darkColorScheme(
    primary = WafattiPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = WafattiDarkContainer,
    onPrimaryContainer = WafattiPurpleLight,
    secondary = WafattiPinkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1035),
    onSecondaryContainer = WafattiPinkLight,
    tertiary = WafattiPinkLight,
    onTertiary = Color.White,
    background = WafattiDarkBackground,
    onBackground = WafattiTextPrimaryDark,
    surface = WafattiDarkSurface,
    onSurface = WafattiTextPrimaryDark,
    surfaceVariant = WafattiDarkSurfaceVariant,
    onSurfaceVariant = WafattiTextSecondaryDark,
    outline = WafattiDarkOutline,
    outlineVariant = Color(0xFF382352),
    error = WafattiError,
    onError = Color.White
)

private val WafattiLightColorScheme = lightColorScheme(
    primary = WafattiPurpleDark,
    onPrimary = Color.White,
    primaryContainer = WafattiLightContainer,
    onPrimaryContainer = WafattiPurpleDark,
    secondary = WafattiPinkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = WafattiPinkDark,
    tertiary = WafattiPinkDark,
    onTertiary = Color.White,
    background = WafattiLightBackground,
    onBackground = WafattiTextPrimaryLight,
    surface = WafattiLightSurface,
    onSurface = WafattiTextPrimaryLight,
    surfaceVariant = WafattiLightSurfaceVariant,
    onSurfaceVariant = WafattiTextSecondaryLight,
    outline = WafattiLightOutline,
    outlineVariant = Color(0xFFE9D5FF),
    error = WafattiError,
    onError = Color.White
)

@Composable
fun WafattiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force Wafatti Brand Colors (don't override with system wallpapers)
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WafattiDarkColorScheme else WafattiLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
