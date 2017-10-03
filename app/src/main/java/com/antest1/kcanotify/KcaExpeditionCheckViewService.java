package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaExpeditionCheckViewService extends Service {
    public static final String SHOW_EXCHECKVIEW_ACTION = "show_excheckview";

    public static boolean active;
    static boolean error_flag = false;
    Context contextWithLocale;
    int displayWidth = 0;
    public KcaDBHelper helper;
    private View mView, itemView;
    String locale;
    LayoutInflater mInflater;
    private WindowManager mManager;
    WindowManager.LayoutParams mParams;
    int selected = 1;

    private void showInfoView(MotionEvent paramMotionEvent, int selected) {
        setItemViewLayout(KcaApiData.getExpeditionInfo(selected, locale));
        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams(-2, -2, KcaUtils.getWindowLayoutType(), 8, -3);
        localLayoutParams.x = ((int) (50.0F + paramMotionEvent.getRawX()));
        localLayoutParams.y = ((int) paramMotionEvent.getRawY());
        localLayoutParams.gravity = 51;
        if (itemView.getParent() != null) {
            mManager.removeViewImmediate(itemView);
        }
        mManager.addView(itemView, localLayoutParams);
    }

    private void updateSelectedView(int idx) {
        for (int i = 1; i < 4; i++) {
            int view_id = getId("fleet_".concat(String.valueOf(i + 1)), R.id.class);
            if (idx == i) {
                mView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            } else {
                mView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
            }
        }
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }

        try {
            active = true;
            locale = LocaleUtils.getLocaleCode(KcaUtils.getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
            helper = new KcaDBHelper(getApplicationContext(), null, 3);
            contextWithLocale = KcaUtils.getContextWithLocale(getApplicationContext(), getBaseContext());
            mInflater = LayoutInflater.from(contextWithLocale);
            mView = mInflater.inflate(R.layout.view_excheck_list, null);
            mView.setVisibility(View.GONE);
            mView.findViewById(R.id.excheckview_head).setOnTouchListener(mViewTouchListener);
            for (int i = 1; i < 4; i++) {
                mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
            }

            for (int i = 0; i < 40; i++) {
                mView.findViewById(KcaUtils.getId("expedition_btn_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
            }

            if (KcaApiData.isEventTime) {
                mView.findViewById(R.id.expedition_btn_133).setOnTouchListener(mViewTouchListener);
                mView.findViewById(R.id.expedition_btn_134).setOnTouchListener(mViewTouchListener);
                mView.findViewById(R.id.excheck_row_e).setVisibility(View.VISIBLE);
            } else {
                mView.findViewById(R.id.excheck_row_e).setVisibility(View.GONE);
            }

            itemView = mInflater.inflate(R.layout.view_excheck_detail, null);
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            mParams.gravity = Gravity.CENTER;

            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            displayWidth = size.x;
        } catch (Exception e) {
            active = false;
            error_flag = true;
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        active = false;
        if (mView != null) {
            if (mView.getParent() != null) mManager.removeViewImmediate(mView);
        }
        if (itemView != null) {
            if (itemView.getParent() != null) mManager.removeViewImmediate(itemView);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().startsWith(SHOW_EXCHECKVIEW_ACTION)) {
                selected = Integer.parseInt(intent.getAction().split("/")[1]);
                if (selected < 1) selected = 1;
                else if (selected > 3) selected = 2;
                int setViewResult = setView();
                if (setViewResult == 0) {
                    if (mView.getParent() != null) {
                        mManager.removeViewImmediate(mView);
                    }
                    mManager.addView(mView, mParams);
                }
                Log.e("KCA", "show_excheckview_action " + String.valueOf(setViewResult));
                mView.setVisibility(View.VISIBLE);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void setItemViewLayout(JsonObject data) {
        ((TextView) itemView.findViewById(R.id.view_excheck_title)).setText(data.toString());
        itemView.setVisibility(View.VISIBLE);
    }

    public int setView() {
        try {
            updateSelectedView(selected);
            return 0;
        } catch (Exception E) {
            return 1;
        }
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e("KCA-FV", "ACTION_DOWN");
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    for (int i = 0; i < 40; i++) {
                        if (id == mView.findViewById(getId("expedition_btn_".concat(String.valueOf(i + 1)), R.id.class)).getId()) {
                            setItemViewLayout(KcaApiData.getExpeditionInfo(i+1, locale));
                            showInfoView(event, i+1);
                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("KCA-FV", "ACTION_UP");
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    itemView.setVisibility(View.GONE);

                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == mView.findViewById(R.id.excheckview_head).getId()) {
                            stopSelf();
                        } else {
                            for (int i = 1; i < 4; i++) {
                                if (id == mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).getId()) {
                                    selected = i;
                                    setView();
                                    break;
                                }
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    };
}
