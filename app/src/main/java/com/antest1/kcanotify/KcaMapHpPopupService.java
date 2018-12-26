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
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_APIMAPINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaMapHpPopupService extends Service {
    public static final String MAPHP_SHOW_ACTION = "maphp_show_action";
    public static final String MAPHP_RESET_ACTION = "maphp_reset_action";

    // public static final String count_data_raw = "{\"15\":4,\"16\":7,\"25\":4,\"35\":4,\"44\":4,\"45\":5,\"52\":4,\"53\":5,\"54\":5,\"55\":5,\"62\":3,\"63\":4,\"64\":5,\"65\":6,\"71\":3}";
    // public static final JsonObject count_data = new JsonParser().parse(count_data_raw).getAsJsonObject();
    public static final String maphp_format = "[%s] %s %d/%d";
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver data_receiver;

    private View mView;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;

    TextView hp_info;

    public static int type;
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
            broadcaster = LocalBroadcastManager.getInstance(this);
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            data_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //String s = intent.getStringExtra(KCA_MSG_DATA);
                    String s = dbHelper.getValue(DB_KEY_BATTLEINFO);
                    broadcaster.sendBroadcast(new Intent(KCA_MSG_BATTLE_VIEW_REFRESH));
                    Log.e("KCA", "KCA_MSG_BATTLE_INFO Received: \n".concat(s));
                }
            };

            LocalBroadcastManager.getInstance(this).registerReceiver((data_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));

            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mView = mInflater.inflate(R.layout.view_map_hp, null);
            mView.setOnTouchListener(mViewTouchListener);
            ((TextView) mView.findViewById(R.id.view_hp_title)).setText(getStringWithLocale(R.string.viewmenu_maphp_title));

            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWidth = mView.getMeasuredWidth();
            popupHeight = mView.getMeasuredHeight();

            hp_info = (TextView) mView.findViewById(R.id.hp_info);

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
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-MPS", "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(MAPHP_RESET_ACTION)) {
                hp_info.setText("data not loaded");
            }
            if (intent.getAction().equals(MAPHP_SHOW_ACTION)) {
                try {
                    JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_APIMAPINFO);
                    if (data != null) {
                        String content = "";
                        List<String> eventhp_list = new ArrayList<>();
                        List<String> maphp_list = new ArrayList<>();
                        for (int i = 0; i < data.size(); i++) {
                            JsonObject item = data.get(i).getAsJsonObject();
                            int id = item.get("api_id").getAsInt();
                            String map_id_str = getMapString(id);
                            if (item.has("api_eventmap")) {
                                JsonObject eventitem = item.getAsJsonObject("api_eventmap");
                                if (eventitem.has("api_state") && eventitem.get("api_state").getAsInt() != 2) {
                                    int gauge_type = 0;
                                    int gauge_num = 0;
                                    if (eventitem.has("api_gauge_type")) {
                                        gauge_type = eventitem.get("api_gauge_type").getAsInt();
                                    }
                                    if (eventitem.has("api_gauge_num")) {
                                        gauge_num = eventitem.get("api_gauge_num").getAsInt();
                                    }
                                    int now_maphp = eventitem.get("api_now_maphp").getAsInt();
                                    int max_maphp = eventitem.get("api_max_maphp").getAsInt();
                                    eventhp_list.add(getMapHpStr(gauge_type, gauge_num, id, now_maphp, max_maphp));
                                }
                            } else if (item.has("api_defeat_count")) {
                                int gauge_type = item.get("api_gauge_type").getAsInt();
                                int gauge_num = item.get("api_gauge_num").getAsInt();
                                int defeat_count = item.get("api_defeat_count").getAsInt();
                                int total_count = item.get("api_required_defeat_count").getAsInt();
                                maphp_list.add(getMapHpStr(gauge_type, gauge_num, id, total_count - defeat_count, total_count));
                            }
                        }
                        for (String item : eventhp_list) {
                            content = content.concat(item).concat("\n");
                        }
                        for (String item : maphp_list) {
                            content = content.concat(item).concat("\n");
                        }
                        hp_info.setText(content.trim());
                    } else {
                        hp_info.setText("data not loaded");
                    }
                } catch (Exception e) {
                    hp_info.setText("error while processing");
                    dbHelper.putValue(DB_KEY_APIMAPINFO, (new JsonArray()).toString());
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(data_receiver);
        if (mManager != null) mManager.removeView(mView);
        super.onDestroy();
    }

    private void stopPopup() {
        active = false;
        stopSelf();
    }

    private String getMapString(int id) {
        int map = id / 10;
        int no = id % 10;
        if (map > 10) {
            return "E-" + no;
        } else {
            return "" + map + "-" + no;
        }
    }

    private String getMapHpStr(int type, int num, int id, int current, int total) {
        String num_text = "";
        if (num > 0 && (id == 72 || id > 100)) num_text = KcaUtils.format("(#%d)", num);
        if (type == 3) {
            return KcaUtils.format(maphp_format, getMapString(id), "TP".concat(num_text), current, total);
        } else {
            return KcaUtils.format(maphp_format, getMapString(id), "HP".concat(num_text), current, total);
        }
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
                        stopPopup();
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
                    else if (mParams.x > screenWidth - popupWidth)
                        mParams.x = screenWidth - popupWidth;
                    if (mParams.y < 0) mParams.y = 0;
                    else if (mParams.y > screenHeight - popupHeight)
                        mParams.y = screenHeight - popupHeight;
                    mManager.updateViewLayout(mView, mParams);
                    break;
            }

            return true;
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
            if (mParams.x < 0) mParams.x = 0;
            else if (mParams.x > screenWidth - popupWidth) mParams.x = screenWidth - popupWidth;
            if (mParams.y < 0) mParams.y = 0;
            else if (mParams.y > screenHeight - popupHeight) mParams.y = screenHeight - popupHeight;
        }

        super.onConfigurationChanged(newConfig);
    }
}