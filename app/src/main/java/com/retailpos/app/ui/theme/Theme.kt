package com.retailpos.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RetailLightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF475569),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF3F8),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFFE4E6)
)

object RetailTokens {
    val Primary = Color(0xFF1D4ED8)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFEFF3F8)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val Border = Color(0xFFCBD5E1)
    val Success = Color(0xFF15803D)
    val Warning = Color(0xFFB45309)
    val Error = Color(0xFFB91C1C)
}

@Composable
fun RetailPosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RetailLightColors,
        typography = Typography(),
        shapes = Shapes(
            small = MaterialTheme.shapes.small.copy(),
            medium = MaterialTheme.shapes.medium.copy(),
            large = MaterialTheme.shapes.large.copy()
        ),
        content = content
    )
}
