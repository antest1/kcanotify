package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

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

    public SnapIndicator(WindowManager windowManager, LayoutInflater inflater) {
        this.windowManager = windowManager;
        snapLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,

                PixelFormat.TRANSLUCENT);
        snapLayoutParams.gravity = Gravity.TOP;
        snapIndicatorLayout = inflater.inflate(R.layout.view_snap_indicator, null);
    }


    public void remove() {
        if (snapIndicatorLayout.getParent() != null)
            windowManager.removeViewImmediate(snapIndicatorLayout);
    }

    public void update(float y, int maxY) {
        if (y < maxY / 4f) {
            snapLayoutParams.y = 0;
        } else if (y < maxY / 4f * 3f) {
            snapLayoutParams.y = maxY / 2;
        } else {
            snapLayoutParams.y = maxY;
        }
        windowManager.updateViewLayout(snapIndicatorLayout, snapLayoutParams);
    }

    public void show(int y, int maxY, int height) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                height
        );
        View outline = snapIndicatorLayout.findViewById(R.id.indicator_outline);
        outline.setLayoutParams(params);
        outline.requestLayout();
        if (y < maxY / 4f) {
            snapLayoutParams.y = 0;
        } else if (y < maxY / 4f * 3f) {
            snapLayoutParams.y = maxY / 2;
        } else {
            snapLayoutParams.y = maxY;
        }
        windowManager.addView(snapIndicatorLayout, snapLayoutParams);
    }
}
