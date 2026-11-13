package com.antest1.kcanotify.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaDocking;
import com.antest1.kcanotify.KcaUtils;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.antest1.kcanotify.KcaApiData.STYPE_CVE;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.isShipCVE;
import static com.antest1.kcanotify.KcaApiData.loadShipExpInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipFilterDataFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadShipNationalityDataFromStorage;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.searchStringFromStart;

/**
 * Business logic for {@link com.antest1.kcanotify.ShipInfoActivity} and its adapter.
 *
 * <p>Owns the ship list (filtering, search, sorting, totals and the export text). The adapter is
 * reduced to row rendering.
 */
public class ShipInfoViewModel extends AndroidViewModel {
    private static final String[] total_key_list = {
            "api_id", "api_lv", "api_stype", "api_cond", "api_locked",
            "api_deck_id", "api_docking", "api_damage", "api_repair", "api_mission", "api_exslot", "api_maxhp",
            "api_karyoku", "api_raisou", "api_taiku", "api_soukou", "api_yasen",
            "api_taisen", "api_kaihi", "api_sakuteki", "api_lucky", "api_soku", "api_sort_id", "api_sally_area"};

    private static final int[] sort_table = {0, 1, 2, 3, 5, 7, 8, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
    private static final int[] filt_table = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23};

    private final KcaDBHelper dbHelper;
    private long expSum = 0L;
    private JsonArray deckInfo = new JsonArray();
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private String searchQuery = "";
    private JsonObject specialEquipment = new JsonObject();
    private JsonObject nationalityCtype = new JsonObject();
    private JsonObject nationalityShipId = new JsonObject();

    public ShipInfoViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
    }

    public boolean init() {
        KcaApiData.setDBHelper(dbHelper);
        KcaUtils.setDefaultGameData(getApplication(), dbHelper);
        loadShipFilterDataFromStorage(getApplication());
        loadShipNationalityDataFromStorage(getApplication());
        return loadShipExpInfoFromAssets(getApplication().getAssets()) == 1;
    }

    public List<JsonObject> getItems() {
        return listViewItemList;
    }

    public long getTotalExp() {
        return expSum;
    }

    public void setSearchQuery(String query) {
        searchQuery = query;
    }

    public void setSpecialEquipment(JsonObject data) {
        specialEquipment = data;
    }

    public void setNationality(JsonObject data) {
        nationalityCtype = data.getAsJsonObject("ctype");
        nationalityShipId = data.getAsJsonObject("ship_id");
    }

    public String getSortKey() {
        return getStringPreferences(getApplication(), KcaConstants.PREF_SHIPINFO_SORTKEY);
    }

    public String getFilterCond() {
        return getStringPreferences(getApplication(), KcaConstants.PREF_SHIPINFO_FILTCOND);
    }

    public String getSpecialEquip() {
        return getStringPreferences(getApplication(), KcaConstants.PREF_SHIPINFO_SPEQUIPS);
    }

    public String getShipStatus() {
        return getStringPreferences(getApplication(), KcaConstants.PREF_SHIPINFO_SHIPSTAT);
    }

    public String getShipNat() {
        return getStringPreferences(getApplication(), KcaConstants.PREF_SHIPINFO_SHIPNAT);
    }

    public boolean isFilterActive() {
        String filtcond = getFilterCond();
        return filtcond.length() > 1
                || !getSpecialEquip().trim().isEmpty()
                || !getShipStatus().trim().isEmpty()
                || !getShipNat().trim().isEmpty();
    }

    /**
     * Rebuilds the list from the stored game data and persisted preferences.
     */
    public void loadList() {
        JsonArray data = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_SHIPIFNO);
        if (data == null) data = new JsonArray();
        JsonArray deckdata = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_DECKPORT);
        if (deckdata == null) deckdata = new JsonArray();

        setSpecialEquipment(KcaApiData.getSpecialEquipmentInfo());
        setNationality(KcaApiData.getNationalityData());
        setListViewItemList(data, deckdata, getSortKey(), getFilterCond(), getSpecialEquip(),
                getShipStatus(), getShipNat());
    }

    /**
     * Re-applies the search query on top of the current data.
     */
    public void reloadList() {
        loadList();
    }

    public void resort() {
        resortListViewItem(getSortKey());
    }

    public void setListViewItemList(JsonArray ship_list, JsonArray deck_list, String sort_key, String special_equip, String ship_status, String ship_nat) {
        setListViewItemList(ship_list, deck_list, sort_key, "|", special_equip, ship_status, ship_nat);
    }

    public void setListViewItemList(JsonArray ship_list, JsonArray deck_list, String sort_key, final String filter, String special_equip, String ship_status, String ship_nat) {
        expSum = 0;
        deckInfo = deck_list;
        Type listType = new TypeToken<List<JsonObject>>() {
        }.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
        if (listViewItemList == null) listViewItemList = new ArrayList<>();
        listViewItemList = addShipInformation(listViewItemList, special_equip, ship_status, ship_nat);
        if (!filter.equals("|") && listViewItemList.size() > 1) {
            listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, input -> {
                String[] filter_list = filter.split("\\|");
                for (String key_op_val : filter_list) {
                    if (key_op_val.length() != 0) {
                        String[] kov_split = key_op_val.split(",");
                        int idx = Integer.valueOf(kov_split[0]);
                        String key = total_key_list[idx];
                        int op = Integer.valueOf(kov_split[1]);

                        String v1 = getstrvalue(input, key);
                        String v2 = kov_split[2].trim();
                        String[] v2_list = v2.split("_");

                        int v1_int = getintvalue(input, key);

                        boolean flag = false;
                        switch (op) {
                            case 0:
                                for (String v2_val : v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (key.equals("api_stype") && v2_int == STYPE_CVE) {
                                        int shipid = getintvalue(input, "api_ship_id");
                                        if (isShipCVE(shipid)) {
                                            flag = true;
                                            break;
                                        }
                                    } else {
                                        if (v1_int == v2_int) {
                                            flag = true;
                                            break;
                                        }
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 1:
                                for (String v2_val : v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int != v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 2:
                                for (String v2_val : v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int < v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 3:
                                for (String v2_val : v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int > v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 4:
                                for (String v2_val : v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int <= v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 5:
                                for (String v2_val : v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int >= v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            default:
                                break;
                        }
                    }
                }
                return true;
            }));
        }

        StatComparator cmp = new StatComparator(sort_key);
        Collections.sort(listViewItemList, cmp);
        for (int i = 0; i < listViewItemList.size(); i++) {
            expSum += listViewItemList.get(i).getAsJsonArray("api_exp").get(0).getAsLong();
        }
    }

    public void resortListViewItem(String sort_key) {
        StatComparator cmp = new StatComparator(sort_key);
        Collections.sort(listViewItemList, cmp);
    }

    public String getSeikuukenSimulatorText() {
        JsonArray data = new JsonArray();
        if (listViewItemList != null) {
            for (JsonObject v : listViewItemList) {
                JsonObject item = new JsonObject();
                item.addProperty("id", v.get("api_ship_id").getAsInt());
                item.addProperty("lv", v.get("api_lv").getAsInt());
                item.addProperty("locked", v.get("api_locked").getAsInt());
                item.add("st", v.get("api_kyouka").getAsJsonArray());
                item.add("exp", v.get("api_exp").getAsJsonArray());
                item.addProperty("ex", v.get("api_slot_ex").getAsInt());
                if (v.has("api_sally_area") && !v.get("api_sally_area").isJsonNull()) {
                    item.addProperty("area", v.get("api_sally_area").getAsInt());
                }
                data.add(item);
            }
        }
        return data.toString();
    }

    public String getKanmusuListText() {
        JsonObject data = KcaApiData.buildShipUpdateData();
        JsonObject frombefore = data.getAsJsonObject("frombefore");
        JsonObject fromafter = data.getAsJsonObject("fromafter");
        JsonObject afterlv = data.getAsJsonObject("afterlv");

        List<String> items = new ArrayList<>();
        Map<String, List<String>> ship_base = new LinkedHashMap<>();
        items.add(".2");
        if (listViewItemList != null) {
            for (JsonObject v : listViewItemList) {
                String lv = v.get("api_lv").getAsString();
                String shipId = v.get("api_ship_id").getAsString();

                String targetId = shipId;
                String baseId = shipId;
                int minlv = 100;
                int k = 0;
                while (fromafter.has(targetId)) {
                    baseId = fromafter.get(targetId).getAsString();
                    String key = KcaUtils.format("%s->%s", baseId, targetId);
                    minlv = afterlv.get(key).getAsInt();
                    targetId = baseId;
                    k += 1;
                }

                targetId = baseId;
                String afterId = baseId;
                int l = 0;
                while (frombefore.has(targetId)) {
                    afterId = frombefore.get(targetId).getAsString();
                    String key = KcaUtils.format("%s->%s", targetId, afterId);
                    if (Integer.parseInt(lv) < afterlv.get(key).getAsInt()) break;
                    targetId = afterId;
                    l += 1;
                }

                String postfix = (k != l) ? "." + String.valueOf(k + 1) : "";
                if (!ship_base.containsKey(baseId)) ship_base.put(baseId, new ArrayList<>());
                ship_base.get(baseId).add(lv + postfix);
            }
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> item : ship_base.entrySet()) {
            result.put(item.getKey(), KcaUtils.joinStr(item.getValue(), ","));
        }
        List<Map.Entry<String, String>> list = new ArrayList<>(result.entrySet());
        Collections.sort(list, (o1, o2) -> o1.getKey().equals(o2.getKey()) ? 0 :
                Integer.parseInt(o1.getKey()) < Integer.parseInt(o2.getKey()) ? -1 : 1);
        for (Map.Entry<String, String> item : list) {
            items.add(item.getKey() + ":" + item.getValue());
        }
        return KcaUtils.joinStr(items, "|");
    }

    private List<JsonObject> addShipInformation(List<JsonObject> data, String sp_eqlist, String ship_status, String ship_nat) {
        List<JsonObject> filteredShipInformation = new ArrayList<>();
        JsonObject deck_ship_info = new JsonObject();
        for (int i = 0; i < deckInfo.size(); i++) {
            JsonObject fleet_item = deckInfo.get(i).getAsJsonObject();
            int api_id = fleet_item.get("api_id").getAsInt();
            JsonArray api_ship = fleet_item.getAsJsonArray("api_ship");
            int api_mission = fleet_item.getAsJsonArray("api_mission").get(1).getAsInt();
            for (int j = 0; j < api_ship.size(); j++) {
                int ship_id = api_ship.get(j).getAsInt();
                JsonObject ship_item = new JsonObject();
                if (ship_id > 0) {
                    ship_item.addProperty("deck_id", api_id);
                    ship_item.addProperty("mission", api_mission);
                }
                deck_ship_info.add(String.valueOf(ship_id), ship_item);
            }
        }

        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i);
            String ship_id = item.get("api_id").getAsString();
            int kc_ship_id = item.get("api_ship_id").getAsInt();
            item.addProperty("api_yasen",
                    item.getAsJsonArray("api_karyoku").get(0).getAsInt() +
                            item.getAsJsonArray("api_raisou").get(0).getAsInt());
            JsonObject kcShipData = getKcShipDataById(kc_ship_id, "api_name,api_yomi,api_stype,api_sort_id,api_ctype");
            String name = kcShipData != null ? kcShipData.get("api_name").getAsString() : "";
            name = KcaApiData.getShipTranslation(name, kc_ship_id, false);
            String yomi = kcShipData != null ? kcShipData.get("api_yomi").getAsString() : "";
            int stype = kcShipData != null ? kcShipData.get("api_stype").getAsInt() : 0;
            int ctype = kcShipData != null ? kcShipData.get("api_ctype").getAsInt() : 0;

            boolean name_matched = searchStringFromStart(name, searchQuery, false);
            boolean yomi_matched = searchStringFromStart(yomi, searchQuery, false);
            if (!name_matched && !yomi_matched) continue;


            if (ship_status.trim().length() > 0) {
                String[] ship_status_list = ship_status.split(",");
                boolean match_flag = false;
                for (String key : ship_status_list) {
                    if (key.equals("type1")) { // expedition
                        match_flag |= deck_ship_info.has(ship_id) &&
                                deck_ship_info.getAsJsonObject(ship_id).get("mission").getAsInt() != 0;
                    }
                    if (key.equals("type2")) { // docking
                        match_flag |= KcaDocking.checkShipInDock(Integer.parseInt(ship_id));
                    }
                }
                if (match_flag) continue;
            }

            if (sp_eqlist.trim().length() > 0) {
                String[] special_eq_list = sp_eqlist.split(",");
                boolean found_flag = false;
                for (String key : special_eq_list) {
                    if (specialEquipment.has(key)) {
                        JsonObject se_item = specialEquipment.getAsJsonObject(key);
                        boolean match_stype = se_item.getAsJsonArray("stype").contains(new JsonPrimitive(stype));
                        boolean match_special = se_item.getAsJsonArray("special").contains(new JsonPrimitive(kc_ship_id));
                        if (match_stype || match_special) {
                            found_flag = true;
                            break;
                        }
                    }
                }
                if (!found_flag) continue;
            }

            if (!ship_nat.trim().isEmpty()) {
                String[] nat_list = ship_nat.split(",");
                boolean found_flag = false;
                for (String key : nat_list) {
                    if (nationalityShipId.has(key)) {
                        found_flag = nationalityShipId.getAsJsonArray(key).contains(new JsonPrimitive(kc_ship_id));
                        if (found_flag) break;
                    }
                    if (nationalityCtype.has(key)) {
                        found_flag = nationalityCtype.getAsJsonArray(key).contains(new JsonPrimitive(ctype));
                        if (found_flag) break;
                    }
                }
                if (!found_flag) continue;
            }

            int max_hp = item.get("api_maxhp").getAsInt();
            int now_hp = item.get("api_nowhp").getAsInt();
            item.addProperty("api_exslot", item.get("api_slot_ex").getAsInt() != 0 ? 1 : 0);
            item.addProperty("api_stype", stype);
            item.addProperty("api_docking", KcaDocking.checkShipInDock(Integer.parseInt(ship_id)) ? 1 : 0);
            item.addProperty("api_damage", KcaApiData.getStatus(now_hp * 100 / max_hp));
            item.addProperty("api_damage_value", (max_hp - now_hp) * 100 / max_hp);
            item.addProperty("api_repair", KcaDocking.getDockingTime(max_hp - now_hp, item.get("api_lv").getAsInt(), stype));
            if (deck_ship_info.has(ship_id)) {
                JsonObject ship_deck_data = deck_ship_info.getAsJsonObject(ship_id);
                item.addProperty("api_deck_id", ship_deck_data.get("deck_id").getAsInt());
                item.addProperty("api_mission", ship_deck_data.get("mission").getAsInt());
            } else {
                item.addProperty("api_deck_id", 0);
                item.addProperty("api_mission", 0);
            }
            item.addProperty("api_sort_id", kcShipData != null ? kcShipData.get("api_sort_id").getAsInt() : 0);
            filteredShipInformation.add(item);
        }
        return filteredShipInformation;
    }

    public static int getSortKeyIndex(int position) {
        return sort_table[position];
    }

    public static int getSortIndexByKey(int key) {
        return Arrays.binarySearch(sort_table, key);
    }

    public static int getFilterKeyIndex(int position) {
        return filt_table[position];
    }

    public static int getFilterIndexByKey(int key) {
        return Arrays.binarySearch(filt_table, key);
    }

    public static boolean isList(int idx) {
        int[] list = {2, 5, 7, 21, 23};  // ship_filt_array
        return (Arrays.binarySearch(list, idx) >= 0);
    }

    public static boolean isBoolean(int idx) {
        int[] list = {4, 6, 9, 10}; // ship_filt_array
        return (Arrays.binarySearch(list, idx) >= 0);
    }

    public static boolean isNumeric(int idx) {
        return !isList(idx) && !isBoolean(idx);
    }

    public static int getintvalue(JsonObject o, String key) {
        if (o.has(key)) {
            if (o.get(key).isJsonArray()) {
                return o.getAsJsonArray(key).get(0).getAsInt();
            } else {
                return o.get(key).getAsInt();
            }
        }
        return 0;
    }

    public static String getstrvalue(JsonObject o, String key) {
        if (o.has(key)) {
            if (o.get(key).isJsonPrimitive()) return o.get(key).getAsString();
            else return o.get(key).toString();
        } else return "";
    }

    private static class StatComparator implements Comparator<JsonObject> {
        String sort_key;

        private StatComparator(String key) {
            sort_key = key;
        }

        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String[] sort_key_list = sort_key.split("\\|");
            for (String key_idx : sort_key_list) {
                if (key_idx.length() != 0) {
                    int idx = Integer.valueOf(key_idx.split(",")[0]);
                    boolean is_desc = Boolean.valueOf(key_idx.split(",")[1]);
                    String key = total_key_list[idx];
                    if (key.equals("api_lv")) key = "api_exp";
                    if (key.equals("api_damage")) key = "api_damage_value";
                    int val1 = getintvalue(o1, key);
                    int val2 = getintvalue(o2, key);
                    if (key.equals("api_deck_id")) {
                        if (val1 == 0) val1 = is_desc ? -10 : 10;
                        if (val2 == 0) val2 = is_desc ? -10 : 10;
                    }
                    if (val1 != val2) {
                        if (is_desc) return val2 - val1;
                        else return val1 - val2;
                    }
                }
            }
            return 0;
        }
    }
}
