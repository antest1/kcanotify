package com.antest1.kcanotify;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.antest1.kcanotify.KcaConstants.*;


public class SettingActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static Handler sHandler;
    static Gson gson = new Gson();
    public static String currentVersion = BuildConfig.VERSION_NAME;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_setting);
       toolbar = (Toolbar) findViewById(R.id.toolbar);
       setSupportActionBar(toolbar);
       getSupportActionBar().setTitle(getResources().getString(R.string.action_settings));
       getSupportActionBar().setDisplayHomeAsUpEnabled(true);

       getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new PrefsFragment(), null).commit();
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
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            addPreferencesFromResource(R.xml.pref_settings);
            Map<String, ?> allEntries = getPreferenceManager().getSharedPreferences().getAll();
            for(String key: allEntries.keySet()) {
                Preference pref = findPreference(key);
                if (key.equals("check_update")) {
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new getRecentVersion(getActivity()).execute();
                            return false;
                        }
                    });
                }
                if (pref instanceof ListPreference) {
                    ListPreference etp = (ListPreference) pref;
                    pref.setSummary(etp.getEntry());
                }

            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (sHandler != null) {
                JsonObject dmpData = new JsonObject();
                String kca_url = "";
                Bundle bundle = new Bundle();
                switch(key) {
                    case "kca_seek_cn":
                        kca_url = KCA_API_PREP_CN_CHANGED;
                        break;
                    default:
                        break;
                }
                bundle.putString("url", kca_url);
                bundle.putString("data", gson.toJson(dmpData));
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
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

        private void serverTempStart() {
            if(!KcaProxyServer.is_on()) KcaProxyServer.start(null);
        }

        private void serverTempStop() {
            if(KcaProxyServer.handler == null) KcaProxyServer.stop();
        }

        @Override
        protected void onPreExecute() {
            serverTempStart();
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

            String checkUrl = String.format("http://antest.hol.es/kcanotify/v.php");
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
                Toast.makeText(context.getApplicationContext(), "업데이트 확인에 문제가 발생했습니다.", Toast.LENGTH_LONG).show();
                serverTempStop();
            } else {
                JsonObject jsonDataObj = new JsonParser().parse(result).getAsJsonObject();
                if (jsonDataObj.has("version")) {
                    String recentVersion = jsonDataObj.get("version").getAsString();
                    if (recentVersion.equals(currentVersion)) {
                        Toast.makeText(context.getApplicationContext(), String.format("최신 버전입니다(%s).", currentVersion), Toast.LENGTH_LONG).show();
                        serverTempStop();
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setMessage(String.format("업데이트가 있습니다(%s).\n다운로드하시겠습니까?", recentVersion));
                        alertDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String downloadUrl = "http://bit.ly/2hNln5o";
                                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                context.startActivity(i);
                            }
                        });
                        alertDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                serverTempStop();
                            }
                        });
                        AlertDialog alert = alertDialog.create();
                        alert.setIcon(R.mipmap.ic_launcher);
                        alert.setTitle("최신 업데이트 확인");
                        alert.show();
                    }
                } else {
                    Toast.makeText(context.getApplicationContext(), "업데이트 서버 데이터 오류", Toast.LENGTH_LONG).show();
                    serverTempStop();
                }
            }
        }
    }
}
