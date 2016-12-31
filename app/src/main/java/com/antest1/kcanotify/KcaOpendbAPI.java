package com.antest1.kcanotify;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import static com.antest1.kcanotify.KcaConstants.KCA_API_OPENDB_FAILED;

public class KcaOpendbAPI {
    public static final String SUCCESSED_CODE = "successed";
    public static final String FAILED_CODE = "failed";
    public static final String TIMEOUT_CODE = "timeout";
    public static final String ERROR_CODE = "erroroccured";

    public static final String REQ_EQUIP_DEV = "/opendb/report/equip_dev.php";
    public static final String REQ_SHIP_DEV = "/opendb/report/ship_dev.php";
    public static final String REQ_SHIP_DROP = "/opendb/report/ship_drop.php";
    public static final String REQ_EQUIP_REMODEL = "/opendb/report/equip_remodel.php";

    public static Handler sHandler;
    private static Gson gson = new Gson();

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static void sendEquipDevData(int flagship, int fuel, int ammo, int steel, int bauxite, int result) {

        String param = String.format("apiver=2&flagship=%d&fuel=%d&ammo=%d&steel=%d&bauxite=%d&result=%d",
                flagship, fuel, ammo, steel, bauxite, result);
        new opendbRequest().execute(REQ_EQUIP_DEV, param);
    }

    public static void sendShipDevData(int flagship, int fuel, int ammo, int steel, int bauxite, int material, int result) {
        String param = String.format("apiver=2&flagship=%d&fuel=%d&ammo=%d&steel=%d&bauxite=%d&material=%d&result=%d",
                flagship, fuel, ammo, steel, bauxite, material, result);
        new opendbRequest().execute(REQ_SHIP_DEV, param);
    }

    public static void sendShipDropData(int world, int map, int node, String rank, int maprank, int result) {
        String param = String.format("apiver=3&world=%d&map=%d&node=%d&rank=%s&maprank=%d&result=%d",
                world, map, node, rank, maprank, result);
        new opendbRequest().execute(REQ_SHIP_DROP, param);
    }

    public static void sendRemodelData(int flagship, int assistant, int item, int level, int result) {
        String param = String.format("apiver=2&flagship=%d&assistant=%d&item=%d&level=%d&result=%d",
                flagship, assistant, item, level, result);
        new opendbRequest().execute(REQ_EQUIP_REMODEL, param);
    }

    public static class opendbRequest extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String content = "";
            try {
                Log.e("KCA", String.format("Opendb Request %s %s", params[0], params[1]));
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
            URL url =  new URL("http://swaytwig.com".concat(uri));
            HttpURLConnection http = null;
            try {
                http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod(HttpMethod.POST.name());
                http.setConnectTimeout(15000);
                http.setReadTimeout(2500);
                http.setDoInput(true);

                http.setRequestProperty("User-Agent", String.format("Kca/%s ", BuildConfig.VERSION_NAME));
                http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                http.setDoOutput(true);
                OutputStream outStream = http.getOutputStream();
                outStream.write(data.getBytes());
                outStream.flush();
                outStream.close();

                int code = http.getResponseCode();
                if (code == HttpResponseStatus.OK.code()) {
                    JsonObject responseData = new JsonObject();
                    InputStream is = http.getInputStream();
                    byte[] bytes = ByteStreams.toByteArray(is);
                    String bytesData = new String(bytes);
                    if(bytesData.contains("Invalid")) {
                        return FAILED_CODE;
                    } else {
                        return SUCCESSED_CODE;
                    }
                } else {
                    return FAILED_CODE;
                }
            } catch(java.net.SocketTimeoutException e) {
                return TIMEOUT_CODE;
            } catch (Exception e) {
                e.printStackTrace();
                return ERROR_CODE;
            } finally {
                if (http != null) http.disconnect();
            }
        }
    }

    private static String makeParameter(Map<String, String> map) {
        List<String> paramList = new ArrayList<String>();
        for (Map.Entry<String, String> entry: map.entrySet()) {
            try {
                paramList.add(URLEncoder.encode(entry.getKey().concat("=").concat(entry.getValue()), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                paramList.add(entry.getKey().concat("=").concat(entry.getValue()));
            }
        }
        return joinStr(paramList, "&");
    }

    private static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }

}
