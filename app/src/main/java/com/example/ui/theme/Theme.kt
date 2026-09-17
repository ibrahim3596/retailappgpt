package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme - Modern retail professional
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),        // Blue 600 - Trustworthy, professional
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), // Blue 100
    onPrimaryContainer = Color(0xFF1E3A8A), // Blue 900

    secondary = Color(0xFF0891B2),      // Cyan 600 - Fresh accent
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE), // Cyan 100
    onSecondaryContainer = Color(0xFF164E63), // Cyan 900

    tertiary = Color(0xFFDB2777),       // Pink 600 - For highlights/alerts
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3), // Pink 100
    onTertiaryContainer = Color(0xFF831843), // Pink 900

    error = Color(0xFFDC2626),          // Red 600
    onError = Color.White,
    errorContainer = Color(0xFFFEF2F2), // Red 50
    onErrorContainer = Color(0xFF991B1B), // Red 900

    background = Color(0xFFFFFFFF),     // Pure white
    onBackground = Color(0xFF0F172A),   // Slate 900

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),

    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF475569), // Slate 600

    outline = Color(0xFFE2E8F0),        // Slate 200
    outlineVariant = Color(0xFFF1F5F9), // Slate 100

    scrim = Color(0xFF0F172A),

    inverseSurface = Color(0xFF1E293B), // Slate 800
    inverseOnSurface = Color(0xFFF8FAFC), // Slate 50
    inversePrimary = Color(0xFF93C5FD)  // Blue 300
)

// Dark theme - Professional dark mode
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),        // Blue 400
    onPrimary = Color(0xFF1E3A8A),      // Blue 900
    primaryContainer = Color(0xFF1E40AF), // Blue 800
    onPrimaryContainer = Color(0xFFDBEAFE), // Blue 100

    secondary = Color(0xFF22D3EE),      // Cyan 400
    onSecondary = Color(0xFF164E63),    // Cyan 900
    secondaryContainer = Color(0xFF0E7490), // Cyan 700
    onSecondaryContainer = Color(0xFFCFFAFE), // Cyan 100

    tertiary = Color(0xFFF472B6),       // Pink 400
    onTertiary = Color(0xFF831843),     // Pink 900
    tertiaryContainer = Color(0xFFBE185D), // Pink 700
    onTertiaryContainer = Color(0xFFFCE7F3), // Pink 100

    error = Color(0xFFF87171),          // Red 400
    onError = Color(0xFF991B1B),        // Red 900
    errorContainer = Color(0xFF7F1D1D), // Red 950
    onErrorContainer = Color(0xFFFEF2F2), // Red 50

    background = Color(0xFF020617),     // Slate 950
    onBackground = Color(0xFFF8FAFC),   // Slate 50

    surface = Color(0xFF0F172A),        // Slate 900
    onSurface = Color(0xFFF8FAFC),      // Slate 50

    surfaceVariant = Color(0xFF1E293B), // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400

    outline = Color(0xFF334155),        // Slate 700
    outlineVariant = Color(0xFF1E293B), // Slate 800

    scrim = Color(0xFF000000),

    inverseSurface = Color(0xFFF8FAFC), // Slate 50
    inverseOnSurface = Color(0xFF0F172A), // Slate 900
    inversePrimary = Color(0xFF2563EB)  // Blue 600
)

// Semantic color aliases for easy theming
object RetailColors {
    // Status colors
    val Success = Color(0xFF059669)     // Emerald 600
    val SuccessContainer = Color(0xFFECFDF5)
    val SuccessOnContainer = Color(0xFF064E3B)

    val Warning = Color(0xFFD97706)     // Amber 600
    val WarningContainer = Color(0xFFFFF8E1)
    val WarningOnContainer = Color(0xFF78350F)

    val Error = Color(0xFFDC2626)       // Red 600
    val ErrorContainer = Color(0xFFFEF2F2)
    val ErrorOnContainer = Color(0xFF991B1B)

    val Info = Color(0xFF0891B2)        // Cyan 600
    val InfoContainer = Color(0xFFCFFAFE)
    val InfoOnContainer = Color(0xFF164E63)

    // Role-based colors
    val Owner = Color(0xFF7C3AED)       // Violet 600
    val Manager = Color(0xFF2563EB)     // Blue 600
    val Cashier = Color(0xFF0891B2)     // Cyan 600

    // Payment method colors
    val Cash = Color(0xFF059669)
    val Upi = Color(0xFF2563EB)
    val Card = Color(0xFF7C3AED)
    val Credit = Color(0xFFD97706)
}

@Composable
fun RetailPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}