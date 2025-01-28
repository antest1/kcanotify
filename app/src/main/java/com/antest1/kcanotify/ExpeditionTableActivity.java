package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromStorage;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;


public class ExpeditionTableActivity extends AppCompatActivity {

    Toolbar toolbar;
    static Gson gson = new Gson();
    ListView listview;

    Chip greatscbtn;
    boolean is_great_success;
    Chip daihatsubtn;
    int daihatsu_count = 0;
    JsonArray expeditionData = new JsonArray();
    int world_idx = 0;

    KcaDBHelper dbHelper;
    KcaExpeditionTableViewAdpater adapter;

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
        loadSimpleExpeditionInfoFromStorage(getApplicationContext());

        adapter = new KcaExpeditionTableViewAdpater(getApplicationContext(), getBaseContext());

        for (int i = 0; i <= 7; i++) {
            final int target = i;
            TextView w_btn = findViewById(getId(KcaUtils.format("btn_w%d", i), R.id.class));
            w_btn.setOnClickListener(view -> {
                world_idx = target;
                adapter.setListViewItemList(expeditionData, world_idx);
                listview = findViewById(R.id.expeditiontable_listiview);
                listview.setAdapter(adapter);
            });
        }

        greatscbtn = findViewById(R.id.btn_success_type);
        setGsButtonStyle(greatscbtn);
        greatscbtn.setOnClickListener(view -> {
            Chip chip = (Chip) view;
            is_great_success = !is_great_success;
            setGsButtonStyle(chip);
            adapter.setGreatSuccess(is_great_success);
            adapter.notifyDataSetChanged();
        });

        daihatsubtn = findViewById(R.id.btn_daihatsu);
        daihatsubtn.setText(getDaihatsuCountString(0));
        daihatsubtn.setOnClickListener(view -> {
            daihatsu_count = (daihatsu_count + 1) % 5;
            ((Chip)view).setText(getDaihatsuCountString(daihatsu_count));
            adapter.setDaihatsuCount(daihatsu_count);
            adapter.notifyDataSetChanged();
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
        return "×".concat(String.valueOf(val));
    }

    private void setGsButtonStyle(Chip tv) {
        if (is_great_success) {
            tv.setText(getStringWithLocale(R.string.expdtable_btn_s2));
        } else {
            tv.setText(getStringWithLocale(R.string.expdtable_btn_s1));
        }
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }
}
