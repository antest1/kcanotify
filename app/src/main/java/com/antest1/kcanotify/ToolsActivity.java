package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_TOOL;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;


public class ToolsActivity extends AppCompatActivity {
    Toolbar toolbar;
    KcaDBHelper dbHelper;
    static Gson gson = new Gson();
    MaterialCardView view_fleetlist, view_shiplist, view_equipment, view_droplog, view_reslog, view_akashi, view_expcalc, view_expdtable;

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_tools));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        view_fleetlist = findViewById(R.id.action_fleetlist);
        view_shiplist = findViewById(R.id.action_shiplist);
        view_equipment = findViewById(R.id.action_equipment);
        view_droplog = findViewById(R.id.action_droplog);
        view_reslog = findViewById(R.id.action_reslog);
        view_akashi = findViewById(R.id.action_akashi);
        view_expcalc = findViewById(R.id.action_expcalc);
        view_expdtable = findViewById(R.id.action_expdtable);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        JsonObject kcDataObj = dbHelper.getJsonObjectValue(DB_KEY_STARTDATA);
        if (kcDataObj != null && kcDataObj.has("api_data")) {
            KcaApiData.getKcGameData(kcDataObj.getAsJsonObject("api_data"));
        }

        view_fleetlist.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("FleetInfo"), null);
            Intent intent = new Intent(ToolsActivity.this, FleetInfoActivity.class);
            startActivity(intent);
        });

        view_shiplist.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("ShipList"), null);
            Intent intent = new Intent(ToolsActivity.this, ShipInfoActivity.class);
            startActivity(intent);
        });

        view_equipment.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("EquipList"), null);
            Intent intent = new Intent(ToolsActivity.this, EquipmentInfoActivity.class);
            startActivity(intent);
        });

        view_droplog.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("DropLog"), null);
            Intent intent = new Intent(ToolsActivity.this, DropLogActivity.class);
            startActivity(intent);
        });

        view_reslog.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("ResourceLog"), null);
            Intent intent = new Intent(ToolsActivity.this, ResourceLogActivity.class);
            startActivity(intent);
        });

        view_akashi.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("AkashiList"), null);
            Intent intent = new Intent(ToolsActivity.this, AkashiActivity.class);
            startActivity(intent);
        });

        view_expcalc.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("ExperienceCalc"), null);
            Intent intent = new Intent(ToolsActivity.this, ExpCalcActivity.class);
            startActivity(intent);
        });

        view_expdtable.setOnClickListener(view -> {
            sendUserAnalytics(getApplicationContext(), OPEN_TOOL.concat("ExpeditionTable"), null);
            Intent intent = new Intent(ToolsActivity.this, ExpeditionTableActivity.class);
            startActivity(intent);
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
