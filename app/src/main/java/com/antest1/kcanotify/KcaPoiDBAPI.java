package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.httpclient.HttpStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.antest1.kcanotify.KcaConstants.KCA_API_POIDB_FAILED;

public class KcaPoiDBAPI {
    public static final String SUCCESSED_CODE = "successed";
    public static final String FAILED_CODE = "failed";
    public static final String TIMEOUT_CODE = "timeout";
    public static final String ERROR_CODE = "erroroccured";

    public static final String REQ_EQUIP_DEV = "/api/report/v2/create_item";
    public static final String REQ_SHIP_DEV = "/api/report/v2/create_ship";
    public static final String REQ_SHIP_DROP = "/api/report/v2/drop_ship";
    //public static final String REQ_EQUIP_REMODEL = "/api/report/v2/remodel_recipe";

    public static final String USER_AGENT = KcaUtils.format("Kcanotify/%s", BuildConfig.VERSION_NAME);

    public Handler sHandler;

    public KcaPoiDBAPI(Handler h) {
        sHandler = h;
    }

    public void sendEquipDevData(String items, int secretary, int itemId, int teitokuLv, boolean successful) {
        if (teitokuLv < 1 || secretary < 0 || itemId < 0) return;

        JsonObject payload = new JsonObject();
        payload.add("items", new Gson().fromJson(items, JsonArray.class));
        payload.addProperty("secretary", secretary);
        payload.addProperty("itemId", itemId);
        payload.addProperty("teitokuLv", teitokuLv);
        payload.addProperty("successful", successful);
        payload.addProperty("origin", USER_AGENT);

        JsonObject body = new JsonObject();
        body.add("data", payload);
        requestApi(REQ_EQUIP_DEV, body.toString());
    }

    public void sendShipDevData(String items, int kdockId, int secretary, int shipId, int highpeed, int teitokuLv, int largeFlag) {
        if (teitokuLv < 1 || kdockId < 0 || secretary < 0 || shipId < 0 || highpeed < 0 || largeFlag < 0) return;

        JsonObject payload = new JsonObject();
        payload.add("items", new Gson().fromJson(items, JsonArray.class));
        payload.addProperty("kdockId", kdockId);
        payload.addProperty("secretary", secretary);
        payload.addProperty("shipId", shipId);
        payload.addProperty("highspeed", highpeed);
        payload.addProperty("teitokuLv", teitokuLv);
        payload.addProperty("largeFlag", largeFlag);
        payload.addProperty("origin", USER_AGENT);

        JsonObject body = new JsonObject();
        body.add("data", payload);
        requestApi(REQ_SHIP_DEV, body.toString());
    }

    public void sendShipDropData(int shipId, int mapId, String quest, int cellId, String enemy, String rank, boolean isBoss, int teitokuLv, int mapLv, JsonObject enemyInfo) {
        if (teitokuLv < 1 || mapId < 0 || cellId < 0 || mapLv < 0) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("shipId", shipId);
        payload.addProperty("mapId", mapId);
        payload.addProperty("quest", quest);
        payload.addProperty("cellId", cellId);
        payload.addProperty("enemy", enemy);
        payload.addProperty("rank", rank);
        payload.addProperty("isBoss", isBoss);
        payload.addProperty("teitokuLv", teitokuLv);
        payload.addProperty("mapLv", mapLv);
        payload.add("enemyShips1", enemyInfo.has("ships") ? enemyInfo.getAsJsonArray("ships") : new JsonArray());
        payload.add("enemyShips2", enemyInfo.has("ships2") ? enemyInfo.getAsJsonArray("ships2") : new JsonArray());
        payload.addProperty("enemyFormation", enemyInfo.get("formation").getAsInt());
        payload.addProperty("origin", USER_AGENT);

        JsonObject body = new JsonObject();
        body.add("data", payload);
        requestApi(REQ_SHIP_DROP, body.toString());
    }

    private void requestApi(String endpoint, String body) {
        Handler handler = new Handler(Looper.getMainLooper());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String content = "";
            try {
                Log.e("KCA", KcaUtils.format("Poi Request %s %s", endpoint, body));
                content = request(endpoint, body);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (content.equals(ERROR_CODE)) {
                Log.e("KCA", "KcaRequest Error: "+endpoint);
            } else {
                Log.e("KCA", "KcaRequest Responsed " + content);
            }

            final String contentFinal = content;
            handler.post(() -> {
                if (contentFinal.equals(FAILED_CODE)) {
                    if (sHandler != null) {
                        JsonObject dmpData = new JsonObject();
                        Bundle bundle = new Bundle();
                        bundle.putString("url", KCA_API_POIDB_FAILED);
                        bundle.putString("data", new Gson().toJson(dmpData));
                        Message sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }
                }
            });
        });
    }

    public String request(String uri, String data) {
        final MediaType MEDIA_TYPE = MediaType.parse("application/json");
        OkHttpClient client = new OkHttpClient.Builder().build();

        String url = "https://api.poi.moe".concat(uri);
        RequestBody body;
        try {
            body = RequestBody.create(data, MEDIA_TYPE);
            Request.Builder builder = new Request.Builder().url(url).post(body);
            builder.addHeader("User-Agent", USER_AGENT);
            builder.addHeader("Referer", "app:/KCA/");
            builder.addHeader("X-Reporter", USER_AGENT);
            builder.addHeader("Content-Type", "application/json");
            Request request = builder.build();
            Response response = client.newCall(request).execute();

            int code = response.code();
            if (code == HttpStatus.SC_OK) {
                return SUCCESSED_CODE;
            } else {
                return FAILED_CODE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception_POIDB";
        }
    }
}
