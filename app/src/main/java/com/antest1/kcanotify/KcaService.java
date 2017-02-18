package com.antest1.kcanotify;

import android.app.Application;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AbstractAjaxCallback;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import static com.antest1.kcanotify.KcaApiData.checkDataLoadTriggered;
import static com.antest1.kcanotify.KcaApiData.getNodeColor;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.isUserItemDataLoaded;
import static com.antest1.kcanotify.KcaApiData.loadItemTranslationDataFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadMapEdgeInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipInitEquipCountFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipTranslationDataFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.setDataLoadTriggered;
import static com.antest1.kcanotify.KcaApiData.updateUserShip;
import static com.antest1.kcanotify.KcaConstants.*;

import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaUtils.*;

public class KcaService extends Service {
    public static String currentLocale;

    public static boolean isServiceOn = false;
    public static boolean isPortAccessed = false;
    public static int heavyDamagedMode = 0;
    public static int checkKdockId = -1;
    public static boolean kaisouProcessFlag = false;
    public static String currentNode = "";
    public static String currentNodeInfo = "";
    public static boolean isInBattle;
    public static boolean isCombined;

    Context contextWithLocale;

    AudioManager mAudioManager;
    Vibrator vibrator = null;
    MediaPlayer mediaPlayer;
    NotificationManager notifiManager;
    Builder viewNotificationBuilder;
    BigTextStyle viewNotificationText;
    KcaBattleToast battleToast;
    public static boolean noti_vibr_on = true;

    kcaServiceHandler handler;
    kcaNotificationHandler nHandler;
    LocalBroadcastManager broadcaster;

    Thread[] kcaExpeditionList = new Thread[3];
    KcaExpedition[] kcaExpeditionRunnableList = new KcaExpedition[3];
    String[] kcaExpeditionInfoList = new String[3];

    Thread[] kcaDockingList = new Thread[4];
    KcaDocking[] kcaDockingRunnableList = new KcaDocking[4];

    String kcaFirstDeckInfo;
    static String kca_version;
    String api_start2_data = null;
    boolean api_start2_down_mode = false;
    boolean api_start2_init = false;
    boolean api_start2_loading_flag = false;
    public JsonArray currentPortDeckData = null;
    Gson gson = new Gson();

    public static boolean getServiceStatus() {
        return isServiceOn;
    }

    private boolean checkKeyInPreferences(String key) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        return pref.contains(key);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.e("KCA", String.valueOf(prefs.contains(PREF_SVC_ENABLED)));
        if (!prefs.getBoolean(PREF_SVC_ENABLED, false)) {
            stopSelf();
        }
        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());

        for (int i = 0; i < 3; i++) {
            kcaExpeditionList[i] = null;
            kcaExpeditionInfoList[i] = "";
        }

        AssetManager assetManager = getResources().getAssets();
        int loadMapEdgeInfoResult = loadMapEdgeInfoFromAssets(assetManager);
        if (loadMapEdgeInfoResult != 1) {
            Toast.makeText(this, "Error loading Map Edge Info", Toast.LENGTH_LONG).show();
        }

        int loadShipTranslationDataResult = loadShipTranslationDataFromAssets(assetManager,
                getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
        if (loadShipTranslationDataResult != 1) {
            Toast.makeText(this, "Error loading Translation Info", Toast.LENGTH_LONG).show();
        }

        int loadItemTranslationDataResult = loadItemTranslationDataFromAssets(assetManager,
                getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
        if (loadItemTranslationDataResult != 1) {
            Toast.makeText(this, "Error loading Translation Info", Toast.LENGTH_LONG).show();
        }

        loadSimpleExpeditionInfoFromAssets(assetManager);
        KcaExpedition.expeditionData = KcaApiData.kcSimpleExpeditionData;

        loadShipInitEquipCountFromAssets(assetManager);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.i("Completion Listener", "Song Complete");
                mp.stop();
                mp.reset();
            }
        });

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notifiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        battleToast = new KcaBattleToast(this);

        handler = new kcaServiceHandler(this);
        nHandler = new kcaNotificationHandler(this);
        broadcaster = LocalBroadcastManager.getInstance(this);

        kcaExpeditionRunnableList = new KcaExpedition[3];
        kcaDockingRunnableList = new KcaDocking[4];

        AbstractAjaxCallback.setGZip(true);

        String initTitle = String.format(getStringWithLocale(R.string.kca_init_title), getStringWithLocale(R.string.app_name));
        String initContent = getStringWithLocale(R.string.kca_init_content);
        String initSubContent = String.format("%s %s", getStringWithLocale(R.string.app_name), getStringWithLocale(R.string.app_version));
        kcaFirstDeckInfo = getStringWithLocale(R.string.kca_init_content);
        startForeground(getNotificationId(NOTI_FRONT, 1), createViewNotification(initTitle, initContent, initSubContent));
        isServiceOn = true;

        KcaExpedition.setContext(getApplicationContext());

        KcaVpnData.setHandler(handler);

        KcaBattle.setHandler(nHandler);
        KcaApiData.setHandler(nHandler);
        KcaOpendbAPI.setHandler(nHandler);
        SettingActivity.setHandler(nHandler);

        return START_STICKY;
    }

    // 서비스가 종료될 때 할 작업

    public void setServiceDown() {
        MainActivity.isKcaServiceOn = false;
        KcaApiData.userItemData = null;
        for (int i = 0; i < 3; i++) {
            if (kcaExpeditionList[i] != null) {
                kcaExpeditionRunnableList[i].stopHandler();
                kcaExpeditionList[i].interrupt();
                kcaExpeditionList[i] = null;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (kcaDockingRunnableList[i] != null) {
                kcaDockingRunnableList[i].stopHandler();
                kcaDockingList[i].interrupt();
                kcaDockingList[i] = null;
            }
        }

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
        stopService(new Intent(this, KcaViewButtonService.class));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("svcenabled", false).apply();
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


    private Notification createViewNotification(String title, String content1, String content2) {
        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Drawable drawable = ContextCompat.getDrawable(this, R.mipmap.noti_icon3);
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        viewNotificationText = new Notification.BigTextStyle();
        viewNotificationBuilder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.noti_icon4)
                .setLargeIcon(bitmap)
                .setContentTitle(title)
                .setContentText(getStringWithLocale(R.string.kca_view_context_expand))
                .setStyle(viewNotificationText.bigText(content1))
                .setTicker(title)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true).setAutoCancel(false);
        if (isExpViewEnabled()) {
            viewNotificationBuilder.setStyle(viewNotificationText.setSummaryText(content2));
        }
        return viewNotificationBuilder.build();
    }

    private Notification createExpeditionNotification(int missionNo, String missionName, String kantaiName, boolean cancelFlag, boolean caFlag) {
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, getKcIntent(getApplicationContext()),
                PendingIntent.FLAG_UPDATE_CURRENT);
        String title = "";
        String content = "";
        if (cancelFlag) {
            title = String.format(getStringWithLocale(R.string.kca_noti_title_exp_canceled), missionNo, missionName);
            content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_canceled), kantaiName, missionNo);
        } else {
            title = String.format(getStringWithLocale(R.string.kca_noti_title_exp_finished), missionNo, missionName);
            if (caFlag)
                content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_finished_canceled), kantaiName, missionNo);
            else
                content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_finished_normal), kantaiName, missionNo);

        }
        Drawable drawable = ContextCompat.getDrawable(this,  R.mipmap.expedition_notify_bigicon);
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.expedition_notify_icon)
                .setLargeIcon(bitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setContentIntent(pendingIntent);

        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                if (soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                builder.setSound(Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
            } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_vibrate))) {
            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_mute))) {
            builder.setDefaults(0);
        }

        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;
        setFrontViewNotifier(FRONT_NONE, 0, null);
        return Notifi;
    }

    private Notification createDockingNotification(int dockId, String shipName) {
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, getKcIntent(getApplicationContext()),
                PendingIntent.FLAG_UPDATE_CURRENT);
        String title = String.format(getStringWithLocale(R.string.kca_noti_title_dock_finished), dockId + 1);
        String content = "";
        if (shipName.length() > 0) {
            content = String.format(getStringWithLocale(R.string.kca_noti_content_dock_finished), dockId + 1, shipName);
        } else {
            content = String.format(getStringWithLocale(R.string.kca_noti_content_dock_finished_nodata), dockId + 1);
        }

        Drawable drawable = ContextCompat.getDrawable(this,  R.mipmap.dockng_notify_bigicon);
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.docking_notify_icon)
                .setLargeIcon(bitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setContentIntent(pendingIntent);
        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                if (soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                builder.setSound(Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
            } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_vibrate))) {
            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_mute))) {
            builder.setDefaults(0);
        }

        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;
        setFrontViewNotifier(FRONT_NONE, 0, null);
        return Notifi;
    }

    private void toastInfo() {
        if (!KcaApiData.isGameDataLoaded()) return;
        int cn = getSeekCn();
        String seekType = getSeekType();
        int[] airPowerRange = KcaDeckInfo.getAirPowerRange(currentPortDeckData, 0, KcaBattle.getEscapeFlag());
        String airPowerValue = String.format(getStringWithLocale(R.string.kca_toast_airpower), airPowerRange[0], airPowerRange[1]);
        String seekValue = String.format(getStringWithLocale(R.string.kca_toast_seekvalue_f), seekType, KcaDeckInfo.getSeekValue(currentPortDeckData, 0, cn, KcaBattle.getEscapeFlag()));
        int[] tp = KcaDeckInfo.getTPValue(currentPortDeckData, "0", KcaBattle.getEscapeFlag());
        String tpValue = String.format(getStringWithLocale(R.string.kca_view_tpvalue), tp[0], tp[1]);
        List<String> toastList = new ArrayList<String>();
        if (airPowerRange[1] > 0) {
            toastList.add(airPowerValue);
        }
        toastList.add(seekValue);
        if (tp[0] > 0) {
            toastList.add(tpValue);
        }
        Toast.makeText(getApplicationContext(), joinStr(toastList, " / "), Toast.LENGTH_LONG).show();
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
        String data = msg.getData().getString("data");
        String request = msg.getData().getString("request");

        if (!prefs.getBoolean(PREF_SVC_ENABLED, false) || url.length() == 0 || viewNotificationBuilder == null) {
            return;
        }

        JsonObject jsonDataObj;
        try {
            jsonDataObj = new JsonParser().parse(data).getAsJsonObject();

            if (url.startsWith(KCA_VERSION)) {
                isPortAccessed = false;
                api_start2_init = false;
                api_start2_loading_flag = true;
                stopService(new Intent(this, KcaViewButtonService.class));
                //Toast.makeText(contextWithLocale, "KCA_VERSION", Toast.LENGTH_LONG).show();
                JsonObject api_version = jsonDataObj.get("api").getAsJsonObject();
                kca_version = api_version.get("api_start2").getAsString();
                Log.e("KCA", kca_version);
                if (!getStringPreferences(getApplicationContext(), "kca_version").equals(kca_version)) {
                    api_start2_down_mode = true;
                } else {
                    api_start2_down_mode = false;
                    JsonObject kcDataObj = readCacheData(getApplicationContext(), KCANOTIFY_S2_CACHE_FILENAME);
                    //Log.e("KCA", kcDataObj.toJSONString());
                    if (kcDataObj != null && kcDataObj.has("api_data")) {
                        //Toast.makeText(contextWithLocale, "Load Kancolle Data", Toast.LENGTH_LONG).show();
                        KcaApiData.getKcGameData(kcDataObj.getAsJsonObject("api_data"));
                        setPreferences(getApplicationContext(), "kca_version", kca_version);
                    }
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

                api_start2_data = data;
                writeCacheData(getApplicationContext(), data.getBytes(), KCANOTIFY_S2_CACHE_FILENAME);

                if (jsonDataObj.has("api_data")) {
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

                if (jsonDataObj.has("api_data")) {
                    JsonObject requiredInfoApiData = jsonDataObj.getAsJsonObject("api_data");
                    int size = KcaApiData.getSlotItemData(requiredInfoApiData);
                    int userId = KcaApiData.getUserId(requiredInfoApiData);
                    //Log.e("KCA", "Total Items: " + String.valueOf(size));
                    //Toast.makeText(contextWithLocale, String.valueOf(userId), Toast.LENGTH_LONG).show();

                    if (api_start2_data == null && api_start2_down_mode) {
                        Toast.makeText(getApplicationContext(), String.format("Downloading Data"), Toast.LENGTH_LONG).show();
                        new retrieveApiStartData().execute("", "down", "");
                        setFrontViewNotifier(FRONT_NONE, 0, getStringWithLocale(R.string.kca_toast_loading_data));
                    }
                }
                return;
            }

            if (url.startsWith(API_GET_MEMBER_DECK)) {
                //Log.e("KCA", "Expedition Handler Called");
                if (jsonDataObj.has("api_data")) {
                    JsonArray reqGetMemberDeckApiData = jsonDataObj.get("api_data").getAsJsonArray();
                    processExpeditionInfo(reqGetMemberDeckApiData);
                }
                return;
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
                notifiManager.cancel(getNotificationId(NOTI_EXP, deck_id - 1));
            }

            if (url.startsWith(API_GET_MEMBER_NDOCK)) {
                if (jsonDataObj.has("api_data")) {
                    JsonArray api_data = jsonDataObj.getAsJsonArray("api_data");
                    processDockingInfo(api_data);
                }
                return;
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
                return;
            }

            if (url.startsWith(API_PORT)) {
                isPortAccessed = true;
                if (jsonDataObj.has("api_data")) {
                    JsonObject reqPortApiData = jsonDataObj.getAsJsonObject("api_data");
                    KcaApiData.getPortData(reqPortApiData);
                    if (reqPortApiData.has("api_deck_port")) {
                        currentPortDeckData = reqPortApiData.getAsJsonArray("api_deck_port");
                    }
                }
            }

            // Game Data Dependent Tasks
            if (!isUserItemDataLoaded()) {
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.kca_toast_restart_at_kcanotify), Toast.LENGTH_LONG).show();
            } else if (!checkDataLoadTriggered()) {
                if(!api_start2_loading_flag) {
                    Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.kca_toast_get_data_at_settings), Toast.LENGTH_LONG).show();
                    new retrieveApiStartData().execute("", "down", "");
                }
            } else if (api_start2_loading_flag) {
                setFrontViewNotifier(FRONT_NONE, 0, getStringWithLocale(R.string.kca_toast_loading_data));
            } else {
                if (url.startsWith(API_PORT)) {
                    heavyDamagedMode = HD_NONE;
                    currentNode = "";
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
                            processFirstDeckInfo(currentPortDeckData);
                            processExpeditionInfo(currentPortDeckData);
                        }
                        if (reqPortApiData.has("api_ndock")) {
                            JsonArray nDockData = reqPortApiData.getAsJsonArray("api_ndock");
                            processDockingInfo(nDockData);
                        }
                    }
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    if (isInBattle) {
                        stopService(new Intent(this, KcaViewButtonService.class));
                        isInBattle = false;
                    }

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
                }

                if (url.startsWith(API_GET_MEMBER_MAPINFO) || url.startsWith(API_GET_MEMBER_MISSION)) {
                    if (url.startsWith(API_GET_MEMBER_MAPINFO) && jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        JsonArray api_map_info = api_data.getAsJsonArray("api_map_info");
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

                    for (int i = 0; i < currentPortDeckData.size(); i++) {
                        if (url.startsWith(API_GET_MEMBER_MISSION) && i == 0) continue;
                        if (KcaDeckInfo.checkNotSuppliedExist(currentPortDeckData, i)) {
                            isNotSuppliedFlag = true;
                            message = message.concat(String.format(getStringWithLocale(R.string.not_supplied), i + 1)).concat("\n");
                        }
                    }

                    if (url.startsWith(API_GET_MEMBER_MAPINFO)) {
                        int firstHeavyDamaged = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, 0);
                        int secondHeavyDamaged = 0;
                        if (currentPortDeckData.size() >= 2) {
                            secondHeavyDamaged = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, 1);
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
                                    Uri notificationUri = Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
                                    Log.e("KCA", getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
                                    KcaUtils.playNotificationSound(mediaPlayer, getApplicationContext(), notificationUri);
                                }
                            }
                            vibrator.vibrate(1500);
                        }

                        Toast.makeText(getApplicationContext(), message.trim(), Toast.LENGTH_LONG).show();
                    }

                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }

                if (API_BATTLE_REQS.contains(url)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject battleApiData = jsonDataObj.getAsJsonObject("api_data");
                        if (url.startsWith(API_REQ_MAP_START) || url.startsWith(API_REQ_PRACTICE_BATTLE)) {
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

                            if (getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                        && !Settings.canDrawOverlays(getApplicationContext())) {
                                    // Can not draw overlays: pass
                                } else {
                                    startService(new Intent(this, KcaViewButtonService.class));
                                }
                            }

                            JsonObject api_data = new JsonObject();
                            JsonArray api_deck_data = new JsonArray();
                            JsonArray api_ship_data = new JsonArray();
                            int checkvalue = 0;

                            if (isCombined) { // TODO: Combined Fleet for Event
                                api_deck_data.add(currentPortDeckData.get(0));
                                JsonArray firstShipInfo = currentPortDeckData.get(0).getAsJsonObject().getAsJsonArray("api_ship");
                                for (JsonElement e : firstShipInfo) {
                                    int ship_id = e.getAsInt();
                                    if (ship_id != -1)
                                        api_ship_data.add(KcaApiData.getUserShipDataById(ship_id, "all"));
                                }
                                api_deck_data.add(currentPortDeckData.get(1));
                                JsonArray secondShipInfo = currentPortDeckData.get(1).getAsJsonObject().getAsJsonArray("api_ship");
                                for (JsonElement e : secondShipInfo) {
                                    int ship_id = e.getAsInt();
                                    if (ship_id != -1)
                                        api_ship_data.add(KcaApiData.getUserShipDataById(ship_id, "all"));
                                }
                                KcaBattle.dameconflag = KcaDeckInfo.getDameconStatus(api_deck_data, 0);
                                KcaBattle.dameconcbflag = KcaDeckInfo.getDameconStatus(api_deck_data, 1);

                                int firstHeavyDamaged = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, 0);
                                int secondHeavyDamaged = 0;
                                if (currentPortDeckData.size() >= 2) {
                                    secondHeavyDamaged = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, 1);
                                }
                                checkvalue = Math.max(firstHeavyDamaged, secondHeavyDamaged);
                            } else {
                                int fleetHeavyDamaged = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, deck_id);
                                checkvalue = fleetHeavyDamaged;
                                api_deck_data.add(currentPortDeckData.get(deck_id));
                                JsonArray firstShipInfo = currentPortDeckData.get(deck_id).getAsJsonObject().getAsJsonArray("api_ship");
                                for (JsonElement e : firstShipInfo) {
                                    int ship_id = e.getAsInt();
                                    if (ship_id != -1)
                                        api_ship_data.add(KcaApiData.getUserShipDataById(ship_id, "all"));
                                }
                                KcaBattle.dameconflag = KcaDeckInfo.getDameconStatus(api_deck_data, 0);
                            }
                            api_data.add("api_deck_data", api_deck_data);
                            api_data.add("api_ship_data", api_ship_data);
                            KcaBattle.setDeckPortData(api_data);
                            KcaBattle.setStartHeavyDamageExist(checkvalue);
                        }
                        KcaBattle.processData(url, battleApiData);
                    } else if (url.equals(API_REQ_COMBINED_GOBACKPORT)) {
                        KcaBattle.processData(url, null);
                    }
                }

                if (url.startsWith(API_GET_MEMBER_SHIP_DECK)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        JsonArray api_deck_data = (JsonArray) api_data.get("api_deck_data");
                        KcaApiData.updatePortDataOnBattle(api_data);
                        for (int i = 0; i < api_deck_data.size(); i++) {
                            if (i == 0) {
                                KcaBattle.dameconflag = KcaDeckInfo.getDameconStatus(api_deck_data, 0);
                            } else if (i == 1) {
                                KcaBattle.dameconcbflag = KcaDeckInfo.getDameconStatus(api_deck_data, 1);
                            }
                        }
                        KcaBattle.setDeckPortData(api_data);
                        processFirstDeckInfo(currentPortDeckData);
                    }
                }

                if (url.startsWith(API_REQ_MISSION_RETURN)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject reqGetMemberDeckApiData = jsonDataObj.getAsJsonObject("api_data");
                        cancelExpeditionInfo(reqGetMemberDeckApiData);
                    }
                    return;
                }

                if (url.startsWith(API_REQ_MEMBER_GET_PRACTICE_ENEMYINFO)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        KcaBattle.currentEnemyDeckName = api_data.get("api_deckname").getAsString();
                    }
                }

                if (url.startsWith(API_GET_MEMBER_SLOT_ITEM)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonArray api_data = jsonDataObj.get("api_data").getAsJsonArray();
                        KcaApiData.updateSlotItemData(api_data);
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_CREATETIEM)) {
                    String[] requestData = request.split("&");
                    int[] materials = {0, 0, 0, 0};

                    int flagship = KcaDeckInfo.getKcShipList(currentPortDeckData, 0)[0];
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
                        int itemKcId = KcaApiData.addUserItem(api_data);
                        if (isOpendbEnabled()) {
                            KcaOpendbAPI.sendEquipDevData(flagship, materials[0], materials[1], materials[2], materials[3], itemKcId);
                        }
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_DESTROYITEM)) {
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_slotitem_ids")) {
                            String itemlist = decodedData.replace("api_slotitem_ids=", "");
                            KcaApiData.deleteUserItem(itemlist);
                            break;
                        }
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_CREATESHIP)) {
                    String[] requestData = request.split("&");
                    for (int i = 0; i < requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if (decodedData.startsWith("api_kdock_id=")) {
                            checkKdockId = Integer.valueOf(decodedData.replace("api_kdock_id=", "")) - 1;
                            break;
                        }
                    }
                }

                if (url.startsWith(API_GET_MEMBER_KDOCK)) {
                    Log.e("KCA", String.valueOf(checkKdockId));
                    if (checkKdockId != -1 && jsonDataObj.has("api_data")) {
                        JsonArray api_data = jsonDataObj.getAsJsonArray("api_data");
                        JsonObject api_kdock = api_data.get(checkKdockId).getAsJsonObject();
                        int flagship = KcaDeckInfo.getKcShipList(currentPortDeckData, 0)[0];
                        int[] materials = {0, 0, 0, 0, 0};
                        for (int i = 0; i < materials.length; i++) {
                            materials[i] = api_kdock.get(String.format("api_item%d", i + 1)).getAsInt();
                        }
                        int created_ship_id = api_kdock.get("api_created_ship_id").getAsInt();
                        if (isOpendbEnabled()) {
                            KcaOpendbAPI.sendShipDevData(flagship, materials[0], materials[1], materials[2], materials[3], materials[4], created_ship_id);
                        }
                        checkKdockId = -1;
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_GETSHIP)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        KcaApiData.addUserShip(api_data);
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
                    for (int i = 0; i < currentPortDeckData.size(); i++) {
                        JsonObject deckData = currentPortDeckData.get(i).getAsJsonObject();
                        JsonArray deckShipData = deckData.get("api_ship").getAsJsonArray();
                        for (int j = 0; j < deckShipData.size(); j++) {
                            if (targetShip.equals(String.valueOf(deckShipData.get(j).getAsInt()))) {
                                deckShipData.set(j, new JsonPrimitive(-1));
                                deckData.add("api_ship", deckShipData);
                                currentPortDeckData.set(i, deckData);
                                break;
                            }
                        }
                    }
                    processFirstDeckInfo(currentPortDeckData);
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
                        JsonObject targetDeckIdxData = currentPortDeckData.get(deckIdx).getAsJsonObject();
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
                            for (int i = 0; i < currentPortDeckData.size(); i++) {
                                JsonArray deckData = currentPortDeckData.get(i).getAsJsonObject().get("api_ship").getAsJsonArray();
                                for (int j = 0; j < deckData.size(); j++) {
                                    if (shipId == deckData.get(j).getAsInt()) {
                                        originalDeckIdx = i;
                                        originalShipIdx = j;
                                        break;
                                    }
                                }
                            }
                            if (originalDeckIdx != -1) { // if in deck
                                JsonObject sourceDeckIdxData = currentPortDeckData.get(originalDeckIdx).getAsJsonObject();
                                JsonArray sourceDeckIdxShipIdata = sourceDeckIdxData.get("api_ship").getAsJsonArray();
                                JsonElement replacement = targetDeckIdxShipIdata.get(shipIdx);
                                if (replacement.getAsInt() != -1) {
                                    sourceDeckIdxShipIdata.set(originalShipIdx, replacement);
                                } else {
                                    sourceDeckIdxShipIdata.remove(originalShipIdx);
                                    sourceDeckIdxShipIdata.add(new JsonPrimitive(-1));
                                    sourceDeckIdxData.add("api_ship", sourceDeckIdxShipIdata);
                                    currentPortDeckData.set(originalDeckIdx, sourceDeckIdxData);
                                }
                            }
                            targetDeckIdxShipIdata.set(shipIdx, new JsonPrimitive(shipId)); // replace
                        }
                        targetDeckIdxData.add("api_ship", targetDeckIdxShipIdata);
                        currentPortDeckData.set(deckIdx, targetDeckIdxData);
                    }
                    processFirstDeckInfo(currentPortDeckData);
                }

                if (url.startsWith(API_REQ_HENSEI_PRESET)) {
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
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            currentPortDeckData.set(deckIdx, api_data);
                        }
                    }
                    processFirstDeckInfo(currentPortDeckData);
                }

                if (url.startsWith(API_REQ_HENSEI_COMBINED)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        int api_combined = api_data.get("api_combined").getAsInt();
                        isCombined = (api_combined > 0);
                        if (api_combined > 0) {
                            KcaBattle.isCombined = true;
                        } else {
                            KcaBattle.isCombined = false;
                        }
                    }
                    Log.e("KCA", "Combined: " + String.valueOf(isCombined));
                    processFirstDeckInfo(currentPortDeckData);
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
                            currentPortDeckData = api_data.get("api_deck_data").getAsJsonArray();
                            KcaApiData.updateUserShip(api_data.get("api_ship_data").getAsJsonArray().get(0).getAsJsonObject());
                        }
                    }
                    if (kaisouProcessFlag) {
                        processFirstDeckInfo(currentPortDeckData);
                        toastInfo();
                        kaisouProcessFlag = false;
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
                    processFirstDeckInfo(currentPortDeckData);
                    toastInfo();
                }

                if (url.startsWith(API_REQ_KAISOU_SLOT_DEPRIVE)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                        JsonObject api_ship_data = api_data.get("api_ship_data").getAsJsonObject();
                        KcaApiData.updateUserShip(api_ship_data.get("api_set_ship").getAsJsonObject());
                        KcaApiData.updateUserShip(api_ship_data.get("api_unset_ship").getAsJsonObject());
                    }
                    processFirstDeckInfo(currentPortDeckData);
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
                        currentPortDeckData = api_data.getAsJsonArray("api_deck");
                        updateUserShip(api_data.getAsJsonObject("api_ship"));
                        KcaApiData.deleteUserShip(itemIds);
                    }
                    processFirstDeckInfo(currentPortDeckData);
                }

                if (url.equals(API_REQ_KOUSYOU_REMOEL_SLOT)) {
                    int[] kcShipData = KcaDeckInfo.getKcShipList(currentPortDeckData, 0);
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
                            KcaApiData.updateSlotItemData(api_slot_item);
                        }
                        JsonElement use_slot_id = api_data.get("api_use_slot_id");
                        List<String> use_slot_id_list = new ArrayList<String>();
                        if (use_slot_id != null) {
                            for (JsonElement id : use_slot_id.getAsJsonArray()) {
                                use_slot_id_list.add(id.getAsString());
                            }
                            KcaApiData.deleteUserItem(joinStr(use_slot_id_list, ","));
                        }
                    }
                    if (certainFlag != 1 && isOpendbEnabled()) {
                        KcaOpendbAPI.sendRemodelData(flagship, assistant, itemKcId, level, api_remodel_flag);
                    }
                }
            }

            if (url.equals(KCA_API_VPN_DATA_ERROR)) { // VPN Data Dump Send
                String app_version = BuildConfig.VERSION_NAME;
                String token = "a49467944c34d567fa7f2b051a59c808";
                String api_url = jsonDataObj.get("uri").getAsString();
                String api_request = "see_data";

                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT).show();
                String dataSendUrl = String.format(getStringWithLocale(R.string.errorlog_service_link), token, api_url, api_request, app_version);
                AjaxCallback<String> cb = new AjaxCallback<String>() {
                    @Override
                    public void callback(String url, String data, AjaxStatus status) {
                        // do nothing
                    }
                };
                AQuery aq = new AQuery(KcaService.this);
                cb.header("Referer", "app:/KCA/");
                cb.header("Content-Type", "application/x-www-form-urlencoded");
                HttpEntity entity = null;

                JsonObject sendData = new JsonObject();
                //sendData.addProperty("data", data);
                sendData.addProperty("error", jsonDataObj.toString());
                String sendDataString = sendData.toString();

                entity = new ByteArrayEntity(sendDataString.getBytes());
                cb.param(AQuery.POST_ENTITY, entity);
                aq.ajax(dataSendUrl, String.class, cb);
            }

        } catch (JsonSyntaxException e) {
            //Log.e("KCA", "ParseError");
            //Log.e("KCA", data);
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();

            String app_version = BuildConfig.VERSION_NAME;
            String token = "a49467944c34d567fa7f2b051a59c808";
            String api_url = "";
            String api_request = "";
            List<String> filtered_resquest_list = new ArrayList<String>();
            try {
                api_url = URLEncoder.encode(url, "utf-8");
                String[] requestData = request.split("&");
                for (int i = 0; i < requestData.length; i++) {
                    String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                    if (!decodedData.startsWith("api_token")) {
                        filtered_resquest_list.add(requestData[i]);
                    }
                }
                api_request = URLEncoder.encode(joinStr(filtered_resquest_list, "&"), "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT).show();
            String dataSendUrl = String.format(getStringWithLocale(R.string.errorlog_service_link), token, api_url, api_request, app_version);
            AjaxCallback<String> cb = new AjaxCallback<String>() {
                @Override
                public void callback(String url, String data, AjaxStatus status) {
                    // do nothing
                }
            };
            AQuery aq = new AQuery(KcaService.this);
            cb.header("Referer", "app:/KCA/");
            cb.header("Content-Type", "application/x-www-form-urlencoded");
            HttpEntity entity = null;

            JsonObject sendData = new JsonObject();
            //sendData.addProperty("data", data);
            sendData.addProperty("error", getStringFromException(e));
            String sendDataString = sendData.toString();

            entity = new ByteArrayEntity(sendDataString.getBytes());
            cb.param(AQuery.POST_ENTITY, entity);
            aq.ajax(dataSendUrl, String.class, cb);
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

        if (!prefs.getBoolean(PREF_SVC_ENABLED, false) || url.length() == 0 || viewNotificationBuilder == null) {
            Log.e("KCA", "url: "+ url);
            Log.e("KCA", "viewNotificationBuilder: "+String.valueOf(viewNotificationBuilder == null));
            return;
        }

        JsonObject jsonDataObj;
        try {
            jsonDataObj = new JsonParser().parse(data).getAsJsonObject();
            if (url.startsWith(KCA_API_DATA_LOADED)) {
                Log.e("KCA", String.format("Ship: %d", jsonDataObj.get("ship").getAsInt()));
                Log.e("KCA", String.format("Item: %d", jsonDataObj.get("item").getAsInt()));
                api_start2_loading_flag = false;
                processFirstDeckInfo(currentPortDeckData);
            }

            if (url.startsWith(KCA_API_NOTI_EXP_LEFT)) {
                // //Log.e("KCA", "Expedition Notification Handler Called");
                //int kantaiIndex = ((Long) jsonDataObj.get("idx")).intValue();
                //String str_data = (String) jsonDataObj.get("str");
                if (isExpViewEnabled()) {
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }
            }

            if (url.startsWith(KCA_API_NOTI_EXP_CANCELED)) {
                // //Log.e("KCA", "Expedition Notification Handler Called");
                int kantaiIndex = jsonDataObj.get("kantai_idx").getAsInt();
                String kantaiName = jsonDataObj.get("kantai_name").getAsString();
                int missionNo = jsonDataObj.get("mission_no").getAsInt();
                String missionName = jsonDataObj.get("mission_krname").getAsString();
                //Intent intent = new Intent(KcaService.this, MainActivity.class);

                if (isExpNotifyEnabled()) {
                    notifiManager.notify(getNotificationId(NOTI_EXP, kantaiIndex), createExpeditionNotification(missionNo, missionName, kantaiName, true, false));
                }
            }

            if (url.startsWith(KCA_API_NOTI_EXP_FIN)) {
                // //Log.e("KCA", "Expedition Notification Handler Called");
                int kantaiIndex = jsonDataObj.get("kantai_idx").getAsInt();
                String kantaiName = jsonDataObj.get("kantai_name").getAsString();
                int missionNo = jsonDataObj.get("mission_no").getAsInt();
                String missionName = jsonDataObj.get("mission_krname").getAsString();
                boolean missionCanceled = jsonDataObj.get("mission_canceled").getAsBoolean();
                //Intent intent = new Intent(KcaService.this, MainActivity.class);

                kcaExpeditionInfoList[kantaiIndex] = "";
                if (isExpNotifyEnabled()) {
                    Log.e("KCA", "fin " + String.valueOf(kantaiIndex));
                    notifiManager.notify(getNotificationId(NOTI_EXP, kantaiIndex), createExpeditionNotification(missionNo, missionName, kantaiName, false, missionCanceled));
                }
            }

            if (url.startsWith(KCA_API_NOTI_DOCK_FIN)) {
                int dockId = jsonDataObj.get("dock_no").getAsInt();
                String shipName = getShipTranslation(jsonDataObj.get("ship_name").getAsString(), false);
                if (isDockNotifyEnabled()) {
                    notifiManager.notify(getNotificationId(NOTI_DOCK, dockId), createDockingNotification(dockId, shipName));
                }
                kcaDockingRunnableList[dockId] = null;
                kcaDockingList[dockId] = null;
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_INFO)) {
                Intent intent = new Intent(KCA_MSG_BATTLE_INFO);
                intent.putExtra(KCA_MSG_DATA, data);
                broadcaster.sendBroadcast(intent);
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_NODE)) {
                // Reference: https://github.com/andanteyk/ElectronicObserver/blob/1052a7b177a62a5838b23387ff35283618f688dd/ElectronicObserver/Other/Information/apilist.txt
                if (jsonDataObj.has("api_maparea_id")) {
                    int currentMapArea = jsonDataObj.get("api_maparea_id").getAsInt();
                    int currentMapNo = jsonDataObj.get("api_mapinfo_no").getAsInt();
                    int currentNode = jsonDataObj.get("api_no").getAsInt();
                    String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);
                    int api_event_kind = jsonDataObj.get("api_event_kind").getAsInt();
                    int api_event_id = jsonDataObj.get("api_event_id").getAsInt();
                    int api_color_no = jsonDataObj.get("api_color_no").getAsInt();
                    currentNodeInfo = KcaApiData.getNodeFullInfo(contextWithLocale, currentNodeAlphabet, api_event_id, api_event_kind, false);

                    battleToast.showToast(currentNodeInfo, Toast.LENGTH_LONG,
                            getNodeColor(getApplicationContext(), api_event_id, api_event_kind, api_color_no));
                    //Toast.makeText(getApplicationContext(), currentNodeInfo, Toast.LENGTH_LONG).show();
                }
                Intent intent = new Intent(KCA_MSG_BATTLE_NODE);
                intent.putExtra(KCA_MSG_DATA, data);
                broadcaster.sendBroadcast(intent);
                setFrontViewNotifier(FRONT_NONE, 0, null);
            }

            if (url.startsWith(KCA_API_NOTI_BATTLE_DROPINFO)) {
                Log.e("KCA", KCA_API_NOTI_BATTLE_DROPINFO + " " + String.valueOf(isOpendbEnabled()));
                int world = jsonDataObj.get("world").getAsInt();
                int map = jsonDataObj.get("map").getAsInt();
                int node = jsonDataObj.get("node").getAsInt();
                String rank = jsonDataObj.get("rank").getAsString();
                int maprank = jsonDataObj.get("maprank").getAsInt();
                int result = jsonDataObj.get("result").getAsInt();
                if (isOpendbEnabled()) {
                    KcaOpendbAPI.sendShipDropData(world, map, node, rank, maprank, result);
                }
            }

            if (url.startsWith(KCA_API_NOTI_HEAVY_DMG)) {
                heavyDamagedMode = jsonDataObj.get("data").getAsInt();
                if (heavyDamagedMode != HD_NONE) {
                    if (isHDVibrateEnabled()) {
                        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
                        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                Uri notificationUri = Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
                                Log.e("KCA", getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
                                KcaUtils.playNotificationSound(mediaPlayer, getApplicationContext(), notificationUri);
                            }
                        }
                        vibrator.vibrate(1500);
                    }
                }

                if (heavyDamagedMode == HD_DANGER) {
                    Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.heavy_damaged), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(KCA_MSG_BATTLE_HDMG);
                    intent.putExtra(KCA_MSG_DATA, "1");
                    broadcaster.sendBroadcast(intent);
                } else {
                    if (heavyDamagedMode == HD_DAMECON) {
                        Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.heavy_damaged_damecon), Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(KCA_MSG_BATTLE_HDMG);
                    intent.putExtra(KCA_MSG_DATA, "0");
                    broadcaster.sendBroadcast(intent);
                }
                setFrontViewNotifier(FRONT_NONE, 0, null);
            }

            if (url.startsWith(KCA_API_NOTI_GOBACKPORT)) {
                if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT && isHDVibrateEnabled()) {
                    vibrator.vibrate(300);
                }
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.goback_left), Toast.LENGTH_LONG).show();
                setFrontViewNotifier(FRONT_NONE, 0, null);
            }

            if (url.startsWith(KCA_API_PREF_CN_CHANGED)) {
                processFirstDeckInfo(currentPortDeckData);
            }

            if (url.startsWith(KCA_API_PREF_EXPVIEW_CHANGED)) {
                setFrontViewNotifier(FRONT_NONE, 0, null);
            }

            if (url.startsWith(KCA_API_OPENDB_FAILED)) {
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.opendb_failed_msg), Toast.LENGTH_SHORT).show();
            }

            if (url.startsWith(KCA_API_PROCESS_BATTLE_FAILED)) {
                String app_version = BuildConfig.VERSION_NAME;
                String api_data = jsonDataObj.get("api_data").getAsString();
                String api_url = jsonDataObj.get("api_url").getAsString();
                String api_node = jsonDataObj.get("api_node").getAsString();
                String api_error = jsonDataObj.get("api_error").getAsString();
                String token = "df1629d6820907e7a09ea1e98d3041c2";

                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.process_battle_failed_msg), Toast.LENGTH_SHORT).show();
                String dataSendUrl = String.format(getStringWithLocale(R.string.errorlog_battle_link), token, api_url, api_node, app_version);
                AjaxCallback<String> cb = new AjaxCallback<String>() {
                    @Override
                    public void callback(String url, String data, AjaxStatus status) {
                        // do nothing
                    }
                };

                JsonObject sendData = new JsonObject();
                sendData.addProperty("data", api_data);
                sendData.addProperty("error", api_error);
                String sendDataString = sendData.toString();

                AQuery aq = new AQuery(KcaService.this);
                cb.header("Referer", "app:/KCA/");
                cb.header("Content-Type", "application/x-www-form-urlencoded");
                HttpEntity entity = new ByteArrayEntity(sendDataString.getBytes());
                cb.param(AQuery.POST_ENTITY, entity);
                aq.ajax(dataSendUrl, String.class, cb);
            }

        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();

            String app_version = BuildConfig.VERSION_NAME;
            String token = "8988e900c3ea9bb1c9df330c4833c144";
            String kca_url = "";
            try {
                kca_url = URLEncoder.encode(url, "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.service_failed_msg), Toast.LENGTH_SHORT).show();
            String dataSendUrl = String.format(getStringWithLocale(R.string.errorlog_notify_link), token, kca_url, app_version);
            AjaxCallback<String> cb = new AjaxCallback<String>() {
                @Override
                public void callback(String url, String data, AjaxStatus status) {
                    // do nothing
                }
            };

            JsonObject sendData = new JsonObject();
            sendData.addProperty("data", data);
            sendData.addProperty("error", getStringFromException(e));
            String sendDataString = sendData.toString();

            AQuery aq = new AQuery(KcaService.this);
            cb.header("Referer", "app:/KCA/");
            cb.header("Content-Type", "application/x-www-form-urlencoded");
            HttpEntity entity = new ByteArrayEntity(sendDataString.getBytes());
            cb.param(AQuery.POST_ENTITY, entity);
            aq.ajax(dataSendUrl, String.class, cb);
        }
    }


    private int getSeekCn() {
        return Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    private boolean isOpendbEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_OPENDB_API_USE);
    }

    private boolean isExpViewEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_EXP_VIEW);
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

    private void processFirstDeckInfo(JsonArray data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String delimeter = " | ";
        if (!isGameDataLoaded()) {
            Log.e("KCA", "processFirstDeckInfo: Game Data is Null");
            new retrieveApiStartData().execute("", "down", "");
            return;
        } else if (!isUserItemDataLoaded() && isPortAccessed) {
            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.kca_toast_restart_at_kcanotify), Toast.LENGTH_LONG).show();
            Log.e("KCA", String.format("processFirstDeckInfo: useritem data is null"));
            return;
        } else {
            Log.e("KCA", String.format("processFirstDeckInfo: data loaded"));
        }

        if (data == null) {
            Log.e("KCA", "data is null");
            kcaFirstDeckInfo = "data is null";
            return;
        } else {
            kcaFirstDeckInfo = "";
        }

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
        String tpValue = String.format(getStringWithLocale(R.string.kca_view_tpvalue), tp[0], tp[1]);
        infoData = new JsonObject();
        infoData.addProperty("is_newline", 1);
        infoData.addProperty("portrait_value", tpValue);
        infoData.addProperty("landscape_value", tpValue);
        deckInfoData.add(infoData);

        if (isCombined) {
            String firstConditionValue = String.format(getStringWithLocale(R.string.kca_view_condition_1), KcaDeckInfo.getConditionStatus(data, 0));
            infoData = new JsonObject();
            infoData.addProperty("is_newline", 1);
            infoData.addProperty("portrait_value", firstConditionValue);
            infoData.addProperty("landscape_value", firstConditionValue);
            deckInfoData.add(infoData);

            String secondConditionValue = String.format(getStringWithLocale(R.string.kca_view_condition_2), KcaDeckInfo.getConditionStatus(data, 1));
            infoData = new JsonObject();
            infoData.addProperty("is_newline", 0);
            infoData.addProperty("portrait_value", secondConditionValue);
            infoData.addProperty("landscape_value", secondConditionValue);
            deckInfoData.add(infoData);
        } else {
            String firstConditionValue = String.format(getStringWithLocale(R.string.kca_view_condition), KcaDeckInfo.getConditionStatus(data, 0));
            infoData = new JsonObject();
            infoData.addProperty("is_newline", 0);
            infoData.addProperty("portrait_value", firstConditionValue);
            infoData.addProperty("landscape_value", firstConditionValue);
            deckInfoData.add(infoData);
        }

        kcaFirstDeckInfo = gson.toJson(deckInfoData);
        setFrontViewNotifier(FRONT_NONE, 0, null);
    }

    private void processExpeditionInfo(JsonArray data) {
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
            //Log.e("KCA", String.format("%d: %d / %d",i, mission_no, arrive_time));
            //Log.e("KCA", String.valueOf(i) + " " + String.valueOf(KcaExpedition.complete_time_check[idx]));
            if (kcaExpeditionRunnableList[idx] != null) {
                if (mission_no == -1) {
                    kcaExpeditionRunnableList[idx].kill();
                    kcaExpeditionList[idx].interrupt();
                    kcaExpeditionList[idx] = null;
                    kcaExpeditionRunnableList[idx] = null;
                } else if (arrive_time != kcaExpeditionRunnableList[idx].getArriveTime()
                        && !kcaExpeditionRunnableList[idx].getCanceledStatus()) {
                    notifiManager.cancel(getNotificationId(NOTI_EXP, idx));
                    kcaExpeditionRunnableList[idx].kill();
                    kcaExpeditionList[idx].interrupt();
                    kcaExpeditionRunnableList[idx] = new KcaExpedition(mission_no, idx, deck_name, arrive_time, nHandler);
                    kcaExpeditionList[idx] = new Thread(kcaExpeditionRunnableList[idx]);
                    kcaExpeditionList[idx].start();
                }
            } else {
                if (mission_no != -1 && (KcaExpedition.complete_time_check[idx] != arrive_time ||
                        kcaExpeditionRunnableList[idx] == null)) {
                    //Log.e("KCA", "Fleet " + String.valueOf(idx) + " went exp");
                    kcaExpeditionRunnableList[idx] = new KcaExpedition(mission_no, idx, deck_name, arrive_time, nHandler);
                    kcaExpeditionList[idx] = new Thread(kcaExpeditionRunnableList[idx]);
                    kcaExpeditionList[idx].start();

                }
            }
        }
        setFrontViewNotifier(FRONT_NONE, 0, null);
    }

    private void cancelExpeditionInfo(JsonObject data) {
        JsonArray canceled_info = (JsonArray) data.get("api_mission");
        int canceled_mission_no = canceled_info.get(1).getAsInt();
        long arrive_time = canceled_info.get(2).getAsLong();
        int idx = -1;
        for (int i = 0; i < 3; i++) {
            if (kcaExpeditionRunnableList[i] != null) {
                if (kcaExpeditionRunnableList[i].mission_no == canceled_mission_no) {
                    idx = i;
                    break;
                }
            }
        }
        if (idx == -1) return;
        Log.e("KCA", "Arrive time: " + String.valueOf(arrive_time));
        kcaExpeditionRunnableList[idx].canceled(arrive_time);
    }

    private void processDockingInfo(JsonArray data) {
        int dockId, shipId, state;
        long completeTime;

        for (int i = 0; i < data.size(); i++) {
            JsonObject ndockData = data.get(i).getAsJsonObject();
            state = ndockData.get("api_state").getAsInt();
            notifiManager.cancel(getNotificationId(NOTI_DOCK, i));
            if (state != -1) {
                dockId = ndockData.get("api_id").getAsInt() - 1;
                shipId = ndockData.get("api_ship_id").getAsInt();
                completeTime = ndockData.get("api_complete_time").getAsLong();

                if (kcaDockingList[dockId] != null) { // Overwrite Data?
                    if (kcaDockingRunnableList[dockId].ship_id != shipId) {
                        kcaDockingList[dockId].interrupt();
                        kcaDockingRunnableList[dockId] = null;
                        if (shipId != 0) {
                            kcaDockingRunnableList[dockId] = new KcaDocking(dockId, shipId, completeTime, nHandler);
                            kcaDockingList[dockId] = new Thread(kcaDockingRunnableList[dockId]);
                            kcaDockingList[dockId].start();
                        }
                    } else {
                        kcaDockingRunnableList[dockId].setCompleteTime(dockId, completeTime);
                    }
                } else {
                    if (state == 1) {
                        kcaDockingRunnableList[dockId] = new KcaDocking(dockId, shipId, completeTime, nHandler);
                        kcaDockingList[dockId] = new Thread(kcaDockingRunnableList[dockId]);
                        kcaDockingList[dockId].start();
                    }
                }
            }
        }
    }

    private void processDockingSpeedup(int dockId) {
        kcaDockingRunnableList[dockId].sendDockingFinished(true);
        kcaDockingRunnableList[dockId] = null;
        kcaDockingList[dockId] = null;
    }

    public void setFrontViewNotifier(int type, int id, String content) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean is_portrait = !is_landscape;
        boolean no_exp_flag = true;

        String notifiTitle = "";
        String notifiString = "";
        String expeditionString = "";

        if (!prefs.getBoolean(PREF_VPN_ENABLED, false)) {
            notifiTitle = String.format(getStringWithLocale(R.string.kca_view_normal_format), getStringWithLocale(R.string.app_name), "");
            notifiString = getStringWithLocale(R.string.kca_view_activate_vpn);
        } else {
            String nodeString = "";
            if (currentNode.length() > 0) {
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

            String expString = "";
            if (content != null) {
                notifiString = content;
            } else {
                try {
                    JsonArray deckInfo = new JsonParser().parse(kcaFirstDeckInfo).getAsJsonArray();
                    List<String> deckInfoStringList = new ArrayList<String>();
                    for (Object item : deckInfo) {
                        JsonObject data = (JsonObject) item;
                        if (is_portrait) {
                            deckInfoStringList.add(data.get("portrait_value").getAsString());
                        } else {
                            deckInfoStringList.add(data.get("landscape_value").getAsString());
                        }
                        if (data.get("is_newline").getAsInt() == 1 || (data.has("is_portrait_newline") && is_portrait)) {
                            notifiString = notifiString.concat(joinStr(deckInfoStringList, " / ")).concat("\n");
                            deckInfoStringList.clear();
                        }
                    }
                    notifiString = notifiString.concat(joinStr(deckInfoStringList, " / "));
                } catch (Exception e) {
                    notifiString = kcaFirstDeckInfo;
                }
            }
        }


        List<String> kcaExpStrList = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            if (KcaExpedition.left_time_str[i] != null) {
                no_exp_flag = false;
                kcaExpStrList.add(KcaExpedition.left_time_str[i]);
            }
        }

        if (isExpViewEnabled()) {
            if (no_exp_flag) {
                expeditionString = expeditionString.concat(getStringWithLocale(R.string.kca_view_noexpedition));
            } else {
                if (is_landscape) {
                    expeditionString = expeditionString.concat(getStringWithLocale(R.string.kca_view_expedition_header)).concat(" ");
                    //expeditionString = expeditionString.concat("\n");
                }
                expeditionString = expeditionString.concat(joinStr(kcaExpStrList, " / "));
            }
        }
        notifiManager.notify(getNotificationId(NOTI_FRONT, 1), createViewNotification(notifiTitle, notifiString, expeditionString));
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


    private int getNotificationId(int type, int n) {
        return n + 1000 * type;
    }

    private class retrieveApiStartData extends AsyncTask<String, Void, String> {
        public final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");
        OkHttpClient client = new OkHttpClient.Builder().build();

        @Override
        protected String doInBackground(String... params) {
            String content = null;
            content = executeClient(params[0], params[1], params[2]);

            return content;
        }

        public String executeClient(String token, String method, String data) {
            if (KcaApiData.isGameDataLoaded()) return null;
            kcaFirstDeckInfo = getStringWithLocale(R.string.kca_toast_loading_data);
            String dataUrl;
            if (kca_version == null) {
                dataUrl = String.format(getStringWithLocale(R.string.api_start2_recent_version_link));
            } else {
                dataUrl = String.format(getStringWithLocale(R.string.api_start2_version_link), kca_version);
            }

            AjaxCallback<String> cb = new AjaxCallback<String>() {
                @Override
                public void callback(String url, String data, AjaxStatus status) {
                    try {
                        String remote_kca_version = status.getHeader("X-Api-Version");
                        if (kca_version != null && !kca_version.equals(remote_kca_version)) {
                            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.kca_toast_inconsistent_data), Toast.LENGTH_LONG).show();
                        } else {
                            if (kca_version == null) kca_version = remote_kca_version;
                            writeCacheData(getApplicationContext(), data.getBytes(), KCANOTIFY_S2_CACHE_FILENAME);
                            KcaApiData.getKcGameData(gson.fromJson(data, JsonObject.class).getAsJsonObject("api_data"));
                            setPreferences(getApplicationContext(), "kca_version", kca_version);
                        }
                        Log.e("KCA", "Data Load Finished");
                    } catch (IOException e1) {
                        Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.kca_toast_ioexceptionerror), Toast.LENGTH_LONG).show();
                        Log.e("KCA", "I/O Error");
                    }
                    api_start2_loading_flag = false;
                }
            };
            AQuery aq = new AQuery(KcaService.this);
            cb.header("Referer", "app:/KCA/");
            cb.header("Content-Type", "application/x-www-form-urlencoded");
            Log.e("KCA", dataUrl);
            aq.ajax(dataUrl, String.class, cb);

            return null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage());
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage());
        }
        // Force Locale Setting
        Locale locale = new Locale(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newConfig.setLocale(locale);
        } else {
            newConfig.locale = locale;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage());
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage());
        }
        super.onConfigurationChanged(newConfig);
        //setFrontViewNotifier(FRONT_NONE, 0, null);
    }
}