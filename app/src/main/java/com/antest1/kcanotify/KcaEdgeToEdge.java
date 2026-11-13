package com.antest1.kcanotify;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Draws activities behind the system bars while keeping their layouts clear of them.
 * The layout gets margins for the bars, and on screens with a toolbar at the top the strip
 * under the status bar is painted in the toolbar color.
 */
public final class KcaEdgeToEdge {
    private KcaEdgeToEdge() {}

    public static void apply(ComponentActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);
        boolean hasTopBar = findTopBar(root) != null;

        // toolbars are colorPrimary with white icons, and screens without one are light;
        // the navigation bar keeps the androidx default, which follows the system dark mode
        SystemBarStyle statusBarStyle = hasTopBar
                ? SystemBarStyle.dark(Color.TRANSPARENT)
                : SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK);
        EdgeToEdge.enable(activity, statusBarStyle);

        StatusBarBackground statusBarBackground = hasTopBar
                ? new StatusBarBackground(ContextCompat.getColor(activity, R.color.colorPrimary)) : null;
        if (statusBarBackground != null) content.setBackground(statusBarBackground);

        // margins rather than padding: a CoordinatorLayout root places snackbar-dodging children
        // from its full height, so padding left the main GAME START button under the navigation bar
        // after a snackbar went away; the content frame always gives its children margin params
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) root.getLayoutParams();
        Rect base = getBaseMargins(root, margins);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int rootLeft = base.left + bars.left;
            int rootTop = base.top + bars.top;
            int rootRight = base.right + bars.right;
            int rootBottom = base.bottom + Math.max(bars.bottom, ime);
            // insets are dispatched again whenever a snackbar or the bottom bar attaches, so only
            // lay out again when the margins change
            if (margins.leftMargin != rootLeft || margins.topMargin != rootTop
                    || margins.rightMargin != rootRight || margins.bottomMargin != rootBottom) {
                margins.setMargins(rootLeft, rootTop, rootRight, rootBottom);
                view.setLayoutParams(margins);
            }
            if (statusBarBackground != null) statusBarBackground.setHeight(bars.top);
            return WindowInsetsCompat.CONSUMED;
        });
        // an attached root gets no insets for the new listener and status bar background otherwise
        ViewCompat.requestApplyInsets(root);
    }

    // the margins the layout itself sets, kept on the root: when the content changes again
    // with the same root, its margins already include the bars
    private static Rect getBaseMargins(View root, ViewGroup.MarginLayoutParams margins) {
        Rect base = (Rect) root.getTag(R.id.edge_to_edge_base_margins);
        if (base == null) {
            base = new Rect(margins.leftMargin, margins.topMargin, margins.rightMargin, margins.bottomMargin);
            root.setTag(R.id.edge_to_edge_base_margins, base);
        }
        return base;
    }

    // the toolbar at the top of the layout, found by following the first children from the root
    @Nullable
    private static Toolbar findTopBar(View view) {
        while (view instanceof ViewGroup) {
            if (view instanceof Toolbar) return (Toolbar) view;
            ViewGroup group = (ViewGroup) view;
            if (group.getChildCount() == 0) return null;
            view = group.getChildAt(0);
        }
        return null;
    }

    // fills only the top of its bounds, the strip under the status bar above the layout
    private static final class StatusBarBackground extends Drawable {
        private final Paint paint = new Paint();
        private int height;

        StatusBarBackground(int color) {
            paint.setColor(color);
        }

        void setHeight(int height) {
            if (this.height == height) return;
            this.height = height;
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + height, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
