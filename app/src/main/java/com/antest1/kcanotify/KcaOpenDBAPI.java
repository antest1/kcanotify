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

import static com.antest1.kcanotify.KcaConstants.KCA_API_OPENDB_FAILED;

public class KcaOpenDBAPI {
    public static final String SUCCESSED_CODE = "successed";
    public static final String FAILED_CODE = "failed";
    public static final String TIMEOUT_CODE = "timeout";
    public static final String ERROR_CODE = "erroroccured";

    public static final String REQ_EQUIP_DEV = "/report/equip_build.php";
    public static final String REQ_SHIP_DEV = "/report/ship_build.php";
    public static final String REQ_SHIP_DROP = "/report/ship_drop.php";
    public static final String REQ_EQUIP_REMODEL = "/report/equip_remodel.php";

    public static final String USER_AGENT = KcaUtils.format("Kcanotify/%s ", BuildConfig.VERSION_NAME);

    public static Handler sHandler;
    private static Gson gson = new Gson();

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static void sendEquipDevData(int flagship, int fuel, int ammo, int steel, int bauxite, int result) {

        String param = KcaUtils.format("apiver=2&flagship=%d&fuel=%d&ammo=%d&steel=%d&bauxite=%d&result=%d",
                flagship, fuel, ammo, steel, bauxite, result);
        new opendbRequest().execute(REQ_EQUIP_DEV, param);
    }

    public static void sendShipDevData(int flagship, int fuel, int ammo, int steel, int bauxite, int material, int result) {
        String param = KcaUtils.format("apiver=2&flagship=%d&fuel=%d&ammo=%d&steel=%d&bauxite=%d&material=%d&result=%d",
                flagship, fuel, ammo, steel, bauxite, material, result);
        new opendbRequest().execute(REQ_SHIP_DEV, param);
    }

    public static void sendShipDropData(int world, int map, int node, String rank, int maprank, JsonObject enemy, int inventory, int result) {
        if (enemy.has("ships")) {
            JsonArray ship_ke = enemy.getAsJsonArray("ships");
            JsonArray enemyinfo_ship = new JsonArray();
            for (int i = 0; i < 6; i++) {
                if (i < ship_ke.size()) {
                    int ship_value = ship_ke.get(i).getAsInt();
                    enemyinfo_ship.add(ship_value);
                } else {
                    enemyinfo_ship.add(0);
                }
            }
            enemy.add("ships", enemyinfo_ship);
        }
        if (enemy.has("ships2")) {
            JsonArray ship_ke_combined = enemy.getAsJsonArray("ships2");
            JsonArray enemyinfo_ship_cb = new JsonArray();
            for (int i = 0; i < 6; i++) {
                if (i < ship_ke_combined.size()) {
                    int ship_value = ship_ke_combined.get(i).getAsInt();
                    enemyinfo_ship_cb.add(ship_value);
                } else {
                    enemyinfo_ship_cb.add(0);
                }
            }
            enemy.add("ships2", enemyinfo_ship_cb);
        }

        String param = KcaUtils.format("apiver=5&world=%d&map=%d&node=%d&rank=%s&maprank=%d&enemy=%s&inventory=%d&result=%d",
                world, map, node, rank, maprank, enemy, inventory, result);
        new opendbRequest().execute(REQ_SHIP_DROP, param);
    }

    public static void sendRemodelData(int flagship, int assistant, int item, int level, int result) {
        String param = KcaUtils.format("apiver=2&flagship=%d&assistant=%d&item=%d&level=%d&result=%d",
                flagship, assistant, item, level, result);
        new opendbRequest().execute(REQ_EQUIP_REMODEL, param);
    }

    public static class opendbRequest extends AsyncTask<String, Void, String> {
        final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");
        OkHttpClient client = new OkHttpClient.Builder().build();

        @Override
        protected String doInBackground(String... params) {
            String content = "";
            try {
                Log.e("KCA", KcaUtils.format("Opendb Request %s %s", params[0], params[1]));
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
                    bundle.putString("url", KCA_API_OPENDB_FAILED);
                    bundle.putString("data", gson.toJson(dmpData));
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
            }
        }

        public String Request(String uri, String data) throws Exception {
            String url = "http://opendb.swaytwig.com".concat(uri);

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
                    if(response.body().string().contains("Invalid")) {
                        return FAILED_CODE;
                    } else {
                        return SUCCESSED_CODE;
                    }
                } else {
                    return FAILED_CODE;
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "IOException_OPENDB";
            }
        }
    }
}
