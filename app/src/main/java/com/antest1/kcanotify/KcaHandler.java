package com.antest1.kcanotify;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class KcaHandler implements Runnable {
    public Handler handler;

    public static Map<String, Boolean> flag = new HashMap<String, Boolean>();
    public static Map<String, JsonObject> data = new HashMap<String, JsonObject>();

    String url;
    byte[] requestBytes;
    byte[] responseBytes;

    public KcaHandler(Handler h, String u, byte[] b1, byte[] b2) {
        handler = h;
        url = u;
        requestBytes = b1;
        responseBytes = b2;
    }

    public void run() {
        if (handler == null) return;
        String reqData = new String(requestBytes);
        Bundle bundle = new Bundle();
        bundle.putString("url", url.replace("/kcsapi", ""));
        bundle.putString("request", reqData);
        bundle.putByteArray("data", responseBytes);
        Message msg = handler.obtainMessage();
        msg.setData(bundle);

        handler.sendMessage(msg);
        Log.e("KCA", "Data Processed: " + url);
    }
}
