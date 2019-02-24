package com.antest1.kcanotify;

import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

import static android.widget.Toast.makeText;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;


public class ExpeditionTableActivity extends AppCompatActivity {
    static final int SHIPINFO_GET_SORT_KEY = 1;
    static final int SHIPINFO_GET_FILTER_RESULT = 2;

    Toolbar toolbar;
    static Gson gson = new Gson();
    ListView listview;

    TextView greatscbtn;
    boolean is_great_success;
    View daihatsubtn;
    int daihatsu_count = 0;
    JsonArray expeditionData = new JsonArray();
    int world_idx = 0;

    KcaDBHelper dbHelper;
    KcaExpeditionTableViewAdpater adapter;

    public ExpeditionTableActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expeditiontable_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_expdtable));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadTranslationData(getApplicationContext());
        loadSimpleExpeditionInfoFromStorage(getApplicationContext());

        AssetManager assetManager = getAssets();

        String locale = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
        adapter = new KcaExpeditionTableViewAdpater(getApplicationContext(), getBaseContext(), getResourceLocaleCode(locale));

        for (int i = 0; i <= 5; i++) {
            final int target = i;
            TextView w_btn = findViewById(getId(KcaUtils.format("btn_w%d", i), R.id.class));
            w_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    world_idx = target;
                    adapter.setListViewItemList(expeditionData, world_idx);
                    listview = findViewById(R.id.expeditiontable_listiview);
                    listview.setAdapter(adapter);
                }
            });
        }

        greatscbtn = findViewById(R.id.btn_success_type);
        setGsButtonStyle(greatscbtn);
        greatscbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView tv = (TextView) view;
                is_great_success = !is_great_success;
                setGsButtonStyle(tv);
                adapter.setGreatSuccess(is_great_success);
                adapter.notifyDataSetChanged();
            }
        });

        daihatsubtn = findViewById(R.id.btn_daihatsu);
        ((TextView) daihatsubtn.findViewById(R.id.btn_daihatsu_value)).setText(getDaihatsuCountString(0));
        daihatsubtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView val = view.findViewById(R.id.btn_daihatsu_value);
                daihatsu_count = (daihatsu_count + 1) % 5;
                val.setText(getDaihatsuCountString(daihatsu_count));
                adapter.setDaihatsuCount(daihatsu_count);
                adapter.notifyDataSetChanged();
            }
        });

        expeditionData = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "expedition.json", dbHelper);
        showDataLoadErrorToast(getApplicationContext(), getStringWithLocale(R.string.download_check_error));
        adapter.setListViewItemList(expeditionData, world_idx);
        listview = findViewById(R.id.expeditiontable_listiview);
        listview.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && adapter != null && requestCode > 0) {
            // Filter task here
            adapter.notifyDataSetChanged();
            listview.setAdapter(adapter);
        }
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

    private String getDaihatsuCountString(int val) {
        return "x".concat(String.valueOf(val));
    }

    private void setGsButtonStyle(TextView tv) {
        if (is_great_success) {
            tv.setText(getStringWithLocale(R.string.expdtable_btn_s2));
            tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
            tv.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            tv.setText(getStringWithLocale(R.string.expdtable_btn_s1));
            tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnText));
            tv.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
        }
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
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
        if(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        loadTranslationData(getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}
