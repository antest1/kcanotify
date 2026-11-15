package com.antest1.kcanotify.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.antest1.kcanotify.ui.theme.KcaColors

/**
 * Replacement for the `androidx.appcompat.widget.Toolbar` used by every current screen.
 *
 * Matching details of the XML toolbar:
 * - background `@color/colorPrimary`, white title and icons;
 * - `?attr/actionBarSize` resolves to the Material 3 compact app bar height (64dp), which is also
 *   the [TopAppBar] default;
 * - the status bar inset is drawn in the toolbar colour, like `KcaEdgeToEdge` paints the strip
 *   above the XML toolbar, so the bar can be used as the top edge of an edge-to-edge screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KcaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = navigationContentDescription
                            ?: stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = KcaColors.Primary,
            titleContentColor = KcaColors.ToolbarText,
            navigationIconContentColor = KcaColors.ToolbarText,
            actionIconContentColor = KcaColors.ToolbarText,
        ),
    )
}
