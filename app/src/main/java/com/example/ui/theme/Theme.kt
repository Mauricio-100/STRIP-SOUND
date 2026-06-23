package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Spotify-like Midnight Blue and fluid Black Palette
private val MidnightBlack = Color(0xFF04060E) // fluid black
private val OceanBlue = Color(0xFF0B1426) // deep slate blue
private val DarkCardBg = Color(0xFF10192C) // card background
val NeonBlue = Color(0xFF0D9488) // vibrant dark teal
val ElectricBlue = Color(0xFF3B82F6) // active neon blue
private val WhiteNeutral = Color(0xFFF1F5F9)
private val GrayNeutral = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = OceanBlue,
    tertiary = NeonBlue,
    background = MidnightBlack,
    surface = DarkCardBg,
    onPrimary = Color.White,
    onSecondary = WhiteNeutral,
    onBackground = WhiteNeutral,
    onSurface = WhiteNeutral,
    surfaceVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = OceanBlue,
    tertiary = NeonBlue,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = OceanBlue,
    onBackground = MidnightBlack,
    onSurface = MidnightBlack,
    surfaceVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default for Premium Spotify Feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
