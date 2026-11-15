package com.antest1.kcanotify.ui.component

import com.antest1.kcanotify.BaseActivity
import com.antest1.kcanotify.KcaEdgeToEdge

/**
 * Base class for activities whose whole content is Compose.
 *
 * It keeps [BaseActivity]'s locale handling, but replaces `KcaEdgeToEdge.apply`: that helper looks
 * for an AppCompat `Toolbar` in the view tree to pick the status-bar icon style and paint the strip
 * above the layout, and neither exists when the content is a `ComposeView`. Instead the Compose
 * screen itself draws the top app bar behind the status bar ([KcaScreen]), so only the
 * edge-to-edge flags and light status-bar icons are needed.
 */
abstract class KcaComposeActivity : BaseActivity() {
    override fun applyEdgeToEdge() {
        KcaEdgeToEdge.applyForCompose(this)
    }
}
