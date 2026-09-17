package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================================================
// RETAIL POS PROFESSIONAL COLOR SYSTEM
// ========================================================================
// Blue-based professional palette with semantic aliases for retail operations
// ========================================================================

// --- Primary Brand Colors (Blue 600) ---
val Primary = Color(0xFF2563EB)
val PrimaryContainer = Color(0xFFDBEAFE)
val OnPrimaryContainer = Color(0xFF1E3A8A)
val OnPrimary = Color.White

// --- Secondary Accent Colors (Cyan 600) ---
val Secondary = Color(0xFF0891B2)
val SecondaryContainer = Color(0xFFCFFAFE)
val OnSecondaryContainer = Color(0xFF164E63)
val OnSecondary = Color.White

// --- Tertiary Highlight Colors (Pink 600) ---
val Tertiary = Color(0xFFDB2777)
val TertiaryContainer = Color(0xFFFCE7F3)
val OnTertiaryContainer = Color(0xFF831843)
val OnTertiary = Color.White

// --- Surface & Background ---
val Background = Color(0xFFFFFFFF)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF1F5F9)
val SurfaceContainer = Color(0xFFE2E8F0)
val SurfaceContainerHigh = Color(0xFFE2E8F0)
val SurfaceContainerHighest = Color(0xFFCBD5E1)

// --- Text Colors ---
val OnBackground = Color(0xFF0F172A)
val OnSurface = Color(0xFF0F172A)
val OnSurfaceVariant = Color(0xFF475569)
val OnSurfaceDisabled = Color(0xFF94A3B8)

// --- Outline & Borders ---
val Outline = Color(0xFFE2E8F0)
val OutlineVariant = Color(0xFFF1F5F9)
val Border = Outline
val BorderSubtle = OutlineVariant

// --- Error / Destructive ---
val Error = Color(0xFFDC2626)
val ErrorContainer = Color(0xFFFEF2F2)
val OnErrorContainer = Color(0xFF991B1B)
val OnError = Color.White

// --- Success / Positive ---
val Success = Color(0xFF059669)
val SuccessContainer = Color(0xFFECFDF5)
val OnSuccessContainer = Color(0xFF064E3B)

// --- Warning / Caution ---
val Warning = Color(0xFFD97706)
val WarningContainer = Color(0xFFFFF8E1)
val OnWarningContainer = Color(0xFF78350F)

// --- Info / Neutral ---
val Info = Color(0xFF0891B2)
val InfoContainer = Color(0xFFCFFAFE)
val OnInfoContainer = Color(0xFF164E63)

// --- Shadow & Scrim ---
val Shadow = Color(0xFF0F172A)
val Scrim = Color(0xFF0F172A)

// ========================================================================
// SEMANTIC ALIASES - Use these in UI code for consistency
// ========================================================================

// Text
val TextPrimary = OnSurface
val TextSecondary = OnSurfaceVariant
val TextTertiary = OnSurfaceDisabled
val TextDisabled = OnSurfaceDisabled

// Primary actions
val PrimaryAction = Primary
val PrimaryActionContainer = PrimaryContainer
val OnPrimaryAction = OnPrimary

// Destructive actions
val DestructiveAction = Error
val DestructiveActionContainer = ErrorContainer

// Success actions
val SuccessAction = Success
val SuccessActionContainer = SuccessContainer

// Status indicators
val InStockColor = Success
val LowStockColor = Warning
val OutOfStockColor = Error

// Payment method colors
val CashColor = Success
val UpiColor = Primary
val CardColor = Color(0xFF7C3AED)
val CreditColor = Warning

// Role colors
val OwnerColor = Color(0xFF7C3AED)
val ManagerColor = Primary
val CashierColor = Secondary

// ========================================================================
// LEGACY COMPATIBILITY - For existing code migration
// ========================================================================

// Old retail palette (deprecated - use semantic aliases above)
@Deprecated("Use Primary instead", ReplaceWith("Primary"))
val RetailPrimary = Primary

@Deprecated("Use PrimaryContainer instead", ReplaceWith("PrimaryContainer"))
val RetailPrimaryContainer = PrimaryContainer

@Deprecated("Use OnPrimaryContainer instead", ReplaceWith("OnPrimaryContainer"))
val RetailOnPrimaryContainer = OnPrimaryContainer

@Deprecated("Use Secondary instead", ReplaceWith("Secondary"))
val RetailSecondary = Secondary

@Deprecated("Use SecondaryContainer instead", ReplaceWith("SecondaryContainer"))
val RetailSecondaryContainer = SecondaryContainer

@Deprecated("Use Background instead", ReplaceWith("Background"))
val RetailBackground = Background

@Deprecated("Use Surface instead", ReplaceWith("Surface"))
val RetailSurface = Surface

@Deprecated("Use SurfaceVariant instead", ReplaceWith("SurfaceVariant"))
val RetailSurfaceVariant = SurfaceVariant

@Deprecated("Use TextPrimary instead", ReplaceWith("TextPrimary"))
val RetailTextPrimary = TextPrimary

@Deprecated("Use TextSecondary instead", ReplaceWith("TextSecondary"))
val RetailTextSecondary = TextSecondary

@Deprecated("Use TextTertiary instead", ReplaceWith("TextTertiary"))
val RetailTextTertiary = TextTertiary

@Deprecated("Use Success instead", ReplaceWith("Success"))
val RetailSuccess = Success

@Deprecated("Use Warning instead", ReplaceWith("Warning"))
val RetailWarning = Warning

@Deprecated("Use Error instead", ReplaceWith("Error"))
val RetailError = Error

@Deprecated("Use Outline instead", ReplaceWith("Outline"))
val RetailBorder = Outline

@Deprecated("Use OutlineVariant instead", ReplaceWith("OutlineVariant"))
val RetailBorderSubtle = OutlineVariant

// Semantic aliases
@Deprecated("Use Primary instead", ReplaceWith("Primary"))
val ColorPrimary = Primary

@Deprecated("Use Background instead", ReplaceWith("Background"))
val ColorBackground = Background

@Deprecated("Use Surface instead", ReplaceWith("Surface"))
val ColorSurface = Surface

@Deprecated("Use TextPrimary instead", ReplaceWith("TextPrimary"))
val ColorTextPrimary = TextPrimary

@Deprecated("Use TextSecondary instead", ReplaceWith("TextSecondary"))
val ColorTextSecondary = TextSecondary

// Additional named colors
val Emerald600 = Success
val Emerald700 = Color(0xFF047857)
val Emerald50 = SuccessContainer

val Sky600 = Info
val Sky700 = Color(0xFF0369A1)

val Amber600 = Warning
val Rose600 = Error

val Slate900 = TextPrimary
val Slate800 = Color(0xFF1E293B)
val Slate100 = SurfaceVariant
val Slate50 = Background