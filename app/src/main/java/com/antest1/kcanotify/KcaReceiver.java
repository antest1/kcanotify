package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.BROADCAST_ACTION;
import static com.antest1.kcanotify.KcaConstants.CONTENT_URI;

public class KcaReceiver extends BroadcastReceiver {
    public static Handler handler = null;
    public ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void setHandler(Handler h) {
        handler = h;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || handler == null) return;

        if (action.equals(BROADCAST_ACTION)) {
            Cursor cursor = context.getContentResolver().query(CONTENT_URI, null, null, null, null, null);
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
