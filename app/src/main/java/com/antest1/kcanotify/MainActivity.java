package com.antest1.kcanotify;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import android.text.Html;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUseStatConstant.END_APP;
import static com.antest1.kcanotify.KcaUseStatConstant.END_SERVICE;
import static com.antest1.kcanotify.KcaUseStatConstant.END_SNIFFER;
import static com.antest1.kcanotify.KcaUseStatConstant.ENTER_MAIN;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_PIC;
import static com.antest1.kcanotify.KcaUseStatConstant.RUN_KANCOLLE;
import static com.antest1.kcanotify.KcaUseStatConstant.START_SERVICE;
import static com.antest1.kcanotify.KcaUseStatConstant.START_SNIFFER;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "KCAV";
    public static boolean[] warnType = new boolean[8];
    private static final int REQUEST_VPN = 1;
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_EXTERNAL_PERMISSION = 3;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 4;
    public static final int REQUEST_EXACT_ALARM_PERMISSION = 5;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    AudioManager audioManager;
    AssetManager assetManager;
    KcaDBHelper dbHelper;
    Toolbar toolbar;
    KcaDownloader downloader;
    MaterialButton vpnbtn, svcbtn;
    Button kcbtn;
    MenuItem fairyButton;
    public static Handler sHandler;
    TextView textDescription;
    TextView textMaintenance;
    Button textMainUpdate, textSpecial, textSpecial2;
    BottomAppBar bottomAppBar;

    ActivityResultLauncher<Intent> vpnPrepareLauncher, exactAlarmPrepareLauncher;

    SharedPreferences prefs;
    private BackPressCloseHandler backPressCloseHandler;

    public static void setHandler(Handler h) {
        sHandler = h;
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

        vpnPrepareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> startActivityResultCallback(REQUEST_VPN, result.getResultCode())
        );

        exactAlarmPrepareLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> startActivityResultCallback(REQUEST_EXACT_ALARM_PERMISSION, result.getResultCode())
        );

        vpnbtn = findViewById(R.id.vpnbtn);
        svcbtn = findViewById(R.id.svcbtn);
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroup);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.svcbtn) {
                svcbtn.setText(isChecked ? getStringWithLocale(R.string.ma_svc_toggleon) : getStringWithLocale(R.string.ma_svc_toggleoff));
                if (isChecked) {
                    if (checkNotificationPermission() && checkExactAlarmPermission()) {
                        if (!prefs.getBoolean(PREF_SVC_ENABLED, false))
                            startKcaService();
                    } else {
                        if (!checkNotificationPermission())
                            grantNotificationPermission();
                        else if (!checkExactAlarmPermission())
                            grantExactAlarmPermission();
                    }
                } else {
                    stopKcaService();
                }
            } else if (checkedId == R.id.vpnbtn) {
                vpnbtn.setText(isChecked ? getStringWithLocale(R.string.ma_vpn_toggleon) : getStringWithLocale(R.string.ma_vpn_toggleoff));
                if (isChecked) {
                    if (getBooleanPreferences(getApplicationContext(), PREF_VPNSERVICE_USAGE_AGREE)) {
                        startVpnService();
                    } else {
                        showVpnServiceNotification();
                    }
                } else {
                    stopVpnService();
                }
            }
        });

        int sniffer_mode = getSnifferMode();
        vpnbtn.setEnabled(sniffer_mode == SNIFFER_ACTIVE);
        if (sniffer_mode != SNIFFER_ACTIVE) {
            vpnbtn.setText("PASSIVE");
        }

        kcbtn = findViewById(R.id.kcbtn);
        kcbtn.setOnClickListener(v -> {
            String kcApp = getStringPreferences(getApplicationContext(), PREF_KC_PACKAGE);
            Intent kcIntent = getKcIntent(getApplicationContext());

            boolean is_kca_installed = (kcIntent != null);

            JsonObject statProperties = new JsonObject();
            statProperties.addProperty("browser", kcApp);
            statProperties.addProperty("sniffer", sniffer_mode);
            statProperties.addProperty("enabled", is_kca_installed ? 1 : 0);

            sendUserAnalytics(getApplicationContext(), RUN_KANCOLLE, statProperties);
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


        bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.fairy) {
                if (Settings.canDrawOverlays(getApplicationContext()) && KcaService.getServiceStatus() && sHandler != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_FAIRY_RETURN);
                    bundle.putString("data", "");
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
                return true;
            } else if (item.getItemId() == R.id.tools) {
                Intent intent = new Intent(MainActivity.this, ToolsActivity.class);
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });


        fairyButton = bottomAppBar.getMenu().getItem(0);
        boolean is_random_fairy = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_RANDOM);
        if (is_random_fairy) {
            fairyButton.setIcon(R.mipmap.ic_help);
        } else {
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            String fairyPath = "noti_icon_".concat(fairyIdValue);
            KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, fairyButton, 24);
        }

        String main_html;
        try {
            InputStream ais = assetManager.open("main-".concat(getResourceLocaleCode()).concat(".html"));
            byte[] bytes = ByteStreams.toByteArray(ais);
            main_html = new String(bytes);
        } catch (IOException e) {
            main_html = "Error loading html file.";
        }

        textMainUpdate = findViewById(R.id.textMainUpdate);
        textMainUpdate.setText(getStringWithLocale(R.string.setting_menu_kand_title_game_data_down));
        textMainUpdate.setOnClickListener(v -> {
            startActivity(new Intent(this, UpdateCheckActivity.class));
        });

        textDescription = findViewById(R.id.textDescription);
        Spanned fromHtml = HtmlCompat.fromHtml(main_html, 0);
        textDescription.setMovementMethod(LinkMovementMethod.getInstance());
        textDescription.setText(fromHtml);

        backPressCloseHandler = new BackPressCloseHandler(this);

        textMaintenance = findViewById(R.id.textMaintenance);
        String maintenanceInfo = dbHelper.getValue(DB_KEY_KCMAINTNC);
        if (maintenanceInfo != null && !maintenanceInfo.trim().isEmpty()) {
            try {
                JsonArray maintenance_data = (JsonParser.parseString(maintenanceInfo)).getAsJsonArray();
                String mt_start = maintenance_data.get(0).getAsString();
                String mt_end = maintenance_data.get(1).getAsString();
                if (!mt_start.isEmpty()) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
                    Date start_date = df.parse(mt_start);
                    Date end_date = df.parse(mt_end);

                    SimpleDateFormat out_df;
                    out_df = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, dd MMM yyyy HH:mm Z"), Locale.getDefault());

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

        ImageView specialImage = findViewById(R.id.special_image);
        specialImage.setImageResource(R.mipmap.special_image);
        specialImage.setVisibility(View.GONE);
        specialImage.setOnClickListener(v -> v.setVisibility(View.GONE));

        textSpecial = findViewById(R.id.textSpecial);
        textSpecial.setText(getStringWithLocale(R.string.special_message));
        textSpecial.setOnClickListener(v -> {
            specialImage.setImageResource(R.mipmap.special_image);
            specialImage.setVisibility(View.VISIBLE);
            sendUserAnalytics(getApplicationContext(), OPEN_PIC, null);
        });

        textSpecial2 = findViewById(R.id.textSpecial2);
        textSpecial2.setText(getStringWithLocale(R.string.notification_message));
        textSpecial2.setOnClickListener(v -> {
            String url = getStringWithLocale(R.string.app_notice_link);

            CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
            intentBuilder.setShowTitle(true);
            CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                    .build();
            intentBuilder.setDefaultColorSchemeParams(params);
            intentBuilder.setUrlBarHidingEnabled(true);

            final CustomTabsIntent customTabsIntent = intentBuilder.build();
            final List<ResolveInfo> customTabsApps = getPackageManager().queryIntentActivities(customTabsIntent.intent, 0);
            if (!customTabsApps.isEmpty()) {
                customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
    }

    private void startKcaService() {
        Intent serviceIntent = new Intent(MainActivity.this, KcaService.class);
        sendUserAnalytics(getApplicationContext(), START_SERVICE, null);
        serviceIntent.setAction(KcaService.KCASERVICE_START);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopKcaService() {
        Intent serviceIntent = new Intent(MainActivity.this, KcaService.class);
        serviceIntent.setAction(KcaService.KCASERVICE_STOP);
        startService(serviceIntent);
        sendUserAnalytics(getApplicationContext(), END_SERVICE, null);
        prefs.edit().putBoolean(PREF_SVC_ENABLED, false).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sendUserAnalytics(getApplicationContext(), ENTER_MAIN, null);

        setVpnBtn();
        setCheckBtn();

        fairyButton = bottomAppBar.getMenu().getItem(0);
        boolean is_random_fairy = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_RANDOM);
        if (is_random_fairy) {
            fairyButton.setIcon(R.mipmap.ic_help);
        } else {
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            String fairyPath = "noti_icon_".concat(fairyIdValue);
            KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, fairyButton, 24);
            showDataLoadErrorToast(getApplicationContext(), getStringWithLocale(R.string.download_check_error));
        }

        Arrays.fill(warnType, false);

        if (getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE)
                || getBooleanPreferences(getApplicationContext(), PREF_KCA_QUESTVIEW_USE)) {
            warnType[REQUEST_OVERLAY_PERMISSION] = !checkOverlayPermission();
        }

        warnType[REQUEST_NOTIFICATION_PERMISSION] = !(checkNotificationPermission() && checkExactAlarmPermission());
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
        sendUserAnalytics(getApplicationContext(), END_APP, null);
        dbHelper.close();
        super.onDestroy();
    }

    public void setWarning() {
        String warnText = "";
        View main = findViewById(R.id.activity_main);
        if (warnType[REQUEST_OVERLAY_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_toast_overay_diabled));
            Snackbar.make(main, warnText.trim(), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.setting_menu_perm_head), view -> {
                        // TODO: Share code with MainPreferenceFragment.java
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getApplicationContext().getPackageName()));
                        startActivity(intent);
                    }).show();
        }
        if (warnType[REQUEST_EXTERNAL_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_permission_external_denied));
            // TODO: Add action
            Snackbar.make(main, warnText.trim(), Snackbar.LENGTH_INDEFINITE).show();
        }
        if (warnType[REQUEST_NOTIFICATION_PERMISSION] || warnType[REQUEST_EXACT_ALARM_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_permission_notification_denied));
            // TODO: Add action
            Snackbar.make(main, warnText.trim(), Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    public final int getSnifferMode() {
        if (KC_PACKAGE_NAME.equals(getStringPreferences(getApplicationContext(), PREF_KC_PACKAGE))) {
            return SNIFFER_ACTIVE;
        } else {
            return Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_SNIFFER_MODE));
        }
    }

    public void startActivityResultCallback(int type, int resultCode) {
        if (type == REQUEST_VPN) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean(PREF_VPN_ENABLED, resultCode == RESULT_OK).apply();
            if (resultCode == RESULT_OK) {
                KcaVpnService.start("prepared", this);
            } else if (resultCode == RESULT_CANCELED) {
                // Canceled
            }
        }
        if (type == REQUEST_EXACT_ALARM_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                new Handler().postDelayed(() -> {
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    warnType[REQUEST_EXACT_ALARM_PERMISSION] = !alarmManager.canScheduleExactAlarms();
                    setWarning();
                }, 1000);
            }
        }
    }

    public void setVpnBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (getSnifferMode() == SNIFFER_ACTIVE) {
            boolean isChecked = prefs.getBoolean(PREF_VPN_ENABLED, false);
            vpnbtn.setText(isChecked ? getStringWithLocale(R.string.ma_vpn_toggleon) : getStringWithLocale(R.string.ma_vpn_toggleoff));
            vpnbtn.setEnabled(true);
            vpnbtn.setChecked(isChecked);
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
        return super.onOptionsItemSelected(item);
    }

    private boolean checkOverlayPermission() {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext()));
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private void grantExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                exactAlarmPrepareLauncher.launch(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_PERMISSION: {
                warnType[REQUEST_EXTERNAL_PERMISSION] = !(grantResults[0] == PackageManager.PERMISSION_GRANTED);
                setWarning();
            }
            case REQUEST_NOTIFICATION_PERMISSION: {
                warnType[REQUEST_NOTIFICATION_PERMISSION] = !(grantResults[0] == PackageManager.PERMISSION_GRANTED);
                setWarning();
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), getString(R.string.ma_permission_service_restart), Toast.LENGTH_LONG).show();
                }
                if (!checkExactAlarmPermission()) grantExactAlarmPermission();
            }
        }
    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    public void startVpnService() {
        JsonObject statProperties = new JsonObject();
        try {
            final Intent prepare = VpnService.prepare(MainActivity.this);
            if (prepare == null) {
                //Log.i(TAG, "Prepare done");
                startActivityResultCallback(REQUEST_VPN, RESULT_OK);
            } else {
                vpnPrepareLauncher.launch(prepare);
            }
            statProperties.addProperty("is_success", true);
        } catch (Throwable ex) {
            // Prepare failed
            statProperties.addProperty("is_success", false);
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
        sendUserAnalytics(getApplicationContext(), START_SNIFFER, statProperties);
    }

    public void stopVpnService() {
        KcaVpnService.stop(VPN_STOP_REASON, MainActivity.this);
        prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
        sendUserAnalytics(getApplicationContext(), END_SNIFFER, null);
    }

    public void showVpnServiceNotification() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setPositiveButton(getStringWithLocale(R.string.dialog_ok), (dialog, which) -> {
            setPreferences(getApplicationContext(), PREF_VPNSERVICE_USAGE_AGREE, true);
            startVpnService();
        }).setNegativeButton(getStringWithLocale(R.string.dialog_cancel), (dialog, which) -> {
            prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
            vpnbtn.setChecked(false);
        }).setOnDismissListener(dialog -> {
            prefs.edit().putBoolean(PREF_VPN_ENABLED, false).apply();
            vpnbtn.setChecked(false);
        });
        alert.setMessage(Html.fromHtml(getStringWithLocale(R.string.ma_dialog_vpn_usage)));
        AlertDialog dialog = alert.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}