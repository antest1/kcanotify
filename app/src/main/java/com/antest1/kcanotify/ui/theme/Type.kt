package com.antest1.kcanotify.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography used by the Compose screens.
 *
 * The app's XML theme is `Theme.Material3.Light.NoActionBar`, so the Material 3 defaults already
 * match what the current widgets render. Individual screens still pass the explicit `dp` text
 * sizes from their layout files (see [asTextSize]) where the XML overrides the default.
 */
val KcaTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
)
