package com.antest1.kcanotify.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaDeckInfo;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.LocaleUtils;
import com.antest1.kcanotify.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;

/**
 * Business logic for {@link com.antest1.kcanotify.activity.FleetInfoActivity}: fleet data assembly
 * and the two export formats.
 */
public class FleetInfoViewModel extends AndroidViewModel {
    public static final String KC_REQ_LIST = "id,name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num";

    private final KcaDBHelper dbHelper;
    private final KcaDeckInfo deckInfoCalc;
    private final Context localizedContext;
    private static int currentFleet = 0;

    /**
     * Cache of equipment status resolved during one {@link #loadFleetData} run, keyed by item id.
     * The same equipment id often appears on several ships, so this turns repeated SQLite reads +
     * JSON parses into a single memory lookup per load. Cleared at the start of every load so it
     * never holds stale data across fleets or rotations.
     */
    private final SparseArray<JsonObject> itemStatusCache = new SparseArray<>();

    public FleetInfoViewModel(@NonNull Application application) {
        super(application);
        localizedContext = LocaleUtils.getLocalizedContext(application);
        deckInfoCalc = new KcaDeckInfo(localizedContext);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
    }

    public void init() {
        KcaApiData.setDBHelper(dbHelper);
        KcaUtils.setDefaultGameData(getApplication(), dbHelper);
    }

    public int getCurrentFleet() {
        return currentFleet;
    }

    public void setCurrentFleet(int currentFleet) {
        this.currentFleet = currentFleet;
    }

    /**
     * Assembles the fleet rows and summary values for the currently selected fleet.
     */
    public FleetData loadFleetData(boolean isPortrait) {
        JsonArray deckData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_DECKPORT);
        FleetData result = new FleetData();
        itemStatusCache.clear();
        if (deckData == null) {
            result.fleetName = "-";
            result.passed = false;
            return result;
        }

        if (currentFleet == 4) {
            result.fleetName = localizedContext.getString(com.antest1.kcanotify.R.string.fleetlist_combined_fleet);
            if (deckData.size() < 2) {
                result.passed = false;
            } else {
                result.isCombined = true;
                JsonArray data = deckInfoCalc.getDeckListInfo(deckData, 0, "all", KC_REQ_LIST);
                JsonArray dataC = deckInfoCalc.getDeckListInfo(deckData, 1, "all", KC_REQ_LIST);
                while (!isPortrait && data.size() < 6) data.add(new JsonObject());

                for (int i = 0; i < dataC.size(); i++) {
                    JsonObject itemC = dataC.get(i).getAsJsonObject();
                    itemC.addProperty("cb_flag", true);
                    data.add(itemC);
                }
                while (!isPortrait && data.size() < 12) data.add(new JsonObject());
                result.rows = buildRows(data);
                result.passed = true;
                result.fp = deckInfoCalc.getAirPowerRangeString(deckData, 0, null);
                result.seek = KcaUtils.format(localizedContext.getString(com.antest1.kcanotify.R.string.fleetview_seekvalue_f),
                        deckInfoCalc.getSeekValue(deckData, "0,1", SEEK_33CN1, null));
            }
        } else {
            if (deckData.size() <= currentFleet) {
                result.fleetName = String.valueOf(currentFleet + 1);
                result.passed = false;
            } else {
                result.isCombined = false;
                JsonObject fleetData = deckData.get(currentFleet).getAsJsonObject();
                result.fleetName = fleetData.get("api_name").getAsString();
                result.rows = buildRows(deckInfoCalc.getDeckListInfo(deckData, currentFleet, "all", KC_REQ_LIST));
                result.passed = true;
                result.fp = deckInfoCalc.getAirPowerRangeString(deckData, currentFleet, null);
                result.seek = KcaUtils.format(localizedContext.getString(com.antest1.kcanotify.R.string.fleetview_seekvalue_f),
                        deckInfoCalc.getSeekValue(deckData, String.valueOf(currentFleet), SEEK_33CN1, null));
            }
        }
        return result;
    }

    /**
     * Converts the raw `{kc, user}` rows into the display model used by the Compose screen.
     * This is the former {@code KcaFleetInfoItemAdapter.getView} assembly logic.
     */
    private List<FleetShipRow> buildRows(JsonArray data) {
        List<FleetShipRow> rows = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i).getAsJsonObject();
            if (!item.has("kc") || item.get("kc").isJsonNull()) {
                rows.add(null);
            } else {
                rows.add(buildRow(item));
            }
        }
        return rows;
    }

    private FleetShipRow buildRow(JsonObject data) {
        JsonObject kcdata = data.getAsJsonObject("kc");
        JsonObject userdata = data.getAsJsonObject("user");

        FleetShipRow row = new FleetShipRow();
        row.cbFlag = data.has("cb_flag");
        row.stype = KcaApiData.getShipTypeAbbr(kcdata.get("stype").getAsInt());
        row.name = KcaApiData.getShipTranslation(kcdata.get("name").getAsString(),
                kcdata.get("id").getAsInt(), false);
        row.level = "Lv ".concat(userdata.get("api_lv").getAsString());
        row.maxHp = "HP ".concat(userdata.get("api_maxhp").getAsString());
        row.speed = KcaApiData.getSpeedString(localizedContext, userdata.get("api_soku").getAsInt());

        int ship_stype = kcdata.get("stype").getAsInt();
        int ship_init_lk = kcdata.getAsJsonArray("luck").get(0).getAsInt();
        int ship_slot_num = kcdata.get("slot_num").getAsInt();

        JsonArray ship_slot = userdata.getAsJsonArray("api_slot");
        JsonArray ship_onslot = userdata.getAsJsonArray("api_onslot");
        int ship_slot_ex = userdata.get("api_slot_ex").getAsInt();

        int slot_sum = 0;
        for (int j = 0; j < ship_onslot.size(); j++) {
            slot_sum += ship_onslot.get(j).getAsInt();
        }

        // Same order and compaction as the adapter: empty slots keep a placeholder, items that
        // cannot be resolved are skipped entirely (which shifts the following rows up).
        boolean flag_plane_taisen_over_7 = false;
        List<JsonObject> shipItem = new ArrayList<>();
        for (int j = 0; j < ship_slot.size(); j++) {
            int item_id = ship_slot.get(j).getAsInt();
            if (item_id > 0) {
                JsonObject itemData = getItemStatusCached(item_id, "level,alv", "id,type,name");
                if (itemData != null) {
                    int item_kc_id = itemData.get("id").getAsInt();
                    if (KcaApiData.kcTaisenOverPlaneItemData.contains(item_kc_id)) {
                        flag_plane_taisen_over_7 = true;
                    }
                    shipItem.add(itemData);
                }
            } else {
                shipItem.add(new JsonObject());
            }
        }

        JsonArray ship_ka = userdata.getAsJsonArray("api_karyoku");
        JsonArray ship_ra = userdata.getAsJsonArray("api_raisou");
        JsonArray ship_ta = userdata.getAsJsonArray("api_taiku");
        JsonArray ship_so = userdata.getAsJsonArray("api_soukou");
        JsonArray ship_ts = userdata.getAsJsonArray("api_taisen");
        JsonArray ship_lk = userdata.getAsJsonArray("api_lucky");
        JsonArray ship_kyouka = userdata.getAsJsonArray("api_kyouka");
        int kc_ship_id = userdata.get("api_ship_id").getAsInt();

        row.karyoku = ship_ka.get(0).getAsString();
        row.raisou = ship_ra.get(0).getAsString();
        row.taiku = ship_ta.get(0).getAsString();
        row.soukou = ship_so.get(0).getAsString();

        int taisen_value = ship_ts.get(0).getAsInt();
        row.taisenHighlighted = taisen_value >= 100 || (ship_stype == 1 && taisen_value >= 60)
                || kc_ship_id == 141 || kc_ship_id == 478 || kc_ship_id == 624 || kc_ship_id == 394
                || kc_ship_id == 893 || kc_ship_id == 681 || kc_ship_id == 692 || kc_ship_id == 689
                || ((kc_ship_id == 529 || kc_ship_id == 536 || kc_ship_id == 889) && taisen_value >= 65)
                || ((kc_ship_id == 380 || kc_ship_id == 526 || kc_ship_id == 381 || kc_ship_id == 534
                        || kc_ship_id == 382 || kc_ship_id == 884)
                        && taisen_value >= 65 && flag_plane_taisen_over_7);
        row.taisen = ship_ts.get(0).getAsString();

        row.luckHighlighted = ship_init_lk + ship_kyouka.get(4).getAsInt() >= 50;
        row.luck = ship_lk.get(0).getAsString();

        String[] slotCounts = new String[5];
        boolean[] showSlotCount = new boolean[5];
        for (int i = 0; i < ship_onslot.size() && i < 5 && i < ship_slot.size(); i++) {
            if (slot_sum != 0 && i < ship_slot_num) {
                slotCounts[i] = ship_onslot.get(i).getAsString();
                showSlotCount[i] = true;
            }
        }

        row.slots = new EquipSlot[5];
        for (int i = 0; i < shipItem.size() && i < row.slots.length; i++) {
            JsonObject item = shipItem.get(i);
            if (!item.has("type")) continue;
            EquipSlot slot = buildEquipSlot(item);
            slot.showSlotCount = showSlotCount[i];
            slot.slotCountText = slotCounts[i] != null ? slotCounts[i] : "";
            row.slots[i] = slot;
        }

        if (ship_slot_ex == 0) {
            row.exSlot = null;
        } else {
            EquipSlot ex = new EquipSlot();
            if (ship_slot_ex > 0) {
                JsonObject exData = getUserItemStatusById(ship_slot_ex, "level,alv", "name,type");
                if (exData != null) {
                    ex.name = KcaApiData.getSlotItemTranslation(exData.get("name").getAsString());
                    ex.iconRes = getItemIconRes(exData.get("type").getAsJsonArray().get(3).getAsInt());
                    int lv = exData.get("level").getAsInt();
                    ex.lvText = lv > 0 ? KcaUtils.format("★%d", lv) : "";
                }
            }
            if (ex.iconRes == 0) ex.iconRes = R.mipmap.item_0;
            row.exSlot = ex;
        }
        return row;
    }

    private EquipSlot buildEquipSlot(JsonObject item) {
        EquipSlot slot = new EquipSlot();
        slot.iconRes = getItemIconRes(item.get("type").getAsJsonArray().get(3).getAsInt());
        slot.name = KcaApiData.getSlotItemTranslation(item.get("name").getAsString());
        int lv = item.has("level") ? item.get("level").getAsInt() : 0;
        slot.lvText = lv > 0 ? KcaUtils.format("★%d", lv) : "";
        int alv = item.has("alv") ? item.get("alv").getAsInt() : 0;
        if (alv > 0) {
            slot.alvText = KcaUtils.format(localizedContext.getString(
                    KcaUtils.getId(KcaUtils.format("alv_%d", alv), R.string.class)), alv);
            slot.alvColorRes = alv <= 3 ? R.color.itemalv1 : R.color.itemalv2;
        } else {
            slot.alvText = "";
            slot.alvColorRes = R.color.itemalv2;
        }
        return slot;
    }

    private int getItemIconRes(int itemType) {
        try {
            return KcaUtils.getId(KcaUtils.format("item_%d", itemType), R.mipmap.class);
        } catch (Exception e) {
            return R.mipmap.item_0;
        }
    }

    /**
     * Like {@link KcaApiData#getUserItemStatusById(int, String, String)} but memoised within this
     * load so the same equipment id is read from SQLite + parsed only once per fleet.
     */
    private JsonObject getItemStatusCached(int id, String list, String kclist) {
        JsonObject cached = itemStatusCache.get(id);
        if (cached != null) return cached;
        JsonObject data = getUserItemStatusById(id, list, kclist);
        if (data != null) itemStatusCache.put(id, data);
        return data;
    }

    public String deckBuilderData() {
        JsonObject deckbuilder = new JsonObject();
        deckbuilder.addProperty("version", 4);
        deckbuilder.addProperty("hqlv", KcaApiData.getAdmiralLevel());
        JsonArray deckData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_DECKPORT);
        if (deckData != null) {
            for (int i = 0; i < deckData.size(); i++) {
                JsonObject fleet = new JsonObject();
                JsonArray item = deckInfoCalc.getDeckListInfo(deckData, i, "all", KC_REQ_LIST);
                for (int j = 0; j < item.size(); j++) {
                    JsonObject sitem = new JsonObject();

                    JsonObject ship = item.get(j).getAsJsonObject();
                    JsonObject user = ship.getAsJsonObject("user");
                    sitem.addProperty("id", user.get("api_ship_id").getAsString());
                    sitem.addProperty("lv", user.get("api_lv").getAsInt());
                    sitem.addProperty("luck", user.getAsJsonArray("api_lucky").get(0).getAsInt());
                    sitem.addProperty("hp", user.get("api_nowhp").getAsInt());
                    JsonObject items = new JsonObject();

                    int taisenEquip = 0;
                    JsonArray apiSlot = user.getAsJsonArray("api_slot");
                    for (int k = 0; k < apiSlot.size(); k++) {
                        JsonObject itemS = new JsonObject();
                        int slotid = apiSlot.get(k).getAsInt();
                        if (slotid <= 0) continue;
                        JsonObject itemData = getUserItemStatusById(slotid, "slotitem_id,level,alv", "tais");
                        if (itemData != null) {
                            taisenEquip += itemData.get("tais").getAsInt();
                            int slotitemId = itemData.get("slotitem_id").getAsInt();
                            itemS.addProperty("id", slotitemId);
                            if (itemData.has("level")) {
                                int level = itemData.get("level").getAsInt();
                                if (level > 0) itemS.addProperty("rf", level);
                            }
                            if (itemData.has("alv")) {
                                int alv = itemData.get("alv").getAsInt();
                                if (alv > 0) itemS.addProperty("mas", alv);
                            }
                            items.add(KcaUtils.format("i%d", k + 1), itemS);
                        }
                    }

                    int shipSlotEx = user.get("api_slot_ex").getAsInt();
                    if (shipSlotEx > 0) {
                        JsonObject itemS = new JsonObject();
                        JsonObject exItemData = getUserItemStatusById(shipSlotEx, "slotitem_id,level,alv", "tais");
                        if (exItemData != null) {
                            taisenEquip += exItemData.get("tais").getAsInt();
                            int slotitemId = exItemData.get("slotitem_id").getAsInt();
                            itemS.addProperty("id", slotitemId);
                            if (exItemData.has("level")) {
                                int level = exItemData.get("level").getAsInt();
                                if (level > 0) itemS.addProperty("rf", level);
                            }
                            if (exItemData.has("alv")) {
                                int alv = exItemData.get("alv").getAsInt();
                                if (alv > 0) itemS.addProperty("mas", alv);
                            }
                            items.add("ix", itemS);
                        }
                    }


                    sitem.addProperty("asw", user.getAsJsonArray("api_taisen").get(0).getAsInt() - taisenEquip);
                    sitem.add("items", items);
                    fleet.add(KcaUtils.format("s%d", j + 1), sitem);
                }
                deckbuilder.add(KcaUtils.format("f%d", i + 1), fleet);
            }
        }
        return deckbuilder.toString();
    }

    public String imageBuilderData() {
        JsonObject builder = new JsonObject();
        builder.addProperty("hqlv", KcaApiData.getAdmiralLevel());

        JsonArray deckData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_DECKPORT);
        if (deckData != null) {
            for (int i = 0; i < deckData.size(); i++) {
                JsonObject fleet = new JsonObject();
                JsonArray item = deckInfoCalc.getDeckListInfo(deckData, i, "all", KC_REQ_LIST);
                for (int j = 0; j < item.size(); j++) {
                    JsonObject sitem = new JsonObject();

                    JsonObject ship = item.get(j).getAsJsonObject();
                    JsonObject user = ship.getAsJsonObject("user");
                    sitem.addProperty("id", user.get("api_ship_id").getAsString());
                    sitem.addProperty("lv", user.get("api_lv").getAsInt());
                    sitem.addProperty("luck", user.getAsJsonArray("api_lucky").get(0).getAsInt());
                    sitem.addProperty("hp", user.get("api_nowhp").getAsInt());
                    JsonObject items = new JsonObject();

                    JsonArray apiSlot = user.getAsJsonArray("api_slot");
                    for (int k = 0; k < apiSlot.size(); k++) {
                        JsonObject itemS = new JsonObject();
                        int slotid = apiSlot.get(k).getAsInt();
                        if (slotid <= 0) continue;
                        JsonObject itemData = getUserItemStatusById(slotid, "slotitem_id,level,alv", "");
                        if (itemData != null) {
                            int slotitemId = itemData.get("slotitem_id").getAsInt();
                            itemS.addProperty("id", slotitemId);
                            if (itemData.has("level")) {
                                int level = itemData.get("level").getAsInt();
                                if (level > 0) itemS.addProperty("rf", level);
                            }
                            if (itemData.has("alv")) {
                                int alv = itemData.get("alv").getAsInt();
                                if (alv > 0) itemS.addProperty("mas", alv);
                            }
                            items.add(KcaUtils.format("i%d", k + 1), itemS);
                        }
                    }

                    int shipSlotEx = user.get("api_slot_ex").getAsInt();
                    if (shipSlotEx > 0) {
                        JsonObject itemS = new JsonObject();
                        JsonObject exItemData = getUserItemStatusById(shipSlotEx, "slotitem_id,level,alv", "");
                        if (exItemData != null) {
                            int slotitemId = exItemData.get("slotitem_id").getAsInt();
                            itemS.addProperty("id", slotitemId);
                            if (exItemData.has("level")) {
                                int level = exItemData.get("level").getAsInt();
                                if (level > 0) itemS.addProperty("rf", level);
                            }
                            if (exItemData.has("alv")) {
                                int alv = exItemData.get("alv").getAsInt();
                                if (alv > 0) itemS.addProperty("mas", alv);
                            }
                            items.add("ix", itemS);
                        }
                    }

                    sitem.addProperty("asw", user.getAsJsonArray("api_taisen").get(0).getAsInt());
                    sitem.add("items", items);
                    sitem.addProperty("fp", user.getAsJsonArray("api_karyoku").get(0).getAsInt());
                    sitem.addProperty("tp", user.getAsJsonArray("api_raisou").get(0).getAsInt());
                    sitem.addProperty("aa", user.getAsJsonArray("api_taiku").get(0).getAsInt());
                    sitem.addProperty("ar", user.getAsJsonArray("api_soukou").get(0).getAsInt());
                    sitem.addProperty("ev", user.getAsJsonArray("api_kaihi").get(0).getAsInt());
                    sitem.addProperty("los", user.getAsJsonArray("api_sakuteki").get(0).getAsInt());
                    fleet.add(KcaUtils.format("s%d", j + 1), sitem);
                }
                builder.add(KcaUtils.format("f%d", i + 1), fleet);
            }
        }
        return builder.toString();
    }

    public static class FleetData {
        /** One entry per grid row; {@code null} is a filler row (invisible but keeps its space). */
        public List<FleetShipRow> rows;
        public String fleetName;
        public String fp;
        public String seek;
        public boolean passed;
        public boolean isCombined;
    }

    /** Display model of one ship row (former adapter row contents). */
    public static class FleetShipRow {
        public boolean cbFlag;
        public String stype;
        public String name;
        public String level;
        public String maxHp;
        public String speed;
        public String karyoku;
        public String raisou;
        public String taiku;
        public String soukou;
        public String taisen;
        public String luck;
        public boolean taisenHighlighted;
        public boolean luckHighlighted;
        /** Five equipment rows; a {@code null} entry keeps the row space but hides its contents. */
        public EquipSlot[] slots;
        /** Ex-slot row; {@code null} hides the area (its space is kept by the screen). */
        public EquipSlot exSlot;
    }

    /** Display model of one equipment slot / ex slot. */
    public static class EquipSlot {
        public int iconRes;
        public String name = "";
        public String lvText = "";
        public String alvText = "";
        public int alvColorRes;
        public String slotCountText = "";
        public boolean showSlotCount;
    }
}
