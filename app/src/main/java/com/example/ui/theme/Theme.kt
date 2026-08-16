package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color(0xFF1E1B4B), // Indigo 950
    primaryContainer = Color(0xFF312E81), // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo 100
    secondary = Color(0xFF94A3B8), // Slate 400
    onSecondary = Color(0xFF0F172A), // Slate 900
    background = Color(0xFF0F172A), // Slate 900
    surface = Color(0xFF1E293B), // Slate 800
    surfaceVariant = Color(0xFF334155), // Slate 700
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFB7185) // Rose 400
)

private val LightColorScheme = lightColorScheme(
    primary = RetailPrimary,
    onPrimary = Color.White,
    primaryContainer = RetailPrimaryContainer,
    onPrimaryContainer = RetailOnPrimaryContainer,
    secondary = RetailSecondary,
    onSecondary = Color.White,
    secondaryContainer = RetailSecondaryContainer,
    onSecondaryContainer = RetailTextPrimary,
    background = RetailBackground,
    surface = RetailSurface,
    surfaceVariant = RetailSurfaceVariant,
    onBackground = RetailTextPrimary,
    onSurface = RetailTextPrimary,
    onSurfaceVariant = RetailTextSecondary,
    outline = RetailBorder,
    outlineVariant = RetailBorderSubtle,
    error = RetailError
)

@Composable
fun RetailPosTheme(
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
