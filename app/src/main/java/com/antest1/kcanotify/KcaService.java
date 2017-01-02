package com.antest1.kcanotify;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.Notification.BigTextStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.AssetManager.AssetInputStream;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.isUserItemDataLoaded;
import static com.antest1.kcanotify.KcaApiData.updateUserShip;
import static com.antest1.kcanotify.KcaConstants.*;

import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;


public class KcaService extends Service {
    public static final int ANTEST_USERID = 15108389;

    public static boolean isServiceOn = false;
    public static boolean isPortAccessed = false;
    public static int heavyDamagedMode = 0;
    public static int checkKdockId = -1;
    public static boolean kaisouProcessFlag = false;
    public static String currentNode = "";
    public static Intent kcIntent = null;

    AudioManager mAudioManager;

    NotificationManager notifiManager;
    Builder viewNotificationBuilder;
    BigTextStyle viewNotificationText;
    public static boolean noti_vibr_on = true;

    kcaServiceHandler handler;
    kcaNotificationHandler nHandler;

    Thread[] kcaExpeditionList = new Thread[3];
    KcaExpedition[] kcaExpeditionRunnableList = new KcaExpedition[3];
    String[] kcaExpeditionInfoList = new String[3];

    Thread[] kcaDockingList = new Thread[4];
    KcaDocking[] kcaDockingRunnableList = new KcaDocking[4];

    String kcaFirstDeckInfo = "깡들리티에서 게임을 실행해주세요";
    String kca_version;
    String api_start2_data = null;
    boolean api_start2_down_mode = false;
    JsonArray currentPortDeckData = null;
    Gson gson = new Gson();

    public static boolean getServiceStatus() {
        return isServiceOn;
    }

    private boolean checkKeyInPreferences(String key) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        return pref.contains(key);
    }

    private String getStringPreferences(String key) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        return pref.getString(key, "");
    }

    private Boolean getBooleanPreferences(String key) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        return pref.getBoolean(key, false);
    }

    // 값 저장하기
    private void setPreferences(String key, Object value) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if(value instanceof String) {
            editor.putString(key, (String)value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean)value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer)value);
        } else {
            editor.putString(key, value.toString());
        }
        editor.commit();
    }

    private JsonObject readCacheData(String filename) {
        try {
            FileInputStream fis = openFileInput(filename);
            byte[] cache_data = new byte[fis.available()];
            //Toast.makeText(getApplicationContext(), String.format("Loading Cached Data %d", fis.available()), Toast.LENGTH_LONG).show();
            while (fis.read(cache_data) != -1) {
                ;
            }
            String cache_data_str = new String(cache_data, 0, cache_data.length);
            return new JsonParser().parse(cache_data_str).getAsJsonObject();
        } catch (FileNotFoundException e) {
            //new retrieveApiStartData().execute("", "down", "");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeCacheData(byte[] data, String filename) throws IOException {
        FileOutputStream fos = openFileOutput(KCANOTIFY_S2_CACHE_FILENAME, MODE_PRIVATE);
        fos.write(data);
        fos.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!MainActivity.isKcaServiceOn) {
            stopSelf();
        }

        for (int i = 0; i < 3; i++) {
            kcaExpeditionList[i] = null;
            kcaExpeditionInfoList[i] = "";
        }

        KcaExpedition.expeditionData = getExpeditionData();
        KcaApiData.kcShipTranslationData = getShipTranslationData();

        AssetManager assetManager = getResources().getAssets();
        try {
            AssetInputStream ais = (AssetInputStream) assetManager.open("edges.json");
            byte[] bytes = ByteStreams.toByteArray(ais);
            JsonElement edgesData = new JsonParser().parse(new String(bytes));
            if (edgesData.isJsonObject()) {
                KcaApiData.loadMapEdgeInfo(edgesData.getAsJsonObject());
            } else {
                Toast.makeText(getApplicationContext(), "Invalid Map Edge Data", Toast.LENGTH_LONG).show();
            }
        } catch(IOException e) {
            Toast.makeText(getApplicationContext(), "Map Edge Data Not Found", Toast.LENGTH_LONG).show();
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notifiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        handler = new kcaServiceHandler();
        nHandler = new kcaNotificationHandler();
        kcaExpeditionRunnableList = new KcaExpedition[3];
        kcaDockingRunnableList = new KcaDocking[4];
        isPortAccessed = false;

        KcaProxyServer.start(handler);

        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String initTitle = String.format("%s 동작중", getResources().getText(R.string.app_name));
        String initContent = "깡들리티에서 게임을 실행해주세요";
        String initSubContent = String.format("%s %s", getResources().getText(R.string.app_name), getResources().getString(R.string.app_version));

        startForeground(getNotificationId(NOTI_FRONT, 1), createViewNotification(initTitle, initContent, initSubContent));
        isServiceOn = true;

        KcaBattle.setHandler(nHandler);
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

        stopForeground(true);
        notifiManager.cancelAll();
        viewNotificationBuilder = null;
        isServiceOn = false;
    }

    public void onDestroy() {
        setServiceDown();
        KcaProxyServer.stop();
    }

    private Notification createViewNotification(String title, String content1, String content2) {
        if (viewNotificationBuilder == null) {
            Intent aIntent = new Intent(KcaService.this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, aIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            viewNotificationText = new Notification.BigTextStyle();
            viewNotificationBuilder = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.mipmap.noti_icon2)
                    .setContentTitle(title)
                    .setStyle(viewNotificationText.bigText(content1))
                    .setStyle(viewNotificationText.setSummaryText(content2))
                    .setTicker(title)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setOngoing(true).setAutoCancel(false);

        } else {
            if(title != null) {
                viewNotificationBuilder.setContentTitle(title);
                viewNotificationBuilder.setTicker(title);
            }
            if (content1 != null) {
                viewNotificationBuilder.setStyle(viewNotificationText.bigText(content1));
            }
            if (content2 != null) {
                viewNotificationBuilder.setStyle(viewNotificationText.setSummaryText(content2));
            }
        }
        return viewNotificationBuilder.build();
    }

    private Notification createExpeditionNotification(int missionNo, String missionName, String kantaiName, boolean cancelFlag) {
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String title = "";
        String content = String.format("<%s> 가\n%d번 원정에서 복귀했습니다.", kantaiName, missionNo);
        if (cancelFlag) {
            title = String.format("%d번 원정(%s) 취소", missionNo, missionName);
        } else {
            title = String.format("%d번 원정(%s) 도착", missionNo, missionName);
        }

        Notification Notifi = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.expedition_notify_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.expedition_notify_bigicon))
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setContentIntent(pendingIntent).build();
        if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            Notifi.defaults = Notification.DEFAULT_VIBRATE;
        } else {
            Notifi.vibrate = new long[]{-1};
        }
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;
        setFrontViewNotifier(FRONT_NONE, 0, null);
        return Notifi;
    }

    private Notification createDockingNotification(int dockId, String shipName) {
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String title = String.format("제%d도크 정비 완료", dockId+1);
        String content = "";
        if (shipName.length() > 0) {
            content = String.format("제%d도크: %s의 정비가 완료되었습니다.", dockId+1, shipName);
        } else {
            content = String.format("제%d도크 내 칸무스의 정비가 완료되었습니다.", dockId+1);
        }

        Notification Notifi = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.docking_notify_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.dockng_notify_bigicon))
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setContentIntent(pendingIntent).build();
        if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            Notifi.defaults = Notification.DEFAULT_VIBRATE;
        } else {
            Notifi.vibrate = new long[]{-1};
        }
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;
        setFrontViewNotifier(FRONT_NONE, 0, null);
        return Notifi;
    }

    private void toastInfo() {
        if (!KcaApiData.isGameDataLoaded()) return;
        int cn = getSeekCn();
        String seekType = getSeekType();
        int[] airPowerRange = KcaDeckInfo.getAirPowerRange(currentPortDeckData, 0);
        String airPowerValue = String.format("제공: %d-%d", airPowerRange[0], airPowerRange[1]);
        String seekValue =  String.format("색적(%s): %.2f", seekType, KcaDeckInfo.getSeekValue(currentPortDeckData, 0, cn));
        List<String> toastList = new ArrayList<String>();
        if (airPowerRange[1] >         0) {
            toastList.add(airPowerValue);
        }
        toastList.add(seekValue);
        Toast.makeText(getApplicationContext(), joinStr(toastList, " / "), Toast.LENGTH_LONG).show();
    }

    class kcaServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String url = msg.getData().getString("url");
            String data = msg.getData().getString("data");
            String request = msg.getData().getString("request");

            if (!KcaProxyServer.is_on() || url.length() == 0 || viewNotificationBuilder == null) {
                return;
            }
            JsonObject jsonDataObj;
            try {
                jsonDataObj = new JsonParser().parse(data).getAsJsonObject();

                if (url.startsWith(KCA_VERSION)) {
                    //Toast.makeText(getApplicationContext(), "KCA_VERSION", Toast.LENGTH_LONG).show();
                    JsonObject api_version = jsonDataObj.get("api").getAsJsonObject();
                    kca_version = api_version.get("api_start2").getAsString();
                    Log.e("KCA", kca_version);
                    if (!getStringPreferences("kca_version").equals(kca_version)) {
                        api_start2_down_mode = true;
                    } else {
                        api_start2_down_mode = false;
                        JsonObject kcDataObj = readCacheData(KCANOTIFY_S2_CACHE_FILENAME);
                        //Log.e("KCA", kcDataObj.toJSONString());
                        if (kcDataObj != null && kcDataObj.has("api_data")) {
                            //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                            KcaApiData.getKcGameData(kcDataObj.getAsJsonObject("api_data"));
                            setPreferences("kca_version", kca_version);
                        }
                    }
                    return;
                    //Toast.makeText(getApplicationContext(), getPreferences("kca_version") + " " + String.valueOf(api_start2_down_mode), Toast.LENGTH_LONG).show();
                }

                if (url.startsWith(API_WORLD_GET_ID)) {
                    return;
                }

                if (url.startsWith(API_REQ_MEMBER_GET_INCENTIVE)) {
                    return;
                }

                if (url.startsWith(API_START2)) {
                    //Log.e("KCA", "Load Kancolle Data");
                    //Toast.makeText(getApplicationContext(), "API_START2", Toast.LENGTH_LONG).show();

                    api_start2_data = data;
                    writeCacheData(data.getBytes(), KCANOTIFY_S2_CACHE_FILENAME);

                    if (jsonDataObj.has("api_data")) {
                        //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                        KcaApiData.getKcGameData(jsonDataObj.getAsJsonObject("api_data"));
                        setPreferences("kca_version", kca_version);
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
                        //Toast.makeText(getApplicationContext(), String.valueOf(userId), Toast.LENGTH_LONG).show();

                        if (userId == ANTEST_USERID && api_start2_down_mode && api_start2_data != null) {
                            Toast.makeText(getApplicationContext(), "Uploading Data...", Toast.LENGTH_LONG).show();
                            new retrieveApiStartData().execute("3a4104a5ef67f0823f78a636fbd2bbbf", "up", api_start2_data);
                        } else if (api_start2_data == null && api_start2_down_mode) {
                            Toast.makeText(getApplicationContext(), String.format("Downloading Data"), Toast.LENGTH_LONG).show();
                            new retrieveApiStartData().execute("", "down", "");
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

                if (url.startsWith(API_REQ_MISSION_RETURN)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject reqGetMemberDeckApiData = jsonDataObj.getAsJsonObject("api_data");
                        cancelExpeditionInfo(reqGetMemberDeckApiData);
                    }
                    return;
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
                            ndock_id = Integer.valueOf(decodedData.replace("api_ndock_id=", "")) -1;
                            break;
                        }
                    }
                    if(ndock_id != -1) processDockingSpeedup(ndock_id);
                    return;
                }

                // Game Data Dependent Tasks
                if(!isGameDataLoaded() || !isUserItemDataLoaded()) {
                    Toast.makeText(getApplicationContext(), "깡들리티에서 게임을 다시 시작해주세요", Toast.LENGTH_LONG).show();
                } else {
                    if (url.startsWith(API_PORT)) {
                        isPortAccessed = true;
                        heavyDamagedMode = HD_NONE;
                        currentNode = "";
                        KcaApiData.resetShipCountInBattle();
                        Log.e("KCA", "Port Handler Called");
                        if (jsonDataObj.has("api_data")) {
                            JsonObject reqPortApiData = jsonDataObj.getAsJsonObject("api_data");
                            int size = KcaApiData.getPortData(reqPortApiData);
                            if (reqPortApiData.has("api_basic")) {
                                processBasicInfo(reqPortApiData.getAsJsonObject("api_basic"));
                            }
                            //Log.e("KCA", "Total Ships: " + String.valueOf(size));
                            if (reqPortApiData.has("api_deck_port")) {
                                JsonArray reqPortDeckApiData = reqPortApiData.getAsJsonArray("api_deck_port");
                                currentPortDeckData = reqPortDeckApiData;
                                processFirstDeckInfo(currentPortDeckData);
                                processExpeditionInfo(currentPortDeckData);
                            }
                            if (reqPortApiData.has("api_ndock")) {
                                JsonArray nDockData = reqPortApiData.getAsJsonArray("api_ndock");
                                processDockingInfo(nDockData);
                            }
                        }
                        setFrontViewNotifier(FRONT_NONE, 0, null);
                    }

                    if (url.startsWith(API_GET_MEMBER_MAPINFO)) {
                        heavyDamagedMode = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, 0);
                        switch (heavyDamagedMode) {
                            case HD_DAMECON:
                            case HD_DANGER:
                                if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    v.vibrate(1500);
                                }
                                if (heavyDamagedMode == HD_DANGER) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged), Toast.LENGTH_LONG).show();
                                } else if (heavyDamagedMode == HD_DAMECON) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged_damecon), Toast.LENGTH_LONG).show();
                                }
                                break;
                            default:
                                break;
                        }
                        setFrontViewNotifier(FRONT_NONE, 0, null);

                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            JsonArray api_map_info = api_data.getAsJsonArray("api_map_info");
                            int eventMapCount = 0;
                            for (JsonElement map : api_map_info) {
                                JsonObject mapData = map.getAsJsonObject();
                                if (mapData.has("api_eventmap")) {
                                    eventMapCount += 1;
                                    KcaApiData.setEventMapDifficulty(eventMapCount, mapData.get("api_selected_rank").getAsInt());
                                }
                            }
                        }
                    }
                    // TODO: add handler for selecting event map rank

                    if (API_BATTLE_REQS.contains(url)) {
                        //Log.e("KCA", "Battle Handler Called");
                        if (jsonDataObj.has("api_data")) {
                            JsonObject battleApiData = jsonDataObj.getAsJsonObject("api_data");
                            KcaBattle.processData(url, battleApiData);
                        }
                    }

                    if (url.startsWith(API_GET_MEMBER_SHIP_DECK)) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.getAsJsonObject("api_data");
                            JsonArray api_deck_data = (JsonArray) api_data.get("api_deck_data");
                            KcaApiData.updatePortDataOnBattle(api_data);
                            for(int i=0; i<api_deck_data.size(); i++) {
                                if (i==0) {
                                    KcaBattle.dameconflag = KcaDeckInfo.getDameconStatus(api_deck_data, 0);
                                } else if(i==1) {
                                    KcaBattle.dameconcbflag = KcaDeckInfo.getDameconStatus(api_deck_data, 1);
                                }
                            }
                            processFirstDeckInfo(api_deck_data);
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
                        if(kaisouProcessFlag) {
                            processFirstDeckInfo(currentPortDeckData);
                            toastInfo();
                            kaisouProcessFlag = false;
                        }
                    }

                    if (url.startsWith(API_REQ_KAISOU_SLOTSET)) {
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
                            JsonArray use_slot_id = api_data.getAsJsonArray("api_use_slot_id");
                            List<String> use_slot_id_list = new ArrayList<String>();
                            for (JsonElement id : use_slot_id) {
                                use_slot_id_list.add(id.getAsString());
                            }
                            KcaApiData.deleteUserItem(joinStr(use_slot_id_list, ","));
                        }
                        if (certainFlag != 1 && isOpendbEnabled()) {
                            KcaOpendbAPI.sendRemodelData(flagship, assistant, itemKcId, level, api_remodel_flag);
                        }
                    }
                }
            } catch (JsonSyntaxException e) {
                //Log.e("KCA", "ParseError");
                //Log.e("KCA", data);
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "오류가 발생하였습니다.", Toast.LENGTH_LONG).show();
                setServiceDown();
                throw e;
            }
        }
    }

    class kcaNotificationHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            String url = msg.getData().getString("url");
            String data = msg.getData().getString("data");

            if (!KcaProxyServer.is_on() || !isPortAccessed || viewNotificationBuilder == null || url.length() == 0) {
                return;
            }

            JsonObject jsonDataObj;
            try {
                jsonDataObj = new JsonParser().parse(data).getAsJsonObject();
                if (url.startsWith(KCA_API_NOTI_EXP_LEFT)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    //int kantaiIndex = ((Long) jsonDataObj.get("idx")).intValue();
                    //String str_data = (String) jsonDataObj.get("str");
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }

                if (url.startsWith(KCA_API_NOTI_EXP_CANCELED)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    int kantaiIndex = jsonDataObj.get("kantai_idx").getAsInt();
                    String kantaiName = jsonDataObj.get("kantai_name").getAsString();
                    int missionNo = jsonDataObj.get("mission_no").getAsInt();
                    String missionName = jsonDataObj.get("mission_krname").getAsString();
                    //Intent intent = new Intent(KcaService.this, MainActivity.class);

                    kcaExpeditionInfoList[kantaiIndex] = "";
                    if(isExpNotifyEnabled()) {
                        notifiManager.notify(getNotificationId(NOTI_EXP, missionNo), createExpeditionNotification(missionNo, missionName, kantaiName, true));
                    }
                }

                if (url.startsWith(KCA_API_NOTI_EXP_FIN)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    int kantaiIndex = jsonDataObj.get("kantai_idx").getAsInt();
                    String kantaiName = jsonDataObj.get("kantai_name").getAsString();
                    int missionNo = jsonDataObj.get("mission_no").getAsInt();
                    String missionName = jsonDataObj.get("mission_krname").getAsString();
                    //Intent intent = new Intent(KcaService.this, MainActivity.class);

                    kcaExpeditionInfoList[kantaiIndex] = "";
                    if(isExpNotifyEnabled()) {
                        notifiManager.notify(getNotificationId(NOTI_EXP, missionNo), createExpeditionNotification(missionNo, missionName, kantaiName, false));
                    }
                }

                if (url.startsWith(KCA_API_NOTI_DOCK_FIN)) {
                    int dockId = jsonDataObj.get("dock_no").getAsInt();
                    String shipName = getShipTranslation(jsonDataObj.get("ship_name").getAsString());
                    if(isDockNotifyEnabled()) {
                        notifiManager.notify(getNotificationId(NOTI_DOCK, dockId), createDockingNotification(dockId, shipName));
                    }
                    kcaDockingRunnableList[dockId] = null;
                    kcaDockingList[dockId] = null;
                }

                if (url.startsWith(KCA_API_NOTI_BATTLE_INFO)) {
                    String message = jsonDataObj.get("msg").getAsString();
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }

                if (url.startsWith(KCA_API_NOTI_BATTLE_NODE)) {
                     // Reference: https://github.com/andanteyk/ElectronicObserver/blob/1052a7b177a62a5838b23387ff35283618f688dd/ElectronicObserver/Other/Information/apilist.txt
                    currentNode = jsonDataObj.get("data").getAsString();
                    int id = jsonDataObj.get("id").getAsInt();
                    int kind = jsonDataObj.get("kind").getAsInt();
                    String message = "";
                    switch(id) {
                        case API_NODE_EVENT_ID_OBTAIN:
                            message = String.format(getResources().getString(R.string.node_info_obtain), currentNode);
                            break;
                        case API_NODE_EVENT_ID_LOSS:
                            message = String.format(getResources().getString(R.string.node_info_loss), currentNode);
                            break;
                        case API_NODE_EVENT_ID_NORMAL:
                            if (kind == API_NODE_EVENT_KIND_BATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_normal), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_NIGHTBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_nightbattle), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_NIGHTDAYBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_nightdaybattle), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_AIRBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_airbattle), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_ECBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_ecbattle), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_LDAIRBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_ldairbattle), currentNode);
                            }
                            break;
                        case API_NODE_EVENT_ID_BOSS:
                            if (kind == API_NODE_EVENT_KIND_BATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_boss), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_NIGHTBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_boss_nightbattle), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_NIGHTDAYBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_boss_nightdaybattle), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_ECBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_boss_ecbattle), currentNode);
                            }
                            break;
                        case API_NODE_EVENT_ID_NOEVENT:
                            message = String.format(getResources().getString(R.string.node_info_noevent), currentNode);
                            break;
                        case API_NODE_EVENT_ID_TPOINT:
                            message = String.format(getResources().getString(R.string.node_info_tpoint), currentNode);
                            break;
                        case API_NODE_EVENT_ID_AIR:
                            if (kind == API_NODE_EVENT_KIND_AIRSEARCH) {
                                message = String.format(getResources().getString(R.string.node_info_airsearch), currentNode);
                            } else if (kind == API_NODE_EVENT_KIND_AIRBATTLE) {
                                message = String.format(getResources().getString(R.string.node_info_airbattle), currentNode);
                            }
                            break;
                        case API_NODE_EVENT_ID_SENDAN:
                            message = String.format(getResources().getString(R.string.node_info_sendan), currentNode);
                            break;
                        case API_NODE_EVENT_ID_LDAIRBATTLE:
                            message = String.format(getResources().getString(R.string.node_info_ldairbattle), currentNode);
                            break;
                        default:
                            message = String.format(getResources().getString(R.string.node_info_normal), currentNode);
                            break;
                    }
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
                    if(isOpendbEnabled()) {
                        KcaOpendbAPI.sendShipDropData(world, map, node, rank, maprank, result);
                    }
                }

                if (url.startsWith(KCA_API_NOTI_HEAVY_DMG)) {
                    heavyDamagedMode = jsonDataObj.get("data").getAsInt();
                    if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(1500);
                    }
                    if(heavyDamagedMode == HD_DANGER) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged), Toast.LENGTH_LONG).show();
                    } else if (heavyDamagedMode == HD_DAMECON) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged_damecon), Toast.LENGTH_LONG).show();
                    }
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }

                if (url.startsWith(KCA_API_NOTI_GOBACKPORT)) {
                    if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(300);
                    }
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.goback_left), Toast.LENGTH_LONG).show();
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }

                if (url.startsWith(KCA_API_PREP_CN_CHANGED)) {
                    processFirstDeckInfo(currentPortDeckData);
                }

                if(url.startsWith(KCA_API_OPENDB_FAILED)) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.opendb_failed_msg), Toast.LENGTH_SHORT).show();
                }

            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "오류가 발생하였습니다.", Toast.LENGTH_LONG).show();
                setServiceDown();
                throw e;
            }
        }
    }

    private int getSeekCn() {
        return Integer.valueOf(getStringPreferences("kca_seek_cn"));
    }

    private boolean isOpendbEnabled() {
        return getBooleanPreferences(PREF_OPENDB_API_USE);
    }
    private boolean isExpNotifyEnabled() {
        return getBooleanPreferences(PREF_KCA_NOTI_EXP);
    }
    private boolean isDockNotifyEnabled() {
        return getBooleanPreferences(PREF_KCA_NOTI_DOCK);
    }

    private String getSeekType() {
        int cn = Integer.valueOf(getStringPreferences("kca_seek_cn"));
        String seekType = "";
        switch(cn) {
            case 1:
                seekType = getResources().getString(R.string.seek_type_1);
                break;
            case 3:
                seekType = getResources().getString(R.string.seek_type_3);
                break;
            case 4:
                seekType = getResources().getString(R.string.seek_type_4);
                break;
            default:
                seekType = getResources().getString(R.string.seek_type_0);
                break;
        }
        return seekType;
    }

    private void processBasicInfo(JsonObject data) {
        KcaApiData.maxShipSize = data.get("api_max_chara").getAsInt();
        KcaApiData.maxItemSize = data.get("api_max_slotitem").getAsInt();
    }

    private void processFirstDeckInfo(JsonArray data) {
        String delimeter = " | ";
        if (!isGameDataLoaded()) {
            Log.e("KCA", "processFirstDeckInfo: Game Data is Null");
            new retrieveApiStartData().execute("", "down", "");
            return;
        } else if(!isUserItemDataLoaded()) {
            Toast.makeText(getApplicationContext(), "깡들리티에서 게임을 시작해주세요", Toast.LENGTH_LONG).show();
            Log.e("KCA", String.format("processFirstDeckInfo: useritem data is null"));
            return;
        } else {
            Log.e("KCA", String.format("processFirstDeckInfo: data loaded"));
        }

        if (data == null) {
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
        int[] airPowerRange = KcaDeckInfo.getAirPowerRange(data, 0);
        if (airPowerRange[0] > 0 && airPowerRange[1] > 0) {
            airPowerValue = String.format("제공: %d-%d", airPowerRange[0], airPowerRange[1]);
            infoData = new JsonObject();
            infoData.addProperty("is_portrait_newline", 0);
            infoData.addProperty("portrait_value", airPowerValue);
            infoData.addProperty("landscape_value", airPowerValue);
            deckInfoData.add(infoData);
        }
        String seekValue = "";
        if (cn == SEEK_PURE) {
            seekValue =  String.format("색적(%s): %d", seekType, (int) KcaDeckInfo.getSeekValue(data, 0, cn));
        } else {
            seekValue =  String.format("색적(%s): %.2f", seekType, KcaDeckInfo.getSeekValue(data, 0, cn));

        }
        infoData = new JsonObject();
        infoData.addProperty("is_portrait_newline", 0);
        infoData.addProperty("portrait_value", seekValue);
        infoData.addProperty("landscape_value", seekValue);
        deckInfoData.add(infoData);

        int speedValue = KcaDeckInfo.getSpeed(data, 0);
        String speedStringValue = "";
        switch(speedValue) {
            case KcaApiData.SPEED_FAST:
                speedStringValue = getResources().getString(R.string.speed_fast);
                break;
            case KcaApiData.SPEED_SLOW:
                speedStringValue = getResources().getString(R.string.speed_slow);
                break;
            default:
                speedStringValue = getResources().getString(R.string.speed_mixed);
                break;
        }
        infoData = new JsonObject();
        infoData.addProperty("is_portrait_newline", 1);
        infoData.addProperty("portrait_value", speedStringValue);
        infoData.addProperty("landscape_value", speedStringValue.concat(getResources().getString(R.string.speed_postfix)));
        deckInfoData.add(infoData);

        String conditionValue = String.format("피로도: %s", KcaDeckInfo.getConditionStatus(data, 0));
        infoData = new JsonObject();
        infoData.addProperty("is_portrait_newline", 0);
        infoData.addProperty("portrait_value", conditionValue);
        infoData.addProperty("landscape_value", conditionValue);
        deckInfoData.add(infoData);

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
                if (arrive_time != kcaExpeditionRunnableList[idx].getArriveTime()) {
                    kcaExpeditionList[idx].interrupt();
                    kcaExpeditionRunnableList[idx] = new KcaExpedition(mission_no, idx, deck_name, arrive_time, nHandler);
                    kcaExpeditionList[idx] = new Thread(kcaExpeditionRunnableList[idx]);
                    kcaExpeditionList[idx].start();
                } else if (mission_no == -1) {
                    //Log.e("KCA", "Fleet " + String.valueOf(idx) + " not in exp");
                    kcaExpeditionList[idx].interrupt();
                    kcaExpeditionList[idx] = null;
                    kcaExpeditionRunnableList[idx] = null;
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
        long arrive_time = canceled_info.get(2).getAsInt();
        int idx = -1;
        for (int i = 0; i < 3; i++) {
            if (kcaExpeditionRunnableList[i].mission_no == canceled_mission_no) {
                idx = i;
                break;
            }
        }
        kcaExpeditionRunnableList[idx].canceled(arrive_time);
    }

    private void processDockingInfo(JsonArray data) {
        int dockId, shipId, state;
        long completeTime;

        for (int i=0; i<data.size(); i++) {
            JsonObject ndockData = data.get(i).getAsJsonObject();
            state = ndockData.get("api_state").getAsInt();
            if(state != -1) {
                dockId = ndockData.get("api_id").getAsInt() - 1;
                shipId = ndockData.get("api_ship_id").getAsInt();
                completeTime = ndockData.get("api_complete_time").getAsLong();

                if(kcaDockingList[dockId] != null) { // Overwrite Data?
                    if(kcaDockingRunnableList[dockId].ship_id != shipId) {
                        kcaDockingList[dockId].interrupt();
                        kcaDockingRunnableList[dockId] = null;
                        if(shipId != 0) {
                            kcaDockingRunnableList[dockId] = new KcaDocking(dockId, shipId, completeTime, nHandler);
                            kcaDockingList[dockId] = new Thread(kcaDockingRunnableList[dockId]);
                            kcaDockingList[dockId].start();
                        }
                    }
                    else {
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
        boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean is_portrait = !is_landscape;
        boolean no_exp_flag = true;

        String nodeString = "";
        if (currentNode.length() > 0) {
            nodeString = String.format(" [%s]", currentNode);
        }
        String notifiTitle = "";
        switch(heavyDamagedMode) {
            case HD_DAMECON:
                notifiTitle = String.format("[대파/다메콘 있음!] %s 동작중%s", getResources().getText(R.string.app_name), nodeString);
                break;
            case HD_DANGER:
                notifiTitle = String.format("[대파 있음!] %s 동작중%s", getResources().getText(R.string.app_name), nodeString);
                break;
            default:
                notifiTitle = String.format("%s 동작중%s", getResources().getText(R.string.app_name), nodeString);
                break;
        }

        String notifiString = "";
        String expeditionString = "";
        String expString = "";
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
                if (data.get("is_portrait_newline").getAsInt() == 1 && is_portrait) {
                    notifiString = notifiString.concat(joinStr(deckInfoStringList, " / ")).concat("\n");
                    deckInfoStringList.clear();
                }
            }
            notifiString = notifiString.concat(joinStr(deckInfoStringList, " / "));
        } catch (Exception e) {
            notifiString = kcaFirstDeckInfo;
        }

        List<String> kcaExpStrList = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            if (KcaExpedition.left_time_str[i] != null) {
                no_exp_flag = false;
                kcaExpStrList.add(KcaExpedition.left_time_str[i]);
            }
        }

        if (no_exp_flag) {
            expeditionString = expeditionString.concat("원정나간 함대가 없습니다.");
        } else {
            if (is_landscape) {
                expeditionString = expeditionString.concat("원정 남은 시간: ");
                //expeditionString = expeditionString.concat("\n");
            }
            expeditionString = expeditionString.concat(joinStr(kcaExpStrList, " / "));
        }
        notifiManager.notify(getNotificationId(NOTI_FRONT, 1), createViewNotification(notifiTitle, notifiString, expeditionString));
    }

    private String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }

    private Map<Integer, HashMap<String, String>> getExpeditionData() {
        Map<Integer, HashMap<String, String>> data = new HashMap<Integer, HashMap<String, String>>();
        XmlResourceParser xpp = getResources().getXml(R.xml.expeditions);
        try {
            xpp.next();
            int eventType = xpp.getEventType();
            int mode = -1;
            int currentNo = 0;
            HashMap<String, String> item = null;
            int childItemNo = 3;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // Start Of Document
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().startsWith("item")) {
                        mode = 0;
                        item = new HashMap<String, String>();
                    } else if (xpp.getName().startsWith("no")) {
                        mode = 1;
                    } else if (xpp.getName().startsWith("krname")) {
                        mode = 2;
                    } else if (xpp.getName().startsWith("time")) {
                        mode = 3;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (mode > 0) {
                        mode = (mode - 1) / childItemNo;
                    } else if (mode == 0) {
                        data.put(currentNo, item);
                        mode = -1;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (mode == 1) {
                        currentNo = Integer.parseInt(xpp.getText());
                    } else if (mode == 2) {
                        item.put("krname", xpp.getText());
                    } else if (mode == 3) {
                        item.put("time", xpp.getText());
                    }
                }
                eventType = xpp.next();
            }
            return data;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getShipTranslationData() {
        Map<String, String> data = new HashMap<String, String>();
        int mode = 0;
        String jp_name = "";
        String kr_name = "";
        XmlResourceParser xpp = getResources().getXml(R.xml.ships_translation);
        try {
            xpp.next();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // Start Of Document
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().startsWith("Ship")) {
                        mode = 0;
                        jp_name = "";
                        kr_name = "";
                    } else if (xpp.getName().startsWith("JP-Name")) {
                        mode = 1;
                    } else if (xpp.getName().startsWith("TR-Name")) {
                        mode = 2;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.getName().startsWith("Ship")) {
                        data.put(jp_name, kr_name);
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (mode == 1) {
                        jp_name = xpp.getText();
                    } else if (mode == 2) {
                        kr_name = xpp.getText();
                    }
                }
                eventType = xpp.next();
            }
            return data;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean isJSONValid(String jsonInString) {
        Gson gson = new Gson();
        try {
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }


    private int getNotificationId(int type, int n) {
        return n + 1000 * type;
    }

    private class retrieveApiStartData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String content = null;

            content = executeClient(params[0], params[1], params[2]);

            return content;
        }

        public byte[] gzipcompress(String value) throws Exception {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                    new BufferedOutputStream(byteArrayOutputStream));
            gzipOutStream.write(value.getBytes());
            gzipOutStream.finish();
            gzipOutStream.close();

            return byteArrayOutputStream.toByteArray();
        }

        public byte[] gzipdecompress(byte[] contentBytes){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try{
                ByteStreams.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
            } catch(IOException e){
                throw new RuntimeException(e);
            }
            return out.toByteArray();
        }

        public String executeClient(String token, String method, String data) {
            HttpURLConnection http = null;
            try {
                if (method.equals("up")) {
                    URL data_send_url = new URL(String.format("http://antest.hol.es/kcanotify/kca_api_start2.php?token=%s&method=%s&v=%s", token, method, kca_version));
                    http = (HttpURLConnection) data_send_url.openConnection();
                    http.setRequestMethod("POST");
                    http.setDoInput(true);
                    http.setDoOutput(true);
                    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    OutputStream outStream = http.getOutputStream();
                    outStream.write(gzipcompress(data));
                    outStream.flush();
                    outStream.close();
                    http.getResponseCode();
                } else {
                    if (KcaApiData.isGameDataLoaded()) return null;
                    kcaFirstDeckInfo = "게임 데이터 로딩 중";
                    URL data_send_url;
                    if(kca_version == null) {
                        data_send_url = new URL(String.format("http://antest.hol.es/kcanotify/kca_api_start2.php?v=recent"));
                    } else {
                        data_send_url = new URL(String.format("http://antest.hol.es/kcanotify/kca_api_start2.php?v=%s", kca_version));
                    }
                    http = (HttpURLConnection) data_send_url.openConnection();
                    http.setRequestMethod("GET");
                    http.setDoInput(true);
                    http.setRequestProperty("Referer", "app:/KCA/");
                    http.setRequestProperty("Accept-Encoding", "gzip");
                    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    http.getResponseCode();

                    InputStream is = http.getInputStream();
                    //byte[] bytes = gzipdecompress(ByteStreams.toByteArray(is));
                    byte[] bytes = ByteStreams.toByteArray(is);
                    String input_data = new String(bytes);
                    Log.e("KCA", input_data);

                    writeCacheData(bytes, KCANOTIFY_S2_CACHE_FILENAME);
                    JsonObject jsonDataObj = new JsonParser().parse(input_data).getAsJsonObject();

                    if (jsonDataObj.has("api_data")) {
                        //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                        KcaApiData.getKcGameData(jsonDataObj.getAsJsonObject("api_data"));
                        if (kca_version == null) {
                            kca_version = http.getHeaderField("X-Api-Version");
                        }
                        setPreferences("kca_version", kca_version);
                        processFirstDeckInfo(currentPortDeckData);
                    }
                }
            } catch (ProtocolException | MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(http != null) http.disconnect();
            }

            return null;
        }
    }

}