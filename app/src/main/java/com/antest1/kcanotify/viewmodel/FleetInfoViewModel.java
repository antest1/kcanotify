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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;

/**
 * Business logic for {@link com.antest1.kcanotify.FleetInfoActivity}: fleet data assembly and
 * the two export formats.
 */
public class FleetInfoViewModel extends AndroidViewModel {
    public static final String KC_REQ_LIST = "id,name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num";

    private final KcaDBHelper dbHelper;
    private final KcaDeckInfo deckInfoCalc;
    private final Context localizedContext;
    // static so the screen reopens on the last chosen fleet, like it did before the refactor
    private static int currentFleet = 0;

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
                result.data = data;
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
                result.data = deckInfoCalc.getDeckListInfo(deckData, currentFleet, "all", KC_REQ_LIST);
                result.passed = true;
                result.fp = deckInfoCalc.getAirPowerRangeString(deckData, currentFleet, null);
                result.seek = KcaUtils.format(localizedContext.getString(com.antest1.kcanotify.R.string.fleetview_seekvalue_f),
                        deckInfoCalc.getSeekValue(deckData, String.valueOf(currentFleet), SEEK_33CN1, null));
            }
        }
        return result;
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
                    sitem.addProperty("aa", user.getAsJsonArray("api_tyku").get(0).getAsInt());
                    sitem.addProperty("ar", user.getAsJsonArray("api_souk").get(0).getAsInt());
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
        public JsonArray data;
        public String fleetName;
        public String fp;
        public String seek;
        public boolean passed;
        public boolean isCombined;
    }
}
