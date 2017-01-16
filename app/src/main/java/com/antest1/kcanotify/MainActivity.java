package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;

import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_V_HD;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_OPENDB_API_USE;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "KCAV";
    public final static String KC_PACKAGE_NAME = "com.dmm.dmmlabo.kancolle";
    private static final int REQUEST_VPN = 1;
    private static final int REQUEST_SVC = 2;


    public static boolean isKcaServiceOn = false;
    Toolbar toolbar;

    private boolean running = false;
    private AlertDialog dialogVpn = null;
    public static Context ctx;

    ToggleButton vpnbtn, svcbtn;
    Button kcbtn;
    TextView textDescription = null;

    Boolean is_kca_installed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        vpnbtn = (ToggleButton) findViewById(R.id.vpnbtn);
        vpnbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (isChecked) {
                    try {
                        final Intent prepare = VpnService.prepare(MainActivity.this);
                        if (prepare == null) {
                            Log.i(TAG, "Prepare done");
                            onActivityResult(REQUEST_VPN, RESULT_OK, null);
                        } else {
                            startActivityForResult(prepare, REQUEST_VPN);
                        }
                    } catch (Throwable ex) {
                        // Prepare failed
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                } else {
                    KcaVpnService.stop("switch off", MainActivity.this);
                    prefs.edit().putBoolean("enabled", false).apply();
                }
            }
        });

        svcbtn = (ToggleButton) findViewById(R.id.svcbtn);
        svcbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, KcaService.class);
                if(isChecked) {
                    if (is_kca_installed) {
                        onActivityResult(REQUEST_SVC, RESULT_OK, intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "칸코레가 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    stopService(intent);
                }

            }
        });

        kcbtn = (Button) findViewById(R.id.kcbtn);

        textDescription = (TextView) findViewById(R.id.textDescription);
        textDescription.setText(R.string.description_proxy);
        Linkify.addLinks(textDescription, Linkify.WEB_URLS);

        ctx = getApplicationContext();

        if (isPackageExist(KC_PACKAGE_NAME)) {
            final Intent kcIntent = getPackageManager().getLaunchIntentForPackage(KC_PACKAGE_NAME);
            kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            is_kca_installed = true;
            kcbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(kcIntent);
                    finish();
                }
            });
        } else {
            is_kca_installed = false;
        }
        setDefaultPreferences();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVpnBtn();
        setCheckBtn();
    }

    @Override
    protected void onResume() {
        super.onStart();
        setVpnBtn();
        setCheckBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    public void setVpnBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vpnbtn.setChecked(prefs.getBoolean("enabled", false));
    }

    public void setCheckBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        svcbtn.setChecked(prefs.getBoolean("svcenabled", false));
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

    public boolean isPackageExist(String name) {
        boolean isExist = false;

        PackageManager pkgMgr = getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(name)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    private void setDefaultPreferences() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        for (String prefKey : PREFS_LIST) {
            if (!pref.contains(prefKey)) {
                Log.e("KCA", prefKey + " pref add");
                switch (prefKey) {
                    case PREF_KCA_SEEK_CN:
                        editor.putString(prefKey, String.valueOf(SEEK_33CN1));
                        break;
                    case PREF_OPENDB_API_USE:
                        editor.putBoolean(prefKey, false);
                        break;
                    case PREF_KCA_EXP_VIEW:
                    case PREF_KCA_NOTI_DOCK:
                    case PREF_KCA_NOTI_EXP:
                    case PREF_KCA_NOTI_V_HD:
                        editor.putBoolean(prefKey, true);
                        break;
                    default:
                        editor.putString(prefKey, "");
                        break;
                }
            }
        }
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.i(TAG, "onActivityResult request=" + requestCode + " result=" + resultCode + " ok=" + (resultCode == RESULT_OK));
        if (requestCode == REQUEST_VPN) {
            Log.e("KCA", "Request_vpn");
            prefs.edit().putBoolean("enabled", resultCode == RESULT_OK).apply();
            if (resultCode == RESULT_OK) {
                KcaVpnService.start("prepared", this);
            } else if (resultCode == RESULT_CANCELED) {
                // Canceled
            }
        } else if (requestCode == REQUEST_SVC) {
            Log.e("KCA", "Request_svc");
            Log.e("KCA", String.valueOf(resultCode == RESULT_OK));
            prefs.edit().putBoolean("svcenabled", resultCode == RESULT_OK).apply();
            if (resultCode == RESULT_OK) {
                setCheckBtn();
                startService(data);
            }
        }
    }
}

