package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_MAIN;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_LAST_UPDATE_CHECK;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class InitStartActivity extends Activity {
    public final static int DELAY_TIME = 250;
    public final static String FAIRY_INFO_FILENAME = "icon_info.json";
    public static final int UPDATECHECK_INTERVAL_MS = 30000;
    public static final String ACTION_RESET = "ACTION_RESET";

    public final static String DOWNLOAD_TYPE_APPDATA = "appdata";
    public final static String DOWNLOAD_TYPE_GAMEDATA = "gamedata";
    public final static String DOWNLOAD_TYPE_QUESTINFO = "questinfo";
    public final static String DOWNLOAD_TYPE_SHIPINFO = "shipinfo";
    public final static String DOWNLOAD_TYPE_EQUIPINFO = "equipinfo";
    public final static String DOWNLOAD_TYPE_AKASHI = "akashi";
    public final static String DOWNLOAD_TYPE_FAIRY = "fairy";
    public final static String[] DOWNLOAD_TYPE_LIST = {
            DOWNLOAD_TYPE_APPDATA, DOWNLOAD_TYPE_GAMEDATA, DOWNLOAD_TYPE_QUESTINFO,
            DOWNLOAD_TYPE_SHIPINFO, DOWNLOAD_TYPE_EQUIPINFO, DOWNLOAD_TYPE_AKASHI,
            DOWNLOAD_TYPE_FAIRY
    };

    boolean download_finished = false;
    Handler handler = new Handler();
    Runnable r;

    KcaDBHelper dbHelper;
    KcaDownloader downloader;
    ProgressDialog mProgressDialog;
    PowerManager pm;
    PowerManager.WakeLock mWakeLock;
    TextView appname, appversion;

    boolean is_first;
    JsonArray download_data = new JsonArray();
    int fairy_flag, new_resversion;
    JsonObject fairy_info = new JsonObject();
    int fairy_list_version;
    boolean reset_flag = false;
    Fetch fetch;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_download);
        Log.e("KCA-DA", "created");

        Intent mainIntent = getIntent();
        reset_flag = mainIntent.getBooleanExtra(ACTION_RESET, false);
        // instantiate it within the onCreate method
        mProgressDialog = new ProgressDialog(InitStartActivity.this);
        mProgressDialog.setMessage(getStringWithLocale(R.string.download_progress));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressNumberFormat("%1d file(s)");

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setDefaultPreferences();

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
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

        is_first = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION)) == 0;

        int setDefaultGameDataResult = KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
        if (setDefaultGameDataResult != 1) {
            Toast.makeText(this, "error loading game data", Toast.LENGTH_LONG).show();
        }

        if (!KcaUtils.checkOnline(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Cannot check the update", Toast.LENGTH_LONG).show();
            startMainActivity();
        } else {
            handler = new Handler();
            Thread t = new Thread(() -> {
                String currentVersion = BuildConfig.VERSION_NAME;

                final Call<String> rv_data = downloader.getRecentVersion();
                String response = getResultFromCall(rv_data);
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
                        JsonObject data = response_data;
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(InitStartActivity.this);
                        alertDialog.setCancelable(false);
                        alertDialog.setMessage(KcaUtils.format(getStringWithLocale(R.string.sa_checkupdate_hasupdate), recentVersion));
                        alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                                (dialog, which) -> {
                                    String downloadUrl = getStringPreferences(getApplicationContext(), PREF_APK_DOWNLOAD_SITE);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (intent.resolveActivity(getPackageManager()) != null) {
                                        startActivity(intent);
                                    } else if (downloadUrl.contains(getStringWithLocale(R.string.app_download_link_playstore))) {
                                        Toast.makeText(getApplicationContext(), "Google Play Store not found", Toast.LENGTH_LONG).show();
                                        AlertDialog.Builder apkDownloadPathDialog = new AlertDialog.Builder(InitStartActivity.this);
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
                                (dialog, which) -> {
                                    dataCheck(data);
                                });

                        handler.post(() -> {
                            AlertDialog alert = alertDialog.create();
                            alert.setIcon(R.mipmap.ic_launcher);
                            alert.setTitle(
                                    getStringWithLocale(R.string.sa_checkupdate_dialogtitle));
                            alert.show();
                        });
                    } else {
                        dataCheck(response_data);
                    }
                }
            });
            t.start();
        }
    }



    private void dataCheck(JsonObject response_data) {
        List<String> update_text = new ArrayList<>();
        String lasttime = getStringPreferences(getApplicationContext(), PREF_LAST_UPDATE_CHECK);
        if (!is_first && !reset_flag && lasttime != null) {
            long current_time = System.currentTimeMillis();
            long last_check_time = Long.parseLong(lasttime);
            if (current_time - last_check_time <= UPDATECHECK_INTERVAL_MS) {
                startMainActivity();
                return;
            }
        }

        String currentDataVersion = getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
        int currentKcaResVersion = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION));

        if (response_data.has("data_version")) {
            String recentVersion = response_data.get("data_version").getAsString();
            if (!compareVersion(currentDataVersion, recentVersion)) { // True if latest
                JsonObject info = new JsonObject();
                info.addProperty("name", "api_start2");
                info.addProperty("url", "");
                if (!update_text.contains(DOWNLOAD_TYPE_GAMEDATA)) update_text.add(DOWNLOAD_TYPE_GAMEDATA);
                download_data.add(info);
            }
        }

        if (response_data.has("kcadata_version")) {
            new_resversion = response_data.get("kcadata_version").getAsInt();
            if (new_resversion > currentKcaResVersion) {
                JsonArray resource_list = new JsonArray();
                final Call<String> load_resource = downloader.getResourceList();
                String result = getResultFromCall(load_resource);
                if (result.length() > 0) resource_list = new JsonParser().parse(result).getAsJsonArray();
                for (int i = 0; i < resource_list.size(); i++) {
                    JsonObject item = resource_list.get(i).getAsJsonObject();
                    String name = item.get("name").getAsString();
                    int version = item.get("version").getAsInt();
                    if (reset_flag || dbHelper.getResVer(name) < version) {
                        if (name.equals(FAIRY_INFO_FILENAME)) {
                            fairy_flag = 1;
                            fairy_info = item;
                            fairy_list_version = version;
                            update_text.add(DOWNLOAD_TYPE_FAIRY);
                        } else {
                            download_data.add(item);
                            if (!update_text.contains(DOWNLOAD_TYPE_APPDATA) &&
                                    (name.contains("edges") || name.contains("expedition") || name.contains("equip_count"))) {
                                update_text.add(DOWNLOAD_TYPE_APPDATA);
                            }
                            if (!update_text.contains(DOWNLOAD_TYPE_QUESTINFO) &&
                                    (name.contains("quests-") || name.contains("quest_track"))) {
                                update_text.add(DOWNLOAD_TYPE_QUESTINFO);
                            }
                            if (!update_text.contains(DOWNLOAD_TYPE_SHIPINFO) &&
                                    (name.contains("ships-") || name.contains("stype"))) {
                                update_text.add(DOWNLOAD_TYPE_SHIPINFO);
                            }
                            if (!update_text.contains(DOWNLOAD_TYPE_AKASHI) && name.contains("akashi")) {
                                update_text.add(DOWNLOAD_TYPE_AKASHI);
                            }
                            if (!update_text.contains(DOWNLOAD_TYPE_EQUIPINFO) && name.contains("items-")) {
                                update_text.add(DOWNLOAD_TYPE_EQUIPINFO);
                            }
                        }
                    }
                }
                Log.e("KCA", download_data.toString());
            }
        }
        setPreferences(getApplicationContext(), PREF_LAST_UPDATE_CHECK, String.valueOf(System.currentTimeMillis()));
        if (download_data.size() == 0 && fairy_flag == 0) {
            startMainActivity();
        } else {
            String message = getStringWithLocale(R.string.download_description_head) + "\n\n";
            for (String s: DOWNLOAD_TYPE_LIST) {
                if (update_text.contains(s)) message = message.concat("- ").concat(getTypeText(s)).concat("\n");
            }
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(InitStartActivity.this);
            alertDialog.setTitle(getStringWithLocale(R.string.download_title));
            alertDialog.setMessage(message.trim());
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                    (dialog, which) -> {
                        final KcaResourceDownloader downloadTask = new KcaResourceDownloader();
                        downloadTask.execute(new_resversion, fairy_flag);
                    });

            alertDialog.setNegativeButton(getStringWithLocale(R.string.dialog_cancel), (dialog, which) -> {
                if (is_first) {
                    finish();
                } else {
                    startMainActivity();
                }
            });

            handler.post(() -> {
                AlertDialog alert = alertDialog.create();
                alert.setIcon(R.mipmap.ic_launcher);
                alert.show();
            });
        }
    }

    private String getTypeText(String s) {
        switch (s) {
            case DOWNLOAD_TYPE_GAMEDATA:
                return getStringWithLocale(R.string.download_gamedata);
            case DOWNLOAD_TYPE_APPDATA:
                return getStringWithLocale(R.string.download_appdata);
            case DOWNLOAD_TYPE_QUESTINFO:
                return getStringWithLocale(R.string.download_questinfo);
            case DOWNLOAD_TYPE_SHIPINFO:
                return getStringWithLocale(R.string.download_shipinfo);
            case DOWNLOAD_TYPE_EQUIPINFO:
                return getStringWithLocale(R.string.download_iteminfo);
            case DOWNLOAD_TYPE_AKASHI:
                return getStringWithLocale(R.string.download_akashi);
            case DOWNLOAD_TYPE_FAIRY:
                return getStringWithLocale(R.string.download_fairy);
            default:
                return "";
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

    private class KcaResourceDownloader extends AsyncTask<Integer, Integer, Integer> {
        boolean fairy_wait = false;
        int update_version = 0;
        int totalFiles = 0;
        int successedFiles = 0;
        int failedFiles = 0;
        int download_result = 0;

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
                String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
                String filename = "noti_icon_".concat(fairyIdValue).concat(".png");
                final File root_dir = cw.getDir("fairy", Context.MODE_PRIVATE);
                final File data = new File(root_dir, filename);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                while(true) { // wait for first fairy be loaded
                    try {
                        if (data.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(data.getPath(), options);
                            if (options.outWidth != -1 && options.outHeight != -1) {
                                break;
                            }
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            startMainActivity();
        }

        private void downloadFile(String folder, String url, String name, int version) {
            final File root_dir = cw.getDir(folder, Context.MODE_PRIVATE);
            final File data = new File(root_dir, name);
            if (data.exists()) {
                boolean deleted = data.delete();
            }

            final Request request = new Request(url, data.getPath());
            fetch.enqueue(request, updatedRequest -> {
                dbHelper.putResVer(name, version);
            }, error -> {
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
                dbHelper.putResVer(FAIRY_INFO_FILENAME, fairy_list_version);
                JsonArray fairy_data = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), FAIRY_INFO_FILENAME);
                for (int i = 0; i < fairy_data.size(); i++) {
                    JsonObject fairy_item = fairy_data.get(i).getAsJsonObject();
                    fairy_item.addProperty("is_fairy", true);
                    boolean reset = fairy_item.has("reset");
                    if (reset_flag || reset || !KcaUtils.checkFairyImageInStorage(getApplicationContext(), fairy_item.get("name").getAsString())) {
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

        private void startDownloadProgress() {
            publishProgress(0);
            fetch.removeListener(fetchFairyListListener);
            fetch.addListener(fetchDownloadListener);
            totalFiles = download_data.size();
            mProgressDialog.setMax(totalFiles);
            for (int i = 0; i < download_data.size(); i++) {
                JsonObject item = download_data.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                String url = item.get("url").getAsString();
                if (name.equals("api_start2")) {
                    downloadGameData();
                } else if (item.has("is_fairy")) {
                    downloadFile("fairy", url, name, 0);
                } else {
                    int version = item.get("version").getAsInt();
                    downloadFile("data", url, name, version);
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(!fairy_wait);
            mProgressDialog.setMax(totalFiles);
            mProgressDialog.setProgress(progress[0]);

            if (totalFiles == 0 || progress[0] == totalFiles) {
                workFinished();
            }
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            update_version = params[0];
            fairy_wait = params[1] == 1;
            if (fairy_wait) {
                final File root_dir = cw.getDir("data", Context.MODE_PRIVATE);
                final File myImageFile = new File(root_dir, FAIRY_INFO_FILENAME);
                final Request request = new Request(fairy_info.get("url").getAsString(), myImageFile.getPath());
                fetch.addListener(fetchFairyListListener);
                fetch.enqueue(request, updatedRequest -> {
                    String name = fairy_info.get("name").getAsString();
                    int version = fairy_info.get("version").getAsInt();
                    dbHelper.putResVer(name, version);
                }, error -> {
                    startDownloadProgress();
                });
            } else {
                startDownloadProgress();
            }
            //downloadFairyIcon();
            return download_result;
        }

        @Override
        protected void onPostExecute(Integer s) {
            super.onPostExecute(s);
            if (s >= 4) {
                Toast.makeText(getApplicationContext(),
                        getStringWithLocale(R.string.kca_toast_inconsistent_data),
                        Toast.LENGTH_LONG).show();
            }
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
                                successedFiles += 1;
                                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                            } else {
                                failedFiles += 1;
                                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                                download_result += 4;
                            }
                        }
                    } catch (Exception e) {
                        failedFiles += 1;
                        if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                        dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "download_data", "", "", getStringFromException(e));
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    if (KcaUtils.checkOnline(getApplicationContext())) {
                        failedFiles += 1;
                        if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                        dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "download_data", "", "", t.getMessage());
                    }
                }
            });
        }
    }
}