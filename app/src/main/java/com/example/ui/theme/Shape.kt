package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes as M3Shapes
import androidx.compose.ui.unit.dp

// Modern, friendly shapes for retail POS
val Shapes = M3Shapes(
    extraSmall = RoundedCornerShape(4.dp),      // Chips, badges
    small = RoundedCornerShape(8.dp),           // Buttons, inputs
    medium = RoundedCornerShape(12.dp),         // Cards, dialogs
    large = RoundedCornerShape(16.dp),          // Sheets, modals
    extraLarge = RoundedCornerShape(24.dp)      // Bottom sheets, main containers
)

// Custom shapes for specific use cases
val M3Shapes.pill: RoundedCornerShape
    get() = RoundedCornerShape(999.dp)          // Pills, FABs

val M3Shapes.circle: RoundedCornerShape
    get() = RoundedCornerShape(50.dp)           // Avatars, icon containers

val M3Shapes.card: RoundedCornerShape
    get() = RoundedCornerShape(16.dp)           // Standard cards

val M3Shapes.cardElevated: RoundedCornerShape
    get() = RoundedCornerShape(20.dp)           // Elevated cards

val M3Shapes.sheet: RoundedCornerShape
    get() = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // Bottom sheets

val M3Shapes.fab: RoundedCornerShape
    get() = RoundedCornerShape(16.dp)           // FABs