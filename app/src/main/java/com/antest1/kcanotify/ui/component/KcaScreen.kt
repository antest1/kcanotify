package com.antest1.kcanotify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.antest1.kcanotify.ui.theme.KcaColors

/**
 * The outer `LinearLayout` every activity currently uses:
 * a [KcaTopAppBar] on top and the screen content filling the rest of the window.
 *
 * The screen content is a [ColumnScope], so screens can use `Modifier.weight(...)` like the
 * `layout_weight` attributes in the XML layouts.
 *
 * Inset handling matches `KcaEdgeToEdge`, which the XML screens rely on:
 * - the top app bar paints the status-bar strip in the toolbar colour,
 * - the content stops above the navigation bar and the IME, with the screen background filling
 *   the area behind those bars.
 */
@Composable
fun KcaScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KcaColors.Primary),
    ) {
        KcaTopAppBar(
            title = title,
            onBackClick = onBackClick,
            navigationContentDescription = navigationContentDescription,
            actions = actions,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            content = content,
        )
    }
}
