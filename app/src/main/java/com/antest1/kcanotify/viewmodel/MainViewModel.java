package com.antest1.kcanotify.viewmodel;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.preference.PreferenceManager;

import com.antest1.kcanotify.KcaService;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.KcaVpnService;
import com.antest1.kcanotify.LocaleUtils;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;

import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_KC_PACKAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_SNIFFER_MODE;
import static com.antest1.kcanotify.KcaConstants.PREF_SVC_ENABLED;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaConstants.SNIFFER_ACTIVE;
import static com.antest1.kcanotify.KcaConstants.VPN_STOP_REASON;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

/**
 * Status, permission and service-control logic for {@link com.antest1.kcanotify.MainActivity}.
 * The activity keeps ownership of the views, dialogs and launchers.
 */
public class MainViewModel extends AndroidViewModel {
    private final SharedPreferences prefs;

    public MainViewModel(@NonNull Application application) {
        super(application);
        prefs = PreferenceManager.getDefaultSharedPreferences(application);
    }

    /**
     * Mirrors the persisted state from the running services on activity creation.
     */
    public void refreshStatusFromServices() {
        prefs.edit().putBoolean(PREF_SVC_ENABLED, KcaService.getServiceStatus()).apply();
        prefs.edit().putBoolean(PREF_VPN_ENABLED, KcaVpnService.checkOn()).apply();
    }

    public int getSnifferMode() {
        if (KC_PACKAGE_NAME.equals(getStringPreferences(getApplication(), PREF_KC_PACKAGE))) {
            return SNIFFER_ACTIVE;
        } else {
            return Integer.parseInt(getStringPreferences(getApplication(), PREF_SNIFFER_MODE));
        }
    }

    public boolean isServiceEnabled() {
        return prefs.getBoolean(PREF_SVC_ENABLED, false);
    }

    public void setServiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_SVC_ENABLED, enabled).apply();
    }

    public boolean isVpnEnabled() {
        return prefs.getBoolean(PREF_VPN_ENABLED, false);
    }

    public void setVpnEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_VPN_ENABLED, enabled).apply();
    }

    /**
     * Synchronous variant used while resuming the VPN state (the original code intentionally
     * committed before the toggles were refreshed).
     */
    public void setVpnEnabledSync(boolean enabled) {
        prefs.edit().putBoolean(PREF_VPN_ENABLED, enabled).commit();
    }

    public boolean isServiceChecked() {
        return prefs.getBoolean(PREF_SVC_ENABLED, false);
    }

    /**
     * The service toggle text/enabled/checked state.
     */
    public VpnButtonState getVpnButtonState() {
        if (getSnifferMode() == SNIFFER_ACTIVE) {
            boolean isChecked = prefs.getBoolean(PREF_VPN_ENABLED, false);
            return new VpnButtonState(isChecked ? com.antest1.kcanotify.R.string.ma_vpn_toggleon
                    : com.antest1.kcanotify.R.string.ma_vpn_toggleoff, true, isChecked);
        } else {
            return new VpnButtonState(0, false, false);
        }
    }

    public boolean checkOverlayPermission() {
        return Settings.canDrawOverlays(getApplication());
    }

    public boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getApplication().getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public void startKcaService(Context context) {
        Intent serviceIntent = new Intent(context, KcaService.class);
        serviceIntent.setAction(KcaService.KCASERVICE_START);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void stopKcaService(Context context) {
        Intent serviceIntent = new Intent(context, KcaService.class);
        serviceIntent.setAction(KcaService.KCASERVICE_STOP);
        context.startService(serviceIntent);
        setServiceEnabled(false);
    }

    public void stopVpnService(Context context) {
        KcaVpnService.stop(VPN_STOP_REASON, context);
        setVpnEnabled(false);
    }

    /**
     * Reads the localized main-page HTML from assets.
     */
    public String loadMainHtml() {
        try {
            InputStream ais = getApplication().getAssets()
                    .open("main-".concat(LocaleUtils.getResourceLocaleCode()).concat(".html"));
            byte[] bytes = ByteStreams.toByteArray(ais);
            return new String(bytes);
        } catch (IOException e) {
            return "Error loading html file.";
        }
    }

    public static class VpnButtonState {
        public final int textResId;
        public final boolean enabled;
        public final boolean checked;

        public VpnButtonState(int textResId, boolean enabled, boolean checked) {
            this.textResId = textResId;
            this.enabled = enabled;
            this.checked = checked;
        }
    }
}
