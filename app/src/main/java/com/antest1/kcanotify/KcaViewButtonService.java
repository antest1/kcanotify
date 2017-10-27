package com.antest1.kcanotify;

import android.app.Service;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
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

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLENODE;
import static com.antest1.kcanotify.KcaConstants.FAIRY_REVERSE_LIST;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_DATA;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_COMPLETE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_NOTI_LONGCLICK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_BATTLEVIEW_USE;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaViewButtonService extends Service {
    public static final int FAIRY_NOTIFICATION_ID = 10118;
    public static final int FAIRY_GLOW_INTERVAL = 800;

    public static final String KCA_STATUS_ON = "kca_status_on";
    public static final String KCA_STATUS_OFF = "kca_status_off";
    public static final String FAIRY_VISIBLE = "fairy_visible";
    public static final String FAIRY_INVISIBLE = "fairy_invisible";
    public static final String FAIRY_CHANGE = "fairy_change";
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
    private boolean battleviewEnabled = false;
    private boolean questviewEnabled = false;
    public int viewBitmapId = 0;
    public int viewBitmapSmallId = 0;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;
    public static JsonObject currentApiData;
    public static int recentVisibility = View.VISIBLE;
    public static int type;
    public static int clickcount;
    public static boolean taiha_status = false;
    private static boolean fairy_glow_on = false;
    private static boolean fairy_glow_mode = false;

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
                        ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(),
                                R.color.colorHeavyDmgStateWarn), PorterDuff.Mode.MULTIPLY);
                    } else {
                        ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().clearColorFilter();
                    }
                    Log.e("KCA", "KCA_MSG_BATTLE_HDMG Received");
                }
            };
            questcmpl_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    if (s.contains("1")) {
                        if (!fairy_glow_mode) {
                            fairy_glow_on = true;
                            startFairyKira();
                        }
                        fairy_glow_mode = true;
                    } else {
                        if (fairy_glow_mode) {
                            fairy_glow_on = false;
                            stopFairyKira();
                        }
                        fairy_glow_mode = false;
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
            viewbutton = mView.findViewById(R.id.viewbutton);
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            String fairyPath = "noti_icon_".concat(fairyIdValue);
            viewBitmapId = getId(fairyPath, R.mipmap.class);
            viewBitmapSmallId = getId(fairyPath.concat("_small"), R.mipmap.class);
            setFairyGlow();

            int index = Arrays.binarySearch(FAIRY_REVERSE_LIST, Integer.parseInt(fairyIdValue));
            if (index >= 0) viewbutton.setScaleX(-1.0f);
            else viewbutton.setScaleX(1.0f);

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
            Log.e("KCA", "w/h: "+String.valueOf(screenWidth) + " "  +String.valueOf(screenHeight));

            mParams.y = screenHeight - buttonHeight;
            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            battleviewEnabled = false;
            questviewEnabled = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
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
                if(mView != null) {
                    mView.setVisibility(View.VISIBLE);
                    recentVisibility = View.VISIBLE;
                }
            }
            if (intent.getAction().equals(FAIRY_INVISIBLE)) {
                if(mView != null) {
                    mView.setVisibility(View.GONE);
                    recentVisibility = View.GONE;
                }
            }
            if (intent.getAction().equals(FAIRY_CHANGE)) {
                String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
                String fairyPath = "noti_icon_".concat(fairyIdValue);
                viewBitmapId = getId(fairyPath, R.mipmap.class);
                viewBitmapSmallId = getId(fairyPath.concat("_small"), R.mipmap.class);
                setFairyGlow();
                int index = Arrays.binarySearch(FAIRY_REVERSE_LIST, Integer.parseInt(fairyIdValue));
                if (index >= 0) viewbutton.setScaleX(-1.0f);
                else viewbutton.setScaleX(1.0f);
            }
            if (intent.getAction().equals(RESET_FAIRY_STATUS_ACTION)) {
                ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().clearColorFilter();
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

    private void setFairyGlow() {
        int margin = 14;
        int halfMargin = margin / 2;
        int glowRadius = 20;
        int glowColor = Color.rgb(0, 192, 255);
        int glowColor2 = Color.rgb(230, 249, 255);

        Bitmap src = BitmapFactory.decodeResource(getResources(), viewBitmapId);
        Bitmap alpha = src.extractAlpha();
        Bitmap bmp = Bitmap.createBitmap(src.getWidth() + margin,
                src.getHeight() + margin, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        if (fairy_glow_on) {
            Paint paint = new Paint();
            paint.setColor(glowColor);
            paint.setMaskFilter(new BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER));
            canvas.drawBitmap(alpha, halfMargin, halfMargin, paint);
        }
        canvas.drawBitmap(src, halfMargin, halfMargin, null);
        viewbutton.setImageBitmap(bmp);
        if(!taiha_status && fairy_glow_on) {
            ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().setColorFilter(glowColor2, PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(questcmpl_receiver);
        if(mManager != null) mManager.removeView(mView);
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
                        Log.e("KCA", String.format("mView: %d %d", mViewX, mViewY));
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
                        Log.e("KCA", String.format("Coord: %d %d", xx, yy));
                        if (mParams.x < 0) mParams.x = 0;
                        else if (mParams.x > screenWidth - buttonWidth) mParams.x = screenWidth - buttonWidth;
                        if (mParams.y < 0) mParams.y = 0;
                        else if (mParams.y > screenHeight - buttonHeight) mParams.y = screenHeight - buttonHeight;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int x = (int) (event.getRawX() - mTouchX);
                        int y = (int) (event.getRawY() - mTouchY);

                        mParams.x = mViewX + x;
                        mParams.y = mViewY + y;
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
        }
    };

    private Runnable mGlowRunner = new Runnable() {
        @Override
        public void run() {
            try {
                fairy_glow_on = !fairy_glow_on;
                setFairyGlow();
            } finally {
                mHandler.postDelayed(mGlowRunner, FAIRY_GLOW_INTERVAL);
            }
        }
    };

    void startFairyKira() {
        mGlowRunner.run();
    }

    void stopFairyKira() {
        fairy_glow_on = false;
        setFairyGlow();
        mHandler.removeCallbacks(mGlowRunner);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: "+String.valueOf(screenWidth) + " "  +String.valueOf(screenHeight));

        int totalWidth = buttonWidth;
        int totalHeight = buttonHeight;

        if (mParams != null) {
            if (mParams.x < 0) mParams.x = 0;
            else if (mParams.x > screenWidth - totalWidth) mParams.x = screenWidth - totalWidth;
            if (mParams.y < 0) mParams.y = 0;
            else if (mParams.y > screenHeight - totalHeight) mParams.y = screenHeight - totalHeight;
        }

        super.onConfigurationChanged(newConfig);
    }
}