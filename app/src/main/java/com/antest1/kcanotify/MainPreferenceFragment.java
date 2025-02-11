package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import retrofit2.Call;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
import static com.antest1.kcanotify.KcaAlarmService.REFRESH_CHANNEL;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.checkContentUri;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.createBuilder;
import static com.antest1.kcanotify.KcaUtils.getUriFromContent;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setSoundSetting;
import static com.antest1.kcanotify.SettingActivity.REQUEST_ALERT_RINGTONE;
import static com.antest1.kcanotify.SettingActivity.REQUEST_BATOPTIM_PERMISSION;
import static com.antest1.kcanotify.SettingActivity.REQUEST_OVERLAY_PERMISSION;
import static com.antest1.kcanotify.SettingActivity.REQUEST_USAGESTAT_PERMISSION;

public class MainPreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private final static int TEST_NOTI_ID = 9999;

    KcaDBHelper dbHelper;
    private boolean isActivitySet = false;
    public KcaDownloader downloader;
    public static Handler sHandler;
    private SharedPreferences sharedPref = null;
    static Gson gson = new Gson();
    private Callback mCallback;

    ActivityResultLauncher<Intent> startActivityResult;
    private int currentActivity = 0;

    private NotificationManager notificationManager;
    private AudioManager mAudioManager;

    private String notification_id;

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

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        sharedPref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        Activity activity = getActivity();
        if (activity instanceof Callback) mCallback = (Callback) activity;

        createNotificationChannel(context);

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
    public void onDetach() {
        super.onDetach();
        deleteNotificationChannel();
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isActivitySet = true;

        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(getString(R.string.action_settings));

        Map<String, ?> allEntries = sharedPref.getAll();

        String playstore_link = getString(R.string.app_download_link_playstore);
        if (sharedPref.getString(PREF_APK_DOWNLOAD_SITE, "").equals(playstore_link)) {
            sharedPref.edit().putString(PREF_APK_DOWNLOAD_SITE, getString(R.string.app_download_link_luckyjervis)).commit();
        }

        for (String key : allEntries.keySet()) {
            Preference preference = findPreference(key);
            if (preference == null) continue;
            if (preference instanceof ListPreference) {
                ListPreference etp = (ListPreference) preference;
                if (!PREF_KCA_LANGUAGE.equals(preference.getKey())) {
                    preference.setSummary(etp.getEntry());
                }
            } else if (preference instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) preference;
                preference.setSummary(getEditTextSummary(key, etp.getText()));
            } else if (PREF_KCA_NOTI_RINGTONE.equals(preference.getKey())) {
                String uri = sharedPref.getString(PREF_KCA_NOTI_RINGTONE, "");
                Uri ringtoneUri = Uri.parse(uri);
                String ringtoneTitle = getString(R.string.settings_string_silent);
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
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Log.e("KCA", "onPreferenceTreeClick " + preference.getKey());
        String key = preference.getKey();
        if (!isActivitySet) return false;

        if (PREF_OVERLAY_SETTING.equals(key)) {
            showObtainingPermissionOverlayWindow();
        }

        if (PREF_BATOPTIM_SETTING.equals(key)) {
            showObtainingPermissionBatteryOptimization();
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
                if (existingValue.isEmpty()) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, DEFAULT_NOTIFICATION_URI);
            }
            currentActivity = REQUEST_ALERT_RINGTONE;
            startActivityResult.launch(intent);
        }

        if (PREF_CHECK_UPDATE.equals(key)) {
            checkRecentVersion();
        }

        if (PREF_NOTI_TEST_SHOW.equals(key)) {
            makeTestNotification();
        }

        return super.onPreferenceTreeClick(preference);
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public void showObtainingPermissionOverlayWindow() {
        String package_name = getContext().getPackageName();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + package_name));
        if (intent.resolveActivity(getContext().getPackageManager()) == null) {
            intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + package_name));
            showToast(getActivity(), getString(R.string.sa_overlay_appearontop), Toast.LENGTH_LONG);
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
            showToast(getActivity(), getString(R.string.sa_batteryoptim_appearontop), Toast.LENGTH_LONG);
        }
        currentActivity = REQUEST_BATOPTIM_PERMISSION;
        startActivityResult.launch(intent);
    }

    private static boolean hasUsageStatPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager)
                context.getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName()
            );
        } else {
            mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName()
            );
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void showObtainingUsageStatPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        currentActivity = REQUEST_USAGESTAT_PERMISSION;
        startActivityResult.launch(intent);
    }

    private void showOverlayPermissionResult() {
        if (checkActivityValid()) {
            int delay = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 0 : 1000;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Context context = getContext();
                if (context != null) {
                    if (Settings.canDrawOverlays(getContext())) {
                        showToast(getActivity(), getString(R.string.sa_overlay_ok), Toast.LENGTH_SHORT);
                    } else {
                        showToast(getActivity(), getString(R.string.sa_overlay_no), Toast.LENGTH_SHORT);
                    }
                }
            }, delay);
        }
    }

    private void showBatteryOptimizationPermissionResult() {
        if (checkActivityValid()) {
            int delay = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 0 : 1000;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Context context = getContext();
                if (context != null) {
                    PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (manager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                        showToast(getActivity(), getString(R.string.sa_batteryoptim_ok), Toast.LENGTH_SHORT);
                    } else {
                        showToast(getActivity(), getString(R.string.sa_batteryoptim_no), Toast.LENGTH_SHORT);
                    }
                }
            }, delay);
        }
    }

    private void showUsageStatsPermissionResult() {
        if (checkActivityValid()) {
            if(hasUsageStatPermission(getContext())) {
                showToast(getActivity(), getString(R.string.sa_usagestat_ok), Toast.LENGTH_SHORT);
            } else {
                showToast(getActivity(), getString(R.string.sa_usagestat_no), Toast.LENGTH_SHORT);
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void callbackAlertRingtoneResult(ActivityResult result) {
        Preference pref = findPreference(PREF_KCA_NOTI_RINGTONE);
        Intent alarmService = new Intent(requireContext(), KcaAlarmService.class);
        alarmService.setAction(REFRESH_CHANNEL);
        if (result != null && result.getData() != null) {
            Uri ringtoneUri;
            Intent intentResult = result.getData();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ringtoneUri = intentResult.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
            } else {
                ringtoneUri = intentResult.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            }
            String ringtoneTitle = getRingtoneTitle(ringtoneUri);
            if (pref != null) pref.setSummary(ringtoneTitle);
            if (ringtoneUri != null) {
                sharedPref.edit().putString(PREF_KCA_NOTI_RINGTONE, ringtoneUri.toString()).commit();
                alarmService.putExtra("uri", ringtoneUri.toString());
            } else {
                sharedPref.edit().putString(PREF_KCA_NOTI_RINGTONE, "").commit();
                alarmService.putExtra("uri", "");
            }
        } else {
            alarmService.putExtra("uri", getStringPreferences(requireContext(), PREF_KCA_NOTI_RINGTONE));
        }
        requireActivity().startService(alarmService);
        deleteNotificationChannel();
        createNotificationChannel(requireContext());
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
                case PREF_FAIRY_OPACITY:
                    kca_url = KCA_API_PREF_FAIRYALPHA_CHANGED;
                    break;
                default:
                    break;
            }
            if (!kca_url.isEmpty()) {
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
            if (PREF_KCA_LANGUAGE.equals(key)) {
                preference.setSummary(KcaUtils.format("%s - %s",
                        getString(R.string.setting_menu_kand_desc_language),
                        lp.getEntry()));
            } else {
                preference.setSummary(lp.getEntry());
            }
        }

        if (preference instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) preference;
            preference.setSummary(getEditTextSummary(key, etp.getText()));
        }

        if (PREF_KCA_NOTI_SOUND_KIND.equals(key)) {
            callbackAlertRingtoneResult(null);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!checkActivityValid()) return false;
        String key = preference.getKey();
        Log.e("KCA", "onPreferenceChange " + key);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (PREF_KC_PACKAGE.equals(key)) {
            String val = (String) newValue;
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
            String val = (String) newValue;
            if (Integer.parseInt(val) == SNIFFER_PASSIVE) {
                if (prefs.getBoolean(PREF_VPN_ENABLED, false)) {
                    KcaVpnService.stop(VPN_STOP_REASON, getActivity());
                    prefs.edit().putBoolean(PREF_VPN_ENABLED, false).commit();
                }
                setActiveSnifferSettingEnabled(false);
            } else if (Integer.parseInt(val) == SNIFFER_ACTIVE) {
                setActiveSnifferSettingEnabled(true);
            }
        }

        if (PREF_FAIRY_AUTOHIDE.equals(key)) {
            boolean isTrue = (Boolean) newValue;
            if (isTrue && !hasUsageStatPermission(getContext())) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle(getString(R.string.sa_usagestat_dialog_title))
                    .setMessage(getString(R.string.sa_usagestat_dialog_desc))
                    .setPositiveButton(getString(R.string.dialog_ok),
                            (dialogInterface, i) -> showObtainingUsageStatPermission())
                    .setNegativeButton(getString(R.string.dialog_cancel),
                            (dialogInterface, i) -> {
                    })
                    .setIcon(R.mipmap.ic_launcher);
                if (getActivity() != null && !getActivity().isFinishing())
                    alertDialog.show();
                return false;
            } else if (KcaService.getServiceStatus()) {
                if (sHandler != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("url", isTrue ?
                            KCA_API_PREF_CHANGE_ON_ACTION : KCA_API_PREF_CHANGE_OFF_ACTION);
                    bundle.putString("data", "");
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
            }
        }

        if (PREF_ALARM_DELAY.equals(key)) {
            String valueString = ((String) newValue);
            if (valueString.isEmpty() || !TextUtils.isDigitsOnly(valueString)) return false;
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
            if (valueString.isEmpty() || !TextUtils.isDigitsOnly(valueString)) return false;
            int value = Integer.parseInt(valueString);
            if (value > 100) {
                showToast(getActivity(), "value must be in 0~100", Toast.LENGTH_LONG);
                return false;
            }
            KcaMoraleInfo.setMinMorale(value);
        }

        if (PREF_FAIRY_OPACITY.equals(key)) {
            String valueString = ((String) newValue);
            if (valueString.isEmpty() || !TextUtils.isDigitsOnly(valueString)) return false;
            int value = Integer.parseInt(valueString);
            if (value < 10 || value > 100) {
                showToast(getActivity(), "value must be in 10~100", Toast.LENGTH_LONG);
                return false;
            }
        }

        if (PREF_USE_TLS_DECRYPTION.equals(key)) {
            boolean val = ((Boolean) newValue);
            setSocks5SettingEnabled(!val);
        }

        return true;
    }

    private String getEditTextSummary(String key, String value) {
        if (PREF_HDNOTI_MINLEVEL.equals(key)) {
            if (value.equals("0")) {
                return KcaUtils.format("%s (%s)", value,
                        getString(R.string.setting_menu_view_desc_hdmg_minlevel));
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
                return getString(R.string.settings_string_silent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "???";
        }
    }

    private void setSnifferModeSettingEnabled(boolean enabled) {
        ListPreference pref = findPreference(PREF_SNIFFER_MODE);
        if (enabled) pref.setSummary(pref.getEntry());
        else pref.setSummary(getString(R.string.setting_menu_kand_desc_sniffer_type_na));
        pref.setEnabled(enabled);
    }

    private void setSocks5SettingEnabled(boolean enabled) {
        Preference pref = findPreference(PREF_SCREEN_ADV_NETWORK);
        pref.setEnabled(enabled);
        if (enabled) {
            pref.setSummary("");
        } else {
            pref.setSummary(getString(R.string.setting_menu_kand_desc_socks5_tls_incompatible));
        }
    }

    private void setActiveSnifferSettingEnabled(boolean enabled) {
        boolean tls_disabled = !sharedPref.getBoolean(PREF_USE_TLS_DECRYPTION, false);
        String[] keys = {
                PREF_SCREEN_SNIFFER_ALLOW, PREF_ALLOW_EXTFILTER,
                PREF_USE_TLS_DECRYPTION, PREF_MITM_SETUP_WIZARD, PREF_SCREEN_ADV_NETWORK};
        setSocks5SettingEnabled(tls_disabled);
        for (String key: keys) {
            if (PREF_SCREEN_ADV_NETWORK.equals(key)) {
                findPreference(key).setEnabled(enabled && tls_disabled);
            } else {
                findPreference(key).setEnabled(enabled);
            }
        }
    }

    private void setSettingDisabledWhenServiceRunning() {
        String[] keys = {PREF_KCA_LANGUAGE, PREF_KC_PACKAGE, PREF_SNIFFER_MODE};
        if (KcaService.getServiceStatus()) {
            for (String key: keys) {
                findPreference(key).setEnabled(false);
                findPreference(key).setSummary(getString(R.string.setting_service_running_desc));
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

                if (isAdded()) {
                    Log.e("KCA", response_data.toString());
                    if (response_data.has("version")) {
                        String recentVersion = response_data.get("version").getAsString();
                        if (compareVersion(currentVersion, recentVersion)) { // True if latest
                            showToast(getActivity(),
                                    KcaUtils.format(getString(R.string.sa_checkupdate_latest), currentVersion),
                                    Toast.LENGTH_LONG);
                        } else if (!getActivity().isFinishing()) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                            alertDialog.setMessage(KcaUtils.format(getString(R.string.sa_checkupdate_hasupdate), recentVersion));
                            alertDialog.setPositiveButton(getString(R.string.dialog_ok),
                                    (dialog, which) -> {
                                        String downloadUrl = getStringPreferences(getContext(), PREF_APK_DOWNLOAD_SITE);
                                        if (downloadUrl.contains(getString(R.string.app_download_link_playstore))) {
                                            downloadUrl = getString(R.string.app_download_link_luckyjervis);
                                        }
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    });
                            alertDialog.setNegativeButton(getString(R.string.dialog_cancel),
                                    (dialog, which) -> {
                                        // None
                                    });
                            AlertDialog alert = alertDialog.create();
                            alert.setIcon(R.mipmap.ic_launcher);
                            alert.setTitle(getString(R.string.sa_checkupdate_dialogtitle));
                            alert.show();
                        }
                    } else {
                        showToast(getActivity(),
                                getString(R.string.sa_checkupdate_servererror),
                                Toast.LENGTH_LONG);
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Activity activity = getActivity();
                if (activity != null && KcaUtils.checkOnline(activity)) {
                    showToast(activity, getString(R.string.sa_checkupdate_servererror), Toast.LENGTH_LONG);
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "version_check", "", "", t.getMessage());
                }
            }
        });
    }

    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String uri = getStringPreferences(context, PREF_KCA_NOTI_RINGTONE);
            String soundKind = getStringPreferences(context, PREF_KCA_NOTI_SOUND_KIND);

            boolean isSound = soundKind.equals(NOTI_SOUND_KIND_MIXED)
                    || soundKind.equals(NOTI_SOUND_KIND_NORMAL);
            boolean isVibrate = soundKind.equals(NOTI_SOUND_KIND_MIXED)
                    || soundKind.equals(NOTI_SOUND_KIND_VIBRATE);

            notification_id = KcaUtils.format("test_%s_%d", KcaAlarmService.createAlarmId(uri, soundKind), System.currentTimeMillis());
            NotificationChannel channel = new NotificationChannel(notification_id,
                    KcaUtils.format("[Test] %s %s", soundKind, uri.hashCode()), NotificationManager.IMPORTANCE_HIGH);

            if (isSound && !uri.isEmpty()) {
                AudioAttributes.Builder attrs = new AudioAttributes.Builder();
                attrs.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
                attrs.setUsage(AudioAttributes.USAGE_NOTIFICATION);

                try {
                    Uri content_uri = getUriFromContent(Uri.parse(uri));
                    if (DEFAULT_NOTIFICATION_URI.equals(content_uri)) {
                        channel.setSound(DEFAULT_NOTIFICATION_URI, attrs.build());
                    } else if (checkContentUri(context, content_uri)) {
                        channel.setSound(content_uri, attrs.build());
                    } else {
                        channel.setSound(DEFAULT_NOTIFICATION_URI, attrs.build());
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "noti_test_init", "", "", getStringFromException(e));
                }
            } else {
                channel.setSound(null, null);
            }

            channel.enableVibration(isVibrate);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void deleteNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(notification_id);
        }
    }

    private void makeTestNotification() {
        try {
            notificationManager.cancel(TEST_NOTI_ID);

            String title = "[Test] " + getString(R.string.app_name);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
            String content = sdf.format(new Date());

            Bitmap testBitmap = KcaUtils.decodeSampledBitmapFromResource(getResources(), R.mipmap.expedition_notify_bigicon, 128, 128);
            NotificationCompat.Builder builder = createBuilder(getContext(), notification_id)
                    .setSmallIcon(R.mipmap.expedition_notify_icon)
                    .setLargeIcon(testBitmap)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setAutoCancel(false)
                    .setTicker(title);
            builder = setSoundSetting(getContext(), mAudioManager, builder);
            Notification noti = builder.build();
            noti.flags = Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(TEST_NOTI_ID, noti);

        } catch (Exception e) {
            Toast.makeText(getContext(), "something went wrong: check error log", Toast.LENGTH_SHORT).show();
            dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "noti_test_show", "", "", getStringFromException(e));
        }
    }
}