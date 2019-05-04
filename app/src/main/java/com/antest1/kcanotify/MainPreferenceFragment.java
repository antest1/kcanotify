package com.antest1.kcanotify;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.antest1.kcanotify.BuildConfig;
import com.antest1.kcanotify.KcaAlarmService;
import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaDownloader;
import com.antest1.kcanotify.KcaMoraleInfo;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.KcaViewButtonService;
import com.antest1.kcanotify.KcaVpnService;
import com.antest1.kcanotify.LocaleUtils;
import com.antest1.kcanotify.R;
import com.antest1.kcanotify.SettingActivity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;
import java.util.Map;

import retrofit2.Call;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static com.antest1.kcanotify.KcaAlarmService.REFRESH_CHANNEL;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_MAIN;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_ALARMDELAY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_CN_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_EXPVIEW_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_LANGUAGE_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_PRIORITY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_ALARM_DELAY;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_AUTOHIDE;
import static com.antest1.kcanotify.KcaConstants.PREF_HDNOTI_MINLEVEL;
import static com.antest1.kcanotify.KcaConstants.PREF_KCAQSYNC_PASS;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DOWNLOAD_DATA;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_MORALE_MIN;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SET_PRIORITY;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_OVERLAY_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_SCREEN_ADV_NETWORK;
import static com.antest1.kcanotify.KcaConstants.PREF_SNIFFER_MODE;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaConstants.SNIFFER_PASSIVE;
import static com.antest1.kcanotify.KcaConstants.VPN_STOP_REASON;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.SettingActivity.REQUEST_OVERLAY_PERMISSION;
import static com.antest1.kcanotify.SettingActivity.REQUEST_USAGESTAT_PERMISSION;

public class MainPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    KcaDBHelper dbHelper;
    public KcaDownloader downloader;
    public static RingtoneManager ringtoneManager;
    public static String silentText;
    public static Handler sHandler;
    static Gson gson = new Gson();

    private Callback mCallback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Callback) {
            mCallback = (Callback) activity;
        } else {
            throw new IllegalStateException("callback interface not implemented");
        }
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getActivity().getApplicationContext(), getActivity().getBaseContext(), id);
    }

    public Context getApplicationContext() {
        Context context = getActivity();
        if (context != null) {
            return context.getApplicationContext();
        } else {
            return null;
        }
    }

    public void showToast(Context context, String text, int length) {
        if (context != null) {
            Toast.makeText(context, text, length).show();
        }
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public interface Callback {
        public void onNestedPreferenceSelected(int key);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(getStringWithLocale(R.string.action_settings));
    }

    @Override
    public void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        silentText = getString(R.string.settings_string_silent);

        getPreferenceManager().setSharedPreferencesName("pref");
        //SharedPreferences prefs = this.getActivity().getSharedPreferences("pref", MODE_PRIVATE);
        addPreferencesFromResource(R.xml.pref_settings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Map<String, ?> allEntries = getPreferenceManager().getSharedPreferences().getAll();
        for (String key : allEntries.keySet()) {
            Preference pref = findPreference(key);
            if (key.equals(PREF_CHECK_UPDATE)) {
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        checkRecentVersion();
                        return false;
                    }
                });
            }
            /*if (key.equals(PREF_KCA_DOWNLOAD_DATA)) {
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        downloadGameData();
                        return false;
                    }
                });
            }*/

            if (key.equals(PREF_FAIRY_AUTOHIDE)) {
                pref.setOnPreferenceChangeListener((preference, o) -> {
                    boolean new_value = (Boolean) o;
                    if (new_value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                            !hasUsageStatPermission(getActivity().getApplicationContext())) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle(getStringWithLocale(R.string.sa_usagestat_dialog_title))
                                .setMessage(getStringWithLocale(R.string.sa_usagestat_dialog_desc))
                                .setPositiveButton(getStringWithLocale(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        showObtainingUsageStatPermission();
                                    }
                                })
                                .setNegativeButton(getStringWithLocale(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                })
                                .setIcon(R.mipmap.ic_launcher)
                                .show();
                        return false;
                    } else {
                        Intent intent = new Intent(getActivity(), KcaViewButtonService.class);
                        if (new_value) {
                            intent.setAction(KcaViewButtonService.FAIRY_FORECHECK_ON);
                        } else {
                            intent.setAction(KcaViewButtonService.FAIRY_FORECHECK_OFF);
                        }
                        getApplicationContext().startService(intent);
                        return true;
                    }
                });
            }

            if (key.equals(PREF_OVERLAY_SETTING)) {
                pref.setOnPreferenceClickListener(preference -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        showObtainingPermissionOverlayWindow();
                    } else {
                        showToast(getApplicationContext(), getStringWithLocale(R.string.sa_overlay_under_m), Toast.LENGTH_SHORT);
                    }
                    return false;
                });
            }



            if (key.equals(PREF_SNIFFER_MODE)) {
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    String val = (String) newValue;
                    if (Integer.parseInt(val) == SNIFFER_PASSIVE && prefs.getBoolean(PREF_VPN_ENABLED, false)) {
                        KcaVpnService.stop(VPN_STOP_REASON, getActivity());
                        prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                    }
                    return true;
                });
            }

            if (key.equals(PREF_KCA_LANGUAGE)) {
                pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String pref = (String) newValue;
                        if (pref.startsWith("default")) {
                            LocaleUtils.setLocale(Locale.getDefault());
                        } else {
                            String[] locale = ((String) newValue).split("-");
                            LocaleUtils.setLocale(new Locale(locale[0], locale[1]));
                        }
                        if (sHandler != null) {
                            Bundle bundle = new Bundle();
                            bundle.putString("url", KCA_API_PREF_LANGUAGE_CHANGED);
                            bundle.putString("data", "");
                            Message sMsg = sHandler.obtainMessage();
                            sMsg.setData(bundle);
                            sHandler.sendMessage(sMsg);
                        }
                        showToast(getApplicationContext(), getStringWithLocale(R.string.sa_language_changed), Toast.LENGTH_LONG);
                        return true;
                    }
                });
            }

            if (key.equals(PREF_ALARM_DELAY)) {
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String new_val = ((String) newValue);
                    if (new_val.length() == 0) return false;
                    int value = Integer.parseInt(new_val);
                    KcaAlarmService.setAlarmDelay(value);
                    if (sHandler != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("url", KCA_API_PREF_ALARMDELAY_CHANGED);
                        bundle.putString("data", "");
                        Message sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }
                    return true;
                });
            }

            if (key.equals(PREF_KCA_MORALE_MIN)) {
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String new_val = ((String) newValue);
                    if (new_val.length() == 0) return false;
                    int value = Integer.parseInt(new_val);
                    if (value > 100) {
                        showToast(getApplicationContext(), "value must be in 0~100", Toast.LENGTH_LONG);
                        return false;
                    }
                    KcaMoraleInfo.setMinMorale(value);
                    return true;
                });
            }

            if (key.equals(PREF_SCREEN_ADV_NETWORK)) {
                pref.setOnPreferenceClickListener(preference -> {
                    mCallback.onNestedPreferenceSelected(NestedPreferenceFragment.FRAGMENT_ADV_NETWORK);
                    return false;
                });
            }

            if (pref instanceof RingtonePreference) {
                String uri = pref.getSharedPreferences().getString(key, "");
                if (uri.length() == 0) {
                    pref.setSummary(silentText);
                } else {
                    Uri ringtoneUri = Uri.parse(uri);
                    getActivity().grantUriPermission(BuildConfig.APPLICATION_ID, ringtoneUri, FLAG_GRANT_READ_URI_PERMISSION);
                    Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                    if (ringtone == null) {
                        showToast(getApplicationContext(),
                                getStringWithLocale(R.string.ma_permission_external_denied),
                                Toast.LENGTH_LONG);
                        pref.setSummary(silentText);
                    } else {
                        String name = ringtone.getTitle(getApplicationContext());
                        pref.setSummary(name);
                    }
                }
            } else if (pref instanceof ListPreference) {
                ListPreference etp = (ListPreference) pref;
                pref.setSummary(etp.getEntry());
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
                if (key.equals(PREF_HDNOTI_MINLEVEL)) {
                    if (etp.getText().equals("0")) {
                        String current = etp.getText();
                        pref.setSummary(KcaUtils.format("%s (%s)", current, getStringWithLocale(R.string.setting_menu_view_desc_hdmg_minlevel)));
                    }
                }
                if (key.equals(PREF_KCAQSYNC_PASS)) {
                    if (etp.getText().trim().length() == 0) {
                        pref.setSummary(getStringWithLocale(R.string.setting_menu_stat_desc_kcaqsync_pass_emtpy));
                    }
                    etp.setDialogMessage(getStringWithLocale(R.string.setting_menu_stat_desc_kcaqsync_pass));
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showObtainingPermissionOverlayWindow() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
        if(intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            try {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS), REQUEST_OVERLAY_PERMISSION);
            } finally {
                showToast(getApplicationContext(), getStringWithLocale(R.string.sa_overlay_appearontop), Toast.LENGTH_LONG);
            }
        }
    }

    public void showObtainingPermissionAccessibility() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void showObtainingUsageStatPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, REQUEST_USAGESTAT_PERMISSION);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (getActivity() != null && getApplicationContext() != null) {
            if (requestCode == REQUEST_OVERLAY_PERMISSION) {
                int delay = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 0 : 1000;
                new Handler().postDelayed(() -> {
                    if (Settings.canDrawOverlays(getApplicationContext())) {
                        showToast(getActivity(), getStringWithLocale(R.string.sa_overlay_ok), Toast.LENGTH_SHORT);
                    } else {
                        showToast(getActivity(), getStringWithLocale(R.string.sa_overlay_no), Toast.LENGTH_SHORT);
                    }
                }, delay);
            } else if (requestCode == REQUEST_USAGESTAT_PERMISSION) {
                if(hasUsageStatPermission(getApplicationContext())) {
                    showToast(getActivity(), getStringWithLocale(R.string.sa_usagestat_ok), Toast.LENGTH_SHORT);
                } else {
                    showToast(getActivity(), getStringWithLocale(R.string.sa_usagestat_no), Toast.LENGTH_SHORT);
                }
            }
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
                default:
                    break;
            }
            if (kca_url.length() != 0) {
                bundle.putString("url", kca_url);
                bundle.putString("data", gson.toJson(dmpData));
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }
        }
        Preference pref = findPreference(key);

        if (pref instanceof RingtonePreference) {
            String uri = sharedPreferences.getString(key, "null");
            if (uri.length() == 0) {
                pref.setSummary(silentText);
            } else {
                Uri ringtoneUri = Uri.parse(uri);
                if (getActivity() != null) {
                    getActivity().grantUriPermission(BuildConfig.APPLICATION_ID, ringtoneUri, FLAG_GRANT_READ_URI_PERMISSION);
                    Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                    if (ringtone == null) {
                        showToast(getApplicationContext(),
                                getStringWithLocale(R.string.ma_permission_external_denied),
                                Toast.LENGTH_LONG);
                        pref.setSummary(silentText);
                    } else {
                        String name = ringtone.getTitle(getApplicationContext());
                        pref.setSummary(name);
                    }
                }
            }
            Log.e("KCA-S", "sdf");
            if (getActivity() != null && !getActivity().isFinishing()) {
                Intent aIntent = new Intent(getActivity(), KcaAlarmService.class);
                aIntent.setAction(REFRESH_CHANNEL);
                aIntent.putExtra("uri", uri);
                getApplicationContext().startService(aIntent);
            }
        } else if (pref instanceof ListPreference) {
            ListPreference etp = (ListPreference) pref;
            pref.setSummary(etp.getEntry());
        } else if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            pref.setSummary(etp.getText());
        }
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
                        response_data = new JsonParser().parse(response.body()).getAsJsonObject();
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
                }

                Log.e("KCA", response_data.toString());

                if (response_data.has("version")) {
                    String recentVersion = response_data.get("version").getAsString();
                    if (compareVersion(currentVersion, recentVersion)) { // True if latest
                        showToast(getApplicationContext(),
                                KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_latest), currentVersion),
                                Toast.LENGTH_LONG);
                    } else if (!getActivity().isFinishing()) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setMessage(KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_hasupdate), recentVersion));
                        alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                                (dialog, which) -> {
                                    String downloadUrl = getStringPreferences(getApplicationContext(), PREF_APK_DOWNLOAD_SITE);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
                                        startActivity(intent);
                                    } else if (downloadUrl.contains(getStringWithLocale(R.string.app_download_link_playstore))) {
                                        showToast(getApplicationContext(), "Google Play Store not found", Toast.LENGTH_LONG);
                                        AlertDialog.Builder apkDownloadPathDialog = new AlertDialog.Builder(getActivity());
                                        apkDownloadPathDialog.setIcon(R.mipmap.ic_launcher);
                                        apkDownloadPathDialog.setTitle(getStringWithLocale(R.string.setting_menu_app_title_down));
                                        apkDownloadPathDialog.setCancelable(true);
                                        apkDownloadPathDialog.setItems(R.array.downloadSiteOptionWithoutPlayStore, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                String[] path_value = getResources().getStringArray(R.array.downloadSiteOptionWithoutPlayStoreValue);
                                                setPreferences(getApplicationContext(), PREF_APK_DOWNLOAD_SITE, path_value[i]);
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(path_value[i]));
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                            }
                                        });
                                        apkDownloadPathDialog.show();
                                    }
                                });
                        alertDialog.setNegativeButton(getStringWithLocale(R.string.dialog_cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // None
                                    }
                                });
                        AlertDialog alert = alertDialog.create();
                        alert.setIcon(R.mipmap.ic_launcher);
                        alert.setTitle(getStringWithLocale(R.string.sa_checkupdate_dialogtitle));
                        alert.show();
                    }
                } else {
                    showToast(getApplicationContext(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Context context = getApplicationContext();
                if (context != null && KcaUtils.checkOnline(context)) {
                    showToast(context, getStringWithLocale(R.string.sa_checkupdate_servererror), Toast.LENGTH_LONG);
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "version_check", "", "", t.getMessage());
                }
            }
        });
    }

    private void downloadGameData() {
        final Call<String> down_gamedata = downloader.getGameData("recent");
        down_gamedata.enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                JsonObject response_data = new JsonObject();
                try {
                    if (response.body() != null) {
                        response_data = new JsonParser().parse(response.body()).getAsJsonObject();
                        String kca_version = KcaUtils.getStringPreferences(getApplicationContext(), PREF_KCA_VERSION);
                        String server_kca_version = response.headers().get("X-Api-Version");
                        Log.e("KCA", "api_version: " + server_kca_version);
                        if (kca_version == null || compareVersion(server_kca_version, kca_version)) {
                            dbHelper.putValue(DB_KEY_STARTDATA, response_data.toString());
                            KcaApiData.getKcGameData(response_data.getAsJsonObject("api_data"));
                            KcaUtils.setPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION, server_kca_version);
                            KcaApiData.setDataLoadTriggered();
                            showToast(getApplicationContext(),
                                    getStringWithLocale(R.string.sa_getupdate_finished),
                                    Toast.LENGTH_LONG);
                        } else {
                            showToast(getApplicationContext(),
                                    getStringWithLocale(R.string.kca_toast_inconsistent_data),
                                    Toast.LENGTH_LONG);
                        }
                    }
                } catch (Exception e) {
                    showToast(getApplicationContext(),
                            "Error: not valid data.",
                            Toast.LENGTH_LONG);
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "fairy_queue", "", "", getStringFromException(e));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    showToast(getApplicationContext(),
                            KcaUtils.format(getStringWithLocale(R.string.sa_getupdate_servererror), t.getMessage()),
                            Toast.LENGTH_LONG);
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "fairy_queue", "", "", t.getMessage());
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean hasUsageStatPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager)
                context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

}