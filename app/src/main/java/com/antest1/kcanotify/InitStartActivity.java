package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import retrofit2.Call;

import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_MAIN;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class InitStartActivity extends Activity {
    public final static int DELAY_TIME = 250;
    public final static String FAIRY_INFO_FILENAME = "icon_info.json";

    boolean download_finished = false;
    Handler handler = new Handler();
    Runnable r;

    KcaDBHelper dbHelper;
    ProgressDialog mProgressDialog;
    PowerManager pm;
    PowerManager.WakeLock mWakeLock;
    TextView appname, appversion;

    int requiredFiles = 0;
    JsonArray download_data = new JsonArray();
    int fairy_flag, new_resversion;
    JsonObject fairy_info = new JsonObject();
    Fetch fetch;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_download);
        Log.e("KCA-DA", "created");
        // instantiate it within the onCreate method
        mProgressDialog = new ProgressDialog(InitStartActivity.this);
        mProgressDialog.setMessage("Downloading..");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressNumberFormat("%1d");

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, InitStartActivity.class.getName());

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(getApplicationContext())
                .setDownloadConcurrentLimit(12)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        appname = findViewById(R.id.app_title);
        appname.setText(getStringWithLocale(R.string.app_name));

        appversion = findViewById(R.id.app_version);
        appversion.setText(getString(R.string.app_version));

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setDefaultPreferences();

        int setDefaultGameDataResult = KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
        if (setDefaultGameDataResult != 1) {
            Toast.makeText(this, "error loading game data", Toast.LENGTH_LONG).show();
        }

        if (!KcaUtils.checkOnline(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Cannot check the update", Toast.LENGTH_LONG).show();
            startMainActivity();
        } else {
            Handler mHandler = new Handler();
            Thread t = new Thread(() -> {
                KcaDownloader downloader = KcaUtils.getInfoDownloader(getApplicationContext());

                String currentVersion = BuildConfig.VERSION_NAME;
                String currentDataVersion = getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
                int currentKcaResVersion = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION));
                final Call<String> rv_data = downloader.getRecentVersion();
                String response = getResultFromCall(rv_data);

                boolean allgreen_flag = true;
                new_resversion = -1;
                fairy_flag = 0;

                JsonObject response_data = new JsonObject();
                try {
                    if (response != null) {
                        response_data = new JsonParser().parse(response).getAsJsonObject();
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
                }

                Log.e("KCA", response_data.toString());
                if (response_data.has("version")) {
                    String recentVersion = response_data.get("version").getAsString();
                    if (!compareVersion(currentVersion, recentVersion)) { // True if latest
                        Toast.makeText(getApplicationContext(), "update required", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                if (response_data.has("data_version")) {
                    String recentVersion = response_data.get("data_version").getAsString();
                    if (!compareVersion(currentDataVersion, recentVersion)) { // True if latest
                        allgreen_flag = false;
                        Toast.makeText(getApplicationContext(), "data update required", Toast.LENGTH_LONG).show();
                    }
                }

                if (response_data.has("kcadata_version")) {
                    new_resversion = response_data.get("kcadata_version").getAsInt();
                    if (new_resversion > currentKcaResVersion) {
                        allgreen_flag = false;
                        JsonArray resource_list = new JsonArray();
                        final Call<String> load_resource = downloader.getResourceList();
                        String result = getResultFromCall(load_resource);
                        if (result.length() > 0) resource_list = new JsonParser().parse(result).getAsJsonArray();
                        for (int i = 0; i < resource_list.size(); i++) {
                            JsonObject item = resource_list.get(i).getAsJsonObject();
                            String name = item.get("name").getAsString();
                            int version = item.get("version").getAsInt();
                            if (dbHelper.getResVer(name) < version) {
                                if (name.equals(FAIRY_INFO_FILENAME)) {
                                    fairy_flag = 1;
                                    fairy_info = item;
                                } else {
                                    download_data.add(item);
                                }
                            }
                        }
                        Log.e("KCA", download_data.toString());
                    }
                }

                if (allgreen_flag) {
                    startMainActivity();
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            final KcaResourceDownloader downloadTask = new KcaResourceDownloader();
                            downloadTask.execute(new_resversion, fairy_flag);
                        }
                    });
                }
            });
            t.start();
        }
    }

    private void startMainActivity() {
        r = () -> {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };
        handler.postDelayed(r, DELAY_TIME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (download_finished && r != null) handler.postDelayed(r, DELAY_TIME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (download_finished && r != null) handler.removeCallbacks(r);
    }

    @Override
    protected void onDestroy() {
        Log.e("KCA-DA", "destroy");
        super.onDestroy();
    }

    private String getResultFromCall(Call<String> call) {
        KcaRequestThread thread = new KcaRequestThread(call);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return thread.getResult();
    }

    @SuppressLint("ApplySharedPref")
    private void setDefaultPreferences() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        for (String prefKey : PREFS_LIST) {
            if (!pref.contains(prefKey)) {
                Log.e("KCA", prefKey + " pref add");
                String value = SettingActivity.getDefaultValue(prefKey);
                if (value.startsWith("R.string")) {
                    editor.putString(prefKey, getString(getId(value.replace("R.string.", ""), R.string.class)));
                } else if (value.startsWith("boolean_")) {
                    editor.putBoolean(prefKey, Boolean.parseBoolean(value.replace("boolean_", "")));
                } else {
                    editor.putString(prefKey, value);
                }
            }
        }
        editor.commit();
    }

    private class KcaResourceDownloader extends AsyncTask<Integer, Integer, String> {
        boolean fairy_wait = false;
        int update_version = 0;
        int totalFiles = 0;
        int successedFiles = 0;
        int failedFiles = 0;

        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            download_finished = false;
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        private void workFinished()  {
            mWakeLock.release();
            if (totalFiles == successedFiles) {
                setPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION, update_version);
            }

            download_finished = true;
            mProgressDialog.dismiss();

            if (fairy_wait) {
                final File root_dir = cw.getDir("fairy", Context.MODE_PRIVATE);
                final File data = new File(root_dir, "noti_icon_0.png");
                while(true) { // wait for first fairy be loaded
                    try {
                        if (data.exists()) break;
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            startMainActivity();
        }

        private void downloadFile(String folder, String url, String name) {
            final File root_dir = cw.getDir(folder, Context.MODE_PRIVATE);
            final File data = new File(root_dir, name);

            final Request request = new Request(url, data.getPath());
            fetch.enqueue(request, updatedRequest -> {}, error -> {
                failedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            });
        }

        FetchListener fetchFairyListListener = new FetchListener() {
            @Override
            public void onDeleted(Download download) {}

            @Override
            public void onRemoved(Download download) {}

            @Override
            public void onResumed(Download download) {}

            @Override
            public void onPaused(Download download) {}

            @Override
            public void onProgress(Download download, long l, long l1) {}

            @Override
            public void onQueued(Download download, boolean b) {}

            @Override
            public void onCancelled(Download download) {
                startDownloadProgress();
            }

            @Override
            public void onError(Download download) {
                startDownloadProgress();
            }

            @Override
            public void onCompleted(@NotNull Download download) {
                JsonArray fairy_data = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), FAIRY_INFO_FILENAME);
                for (int i = 0; i < fairy_data.size(); i++) {
                    JsonObject fairy_item = fairy_data.get(i).getAsJsonObject();
                    fairy_item.addProperty("is_fairy", true);
                    if (!KcaUtils.checkFairyImageInStorage(getApplicationContext(), fairy_item.get("name").getAsString())) {
                        download_data.add(fairy_item);
                    }
                }
                startDownloadProgress();
            }
        };

        FetchListener fetchDownloadListener = new FetchListener() {
            @Override
            public void onDeleted(Download download) {}

            @Override
            public void onRemoved(Download download) {}

            @Override
            public void onResumed(Download download) {}

            @Override
            public void onPaused(Download download) {}

            @Override
            public void onProgress(Download download, long l, long l1) {}

            @Override
            public void onQueued(Download download, boolean b) {}

            @Override
            public void onCancelled(Download download) {}

            @Override
            public void onError(Download download) {
                failedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            }

            @Override
            public void onCompleted(@NotNull Download download) {
                successedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            }
        };

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(!fairy_wait);
            mProgressDialog.setMax(totalFiles);
            mProgressDialog.setProgress(progress[0]);

            if (progress[0] == totalFiles) {
                workFinished();
            }
        }

        @Override
        protected String doInBackground(Integer... params) {
            update_version = params[0];
            fairy_wait = params[1] == 1;
            if (fairy_wait) {
                final File root_dir = cw.getDir("data", Context.MODE_PRIVATE);
                final File myImageFile = new File(root_dir, FAIRY_INFO_FILENAME);
                final Request request = new Request(fairy_info.get("url").getAsString(), myImageFile.getPath());
                fetch.addListener(fetchFairyListListener);
                fetch.enqueue(request, updatedRequest -> {}, error -> {
                    startDownloadProgress();
                });
            } else {
                startDownloadProgress();
            }
            //downloadFairyIcon();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        private void startDownloadProgress() {
            fetch.removeListener(fetchFairyListListener);
            fetch.addListener(fetchDownloadListener);
            totalFiles = download_data.size();
            mProgressDialog.setMax(totalFiles);
            for (int i = 0; i < download_data.size(); i++) {
                JsonObject item = download_data.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                String url = item.get("url").getAsString();
                if (item.has("is_fairy")) {
                    downloadFile("fairy", url, name);
                } else {
                    downloadFile("data", url, name);
                    int version = item.get("version").getAsInt();
                    dbHelper.putResVer(name, version);
                }
            }
        }
    }
}

