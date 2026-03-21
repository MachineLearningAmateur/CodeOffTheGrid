package dev.kaixinguo.standalonecodepractice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val storageValue: String,
    val label: String,
    val description: String
) {
    Night(
        storageValue = "night",
        label = "Night mode",
        description = "Higher contrast for low-light sessions."
    ),
    Light(
        storageValue = "light",
        label = "Light mode",
        description = "Warm paper tones for brighter rooms."
    );

    companion object {
        fun fromStorageValue(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: Night
        }
    }
}

@Composable
fun OffTheCodeGridTheme(
    themeMode: AppThemeMode = AppThemeMode.Night,
    content: @Composable () -> Unit
) {
    val offTheCodeGridColors = when (themeMode) {
        AppThemeMode.Night -> DarkOffTheCodeGridColors
        AppThemeMode.Light -> LightOffTheCodeGridColors
    }
    val accentOnColor = Color(0xFF102038)
    val materialColorScheme = when (themeMode) {
        AppThemeMode.Night -> darkColorScheme(
            primary = AccentBlue,
            secondary = AccentGreen,
            tertiary = AccentAmber,
            background = offTheCodeGridColors.appBackground,
            surface = offTheCodeGridColors.paneBackground,
            surfaceVariant = offTheCodeGridColors.cardBackground,
            onPrimary = accentOnColor,
            onSecondary = accentOnColor,
            onTertiary = accentOnColor,
            onBackground = offTheCodeGridColors.textPrimary,
            onSurface = offTheCodeGridColors.textPrimary,
            onSurfaceVariant = offTheCodeGridColors.textSecondary,
            outline = offTheCodeGridColors.cardBorder
        )
        AppThemeMode.Light -> lightColorScheme(
            primary = AccentBlue,
            secondary = AccentGreen,
            tertiary = AccentAmber,
            background = offTheCodeGridColors.appBackground,
            surface = offTheCodeGridColors.paneBackground,
            surfaceVariant = offTheCodeGridColors.cardBackground,
            onPrimary = accentOnColor,
            onSecondary = accentOnColor,
            onTertiary = accentOnColor,
            onBackground = offTheCodeGridColors.textPrimary,
            onSurface = offTheCodeGridColors.textPrimary,
            onSurfaceVariant = offTheCodeGridColors.textSecondary,
            outline = offTheCodeGridColors.cardBorder
        )
    }

    CompositionLocalProvider(LocalOffTheCodeGridColors provides offTheCodeGridColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}

