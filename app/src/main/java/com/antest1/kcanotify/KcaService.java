package com.antest1.kcanotify;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_HIGH;
import static android.widget.Toast.makeText;
import static com.antest1.kcanotify.KcaAlarmService.DELETE_ACTION;
import static com.antest1.kcanotify.KcaApiData.AKASHI_TIMER_20MIN;
import static com.antest1.kcanotify.KcaApiData.T2_DRUM_CAN;
import static com.antest1.kcanotify.KcaApiData.T2_FIGHTER;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE_II;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_MEDIUM;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_SMALL;
import static com.antest1.kcanotify.KcaApiData.T2_MACHINE_GUN;
import static com.antest1.kcanotify.KcaApiData.T2_RADAR_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_RADAR_SMALL;
import static com.antest1.kcanotify.KcaApiData.T2_RADER_LARGE_II;
import static com.antest1.kcanotify.KcaApiData.T2_SEA_SCOUT;
import static com.antest1.kcanotify.KcaApiData.T2_SUB_GUN;
import static com.antest1.kcanotify.KcaApiData.T2_TORPEDO;
import static com.antest1.kcanotify.KcaApiData.checkDataLoadTriggered;
import static com.antest1.kcanotify.KcaApiData.getAdmiralLevel;
import static com.antest1.kcanotify.KcaApiData.getNodeColor;
import static com.antest1.kcanotify.KcaApiData.getReturnFlag;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.loadMapEdgeInfoFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadQuestTrackDataFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadShipExpInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipInitEquipCountFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadSubMapInfoFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaApiData.updateUserShip;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaFleetViewService.REFRESH_FLEETVIEW_ACTION;
import static com.antest1.kcanotify.KcaMoraleInfo.setMoraleValue;
import static com.antest1.kcanotify.KcaQuestViewService.REFRESH_QUESTVIEW_ACTION;
import static com.antest1.kcanotify.KcaTimerWidget.WIDGET_DATA_UPDATE;
import static com.antest1.kcanotify.KcaUtils.createBuilder;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;
import static com.antest1.kcanotify.KcaViewButtonService.REMOVE_FAIRY_ACTION;
import static com.antest1.kcanotify.KcaViewButtonService.RETURN_FAIRY_ACTION;

public class KcaService extends Service {
    public static final String SERVICE_CHANNEL_ID = "kcaservice_noti_channel_";
    public static final String SERVICE_CHANNEL_ID_OLD = "noti_service_channel";
    public static final String SERVICE_CHANNEL_NAME = "Kcanotify Service";

    public static String currentLocale;
    public static boolean isInitState = false;
    public static boolean isFirstState;
    public static boolean isPassiveMode = false;
    public static boolean restartFlag = false;

    public static boolean isServiceOn = false;
    public static boolean isPortAccessed = false;
    public static boolean isAkashiTimerNotiWait = true;
    public static int heavyDamagedMode = 0;

    public static int checkKdockId = -1;
    public static int checkLargeFlag = -1;
    public static int checkHighSpeed = -1;

    public static boolean kaisouProcessFlag = false;
    public static String currentNodeInfo = "";
    public static boolean isInBattle;
    public static boolean isCombined;

    Context contextWithLocale;
    KcaDBHelper dbHelper;
    KcaQuestTracker questTracker;
    KcaDropLogger dropLogger;
    KcaResourceLogger resourceLogger;
    KcaPacketLogger packetLogger;

    KcaDeckInfo deckInfoCalc;
    KcaQSyncAPI kcaQSyncEndpoint;

    AlarmManager alarmManager;
    AudioManager mAudioManager;
    Vibrator vibrator = null;
    MediaPlayer mediaPlayer;
    NotificationManager notifiManager;

    public static boolean noti_vibr_on = true;
    // int viewBitmapId, viewBitmapSmallId;
    // Bitmap viewBitmap = null;
    Runnable timer;
    int notificationTimeCounter;

    private String notifyTitle = "";
    private String notifyContent = "";
    private boolean notifyFirstTime = true;
    private NotificationCompat.Builder notifyBuilder;

    kcaServiceHandler handler;
    kcaNotificationHandler nHandler;
    LocalBroadcastManager broadcaster;
    ScheduledExecutorService timeScheduler = null;
    private BroadcastReceiver receiver;

    String kcaFirstDeckInfo;
    static String kca_version;
    String api_start2_data = null;
    boolean api_start2_down_mode = false;
    boolean api_start2_init = false;
    boolean api_start2_loading_flag = false;
    Gson gson = new GsonBuilder().setLenient().create();

    public static boolean getServiceStatus() {
        return isServiceOn;
    }

    public Handler getNofiticationHandler() {
        return nHandler;
    }

    private boolean checkKeyInPreferences(String key) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        return pref.contains(key);
    }

    private void createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int priority = IMPORTANCE_DEFAULT;
            if (getBooleanPreferences(getApplicationContext(), PREF_KCA_SET_PRIORITY)) {
                priority = IMPORTANCE_HIGH;
            }
            NotificationChannel channel = new NotificationChannel(getServiceChannelId(),
                    SERVICE_CHANNEL_NAME, priority);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notifiManager.deleteNotificationChannel(SERVICE_CHANNEL_ID_OLD);
            notifiManager.createNotificationChannel(channel);
        }
    }

    private String getServiceChannelId() {
        boolean priority = getBooleanPreferences(getApplicationContext(), PREF_KCA_SET_PRIORITY);
        return SERVICE_CHANNEL_ID.concat(String.valueOf(priority));
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    void runTimer() {
        if (timeScheduler == null || timeScheduler.isShutdown()) {
            if (isMissionTimerViewEnabled() || isAkashiTimerNotiEnabled()) {
                timeScheduler = Executors.newSingleThreadScheduledExecutor();
                timeScheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
            }
        }
    }

    void stopTimer() {
        if (timeScheduler != null) {
            timeScheduler.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-S", "onStartCommand Called");
        isServiceOn = true;

        isFirstState = true;
        restartFlag = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(PREF_SVC_ENABLED, true).apply();

        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);
        dropLogger = new KcaDropLogger(getApplicationContext(), null, KCANOTIFY_DROPLOG_VERSION);
        resourceLogger = new KcaResourceLogger(getApplicationContext(), null, KCANOTIFY_RESOURCELOG_VERSION);
        packetLogger = new KcaPacketLogger(getApplicationContext(), null, KCANOTIFY_PACKETLOG_VERSION);
        deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());
        kcaQSyncEndpoint = KcaUtils.getQuestSync(getApplicationContext());
        KcaApiData.setDBHelper(dbHelper);

        AssetManager assetManager = getResources().getAssets();
        int loadMapEdgeInfoResult = loadMapEdgeInfoFromStorage(getApplicationContext());
        if (loadMapEdgeInfoResult != 1) {
            Toast.makeText(this, "Error loading Map Edge Info", Toast.LENGTH_LONG).show();
        }

        int loadSubMapInfoResult = loadSubMapInfoFromStorage(getApplicationContext());
        if (loadSubMapInfoResult != 1) {
            Toast.makeText(this, "Error loading Map Sub Info", Toast.LENGTH_LONG).show();
        }

        int loadExpShipInfoResult = loadShipExpInfoFromAssets(assetManager);
        if (loadExpShipInfoResult != 1) {
            Toast.makeText(this, "Error loading Exp Ship Info", Toast.LENGTH_LONG).show();
        }

        loadSimpleExpeditionInfoFromStorage(getApplicationContext());
        loadShipInitEquipCountFromStorage(getApplicationContext());
        loadQuestTrackDataFromStorage(dbHelper, getApplicationContext());
        dbHelper.initQuestCheck();
        dbHelper.initExpScore();
        QSyncRead();

        showDataLoadErrorToast(getApplicationContext(), getBaseContext(), getStringWithLocale(R.string.download_check_error));

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.i("Completion Listener", "Song Complete");
                mp.stop();
                mp.reset();
            }
        });
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notifiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createServiceChannel();

        handler = new kcaServiceHandler(this);
        nHandler = new kcaNotificationHandler(this);
        broadcaster = LocalBroadcastManager.getInstance(this);

        int sniffer_mode = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_SNIFFER_MODE));
        Log.e("KCA-S", String.valueOf(sniffer_mode));
        switch (sniffer_mode) {
            case SNIFFER_ACTIVE:
                KcaVpnData.setHandler(handler);
                break;
            case SNIFFER_PASSIVE:
                KcaReceiver.setHandler(handler);
                break;
            default:
                stopSelf();
        }

        KcaBattle.setHandler(nHandler);
        KcaApiData.setHandler(nHandler);
        KcaAlarmService.setHandler(nHandler);
        KcaOpenDBAPI.setHandler(nHandler);
        MainActivity.setHandler(nHandler);
        MainPreferenceFragment.setHandler(nHandler);
        KcaFairySelectActivity.setHandler(nHandler);
        KcaViewButtonService.setHandler(nHandler);
        KcaAkashiRepairInfo.initAkashiTimer();

        isPassiveMode = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_SNIFFER_MODE)) == SNIFFER_PASSIVE;
        if (isPassiveMode) {
            receiver = new KcaReceiver();
            IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
            registerReceiver(receiver, filter);
        }

        notifyFirstTime = true;
        notifyBuilder = createBuilder(contextWithLocale, getServiceChannelId());
        notifyTitle = KcaUtils.format(getStringWithLocale(R.string.kca_init_title), getStringWithLocale(R.string.app_name));
        notifyContent = getStringWithLocale(R.string.kca_init_content);
        // String initSubContent = KcaUtils.format("%s %s", getStringWithLocale(R.string.app_name), getStringWithLocale(R.string.app_version));
        kcaFirstDeckInfo = getStringWithLocale(R.string.kca_init_content);
        initViewNotificationBuilder(notifyTitle, notifyContent);
        startForeground(getNotificationId(NOTI_FRONT, 1), notifyBuilder.build());

        notificationTimeCounter = -1;
        timer = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isMissionTimerViewEnabled()) {
                        notificationTimeCounter += 1;
                        if (notificationTimeCounter == 120) {
                            notificationTimeCounter = 0;
                        }
                        updateExpViewNotification();
                    }
                    if (KcaAkashiRepairInfo.getAkashiTimerValue() > 0) {
                        int second = KcaAkashiRepairInfo.getAkashiElapsedTimeInSecond();
                        if (second >= AKASHI_TIMER_20MIN && isAkashiTimerNotiWait) {
                            isAkashiTimerNotiWait = false;
                        }
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "tmer", "", "", getStringFromException(e));
                }
            }
        };
        processExpeditionInfo(true);
        runTimer();
        return START_STICKY;
    }

    // 서비스가 종료될 때 할 작업

    public void setServiceDown() {
        isPortAccessed = false;
        stopTimer();

        handler = null;
        nHandler = null;

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (receiver != null) unregisterReceiver(receiver);
        receiver = null;

        if (notifiManager != null) notifiManager.cancelAll();
        notifiManager = null;
        isServiceOn = false;
    }

    public void onDestroy() {
        Log.e("KCA-S", "onDestroy Called");
        stopService(new Intent(this, KcaBattleViewService.class));
        stopService(new Intent(this, KcaQuestViewService.class));
        stopService(new Intent(this, KcaFleetViewService.class));
        stopService(new Intent(this, KcaAkashiViewService.class));
        stopService(new Intent(this, KcaMapHpPopupService.class));
        stopService(new Intent(this, KcaConstructPopupService.class));
        stopService(new Intent(this, KcaDevelopPopupService.class));
        stopService(new Intent(this, KcaLandAirBasePopupService.class));
        stopService(new Intent(this, KcaViewButtonService.class));
        stopService(new Intent(this, KcaExpeditionCheckViewService.class));
        stopService(new Intent(this, KcaCustomToastService.class));
        setServiceDown();
        KcaAlarmService.clearAlarmCount();
        stopForeground(true);
        if (dbHelper != null) dbHelper.close();
        super.onDestroy();
    }

    public boolean isPackageExist(String name) {
        boolean isExist = false;

        PackageManager pkgMgr = getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(name)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    private void initViewNotificationBuilder(String title, String content) {
        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        int type = KcaAlarmService.getAlarmCount() > 0 ? 1 : 0;
        String fairyId = "noti_icon_".concat(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
        int viewBitmapSmallId = getId("ic_stat_notify_".concat(String.valueOf(type)), R.mipmap.class);
        Bitmap viewBitmap = KcaUtils.getFairyImageFromStorage(getApplicationContext(), fairyId, dbHelper);

        notifyBuilder.setContentTitle(title)
                .setSmallIcon(viewBitmapSmallId)
                .setLargeIcon(viewBitmap)
                .setTicker(title)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(content))
                .setOngoing(true).setAutoCancel(false);

        if (getBooleanPreferences(getApplicationContext(), PREF_KCA_SET_PRIORITY)) {
            notifyBuilder.setPriority(IMPORTANCE_HIGH);
        } else {
            notifyBuilder.setPriority(IMPORTANCE_DEFAULT);
        }
    }

    private void updateViewNotificationBuilder(String title, String content2) {
        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        notifyBuilder.setContentTitle(title)
                .setTicker(title)
                .setContentIntent(pendingIntent)
                .setContentText(content2)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(content2));
    }

    private void updateExpViewNotification() {
        int viewType = getExpeditionType();
        notifyContent = "";
        if (isMissionTimerViewEnabled()) {
            if (!KcaExpedition2.isMissionExist()) {
                if (isFirstState) notifyContent = KcaUtils.format("%s %s", getStringWithLocale(R.string.app_name), getStringWithLocale(R.string.app_version));
                else notifyContent = notifyContent.concat(getStringWithLocale(R.string.kca_view_noexpedition));
            } else {
                List<String> kcaExpStrList = new ArrayList<String>();
                for (int i = 1; i < 4; i++) {
                    String str = KcaExpedition2.getTimeInfoStr(i, viewType);
                    if (str.length() > 0) {
                        kcaExpStrList.add(str);
                    }
                }
                if (viewType == 1) {
                    int value = (notificationTimeCounter / 2) % (kcaExpStrList.size());
                    String countStr = KcaUtils.format(" (%d/%d)", value+1, kcaExpStrList.size());
                    notifyContent = kcaExpStrList.get(value).concat(countStr);
                } else {
                    notifyContent = joinStr(kcaExpStrList, " / ");
                }
            }
            if (notifyContent.trim().length() == 0) {
                notifyContent = notifyContent.concat(getStringWithLocale(R.string.kca_view_noexpedition));
            }
        } else {
            notifyContent = KcaUtils.format("%s %s", getStringWithLocale(R.string.app_name), getStringWithLocale(R.string.app_version));
        }

        String nodeString = "";
        if (currentNodeInfo.length() > 0) {
            nodeString = KcaUtils.format("[%s]", currentNodeInfo.replaceAll("[()]", "").replaceAll("\\s", "/"));
        }

        switch (heavyDamagedMode) {
            case HD_DAMECON:
                notifyTitle = KcaUtils.format(getStringWithLocale(R.string.kca_view_hdmg_damecon_format), getStringWithLocale(R.string.app_name), nodeString).trim();
                break;
            case HD_DANGER:
                notifyTitle = KcaUtils.format(getStringWithLocale(R.string.kca_view_hdmg_format), getStringWithLocale(R.string.app_name), nodeString).trim();
                break;
            default:
                notifyTitle = KcaUtils.format(getStringWithLocale(R.string.kca_view_normal_format), getStringWithLocale(R.string.app_name), nodeString).trim();
                break;
        }
        updateNotification(true);
    }

    private void updateNotification(boolean change_content) {
        if (change_content) {
            updateViewNotificationBuilder(notifyTitle, notifyContent);
        }
        notifiManager.notify(getNotificationId(NOTI_FRONT, 1), notifyBuilder.build());
    }

    private void setExpeditionAlarm(int idx, int mission_no, String deck_name, long arrive_time, boolean cancel_flag, boolean ca_flag, Intent aIntent) {
        if (!getReturnFlag(mission_no) && !cancel_flag) return;

        JsonObject expeditionAlarmData = new JsonObject();
        expeditionAlarmData.addProperty("type", KcaAlarmService.TYPE_EXPEDITION);
        expeditionAlarmData.addProperty("idx", idx);
        expeditionAlarmData.addProperty("mission_no", mission_no);
        expeditionAlarmData.addProperty("kantai_name", deck_name);
        expeditionAlarmData.addProperty("cancel_flag", cancel_flag);
        expeditionAlarmData.addProperty("ca_flag", ca_flag);
        if (ca_flag) idx = idx | KcaAlarmService.EXP_CANCEL_FLAG;

        aIntent.putExtra("data", expeditionAlarmData.toString());
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                getNotificationId(NOTI_EXP, idx),
                aIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        setAlarm(arrive_time, pendingIntent, getNotificationId(NOTI_EXP, idx), true);
    }

    private void setDockingAlarm(int dockId, int shipId, long complete_time, Intent aIntent) {
        String shipName = "";
        int shipKcId = -1;
        if (isGameDataLoaded()) {
            JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "ship_id");
            shipKcId = shipData.get("ship_id").getAsInt();
        }
        JsonObject dockingAlarmData = new JsonObject();
        dockingAlarmData.addProperty("type", KcaAlarmService.TYPE_DOCKING);
        dockingAlarmData.addProperty("dock_id", dockId);
        dockingAlarmData.addProperty("ship_id", shipKcId);

        aIntent.putExtra("data", dockingAlarmData.toString());
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                getNotificationId(NOTI_DOCK, dockId),
                aIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        setAlarm(complete_time, pendingIntent, getNotificationId(NOTI_DOCK, dockId), true);
    }

    private void setMoraleAlarm(int deckId, String deck_name, long complete_time, Intent aIntent) {
        JsonObject moraleAlarmData = new JsonObject();
        moraleAlarmData.addProperty("type", KcaAlarmService.TYPE_MORALE);
        moraleAlarmData.addProperty("kantai_name", deck_name);
        moraleAlarmData.addProperty("idx", deckId);

        aIntent.putExtra("data", moraleAlarmData.toString());
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                getNotificationId(NOTI_MORALE, deckId),
                aIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        setAlarm(complete_time, pendingIntent, getNotificationId(NOTI_DOCK, deckId), false);
    }

    private void setAkashiAlarm(Intent aIntent) {
        JsonObject akashiAlarmData = new JsonObject();
        akashiAlarmData.addProperty("type", KcaAlarmService.TYPE_AKASHI);
        aIntent.putExtra("data", akashiAlarmData.toString());
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                getNotificationId(NOTI_AKASHI, 0),
                aIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        long complete_time = KcaAkashiRepairInfo.getAkashiRepairTime();
        if (complete_time > 0) setAlarm(complete_time, pendingIntent, getNotificationId(NOTI_AKASHI, 0), false);
    }

    private void toastInfo() {
        if (KcaFleetCheckPopupService.isActive()) {
            Intent qintent = new Intent(getBaseContext(), KcaFleetCheckPopupService.class);
            qintent.setAction(KcaFleetCheckPopupService.FCHK_SHOW_ACTION);
            startService(qintent);
        }
    }

    private static class kcaServiceHandler extends Handler {
        private final WeakReference<KcaService> mService;

        kcaServiceHandler(KcaService service) {
            mService = new WeakReference<KcaService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            KcaService service = mService.get();
            if (service != null) {
                service.handleServiceMessage(msg);
            }
        }
    }

    private void makeToast(String message, int length, int color) {
        KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());
        showCustomToast(customToast, message, length, color);
    }

    public void handleServiceMessage(Message msg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = msg.getData().getString("url");
        byte[] raw = msg.getData().getByteArray("data");
        Reader data = new InputStreamReader(new ByteArrayInputStream(raw));
        String request = msg.getData().getString("request");

        if (!prefs.getBoolean(PREF_SVC_ENABLED, false) || url.length() == 0) {
            return;
        }

        final JsonObject jsonDataObj;
        try {
            String init = new String(Arrays.copyOfRange(raw, 0, 7));
            if (init.contains("svdata=")) {
                data.skip("svdata=".length());
            }
            if (raw.length > 0) jsonDataObj = gson.fromJson(data, JsonObject.class);
            else jsonDataObj = new JsonObject();
            if (url.equals(KCA_API_RESOURCE_URL)) {
                dbHelper.recordErrorLog(ERROR_TYPE_VPN, KCA_API_RESOURCE_URL, "", "", request);
                return;
            }
            if (getBooleanPreferences(getApplicationContext(), PREF_PACKET_LOG)) {
                packetLogger.log(url, request, jsonDataObj.toString());
            }

            if (url.equals(KCA_API_VPN_DATA_ERROR)) { // VPN Data Dump Send
                String api_url = jsonDataObj.get("uri").getAsString();
                String api_request = jsonDataObj.get("request").getAsString();
                String api_response = jsonDataObj.get("response").getAsString();
                String api_error = jsonDataObj.get("error").getAsString();

                List<String> filtered_resquest_list = new ArrayList<String>();
                try {
                    String[] requestData = api_request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (!decodedData.startsWith("api_token")) {
                            filtered_resquest_list.add(requestData[i]);
                        }
                    }
                    api_request = joinStr(filtered_resquest_list, "&");
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }

                makeToast(getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                dbHelper.recordErrorLog(ERROR_TYPE_VPN, api_url, api_request, api_response, api_error);
                return;
            }

            if (url.startsWith(KCA_VERSION)) {

                isPortAccessed = false;
                isInBattle = false;
                api_start2_init = false;
                api_start2_loading_flag = true;
                KcaFleetViewService.setReadyFlag(false);
                //Toast.makeText(contextWithLocale, "KCA_VERSION", Toast.LENGTH_LONG).show();
                String version_data = new String(raw);
                JsonObject api_data = gson.fromJson(version_data, JsonObject.class);

                if (api_data != null && api_data.has("api")) {
                    JsonObject api_version = api_data.getAsJsonObject("api");
                    kca_version = api_version.get("api_start2").getAsString();
                    Log.e("KCA", kca_version);

                    setPreferences(getApplicationContext(), PREF_KCA_VERSION, kca_version);
                    if (!getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION).equals(kca_version)) {
                        makeToast("new game data detected: " + String.valueOf(kca_version), Toast.LENGTH_LONG,
                                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    }
                }

                JsonObject kcDataObj = dbHelper.getJsonObjectValue(DB_KEY_STARTDATA);
                //Log.e("KCA", kcDataObj.toJSONString());
                if (kcDataObj != null && kcDataObj.has("api_data")) {
                    //Toast.makeText(contextWithLocale, "Load Kancolle Data", Toast.LENGTH_LONG).show();
                    KcaApiData.getKcGameData(kcDataObj.getAsJsonObject("api_data"));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(getApplicationContext())) {
                    // Can not draw overlays: pass
                } else {
                    startService(new Intent(this, KcaViewButtonService.class));
                    startService(new Intent(this, KcaQuestViewService.class));
                    sendQuestCompletionInfo();
                }

                KcaMoraleInfo.initMoraleValue(Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCA_MORALE_MIN)));
                return;
                //Toast.makeText(contextWithLocale, getPreferences("kca_version") + " " + String.valueOf(api_start2_down_mode), Toast.LENGTH_LONG).show();
            }

            if (url.startsWith(API_WORLD_GET_WORLDINFO)) {
                makeToast(getStringWithLocale(R.string.kca_toast_server_select), Toast.LENGTH_LONG,
                        ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                return;
            }

            if (url.startsWith(API_WORLD_GET_ID)) {
                return;
            }

            if (url.startsWith(API_REQ_MEMBER_GET_INCENTIVE)) {
                return;
            }

            if (url.equals(API_START2) || url.equals(API_START2_NEW)) {
                if (jsonDataObj.has("api_data")) {
                    api_start2_data = jsonDataObj.toString();
                    dbHelper.putValue(DB_KEY_STARTDATA, api_start2_data);
                    //Toast.makeText(contextWithLocale, "Load Kancolle Data", Toast.LENGTH_LONG).show();
                    KcaApiData.getKcGameData(jsonDataObj.getAsJsonObject("api_data"));
                    if (kca_version != null) {
                        setPreferences(getApplicationContext(), "kca_version", kca_version);
                    }
                }
                return;
            }

            if (url.startsWith(API_GET_MEMBER_REQUIRED_INFO)) {
                //Log.e("KCA", "Load Item Data");
                isInitState = true;
                if (jsonDataObj.has("api_data")) {
                    //dbHelper.putValue(DB_KEY_USEREQUIP, jsonDataObj.getAsJsonObject("api_data").getAsJsonArray("api_slot_item").toString());
                    JsonObject requiredInfoApiData = jsonDataObj.getAsJsonObject("api_data");
                    dbHelper.putValue(DB_KEY_KDOCKDATA, requiredInfoApiData.getAsJsonArray("api_kdock").toString());
                    JsonArray slotitem_data = requiredInfoApiData.getAsJsonArray("api_slot_item");
                    int size2 = KcaApiData.putSlotItemDataToDB(slotitem_data);
                    Log.e("KCA", "Total Items: " + String.valueOf(size2));
                    if (size2 > 0) restartFlag = false;
                }
                return;
            }

            if (url.startsWith(API_GET_MEMBER_USEITEM)) {
                if (jsonDataObj.has("api_data")) {
                    dbHelper.putValue(DB_KEY_USEITEMS, jsonDataObj.getAsJsonArray("api_data").toString());
                }
            }

            if (url.startsWith(API_GET_MEMBER_DECK)) {
                //Log.e("KCA", "Expedition Handler Called");
                if (jsonDataObj.has("api_data")) {
                    dbHelper.putValue(DB_KEY_DECKPORT, jsonDataObj.getAsJsonArray("api_data").toString());
                    processExpeditionInfo();
                }
            }

            if (url.startsWith(API_GET_MEMBER_NDOCK)) {
                if (jsonDataObj.has("api_data")) {
                    JsonArray api_data = jsonDataObj.getAsJsonArray("api_data");
                    KcaDocking.setDockData(api_data);
                    dbHelper.putValue(DB_KEY_NDOCKDATA, api_data.toString());
                }
            }

            if (url.startsWith(API_REQ_MISSION_RESULT)) {
                int deck_id = -1;
                String[] requestData = request.split("&");
                for (int i = 0; i < requestData.length; i++) {
                    String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                    if (decodedData.startsWith("api_deck_id")) {
                        deck_id = Integer.valueOf(decodedData.replace("api_deck_id=", "")) - 1;
                        break;
                    }
                }
                int nid = getNotificationId(NOTI_EXP, deck_id);
                Intent deleteIntent = new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid)));
                startService(deleteIntent);
                notifiManager.cancel(nid);
                if (jsonDataObj.has("api_data")) {
                    JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                    if (api_data.has("api_clear_result") && api_data.get("api_clear_result").getAsInt() > 0) {
                        questTracker.updateIdCountTracker("402");
                        questTracker.updateIdCountTracker("403");
                        questTracker.updateIdCountTracker("404");

                        String api_name = api_data.get("api_quest_name").getAsString();
                        int api_no = KcaApiData.getExpeditionNoByName(api_name);
                        switch (api_no) {
                            case 3: // 경계임무 (3)
                                questTracker.updateIdCountTracker("426", 0);
                                break;
                            case 4: // 대잠경계임무 (4)
                                questTracker.updateIdCountTracker("426", 1);
                                questTracker.updateIdCountTracker("428", 0);
                                break;
                            case 5: // 해상호위 (5)
                                questTracker.updateIdCountTracker("424");
                                questTracker.updateIdCountTracker("426", 2);
                                break;
                            case 10: // 강행정찰임무 (10)
                                questTracker.updateIdCountTracker("426", 3);
                                break;
                            case 37: // 도쿄급행 (37, 38)
                            case 38:
                                questTracker.updateIdCountTracker("410");
                                questTracker.updateIdCountTracker("411");
                                break;
                            case 101: // 해협경계 (A2)
                                questTracker.updateIdCountTracker("428", 1);
                                break;
                            case 102: // 장시간대잠 (A3)
                                questTracker.updateIdCountTracker("428", 2);
                                break;
                            default:
                                break;
                        }
                        updateQuestView();
                    }
                    if (api_data.has("api_get_exp")) {
                        dbHelper.updateExpScore(api_data.get("api_get_exp").getAsInt());
                    }
                }
            }

            if (url.startsWith(API_REQ_HOKYU_CHARGE)) {
                questTracker.updateIdCountTracker("504");
                updateQuestView();
            }

            if (url.startsWith(API_GET_MEMBER_RECORD)) {
                if (jsonDataObj.has("api_data")) {
                    JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                    JsonArray slotitem = api_data.getAsJsonArray("api_slotitem");
                    restartFlag = (slotitem.get(0).getAsInt() != dbHelper.getItemCount());
                }
                return;
            }

            if (url.startsWith(API_PORT)) {
                isPortAccessed = true;
                isFirstState = false;
                stopService(new Intent(this, KcaBattleViewService.class));
                startService(new Intent(this, KcaViewButtonService.class)
                        .setAction(KcaViewButtonService.DEACTIVATE_BATTLEVIEW_ACTION));
                startService(new Intent(this, KcaViewButtonService.class)
                        .setAction(KcaViewButtonService.RESET_FAIRY_STATUS_ACTION));

                if (jsonDataObj.has("api_data")) {
                    JsonObject reqPortApiData = jsonDataObj.getAsJsonObject("api_data");
                    if (reqPortApiData.has("api_basic")) {
                        JsonObject prev_basic = dbHelper.getJsonObjectValue(DB_KEY_BASICIFNO);
                        JsonObject current_basic = reqPortApiData.getAsJsonObject("api_basic");
                        if (prev_basic != null && isInitState) {
                            long prev_ts = prev_basic.get("api_starttime").getAsLong();
                            long current_ts = current_basic.get("api_starttime").getAsLong();
                            if (current_ts - prev_ts < 43200000L) { // in 12h
                                int prev_exp = prev_basic.get("api_experience").getAsInt();
                                int current_exp = current_basic.get("api_experience").getAsInt();
                                int diff = current_exp - prev_exp;
                                if (diff >= 0) dbHelper.updateExpScore(current_exp - prev_exp, true);
                            }
                        }
                        isInitState = false;
                    }
                    KcaApiData.getPortData(reqPortApiData);
                    if (reqPortApiData.has("api_deck_port")) {
                        dbHelper.putValue(DB_KEY_DECKPORT, reqPortApiData.getAsJsonArray("api_deck_port").toString());
                        dbHelper.test();
                    }
                    if (reqPortApiData.has("api_ship")) {
                        final String ship_data = reqPortApiData.get("api_ship").toString();
                        Thread ship_data_thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                dbHelper.putValue(DB_KEY_SHIPIFNO, ship_data);
                            }
                        });
                        ship_data_thread.start();
                    }
                    if (reqPortApiData.has("api_ndock")) {
                        dbHelper.putValue(DB_KEY_NDOCKDATA, reqPortApiData.getAsJsonArray("api_ndock").toString());
                        JsonArray nDockData = reqPortApiData.getAsJsonArray("api_ndock");
                        KcaDocking.setDockData(nDockData);
                        processDockingInfo();
                    }

                    if (reqPortApiData.has("api_material")) {
                        JsonArray material_data = reqPortApiData.getAsJsonArray("api_material");
                        dbHelper.putValue(DB_KEY_MATERIALS, material_data.toString());
                        recordResourceLog(material_data, true);
                    }
                }
            }

            if (url.startsWith(API_REQ_MAP_START) || url.startsWith(API_REQ_MAP_NEXT)) {
                if (jsonDataObj.has("api_data") && isBattleNodeEnabled()) {
                    JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                    int currentMapArea = api_data.get("api_maparea_id").getAsInt();
                    int currentMapNo = api_data.get("api_mapinfo_no").getAsInt();
                    int currentNode = api_data.get("api_no").getAsInt();
                    String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);
                    int api_event_kind = api_data.get("api_event_kind").getAsInt();
                    int api_event_id = api_data.get("api_event_id").getAsInt();
                    int api_color_no = api_data.get("api_color_no").getAsInt();
                    currentNodeInfo = KcaApiData.getNodeFullInfo(contextWithLocale, currentNodeAlphabet, api_event_id, api_event_kind, false);
                    makeToast(currentNodeInfo, Toast.LENGTH_LONG, getNodeColor(getApplicationContext(), api_event_id, api_event_kind, api_color_no));
                }
            }

            if (url.startsWith(API_GET_MEMBER_SLOT_ITEM)) {
                if (jsonDataObj.has("api_data")) {
                    JsonArray api_data = jsonDataObj.get("api_data").getAsJsonArray();
                    KcaApiData.putSlotItemDataToDB(api_data);
                    restartFlag = false;
                }
            }

            if (url.equals(API_GET_MEMBER_MATERIAL)) {
                if (jsonDataObj.has("api_data")) {
                    JsonArray api_data = jsonDataObj.get("api_data").getAsJsonArray();
                    dbHelper.putValue(DB_KEY_MATERIALS, api_data.toString());
                    recordResourceLog(api_data, true);
                }
                return;
            }

            if (!API_QUEST_REQS.contains(url) && !url.equals(API_GET_MEMBER_MATERIAL) && KcaQuestViewService.getQuestMode()) {
                startService(new Intent(this, KcaViewButtonService.class)
                        .setAction(KcaViewButtonService.DEACTIVATE_QUESTVIEW_ACTION));
                KcaQuestViewService.setQuestMode(false);
                startService(new Intent(this, KcaQuestViewService.class)
                        .setAction(KcaQuestViewService.CLOSE_QUESTVIEW_ACTION));
                updateQuestView();
            }

            if (url.startsWith(API_GET_MEMBER_QUESTLIST)) {
                startService(new Intent(this, KcaViewButtonService.class)
                        .setAction(KcaViewButtonService.ACTIVATE_QUESTVIEW_ACTION));
                KcaQuestViewService.setQuestMode(true);
                int api_tab_id = -1;
                String[] requestData = request.split("&");
                for (int i = 0; i < requestData.length; i++) {
                    String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                    if (decodedData.startsWith("api_tab_id")) {
                        api_tab_id = Integer.valueOf(decodedData.replace("api_tab_id=", ""));
                        break;
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(getApplicationContext())) {
                    // Can not draw overlays: pass
                } else if (jsonDataObj.has("api_data")) {
                    JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                    KcaQuestViewService.setApiData(api_data);
                    dbHelper.updateQuestCheck(api_tab_id, api_data);
                    startService(new Intent(getBaseContext(), KcaQuestViewService.class)
                            .setAction(REFRESH_QUESTVIEW_ACTION).putExtra("tab_id", api_tab_id));
                    if (dbHelper.checkQuestListValid()) QSyncWrite();
                }

                sendQuestCompletionInfo();
                return;
            }

            if (url.startsWith(API_REQ_QUEST_CLEARITEMGET)) {
                int quest_id = 0;
                String[] requestData = request.split("&");
                for (int i = 0; i < requestData.length; i++) {
                    String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                    if (decodedData.startsWith("api_quest_id")) {
                        quest_id = Integer.valueOf(decodedData.replace("api_quest_id=", ""));
                        break;
                    }
                }
                Log.e("KCA", "clear " + String.valueOf(quest_id));
                dbHelper.removeQuest(quest_id);
                questTracker.removeQuestTrack(quest_id, true);
                if (quest_id == 212 || quest_id == 218) questTracker.clearApDupFlag();
                sendQuestCompletionInfo();
                return;
            }

            if (API_WIDGET_TU_REQS.contains(url)) {
                Intent widgetUpdateIndent = new Intent(getApplicationContext(), KcaTimerWidget.class);
                widgetUpdateIndent.setAction(WIDGET_DATA_UPDATE);
                sendBroadcast(widgetUpdateIndent);
            }

            // Game Data Dependent Tasks
            if (restartFlag) {
                makeToast(getStringWithLocale(R.string.kca_toast_restart_at_kcanotify), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            } else if (!checkDataLoadTriggered()) {
                if (!api_start2_loading_flag) {
                    makeToast(getStringWithLocale(R.string.kca_toast_get_data_at_settings), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    //new retrieveApiStartData().execute("", "down", "");
                }
            } else if (api_start2_loading_flag) {
                makeToast(getStringWithLocale(R.string.kca_toast_loading_data), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            } else {
                if (url.startsWith(API_PORT)) {
                    KcaFleetViewService.setReadyFlag(true);
                    heavyDamagedMode = HD_NONE;
                    currentNodeInfo = "";
                    KcaBattle.currentFleet = -1;
                    KcaApiData.resetShipCountInBattle();
                    KcaApiData.resetItemCountInBattle();
                    Log.e("KCA", "Port Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject reqPortApiData = jsonDataObj.getAsJsonObject("api_data");
                        int size = KcaApiData.getPortData(reqPortApiData);

                        if (reqPortApiData.has("api_combined_flag")) {
                            int combined_flag = reqPortApiData.get("api_combined_flag").getAsInt();
                            isCombined = (combined_flag > 0);
                            KcaBattle.isCombined = isCombined;
                            KcaBattle.cleanEscapeList();
                        }



                        //Log.e("KCA", "Total Ships: " + String.valueOf(size));
                        if (reqPortApiData.has("api_deck_port")) {
                            processExpeditionInfo();
                            JsonArray portdeckdata = reqPortApiData.getAsJsonArray("api_deck_port");
                            KcaMoraleInfo.setDeckCount(portdeckdata.size());
                            for (int i = 0; i < portdeckdata.size(); i++) {
                                boolean result = KcaMoraleInfo.setMoraleValue(i, deckInfoCalc.checkMinimumMorale(portdeckdata, i), isInBattle, false);
                                processMoraleInfo(i, portdeckdata, result);
                            }

                            JsonArray akashi_flagship_deck = deckInfoCalc.checkAkashiFlagship(portdeckdata);
                            boolean is_init_state = KcaAkashiRepairInfo.getAkashiTimerValue() < 0;
                            boolean over_20min = KcaAkashiRepairInfo.getAkashiElapsedTimeInSecond() >= AKASHI_TIMER_20MIN;
                            if (akashi_flagship_deck.size() > 0) {
                                if (is_init_state || over_20min) {
                                    KcaAkashiRepairInfo.setAkashiTimer();
                                    processAkashiTimerInfo();
                                    isAkashiTimerNotiWait = true;
                                }
                            } else {
                                if (!is_init_state && over_20min) {
                                    KcaAkashiRepairInfo.initAkashiTimer();
                                    processAkashiTimerInfo();
                                    isAkashiTimerNotiWait = false;
                                }
                            }
                            KcaAkashiRepairInfo.setAkashiExist(akashi_flagship_deck.size() > 0);
                            updateFleetView();
                        }
                    }
                    runTimer();
                    updateFleetView();
                    isInBattle = false;
                }

                if (url.startsWith(API_REQ_MAP_SELECT_EVENTMAP_RANK)) {
                    String[] requestData = request.split("&");
                    int mapno = 0;
                    int rank = 0;
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_map_no")) {
                            mapno = Integer.valueOf(decodedData.replace("api_map_no=", ""));
                            break;
                        } else if (decodedData.startsWith("api_rank")) {
                            rank = Integer.valueOf(decodedData.replace("api_rank=", ""));
                            break;
                        }
                    }
                    KcaApiData.setEventMapDifficulty(mapno, rank);
                    if (KcaMapHpPopupService.isActive()) {
                        Intent qintent = new Intent(this, KcaMapHpPopupService.class);
                        qintent.setAction(KcaMapHpPopupService.MAPHP_RESET_ACTION);
                        startService(qintent);
                    }
                }

                if (url.startsWith(API_GET_MEMBER_MAPINFO) || url.startsWith(API_GET_MEMBER_MISSION)) {
                    if (url.startsWith(API_GET_MEMBER_MAPINFO) && jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        JsonArray api_map_info = api_data.getAsJsonArray("api_map_info");
                        dbHelper.putValue(DB_KEY_APIMAPINFO, api_map_info.toString());
                        if (KcaMapHpPopupService.isActive()) {
                            Intent qintent = new Intent(getBaseContext(), KcaMapHpPopupService.class);
                            qintent.setAction(KcaMapHpPopupService.MAPHP_SHOW_ACTION);
                            startService(qintent);
                        }
                        int eventMapCount = 0;
                        for (JsonElement map : api_map_info) {
                            JsonObject mapData = map.getAsJsonObject();
                            if (mapData.has("api_eventmap")) {
                                eventMapCount += 1;
                                JsonObject eventData = mapData.getAsJsonObject("api_eventmap");
                                if (eventData.has("api_selected_rank"))
                                    KcaApiData.setEventMapDifficulty(eventMapCount, eventData.get("api_selected_rank").getAsInt());
                            }
                        }

                        if (api_data.has("api_air_base") && api_data.get("api_air_base").isJsonArray()) {
                            JsonArray api_airbase_info = api_data.getAsJsonArray("api_air_base");
                            dbHelper.putValue(DB_KEY_LABSIFNO, api_airbase_info.toString());
                            updateAirbasePopupInfo();
                        } else {
                            JsonArray api_airbase_info = new JsonArray();
                            dbHelper.putValue(DB_KEY_LABSIFNO, api_airbase_info.toString());
                            updateAirbasePopupInfo();
                        }
                    }

                    // Notification Part
                    String message = "";
                    boolean isHeavyDamagedFlag = false;
                    boolean isNotSuppliedFlag = false;
                    JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                    if (isCurrentPortDeckDataReady()) {
                        for (int i = 0; i < portdeckdata.size(); i++) {
                            if (url.startsWith(API_GET_MEMBER_MISSION) && i == 0) continue;
                            if (deckInfoCalc.checkNotSuppliedExist(portdeckdata, i)) {
                                isNotSuppliedFlag = true;
                                message = message.concat(KcaUtils.format(getStringWithLocale(R.string.not_supplied), i + 1)).concat("\n");
                            }
                        }

                        if (url.startsWith(API_GET_MEMBER_MAPINFO)) {
                            for (int i = 0; i < portdeckdata.size(); i++) {
                                int checkvalue = deckInfoCalc.checkHeavyDamageExist(portdeckdata, i);
                                switch (checkvalue) {
                                    case HD_DAMECON:
                                    case HD_DANGER:
                                        isHeavyDamagedFlag = true;
                                        if (checkvalue == HD_DANGER) {
                                            message = message.concat(KcaUtils.format("[#%d] %s", i + 1, getStringWithLocale(R.string.heavy_damaged))).concat("\n");
                                        } else if (checkvalue == HD_DAMECON) {
                                            message = message.concat(KcaUtils.format("[#%d] %s", i + 1, getStringWithLocale(R.string.heavy_damaged_damecon))).concat("\n");
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }

                        if (message.length() > 0) {
                            boolean hcondition = (isHeavyDamagedFlag && isHDVibrateEnabled());
                            boolean ncondition = (isNotSuppliedFlag && isNSVibrateEnabled());
                            if (hcondition || ncondition) {
                                String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
                                if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                                    if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                        Uri notificationUri = KcaUtils.getContentUri(getApplicationContext(),
                                                Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
                                        Log.e("KCA", getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
                                        KcaUtils.playNotificationSound(mediaPlayer, getApplicationContext(), notificationUri);
                                    }
                                }
                                doVibrate(vibrator, 1000);
                            }

                            int toastColor;
                            if (hcondition) toastColor = R.color.colorHeavyDmgStatePanel;
                            else toastColor = R.color.colorWarningPanel;
                            makeToast(message.trim(), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), toastColor));
                        }
                    }
                }

                if (url.startsWith(API_GET_MEMBER_MAPINFO) || url.startsWith(API_GET_MEMBER_PRACTICE)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && !Settings.canDrawOverlays(getApplicationContext())) {
                        // Can not draw overlays: pass
                    } else {
                        Intent qintent = new Intent(this, KcaBattleViewService.class);
                        startService(qintent);
                    }
                }

                if (API_BATTLE_REQS.contains(url) && isCurrentPortDeckDataReady()) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject battleApiData = jsonDataObj.getAsJsonObject("api_data");
                        if (url.equals(API_REQ_MAP_START) || url.equals(API_REQ_PRACTICE_BATTLE)) {
                            JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                            isInBattle = true;

                            int deck_id = -1;
                            String[] requestData = request.split("&");
                            for (int i = 0; i < requestData.length; i++) {
                                String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                                if (decodedData.startsWith("api_deck_id")) {
                                    deck_id = Integer.valueOf(decodedData.replace("api_deck_id=", "")) - 1;
                                    break;
                                }
                            }
                            if (deck_id != -1) {
                                KcaBattle.currentFleet = deck_id;
                            }

                            JsonObject api_data = new JsonObject();
                            JsonArray api_deck_data = new JsonArray();
                            JsonArray api_ship_data = new JsonArray();
                            int checkvalue = 0;

                            if (deck_id == 0 && isCombined) {
                                JsonObject first = portdeckdata.get(0).getAsJsonObject();
                                JsonObject second = portdeckdata.get(1).getAsJsonObject();

                                api_deck_data.add(first);
                                JsonArray firstShipInfo = first.getAsJsonArray("api_ship");
                                for (JsonElement e : firstShipInfo) {
                                    int ship_id = e.getAsInt();
                                    if (ship_id != -1)
                                        api_ship_data.add(KcaApiData.getUserShipDataById(ship_id, "all"));
                                }
                                api_deck_data.add(second);
                                JsonArray secondShipInfo = second.getAsJsonArray("api_ship");
                                for (JsonElement e : secondShipInfo) {
                                    int ship_id = e.getAsInt();
                                    if (ship_id != -1)
                                        api_ship_data.add(KcaApiData.getUserShipDataById(ship_id, "all"));
                                }

                                KcaBattle.checkhdmgflag = deckInfoCalc.getHeavyDmgCheckStatus(api_deck_data, 0);
                                KcaBattle.checkhdmgcbflag = deckInfoCalc.getHeavyDmgCheckStatus(api_deck_data, 1);
                                KcaBattle.dameconflag = deckInfoCalc.getDameconStatus(api_deck_data, 0);
                                KcaBattle.dameconcbflag = deckInfoCalc.getDameconStatus(api_deck_data, 1);

                                int firstHeavyDamaged = deckInfoCalc.checkHeavyDamageExist(portdeckdata, 0);
                                int secondHeavyDamaged = 0;
                                if (portdeckdata.size() >= 2) {
                                    secondHeavyDamaged = deckInfoCalc.checkHeavyDamageExist(portdeckdata, 1);
                                }
                                checkvalue = Math.max(firstHeavyDamaged, secondHeavyDamaged);
                            } else {
                                JsonObject fleet = portdeckdata.get(deck_id).getAsJsonObject();
                                int fleetHeavyDamaged = deckInfoCalc.checkHeavyDamageExist(portdeckdata, deck_id);
                                checkvalue = fleetHeavyDamaged;
                                api_deck_data.add(fleet);
                                JsonArray firstShipInfo = fleet.getAsJsonArray("api_ship");
                                for (JsonElement e : firstShipInfo) {
                                    int ship_id = e.getAsInt();
                                    if (ship_id != -1)
                                        api_ship_data.add(KcaApiData.getUserShipDataById(ship_id, "all"));
                                }
                                KcaBattle.checkhdmgflag = deckInfoCalc.getHeavyDmgCheckStatus(api_deck_data, 0);
                                KcaBattle.dameconflag = deckInfoCalc.getDameconStatus(api_deck_data, 0);
                            }
                            api_data.add("api_deck_data", api_deck_data);
                            api_data.add("api_ship_data", api_ship_data);
                            KcaBattle.setDeckPortData(api_data);
                            KcaBattle.setStartHeavyDamageExist(checkvalue);

                            if (isBattleViewEnabled()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                        && !Settings.canDrawOverlays(getApplicationContext())) {
                                    // Can not draw overlays: pass
                                } else {
                                    startService(new Intent(this, KcaViewButtonService.class)
                                            .setAction(KcaViewButtonService.ACTIVATE_BATTLEVIEW_ACTION));
                                    startService(new Intent(this, KcaBattleViewService.class));
                                }
                            }
                        }
                        KcaBattle.processData(dbHelper, url, battleApiData);
                    } else if (url.equals(API_REQ_COMBINED_GOBACKPORT) || url.equals(API_REQ_SORTIE_GOBACKPORT)) {
                        KcaBattle.processData(dbHelper, url, null);
                    }
                    updateFleetView();
                }

                if (url.startsWith(API_GET_MEMBER_SHIP_DECK) && isCurrentPortDeckDataReady()) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        JsonArray api_deck_data = (JsonArray) api_data.get("api_deck_data");
                        KcaApiData.updatePortDataOnBattle(api_data);
                        for (int i = 0; i < api_deck_data.size(); i++) {
                            if (i == 0) {
                                KcaBattle.dameconflag = deckInfoCalc.getDameconStatus(api_deck_data, 0);
                            } else if (i == 1) {
                                KcaBattle.dameconcbflag = deckInfoCalc.getDameconStatus(api_deck_data, 1);
                            }
                        }
                        KcaBattle.setDeckPortData(api_data);

                        updateFleetView();
                    }
                }

                if (url.startsWith(API_REQ_MISSION_RETURN)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject reqGetMemberDeckApiData = jsonDataObj.getAsJsonObject("api_data");
                        cancelExpeditionInfo(reqGetMemberDeckApiData);
                    }
                }

                if (url.startsWith(API_REQ_MEMBER_GET_PRACTICE_ENEMYINFO)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        KcaBattle.currentEnemyDeckName = api_data.get("api_deckname").getAsString();
                    }
                }

                if (url.startsWith(API_REQ_HOKYU_CHARGE)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        KcaApiData.updateSuppliedUserShip(api_data.getAsJsonArray("api_ship"));
                    }
                }

                if (url.startsWith(API_GET_MEMBER_NDOCK)) {
                    processDockingInfo();
                    updateFleetView();
                }

                if (url.startsWith(API_REQ_NYUKYO_SPEEDCHAGNE)) {
                    int ndock_id = -1;
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_ndock_id")) {
                            ndock_id = Integer.valueOf(decodedData.replace("api_ndock_id=", "")) - 1;
                            break;
                        }
                    }
                    if (ndock_id != -1) processDockingSpeedup(ndock_id);
                    updateFleetView();
                }

                if (url.startsWith(API_REQ_NYUKYO_START)) {
                    questTracker.updateIdCountTracker("503");
                    updateQuestView();

                    int ship_id = -1;
                    int highspeed = -1;
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_ship_id")) {
                            ship_id = Integer.valueOf(decodedData.replace("api_ship_id=", ""));
                        } else if (decodedData.startsWith("api_highspeed")) {
                            highspeed = Integer.valueOf(decodedData.replace("api_highspeed=", ""));
                        }
                    }
                    KcaApiData.updateShipMorale(ship_id);
                    JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                    int check_in_deck = deckInfoCalc.checkShipInDeck(portdeckdata, ship_id);
                    if (check_in_deck != -1) {
                        boolean result = setMoraleValue(check_in_deck, deckInfoCalc.checkMinimumMorale(portdeckdata, check_in_deck), false, false);
                        processMoraleInfo(check_in_deck, portdeckdata, result);
                    }
                    if (highspeed > 0) KcaApiData.updateShipHpFull(ship_id);
                    updateFleetView();
                }

                if (url.startsWith(API_REQ_AIR_CORPS_SETPLANE)) {
                    int target_area_id = 0;
                    int target_base_id = 0;
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_area_id")) {
                            target_area_id = Integer.valueOf(decodedData.replace("api_area_id=", ""));
                        } else if (decodedData.startsWith("api_base_id")) {
                            target_base_id = Integer.valueOf(decodedData.replace("api_base_id=", ""));
                        }
                    }

                    int new_distance = -1;
                    JsonObject distance_data = new JsonObject();
                    JsonArray api_plane_info = new JsonArray();
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        api_plane_info = api_data.getAsJsonArray("api_plane_info");
                        distance_data = api_data.getAsJsonObject("api_distance");
                    }

                    JsonArray airbase_data = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);

                    if (airbase_data != null) {
                        for (int i = 0; i < airbase_data.size(); i++) {
                            JsonObject airbase_item = airbase_data.get(i).getAsJsonObject();
                            int area_id = airbase_item.get("api_area_id").getAsInt();
                            int rid = airbase_item.get("api_rid").getAsInt();
                            if (area_id == target_area_id && rid == target_base_id) {
                                airbase_item.add("api_distance", distance_data);
                                JsonArray airbase_plane_info = airbase_item.getAsJsonArray("api_plane_info");
                                for (int j = 0; j < api_plane_info.size(); j++) {
                                    JsonObject plane_item = api_plane_info.get(j).getAsJsonObject();
                                    int plane_item_id = plane_item.get("api_squadron_id").getAsInt();
                                    for (int k = 0; k < airbase_plane_info.size(); k++) {
                                        JsonObject target_plane_item = airbase_plane_info.get(k).getAsJsonObject();
                                        if (target_plane_item.get("api_squadron_id").getAsInt() == plane_item_id) {
                                            airbase_plane_info.set(k, plane_item);
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        dbHelper.putValue(DB_KEY_LABSIFNO, airbase_data.toString());
                        updateAirbasePopupInfo();
                    }
                }

                if (url.startsWith(API_REQ_AIR_CORPS_CHANGENAME)) {
                    int target_area_id = 0;
                    int target_base_id = 0;
                    String new_name = "";
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_area_id")) {
                            target_area_id = Integer.valueOf(decodedData.replace("api_area_id=", ""));
                        } else if (decodedData.startsWith("api_base_id")) {
                            target_base_id = Integer.valueOf(decodedData.replace("api_base_id=", ""));
                        } else if (decodedData.startsWith("api_name")) {
                            new_name = decodedData.replace("api_name=", "");
                        }
                    }

                    JsonArray airbase_data = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
                    if (airbase_data != null) {
                        for (int i = 0; i < airbase_data.size(); i++) {
                            JsonObject airbase_item = airbase_data.get(i).getAsJsonObject();
                            int area_id = airbase_item.get("api_area_id").getAsInt();
                            int rid = airbase_item.get("api_rid").getAsInt();
                            if (area_id == target_area_id && rid == target_base_id) {
                                airbase_item.addProperty("api_name", new_name);
                                break;
                            }
                        }
                        dbHelper.putValue(DB_KEY_LABSIFNO, airbase_data.toString());
                        updateAirbasePopupInfo();
                    }
                }

                if (url.startsWith(API_REQ_AIR_CORPS_SETACTION)) {
                    int target_area_id = 0;
                    String target_base_id = "";
                    String new_action_kind = "";
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_area_id")) {
                            target_area_id = Integer.valueOf(decodedData.replace("api_area_id=", ""));
                        } else if (decodedData.startsWith("api_base_id")) {
                            target_base_id = decodedData.replace("api_base_id=", "");
                        } else if (decodedData.startsWith("api_action_kind")) {
                            new_action_kind = decodedData.replace("api_action_kind=", "");
                        }
                    }
                    String[] target_base_list = target_base_id.split(",");
                    String[] new_action_list = new_action_kind.split(",");

                    JsonArray airbase_data = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
                    if (airbase_data != null) {
                        for (int i = 0; i < airbase_data.size(); i++) {
                            JsonObject airbase_item = airbase_data.get(i).getAsJsonObject();
                            int area_id = airbase_item.get("api_area_id").getAsInt();
                            int rid = airbase_item.get("api_rid").getAsInt();
                            for (int j = 0; j < target_base_list.length; j++) {
                                int target_base = Integer.parseInt(target_base_list[j]);
                                if (area_id == target_area_id && rid == target_base) {
                                    airbase_item.addProperty("api_action_kind", Integer.parseInt(new_action_list[j]));
                                    break;
                                }
                            }
                        }
                        dbHelper.putValue(DB_KEY_LABSIFNO, airbase_data.toString());
                        updateAirbasePopupInfo();
                    }
                }

                if (url.startsWith(API_REQ_AIR_CORPS_SUPPLY)) {
                    int target_area_id = 0;
                    int target_base_id = 0;
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_area_id")) {
                            target_area_id = Integer.valueOf(decodedData.replace("api_area_id=", ""));
                        } else if (decodedData.startsWith("api_base_id")) {
                            target_base_id = Integer.valueOf(decodedData.replace("api_base_id=", ""));
                        }
                    }

                    JsonArray api_plane_info = new JsonArray();
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        api_plane_info = api_data.getAsJsonArray("api_plane_info");
                    }

                    JsonArray airbase_data = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
                    if (airbase_data != null) {
                        for (int i = 0; i < airbase_data.size(); i++) {
                            JsonObject airbase_item = airbase_data.get(i).getAsJsonObject();
                            int area_id = airbase_item.get("api_area_id").getAsInt();
                            int rid = airbase_item.get("api_rid").getAsInt();
                            if (area_id == target_area_id && rid == target_base_id) {
                                JsonArray airbase_plane_info = airbase_item.getAsJsonArray("api_plane_info");
                                for (int j = 0; j < api_plane_info.size(); j++) {
                                    JsonObject plane_item = api_plane_info.get(j).getAsJsonObject();
                                    int plane_item_id = plane_item.get("api_squadron_id").getAsInt();
                                    for (int k = 0; k < airbase_plane_info.size(); k++) {
                                        JsonObject target_plane_item = airbase_plane_info.get(k).getAsJsonObject();
                                        if (target_plane_item.get("api_squadron_id").getAsInt() == plane_item_id) {
                                            airbase_plane_info.set(k, plane_item);
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        dbHelper.putValue(DB_KEY_LABSIFNO, airbase_data.toString());
                        updateAirbasePopupInfo();
                    }
                }

                if (url.startsWith(API_REQ_AIR_CORPS_EXPANDBASE)) {
                    JsonObject api_base_info = new JsonObject();
                    if (jsonDataObj.has("api_data")) {
                        api_base_info = jsonDataObj.getAsJsonObject("api_data");
                    }

                    JsonArray airbase_data = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
                    if (airbase_data != null) {
                        airbase_data.add(api_base_info);
                        dbHelper.putValue(DB_KEY_LABSIFNO, airbase_data.toString());
                        updateAirbasePopupInfo();
                    }
                }

                if (isCurrentPortDeckDataReady()) {
                    if (url.startsWith(API_REQ_KOUSYOU_CREATEITEM)) {
                        String[] requestData = request.split("&");
                        int[] materials = {0, 0, 0, 0};
                        JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                        int flagship = deckInfoCalc.getKcShipList(portdeckdata, 0)[0];
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_item1")) {
                                materials[0] = Integer.valueOf(decodedData.replace("api_item1=", ""));
                            }
                            if (decodedData.startsWith("api_item2")) {
                                materials[1] = Integer.valueOf(decodedData.replace("api_item2=", ""));
                            }
                            if (decodedData.startsWith("api_item3")) {
                                materials[2] = Integer.valueOf(decodedData.replace("api_item3=", ""));
                            }
                            if (decodedData.startsWith("api_item4")) {
                                materials[3] = Integer.valueOf(decodedData.replace("api_item4=", ""));
                            }
                        }

                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            boolean createFlag = api_data.get("api_create_flag").getAsInt() == 1;
                            int itemKcId = KcaApiData.updateSlotItemData(api_data);
                            int itemFailKcId = -1;
                            if (api_data.has("api_fdata")) {
                                String[] fdata = api_data.get("api_fdata").getAsString().split(",");
                                itemFailKcId = Integer.parseInt(fdata[1]);
                            }

                            if (isOpenDBEnabled()) KcaOpenDBAPI.sendEquipDevData(flagship, materials[0], materials[1], materials[2], materials[3], itemKcId);
                            if (isPoiDBEnabled()) KcaPoiDBAPI.sendEquipDevData(Arrays.toString(materials), flagship, createFlag ? itemKcId : itemFailKcId, getAdmiralLevel(), createFlag);

                            questTracker.updateIdCountTracker("605");
                            questTracker.updateIdCountTracker("607");
                            updateQuestView();

                            JsonArray material_data = api_data.getAsJsonArray("api_material");
                            recordResourceLog(material_data, false);

                            String itemname = "";
                            int itemtype = 0;
                            String itemcount = "";

                            if (createFlag) {
                                JsonObject itemData = KcaApiData.getKcItemStatusById(itemKcId, "name,type");
                                itemname = itemData.get("name").getAsString();
                                itemtype = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                                itemcount = KcaUtils.format("(%d)", KcaApiData.getItemCountByKcId(itemKcId));
                            } else {
                                itemname = "item_fail";
                                itemtype = 999;
                            }

                            JsonObject shipData = KcaApiData.getKcShipDataById(flagship, "name");
                            String shipname = shipData.get("name").getAsString();

                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String timetext = dateFormat.format(new Date());

                            JsonObject equipdevdata = new JsonObject();
                            equipdevdata.addProperty("flagship", shipname);
                            equipdevdata.addProperty("name", itemname);
                            equipdevdata.addProperty("type", itemtype);
                            equipdevdata.addProperty("count", itemcount);
                            equipdevdata.addProperty("time", timetext);

                            dbHelper.putValue(DB_KEY_LATESTDEV, equipdevdata.toString());

                            if (KcaDevelopPopupService.isActive()) {
                                Intent qintent = new Intent(getBaseContext(), KcaDevelopPopupService.class);
                                qintent.setAction(KcaDevelopPopupService.DEV_DATA_ACTION);
                                qintent.putExtra("data", equipdevdata.toString());
                                startService(qintent);
                            }
                        }
                    }

                    if (url.startsWith(API_REQ_KOUSYOU_DESTROYITEM)) {
                        String[] requestData = request.split("&");
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_slotitem_ids")) {
                                String itemlist = decodedData.replace("api_slotitem_ids=", "");
                                String[] itemlist_array = itemlist.split(",");
                                for (String item : itemlist_array) {
                                    JsonObject status = getUserItemStatusById(Integer.parseInt(item), "lv,alv", "id,type");
                                    if (status != null && status.has("id")) {
                                        int item_id = status.get("id").getAsInt();
                                        switch (item_id) {
                                            case 19: // Type 96 Fighter
                                                questTracker.updateIdCountTracker("678", 0);
                                            case 20: // Type 0 Model 21
                                                questTracker.updateIdCountTracker("678", 1);
                                            default:
                                                break;
                                        }
                                    }
                                    if (status != null && status.has("type")) {
                                        switch (status.getAsJsonArray("type").get(2).getAsInt()) {
                                            case T2_GUN_SMALL:
                                                questTracker.updateIdCountTracker("673");
                                                break;
                                            case T2_GUN_MEDIUM:
                                                questTracker.updateIdCountTracker("676", 0);
                                                break;
                                            case T2_SUB_GUN:
                                                questTracker.updateIdCountTracker("676", 1);
                                                break;
                                            case T2_TORPEDO:
                                                questTracker.updateIdCountTracker("677", 2);
                                                break;
                                            case T2_FIGHTER:
                                                questTracker.updateIdCountTracker("675", 0);
                                                break;
                                            case T2_SEA_SCOUT:
                                                questTracker.updateIdCountTracker("677", 1);
                                                break;
                                            case T2_MACHINE_GUN:
                                                questTracker.updateIdCountTracker("638");
                                                questTracker.updateIdCountTracker("674");
                                                questTracker.updateIdCountTracker("675", 1);
                                                questTracker.updateIdCountTracker("680", 0);
                                                break;
                                            case T2_RADAR_SMALL:
                                            case T2_RADAR_LARGE:
                                            case T2_RADER_LARGE_II:
                                                questTracker.updateIdCountTracker("680", 1);
                                                break;
                                            case T2_GUN_LARGE:
                                            case T2_GUN_LARGE_II:
                                                questTracker.updateIdCountTracker("663");
                                                questTracker.updateIdCountTracker("677", 0);
                                                break;
                                            case T2_DRUM_CAN:
                                                questTracker.updateIdCountTracker("676", 2);
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }
                                KcaApiData.removeSlotItemData(itemlist);
                                break;
                            }
                        }
                        questTracker.updateIdCountTracker("613");
                        updateQuestView();
                        updateFleetView();
                    }

                    if (url.equals(API_REQ_KOUSYOU_CREATESHIP)) {
                        String[] requestData = request.split("&");
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_kdock_id=")) {
                                checkKdockId = Integer.valueOf(decodedData.replace("api_kdock_id=", "")) - 1;
                            } else if (decodedData.startsWith("api_highspeed=")) {
                                checkHighSpeed = Integer.valueOf(decodedData.replace("api_highspeed=", ""));
                            } else if (decodedData.startsWith("api_large_flag=")) {
                                checkLargeFlag = Integer.valueOf(decodedData.replace("api_large_flag=", ""));
                            }
                        }
                        questTracker.updateIdCountTracker("606");
                        questTracker.updateIdCountTracker("608");
                        updateQuestView();
                    }

                    if (url.equals(API_REQ_KOUSYOU_CREATESHIP_SPEEDCHANGE)) {
                        int kdockid = -1;
                        String[] requestData = request.split("&");
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_kdock_id=")) {
                                kdockid = Integer.valueOf(decodedData.replace("api_kdock_id=", "")) - 1;
                                break;
                            }
                        }
                        Log.e("KCA-S", "" + kdockid);
                        if (kdockid > -1) {
                            JsonArray kdata = dbHelper.getJsonArrayValue(DB_KEY_KDOCKDATA);
                            if (kdata != null) {
                                JsonObject item = kdata.get(kdockid).getAsJsonObject();
                                item.addProperty("api_complete_time", 0);
                                kdata.set(kdockid, item);
                            }
                            dbHelper.putValue(DB_KEY_KDOCKDATA, kdata.toString());
                        }
                    }

                    if (url.startsWith(API_REQ_KOUSYOU_GETSHIP)) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            KcaApiData.addUserShip(api_data);
                            JsonArray api_kdock = api_data.getAsJsonArray("api_kdock");
                            dbHelper.putValue(DB_KEY_KDOCKDATA, api_kdock.toString());
                        }
                    }

                    if (url.startsWith(API_REQ_KAISOU_SLOTSET)) {
                        kaisouProcessFlag = true;
                    }

                    if (url.startsWith(API_REQ_KAISOU_SLOTSET_EX)) {
                        kaisouProcessFlag = true;
                    }

                    if (url.startsWith(API_REQ_KAISOU_UNSLOTSET_ALL)) {
                        kaisouProcessFlag = true;
                    }

                    if (url.startsWith(API_GET_MEMBER_KDOCK)) {
                        Log.e("KCA", String.valueOf(checkKdockId));
                        if (jsonDataObj.has("api_data")) {
                            JsonArray api_data = jsonDataObj.getAsJsonArray("api_data");
                            dbHelper.putValue(DB_KEY_KDOCKDATA, api_data.toString());
                            if (checkKdockId != -1) {
                                JsonObject api_kdock_item = api_data.get(checkKdockId).getAsJsonObject();
                                JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                                int flagship = deckInfoCalc.getKcShipList(portdeckdata, 0)[0];
                                int[] materials = {0, 0, 0, 0, 0};
                                for (int i = 0; i < materials.length; i++) {
                                    materials[i] = api_kdock_item.get(KcaUtils.format("api_item%d", i + 1)).getAsInt();
                                }
                                int created_ship_id = api_kdock_item.get("api_created_ship_id").getAsInt();
                                if (isOpenDBEnabled()) KcaOpenDBAPI.sendShipDevData(flagship, materials[0], materials[1], materials[2], materials[3], materials[4], created_ship_id);
                                if (isPoiDBEnabled()) KcaPoiDBAPI.sendShipDevData(Arrays.toString(materials), checkKdockId, flagship, created_ship_id, checkHighSpeed, getAdmiralLevel(), checkLargeFlag);

                                checkKdockId = -1;
                                checkLargeFlag = -1;
                                checkHighSpeed = -1;
                            }
                        }
                    }

                    if (url.startsWith(API_REQ_KOUSYOU_DESTROYSHIP)) {
                        String targetShip = "";
                        int slotDestFlag = 0;
                        String[] requestData = request.split("&");
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_ship_id")) {
                                targetShip = decodedData.replace("api_ship_id=", "");
                            } else if (decodedData.startsWith("api_slot_dest_flag")) {
                                slotDestFlag = Integer.parseInt(decodedData.replace("api_slot_dest_flag=", ""));
                            }
                        }
                        KcaApiData.deleteUserShip(targetShip, slotDestFlag);
                        String[] targetShipList = targetShip.split(",");

                        JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                        for (int i = 0; i < portdeckdata.size(); i++) {
                            JsonObject deckData = portdeckdata.get(i).getAsJsonObject();
                            JsonArray deckShipData = deckData.get("api_ship").getAsJsonArray();
                            for (int j = 0; j < deckShipData.size(); j++) {
                                for (int k = 0; k < targetShipList.length; k++) {
                                    if (targetShipList[k].equals(String.valueOf(deckShipData.get(j).getAsInt()))) {
                                        deckShipData.set(j, new JsonPrimitive(-1));
                                        deckData.add("api_ship", deckShipData);
                                        portdeckdata.set(i, deckData);
                                    }
                                }
                            }
                        }
                        dbHelper.putValue(DB_KEY_DECKPORT, portdeckdata.toString());
                        for (int i = 0; i < targetShipList.length; i++) {
                            questTracker.updateIdCountTracker("609");
                        }
                        updateQuestView();
                        updateFleetView();
                    }

                    if (url.startsWith(API_REQ_HENSEI_CHANGE)) {
                        String[] requestData = request.split("&");
                        int deckIdx = -1;
                        int shipIdx = -1;
                        int shipId = -3;

                        int originalDeckIdx = -1;
                        int originalShipIdx = -1;

                        boolean in_change = false;
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_ship_idx=")) {
                                shipIdx = Integer.valueOf(decodedData.replace("api_ship_idx=", ""));
                            } else if (decodedData.startsWith("api_ship_id=")) {
                                shipId = Integer.valueOf(decodedData.replace("api_ship_id=", ""));
                            } else if (decodedData.startsWith("api_id=")) {
                                deckIdx = Integer.valueOf(decodedData.replace("api_id=", "")) - 1;
                            }
                        }
                        if (deckIdx != -1) {
                            JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                            JsonObject targetDeckIdxData = portdeckdata.get(deckIdx).getAsJsonObject();
                            JsonArray targetDeckIdxShipIdata = targetDeckIdxData.get("api_ship").getAsJsonArray();

                            if (shipId == -2) {
                                in_change = true;
                                for (int i = 1; i < targetDeckIdxShipIdata.size(); i++) {
                                    targetDeckIdxShipIdata.set(i, new JsonPrimitive(-1));
                                }
                            } else if (shipId == -1) { // remove ship
                                in_change = true;
                                targetDeckIdxShipIdata.remove(shipIdx);
                                targetDeckIdxShipIdata.add(new JsonPrimitive(-1));
                            } else { // add ship
                                // check whether target ship is in deck
                                for (int i = 0; i < portdeckdata.size(); i++) {
                                    JsonArray deckData = portdeckdata.get(i).getAsJsonObject().get("api_ship").getAsJsonArray();
                                    for (int j = 0; j < deckData.size(); j++) {
                                        if (shipId == deckData.get(j).getAsInt()) {
                                            originalDeckIdx = i;
                                            originalShipIdx = j;
                                            break;
                                        }
                                    }
                                }
                                if (originalDeckIdx != -1) { // if in deck
                                    JsonObject sourceDeckIdxData = portdeckdata.get(originalDeckIdx).getAsJsonObject();
                                    JsonArray sourceDeckIdxShipIdata = sourceDeckIdxData.get("api_ship").getAsJsonArray();
                                    JsonElement replacement = targetDeckIdxShipIdata.get(shipIdx);
                                    if (replacement.getAsInt() != -1) {
                                        sourceDeckIdxShipIdata.set(originalShipIdx, replacement);
                                    } else {
                                        sourceDeckIdxShipIdata.remove(originalShipIdx);
                                        sourceDeckIdxShipIdata.add(new JsonPrimitive(-1));
                                        sourceDeckIdxData.add("api_ship", sourceDeckIdxShipIdata);
                                        portdeckdata.set(originalDeckIdx, sourceDeckIdxData);
                                    }
                                }
                                targetDeckIdxShipIdata.set(shipIdx, new JsonPrimitive(shipId)); // replace
                            }
                            targetDeckIdxData.add("api_ship", targetDeckIdxShipIdata);
                            portdeckdata.set(deckIdx, targetDeckIdxData);
                            dbHelper.putValue(DB_KEY_DECKPORT, portdeckdata.toString());

                            for (int i = 0; i < portdeckdata.size(); i++) {
                                boolean result = setMoraleValue(i, deckInfoCalc.checkMinimumMorale(portdeckdata, i), false, in_change);
                                processMoraleInfo(i, portdeckdata, result);
                            }
                            JsonArray akashi_flagship_deck = deckInfoCalc.checkAkashiFlagship(portdeckdata);
                            boolean akashi_nochange_flag = true;
                            for (int i = 0; i < akashi_flagship_deck.size(); i++) {
                                int deckid = akashi_flagship_deck.get(i).getAsInt();
                                if (deckid == deckIdx || deckid == originalDeckIdx) {
                                    akashi_nochange_flag = false;
                                    break;
                                }
                            }
                            KcaAkashiRepairInfo.setAkashiExist(akashi_flagship_deck.size() > 0);
                            if (!akashi_nochange_flag) {
                                KcaAkashiRepairInfo.setAkashiTimer();
                                processAkashiTimerInfo();
                                isAkashiTimerNotiWait = true;
                            }
                        }
                        updateFleetView();
                    }

                    if (url.startsWith(API_REQ_HENSEI_PRESET) && isCurrentPortDeckDataReady()) {
                        String[] requestData = request.split("&");
                        int deckIdx = -1;
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_deck_id=")) {
                                deckIdx = Integer.valueOf(decodedData.replace("api_deck_id=", "")) - 1;
                                break;
                            }
                        }
                        if (deckIdx != -1) {
                            if (jsonDataObj.has("api_data")) {
                                boolean is_same = true;
                                JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                                JsonArray before_ship_list = portdeckdata.get(deckIdx).getAsJsonObject().getAsJsonArray("api_ship");

                                JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                                JsonArray after_ship_list = api_data.getAsJsonArray("api_ship");
                                for (int i = 0; i < after_ship_list.size(); i++) {
                                    if (!before_ship_list.contains(after_ship_list.get(i))) {
                                        is_same = false;
                                        break;
                                    }
                                }
                                portdeckdata.set(deckIdx, api_data);
                                dbHelper.putValue(DB_KEY_DECKPORT, portdeckdata.toString());
                                boolean result = setMoraleValue(deckIdx, deckInfoCalc.checkMinimumMorale(portdeckdata, deckIdx), false, is_same);
                                processMoraleInfo(deckIdx, portdeckdata, result);
                            }
                        }
                        updateFleetView();

                    }

                    if (url.startsWith(API_REQ_HENSEI_COMBINED)) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            int api_combined = api_data.get("api_combined").getAsInt();
                            isCombined = (api_combined > 0);
                            KcaBattle.isCombined = api_combined > 0;
                        }
                        Log.e("KCA", "Combined: " + String.valueOf(isCombined));
                        updateFleetView();
                    }

                    if (url.startsWith(API_REQ_MEMBER_ITEMUSE_COND)) {
                        String[] requestData = request.split("&");
                        int deckIdx = -1;
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_deck_id=")) {
                                deckIdx = Integer.valueOf(decodedData.replace("api_deck_id=", "")) - 1;
                                break;
                            }
                        }
                        KcaMoraleInfo.setItemUseDeck(deckIdx);
                        updateFleetView();
                    }

                    if (url.startsWith(API_GET_MEMBER_SHIP2)) {
                        if (jsonDataObj.has("api_data")) {
                            JsonArray api_data = jsonDataObj.getAsJsonArray("api_data");
                            JsonArray api_data_deck = jsonDataObj.getAsJsonArray("api_data_deck");
                            KcaApiData.updateUserShipData(api_data);
                            dbHelper.putValue(DB_KEY_DECKPORT, api_data_deck.toString());

                            int itemuse_deck = KcaMoraleInfo.getItemUseDeckAndReset();
                            if (itemuse_deck > 0) {
                                boolean result = setMoraleValue(itemuse_deck, deckInfoCalc.checkMinimumMorale(api_data_deck, itemuse_deck), false, false);
                                processMoraleInfo(itemuse_deck, api_data_deck, result);
                            }
                        }
                        updateFleetView();
                    }

                    if (url.startsWith(API_GET_MEMBER_SHIP3)) {
                        String[] requestData = request.split("&");
                        int userShipId = -1;
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_shipid=")) {
                                userShipId = Integer.valueOf(decodedData.replace("api_shipid=", ""));
                                break;
                            }
                        }
                        if (userShipId != -1) {
                            if (jsonDataObj.has("api_data")) {
                                JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                                dbHelper.putValue(DB_KEY_DECKPORT, api_data.get("api_deck_data").getAsJsonArray().toString());
                                dbHelper.test();
                                KcaApiData.updateUserShip(api_data.get("api_ship_data").getAsJsonArray().get(0).getAsJsonObject());
                            }
                        }
                        if (kaisouProcessFlag) {
                            toastInfo();
                            kaisouProcessFlag = false;
                            updateFleetView();
                        }
                    }

                    if (url.startsWith(API_REQ_KAISOU_SLOT_EXCHANGE)) {
                        String[] requestData = request.split("&");
                        int userShipId = -1;
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_id=")) {
                                userShipId = Integer.valueOf(decodedData.replace("api_id=", ""));
                                break;
                            }
                        }
                        if (userShipId != -1 && jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            if (api_data.has("api_ship_data")) {
                                JsonObject ship_data = api_data.getAsJsonObject("api_ship_data");
                                KcaApiData.updateUserShipSlot(userShipId, ship_data);
                            }
                        }
                        updateFleetView();
                        //toastInfo();
                    }

                    if (url.startsWith(API_REQ_KAISOU_SLOT_DEPRIVE)) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            JsonObject api_ship_data = api_data.get("api_ship_data").getAsJsonObject();
                            KcaApiData.updateUserShip(api_ship_data.get("api_set_ship").getAsJsonObject());
                            KcaApiData.updateUserShip(api_ship_data.get("api_unset_ship").getAsJsonObject());
                        }
                        updateFleetView();
                        //toastInfo();
                    }

                    if (url.startsWith(API_REQ_KAISOU_POWERUP)) {
                        String[] requestData = request.split("&");
                        int targetId = -1;
                        String itemIds = "";
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_id=")) {
                                targetId = Integer.valueOf(decodedData.replace("api_id=", ""));
                            }
                            if (decodedData.startsWith("api_id_items=")) {
                                itemIds = decodedData.replace("api_id_items=", "");
                            }
                        }
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            dbHelper.putValue(DB_KEY_DECKPORT, api_data.getAsJsonArray("api_deck").toString());
                            dbHelper.test();
                            updateUserShip(api_data.getAsJsonObject("api_ship"));
                            KcaApiData.deleteUserShip(itemIds, 1);
                            if (api_data.has("api_powerup_flag") && api_data.get("api_powerup_flag").getAsInt() == 1) {
                                questTracker.updateIdCountTracker("702");
                                questTracker.updateIdCountTracker("703");
                                updateQuestView();
                            }
                        }

                        updateFleetView();
                    }

                    if (url.equals(API_REQ_KOUSYOU_REMOEL_SLOT)) {
                        JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                        int[] kcShipData = deckInfoCalc.getKcShipList(portdeckdata, 0);
                        int flagship = kcShipData[0];
                        int assistant = kcShipData[1];

                        String[] requestData = request.split("&");
                        int certainFlag = 0;
                        int itemId = 0;
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_certain_flag=")) {
                                certainFlag = Integer.valueOf(decodedData.replace("api_certain_flag=", ""));
                            }
                            if (decodedData.startsWith("api_slot_id=")) {
                                itemId = Integer.valueOf(decodedData.replace("api_slot_id=", ""));
                            }
                        }

                        JsonObject itemData = KcaApiData.getUserItemStatusById(itemId, "slotitem_id,level", "");
                        if (itemData != null) {
                            int itemKcId = itemData.get("slotitem_id").getAsInt();
                            int level = itemData.get("level").getAsInt();
                            int api_remodel_flag = 0;
                            if (jsonDataObj.has("api_data")) {
                                JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                                api_remodel_flag = api_data.get("api_remodel_flag").getAsInt();
                                if (certainFlag == 1 || api_remodel_flag == 1) {
                                    JsonObject api_after_slot = api_data.get("api_after_slot").getAsJsonObject();
                                    JsonArray api_slot_item = new JsonArray();
                                    api_slot_item.add(api_after_slot);
                                    for (int i = 0; i < api_slot_item.size(); i++) {
                                        JsonObject item = api_slot_item.get(i).getAsJsonObject();
                                        dbHelper.putItemValue(item.get("api_id").getAsInt(), item.toString());
                                    }
                                }
                                JsonElement use_slot_id = api_data.get("api_use_slot_id");
                                List<String> use_slot_id_list = new ArrayList<String>();
                                if (use_slot_id != null) {
                                    for (JsonElement id : use_slot_id.getAsJsonArray()) {
                                        use_slot_id_list.add(id.getAsString());
                                    }
                                    KcaApiData.removeSlotItemData(joinStr(use_slot_id_list, ","));
                                }
                            }
                            if (certainFlag != 1 && isOpenDBEnabled()) {
                                KcaOpenDBAPI.sendRemodelData(flagship, assistant, itemKcId, level, api_remodel_flag);
                            }
                            questTracker.updateIdCountTracker("619");
                            updateQuestView();
                        }
                    }
                }
            }
            sendQuestCompletionInfo();
        } catch (Exception e) {
            e.printStackTrace();
            String api_request = "";
            List<String> filtered_resquest_list = new ArrayList<String>();
            try {
                String[] requestData = request.split("&");
                for (int i = 0; i < requestData.length; i++) {
                    String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                    if (!decodedData.startsWith("api_token")) {
                        filtered_resquest_list.add(requestData[i]);
                    }
                }
                api_request = joinStr(filtered_resquest_list, "&");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            String process_data = new String(raw);
            if (url.contains(API_PORT)) {
                process_data = "PORT DATA OMITTED";
            }
            dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, url, api_request, process_data, getStringFromException(e));
        }
    }

    private static class kcaNotificationHandler extends Handler {
        private final WeakReference<KcaService> mService;

        kcaNotificationHandler(KcaService service) {
            mService = new WeakReference<KcaService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            KcaService service = mService.get();
            if (service != null) {
                service.handleNotificationMessage(msg);
            }
        }
    }

    public void handleNotificationMessage(Message msg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = msg.getData().getString("url");
        String data = msg.getData().getString("data");
        KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());

        if (!prefs.getBoolean(PREF_SVC_ENABLED, false) || url.length() == 0) {
            Log.e("KCA", "url: " + url);
            return;
        }

        JsonObject jsonDataObj = null;
        try {
            if (data != null && data.length() > 0) {
                jsonDataObj = gson.fromJson(data, JsonObject.class);
            }

            if (url.startsWith(KCA_API_DATA_LOADED)) {
                if (jsonDataObj.has("ship")) {
                    Log.e("KCA", KcaUtils.format("Ship: %d", jsonDataObj.get("ship").getAsInt()));
                    Log.e("KCA", KcaUtils.format("Item: %d", jsonDataObj.get("item").getAsInt()));
                    restartFlag = false;
                }
                api_start2_loading_flag = false;
                updateFleetView();
            }

            if (url.startsWith(KCA_API_FAIRY_RETURN)) {
                startService(new Intent(this, KcaViewButtonService.class)
                        .setAction(KcaViewButtonService.RETURN_FAIRY_ACTION));
            }

            if (url.startsWith(KCA_API_FAIRY_HIDDEN)) {
                Intent returnIntent = new Intent(this, KcaViewButtonService.class)
                        .setAction(RETURN_FAIRY_ACTION);
                Intent removeIntent = new Intent(this, KcaViewButtonService.class)
                        .setAction(REMOVE_FAIRY_ACTION);
                PendingIntent returnPendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                        returnIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent removePendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                        removeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                notifyBuilder.addAction(new NotificationCompat.Action(0, getStringWithLocale(R.string.fairy_hidden_notification_action_return), returnPendingIntent))
                        .addAction(new NotificationCompat.Action(0, getStringWithLocale(R.string.fairy_hidden_notification_action_remove), removePendingIntent));
            }

            if (url.startsWith(KCA_API_FAIRY_CHECKED)) {
                notifyBuilder.mActions.clear();
                updateNotification(false);
            }

            if (url.startsWith(KCA_API_PREF_FAIRY_CHANGED)) {
                String fairyId = "noti_icon_".concat(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
                Bitmap viewBitmap = KcaUtils.getFairyImageFromStorage(getApplicationContext(), fairyId, dbHelper);
                if (viewBitmap != null) notifyBuilder.setLargeIcon(viewBitmap);
                updateNotification(false);
                startService(new Intent(this, KcaViewButtonService.class)
                        .setAction(KcaViewButtonService.FAIRY_CHANGE));
            }

            if (url.startsWith(KCA_API_PREF_NOTICOUNT_CHANGED)) {
                int count = KcaAlarmService.getAlarmCount();
                if (count > 0) {
                    notifyBuilder.setSmallIcon(R.mipmap.ic_stat_notify_1);
                } else {
                    notifyBuilder.setSmallIcon(R.mipmap.ic_stat_notify_0);
                }
                updateNotification(false);
            }

            if (url.startsWith(KCA_API_PREF_LANGUAGE_CHANGED)) {
                updateExpViewNotification();
            }

            if (url.startsWith(KCA_API_PREF_ALARMDELAY_CHANGED)) {
                processExpeditionInfo(true);
                processDockingInfo(true);

                updateExpViewNotification();
            }

            if (url.startsWith(KCA_API_PREF_PRIORITY_CHANGED)) {
                if (getPriority()) {
                    notifyBuilder.setPriority(IMPORTANCE_HIGH);
                } else {
                    notifyBuilder.setPriority(IMPORTANCE_DEFAULT);
                }
                updateNotification(false);
            }

            if (url.startsWith(KCA_API_NOTI_EXP_FIN)) {
                // Currently Nothing
            }

            if (url.startsWith(KCA_API_NOTI_DOCK_FIN)) {
                // Currently Nothing
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_INFO)) {
                // jsonDataObj = dbHelper.getJsonObjectValue(DB_KEY_BATTLEINFO);
                String api_url = jsonDataObj.get("api_url").getAsString();
                if (api_url.startsWith(API_REQ_SORTIE_BATTLE_RESULT) || url.startsWith(API_REQ_COMBINED_BATTLERESULT)) {
                    JsonObject questTrackData = dbHelper.getJsonObjectValue(DB_KEY_QTRACKINFO);
                    questTracker.updateBattleTracker(questTrackData);
                    updateQuestView();
                } else if (api_url.startsWith(API_REQ_PRACTICE_BATTLE_RESULT)) {
                    JsonObject questTrackData = dbHelper.getJsonObjectValue(DB_KEY_QTRACKINFO);
                    String rank = questTrackData.get("result").getAsString();
                    questTracker.updateIdCountTracker("303");
                    if (rank.equals("S") || rank.equals("A") || rank.equals("B")) {
                        questTracker.updateIdCountTracker("304");
                        questTracker.updateIdCountTracker("302");
                        questTracker.updateIdCountTracker("311");
                    }
                    updateQuestView();
                }
                Intent intent = new Intent(KCA_MSG_BATTLE_INFO);
                broadcaster.sendBroadcast(intent);
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_NODE)) {
                // Reference: https://github.com/andanteyk/ElectronicObserver/blob/1052a7b177a62a5838b23387ff35283618f688dd/ElectronicObserver/Other/Information/apilist.txt
                if (jsonDataObj.has("api_maparea_id")) {
                    JsonObject questTrackData = dbHelper.getJsonObjectValue(DB_KEY_QTRACKINFO);
                    questTracker.updateNodeTracker(questTrackData);
                }

                Intent intent = new Intent(KCA_MSG_BATTLE_NODE);
                broadcaster.sendBroadcast(intent);
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_DROPINFO)) {
                Log.e("KCA", KCA_API_NOTI_BATTLE_DROPINFO + " " + String.valueOf(isOpenDBEnabled()));
                int world = jsonDataObj.get("world").getAsInt();
                int map = jsonDataObj.get("map").getAsInt();
                int node = jsonDataObj.get("node").getAsInt();
                String rank = jsonDataObj.get("rank").getAsString();
                int maprank = jsonDataObj.get("maprank").getAsInt();
                JsonObject enemy = jsonDataObj.getAsJsonObject("enemy");
                boolean isboss = jsonDataObj.get("isboss").getAsBoolean();
                String quest_name = jsonDataObj.get("quest_name").getAsString();
                String enemy_name = jsonDataObj.get("enemy_name").getAsString();
                int inventory = jsonDataObj.get("inventory").getAsInt();
                int result = jsonDataObj.get("result").getAsInt();

                if (KcaApiData.checkUserPortEnough()) {
                    if (isOpenDBEnabled()) KcaOpenDBAPI.sendShipDropData(world, map, node, rank, maprank, enemy, inventory, result);
                    if (isPoiDBEnabled()) KcaPoiDBAPI.sendShipDropData(result, world * 10 + map, quest_name, node, enemy_name, rank, isboss, getAdmiralLevel(), maprank, enemy);
                }
                recordDropLog(jsonDataObj, !KcaApiData.checkUserPortEnough());
                if (result > 0) {
                    KcaApiData.addShipCountInBattle();
                    KcaApiData.addItemCountInBattle(result);
                }
            }

            if (url.startsWith(KCA_API_NOTI_HEAVY_DMG)) {
                heavyDamagedMode = jsonDataObj.get("data").getAsInt();
                if (heavyDamagedMode != HD_NONE) {
                    if (isHDVibrateEnabled()) {
                        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
                        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                Uri notificationUri = KcaUtils.getContentUri(getApplicationContext(),
                                        Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
                                Log.e("KCA", getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
                                KcaUtils.playNotificationSound(mediaPlayer, getApplicationContext(), notificationUri);
                            }
                        }
                        doVibrate(vibrator, 1500);
                    }
                }

                if (heavyDamagedMode == HD_DANGER) {
                    makeToast(getStringWithLocale(R.string.heavy_damaged), Toast.LENGTH_LONG, ContextCompat.getColor(this, R.color.colorHeavyDmgStatePanel));
                    Intent intent = new Intent(KCA_MSG_BATTLE_HDMG);
                    intent.putExtra(KCA_MSG_DATA, "1");
                    broadcaster.sendBroadcast(intent);
                } else {
                    if (heavyDamagedMode == HD_DAMECON) {
                        makeToast(getStringWithLocale(R.string.heavy_damaged_damecon), Toast.LENGTH_LONG, ContextCompat.getColor(this, R.color.colorHeavyDmgStatePanel));
                    }
                    Intent intent = new Intent(KCA_MSG_BATTLE_HDMG);
                    intent.putExtra(KCA_MSG_DATA, "0");
                    broadcaster.sendBroadcast(intent);
                }
            }

            if (url.startsWith(KCA_API_PREF_CN_CHANGED)) {
                updateFleetView();
            }

            if (url.startsWith(KCA_API_PREF_EXPVIEW_CHANGED)) {
                if (isMissionTimerViewEnabled()) {
                    timeScheduler = Executors.newSingleThreadScheduledExecutor();
                    timeScheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
                } else {
                    if (timeScheduler != null) timeScheduler.shutdown();
                    timeScheduler = null;
                }
            }

            if (url.startsWith(KCA_API_OPENDB_FAILED)) {
                makeToast(getStringWithLocale(R.string.opendb_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(this, R.color.colorPrimaryDark));
                dbHelper.recordErrorLog(ERROR_TYPE_OPENDB, url, "opendb", data, "failed");
            }

            if (url.startsWith(KCA_API_PROCESS_BATTLE_FAILED)) {
                String api_data = jsonDataObj.get("api_data").getAsString();
                String api_url = jsonDataObj.get("api_url").getAsString();
                String api_node = jsonDataObj.get("api_node").getAsString();
                String api_error = jsonDataObj.get("api_error").getAsString();
                makeToast(getStringWithLocale(R.string.process_battle_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(this, R.color.colorPrimaryDark));
                dbHelper.recordErrorLog(ERROR_TYPE_BATTLE, api_url, api_node, api_data, api_error);
            }

            sendQuestCompletionInfo();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            makeToast(getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(this, R.color.colorPrimaryDark));
            dbHelper.recordErrorLog(ERROR_TYPE_NOTI, url, "notification", data, getStringFromException(e));
        }
    }

    private void sendQuestCompletionInfo() {
        boolean quest_completed_exist = questTracker.check_quest_completed(dbHelper);
        Intent intent = new Intent(KCA_MSG_QUEST_COMPLETE);
        String response = "0";
        if (quest_completed_exist) response = "1";
        intent.putExtra(KCA_MSG_DATA, response);
        broadcaster.sendBroadcast(intent);
    }

    private int getSeekCn() {
        return Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    private int getExpeditionType() {
        return Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_EXP_TYPE));
    }

    private boolean isOpenDBEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_OPENDB_API_USE);
    }

    private boolean isPoiDBEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_POIDB_API_USE);
    }

    private boolean isDropLogEnable() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_ACTIVATE_DROPLOG);
    }

    private boolean isResourceLogEnable() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_ACTIVATE_DROPLOG);
    }


    private boolean isBattleViewEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE);
    }

    private boolean isBattleNodeEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLENODE_USE);
    }

    private boolean isMissionTimerViewEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_EXP_VIEW);
    }

    private boolean isAkashiTimerNotiEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_AKASHI);
    }

    private boolean getPriority() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_SET_PRIORITY);
    }

    private boolean isHDVibrateEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_V_HD);
    }

    private boolean isNSVibrateEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_V_NS);
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

    private void processExpeditionInfo() {
        processExpeditionInfo(false);
    }

    private void processExpeditionInfo(boolean reset_flag) {
        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (data == null) return;
        //Log.e("KCA", "processExpeditionInfo Called");
        int deck_id, mission_no;
        long arrive_time;
        String deck_name;
        for (int i = 1; i < data.size(); i++) {
            int idx = i - 1; // 1=>0, 2=>1, 3=>2 (0: Fleet #1)
            JsonObject deck = (JsonObject) data.get(i);
            deck_id = deck.get("api_id").getAsInt();
            deck_name = deck.get("api_name").getAsString();
            JsonArray apiMission = (JsonArray) deck.get("api_mission");
            if (apiMission.get(0).getAsInt() == 1) {
                mission_no = apiMission.get(1).getAsInt();
                arrive_time = apiMission.get(2).getAsLong();
            } else {
                mission_no = -1;
                arrive_time = -1;
            }

            Intent aIntent = new Intent(getApplicationContext(), KcaAlarmService.class);
            if (KcaExpedition2.isInMission(i)) {
                boolean isNotIdenticalMission = (arrive_time != KcaExpedition2.getArriveTime(i));
                if (isNotIdenticalMission || reset_flag) {
                    PendingIntent pendingIntent = PendingIntent.getService(
                            getApplicationContext(),
                            getNotificationId(NOTI_EXP, i),
                            new Intent(getApplicationContext(), KcaAlarmService.class),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    pendingIntent.cancel();
                    alarmManager.cancel(pendingIntent);
                    KcaExpedition2.clearMissionData(i);
                    if (mission_no != -1) {
                        KcaExpedition2.setMissionData(i, deck_name, mission_no, arrive_time);
                        setExpeditionAlarm(i, mission_no, deck_name, arrive_time, false, false, aIntent);
                    }
                }
            } else if (mission_no != -1) {
                KcaExpedition2.setMissionData(i, deck_name, mission_no, arrive_time);
                setExpeditionAlarm(i, mission_no, deck_name, arrive_time, false, false, aIntent);
            }
        }
    }

    private void cancelExpeditionInfo(JsonObject data) {
        JsonArray canceled_info = (JsonArray) data.get("api_mission");
        int canceled_mission_no = canceled_info.get(1).getAsInt();
        long arrive_time = canceled_info.get(2).getAsLong();
        int idx = KcaExpedition2.getIdxByMissionNo(canceled_mission_no);
        if (idx == -1) return;
        else {
            KcaExpedition2.cancel(idx, arrive_time);
            Intent aIntent = new Intent(getApplicationContext(), KcaAlarmService.class);
            String kantai_name = KcaExpedition2.getDeckName(idx);
            setExpeditionAlarm(idx, canceled_mission_no, kantai_name, -1, true, false, aIntent);
            setExpeditionAlarm(idx, canceled_mission_no, kantai_name, arrive_time, false, true, aIntent);
        }
    }

    private void processDockingInfo() {
        processDockingInfo(false);
    }

    private void processDockingInfo(boolean reset_flag) {
        int dockId, shipId, state;
        long completeTime;
        JsonArray data = KcaDocking.getDockData();
        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            JsonObject ndockData = data.get(i).getAsJsonObject();
            state = ndockData.get("api_state").getAsInt();
            int nid = getNotificationId(NOTI_DOCK, i);
            Intent deleteIntent = new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid)));
            startService(deleteIntent);

            if (state != -1) {
                dockId = ndockData.get("api_id").getAsInt() - 1;
                shipId = ndockData.get("api_ship_id").getAsInt();
                completeTime = ndockData.get("api_complete_time").getAsLong();
                Intent aIntent = new Intent(getApplicationContext(), KcaAlarmService.class);
                if (KcaDocking.getCompleteTime(dockId) != -1) {
                    if (KcaDocking.getShipId(dockId) != shipId || reset_flag) {
                        PendingIntent pendingIntent = PendingIntent.getService(
                                getApplicationContext(),
                                getNotificationId(NOTI_DOCK, dockId),
                                new Intent(getApplicationContext(), KcaAlarmService.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                        pendingIntent.cancel();
                        alarmManager.cancel(pendingIntent);
                        if (shipId != 0) setDockingAlarm(dockId, shipId, completeTime, aIntent);
                        KcaDocking.setShipId(dockId, shipId);
                    }
                    KcaDocking.setCompleteTime(dockId, completeTime);
                } else {
                    if (state == 1) {
                        setDockingAlarm(dockId, shipId, completeTime, aIntent);
                        KcaDocking.setShipId(dockId, shipId);
                        KcaDocking.setCompleteTime(dockId, completeTime);
                    }
                }
            }
        }
    }

    private void processDockingSpeedup(int dockId) {
        KcaApiData.updateShipHpFull(KcaDocking.getShipId(dockId));
        KcaDocking.setShipId(dockId, 0);
        KcaDocking.setCompleteTime(dockId, -1);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                getNotificationId(NOTI_DOCK, dockId),
                new Intent(getApplicationContext(), KcaAlarmService.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);
    }

    private void processMoraleInfo(int idx, JsonArray deckportdata, boolean set_alarm) {
        if (KcaBattle.currentFleet == idx) return;

        JsonObject deck = (JsonObject) deckportdata.get(idx);
        String deck_name = deck.get("api_name").getAsString();
        long morale_time = KcaMoraleInfo.getMoraleCompleteTime(idx);
        Intent aIntent = new Intent(getApplicationContext(), KcaAlarmService.class);
        int nid = getNotificationId(NOTI_MORALE, idx);
        if (morale_time < 0) {
            Intent deleteIntent = new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid)));
            startService(deleteIntent);

            PendingIntent pendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    nid,
                    new Intent(getApplicationContext(), KcaAlarmService.class),
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            pendingIntent.cancel();
            alarmManager.cancel(pendingIntent);
        } else if (set_alarm) {
            setMoraleAlarm(idx, deck_name, morale_time, aIntent);
        }
    }

    private void processAkashiTimerInfo() {
        int nid = getNotificationId(NOTI_AKASHI, 0);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                nid,
                new Intent(getApplicationContext(), KcaAlarmService.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);
        Intent aIntent = new Intent(getApplicationContext(), KcaAlarmService.class);
        notifiManager.cancel(nid);
        setAkashiAlarm(aIntent);
    }

    private boolean isCurrentPortDeckDataReady() {
        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (data == null) {
            KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());
            makeToast(getStringWithLocale(R.string.kca_toast_restart_at_kcanotify), Toast.LENGTH_LONG, ContextCompat.getColor(this, R.color.colorPrimaryDark));
            Log.e("KCA", KcaUtils.format("currentPortDeckData is null"));
            return false;
        } else {
            return true;
        }
    }

    private void setAlarm(long time, PendingIntent alarmIntent, int code, boolean delay) {
        if (time == -1) {
            time = System.currentTimeMillis();
        } else if (delay) {
            time = time - KcaAlarmService.ALARM_DELAY;
            if (time < System.currentTimeMillis()) return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, time, alarmIntent);
        } else {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, time, alarmIntent);
        }
        Log.e("KCA", "Alarm set to: " + String.valueOf(time) + " " + String.valueOf(code));
    }

    public void updateQuestView() {
        startService(new Intent(getBaseContext(), KcaQuestViewService.class)
                .setAction(REFRESH_QUESTVIEW_ACTION));
        if (dbHelper.checkQuestListValid()) QSyncWrite();
    }

    public void updateFleetView() {
        startService(new Intent(getBaseContext(), KcaFleetViewService.class)
                .setAction(REFRESH_FLEETVIEW_ACTION));
    }

    public void updateAirbasePopupInfo() {
        if (KcaLandAirBasePopupService.isActive()) {
            Intent qintent = new Intent(getBaseContext(), KcaLandAirBasePopupService.class);
            qintent.setAction(KcaLandAirBasePopupService.LAB_DATA_ACTION);
            startService(qintent);
        }
    }

    public void recordDropLog(JsonObject data, boolean port_full) {
        if (isDropLogEnable()) {
            dropLogger.recordDropLog(data, port_full);
        }
    }

    public void recordResourceLog(JsonArray material_data, boolean is_object) {
        if (isResourceLogEnable()) {
            resourceLogger.recordResourceLog(material_data, is_object);
        }
    }

    public void showCustomToast(KcaCustomToast toast, String body, int duration, int color) {
        KcaUtils.showCustomToast(getApplicationContext(), getBaseContext(), toast, body, duration, color);
    }

    public void QSyncRead() {
        boolean is_available = getBooleanPreferences(getApplicationContext(), PREF_KCAQSYNC_USE);
        String qsync_pass = getStringPreferences(getApplicationContext(), PREF_KCAQSYNC_PASS).trim();
        if (!is_available || qsync_pass.length() == 0) return;

        long recent_check = Long.parseLong(getStringPreferences(getApplicationContext(), PREF_LAST_QUEST_CHECK));
        int userid = KcaApiData.getUserId();
        final boolean[] error_flag = {false, false};
        if (userid > 0) {
            JsonObject quest_data = new JsonObject();
            quest_data.addProperty("userid", userid);
            quest_data.addProperty("pass", qsync_pass);
            Log.e("KCA", String.valueOf(quest_data.toString().length()));
            try {
                final Call<String> qsync_read = kcaQSyncEndpoint.read(KcaUtils.getKcaQSyncHeaderMap(),
                        KcaUtils.getRSAEncodedString(getApplicationContext(), quest_data.toString()));

                qsync_read.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        JsonObject response_data = new JsonObject();
                        if (response.body() != null) {
                            try {
                                response_data = gson.fromJson(response.body(), JsonObject.class);
                                if (response_data.has("status")) {
                                    String result = response_data.get("status").getAsString();
                                    if (result.equals("done")) {
                                        long recent_ts = response_data.get("timestamp").getAsLong() * 1000;
                                        if (recent_ts > recent_check) {
                                            String quest_code = response_data.get("data").getAsString();
                                            dbHelper.loadQuestDataFromCode(quest_code, true, recent_ts);
                                        } else {
                                            error_flag[1] = true;
                                        }
                                    } else { // error
                                        String detail = response_data.get("detail").getAsString();
                                        dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "qsync_read", "", quest_data.toString(), detail);
                                        error_flag[0] = true;
                                    }
                                }
                            } catch (Exception e) {
                                makeToast("failed to sync quest data: " + getStringFromException(e), Toast.LENGTH_LONG,
                                        ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "qsync_read", "", "", t.getMessage());
                        error_flag[0] = true;
                    }
                });
            } catch (Exception e) {
                dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "qsync_read", "", "", getStringFromException(e));
                error_flag[0] = true;
            }
        }
        if (error_flag[0]) {
            makeToast("failed to sync quest data", Toast.LENGTH_LONG,
                    ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }
    }

    public void QSyncWrite() {
        boolean is_available = getBooleanPreferences(getApplicationContext(), PREF_KCAQSYNC_USE);
        String qsync_pass = getStringPreferences(getApplicationContext(), PREF_KCAQSYNC_PASS).trim();
        if (!is_available || qsync_pass.length() == 0) return;

        setPreferences(getApplicationContext(), PREF_LAST_QUEST_CHECK,
                String.valueOf(System.currentTimeMillis()));
        int userid = KcaApiData.getUserId();
        final boolean[] error_flag = {false};
        if (userid > 0) {
            JsonObject quest_data = new JsonObject();
            quest_data.addProperty("userid", userid);
            quest_data.addProperty("data", dbHelper.getCurrentQuestCode());
            quest_data.addProperty("pass", qsync_pass);
            Log.e("KCA", String.valueOf(quest_data.toString().length()));
            try {
                final Call<String> qsync_write = kcaQSyncEndpoint.write(KcaUtils.getKcaQSyncHeaderMap(),
                        KcaUtils.getRSAEncodedString(getApplicationContext(), quest_data.toString()));

                qsync_write.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        JsonObject response_data = new JsonObject();
                        if (response.body() != null) {
                            try {
                                response_data = gson.fromJson(response.body(), JsonObject.class);
                                if (response_data.has("status")) {
                                    String result = response_data.get("status").getAsString();
                                    if (!result.equals("done")) {
                                        String detail = response_data.get("detail").getAsString();
                                        dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "qsync_write", "", quest_data.toString(), detail);
                                        error_flag[0] = true;
                                    }
                                }
                            } catch (Exception e) {
                                makeToast("failed to sync quest data: " + getStringFromException(e), Toast.LENGTH_LONG,
                                        ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "qsync_write", "", "", t.getMessage());
                        error_flag[0] = true;
                    }
                });
            } catch (Exception e) {
                dbHelper.recordErrorLog(ERROR_TYPE_SERVICE, "qsync_write", "", "", getStringFromException(e));
                error_flag[0] = true;
            }
        }
        if (error_flag[0]) {
            makeToast("failed to sync quest data", Toast.LENGTH_LONG,
                    ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }

        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
        loadTranslationData(getApplicationContext());
        showDataLoadErrorToast(getApplicationContext(), getBaseContext(), getStringWithLocale(R.string.download_check_error));

        super.onConfigurationChanged(newConfig);
    }
}