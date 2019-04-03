package com.antest1.kcanotify;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_KDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.PREF_SHOW_CONSTRSHIP_NAME;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaConstructPopupService extends Service {
    public final static String CONSTR_DATA_ACTION = "constr_data_action";

    Runnable constructTimer;
    ScheduledExecutorService constructTimeScheduler = null;

    private View mView;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    private boolean spoilerStatus = true;
    TextView constructionViewButton;

    WindowManager.LayoutParams mParams;

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

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            clickcount = 0;
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);

            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = mInflater.inflate(R.layout.view_ship_constr, null);
            mView.setOnTouchListener(mViewTouchListener);
            ((TextView) mView.findViewById(R.id.view_sc_title)).setText(getStringWithLocale(R.string.viewmenu_construction_title));

            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWidth = mView.getMeasuredWidth();
            popupHeight = mView.getMeasuredHeight();

            // Button (Fairy) Settings
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            mParams.gravity = Gravity.TOP | Gravity.START;
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

            mParams.x = (screenWidth - popupWidth) / 2;
            mParams.y = (screenHeight - popupHeight) / 2;
            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            constructTimer = () -> {
                Log.e("KCA-CPS", "constructTimer");
                try {
                    updatePopup();
                } catch (Exception e) {
                    Log.e("KCA-CPS", getStringFromException(e));
                }
            };

            spoilerStatus = getBooleanPreferences(getApplicationContext(), PREF_SHOW_CONSTRSHIP_NAME);
            constructionViewButton = mView.findViewById(R.id.view_sc_btn);
            constructionViewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    spoilerStatus = !spoilerStatus;
                    updateSpoilerButton();
                    updatePopup();
                }
            });

            updateSpoilerButton();
            constructTimeScheduler = Executors.newSingleThreadScheduledExecutor();
            constructTimeScheduler.scheduleAtFixedRate(constructTimer, 0, 1, TimeUnit.SECONDS);
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
            if(intent.getAction().equals(CONSTR_DATA_ACTION)) {
                updatePopup();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (constructTimeScheduler != null) {
            Log.e("KCA-CPS", "scheduler shutdown");
            constructTimeScheduler.shutdown();
            constructTimeScheduler = null;
        }
        if (mManager != null) mManager.removeView(mView);
        super.onDestroy();
    }

    final Handler handler = new Handler()  {
        public void handleMessage(Message msg) {
            //boolean show_shipname = getBooleanPreferences(getApplicationContext(), PREF_SHOW_CONSTRSHIP_NAME);
            JsonArray api_kdock = dbHelper.getJsonArrayValue(DB_KEY_KDOCKDATA);
            if (api_kdock != null) {
                for (int i = 0; i<api_kdock.size(); i++) {
                    int index = i + 1;
                    JsonObject item = api_kdock.get(i).getAsJsonObject();
                    TextView nameview = (TextView) mView.findViewById(getId(KcaUtils.format("sc%d_name", index), R.id.class));
                    TextView timeview = (TextView) mView.findViewById(getId(KcaUtils.format("sc%d_time", index), R.id.class));
                    if (item.get("api_state").getAsInt() != -1) {
                        int ship_id = item.get("api_created_ship_id").getAsInt();
                        if (ship_id > 0) {
                            JsonObject shipdata = KcaApiData.getKcShipDataById(item.get("api_created_ship_id").getAsInt(), "name");
                            if (spoilerStatus) {
                                nameview.setText(KcaApiData.getShipTranslation(shipdata.get("name").getAsString(), false));
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
    };

    private void updateSpoilerButton() {
        if (spoilerStatus) {
            constructionViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            constructionViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
        }
    }

    private void updatePopup() {
        Log.e("KCA-CPS", "updatePopup");
        handler.sendEmptyMessage(0);
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private static final int LONG_CLICK_DURATION = 800;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = mParams.x;
                    mViewY = mParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", mViewX, mViewY));
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.e("KCA", "Callback Canceled");
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        stopSelf();
                    }

                    int[] locations = new int[2];
                    mView.getLocationOnScreen(locations);
                    int xx = locations[0];
                    int yy = locations[1];
                    Log.e("KCA", KcaUtils.format("Coord: %d %d", xx, yy));
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;
                    if (mParams.x < 0) mParams.x = 0;
                    else if (mParams.x > screenWidth - popupWidth) mParams.x = screenWidth - popupWidth;
                    if (mParams.y < 0) mParams.y = 0;
                    else if (mParams.y > screenHeight - popupHeight) mParams.y = screenHeight - popupHeight;
                    mManager.updateViewLayout(mView, mParams);
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
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

        if (mParams != null) {
            if (mParams.x < 0) mParams.x = 0;
            else if (mParams.x > screenWidth - popupWidth) mParams.x = screenWidth - popupWidth;
            if (mParams.y < 0) mParams.y = 0;
            else if (mParams.y > screenHeight - popupHeight) mParams.y = screenHeight - popupHeight;
        }

        super.onConfigurationChanged(newConfig);
    }
}