package com.antest1.kcanotify;

import com.antest1.kcanotify.remote_capture.MitmAddon;
import com.antest1.kcanotify.viewmodel.MainViewModel;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.activity.OnBackPressedCallback;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

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
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;

public class MainActivity extends BaseActivity {
    private final static String TAG = "MainActivity";
    public static boolean[] warnType = new boolean[8];
    public static final String ACTION_OPEN_TOOL = "open_tool";
    public static final String ACTION_RESTART_SERVICE = "restart_service";
    private static final int REQUEST_VPN = 1;
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_EXTERNAL_PERMISSION = 3;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 4;
    public static final int REQUEST_EXACT_ALARM_PERMISSION = 5;

    Toolbar toolbar;
    Snackbar snackbar;
    MaterialButton vpnbtn, svcbtn;
    Button kcbtn;
    MenuItem fairyButton;
    public static Handler sHandler;
    TextView textDescription;
    Button textMainUpdate, textSpecial, textSpecial2;
    BottomAppBar bottomAppBar;
    FrameLayout bottomSheet;
    BottomSheetBehavior<FrameLayout> bottomSheetBehavior;

    ActivityResultLauncher<Intent> vpnPrepareLauncher, exactAlarmPrepareLauncher;

    MainViewModel viewModel;
    private BackPressCloseHandler backPressCloseHandler;
    private boolean mToolOpened = false;
    private boolean vpnFirstPrepared = false;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vpn_main);
        // a recreated activity gets the original intent again; the restart request was already handled
        if (savedInstanceState != null && ACTION_RESTART_SERVICE.equals(getIntent().getAction())) {
            getIntent().setAction(null);
        }
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.refreshStatusFromServices();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

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
                svcbtn.setText(isChecked ? getString(R.string.ma_svc_toggleon) : getString(R.string.ma_svc_toggleoff));
                if (isChecked) {
                    if (checkNotificationPermission() && checkExactAlarmPermission()) {
                        if (!viewModel.isServiceEnabled()) {
                            sendUserAnalytics(getApplicationContext(), START_SERVICE, null);
                            viewModel.startKcaService(getApplicationContext());
                        }
                    } else {
                        if (!checkNotificationPermission())
                            grantNotificationPermission();
                        else if (!checkExactAlarmPermission())
                            grantExactAlarmPermission();
                    }
                } else {
                    viewModel.stopKcaService(getApplicationContext());
                    sendUserAnalytics(getApplicationContext(), END_SERVICE, null);
                }
            } else if (checkedId == R.id.vpnbtn) {
                vpnbtn.setText(isChecked ? getString(R.string.ma_vpn_toggleon) : getString(R.string.ma_vpn_toggleoff));
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

        int sniffer_mode = viewModel.getSnifferMode();
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

        bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

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
            } else if (item.getItemId() == R.id.tools)
            {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                return true;
            } else {
                return false;
            }
        });


        fairyButton = bottomAppBar.getMenu().getItem(0);
        boolean is_random_fairy = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_RANDOM);
        if (is_random_fairy) {
            fairyButton.setIcon(R.drawable.ic_dice);
        } else {
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            String fairyPath = "noti_icon_".concat(fairyIdValue);
            KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, fairyButton, 24);
        }


        String main_html = viewModel.loadMainHtml();

        textMainUpdate = findViewById(R.id.textMainUpdate);
        textMainUpdate.setText(getString(R.string.setting_menu_kand_title_game_data_down));
        textMainUpdate.setOnClickListener(v -> {
            startActivity(new Intent(this, UpdateCheckActivity.class));
        });

        textDescription = findViewById(R.id.textDescription);
        Spanned fromHtml = HtmlCompat.fromHtml(main_html, 0);
        textDescription.setMovementMethod(LinkMovementMethod.getInstance());
        textDescription.setText(fromHtml);

        backPressCloseHandler = new BackPressCloseHandler();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backPressCloseHandler.onBackPressed();
            }
        });

        ImageView specialImage = findViewById(R.id.special_image);
        specialImage.setImageResource(R.mipmap.special_image);
        specialImage.setVisibility(View.GONE);
        specialImage.setOnClickListener(v -> v.setVisibility(View.GONE));

        textSpecial = findViewById(R.id.textSpecial);
        textSpecial.setText(getString(R.string.special_message));
        textSpecial.setOnClickListener(v -> {
            specialImage.setImageResource(R.mipmap.special_image);
            specialImage.setVisibility(View.VISIBLE);
            sendUserAnalytics(getApplicationContext(), OPEN_PIC, null);
        });

        textSpecial2 = findViewById(R.id.textSpecial2);
        textSpecial2.setText(getString(R.string.notification_message));
        textSpecial2.setOnClickListener(v -> showWebPage(KCANOTIFY_NOTICE_LINK));
    }

    @Override
    protected void onStart() {
        super.onStart();
        sendUserAnalytics(getApplicationContext(), ENTER_MAIN, null);

        setVpnBtn();
        setCheckBtn();
        handleOpenToolIntent(getIntent());

        fairyButton = bottomAppBar.getMenu().getItem(0);
        boolean is_random_fairy = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_RANDOM);
        if (is_random_fairy) {
            fairyButton.setIcon(R.drawable.ic_dice);
        } else {
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            String fairyPath = "noti_icon_".concat(fairyIdValue);
            KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, fairyButton, 24);
            showDataLoadErrorToast(getApplicationContext(), getString(R.string.download_check_error));
        }

        Arrays.fill(warnType, false);

        if (getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE)
                || getBooleanPreferences(getApplicationContext(), PREF_KCA_QUESTVIEW_USE)) {
            warnType[REQUEST_OVERLAY_PERMISSION] = !checkOverlayPermission();
        }
        setWarning();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onResume() {
        super.onResume();
        if (!vpnFirstPrepared && viewModel.getSnifferMode() == SNIFFER_ACTIVE) {
            viewModel.setVpnEnabledSync(KcaVpnService.checkOn());
        }
        setVpnBtn();
        setCheckBtn();
        // after setCheckBtn, which would otherwise turn the toggle back off
        handleRestartServiceIntent(getIntent());
        setUpdateDot();
    }

    // a small red dot (M3 small badge) on the icon of the update chip while the last check found updates;
    // on the icon, since the end of the chip can be under the GAME START button
    private Drawable updateChipIcon;
    private ColorStateList updateChipIconTint;

    private void setUpdateDot() {
        if (!(textMainUpdate instanceof Chip)) return;
        Chip chip = (Chip) textMainUpdate;
        if (updateChipIcon == null) {
            updateChipIcon = chip.getChipIcon();
            updateChipIconTint = chip.getChipIconTint();
        }
        int count = KcaDataUpdateManager.getPendingCount(getApplicationContext());
        String title = getString(R.string.setting_menu_kand_title_game_data_down);
        if (count > 0) {
            chip.setContentDescription(title + ", " + KcaUtils.format(getString(R.string.data_update_available), count));
            Drawable icon = AppCompatResources.getDrawable(this, R.drawable.ic_download);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(KcaDataUpdateAdapter.getThemeColor(this, androidx.appcompat.R.attr.colorError));
            // outlined in the chip's text color, to stand out on the blue chip
            float density = getResources().getDisplayMetrics().density;
            dot.setStroke(Math.round(density), chip.getCurrentTextColor());
            LayerDrawable badged = new LayerDrawable(new Drawable[]{icon, dot});
            int size = Math.round(8 * density);
            badged.setLayerSize(1, size, size);
            badged.setLayerGravity(1, Gravity.TOP | Gravity.END);
            // the icon is white by itself; a tint would also cover the dot
            chip.setChipIconTint(null);
            chip.setChipIcon(badged);
        } else {
            chip.setContentDescription(title);
            chip.setChipIcon(updateChipIcon);
            chip.setChipIconTint(updateChipIconTint);
        }
    }


    @Override
    protected void onDestroy() {
        sendUserAnalytics(getApplicationContext(), END_APP, null);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mToolOpened = false;
        handleOpenToolIntent(intent);
    }

    // from the "service stopped" notification: turn the service toggle on, which also asks
    // for missing permissions like a manual tap does
    private void handleRestartServiceIntent(Intent intent) {
        if (intent == null || !ACTION_RESTART_SERVICE.equals(intent.getAction())) return;
        intent.setAction(null);
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return;
        if (!svcbtn.isChecked()) svcbtn.setChecked(true);
    }

    public void setWarning() {
        String warnText = "";
        View main = findViewById(R.id.activity_main);
        warnType[REQUEST_NOTIFICATION_PERMISSION] = !(checkNotificationPermission() && checkExactAlarmPermission());
        if (snackbar != null) snackbar.dismiss();

        if (warnType[REQUEST_OVERLAY_PERMISSION]) {
            warnText = getString(R.string.ma_toast_overay_diabled);
            snackbar = Snackbar.make(main, warnText.trim(), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.action_perm), view -> {
                        // TODO: Share code with MainPreferenceFragment.java
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getApplicationContext().getPackageName()));
                        startActivity(intent);
                    });
        } else if (warnType[REQUEST_NOTIFICATION_PERMISSION] || warnType[REQUEST_EXACT_ALARM_PERMISSION]) {
            warnText = getString(R.string.ma_permission_notification_denied);
            snackbar = Snackbar.make(main, warnText.trim(), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.action_perm), view -> {
                        grantNotificationPermission();
                    });
        } else if (warnType[REQUEST_EXTERNAL_PERMISSION]) {
            warnText = getString(R.string.ma_permission_external_denied);
            // not called in general situation, implement grant permission when required
            snackbar = Snackbar.make(main, warnText.trim(), Snackbar.LENGTH_INDEFINITE);
        } else {
            snackbar = null;
        }

        if (snackbar != null) snackbar.show();
    }

    public final int getSnifferMode() {
        return viewModel.getSnifferMode();
    }
    public void setVpnBtn() {
        MainViewModel.VpnButtonState state = viewModel.getVpnButtonState();
        if (state.enabled) {
            vpnbtn.setText(getString(state.textResId));
            vpnbtn.setEnabled(true);
            vpnbtn.setChecked(state.checked);
        } else {
            vpnbtn.setText("PASSIVE");
            vpnbtn.setEnabled(false);
        }
    }

    public void setCheckBtn() {
        svcbtn.setChecked(viewModel.isServiceChecked());
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
        } else if (id == R.id.action_manual) {
            showWebPage(KCANOTIFY_DOCS_LINK);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkOverlayPermission() {
        return viewModel.checkOverlayPermission();
    }

    private boolean checkNotificationPermission() {
        return viewModel.checkNotificationPermission();
    }

    private boolean checkExactAlarmPermission() {
        return viewModel.checkExactAlarmPermission();
    }

    private void grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
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
                if (!checkExactAlarmPermission()) {
                    grantExactAlarmPermission();
                } else {
                    setWarning();
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        Toast.makeText(getApplicationContext(), getString(R.string.ma_permission_service_restart), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void startActivityResultCallback(int type, int resultCode) {
        if (type == REQUEST_VPN) {
            viewModel.setVpnEnabled(resultCode == RESULT_OK);
            if (resultCode == RESULT_OK) {
                vpnFirstPrepared = true;
                KcaVpnService.start("prepared", this);
            } else if (resultCode == RESULT_CANCELED) {
                // Canceled
            }
        }
        if (type == REQUEST_EXACT_ALARM_PERMISSION) {
            if (resultCode == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(getApplicationContext(), getString(R.string.ma_permission_service_restart), Toast.LENGTH_LONG).show();
            setWarning();
        }
    }

    private void showWebPage(String url) {
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
    }

    public void startVpnService() {
        if (checkMitmSetting()) {
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
    }

    public void stopVpnService() {
        viewModel.stopVpnService(getApplicationContext());
        vpnbtn.setChecked(false);
        sendUserAnalytics(getApplicationContext(), END_SNIFFER, null);
    }

    public void showVpnServiceNotification() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
            setPreferences(getApplicationContext(), PREF_VPNSERVICE_USAGE_AGREE, true);
            startVpnService();
        }).setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
            stopVpnService();
        }).setCancelable(false);
        alert.setMessage(Html.fromHtml(getString(R.string.ma_dialog_vpn_usage)));
        AlertDialog dialog = alert.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private boolean checkMitmSetting() {
        if (getBooleanPreferences(this, PREF_USE_TLS_DECRYPTION)) {
            MitmAddon.setCAInstallationSkipped(this, false);
            if (MitmAddon.needsSetup(this)) {
                Intent intent = new Intent(this, MitmSetupWizardActivity.class);
                startActivity(intent);
                return false;
            }

            if (!MitmAddon.getNewVersionAvailable(this).isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.update_available)
                        .setMessage(R.string.mitm_addon_update_available)
                        .setCancelable(false)
                        .setPositiveButton(R.string.update_action, (dialog, whichButton) -> {
                            Intent intent = new Intent(this, MitmSetupWizardActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.cancel_action, (dialog, whichButton) -> {
                            MitmAddon.ignoreNewVersion(this);
                            startVpnService();
                        })
                        .show();
                return false;
            }
        }
        return true;
    }

    private void handleOpenToolIntent(Intent intent) {
        boolean result = ACTION_OPEN_TOOL.equals(intent.getAction())
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0;
        if (result && !mToolOpened) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            mToolOpened = true;
        }
    }

    private class BackPressCloseHandler {
        private static final int INTERVAL = 1500;
        long pressedTime = 0;
        Toast toast;

        public BackPressCloseHandler (){}

        public void onBackPressed() {
            FrameLayout bottomSheet = findViewById(R.id.bottomSheet);
            BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                return;
            }
            if (System.currentTimeMillis() > pressedTime + INTERVAL) {
                pressedTime = System.currentTimeMillis();
                showMessage();
                return;
            }
            if (System.currentTimeMillis() <= pressedTime + INTERVAL) {
                MainActivity.this.finish();
                toast.cancel();
            }
        }

        public void showMessage() {
            toast = Toast.makeText(MainActivity.this, getString(R.string.backpress_msg), Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}