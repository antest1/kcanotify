package com.antest1.kcanotify;

import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.helper;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_EQUIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaEquipListViewAdpater.STAT_KEYS;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class EquipmentInfoActivity extends AppCompatActivity {
    static final int EQUIPINFO_GET_SORT_KEY = 1;
    static final int EQUIPINFO_GET_FILTER_RESULT = 2;

    Toolbar toolbar;
    static Gson gson = new Gson();
    ListView listview;
    Button filterBtn;
    EditText searchEditText;

    KcaDBHelper dbHelper;
    KcaEquipListViewAdpater adapter;
    JsonArray equipment_data = new JsonArray();
    JsonArray ship_data = new JsonArray();
    JsonObject ship_equip_info = new JsonObject();
    Map<String, AtomicInteger> counter = new HashMap<>();

    public EquipmentInfoActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_equipmentinfo));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadTranslationData(getApplicationContext());

        AssetManager assetManager = getAssets();

        equipment_data = KcaApiData.getKcSlotitemGameData();
        JsonArray user_equipment_data = dbHelper.getItemData();
        String filtcond = getStringPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND);

        for (JsonElement data: user_equipment_data) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = new JsonParser().parse(equip.get("value").getAsString()).getAsJsonObject();
            String id = getItemKey(value);
            if(!counter.containsKey(id)) {
                counter.put(id,  new AtomicInteger(1));
            } else {
                counter.get(id).incrementAndGet();
            }
        }

        ship_data = dbHelper.getJsonArrayValue(DB_KEY_SHIPIFNO);
        if (ship_data != null) {
            for (int i = 0; i < ship_data.size(); i++) {
                List<String> item_list = new ArrayList<>();
                JsonObject ship_info = ship_data.get(i).getAsJsonObject();
                int ship_id = ship_info.get("api_id").getAsInt();
                int ship_lv = ship_info.get("api_lv").getAsInt();
                JsonObject ship_kc_info = getKcShipDataById(ship_info.get("api_ship_id").getAsInt(), "name");
                String ship_name = "";
                if (ship_kc_info != null) ship_name = ship_kc_info.get("name").getAsString();

                JsonObject ship_id_lv = new JsonObject();
                ship_id_lv.addProperty("id", ship_id);
                ship_id_lv.addProperty("name", ship_name);
                ship_id_lv.addProperty("lv", ship_lv);

                JsonArray shipItem = ship_info.getAsJsonArray("api_slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id > 0) {
                        String item_info_str = helper.getItemValue(item_id);
                        if (item_info_str != null) {
                            JsonObject item_info = new JsonParser().parse(item_info_str).getAsJsonObject();
                            item_list.add(getItemKey(item_info));
                        }
                    }
                }
                int ex_item_id = ship_info.get("api_slot_ex").getAsInt();
                if (ex_item_id > 0) {
                    String item_info_str = helper.getItemValue(ex_item_id);
                    if (item_info_str != null) {
                        JsonObject item_info = new JsonParser().parse(item_info_str).getAsJsonObject();
                        item_list.add(getItemKey(item_info));
                    }
                }

                for (String item: item_list) {
                    if (!ship_equip_info.has(item)) {
                        ship_equip_info.add(item, new JsonArray());
                    }
                    ship_equip_info.getAsJsonArray(item).add(ship_id_lv);
                }
            }
        }

        JsonObject itemStatTranslation = new JsonObject();
        for (String key: KcaEquipListViewAdpater.STAT_KEYS) {
            itemStatTranslation.addProperty(key, getStringWithLocale(getId("text_"+key, R.string.class)));
        }
        itemStatTranslation.addProperty("api_houm2", getStringWithLocale(R.string.text_api_houm2));
        itemStatTranslation.addProperty("api_houk2", getStringWithLocale(R.string.text_api_houk2));
        for (int i = 1; i <= 4; i++) {
            itemStatTranslation.addProperty("api_leng"+i, getStringWithLocale(getId("text_api_leng_"+i, R.string.class)));
        }

        adapter = new KcaEquipListViewAdpater();
        adapter.setSummaryFormat(getStringWithLocale(R.string.equipinfo_summary));
        adapter.setStatTranslation(itemStatTranslation);
        adapter.setListViewItemList(equipment_data, counter, ship_equip_info, filtcond);

        listview = findViewById(R.id.equipment_listview);
        listview.setAdapter(adapter);

        filterBtn = findViewById(R.id.equipment_btn_filter);
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent aIntent = new Intent(EquipmentInfoActivity.this, EquipmentListFilterActivity.class);
                startActivityForResult(aIntent, EQUIPINFO_GET_FILTER_RESULT);
            }
        });
        setfilterBtn(!filtcond.equals("all"));

        searchEditText = findViewById(R.id.equipinfo_search);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filtcond = getStringPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND);
                adapter.setSearchQuery(s.toString());
                adapter.setListViewItemList(equipment_data, counter, ship_equip_info, filtcond);
                adapter.notifyDataSetChanged();
                listview.setAdapter(adapter);
                listview.invalidateViews();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private String getItemKey(JsonObject item) {
        if (item.has("api_alv")) {
            return KcaUtils.format("%d_%d_%d", item.get("api_slotitem_id").getAsInt(),
                    item.get("api_level").getAsInt(), item.get("api_alv").getAsInt());
        } else {
            return KcaUtils.format("%d_%d_n", item.get("api_slotitem_id").getAsInt(), item.get("api_level").getAsInt());
        }
    }

    private void setfilterBtn(boolean is_active) {
        if (is_active) {
            filterBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
            filterBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            filterBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnText));
            filterBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String filtcond = getStringPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND);

        if (resultCode == RESULT_OK && adapter != null && requestCode > 0) {
            if (requestCode == EQUIPINFO_GET_FILTER_RESULT) {
                adapter.setListViewItemList(equipment_data, counter, ship_equip_info, filtcond);
                setfilterBtn(!filtcond.equals("all"));
            }
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
