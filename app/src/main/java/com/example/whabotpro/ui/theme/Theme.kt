package com.example.whabotpro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// WhatsApp-inspired dark palette
val WaGreen = Color(0xFF25D366)
val WaGreenDark = Color(0xFF128C7E)
val WaTeal = Color(0xFF075E54)
val WaDarkBg = Color(0xFF0B141A)
val WaDarkSurface = Color(0xFF111B21)
val WaDarkSurface2 = Color(0xFF1D2A31)
val WaDarkBorder = Color(0xFF2A3942)
val WaLightBg = Color(0xFFF0F2F5)
val WaLightSurface = Color(0xFFFFFFFF)
val WaLightSurface2 = Color(0xFFE9EDEF)

private val DarkColors = darkColorScheme(
    primary = WaGreen,
    onPrimary = Color.Black,
    primaryContainer = WaTeal,
    onPrimaryContainer = Color.White,
    secondary = WaGreenDark,
    onSecondary = Color.White,
    background = WaDarkBg,
    onBackground = Color(0xFFE9EDEF),
    surface = WaDarkSurface,
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = WaDarkSurface2,
    onSurfaceVariant = Color(0xFF8696A0),
    outline = WaDarkBorder
)

private val LightColors = lightColorScheme(
    primary = WaGreenDark,
    onPrimary = Color.White,
    primaryContainer = WaGreen,
    onPrimaryContainer = Color.Black,
    secondary = WaTeal,
    onSecondary = Color.White,
    background = WaLightBg,
    onBackground = Color(0xFF111B21),
    surface = WaLightSurface,
    onSurface = Color(0xFF111B21),
    surfaceVariant = WaLightSurface2,
    onSurfaceVariant = Color(0xFF667781),
    outline = Color(0xFFD1D7DB)
)

@Composable
fun WhaBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
