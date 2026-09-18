package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandTealDark,
    onPrimaryContainer = Color.White,
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandBlueDark,
    onSecondaryContainer = Color.White,
    tertiary = AccentAmber,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceLineDark,
    onBackground = TextInkDark,
    onSurface = TextInkDark,
    onSurfaceVariant = TextMutedDark,
    outline = SurfaceLineDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandBlueLight,
    onSecondaryContainer = BrandBlueDark,
    tertiary = AccentAmber,
    background = BackgroundLight,
    surface = SurfaceCard,
    surfaceVariant = BackgroundLight,
    onBackground = TextInk,
    onSurface = TextInk,
    onSurfaceVariant = TextMuted,
    outline = SurfaceLine
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun EmployeeManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, content = content)
}
