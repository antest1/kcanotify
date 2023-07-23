package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.helper;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_EQUIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
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
    TextView totalcountview, totalstarview;

    KcaDBHelper dbHelper;
    KcaEquipListViewAdpater adapter;
    JsonArray equipment_data = new JsonArray();
    JsonArray seikuuken_data = new JsonArray();
    JsonArray ship_data = new JsonArray();
    JsonObject ship_equip_info = new JsonObject();
    Map<String, AtomicInteger> counter = new HashMap<>();

    boolean is_popup_on;
    View export_popup, export_exit;
    TextView export_clipboard, export_openpage2;
    Vibrator vibrator;

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
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();

        is_popup_on = false;

        equipment_data = KcaApiData.getKcSlotitemGameData();
        JsonArray user_equipment_data = dbHelper.getItemData();
        String filtcond = getStringPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND);

        for (JsonElement data: user_equipment_data) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = new JsonParser().parse(equip.get("value").getAsString()).getAsJsonObject();
            seikuuken_data.add(getSeikuukenSimluatorData(value));
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
                int ship_kc_id = ship_info.get("api_ship_id").getAsInt();
                JsonObject ship_kc_info = getKcShipDataById(ship_kc_id, "name");
                String ship_name = "";
                if (ship_kc_info != null) ship_name = ship_kc_info.get("name").getAsString();

                JsonObject ship_id_lv = new JsonObject();
                ship_id_lv.addProperty("id", ship_id);
                ship_id_lv.addProperty("kc_id", ship_kc_id);
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

        totalcountview = findViewById(R.id.equipinfo_count);
        totalstarview = findViewById(R.id.equipinfo_total_star);

        adapter = new KcaEquipListViewAdpater();
        adapter.setSummaryFormat(getStringWithLocale(R.string.equipinfo_summary));
        adapter.setStatTranslation(itemStatTranslation);
        adapter.setListViewItemList(equipment_data, counter, ship_equip_info, filtcond);

        listview = findViewById(R.id.equipment_listview);
        listview.setAdapter(adapter);
        setSummary();

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
                setSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        export_popup = findViewById(R.id.export_popup);
        ((TextView) export_popup.findViewById(R.id.export_title))
                .setText(getStringWithLocale(R.string.equipinfo_export_title));
        export_popup.setVisibility(View.GONE);

        export_exit = export_popup.findViewById(R.id.export_exit);
        ((ImageView) export_exit).setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);

        export_popup.findViewById(R.id.export_bar).setOnClickListener(v -> {
            is_popup_on = false;
            export_popup.setVisibility(View.GONE);
        });

        export_clipboard = export_popup.findViewById(R.id.export_clipboard);
        export_clipboard.setText(getStringWithLocale(R.string.equipinfo_export_clipboard));
        export_clipboard.setOnClickListener(v -> {
            CharSequence text = ((TextView) findViewById(R.id.export_content)).getText();
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("text", text));
            doVibrate(vibrator, 100);
            Toast.makeText(getApplicationContext(),
                    getStringWithLocale(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        });

        export_openpage2 = export_popup.findViewById(R.id.export_openpage2);
        export_openpage2.setText(getStringWithLocale(R.string.equipinfo_export_openpage2));
        export_openpage2.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://noro6.github.io/kcTools/simulator/"));
            startActivity(bIntent);
        });

    }

    private void setSummary() {
        totalcountview.setText(KcaUtils.format(getStringWithLocale(R.string.equipinfo_btn_total_format), adapter.getTotalCount()));
        totalstarview.setText(KcaUtils.format(getStringWithLocale(R.string.equipinfo_btn_total_star_format), adapter.getStarCount()));
    }

    private String getItemKey(JsonObject item) {
        if (item.has("api_alv")) {
            return KcaUtils.format("%d_%d_%d", item.get("api_slotitem_id").getAsInt(),
                    item.get("api_level").getAsInt(), item.get("api_alv").getAsInt());
        } else {
            return KcaUtils.format("%d_%d_n", item.get("api_slotitem_id").getAsInt(), item.get("api_level").getAsInt());
        }
    }

    private JsonObject getSeikuukenSimluatorData(JsonObject item) {
        JsonObject new_item = new JsonObject();
        new_item.addProperty("id", item.get("api_slotitem_id").getAsInt());
        new_item.addProperty("lv", item.get("api_level").getAsInt());
        new_item.addProperty("locked", item.get("api_locked").getAsInt());
        return new_item;
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
            setSummary();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.equipinfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_equip_export:
                String data = seikuuken_data.toString();
                ((TextView) export_popup.findViewById(R.id.export_content)).setText(data);
                is_popup_on = true;
                export_popup.setVisibility(View.VISIBLE);
                return true;
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
}
