package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.T3_COUNT;
import static com.antest1.kcanotify.KcaConstants.PREF_EQUIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class EquipmentListFilterActivity extends BaseActivity {
    public static final int IMG_SIZE_DP = 36;
    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();
    GridView gv;
    Button btnAll, btnClear, btnReverse;
    KcaItemMultipleAdapter adapter;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_list_filter);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.equipinfo_filter));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        List<Integer> equipment_id = new ArrayList<>();
        for(int i = 0; i < T3_COUNT; i++) {
            try {
                int btnId = getId("item_".concat(String.valueOf(i+1)), R.mipmap.class);
                equipment_id.add(btnId);
            } catch (RuntimeException e){
                equipment_id.add(0);
            }
        }

        while (equipment_id.get(equipment_id.size() - 1) == 0) {
            equipment_id.remove(equipment_id.size() - 1);
        }

        final float scale = getResources().getDisplayMetrics().density;
        int dp_size  = (int) (IMG_SIZE_DP * scale);
        adapter = new KcaItemMultipleAdapter(getApplicationContext(),
                R.layout.listview_image_item, equipment_id);
        adapter.setRescaleDp(dp_size);

        String pref_value = getStringPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND);
        adapter.loadFromPreference(pref_value);

        gv = findViewById(R.id.equipment_gridview);
        gv.setColumnWidth(dp_size);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener((parent, view, position, id) -> {
            adapter.reverseSelected(position);
            gv.invalidateViews();
        });

        btnAll = findViewById(R.id.equipment_filter_btn_all);
        btnClear = findViewById(R.id.equipment_filter_btn_none);
        btnReverse = findViewById(R.id.equipment_filter_btn_reverse);
        btnAll.setOnClickListener(mClickListener);
        btnClear.setOnClickListener(mClickListener);
        btnReverse.setOnClickListener(mClickListener);
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == btnAll.getId()) {
                adapter.selectAll();
            } else if (view.getId() == btnClear.getId()) {
                adapter.unselectAll();
            } else if (view.getId() == btnReverse.getId()) {
                adapter.reverseSelect();
            }
            gv.invalidateViews();
        }
    };

    private void setValueAndFinish() {
        setPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND, adapter.getPreference());
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() { setValueAndFinish(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setValueAndFinish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}