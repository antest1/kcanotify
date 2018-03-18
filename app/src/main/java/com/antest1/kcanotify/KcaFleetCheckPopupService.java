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
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.R.attr.id;
import static com.antest1.kcanotify.KcaApiData.checkUserShipDataLoaded;
import static com.antest1.kcanotify.KcaApiData.getCurrentNodeAlphabet;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_APIMAPINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.SEEK_PURE;
import static com.antest1.kcanotify.KcaMapHpPopupService.MAPHP_RESET_ACTION;
import static com.antest1.kcanotify.KcaMapHpPopupService.MAPHP_SHOW_ACTION;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaFleetCheckPopupService extends Service {
    public static final String FCHK_SHOW_ACTION = "fchk_show_action";
    public static final String FCHK_RESET_ACTION = "fchk_reset_action";

    private static final int FCHK_FUNC_SEEKTP = 0;
    private static final int FCHK_FUNC_AIRBATTLE = 1;
    private static final int FCHK_FUNC_FUELBULL = 2;

    private static final int[] FCHK_FLEET_LIST = {
        R.id.fleet_1, R.id.fleet_2, R.id.fleet_3, R.id.fleet_4, R.id.fleet_5
    };

    private static final int[] FCHK_BTN_LIST = {
        R.id.fchk_btn_seektp, R.id.fchk_btn_airbattle, R.id.fchk_btn_fuelbull
    };

    Context contextWithLocale;
    private View mView;
    LayoutInflater mInflater;
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
    public static int recent_no = 0;
    public static int current_func = FCHK_FUNC_SEEKTP;
    public static int deck_cnt = 1;

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
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());

            contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());

            mInflater = LayoutInflater.from(contextWithLocale);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mView = mInflater.inflate(R.layout.view_fleet_check, null);
            mView.setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.view_fchk_head).setOnTouchListener(mViewTouchListener);
            ((TextView) mView.findViewById(R.id.view_fchk_title)).setText(getStringWithLocale(R.string.fleetcheckview_title));

            for (int fchk_id: FCHK_BTN_LIST) {
                mView.findViewById(fchk_id).setOnTouchListener(mViewTouchListener);
            }
            for (int fleet_id: FCHK_FLEET_LIST) {
                mView.findViewById(fleet_id).setOnTouchListener(mViewTouchListener);
            }

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
            if (intent.getAction().equals(FCHK_SHOW_ACTION)) {
                portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                if (portdeckdata != null) {
                    deck_cnt = portdeckdata.size();
                    setFchkFleetBtnColor(recent_no, deck_cnt);
                    setFchkFuncBtnColor(current_func);
                    setText();
                }

                mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                popupWidth = mView.getMeasuredWidth();
                popupHeight = mView.getMeasuredHeight();

                mParams.x = (screenWidth - popupWidth) / 2;
                mParams.y = (screenHeight - popupHeight) / 2;
                mManager.updateViewLayout(mView, mParams);
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
            int target = recent_no;
            String target_str;
            if (recent_no == 4) {
                target = 0;
                target_str = "0,1";
            } else {
                target_str = String.valueOf(target);
            }

            if (KcaApiData.isGameDataLoaded() && KcaApiData.checkUserShipDataLoaded() && portdeckdata != null) {
                int cn = getSeekCn();
                String seekType = getSeekType();

                switch (current_func) {
                    case FCHK_FUNC_SEEKTP:
                        int seekValue_0 = (int) deckInfoCalc.getSeekValue(portdeckdata, target_str, SEEK_PURE, KcaBattle.getEscapeFlag());
                        double seekValue_1 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 1, KcaBattle.getEscapeFlag());
                        double seekValue_3 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 3, KcaBattle.getEscapeFlag());
                        double seekValue_4 = deckInfoCalc.getSeekValue(portdeckdata, target_str, 4, KcaBattle.getEscapeFlag());

                        int[] tp = deckInfoCalc.getTPValue(portdeckdata, target_str, KcaBattle.getEscapeFlag());
                        fchk_info.setText(KcaUtils.format(getStringWithLocale(R.string.fleetcheckview_content_seeklos),
                                seekValue_0, seekValue_1, seekValue_3, seekValue_4, tp[0], tp[1]));
                        break;
                    case FCHK_FUNC_AIRBATTLE:
                        int[] airPowerRange = deckInfoCalc.getAirPowerRange(portdeckdata, target, KcaBattle.getEscapeFlag());
                        JsonObject contact = deckInfoCalc.getContactProb(portdeckdata, target_str, KcaBattle.getEscapeFlag());
                        double start_rate_1 = contact.getAsJsonArray("stage1").get(0).getAsDouble() * 100;
                        double select_rate_1 = contact.getAsJsonArray("stage2").get(0).getAsDouble() * 100;
                        double start_rate_2 = contact.getAsJsonArray("stage1").get(1).getAsDouble() * 100;
                        double select_rate_2 = contact.getAsJsonArray("stage2").get(1).getAsDouble() * 100;
                        fchk_info.setText(KcaUtils.format(getStringWithLocale(R.string.fleetcheckview_content_airbattle),
                                airPowerRange[0], airPowerRange[1], start_rate_1, select_rate_1, start_rate_2, select_rate_2));
                        break;
                    case FCHK_FUNC_FUELBULL:
                        fchk_info.setText("fuel_bull");
                        break;
                    default:
                        fchk_info.setText("");
                        break;
                }
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
                        for (int i = 0; i < FCHK_FLEET_LIST.length; i++) {
                            if ((id == FCHK_FLEET_LIST[i]) && (i < deck_cnt || i == 4)) {
                                recent_no = i;
                                setFchkFleetBtnColor(recent_no, deck_cnt);
                                setText();
                            }
                        }
                        for (int i = 0; i < FCHK_BTN_LIST.length; i++) {
                            if (id == FCHK_BTN_LIST[i]) {
                                current_func = i;
                                setFchkFuncBtnColor(current_func);
                                setText();
                            }
                        }
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

    private void setFchkFleetBtnColor(int n, int size) {
        for (int i = 0; i < FCHK_FLEET_LIST.length; i++) {
            int fleet_id = FCHK_FLEET_LIST[i];
            if (size < 4 && i >= size) {
                mView.findViewById(fleet_id).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                ((TextView) mView.findViewById(fleet_id)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            } else if (i == n) {
                mView.findViewById(fleet_id).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                ((TextView) mView.findViewById(fleet_id)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
            } else {
                mView.findViewById(fleet_id).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                ((TextView) mView.findViewById(fleet_id)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            }
        }
    }

    private void setFchkFuncBtnColor(int n) {
        for (int i = 0; i < FCHK_BTN_LIST.length; i++) {
            int fchk_id = FCHK_BTN_LIST[i];
            if (i == n) {
                mView.findViewById(fchk_id).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                ((TextView) mView.findViewById(fchk_id)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
            } else {
                mView.findViewById(fchk_id).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                ((TextView) mView.findViewById(fchk_id)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
        mInflater = LayoutInflater.from(contextWithLocale);
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