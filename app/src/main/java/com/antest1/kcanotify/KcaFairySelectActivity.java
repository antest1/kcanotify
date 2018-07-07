package com.antest1.kcanotify;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_FAIRY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaFairySelectActivity extends AppCompatActivity {
    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();
    KcaDBHelper dbHelper;
    GridView gv;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public KcaFairySelectActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_fairy);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.setting_menu_kand_title_fairy_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        JsonArray icon_info = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "icon_info.json", dbHelper);

        List<String> fairy_id = new ArrayList<>();
        for (int i = 0; i < icon_info.size(); i++) {
            fairy_id.add("noti_icon_".concat(String.valueOf(i)));
        }

        final KcaItemAdapter adapter = new KcaItemAdapter(getApplicationContext(),
                R.layout.listview_image_item, fairy_id);

        String pref_value = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        if (pref_value.length() > 0) {
            adapter.setPrevActive(Integer.parseInt(pref_value));
        }

        gv = (GridView)findViewById(R.id.fairy_gridview);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPreferences(getApplicationContext(), PREF_FAIRY_ICON, String.valueOf(position));
                if (KcaService.getServiceStatus()) {
                    JsonObject data = new JsonObject();
                    data.addProperty("id", position);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
                    bundle.putString("data", data.toString());
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
                adapter.setPrevActive(position);
                gv.invalidateViews();
            }
        });
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}