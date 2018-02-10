package com.antest1.kcanotify;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class FleetInfoActivity extends AppCompatActivity {
    static final String KC_REQ_LIST = "name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num";

    Toolbar toolbar;
    KcaDBHelper dbHelper;

    int current_fleet = 0;
    ArrayAdapter<String> fleet_no_adapter;
    TextView fleetlist_name, fleetlist_raw, fleetlist_fp, fleetlist_seek;
    ImageView fleetlist_select;
    GridView fleetlist_ships;
    KcaFleetInfoItemAdapter adapter;
    KcaDeckInfo deckInfoCalc;

    public FleetInfoActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fleetlist);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_fleetlist));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadTranslationData(getApplicationContext());

        final String[] deck_list = {"1", "2", "3", "4", getStringWithLocale(R.string.fleetview_combined)};

        fleetlist_select = findViewById(R.id.fleetlist_select);
        fleetlist_select.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);

        fleetlist_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(FleetInfoActivity.this);
                dialog.setSingleChoiceItems(deck_list, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        current_fleet = n;
                        setShipList();
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        fleetlist_name = findViewById(R.id.fleetlist_name);
        fleetlist_raw = findViewById(R.id.fleetlist_raw);
        fleetlist_ships = findViewById(R.id.fleetlist_ships);
        fleetlist_fp = findViewById(R.id.fleetlist_fp);
        fleetlist_seek = findViewById(R.id.fleetlist_seek);

        for (int i = 1; i <= 7; i++) {
            KcaFleetInfoItemAdapter.alv_format[i] = getStringWithLocale(getId(KcaUtils.format("alv_%d", i), R.string.class));
        }

        adapter = new KcaFleetInfoItemAdapter();
        fleetlist_ships.setAdapter(adapter);
        fleetlist_ships.setNumColumns(1);
        setShipList();
    }

    private void setShipList() {
        JsonArray deck_data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (deck_data == null) {
            fleetlist_raw.setText("no data");
        } else {
            if (current_fleet == 4) {
                if (deck_data.size() < 2) {
                    findViewById(R.id.fleetlist_info_area).setVisibility(View.GONE);

                } else {
                    findViewById(R.id.fleetlist_info_area).setVisibility(View.VISIBLE);
                }
            } else {
                if (deck_data.size() <= current_fleet) {
                    findViewById(R.id.fleetlist_info_area).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.fleetlist_info_area).setVisibility(View.VISIBLE);
                    fleetlist_raw.setText(deck_data.get(current_fleet).toString());
                    JsonObject fleet_data = deck_data.get(current_fleet).getAsJsonObject();
                    String fleet_name = fleet_data.get("api_name").getAsString();
                    JsonArray data = deckInfoCalc.getDeckListInfo(deck_data, current_fleet, "all", KC_REQ_LIST);
                    fleetlist_name.setText(fleet_name);
                    adapter.setListViewItemList(data);

                    fleetlist_fp.setText(deckInfoCalc.getAirPowerRangeString(deck_data, current_fleet, null));
                    fleetlist_seek.setText(KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_f),
                            deckInfoCalc.getSeekValue(deck_data, String.valueOf(current_fleet), SEEK_33CN1, null)));
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }
}
