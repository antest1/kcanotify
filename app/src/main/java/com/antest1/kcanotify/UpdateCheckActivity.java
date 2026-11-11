package com.antest1.kcanotify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.kcanotify.InitStartActivity.ACTION_RESET;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE_START;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_UPDATE_SERVER;
import static com.antest1.kcanotify.KcaResCheckItemAdpater.RESCHK_KEY;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class UpdateCheckActivity extends BaseActivity {
    public static final int LOAD_DELAY = 500;
    private static final String DOWNLOAD_SUFFIX = ".download";
    private static final ExecutorService fileWorker = Executors.newSingleThreadExecutor();
    private static final Set<String> downloadingFiles = Collections.synchronizedSet(new HashSet<>());

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

    boolean main_flag = false;
    int checked = -1;

    KcaDBHelper dbHelper;
    UpdateHandler handler;
    KcaDownloader downloader;
    KcaFairyDownloader fairyDownloader;
    Type listType = new TypeToken<ArrayList<JsonObject>>(){}.getType();

    private String getVersionString(int current, int latest) {
        if (current == latest) {
            return KcaUtils.format("UP-TO-DATE: %d", latest);
        } else {
            return KcaUtils.format("CURRENT: %d | LATEST: %d", current, latest);
        }
    }

    private String getVersionString(String current, String latest) {
        if (current != null && current.equalsIgnoreCase(latest)) {
            return KcaUtils.format("UP-TO-DATE: %s", latest);
        } else {
            return KcaUtils.format("CURRENT: %s | LATEST: %s", current, latest);
        }
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
        getSupportActionBar().setTitle(getString(R.string.setting_menu_kand_title_game_data_down));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);

        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        fairyDownloader = new KcaFairyDownloader(this);
        PRDownloader.initialize(getApplicationContext());

        handler = new UpdateHandler(this);
        gamedata_adapter.setHandler(handler);
        resource_adapter.setHandler(handler);

        resource_target = KcaUtils.getResourceFilter(getApplicationContext());

        checkstart_chkbox = findViewById(R.id.reschk_checkatstart);
        checkstart_chkbox.setChecked(getBooleanPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START));
        checkstart_chkbox.setOnCheckedChangeListener((buttonView, isChecked)
                -> setPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START, isChecked));

        resource_reset = findViewById(R.id.reschk_reset);
        resource_reset.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(UpdateCheckActivity.this);
                alertDialog.setMessage(getString(R.string.download_reset_message));
                alertDialog.setPositiveButton(getString(R.string.dialog_ok),
                        (dialog, which) -> {
                            dbHelper.clearResVer();
                            setPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION, 0);
                            Intent mainIntent = new Intent(this, InitStartActivity.class);
                            mainIntent.putExtra(ACTION_RESET, true);
                            startActivity(mainIntent);
                            finish();
                        });
                alertDialog.setNegativeButton(getString(R.string.dialog_cancel),
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
        gamedata_server.setText(getString(R.string.action_server));
        gamedata_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int initValue = checked;
                String[] listItems = getResources().getStringArray(R.array.ServerLocation);
                String[] listEntry = getResources().getStringArray(R.array.ServerLocationValue);
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(UpdateCheckActivity.this);
                mBuilder.setTitle(getString(R.string.setting_menu_app_title_updatecheckserver));
                String currentServer = getStringPreferences(getApplicationContext(), PREF_UPDATE_SERVER);
                for (int i = 0; i < listEntry.length; i++) if (currentServer.equals(listEntry[i])) {
                    checked = i;
                    break;
                }

                mBuilder.setSingleChoiceItems(listItems, checked, (dialog, which) -> {
                    checked = which;
                });
                mBuilder.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
                    Log.e("KCA", "selected: " + checked);
                    if (checked != -1) {
                        String selectedServer = listEntry[checked];
                        setPreferences(getApplicationContext(), PREF_UPDATE_SERVER, selectedServer);
                    }
                });
                mBuilder.setNegativeButton(getString(R.string.dialog_cancel), ((dialog, which) -> {
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
                    String info_version;
                    try {
                        JsonObject ver_data = gson.fromJson(response.body(), JsonObject.class);
                        info_version = ver_data.get("data_version").getAsString();
                    } catch (Exception e) {
                        gamedata_load.setText("Error: " + e.getMessage());
                        return;
                    }
                    final Context context = getApplicationContext();
                    // the update server itself is the source of truth (KCAINFO can lag behind on the CDN);
                    // a version-only update (same content) is recorded here without a download
                    new Thread(() -> {
                        KcaGameDataChecker.Info latest = KcaGameDataChecker.syncVersion(context, downloader);
                        String latest_version = latest == null ? info_version
                                : KcaGameDataChecker.newerVersion(info_version, latest.version);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            latest_gamedata_version = latest_version;
                            showGameDataItem();
                        });
                    }).start();
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    gamedata_load.setText("Error: " + t);
                }
            });
        }
    }

    private void showGameDataItem() {
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
        // ask for the exact version shown as latest, so the recorded version always matches the data
        final String target_version = latest_gamedata_version;
        final Call<String> down_gamedata = downloader.getGameData(target_version);
        down_gamedata.enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                if (response.code() == 404) {
                    // this version is not on the server yet
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.kca_toast_inconsistent_data),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String server_version = response.headers().get(KcaGameDataChecker.HEADER_VERSION);
                    Log.e("KCA", "api_version: " + server_version);
                    if (!response.isSuccessful() || response.body() == null
                            || server_version == null || !server_version.trim().equals(target_version)) {
                        throw new IllegalStateException("unexpected response: " + response.code() + " " + server_version);
                    }
                    String start_data = response.body();
                    JsonObject api_data = JsonParser.parseString(start_data).getAsJsonObject().getAsJsonObject("api_data");
                    if (api_data == null) throw new IllegalStateException("no api_data");

                    // save the data and its version together, then apply it
                    dbHelper.putValue(DB_KEY_STARTDATA, start_data);
                    KcaUtils.setPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION, target_version);
                    KcaApiData.getKcGameData(api_data);
                    KcaApiData.setDataLoadTriggered();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.sa_getupdate_finished),
                            Toast.LENGTH_LONG).show();
                    showGameDataItem();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: not valid data.",
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "gamedata_download", target_version, "", getStringFromException(e));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            KcaUtils.format(getString(R.string.sa_getupdate_servererror), t.getMessage()),
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "gamedata_download", target_version, "", t.getMessage());
                }
            }
        });
    }

    private void downloadFile(String url, String name, int version) {
        if (!downloadingFiles.add(name)) return; // already downloading
        final Context context = getApplicationContext();
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        long timestamp = System.currentTimeMillis() / 1000;
        final File root_dir = getDir("data", Context.MODE_PRIVATE);
        // download next to the file and replace it only after the download is valid
        final String tempName = name + DOWNLOAD_SUFFIX;
        new File(root_dir, tempName).delete();

        PRDownloader.download(KcaUtils.format("%s?t=%d", url, timestamp), root_dir.getPath(), tempName)
                .build()
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        fileWorker.execute(() -> {
                            boolean saved = saveDownloadedFile(context, root_dir, name, tempName, version);
                            downloadingFiles.remove(name);
                            mainHandler.post(() -> {
                                if (saved) {
                                    KcaApiData.reloadResource(context, name);
                                    Toast.makeText(context, "Download Completed: " + name, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(context, "failed to read data", Toast.LENGTH_SHORT).show();
                                }
                                if (!isFinishing() && !isDestroyed()) onResourceDownloaded(name, version, saved);
                            });
                        });
                    }

                    @Override
                    public void onError(Error error) {
                        new File(root_dir, tempName).delete();
                        downloadingFiles.remove(name);
                        Toast.makeText(context, "Error when downloading " + name, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onResourceDownloaded(String name, int version, boolean saved) {
        if (!saved) return;
        if (name.equals("icon_info.json")) {
            Toast.makeText(getApplicationContext(), "Download Completed: " + name + "\nRetrieving Fairy Images..", Toast.LENGTH_LONG).show();
            final Handler handler1 = new Handler(Looper.getMainLooper());
            handler1.postDelayed(() -> fairyDownloader.run(false), LOAD_DELAY);
        }
        int num_count = 0;
        for (int i = 0; i < resource_info.size(); i++) {
            JsonObject item = resource_info.get(i).getAsJsonObject();
            int latest_res_v = item.get("version").getAsInt();
            if (item.get("name").getAsString().equals(name)) {
                item.addProperty("version_str", getVersionString(version, latest_res_v));
                item.addProperty("highlight", version < latest_res_v);
            }
            if (item.get("highlight").getAsBoolean()) num_count += 1;
        }
        resource_adapter.setListItem(resource_info);
        resource_adapter.notifyDataSetChanged();
        resource_downall.setVisibility(num_count > 0 ? View.VISIBLE : View.GONE);
    }

    // Replace the resource file with the download only when it is valid JSON, then record its version.
    // Runs whether or not this screen is still open, so the file and its version stay in sync.
    private static boolean saveDownloadedFile(Context context, File dir, String name, String tempName, int version) {
        File temp = new File(dir, tempName);
        try (Reader reader = new FileReader(temp)) {
            JsonParser.parseReader(reader);
        } catch (Exception e) {
            temp.delete();
            return false;
        }
        if (!temp.renameTo(new File(dir, name))) {
            temp.delete();
            return false;
        }
        KcaDBHelper helper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        helper.putResVer(name, version);
        helper.close();
        return true;
    }

    public boolean isInResourcesFilter(String key) {
        return !resource_target.isEmpty() && resource_target.contains(key);
    }
}
