package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
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
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLENODE;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_FAIRYLOC;
import static com.antest1.kcanotify.KcaConstants.GOTO_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_FAIRY_CHECKED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_FAIRY_HIDDEN;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_DATA;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_COMPLETE;
import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_AUTOHIDE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_NOTI_LONGCLICK;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_REV;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_BATTLEVIEW_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_QUEST_FAIRY_GLOW;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_FLAG;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_PREFIX;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getOrientationPrefix;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaViewButtonService extends Service {
    public static final int FAIRY_NOTIFICATION_ID = 10118;
    public static final int FAIRY_GLOW_INTERVAL = 800;
    public static final int FOREGROUND_CHECK_INTERVAL = 500;

    public static final String KCA_STATUS_ON = "kca_status_on";
    public static final String KCA_STATUS_OFF = "kca_status_off";
    public static final String FAIRY_VISIBLE = "fairy_visible";
    public static final String FAIRY_INVISIBLE = "fairy_invisible";
    public static final String FAIRY_CHANGE = "fairy_change";
    public static final String FAIRY_FORECHECK_ON = "fairy_forecheck_on";
    public static final String FAIRY_FORECHECK_OFF = "fairy_forecheck_off";
    public static final String RETURN_FAIRY_ACTION = "return_fairy_action";
    public static final String RESET_FAIRY_STATUS_ACTION = "reset_fairy_status_action";
    public static final String REMOVE_FAIRY_ACTION = "remove_fairy_action";
    public static final String SHOW_BATTLE_INFO = "show_battle_info";
    public static final String SHOW_QUEST_INFO = "show_quest_info";
    public static final String ACTIVATE_BATTLEVIEW_ACTION = "activate_battleview";
    public static final String DEACTIVATE_BATTLEVIEW_ACTION = "deactivate_battleview";
    public static final String ACTIVATE_QUESTVIEW_ACTION = "activate_questview";
    public static final String DEACTIVATE_QUESTVIEW_ACTION = "deactivate_questview";

    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver battleinfo_receiver;
    private BroadcastReceiver battlehdmg_receiver;
    private BroadcastReceiver battlenode_receiver;
    private BroadcastReceiver questcmpl_receiver;
    private View mView;
    private WindowManager mManager;
    private Handler mHandler;
    private Vibrator vibrator;
    private ImageView viewbutton;
    private int screenWidth, screenHeight;
    private int buttonWidth, buttonHeight;
    private KcaDBHelper dbHelper;
    private JsonArray icon_info;
    private boolean battleviewEnabled = false;
    private boolean questviewEnabled = false;
    public String viewBitmapId;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;
    public static JsonObject currentApiData;
    public static int recentVisibility = View.VISIBLE;
    public static boolean hiddenByUser = false;
    public static int type;
    public static int clickcount;
    public static Handler sHandler;
    public boolean taiha_status = false;
    private boolean fairy_glow_on = false;
    private boolean fairy_glow_mode = false;
    ScheduledExecutorService checkForegroundScheduler;
    private boolean is_kc_foreground = true;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static JsonObject getCurrentApiData() {
        return currentApiData;
    }

    public static int getClickCount() {
        return clickcount;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    public static int getRecentVisibility() {
        return recentVisibility;
    }

    public void runForegroundCheck() {
        checkForegroundScheduler = Executors.newSingleThreadScheduledExecutor();
        checkForegroundScheduler.scheduleAtFixedRate(mForegroundCheckRunnable, 0,
                FOREGROUND_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stopForegroundCheck() {
        if (checkForegroundScheduler != null) {
            checkForegroundScheduler.shutdown();
        }
        is_kc_foreground = true;
        Intent intent = new Intent(getApplicationContext(), KcaViewButtonService.class);
        intent.setAction(KcaViewButtonService.RETURN_FAIRY_ACTION);
        startService(intent);
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
        } else if (!KcaService.getServiceStatus()) {
            stopSelf();
        } else {
            clickcount = 0;
            mHandler = new Handler();
            broadcaster = LocalBroadcastManager.getInstance(this);
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            battleinfo_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //String s = intent.getStringExtra(KCA_MSG_DATA);
                    String s = dbHelper.getValue(DB_KEY_BATTLEINFO);
                    broadcaster.sendBroadcast(new Intent(KCA_MSG_BATTLE_VIEW_REFRESH));
                    Log.e("KCA", "KCA_MSG_BATTLE_INFO Received: \n".concat(s));
                }
            };
            battlenode_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //String s = intent.getStringExtra(KCA_MSG_DATA);
                    String s = dbHelper.getValue(DB_KEY_BATTLENODE);
                    broadcaster.sendBroadcast(new Intent(KCA_MSG_BATTLE_VIEW_REFRESH));
                    Log.e("KCA", "KCA_MSG_BATTLE_NODE Received: \n".concat(s));
                }
            };
            battlehdmg_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    if (s.contains("1")) {
                        taiha_status = true;
                    } else {
                        taiha_status = false;
                    }
                    setFairyImage();
                    Log.e("KCA", "KCA_MSG_BATTLE_HDMG Received");
                }
            };
            questcmpl_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    if (s.contains("1")) {
                        if (!fairy_glow_mode) {
                            startFairyKira();
                        }
                    } else {
                        if (fairy_glow_mode) {
                            stopFairyKira();
                        }
                    }
                    Log.e("KCA", "KCA_MSG_QUEST_COMPLETE Received");
                }
            };

            LocalBroadcastManager.getInstance(this).registerReceiver((battleinfo_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));
            LocalBroadcastManager.getInstance(this).registerReceiver((battlenode_receiver), new IntentFilter(KCA_MSG_BATTLE_NODE));
            LocalBroadcastManager.getInstance(this).registerReceiver((battlehdmg_receiver), new IntentFilter(KCA_MSG_BATTLE_HDMG));
            LocalBroadcastManager.getInstance(this).registerReceiver((questcmpl_receiver), new IntentFilter(KCA_MSG_QUEST_COMPLETE));
            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mView = mInflater.inflate(R.layout.view_button, null);

            // Button (Fairy) Settings
            icon_info = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "icon_info.json", dbHelper);
            viewbutton = mView.findViewById(R.id.viewbutton);
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            viewBitmapId = "noti_icon_".concat(fairyIdValue);
            setFairyImage();
            if (icon_info.size() > 0) {
                int fairy_id = Integer.parseInt(fairyIdValue);
                int rev_internal = 0;
                if (fairy_id < icon_info.size()) {
                    JsonObject fairy_info = icon_info.get(fairy_id).getAsJsonObject();
                    rev_internal = fairy_info.has("rev") ? fairy_info.get("rev").getAsInt() : 0;
                }
                int rev_setting = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_REV));
                if ((rev_internal + rev_setting) % 2 == 1) {
                    viewbutton.setScaleX(-1.0f);
                } else {
                    viewbutton.setScaleX(1.0f);
                }
            }

            viewbutton.setOnTouchListener(mViewTouchListener);
            viewbutton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            buttonWidth = viewbutton.getMeasuredWidth();
            buttonHeight = viewbutton.getMeasuredHeight();

            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            mParams.gravity = Gravity.TOP | Gravity.START;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

            JsonObject locdata = null;
            String ori_prefix = getOrientationPrefix(getResources().getConfiguration().orientation);
            if (dbHelper != null) locdata = dbHelper.getJsonObjectValue(DB_KEY_FAIRYLOC);
            if (locdata != null && locdata.toString().length() > 0) {
                if (locdata.has(ori_prefix.concat("x"))) {
                    mParams.x = locdata.get(ori_prefix.concat("x")).getAsInt();
                }
                if (locdata.has(ori_prefix.concat("y"))) {
                    mParams.y = locdata.get(ori_prefix.concat("y")).getAsInt();
                }
            } else {
                mParams.y = screenHeight - buttonHeight / 2;
            }

            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            battleviewEnabled = false;
            questviewEnabled = false;

            if (getBooleanPreferences(getApplicationContext(), PREF_FAIRY_AUTOHIDE)) {
                runForegroundCheck();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (!KcaService.getServiceStatus()) {
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(KCA_STATUS_ON)) {
                Log.e("KCA", KCA_STATUS_ON);
                if (mView != null) mView.setVisibility(recentVisibility);
            }
            if (intent.getAction().equals(KCA_STATUS_OFF)) {
                Log.e("KCA", KCA_STATUS_OFF);
                if (mView != null) mView.setVisibility(View.GONE);
            }
            if (intent.getAction().equals(FAIRY_VISIBLE) || intent.getAction().equals(RETURN_FAIRY_ACTION)) {
                if (mView != null) {
                    mView.setVisibility(View.VISIBLE);
                    recentVisibility = View.VISIBLE;
                }
            }
            if (intent.getAction().equals(RETURN_FAIRY_ACTION) || intent.getAction().equals(REMOVE_FAIRY_ACTION)) {
                if (sHandler != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_FAIRY_CHECKED);
                    bundle.putString("data", "");
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                    hiddenByUser = false;
                }
            }
            if (intent.getAction().equals(FAIRY_INVISIBLE)) {
                if (mView != null) {
                    mView.setVisibility(View.GONE);
                    recentVisibility = View.GONE;
                }
            }
            if (intent.getAction().equals(FAIRY_FORECHECK_ON)) {
                runForegroundCheck();
            }
            if (intent.getAction().equals(FAIRY_FORECHECK_OFF)) {
                stopForegroundCheck();
            }
            if (intent.getAction().equals(FAIRY_CHANGE)) {
                String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
                viewBitmapId = "noti_icon_".concat(fairyIdValue);
                setFairyImage();
                if (icon_info.size() > 0) {
                    int fairy_id = Integer.parseInt(fairyIdValue);
                    int rev_internal = 0;
                    if (fairy_id < icon_info.size()) {
                        JsonObject fairy_info = icon_info.get(fairy_id).getAsJsonObject();
                        rev_internal = fairy_info.has("rev") ? fairy_info.get("rev").getAsInt() : 0;
                    }
                    int rev_setting = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_REV));
                    if ((rev_internal + rev_setting) % 2 == 1) {
                        viewbutton.setScaleX(-1.0f);
                    } else {
                        viewbutton.setScaleX(1.0f);
                    }
                }
            }
            if (intent.getAction().equals(RESET_FAIRY_STATUS_ACTION)) {
                taiha_status = false;
                setFairyImage();
            }
            if (intent.getAction().equals(ACTIVATE_BATTLEVIEW_ACTION)) {
                Intent qintent = new Intent(getBaseContext(), KcaFleetViewService.class);
                qintent.setAction(KcaFleetViewService.CLOSE_FLEETVIEW_ACTION);
                startService(qintent);
                battleviewEnabled = true;
            }
            if (intent.getAction().equals(DEACTIVATE_BATTLEVIEW_ACTION)) {
                taiha_status = false;
                battleviewEnabled = false;
            }
            if (intent.getAction().equals(ACTIVATE_QUESTVIEW_ACTION)) {
                Intent qintent = new Intent(getBaseContext(), KcaFleetViewService.class);
                qintent.setAction(KcaFleetViewService.CLOSE_FLEETVIEW_ACTION);
                startService(qintent);
                questviewEnabled = true;
            }
            if (intent.getAction().equals(DEACTIVATE_QUESTVIEW_ACTION)) {
                questviewEnabled = false;
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean isBattleViewEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE);
    }

    private final int margin = 14;
    private final int halfMargin = margin / 2;
    private final int glowRadius = 20;
    private final int glowColor = Color.rgb(0, 192, 255);
    private final int glowColor2 = Color.rgb(230, 249, 255);

    private void setFairyImage() {
        boolean glow_available = fairy_glow_on && getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_QUEST_FAIRY_GLOW);
        Bitmap src = KcaUtils.getFairyImageFromStorage(getApplicationContext(), viewBitmapId, dbHelper);
        Bitmap alpha = src.extractAlpha();
        Bitmap bmp = Bitmap.createBitmap(src.getWidth() + margin,
                src.getHeight() + margin, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        if (glow_available) {
            Paint glow_paint = new Paint();
            glow_paint.setColor(glowColor);
            glow_paint.setMaskFilter(new BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER));
            canvas.drawBitmap(alpha, halfMargin, halfMargin, glow_paint);
        }
        Paint color_paint = new Paint();
        if (taiha_status) {
            color_paint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getApplicationContext(),
                    R.color.colorHeavyDmgStateWarn), PorterDuff.Mode.MULTIPLY));
        } else if (glow_available) {
            color_paint.setColorFilter(new PorterDuffColorFilter(glowColor2, PorterDuff.Mode.MULTIPLY));
        }
        canvas.drawBitmap(src, halfMargin, halfMargin, color_paint);
        viewbutton.setImageBitmap(bmp);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(questcmpl_receiver);
        if (mManager != null) mManager.removeView(mView);
        super.onDestroy();
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
            if (id == viewbutton.getId()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchX = event.getRawX();
                        mTouchY = event.getRawY();
                        mViewX = mParams.x;
                        mViewY = mParams.y;
                        Log.e("KCA", KcaUtils.format("mView: %d %d", mViewX, mViewY));
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        mHandler.postDelayed(mRunnable, LONG_CLICK_DURATION);
                        break;

                    case MotionEvent.ACTION_UP:
                        Log.e("KCA", "Callback Canceled");
                        mHandler.removeCallbacks(mRunnable);
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            clickcount += 1;
                            if (battleviewEnabled && isBattleViewEnabled()) {
                                Intent qintent = new Intent(getBaseContext(), KcaBattleViewService.class);
                                qintent.setAction(KcaBattleViewService.SHOW_BATTLEVIEW_ACTION);
                                startService(qintent);
                            } else if (questviewEnabled) {
                                Intent qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                                qintent.setAction(KcaQuestViewService.SHOW_QUESTVIEW_ACTION_NEW);
                                startService(qintent);
                            } else {
                                Intent qintent = new Intent(getBaseContext(), KcaFleetViewService.class);
                                qintent.setAction(KcaFleetViewService.SHOW_FLEETVIEW_ACTION);
                                startService(qintent);
                            }
                        }

                        int[] locations = new int[2];
                        mView.getLocationOnScreen(locations);
                        int xx = locations[0];
                        int yy = locations[1];
                        Log.e("KCA", KcaUtils.format("Coord: %d %d", xx, yy));
                        if (mParams.x < 0) mParams.x = 0;
                        else if (mParams.x > screenWidth - buttonWidth / 2)
                            mParams.x = screenWidth - buttonWidth / 2;
                        if (mParams.y < 0) mParams.y = 0;
                        else if (mParams.y > screenHeight - buttonHeight / 2)
                            mParams.y = screenHeight - buttonHeight / 2;

                        JsonObject locdata = dbHelper.getJsonObjectValue(DB_KEY_FAIRYLOC);
                        String ori_prefix = getOrientationPrefix(getResources().getConfiguration().orientation);
                        if (locdata != null && locdata.toString().length() > 0) {
                            locdata.addProperty(ori_prefix.concat("x"), mParams.x);
                            locdata.addProperty(ori_prefix.concat("y"), mParams.y);
                        } else {
                            locdata = new JsonObject();
                        }
                        dbHelper.putValue(DB_KEY_FAIRYLOC, locdata.toString());
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int x = (int) (event.getRawX() - mTouchX);
                        int y = (int) (event.getRawY() - mTouchY);

                        mParams.x = mViewX + x;
                        mParams.y = mViewY + y;
                        if (mParams.x < 0) mParams.x = 0;
                        else if (mParams.x > screenWidth - buttonWidth / 2) mParams.x = screenWidth - buttonWidth / 2;
                        if (mParams.y < 0) mParams.y = 0;
                        else if (mParams.y > screenHeight - buttonHeight / 2) mParams.y = screenHeight - buttonHeight / 2;
                        mManager.updateViewLayout(mView, mParams);
                        if (Math.abs(x) > 20 || Math.abs(y) > 20) {
                            Log.e("KCA", "Callback Canceled");
                            mHandler.removeCallbacks(mRunnable);
                        }
                        break;
                }
            }
            return true;
        }
    };


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (getBooleanPreferences(getApplicationContext(), PREF_FAIRY_NOTI_LONGCLICK)) {
                doVibrate(vibrator, 100);
            }
            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.viewbutton_hide), Toast.LENGTH_LONG).show();
            mView.setVisibility(View.GONE);
            recentVisibility = View.GONE;
            hiddenByUser = true;
            if (sHandler != null) {
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_FAIRY_HIDDEN);
                bundle.putString("data", "");
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

        }
    };

    private Runnable mForegroundCheckRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                boolean current_foreground_status = false;
                String foregroundPackage = checkForegroundPackage();
                if (foregroundPackage.trim().length() > 0) {
                    if (foregroundPackage.contains(KC_PACKAGE_NAME)) {
                        current_foreground_status = true;
                    } else if (foregroundPackage.contains(GOTO_PACKAGE_NAME)) {
                        current_foreground_status = true;
                    }
                } else {
                    current_foreground_status = is_kc_foreground;
                }

                if (current_foreground_status != is_kc_foreground) {
                    is_kc_foreground = current_foreground_status;
                    Intent intent = new Intent(getApplicationContext(), KcaViewButtonService.class);
                    if (is_kc_foreground) {
                        intent.setAction(KcaViewButtonService.KCA_STATUS_ON);
                        Log.e("KCA-VB", "kancolle detected: " + foregroundPackage);
                    } else {
                        intent.setAction(KcaViewButtonService.KCA_STATUS_OFF);
                        Log.e("KCA-VB", "kancolle not detected: " + foregroundPackage);
                    }
                    startService(intent);
                }
            } catch (Exception e) {
                Log.e("KCA-VB", getStringFromException(e));
            }
        }
    };

    private Runnable mGlowRunner = new Runnable() {
        @Override
        public void run() {
            try {
                fairy_glow_on = !fairy_glow_on;
            } finally {
                if (fairy_glow_mode) {
                    mHandler.postDelayed(mGlowRunner, FAIRY_GLOW_INTERVAL);
                } else {
                    fairy_glow_on = false;
                }
                setFairyImage();
            }
        }
    };

    void startFairyKira() {
        fairy_glow_mode = true;
        mGlowRunner.run();
    }

    void stopFairyKira() {
        fairy_glow_mode = false;
        setFairyImage();
    }

    @SuppressLint("WrongConstant")
    public String checkForegroundPackage() {
        String classByUsageStats = null;
        String packageNameByUsageStats = null;
        String recentPackageName = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager mUsageStatsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            } else {
                //noinspection ResourceType
                mUsageStatsManager = (UsageStatsManager) getSystemService("usagestats");
            }
            final long INTERVAL = 5000;
            final long end = System.currentTimeMillis();
            final long begin = end - INTERVAL;
            final UsageEvents usageEvents = mUsageStatsManager.queryEvents(begin, end);

            while (usageEvents.hasNextEvent()) {
                UsageEvents.Event event = new UsageEvents.Event();
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    packageNameByUsageStats = event.getPackageName();
                    Date d = new Date(event.getTimeStamp());
                    classByUsageStats = event.getClassName() + " " + d.toString();
                    recentPackageName = classByUsageStats;
                }
            }
        } else {
            recentPackageName = "not_kancolle_process";
            ActivityManager activityManager = (ActivityManager) getSystemService( Context.ACTIVITY_SERVICE );
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            for(ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                    if (appProcess.processName.contains(KC_PACKAGE_NAME)) {
                        recentPackageName = KC_PACKAGE_NAME;
                    } else if (appProcess.processName.contains(GOTO_PACKAGE_NAME)) {
                        recentPackageName = GOTO_PACKAGE_NAME;
                    }
                }
            }
        }
        return recentPackageName;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        String ori_prefix = getOrientationPrefix(newConfig.orientation);
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

        JsonObject locdata = null;
        if (dbHelper != null) {
            locdata = dbHelper.getJsonObjectValue(DB_KEY_FAIRYLOC);

            if (locdata != null && locdata.toString().length() > 0) {
                if (locdata.has(ori_prefix.concat("x"))) {
                    mParams.x = locdata.get(ori_prefix.concat("x")).getAsInt();
                }
                if (locdata.has(ori_prefix.concat("y"))) {
                    mParams.y = locdata.get(ori_prefix.concat("y")).getAsInt();
                }
            }

            if (mManager != null && mParams != null) {
                if (mParams.x < 0) mParams.x = 0;
                else if (mParams.x > screenWidth - buttonWidth / 2) mParams.x = screenWidth - buttonWidth / 2;
                if (mParams.y < 0) mParams.y = 0;
                else if (mParams.y > screenHeight - buttonHeight / 2) mParams.y = screenHeight - buttonHeight / 2;
                mManager.updateViewLayout(mView, mParams);
            }

            if (locdata != null && locdata.toString().length() > 0) {
                locdata.addProperty(ori_prefix.concat("x"), mParams.x);
                locdata.addProperty(ori_prefix.concat("y"), mParams.y);
            } else {
                locdata = new JsonObject();
            }
            dbHelper.putValue(DB_KEY_FAIRYLOC, locdata.toString());
        }
        super.onConfigurationChanged(newConfig);
    }
}