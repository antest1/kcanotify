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
    KcaDataUpdateManager updateManager;
    // Last-Modified of list.json, to detect resource files served from an older CDN copy
    String resource_list_last_modified = null;
    Type listType = new TypeToken<ArrayList<JsonObject>>(){}.getType();

    private final KcaDataUpdateManager.Listener updateListener = new KcaDataUpdateManager.Listener() {
        @Override
        public void onItemChanged(String key, KcaDataUpdateManager.State state, int progress) {
            onUpdateItemChanged(key, state, progress);
        }

        @Override
        public void onFinished(KcaDataUpdateManager.Summary summary) {
            String message = KcaDataUpdateManager.getSummaryText(getApplicationContext(), summary);
            if (message != null) Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    };

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
        updateManager = KcaDataUpdateManager.getInstance(this);
        updateManager.addListener(updateListener);

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
                            resetDownloadedData(getApplicationContext(), dbHelper);
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
        // downloads keep going; the manager shows one result toast when no screen is listening
        updateManager.removeListener(updateListener);
        super.onDestroy();
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
                        resource_list_last_modified = response.headers().get("Last-Modified");
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
                updateManager.enqueueResource(name, url, version, resource_list_last_modified);
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
            // the exact version shown as latest, so the recorded version always matches the data
            updateManager.enqueueGameData(latest_gamedata_version);
        } else {
            int version = obj.get("version").getAsInt();
            updateManager.enqueueResource(name, url, version, resource_list_last_modified);
        }
    }

    // shows the download state in the row of the item (texts are replaced with the new screen design)
    private void onUpdateItemChanged(String key, KcaDataUpdateManager.State state, int progress) {
        if (KcaDataUpdateManager.GAME_DATA_KEY.equals(key)) {
            // DONE: shows the new version; null (cancelled before it started): shows the versions again
            if (state == null || state == KcaDataUpdateManager.State.DONE) {
                if (!gamedata_info.isEmpty()) showGameDataItem();
            } else if (!gamedata_info.isEmpty()) {
                gamedata_info.get(0).addProperty("version_str", getStateString(state, progress));
                gamedata_adapter.notifyDataSetChanged();
            }
            return;
        }
        int num_count = 0;
        for (JsonObject item : resource_info) {
            if (item.get("name").getAsString().equals(key)) {
                int latest_res_v = item.get("version").getAsInt();
                if (state == KcaDataUpdateManager.State.DONE) {
                    item.addProperty("version_str", getVersionString(latest_res_v, latest_res_v));
                    item.addProperty("highlight", false);
                } else if (state != null) {
                    item.addProperty("version_str", getStateString(state, progress));
                }
            }
            if (item.get("highlight").getAsBoolean()) num_count += 1;
        }
        resource_adapter.notifyDataSetChanged();
        resource_downall.setVisibility(num_count > 0 ? View.VISIBLE : View.GONE);

        if (state == KcaDataUpdateManager.State.DONE && key.equals("icon_info.json")) {
            Toast.makeText(getApplicationContext(), "Retrieving Fairy Images..", Toast.LENGTH_LONG).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) fairyDownloader.run(false);
            }, LOAD_DELAY);
        }
    }

    private static String getStateString(KcaDataUpdateManager.State state, int progress) {
        switch (state) {
            case QUEUED:
                return "QUEUED";
            case DOWNLOADING:
                return progress >= 0 ? KcaUtils.format("DOWNLOADING %d%%", progress) : "DOWNLOADING";
            case NOT_READY:
                return "NOT READY ON SERVER - TRY LATER";
            case FAILED:
                return "FAILED";
            default:
                return "";
        }
    }

    // Remove downloaded resources and game data, so that InitStartActivity (ACTION_RESET) restores
    // the bundled ones. Fairy images (separate folder), logs and settings are kept.
    private static void resetDownloadedData(Context context, KcaDBHelper helper) {
        File[] files = context.getDir("data", Context.MODE_PRIVATE).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) file.delete();
            }
        }
        helper.clearResVer();
        setPreferences(context, PREF_KCARESOURCE_VERSION, 0);
        helper.deleteValue(DB_KEY_STARTDATA);
        setPreferences(context, PREF_KCA_DATA_VERSION, "");
    }

    public boolean isInResourcesFilter(String key) {
        return !resource_target.isEmpty() && resource_target.contains(key);
    }
}
