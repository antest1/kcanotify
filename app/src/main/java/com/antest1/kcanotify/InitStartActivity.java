package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.persistence.room.Update;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_DATALOAD;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_MAIN;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE_START;
import static com.antest1.kcanotify.KcaConstants.PREF_DATALOAD_ERROR_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_LAST_UPDATE_CHECK;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_FLAG;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_PREFIX;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class InitStartActivity extends Activity {
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

    KcaDBHelper dbHelper;
    KcaDownloader downloader;
    ProgressDialog mProgressDialog;
    TextView appname, appversion, appmessage;
    ImageView appfront;

    boolean is_destroyed = false;
    int fairy_flag, new_resversion;
    boolean reset_flag = false;
    boolean is_skipped = false;
    Fetch fetch;

    static Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<JsonObject>>(){}.getType();

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
        is_destroyed = false;

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setDefaultPreferences();

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(getApplicationContext())
                .setDownloadConcurrentLimit(24)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        KcaApiData.setDBHelper(dbHelper);

        appname = findViewById(R.id.app_title);
        appname.setText(getStringWithLocale(R.string.app_name));

        appversion = findViewById(R.id.app_version);
        appversion.setText(getString(R.string.app_version));

        appmessage = findViewById(R.id.app_message);
        appmessage.setText("");

        appfront = findViewById(R.id.app_icon);
        int img_id = (int) (Math.random() * 4);
        appfront.setImageResource(R.mipmap.main_image);

        int fairy_id = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
        if (!FAIRY_SPECIAL_FLAG && fairy_id >= FAIRY_SPECIAL_PREFIX) {
            setPreferences(getApplicationContext(), PREF_FAIRY_ICON, 0);
        }

        // Initialize resources
        setPreferences(getApplicationContext(), PREF_DATALOAD_ERROR_FLAG, false);
        if (Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION)) == 0) {
            AssetManager am = getAssets();
            try {
                int latest_version = -1;
                AssetManager.AssetInputStream ais =
                        (AssetManager.AssetInputStream) am.open("list.json");
                byte[] bytes = ByteStreams.toByteArray(ais);
                List<JsonObject> data_list = gson.fromJson(new String(bytes), listType);
                for (int i = 0; i < data_list.size(); i++) {
                    String name = data_list.get(i).get("name").getAsString();
                    int version = data_list.get(i).get("version").getAsInt();
                    if (dbHelper.getResVer(name) == -1) {
                        appmessage.setText("Loading Resources from Assets...");
                        final File root_dir = getDir("data", Context.MODE_PRIVATE);
                        final File data = new File(root_dir, name);
                        if (data.exists()) data.delete();
                        FileOutputStream stream = new FileOutputStream(data);
                        try {
                            AssetManager.AssetInputStream ais_item =
                                    (AssetManager.AssetInputStream) am.open(name);
                            byte[] data_bytes = ByteStreams.toByteArray(ais_item);
                            stream.write(data_bytes);
                            dbHelper.putResVer(name, version);
                            if (version > latest_version) latest_version = version;
                        } finally {
                            stream.close();
                        }
                    }
                    setPreferences(getApplicationContext(), PREF_KCARESOURCE_VERSION, latest_version);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                if (dbHelper != null) dbHelper.recordErrorLog(ERROR_TYPE_DATALOAD, "list.json", "init", "1", getStringFromException(e1));
            }
        }

        boolean all_passed = KcaUtils.validateResourceFiles(getApplicationContext(), dbHelper);
        if (all_passed) {
            setPreferences(getApplicationContext(), PREF_DATALOAD_ERROR_FLAG, false);
            findViewById(R.id.init_layout).setOnClickListener(v -> {
                if (!reset_flag) {
                    startMainActivity(false);
                    is_skipped = true;
                }
            });
        }

        appmessage.setText("Loading Translation Data...");
        loadTranslationData(getApplicationContext());

        appmessage.setText("Loading KanColle Game Data...");
        int setDefaultGameDataResult = KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
        if (setDefaultGameDataResult != 1) {
            Toast.makeText(this, "error loading game data", Toast.LENGTH_LONG).show();
        }

        if (!KcaUtils.checkOnline(getApplicationContext()) || !getBooleanPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START)) {
            startMainActivity(true);
        } else {
            appmessage.setText("Checking Updates...");
            handler = new Handler();
            Thread t = new Thread(() -> {
                if (is_skipped) return;
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
                            if (!is_skipped) {
                                AlertDialog alert = alertDialog.create();
                                alert.setIcon(R.mipmap.ic_launcher);
                                alert.setTitle(
                                        getStringWithLocale(R.string.sa_checkupdate_dialogtitle));
                                alert.show();
                            }
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
        boolean latest_flag = true;
        List<String> update_text = new ArrayList<>();
        String currentDataVersion = getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
        int currentKcaResVersion = dbHelper.getTotalResVer();

        if (response_data.has("data_version")) {
            String recentVersion = response_data.get("data_version").getAsString();
            latest_flag = compareVersion(currentDataVersion, recentVersion);
        }

        if (response_data.has("kcadata_version")) {
            new_resversion = response_data.get("kcadata_version").getAsInt();
            latest_flag = latest_flag && new_resversion <= currentKcaResVersion;
        }

        setPreferences(getApplicationContext(), PREF_LAST_UPDATE_CHECK, String.valueOf(System.currentTimeMillis()));
        if (latest_flag) {
            startMainActivity(true);
        } else {
            String message = getStringWithLocale(R.string.download_description_head);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(InitStartActivity.this);
            alertDialog.setTitle(getStringWithLocale(R.string.download_title));
            alertDialog.setMessage(message.trim());
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton(getStringWithLocale(R.string.dialog_ok), (dialog, which) -> {
                startUpdateActivity();
             });

            alertDialog.setNegativeButton(getStringWithLocale(R.string.dialog_cancel), (dialog, which) -> {
                startMainActivity(true);
            });

            handler.post(() -> {
                if (!is_destroyed) {
                    AlertDialog alert = alertDialog.create();
                    alert.setIcon(R.mipmap.ic_launcher);
                    alert.show();
                }
            });
        }
    }

    private void startMainActivity(boolean transition) {
        if (!is_destroyed) {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
            finish();
        }
        //if(transition) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void startUpdateActivity() {
        if (!is_destroyed) {
            Intent updateIntent = new Intent(getApplicationContext(), UpdateCheckActivity.class);
            updateIntent.putExtra("main_flag", true);
            startActivity(updateIntent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("KCA-DA", "destroy");
        is_destroyed = true;
        dbHelper.close();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
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
}