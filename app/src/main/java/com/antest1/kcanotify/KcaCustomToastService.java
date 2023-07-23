package com.antest1.kcanotify;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.antest1.kcanotify.KcaUtils.adjustAlpha;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaCustomToastService extends Service {
    public static final String TOAST_SHOW_ACTION = "toast_show_action";
    public static final int CUSTOM_LENGTH_SHORT = 2000;
    public static final int CUSTOM_LENGTH_LONG = 3500;
    public static final int CUSTOM_FADE_DURATION = 500;

    private View mView;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    WindowManager.LayoutParams mParams;
    private Handler mHandler;
    TextView toast_view;

    public static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        active = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            mHandler = new Handler();
            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = mInflater.inflate(R.layout.toast_layout, null);
            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            toast_view = mView.findViewById(R.id.toast_text);
            mView.setVisibility(View.GONE);
            //mView.setOnClickListener(mViewClickListener);
            mView.setOnTouchListener(mViewTouchListener);
            // Button (Fairy) Settings
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            mParams.gravity = Gravity.TOP | Gravity.START;
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));
            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-CPS", "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if(intent.getAction().equals(TOAST_SHOW_ACTION)) {
                JsonObject data = JsonParser.parseString(intent.getExtras().getString("data")).getAsJsonObject();
                String body = data.get("text").getAsString();
                int color = data.get("color").getAsInt();
                int duration = data.get("duration").getAsInt();
                toast_view.setText(body);
                toast_view.setBackgroundColor(adjustAlpha(color, 0.8f));

                mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                popupWidth = mView.getMeasuredWidth();
                popupHeight = mView.getMeasuredHeight();
                mParams.x = (screenWidth - popupWidth) / 2;
                mParams.y = (int)((screenHeight - popupHeight) * 0.8);
                mManager.updateViewLayout(mView, mParams);
                mView.setAlpha(0f);
                mView.setVisibility(View.VISIBLE);
                mView.animate()
                        .alpha(1f)
                        .setDuration(CUSTOM_FADE_DURATION)
                        .setListener(null);

                if (duration == 1) duration = CUSTOM_LENGTH_LONG;
                else duration = CUSTOM_LENGTH_SHORT;
                mHandler.postDelayed(mRunnable, duration);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mManager != null) mManager.removeView(mView);
        super.onDestroy();
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (view.getId() == R.id.toast_text) {
                mView.postDelayed(() -> mView.setVisibility(View.GONE), CUSTOM_FADE_DURATION);
                mHandler.removeCallbacks(mRunnable);
            }
            return false;
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mView.animate()
            .alpha(0f)
            .setDuration(CUSTOM_FADE_DURATION)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mView.setVisibility(View.GONE);
                }
            });
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

        if (mParams != null) {
            mParams.x = (screenWidth - popupWidth) / 2;
            mParams.y = (int)((screenHeight - popupHeight) * 0.8);
        }

        super.onConfigurationChanged(newConfig);
    }
}