package com.example.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Consistent spacing scale for the app
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    // Component-specific
    val cardPadding = 16.dp
    val screenPadding = 16.dp
    val fabMargin = 16.dp
    val sectionGap = 24.dp
    val itemGap = 12.dp
    val chipGap = 8.dp
    val inlineGap = 8.dp

    // Typography spacing
    val textLineHeightTight = 1.2
    val textLineHeightNormal = 1.5
    val textLineHeightRelaxed = 1.75
}

// Icon sizes
object IconSizes {
    val xs = 16.dp
    val sm = 20.dp
    val md = 24.dp
    val lg = 28.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp
    val avatar = 40.dp
    val avatarLarge = 56.dp
}

// Elevation/Shadow system
object Elevation {
    val none = 0.dp
    val level1 = 1.dp   // Cards, surface
    val level2 = 3.dp   // Raised buttons, dropdowns
    val level3 = 6.dp   // FABs, dialogs
    val level4 = 8.dp   // Bottom sheets, modals
    val level5 = 12.dp  // Toasts, snackbars
    val level6 = 16.dp  // Overlay, drawers
}

// Animation durations
object Motion {
    val fast = 150  // ms
    val medium = 250
    val slow = 350
    val extraSlow = 500
}