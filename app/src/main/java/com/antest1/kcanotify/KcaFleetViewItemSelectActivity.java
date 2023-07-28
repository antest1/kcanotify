package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaApiData.getUseItemDataByUseType;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_1;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_2;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_3;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_4;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class KcaFleetViewItemSelectActivity extends AppCompatActivity {
    public static final int API_USETYPE_4 = 4;
    public static final String[] FV_ITEMS = {PREF_FV_ITEM_1, PREF_FV_ITEM_2, PREF_FV_ITEM_3, PREF_FV_ITEM_4};

    Toolbar toolbar;
    Spinner[] spinnerUseItem;
    JsonArray useItemList = getUseItemDataByUseType(API_USETYPE_4);

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_fleetview_items);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.setting_menu_kand_title_fleetview_item_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        spinnerUseItem = new Spinner[4];
        for (int i = 0; i < spinnerUseItem.length; i++) {
            final int idx = i;
            int pref_value = Integer.parseInt(KcaUtils.getStringPreferences(getApplicationContext(), FV_ITEMS[i]));
            spinnerUseItem[i] = findViewById(KcaUtils.getId(KcaUtils.format("fleetview_item_%d", i+1), R.id.class));
            spinnerUseItem[i].setAdapter(getUseItemListAdapter());
            spinnerUseItem[i].setSelection(getIndexByUserItemId(pref_value));
            spinnerUseItem[i].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    KcaUtils.setPreferences(getApplicationContext(), FV_ITEMS[idx], getUseItemIdByIndex(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private int getUseItemIdByIndex(int idx) {
       if (idx == 0) return -1;
       else {
           JsonObject item = useItemList.get(idx-1).getAsJsonObject();
           return item.get("id").getAsInt();
       }
    }

    private int getIndexByUserItemId(int id) {
        if (id > -1) {
            for (int i = 0; i < useItemList.size(); i++) {
                JsonObject item = useItemList.get(i).getAsJsonObject();
                if (item.get("id").getAsInt() == id) return i + 1;
            }
        }
        return 0;
    }

    private ArrayAdapter<CharSequence> getUseItemListAdapter() {
        String[] name_list = new String[useItemList.size()+1];
        name_list[0] = "-";
        for (int i = 0; i < useItemList.size(); i++) {
            JsonObject item = useItemList.get(i).getAsJsonObject();
            int id = item.get("id").getAsInt();
            String name = item.get("name").getAsString();
            name_list[i+1] = KcaUtils.format("[%d] %s", id, name);
        }
        return new ArrayAdapter<>(this, R.layout.spinner_dropdown_item_14dp, name_list);
    }
}
