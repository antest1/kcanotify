package com.antest1.kcanotify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_EXP_VIEW;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_V_HD;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_OPENDB_API_USE;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String KC_PACKAGE_NAME = "com.dmm.dmmlabo.kancolle";
    public static boolean isKcaServiceOn = false;

    Toolbar toolbar;

    Button btnStart, btnCheck, btnWifi, btnApn;
    TextView textDescription;
    Boolean is_kca_installed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Toast.makeText(getApplicationContext(),
        // String.valueOf(KcaProxyServer.is_on), Toast.LENGTH_LONG).show();
        KcaProxyServer.cntx = getApplicationContext();

        btnStart = (Button) findViewById(R.id.btnStart);
        btnCheck = (Button) findViewById(R.id.btnCheck);
        btnWifi = (Button) findViewById(R.id.btnWifi);
        btnApn = (Button) findViewById(R.id.btnApn);
        textDescription = (TextView) findViewById(R.id.textDescription);

        btnStart.setBackgroundResource(R.color.colorBtn);
        btnWifi.setBackgroundResource(R.color.colorBtn);
        btnApn.setBackgroundResource(R.color.colorBtn);
        setCheckBtn();

        textDescription.setText(R.string.description_proxy);
        Linkify.addLinks(textDescription, Linkify.WEB_URLS);

        findViewById(R.id.btnStart).setOnClickListener(this);
        findViewById(R.id.btnCheck).setOnClickListener(this);
        findViewById(R.id.btnWifi).setOnClickListener(this);
        findViewById(R.id.btnApn).setOnClickListener(this);
        findViewById(R.id.btnApn).setOnClickListener(this);

        if (isPackageExist(KC_PACKAGE_NAME)) {
            // Toast.makeText(getApplicationContext(),
            // String.valueOf("Kancolle Found"), Toast.LENGTH_LONG).show();
            KcaService.kcIntent = getPackageManager().getLaunchIntentForPackage(KC_PACKAGE_NAME);
            KcaService.kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            is_kca_installed = true;
        } else {
            is_kca_installed = false;
        }
        setDefaultPreferences();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isKcaServiceOn = KcaService.getServiceStatus();
        setCheckBtn();
    }

    @Override
    protected void onResume() {
        super.onStart();
        isKcaServiceOn = KcaService.getServiceStatus();
        setCheckBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();

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

    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        // mDatagramSocketThread = new DatagramSocketThread();
        // mDatagramSocketThread.start();
        if (v.getId() == R.id.btnStart && isKcaServiceOn && is_kca_installed) {
            startActivity(KcaService.kcIntent);
            finish();
        }

        if (v.getId() == R.id.btnCheck) {
            if (is_kca_installed) {
                if (!KcaService.getServiceStatus()) {
                    Intent intent = new Intent(MainActivity.this, KcaService.class);
                    startService(intent);
                    isKcaServiceOn = true;
                } else {
                    Intent intent = new Intent(MainActivity.this, KcaService.class);
                    stopService(intent);
                    isKcaServiceOn = false;
                }
                setCheckBtn();
            } else {
                Toast.makeText(this, "칸코레가 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show();
            }
        }

        if (v.getId() == R.id.btnWifi) {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }

        if (v.getId() == R.id.btnApn) {
            startActivity(new Intent(Settings.ACTION_APN_SETTINGS));
        }
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

    public void setCheckBtn() {
        if (isKcaServiceOn) {
            btnCheck.setBackgroundResource(R.color.colorAccent);
            btnCheck.setTextColor(Color.WHITE);
            btnCheck.setText("ON");
        } else {
            btnCheck.setBackgroundResource(R.color.colorBtn);
            btnCheck.setTextColor(Color.BLACK);
            btnCheck.setText("OFF");
        }
    }

    private void setDefaultPreferences() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        for(String prefKey: PREFS_LIST) {
            if(!pref.contains(prefKey)) {
                Log.e("KCA", prefKey + " pref add");
                switch(prefKey) {
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

}

