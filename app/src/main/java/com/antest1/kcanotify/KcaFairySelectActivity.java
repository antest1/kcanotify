package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_FAIRY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_DOWN_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_REV;
import static com.antest1.kcanotify.KcaUseStatConstant.SELECT_FAIRY;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaFairySelectActivity extends BaseActivity {
    public final static boolean FAIRY_SPECIAL_FLAG = false;
    public final static int FAIRY_SPECIAL_PREFIX = 900;
    public final static int FAIRY_SPECIAL_COUNT = 16;

    Toolbar toolbar;
    GridView gv;
    KcaDBHelper dbHelper;
    KcaFairyDownloader downloader;

    private static Handler sHandler;
    public static void setHandler(Handler h) {
        sHandler = h;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_fairy);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.setting_menu_kand_title_fairy_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        JsonArray icon_info = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "icon_info.json", dbHelper);
        Log.e("KCA-FS", icon_info.toString());
        List<String> fairy_id = new ArrayList<>();

        downloader = new KcaFairyDownloader(this);

        boolean fairy_downloaded = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_DOWN_FLAG);
        int fairy_size = fairy_downloaded ? icon_info.size() : 1;

        if (FAIRY_SPECIAL_FLAG) {
            for (int i = 0; i < FAIRY_SPECIAL_COUNT; i++) {
                fairy_id.add("noti_icon_".concat(String.valueOf(i + FAIRY_SPECIAL_PREFIX)));
            }
        }

        for (int i = 0; i < fairy_size; i++) {
            fairy_id.add("noti_icon_".concat(String.valueOf(i)));
        }

        final KcaItemAdapter adapter = new KcaItemAdapter(getApplicationContext(),
                R.layout.listview_image_item, fairy_id);

        String pref_value = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        if (!pref_value.isEmpty()) {
            adapter.setPrevActive(Integer.parseInt(pref_value));
        }

        gv = findViewById(R.id.fairy_gridview);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener((parent, view, position, id) -> {
            int value = Integer.parseInt(fairy_id.get(position).replace("noti_icon_", ""));
            setPreferences(getApplicationContext(), PREF_FAIRY_REV, 0);
            setPreferences(getApplicationContext(), PREF_FAIRY_ICON, String.valueOf(value));
            if (KcaService.getServiceStatus()) {
                JsonObject data = new JsonObject();
                data.addProperty("id", value);
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
                bundle.putString("data", data.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }
            adapter.setPrevActive(position);
            gv.invalidateViews();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fairyselect, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_fairy_down:
                downloader.run(true);
                return true;
            case R.id.action_fairy_init:
                setPreferences(getApplicationContext(), PREF_FAIRY_DOWN_FLAG, false);
                setPreferences(getApplicationContext(), PREF_FAIRY_ICON, "0");
                changeFairyInService(false);
                KcaUtils.clearFairyImageFileFromStorage(getApplicationContext());
                Toast.makeText(getApplicationContext(), "cleared", Toast.LENGTH_LONG).show();
                finish();
                return true;
            case R.id.action_fairy_rev:
                int current_rev = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_REV));
                setPreferences(getApplicationContext(), PREF_FAIRY_REV, 1 - current_rev);
                changeFairyInService(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeFairyInService(boolean send_analytics) {
        if (KcaService.getServiceStatus()) {
            int current_id = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
            JsonObject data = new JsonObject();
            data.addProperty("id", current_id);
            sendUserAnalytics(getApplicationContext(), SELECT_FAIRY, data);

            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
            bundle.putString("data", data.toString());
            Message sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
    }
}