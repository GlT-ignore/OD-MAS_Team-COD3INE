package com.example.odmas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_primaryForeground,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_secondaryForeground,
    tertiary = md_theme_dark_accent,
    onTertiary = md_theme_dark_accentForeground,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_foreground,
    surface = md_theme_dark_background,
    onSurface = md_theme_dark_foreground,
    error = md_theme_dark_destructive,
    onError = md_theme_dark_destructiveForeground,
    surfaceVariant = md_theme_dark_muted,
    onSurfaceVariant = md_theme_dark_mutedForeground,
    outline = md_theme_dark_border,
    scrim = md_theme_dark_border,
    inverseSurface = md_theme_dark_foreground,
    inverseOnSurface = md_theme_dark_background,
    inversePrimary = md_theme_dark_primary,
    surfaceTint = md_theme_dark_primary
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_primaryForeground,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_secondaryForeground,
    tertiary = md_theme_dark_accent,
    onTertiary = md_theme_dark_accentForeground,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_foreground,
    surface = md_theme_dark_background,
    onSurface = md_theme_dark_foreground,
    error = md_theme_dark_destructive,
    onError = md_theme_dark_destructiveForeground,
    surfaceVariant = md_theme_dark_muted,
    onSurfaceVariant = md_theme_dark_mutedForeground,
    outline = md_theme_dark_border,
    scrim = md_theme_dark_border,
    inverseSurface = md_theme_dark_foreground,
    inverseOnSurface = md_theme_dark_background,
    inversePrimary = md_theme_dark_primary,
    surfaceTint = md_theme_dark_primary
)

@Composable
fun ODMASTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}