package com.antest1.kcanotify;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pixplicity.htmlcompat.HtmlCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;

import static com.antest1.kcanotify.KcaAlarmService.DELETE_ACTION;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "KCAV";
    public static boolean[] warnType = new boolean[5];
    private static final int REQUEST_VPN = 1;
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_EXTERNAL_PERMISSION = 3;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    AudioManager audioManager;
    AssetManager assetManager;
    KcaDBHelper dbHelper;
    Toolbar toolbar;
    KcaDownloader downloader;
    private boolean running = false;
    private AlertDialog dialogVpn = null;
    Context ctx;
    ToggleButton vpnbtn, svcbtn;
    Button kcbtn;
    ImageButton kctoolbtn;
    public ImageButton kcafairybtn;
    public static Handler sHandler;
    TextView textDescription;
    TextView textWarn, textSpecial, textMaintenance;
    TextView textSpecial2;
    Gson gson = new Gson();

    SharedPreferences prefs;
    private WindowManager windowManager;
    private BackPressCloseHandler backPressCloseHandler;
    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public MainActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        assetManager = getAssets();
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        prefs.edit().putBoolean(PREF_SVC_ENABLED, KcaService.getServiceStatus()).apply();
        prefs.edit().putBoolean(PREF_VPN_ENABLED, KcaVpnService.checkOn()).apply();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        int sniffer_mode = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_SNIFFER_MODE));

        vpnbtn = findViewById(R.id.vpnbtn);
        vpnbtn.setTextOff(getStringWithLocale(R.string.ma_vpn_toggleoff));
        vpnbtn.setTextOn(getStringWithLocale(R.string.ma_vpn_toggleon));
        vpnbtn.setText("PASSIVE");
        vpnbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        final Intent prepare = VpnService.prepare(MainActivity.this);
                        if (prepare == null) {
                            //Log.i(TAG, "Prepare done");
                            onActivityResult(REQUEST_VPN, RESULT_OK, null);
                        } else {
                            startActivityForResult(prepare, REQUEST_VPN);
                        }
                    } catch (Throwable ex) {
                        // Prepare failed
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                } else {
                    KcaVpnService.stop(VPN_STOP_REASON, MainActivity.this);
                    prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
                }
            }
        });

        if (sniffer_mode == SNIFFER_ACTIVE) {
            vpnbtn.setEnabled(true);

        } else {
            vpnbtn.setEnabled(false);
        }

        svcbtn = findViewById(R.id.svcbtn);
        svcbtn.setTextOff(getStringWithLocale(R.string.ma_svc_toggleoff));
        svcbtn.setTextOn(getStringWithLocale(R.string.ma_svc_toggleon));
        svcbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, KcaService.class);
                if (isChecked) {
                    if (!prefs.getBoolean(PREF_SVC_ENABLED, false)) {
                        loadTranslationData(getApplicationContext());
                        startService(intent);
                    }
                } else {
                    stopService(intent);
                    prefs.edit().putBoolean(PREF_SVC_ENABLED, false).apply();
                }
            }
        });

        kcbtn = findViewById(R.id.kcbtn);
        kcbtn.setOnClickListener(v -> {
            String kcApp = getStringPreferences(getApplicationContext(), PREF_KC_PACKAGE);
            Intent kcIntent = getKcIntent(getApplicationContext());
            boolean is_kca_installed = false;
            if (!BuildConfig.DEBUG) is_kca_installed = (kcIntent != null);
            if (is_kca_installed) {
                kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(kcIntent);
                finish();
            } else {
                if (kcApp.equals(KC_PACKAGE_NAME)) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.ma_toast_kancolle_not_installed),
                            Toast.LENGTH_LONG).show();
                } else if (kcApp.equals(GOTO_PACKAGE_NAME)) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.ma_toast_gotobrowser_not_installed),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        kctoolbtn = findViewById(R.id.kcatoolbtn);
        kctoolbtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ToolsActivity.class);
            startActivity(intent);
        });
        kctoolbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.white), PorterDuff.Mode.SRC_ATOP);

        kcafairybtn = findViewById(R.id.kcafairybtn);
        String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        String fairyPath = "noti_icon_".concat(fairyIdValue);
        KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, kcafairybtn, 24);
        kcafairybtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.white), PorterDuff.Mode.SRC_ATOP);
        kcafairybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(getApplicationContext())) {
                    // Can not draw overlays: pass
                } else if (KcaService.getServiceStatus() && sHandler != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_FAIRY_RETURN);
                    bundle.putString("data", "");
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
            }
        });

        textWarn = findViewById(R.id.textMainWarn);
        textWarn.setVisibility(View.GONE);

        String main_html = "";
        try {
            String locale = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
            InputStream ais = assetManager.open("main-".concat(getResourceLocaleCode(locale)).concat(".html"));
            byte[] bytes = ByteStreams.toByteArray(ais);
            main_html = new String(bytes);
        } catch (IOException e) {
            main_html = "Error loading html file.";
        }

        textDescription = findViewById(R.id.textDescription);
        Spanned fromHtml = HtmlCompat.fromHtml(getApplicationContext(), main_html, 0);
        textDescription.setMovementMethod(LinkMovementMethod.getInstance());
        textDescription.setText(fromHtml);
        //Linkify.addLinks(textDescription, Linkify.WEB_URLS);

        backPressCloseHandler = new BackPressCloseHandler(this);

        textMaintenance = findViewById(R.id.textMaintenance);
        String maintenanceInfo = dbHelper.getValue(DB_KEY_KCMAINTNC);
        if (maintenanceInfo != null && maintenanceInfo.trim().length() > 0) {
            try {
                JsonArray maintenance_data = (new JsonParser().parse(maintenanceInfo)).getAsJsonArray();
                String mt_start = maintenance_data.get(0).getAsString();
                String mt_end = maintenance_data.get(1).getAsString();
                if (!mt_start.equals("")) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
                    Date start_date =  df.parse(mt_start);
                    Date end_date = df.parse(mt_end);

                    SimpleDateFormat out_df = df;
                    if (Build.VERSION.SDK_INT >= 18) {
                        out_df = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, dd MMM yyyy HH:mm Z"), Locale.getDefault());
                    }

                    boolean is_passed = end_date.getTime() < System.currentTimeMillis();
                    boolean before_maintenance = System.currentTimeMillis() < start_date.getTime();

                    if (before_maintenance) {
                        textMaintenance.setText(KcaUtils.format(getStringWithLocale(R.string.ma_nextmaintenance), out_df.format(start_date)));
                    } else if (!is_passed) {
                        textMaintenance.setText(KcaUtils.format(getStringWithLocale(R.string.ma_endmaintenance), out_df.format(end_date)));
                    }
                    textMaintenance.setVisibility(View.VISIBLE);
                } else {
                    textMaintenance.setVisibility(View.GONE);
                }
            } catch (ParseException | IllegalStateException e) {
                textMaintenance.setText(getStringFromException(e));
                textMaintenance.setVisibility(View.VISIBLE);
            }
        } else {
            textMaintenance.setVisibility(View.GONE);
        }

        String locale = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
        ImageView specialImage = findViewById(R.id.special_image);
        specialImage.setImageResource(R.mipmap.special_image);
        specialImage.setVisibility(View.GONE);
        specialImage.setOnClickListener(v -> v.setVisibility(View.GONE));

        textSpecial = findViewById(R.id.textSpecial);
        textSpecial.setText(getStringWithLocale(R.string.special_message));
        textSpecial.setOnClickListener(v -> specialImage.setVisibility(View.VISIBLE));

        textSpecial2 = findViewById(R.id.textSpecial2);
        textSpecial2.setText(getStringWithLocale(R.string.ask_to_dev));
        textSpecial2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locale = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
                String locale_code = getResourceLocaleCode(locale);
                String url = KcaUtils.format("http://52.55.91.44/kcanotify/asktodev/index_%s.html", locale_code);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVpnBtn();
        setCheckBtn();

        kcafairybtn = findViewById(R.id.kcafairybtn);
        String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        String fairyPath = "noti_icon_".concat(fairyIdValue);
        KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, kcafairybtn, 24);
        showDataLoadErrorToast(getApplicationContext(), getStringWithLocale(R.string.download_check_error));
        kcafairybtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.white), PorterDuff.Mode.SRC_ATOP);

        Arrays.fill(warnType, false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_PERMISSION);
        }

        if (getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE)
                || getBooleanPreferences(getApplicationContext(), PREF_KCA_QUESTVIEW_USE)) {
            warnType[REQUEST_OVERLAY_PERMISSION] = !checkOverlayPermission();
        }
        setWarning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVpnBtn();
        setCheckBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    public void setWarning() {
        String warnText = "";
        if (warnType[REQUEST_OVERLAY_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_toast_overay_diabled));
        }
        if (warnType[REQUEST_EXTERNAL_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_permission_external_denied));
        }

        if (warnText.length() > 0) {
            textWarn.setVisibility(View.VISIBLE);
            textWarn.setText(warnText.trim());
        } else {
            textWarn.setVisibility(View.GONE);
            textWarn.setText("");
        }
    }

    public void setVpnBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int sniffer_mode = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_SNIFFER_MODE));
        if (sniffer_mode == SNIFFER_ACTIVE) {
            vpnbtn.setEnabled(true);
            vpnbtn.setChecked(prefs.getBoolean(PREF_VPN_ENABLED, false));
        } else {
            vpnbtn.setText("PASSIVE");
            vpnbtn.setEnabled(false);
        }
    }

    public void setCheckBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        svcbtn.setChecked(prefs.getBoolean(PREF_SVC_ENABLED, false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        if (id == R.id.action_errorlog) {
            startActivity(new Intent(this, ErrorlogActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkOverlayPermission() {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Log.i(TAG, "onActivityResult request=" + requestCode + " result=" + resultCode + " ok=" + (resultCode == RESULT_OK));
        if (requestCode == REQUEST_VPN) {
            prefs.edit().putBoolean(PREF_VPN_ENABLED, resultCode == RESULT_OK).apply();
            if (resultCode == RESULT_OK) {
                KcaVpnService.start("prepared", this);
            } else if (resultCode == RESULT_CANCELED) {
                // Canceled
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_PERMISSION: {
                warnType[REQUEST_EXTERNAL_PERMISSION] = !(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                setWarning();
            }
        }
    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    private void checkRecentVersion() {
        String currentVersion = BuildConfig.VERSION_NAME;
        String currentDataVersion = getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
        final Call<String> rv_data = downloader.getRecentVersion();
        rv_data.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                JsonObject response_data = new JsonObject();
                try {
                    if (response.body() != null) {
                        response_data = new JsonParser().parse(response.body()).getAsJsonObject();
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
                }

                Log.e("KCA", response_data.toString());
                int nid = getNotificationId(NOTI_UPDATE, 0);
                Intent deleteIntent = new Intent(MainActivity.this, KcaAlarmService.class)
                        .setAction(DELETE_ACTION.concat(String.valueOf(nid)));
                startService(deleteIntent);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", t.getMessage());
                }
                int nid = getNotificationId(NOTI_UPDATE, 0);
                Intent deleteIntent = new Intent(MainActivity.this, KcaAlarmService.class)
                        .setAction(DELETE_ACTION.concat(String.valueOf(nid)));
                startService(deleteIntent);
            }
        });
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
        loadTranslationData(getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}

