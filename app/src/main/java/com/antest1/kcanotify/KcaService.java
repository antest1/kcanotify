package com.antest1.kcanotify;

import android.app.Notification;
import android.app.Notification.Builder;
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
import com.google.common.io.Resources;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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


import static com.antest1.kcanotify.KcaApiData.addUserItem;
import static com.antest1.kcanotify.KcaApiData.addUserShip;
import static com.antest1.kcanotify.KcaApiData.deleteUserItem;
import static com.antest1.kcanotify.KcaApiData.deleteUserShip;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.kcGameData;

public class KcaService extends Service {
    public static final int ANTEST_USERID = 15108389;
    public static final String KCA_VERSION = "/kca/version.json";
    public static final String KCANOTIFY_S2 = "/kcanotify/kca_api_start2.php";
    public static final String KCANOTIFY_S2_CACHE_FILENAME = "kca_api_start2";


    public static final String API_PORT = "/api_port/port";
    public static final String API_START2 = "/api_start2";
    public static final String API_GET_MEMBER_REQUIRED_INFO = "/api_get_member/require_info";
    public static final String API_GET_MEMBER_DECK = "/api_get_member/deck";
    public static final String API_REQ_MISSION_RETURN = "/api_req_mission/return_instruction";

    public static final String API_REQ_HENSEI_CHANGE = "/api_req_hensei/change";
    public static final String API_REQ_HENSEI_PRESET = "/api_req_hensei/preset_select";

    public static final String API_REQ_KOUSYOU_CREATETIEM = "/api_req_kousyou/createitem";
    public static final String API_REQ_KOUSYOU_DESTROYITEM = "/api_req_kousyou/destroyitem2";
    public static final String API_REQ_KOUSYOU_GETSHIP = "/api_req_kousyou/getship";
    public static final String API_REQ_KOUSYOU_DESTROYSHIP = "/api_req_kousyou/destroyship";

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

    public static boolean isServiceOn = false;
    public static boolean isPortAccessed = false;
    public static boolean heavyDamagedMode = false;
    public static Intent kcIntent = null;

    AudioManager mAudioManager;

    NotificationManager Notifi_M;
    Notification Notifi;
    Builder viewNotifi;
    public static boolean noti_vibr_on = true;

    kcaServiceHandler handler;
    kcaNotificationHandler nHandler;
    Thread[] kcaExpeditionList = new Thread[3];
    KcaExpedition[] kcaExpeditionRunnableList = new KcaExpedition[3];
    String[] kcaExpeditionInfoList = new String[3];
    String kcaFirstDeckInfo = "깡들리티에서 게임을 시작해주세요";
    String kca_version;
    String api_start2_data = null;
    boolean api_start2_down_mode = false;
    JSONArray currentPortDeckData = null;
    SharedPreferences preferences;

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

    private JSONObject readCacheData() {
        try {
            JSONParser jsonParser = new JSONParser();
            FileInputStream fis = openFileInput(KCANOTIFY_S2_CACHE_FILENAME);
            byte[] cache_data = new byte[fis.available()];
            //Toast.makeText(getApplicationContext(), String.format("Loading Cached Data %d", fis.available()), Toast.LENGTH_LONG).show();
            while (fis.read(cache_data) != -1) {
                ;
            }
            String cache_data_str = new String(cache_data, 0, cache_data.length);
            return (JSONObject) jsonParser.parse(cache_data_str);
        } catch (FileNotFoundException e) {
            new retApiStartData().execute("", "down", "");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ParseException e) {
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
        if (!MainActivity.isKcaProxyOn) {
            stopSelf();
        }

        for (int i = 0; i < 3; i++) {
            kcaExpeditionList[i] = null;
            kcaExpeditionInfoList[i] = "";
        }

        KcaExpedition.expeditionData = getExpeditionData();
        API_BATTLE_REQS = Arrays.asList(API_BATTLE_REQ_LIST);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        handler = new kcaServiceHandler();
        nHandler = new kcaNotificationHandler();
        kcaExpeditionRunnableList = new KcaExpedition[3];
        isPortAccessed = false;

        if(getPreferences("kca_seek_cn").equals("")) {
            setPreferences("kca_seek_cn", "1");
        }

        KcaBattle.setHandler(nHandler);
        KcaProxyServer.start(handler);

        Intent aIntent = new Intent(KcaService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        viewNotifi = new Notification.Builder(getApplicationContext())
                // .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                // R.drawable.ic_launcher))
                .setSmallIcon(R.mipmap.ic_stat_notify)
                .setContentTitle(String.format("%s 동작중", getResources().getText(R.string.app_name)))
                .setStyle(new Notification.BigTextStyle().bigText("깡들리티에서 게임을 시작해주세요"))
                .setTicker(String.format("%s 동작중", getResources().getText(R.string.app_name)))
                .setContentIntent(pendingIntent)
                .setOngoing(true).setAutoCancel(false);

        startForeground(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());
        isServiceOn = true;

        return START_STICKY;
    }

    // 서비스가 종료될 때 할 작업

    public void onDestroy() {
        MainActivity.isKcaProxyOn = false;
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
        Notifi_M.cancelAll();
        viewNotifi = null;
        KcaProxyServer.stop();
        isServiceOn = false;
    }

    class kcaServiceHandler extends Handler {
        JSONParser jsonParser = new JSONParser();

        @Override
        public void handleMessage(Message msg) {
            String url = msg.getData().getString("url");
            String data = msg.getData().getString("data");
            String request = msg.getData().getString("request");

            if (!KcaProxyServer.is_on() || url.length() == 0 || viewNotifi == null) {
                return;
            }

            JSONObject jsonDataObj;
            try {
                jsonDataObj = (JSONObject) jsonParser.parse(data);

                if (url.startsWith(KCA_VERSION)) {
                    //Toast.makeText(getApplicationContext(), "KCA_VERSION", Toast.LENGTH_LONG).show();
                    JSONObject api_version = (JSONObject) jsonDataObj.get("api");
                    kca_version = (String) api_version.get("api_start2");
                    if (!getPreferences("kca_version").equals(kca_version)) {
                        api_start2_down_mode = true;
                    } else {
                        api_start2_down_mode = false;
                        JSONObject kcDataObj = readCacheData();
                        //Log.e("KCA", kcDataObj.toJSONString());
                        if (kcDataObj != null && kcDataObj.containsKey("api_data")) {
                            //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                            KcaApiData.getKcGameData((JSONObject) kcDataObj.get("api_data"));
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

                    if (jsonDataObj.containsKey("api_data")) {
                        //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                        KcaApiData.getKcGameData((JSONObject) jsonDataObj.get("api_data"));
                        setPreferences("kca_version", kca_version);
                    }

                }

                if (url.startsWith(API_PORT)) {
                    isPortAccessed = true;
                    heavyDamagedMode = false;
                    Log.e("KCA", "Port Handler Called");
                    if (jsonDataObj.containsKey("api_data")) {
                        JSONObject reqPortApiData = (JSONObject) jsonDataObj.get("api_data");
                        int size = KcaApiData.getPortData(reqPortApiData);
                        //Log.e("KCA", "Total Ships: " + String.valueOf(size));
                        if (reqPortApiData.containsKey("api_deck_port")) {
                            JSONArray reqPortDeckApiData = (JSONArray) reqPortApiData.get("api_deck_port");
                            currentPortDeckData = reqPortDeckApiData;
                            processFirstDeckInfo(currentPortDeckData);
                            processExpeditionInfo(currentPortDeckData);
                        }
                    }
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    Notifi_M.notify(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());

                }

                if (API_BATTLE_REQS.contains(url)) {
                    //Log.e("KCA", "Battle Handler Called");
                    if (jsonDataObj.containsKey("api_data")) {
                        JSONObject battleApiData = (JSONObject) jsonDataObj.get("api_data");
                        KcaBattle.processData(url, battleApiData);
                    }
                }

                if (url.startsWith(API_GET_MEMBER_DECK)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.containsKey("api_data")) {
                        JSONArray reqGetMemberDeckApiData = (JSONArray) jsonDataObj.get("api_data");
                        processExpeditionInfo(reqGetMemberDeckApiData);
                    }
                }

                if (url.startsWith(API_REQ_MISSION_RETURN)) {
                    //Log.e("KCA", "Expedition Handler Called");
                    if (jsonDataObj.containsKey("api_data")) {
                        JSONObject reqGetMemberDeckApiData = (JSONObject) jsonDataObj.get("api_data");
                        cancelExpeditionInfo(reqGetMemberDeckApiData);
                    }
                }

                if (url.startsWith(API_GET_MEMBER_REQUIRED_INFO)) {
                    //Log.e("KCA", "Load Item Data");

                    if (jsonDataObj.containsKey("api_data")) {
                        JSONObject requiredInfoApiData = (JSONObject) jsonDataObj.get("api_data");
                        int size = KcaApiData.getSlotItemData(requiredInfoApiData);
                        int userId = KcaApiData.getUserId(requiredInfoApiData);
                        //Log.e("KCA", "Total Items: " + String.valueOf(size));
                        //Toast.makeText(getApplicationContext(), String.valueOf(userId), Toast.LENGTH_LONG).show();

                        if (userId == ANTEST_USERID && api_start2_down_mode && api_start2_data != null) {
                            Toast.makeText(getApplicationContext(), "Uploading Data...", Toast.LENGTH_LONG).show();
                            new retApiStartData().execute("3a4104a5ef67f0823f78a636fbd2bbbf", "up", api_start2_data);
                        } else if (api_start2_data == null && api_start2_down_mode) {
                            Toast.makeText(getApplicationContext(), String.format("Downloading Data"), Toast.LENGTH_LONG).show();
                            new retApiStartData().execute("", "down", "");
                        }
                    }

                }

                if (url.startsWith(API_REQ_KOUSYOU_CREATETIEM)) {
                    if (jsonDataObj.containsKey("api_data")) {
                        JSONObject api_data = (JSONObject) jsonDataObj.get("api_data");
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
                    if (jsonDataObj.containsKey("api_data")) {
                        JSONObject api_data = (JSONObject) jsonDataObj.get("api_data");
                        KcaApiData.addUserShip(api_data);
                    }
                }

                if (url.startsWith(API_REQ_KOUSYOU_DESTROYSHIP)) {
                    String[] requestData = request.split("&");
                    for(int i=0; i<requestData.length; i++) {
                        String decodedData = URLDecoder.decode(requestData[i], "utf-8");
                        if(decodedData.startsWith("api_ship_id")) {
                            String itemlist = decodedData.replace("api_ship_id=", "");
                            KcaApiData.deleteUserShip(itemlist);
                            break;
                        }
                    }
                }

            } catch (ParseException e) {
                // TODO Auto-generated catch block
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

    ;

    class kcaNotificationHandler extends Handler {
        JSONParser jsonParser = new JSONParser();

        @Override
        public void handleMessage(Message msg) {
            String url = msg.getData().getString("url");
            String data = msg.getData().getString("data");

            if (!KcaProxyServer.is_on() || !isPortAccessed || viewNotifi == null || url.length() == 0) {
                return;
            }

            // //Log.e("KCA", "Noti Handle " + url);

            JSONObject jsonDataObj;
            try {
                jsonDataObj = (JSONObject) jsonParser.parse(data);
                if (url.startsWith(KCA_API_NOTI_EXP_LEFT)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    //int kantai_idx = ((Long) jsonDataObj.get("idx")).intValue();
                    //String str_data = (String) jsonDataObj.get("str");
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    Notifi_M.notify(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());
                }

                if (url.startsWith(KCA_API_NOTI_EXP_CANCELED)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    jsonDataObj = (JSONObject) jsonParser.parse(data);
                    int kantai_idx = ((Long) jsonDataObj.get("kantai_idx")).intValue();
                    String kantai_name = (String) jsonDataObj.get("kantai_name");
                    int mission_no = ((Long) jsonDataObj.get("mission_no")).intValue();
                    String mission_krname = (String) jsonDataObj.get("mission_krname");
                    Intent intent = new Intent(KcaService.this, MainActivity.class);

                    kcaExpeditionInfoList[kantai_idx] = "";
                    PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    Notifi = new Notification.Builder(getApplicationContext()).setSmallIcon(R.mipmap.ic_stat_notify)
                            .setContentTitle(String.format("%d번 원정(%s) 취소", mission_no, mission_krname))
                            .setContentText(String.format("<%s> 가\n%d번 원정에서 복귀했습니다.", kantai_name, mission_no))
                            .setTicker(String.format("<%s>: n%d번 원정 취소", kantai_name, mission_no))
                            .setContentIntent(pendingIntent).build();
                    //if (noti_vibr_on) {
                    if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        Notifi.defaults = Notification.DEFAULT_VIBRATE;
                    } else {
                        Notifi.vibrate = new long[]{-1};
                    }
                    Notifi.flags = Notification.FLAG_AUTO_CANCEL;
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    Notifi_M.notify(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());
                    Notifi_M.notify(getNotiIdx(NOTI_EXP, mission_no), Notifi);
                }

                if (url.startsWith(KCA_API_NOTI_EXP_FIN)) {
                    // //Log.e("KCA", "Expedition Notification Handler Called");
                    jsonDataObj = (JSONObject) jsonParser.parse(data);
                    int kantai_idx = ((Long) jsonDataObj.get("kantai_idx")).intValue();
                    String kantai_name = (String) jsonDataObj.get("kantai_name");
                    int mission_no = ((Long) jsonDataObj.get("mission_no")).intValue();
                    String mission_krname = (String) jsonDataObj.get("mission_krname");
                    Intent intent = new Intent(KcaService.this, MainActivity.class);

                    kcaExpeditionInfoList[kantai_idx] = "";
                    PendingIntent pendingIntent = PendingIntent.getActivity(KcaService.this, 0, kcIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    Notifi = new Notification.Builder(getApplicationContext()).setSmallIcon(R.mipmap.ic_stat_notify)
                            .setContentTitle(String.format("%d번 원정(%s) 도착", mission_no, mission_krname))
                            .setContentText(String.format("<%s> 가\n%d번 원정에서 복귀했습니다.", kantai_name, mission_no))
                            .setTicker(String.format("<%s>: n%d번 원정에서 복귀", kantai_name, mission_no))
                            .setContentIntent(pendingIntent).build();
                    //if (noti_vibr_on) {
                    if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        Notifi.defaults = Notification.DEFAULT_VIBRATE;
                    } else {
                        Notifi.vibrate = new long[]{-1};
                    }
                    Notifi.flags = Notification.FLAG_AUTO_CANCEL;
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    Notifi_M.notify(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());
                    Notifi_M.notify(getNotiIdx(NOTI_EXP, mission_no), Notifi);
                }

                if (url.startsWith(KCA_API_NOTI_BATTLE_INFO)) {
                    jsonDataObj = (JSONObject) jsonParser.parse(data);
                    String message = (String) jsonDataObj.get("msg");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }

                if (url.startsWith(KCA_API_NOTI_HEAVY_DMG)) {
                    heavyDamagedMode = true;
                    if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(1500);
                    }
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.heavy_damaged), Toast.LENGTH_LONG).show();
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    Notifi_M.notify(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());
                }

                if (url.startsWith(KCA_API_NOTI_GOBACKPORT)) {
                    if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(300);
                    }
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.goback_left), Toast.LENGTH_LONG).show();
                    setFrontViewNotifier(FRONT_NONE, 0, null);
                    Notifi_M.notify(getNotiIdx(NOTI_FRONT, 1), viewNotifi.build());
                }

            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), url, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "오류가 발생하였습니다.", Toast.LENGTH_LONG).show();
                onDestroy();
                throw e;
            }
        }
    }

    ;

    private Integer intv(Object o) {
        return ((Long) o).intValue();
    }

    private void processFirstDeckInfo(JSONArray data) {
        if (!isGameDataLoaded()) {
            Log.e("KCA", "processFirstDeckInfo: Game Data is Null");
            new retApiStartData().execute("", "down", "");
            return;
        } else {
            Log.e("KCA", String.format("processFirstDeckInfo: data loaded"));
        }

        if (data == null) {
            kcaFirstDeckInfo = "data is null";
            return;
        }

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

        double seekValue = KcaDeckInfo.getSeekValue(data, 0, cn);
        kcaFirstDeckInfo = String.format("색적(%s): %.2f", seekType, Math.floor(seekValue*100)/100);
        setFrontViewNotifier(FRONT_NONE, 0, null);
    }

    private void processExpeditionInfo(JSONArray data) {
        //Log.e("KCA", "processExpeditionInfo Called");
        int deck_id, mission_no;
        long arrive_time;
        String deck_name;
        for (int i = 1; i < data.size(); i++) {
            int idx = i - 1; // 1=>0, 2=>1, 3=>2 (0: Fleet #1)
            JSONObject deck = (JSONObject) data.get(i);
            deck_id = ((Long) deck.get("api_id")).intValue();
            deck_name = (String) deck.get("api_name");
            JSONArray apiMission = (JSONArray) deck.get("api_mission");
            if (((Long) apiMission.get(0)).intValue() == 1) {
                mission_no = ((Long) apiMission.get(1)).intValue();
                arrive_time = (Long) apiMission.get(2);
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

    private void cancelExpeditionInfo(JSONObject data) {
        JSONArray canceled_info = (JSONArray) data.get("api_mission");
        int canceled_mission_no = ((Long) canceled_info.get(1)).intValue();
        long arrive_time = (Long) canceled_info.get(2);
        int idx = -1;
        for (int i = 0; i < 3; i++) {
            if (kcaExpeditionRunnableList[i].mission_no == canceled_mission_no) {
                idx = i;
                break;
            }
        }

        kcaExpeditionRunnableList[idx].canceled(arrive_time);
        /*
		kcaExpeditionList[idx].interrupt();
		kcaExpeditionList[idx] = null;
		kcaExpeditionInfoList[idx] = "";
		*/
    }

    public void setFrontViewNotifier(int type, int id, String content) {

        boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean no_exp_flag = true;

        if (heavyDamagedMode) {
            viewNotifi.setContentTitle(String.format("[대파 있음!] %s 동작중", getResources().getText(R.string.app_name)));
        } else {
            viewNotifi.setContentTitle(String.format("%s 동작중", getResources().getText(R.string.app_name)));
        }

        String notifiString = "";
        String expString = "";

        //notifiString = notifiString.concat("색적: 33.45 / 제공: 180 / 고속함대\n");
        notifiString = notifiString.concat(kcaFirstDeckInfo).concat("\n");

        List<String> kcaExpStrList = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            if (KcaExpedition.left_time_str[i] != null) {
                no_exp_flag = false;
                kcaExpStrList.add(KcaExpedition.left_time_str[i]);
            }
        }

        if (no_exp_flag) {
            notifiString = notifiString.concat("원정나간 함대가 없습니다.");
        } else {
            notifiString = notifiString.concat("원정 남은 시간: ");
            if (!is_landscape) {
                notifiString = notifiString.concat("\n");
            }
            notifiString = notifiString.concat(joinStr(kcaExpStrList, " / "));
        }
        viewNotifi.setStyle(new Notification.BigTextStyle().bigText(notifiString));

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private int getNotiIdx(int type, int n) {
        return n + 1000 * type;
    }

    private class retApiStartData extends AsyncTask<String, Void, String> {

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
            JSONParser parser = new JSONParser();
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
                InputStream is = http.getInputStream();
                byte[] bytes = ByteStreams.toByteArray(is);
                String input_data = new String(bytes);

                writeCacheData(bytes);

                JSONObject jsonDataObj = (JSONObject) parser.parse(input_data);
                if (jsonDataObj.containsKey("api_data")) {
                    //Toast.makeText(getApplicationContext(), "Load Kancolle Data", Toast.LENGTH_LONG).show();
                    KcaApiData.getKcGameData((JSONObject) jsonDataObj.get("api_data"));
                    setPreferences("kca_version", kca_version);
                    processFirstDeckInfo(currentPortDeckData);
                }
            }
            return null;
        }
    }

}