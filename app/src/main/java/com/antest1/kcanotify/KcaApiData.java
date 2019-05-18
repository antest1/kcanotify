package com.antest1.kcanotify;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;

public class KcaApiData {
    public static JsonObject kcGameData = null;
    public static boolean dataLoadTriggered = false;
    public static String currentLocaleCode = "";

    public static Map<Integer, JsonObject> kcShipData = new HashMap<Integer, JsonObject>();
    public static Map<Integer, JsonObject> kcItemData = new HashMap<Integer, JsonObject>();
    public static Map<Integer, JsonObject> userShipData = null;

    public static Map<Integer, JsonObject> kcMissionData = new HashMap<Integer, JsonObject>();
    public static Map<Integer, JsonObject> kcUseitemData = new HashMap<Integer, JsonObject>();
    //public static Map<String, String> kcShipTranslationData = null;

    public static int getShipCountInBattle = 0;
    public static int getItemCountInBattle = 0;

    public static int[] eventMapDifficulty = new int[10];

    public static JsonObject kcShipTranslationData = new JsonObject();
    public static JsonObject kcItemTranslationData = new JsonObject();
    public static JsonObject kcQuestInfoData = new JsonObject();
    public static JsonArray kcStypeData = new JsonArray();

    public static JsonObject kcShipAbbrData = new JsonObject(); // For English
    public static JsonObject kcExpeditionData = new JsonObject();
    public static JsonObject kcShipInitEquipCount = new JsonObject();

    public static Handler sHandler = null;
    public static KcaDBHelper helper = null;
    public static boolean isEventTime = false;

    // Ship Type Constants (api_stype)
    public static final int STYPE_DE = 1;
    public static final int STYPE_DD = 2;
    public static final int STYPE_CL = 3;
    public static final int STYPE_CLT = 4;
    public static final int STYPE_CA = 5;
    public static final int STYPE_CAV = 6;
    public static final int STYPE_CVL = 7;
    public static final int STYPE_FBB = 8;
    public static final int STYPE_BB = 9;
    public static final int STYPE_BBV = 10;
    public static final int STYPE_CV = 11;
    public static final int STYPE_XBB = 12;
    public static final int STYPE_SS = 13;
    public static final int STYPE_SSV = 14;
    public static final int STYPE_AP = 15;
    public static final int STYPE_AV = 16;
    public static final int STYPE_LHA = 17;
    public static final int STYPE_CVB = 18;
    public static final int STYPE_AR = 19;
    public static final int STYPE_AS = 20;
    public static final int STYPE_CT = 21;
    public static final int STYPE_AO = 22;


    // Equipment Constants (api_type 2)
    public static final int T2_GUN_SMALL = 1;
    public static final int T2_GUN_MEDIUM = 2;
    public static final int T2_GUN_LARGE = 3;
    public static final int T2_SUB_GUN = 4;
    public static final int T2_TORPEDO = 5;
    public static final int T2_FIGHTER = 6;
    public static final int T2_BOMBER = 7;
    public static final int T2_TORPEDO_BOMBER = 8;
    public static final int T2_SCOUT = 9;
    public static final int T2_SEA_SCOUT = 10;
    public static final int T2_SEA_BOMBER = 11;
    public static final int T2_RADAR_SMALL = 12;
    public static final int T2_RADAR_LARGE = 13;
    public static final int T2_SONAR = 14;
    public static final int T2_DEPTH_CHARGE = 15; // 폭뢰
    public static final int T2_EXT_ARMOR = 16; // NOT USED
    public static final int T2_TURBINE = 17; // 기관부강화
    public static final int T2_SANSHIKIDAN = 18;
    public static final int T2_AP_SHELL = 19;
    public static final int T2_VT_FUZE = 20; // NOT USED
    public static final int T2_MACHINE_GUN = 21;
    public static final int T2_KOHYOTEKI = 22;
    public static final int T2_DAMECON = 23;
    public static final int T2_LANDING_CRAFT = 24;
    public static final int T2_AUTOGYRO = 25;
    public static final int T2_ANTISUB_PATROL = 26;
    public static final int T2_EXT_ARMOR_M = 27;
    public static final int T2_EXT_ARMOR_L = 28;
    public static final int T2_SEARCHLIGHT = 29;
    public static final int T2_DRUM_CAN = 30;
    public static final int T2_REPAIR_INFRA = 31;
    public static final int T2_SS_TORPEDO = 32;
    public static final int T2_STAR_SHELL = 33;
    public static final int T2_COMMAND_FAC = 34;
    public static final int T2_AVI_PERSONNEL = 35;
    public static final int T2_ANTI_AIR_DEVICE = 36;
    public static final int T2_ANTI_GROUND_EQIP = 37;
    public static final int T2_GUN_LARGE_II = 38;
    public static final int T2_SHIP_PERSONNEL = 39;
    public static final int T2_SONAR_LARGE = 40;
    public static final int T2_FLYING_BOAT = 41;
    public static final int T2_SEARCHLIGHT_LARGE = 42;
    public static final int T2_COMBAT_FOOD = 43;
    public static final int T2_SUPPLIES = 44;
    public static final int T2_SEA_FIGHTER = 45;
    public static final int T2_AMP_TANK = 46;
    public static final int T2_LBA_AIRCRAFT = 47;
    public static final int T2_ITCP_FIGHTER = 48;

    public static final int T2_SAIUN_PART = 50;
    public static final int T2_SUBMARINE_RADER = 51;

    public static final int T2_JET_FIGHTER = 56;
    public static final int T2_JET_BOMBER = 57;
    public static final int T2_JET_TORPEDO_BOMBER = 58;
    public static final int T2_JET_SCOUT = 59;

    public static final int T2_RADER_LARGE_II = 93;
    public static final int T2_SCOUT_II = 94;

    public static final int[] T2LIST_AIRCRAFTS = {T2_FIGHTER, T2_BOMBER, T2_TORPEDO_BOMBER, T2_SCOUT, T2_SEA_SCOUT, T2_SEA_BOMBER, T2_FLYING_BOAT,
            T2_SEA_FIGHTER, T2_LBA_AIRCRAFT, T2_ITCP_FIGHTER, T2_JET_FIGHTER, T2_JET_BOMBER, T2_JET_TORPEDO_BOMBER, T2_JET_SCOUT};

    public static final int[] T2LIST_FIGHT_AIRCRAFTS = {T2_FIGHTER, T2_BOMBER, T2_TORPEDO_BOMBER, T2_SEA_BOMBER,
            T2_SEA_FIGHTER, T2_LBA_AIRCRAFT, T2_ITCP_FIGHTER, T2_JET_FIGHTER, T2_JET_BOMBER, T2_JET_TORPEDO_BOMBER};

    public static final int T3_COUNT = 47;
    public static final int[] T3LIST_IMPROVABLE = {1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 20, 21, 23, 24, 30, 34, 36, 38, 42, 43, 44};

    public static final int[] BASIC_MASTERY_MIN_BONUS = {0, 10, 25, 40, 55, 70, 85, 100};
    public static final int[] BASIC_MASTERY_MAX_BONUS = {9, 24, 39, 54, 69, 84, 99, 120};

    public static final int[] FIGHTER_MASTERY_BONUS = {0, 0, 2, 5, 9, 14, 14, 22, 0, 0, 0};
    public static final int[] SEA_BOMBER_MASTERY_BONUS = {0, 0, 1, 1, 1, 3, 3, 6, 0, 0, 0};

    public static final int TAG_COUNT = 6;

    public static final int SPEED_NONE = 0;
    public static final int SPEED_SLOW = 5;
    public static final int SPEED_FAST = 10;
    public static final int SPEED_FASTPLUS = 15;
    public static final int SPEED_SUPERFAST = 20;

    public static final int SPEED_MIXED_NORMAL = 100;
    public static final int SPEED_MIXED_FAST = 101;
    public static final int SPEED_MIXED_FASTPLUS = 102;

    public static final int AKASHI_TIMER_20MIN = 1200;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static void setDBHelper(KcaDBHelper hp) {
        if (helper == null) helper = hp;
    }

    public static boolean checkDataLoadTriggered() {
        return dataLoadTriggered;
    }

    public static boolean checkUserShipDataLoaded() {
        return userShipData != null && userShipData.size() > 0;
    }

    public static void setDataLoadTriggered() {
        dataLoadTriggered = true;
    }

    public static int getKcGameData(JsonObject api_data) {
        Log.e("KCA", "getKcGameData Called");
        kcGameData = api_data;
        if (kcGameData.has("api_mst_ship")) {
            JsonArray shipStatusArray = (JsonArray) kcGameData.get("api_mst_ship");
            JsonElement temp;
            for (Iterator<JsonElement> itr = shipStatusArray.iterator(); itr.hasNext(); ) {
                temp = itr.next();
                Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
                kcShipData.put(api_id, temp.getAsJsonObject());
            }
        }
        if (kcGameData.has("api_mst_slotitem")) {
            JsonArray itemStatusArray = (JsonArray) kcGameData.get("api_mst_slotitem");
            JsonElement temp;
            for (Iterator<JsonElement> itr = itemStatusArray.iterator(); itr.hasNext(); ) {
                temp = itr.next();
                Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
                kcItemData.put(api_id, temp.getAsJsonObject());
            }
        }
        if (kcGameData.has("api_mst_maparea")) {
            JsonArray mapAreaData = kcGameData.getAsJsonArray("api_mst_maparea");
            isEventTime = false;
            for (JsonElement e : mapAreaData) {
                int api_type = e.getAsJsonObject().get("api_type").getAsInt();
                if (api_type != 0) {
                    isEventTime = true;
                    break;
                }
            }
        }
        if (kcGameData.has("api_mst_useitem")) {
            JsonArray useitemData = kcGameData.getAsJsonArray("api_mst_useitem");
            for (JsonElement e : useitemData) {
                int api_id = e.getAsJsonObject().get("api_id").getAsInt();
                kcUseitemData.put(api_id, e.getAsJsonObject());
            }
        }
        if (kcGameData.has("api_mst_mission")) {
            JsonArray missionData = kcGameData.getAsJsonArray("api_mst_mission");
            for (JsonElement e : missionData) {
                int api_id = e.getAsJsonObject().get("api_id").getAsInt();
                kcMissionData.put(api_id, e.getAsJsonObject());
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("ship", kcShipData.size());
        data.addProperty("item", kcItemData.size());

        dataLoadTriggered = true;
        if (sHandler != null) {
            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_DATA_LOADED);
            bundle.putString("data", data.toString());
            Message sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        } else {
        }
        return kcGameData.entrySet().size();
    }

    public static boolean isGameDataLoaded() {
        return kcGameData != null;
    }

    public static int getAdmiralLevel() {
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) return basic_info.get("api_level").getAsInt();
        else return 0;
    }

    public static Integer getUserId() {
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if(basic_info != null) return basic_info.get("api_member_id").getAsInt();
        else return 0;
    }

    public static String getUserName() {
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) return basic_info.get("api_nickname").getAsString();
        else return "";
    }

    public static Integer getUserExperience() {
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) return basic_info.get("api_experience").getAsInt();
        else return 0;
    }

    public static JsonArray getKcSlotitemGameData() {
        if (kcGameData != null) return kcGameData.getAsJsonArray("api_mst_slotitem");
        else return null;
    }

    public static void addShipCountInBattle() {
        getShipCountInBattle += 1;
    }

    public static void addItemCountInBattle(int ship_id) {
        if (kcShipInitEquipCount != null) {
            String ship_id_str = String.valueOf(ship_id);
            int item_count = 0;
            if (kcShipInitEquipCount.has(ship_id_str)) {
                item_count = kcShipInitEquipCount.get(ship_id_str).getAsInt();
            }
            getItemCountInBattle += item_count;
            Log.e("KCA", "addItemCountInBattle: +" + String.valueOf(item_count));
        } else {
            Log.e("KCA", "addItemCountInBattle: init data is null");
        }
    }

    public static void resetShipCountInBattle() {
        getShipCountInBattle = 0;
    }

    public static void resetItemCountInBattle() {
        getItemCountInBattle = 0;
    }

    public static boolean checkUserShipMax() {
        int max_ship_size = 0;
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) max_ship_size = basic_info.get("api_max_chara").getAsInt();
        return max_ship_size == (getShipSize() + getShipCountInBattle);
    }

    public static int getUserMaxShipCount() {
        int max_ship_size = 0;
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) max_ship_size = basic_info.get("api_max_chara").getAsInt();
        return max_ship_size;
    }

    public static int getUserMaxItemCount() {
        int max_item_size = 0;
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) max_item_size = basic_info.get("api_max_slotitem").getAsInt();
        return max_item_size + 3;
    }

    public static boolean checkUserItemMax() {
        //Log.e("KCA", KcaUtils.format("Item: %d - %d", maxItemSize, helper.getItemCount() + getItemCountInBattle));
        int max_equip_size = 0;
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) max_equip_size = basic_info.get("api_max_slotitem").getAsInt();
        return max_equip_size <= getItemSize();
    }

    public static boolean checkEventUserShip() {
        int max_ship_size = 0;
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) max_ship_size = basic_info.get("api_max_chara").getAsInt();
        return max_ship_size < (getShipSize() + 5);
    }

    public static boolean checkEventUserItem() {
        int max_item_size = 0;
        JsonObject basic_info = helper.getJsonObjectValue(DB_KEY_BASICIFNO);
        if (basic_info != null) max_item_size = basic_info.get("api_max_slotitem").getAsInt();
        //Log.e("KCA", KcaUtils.format("Item: %d - %d", maxItemSize, helper.getItemCount() + getItemCountInBattle));
        return (max_item_size + 3) < (getItemSize() + 20);
    }


    public static boolean checkUserPortEnough() {
        return !(checkUserShipMax() || checkUserItemMax());
    }

    public static int getEventMapDifficulty(int no) {
        return eventMapDifficulty[no];
    }

    public static void setEventMapDifficulty(int no, int diff) {
        eventMapDifficulty[no] = diff;
    }

    public static boolean getReturnFlag(int mission_no) {
        return !(mission_no == 33 || mission_no == 34 || (mission_no > 130));
    }

    public static String getShipTranslation(String jp_name, boolean abbr) {
        if (currentLocaleCode.equals("jp")) {
            return jp_name;
        }

        String name = jp_name;
        String name_suffix = "";
        if (!kcShipTranslationData.has("suffixes")) {
            return jp_name;
        }

        JsonObject suffixes = kcShipTranslationData.getAsJsonObject("suffixes");
        for (Map.Entry<String, JsonElement> entry : suffixes.entrySet()) {
            if (jp_name.endsWith(entry.getKey())) {
                name = name.replaceAll(entry.getKey(), "");
                name_suffix = entry.getValue().getAsString();
                break;
            }
        }

        if (kcShipTranslationData.has(name)) {
            name = kcShipTranslationData.get(name).getAsString();
        }

        if (abbr) {
            for (Map.Entry<String, JsonElement> entry : kcShipAbbrData.entrySet()) {
                if (name.startsWith(entry.getKey())) {
                    name = name.replaceAll(entry.getKey(), entry.getValue().getAsString());
                    break;
                }
            }
        }

        return name.concat(name_suffix);
    }

    public static String getItemTranslation(String jp_name) {
        String name = jp_name;
        if (currentLocaleCode.equals("jp")) {
            return jp_name;
        } else if (kcItemTranslationData.has(name)) {
            name = kcItemTranslationData.get(name).getAsString();
        }
        return name;
    }

    public static String getUseitemTranslation(int id) {
        if (kcUseitemData.containsKey(id)) {
            JsonObject data = kcUseitemData.get(id).getAsJsonObject();
            return getItemTranslation(data.get("api_name").getAsString());
        } else {
            return "";
        }
    }

    public static int getShipTypeSize() {
        if (kcStypeData != null) return kcStypeData.size();
        else return -1;
    }

    public static String getShipTypeAbbr(int idx) {
        if (kcStypeData != null && idx < kcStypeData.size()) {
            return kcStypeData.get(idx).getAsString();
        } else {
            return "";
        }
    }

    public static int getShipSize() {
        int ship_size = 0;
        if (userShipData != null) ship_size = userShipData.size();
        else ship_size = helper.getShipCount();
        return ship_size + getShipCountInBattle;
    }

    public static int getItemSize() { return helper.getItemCount() + getItemCountInBattle; }

    private static JsonObject getJsonObjectFromStorage(Context context, String name) {
        return KcaUtils.getJsonObjectFromStorage(context, name, helper);
    }
    private static JsonArray getJsonArrayFromStorage(Context context, String name) {
        return KcaUtils.getJsonArrayFromStorage(context, name, helper);
    }


    public static int loadMapEdgeInfoFromStorage(Context context) {
        JsonObject data = getJsonObjectFromStorage(context, "edges.json");
        if (data != null) {
            helper.putValue(DB_KEY_MAPEDGES, data.toString());
            return 1;
        } else {
            return -1;
        }
    }

    public static int loadSubMapInfoFromStorage(Context context) {
        JsonObject data = getJsonObjectFromStorage(context, "map_sub.json");
        if (data != null) {
            helper.putValue(DB_KEY_MAPSUBDT, data.toString());
            return 1;
        } else {
            return -1;
        }
    }

    public static int loadShipExpInfoFromAssets(AssetManager am) {
        try {
            AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) am.open("exp_ship.json");
            byte[] bytes = ByteStreams.toByteArray(ais);
            JsonElement expShipData = new JsonParser().parse(new String(bytes));
            if (expShipData.isJsonObject()) {
                helper.putValue(DB_KEY_EXPSHIP, expShipData.toString());
                return 1;
            } else {
                return -1;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    public static int loadSortieExpInfoFromAssets(AssetManager am) {
        try {
            AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) am.open("exp_sortie.json");
            byte[] bytes = ByteStreams.toByteArray(ais);
            JsonElement expSortieData = new JsonParser().parse(new String(bytes));
            if (expSortieData.isJsonObject()) {
                helper.putValue(DB_KEY_EXPSORTIE, expSortieData.toString());
                return 1;
            } else {
                return -1;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    public static int loadShipTranslationDataFromStorage(Context context, String locale) {
        try {
            locale = getResourceLocaleCode(locale);
            JsonObject data = getJsonObjectFromStorage(context, KcaUtils.format("ships-%s.json", locale));
            AssetManager.AssetInputStream ais_abbr =
                    (AssetManager.AssetInputStream) context.getResources().getAssets().open("en-abbr.json");
            byte[] bytes_abbr = ByteStreams.toByteArray(ais_abbr);
            JsonElement data_abbr = new JsonParser().parse(new String(bytes_abbr));

            if (data != null) {
                kcShipTranslationData = data;
                kcShipAbbrData = data_abbr.getAsJsonObject();
                return 1;
            } else {
                return -1;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    public static int loadItemTranslationDataFromStorage(Context context, String locale) {
        locale = getResourceLocaleCode(locale);
        JsonObject data = getJsonObjectFromStorage(context, KcaUtils.format("items-%s.json", locale));
        if (data != null) {
            kcItemTranslationData = data.getAsJsonObject();
            return 1;
        } else {
            return -1;
        }
    }

    public static int loadStypeTranslationDataFromStorage(Context context, String locale) {
        locale = getResourceLocaleCode(locale);
        JsonArray data = getJsonArrayFromStorage(context, KcaUtils.format("stype-%s.json", locale));
        if (data != null) {
            kcStypeData = data;
            return 1;
        } else {
            return -1;
        }
    }

    public static int loadQuestInfoDataFromStorage(Context context, String locale) {
        locale = getResourceLocaleCode(locale);
        JsonObject data = getJsonObjectFromStorage(context, KcaUtils.format("quests-%s.json", locale));
        if (data != null) {
            kcQuestInfoData = data.getAsJsonObject();
            return 1;
        } else {
            return -1;
        }
    }

    public static void loadTranslationData(Context context) {
        loadTranslationData(context, false);
    }

    public static void loadTranslationData(Context context, boolean force) {
        boolean isDataLoaded = (kcShipTranslationData.entrySet().size() != 0) &&
                (kcItemTranslationData.entrySet().size() != 0) &&
                (kcQuestInfoData.entrySet().size() != 0);
        String locale = getStringPreferences(context, PREF_KCA_LANGUAGE);
        if (force || !isDataLoaded || !currentLocaleCode.equals(getResourceLocaleCode(locale))) {
            currentLocaleCode = getResourceLocaleCode(locale);
            if (!currentLocaleCode.equals("jp")) {
                int loadShipTranslationDataResult = loadShipTranslationDataFromStorage(context, locale);
                if (loadShipTranslationDataResult != 1) {
                    Toast.makeText(context, "Error loading Translation Info", Toast.LENGTH_LONG).show();
                }
                int loadItemTranslationDataResult = loadItemTranslationDataFromStorage(context, locale);
                if (loadItemTranslationDataResult != 1) {
                    Toast.makeText(context, "Error loading Translation Info", Toast.LENGTH_LONG).show();
                }
            }
            int loadStypeTranslationDataResult = loadStypeTranslationDataFromStorage(context, locale);
            if (loadStypeTranslationDataResult != 1) {
                Toast.makeText(context, "Error loading Stype Info", Toast.LENGTH_LONG).show();
            }
            int loadQuestInfoTranslationDataResult = loadQuestInfoDataFromStorage(context, locale);
            if (loadQuestInfoTranslationDataResult != 1) {
                Toast.makeText(context, "Error loading Quest Info", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static int loadSimpleExpeditionInfoFromStorage(Context context) {
        JsonArray data = getJsonArrayFromStorage(context, "expedition.json");
        if (data != null) {
            for (JsonElement item : data) {
                JsonObject expdata = item.getAsJsonObject();
                kcExpeditionData.add(expdata.get("no").getAsString(), expdata);
            }
            return 1;
        } else {
            return -1;
        }
    }

    public static int loadShipInitEquipCountFromStorage(Context context) {
        JsonObject data = getJsonObjectFromStorage(context, "ships_init_equip_count.json");
        if (data != null) {
            kcShipInitEquipCount = data.getAsJsonObject();
            return 1;
        } else {
            return -1;
        }
    }

    public static int loadQuestTrackDataFromStorage(KcaDBHelper helper, Context context) {
        JsonObject data = getJsonObjectFromStorage(context, "quest_track.json");
        if (data != null) {
            helper.putValue(DB_KEY_QUESTTRACK, data.toString());
            return 1;
        } else {
            return -1;
        }
    }

    public static JsonObject loadSpecialEquipmentShipInfo(AssetManager am) {
        try {
            AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) am.open("equip_special.json");
            byte[] bytes = ByteStreams.toByteArray(ais);
            JsonElement data = new JsonParser().parse(new String(bytes));
            if (data.isJsonObject()) {
                return data.getAsJsonObject();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isQuestTrackable(String id) {
        JsonObject kcQuestTrackData = helper.getJsonObjectValue(DB_KEY_QUESTTRACK);
        int id_int = Integer.parseInt(id);
        return kcQuestTrackData.has(id);
    }

    public static JsonObject getQuestTrackInfo(String id) {
        JsonObject kcQuestTrackData = helper.getJsonObjectValue(DB_KEY_QUESTTRACK);
        if (kcQuestTrackData.has(id)) {
            return kcQuestTrackData.getAsJsonObject(id);
        } else {
            return null;
        }
    }

    public static int getUseitemCount(int id) {
        JsonArray kcUseitemData = helper.getJsonArrayValue(DB_KEY_USEITEMS);
        if (kcUseitemData != null) {
            for (int i = 0; i < kcUseitemData.size(); i++) {
                JsonObject data = kcUseitemData.get(i).getAsJsonObject();
                if (data.get("api_id").getAsInt() == id) {
                    return data.get("api_count").getAsInt();
                }
            }
            return 0;
        }
        return -1;
    }

    public static void addUseitemCount(int id) {
        JsonArray kcUseitemData = helper.getJsonArrayValue(DB_KEY_USEITEMS);
        if (kcUseitemData != null) {
            for (int i = 0; i < kcUseitemData.size(); i++) {
                JsonObject data = kcUseitemData.get(i).getAsJsonObject();
                if (data.get("api_id").getAsInt() == id) {
                    int orig = data.get("api_count").getAsInt();
                    data.addProperty("api_count", orig + 1);
                    helper.putValue(DB_KEY_USEITEMS, kcUseitemData.toString());
                    return;
                }
            }
            // if not exist, add new item
            JsonObject data = new JsonObject();
            data.addProperty("api_id", id);
            data.addProperty("api_count", 1);
            kcUseitemData.add(data);
            helper.putValue(DB_KEY_USEITEMS, kcUseitemData.toString());
        }
    }

    public static String getCurrentNodeAlphabet(int maparea, int mapno, int no) {
        String currentMapString = KcaUtils.format("%d-%d", maparea, mapno);
        String no_str = String.valueOf(no);
        if (helper != null) {
            JsonObject mapEdgeInfo = helper.getJsonObjectValue(DB_KEY_MAPEDGES);
            if (mapEdgeInfo != null && mapEdgeInfo.has(currentMapString)) {
                JsonObject currentMapInfo = mapEdgeInfo.getAsJsonObject(currentMapString);
                if (currentMapInfo.has(no_str)) {
                    JsonArray nodeInfo = currentMapInfo.getAsJsonArray(no_str);
                    no_str = nodeInfo.get(1).getAsString();
                }
            }
        }
        return no_str;
    }

    public static boolean getCurrentNodeSubExist(int maparea, int mapno, int no) {
        String currentMapString = KcaUtils.format("%d-%d", maparea, mapno);
        if (helper != null) {
            JsonObject subEdgeInfo = helper.getJsonObjectValue(DB_KEY_MAPSUBDT);
            if (subEdgeInfo != null && subEdgeInfo.has(currentMapString)) {
                JsonArray sub_edges = subEdgeInfo.getAsJsonArray(currentMapString);
                return sub_edges.contains(new JsonPrimitive(no));
            }
        }
        return false;
    }

    public static boolean isExpeditionDataLoaded() {
        return kcExpeditionData.entrySet().size() > 0;
    }

    public static JsonObject getExpeditionInfo(int mission_no, String locale) {
        int mission_key = mission_no;
        if (mission_no >= 130) {
            if (mission_no % 2 == 1) mission_key = 133;
            else mission_key = 134;
        }
        JsonObject rawdata = kcExpeditionData.getAsJsonObject(String.valueOf(mission_key));
        JsonObject data = new JsonParser().parse(rawdata.toString()).getAsJsonObject();
        JsonObject name = data.getAsJsonObject("name");
        if (name.has(locale)) {
            data.addProperty("name", name.get(locale).getAsString());
        } else {
            data.addProperty("name", name.get("en").getAsString());
        }
        return data;
    }

    public static String getExpeditionName(int mission_no, String locale) {
        int mission_key = mission_no;
        if (mission_no >= 130) {
            if (mission_no % 2 == 1) mission_key = 133;
            else mission_key = 134;
        }
        JsonObject data = kcExpeditionData.getAsJsonObject(String.valueOf(mission_key));
        JsonObject name = data.getAsJsonObject("name");
        if (name.has(locale)) {
            return name.get(locale).getAsString();
        } else {
            return name.get("en").getAsString();
        }
    }

    public static long getExpeditionDuration(int mission_no) {
        int mission_key = mission_no;
        if (mission_no >= 130) {
            if (mission_no % 2 == 1) mission_key = 133;
            else mission_key = 134;
        }
        JsonObject data = kcExpeditionData.getAsJsonObject(String.valueOf(mission_key));
        int time = data.get("time").getAsInt();
        return (long) (time * 60 * 1000);
    }

    // warning: event support expedition will not work with this
    public static int getExpeditionNoByName(String name) {
        for(Map.Entry<String, JsonElement> v: kcExpeditionData.entrySet()) {
            JsonObject data = v.getValue().getAsJsonObject();
            String exp_name = data.getAsJsonObject("name").get("jp").getAsString();
            if (exp_name.equals(name)) {
                return Integer.parseInt(v.getKey());
            }
        }
        return -1;
    }

    public static int getPortData(JsonObject api_data) {
        if (api_data.has("api_basic")) {
            JsonObject basicInfo = (JsonObject) api_data.get("api_basic");
            helper.putValue(DB_KEY_BASICIFNO, basicInfo.toString());
        }
        if (api_data.has("api_ship")) {
            JsonArray shipDataArray = (JsonArray) api_data.get("api_ship");
            updateUserShipData(shipDataArray);
        }
        return userShipData.size();
    }

    public static int updateUserShipData(JsonArray data) {
        Set<Integer> prevItemIds;
        if (userShipData == null) {
            userShipData = new HashMap<Integer, JsonObject>();
            prevItemIds = new HashSet<Integer>();
        } else {
            prevItemIds = new HashSet<Integer>(userShipData.keySet());
        }

        JsonElement temp;
        for (Iterator<JsonElement> itr = data.iterator(); itr.hasNext(); ) {
            temp = itr.next();
            Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
            if (!prevItemIds.contains(api_id)) {
                userShipData.put(api_id, temp.getAsJsonObject());
            } else if (!userShipData.get(api_id).equals(temp)) {
                userShipData.put(api_id, temp.getAsJsonObject());
            }
            prevItemIds.remove(api_id);
        }
        for (Integer i : prevItemIds) {
            userShipData.remove(i);
        }
        return userShipData.size();
    }

    public static int updatePortDataOnBattle(JsonObject api_data) {
        if (api_data.has("api_ship_data")) {
            JsonArray shipDataArray = (JsonArray) api_data.get("api_ship_data");
            JsonElement temp;
            for (Iterator<JsonElement> itr = shipDataArray.iterator(); itr.hasNext(); ) {
                temp = itr.next();
                Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
                userShipData.put(api_id, temp.getAsJsonObject());
            }
            return shipDataArray.size();
        } else {
            return -1;
        }
    }

    // DB-based functions
    public static int putSlotItemDataToDB(JsonArray api_data) {
        if (helper == null) return -1;
        helper.putBulkItemValue(api_data);
        return api_data.size();
    }

    public static void removeSlotItemData(String list) {
        if (helper == null) return;
        String[] requestList = list.split(",");
        helper.removeItemValue(requestList);
    }

    public static int updateSlotItemData(JsonObject api_data) {
        if (helper == null) return -1;
        JsonObject item = null;
        if (api_data.has("api_create_flag") && api_data.get("api_create_flag").getAsInt() == 1) {
            item = (JsonObject) api_data.get("api_slot_item");
        } else if (api_data.has("api_slotitem_id")) {
            item = api_data;
        }
        if (item != null) {
            int item_id = item.get("api_id").getAsInt();
            int kc_item_id = item.get("api_slotitem_id").getAsInt();
            int itemType = getKcItemStatusById(kc_item_id, "type").get("type").getAsJsonArray().get(2).getAsInt();
            item.addProperty("api_locked", 0);
            item.addProperty("api_level", 0);
            if (isItemAircraft(itemType)) {
                item.addProperty("api_alv", 0);
            }
            helper.putItemValue(item_id, item.toString());
            Log.e("KCA", KcaUtils.format("add item %d", item_id));
            return kc_item_id;
        }
        return 0;
    }

    public static int getItemCountByKcId(int id) {
        return helper.getItemCountByKcId(id);
    }

    public static JsonObject getKcShipDataById(int id, String list) {
        if (kcGameData == null) return null;
        JsonObject temp = new JsonObject();
        if (kcShipData.containsKey(id)) {
            if (list.equals("all")) {
                return kcShipData.get(id);
            } else {
                String[] requestList = list.split(",");
                for (int i = 0; i < requestList.length; i++) {
                    String orig_api_item = requestList[i];
                    String api_item = orig_api_item;
                    if (!api_item.startsWith("api_")) {
                        api_item = "api_" + api_item;
                    }
                    temp.add(orig_api_item, kcShipData.get(id).get(api_item));
                }
                return temp;
            }
        } else {
            Log.e("KCA", String.valueOf(id) + " not in list");
            return null;
        }
    }

    public static JsonObject getUserShipDataById(int id, String list) {
        JsonObject target_data = null;
        JsonObject return_data = new JsonObject();
        if (userShipData != null) {
            if (userShipData.containsKey(id)) {
                target_data = userShipData.get(id);
            }
        } else if (helper.getShipCount() > 0) {
            JsonArray data = helper.getJsonArrayValue(DB_KEY_SHIPIFNO);
            for (int i = 0; i < data.size(); i++) {
                JsonObject item = data.get(i).getAsJsonObject();
                int aid = item.get("api_id").getAsInt();
                if (aid == id) {
                    target_data = item;
                    break;
                }
            }
        }

        if (target_data != null) {
            if (list.equals("all")) {
                return target_data;
            } else {
                String[] requestList = list.split(",");
                for (String orig_api_item: requestList) {
                    String api_item = orig_api_item;
                    if (!api_item.startsWith("api_")) {
                        api_item = "api_" + api_item;
                    }
                    if (target_data.has(api_item)) {
                        return_data.add(orig_api_item, target_data.get(api_item));
                    }
                }
            }
        }
        return return_data;
    }

    public static int countUserShipById(int ship_id) {
        int count = 0;
        if (userShipData == null && helper.getShipCount() == 0) return -1;
        else if (userShipData != null) {
            for (JsonObject ship : userShipData.values()) {
                if (ship_id == ship.get("api_ship_id").getAsInt()) count += 1;
            }
        }
        else if (helper.getShipCount() > 0) {
            JsonArray data = helper.getJsonArrayValue(DB_KEY_SHIPIFNO);
            for (int i = 0; i < data.size(); i++) {
                JsonObject ship = data.get(i).getAsJsonObject();
                if (ship_id == ship.get("api_ship_id").getAsInt()) count += 1;
            }
        }
        return count;
    }

    public static JsonObject getUserItemStatusById(int id, String list, String kclist) {
        if (helper == null || kcGameData == null) return null;
        String data = helper.getItemValue(id);
        if (data != null) {
            JsonObject userData = new JsonParser().parse(data).getAsJsonObject();
            int kc_item_id = userData.get("api_slotitem_id").getAsInt();
            JsonObject kcData = getKcItemStatusById(kc_item_id, kclist);

            if (list.equals("all")) {
                for (Map.Entry<String, JsonElement> k : userData.entrySet()) {
                    kcData.add(k.getKey(), userData.get(k.getKey()));
                }
            } else {
                String[] requestList = list.split(",");
                for (int i = 0; i < requestList.length; i++) {
                    String orig_api_item = requestList[i];
                    String api_item = orig_api_item;
                    if (!api_item.startsWith("api_")) {
                        api_item = "api_" + api_item;
                    }
                    if (userData.has(api_item)) {
                        kcData.add(orig_api_item, userData.get(api_item));
                    }
                }
            }
            kcData.remove("");
            return kcData;
        } else {
            return null;
        }
    }

    public static JsonObject getKcItemStatusById(int id, String list) {
        if (kcGameData == null) return null;
        JsonObject temp = new JsonObject();
        if (kcItemData.containsKey(id)) {
            if (list.equals("all")) {
                return kcItemData.get(id);
            } else {
                String[] requestList = list.split(",");
                for (int i = 0; i < requestList.length; i++) {
                    String orig_api_item = requestList[i];
                    String api_item = orig_api_item;
                    if (!api_item.startsWith("api_")) {
                        api_item = "api_" + api_item;
                    }
                    temp.add(orig_api_item, kcItemData.get(id).get(api_item));
                }
                return temp.getAsJsonObject();
            }
        } else {
            return null;
        }

    }

    public static boolean isItemAircraft(int id) {
        int result = Arrays.binarySearch(T2LIST_AIRCRAFTS, id);
        return result > -1;
    }

    public static void addUserShip(JsonObject api_data) {
        if (kcGameData == null) return;
        if (api_data.has("api_id")) {
            int shipId = api_data.get("api_id").getAsInt();
            JsonObject shipData = (JsonObject) api_data.get("api_ship");
            userShipData.put(shipId, shipData);
            int shipKcId = api_data.get("api_ship_id").getAsInt();
            String shipName = getKcShipDataById(shipKcId, "name").get("name").getAsString();
            Log.e("KCA", KcaUtils.format("add ship %d (%s)", shipId, shipName));
            if (api_data.has("api_slotitem") && !api_data.get("api_slotitem").isJsonNull()) {
                JsonArray shipSlotItemData = (JsonArray) api_data.get("api_slotitem");
                for (int i = 0; i < shipSlotItemData.size(); i++) {
                    JsonObject item = shipSlotItemData.get(i).getAsJsonObject();
                    updateSlotItemData(item);
                }
            }
        }
    }

    public static void updateSuppliedUserShip(JsonArray api_ship) {
        if (kcGameData == null) return;
        for (int i = 0; i < api_ship.size(); i++) {
            JsonObject item = api_ship.get(i).getAsJsonObject();
            if (item.has("api_id")) {
                int shipId = item.get("api_id").getAsInt();
                JsonObject data = userShipData.get(shipId);
                data.addProperty("api_fuel", item.get("api_fuel").getAsInt());
                data.addProperty("api_bull", item.get("api_bull").getAsInt());
                data.add("api_onslot", item.get("api_onslot"));
                userShipData.put(shipId, data);
            }
        }
    }

    public static void updateShipMorale(int ship_id) {
        if (userShipData.containsKey(ship_id)) {
            JsonObject data = userShipData.get(ship_id).getAsJsonObject();
            int cond = data.get("api_cond").getAsInt();
            if (cond < 40) {
                cond = 40;
                data.addProperty("api_cond", cond);
                userShipData.put(ship_id, data);
            }
        }
    }

    public static void updateShipHpFull(int ship_id) {
        if (userShipData.containsKey(ship_id)) {
            JsonObject data = userShipData.get(ship_id).getAsJsonObject();
            int maxhp = data.get("api_maxhp").getAsInt();
            data.addProperty("api_nowhp", maxhp);
            userShipData.put(ship_id, data);
        }
    }

    public static void updateUserShip(JsonObject api_data) {
        if (kcGameData == null) return;
        if (api_data.has("api_id")) {
            int shipId = api_data.get("api_id").getAsInt();
            userShipData.put(shipId, api_data);
            int shipKcId = api_data.get("api_ship_id").getAsInt();
            String shipName = getKcShipDataById(shipKcId, "name").get("name").getAsString();
            Log.e("KCA", KcaUtils.format("update ship %d (%s)", shipId, shipName));
        }
    }

    public static void updateUserShipSlot(int shipId, JsonObject api_data) {
        if (kcGameData == null) return;
        if (api_data.has("api_slot")) {
            JsonObject shipData = userShipData.get(shipId).getAsJsonObject();
            shipData.add("api_slot", api_data.getAsJsonArray("api_slot"));
            userShipData.put(shipId, shipData);
        }
    }

    public static void deleteUserShip(String list, int dest_flag) {
        if (kcGameData == null) return;

        String[] requestList = list.split(",");
        for (int i = 0; i < requestList.length; i++) {
            int shipId = Integer.valueOf(requestList[i]);
            JsonObject shipKcData = getUserShipDataById(shipId, "ship_id,slot");
            if (shipKcData != null) {
                int shipKcId = shipKcData.get("ship_id").getAsInt();
                if (dest_flag > 0) {
                    JsonArray shipSlotItem = (JsonArray) shipKcData.get("slot");
                    List<String> shipSlotItemList = new ArrayList<String>();
                    for (int j = 0; j < shipSlotItem.size(); j++) {
                        int item = shipSlotItem.get(j).getAsInt();
                        if (item != -1) {
                            shipSlotItemList.add(String.valueOf(item));
                        }
                    }
                    if (shipSlotItemList.size() > 0) {
                        removeSlotItemData(joinStr(shipSlotItemList, ","));
                    }
                }
                userShipData.remove(shipId);

                String shipName = getKcShipDataById(shipKcId, "name").get("name").getAsString();
                Log.e("KCA", KcaUtils.format("remove ship %d (%s)", shipId, shipName));
            } else {
                Log.e("KCA", KcaUtils.format("Not found info with %d", shipId));
            }
        }
    }

    public static int getStatus(int value) {
        if (value > 75) return STATE_NORMAL;
        else if (value > 50) return STATE_LIGHTDMG;
        else if (value > 25) return STATE_MODERATEDMG;
        else return STATE_HEAVYDMG;
    }

    public static String getNodeFullInfo(Context context, String currentNode, int id, int kind, boolean includeNormal) {

        String currentNodeInfo = "";
        switch (id) {
            case API_NODE_EVENT_ID_OBTAIN:
                currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_obtain), currentNode);
                break;
            case API_NODE_EVENT_ID_LOSS:
                currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_loss), currentNode);
                break;
            case API_NODE_EVENT_ID_NORMAL:
                if (kind == API_NODE_EVENT_KIND_BATTLE) {
                    if (includeNormal) {
                        currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_normal_battle), currentNode);
                    } else {
                        currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_normal), currentNode);
                    }
                } else if (kind == API_NODE_EVENT_KIND_NIGHTBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_nightbattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_NIGHTDAYBATTLE_EC) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_nightdaybattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_AIRBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_airbattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_ECBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_ecbattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_LDAIRBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_ldairbattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_LDSHOOTING) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_ldshooting), currentNode);
                }
                break;
            case API_NODE_EVENT_ID_BOSS:
                if (kind == API_NODE_EVENT_KIND_NIGHTBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_boss_nightbattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_NIGHTDAYBATTLE_EC) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_boss_nightdaybattle), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_ECBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_boss_ecbattle), currentNode);
                } else {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_boss), currentNode);
                }
                break;
            case API_NODE_EVENT_ID_NOEVENT:
                if (kind == API_NODE_EVENT_KIND_SELECTABLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_selectable), currentNode);
                } else {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_noevent), currentNode);
                }
                break;
            case API_NODE_EVENT_ID_TPOINT:
                currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_tpoint), currentNode);
                break;
            case API_NODE_EVENT_ID_AIR:
                if (kind == API_NODE_EVENT_KIND_AIRSEARCH) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_airsearch), currentNode);
                } else if (kind == API_NODE_EVENT_KIND_AIRBATTLE) {
                    currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_airbattle), currentNode);
                }
                break;
            case API_NODE_EVENT_ID_SENDAN:
                currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_sendan), currentNode);
                break;
            case API_NODE_EVENT_ID_LDAIRBATTLE:
                currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_ldairbattle), currentNode);
                break;
            default:
                currentNodeInfo = KcaUtils.format(context.getString(R.string.node_info_normal), currentNode);
                break;
        }
        return currentNodeInfo;
    }

    public static int getNodeColor(Context context, int api_event_id, int api_event_kind, int api_color_no) {
        switch (api_color_no) {
            case 2:
                return ContextCompat.getColor(context, R.color.colorItem);
            case 6:
            case 9:
                return ContextCompat.getColor(context, R.color.colorItemSpecial);
            case 3:
                return ContextCompat.getColor(context, R.color.colorVortex);
            case 4:
                if (api_event_id == API_NODE_EVENT_ID_NOEVENT) {
                    if (api_event_kind == API_NODE_EVENT_KIND_SELECTABLE) { // selectable
                        return ContextCompat.getColor(context, R.color.colorSelectable);
                    } else {
                        return ContextCompat.getColor(context, R.color.colorNone);
                    }
                } else if (api_event_kind == API_NODE_EVENT_KIND_NIGHTBATTLE) {
                    return ContextCompat.getColor(context, R.color.colorNightBattle);
                } else {
                    return ContextCompat.getColor(context, R.color.colorBattle);
                }
            case 5:
                return ContextCompat.getColor(context, R.color.colorBossBattle);
            case 7:
            case 10:
                return ContextCompat.getColor(context, R.color.colorAirBattle);
            case 8:
                return ContextCompat.getColor(context, R.color.colorNone);
            case 11:
            case 12:
                return ContextCompat.getColor(context, R.color.colorNightBattle);
            case 13:
                return ContextCompat.getColor(context, R.color.colorLdShooting);
            default:
                return ContextCompat.getColor(context, R.color.colorPrimaryDark);
        }
    }


    public static String getFormationString(Context context, int v, boolean is_short) {
        switch (v) {
            case FORMATION_LAH:
                return context.getString(R.string.formation_lineahead);
            case FORMATION_DLN:
                return context.getString(R.string.formation_doubleline);
            case FORMATION_DIA:
                return context.getString(R.string.formation_diamond);
            case FORMATION_ECH:
                return context.getString(R.string.formation_echelon);
            case FORMATION_LAB:
                return context.getString(R.string.formation_lineabreast);
            case FORMATION_DEF:
                return context.getString(R.string.formation_defensive);
            case FORMATION_C1:
                if (is_short) return context.getString(R.string.formation_c1_short);
                else return context.getString(R.string.formation_c1);
            case FORMATION_C2:
                if (is_short) return context.getString(R.string.formation_c2_short);
                else return context.getString(R.string.formation_c2);
            case FORMATION_C3:
                if (is_short) return context.getString(R.string.formation_c3_short);
                else return context.getString(R.string.formation_c3);
            case FORMATION_C4:
                if (is_short) return context.getString(R.string.formation_c4_short);
                else return context.getString(R.string.formation_c4);
            default:
                return "";
        }
    }

    public static String getEngagementString(Context context, int v) {
        switch (v) {
            case ENGAGE_PARL:
                return context.getString(R.string.engagement_parallel);
            case ENGAGE_HDON:
                return context.getString(R.string.engagement_headon);
            case ENGAGE_TADV:
                return context.getString(R.string.engagement_t_advantage);
            case ENGAGE_TDIS:
                return context.getString(R.string.engagement_t_disadvantage);
            default:
                return "";
        }
    }

    public static String getAirForceResultString(Context context, int v) {
        switch (v) {
            case AIR_SUPERMACY:
                return context.getString(R.string.air_supermacy);
            case AIR_SUPERIORITY:
                return context.getString(R.string.air_superiority);
            case AIR_PARITY:
                return context.getString(R.string.air_parity);
            case AIR_DENIAL:
                return context.getString(R.string.air_denial);
            case AIR_INCAPABILITY:
                return context.getString(R.string.air_incapability);
            default:
                Log.e("KCA", "Unknown Value: " + String.valueOf(v));
                return "";
        }
    }

    public static String getItemString(Context context, int v) {
        switch (v) {
            case ITEM_FUEL:
                return context.getString(R.string.item_fuel);
            case ITEM_AMMO:
                return context.getString(R.string.item_ammo);
            case ITEM_STEL:
                return context.getString(R.string.item_stel);
            case ITEM_BAUX:
                return context.getString(R.string.item_baux);
            case ITEM_BRNR:
                return context.getString(R.string.item_brnr);
            case ITEM_BGTZ:
                return context.getString(R.string.item_bgtz);
            case ITEM_MMAT:
                return context.getString(R.string.item_mmat);
            case ITEM_KMAT:
                return context.getString(R.string.item_kmat);
            case ITEM_BOXS:
                return context.getString(R.string.item_boxs);
            case ITEM_BOXM:
                return context.getString(R.string.item_boxm);
            case ITEM_BOXL:
                return context.getString(R.string.item_boxl);
            default:
                return "";
        }
    }

    public static String getSpeedString(Context context, int speedValue) {
        String speedStringValue = "";
        switch (speedValue) {
            case KcaApiData.SPEED_SUPERFAST:
                speedStringValue = context.getString(R.string.speed_superfast);
                break;
            case KcaApiData.SPEED_FASTPLUS:
                speedStringValue = context.getString(R.string.speed_fastplus);
                break;
            case KcaApiData.SPEED_FAST:
                speedStringValue = context.getString(R.string.speed_fast);
                break;
            case KcaApiData.SPEED_SLOW:
                speedStringValue = context.getString(R.string.speed_slow);
                break;
            case KcaApiData.SPEED_MIXED_FASTPLUS:
                speedStringValue = context.getString(R.string.speed_mixed_fastplus);
                break;
            case KcaApiData.SPEED_MIXED_FAST:
                speedStringValue = context.getString(R.string.speed_mixed_fast);
                break;
            case KcaApiData.SPEED_MIXED_NORMAL:
                speedStringValue = context.getString(R.string.speed_mixed_normal);
                break;
            default:
                speedStringValue = context.getString(R.string.speed_none);
                break;
        }
        return speedStringValue;
    }

    public static JsonObject findRemodelLv(String idlist) {
        JsonObject result = new JsonObject();
        String[] target = idlist.split(",");
        for (String id : target) {
            result.addProperty(id, 1);
        }
        for (Map.Entry<Integer, JsonObject> entry : kcShipData.entrySet()) {
            JsonObject data = entry.getValue();
            if (data.has("api_aftershipid")) {
                String[] targetiter = target;
                for (String id : targetiter) {
                    if (id.equals(data.get("api_aftershipid").getAsString())) {
                        result.addProperty(id, data.get("api_afterlv").getAsString());
                        target = ArrayUtils.removeElement(target, id);
                        break;
                    }
                }
            }
            if (target.length == 0) break;
        }
        return result;
    }

    public static JsonObject buildShipUpdateData() {
        List<Map.Entry<Integer, JsonObject>> list = new ArrayList<>(kcShipData.entrySet());
        Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));

        JsonObject frombefore = new JsonObject();
        JsonObject fromafter = new JsonObject();
        JsonObject afterlv_data = new JsonObject();
        Set<String> checked = new HashSet<>();
        for (Map.Entry<Integer, JsonObject> entry : list) {
            JsonObject data = entry.getValue();
            String api_id = data.get("api_id").getAsString();
            if (data.has("api_aftershipid")) {
                String after_id = data.get("api_aftershipid").getAsString();
                String key = KcaUtils.format("%s->%s", api_id, after_id);
                String revkey = KcaUtils.format("%s->%s", after_id, api_id);
                if (!after_id.equals("0")) checked.add(api_id);
                int afterlv = data.get("api_afterlv").getAsInt();
                checked.add(key);
                if (!checked.contains(revkey) && !after_id.equals("0")) {
                    afterlv_data.addProperty(key, afterlv);
                    frombefore.addProperty(api_id, after_id);
                    fromafter.addProperty(after_id, api_id);
                }
            }
        }
        JsonObject sdata = new JsonObject();
        sdata.add("afterlv", afterlv_data);
        sdata.add("frombefore", frombefore);
        sdata.add("fromafter", fromafter);
        return sdata;
    }

    public static int findAfterShipId(int startid, int level) {
        int sid = startid;
        for (int i = 0; i < level; i++) {
            if (sid == 0) return 0;
            else {
                JsonObject kcShipData = getKcShipDataById(sid, "aftershipid");
                sid = kcShipData.get("aftershipid").getAsInt();
            }
        }
        return sid;
    }

    public static int[] removeKai(JsonArray slist, boolean exception) {
        List<Integer> afterShipList = new ArrayList<Integer>();
        List filteredShipList = new ArrayList<>();
        for (int i = 0; i < slist.size(); i++) {
            int sid = slist.get(i).getAsInt();
            for (int lv = 1; lv <= 3; lv++) {
                int after = findAfterShipId(sid, lv);
                if (after == 0 || after != sid) {
                    afterShipList.add(after);
                } else {
                    break;
                }
            }
        }
        for (int i = 0; i < slist.size(); i++) {
            int sid = slist.get(i).getAsInt();
            if (exception || afterShipList.indexOf(sid) == -1) {
                filteredShipList.add(sid);
            }
        }
        int[] result = new int[filteredShipList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (int) filteredShipList.get(i);
        }
        return result;
    }

    public static int[] getLeftExpToNext(int current_lv, int exp) {
        int[] info = {current_lv, 0};
        if (current_lv != 99 && current_lv != 165) {
            JsonObject expship_data = helper.getJsonObjectValue(DB_KEY_EXPSHIP);
            JsonArray data_for_current = expship_data.getAsJsonArray(String.valueOf(current_lv));
            int left = data_for_current.get(0).getAsInt() + data_for_current.get(1).getAsInt() - exp;
            if (left > 0) {
                info[1] = left;
            } else {
                return getLeftExpToNext(current_lv + 1, exp);
            }
        }
        return info;
    }

    public static int getTypeRes(int type) {
        int typeres = 0;
        try {
            typeres = KcaUtils.getId(KcaUtils.format("item_%d", type), R.mipmap.class);
        }
        catch (Exception e) {
            helper.recordErrorLog(ERROR_TYPE_BATTLEVIEW, "", "", "", getStringFromException(e));
            typeres = R.mipmap.item_0;
        }
        return typeres;
    }
}
