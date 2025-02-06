package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLENODE;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_FAIRYLOC;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_FAIRY_CHECKED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_FAIRY_HIDDEN;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_DATA;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_COMPLETE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_NOTI_LONGCLICK;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_OPACITY;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_RANDOM;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_REV;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_SIZE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_BATTLEVIEW_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_QUEST_FAIRY_GLOW;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getOrientationPrefix;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static java.lang.Math.abs;
import static java.lang.Math.max;

public class KcaViewButtonService extends BaseService {
    private static final String TAG = "KcaViewButtonService";
    private static final int FAIRY_GLOW_INTERVAL = 800;

    public static final String KCA_STATUS_ON = "kca_status_on";
    public static final String KCA_STATUS_OFF = "kca_status_off";
    public static final String FAIRY_VISIBLE = "fairy_visible";
    public static final String FAIRY_INVISIBLE = "fairy_invisible";
    public static final String FAIRY_CHANGE = "fairy_change";
    public static final String FAIRY_SIZE_CHANGE = "fairy_size_change";
    public static final String FAIRY_ALPHA_CHANGE = "fairy_alpha_change";
    public static final String RETURN_FAIRY_ACTION = "return_fairy_action";
    public static final String RESET_FAIRY_STATUS_ACTION = "reset_fairy_status_action";
    public static final String REMOVE_FAIRY_ACTION = "remove_fairy_action";
    public static final String ACTIVATE_BATTLEVIEW_ACTION = "activate_battleview";
    public static final String DEACTIVATE_BATTLEVIEW_ACTION = "deactivate_battleview";
    public static final String ACTIVATE_QUESTVIEW_ACTION = "activate_questview";
    public static final String DEACTIVATE_QUESTVIEW_ACTION = "deactivate_questview";

    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver battleinfo_receiver;
    private BroadcastReceiver battlehdmg_receiver;
    private BroadcastReceiver battlenode_receiver;
    private BroadcastReceiver questcmpl_receiver;
    private DraggableOverlayButtonLayout buttonView;
    private WindowManager windowManager;
    private Handler mHandler;
    private Vibrator vibrator;
    private ImageView button;
    private int screenWidth, screenHeight, screenPaddingLeft = 0, screenPaddingTop = 0;
    private int buttonWidth, buttonHeight;
    private KcaDBHelper dbHelper;
    private JsonArray icon_info;
    private boolean battleviewEnabled = false;
    private boolean questviewEnabled = false;
    public String viewBitmapId;
    WindowManager.LayoutParams layoutParams;
    NotificationManagerCompat notificationManager;
    public static int recentVisibility = View.VISIBLE;
    public static boolean hiddenByUser = false;
    public static int type;
    public static int clickcount;
    public static Handler sHandler;
    public boolean taiha_status = false;
    private boolean fairy_glow_on = false;
    private boolean fairy_glow_mode = false;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static int getClickCount() {
        return clickcount;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(getApplicationContext())) {
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
                    if (s == null) return;
                    taiha_status = s.contains("1");
                    setFairyImage();
                    Log.e("KCA", "KCA_MSG_BATTLE_HDMG Received");
                }
            };
            questcmpl_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    if (s == null) return;
                    if (s.contains("1")) {
                        if (!fairy_glow_mode) startFairyKira();
                    } else {
                        if (fairy_glow_mode) stopFairyKira();
                    }
                    Log.e("KCA", "KCA_MSG_QUEST_COMPLETE Received");
                }
            };

            LocalBroadcastManager.getInstance(this).registerReceiver((battleinfo_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));
            LocalBroadcastManager.getInstance(this).registerReceiver((battlenode_receiver), new IntentFilter(KCA_MSG_BATTLE_NODE));
            LocalBroadcastManager.getInstance(this).registerReceiver((battlehdmg_receiver), new IntentFilter(KCA_MSG_BATTLE_HDMG));
            LocalBroadcastManager.getInstance(this).registerReceiver((questcmpl_receiver), new IntentFilter(KCA_MSG_QUEST_COMPLETE));

            LayoutInflater mInflater = LayoutInflater.from(this);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            buttonView = (DraggableOverlayButtonLayout) mInflater.inflate(R.layout.view_button, null);

            // Button (Fairy) Settings
            icon_info = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "icon_info.json", dbHelper);
            button = buttonView.findViewById(R.id.viewbutton);
            setFairySize();
            setFairyAlpha();

            String fairyIdValue;
            boolean random_fairy = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_RANDOM);
            if (random_fairy) {
                int fairy_size = icon_info.size();
                int random_value = (int)(Math.random() * (fairy_size + 1));
                fairyIdValue = String.valueOf(random_value);
            } else {
                fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            }
            viewBitmapId = "noti_icon_".concat(fairyIdValue);
            setFairyImage();
            if (!icon_info.isEmpty()) {
                int fairy_id = Integer.parseInt(fairyIdValue);
                int rev_internal = 0;
                if (fairy_id < icon_info.size()) {
                    JsonObject fairy_info = icon_info.get(fairy_id).getAsJsonObject();
                    rev_internal = fairy_info.has("rev") ? fairy_info.get("rev").getAsInt() : 0;
                }
                int rev_setting = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_REV));
                if ((rev_internal + rev_setting) % 2 == 1) {
                    button.setScaleX(-1.0f);
                } else {
                    button.setScaleX(1.0f);
                }
            }

            button.setOnClickListener(clickListener);
            button.setOnLongClickListener(longClickListener);

            View bg = buttonView.findViewById(R.id.bg);
            bg.setOnTouchListener(backgroundOnTouchListener);
            button.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            buttonWidth = button.getMeasuredWidth();
            buttonHeight = button.getMeasuredHeight();

            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    getLayoutParamsFlags(),
                    PixelFormat.TRANSLUCENT);

            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            updateScreenSize();
            Log.e("KCA", "w/h: " + screenWidth + " " + screenHeight);

            JsonObject locdata = null;
            String ori_prefix = getOrientationPrefix(getResources().getConfiguration().orientation);
            if (dbHelper != null) locdata = dbHelper.getJsonObjectValue(DB_KEY_FAIRYLOC);
            if (locdata != null && !locdata.toString().isEmpty()) {
                if (locdata.has(ori_prefix.concat("x"))) {
                    try {
                        layoutParams.x = locdata.get(ori_prefix.concat("x")).getAsInt();
                    } catch (NumberFormatException e) {
                        layoutParams.x = 0;
                    }
                }
                if (locdata.has(ori_prefix.concat("y"))) {
                    try {
                        layoutParams.y = locdata.get(ori_prefix.concat("y")).getAsInt();
                    } catch (NumberFormatException e) {
                        layoutParams.y = 0;
                    }
                }
            } else {
                layoutParams.y = screenHeight - buttonHeight / 2;
            }

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.addView(buttonView, layoutParams);

            battleviewEnabled = false;
            questviewEnabled = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (!KcaService.getServiceStatus()) {
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(KCA_STATUS_ON)) {
                Log.e("KCA", KCA_STATUS_ON);
                if (buttonView != null) buttonView.setVisibility(recentVisibility);
            }
            if (intent.getAction().equals(KCA_STATUS_OFF)) {
                Log.e("KCA", KCA_STATUS_OFF);
                if (buttonView != null) buttonView.setVisibility(View.GONE);
            }
            if (intent.getAction().equals(FAIRY_VISIBLE) || intent.getAction().equals(RETURN_FAIRY_ACTION)) {
                if (buttonView != null) {
                    buttonView.setVisibility(View.VISIBLE);
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
                if (buttonView != null) {
                    buttonView.setVisibility(View.GONE);
                    recentVisibility = View.GONE;
                }
            }
            if (intent.getAction().equals(FAIRY_SIZE_CHANGE)) {
                setFairySize();
            }
            if (intent.getAction().equals(FAIRY_ALPHA_CHANGE)) {
                setFairyAlpha();
            }
            if (intent.getAction().equals(FAIRY_CHANGE)) {
                String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
                viewBitmapId = "noti_icon_".concat(fairyIdValue);
                setFairyImage();
                if (!icon_info.isEmpty()) {
                    int fairy_id = Integer.parseInt(fairyIdValue);
                    int rev_internal = 0;
                    if (fairy_id < icon_info.size()) {
                        JsonObject fairy_info = icon_info.get(fairy_id).getAsJsonObject();
                        rev_internal = fairy_info.has("rev") ? fairy_info.get("rev").getAsInt() : 0;
                    }
                    int rev_setting = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_REV));
                    if ((rev_internal + rev_setting) % 2 == 1) {
                        button.setScaleX(-1.0f);
                    } else {
                        button.setScaleX(1.0f);
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

    private void setFairySize() {
        int fairy_size_id = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_SIZE));
        int size_dp;
        switch (fairy_size_id) {
            case 1:
                size_dp = getResources().getDimensionPixelSize(R.dimen.button_size_xsmall);
                break;
            case 2:
                size_dp = getResources().getDimensionPixelSize(R.dimen.button_size_small);
                break;
            case 4:
                size_dp = getResources().getDimensionPixelSize(R.dimen.button_size_large);
                break;
            case 5:
                size_dp = getResources().getDimensionPixelSize(R.dimen.button_size_xlarge);
                break;
            default:
                size_dp = getResources().getDimensionPixelSize(R.dimen.button_size_normal);
                break;
        }

        if (button != null) {
            ViewGroup.LayoutParams params = button.getLayoutParams();
            params.width = size_dp;
            params.height = size_dp;
            button.setLayoutParams(params);
        }
    }

    private void setFairyAlpha() {
        if (button != null) {
            int fairy_opacity = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_OPACITY));
            float alpha = fairy_opacity / 100f;
            Log.d(TAG, "opacity: " + fairy_opacity);
            button.setAlpha(alpha);
        }
    }

    private final int glowColor = Color.rgb(0, 192, 255);
    private final int glowColor2 = Color.rgb(230, 249, 255);

    private void setFairyImage() {
        boolean glow_available = fairy_glow_on && getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_QUEST_FAIRY_GLOW);
        Bitmap src = KcaUtils.getFairyImageFromStorage(getApplicationContext(), viewBitmapId, dbHelper);
        Bitmap alpha = src.extractAlpha();
        int margin = 14;
        Bitmap bmp = Bitmap.createBitmap(src.getWidth() + margin,
                src.getHeight() + margin, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        int halfMargin = margin / 2;
        if (glow_available) {
            Paint glow_paint = new Paint();
            glow_paint.setColor(glowColor);
            int glowRadius = 20;
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
        button.setImageBitmap(bmp);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(questcmpl_receiver);

        if (windowManager != null) windowManager.removeView(buttonView);
        super.onDestroy();
    }

    private int startViewX, startViewY;
    private final View.OnTouchListener backgroundOnTouchListener = new View.OnTouchListener() {
        private final float[] lastX = new float[3];
        private final float[] lastY = new float[3];
        private final long[] lastT = new long[3];
        private float startX, startY;
        private int curr = 0;
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    lastX[0] = lastX[1] = lastX[2] = startX;
                    lastY[0] = lastY[1] = lastY[2] = startY;
                    lastT[0] = lastT[1] = lastT[2] = Calendar.getInstance().getTimeInMillis();
                    curr = (curr + 1) % 3;
                    startViewX = layoutParams.x;
                    startViewY = layoutParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", startViewX, startViewY));

                    buttonView.cancelAnimations();
                    break;

                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - lastX[(curr + 1) % 3];
                    float dy = event.getRawY() - lastY[(curr + 1) % 3];
                    long dt = Calendar.getInstance().getTimeInMillis() - lastT[(curr + 1) % 3];
                    float finalX, finalY;
                    if (dt < 50) {
                        float finalXUncap = layoutParams.x + dx / dt * 400;
                        float finalYUncap = layoutParams.y + dy / dt * 400;
                        finalX = max(screenPaddingLeft, Math.min(finalXUncap, screenPaddingLeft + screenWidth - buttonView.getWidth()));
                        finalY = max(screenPaddingTop, Math.min(finalYUncap, screenPaddingTop + screenHeight - buttonView.getHeight()));

                        buttonView.animateTo(layoutParams.x, layoutParams.y,
                                (int) finalX, (int) finalY,
                                finalXUncap == finalX ? 0 : max(2f, abs(dx / dt) / 2f), finalYUncap == finalY ? 0 : max(2f, abs(dy / dt) / 2f),
                                500, windowManager, layoutParams);
                    } else {
                        finalX = max(screenPaddingLeft, Math.min(layoutParams.x, screenPaddingLeft + screenWidth - buttonView.getWidth()));
                        finalY = max(screenPaddingTop, Math.min(layoutParams.y, screenPaddingTop + screenHeight - buttonView.getHeight()));
                        layoutParams.x = (int) finalX;
                        layoutParams.y = (int) finalY;
                        windowManager.updateViewLayout(buttonView, layoutParams);
                    }

                    JsonObject locdata = dbHelper.getJsonObjectValue(DB_KEY_FAIRYLOC);
                    String ori_prefix = getOrientationPrefix(getResources().getConfiguration().orientation);
                    if (locdata != null && !locdata.toString().isEmpty()) {
                        locdata.addProperty(ori_prefix.concat("x"), finalX);
                        locdata.addProperty(ori_prefix.concat("y"), finalY);
                    } else {
                        locdata = new JsonObject();
                    }
                    dbHelper.putValue(DB_KEY_FAIRYLOC, locdata.toString());
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - startX);
                    int y = (int) (event.getRawY() - startY);
                    Log.e("KCA", KcaUtils.format("Coord: %d %d", x, y));

                    lastX[curr] = event.getRawX();
                    lastY[curr] = event.getRawY();
                    lastT[curr] = Calendar.getInstance().getTimeInMillis();
                    curr = (curr + 1) % 3;
                    layoutParams.x = startViewX + x;
                    layoutParams.y = startViewY + y;
                    windowManager.updateViewLayout(buttonView, layoutParams);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    layoutParams.x = startViewX;
                    layoutParams.y = startViewY;
                    buttonView.cancelAnimations();
                    windowManager.updateViewLayout(buttonView, layoutParams);
                break;
            }
            return false;
        }
    };

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (abs(layoutParams.x - startViewX) < 20 && abs(layoutParams.y - startViewY) < 20) {
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
        }
    };

    private final View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (abs(layoutParams.x - startViewX) < 20 && abs(layoutParams.y - startViewY) < 20) {
                if (getBooleanPreferences(getApplicationContext(), PREF_FAIRY_NOTI_LONGCLICK)) {
                    doVibrate(vibrator, 100);
                }
                Toast.makeText(getApplicationContext(), getString(R.string.viewbutton_hide), Toast.LENGTH_LONG).show();
                buttonView.setVisibility(View.GONE);
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
                return true;
            } else {
                // Moved too much distance, not count as a long press
                return false;
            }
        }
    };

    private final Runnable mGlowRunner = new Runnable() {
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
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        String ori_prefix = getOrientationPrefix(newConfig.orientation);
        updateScreenSize();
        Log.e("KCA", "w/h: " + screenWidth + " " + screenHeight);

        JsonObject locdata = null;
        if (dbHelper != null) {
            locdata = dbHelper.getJsonObjectValue(DB_KEY_FAIRYLOC);

            layoutParams.flags = getLayoutParamsFlags();
            if (locdata != null && !locdata.toString().isEmpty()) {
                if (locdata.has(ori_prefix.concat("x"))) {
                    layoutParams.x = locdata.get(ori_prefix.concat("x")).getAsInt();
                }
                if (locdata.has(ori_prefix.concat("y"))) {
                    layoutParams.y = locdata.get(ori_prefix.concat("y")).getAsInt();
                }
            }

            if (windowManager != null && layoutParams != null) {
                if (layoutParams.x < 0) layoutParams.x = 0;
                else if (layoutParams.x > screenWidth - buttonWidth / 2) layoutParams.x = screenWidth - buttonWidth / 2;
                if (layoutParams.y < 0) layoutParams.y = 0;
                else if (layoutParams.y > screenHeight - buttonHeight / 2) layoutParams.y = screenHeight - buttonHeight / 2;

                windowManager.updateViewLayout(buttonView, layoutParams);
            }

            if (locdata != null && !locdata.toString().isEmpty()) {
                locdata.addProperty(ori_prefix.concat("x"), layoutParams.x);
                locdata.addProperty(ori_prefix.concat("y"), layoutParams.y);
            } else {
                locdata = new JsonObject();
            }
            dbHelper.putValue(DB_KEY_FAIRYLOC, locdata.toString());
        }
        super.onConfigurationChanged(newConfig);
    }

    private int getLayoutParamsFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    }

    private void updateScreenSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getCurrentWindowMetrics();
            WindowInsets insets = windowMetrics.getWindowInsets();
            // Not allow fairy to stay on cutout or navigation bar
            Insets safeInsets = insets.getInsets(WindowInsets.Type.displayCutout() | WindowInsets.Type.navigationBars());
            screenPaddingLeft = safeInsets.left;
            screenPaddingTop = safeInsets.top;
            Rect bounds = windowMetrics.getBounds();
            screenWidth = bounds.width() - safeInsets.left - safeInsets.right;
            screenHeight = bounds.height() - safeInsets.top - safeInsets.bottom;
        } else {
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }
    }
}