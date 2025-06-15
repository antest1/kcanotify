package com.antest1.kcanotify;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.antest1.kcanotify.KcaApiData.findRemodelLv;
import static com.antest1.kcanotify.KcaApiData.getSlotItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.removeKai;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_MATERIALS;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_USEITEMS;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_EQUIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getJapanCalendarInstance;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;


public class AkashiDetailActivity extends BaseActivity {
    Toolbar toolbar;
    KcaDBHelper dbHelper;
    TextView itemNameTextView, itemImprovDefaultShipTextView, getItemImprovStat;
    JsonObject itemImprovementData;
    static JsonArray requiredItemTranslation;
    TextView dmat_count, smat_count;
    TextView current_date;

    public static void setRequiredItemTranslation(JsonArray data) {
        requiredItemTranslation = data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akashi_detail);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_akashi_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        Calendar calendar = getJapanCalendarInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
        Date date = calendar.getTime();
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        current_date = findViewById(R.id.current_date);
        current_date.setText(KcaUtils.format("%s (%s)", date_format.format(date),
                getString(getId("akashi_term_day_" + dayOfWeek, R.string.class))));

        dmat_count = findViewById(R.id.count_dmat);
        smat_count = findViewById(R.id.count_smat);
        JsonArray material_data = dbHelper.getJsonArrayValue(DB_KEY_MATERIALS);
        if (material_data != null) {
            JsonElement dmat_value = material_data.get(6);
            String dmat_str = dmat_value.isJsonPrimitive() ? dmat_value.getAsString() : dmat_value.getAsJsonObject().get("api_value").getAsString();
            dmat_count.setText(String.valueOf(dmat_str));

            JsonElement smat_value = material_data.get(7);
            String smat_str = smat_value.isJsonPrimitive() ? smat_value.getAsString() : smat_value.getAsJsonObject().get("api_value").getAsString();
            smat_count.setText(String.valueOf(smat_str));
        }

        JsonObject useritem_count = new JsonObject();
        JsonArray useitem_data = dbHelper.getJsonArrayValue(DB_KEY_USEITEMS);
        if (useitem_data != null) {
            for (int n = 0; n < useitem_data.size(); n++) {
                JsonObject item = useitem_data.get(n).getAsJsonObject();
                String key = item.get("api_id").getAsString();
                String value = item.get("api_count").getAsString();
                useritem_count.addProperty(key, value);
            }
        }

        if (!getIntent().hasExtra("item_id")) {
            finish();
        } else {
            itemNameTextView = findViewById(R.id.akashi_detail_name);
            itemImprovDefaultShipTextView = findViewById(R.id.akashi_improv_default_list);

            int itemid = getIntent().getIntExtra("item_id", 0);
            String itemImprovmetInfo = getIntent().getStringExtra("item_info");
            String kcItemName = "";
            if (itemid != 0) {
                JsonObject kcItemData = getKcItemStatusById(itemid, "type,name");
                kcItemName = getSlotItemTranslation(kcItemData.get("name").getAsString());
                itemNameTextView.setText(kcItemName);
            } else {
                Toast.makeText(this, "??", Toast.LENGTH_LONG).show();
            }

            getItemImprovStat = findViewById(R.id.akashi_improv_status_list);
            List<Integer> status = getImprovedCount(String.valueOf(itemid));
            List<String> statusText = new ArrayList<>();
            for (int i = 0; i < status.size(); i++) {
                if (status.get(i) > 0) {
                    int val = status.get(i);
                    statusText.add(KcaUtils.format("★%s: %d", i == 10 ? "max" : String.valueOf(i), val));
                }
            }
            if (!statusText.isEmpty()) {
                getItemImprovStat.setText(KcaUtils.joinStr(statusText, ", "));
            } else {
                getItemImprovStat.setText("-");
            }

            if (itemImprovmetInfo != null && !itemImprovmetInfo.isEmpty()) {
                itemImprovementData = JsonParser.parseString(itemImprovmetInfo).getAsJsonObject();
                JsonArray itemImprovementDetail = itemImprovementData.getAsJsonArray("improvement");
                JsonArray itemDefaultEquippedOn = itemImprovementData.getAsJsonArray("default_equipped_on");
                boolean itemConvertException = itemImprovementData.has("convert_exception");

                for (int i = 0; i < 3; i++) {
                    int detail_id = getId("akashi_improv_detail_" + (i+1), R.id.class);
                    if (i < itemImprovementDetail.size()) {
                        findViewById(detail_id).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(detail_id).setVisibility(View.GONE);
                    }
                }

                for (int i = 0; i < itemImprovementDetail.size(); i++) {
                    JsonObject data = itemImprovementDetail.get(i).getAsJsonObject();
                    JsonArray resources = data.getAsJsonArray("resource");

                    Set<String> total_items = new HashSet<>();
                    for (int j = 1; j <= 3; j++) {
                        int item = resources.get(j).getAsJsonArray().get(4).getAsInt();
                        if (item != 0) total_items.add(String.valueOf(item));
                    }
                    Log.e("KCA", total_items.toString());
                    JsonObject item_count = getItemCount(total_items);

                    for (int j = 1; j <= 3; j++) {
                        String[] mse_string = getMseString(resources.get(j).getAsJsonArray(), item_count);
                        ((TextView) findViewById(getId("akashi_improv_detail_m".concat(String.valueOf(j))
                                .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(mse_string[0]);
                        ((TextView) findViewById(getId("akashi_improv_detail_s".concat(String.valueOf(j))
                                .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(mse_string[1]);
                        if (j == 3 && data.has("require_item")) {
                            String e3 = mse_string[2];
                            JsonArray require_items = data.getAsJsonArray("require_item");

                            List<String> require_items_str = new ArrayList<>();
                            require_items_str.add(e3);
                            for (int k = 0; k < require_items.size(); k++) {
                                JsonArray r_item = require_items.get(k).getAsJsonArray();
                                int require_item_count = r_item.get(1).getAsInt();
                                JsonObject item_info = getRequiredItemData(r_item.get(0).getAsInt());
                                String require_item_name = getRequiredItemName(item_info.getAsJsonObject("name"));
                                String useitem_id = item_info.get("useitem_id").getAsString();
                                int useritem_count_view = 0;
                                if (useritem_count.has(useitem_id)) {
                                    useritem_count_view = useritem_count.get(useitem_id).getAsInt();
                                }
                                require_items_str.add(KcaUtils.format("%sx%d (%d)",
                                        require_item_name, require_item_count, useritem_count_view));
                            }
                            e3 = KcaUtils.joinStr(require_items_str, "\n");
                            ((TextView) findViewById(getId(KcaUtils.format("akashi_improv_detail_e%d_%d", j, i+1), R.id.class)))
                                    .setText(e3);
                        } else if (j == 2 && data.has("require_item2")) {
                            List<String> require_items_str = new ArrayList<>();
                            require_items_str.add(mse_string[2]);

                            JsonArray require_item2 = data.getAsJsonArray("require_item2");
                            int item2_from = require_item2.get(0).getAsInt();
                            JsonArray item2_req_items = require_item2.get(1).getAsJsonArray();

                            for (int k = 0; k < item2_req_items.size(); k++) {
                                JsonArray r_item = item2_req_items.get(k).getAsJsonArray();
                                int require_item_count = r_item.get(1).getAsInt();
                                JsonObject item_info = getRequiredItemData(r_item.get(0).getAsInt());
                                String require_item_name = getRequiredItemName(item_info.getAsJsonObject("name"));
                                String useitem_id = item_info.get("useitem_id").getAsString();
                                int useritem_count_view = 0;
                                if (useritem_count.has(useitem_id)) {
                                    useritem_count_view = useritem_count.get(useitem_id).getAsInt();
                                }
                                require_items_str.add(KcaUtils.format("[★%d~] %sx%d (%d)",
                                        item2_from, require_item_name, require_item_count, useritem_count_view));
                            }

                            String e3 = KcaUtils.joinStr(require_items_str, "\n");
                            ((TextView) findViewById(getId(KcaUtils.format("akashi_improv_detail_e%d_%d", j, i+1), R.id.class)))
                                    .setText(e3);
                        } else {
                            ((TextView) findViewById(getId(KcaUtils.format("akashi_improv_detail_e%d_%d", j, i+1), R.id.class)))
                                    .setText(mse_string[2]);
                        }

                    }

                    ((TextView) findViewById(getId("akashi_improv_consume_".concat(String.valueOf(i + 1)), R.id.class)))
                            .setText(getConcatString(resources.get(0).getAsJsonArray()));

                    JsonElement upgrade = data.get("upgrade");
                    if (upgrade.isJsonArray()) {
                        int upgradeItemId = upgrade.getAsJsonArray().get(0).getAsInt();
                        int upgradeItemStar = upgrade.getAsJsonArray().get(1).getAsInt();
                        String upgradeItemName = getSlotItemTranslation(getKcItemStatusById(upgradeItemId, "name").get("name").getAsString());
                        String upgradeStarString = "";
                        if (upgradeItemStar > 0) {
                            upgradeStarString = upgradeStarString.concat(" ").concat("★").concat(String.valueOf(upgradeItemStar));
                        }
                        ((TextView) findViewById(getId("akashi_improv_detail_upgrade_".concat(String.valueOf(i + 1)), R.id.class)))
                                .setText("→ ".concat(upgradeItemName.concat(upgradeStarString)));
                    } else {
                        findViewById(getId("akashi_improv_detail_row3_".concat(String.valueOf(i + 1)), R.id.class)).
                                setVisibility(View.GONE);
                    }

                    JsonArray req = data.get("req").getAsJsonArray();
                    ((TextView) findViewById(getId("akashi_improv_support_".concat(String.valueOf(i + 1)), R.id.class)))
                            .setText(getSupportString(req, itemConvertException));
                }
                List<String> shipList = new ArrayList<>();
                List<String> shipNameList = new ArrayList<>();
                JsonObject shipName = new JsonObject();

                if (itemDefaultEquippedOn != null) {
                    for (JsonElement element : itemDefaultEquippedOn) {
                        int shipId = element.getAsInt();
                        shipList.add(String.valueOf(shipId));
                        JsonObject kcShipData = getKcShipDataById(shipId, "name");
                        String translatedName = getShipTranslation(kcShipData.get("name").getAsString(), shipId, false);
                        shipName.addProperty(String.valueOf(shipId), translatedName);
                    }
                    if (shipList.size() > 0) {
                        JsonObject shipRemovelLv = findRemodelLv(joinStr(shipList, ","));
                        for (int i = 0; i < shipList.size(); i++) {
                            String id = shipList.get(i);
                            shipNameList.add(KcaUtils.format("%s(%d)", shipName.get(id).getAsString(), shipRemovelLv.get(id).getAsInt()));
                        }
                        itemImprovDefaultShipTextView.setText(joinStr(shipNameList, " / "));
                    } else {
                        itemImprovDefaultShipTextView.setText("-");
                    }
                } else {
                    itemImprovDefaultShipTextView.setText("-");
                }

            } else {
                Toast.makeText(this, "??", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
       finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private JsonObject getRequiredItemData(int index) {
        if (index >= requiredItemTranslation.size()) {
            return requiredItemTranslation.get(0).getAsJsonObject();
        } else {
            return requiredItemTranslation.get(index).getAsJsonObject();
        }
    }

    private String getRequiredItemName(JsonObject data) {
        String locale = LocaleUtils.getResourceLocaleCode();
        if (data.has(locale)) {
            return data.get(locale).getAsString();
        } else {
            return data.get("jp").getAsString();
        }
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

    private String getConcatString(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(e.getAsString());
        }
        return joinStr(list, " / ");
    }

    private String[] getMseString(JsonArray array, JsonObject item_count) {
        String[] value_list = new String[7];
        for (int i = 0; i < array.size(); i++) {
            value_list[i] = array.get(i).getAsString();
            if (i >= 4 && value_list[i].equals("0")) {
                value_list[i] = "-";
            } else if (value_list[i].equals("-1")) {
                value_list[i] = "?";
            }
        }

        if (!value_list[4].equals("-")) {
            int count = 0;
            if (item_count.has(value_list[4])) count = item_count.get(value_list[4]).getAsInt();
            String equipName = getSlotItemTranslation(getKcItemStatusById(Integer.parseInt(value_list[4]), "name")
                    .get("name").getAsString());
            value_list[4] = equipName;
            value_list[6] = String.valueOf(count);
        }

        String material_data = value_list[0].concat(" / ").concat(value_list[1]);
        String screw_data = value_list[2].concat(" / ").concat(value_list[3]);
        String equip_data = "";
        if (value_list[4].equals("-")) {
            equip_data = value_list[4];
        } else {
            equip_data = KcaUtils.format( "%sx%s (%s)", value_list[4], value_list[5], value_list[6] );
        }
        String[] result = {material_data, screw_data, equip_data};
        return result;
    }


    private String getSupportString(JsonArray array, boolean exception) {
        String supportString = "";
        for (int i = 0; i < array.size(); i++) {
            List<String> daylist = new ArrayList<>();
            JsonArray item = array.get(i).getAsJsonArray();
            if (item.get(1).isJsonArray()) {
                JsonArray day = item.get(0).getAsJsonArray();
                for (int j = 0; j < day.size(); j++) {
                    if (day.get(j).getAsBoolean()) {
                        daylist.add(getString(getId("akashi_term_day_".concat(String.valueOf(j)), R.string.class)));
                    }
                }

                String daytext;
                if (daylist.isEmpty()) {
                    daytext = getString(R.string.akashi_term_day_none);
                } else {
                    daytext = joinStr(daylist, ",");
                }

                int[] shiplist = removeKai(item.get(1).getAsJsonArray(), exception);
                for (int j = 0; j < shiplist.length; j++) {
                    JsonObject kcShipData = getKcShipDataById(shiplist[j], "name");
                    String shipname = getShipTranslation(kcShipData.get("name").getAsString(), shiplist[j], false);
                    supportString = supportString.concat(KcaUtils.format("%s(%s)", shipname, daytext)).concat("\n");
                }
            } else {
                supportString = "-";
            }

        }
        return supportString.trim();
    }

    private List<Integer> getImprovedCount(String equip_id) {
        JsonArray user_equipment_data = dbHelper.getItemData();
        List<Integer> count = new ArrayList<>(11);
        for (int i = 0; i <= 10; i++) count.add(0);

        for (JsonElement data: user_equipment_data) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = JsonParser.parseString(equip.get("value").getAsString()).getAsJsonObject();
            String slotitem_id = value.get("api_slotitem_id").getAsString();
            int level = value.get("api_level").getAsInt();
            if (equip_id.equals(slotitem_id)) {
                count.set(level, count.get(level) + 1);
            }
        }
        return count;
    }


    private JsonObject getItemCount(Set<String> equip_id) {
        JsonArray user_equipment_data = dbHelper.getItemData();
        String filtcond = getStringPreferences(getApplicationContext(), PREF_EQUIPINFO_FILTCOND);
        JsonObject count_result = new JsonObject();
        for (String e: equip_id) {
            count_result.addProperty(e, 0);
        }

        for (JsonElement data: user_equipment_data) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = JsonParser.parseString(equip.get("value").getAsString()).getAsJsonObject();
            String slotitem_id = value.get("api_slotitem_id").getAsString();
            int level = value.get("api_level").getAsInt();
            if (level == 0 && equip_id.contains(slotitem_id)) {
                count_result.addProperty(slotitem_id, count_result.get(slotitem_id).getAsInt() + 1);
            }
        }
        return count_result;
    }
}
