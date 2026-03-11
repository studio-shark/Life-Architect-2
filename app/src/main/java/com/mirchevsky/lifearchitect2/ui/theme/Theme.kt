package com.mirchevsky.lifearchitect2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary            = BrandGreen,
    onPrimary          = Color.White,
    primaryContainer   = DarkSurface,
    onPrimaryContainer = DarkOnSurface, // Explicitly set to brand white
    secondary          = Purple,
    onSecondary        = Color.White,
    background         = DarkBackground,
    onBackground       = DarkOnBackground,
    surface            = DarkSurface,
    onSurface          = DarkOnSurface,
    surfaceVariant     = DarkSurfaceVariant,
    onSurfaceVariant   = DarkOnSurfaceVariant,
    surfaceTint        = Color.Transparent,
    error              = Color(0xFFF87171),
    onError            = Color.White,
    outline            = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary            = BrandGreen,
    onPrimary          = Color.White,
    primaryContainer   = BrandGreenLight,
    secondary          = Purple,
    onSecondary        = Color.White,
    background         = LightBackground,
    onBackground       = LightOnBackground,
    surface            = LightSurface,
    onSurface          = LightOnSurface,
    surfaceVariant     = LightSurfaceVariant,
    onSurfaceVariant   = LightOnSurfaceVariant,
    surfaceTint        = Color.Transparent,
    error              = BrandError,
    onError            = Color.White,
    outline            = LightBorderLight
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
