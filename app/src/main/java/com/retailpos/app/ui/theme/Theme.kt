package com.retailpos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val RetailLightColors = lightColorScheme(
    primary = Color(0xFF155EEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF062E73),
    secondary = Color(0xFF475467),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EEF5),
    onSecondaryContainer = Color(0xFF17212F),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F3F7),
    onSurface = Color(0xFF101828),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD0D5DD),
    outlineVariant = Color(0xFFE4E7EC),
    error = Color(0xFFD92D20),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A)
)

private val RetailDarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FF),
    onPrimary = Color(0xFF002E6A),
    primaryContainer = Color(0xFF08469A),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFB8C2D0),
    onSecondary = Color(0xFF1B2532),
    secondaryContainer = Color(0xFF3B4655),
    onSecondaryContainer = Color(0xFFE5E7EB),
    background = Color(0xFF101318),
    surface = Color(0xFF151922),
    surfaceVariant = Color(0xFF202631),
    onSurface = Color(0xFFF2F4F7),
    onSurfaceVariant = Color(0xFF98A2B3),
    outline = Color(0xFF475467),
    outlineVariant = Color(0xFF344054),
    error = Color(0xFFF97066),
    errorContainer = Color(0xFF55160C),
    onErrorContainer = Color(0xFFFEE4E2)
)

object RetailTokens {
    val Primary = Color(0xFF155EEF)
    val Background = Color(0xFFF7F9FC)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFF0F3F7)
    val TextPrimary = Color(0xFF101828)
    val TextSecondary = Color(0xFF667085)
    val Border = Color(0xFFE4E7EC)
    val Success = Color(0xFF079455)
    val Warning = Color(0xFFDC6803)
    val Error = Color(0xFFD92D20)
    val Ai = Color(0xFF6941C6)
}

private val RetailTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = bodyLarge.lineHeight * 1.15f)
    )
}

private val RetailShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun RetailPosTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) RetailDarkColors else RetailLightColors,
        typography = RetailTypography,
        shapes = RetailShapes,
        content = content
    )
}
