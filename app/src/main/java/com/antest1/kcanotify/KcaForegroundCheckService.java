package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaConstants.DMM_SDK_STORE_PACKAGE;
import static com.antest1.kcanotify.KcaConstants.GOTO_FOREGROUND_ACTION;
import static com.antest1.kcanotify.KcaConstants.GOTO_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.KC_WV_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.KC_WV_START_ACTIVITY;
import static com.antest1.kcanotify.KcaConstants.PREF_KC_PACKAGE;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KcaForegroundCheckService extends BaseService {
    private static final String TAG = "KCA-FCS";

    public static final String FAIRY_FORECHECK_ON = "fairy_forecheck_on";
    public static final String FAIRY_FORECHECK_OFF = "fairy_forecheck_off";
    public static final int FOREGROUND_CHECK_INTERVAL = 500;

    ScheduledExecutorService checkForegroundScheduler;
    private boolean is_kc_foreground;
    private boolean is_login_done;
    private GotoForegroundReceiver gotoFgReceiver;


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        is_kc_foreground = true;
        is_login_done = false;
        gotoFgReceiver = new GotoForegroundReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GOTO_FOREGROUND_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gotoFgReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(gotoFgReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "onStartCommand " + intent.getAction());
            if (Objects.equals(intent.getAction(), FAIRY_FORECHECK_ON)) {
                runForegroundCheck();
            } else if (Objects.equals(intent.getAction(), FAIRY_FORECHECK_OFF)) {
                stopForegroundCheck();
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (gotoFgReceiver != null) unregisterReceiver(gotoFgReceiver);
        if (checkForegroundScheduler != null && !checkForegroundScheduler.isShutdown()) {
            checkForegroundScheduler.shutdown();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    public void runForegroundCheck() {
        checkForegroundScheduler = Executors.newSingleThreadScheduledExecutor();
        checkForegroundScheduler.scheduleWithFixedDelay(mForegroundCheckRunnable, 0,
                FOREGROUND_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stopForegroundCheck() {
        if (checkForegroundScheduler != null) {
            checkForegroundScheduler.shutdown();
        }
        Intent intent = new Intent(getApplicationContext(), KcaViewButtonService.class);
        intent.setAction(KcaViewButtonService.RETURN_FAIRY_ACTION);
        startService(intent);
    }

    private static boolean isForeGroundEvent(UsageEvents.Event event) {
        if(event == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED;
        } else {
            return event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND;
        }
    }

    public String checkForegroundPackage() {
        String classByUsageStats = null;
        String recentPackageName = "";

        UsageStatsManager mUsageStatsManager;
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        final long INTERVAL = 5000;
        final long end = System.currentTimeMillis();
        final long begin = end - INTERVAL;
        final UsageEvents usageEvents = mUsageStatsManager.queryEvents(begin, end);

        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (isForeGroundEvent(event)) {
                Date d = new Date(event.getTimeStamp());
                classByUsageStats = event.getClassName() + " " + d.toString();
                recentPackageName = classByUsageStats;
            }
        }
        return recentPackageName;
    }

    private Runnable mForegroundCheckRunnable = () -> {
        try {
            boolean current_foreground_status = false;
            byte current_login_status = -1;
            String kcApp = getStringPreferences(getApplicationContext(), PREF_KC_PACKAGE);
            String foregroundPackage = "";

            if (kcApp.equals(GOTO_PACKAGE_NAME)) {
                foregroundPackage = "<gotobrowser>";
                current_foreground_status = gotoFgReceiver.checkForeground();
            } else {
                foregroundPackage = checkForegroundPackage();
                if (!foregroundPackage.trim().isEmpty()) {
                    if (foregroundPackage.contains(KC_PACKAGE_NAME)) {
                        current_foreground_status = true;
                    } if (foregroundPackage.contains(KC_WV_PACKAGE_NAME)) {
                        current_foreground_status = true;
                    } else if (foregroundPackage.contains(GOTO_PACKAGE_NAME)) {
                        current_foreground_status = true;
                    }

                    if (foregroundPackage.contains(KC_WV_START_ACTIVITY)) {
                        Log.d(TAG, "dmm detected: " + foregroundPackage);
                        current_login_status = 1;
                    } else if (foregroundPackage.contains(DMM_SDK_STORE_PACKAGE)) {
                        Log.d(TAG, "dmm detected: " + foregroundPackage);
                        current_login_status = 0;
                    }
                } else {
                    current_foreground_status = is_kc_foreground;
                }
            }

            if (current_foreground_status != is_kc_foreground) {
                is_kc_foreground = current_foreground_status;
                Intent intent = new Intent(getApplicationContext(), KcaViewButtonService.class);
                if (is_kc_foreground) {
                    intent.setAction(KcaViewButtonService.KCA_STATUS_ON);
                    Log.e(TAG, "kancolle detected: " + foregroundPackage);
                } else {
                    intent.setAction(KcaViewButtonService.KCA_STATUS_OFF);
                    Log.e(TAG, "kancolle not detected: " + foregroundPackage);
                }
                startService(intent);
            }

            boolean current_login_flag = (current_login_status == 1);
            if (current_login_status != -1 && current_login_flag != is_login_done) {
                is_login_done = current_login_flag;
                Log.e(TAG, "login event detected: " + current_login_flag);
            }
        } catch (Exception e) {
            Log.e(TAG, getStringFromException(e));
        }
    };
}
