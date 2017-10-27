package com.antest1.kcanotify;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static android.R.attr.id;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static android.support.v4.app.NotificationManagerCompat.IMPORTANCE_HIGH;
import static android.widget.Toast.makeText;
import static com.antest1.kcanotify.KcaAlarmService.DELETE_ACTION;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE_II;
import static com.antest1.kcanotify.KcaApiData.T2_MACHINE_GUN;
import static com.antest1.kcanotify.KcaApiData.checkDataLoadTriggered;
import static com.antest1.kcanotify.KcaApiData.getExpeditionNoByName;
import static com.antest1.kcanotify.KcaApiData.getNodeColor;
import static com.antest1.kcanotify.KcaApiData.getReturnFlag;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.loadMapEdgeInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadQuestTrackDataFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipInitEquipCountFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaApiData.updateUserShip;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaFleetViewService.REFRESH_FLEETVIEW_ACTION;
import static com.antest1.kcanotify.KcaQuestViewService.REFRESH_QUESTVIEW_ACTION;
import static com.antest1.kcanotify.KcaUtils.createBuilder;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getContentUri;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaService extends Service {
    public static final String SERVICE_CHANNEL_ID = "noti_service_channel";
    public static final String SERVICE_CHANNEL_NAME = "Kcanotify Service";

    public static String currentLocale;
    public static boolean isInitState;
    public static boolean isFirstState;

    public static boolean isServiceOn = false;
    public static boolean isPortAccessed = false;
    public static boolean isUserItemDataLoaded = false;
    public static int heavyDamagedMode = 0;
    public static int checkKdockId = -1;
    public static boolean kaisouProcessFlag = false;
    public static String currentNodeInfo = "";
    public static boolean isInBattle;
    public static boolean isCombined;

    Context contextWithLocale;
    KcaDBHelper dbHelper;
    KcaQuestTracker questTracker;
    KcaDeckInfo deckInfoCalc;

    AlarmManager alarmManager;
    AudioManager mAudioManager;
    Vibrator vibrator = null;
    MediaPlayer mediaPlayer;
    NotificationManager notifiManager;
    NotificationCompat.Builder viewNotificationBuilder;
    //NotificationCompat.BigTextStyle viewNotificationText;
    private boolean viewNotificationFirstTime = true;

    public static boolean noti_vibr_on = true;
    int viewBitmapId, viewBitmapSmallId;
    Bitmap viewBitmap = null;
    Runnable missionTimer;
    int notificationTimeCounter;

    kcaServiceHandler handler;
    kcaNotificationHandler nHandler;
    LocalBroadcastManager broadcaster;
    ScheduledExecutorService missionTimeScheduler = null;

    String kcaFirstDeckInfo;
    static String kca_version;
    String api_start2_data = null;
    boolean api_start2_down_mode = false;
    boolean api_start2_init = false;
    boolean api_start2_loading_flag = false;
    Gson gson = new Gson();

    public static boolean getServiceStatus() {
        return isServiceOn;
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
            NotificationChannel channel = new NotificationChannel(SERVICE_CHANNEL_ID,
                    SERVICE_CHANNEL_NAME, priority);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notifiManager.createNotificationChannel(channel);
        }
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    void runTimer() {
        if (isMissionTimerViewEnabled()) {
            missionTimeScheduler = Executors.newSingleThreadScheduledExecutor();
            missionTimeScheduler.scheduleAtFixedRate(missionTimer, 0, 1, TimeUnit.SECONDS);
        } else {
            missionTimeScheduler = null;
        }
    }

    void stopTimer() {
        if (missionTimeScheduler != null) {
            missionTimeScheduler.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isInitState = true;
        isFirstState = true;
        isUserItemDataLoaded = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.e("KCA", String.valueOf(prefs.contains(PREF_SVC_ENABLED)));
        if (!prefs.getBoolean(PREF_SVC_ENABLED, false)) {
            stopSelf();
        }
        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);
        deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());
        KcaApiData.setDBHelper(dbHelper);

        AssetManager assetManager = getResources().getAssets();
        int loadMapEdgeInfoResult = loadMapEdgeInfoFromAssets(assetManager);
        if (loadMapEdgeInfoResult != 1) {
            makeText(this, "Error loading Map Edge Info", Toast.LENGTH_LONG).show();
        }

        loadSimpleExpeditionInfoFromAssets(assetManager);
        loadShipInitEquipCountFromAssets(assetManager);
        loadQuestTrackDataFromAssets(dbHelper, assetManager);

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

        String fairyId = "noti_icon_".concat(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
        viewBitmapId = getId(fairyId, R.mipmap.class);
        viewBitmapSmallId = R.mipmap.ic_stat_notify_0;
        viewBitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, viewBitmapId)).getBitmap();
        viewNotificationBuilder = createBuilder(this, SERVICE_CHANNEL_ID);

        handler = new kcaServiceHandler(this);
        nHandler = new kcaNotificationHandler(this);
        broadcaster = LocalBroadcastManager.getInstance(this);

        String initTitle = String.format(getStringWithLocale(R.string.kca_init_title), getStringWithLocale(R.string.app_name));
        String initContent = getStringWithLocale(R.string.kca_init_content);
        // String initSubContent = String.format("%s %s", getStringWithLocale(R.string.app_name), getStringWithLocale(R.string.app_version));
        kcaFirstDeckInfo = getStringWithLocale(R.string.kca_init_content);
        startForeground(getNotificationId(NOTI_FRONT, 1), createViewNotification(initTitle, initContent));
        isServiceOn = true;

        KcaVpnData.setHandler(handler);
        KcaBattle.setHandler(nHandler);
        KcaApiData.setHandler(nHandler);
        KcaAlarmService.setHandler(nHandler);
        KcaOpendbAPI.setHandler(nHandler);
        SettingActivity.setHandler(nHandler);
        KcaFairySelectActivity.setHandler(nHandler);

        notificationTimeCounter = -1;
        missionTimer = new Runnable() {
            @Override
            public void run() {
                if (viewNotificationBuilder != null && isMissionTimerViewEnabled()) {
                    notificationTimeCounter += 1;
                    if (notificationTimeCounter == 120) {
                        notificationTimeCounter = 0;
                    }
                    updateExpViewNotification();
                }
            }
        };
        runTimer();
        dbHelper.initExpScore();
        return START_STICKY;
    }

    // 서비스가 종료될 때 할 작업

    public void setServiceDown() {
        isPortAccessed = false;
        stopTimer();

        handler = null;
        nHandler = null;

        mediaPlayer.release();
        mediaPlayer = null;

        stopForeground(true);
        notifiManager.cancelAll();
        viewNotificationBuilder = null;
        isServiceOn = false;
    }

    public void onDestroy() {
        setServiceDown();
        stopService(new Intent(this, KcaBattleViewService.class));
        stopService(new Intent(this, KcaQuestViewService.class));
        stopService(new Intent(this, KcaFleetViewService.class));
        stopService(new Intent(this, KcaAkashiViewService.class));
        stopService(new Intent(this, KcaExpeditionCheckViewService.class));
        stopService(new Intent(this, KcaViewButtonService.class));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("svcenabled", false).apply();
        KcaAlarmService.clearAlarmCount();
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

    private Notification createViewNotification(String title, String content2) {
        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        if (viewNotificationFirstTime) {
            viewNotificationBuilder.setContentTitle(title)
                    .setSmallIcon(viewBitmapSmallId)
                    .setLargeIcon(viewBitmap)
                    .setTicker(title)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true)
                    .setContentText(content2)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(content2))
                    .setOngoing(true).setAutoCancel(false);
            if (getBooleanPreferences(getApplicationContext(), PREF_KCA_SET_PRIORITY)) {
                viewNotificationBuilder.setPriority(IMPORTANCE_HIGH);
            } else {
                viewNotificationBuilder.setPriority(IMPORTANCE_DEFAULT);
            }
            viewNotificationFirstTime = false;
        }

        if (isMissionTimerViewEnabled() && content2 != null) {
            viewNotificationBuilder.setContentText(content2);
            viewNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(content2));
            //viewNotificationBuilder.setStyle(viewNotificationText.setSummaryText(content2));
        }

        return viewNotificationBuilder.build();
    }

    private void updateExpViewNotification() {
        int viewType = getExpeditionType();
        String expeditionString = "";
        if (!isFirstState && isMissionTimerViewEnabled()) {
            if (!KcaExpedition2.isMissionExist()) {
                expeditionString = expeditionString.concat(getStringWithLocale(R.string.kca_view_noexpedition));
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
                    String countStr = String.format(" (%d/%d)", value+1, kcaExpStrList.size());
                    expeditionString = kcaExpStrList.get(value).concat(countStr);
                } else {
                    expeditionString = joinStr(kcaExpStrList, " / ");
                }
            }
        } else {
            expeditionString = String.format("%s %s", getStringWithLocale(R.string.app_name), getStringWithLocale(R.string.app_version));
        }

        String notifiTitle = "";
        String nodeString = "";
        if (currentNodeInfo.length() > 0) {
            nodeString = String.format("[%s]", currentNodeInfo.replaceAll("[()]", "").replaceAll("\\s", "/"));
        }

        switch (heavyDamagedMode) {
            case HD_DAMECON:
                notifiTitle = String.format(getStringWithLocale(R.string.kca_view_hdmg_damecon_format), getStringWithLocale(R.string.app_name), nodeString);
                break;
            case HD_DANGER:
                notifiTitle = String.format(getStringWithLocale(R.string.kca_view_hdmg_format), getStringWithLocale(R.string.app_name), nodeString);
                break;
            default:
                notifiTitle = String.format(getStringWithLocale(R.string.kca_view_normal_format), getStringWithLocale(R.string.app_name), nodeString);
                break;
        }

        if (viewNotificationBuilder != null) {
            Intent aIntent = new Intent(KcaService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            viewNotificationBuilder.setContentTitle(notifiTitle.trim());
            viewNotificationBuilder.setContentText(expeditionString);
            viewNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(expeditionString));
            notifiManager.notify(getNotificationId(NOTI_FRONT, 1), viewNotificationBuilder.setContentIntent(pendingIntent).build());
        }
    }

    private void updateNotificationFairy() {
        if (viewNotificationBuilder != null) {
            viewNotificationBuilder.setLargeIcon(viewBitmap);
            Intent aIntent = new Intent(KcaService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notifiManager.notify(getNotificationId(NOTI_FRONT, 1), viewNotificationBuilder.setContentIntent(pendingIntent).build());
        }
    }

    private void updateNotificationIcon(int type) {
        if (viewNotificationBuilder != null) {
            int id = getId("ic_stat_notify_".concat(String.valueOf(type)), R.mipmap.class);
            viewNotificationBuilder.setSmallIcon(id);
            Intent aIntent = new Intent(KcaService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notifiManager.notify(getNotificationId(NOTI_FRONT, 1), viewNotificationBuilder.setContentIntent(pendingIntent).build());
        }
    }

    private void updateNotificationPriority(boolean isprior) {
        if (viewNotificationBuilder != null) {
            if (isprior) {
                viewNotificationBuilder.setPriority(IMPORTANCE_HIGH);
            } else {
                viewNotificationBuilder.setPriority(IMPORTANCE_DEFAULT);
            }
            Intent aIntent = new Intent(KcaService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notifiManager.notify(getNotificationId(NOTI_FRONT, 1), viewNotificationBuilder.setContentIntent(pendingIntent).build());
        }
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
        setAlarm(arrive_time, pendingIntent, getNotificationId(NOTI_EXP, idx));
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
        setAlarm(complete_time, pendingIntent, getNotificationId(NOTI_DOCK, dockId));
    }

    private void toastInfo() {
        if (!KcaApiData.isGameDataLoaded()) return;
        else if (!isCurrentPortDeckDataReady()) return;
        int cn = getSeekCn();
        String seekType = getSeekType();
        JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);

        int[] airPowerRange = deckInfoCalc.getAirPowerRange(portdeckdata, 0, KcaBattle.getEscapeFlag());
        String airPowerValue = String.format(getStringWithLocale(R.string.kca_toast_airpower), airPowerRange[0], airPowerRange[1]);
        String seekValue = String.format(getStringWithLocale(R.string.kca_toast_seekvalue_f), seekType, deckInfoCalc.getSeekValue(portdeckdata, "0", cn, KcaBattle.getEscapeFlag()));
        int[] tp = deckInfoCalc.getTPValue(portdeckdata, "0", KcaBattle.getEscapeFlag());
        String tpValue = String.format(getStringWithLocale(R.string.kca_view_tpvalue), tp[1], tp[0]);
        List<String> toastList = new ArrayList<String>();
        if (airPowerRange[1] > 0) {
            toastList.add(airPowerValue);
        }
        toastList.add(seekValue);
        if (tp[0] > 0) {
            toastList.add(tpValue);
        }
        KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());
        showCustomToast(customToast, joinStr(toastList, " / "), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
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

    public void handleServiceMessage(Message msg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = msg.getData().getString("url");
        byte[] raw = msg.getData().getByteArray("data");
        Reader data = new InputStreamReader(new ByteArrayInputStream(raw));
        String request = msg.getData().getString("request");
        KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());

        if (!prefs.getBoolean(PREF_SVC_ENABLED, false) || url.length() == 0 || viewNotificationBuilder == null) {
            return;
        }

        JsonObject jsonDataObj;
        try {
            String init = new String(Arrays.copyOfRange(raw, 0, 7));
            if (init.contains("svdata=")) {
                data.skip("svdata=".length());
            }
            jsonDataObj = new JsonParser().parse(data).getAsJsonObject();

            if (url.startsWith(KCA_VERSION)) {
                isInitState = false;
                isPortAccessed = false;
                api_start2_init = false;
                api_start2_loading_flag = true;
                KcaFleetViewService.setReadyFlag(false);
                //Toast.makeText(contextWithLocale, "KCA_VERSION", Toast.LENGTH_LONG).show();
                JsonObject api_version = jsonDataObj.get("api").getAsJsonObject();
                kca_version = api_version.get("api_start2").getAsString();
                Log.e("KCA", kca_version);

                if (!getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION).equals(kca_version)) {
                    showCustomToast(customToast, "new game data detected: " + String.valueOf(kca_version), Toast.LENGTH_LONG,
                            ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
                JsonObject kcDataObj = dbHelper.getJsonObjectValue(DB_KEY_STARTDATA);
                //Log.e("KCA", kcDataObj.toJSONString());
                if (kcDataObj != null && kcDataObj.has("api_data")) {
                    //Toast.makeText(contextWithLocale, "Load Kancolle Data", Toast.LENGTH_LONG).show();
                    KcaApiData.getKcGameData(kcDataObj.getAsJsonObject("api_data"));
                    setPreferences(getApplicationContext(), PREF_KCA_VERSION, kca_version);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(getApplicationContext())) {
                    // Can not draw overlays: pass
                } else {
                    startService(new Intent(this, KcaViewButtonService.class));
                    startService(new Intent(this, KcaQuestViewService.class));
                    startService(new Intent(this, KcaAkashiViewService.class));
                    sendQuestCompletionInfo();
                }
                return;
                //Toast.makeText(contextWithLocale, getPreferences("kca_version") + " " + String.valueOf(api_start2_down_mode), Toast.LENGTH_LONG).show();
            }

            if (url.startsWith(API_WORLD_GET_ID)) {
                return;
            }

            if (url.startsWith(API_REQ_MEMBER_GET_INCENTIVE)) {
                return;
            }

            if (url.startsWith(API_START2)) {
                //Log.e("KCA", "Load Kancolle Data");
                //Toast.makeText(contextWithLocale, "API_START2", Toast.LENGTH_LONG).show();

                api_start2_data = jsonDataObj.toString();
                dbHelper.putValue(DB_KEY_STARTDATA, api_start2_data);

                if (jsonDataObj.has("api_data")) {
                    //Toast.makeText(contextWithLocale, "Load Kancolle Data", Toast.LENGTH_LONG).show();
                    KcaApiData.getKcGameData(jsonDataObj.getAsJsonObject("api_data"));
                    if (kca_version != null) {
                        setPreferences(getApplicationContext(), "kca_version", kca_version);
                    }
                }
            }

            if (url.startsWith(API_GET_MEMBER_REQUIRED_INFO)) {
                //Log.e("KCA", "Load Item Data");

                if (jsonDataObj.has("api_data")) {
                    //dbHelper.putValue(DB_KEY_USEREQUIP, jsonDataObj.getAsJsonObject("api_data").getAsJsonArray("api_slot_item").toString());
                    JsonObject requiredInfoApiData = jsonDataObj.getAsJsonObject("api_data");
                    dbHelper.putValue(DB_KEY_KDOCKDATA, requiredInfoApiData.getAsJsonArray("api_kdock").toString());
                    int size2 = KcaApiData.putSlotItemDataToDB(requiredInfoApiData.getAsJsonArray("api_slot_item"));
                    int userId = KcaApiData.getUserId(requiredInfoApiData);
                    Log.e("KCA", "Total Items: " + String.valueOf(size2));
                    if (size2 > 0) isUserItemDataLoaded = true;
                    //Toast.makeText(contextWithLocale, String.valueOf(userId), Toast.LENGTH_LONG).show();

                    /*
                    // Deactivate this code, force to download manually
                    if (api_start2_data == null && api_start2_down_mode) {
                       showCustomToast(customToast, getStringWithLocale(R.string.kca_toast_get_data_at_settings), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        // new retrieveApiStartData().execute("", "down", "");
                    }
                    */
                }
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

            if (url.startsWith(API_GET_MEMBER_NDOCK)) {
                if (jsonDataObj.has("api_data")) {
                    JsonArray api_data = jsonDataObj.getAsJsonArray("api_data");
                    processDockingInfo(api_data);
                }
            }

            if (url.startsWith(API_REQ_NYUKYO_START)) {
                questTracker.updateIdCountTracker("503");
                updateQuestView();
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
            }

            if (url.startsWith(API_REQ_HOKYU_CHARGE)) {
                questTracker.updateIdCountTracker("504");
                updateQuestView();
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
                    KcaApiData.getPortData(reqPortApiData);
                    if (reqPortApiData.has("api_deck_port")) {
                        dbHelper.putValue(DB_KEY_DECKPORT, reqPortApiData.getAsJsonArray("api_deck_port").toString());
                        dbHelper.test();
                    }
                }
            }

            if (url.startsWith(API_GET_MEMBER_SLOT_ITEM)) {
                if (jsonDataObj.has("api_data")) {
                    JsonArray api_data = jsonDataObj.get("api_data").getAsJsonArray();
                    KcaApiData.putSlotItemDataToDB(api_data);
                    isUserItemDataLoaded = true;
                }
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
                    startService(new Intent(getBaseContext(), KcaQuestViewService.class)
                            .setAction(REFRESH_QUESTVIEW_ACTION).putExtra("tab_id", api_tab_id));
                }
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
            }

            // Game Data Dependent Tasks
            if (!isUserItemDataLoaded) {
                showCustomToast(customToast, getStringWithLocale(R.string.kca_toast_restart_at_kcanotify), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            } else if (!checkDataLoadTriggered()) {
                if (!api_start2_loading_flag) {
                    showCustomToast(customToast, getStringWithLocale(R.string.kca_toast_get_data_at_settings), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    //new retrieveApiStartData().execute("", "down", "");
                }
            } else if (api_start2_loading_flag) {
                showCustomToast(customToast, getStringWithLocale(R.string.kca_toast_loading_data), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            } else {
                if (url.startsWith(API_PORT)) {
                    KcaFleetViewService.setReadyFlag(true);
                    heavyDamagedMode = HD_NONE;
                    currentNodeInfo = "";
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

                        if (reqPortApiData.has("api_basic")) {
                            processBasicInfo(reqPortApiData.getAsJsonObject("api_basic"));
                        }
                        //Log.e("KCA", "Total Ships: " + String.valueOf(size));
                        if (reqPortApiData.has("api_deck_port")) {
                            processExpeditionInfo();
                            updateFleetView();
                        }
                        if (reqPortApiData.has("api_ndock")) {
                            JsonArray nDockData = reqPortApiData.getAsJsonArray("api_ndock");
                            processDockingInfo(nDockData);
                        }
                    }
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
                                message = message.concat(String.format(getStringWithLocale(R.string.not_supplied), i + 1)).concat("\n");
                            }
                        }

                        if (url.startsWith(API_GET_MEMBER_MAPINFO)) {
                            int firstHeavyDamaged = deckInfoCalc.checkHeavyDamageExist(portdeckdata, 0);
                            int secondHeavyDamaged = 0;
                            if (portdeckdata.size() >= 2) {
                                secondHeavyDamaged = deckInfoCalc.checkHeavyDamageExist(portdeckdata, 1);
                            }

                            int checkvalue = 0;
                            if (isCombined) {
                                checkvalue = Math.max(firstHeavyDamaged, secondHeavyDamaged);
                            } else {
                                checkvalue = firstHeavyDamaged;
                            }

                            switch (checkvalue) {
                                case HD_DAMECON:
                                case HD_DANGER:
                                    isHeavyDamagedFlag = true;
                                    if (checkvalue == HD_DANGER) {
                                        message = message.concat(getStringWithLocale(R.string.heavy_damaged)).concat("\n");
                                    } else if (checkvalue == HD_DAMECON) {
                                        message = message.concat(getStringWithLocale(R.string.heavy_damaged_damecon)).concat("\n");
                                    }
                                    break;
                                default:
                                    break;
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
                            showCustomToast(customToast, message.trim(), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), toastColor));
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

                            int deck_id = 0;
                            String[] requestData = request.split("&");
                            for (int i = 0; i < requestData.length; i++) {
                                String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                                if (decodedData.startsWith("api_deck_id")) {
                                    deck_id = Integer.valueOf(decodedData.replace("api_deck_id=", "")) - 1;
                                    break;
                                }
                            }

                            KcaBattle.currentFleet = deck_id;

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
                    } else if (url.equals(API_REQ_COMBINED_GOBACKPORT)) {
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
                            int itemKcId = KcaApiData.updateSlotItemData(api_data);
                            if (isOpendbEnabled()) {
                                KcaOpendbAPI.sendEquipDevData(flagship, materials[0], materials[1], materials[2], materials[3], itemKcId);
                            }
                            questTracker.updateIdCountTracker("605");
                            questTracker.updateIdCountTracker("607");
                            updateQuestView();

                            if (KcaDevelopPopupService.isActive()) {
                                String itemname = "";
                                int itemtype = 0;
                                String itemcount = "";

                                if (itemKcId > 0) {
                                    JsonObject itemData = KcaApiData.getKcItemStatusById(itemKcId, "name,type");
                                    itemname = KcaApiData.getItemTranslation(itemData.get("name").getAsString());
                                    itemtype = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                                    itemcount = String.format("(%d)", KcaApiData.getItemCountByKcId(itemKcId));
                                } else {
                                    itemname = getStringWithLocale(R.string.develop_failed_text);
                                    itemtype = 999;
                                }

                                JsonObject shipData = KcaApiData.getKcShipDataById(flagship, "name");
                                String shipname = KcaApiData.getShipTranslation(shipData.get("name").getAsString(), false);

                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String timetext = dateFormat.format(new Date());

                                JsonObject equipdevdata = new JsonObject();
                                equipdevdata.addProperty("flagship", shipname);
                                equipdevdata.addProperty("name", itemname);
                                equipdevdata.addProperty("type", itemtype);
                                equipdevdata.addProperty("count", itemcount);
                                equipdevdata.addProperty("time", timetext);

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
                                    JsonObject status = getUserItemStatusById(Integer.parseInt(item), "alv", "type");
                                    if (status.has("type")) {
                                        switch (status.getAsJsonArray("type").get(2).getAsInt()) {
                                            case T2_MACHINE_GUN:
                                                questTracker.updateIdCountTracker("638");
                                                break;
                                            case T2_GUN_LARGE:
                                            case T2_GUN_LARGE_II:
                                                questTracker.updateIdCountTracker("663");
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
                    }

                    if (url.equals(API_REQ_KOUSYOU_CREATESHIP)) {
                        String[] requestData = request.split("&");
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_kdock_id=")) {
                                checkKdockId = Integer.valueOf(decodedData.replace("api_kdock_id=", "")) - 1;
                                break;
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
                                    materials[i] = api_kdock_item.get(String.format("api_item%d", i + 1)).getAsInt();
                                }
                                int created_ship_id = api_kdock_item.get("api_created_ship_id").getAsInt();
                                if (isOpendbEnabled()) {
                                    KcaOpendbAPI.sendShipDevData(flagship, materials[0], materials[1], materials[2], materials[3], materials[4], created_ship_id);
                                }
                                checkKdockId = -1;
                            }
                        }
                    }

                    if (url.startsWith(API_REQ_KOUSYOU_DESTROYSHIP)) {
                        String targetShip = "";
                        String[] requestData = request.split("&");
                        for (int i = 0; i < requestData.length; i++) {
                            String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                            if (decodedData.startsWith("api_ship_id")) {
                                targetShip = decodedData.replace("api_ship_id=", "");
                                KcaApiData.deleteUserShip(targetShip);
                                break;
                            }
                        }
                        JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                        for (int i = 0; i < portdeckdata.size(); i++) {
                            JsonObject deckData = portdeckdata.get(i).getAsJsonObject();
                            JsonArray deckShipData = deckData.get("api_ship").getAsJsonArray();
                            for (int j = 0; j < deckShipData.size(); j++) {
                                if (targetShip.equals(String.valueOf(deckShipData.get(j).getAsInt()))) {
                                    deckShipData.set(j, new JsonPrimitive(-1));
                                    deckData.add("api_ship", deckShipData);
                                    portdeckdata.set(i, deckData);
                                    dbHelper.putValue(DB_KEY_DECKPORT, portdeckdata.toString());
                                    break;
                                }
                            }
                        }
                        questTracker.updateIdCountTracker("609");
                        updateQuestView();
                        updateFleetView();
                    }

                    if (url.startsWith(API_REQ_HENSEI_CHANGE)) {
                        String[] requestData = request.split("&");
                        int deckIdx = -1;
                        int shipIdx = -1;
                        int shipId = -3;

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
                                for (int i = 1; i < 6; i++) {
                                    targetDeckIdxShipIdata.set(i, new JsonPrimitive(-1));
                                }
                            } else if (shipId == -1) { // remove ship
                                targetDeckIdxShipIdata.remove(shipIdx);
                                targetDeckIdxShipIdata.add(new JsonPrimitive(-1));
                            } else { // add ship
                                int originalDeckIdx = -1;
                                int originalShipIdx = -1;
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
                                JsonArray portdeckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                                JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                                portdeckdata.set(deckIdx, api_data);
                                dbHelper.putValue(DB_KEY_DECKPORT, portdeckdata.toString());
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
                            KcaApiData.updateUserShipSlot(userShipId, api_data);
                        }
                        updateFleetView();
                        toastInfo();
                    }

                    if (url.startsWith(API_REQ_KAISOU_SLOT_DEPRIVE)) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            JsonObject api_ship_data = api_data.get("api_ship_data").getAsJsonObject();
                            KcaApiData.updateUserShip(api_ship_data.get("api_set_ship").getAsJsonObject());
                            KcaApiData.updateUserShip(api_ship_data.get("api_unset_ship").getAsJsonObject());
                        }
                        updateFleetView();
                        toastInfo();
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
                            KcaApiData.deleteUserShip(itemIds);
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
                        if (certainFlag != 1 && isOpendbEnabled()) {
                            KcaOpendbAPI.sendRemodelData(flagship, assistant, itemKcId, level, api_remodel_flag);
                        }
                        questTracker.updateIdCountTracker("619");
                        updateQuestView();
                    }
                }
            }
            sendQuestCompletionInfo();
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

                showCustomToast(customToast, getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                dbHelper.recordErrorLog(ERROR_TYPE_VPN, api_url, api_request, api_response, api_error);
            }
        } catch (JsonSyntaxException e) {
            //Log.e("KCA", "ParseError");
            //Log.e("KCA", data);
            e.printStackTrace();

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

            String process_data = raw.toString();
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

        if (!prefs.getBoolean(PREF_SVC_ENABLED, false) || url.length() == 0 || viewNotificationBuilder == null) {
            Log.e("KCA", "url: " + url);
            Log.e("KCA", "viewNotificationBuilder: " + String.valueOf(viewNotificationBuilder == null));
            return;
        }

        JsonObject jsonDataObj = null;
        try {
            if (data.length() > 0) {
                jsonDataObj = new JsonParser().parse(data).getAsJsonObject();
            }

            if (url.startsWith(KCA_API_DATA_LOADED)) {
                if (jsonDataObj.has("ship")) {
                    Log.e("KCA", String.format("Ship: %d", jsonDataObj.get("ship").getAsInt()));
                    Log.e("KCA", String.format("Item: %d", jsonDataObj.get("item").getAsInt()));
                }
                api_start2_loading_flag = false;
                if (isUserItemDataLoaded) {

                    updateFleetView();
                }
            }

            if (url.startsWith(KCA_API_PREF_FAIRY_CHANGED)) {
                if (jsonDataObj.has("id")) {
                    String fairyId = "noti_icon_".concat(jsonDataObj.get("id").getAsString());
                    viewBitmapId = getId(fairyId, R.mipmap.class);
                    viewBitmapSmallId = getId(fairyId.concat("_small"), R.mipmap.class);
                    viewBitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, viewBitmapId)).getBitmap();
                    updateNotificationFairy();

                    startService(new Intent(this, KcaViewButtonService.class)
                            .setAction(KcaViewButtonService.FAIRY_CHANGE));
                }
            }

            if (url.startsWith(KCA_API_PREF_NOTICOUNT_CHANGED)) {
                int count = KcaAlarmService.getAlarmCount();
                if (count > 0) {
                    updateNotificationIcon(1);
                } else {
                    updateNotificationIcon(0);
                }
            }

            if (url.startsWith(KCA_API_PREF_LANGUAGE_CHANGED)) {
                updateExpViewNotification();
            }

            if (url.startsWith(KCA_API_PREF_PRIORITY_CHANGED)) {
                updateNotificationPriority(getPriority());
            }

            if (url.startsWith(KCA_API_NOTI_EXP_FIN)) {
                // Currently Nothing
            }

            if (url.startsWith(KCA_API_NOTI_DOCK_FIN)) {
                // Currently Nothing
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_INFO)) {
                jsonDataObj = dbHelper.getJsonObjectValue(DB_KEY_BATTLEINFO);
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
                // intent.putExtra(KCA_MSG_DATA, data);
                broadcaster.sendBroadcast(intent);
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_NODE)) {
                // Reference: https://github.com/andanteyk/ElectronicObserver/blob/1052a7b177a62a5838b23387ff35283618f688dd/ElectronicObserver/Other/Information/apilist.txt
                jsonDataObj = dbHelper.getJsonObjectValue(DB_KEY_BATTLENODE);
                if (jsonDataObj.has("api_maparea_id") && isBattleNodeEnabled()) {
                    int currentMapArea = jsonDataObj.get("api_maparea_id").getAsInt();
                    int currentMapNo = jsonDataObj.get("api_mapinfo_no").getAsInt();
                    int currentNode = jsonDataObj.get("api_no").getAsInt();
                    String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);
                    int api_event_kind = jsonDataObj.get("api_event_kind").getAsInt();
                    int api_event_id = jsonDataObj.get("api_event_id").getAsInt();
                    int api_color_no = jsonDataObj.get("api_color_no").getAsInt();
                    currentNodeInfo = KcaApiData.getNodeFullInfo(contextWithLocale, currentNodeAlphabet, api_event_id, api_event_kind, false);
                    showCustomToast(customToast, currentNodeInfo, Toast.LENGTH_LONG,
                            getNodeColor(getApplicationContext(), api_event_id, api_event_kind, api_color_no));
                    JsonObject questTrackData = dbHelper.getJsonObjectValue(DB_KEY_QTRACKINFO);
                    questTracker.updateNodeTracker(questTrackData);
                }

                Intent intent = new Intent(KCA_MSG_BATTLE_NODE);
                //intent.putExtra(KCA_MSG_DATA, data);
                broadcaster.sendBroadcast(intent);
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_DROPINFO)) {
                Log.e("KCA", KCA_API_NOTI_BATTLE_DROPINFO + " " + String.valueOf(isOpendbEnabled()));
                int world = jsonDataObj.get("world").getAsInt();
                int map = jsonDataObj.get("map").getAsInt();
                int node = jsonDataObj.get("node").getAsInt();
                String rank = jsonDataObj.get("rank").getAsString();
                int maprank = jsonDataObj.get("maprank").getAsInt();
                int inventory = jsonDataObj.get("inventory").getAsInt();
                int result = jsonDataObj.get("result").getAsInt();
                if (isOpendbEnabled()) {
                    KcaOpendbAPI.sendShipDropData(world, map, node, rank, maprank, inventory, result);
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
                    showCustomToast(customToast, getStringWithLocale(R.string.heavy_damaged), Toast.LENGTH_LONG, ContextCompat.getColor(this, R.color.colorHeavyDmgStatePanel));
                    Intent intent = new Intent(KCA_MSG_BATTLE_HDMG);
                    intent.putExtra(KCA_MSG_DATA, "1");
                    broadcaster.sendBroadcast(intent);
                } else {
                    if (heavyDamagedMode == HD_DAMECON) {
                        showCustomToast(customToast, getStringWithLocale(R.string.heavy_damaged_damecon), Toast.LENGTH_LONG, ContextCompat.getColor(this, R.color.colorHeavyDmgStatePanel));
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
                    missionTimeScheduler = Executors.newSingleThreadScheduledExecutor();
                    missionTimeScheduler.scheduleAtFixedRate(missionTimer, 0, 1, TimeUnit.SECONDS);
                } else {
                    if (missionTimeScheduler != null) missionTimeScheduler.shutdown();
                    missionTimeScheduler = null;
                }
            }

            if (url.startsWith(KCA_API_OPENDB_FAILED)) {
                showCustomToast(customToast, getStringWithLocale(R.string.opendb_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(this, R.color.colorPrimaryDark));
                dbHelper.recordErrorLog(ERROR_TYPE_OPENDB, url, "opendb", data, "failed");
            }

            if (url.startsWith(KCA_API_PROCESS_BATTLE_FAILED)) {
                String api_data = jsonDataObj.get("api_data").getAsString();
                String api_url = jsonDataObj.get("api_url").getAsString();
                String api_node = jsonDataObj.get("api_node").getAsString();
                String api_error = jsonDataObj.get("api_error").getAsString();
                showCustomToast(customToast, getStringWithLocale(R.string.process_battle_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(this, R.color.colorPrimaryDark));
                dbHelper.recordErrorLog(ERROR_TYPE_BATTLE, api_url, api_node, api_data, api_error);
            }

            sendQuestCompletionInfo();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToast(customToast, getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT, ContextCompat.getColor(this, R.color.colorPrimaryDark));
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

    private boolean isOpendbEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_OPENDB_API_USE);
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

    private boolean getPriority() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_SET_PRIORITY);
    }

    private boolean isExpNotifyEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_EXP);
    }

    private boolean isDockNotifyEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_DOCK);
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

    private void processBasicInfo(JsonObject data) {
        KcaApiData.maxShipSize = data.get("api_max_chara").getAsInt();
        KcaApiData.maxItemSize = data.get("api_max_slotitem").getAsInt();
    }

    /*private void processFirstDeckInfo() {
        KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String delimeter = " | ";
        if (!isGameDataLoaded()) {
            Log.e("KCA", "processFirstDeckInfo: Game Data is Null");
           showCustomToast(customToast, getStringWithLocale(R.string.kca_toast_get_data_at_settings), Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            // new retrieveApiStartData().execute("", "down", "");
            return;
        } else {
            Log.e("KCA", String.format("processFirstDeckInfo: data loaded"));
        }

        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);

        int cn = getSeekCn();
        String seekType = getSeekType();

        JsonArray deckInfoData = new JsonArray();
        JsonObject infoData = null;

        String airPowerValue = "";
        int[] airPowerRange = KcaDeckInfo.getAirPowerRange(data, 0, null);
        if (airPowerRange[0] > 0 && airPowerRange[1] > 0) {
            airPowerValue = String.format(getStringWithLocale(R.string.kca_toast_airpower), airPowerRange[0], airPowerRange[1]);
            infoData = new JsonObject();
            infoData.addProperty("is_newline", 0);
            infoData.addProperty("portrait_value", airPowerValue);
            infoData.addProperty("landscape_value", airPowerValue);
            deckInfoData.add(infoData);
        }

        double seekValue = 0;
        String seekStringValue = "";
        seekValue = KcaDeckInfo.getSeekValue(data, 0, cn, KcaBattle.getEscapeFlag());
        if (isCombined)
            seekValue += KcaDeckInfo.getSeekValue(data, 1, cn, KcaBattle.getEscapeFlag());
        if (cn == SEEK_PURE) {
            seekStringValue = String.format(getStringWithLocale(R.string.kca_toast_seekvalue_d), seekType, (int) seekValue);
        } else {
            seekStringValue = String.format(getStringWithLocale(R.string.kca_toast_seekvalue_f), seekType, seekValue);
        }
        infoData = new JsonObject();
        infoData.addProperty("is_newline", 0);
        infoData.addProperty("is_portrait_newline", 0);
        infoData.addProperty("portrait_value", seekStringValue);
        infoData.addProperty("landscape_value", seekStringValue);
        deckInfoData.add(infoData);

        int speedValue = 0;
        if (isCombined) {
            speedValue = KcaDeckInfo.getSpeed(data, "0,1", KcaBattle.getEscapeFlag());
        } else {
            speedValue = KcaDeckInfo.getSpeed(data, "0", KcaBattle.getEscapeFlag());
        }

        String speedStringValue = "";
        switch (speedValue) {
            case KcaApiData.SPEED_SUPERFAST:
                speedStringValue = getStringWithLocale(R.string.speed_superfast);
                break;
            case KcaApiData.SPEED_FASTPLUS:
                speedStringValue = getStringWithLocale(R.string.speed_fastplus);
                break;
            case KcaApiData.SPEED_FAST:
                speedStringValue = getStringWithLocale(R.string.speed_fast);
                break;
            case KcaApiData.SPEED_SLOW:
                speedStringValue = getStringWithLocale(R.string.speed_slow);
                break;
            case KcaApiData.SPEED_MIXED_FASTPLUS:
                speedStringValue = getStringWithLocale(R.string.speed_mixed_fastplus);
                break;
            case KcaApiData.SPEED_MIXED_FAST:
                speedStringValue = getStringWithLocale(R.string.speed_mixed_fast);
                break;
            case KcaApiData.SPEED_MIXED_NORMAL:
                speedStringValue = getStringWithLocale(R.string.speed_mixed_normal);
                break;
            default:
                speedStringValue = getStringWithLocale(R.string.speed_none);
                break;
        }

        infoData = new JsonObject();
        infoData.addProperty("is_newline", 0);
        infoData.addProperty("portrait_value", speedStringValue);
        //infoData.addProperty("landscape_value", speedStringValue.concat(getStringWithLocale(R.string.speed_postfix)));
        infoData.addProperty("landscape_value", speedStringValue);
        deckInfoData.add(infoData);

        int[] tp = new int[2];
        if (isCombined) {
            tp = KcaDeckInfo.getTPValue(data, "0,1", KcaBattle.getEscapeFlag());
        } else {
            tp = KcaDeckInfo.getTPValue(data, "0", KcaBattle.getEscapeFlag());
        }
        String tpValue = String.format(getStringWithLocale(R.string.kca_view_tpvalue), tp[1], tp[0]);
        infoData = new JsonObject();
        infoData.addProperty("is_newline", 1);
        infoData.addProperty("portrait_value", tpValue);
        infoData.addProperty("landscape_value", tpValue);
        deckInfoData.add(infoData);

        int count = 0;
        if (getBooleanPreferences(getApplicationContext(), PREF_FULLMORALE_SETTING)) {
            count = data.size();
        } else if (isCombined) {
            count = 2;
        } else {
            count = 1;
        }

        for (int i = 0; i < count; i++) {
            String conditionValue;
            if (count == 1) {
                conditionValue = String.format(getStringWithLocale(R.string.kca_view_condition), KcaDeckInfo.getConditionStatus(data, i));
            } else {
                conditionValue = String.format(getStringWithLocale(R.string.kca_view_condition_nformat), i + 1, KcaDeckInfo.getConditionStatus(data, i));
            }
            if (conditionValue.length() == 0) continue;

            infoData = new JsonObject();
            if (i < count - 1) {
                infoData.addProperty("is_newline", 1);
            } else {
                infoData.addProperty("is_newline", 0);
            }
            infoData.addProperty("portrait_value", conditionValue);
            infoData.addProperty("landscape_value", conditionValue);
            deckInfoData.add(infoData);
        }

        kcaFirstDeckInfo = gson.toJson(deckInfoData);
        
    }*/

    private void processExpeditionInfo() {
        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
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
                if (mission_no == -1 || isNotIdenticalMission) {
                    PendingIntent pendingIntent = PendingIntent.getService(
                            getApplicationContext(),
                            getNotificationId(NOTI_EXP, i),
                            new Intent(getApplicationContext(), KcaAlarmService.class),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    pendingIntent.cancel();
                    alarmManager.cancel(pendingIntent);
                    KcaExpedition2.clearMissionData(i);
                }
                if (arrive_time != KcaExpedition2.getArriveTime(i)) {
                    KcaExpedition2.setMissionData(i, deck_name, mission_no, arrive_time);
                    setExpeditionAlarm(i, mission_no, deck_name, arrive_time, false, false, aIntent);
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

    private void processDockingInfo(JsonArray data) {
        int dockId, shipId, state;
        long completeTime;

        for (int i = 0; i < data.size(); i++) {
            JsonObject ndockData = data.get(i).getAsJsonObject();
            state = ndockData.get("api_state").getAsInt();
            int nid = getNotificationId(NOTI_DOCK, i);
            notifiManager.cancel(nid);
            Intent deleteIntent = new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid)));
            startService(deleteIntent);

            if (state != -1) {
                dockId = ndockData.get("api_id").getAsInt() - 1;
                shipId = ndockData.get("api_ship_id").getAsInt();
                completeTime = ndockData.get("api_complete_time").getAsLong();
                Intent aIntent = new Intent(getApplicationContext(), KcaAlarmService.class);
                if (KcaDocking.getCompleteTime(dockId) != -1) {
                    if (KcaDocking.getShipId(dockId) != shipId) {
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

    public static boolean isJSONValid(String jsonInString) {
        Gson gson = new Gson();
        try {
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    private boolean isCurrentPortDeckDataReady() {
        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (data == null) {
            KcaCustomToast customToast = new KcaCustomToast(getApplicationContext());
            showCustomToast(customToast, getStringWithLocale(R.string.kca_toast_restart_at_kcanotify), Toast.LENGTH_LONG, ContextCompat.getColor(this, R.color.colorPrimaryDark));
            Log.e("KCA", String.format("currentPortDeckData is null"));
            return false;
        } else {
            return true;
        }
    }

    private void setAlarm(long time, PendingIntent alarmIntent, int code) {
        if (time == -1) {
            time = System.currentTimeMillis();
        } else {
            time = time - KcaAlarmService.ALARM_DELAY;
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
    }

    public void updateFleetView() {
        startService(new Intent(getBaseContext(), KcaFleetViewService.class)
                .setAction(REFRESH_FLEETVIEW_ACTION));
    }

    public void showCustomToast(KcaCustomToast toast, String body, int duration, int color) {
        Context ctx = getApplicationContext();
        if (getBooleanPreferences(ctx, PREF_DISABLE_CUSTOMTOAST)) {
            Toast.makeText(ctx, body, duration).show();
        } else {
            toast.showToast(body, duration, color);
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
        loadTranslationData(getAssets(), getApplicationContext());


        super.onConfigurationChanged(newConfig);
    }
}