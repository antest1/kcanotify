package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.VpnService;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.BROADCAST_ACTION;
import static com.antest1.kcanotify.KcaConstants.CONTENT_URI;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaConstants.VPN_STOP_REASON;

public class KcaAutomationReceiver extends BroadcastReceiver {
    public static final String SNIFFER_ON_ACTION = "sniffer_on";
    public static final String SNIFFER_OFF_ACTION = "sniffer_off";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            switch (action) {
                case SNIFFER_ON_ACTION:
                    try {
                        VpnService.prepare(context);
                        prefs.edit().putBoolean(PREF_VPN_ENABLED, true).apply();
                        KcaVpnService.start("prepared", context);
                    } catch (Throwable ex) {
                        // Prepare failed
                        Log.e("KCA", ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                    break;

                case SNIFFER_OFF_ACTION:
                    KcaVpnService.stop(VPN_STOP_REASON, context);
                    prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                    break;

                default:
                    break;
            }
        }
    }
}
