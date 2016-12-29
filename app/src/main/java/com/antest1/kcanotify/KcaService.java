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
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;

public class KcaService extends Service {
    public static final int ANTEST_USERID = 15108389;
    public static final String KCA_VERSION = "/kca/version.json";
    public static final String KCANOTIFY_S2 = "/kcanotify/kca_api_start2.php";
    public static final String KCANOTIFY_S2_CACHE_FILENAME = "kca_api_start2";


    public static final String API_PORT = "/api_port/port";
    public static final String API_START2 = "/api_start2";
    public static final String API_GET_MEMBER_REQUIRED_INFO = "/api_get_member/require_info";
    public static final String API_REQ_MEMBER_PRESET_DECK = "/api_get_member/preset_deck";
    public static final String API_GET_MEMBER_DECK = "/api_get_member/deck";
    public static final String API_GET_MEMBER_SHIP_DECK = "/api_get_member/ship_deck";
    public static final String API_GET_MEMBER_SLOT_ITEM = "/api_get_member/slot_item";
    public static final String API_REQ_MISSION_RETURN = "/api_req_mission/return_instruction";

    public static final String API_REQ_HENSEI_CHANGE = "/api_req_hensei/change";
    public static final String API_REQ_HENSEI_PRESET = "/api_req_hensei/preset_select";

    public static final String API_GET_MEMBER_SHIP3 = "/api_get_member/ship3";
    public static final String API_REQ_KAISOU_SLOT_EXCHANGE = "/api_req_kaisou/slot_exchange";
    public static final String API_REQ_KAISOU_SLOT_DEPRIVE = "/api_req_kaisou/slot_deprive";

    public static final String API_REQ_KOUSYOU_CREATETIEM = "/api_req_kousyou/createitem";
    public static final String API_REQ_KOUSYOU_DESTROYITEM = "/api_req_kousyou/destroyitem2";
    public static final String API_REQ_KOUSYOU_GETSHIP = "/api_req_kousyou/getship";
    public static final String API_REQ_KOUSYOU_DESTROYSHIP = "/api_req_kousyou/destroyship";

    public static final String API_GET_MEMBER_MAPINFO = "/api_get_member/mapinfo";
    public static final String API_REQ_MAP_START = "/api_req_map/start";
    public static final String API_REQ_MAP_NEXT = "/api_req_map/next";
    public static final String API_REQ_SORTIE_BATTLE = "/api_req_sortie/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT = "/api_req_battle_midnight/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT_SP = "/api_req_battle_midnight/sp_midnight";
    public static final String API_REQ_SORTIE_AIRBATTLE = "/api_req_sortie/airbattle";
    public static final String API_REQ_SORTIE_LDAIRBATTLE = "/api_req_sortie/ld_airbattle";
    public static final String API_REQ_SORTIE_BATTLE_RESULT = "/api_req_sortie/battleresult";

    public static final String API_REQ_COMBINED_BATTLE = "/api_req_combined_battle/battle"; // 기동
    public static final String API_REQ_COMBINED_BATTLE_WATER = "/api_req_combined_battle/battle_water"; // 수상
    public static final String API_REQ_COMBINED_BATTLE_EC = "/api_req_combined_battle/ec_battle"; // 단일-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH = "/api_req_combined_battle/each_battle"; // 기동-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH_WATER = "/api_req_combined_battle/each_battle_water"; // 수상-연합

    public static final String API_REQ_COMBINED_AIRBATTLE = "/api_req_combined_battle/airbattle"; // 아웃레인지
    public static final String API_REQ_COMBINED_LDAIRBATTLE = "/api_req_combined_battle/ld_airbattle"; // 공습

    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT = "/api_req_combined_battle/midnight_battle";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_SP = "/api_req_combined_battle/sp_midnight";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_EC = "/api_req_combined_battle/ec_midnight_battle"; // 단대연 야전

    public static final String API_REQ_COMBINED_BATTLERESULT = "/api_req_combined_battle/battleresult";
    public static final String API_REQ_COMBINED_GOBACKPORT = "/api_req_combined_battle/goback_port"; // 퇴피

    public static List<String> API_BATTLE_REQS;
    public static final String[] API_BATTLE_REQ_LIST = new String[]{
            API_REQ_MAP_START,
            API_REQ_MAP_NEXT,
            API_REQ_SORTIE_BATTLE,
            API_REQ_SORTIE_BATTLE_MIDNIGHT,
            API_REQ_SORTIE_BATTLE_MIDNIGHT_SP,
            API_REQ_SORTIE_AIRBATTLE,
            API_REQ_SORTIE_LDAIRBATTLE,
            API_REQ_SORTIE_BATTLE_RESULT,
            API_REQ_COMBINED_BATTLE,
            API_REQ_COMBINED_BATTLE_WATER,
            API_REQ_COMBINED_AIRBATTLE,
            API_REQ_COMBINED_LDAIRBATTLE,
            API_REQ_COMBINED_BATTLE_MIDNIGHT,
            API_REQ_COMBINED_BATTLE_MIDNIGHT_SP,
            API_REQ_COMBINED_BATTLE_EC,
            API_REQ_COMBINED_BATTLE_EACH,
            API_REQ_COMBINED_BATTLE_EACH_WATER,
            API_REQ_COMBINED_BATTLE_MIDNIGHT_EC,
            API_REQ_COMBINED_BATTLERESULT,
            API_REQ_COMBINED_GOBACKPORT
    };

    public static final String KCA_API_NOTI_EXP_LEFT = "/kca_api/noti_exp_left";
    public static final String KCA_API_NOTI_EXP_FIN = "/kca_api/noti_exp_fin";
    public static final String KCA_API_NOTI_EXP_CANCELED = "/kca_api/noti_exp_canceled";
    public static final String KCA_API_NOTI_HEAVY_DMG = "/kca_api/noti_heavy_dmg";
    public static final String KCA_API_NOTI_BATTLE_INFO = "/kca_api/noti_battle_info";
    public static final String KCA_API_NOTI_GOBACKPORT = "/kca_api/noti_gobackport";

    public static final int NOTI_FRONT = 0;
    public static final int NOTI_EXP = 1;

    public static final int FRONT_NONE = 0;
    public static final int FRONT_EXP_SET = 2;

    public static final int HD_NONE = 0;
    public static final int HD_DAMECON = 1;
    public static final int HD_DANGER = 2;

    public static boolean isServiceOn = false;
    public static boolean isPortAccessed = false;
    public static int heavyDamagedMode = 0;
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
    String kcaFirstDeckInfo = "깡들리티에서 게임을 실행해주세요";
    String kca_version;
    String api_start2_data = null;
    boolean api_start2_down_mode = false;
    JsonArray currentPortDeckData = null;
    SharedPreferences preferences;
    Gson gson = new Gson();

    public static boolean getServiceStatus() {
        return isServiceOn;
    }

    private String getPreferences(String key) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        return pref.getString(key, "");
    }

    // 값 저장하기
    private void setPreferences(String key, String value) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private JsonObject readCacheData() {
        try {
            FileInputStream fis = openFileInput(KCANOTIFY_S2_CACHE_FILENAME);
            byte[] cache_data = new byte[fis.available()];
            //Toast.makeText(getApplicationContext(), String.format("Loading Cached Data %d", fis.available()), Toast.LENGTH_LONG).show();
            while (fis.read(cache_data) != -1) {
                ;
            }
            String cache_data_str = new String(cache_data, 0, cache_data.length);
            return new JsonParser().parse(cache_data_str).getAsJsonObject();
        } catch (FileNotFoundException e) {
            new retrieveApiStartData().execute("", "down", "");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeCacheData(byte[] data) throws IOException {
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
        API_BATTLE_REQS = Arrays.asList(API_BATTLE_REQ_LIST);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notifiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        handler = new kcaServiceHandler();
        nHandler = new kcaNotificationHandler();
        kcaExpeditionRunnableList = new KcaExpedition[3];
        isPortAccessed = false;

        if(getPreferences("kca_seek_cn").equals("")) {
            setPreferences("kca_seek_cn", "1");
        }

        KcaProxyServer.start(handler);

        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String initTitle = String.format("%s 동작중", getResources().getText(R.string.app_name));
        String initContent = "깡들리티에서 게임을 실행해주세요";
        String initSubContent = String.format("%s %s", getResources().getText(R.string.app_name), BuildConfig.VERSION_NAME);

        startForeground(getNotificationId(NOTI_FRONT, 1), createViewNotification(initTitle, initContent, initSubContent));
        isServiceOn = true;

        KcaBattle.setHandler(nHandler);

        return START_STICKY;
    }

    // 서비스가 종료될 때 할 작업

    public void onDestroy() {
        MainActivity.isKcaServiceOn = false;
        for (int i = 0; i < 3; i++) {
            if (kcaExpeditionList[i] != null) {
                kcaExpeditionRunnableList[i].stopHandler();
                kcaExpeditionList[i].interrupt();
                kcaExpeditionList[i] = null;
            }
        }

        handler = null;
        nHandler = null;

        stopForeground(true);
        notifiManager.cancelAll();
        viewNotificationBuilder = null;
        //KcaProxyServer.stop();
        isServiceOn = false;
    }

    private Notification createViewNotification(String title, String content1, String content2) {
        if (viewNotificationBuilder == null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            viewNotificationText = new Notification.BigTextStyle();
            viewNotificationBuilder = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.mipmap.ic_stat_notify)
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

        Notification Notifi = new Notification.Builder(getApplicationContext()).setSmallIcon(R.mipmap.ic_stat_notify)
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
                    if (!getPreferences("kca_version").equals(kca_version)) {
                        api_start2_down_mode = true;
                    } else {
                        api_start2_down_mode = false;
                        JsonObject kcDataObj = readCacheData();
                        //Log.e("KCA", kcDataObj.toJSONString());
                        if (kcDataObj != null && kcDataObj.has("api_data")) {
                            //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                            KcaApiData.getKcGameData(kcDataObj.get("api_data").getAsJsonObject());
                            setPreferences("kca_version", kca_version);
                        }
                    }
                    //Toast.makeText(getApplicationContext(), getPreferences("kca_version") + " " + String.valueOf(api_start2_down_mode), Toast.LENGTH_LONG).show();
                }

                if (url.startsWith(API_START2)) {
                    //Log.e("KCA", "Load Kancolle Data");
                    //Toast.makeText(getApplicationContext(), "API_START2", Toast.LENGTH_LONG).show();

                    api_start2_data = data;
                    writeCacheData(data.getBytes());

                    if (jsonDataObj.has("api_data")) {
                        //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                        KcaApiData.getKcGameData(jsonDataObj.get("api_data").getAsJsonObject());
                        setPreferences("kca_version", kca_version);
                    }

                }

                if (url.startsWith(API_PORT)) {
                    isPortAccessed = true;
                    heavyDamagedMode = HD_NONE;
                    Log.e("KCA", "Port Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject reqPortApiData = jsonDataObj.get("api_data").getAsJsonObject();
                        int size = KcaApiData.getPortData(reqPortApiData);
                        //Log.e("KCA", "Total Ships: " + String.valueOf(size));
                        if (reqPortApiData.has("api_deck_port")) {
                            JsonArray reqPortDeckApiData = reqPortApiData.get("api_deck_port").getAsJsonArray();
                            currentPortDeckData = reqPortDeckApiData;
                            processFirstDeckInfo(currentPortDeckData);
                            processExpeditionInfo(currentPortDeckData);
                        }
                    }
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }

                if (url.startsWith(API_GET_MEMBER_MAPINFO)) {
                    heavyDamagedMode = KcaDeckInfo.checkHeavyDamageExist(currentPortDeckData, 0);
                    switch(heavyDamagedMode) {
                        case HD_DAMECON:
                        case HD_DANGER:
                            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(1500);
                            }
                            if(heavyDamagedMode == HD_DANGER) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged), Toast.LENGTH_LONG).show();
                            } else if (heavyDamagedMode == HD_DAMECON) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged_damecon), Toast.LENGTH_LONG).show();
                            }
                            break;
                        default:
                            break;
                    }
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                }

                if (url.startsWith(API_GET_MEMBER_REQUIRED_INFO)) {
                    //Log.e("KCA", "Load Item Data");

                    if (jsonDataObj.has("api_data")) {
                        JsonObject requiredInfoApiData = jsonDataObj.get("api_data").getAsJsonObject();
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
                }

                if (API_BATTLE_REQS.contains(url)) {
                    //Log.e("KCA", "Battle Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject battleApiData = jsonDataObj.get("api_data").getAsJsonObject();
                        KcaBattle.processData(url, battleApiData);
                    }
                }

                if (url.startsWith(API_GET_MEMBER_SHIP_DECK)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
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

                if (url.startsWith(API_GET_MEMBER_DECK)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonArray reqGetMemberDeckApiData = jsonDataObj.get("api_data").getAsJsonArray();
                        processExpeditionInfo(reqGetMemberDeckApiData);
                    }
                }

                if (url.startsWith(API_REQ_MISSION_RETURN)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.has("api_data")) {
                        JsonObject reqGetMemberDeckApiData = jsonDataObj.get("api_data").getAsJsonObject();
                        cancelExpeditionInfo(reqGetMemberDeckApiData);
                    }
                }

                if (url.startsWith(API_GET_MEMBER_SLOT_ITEM)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonArray api_data = jsonDataObj.get("api_data").getAsJsonArray();
                        KcaApiData.updateSlotItemData(api_data);
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_CREATETIEM)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
                        KcaApiData.addUserItem(api_data);
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_DESTROYITEM)) {
                    String[] requestData = request.split("&");
                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_slotitem_ids")) {
                            String itemlist = decodedData.replace("api_slotitem_ids=", "");
                            KcaApiData.deleteUserItem(itemlist);
                            break;
                        }
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_GETSHIP)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
                        KcaApiData.addUserShip(api_data);
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_DESTROYSHIP)) {
                    String targetShip = "";
                    String[] requestData = request.split("&");
                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_ship_id")) {
                            targetShip = decodedData.replace("api_ship_id=", "");
                            KcaApiData.deleteUserShip(targetShip);
                            break;
                        }
                    }
                    for (int i=0; i<currentPortDeckData.size(); i++) {
                        JsonObject deckData = currentPortDeckData.get(i).getAsJsonObject();
                        JsonArray deckShipData = deckData.get("api_ship").getAsJsonArray();
                        for (int j=0; j<deckShipData.size(); j++) {
                            if(targetShip.equals(String.valueOf(deckShipData.get(j).getAsInt()))) {
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

                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_ship_idx=")) {
                            shipIdx = Integer.valueOf(decodedData.replace("api_ship_idx=", ""));
                        } else if(decodedData.startsWith("api_ship_id=")) {
                            shipId = Integer.valueOf(decodedData.replace("api_ship_id=", ""));
                        } else if(decodedData.startsWith("api_id=")) {
                            deckIdx = Integer.valueOf(decodedData.replace("api_id=", "")) - 1;
                        }
                    }
                    if (deckIdx != -1) {
                        JsonObject targetDeckIdxData = currentPortDeckData.get(deckIdx).getAsJsonObject();
                        JsonArray targetDeckIdxShipIdata = targetDeckIdxData.get("api_ship").getAsJsonArray();

                        if (shipId == -2) {
                            for(int i=1; i<6; i++) {
                                targetDeckIdxShipIdata.set(i, new JsonPrimitive(-1));
                            }
                        }
                        else if (shipId == -1) { // remove ship
                            targetDeckIdxShipIdata.remove(shipIdx);
                            targetDeckIdxShipIdata.add(new JsonPrimitive(-1));
                        } else { // add ship
                            int originalDeckIdx = -1;
                            int originalShipIdx = -1;
                            // check whether target ship is in deck
                            for (int i=0; i<currentPortDeckData.size(); i++) {
                                JsonArray deckData = currentPortDeckData.get(i).getAsJsonObject().get("api_ship").getAsJsonArray();
                                for (int j=0; j<deckData.size(); j++) {
                                    if(shipId == deckData.get(j).getAsInt()) {
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
                                if(replacement.getAsInt() != -1) {
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
                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_deck_id=")) {
                            deckIdx = Integer.valueOf(decodedData.replace("api_deck_id=", "")) - 1;
                            break;
                        }
                    }
                    if (deckIdx != -1) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
                            currentPortDeckData.set(deckIdx, api_data);
                        }
                    }
                    processFirstDeckInfo(currentPortDeckData);
                }

                if (url.startsWith(API_GET_MEMBER_SHIP3)) {
                    String[] requestData = request.split("&");
                    int userShipId = -1;
                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_shipid=")) {
                            userShipId = Integer.valueOf(decodedData.replace("api_shipid=", ""));
                            break;
                        }
                    }
                    if (userShipId != -1) {
                        if (jsonDataObj.has("api_data")) {
                            JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
                            currentPortDeckData = api_data.get("api_deck_data").getAsJsonArray();
                            KcaApiData.updateUserShip(api_data.get("api_ship_data").getAsJsonArray().get(0).getAsJsonObject());
                        }
                    }
                    processFirstDeckInfo(currentPortDeckData);
                    toastInfo();
                }

                if(url.startsWith(API_REQ_KAISOU_SLOT_EXCHANGE)) {
                    String[] requestData = request.split("&");
                    int userShipId = -1;
                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_id=")) {
                            userShipId = Integer.valueOf(decodedData.replace("api_id=", ""));
                            break;
                        }
                    }
                    if (userShipId != -1 && jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
                        KcaApiData.updateUserShipSlot(userShipId, api_data);
                    }
                    processFirstDeckInfo(currentPortDeckData);
                    toastInfo();
                }

                if(url.startsWith(API_REQ_KAISOU_SLOT_DEPRIVE)) {
                    if (jsonDataObj.has("api_data")) {
                        JsonObject api_data = jsonDataObj.get("api_data").getAsJsonObject();
                        JsonObject api_ship_data = api_data.get("api_ship_data").getAsJsonObject();
                        KcaApiData.updateUserShip(api_ship_data.get("api_set_ship").getAsJsonObject());
                        KcaApiData.updateUserShip(api_ship_data.get("api_unset_ship").getAsJsonObject());
                    }
                    processFirstDeckInfo(currentPortDeckData);
                    toastInfo();
                }

            } catch (JsonSyntaxException e) {
                //Log.e("KCA", "ParseError");
                //Log.e("KCA", data);
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "오류가 발생하였습니다.", Toast.LENGTH_LONG).show();
                onDestroy();
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
                    notifiManager.notify(getNotificationId(NOTI_EXP, missionNo), createExpeditionNotification(missionNo, missionName, kantaiName, true));
                }

                if (url.startsWith(KCA_API_NOTI_EXP_FIN)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    int kantaiIndex = jsonDataObj.get("kantai_idx").getAsInt();
                    String kantaiName = jsonDataObj.get("kantai_name").getAsString();
                    int missionNo = jsonDataObj.get("mission_no").getAsInt();
                    String missionName = jsonDataObj.get("mission_krname").getAsString();
                    //Intent intent = new Intent(KcaService.this, MainActivity.class);

                    kcaExpeditionInfoList[kantaiIndex] = "";
                    kcaExpeditionInfoList[kantaiIndex] = "";
                    notifiManager.notify(getNotificationId(NOTI_EXP, missionNo), createExpeditionNotification(missionNo, missionName, kantaiName, false));
                }

                if (url.startsWith(KCA_API_NOTI_BATTLE_INFO)) {
                    String message = jsonDataObj.get("msg").getAsString();
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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

            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "오류가 발생하였습니다.", Toast.LENGTH_LONG).show();
                onDestroy();
                throw e;
            }
        }
    }

    private int getSeekCn() {
        return Integer.valueOf(getPreferences("kca_seek_cn"));
    }

    private String getSeekType() {
        int cn = Integer.valueOf(getPreferences("kca_seek_cn"));
        String seekType = "";
        switch(cn) {
            case 1:
                seekType = getResources().getString(R.string.seek_type_1);
                break;
            case 3:
                seekType = getResources().getString(R.string.seek_type_3);
                break;
            case 4:
                seekType = getResources().getString(R.string.seek_type_3);
                break;
            default:
                seekType = getResources().getString(R.string.seek_type_0);
                break;
        }
        return seekType;
    }

    private void processFirstDeckInfo(JsonArray data) {
        String delimeter = " | ";

        if (!isGameDataLoaded()) {
            Log.e("KCA", "processFirstDeckInfo: Game Data is Null");
            new retrieveApiStartData().execute("", "down", "");
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

        String seekValue =  String.format("색적(%s): %.2f", seekType, KcaDeckInfo.getSeekValue(data, 0, cn));
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
            if (kcaExpeditionList[idx] != null) {
                if (mission_no == -1) {
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

    public void setFrontViewNotifier(int type, int id, String content) {
        boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean is_portrait = !is_landscape;
        boolean no_exp_flag = true;

        String notifiTitle = "";
        switch(heavyDamagedMode) {
            case HD_DAMECON:
                notifiTitle = String.format("[대파/다메콘 있음!] %s 동작중", getResources().getText(R.string.app_name));
                break;
            case HD_DANGER:
                notifiTitle = String.format("[대파 있음!] %s 동작중", getResources().getText(R.string.app_name));
                break;
            default:
                notifiTitle = String.format("%s 동작중", getResources().getText(R.string.app_name));
                break;
        }

        String notifiString = "";
        String expeditionString = "";
        String expString = "";
        if(isJSONValid(kcaFirstDeckInfo)) {
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
        } else {
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
            try {
                content = executeClient(params[0], params[1], params[2]);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        public String executeClient(String token, String method, String data) throws Exception {
            if (kca_version == null) return null;
            if (method == "up") {
                URL data_send_url = new URL(String.format("http://antest.hol.es/kcanotify/kca_api_start2.php?token=%s&method=%s&v=%s", token, method, kca_version));
                HttpURLConnection http = (HttpURLConnection) data_send_url.openConnection();
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
                kcaFirstDeckInfo = "게임 데이터 로딩 중";
                URL data_send_url = new URL(String.format("http://antest.hol.es/kcanotify/kca_api_start2.php?v=%s", kca_version));
                HttpURLConnection http = (HttpURLConnection) data_send_url.openConnection();
                http.setRequestMethod("GET");
                http.setDoInput(true);
                http.setRequestProperty("Referer", "app:/KCA/");
                http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                http.getResponseCode();
                InputStream is = http.getInputStream();
                byte[] bytes = ByteStreams.toByteArray(is);
                String input_data = new String(bytes);

                writeCacheData(bytes);
                JsonObject jsonDataObj = new JsonParser().parse(input_data).getAsJsonObject();

                if (jsonDataObj.has("api_data")) {
                    //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                    KcaApiData.getKcGameData(jsonDataObj.get("api_data").getAsJsonObject());
                    setPreferences("kca_version", kca_version);
                    processFirstDeckInfo(currentPortDeckData);
                }
            }
            return null;
        }
    }

}