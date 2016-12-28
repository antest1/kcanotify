package com.antest1.kcanotify;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static String KC_PACKAGE_NAME = "com.dmm.dmmlabo.kancolle";
    public static boolean isKcaServiceOn = false;

    Button btnStart, btnCheck, btnWifi, btnApn, btnUpdate;
    TextView textDescription;
    Boolean is_kca_installed = false;
    public static String currentVersion = BuildConfig.VERSION_NAME;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toast.makeText(getApplicationContext(),
        // String.valueOf(KcaProxyServer.is_on), Toast.LENGTH_LONG).show();
        KcaProxyServer.cntx = getApplicationContext();

        btnStart = (Button) findViewById(R.id.btnStart);
        btnCheck = (Button) findViewById(R.id.btnCheck);
        btnWifi = (Button) findViewById(R.id.btnWifi);
        btnApn = (Button) findViewById(R.id.btnApn);
        textDescription = (TextView) findViewById(R.id.textDescription);
        btnUpdate = (Button) findViewById(R.id.btnUpdate);

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
        findViewById(R.id.btnUpdate).setOnClickListener(this);

        if (isPackageExist(KC_PACKAGE_NAME)) {
            // Toast.makeText(getApplicationContext(),
            // String.valueOf("Kancolle Found"), Toast.LENGTH_LONG).show();
            KcaService.kcIntent = getPackageManager().getLaunchIntentForPackage(KC_PACKAGE_NAME);
            KcaService.kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            is_kca_installed = true;
        } else {
            is_kca_installed = false;
        }
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
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(Settings.ACTION_APN_SETTINGS));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        // mDatagramSocketThread = new DatagramSocketThread();
        // mDatagramSocketThread.start();
        if (v.getId() == R.id.btnStart && KcaProxyServer.is_on() && is_kca_installed) {
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

        if (v.getId() == R.id.btnUpdate) {
            new getRecentVersion().execute();
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

    private class getRecentVersion extends AsyncTask<Context, String, String> {

        private void serverTempStart() {
            if(!KcaProxyServer.is_on()) KcaProxyServer.start(null);
        }

        private void serverTempStop() {
            if(KcaProxyServer.handler == null) KcaProxyServer.stop();
        }

        @Override
        protected void onPreExecute() {
            serverTempStart();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... params) {
            String content = null;
            try {
                content = executeClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content;
        }

        public byte[] gzipcompress(String value) throws Exception {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                    new BufferedOutputStream(byteArrayOutputStream));
            gzipOutStream.write(value.getBytes());
            gzipOutStream.finish();
            gzipOutStream.close();

            return byteArrayOutputStream.toByteArray();
        }

        public String executeClient() {

            URL data_send_url = null;
            try {
                data_send_url = new URL(String.format("http://antest.hol.es/kcanotify/v.php"));
                HttpURLConnection http = (HttpURLConnection) data_send_url.openConnection();
                http.setRequestMethod("GET");
                http.setDoInput(true);
                http.setRequestProperty("Referer", "app:/KCA/");
                http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                http.getResponseCode();
                InputStream is = http.getInputStream();
                byte[] bytes = ByteStreams.toByteArray(is);
                String input_data = new String(bytes);

                return input_data;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(getApplicationContext(), "업데이트 확인에 문제가 발생했습니다.", Toast.LENGTH_LONG).show();
                serverTempStop();
            } else {
                JsonObject jsonDataObj = new JsonParser().parse(result).getAsJsonObject();
                if (jsonDataObj.has("version")) {
                    String recentVersion = jsonDataObj.get("version").getAsString();
                    if (recentVersion.equals(currentVersion)) {
                        Toast.makeText(getApplicationContext(), String.format("최신 버전입니다(%s).", currentVersion), Toast.LENGTH_LONG).show();
                        serverTempStop();
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                        alertDialog.setMessage(String.format("최신 업데이트가 있습니다(%s).\n다운로드하시겠습니까?", recentVersion));
                        alertDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String downloadUrl = "http://bit.ly/2hNln5o";
                                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                                startActivity(i);
                            }
                        });
                        alertDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                serverTempStop();
                            }
                        });
                        AlertDialog alert = alertDialog.create();
                        alert.setIcon(R.mipmap.ic_launcher);
                        alert.setTitle("최신 업데이트 확인");
                        alert.show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "업데이트 서버 데이터 오류", Toast.LENGTH_LONG).show();
                    serverTempStop();
                }
            }
        }
    }

}

