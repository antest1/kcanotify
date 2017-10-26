package com.antest1.kcanotify;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.category;
import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_CN_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_EXPVIEW_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_LANGUAGE_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_PRIORITY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_ACCESSIBILITY_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DOWNLOAD_DATA;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_MOVETOAPPINFO;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_RINGTONE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_SOUND_KIND;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SET_PRIORITY;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_OVERLAY_SETTING;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_BYPASS_ADDRESS;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class SettingActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static Handler sHandler;
    static Gson gson = new Gson();
    public static String currentVersion = BuildConfig.VERSION_NAME;
    public static final String TAG = "KCA";
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
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

    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        public Context context;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
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
                            new getRecentVersion(getActivity(), true).execute();
                            return false;
                        }
                    });
                }
                if (key.equals(PREF_KCA_DOWNLOAD_DATA)) {
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new getKcaStart2Data(getActivity()).execute();
                            return false;
                        }
                    });
                }
                if (key.equals(PREF_ACCESSIBILITY_SETTING)) {
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Log.e("KCA", PREF_ACCESSIBILITY_SETTING);
                            showObtainingPermissionAccessibility();
                            return false;
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
                                Toast.makeText(context, context.getString(R.string.sa_overlay_under_m), Toast.LENGTH_SHORT).show();
                            }
                            return false;
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
                            Toast.makeText(context, context.getString(R.string.sa_language_changed), Toast.LENGTH_LONG).show();
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (key.equals(PREF_KCA_NOTI_SOUND_KIND) || key.equals(PREF_KCA_NOTI_RINGTONE)) {
                        PreferenceCategory category = (PreferenceCategory) findPreference("pref_noti_category");
                        category.removePreference(findPreference(key));
                    } else if (key.equals(PREF_KCA_NOTI_MOVETOAPPINFO)) {
                        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                startActivity(i);
                                return false;
                            }
                        });
                    }
                } else {
                    if (key.equals(PREF_KCA_NOTI_MOVETOAPPINFO)) {
                        PreferenceCategory category = (PreferenceCategory) findPreference("pref_noti_category");
                        category.removePreference(findPreference(key));
                    }
                }

                if (pref instanceof RingtonePreference) {
                    String uri = pref.getSharedPreferences().getString(key, "");
                    if (uri.length() == 0) {
                        pref.setSummary(silentText);
                    } else {
                        Uri ringtoneUri = Uri.parse(uri);
                        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                        if (ringtone == null) {
                            Toast.makeText(context,
                                    context.getString(R.string.ma_permission_external_denied),
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
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }

        public void showObtainingPermissionAccessibility() {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_OVERLAY_PERMISSION) {
                if (Settings.canDrawOverlays(getActivity())) {
                    Toast.makeText(getActivity(), context.getString(R.string.sa_overlay_ok), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), context.getString(R.string.sa_overlay_no), Toast.LENGTH_SHORT).show();
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
                    Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                    if (ringtone == null) {
                        Toast.makeText(context,
                                context.getString(R.string.ma_permission_external_denied),
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
            }
        }
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static class getRecentVersion extends AsyncTask<Context, String, String> {
        Activity activity;
        Context context;
        boolean toastflag;

        public getRecentVersion(Activity a, boolean tf) {
            activity = a;
            context = a.getApplicationContext();
            toastflag = tf;
        }

        public String getStringWithLocale(int id) {
            return KcaUtils.getStringWithLocale(activity.getApplicationContext(), activity.getBaseContext(), id);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... params) {
            String content = null;
            try {
                content = executeClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content;
        }

        public String executeClient() {
            final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");
            OkHttpClient client = new OkHttpClient.Builder().build();

            String checkUrl = String.format(context.getString(R.string.kcanotify_checkversion_link));
            Request.Builder builder = new Request.Builder().url(checkUrl).get();
            builder.addHeader("Referer", "app:/KCA/");
            builder.addHeader("Content-Type", "application/x-www-form-urlencoded");
            Request request = builder.build();

            try {
                Response response = client.newCall(request).execute();
                return response.body().string().trim();
            } catch (IOException e1) {
                e1.printStackTrace();
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(context, getStringWithLocale(R.string.sa_checkupdate_nodataerror), Toast.LENGTH_LONG).show();
            } else if (result.length() > 0) {
                Log.e("KCA", "Received: " + result);
                JsonObject jsonDataObj = new JsonObject();
                try {
                    StringReader reader = new StringReader(result.trim());
                    JsonReader jsonReader = new JsonReader(reader);
                    jsonReader.setLenient(true);
                    jsonDataObj = new JsonParser().parse(jsonReader).getAsJsonObject();
                } catch (Exception e) {
                    // data from server is broken?
                }
                if (jsonDataObj.has("version")) {
                    String recentVersion = jsonDataObj.get("version").getAsString();
                    if (compareVersion(currentVersion, recentVersion)) { // True if latest
                        if (toastflag) {
                            Toast.makeText(context,
                                    String.format(getStringWithLocale(R.string.sa_checkupdate_latest), currentVersion),
                                    Toast.LENGTH_LONG).show();
                        }
                    } else if (!activity.isFinishing()) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                        alertDialog.setMessage(String.format(getStringWithLocale(R.string.sa_checkupdate_hasupdate), recentVersion));
                        alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String downloadUrl = getStringPreferences(context, PREF_APK_DOWNLOAD_SITE);
                                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(i);
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
                    Toast.makeText(context,
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static class getKcaStart2Data extends AsyncTask<Context, String, String> {
        final String SUCCESS = "S";
        final String FAILURE = "F";
        final String ERROR = "E";
        final String NODATA = "N";
        String error_msg = "";

        Activity activity;
        Context context;
        KcaDBHelper dbHelper;

        public getKcaStart2Data(Activity a) {
            activity = a;
            context = a.getApplicationContext();
            dbHelper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        }

        public String getStringWithLocale(int id) {
            return KcaUtils.getStringWithLocale(activity.getApplicationContext(), activity.getBaseContext(), id);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... params) {
            String content = null;
            try {
                content = executeClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content;
        }

        public String executeClient() {
            String dataUrl = String.format(context.getString(R.string.api_start2_recent_version_link));

            OkHttpClient client = new OkHttpClient.Builder().build();
            Request.Builder builder = new Request.Builder().url(dataUrl).get();
            builder.addHeader("Referer", "app:/KCA/");
            builder.addHeader("Content-Type", "application/x-www-form-urlencoded");
            error_msg = "";
            Request request = builder.build();
            String result;
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == org.apache.http.HttpStatus.SC_OK) {
                    String kca_version = KcaUtils.getStringPreferences(context, PREF_KCA_VERSION);
                    String server_kca_version = response.header("X-Api-Version");
                    if (kca_version == null || compareVersion(server_kca_version, kca_version)) {
                        String data = response.body().string().trim();
                        dbHelper.putValue(DB_KEY_STARTDATA, data);
                        KcaApiData.getKcGameData(gson.fromJson(data, JsonObject.class).getAsJsonObject("api_data"));
                        KcaUtils.setPreferences(context, PREF_KCA_DATA_VERSION, server_kca_version);
                        KcaApiData.setDataLoadTriggered();
                        result = SUCCESS;
                    } else {
                        result = NODATA;
                    }
                } else {
                    error_msg = response.message();
                    result = FAILURE;
                }
            } catch (IOException e) {
                error_msg = getStringFromException(e);
                result = ERROR;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                switch (s) {
                    case SUCCESS:
                        Toast.makeText(context,
                                getStringWithLocale(R.string.sa_getupdate_finished),
                                Toast.LENGTH_LONG).show();
                        break;
                    case FAILURE:
                        if (error_msg == null) error_msg = "null";
                        Toast.makeText(context,
                                String.format(getStringWithLocale(R.string.sa_getupdate_servererror), error_msg),
                                Toast.LENGTH_LONG).show();
                        break;
                    case ERROR:
                    case NODATA:
                        // temoporal message: this situation occured in case of no file in server.
                        // this will be reverted after server issue fixed.
                        Toast.makeText(context,
                                getStringWithLocale(R.string.kca_toast_inconsistent_data),
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
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
}
