package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// RetailPOS palette — restrained indigo/slate system used by the original UI.
val RetailPrimary = Color(0xFF4F46E5)
val RetailPrimaryContainer = Color(0xFFEEF2FF)
val RetailOnPrimaryContainer = Color(0xFF3730A3)
val RetailSecondary = Color(0xFF475569)
val RetailSecondaryContainer = Color(0xFFF1F5F9)
val RetailBackground = Color(0xFFF8FAFC)
val RetailSurface = Color(0xFFFFFFFF)
val RetailSurfaceVariant = Color(0xFFF1F5F9)
val RetailTextPrimary = Color(0xFF0F172A)
val RetailTextSecondary = Color(0xFF64748B)
val RetailTextTertiary = Color(0xFF94A3B8)
val RetailSuccess = Color(0xFF059669)
val RetailWarning = Color(0xFFD97706)
val RetailError = Color(0xFFE11D48)
val RetailBorder = Color(0xFFE2E8F0)
val RetailBorderSubtle = Color(0xFFF1F5F9)

val Primary = RetailPrimary
val PrimaryContainer = RetailPrimaryContainer
val OnPrimaryContainer = RetailOnPrimaryContainer
val OnPrimary = Color.White
val Secondary = RetailSecondary
val SecondaryContainer = RetailSecondaryContainer
val OnSecondaryContainer = RetailTextPrimary
val OnSecondary = Color.White
val Tertiary = Color(0xFF7C3AED)
val TertiaryContainer = Color(0xFFF3E8FF)
val OnTertiaryContainer = Color(0xFF581C87)
val OnTertiary = Color.White

val Background = RetailBackground
val Surface = RetailSurface
val SurfaceVariant = RetailSurfaceVariant
val SurfaceContainer = RetailSurfaceVariant
val SurfaceContainerHigh = RetailSurfaceVariant
val SurfaceContainerHighest = RetailBorder
val OnBackground = RetailTextPrimary
val OnSurface = RetailTextPrimary
val OnSurfaceVariant = RetailTextSecondary
val OnSurfaceDisabled = RetailTextTertiary

val Outline = RetailBorder
val OutlineVariant = RetailBorderSubtle
val Border = RetailBorder
val BorderSubtle = RetailBorderSubtle

val Error = RetailError
val ErrorContainer = Color(0xFFFFF1F2)
val OnErrorContainer = Color(0xFF9F1239)
val OnError = Color.White

val Success = RetailSuccess
val SuccessContainer = Color(0xFFECFDF5)
val OnSuccessContainer = Color(0xFF065F46)

val Warning = RetailWarning
val WarningContainer = Color(0xFFFFF7ED)
val OnWarningContainer = Color(0xFF92400E)

val Info = Color(0xFF0284C7)
val InfoContainer = Color(0xFFE0F2FE)
val OnInfoContainer = Color(0xFF075985)

val Shadow = RetailTextPrimary
val Scrim = RetailTextPrimary

val TextPrimary = RetailTextPrimary
val TextSecondary = RetailTextSecondary
val TextTertiary = RetailTextTertiary
val TextDisabled = RetailTextTertiary

val PrimaryAction = Primary
val PrimaryActionContainer = PrimaryContainer
val OnPrimaryAction = OnPrimary
val DestructiveAction = Error
val DestructiveActionContainer = ErrorContainer
val SuccessAction = Success
val SuccessActionContainer = SuccessContainer

val InStockColor = Success
val LowStockColor = Warning
val OutOfStockColor = Error

val CashColor = RetailSuccess
val UpiColor = RetailPrimary
val CardColor = Color(0xFF7C3AED)
val CreditColor = RetailWarning

val OwnerColor = Color(0xFF7C3AED)
val ManagerColor = RetailPrimary
val CashierColor = RetailSecondary

val Emerald600 = RetailSuccess
val Emerald700 = Color(0xFF047857)
val Emerald50 = SuccessContainer
val Sky600 = Info
val Sky700 = Color(0xFF0369A1)
val Amber600 = RetailWarning
val Rose600 = RetailError
val Slate900 = RetailTextPrimary
val Slate800 = Color(0xFF1E293B)
val Slate100 = RetailBorderSubtle
val Slate50 = RetailBackground

object RetailColors {
    val Success = RetailSuccess
    val SuccessContainer = Color(0xFFECFDF5)
    val SuccessOnContainer = Color(0xFF065F46)
    val Warning = RetailWarning
    val WarningContainer = Color(0xFFFFF7ED)
    val WarningOnContainer = Color(0xFF92400E)
    val Error = RetailError
    val ErrorContainer = Color(0xFFFFF1F2)
    val ErrorOnContainer = Color(0xFF9F1239)
    val Info = com.example.ui.theme.Info
    val InfoContainer = com.example.ui.theme.InfoContainer
    val InfoOnContainer = com.example.ui.theme.OnInfoContainer
    val Owner = OwnerColor
    val Manager = ManagerColor
    val Cashier = CashierColor
    val Cash = CashColor
    val Upi = UpiColor
    val Card = CardColor
    val Credit = CreditColor
}

val ColorPrimary = RetailPrimary
val ColorBackground = RetailBackground
val ColorSurface = RetailSurface
val ColorTextPrimary = RetailTextPrimary
val ColorTextSecondary = RetailTextSecondary
