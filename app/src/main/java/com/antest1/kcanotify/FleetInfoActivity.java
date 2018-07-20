package com.antest1.kcanotify;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
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
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.R.attr.orientation;
import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.R.id.fleetlist_raw;


public class FleetInfoActivity extends AppCompatActivity {
    static final String KC_REQ_LIST = "name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num";

    Toolbar toolbar;
    KcaDBHelper dbHelper;
    Context contextWithLocale;

    static int current_fleet = 0;
    static boolean is_portrait = true;
    TextView fleetlist_name, fleetlist_fp, fleetlist_seek, fleetlist_loading;
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

        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
        deckInfoCalc = new KcaDeckInfo(getApplicationContext(), contextWithLocale);
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
                        new ShipItemLoadTask().execute();
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        fleetlist_name = findViewById(R.id.fleetlist_name);
        fleetlist_ships = findViewById(R.id.fleetlist_ships);
        fleetlist_fp = findViewById(R.id.fleetlist_fp);
        fleetlist_seek = findViewById(R.id.fleetlist_seek);
        fleetlist_loading = findViewById(R.id.fleetlist_loading);
        for (int i = 1; i <= 7; i++) {
            KcaFleetInfoItemAdapter.alv_format[i] = getStringWithLocale(getId(KcaUtils.format("alv_%d", i), R.string.class));
        }

        adapter = new KcaFleetInfoItemAdapter();
        fleetlist_ships.setAdapter(adapter);

        is_portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (is_portrait) {
            fleetlist_ships.setNumColumns(1);
        } else {
            fleetlist_ships.setNumColumns(2);
        }

        new ShipItemLoadTask().execute();
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    private class ShipItemLoadTask extends AsyncTask<String, String, JsonArray> {
        String fleet_name = "";

        @Override
        protected void onPreExecute() {
            fleetlist_loading.setVisibility(View.VISIBLE);
        }

        @Override
        protected JsonArray doInBackground(String[] params) {
            JsonArray deck_data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
            JsonArray data = null;
            if (deck_data == null) {
                fleet_name = "-";
                return null;
            } else {
                if (current_fleet == 4) {
                    fleet_name = getStringWithLocale(R.string.fleetlist_combined_fleet);
                    if (deck_data.size() < 2) {
                        return null;
                    } else {
                        KcaFleetInfoItemAdapter.is_combined = true;
                        data = deckInfoCalc.getDeckListInfo(deck_data, 0, "all", KC_REQ_LIST);
                        JsonArray data_c = deckInfoCalc.getDeckListInfo(deck_data, 1, "all", KC_REQ_LIST);
                        while (!is_portrait && data.size() < 6) data.add(new JsonObject());

                        for (int i = 0; i < data_c.size(); i++) {
                            JsonObject item_c = data_c.get(i).getAsJsonObject();
                            item_c.addProperty("cb_flag", true);
                            data.add(item_c);
                        }
                        while (!is_portrait && data.size() < 12) data.add(new JsonObject());
                        adapter.setListViewItemList(data);
                    }
                } else {
                    if (deck_data.size() <= current_fleet) {
                        fleet_name = String.valueOf(current_fleet + 1);
                        return null;
                    } else {
                        KcaFleetInfoItemAdapter.is_combined = false;
                        JsonObject fleet_data = deck_data.get(current_fleet).getAsJsonObject();
                        fleet_name = fleet_data.get("api_name").getAsString();
                        data = deckInfoCalc.getDeckListInfo(deck_data, current_fleet, "all", KC_REQ_LIST);
                        adapter.setListViewItemList(data);
                    }
                }
            }
            return deck_data;
        }

        @Override
        protected void onPostExecute(JsonArray deck_data) {
            fleetlist_name.setText(fleet_name);
            if (deck_data == null) {
                findViewById(R.id.fleetlist_info_area).setVisibility(View.GONE);
            } else {
                if (current_fleet == 4) {
                    findViewById(R.id.fleetlist_info_area).setVisibility(View.VISIBLE);
                    fleetlist_fp.setText(deckInfoCalc.getAirPowerRangeString(deck_data, 0, null));
                    fleetlist_seek.setText(KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_f),
                            deckInfoCalc.getSeekValue(deck_data, "0,1", SEEK_33CN1, null)));

                } else {
                    findViewById(R.id.fleetlist_info_area).setVisibility(View.VISIBLE);
                    fleetlist_fp.setText(deckInfoCalc.getAirPowerRangeString(deck_data, current_fleet, null));
                    fleetlist_seek.setText(KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_f),
                            deckInfoCalc.getSeekValue(deck_data, String.valueOf(current_fleet), SEEK_33CN1, null)));
                }
                adapter.notifyDataSetChanged();
            }
            fleetlist_loading.setVisibility(View.GONE);
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
        is_portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        if (is_portrait) {
            fleetlist_ships.setNumColumns(1);
        } else {
            fleetlist_ships.setNumColumns(2);
        }
        fleetlist_ships.invalidateViews();
    }
}
