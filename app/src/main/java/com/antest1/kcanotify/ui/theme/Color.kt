package com.antest1.kcanotify.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colours mirrored from `res/values/colors.xml`.
 *
 * These are copies on purpose: the XML resources stay the source of truth for the existing
 * screens, and the Compose screens use the same values so both render identically while the
 * migration is in progress.
 */
object KcaColors {
    /** `colorPrimary` — toolbar / top app bar background. */
    val Primary = Color(0xFF313447)
    val PrimaryDark = Color(0xFF2B2B30)

    /** `colorAccent` — secondary accent (FAB, accents). */
    val Accent = Color(0xFF03A9F4)

    /** `colorMainBtn` — highlighted main action button. */
    val MainBtn = Color(0xFF3160D9)

    /** `colorBtn` / `lightgray` — default button and chip background. */
    val Btn = Color(0xFFD6D6D6)
    val BtnText = Color(0xFF000000)
    val BtnTextAccent = Color(0xFFFFFFFF)

    val ToolbarText = Color(0xFFFFFFFF)
    val ActivityBack = Color(0xFFFFFFFF)
    val ActivityText = Color(0xFF888888)

    /** `colorPanelBack` — translucent panel background used by the EXP calculator. */
    val PanelBack = Color(0x7FF5F5F5)

    /** `colorDataArea` — grey value boxes. */
    val DataArea = Color(0xFFE0E0E0)

    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFD6D6D6)
    val Grey = Color(0xFF888888)
    val DarkGray = Color(0xFF212121)
    val Transparent = Color(0x00FFFFFF)

    val Ok = Color(0xFF0CB350)
    val Warning = Color(0xFFFF9800)
    val Danger = Color(0xFFF80013)

    /**
     * `colorSurface` (#FEF7FF), the container colour of the XML chips whose `android:background`
     * is ignored, and `colorOutline` (#79747E), the 1dp chip stroke. `AppTheme` does not override
     * either role, so they keep the Material 3 defaults; `outline` is also used by the theme below.
     */
    val ChipSurface = Color(0xFFFEF7FF)
    val ChipOutline = Color(0xFF79747E)
}
