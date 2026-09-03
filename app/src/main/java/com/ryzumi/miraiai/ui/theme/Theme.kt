package com.ryzumi.miraiai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MiraiPrimary,
    onPrimary = Color.White,
    primaryContainer = MiraiPrimaryContainer,
    onPrimaryContainer = MiraiOnPrimaryContainer,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121218),
    surface = Color(0xFF1A1A22),
    onBackground = Color(0xFFE6E6EE),
    onSurface = Color(0xFFE6E6EE)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF1A1A3F),
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF8F8FC),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1C24),
    onSurface = Color(0xFF1C1C24)
)

@Composable
fun MiraiAITheme(
    themeMode: String = "system",
    isMonetEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        isMonetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
