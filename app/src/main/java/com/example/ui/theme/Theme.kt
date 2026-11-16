package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CockpitPalette(
    val isNight: Boolean,
    val background: Color,
    val bezel: Color,
    val card: Color,
    val cardFocused: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val segmentInactive: Color,
)

private val NightCockpitPalette = CockpitPalette(
    isNight = true,
    background = CockpitObsidian,
    bezel = CockpitBezel,
    card = CockpitCard,
    cardFocused = CockpitCardFocused,
    border = CockpitBorder,
    textPrimary = CockpitTextPrimary,
    textSecondary = CockpitTextSecondary,
    textMuted = CockpitTextMuted,
    segmentInactive = Color(0xFF252E3A),
)

private val DayRallyPalette = CockpitPalette(
    isNight = false,
    background = DayRallyBackground,
    bezel = DayRallyBezel,
    card = DayRallyCard,
    cardFocused = DayRallyCardFocused,
    border = DayRallyBorder,
    textPrimary = DayRallyTextPrimary,
    textSecondary = DayRallyTextSecondary,
    textMuted = DayRallyTextMuted,
    segmentInactive = Color(0xFFCBD5E1),
)

val LocalCockpitPalette = staticCompositionLocalOf { NightCockpitPalette }

private val NightColorScheme = darkColorScheme(
    primary = YezdiCrimson,
    onPrimary = Color.White,
    secondary = RallyAmber,
    onSecondary = Color.Black,
    tertiary = RainCyan,
    background = CockpitObsidian,
    onBackground = CockpitTextPrimary,
    surface = CockpitCard,
    onSurface = CockpitTextPrimary,
    surfaceVariant = CockpitBezel,
    onSurfaceVariant = CockpitTextSecondary,
    outline = CockpitBorder,
    error = HazardRed,
)

private val DayColorScheme = lightColorScheme(
    primary = YezdiCrimson,
    onPrimary = Color.White,
    secondary = RallyAmber,
    onSecondary = Color.Black,
    tertiary = RainCyan,
    background = DayRallyBackground,
    onBackground = DayRallyTextPrimary,
    surface = DayRallyCard,
    onSurface = DayRallyTextPrimary,
    surfaceVariant = DayRallyBezel,
    onSurfaceVariant = DayRallyTextSecondary,
    outline = DayRallyBorder,
    error = HazardRed,
)

@Composable
fun YezdiDashTheme(
    isNightMode: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (isNightMode) NightCockpitPalette else DayRallyPalette
    val colorScheme = if (isNightMode) NightColorScheme else DayColorScheme

    CompositionLocalProvider(LocalCockpitPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YezdiTypography,
            content = content,
        )
    }
}
