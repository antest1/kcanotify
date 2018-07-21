package com.antest1.kcanotify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.antest1.kcanotify.KcaApiData.STYPE_AR;
import static com.antest1.kcanotify.KcaApiData.STYPE_AS;
import static com.antest1.kcanotify.KcaApiData.STYPE_BB;
import static com.antest1.kcanotify.KcaApiData.STYPE_BBV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CA;
import static com.antest1.kcanotify.KcaApiData.STYPE_CAV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVB;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVL;
import static com.antest1.kcanotify.KcaApiData.STYPE_DE;
import static com.antest1.kcanotify.KcaApiData.STYPE_FBB;
import static com.antest1.kcanotify.KcaApiData.STYPE_SS;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_NDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.STATE_HEAVYDMG;
import static com.antest1.kcanotify.KcaConstants.STATE_LIGHTDMG;
import static com.antest1.kcanotify.KcaConstants.STATE_MODERATEDMG;
import static com.antest1.kcanotify.KcaConstants.STATE_NORMAL;
import static com.antest1.kcanotify.KcaConstructPopupService.CONSTR_DATA_ACTION;
import static com.antest1.kcanotify.KcaConstructPopupService.getLeftTimeStr;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaDockingPopupService extends Service {
    public final static String DOCKING_DATA_ACTION = "docking_data_action";

    Runnable dockingTimer;
    ScheduledExecutorService dockingTimeScheduler = null;
    KcaDockingPopupListAdapter adapter;

    private View mView;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    LinearLayout timerView;
    ListView dockListView;
    JsonArray api_ndock;
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
            mView = mInflater.inflate(R.layout.view_docking_info, null);
            mView.setOnTouchListener(mViewTouchListener);
            ((TextView) mView.findViewById(R.id.view_dock_title)).setText(getStringWithLocale(R.string.viewmenu_docking_title));
            ((TextView) mView.findViewById(R.id.view_dock_list_btn)).setText(getStringWithLocale(R.string.viewmenu_docking_list));

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

            api_ndock = dbHelper.getJsonArrayValue(DB_KEY_NDOCKDATA);
            dockingTimer = new Runnable() {
                @Override
                public void run() {
                    Log.e("KCA-DKS", "dockingTimer");
                    try {
                        updatePopup();
                    } catch (Exception e) {
                        Log.e("KCA-DKS", getStringFromException(e));
                    }
                }
            };

            dockingTimeScheduler = Executors.newSingleThreadScheduledExecutor();
            dockingTimeScheduler.scheduleAtFixedRate(dockingTimer, 0, 1, TimeUnit.SECONDS);

            adapter = new KcaDockingPopupListAdapter();
            JsonArray damage_info = new JsonArray();
            JsonArray ship_info = dbHelper.getJsonArrayValue(DB_KEY_SHIPIFNO);
            if (ship_info != null) {
                JsonObject indock_list = new JsonObject();
                if (api_ndock != null) {
                    for (int i = 0; i < api_ndock.size(); i++) {
                        JsonObject item = api_ndock.get(i).getAsJsonObject();
                        if (item.get("api_state").getAsInt() != -1) {
                            int ship_id = item.get("api_ship_id").getAsInt();
                            if (ship_id > 0) indock_list.addProperty(item.get("api_ship_id").getAsString(), true);
                        }
                    }
                }

                for (int i = 0; i < ship_info.size(); i++) {
                    JsonObject item = ship_info.get(i).getAsJsonObject();
                    int max_hp = item.get("api_maxhp").getAsInt();
                    int now_hp = item.get("api_nowhp").getAsInt();
                    int ship_id = item.get("api_ship_id").getAsInt();
                    int hp_loss = max_hp - now_hp;
                    if (hp_loss > 0) {
                        JsonObject kcdata = KcaApiData.getKcShipDataById(ship_id, "name,stype");
                        if (kcdata != null && kcdata.has("stype")) {
                            String id = item.get("api_id").getAsString();
                            int level = item.get("api_lv").getAsInt();
                            JsonObject repair_item = new JsonObject();
                            String name = getShipTranslation(kcdata.get("name").getAsString(), false);
                            String name_level = KcaUtils.format("%s (Lv %d)", name, level);
                            int stype = kcdata.get("stype").getAsInt();
                            int repair_time = KcaDocking.getDockingTime(hp_loss, level, stype);
                            repair_item.addProperty("name", name_level);
                            repair_item.addProperty("time_raw", repair_time);
                            repair_item.addProperty("time", KcaUtils.getTimeStr(repair_time));
                            repair_item.addProperty("state", getState(now_hp, max_hp));
                            repair_item.addProperty("dock", indock_list.has(id));
                            damage_info.add(repair_item);
                        }
                    }
                }

                Type listType = new TypeToken<List<JsonObject>>() {
                }.getType();
                final List<JsonObject> shipItemList = new Gson().fromJson(damage_info, listType);
                StatComparator cmp = new StatComparator();
                Collections.sort(shipItemList, cmp);
                adapter.setItemList(shipItemList);

                timerView = mView.findViewById(R.id.view_dock_timer);
                timerView.setVisibility(View.VISIBLE);
                dockListView = mView.findViewById(R.id.view_dock_list);
                dockListView.setAdapter(adapter);
                dockListView.setVisibility(View.GONE);
                TextView dockListViewButton = mView.findViewById(R.id.view_dock_list_btn);
                dockListViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                dockListViewButton.setOnClickListener(v -> {
                    if (dockListView.getVisibility() == View.GONE) {
                        dockListViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                        dockListView.setVisibility(View.VISIBLE);
                        timerView.setVisibility(View.GONE);
                    } else {
                        dockListViewButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                        dockListView.setVisibility(View.GONE);
                        timerView.setVisibility(View.VISIBLE);
                    }
                });


            }
        }
    }

    private class StatComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String sort_key = "time_raw";
            return o2.get(sort_key).getAsInt() - o1.get(sort_key).getAsInt();
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
            if(intent.getAction().equals(DOCKING_DATA_ACTION)) {
                api_ndock = dbHelper.getJsonArrayValue(DB_KEY_NDOCKDATA);
                updatePopup();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (dockingTimeScheduler != null) {
            Log.e("KCA-CPS", "scheduler shutdown");
            dockingTimeScheduler.shutdown();
            dockingTimeScheduler = null;
        }
        if (mManager != null) mManager.removeView(mView);
        super.onDestroy();
    }

    final Handler handler = new Handler()  {
        public void handleMessage(Message msg) {
            if (api_ndock != null) {
                for (int i = 0; i < api_ndock.size(); i++) {
                    int index = i + 1;
                    JsonObject item = api_ndock.get(i).getAsJsonObject();
                    TextView nameview = mView.findViewById(getId(KcaUtils.format("dock%d_name", index), R.id.class));
                    TextView timeview = mView.findViewById(getId(KcaUtils.format("dock%d_time", index), R.id.class));
                    if (item.get("api_state").getAsInt() != -1) {
                        int ship_id = item.get("api_ship_id").getAsInt();
                        if (ship_id > 0) {
                            String ship_name = "";
                            JsonObject shipData = getUserShipDataById(ship_id, "ship_id");
                            JsonObject kcShipData = KcaApiData.getKcShipDataById(shipData.get("ship_id").getAsInt(), "name");
                            if (kcShipData != null) {
                                ship_name = getShipTranslation(kcShipData.get("name").getAsString(), false);
                            }
                            nameview.setText(ship_name);
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

    private void updatePopup() {
        Log.e("KCA-DKS", "updatePopup");
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

    private int getState(int nowhp, int maxhp) {
        float value = nowhp * 100 / maxhp;
        if (value > 75) return STATE_NORMAL;
        else if (value > 50) return STATE_LIGHTDMG;
        else if (value > 25) return STATE_MODERATEDMG;
        else return STATE_HEAVYDMG;
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