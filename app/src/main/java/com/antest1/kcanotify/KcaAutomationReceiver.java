package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import static com.antest1.kcanotify.KcaConstants.PREF_SNIFFER_MODE;
import static com.antest1.kcanotify.KcaConstants.PREF_USE_TLS_DECRYPTION;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaConstants.SNIFFER_PASSIVE;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

import com.antest1.kcanotify.remote_capture.CaptureHelper;
import com.antest1.kcanotify.remote_capture.CaptureService;
import com.antest1.kcanotify.remote_capture.MitmAddon;
import com.antest1.kcanotify.remote_capture.model.CaptureSettings;

public class KcaAutomationReceiver extends BroadcastReceiver {
    public static final String SNIFFER_ON_ACTION = "sniffer_on";
    public static final String SNIFFER_OFF_ACTION = "sniffer_off";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int sniffer_mode = Integer.parseInt(getStringPreferences(context, PREF_SNIFFER_MODE));
            if (sniffer_mode == SNIFFER_PASSIVE) return; // do nothing when passive mode
            switch (action) {
                case SNIFFER_ON_ACTION:
                    if (startCapture(context))
                        prefs.edit().putBoolean(PREF_VPN_ENABLED, true).apply();
                    break;
                case SNIFFER_OFF_ACTION:
                    stopCapture();
                    prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                    break;
                default:
                    break;
            }
        }
    }

    public boolean startCapture(Context context) {
        if (CaptureService.isServiceActive()) return true;

        if (getBooleanPreferences(context, PREF_USE_TLS_DECRYPTION)) {
            MitmAddon.setCAInstallationSkipped(context, false);
            if (MitmAddon.needsSetup(context)) {
                Toast.makeText(context, "mitm_needs_setup", Toast.LENGTH_LONG).show();
                return false;
            }

            if (!MitmAddon.getNewVersionAvailable(context).isEmpty()) {
                Toast.makeText(context, context.getString(R.string.mitm_addon_update_available), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        doStartCaptureService(context);
        return true;
    }

    public void stopCapture() {
        if (!CaptureService.isServiceActive()) return;
        CaptureService.stopService();
    }

    private void doStartCaptureService(Context context) {
        // appStateStarting();
        CaptureHelper mCapHelper = new CaptureHelper(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        CaptureSettings settings = new CaptureSettings(context, prefs);
        mCapHelper.startCapture(settings);
    }
}
