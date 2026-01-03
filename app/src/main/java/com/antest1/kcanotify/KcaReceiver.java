package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.BROADCAST_ACTION;
import static com.antest1.kcanotify.KcaConstants.CONTENT_URI;
import static com.antest1.kcanotify.KcaConstants.GOTO_BROADCAST_ACTION;
import static com.antest1.kcanotify.KcaConstants.GOTO_CONTENT_URI;
import static com.antest1.kcanotify.KcaConstants.GOTO_PACKAGE_NAME;

public class KcaReceiver extends BroadcastReceiver {
    public static Handler handler = null;
    public ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void setHandler(Handler h) {
        handler = h;
    }

    Uri getContentURI(Context context, String action) {
        if (action.equals(BROADCAST_ACTION)) {
            context.grantUriPermission(GOTO_PACKAGE_NAME, CONTENT_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return CONTENT_URI;
        } else if (action.equals(GOTO_BROADCAST_ACTION)) {
            context.grantUriPermission(GOTO_PACKAGE_NAME, GOTO_CONTENT_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return GOTO_CONTENT_URI;
        } else {
            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || handler == null) return;
        if (action.equals(GOTO_BROADCAST_ACTION)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String url = bundle.getString("url", "");
                if (!url.isEmpty()) {
                    byte[] request = bundle.getString("request", "").getBytes();
                    byte[] response = bundle.getByteArray("response");
                    boolean use_devtools = bundle.getBoolean("use_devtools", true);
                    if (bundle.getBoolean("gzipped", false)) {
                        try {
                            response = KcaUtils.gzipdecompress(response);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    KcaHandler k = new KcaHandler(handler, url, request, response, use_devtools);
                    executorService.execute(k);
                }
            }
        } else {
            Uri uri = getContentURI(context, action);
            if (uri != null) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null);
                if(cursor != null) {
                    if (cursor.moveToFirst()) {
                        String url = cursor.getString(cursor.getColumnIndexOrThrow("URL"));
                        byte[] request = cursor.getString(cursor.getColumnIndexOrThrow("REQUEST")).getBytes();
                        byte[] response = cursor.getString(cursor.getColumnIndexOrThrow("RESPONSE")).getBytes();
                        KcaHandler k = new KcaHandler(handler, url, request, response, true);
                        executorService.execute(k);
                    }
                    cursor.close();
                } else {
                    Toast.makeText(context, "cursor is null", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
