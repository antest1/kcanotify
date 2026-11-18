package com.antest1.kcanotify.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
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
    // Material 3 installs `bodyLarge` as `LocalTextStyle`, so its 24sp line height and 0.5sp
    // letter spacing would also be inherited by every Text that only overrides `fontSize` (all
    // screens copy the XML `android:textSize`). Resetting the style to just the default size +
    // View-like font padding matches the XML TextViews.
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
)
