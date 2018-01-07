package com.antest1.kcanotify;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;

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

    public static final String USER_AGENT = KcaUtils.format("Kcanotify/%s ", BuildConfig.VERSION_NAME);

    public static Handler sHandler;
    private static Gson gson = new Gson();

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static void sendEquipDevData(String items, int secretary, int itemId, int teitokuLv, boolean successful) {
        if (teitokuLv < 1 || secretary < 0 || itemId < 0) return;

        JsonObject response = new JsonObject();
        response.add("items", gson.fromJson(items, JsonArray.class));
        response.addProperty("secretary", secretary);
        response.addProperty("itemId", itemId);
        response.addProperty("teitokuLv", teitokuLv);
        response.addProperty("successful", successful);
        response.addProperty("origin", USER_AGENT);
        new poiDbRequest().execute(REQ_EQUIP_DEV, KcaUtils.format("data=%s", response.toString()));
    }

    public static void sendShipDevData(String items, int kdockId, int secretary, int shipId, int highpeed, int teitokuLv, int largeFlag) {
        if (teitokuLv < 1 || kdockId < 0 || secretary < 0 || shipId < 0 || highpeed < 0 || largeFlag < 0) return;

        JsonObject response = new JsonObject();
        response.add("items", gson.fromJson(items, JsonArray.class));
        response.addProperty("kdockId", kdockId);
        response.addProperty("secretary", secretary);
        response.addProperty("shipId", shipId);
        response.addProperty("highspeed", highpeed);
        response.addProperty("teitokuLv", teitokuLv);
        response.addProperty("largeFlag", largeFlag);
        response.addProperty("origin", USER_AGENT);
        new poiDbRequest().execute(REQ_SHIP_DEV, KcaUtils.format("data=%s", response.toString()));
    }

    public static void sendShipDropData(int shipId, int mapId, String quest, int cellId, String enemy, String rank, boolean isBoss, int teitokuLv, int mapLv, JsonObject enemyInfo) {
        if (teitokuLv < 1 || mapId < 0 || cellId < 0 || mapLv < 0) return;

        JsonObject response = new JsonObject();
        response.addProperty("shipId", shipId);
        response.addProperty("mapId", mapId);
        response.addProperty("quest", quest);
        response.addProperty("cellId", cellId);
        response.addProperty("enemy", enemy);
        response.addProperty("rank", rank);
        response.addProperty("isBoss", isBoss);
        response.addProperty("teitokuLv", teitokuLv);
        response.addProperty("mapLv", mapLv);
        response.add("enemyShips1", enemyInfo.has("ships") ? enemyInfo.getAsJsonArray("ships") : new JsonArray());
        response.add("enemyShips2", enemyInfo.has("ships2") ? enemyInfo.getAsJsonArray("ships2") : new JsonArray());
        response.addProperty("enemyFormation", enemyInfo.get("formation").getAsInt());
        response.addProperty("origin", USER_AGENT);
        new poiDbRequest().execute(REQ_SHIP_DROP, KcaUtils.format("data=%s", response.toString()));
    }

    public static class poiDbRequest extends AsyncTask<String, Void, String> {
        final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");
        OkHttpClient client = new OkHttpClient.Builder().build();

        @Override
        protected String doInBackground(String... params) {
            String content = "";
            try {
                Log.e("KCA", KcaUtils.format("Poi Request %s %s", params[0], params[1]));
                content = Request(params[0], params[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (content.equals(ERROR_CODE)) {
                Log.e("KCA", "KcaRequest Error: "+params[0]);
            } else {
                Log.e("KCA", "KcaRequest Responsed "+String.valueOf(content.length()));
            }
            return content;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s.equals(FAILED_CODE)) {
                if (sHandler != null) {
                    JsonObject dmpData = new JsonObject();
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_POIDB_FAILED);
                    bundle.putString("data", gson.toJson(dmpData));
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
            }
        }

        public String Request(String uri, String data) throws Exception {
            String url = "http://poi.0u0.moe".concat(uri);

            RequestBody body;
            try {
                body = RequestBody.create(FORM_DATA, data);
                Request.Builder builder = new Request.Builder().url(url).post(body);
                builder.addHeader("User-Agent", USER_AGENT);
                builder.addHeader("Referer", "app:/KCA/");
                builder.addHeader("Content-Type", "application/x-www-form-urlencoded");
                Request request = builder.build();

                Response response = client.newCall(request).execute();

                int code = response.code();
                if (code == HttpStatus.SC_OK) {
                    return SUCCESSED_CODE;
                } else {
                    return FAILED_CODE;
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "IOException_POIDB";
            }
        }
    }
}
