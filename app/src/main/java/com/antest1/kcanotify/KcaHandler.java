package com.antest1.kcanotify;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class KcaHandler implements Runnable {
    private static final Gson gson = new GsonBuilder().setLenient().create();

    public Handler handler;

    public static Map<String, Boolean> flag = new HashMap<String, Boolean>();
    public static Map<String, JsonObject> data = new HashMap<String, JsonObject>();

    String url;
    byte[] requestBytes;
    byte[] responseBytes;
    boolean useDevTools;
    boolean gzipped;

    public KcaHandler(Handler h, String u, byte[] b1, byte[] b2, boolean ud) {
        this(h, u, b1, b2, ud, false);
    }

    public KcaHandler(Handler h, String u, byte[] b1, byte[] b2, boolean ud, boolean gz) {
        handler = h;
        url = u;
        requestBytes = b1;
        responseBytes = b2;
        useDevTools = ud;
        gzipped = gz;
    }

    // where the JSON of a response starts: game API responses begin with "svdata="
    static int getJsonOffset(byte[] raw) {
        String init = new String(Arrays.copyOfRange(raw, 0, 7));
        return init.contains("svdata=") ? "svdata=".length() : 0;
    }

    // parses a response off the main thread, the way the service did: lenient, without the "svdata=" prefix.
    // an empty response or one that is not an object gives an empty object
    static JsonObject parseResponse(byte[] raw) {
        if (raw.length == 0) return new JsonObject();
        int offset = getJsonOffset(raw);
        Reader data = new InputStreamReader(new ByteArrayInputStream(raw, offset, raw.length - offset));
        JsonElement jsonData = gson.fromJson(data, JsonElement.class);
        return jsonData.isJsonObject() ? jsonData.getAsJsonObject() : new JsonObject();
    }

    public void run() {
        if (handler == null) return;
        Message msg = handler.obtainMessage();
        if (gzipped && responseBytes != null) {
            try {
                responseBytes = KcaUtils.gzipdecompress(responseBytes);
            } catch (IOException e) {
                // the service records the error with the url, without the compressed bytes
                responseBytes = new byte[0];
                msg.obj = e;
            }
        }
        if (msg.obj == null && responseBytes != null) {
            // the service gets the parsed object, or the error to record
            try {
                msg.obj = parseResponse(responseBytes);
            } catch (Exception e) {
                msg.obj = e;
            }
        }

        String reqData = new String(requestBytes);
        Bundle bundle = new Bundle();
        bundle.putString("url", url.replace("/kcsapi", ""));
        bundle.putString("request", reqData);
        bundle.putByteArray("data", responseBytes);
        bundle.putBoolean("dev_flag", useDevTools);
        msg.setData(bundle);

        handler.sendMessage(msg);
        Log.e("KCA", "Data Processed: " + url);
    }
}
