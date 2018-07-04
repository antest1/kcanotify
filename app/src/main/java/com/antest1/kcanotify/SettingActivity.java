package com.antest1.kcanotify;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;

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
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_FILTERLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STAR_CHECKED;
import static com.antest1.kcanotify.KcaConstants.PREF_ALARM_DELAY;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE;
import static com.antest1.kcanotify.KcaConstants.PREF_DISABLE_CUSTOMTOAST;
import static com.antest1.kcanotify.KcaConstants.PREF_EQUIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_AUTOHIDE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_NOTI_LONGCLICK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_ACTIVATE_DROPLOG;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_ACTIVATE_RESLOG;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_BATTLENODE_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_BATTLEVIEW_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DOWNLOAD_DATA;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_TYPE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_MORALE_MIN;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_AKASHI;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_MORALE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_NOTIFYATSVCOFF;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_QUEST_FAIRY_GLOW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_RINGTONE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_SOUND_KIND;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_V_HD;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_V_NS;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_QUESTVIEW_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SET_PRIORITY;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_LAST_UPDATE_CHECK;
import static com.antest1.kcanotify.KcaConstants.PREF_OPENDB_API_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_OVERLAY_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_POIDB_API_USE;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SORTKEY;
import static com.antest1.kcanotify.KcaConstants.PREF_SHOWDROP_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_SHOW_CONSTRSHIP_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_SNIFFER_MODE;
import static com.antest1.kcanotify.KcaConstants.PREF_TIMER_WIDGET_STATE;
import static com.antest1.kcanotify.KcaConstants.PREF_UPDATE_SERVER;
import static com.antest1.kcanotify.KcaConstants.PREF_VIEW_YLOC;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_BYPASS_ADDRESS;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;
import static com.antest1.kcanotify.KcaConstants.SNIFFER_PASSIVE;
import static com.antest1.kcanotify.KcaConstants.VPN_STOP_REASON;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;


public class SettingActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static Handler sHandler;
    static Gson gson = new Gson();
    KcaDBHelper dbHelper;
    public KcaDownloader downloader;
    public static String currentVersion = BuildConfig.VERSION_NAME;
    public static final String TAG = "KCA";
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_USAGESTAT_PERMISSION = 3;
    public static RingtoneManager ringtoneManager;
    public static String silentText;

    public SettingActivity() {
        LocaleUtils.updateConfig(this);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_settings));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        silentText = getString(R.string.settings_string_silent);

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, new PrefsFragment()).commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.closeDatabase();
        super.onDestroy();
    }

    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        public SettingActivity activity;
        public Context context;

        public String getStringWithLocale(int id) {
            return KcaUtils.getStringWithLocale(getActivity().getApplicationContext(), getActivity().getBaseContext(), id);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity = (SettingActivity) getActivity();
            context = getActivity().getApplicationContext();
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
                            activity.checkRecentVersion();
                            return false;
                        }
                    });
                }
                if (key.equals(PREF_KCA_DOWNLOAD_DATA)) {
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            activity.downloadGameData();
                            return false;
                        }
                    });
                }

                if (key.equals(PREF_FAIRY_AUTOHIDE)) {
                    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
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
                                context.startService(intent);
                                return true;
                            }
                        }
                    });
                }

                if (key.equals(PREF_OVERLAY_SETTING)) {
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                showObtainingPermissionOverlayWindow();
                            } else {
                                Toast.makeText(context, getStringWithLocale(R.string.sa_overlay_under_m), Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }
                    });
                }

                if (key.equals(PREF_SNIFFER_MODE)) {
                    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                            String val = (String) newValue;
                            if (Integer.parseInt(val) == SNIFFER_PASSIVE && prefs.getBoolean(PREF_VPN_ENABLED, false)) {
                                KcaVpnService.stop(VPN_STOP_REASON, getActivity());
                                prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                            }
                            return true;
                        }
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
                            Toast.makeText(context, getStringWithLocale(R.string.sa_language_changed), Toast.LENGTH_LONG).show();
                            return true;
                        }
                    });
                }

                if (key.equals(PREF_ALARM_DELAY)) {
                    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
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
                        }
                    });
                }

                if (key.equals(PREF_KCA_MORALE_MIN)) {
                    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            String new_val = ((String) newValue);
                            if (new_val.length() == 0) return false;
                            int value = Integer.parseInt(new_val);
                            if (value > 100) {
                                Toast.makeText(context, "value must be in 0~100", Toast.LENGTH_LONG).show();
                                return false;
                            }
                            KcaMoraleInfo.setMinMorale(value);
                            return true;
                        }
                    });
                }

                if (key.equals(PREF_VPN_BYPASS_ADDRESS)) {
                    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (newValue.equals(""))
                                return true;
                            Pattern cidrPattern = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])/(\\d|[1-2]\\d|3[0-2])$");
                            String[] cidrs = ((String) newValue).split(",");
                            for (String cidr : cidrs) {
                                if (!cidrPattern.matcher(cidr.trim()).find()) {
                                    Toast.makeText(context, getString(R.string.sa_bypass_list_invalid), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                            }
                            return true;
                        }
                    });
                }

                if (pref instanceof RingtonePreference) {
                    String uri = pref.getSharedPreferences().getString(key, "");
                    if (uri.length() == 0) {
                        pref.setSummary(silentText);
                    } else {
                        Uri ringtoneUri = Uri.parse(uri);
                        getActivity().grantUriPermission(BuildConfig.APPLICATION_ID, ringtoneUri, FLAG_GRANT_READ_URI_PERMISSION);
                        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                        if (ringtone == null) {
                            Toast.makeText(context,
                                    getStringWithLocale(R.string.ma_permission_external_denied),
                                    Toast.LENGTH_LONG).show();
                            pref.setSummary(silentText);
                        } else {
                            String name = ringtone.getTitle(context);
                            pref.setSummary(name);
                        }
                    }
                } else if (pref instanceof ListPreference) {
                    ListPreference etp = (ListPreference) pref;
                    pref.setSummary(etp.getEntry());
                } else if (pref instanceof EditTextPreference) {
                    EditTextPreference etp = (EditTextPreference) pref;
                    pref.setSummary(etp.getText());
                }
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            context = getActivity().getApplicationContext();
        }

        @TargetApi(Build.VERSION_CODES.M)
        public void showObtainingPermissionOverlayWindow() {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
            if(intent.resolveActivity(context.getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            } else {
                try {
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS), REQUEST_OVERLAY_PERMISSION);
                } finally {
                    Toast.makeText(context, getStringWithLocale(R.string.sa_overlay_appearontop), Toast.LENGTH_LONG).show();
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
            if (requestCode == REQUEST_OVERLAY_PERMISSION) {
                int delay = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? 0 : 1000;
                new Handler().postDelayed(() -> {
                    if (Settings.canDrawOverlays(getActivity())) {
                        Toast.makeText(getActivity(), getStringWithLocale(R.string.sa_overlay_ok), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), getStringWithLocale(R.string.sa_overlay_no), Toast.LENGTH_SHORT).show();
                    }
                }, delay);
            } else if (requestCode == REQUEST_USAGESTAT_PERMISSION) {
                if(hasUsageStatPermission(getActivity().getApplicationContext())) {
                    Toast.makeText(getActivity(), getStringWithLocale(R.string.sa_usagestat_ok), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getStringWithLocale(R.string.sa_usagestat_no), Toast.LENGTH_SHORT).show();
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
                    getActivity().grantUriPermission(BuildConfig.APPLICATION_ID, ringtoneUri, FLAG_GRANT_READ_URI_PERMISSION);
                    Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                    if (ringtone == null) {
                        Toast.makeText(context,
                                getStringWithLocale(R.string.ma_permission_external_denied),
                                Toast.LENGTH_LONG).show();
                        pref.setSummary(silentText);
                    } else {
                        String name = ringtone.getTitle(context);
                        pref.setSummary(name);
                    }
                }
                Log.e("KCA-S", "sdf");
                Intent aIntent = new Intent(getActivity(), KcaAlarmService.class);
                aIntent.setAction(REFRESH_CHANNEL);
                aIntent.putExtra("uri", uri);
                context.startService(aIntent);
            } else if (pref instanceof ListPreference) {
                ListPreference etp = (ListPreference) pref;
                pref.setSummary(etp.getEntry());
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
            }
        }
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }


    private void checkRecentVersion() {
        String currentVersion = BuildConfig.VERSION_NAME;
        final Call<String> rv_data = downloader.getRecentVersion();
        rv_data.enqueue(new Callback<String>() {
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
                        Toast.makeText(getApplicationContext(),
                                KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_latest), currentVersion),
                                Toast.LENGTH_LONG).show();
                    } else if (!isFinishing()) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingActivity.this);
                        alertDialog.setMessage(KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_hasupdate), recentVersion));
                        alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String downloadUrl = getStringPreferences(getApplicationContext(), PREF_APK_DOWNLOAD_SITE);
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        } else if (downloadUrl.contains(getStringWithLocale(R.string.app_download_link_playstore))) {
                                            Toast.makeText(getApplicationContext(), "Google Play Store not found", Toast.LENGTH_LONG).show();
                                            AlertDialog.Builder apkDownloadPathDialog = new AlertDialog.Builder(SettingActivity.this);
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
                    Toast.makeText(getApplicationContext(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "version_check", "", "", t.getMessage());
                }
            }
        });
    }

    private void downloadGameData() {
        final Call<String> down_gamedata = downloader.getGameData("recent");
        down_gamedata.enqueue(new Callback<String>() {
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
                            Toast.makeText(getApplicationContext(),
                                    getStringWithLocale(R.string.sa_getupdate_finished),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    getStringWithLocale(R.string.kca_toast_inconsistent_data),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: not valid data.",
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "download_data", "", "", getStringFromException(e));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            KcaUtils.format(getStringWithLocale(R.string.sa_getupdate_servererror), t.getMessage()),
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "download_data", "", "", t.getMessage());
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean hasUsageStatPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager)
                context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static String getDefaultValue(String prefKey) {
        switch (prefKey) {
            case PREF_KCA_SEEK_CN:
                return String.valueOf(SEEK_33CN1);
            case PREF_OPENDB_API_USE:
            case PREF_POIDB_API_USE:
            case PREF_AKASHI_STAR_CHECKED:
            case PREF_KCA_SET_PRIORITY:
            case PREF_DISABLE_CUSTOMTOAST:
            case PREF_FAIRY_AUTOHIDE:
            case PREF_KCA_NOTI_AKASHI:
            case PREF_SHOW_CONSTRSHIP_NAME:
                return "boolean_false";
            case PREF_KCA_EXP_VIEW:
            case PREF_KCA_NOTI_NOTIFYATSVCOFF:
            case PREF_KCA_NOTI_DOCK:
            case PREF_KCA_NOTI_EXP:
            case PREF_KCA_NOTI_MORALE:
            case PREF_KCA_BATTLEVIEW_USE:
            case PREF_KCA_BATTLENODE_USE:
            case PREF_KCA_QUESTVIEW_USE:
            case PREF_KCA_NOTI_V_HD:
            case PREF_KCA_NOTI_V_NS:
            case PREF_SHOWDROP_SETTING:
            case PREF_FAIRY_NOTI_LONGCLICK:
            case PREF_KCA_NOTI_QUEST_FAIRY_GLOW:
            case PREF_KCA_ACTIVATE_DROPLOG:
            case PREF_KCA_ACTIVATE_RESLOG:
                return "boolean_true";
            case PREF_KCA_LANGUAGE:
                return "R.string.default_locale";
            case PREF_KCA_NOTI_SOUND_KIND:
                return "R.string.sound_kind_value_vibrate";
            case PREF_KCA_NOTI_RINGTONE:
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();
            case PREF_APK_DOWNLOAD_SITE:
                return "R.string.app_download_link_playstore";
            case PREF_AKASHI_STARLIST:
            case PREF_AKASHI_FILTERLIST:
            case PREF_SHIPINFO_FILTCOND:
                return "|";
            case PREF_SHIPINFO_SORTKEY:
                return "|1,true|";
            case PREF_FAIRY_ICON:
            case PREF_KCA_EXP_TYPE:
            case PREF_VIEW_YLOC:
            case PREF_LAST_UPDATE_CHECK:
            case PREF_SNIFFER_MODE:
            case PREF_KCARESOURCE_VERSION:
                return "0";
            case PREF_ALARM_DELAY:
                return "61";
            case PREF_KCA_MORALE_MIN:
                return "40";
            case PREF_EQUIPINFO_FILTCOND:
                return "all";
            case PREF_KCA_DATA_VERSION:
                return "R.string.default_gamedata_version";
            case PREF_UPDATE_SERVER:
                return "R.string.server_nova";
            case PREF_TIMER_WIDGET_STATE:
                return "{}";
            default:
                return "";
        }
    }
}
