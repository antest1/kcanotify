package com.antest1.kcanotify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material 3 colour roles derived from the existing `AppTheme` (`Theme.Material3.Light.NoActionBar`).
 *
 * The app only ships a light theme, so no dark scheme is provided yet.
 */
private val KcaLightColorScheme = lightColorScheme(
    primary = KcaColors.Primary,
    onPrimary = KcaColors.ToolbarText,
    primaryContainer = KcaColors.Accent,
    onPrimaryContainer = KcaColors.White,
    secondary = KcaColors.Accent,
    onSecondary = KcaColors.White,
    secondaryContainer = KcaColors.Btn,
    onSecondaryContainer = KcaColors.BtnText,
    tertiary = KcaColors.MainBtn,
    onTertiary = KcaColors.BtnTextAccent,
    background = KcaColors.ActivityBack,
    onBackground = KcaColors.DarkGray,
    surface = KcaColors.ActivityBack,
    onSurface = KcaColors.DarkGray,
    surfaceVariant = KcaColors.DataArea,
    onSurfaceVariant = KcaColors.DarkGray,
    outline = KcaColors.ChipOutline,
    error = KcaColors.Danger,
    onError = KcaColors.White,
)

/**
 * Theme wrapper for every Compose screen and for Compose content hosted inside the old views.
 */
@Composable
fun KcaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KcaLightColorScheme,
        typography = KcaTypography,
        content = content,
    )
}
