package com.antest1.kcanotify;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.kcanotify.InitStartActivity.ACTION_RESET;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE_START;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_DOWN_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_UPDATE_SERVER;
import static com.antest1.kcanotify.KcaResCheckItemAdpater.RESCHK_KEY;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class UpdateCheckActivity extends AppCompatActivity {
    public static final int LOAD_DELAY = 500;

    Toolbar toolbar;
    static Gson gson = new Gson();
    String latest_gamedata_version = "";
    List<JsonObject> gamedata_info = new ArrayList<>();
    List<JsonObject> resource_info = new ArrayList<>();
    List<String> resource_target;
    ListView data_list, resource_list;
    KcaResCheckItemAdpater gamedata_adapter = new KcaResCheckItemAdpater();
    KcaResCheckItemAdpater resource_adapter = new KcaResCheckItemAdpater();
    Button gamedata_chk, resource_chk, resource_downall;
    TextView gamedata_server;
    TextView gamedata_load, resource_load;
    MaterialSwitch checkstart_chkbox;
    MaterialSwitch resource_reset;

    JsonArray fairy_queue = new JsonArray();
    boolean main_flag = false;
    int checked = -1;

    KcaDBHelper dbHelper;
    UpdateHandler handler;
    public KcaDownloader downloader;
    Type listType = new TypeToken<ArrayList<JsonObject>>(){}.getType();

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    private String getVersionString(int current, int latest) {
        return KcaUtils.format("CURRENT: %d | LATEST: %d", current, latest);
    }

    private String getVersionString(String current, String latest) {
        return KcaUtils.format("CURRENT: %s | LATEST: %s", current, latest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescheck);

        Intent intent = this.getIntent();
        if (intent != null && intent.getExtras() != null) {
            main_flag = intent.getExtras().getBoolean("main_flag", false);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.setting_menu_kand_title_game_data_down));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);

        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        PRDownloader.initialize(getApplicationContext());

        handler = new UpdateHandler(this);
        gamedata_adapter.setHandler(handler);
        resource_adapter.setHandler(handler);

        resource_target = getResourcesFilter();

        checkstart_chkbox = findViewById(R.id.reschk_checkatstart);
        checkstart_chkbox.setChecked(getBooleanPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START));
        checkstart_chkbox.setOnCheckedChangeListener((buttonView, isChecked)
                -> setPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START, isChecked));

        resource_reset = findViewById(R.id.reschk_reset);
        resource_reset.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(UpdateCheckActivity.this);
                alertDialog.setMessage(getString(R.string.download_reset_message));
                alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok),
                        (dialog, which) -> {
                            dbHelper.clearResVer();
                            setPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION, 0);
                            Intent mainIntent = new Intent(this, InitStartActivity.class);
                            mainIntent.putExtra(ACTION_RESET, true);
                            startActivity(mainIntent);
                            finish();
                        });
                alertDialog.setNegativeButton(getStringWithLocale(R.string.dialog_cancel),
                        (dialog, which) -> {
                            resource_reset.setChecked(false);
                            dialog.dismiss();
                        });
                AlertDialog alert = alertDialog.create();
                alert.setIcon(R.mipmap.ic_launcher);
                alert.show();
            }
        });

        data_list = findViewById(R.id.gamedata_list);
        resource_list = findViewById(R.id.resources_list);

        data_list.setAdapter(gamedata_adapter);
        resource_list.setAdapter(resource_adapter);

        gamedata_load = findViewById(R.id.gamedata_loading);
        resource_load = findViewById(R.id.resources_loading);

        gamedata_chk = findViewById(R.id.gamedata_updatecheck);
        resource_chk = findViewById(R.id.resources_updatecheck);
        resource_downall = findViewById(R.id.resources_downloadall);

        gamedata_chk.setOnClickListener(v -> checkVersionUpdate());
        resource_chk.setOnClickListener(v -> checkResourceUpdate());
        resource_downall.setOnClickListener(v -> downloadAllResources());
        resource_downall.setVisibility(View.GONE);


        gamedata_server = findViewById(R.id.gamedata_server);
        gamedata_server.setText(getStringWithLocale(R.string.action_server));
        gamedata_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int initValue = checked;
                String[] listItems = getResources().getStringArray(R.array.ServerLocation);
                String[] listEntry = getResources().getStringArray(R.array.ServerLocationValue);
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(UpdateCheckActivity.this);
                mBuilder.setTitle(getStringWithLocale(R.string.setting_menu_app_title_updatecheckserver));
                String currentServer = getStringPreferences(getApplicationContext(), PREF_UPDATE_SERVER);
                for (int i = 0; i < listEntry.length; i++) if (currentServer.equals(listEntry[i])) {
                    checked = i;
                    break;
                }

                mBuilder.setSingleChoiceItems(listItems, checked, (dialog, which) -> {
                    checked = which;
                });
                mBuilder.setPositiveButton(getStringWithLocale(R.string.dialog_ok), (dialog, which) -> {
                    Log.e("KCA", "selected: " + checked);
                    if (checked != -1) {
                        String selectedServer = listEntry[checked];
                        setPreferences(getApplicationContext(), PREF_UPDATE_SERVER, selectedServer);
                    }
                });
                mBuilder.setNegativeButton(getStringWithLocale(R.string.dialog_cancel), ((dialog, which) -> {
                    checked = initValue;
                }));

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });

        checkVersionUpdate();
        checkResourceUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        loadTranslationData(getApplicationContext(), true);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (main_flag) {
                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(mainIntent);
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (main_flag) {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
            finish();
        }  else {
            super.onBackPressed();
        }
    }

    private void checkVersionUpdate() {
        gamedata_load.setVisibility(View.VISIBLE);
        data_list.setVisibility(View.GONE);

        gamedata_info.clear();
        final Call<String> load_version = downloader.getRecentVersion();

        if (!KcaUtils.checkOnline(getApplicationContext())) {
            gamedata_load.setText("No Internet Connection.");
        } else {
            load_version.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    try {
                        String ver_result = response.body();
                        JsonObject ver_data = gson.fromJson(ver_result, JsonObject.class);
                        latest_gamedata_version = ver_data.get("data_version").getAsString();

                        JsonObject gamedata = new JsonObject();
                        gamedata.addProperty("name", "api_start2");
                        gamedata.addProperty("desc", "kancolle game data for kcanotify");
                        String current_gd_v = getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
                        String latest_gd_v = latest_gamedata_version;
                        gamedata.addProperty("version", latest_gd_v);
                        gamedata.addProperty("version_str", getVersionString(current_gd_v, latest_gd_v));
                        gamedata.addProperty("highlight", !KcaUtils.compareVersion(current_gd_v, latest_gd_v));
                        gamedata.addProperty("url", "call_kcadata_download");
                        gamedata_info.clear();
                        gamedata_info.add(gamedata);
                        gamedata_adapter.setContext(getApplicationContext());
                        gamedata_adapter.setListItem(gamedata_info);
                        gamedata_adapter.notifyDataSetChanged();

                        gamedata_load.setVisibility(View.GONE);
                        data_list.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        gamedata_load.setText("Error: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    gamedata_load.setText("Error: " + t);
                }
            });
        }
    }

    private void checkResourceUpdate() {
        resource_load.setVisibility(View.VISIBLE);
        resource_list.setVisibility(View.GONE);

        resource_info.clear();
        final Call<String> load_resource = downloader.getResourceList();

        if (!KcaUtils.checkOnline(getApplicationContext())) {
            resource_load.setText("No Internet Connection.");
        } else {
            load_resource.enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    int num_count = 0;
                    try {
                        resource_info = new ArrayList<>();
                        List<JsonObject> res_result = gson.fromJson(response.body(), listType);
                        if (res_result == null) {
                            return;
                        } else {
                            for (int i = 0; i < res_result.size(); i++) {
                                JsonObject item = res_result.get(i).getAsJsonObject();
                                String name = item.get("name").getAsString();
                                if (!isInResourcesFilter(name)) continue;
                                String desc = "download " + name;
                                item.addProperty("desc", desc);
                                int current_res_v = dbHelper.getResVer(name);
                                int latest_res_v = item.get("version").getAsInt();
                                item.addProperty("version_str", getVersionString(current_res_v, latest_res_v));
                                item.addProperty("highlight", current_res_v < latest_res_v);
                                if (current_res_v < latest_res_v) num_count += 1;
                                resource_info.add(item);
                            }

                            resource_adapter.setContext(getApplicationContext());
                            resource_adapter.setListItem(resource_info);

                            resource_adapter.notifyDataSetChanged();

                            resource_load.setVisibility(View.GONE);
                            resource_list.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        resource_load.setText("Error: " + e.getMessage());
                    } finally {
                        resource_downall.setVisibility(num_count > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    resource_load.setText("Error: " + t);
                }
            });

        }
    }

    private void downloadAllResources() {
        for (int i = 0; i < resource_info.size(); i++) {
            JsonObject item = resource_info.get(i);
            boolean highlight = item.get("highlight").getAsBoolean();
            if (highlight) {
                String url = item.get("url").getAsString();
                String name = item.get("name").getAsString();
                int version = item.get("version").getAsInt();
                downloadFile(url, name, version);
            }
        }
    }


    private static class UpdateHandler extends Handler {
        private final WeakReference<UpdateCheckActivity> mActivity;

        UpdateHandler(UpdateCheckActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            UpdateCheckActivity activity = mActivity.get();
            if (activity != null) {
                Bundle bundle = msg.getData();
                String data = bundle.getString(RESCHK_KEY);
                activity.handleUpdateMessage(data);
            }
        }
    }

    public void handleUpdateMessage(String data) {
        JsonObject obj = gson.fromJson(data, JsonObject.class);
        String name = obj.get("name").getAsString();
        String url = obj.get("url").getAsString();
        if (url.equals("call_kcadata_download")) {
            downloadGameData();
        } else {
            int version = obj.get("version").getAsInt();
            downloadFile(url, name, version);
        }
    }

    private void downloadGameData() {
        final Call<String> down_gamedata = downloader.getGameData("recent");
        down_gamedata.enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                JsonObject response_data;
                try {
                    if (response.body() != null) {
                        response_data = JsonParser.parseString(response.body()).getAsJsonObject();
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

                            JsonObject gamedata = gamedata_info.get(0);
                            String latest_gd_v = latest_gamedata_version;
                            gamedata.addProperty("version", latest_gd_v);
                            gamedata.addProperty("version_str", getVersionString(server_kca_version, latest_gd_v));
                            gamedata.addProperty("highlight", !KcaUtils.compareVersion(server_kca_version, latest_gd_v));
                            gamedata_adapter.setContext(getApplicationContext());
                            gamedata_adapter.setListItem(gamedata_info);
                            gamedata_adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    getStringWithLocale(R.string.kca_toast_inconsistent_data),
                                    Toast.LENGTH_LONG).show();;
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: not valid data.",
                            Toast.LENGTH_LONG).show();;
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "fairy_queue", "", "", getStringFromException(e));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            KcaUtils.format(getStringWithLocale(R.string.sa_getupdate_servererror), t.getMessage()),
                            Toast.LENGTH_LONG).show();;
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "fairy_queue", "", "", t.getMessage());
                }
            }
        });
    }

    private void downloadFile(String url, String name, int version) {
        long timestamp = System.currentTimeMillis() / 1000;
        final File root_dir = getDir("data", Context.MODE_PRIVATE);
        final File data = new File(root_dir, name);
        if (data.exists()) data.delete();

        PRDownloader.download(KcaUtils.format("%s?t=%d", url, timestamp), root_dir.getPath(), name)
                .build()
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        new DataSaveTask(UpdateCheckActivity.this).execute(name, String.valueOf(version));
                        if (name.equals("icon_info.json")) {
                            Toast.makeText(getApplicationContext(), "Download Completed: " + name + "\nRetrieving Fairy Images..", Toast.LENGTH_LONG).show();
                            final Handler handler1 = new Handler();
                            handler1.postDelayed(() -> new KcaFairyDownloader().execute(), LOAD_DELAY);
                        }
                        int num_count = 0;
                        for (int i = 0; i < resource_info.size(); i++) {
                            JsonObject item = resource_info.get(i).getAsJsonObject();
                            if (item.get("name").getAsString().equals(name)) {
                                int latest_res_v = item.get("version").getAsInt();
                                item.addProperty("version_str", getVersionString(version, latest_res_v));
                                item.addProperty("highlight", version < latest_res_v);
                                if (version < latest_res_v) num_count += 1;
                            }
                        }
                        resource_adapter.setListItem(resource_info);
                        resource_adapter.notifyDataSetChanged();
                        resource_downall.setVisibility(num_count > 0 ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(Error error) {
                        Toast.makeText(getApplicationContext(), "Error when downloading " + name, Toast.LENGTH_LONG).show();
                    }
                });
    }

    public List<String> getResourcesFilter() {
        List<String> filter = new ArrayList<>();
        AssetManager am = getAssets();
        try {
            AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) am.open("resources_config.json");
            byte[] bytes = ByteStreams.toByteArray(ais);
            JsonObject config = JsonParser.parseString(new String(bytes)).getAsJsonObject();
            for (JsonElement item : config.getAsJsonArray("common")) {
                filter.add(item.getAsString());
            }
            for (JsonElement item : config.getAsJsonObject("langpacks").getAsJsonArray(LocaleUtils.getResourceLocaleCode())) {
                filter.add(item.getAsString());
            }
        } catch (IOException e) {
            // do nothing
        }
        return filter;
    }

    public boolean isInResourcesFilter(String key) {
        return resource_target.size() > 0 && resource_target.contains(key);
    }

    private class KcaFairyDownloader extends AsyncTask<Integer, Integer, Integer> {
        boolean fairy_wait = false;
        int totalFiles = 0;
        int successedFiles = 0;
        int failedFiles = 0;
        int download_result = 0;

        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(UpdateCheckActivity.this);
            mProgressDialog.setMessage(getStringWithLocale(R.string.download_progress));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setProgressNumberFormat("%1d file(s)");

            if (!isFinishing()) {
                mProgressDialog.show();
            }
        }

        private void workFinished()  {
            if (isFinishing()) return;
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (totalFiles == 0) {
                Toast.makeText(getApplicationContext(), "No file to download", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), KcaUtils.format("Download Completed: %d", successedFiles), Toast.LENGTH_LONG).show();
                setPreferences(getApplicationContext(), PREF_FAIRY_DOWN_FLAG, true);
            }
        }

        private void downloadFile(String folder, String url, String name, int version) {
            final File root_dir = cw.getDir(folder, Context.MODE_PRIVATE);
            final File data = new File(root_dir, name);
            if (data.exists()) data.delete();

            PRDownloader.download(url, root_dir.getPath(), name)
                    .build()
                    .start(new OnDownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            successedFiles += 1;
                            if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                            dbHelper.putResVer(name, version);
                        }

                        @Override
                        public void onError(Error error) {
                            failedFiles += 1;
                            if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                        }
                    });
        }

        private void startDownloadProgress() {
            fairy_wait = true;
            publishProgress(0);
            totalFiles = fairy_queue.size();
            mProgressDialog.setMax(totalFiles);
            for (int i = 0; i < fairy_queue.size(); i++) {
                JsonObject item = fairy_queue.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                String url = item.get("url").getAsString();
                downloadFile("fairy", url, name, 0);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(!fairy_wait);
            mProgressDialog.setMax(totalFiles);
            mProgressDialog.setProgress(progress[0]);
            if (totalFiles == 0 || progress[0] == totalFiles) {
                workFinished();
            }
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            JsonArray fairy_data = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "icon_info.json", dbHelper);
            for (int i = 0; i < fairy_data.size(); i++) {
                JsonObject fairy_item = fairy_data.get(i).getAsJsonObject();
                if (!KcaUtils.checkFairyImageFileFromStorage(getApplicationContext(), fairy_item.get("name").getAsString())) {
                    fairy_queue.add(fairy_item);
                }
            }
            startDownloadProgress();
            return download_result;
        }

        @Override
        protected void onPostExecute(Integer s) {
            super.onPostExecute(s);

        }
    }

    static class DataSaveTask extends AsyncTask<String, Void, Boolean> {
        private final WeakReference<Activity> weakReference;
        String name = "";

        DataSaveTask(Activity myActivity) {
            this.weakReference = new WeakReference<>(myActivity);
        }

        @Override
        public Boolean doInBackground(String... params) {
            Activity activity = this.weakReference.get();
            if (activity == null || activity.isFinishing()
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                // weakReference is no longer valid, don't do anything!
                return false;
            }
            name = params[0];
            KcaDBHelper dbHelper = new KcaDBHelper(activity.getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            dbHelper.putResVer(params[0], Long.parseLong(params[1]));
            dbHelper.close();
            return true;
        }

        @Override
        public void onPostExecute(Boolean result) {
            // Re-acquire a strong reference to the weakReference, and verify
            // that it still exists and is active.
            Activity activity = this.weakReference.get();
            if (activity == null || activity.isFinishing()
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                return;
            }
            Context context = activity.getApplicationContext();
            if (result) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Download Completed: " + name, Toast.LENGTH_LONG).show();
                    }
                }, LOAD_DELAY);
            } else {
                Toast.makeText(context, "failed to read data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
