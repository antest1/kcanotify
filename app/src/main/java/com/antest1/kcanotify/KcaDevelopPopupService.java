package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
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

import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_LATESTDEV;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaDevelopPopupService extends BaseService {
    public static final String DEV_DATA_ACTION = "dev_data_action";

    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver data_receiver;

    private View popupView;
    private WindowManager windowManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    private JsonObject receivedDevResult;
    WindowManager.LayoutParams layoutParams;
    NotificationManagerCompat notificationManager;

    List<View> ed_items = new ArrayList();
    ImageView ed_icon;
    TextView ed_name, ed_count, ed_ship, ed_time;

    public static int type;
    public static boolean active = false;

    public static boolean isActive() {
        return active;
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
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
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
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
        }
    }

    private void setPopupLayout() {
        if (checkPopupExist()) return;

        LayoutInflater mInflater = LayoutInflater.from(this);
        popupView = mInflater.inflate(R.layout.view_equip_dev, null);
        popupView.setOnTouchListener(mViewTouchListener);
        popupView.findViewById(R.id.view_ed_head).setOnTouchListener(mViewTouchListener);
        ((TextView) popupView.findViewById(R.id.view_ed_title)).setText(getString(R.string.viewmenu_develop_title));

        setPopupContent();
        updatePopup();

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWidth = popupView.getMeasuredWidth();
        popupHeight = popupView.getMeasuredHeight();

        SizeInsets screenSize = KcaUtils.getDefaultDisplaySizeInsets(this);
        screenWidth = screenSize.size.x;
        screenHeight = screenSize.size.y;
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
        // do nothing
    }

    private void updatePopup() {
        if (popupView != null) {
            int ship_id = -1;
            if (receivedDevResult.has("flagship_id")) ship_id = receivedDevResult.get("flagship_id").getAsInt();
            String ship_name = receivedDevResult.get("flagship").getAsString();

            ed_time = popupView.findViewById(R.id.ed_time);
            ed_ship = popupView.findViewById(R.id.ed_ship);
            ed_time.setText(receivedDevResult.get("time").getAsString());
            ed_ship.setText(KcaApiData.getShipTranslation(ship_name, ship_id, false));
            if (receivedDevResult.has("items")) {
                JsonArray arr = receivedDevResult.getAsJsonArray("items");
                for (int i = 0; i < arr.size(); i++) {
                    setItemLayout(i+1, arr.get(i).getAsJsonObject());
                }
            } else {
                setSingleItemLayout(receivedDevResult.getAsJsonObject());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-CPS", "onStartCommand");
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null) {
            receivedDevResult = null;
            if(intent.getAction() != null && intent.getAction().equals(DEV_DATA_ACTION)) {
                receivedDevResult = JsonParser.parseString(intent.getExtras().getString("data")).getAsJsonObject();
                updatePopup();
            } else {
                receivedDevResult = dbHelper.getJsonObjectValue(DB_KEY_LATESTDEV);
                if (receivedDevResult != null) setPopupLayout();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void setSingleItemLayout(JsonObject data) {
        setItemLayout(1, data);
        popupView.findViewById(R.id.ed_item2).setVisibility(View.GONE);
        popupView.findViewById(R.id.ed_item3).setVisibility(View.GONE);
    }

    private void setItemLayout(int index, JsonObject data) {
        int typeres = 0;
        ImageView ed_icon = popupView.findViewById(getId("ed_icon" + index, R.id.class));
        TextView ed_name = popupView.findViewById(getId("ed_name" + index, R.id.class));
        TextView ed_count = popupView.findViewById(getId("ed_count" + index, R.id.class));

        ed_count.setText(data.get("count").getAsString());
        String item_name = data.get("name").getAsString();
        if (item_name.equals("item_fail")) {
            ed_name.setText(getString(R.string.develop_failed_text));
            typeres = R.mipmap.item_99;
        } else {
            ed_name.setText(KcaApiData.getSlotItemTranslation(item_name));
            try {
                typeres = getId(KcaUtils.format("item_%d", data.get("type").getAsInt()), R.mipmap.class);
            } catch (Exception e) {
                typeres = R.mipmap.item_0;
            }
        }
        ed_icon.setImageResource(typeres);
        popupView.findViewById(getId("ed_item" + index, R.id.class)).setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        active = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(data_receiver);
        if (windowManager != null && popupView != null) windowManager.removeView(popupView);
        super.onDestroy();
    }

    private void stopPopup() {
        stopSelf();
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
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
                        if (id == R.id.view_ed_head) {
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && popupView != null && popupView.getParent() != null) {
            windowManager.removeViewImmediate(popupView);
        }
    }

    public boolean checkPopupExist() {
        return popupView != null && popupView.getParent() != null;
    }
}