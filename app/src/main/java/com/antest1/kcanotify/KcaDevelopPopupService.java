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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_LATESTDEV;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaDevelopPopupService extends Service {
    public static final String DEV_DATA_ACTION = "dev_data_action";

    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver data_receiver;

    private View mView;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;

    ImageView ed_icon;
    TextView ed_name, ed_count, ed_ship, ed_time;

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
            loadTranslationData(getApplicationContext());
            LocalBroadcastManager.getInstance(this).registerReceiver((data_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));

            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mView = mInflater.inflate(R.layout.view_equip_dev, null);
            mView.setOnTouchListener(mViewTouchListener);
            ((TextView) mView.findViewById(R.id.view_ed_title)).setText(getStringWithLocale(R.string.viewmenu_develop_title));

            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWidth = mView.getMeasuredWidth();
            popupHeight = mView.getMeasuredHeight();

            ed_icon = (ImageView) mView.findViewById(R.id.ed_icon);
            ed_name = (TextView) mView.findViewById(R.id.ed_name);
            ed_count = (TextView) mView.findViewById(R.id.ed_count);
            ed_ship = (TextView) mView.findViewById(R.id.ed_ship);
            ed_time = (TextView) mView.findViewById(R.id.ed_time);

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
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-CPS", "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null) {
            int typeres = 0;
            JsonObject data = null;
            if(intent.getAction() != null && intent.getAction().equals(DEV_DATA_ACTION)) {
                data = new JsonParser().parse(intent.getExtras().getString("data")).getAsJsonObject();
            } else {
                data = dbHelper.getJsonObjectValue(DB_KEY_LATESTDEV);
            }
            if (data != null) {
                ed_time.setText(data.get("time").getAsString());
                ed_ship.setText(KcaApiData.getShipTranslation(data.get("flagship").getAsString(), false));
                ed_count.setText(data.get("count").getAsString());
                String item_name = data.get("name").getAsString();
                if (item_name.equals("item_fail")) {
                    ed_name.setText(getStringWithLocale(R.string.develop_failed_text));
                } else {
                    ed_name.setText(KcaApiData.getItemTranslation(item_name));
                }
                try {
                    typeres = getId(KcaUtils.format("item_%d", data.get("type").getAsInt()), R.mipmap.class);
                } catch (Exception e) {
                    typeres = R.mipmap.item_0;
                }
                ed_icon.setImageResource(typeres);
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
                    else if (mParams.x > screenWidth - popupWidth) mParams.x = screenWidth - popupWidth;
                    if (mParams.y < 0) mParams.y = 0;
                    else if (mParams.y > screenHeight - popupHeight) mParams.y = screenHeight - popupHeight;
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