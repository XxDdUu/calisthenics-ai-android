package com.calisthenics.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createDarkColorScheme(colors: AppColors): ColorScheme = darkColorScheme(
    primary = colors.accent,
    onPrimary = colors.onAccent,
    secondary = colors.success,
    onSecondary = colors.onAccent,
    tertiary = colors.info,
    onTertiary = colors.onAccent,
    background = colors.background,
    onBackground = colors.primaryText,
    surface = colors.surface,
    onSurface = colors.primaryText,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.secondaryText,
    outline = colors.border,
    outlineVariant = colors.subtleBorder,
    error = colors.error,
    onError = colors.onAccent
)

private fun createLightColorScheme(colors: AppColors): ColorScheme = lightColorScheme(
    primary = colors.accent,
    onPrimary = colors.onAccent,
    secondary = colors.success,
    onSecondary = colors.onAccent,
    tertiary = colors.error,
    background = colors.background,
    onBackground = colors.primaryText,
    surface = colors.surface,
    onSurface = colors.primaryText,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.secondaryText,
    outline = colors.border,
    error = colors.error
)

@Composable
fun CalisthenicsAITheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val resolvedMode = when (themeMode) {
        ThemeMode.SYSTEM -> if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
        else -> themeMode
    }

    val appColors = when (resolvedMode) {
        ThemeMode.LIGHT -> LightAppColors
        ThemeMode.DARK_V2 -> DarkV2AppColors
        ThemeMode.NEUTRAL -> NeutralAppColors
        ThemeMode.DARK, ThemeMode.SYSTEM -> DarkAppColors
    }

    val colorScheme = when (resolvedMode) {
        ThemeMode.LIGHT -> createLightColorScheme(appColors)
        ThemeMode.NEUTRAL -> createLightColorScheme(appColors)
        ThemeMode.DARK, ThemeMode.DARK_V2, ThemeMode.SYSTEM -> createDarkColorScheme(appColors)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !appColors.isDark
                insetsController.isAppearanceLightNavigationBars = !appColors.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
