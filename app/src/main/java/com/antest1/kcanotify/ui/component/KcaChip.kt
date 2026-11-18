package com.antest1.kcanotify.ui.component

import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.antest1.kcanotify.ui.theme.KcaColors

/**
 * Replacement for the `com.google.android.material.chip.Chip` used for the world selectors,
 * great-success toggle, daihatsu counter, filters, etc.
 *
 * The XML chips are non-selectable action chips (no check mark) with an explicit
 * `app:chipBackgroundColor`, 8dp corners and a 15dp label, so this wraps [SuggestionChip] instead
 * of `FilterChip`.
 */
@Composable
fun KcaChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = KcaColors.Btn,
    contentColor: Color = KcaColors.BtnText,
    textSize: TextUnit = 15.sp,
    icon: @Composable (() -> Unit)? = null,
) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                color = contentColor,
                fontSize = textSize,
            )
        },
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            iconContentColor = contentColor,
        ),
    )
}
