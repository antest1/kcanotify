package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class SnapIndicator {
    private final View snapIndicatorLayout;
    private final WindowManager.LayoutParams snapLayoutParams;
    private final WindowManager windowManager;

    public SnapIndicator(Context context, WindowManager windowManager, LayoutInflater inflater) {
        this.windowManager = windowManager;
        snapLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                getParamsFlags(),
                PixelFormat.TRANSLUCENT);
        snapLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        snapIndicatorLayout = inflater.inflate(R.layout.view_snap_indicator, null);
    }

    private int getParamsFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    }

    public void remove() {
        if (snapIndicatorLayout.getParent() != null)
            windowManager.removeViewImmediate(snapIndicatorLayout);
    }

    public void update(float y, int maxY, int paddingTop) {
        if (y < paddingTop + maxY / 4f) {
            snapLayoutParams.y = paddingTop;
        } else if (y < paddingTop + maxY / 4f * 3f) {
            snapLayoutParams.y = paddingTop + maxY / 2;
        } else {
            snapLayoutParams.y = paddingTop + maxY;
        }
        windowManager.updateViewLayout(snapIndicatorLayout, snapLayoutParams);
    }

    public void show(int y, int maxY, int w, int h, int paddingLeft, int paddingTop) {
        snapLayoutParams.width = w;
        snapLayoutParams.height = h;
        View outline = snapIndicatorLayout.findViewById(R.id.indicator_outline);
        snapLayoutParams.x = paddingLeft;
        if (y < paddingTop + maxY / 4f) {
            snapLayoutParams.y = paddingTop;
        } else if (y < paddingTop + maxY / 4f * 3f) {
            snapLayoutParams.y = paddingTop + maxY / 2;
        } else {
            snapLayoutParams.y = paddingTop + maxY;
        }
        windowManager.addView(snapIndicatorLayout, snapLayoutParams);
    }
}
