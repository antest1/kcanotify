package com.antest1.kcanotify.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaUtils;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaConstants.PREF_EQUIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

/**
 * Business logic for {@link com.antest1.kcanotify.EquipmentInfoActivity} and its adapter: loading
 * the owned-equipment index, filtering/sorting the list and computing the summary totals.
 */
public class EquipmentInfoViewModel extends AndroidViewModel {
    private final KcaDBHelper dbHelper;
    private JsonArray equipmentData = new JsonArray();
    private JsonArray seikuukenData = new JsonArray();
    private JsonObject shipEquipInfo = new JsonObject();
    private Map<String, AtomicInteger> counter = new HashMap<>();
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private String searchQuery = "";
    private int totalEquipment = 0;
    private int totalStars = 0;

    public EquipmentInfoViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
    }

    public void init() {
        KcaApiData.setDBHelper(dbHelper);
        KcaUtils.setDefaultGameData(getApplication(), dbHelper);

        seikuukenData = new JsonArray();
        counter = new HashMap<>();
        shipEquipInfo = new JsonObject();

        equipmentData = KcaApiData.getKcSlotitemGameData();
        JsonArray userEquipmentData = dbHelper.getItemData();

        for (JsonElement data : userEquipmentData) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = JsonParser.parseString(equip.get("value").getAsString()).getAsJsonObject();
            seikuukenData.add(getSeikuukenSimletorData(value));
            String id = getItemKey(value);
            if (!counter.containsKey(id)) {
                counter.put(id, new AtomicInteger(1));
            } else {
                counter.get(id).incrementAndGet();
            }
        }

        JsonArray shipData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_SHIPIFNO);
        if (shipData != null) {
            for (int i = 0; i < shipData.size(); i++) {
                List<String> itemList = new ArrayList<>();
                JsonObject shipInfo = shipData.get(i).getAsJsonObject();
                int shipId = shipInfo.get("api_id").getAsInt();
                int shipLv = shipInfo.get("api_lv").getAsInt();
                int shipKcId = shipInfo.get("api_ship_id").getAsInt();
                JsonObject shipKcInfo = getKcShipDataById(shipKcId, "name");
                String shipName = "";
                if (shipKcInfo != null) shipName = shipKcInfo.get("name").getAsString();

                JsonObject shipIdLv = new JsonObject();
                shipIdLv.addProperty("id", shipId);
                shipIdLv.addProperty("kc_id", shipKcId);
                shipIdLv.addProperty("name", shipName);
                shipIdLv.addProperty("lv", shipLv);

                JsonArray shipItem = shipInfo.getAsJsonArray("api_slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int itemId = shipItem.get(j).getAsInt();
                    if (itemId > 0) {
                        String itemInfoStr = dbHelper.getItemValue(itemId);
                        if (itemInfoStr != null) {
                            JsonObject itemInfo = JsonParser.parseString(itemInfoStr).getAsJsonObject();
                            itemList.add(getItemKey(itemInfo));
                        }
                    }
                }
                int exItemId = shipInfo.get("api_slot_ex").getAsInt();
                if (exItemId > 0) {
                    String itemInfoStr = dbHelper.getItemValue(exItemId);
                    if (itemInfoStr != null) {
                        JsonObject itemInfo = JsonParser.parseString(itemInfoStr).getAsJsonObject();
                        itemList.add(getItemKey(itemInfo));
                    }
                }

                for (String item : itemList) {
                    if (!shipEquipInfo.has(item)) {
                        shipEquipInfo.add(item, new JsonArray());
                    }
                    shipEquipInfo.getAsJsonArray(item).add(shipIdLv);
                }
            }
        }
    }

    public JsonArray getSeikuukenData() {
        return seikuukenData;
    }

    public Map<String, AtomicInteger> getCountInfo() {
        return counter;
    }

    public JsonObject getShipEquipInfo() {
        return shipEquipInfo;
    }

    public List<JsonObject> getItems() {
        return listViewItemList;
    }

    public int getTotalCount() {
        return totalEquipment;
    }

    public int getStarCount() {
        return totalStars;
    }

    public void setSearchQuery(String query) {
        searchQuery = query;
    }

    public String getFilterCond() {
        return getStringPreferences(getApplication(), PREF_EQUIPINFO_FILTCOND);
    }

    public void loadList() {
        String filtcond = getStringPreferences(getApplication(), PREF_EQUIPINFO_FILTCOND);
        setListViewItemList(equipmentData, counter, shipEquipInfo, filtcond);
    }

    public void setListViewItemList(JsonArray total_equip_list, Map<String, AtomicInteger> counter, JsonObject ship_equip_info, final String filtcond) {
        shipEquipInfo = ship_equip_info;
        this.counter = counter;
        totalEquipment = 0;
        totalStars = 0;
        if (!filtcond.isEmpty()) {
            Type listType = new TypeToken<List<JsonObject>>() {
            }.getType();
            listViewItemList = new Gson().fromJson(total_equip_list, listType);
            listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, input -> {
                int key = input.get("api_id").getAsInt();
                String item_name = KcaApiData.getSlotItemTranslation(input.get("api_name").getAsString());
                if (!item_name.contains(searchQuery)) return false;
                if (filtcond.equals("all")) {
                    return checkCountExist(this.counter, key);
                } else {
                    Integer type_3 = input.getAsJsonArray("api_type").get(3).getAsInt();
                    String[] position_list = filtcond.split(",");
                    List<Integer> filt_list = new ArrayList<Integer>();
                    for (String p : position_list) {
                        filt_list.add(Integer.valueOf(p) + 1);
                    }
                    return filt_list.contains(type_3) && checkCountExist(this.counter, key);
                }
            }));
            StatComparator cmp = new StatComparator();
            Collections.sort(listViewItemList, cmp);
            calculateStats(listViewItemList);
        } else {
            listViewItemList.clear();
        }
    }

    public String getItemKey(JsonObject item) {
        if (item.has("api_alv")) {
            return KcaUtils.format("%d_%d_%d", item.get("api_slotitem_id").getAsInt(),
                    item.get("api_level").getAsInt(), item.get("api_alv").getAsInt());
        } else {
            return KcaUtils.format("%d_%d_n", item.get("api_slotitem_id").getAsInt(), item.get("api_level").getAsInt());
        }
    }

    private JsonObject getSeikuukenSimletorData(JsonObject item) {
        JsonObject new_item = new JsonObject();
        new_item.addProperty("id", item.get("api_slotitem_id").getAsInt());
        new_item.addProperty("lv", item.get("api_level").getAsInt());
        new_item.addProperty("locked", item.get("api_locked").getAsInt());
        return new_item;
    }

    private void calculateStats(List<JsonObject> list) {
        Set<String> check = new HashSet<>();
        for (JsonObject item : list) {
            String id = item.get("api_id").getAsString();
            check.add(id);
        }

        for (String key : counter.keySet()) {
            String[] key_split = key.split("_");
            String id = key_split[0];
            if (check.contains(id)) {
                int star = Integer.parseInt(key_split[1]);
                int count = counter.get(key).get();
                totalEquipment += count;
                totalStars += (star * count);
            }
        }
    }

    private boolean checkCountExist(Map<String, AtomicInteger> count, int equip_id) {
        for (String key : count.keySet()) {
            String[] key_split = key.split("_");
            int target_equip_id = Integer.parseInt(key_split[0]);
            if (target_equip_id == equip_id) return true;
        }
        return false;
    }

    private static class StatComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            int o1_id = o1.get("api_id").getAsInt();
            int o2_id = o2.get("api_id").getAsInt();

            int o1_type_2 = o1.getAsJsonArray("api_type").get(2).getAsInt();
            int o2_type_2 = o2.getAsJsonArray("api_type").get(2).getAsInt();

            if (o1_type_2 != o2_type_2) return o1_type_2 - o2_type_2;
            return o1_id - o2_id;
        }
    }
}
