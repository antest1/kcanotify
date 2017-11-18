package com.antest1.kcanotify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
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
import android.widget.Toast;

import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.checkUserShipDataLoaded;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_APIMAPINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaMapHpPopupService.MAPHP_RESET_ACTION;
import static com.antest1.kcanotify.KcaMapHpPopupService.MAPHP_SHOW_ACTION;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaFleetCheckPopupService extends Service {
    public static final String FCHK_SHOW_ACTION = "fchk_show_action";
    public static final String FCHK_RESET_ACTION = "fchk_reset_action";

    private View mView;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDeckInfo deckInfoCalc;
    private KcaDBHelper dbHelper;
    JsonArray portdeckdata;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;

    TextView fchk_info;

    public static int type;
    public static boolean active = false;
    public static int recent_no = 1;
    public static boolean isActive() {
        return active;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    public String getFleetName() {
        if (recent_no == 5) return getStringWithLocale(R.string.fleetview_combined);
        else return KcaUtils.format("#%d", recent_no);
    }

    public void increaseNo() {
        recent_no += 1;
        if (recent_no > 5) recent_no = 1;
        setText();
    }

    public void decreaseNo() {
        recent_no -= 1;
        if (recent_no < 1) recent_no = 5;
        setText();
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
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());

            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mView = mInflater.inflate(R.layout.view_fleet_check, null);
            mView.setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.view_fchk_head).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.fchk_prev).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.fchk_next).setOnTouchListener(mViewTouchListener);

            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWidth = mView.getMeasuredWidth();
            popupHeight = mView.getMeasuredHeight();

            fchk_info = mView.findViewById(R.id.fchk_value);

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

            mParams.x = 0;
            mParams.y = 0;
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
            if (intent.getAction().equals(FCHK_SHOW_ACTION)) {
                portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                setText();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mManager != null) mManager.removeView(mView);
        super.onDestroy();
    }

    private void stopPopup() {
        active = false;
        stopSelf();
    }

    private void setText() {
        if (fchk_info != null) {
            int target = recent_no - 1;
            String target_str = String.valueOf(target);
            if (recent_no == 5) {
                target = 0;
                target_str = "0,1";
            }

            ((TextView) mView.findViewById(R.id.view_fchk_title)).setText(getFleetName());
            if (KcaApiData.isGameDataLoaded() && KcaApiData.checkUserShipDataLoaded() && portdeckdata != null) {
                int cn = getSeekCn();
                String seekType = getSeekType();

                int[] airPowerRange = deckInfoCalc.getAirPowerRange(portdeckdata, target, KcaBattle.getEscapeFlag());
                String airPowerValue = KcaUtils.format(getStringWithLocale(R.string.kca_toast_airpower), airPowerRange[0], airPowerRange[1]);
                String seekValue = KcaUtils.format(getStringWithLocale(R.string.kca_toast_seekvalue_f), seekType, deckInfoCalc.getSeekValue(portdeckdata, target_str, cn, KcaBattle.getEscapeFlag()));
                int[] tp = deckInfoCalc.getTPValue(portdeckdata, target_str, KcaBattle.getEscapeFlag());
                String tpValue = KcaUtils.format(getStringWithLocale(R.string.kca_view_tpvalue), tp[1], tp[0]);
                List<String> toastList = new ArrayList<String>();
                if (airPowerRange[1] > 0) {
                    toastList.add(airPowerValue);
                }
                toastList.add(seekValue);
                if (tp[0] > 0) {
                    toastList.add(tpValue);
                }

                fchk_info.setText(joinStr(toastList, " / "));
            } else {
                fchk_info.setText("data not loaded");
            }
        }
    }

    private int getSeekCn() {
        return Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    private String getSeekType() {
        int cn = Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
        String seekType = "";
        switch (cn) {
            case 1:
                seekType = getStringWithLocale(R.string.seek_type_1);
                break;
            case 3:
                seekType = getStringWithLocale(R.string.seek_type_3);
                break;
            case 4:
                seekType = getStringWithLocale(R.string.seek_type_4);
                break;
            default:
                seekType = getStringWithLocale(R.string.seek_type_0);
                break;
        }
        return seekType;
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
                    mViewX = mParams.x;
                    mViewY = mParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", mViewX, mViewY));
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.e("KCA", "Callback Canceled");
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == R.id.view_fchk_head) stopPopup();
                        else if (id == R.id.fchk_prev) decreaseNo();
                        else if (id == R.id.fchk_next) increaseNo();
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