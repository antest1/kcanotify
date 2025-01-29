package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_KDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_SHOW_CONSTRSHIP_NAME;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaConstructPopupService extends Service {
    public final static String CONSTR_DATA_ACTION = "constr_data_action";

    Runnable constructTimer;
    ScheduledExecutorService constructTimeScheduler = null;

    private View popupView;
    private WindowManager windowManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    private boolean spoilerStatus = true;
    View constructionViewButton;

    WindowManager.LayoutParams layoutParams;

    public static int type;
    public static int clickcount;
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

    @SuppressLint("DiscouragedApi")
    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            active = true;
            clickcount = 0;
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            constructTimer = () -> {
                Log.e("KCA-CPS", "constructTimer");
                try {
                    updatePopup();
                } catch (Exception e) {
                    Log.e("KCA-CPS", getStringFromException(e));
                }
            };

            constructTimeScheduler = Executors.newSingleThreadScheduledExecutor();
            constructTimeScheduler.scheduleAtFixedRate(constructTimer, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void setPopupLayout() {
        if (checkPopupExist()) return;

        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = mInflater.inflate(R.layout.view_ship_constr, null);
        popupView.setOnTouchListener(mViewTouchListener);
        popupView.findViewById(R.id.view_sc_head).setOnTouchListener(mViewTouchListener);
        ((TextView) popupView.findViewById(R.id.view_sc_title)).setText(getStringWithLocale(R.string.viewmenu_construction_title));

        setPopupContent();
        updatePopup();

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWidth = popupView.getMeasuredWidth();
        popupHeight = popupView.getMeasuredHeight();

        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: " + screenWidth + " " + screenHeight);

        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.START;
        }

        layoutParams.x = (screenWidth - popupWidth) / 2;
        layoutParams.y = (screenHeight - popupHeight) / 2;
        windowManager.addView(popupView, layoutParams);
    }

    private void setPopupContent() {
        spoilerStatus = getBooleanPreferences(getApplicationContext(), PREF_SHOW_CONSTRSHIP_NAME);
        constructionViewButton = popupView.findViewById(R.id.view_sc_btn);
        constructionViewButton.setOnClickListener(v -> {
            spoilerStatus = !spoilerStatus;
            updateSpoilerButton();
            updatePopup();
        });
        updateSpoilerButton();
        popupView.setVisibility(View.VISIBLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-CPS", "onStartCommand");
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(CONSTR_DATA_ACTION)) {
                setPopupLayout();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        active = false;
        if (windowManager != null && popupView != null && popupView.getParent() != null) {
            windowManager.removeViewImmediate(popupView);
        }
        if (constructTimeScheduler != null) {
            Log.e("KCA-CPS", "scheduler shutdown");
            constructTimeScheduler.shutdown();
            constructTimeScheduler = null;
        }
        super.onDestroy();
    }

    private void stopPopup() {
        if (popupView != null) popupView.setVisibility(View.GONE);
        stopSelf();
    }

    private void updateSpoilerButton() {
        if (spoilerStatus) {
            constructionViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            constructionViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
        }
    }

    private void updatePopup() {
        JsonArray api_kdock = dbHelper.getJsonArrayValue(DB_KEY_KDOCKDATA);
        if (api_kdock != null) {
            for (int i = 0; i<api_kdock.size(); i++) {
                int index = i + 1;
                JsonObject item = api_kdock.get(i).getAsJsonObject();
                TextView nameview = popupView.findViewById(getId(KcaUtils.format("sc%d_name", index), R.id.class));
                TextView timeview = popupView.findViewById(getId(KcaUtils.format("sc%d_time", index), R.id.class));
                if (item.get("api_state").getAsInt() != -1) {
                    int ship_id = item.get("api_created_ship_id").getAsInt();
                    if (ship_id > 0) {
                        JsonObject shipdata = KcaApiData.getKcShipDataById(ship_id, "name");
                        if (spoilerStatus) {
                            nameview.setText(KcaApiData.getShipTranslation(shipdata.get("name").getAsString(), ship_id, false));
                        } else {
                            nameview.setText("？？？");
                        }
                        timeview.setText(getLeftTimeStr(item.get("api_complete_time").getAsLong()));
                    } else {
                        nameview.setText("-");
                        timeview.setText("");
                    }
                } else {
                    nameview.setText("CLOSED");
                    timeview.setText("");
                }
            }
        }
    }
    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private final View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = layoutParams.x;
                    mViewY = layoutParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", mViewX, mViewY));
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.e("KCA", "Callback Canceled");
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == R.id.view_sc_head) {
                            stopPopup();
                        }
                    }

                    int[] locations = new int[2];
                    popupView.getLocationOnScreen(locations);
                    int xx = locations[0];
                    int yy = locations[1];
                    Log.e("KCA", KcaUtils.format("Coord: %d %d", xx, yy));
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    layoutParams.x = mViewX + x;
                    layoutParams.y = mViewY + y;
                    if (layoutParams.x < 0) layoutParams.x = 0;
                    else if (layoutParams.x > screenWidth - popupWidth) layoutParams.x = screenWidth - popupWidth;
                    if (layoutParams.y < 0) layoutParams.y = 0;
                    else if (layoutParams.y > screenHeight - popupHeight) layoutParams.y = screenHeight - popupHeight;
                    windowManager.updateViewLayout(popupView, layoutParams);
                    break;
            }

            return true;
        }
    };

    public static String getLeftTimeStr(long complete_time) {
        if (complete_time <= 0) return "Completed!";
        else {
            int left_time = (int) (complete_time - System.currentTimeMillis()) / 1000;
            if (left_time < 0) return "Completed!";

            int sec, min, hour;
            sec = left_time;
            min = sec / 60;
            hour = min / 60;
            sec = sec % 60;
            min = min % 60;

            return KcaUtils.format("%02d:%02d:%02d", hour, min, sec);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && checkPopupExist()) {
            windowManager.removeViewImmediate(popupView);
        }
    }

    public boolean checkPopupExist() {
        return popupView != null && popupView.getParent() != null;
    }
}