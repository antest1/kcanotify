package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.BROADCAST_ACTION;
import static com.antest1.kcanotify.KcaConstants.CONTENT_URI;
import static com.antest1.kcanotify.KcaConstants.GOTO_BROADCAST_ACTION;
import static com.antest1.kcanotify.KcaConstants.GOTO_CONTENT_URI;

public class KcaReceiver extends BroadcastReceiver {
    public static Handler handler = null;
    public ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void setHandler(Handler h) {
        handler = h;
    }

    Uri getContentURI(String action) {
        if (action.equals(BROADCAST_ACTION)) {
            return CONTENT_URI;
        } else if (action.equals(GOTO_BROADCAST_ACTION)) {
            return GOTO_CONTENT_URI;
        } else {
            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || handler == null) return;
        Uri uri = getContentURI(action);
        if (uri != null) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null);
            if(cursor != null) {
                if (cursor.moveToFirst()) {
                    String url = cursor.getString(cursor.getColumnIndex("URL"));
                    byte[] request = cursor.getString(cursor.getColumnIndex("REQUEST")).getBytes();
                    byte[] response = cursor.getString(cursor.getColumnIndex("RESPONSE")).getBytes();
                    KcaHandler k = new KcaHandler(handler, url, request, response);
                    executorService.execute(k);
                }
                cursor.close();
            } else {
                Toast.makeText(context, "cursor is null", Toast.LENGTH_LONG).show();
            }
        }
    }
}
