package dev.kaixinguo.standalonecodepractice.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
internal data class StandaloneColors(
    val appBackground: Color,
    val sidebarBackground: Color,
    val paneBackground: Color,
    val cardBackground: Color,
    val cardBackgroundAlt: Color,
    val cardBorder: Color,
    val dividerColor: Color,
    val insetSurface: Color,
    val mediaSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

internal val DarkStandaloneColors = StandaloneColors(
    appBackground = Color(0xFF161A24),
    sidebarBackground = Color(0xFF1A1F2B),
    paneBackground = Color(0xFF1D2330),
    cardBackground = Color(0xFF242B39),
    cardBackgroundAlt = Color(0xFF202735),
    cardBorder = Color(0xFF313A4B),
    dividerColor = Color(0xFF2B3445),
    insetSurface = Color(0xFF1A2230),
    mediaSurface = Color(0xFF141B25),
    textPrimary = Color(0xFFF3F5F8),
    textSecondary = Color(0xFFB5BECC),
    textMuted = Color(0xFF8892A3)
)

internal val LightStandaloneColors = StandaloneColors(
    appBackground = Color(0xFFF6F0E8),
    sidebarBackground = Color(0xFFEAE0D2),
    paneBackground = Color(0xFFFBF7F1),
    cardBackground = Color(0xFFFFFFFF),
    cardBackgroundAlt = Color(0xFFF2EBE1),
    cardBorder = Color(0xFFD8CDBE),
    dividerColor = Color(0xFFE3D8CA),
    insetSurface = Color(0xFFF5EFE6),
    mediaSurface = Color(0xFFE9E0D2),
    textPrimary = Color(0xFF1F2430),
    textSecondary = Color(0xFF596272),
    textMuted = Color(0xFF7D8694)
)

internal val LocalStandaloneColors = staticCompositionLocalOf { DarkStandaloneColors }

val AccentBlue = Color(0xFF79A8FF)
val AccentBlueSoft = Color(0xFF5A6F96)
val AccentGreen = Color(0xFF79C88D)
val AccentRed = Color(0xFFE58BA5)
val AccentAmber = Color(0xFFE6B25A)

val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.appBackground

val SidebarBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.sidebarBackground

val PaneBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.paneBackground

val CardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.cardBackground

val CardBackgroundAlt: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.cardBackgroundAlt

val CardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.cardBorder

val DividerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.dividerColor

val InsetSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.insetSurface

val MediaSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.mediaSurface

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalStandaloneColors.current.textMuted

