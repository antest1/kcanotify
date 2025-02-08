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

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KcaForegroundCheck {
    private static final String TAG = "KCA-FCS";

    public static final String FAIRY_FORECHECK_ON = "fairy_forecheck_on";
    public static final String FAIRY_FORECHECK_OFF = "fairy_forecheck_off";
    public static final int FOREGROUND_CHECK_INTERVAL = 500;

    private KcaViewButtonService service;
    ScheduledExecutorService checkForegroundScheduler;
    private boolean is_kc_foreground;
    private boolean is_login_done;
    private GotoForegroundReceiver gotoFgReceiver;
    
    public KcaForegroundCheck(KcaViewButtonService service) {
        this.service = service;
        is_kc_foreground = true;
        is_login_done = false;
        gotoFgReceiver = new GotoForegroundReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GOTO_FOREGROUND_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.service.registerReceiver(gotoFgReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            this.service.registerReceiver(gotoFgReceiver, filter);
        }
    }

    public void command(String action) {
        Log.d(TAG, "Command: " + action);
        if (FAIRY_FORECHECK_ON.equals(action)) {
            runForegroundCheck();
        } else if (FAIRY_FORECHECK_OFF.equals(action)) {
            stopForegroundCheck();
        }
    }

    public void exit() {
        if (gotoFgReceiver != null) service.unregisterReceiver(gotoFgReceiver);
        if (checkForegroundScheduler != null && !checkForegroundScheduler.isShutdown()) {
            checkForegroundScheduler.shutdown();
        }
    }

    public void runForegroundCheck() {
        checkForegroundScheduler = Executors.newSingleThreadScheduledExecutor();
        checkForegroundScheduler.scheduleWithFixedDelay(mForegroundCheckRunnable, 0,
                FOREGROUND_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stopForegroundCheck() {
        if (checkForegroundScheduler != null) {
            checkForegroundScheduler.shutdown();
        }
        service.showFairy();
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
        mUsageStatsManager = (UsageStatsManager) service.getSystemService(Context.USAGE_STATS_SERVICE);

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

    private final Runnable mForegroundCheckRunnable = () -> {
        try {
            boolean current_foreground_status = false;
            byte current_login_status = -1;
            String kcApp = getStringPreferences(service.getApplicationContext(), PREF_KC_PACKAGE);
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
                if (is_kc_foreground) {
                    service.showFairy();
                    Log.e(TAG, "kancolle detected: " + foregroundPackage);
                } else {
                    service.hideFairy();
                    Log.e(TAG, "kancolle not detected: " + foregroundPackage);
                }
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
