package com.antest1.kcanotify.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Filled Material 3 button, equivalent to a plain XML `<Button>`.
 *
 * Like the XML buttons, the caller can override the colours (most layouts set
 * `android:backgroundTint="@color/colorBtn"`, others keep the theme primary).
 */
@Composable
fun KcaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text)
    }
}

/**
 * Tonal Material 3 button, equivalent to `style="@style/Widget.Material3.Button.TonalButton"`.
 *
 * `AppTheme` sets `colorSecondaryContainer = @color/colorBtn` and the button text appearance to
 * black, so the defaults here reproduce the grey tonal buttons used throughout the app.
 */
@Composable
fun KcaTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text)
    }
}

/** Convenience overload of [KcaButton] with the `colorMainBtn` blue used for primary actions. */
@Composable
fun KcaMainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    KcaButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = com.antest1.kcanotify.ui.theme.KcaColors.MainBtn,
        contentColor = com.antest1.kcanotify.ui.theme.KcaColors.BtnTextAccent,
    )
}
