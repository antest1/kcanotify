package com.antest1.kcanotify;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;

public class DraggableOverlayButtonLayout extends RelativeLayout {
    public DraggableOverlayButtonLayout(Context context) {
        this(context, null);
    }

    public DraggableOverlayButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableOverlayButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getChildCount() > 0) {
            // Pass the event to all the children
            for (int i = 0; i < getChildCount(); i++) {
                MotionEvent backEvent = MotionEvent.obtain(event);
                View child = getChildAt(i);
                child.dispatchTouchEvent(backEvent);
                backEvent.recycle();
            }
        }
        // Always consume the event
        return true;
    }

    private ValueAnimator animatorX;
    private ValueAnimator animatorY;
    public void animateTo(int startX, int startY, int endX, int endY, float tensionX, float tensionY, int duration, WindowManager windowManager, WindowManager.LayoutParams layoutParams) {
        animatorX = ValueAnimator.ofInt(startX, endX);
        animatorY = ValueAnimator.ofInt(startY, endY);
        animatorX.setDuration(duration);
        animatorX.setInterpolator(new OvershootInterpolator(tensionX));
        animatorY.setDuration(duration);
        animatorY.setInterpolator(new OvershootInterpolator(tensionY));

        animatorX.addUpdateListener(animation -> {
            // Get the current animated value
            layoutParams.x = (int) animation.getAnimatedValue();
            layoutParams.y = (int) animatorY.getAnimatedValue();
            windowManager.updateViewLayout(this, layoutParams);
        });

        // Start the animation
        animatorX.start();
        animatorY.start();
    }

    public void cancelAnimations(){
        if (animatorX!= null) animatorX.cancel();
        if (animatorY!= null) animatorY.cancel();
    }
}