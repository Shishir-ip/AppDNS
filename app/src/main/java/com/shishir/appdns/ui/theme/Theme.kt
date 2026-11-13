package com.shishir.appdns.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shishir.appdns.data.ThemeMode

val LocalHapticsOn = compositionLocalOf { true }

private val PureWhite = Color(0xFFFFFFFF)
private val PureBlack = Color(0xFF000000)

private val LightScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    secondary = PureBlack,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = Color(0xFFD9D9D9),
    outlineVariant = Color(0xFFEBEBEB),
    error = PureBlack,
    onError = PureWhite,
    inverseSurface = PureBlack,
    inverseOnSurface = PureWhite
)

private val DarkScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = PureWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = Color(0xFF0A0A0A),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFB5B5B5),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF242424),
    error = PureWhite,
    onError = PureBlack,
    inverseSurface = PureWhite,
    inverseOnSurface = PureBlack
)

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp)
    )
}

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 0.sp)

@Composable
fun AppDNSTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}