package dev.kaixinguo.standalonecodepractice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    tertiary = AccentAmber,
    background = AppBackground,
    surface = PaneBackground,
    surfaceVariant = CardBackground,
    onPrimary = AppBackground,
    onSecondary = AppBackground,
    onTertiary = AppBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun StandaloneLeetCodeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

