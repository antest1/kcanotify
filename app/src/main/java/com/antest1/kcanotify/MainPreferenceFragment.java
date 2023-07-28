package com.antest1.kcanotify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

import retrofit2.Call;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.KcaViewButtonService.FAIRY_FORECHECK_OFF;
import static com.antest1.kcanotify.KcaViewButtonService.FAIRY_FORECHECK_ON;
import static com.antest1.kcanotify.SettingActivity.REQUEST_ALERT_RINGTONE;
import static com.antest1.kcanotify.SettingActivity.REQUEST_BATOPTIM_PERMISSION;
import static com.antest1.kcanotify.SettingActivity.REQUEST_OVERLAY_PERMISSION;
import static com.antest1.kcanotify.SettingActivity.REQUEST_USAGESTAT_PERMISSION;

public class MainPreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    KcaDBHelper dbHelper;
    private boolean isActivitySet = false;
    public KcaDownloader downloader;
    public static Handler sHandler;
    private SharedPreferences sharedPref = null;
    static Gson gson = new Gson();
    private Callback mCallback;

    ActivityResultLauncher<Intent> startActivityResult;
    private int currentActivity = 0;

    public interface Callback {
        void onNestedPreferenceSelected(int key);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbHelper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        downloader = KcaUtils.getInfoDownloader(context);

        startActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    switch (currentActivity) {
                        case REQUEST_OVERLAY_PERMISSION:
                            showOverlayPermissionResult();
                            break;
                        case REQUEST_BATOPTIM_PERMISSION:
                            showBatteryOptimizationPermissionResult();
                            break;
                        case REQUEST_USAGESTAT_PERMISSION:
                            showUsageStatsPermissionResult();
                            break;
                        case REQUEST_ALERT_RINGTONE:
                            callbackAlertRingtoneResult(result);
                            break;
                    }
                    currentActivity = 0;
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isActivitySet = true;
        Activity activity = getActivity();
        if (activity instanceof Callback) mCallback = (Callback) activity;

        Map<String, ?> allEntries = sharedPref.getAll();
        for (String key : allEntries.keySet()) {
            Preference preference = findPreference(key);
            if (preference == null) continue;
            if (preference instanceof ListPreference) {
                if (!PREF_KCA_LANGUAGE.equals(preference.getKey())) {
                    ListPreference etp = (ListPreference) preference;
                    preference.setSummary(etp.getEntry());
                }
            } else if (preference instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) preference;
                preference.setSummary(getEditTextSummary(key, etp.getText()));
            } else if (PREF_KCA_NOTI_RINGTONE.equals(preference.getKey())) {
                String uri = sharedPref.getString(PREF_KCA_NOTI_RINGTONE, "");
                Uri ringtoneUri = Uri.parse(uri);
                String ringtoneTitle = getStringWithLocale(R.string.settings_string_silent);
                if (uri.length() > 0) {
                    ringtoneTitle = getRingtoneTitle(ringtoneUri);
                }
                preference.setSummary(ringtoneTitle);
            }
            preference.setOnPreferenceChangeListener(this);
        }

        String kca_package = sharedPref.getString(PREF_KC_PACKAGE, KC_PACKAGE_NAME);
        setSnifferModeSettingEnabled(GOTO_PACKAGE_NAME.equals(kca_package)); // disable for KC_PACKAGE_NAME

        int sniffer_mode = Integer.parseInt(sharedPref.getString(PREF_SNIFFER_MODE, String.valueOf(SNIFFER_ACTIVE)));
        setActiveSnifferSettingEnabled(KC_PACKAGE_NAME.equals(kca_package) || sniffer_mode == SNIFFER_ACTIVE);

        setSettingDisabledWhenServiceRunning();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName("pref");
        setPreferencesFromResource(R.xml.pref_settings, rootKey);
        sharedPref = getPreferenceManager().getSharedPreferences();
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Log.e("KCA", "onPreferenceTreeClick " + preference.getKey());
        String key = preference.getKey();
        if (!isActivitySet) return false;

        if (PREF_OVERLAY_SETTING.equals(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showObtainingPermissionOverlayWindow();
            } else {
                showToast(getActivity(), getStringWithLocale(R.string.sa_under_m), Toast.LENGTH_SHORT);
            }
        }

        if (PREF_BATOPTIM_SETTING.equals(key)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showObtainingPermissionBatteryOptimization();
            } else {
                showToast(getActivity(), getStringWithLocale(R.string.sa_under_m), Toast.LENGTH_SHORT);
            }
        }

        if (PREF_SCREEN_ADV_NETWORK.equals(key)) {
            mCallback.onNestedPreferenceSelected(NestedPreferenceFragment.FRAGMENT_ADV_NETWORK);
            return false;
        }

        if (PREF_KCA_NOTI_RINGTONE.equals(key)) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, DEFAULT_NOTIFICATION_URI);
            String existingValue = sharedPref.getString(key, null);
            if (existingValue != null) {
                if (existingValue.length() == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
            }
            currentActivity = REQUEST_ALERT_RINGTONE;
            startActivityResult.launch(intent);
        }

        if (PREF_CHECK_UPDATE.equals(key)) {
            checkRecentVersion();
        }

        return super.onPreferenceTreeClick(preference);
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getActivity().getApplicationContext(), getActivity().getBaseContext(), id);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showObtainingPermissionOverlayWindow() {
        String package_name = getContext().getPackageName();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + package_name));

        if (intent.resolveActivity(getContext().getPackageManager()) == null) {
            intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + package_name));
            showToast(getActivity(), getStringWithLocale(R.string.sa_overlay_appearontop), Toast.LENGTH_LONG);
        }
        currentActivity = REQUEST_OVERLAY_PERMISSION;
        startActivityResult.launch(intent);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showObtainingPermissionBatteryOptimization() {
        String package_name = getContext().getPackageName();
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);

        if (intent.resolveActivity(getContext().getPackageManager()) == null) {
            intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + package_name));
            showToast(getActivity(), getStringWithLocale(R.string.sa_batteryoptim_appearontop), Toast.LENGTH_LONG);
        }
        currentActivity = REQUEST_BATOPTIM_PERMISSION;
        startActivityResult.launch(intent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean hasUsageStatPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager)
                context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void showObtainingUsageStatPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        currentActivity = REQUEST_USAGESTAT_PERMISSION;
        startActivityResult.launch(intent);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showOverlayPermissionResult() {
        if (checkActivityValid()) {
            int delay = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 0 : 1000;
            new Handler().postDelayed(() -> {
                Context context = getContext();
                if (context != null) {
                    if (Settings.canDrawOverlays(getContext())) {
                        showToast(getActivity(), getStringWithLocale(R.string.sa_overlay_ok), Toast.LENGTH_SHORT);
                    } else {
                        showToast(getActivity(), getStringWithLocale(R.string.sa_overlay_no), Toast.LENGTH_SHORT);
                    }
                }
            }, delay);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showBatteryOptimizationPermissionResult() {
        if (checkActivityValid()) {
            int delay = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 0 : 1000;
            new Handler().postDelayed(() -> {
                Context context = getContext();
                if (context != null) {
                    PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (manager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                        showToast(getActivity(), getStringWithLocale(R.string.sa_batteryoptim_ok), Toast.LENGTH_SHORT);
                    } else {
                        showToast(getActivity(), getStringWithLocale(R.string.sa_batteryoptim_no), Toast.LENGTH_SHORT);
                    }
                }
            }, delay);
        }
    }

    private void showUsageStatsPermissionResult() {
        if (checkActivityValid()) {
            if(hasUsageStatPermission(getContext())) {
                showToast(getActivity(), getStringWithLocale(R.string.sa_usagestat_ok), Toast.LENGTH_SHORT);
            } else {
                showToast(getActivity(), getStringWithLocale(R.string.sa_usagestat_no), Toast.LENGTH_SHORT);
            }
        }
    }

    private void callbackAlertRingtoneResult(ActivityResult result) {
        Preference pref = findPreference(PREF_KCA_NOTI_RINGTONE);
        if (pref != null && result.getData() != null) {
            Uri ringtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String ringtoneTitle = getRingtoneTitle(ringtoneUri);
            pref.setSummary(ringtoneTitle);
            if (ringtoneUri != null) {
                sharedPref.edit().putString(PREF_KCA_NOTI_RINGTONE, ringtoneUri.toString()).apply();
            } else {
                sharedPref.edit().putString(PREF_KCA_NOTI_RINGTONE, "").apply();
            }
        }
    }

    public void showToast(Context context, String text, int length) {
        if (checkActivityValid() && !getActivity().isFinishing()) {
            Toast.makeText(context, text, length).show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sHandler != null) {
            JsonObject dmpData = new JsonObject();
            String kca_url = "";
            Bundle bundle = new Bundle();
            switch (key) {
                case PREF_KCA_SEEK_CN:
                    kca_url = KCA_API_PREF_CN_CHANGED;
                    break;
                case PREF_KCA_EXP_VIEW:
                    kca_url = KCA_API_PREF_EXPVIEW_CHANGED;
                    break;
                case PREF_KCA_SET_PRIORITY:
                    kca_url = KCA_API_PREF_PRIORITY_CHANGED;
                    break;
                case PREF_FAIRY_SIZE:
                    kca_url = KCA_API_PREF_FAIRYSIZE_CHANGED;
                    break;
                default:
                    break;
            }
            if (kca_url.length() > 0) {
                bundle.putString("url", kca_url);
                bundle.putString("data", gson.toJson(dmpData));
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }
        }

        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference lp = (ListPreference) preference;
            preference.setSummary(lp.getEntry());
        }

        if (preference instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) preference;
            preference.setSummary(getEditTextSummary(key, etp.getText()));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!checkActivityValid()) return false;
        String key = preference.getKey();
        Log.e("KCA", "onPreferenceChange " + key);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (PREF_KCA_LANGUAGE.equals(key)) {
            if (KcaService.getServiceStatus()) {
                showToast(getActivity(), "Cannot change while service is running.", Toast.LENGTH_SHORT);
                return false;
            }
        }

        if (PREF_KC_PACKAGE.equals(key)) {
            String val = (String) newValue;
            ListPreference sm_pref = (ListPreference) findPreference(PREF_SNIFFER_MODE);
            if (KC_PACKAGE_NAME.equals(val)) {
                setActiveSnifferSettingEnabled(true);
                setSnifferModeSettingEnabled(false);
            } else if (GOTO_PACKAGE_NAME.equals(val)) {
                int sniffer_mode = Integer.parseInt(prefs.getString(PREF_SNIFFER_MODE, String.valueOf(SNIFFER_ACTIVE)));
                setActiveSnifferSettingEnabled(sniffer_mode == SNIFFER_ACTIVE);
                setSnifferModeSettingEnabled(true);
            }
        }

        if (PREF_SNIFFER_MODE.equals(key)) {
            if (!KcaService.getServiceStatus()) {
                String val = (String) newValue;
                if (Integer.parseInt(val) == SNIFFER_PASSIVE) {
                    if (prefs.getBoolean(PREF_VPN_ENABLED, false)) {
                        KcaVpnService.stop(VPN_STOP_REASON, getActivity());
                        prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                    }
                    setActiveSnifferSettingEnabled(false);
                } else if (Integer.parseInt(val) == SNIFFER_ACTIVE) {
                    setActiveSnifferSettingEnabled(true);
                }
            } else {
                showToast(getActivity(), "Cannot change while service is running.", Toast.LENGTH_SHORT);
                return false;
            }
        }

        if (PREF_FAIRY_AUTOHIDE.equals(key)) {
            boolean isTrue = (Boolean) newValue;
            if (isTrue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    !hasUsageStatPermission(getContext())) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle(getStringWithLocale(R.string.sa_usagestat_dialog_title))
                    .setMessage(getStringWithLocale(R.string.sa_usagestat_dialog_desc))
                    .setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                            (dialogInterface, i) -> showObtainingUsageStatPermission())
                    .setNegativeButton(getStringWithLocale(R.string.dialog_cancel),
                            (dialogInterface, i) -> {
                    })
                    .setIcon(R.mipmap.ic_launcher);
                if (!getActivity().isFinishing()) alertDialog.show();
                return false;
            } else {
                Intent intent = new Intent(getActivity(), KcaViewButtonService.class);
                intent.setAction(isTrue ? FAIRY_FORECHECK_ON : FAIRY_FORECHECK_OFF);
                getActivity().startService(intent);
            }
        }

        if (PREF_ALARM_DELAY.equals(key)) {
            String valueString = ((String) newValue);
            if (valueString.length() == 0 || !TextUtils.isDigitsOnly(valueString)) return false;
            int value = Integer.parseInt(valueString);
            KcaAlarmService.setAlarmDelay(value);
            if (sHandler != null) {
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_PREF_ALARMDELAY_CHANGED);
                bundle.putString("data", "");
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }
        }

        if (PREF_KCA_MORALE_MIN.equals(key)) {
            String valueString = ((String) newValue);
            if (valueString.length() == 0 || !TextUtils.isDigitsOnly(valueString)) return false;
            int value = Integer.parseInt(valueString);
            if (value > 100) {
                showToast(getActivity(), "value must be in 0~100", Toast.LENGTH_LONG);
                return false;
            }
            KcaMoraleInfo.setMinMorale(value);
        }
        return true;
    }

    private String getEditTextSummary(String key, String value) {
        if (PREF_HDNOTI_MINLEVEL.equals(key)) {
            if (value.equals("0")) {
                return KcaUtils.format("%s (%s)", value,
                        getStringWithLocale(R.string.setting_menu_view_desc_hdmg_minlevel));
            }
        }
        return value;
    }

    private String getRingtoneTitle(Uri ringtoneUri) {
        Log.e("KCA", "uri: " + (ringtoneUri != null ? ringtoneUri.toString() : ""));
        Log.e("KCA", "valid: " + checkActivityValid());
        try {
            if (ringtoneUri != null && checkActivityValid()) {
                getActivity().grantUriPermission(BuildConfig.APPLICATION_ID, ringtoneUri, FLAG_GRANT_READ_URI_PERMISSION);
                Ringtone ringtone = RingtoneManager.getRingtone(getContext(), ringtoneUri);
                return ringtone.getTitle(getContext());
            } else {
                return getStringWithLocale(R.string.settings_string_silent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "???";
        }
    }

    private void setSnifferModeSettingEnabled(boolean enabled) {
        ListPreference pref = findPreference(PREF_SNIFFER_MODE);
        if (enabled) pref.setSummary(pref.getEntry());
        else pref.setSummary(getStringWithLocale(R.string.setting_menu_kand_desc_sniffer_type_na));
        pref.setEnabled(enabled);
    }

    private void setActiveSnifferSettingEnabled(boolean enabled) {
        findPreference(PREF_SCREEN_SNIFFER_ALLOW).setEnabled(enabled);
        findPreference(PREF_ALLOW_EXTFILTER).setEnabled(enabled);
        findPreference(PREF_SCREEN_ADV_NETWORK).setEnabled(enabled);
    }

    private void setSettingDisabledWhenServiceRunning() {
        String[] keys = {PREF_KCA_LANGUAGE, PREF_SNIFFER_MODE};
        if (KcaService.getServiceStatus()) {
            for (String key: keys) {
                findPreference(key).setEnabled(false);
                findPreference(key).setSummary(getStringWithLocale(R.string.setting_service_running_desc));
            }
        }
    }

    private boolean checkActivityValid() {
        return isActivitySet && getContext() != null && getActivity() != null;
    }

    private void checkRecentVersion() {
        String currentVersion = BuildConfig.VERSION_NAME;
        final Call<String> rv_data = downloader.getRecentVersion();
        rv_data.enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                JsonObject response_data = new JsonObject();
                try {
                    if (response.body() != null) {
                        response_data = JsonParser.parseString(response.body()).getAsJsonObject();
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
                }

                Log.e("KCA", response_data.toString());

                if (response_data.has("version")) {
                    String recentVersion = response_data.get("version").getAsString();
                    if (compareVersion(currentVersion, recentVersion)) { // True if latest
                        showToast(getActivity(),
                                KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_latest), currentVersion),
                                Toast.LENGTH_LONG);
                    } else if (!getActivity().isFinishing()) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setMessage(KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_hasupdate), recentVersion));
                        alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                                (dialog, which) -> {
                                    String downloadUrl = getStringPreferences(getContext(), PREF_APK_DOWNLOAD_SITE);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                                        startActivity(intent);
                                    } else if (downloadUrl.contains(getStringWithLocale(R.string.app_download_link_playstore))) {
                                        showToast(getContext(), "Google Play Store not found", Toast.LENGTH_LONG);
                                        AlertDialog.Builder apkDownloadPathDialog = new AlertDialog.Builder(getActivity());
                                        apkDownloadPathDialog.setIcon(R.mipmap.ic_launcher);
                                        apkDownloadPathDialog.setTitle(getStringWithLocale(R.string.setting_menu_app_title_down));
                                        apkDownloadPathDialog.setCancelable(true);
                                        apkDownloadPathDialog.setItems(R.array.downloadSiteOptionWithoutPlayStore, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                String[] path_value = getResources().getStringArray(R.array.downloadSiteOptionWithoutPlayStoreValue);
                                                setPreferences(getContext(), PREF_APK_DOWNLOAD_SITE, path_value[i]);
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(path_value[i]));
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                            }
                                        });
                                        apkDownloadPathDialog.show();
                                    }
                                });
                        alertDialog.setNegativeButton(getStringWithLocale(R.string.dialog_cancel),
                                (dialog, which) -> {
                                    // None
                                });
                        AlertDialog alert = alertDialog.create();
                        alert.setIcon(R.mipmap.ic_launcher);
                        alert.setTitle(getStringWithLocale(R.string.sa_checkupdate_dialogtitle));
                        alert.show();
                    }
                } else {
                    showToast(getActivity(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Activity activity = getActivity();
                if (activity != null && KcaUtils.checkOnline(activity)) {
                    showToast(activity, getStringWithLocale(R.string.sa_checkupdate_servererror), Toast.LENGTH_LONG);
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "version_check", "", "", t.getMessage());
                }
            }
        });
    }
}