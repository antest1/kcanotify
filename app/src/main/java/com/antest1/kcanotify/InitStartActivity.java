package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_MAIN;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_ALLOW_EXTFILTER;
import static com.antest1.kcanotify.KcaConstants.PREF_APK_DOWNLOAD_SITE;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE_START;
import static com.antest1.kcanotify.KcaConstants.PREF_DATALOAD_ERROR_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_DEFAULT_APIVER;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_LAST_UPDATE_CHECK;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_FLAG;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_PREFIX;
import static com.antest1.kcanotify.KcaUseStatConstant.START_APP;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.gzipdecompress;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class InitStartActivity extends BaseActivity {
    public static final String ACTION_RESET = "ACTION_RESET";

    Handler handler = new Handler();

    KcaDBHelper dbHelper;
    KcaDownloader downloader;
    TextView appname, appversion, appmessage;
    ImageView appfront;

    boolean is_destroyed = false;
    int fairy_flag;
    boolean reset_flag = false;
    boolean validated_flag = false;
    boolean is_skipped = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setDefaultPreferences();

        setAppLocale();

        setContentView(R.layout.activity_init_download);
        Log.e("KCA-DA", "created");

        sendUserAnalytics(getApplicationContext(), START_APP, null);

        Intent mainIntent = getIntent();
        reset_flag = mainIntent.getBooleanExtra(ACTION_RESET, false);
        is_destroyed = false;

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());

        KcaApiData.setDBHelper(dbHelper);

        appname = findViewById(R.id.app_title);

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

        // make backup location directory at first
        File backup_dir = new File(getExternalFilesDir(null), "backup");
        if (!backup_dir.exists()) backup_dir.mkdirs();

        // Initialize resources
        setPreferences(getApplicationContext(), PREF_DATALOAD_ERROR_FLAG, false);
        loadDefaultAsset();

        validated_flag = KcaUtils.validateResourceFiles(getApplicationContext(), dbHelper);
        setPreferences(getApplicationContext(), PREF_DATALOAD_ERROR_FLAG, false);
        findViewById(R.id.init_layout).setOnClickListener(v -> {
            if (validated_flag && !reset_flag) {
                startMainActivity(false);
                is_skipped = true;
            }
        });

        new Thread(() -> {
            runOnUiThread(() -> appmessage.setText("Loading Translation Data..."));
            int tl_result = loadTranslationData(getApplicationContext());
            if (tl_result > 0) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "error loading traslation data: " + tl_result, Toast.LENGTH_LONG).show());
            }

            runOnUiThread(() -> appmessage.setText("Loading KanColle Game Data..."));
            int setDefaultGameDataResult = KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
            if (setDefaultGameDataResult != 1) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "error loading game data", Toast.LENGTH_LONG).show());
            }

            runOnUiThread(() -> appmessage.setText("Checking External Filter..."));
            boolean allow_ext = KcaUtils.getBooleanPreferences(getApplicationContext(), PREF_ALLOW_EXTFILTER);
            KcaVpnData.setExternalFilter(allow_ext);

            boolean is_default_written = KcaUtils.getStringPreferences(getApplicationContext(), PREF_DEFAULT_APIVER).equals(BuildConfig.VERSION_NAME);
            if (!is_default_written) {
                runOnUiThread(() -> appmessage.setText("Writing Default Data..."));
                String internal_kca_version = getString(R.string.default_gamedata_version);
                try {
                    InputStream api_ais = getAssets().open("api_start2");
                    byte[] bytes = gzipdecompress(ByteStreams.toByteArray(api_ais));
                    String asset_start2_data = new String(bytes);
                    dbHelper.putValue(DB_KEY_STARTDATA, asset_start2_data);
                    KcaUtils.setPreferences(getApplicationContext(), PREF_DEFAULT_APIVER, BuildConfig.VERSION_NAME);
                    KcaUtils.setPreferences(getApplicationContext(), PREF_KCA_VERSION, internal_kca_version);
                    KcaUtils.setPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION, internal_kca_version);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // main update check
            if (!KcaUtils.checkOnline(getApplicationContext()) || !getBooleanPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START)) {
                startMainActivity(true);
            } else {
                if (is_skipped) return;
                runOnUiThread(() -> appmessage.setText("Checking Updates..."));
                String currentVersion = BuildConfig.VERSION_NAME;

                final Call<String> rv_data = downloader.getRecentVersion();
                String response = getResultFromCall(rv_data);
                fairy_flag = 0;

                JsonObject response_data = new JsonObject();
                try {
                    if (response != null) {
                        response_data = JsonParser.parseString(response).getAsJsonObject();
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
                }

                Log.e("KCA", response_data.toString());
                if (response_data.has("version")) {
                    String recentVersion = response_data.get("version").getAsString();
                    if (!compareVersion(currentVersion, recentVersion)) { // True if latest
                        JsonObject data = response_data;
                        if (!is_skipped) {
                            runOnUiThread(() -> {
                                try {
                                    AlertDialog.Builder alertDialog = getUpdateAlertDialog(recentVersion, data);
                                    AlertDialog alert = alertDialog.create();
                                    alert.setIcon(R.mipmap.ic_launcher);
                                    alert.setTitle(
                                            getString(R.string.sa_checkupdate_dialogtitle));
                                    alert.show();
                                } catch (WindowManager.BadTokenException e) {
                                    // activity closed, no need to show dialog
                                }
                            });
                        }
                    } else {
                        dataCheck(response_data);
                    }
                }
            }
        }).start();
    }

    private AlertDialog.Builder getUpdateAlertDialog(String recentVersion, JsonObject data) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(InitStartActivity.this);
        alertDialog.setCancelable(false);
        alertDialog.setMessage(KcaUtils.format(getString(R.string.sa_checkupdate_hasupdate), recentVersion));
        alertDialog.setPositiveButton(getString(R.string.dialog_ok),
                (dialog, which) -> {
                    String downloadUrl = getStringPreferences(getApplicationContext(), PREF_APK_DOWNLOAD_SITE);
                    if (downloadUrl.contains(getString(R.string.app_download_link_playstore))) {
                        downloadUrl = getString(R.string.app_download_link_luckyjervis);
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
                });
        alertDialog.setNegativeButton(getString(R.string.dialog_cancel),
                (dialog, which) -> {
                    dataCheck(data);
                });
        return alertDialog;
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
            int common_version = response_data.get("kcadata_version").getAsInt();
            int new_resversion = common_version;
            if (response_data.has("kcadata_langpacks")) {
                int langpacks_version = response_data.getAsJsonObject("kcadata_langpacks")
                        .get(LocaleUtils.getResourceLocaleCode()).getAsInt();
                new_resversion = Math.max(common_version, langpacks_version);
            }
            latest_flag = latest_flag && new_resversion <= currentKcaResVersion;
        }

        setPreferences(getApplicationContext(), PREF_LAST_UPDATE_CHECK, String.valueOf(System.currentTimeMillis()));
        if (latest_flag) {
            startMainActivity(true);
        } else {
            String message = getString(R.string.download_description_head);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(InitStartActivity.this);
            alertDialog.setTitle(getString(R.string.download_title));
            alertDialog.setMessage(message.trim());
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
                startUpdateActivity();
             });

            alertDialog.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
                startMainActivity(true);
            });

            handler.post(() -> {
                if (!is_destroyed && !InitStartActivity.this.isFinishing()) {
                    AlertDialog alert = alertDialog.create();
                    alert.setIcon(R.mipmap.ic_launcher);
                    alert.show();
                }
            });
        }
    }

    private void startMainActivity(boolean transition) {
        if (!is_destroyed) {
            runOnUiThread(() -> {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
            });
        }
        //if(transition) overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void startUpdateActivity() {
        if (!is_destroyed) {
            runOnUiThread(() -> {
                Intent updateIntent = new Intent(getApplicationContext(), UpdateCheckActivity.class);
                updateIntent.putExtra("main_flag", true);
                startActivity(updateIntent);
                finish();
            });
        }
    }

    private void loadDefaultAsset() {
        AssetManager am = getAssets();
        byte[] bytes;
        String kca_data_version = KcaUtils.getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
        String internal_kca_version = getString(R.string.default_gamedata_version);
        int currentKcaResVersion = dbHelper.getTotalResVer();
        try {
            if (kca_data_version == null || compareVersion(internal_kca_version, kca_data_version)) {
                InputStream api_ais = am.open("api_start2");
                bytes = gzipdecompress(ByteStreams.toByteArray(api_ais));
                String asset_start2_data = new String(bytes);
                dbHelper.putValue(DB_KEY_STARTDATA, asset_start2_data);
                KcaUtils.setPreferences(getApplicationContext(), PREF_KCA_VERSION, internal_kca_version);
                KcaUtils.setPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION, internal_kca_version);
            }

            AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) am.open("list.json");
            bytes = ByteStreams.toByteArray(ais);
            JsonArray data = JsonParser.parseString(new String(bytes)).getAsJsonArray();

            for (JsonElement item: data) {
                JsonObject res_info = item.getAsJsonObject();
                String name = res_info.get("name").getAsString();
                int version = res_info.get("version").getAsInt();
                if (currentKcaResVersion < version) {
                    final File root_dir = getDir("data", Context.MODE_PRIVATE);
                    final File new_data = new File(root_dir, name);
                    if (new_data.exists()) new_data.delete();
                    InputStream file_is = am.open(name);
                    OutputStream file_out = new FileOutputStream(new_data);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = file_is.read(buffer)) != -1) {
                        file_out.write(buffer, 0, bytesRead);
                    }
                    file_is.close();
                    file_out.close();
                    dbHelper.putResVer(name, version);
                }
            }
            ais.close();
        } catch (IOException e) {

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

    private void setAppLocale() {
        String pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
        if (pref.startsWith("default") || !pref.contains("-")) {
            LocaleUtils.setLocale(getBaseContext(), Locale.getDefault());
        } else {
            String[] locale = pref.split("-");
            LocaleUtils.setLocale(getBaseContext(), new Locale(locale[0], locale[1]));
        }
        KcaApplication.defaultLocale = LocaleUtils.getLocale();
        updateLocaleConfig();
    }
}