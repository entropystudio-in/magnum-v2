package com.magnum.player

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val AppleRed = Color(0xFFFA2D48)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Immutable
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val panel: Color,
    val panelHigh: Color,
    val sidebar: Color,
    val text: Color,
)

val DarkPalette = Palette(
    isDark = true,
    bg = Color.Black,
    panel = Color(0xFF1C1C1E),
    panelHigh = Color(0xFF2C2C2E),
    sidebar = Color(0xFF111111),
    text = Color.White,
)

val LightPalette = Palette(
    isDark = false,
    bg = Color.White,
    panel = Color(0xFFF2F2F7),
    panelHigh = Color(0xFFE5E5EA),
    sidebar = Color(0xFFF2F2F7),
    text = Color(0xFF111111),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

private fun typographyWith(f: FontFamily): Typography {
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = f),
        displayMedium = d.displayMedium.copy(fontFamily = f),
        displaySmall = d.displaySmall.copy(fontFamily = f),
        headlineLarge = d.headlineLarge.copy(fontFamily = f),
        headlineMedium = d.headlineMedium.copy(fontFamily = f),
        headlineSmall = d.headlineSmall.copy(fontFamily = f),
        titleLarge = d.titleLarge.copy(fontFamily = f),
        titleMedium = d.titleMedium.copy(fontFamily = f),
        titleSmall = d.titleSmall.copy(fontFamily = f),
        bodyLarge = d.bodyLarge.copy(fontFamily = f),
        bodyMedium = d.bodyMedium.copy(fontFamily = f),
        bodySmall = d.bodySmall.copy(fontFamily = f),
        labelLarge = d.labelLarge.copy(fontFamily = f),
        labelMedium = d.labelMedium.copy(fontFamily = f),
        labelSmall = d.labelSmall.copy(fontFamily = f),
    )
}

@Composable
fun MagnumTheme(mode: ThemeMode, fontFamily: FontFamily?, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = AppleRed, background = palette.bg, surface = palette.panel,
            onBackground = palette.text, onSurface = palette.text,
        )
    } else {
        lightColorScheme(
            primary = AppleRed, background = palette.bg, surface = palette.panel,
            onBackground = palette.text, onSurface = palette.text,
        )
    }
    // One font family applied to every text style, so it covers the whole app.
    val typo = remember(fontFamily) {
        if (fontFamily == null) Typography() else typographyWith(fontFamily)
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = typo) {
            ProvideTextStyle(typo.bodyLarge, content)
        }
    }
}
