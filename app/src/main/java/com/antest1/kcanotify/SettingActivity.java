package com.antest1.kcanotify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_S2_CACHE_FILENAME;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_CN_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_EXPVIEW_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DOWNLOAD_DATA;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_OVERLAY_SETTING;
import static com.antest1.kcanotify.KcaService.kca_version;
import static com.antest1.kcanotify.KcaUtils.getLocaleInArray;


public class SettingActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static Handler sHandler;
    static Gson gson = new Gson();
    public static String currentVersion = BuildConfig.VERSION_NAME;
    public static final String TAG = "KCA";
    public static final int REQUEST_OVERLAY_PERMISSION = 2;

    public SettingActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_settings));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
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
                            new getRecentVersion(getActivity()).execute();
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
                if (key.equals(PREF_OVERLAY_SETTING)) {
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                showObtainingPermissionOverlayWindow();
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.sa_overlay_under_m), Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }
                    });
                }
                if (key.equals(PREF_KCA_LANGUAGE)) {
                    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            LocaleUtils.setLocale(new Locale((String) newValue));
                            Toast.makeText(getActivity(), getString(R.string.sa_language_changed), Toast.LENGTH_LONG).show();
                            return true;
                        }
                    });
                }
                if (pref instanceof ListPreference) {
                    ListPreference etp = (ListPreference) pref;
                    pref.setSummary(etp.getEntry());
                }

            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        public void showObtainingPermissionOverlayWindow() {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_OVERLAY_PERMISSION) {
                if (Settings.canDrawOverlays(getActivity())) {
                    Toast.makeText(getActivity(), getString(R.string.sa_overlay_ok), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.sa_overlay_no), Toast.LENGTH_SHORT).show();
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
            if (pref instanceof ListPreference) {
                ListPreference etp = (ListPreference) pref;
                pref.setSummary(etp.getEntry());
            }

        }
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    private static class getRecentVersion extends AsyncTask<Context, String, String> {
        Activity context;

        public getRecentVersion(Activity ctx) {
            context = ctx;
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
                return response.body().string();
            } catch (IOException e1) {
                e1.printStackTrace();
                return "IOException_Check";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(context.getApplicationContext(), context.getString(R.string.sa_checkupdate_nodataerror), Toast.LENGTH_LONG).show();
            } else {
                Log.e("KCA", "Received: " + result);
                JsonObject jsonDataObj = new JsonParser().parse(result).getAsJsonObject();
                if (jsonDataObj.has("version")) {
                    String recentVersion = jsonDataObj.get("version").getAsString();
                    if (recentVersion.equals(currentVersion)) {
                        Toast.makeText(context.getApplicationContext(),
                                String.format(context.getString(R.string.sa_checkupdate_latest), currentVersion),
                                Toast.LENGTH_LONG).show();
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setMessage(String.format(context.getString(R.string.sa_checkupdate_hasupdate), recentVersion));
                        alertDialog.setPositiveButton(context.getString(R.string.dialog_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String downloadUrl = context.getString(R.string.app_download_link);
                                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                        context.startActivity(i);
                                    }
                                });
                        alertDialog.setNegativeButton(context.getString(R.string.dialog_cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // None
                                    }
                                });
                        AlertDialog alert = alertDialog.create();
                        alert.setIcon(R.mipmap.ic_launcher);
                        alert.setTitle(context.getString(R.string.sa_checkupdate_dialogtitle));
                        alert.show();
                    }
                } else {
                    Toast.makeText(context.getApplicationContext(),
                            context.getString(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static class getKcaStart2Data extends AsyncTask<Context, String, String> {
        Context context;
        String result = null;

        public getKcaStart2Data(Context ctx) {
            context = ctx;
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
            String dataUrl;
            if (kca_version == null) {
                dataUrl = String.format(context.getString(R.string.api_start2_recent_version_link));
            } else {
                dataUrl = String.format(context.getString(R.string.api_start2_version_link), kca_version);
            }

            AjaxCallback<String> cb = new AjaxCallback<String>() {
                @Override
                public void callback(String url, String data, AjaxStatus status) {
                    try {
                        if (status.getCode() == HttpResponseStatus.OK.code()) {
                            KcaUtils.writeCacheData(context, data.getBytes(), KCANOTIFY_S2_CACHE_FILENAME);
                            KcaApiData.getKcGameData(gson.fromJson(data, JsonObject.class).getAsJsonObject("api_data"));
                            if (kca_version == null) {
                                kca_version = status.getHeader("X-Api-Version");
                            }
                            KcaUtils.setPreferences(context, "kca_version", kca_version);
                            KcaApiData.setDataLoadTriggered();
                            Toast.makeText(context.getApplicationContext(),
                                    context.getString(R.string.sa_getupdate_finished),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context.getApplicationContext(),
                                    String.format(context.getString(R.string.sa_getupdate_servererror), String.valueOf(status.getCode())),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e1) {
                        Toast.makeText(context.getApplicationContext(),
                                context.getString(R.string.sa_getupdate_ioexceptionerror),
                                Toast.LENGTH_LONG).show();
                        //Log.e(TAG, "I/O Error");
                    }

                }
            };
            AQuery aq = new AQuery(context);
            cb.header("Referer", "app:/KCA/");
            cb.header("Content-Type", "application/x-www-form-urlencoded");
            //Log.e(TAG, dataUrl);
            aq.ajax(dataUrl, String.class, cb);
            return null;
        }

    }
}
