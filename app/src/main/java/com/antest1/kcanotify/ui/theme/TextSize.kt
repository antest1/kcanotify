package com.antest1.kcanotify.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * The XML layouts size text in `dp` (e.g. `android:textSize="18dp"`), which does not follow the
 * system font scale. Compose sizes text in `sp`, which does.
 *
 * Using this helper keeps the rendered size identical to the current screens: `dp.toSp()` divides
 * by the font scale, and the text system multiplies it back, so both end up at the same pixels.
 */
@Composable
@ReadOnlyComposable
fun Dp.asTextSize(): TextUnit = with(LocalDensity.current) { toSp() }
