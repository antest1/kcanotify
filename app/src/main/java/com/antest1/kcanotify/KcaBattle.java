package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaApiData.helper;
import static com.antest1.kcanotify.KcaConstants.API_NODE_EVENT_ID_BOSS;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_AIRBATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLERESULT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_EACH;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_EACH_WATER;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_EC;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_EC_NIGHTTODAY;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_MIDNIGHT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_MIDNIGHT_EC;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_MIDNIGHT_SP;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_BATTLE_WATER;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_GOBACKPORT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_COMBINED_LDAIRBATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_MAP_NEXT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_MAP_START;
import static com.antest1.kcanotify.KcaConstants.API_REQ_PRACTICE_BATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_PRACTICE_BATTLE_RESULT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_PRACTICE_MIDNIGHT_BATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_AIRBATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_BATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_BATTLE_MIDNIGHT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_BATTLE_MIDNIGHT_SP;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_BATTLE_RESULT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_GOBACKPORT;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_LDAIRBATTLE;
import static com.antest1.kcanotify.KcaConstants.API_REQ_SORTIE_LDSHOOTING;
import static com.antest1.kcanotify.KcaConstants.COMBINED_A;
import static com.antest1.kcanotify.KcaConstants.COMBINED_W;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLEINFO;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_BATTLENODE;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_QTRACKINFO;
import static com.antest1.kcanotify.KcaConstants.HD_DAMECON;
import static com.antest1.kcanotify.KcaConstants.HD_DANGER;
import static com.antest1.kcanotify.KcaConstants.HD_NONE;
import static com.antest1.kcanotify.KcaConstants.JUDGE_A;
import static com.antest1.kcanotify.KcaConstants.JUDGE_B;
import static com.antest1.kcanotify.KcaConstants.JUDGE_C;
import static com.antest1.kcanotify.KcaConstants.JUDGE_D;
import static com.antest1.kcanotify.KcaConstants.JUDGE_E;
import static com.antest1.kcanotify.KcaConstants.JUDGE_S;
import static com.antest1.kcanotify.KcaConstants.JUDGE_SS;
import static com.antest1.kcanotify.KcaConstants.KCA_API_NOTI_BATTLE_DROPINFO;
import static com.antest1.kcanotify.KcaConstants.KCA_API_NOTI_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_API_NOTI_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_API_NOTI_HEAVY_DMG;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PROCESS_BATTLE_FAILED;
import static com.antest1.kcanotify.KcaConstants.PHASE_1;
import static com.antest1.kcanotify.KcaConstants.PHASE_2;
import static com.antest1.kcanotify.KcaConstants.PHASE_3;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;

public class KcaBattle {
    public static JsonObject deckportdata = null;
    public static JsonObject currentApiData = null;

    public static JsonArray ship_ke = null;
    public static JsonArray ship_ke_combined = null;

    public static JsonArray friendMaxHps, friendCbMaxHps;
    public static JsonArray friendNowHps, friendCbNowHps;
    public static JsonArray friendAfterHps, friendCbAfterHps;

    public static JsonArray enemyMaxHps, enemyCbMaxHps;
    public static JsonArray enemyNowHps, enemyCbNowHps;
    public static JsonArray enemyAfterHps, enemyCbAfterHps;

    public static JsonObject escapedata = null;
    public static JsonArray escapelist = new JsonArray();
    public static JsonArray escapecblist = new JsonArray();
    public static JsonArray damecon_used = new JsonArray();

    public static boolean[] checkhdmgflag = new boolean[7];
    public static boolean[] checkhdmgcbflag = new boolean[7];
    public static boolean[] dameconflag = new boolean[7];
    public static boolean[] dameconcbflag = new boolean[7];

    private static Gson gson = new Gson();
    public static Handler sHandler;

    public static int currentMapArea = -1;
    public static int currentMapNo = -1;
    public static int currentNode = -1;
    public static int currentEventMapRank = 0;
    public static int currentEnemyFormation = -1;

    public static int currentFleet = -1;
    public static boolean isCombined = false;
    public static boolean isEndReached = false;
    public static boolean isBossReached = false;
    public static int startHeavyDamageExist;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static JsonObject getCurrentApiData() {
        return currentApiData;
    }

    public static void setCurrentApiData(JsonObject obj) {
        currentApiData = obj;
    }

    public static void setStartHeavyDamageExist(int v) {
        startHeavyDamageExist = v;
    }

    public static boolean isCombinedFleetInSortie() { return KcaBattle.currentFleet == 0 && KcaBattle.isCombined; }

    public static String currentEnemyDeckName = "";

    public static int checkHeavyDamagedExist() {
        int status = HD_NONE;
        for (int i = 0; i < friendMaxHps.size(); i++) {
            if (!checkhdmgflag[i]) continue;
            if (friendNowHps.get(i).getAsInt() * 4 <= friendMaxHps.get(i).getAsInt()
                    && !escapelist.contains(new JsonPrimitive(i + 1))) {
                if (dameconflag[i]) {
                    status = Math.max(status, HD_DAMECON);
                } else {
                    status = Math.max(status, HD_DANGER);
                    if (status == HD_DANGER) {
                        return status;
                    }
                }
            }
        }
        return status;
    }

    public static int checkCombinedHeavyDamagedExist() {
        int status = HD_NONE;
        for (int i = 0; i < friendMaxHps.size(); i++) {
            if (!checkhdmgflag[i]) continue;
            if (friendNowHps.get(i).getAsInt() * 4 <= friendMaxHps.get(i).getAsInt()
                    && !escapelist.contains(new JsonPrimitive(i + 1))) {
                if (dameconflag[i]) {
                    status = Math.max(status, HD_DAMECON);
                } else {
                    status = Math.max(status, HD_DANGER);
                    if (status == HD_DANGER) {
                        return status;
                    }
                }
            }
        }
        for (int i = 1; i < friendCbMaxHps.size(); i++) {
            if (!checkhdmgcbflag[i]) continue;
            if (friendCbNowHps.get(i).getAsInt() * 4 <= friendCbMaxHps.get(i).getAsInt() &&
                    !escapecblist.contains(new JsonPrimitive(i + 1))) {
                if (dameconcbflag[i]) {
                    status = Math.max(status, HD_DAMECON);
                } else {
                    status = Math.max(status, HD_DANGER);
                    if (status == HD_DANGER) {
                        return status;
                    }
                }
            }
        }
        return status;
    }

    public static int cnv(JsonElement value) {
        Float f = value.getAsFloat();
        return f.intValue();
    }

    public static void reduce_value(boolean is_friend, JsonArray target, int idx, int amount, boolean cb_flag) {
        if (idx >= 0 && idx < target.size()) {
            int before_value = target.get(idx).getAsInt();
            int after_value = before_value - amount;
            if (is_friend && after_value <= 0) after_value = damecon_calculate(idx, after_value, cb_flag);
            target.set(idx, new JsonPrimitive(after_value));
        }
    }

    public static int damecon_calculate(int idx, int value, boolean cb_flag) {
        int max_hp = 1;
        JsonArray fleet_data;
        if (deckportdata != null) {
            JsonArray deck_data = deckportdata.getAsJsonArray("api_deck_data");
            if (cb_flag) fleet_data = deck_data.get(1).getAsJsonObject().getAsJsonArray("api_ship");
            else fleet_data = deck_data.get(0).getAsJsonObject().getAsJsonArray("api_ship");
            if (idx < fleet_data.size()) {
                int ship_id = fleet_data.get(idx).getAsInt();
                if (ship_id > 0) {
                    JsonObject shipData = getUserShipDataById(ship_id, "slot,slot_ex,maxhp");
                    JsonArray shipItem = shipData.getAsJsonArray("slot");
                    max_hp = shipData.get("maxhp").getAsInt();
                    for (int j = 0; j < shipItem.size(); j++) {
                        int item_id = shipItem.get(j).getAsInt();
                        if (item_id != -1) {
                            JsonObject itemData = getUserItemStatusById(item_id, "slotitem_id", "type");
                            if (itemData == null) return value;
                            int item = itemData.get("slotitem_id").getAsInt();
                            if (item == 42 || item == 43) { // 요원 / 여신
                                damecon_used.add(KcaUtils.format("%d_%d", cb_flag ? 1 : 0, idx));
                                return item == 43 ? max_hp : max_hp / 4;
                            }
                        }
                    }
                    int ex_item_id = shipData.get("slot_ex").getAsInt();
                    if (ex_item_id > 0) {
                        JsonObject itemData = getUserItemStatusById(ex_item_id, "slotitem_id", "type");
                        if (itemData == null) return value;
                        int item = itemData.get("slotitem_id").getAsInt();
                        if (item == 42 || item == 43) { // 요원 / 여신
                            damecon_used.add(KcaUtils.format("%d_%d", cb_flag ? 1 : 0, idx));
                            return item == 43 ? max_hp : max_hp / 4;
                        }
                    }
                }
            }
        }
        return value;
    }


    public static void cleanEscapeList() {
        escapedata = null;
        escapelist = new JsonArray();
        escapecblist = new JsonArray();
    }

    public static JsonObject getEscapeFlag() {
        JsonObject data = new JsonObject();
        data.add("escape", escapelist);
        data.add("escape_cb", escapecblist);
        return data;
    }

    public static boolean isKeyExist(JsonObject data, String key) {
        return (data.has(key) && !data.get(key).isJsonNull());
    }

    public static boolean isMainFleetInNight(JsonObject fleetdata) {
        JsonArray api_e_nowhps = fleetdata.getAsJsonArray("e_start");
        JsonArray api_e_afterhps = fleetdata.getAsJsonArray("e_after");

        JsonArray api_e_nowhps_combined = fleetdata.getAsJsonArray("e_start_cb");
        JsonArray api_e_afterhps_combined = fleetdata.getAsJsonArray("e_after_cb");

        List<Integer> enemySunkIdx = new ArrayList<>();
        List<Integer> enemyCbSunkIdx = new ArrayList<>();

        int enemyMainCount = 0;
        int enemyCbCount = 0;

        int enemyMainSunkCount = 0;
        int enemyCbSunkCount = 0;
        int enemyCbGoodHealth = 0;

        for (int i = 0; i < api_e_nowhps.size(); i++) {
            if (api_e_afterhps.get(i).getAsInt() <= 0) {
                enemyMainSunkCount += 1;
                enemySunkIdx.add(i);
            }
        }

        for (int i = 0; i < api_e_nowhps_combined.size(); i++) {
            if (api_e_afterhps_combined.get(i).getAsInt() <= 0) {
                enemyCbSunkCount += 1;
                enemyCbSunkIdx.add(i);
            } else if (api_e_afterhps_combined.get(i).getAsInt() * 2 > api_e_nowhps_combined.get(i).getAsInt()) {
                enemyCbGoodHealth += 1;
            }
        }

        Log.e("KCA", "nb-enemyCbCount " + String.valueOf(enemyCbCount));
        Log.e("KCA", "nb-enemyCbSunkCount " + String.valueOf(enemyCbSunkCount));
        Log.e("KCA", "nb-enemyCbGoodHealth " + String.valueOf(enemyCbGoodHealth));

        if (!enemyCbSunkIdx.contains(0) && enemyCbGoodHealth >= 2) {
            return false;
        } else if (enemyCbGoodHealth >= 3) {
            return false;
        } else return !(enemyMainSunkCount == enemyMainCount && enemyCbSunkCount != enemyCbCount);

    }

    public static JsonObject calculateRank(JsonObject fleetdata) {
        JsonObject result = new JsonObject();

        JsonArray api_f_nowhps = fleetdata.getAsJsonArray("f_start");
        JsonArray api_f_afterhps = fleetdata.getAsJsonArray("f_after");

        JsonArray api_e_nowhps = fleetdata.getAsJsonArray("e_start");
        JsonArray api_e_afterhps = fleetdata.getAsJsonArray("e_after");

        JsonArray api_f_nowhps_combined = fleetdata.getAsJsonArray("f_start_cb");
        JsonArray api_f_afterhps_combined = fleetdata.getAsJsonArray("f_after_cb");

        JsonArray api_e_nowhps_combined = fleetdata.getAsJsonArray("e_start_cb");
        JsonArray api_e_afterhps_combined = fleetdata.getAsJsonArray("e_after_cb");

        List<Integer> enemySunkIdx = new ArrayList<>();

        int friendCount = 0;
        int enemyCount = 0;
        int friendSunkCount = 0;
        int enemySunkCount = 0;
        int friendNowSum = 0;
        int enemyNowSum = 0;
        int friendAfterSum = 0;
        int enemyAfterSum = 0;

        for (int i = 0; i < api_f_nowhps.size(); i++) {
            if (escapelist.contains(new JsonPrimitive(i + 1))) continue;
            friendCount += 1;
            if (api_f_afterhps.get(i).getAsInt() <= 0) {
                friendSunkCount += 1;
            }
            friendNowSum += api_f_nowhps.get(i).getAsInt();
            friendAfterSum += Math.max(0, api_f_afterhps.get(i).getAsInt());
        }

        for (int i = 0; i < api_f_nowhps_combined.size(); i++) {
            if (escapecblist.contains(new JsonPrimitive(i + 1))) continue;
            friendCount += 1;
            if (api_f_afterhps_combined.get(i).getAsInt() <= 0) {
                friendSunkCount += 1;
            }
            friendNowSum += api_f_nowhps_combined.get(i).getAsInt();
            friendAfterSum += Math.max(0, api_f_afterhps_combined.get(i).getAsInt());
        }

        for (int i = 0; i < api_e_nowhps.size(); i++) {
            enemyCount += 1;
            if (api_e_afterhps.get(i).getAsInt() <= 0) {
                enemySunkIdx.add(i);
                enemySunkCount += 1;
            }
            enemyNowSum += api_e_nowhps.get(i).getAsInt();
            enemyAfterSum += Math.max(0, api_e_afterhps.get(i).getAsInt());
        }

        for (int i = 0; i < api_e_nowhps_combined.size(); i++) {
            enemyCount += 1;
            if (api_e_afterhps_combined.get(i).getAsInt() <= 0) {
                enemySunkCount += 1;
            }
            enemyNowSum += api_e_nowhps_combined.get(i).getAsInt();
            enemyAfterSum += Math.max(0, api_e_afterhps_combined.get(i).getAsInt());
        }

        int friendDamageRate = (friendNowSum - friendAfterSum) * 100 / friendNowSum;
        int enemyDamageRate = (enemyNowSum - enemyAfterSum) * 100 / enemyNowSum;

        result.addProperty("fnowhpsum", friendNowSum);
        result.addProperty("fafterhpsum", friendAfterSum);
        result.addProperty("enowhpsum", enemyNowSum);
        result.addProperty("eafterhpsum", enemyAfterSum);
        result.addProperty("fdmgrate", friendDamageRate);
        result.addProperty("edmgrate", enemyDamageRate);

        if (friendSunkCount == 0) {
            if (enemySunkCount == enemyCount) {
                if (friendAfterSum >= friendNowSum) result.addProperty("rank", JUDGE_SS);
                else result.addProperty("rank", JUDGE_S);
            } else if (enemyCount > 1 && (enemySunkCount >= (int) Math.floor(0.7 * enemyCount))) {
                result.addProperty("rank", JUDGE_A);
            }
        }
        if (!result.has("rank") && enemySunkIdx.contains(0) && friendSunkCount < enemySunkCount) {
            result.addProperty("rank", JUDGE_B);
        }
        if (!result.has("rank") && (friendCount == 1) && (api_f_afterhps.get(0).getAsInt() * 4 <= api_f_nowhps.get(0).getAsInt())) {
            result.addProperty("rank", JUDGE_D);
        }
        if (!result.has("rank") && enemyDamageRate * 2 > friendDamageRate * 5) {
            result.addProperty("rank", JUDGE_B);
        }
        if (!result.has("rank") && enemyDamageRate * 10 > friendDamageRate * 9) {
            result.addProperty("rank", JUDGE_C);
        }
        if (!result.has("rank") && friendSunkCount > 0 && (friendCount - friendSunkCount) == 1) {
            result.addProperty("rank", JUDGE_E);
        }
        if (!result.has("rank")) {
            result.addProperty("rank", JUDGE_D);
        }
        Log.e("KCA", "BattleResult: " + result.toString());
        return result;
    }

    public static JsonObject calculateLdaRank(JsonObject fleetdata) {
        JsonObject result = new JsonObject();

        JsonArray api_f_nowhps = fleetdata.getAsJsonArray("f_start");
        JsonArray api_f_afterhps = fleetdata.getAsJsonArray("f_after");

        JsonArray api_f_nowhps_combined = fleetdata.getAsJsonArray("f_start_cb");
        JsonArray api_f_afterhps_combined = fleetdata.getAsJsonArray("f_after_cb");


        boolean[] friendSunk = new boolean[12];
        int friendCount = 12;
        int friendSunkCount = 0;
        int friendNowSum = 0;
        int friendAfterSum = 0;

        for (int i = 0; i < api_f_nowhps.size(); i++) {
            friendCount += 1;
            if (api_f_afterhps.get(i).getAsInt() <= 0) {
                friendSunkCount += 1;
            }
            friendNowSum += friendNowHps.get(i).getAsInt();
            friendAfterSum += Math.max(0, friendAfterHps.get(i).getAsInt());
        }

        for (int i = 0; i < api_f_nowhps_combined.size(); i++) {
            friendCount += 1;
            if (api_f_afterhps_combined.get(i).getAsInt() <= 0) {
                friendSunkCount += 1;
            }
            friendNowSum += friendCbNowHps.get(i).getAsInt();
            friendAfterSum += Math.max(0, friendCbAfterHps.get(i).getAsInt());
        }

        int friendDamageRate = (friendNowSum - friendAfterSum) * 100 / friendNowSum;

        result.addProperty("fnowhpsum", friendNowSum);
        result.addProperty("fafterhpsum", friendAfterSum);
        result.addProperty("fdmgrate", friendDamageRate);

        if (friendAfterSum >= friendNowSum) result.addProperty("rank", JUDGE_SS);
        else if (friendDamageRate < 10) result.addProperty("rank", JUDGE_A);
        else if (friendDamageRate < 20) result.addProperty("rank", JUDGE_B);
        else if (friendDamageRate < 50) result.addProperty("rank", JUDGE_C);
        else if (friendDamageRate < 80) result.addProperty("rank", JUDGE_D);
        else result.addProperty("rank", JUDGE_E);

        return result;
    }

    public static void setDeckPortData(JsonObject api_data) {
        deckportdata = api_data;
        JsonArray apiDeckData = deckportdata.getAsJsonArray("api_deck_data");
        JsonArray apiShipData = deckportdata.getAsJsonArray("api_ship_data");

        friendMaxHps = new JsonArray();
        friendNowHps = new JsonArray();
        friendCbMaxHps = new JsonArray();
        friendCbNowHps = new JsonArray();

        int firstDeckSize = 0;
        JsonArray firstDeck = apiDeckData.get(0).getAsJsonObject().getAsJsonArray("api_ship");
        for (JsonElement element : firstDeck) {
            if (element.getAsInt() != -1) {
                firstDeckSize += 1;
            } else {
                break;
            }
        }
        JsonObject deckMaxHpData = new JsonObject();
        JsonObject deckNowHpData = new JsonObject();

        for (int i = 0; i < apiShipData.size(); i++) {
            JsonObject shipData = apiShipData.get(i).getAsJsonObject();
            deckMaxHpData.addProperty(shipData.get("api_id").getAsString(), shipData.get("api_maxhp").getAsInt());
            deckNowHpData.addProperty(shipData.get("api_id").getAsString(), shipData.get("api_nowhp").getAsInt());
        }

        for (int i = 0; i < firstDeckSize; i++) {
            friendMaxHps.add(deckMaxHpData.get(firstDeck.get(i).getAsString()).getAsInt());
            friendNowHps.add(deckNowHpData.get(firstDeck.get(i).getAsString()).getAsInt());
        }
        if (apiDeckData.size() == 2) {
            int secondDeckSize = 0;
            JsonArray secondDeck = apiDeckData.get(1).getAsJsonObject().getAsJsonArray("api_ship");
            for (JsonElement element : secondDeck) {
                if (element.getAsInt() != -1) {
                    secondDeckSize += 1;
                } else {
                    break;
                }
            }

            for (int i = 0; i < secondDeckSize; i++) {
                friendCbMaxHps.add(deckMaxHpData.get(secondDeck.get(i).getAsString()).getAsInt());
                friendCbNowHps.add(deckNowHpData.get(secondDeck.get(i).getAsString()).getAsInt());
            }
        }
    }

    public static void calculateAirBattle(JsonObject data) {
        if (isKeyExist(data, "api_stage3")) {
            JsonObject api_stage3 = data.getAsJsonObject("api_stage3");
            if (isKeyExist(api_stage3, "api_fdam")) {
                JsonArray api_fdam = api_stage3.getAsJsonArray("api_fdam");
                for (int i = 0; i < api_fdam.size(); i++) {
                    if (isCombinedFleetInSortie() && i >= 6) {
                        reduce_value(true, friendCbAfterHps, i - 6, cnv(api_fdam.get(i)), true);
                    } else {
                        reduce_value(true, friendAfterHps, i, cnv(api_fdam.get(i)), false);
                    }
                }
            }
            if (isKeyExist(api_stage3, "api_edam")) {
                JsonArray api_edam = api_stage3.getAsJsonArray("api_edam");
                for (int i = 0; i < api_edam.size(); i++) {
                    if (i < 6) reduce_value(false, enemyAfterHps, i, cnv(api_edam.get(i)), false);
                    else if (ship_ke_combined != null)
                        reduce_value(false, enemyCbAfterHps, i - 6, cnv(api_edam.get(i)), true);
                }
            }
        }
        if (isKeyExist(data, "api_stage3_combined")) {
            JsonObject api_stage3_combined = data.getAsJsonObject("api_stage3_combined");
            if (isKeyExist(api_stage3_combined, "api_fdam")) {
                JsonArray api_fdam = api_stage3_combined.getAsJsonArray("api_fdam");
                for (int i = 0; i < api_fdam.size(); i++) {
                    reduce_value(true, friendCbAfterHps, i, cnv(api_fdam.get(i)), true);
                }
            }
            if (isKeyExist(api_stage3_combined, "api_edam")) {
                JsonArray api_edam = api_stage3_combined.getAsJsonArray("api_edam");
                for (int i = 0; i < api_edam.size(); i++) {
                    reduce_value(false, enemyCbAfterHps, i, cnv(api_edam.get(i)), true);
                }
            }
        }
    }

    public static void calculateSupportDamage(JsonObject support_info) {
        JsonArray damage = new JsonArray();
        if (isKeyExist(support_info, "api_support_airatack")) {
            // 항공지원
            JsonObject support_airattack = support_info.getAsJsonObject("api_support_airatack");
            damage = support_airattack.getAsJsonObject("api_stage3").getAsJsonArray("api_edam");
        } else if (isKeyExist(support_info, "api_support_hourai")) {
            JsonObject support_hourai = support_info.getAsJsonObject("api_support_hourai");
            damage = support_hourai.getAsJsonArray("api_damage");
        }
        for (int d = 0; d < damage.size(); d++) {
            int damage_value = cnv(damage.get(d));
            if (damage_value > 0) {
                if (d < 6) reduce_value(false, enemyAfterHps, d, damage_value, false);
                else reduce_value(false, enemyCbAfterHps, d - 6, damage_value, true);
            }
        }
    }

    public static void calculateRaigekiDamage(JsonObject damage_info) {
        JsonArray damage_info_fdam = damage_info.getAsJsonArray("api_fdam");
        JsonArray damage_info_edam = damage_info.getAsJsonArray("api_edam");
        for (int i = 0; i < damage_info_fdam.size(); i++) {
            if (isCombinedFleetInSortie() && i >= 6) {
                reduce_value(true, friendCbAfterHps, i - 6, cnv(damage_info_fdam.get(i)), true);
            } else {
                reduce_value(true, friendAfterHps, i, cnv(damage_info_fdam.get(i)), false);
            }
        }
        for (int i = 0; i < damage_info_edam.size(); i++) {
            int value = cnv(damage_info_edam.get(i));
            if (value > 0) {
                if (i < 6) reduce_value(false, enemyAfterHps, i, value, false);
                else reduce_value(false, enemyCbAfterHps, i - 6, value, true);
            }
        }
    }

    public static void calculateHougekiDamage(JsonObject api_data) {
        JsonArray at_eflag = api_data.getAsJsonArray("api_at_eflag");
        JsonArray df_list = api_data.getAsJsonArray("api_df_list");
        JsonArray df_damage = api_data.getAsJsonArray("api_damage");
        for (int i = 0; i < df_list.size(); i++) {
            int eflag = at_eflag.get(i).getAsInt();
            JsonArray target = df_list.get(i).getAsJsonArray();
            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
            for (int j = 0; j < target.size(); j++) {
                int target_val = cnv(target.get(j));
                int dmg_val = cnv(target_dmg.get(j));
                if (eflag == 0) reduce_value(false, enemyAfterHps, target_val, dmg_val, false);
                else reduce_value(true, friendAfterHps, target_val, dmg_val, false);
            }
        }
    }

    public static void calculateCombinedHougekiDamage(JsonObject api_data, int combined_type, int phase) {
        JsonArray at_eflag = api_data.getAsJsonArray("api_at_eflag");
        JsonArray df_list = api_data.getAsJsonArray("api_df_list");
        JsonArray df_damage = api_data.getAsJsonArray("api_damage");
        for (int i = 0; i < df_list.size(); i++) {
            int eflag = at_eflag.get(i).getAsInt();
            JsonArray target = df_list.get(i).getAsJsonArray();
            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();

            boolean friend_cb_target_flag;
            for (int j = 0; j < target.size(); j++) {
                int target_val = cnv(target.get(j));
                int dmg_val = cnv(target_dmg.get(j));
                friend_cb_target_flag = false;
                if (combined_type == COMBINED_A && phase == 1) friend_cb_target_flag = true;
                if (combined_type == COMBINED_W && phase == 3) friend_cb_target_flag = true;
                if (combined_type == 0 && target_val >= 6) friend_cb_target_flag = true;

                if (eflag == 0) reduce_value(false, enemyAfterHps, target_val, dmg_val, false);
                else if (friend_cb_target_flag) reduce_value(true, friendCbAfterHps, target_val - 6, dmg_val, true);
                else reduce_value(true, friendAfterHps, target_val, dmg_val, false);
            }
        }
    }

    public static void calculateFriendSupportFleetHougekiDamage(JsonObject api_data) {
        JsonArray at_eflag = api_data.getAsJsonArray("api_at_eflag");
        JsonArray df_list = api_data.getAsJsonArray("api_df_list");
        JsonArray df_damage = api_data.getAsJsonArray("api_damage");
        for (int i = 0; i < df_list.size(); i++) {
            int eflag = at_eflag.get(i).getAsInt();
            JsonArray target = df_list.get(i).getAsJsonArray();
            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
            for (int j = 0; j < target.size(); j++) {
                int target_val = cnv(target.get(j));
                int dmg_val = cnv(target_dmg.get(j));
                boolean target_idx_cb = target.get(0).getAsInt() >= 6;
                boolean target_idx_valid = target.get(0).getAsInt() != -1;
                if (eflag == 0) {
                    if (target_idx_cb) reduce_value(false, enemyCbAfterHps, target_val - 6, dmg_val, true);
                    else if (target_idx_valid) reduce_value(false, enemyAfterHps, target_val, dmg_val, false);
                } else { // Do not count damage for friend fleet
                    //if (isCombinedFleetInSortie() && target_idx_cb) reduce_value(true, friendCbAfterHps, target, -6, target_dmg, true);
                    //else if (target_idx_valid) reduce_value(true, friendAfterHps, target, target_dmg, false);
                }
            }
        }
    }

    public static void calculateEnemyCombinedHougekiDamage(JsonObject api_data, boolean is_combined, int phase) {
        JsonArray at_eflag = api_data.getAsJsonArray("api_at_eflag");
        JsonArray df_list = api_data.getAsJsonArray("api_df_list");
        JsonArray df_damage = api_data.getAsJsonArray("api_damage");
        for (int i = 0; i < df_list.size(); i++) {
            int eflag = at_eflag.get(i).getAsInt();
            JsonArray target = df_list.get(i).getAsJsonArray();
            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();

            for (int j = 0; j < target.size(); j++) {
                int target_val = cnv(target.get(j));
                int dmg_val = cnv(target_dmg.get(j));
                switch (phase) {
                    case PHASE_1:
                        if (eflag == 0) reduce_value(false, enemyAfterHps, target_val, dmg_val, false);
                        else reduce_value(true, friendAfterHps, target_val, dmg_val, false);
                        break;
                    case PHASE_2:
                        if (eflag == 0)
                            reduce_value(false, enemyCbAfterHps, target_val - 6, dmg_val, true);
                        else if (is_combined)
                            reduce_value(true, friendCbAfterHps, target_val - 6, dmg_val, true);
                        else reduce_value(true, friendAfterHps, target_val, dmg_val, false);
                        break;
                    case PHASE_3:
                        boolean target_idx_cb = target_val >= 6;
                        boolean target_idx_valid = target_val != -1;
                        if (eflag == 0) {
                            if (target_idx_cb)
                                reduce_value(false, enemyCbAfterHps, target_val - 6, dmg_val, true);
                            else if (target_idx_valid)
                                reduce_value(false, enemyAfterHps, target_val, dmg_val, false);
                        } else {
                            if (isCombinedFleetInSortie() && target_idx_cb)
                                reduce_value(true, friendCbAfterHps, target_val - 6, dmg_val, true);
                            else if (target_idx_valid)
                                reduce_value(true, friendAfterHps, target_val, dmg_val, false);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }


    public static void calculateEnemyCombinedNightDamage(JsonObject api_data, int[] activedeck) {
        JsonArray at_eflag = api_data.getAsJsonArray("api_at_eflag");
        JsonArray df_list = api_data.getAsJsonArray("api_df_list");
        JsonArray df_damage = api_data.getAsJsonArray("api_damage");
        for (int i = 0; i < df_list.size(); i++) {
            int eflag = at_eflag.get(i).getAsInt();
            JsonArray target = df_list.get(i).getAsJsonArray();
            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
            for (int j = 0; j < target.size(); j++) {
                int target_val = cnv(target.get(j));
                int dmg_val = cnv(target_dmg.get(j));
                if (eflag == 0) {
                    if (activedeck[1] == 1) reduce_value(false, enemyAfterHps, target_val, dmg_val, false);
                    else reduce_value(false, enemyCbAfterHps, target_val - 6, dmg_val, true);
                } else {
                    if (!isCombinedFleetInSortie()) reduce_value(true, friendAfterHps, target_val, dmg_val, false);
                    else reduce_value(true, friendCbAfterHps, target_val - 6, dmg_val, true);
                }
            }
         }
    }

    public static void processData(KcaDBHelper helper, String url, JsonObject api_data) {
        //
        try {
            // Log.e("KCA", "processData: "+url );
            if (url.equals(API_REQ_MAP_START)) {
                Bundle bundle;
                Message sMsg;

                ship_ke = null;
                ship_ke_combined = null;
                KcaApiData.resetShipCountInBattle();
                KcaApiData.resetItemCountInBattle();
                cleanEscapeList();
                damecon_used = new JsonArray();

                currentMapArea = api_data.get("api_maparea_id").getAsInt();
                currentMapNo = api_data.get("api_mapinfo_no").getAsInt();
                currentNode = api_data.get("api_no").getAsInt();
                isBossReached = false;
                isEndReached = false;
                currentEnemyDeckName = "";
                currentEnemyFormation = -1;

                int api_event_kind = api_data.get("api_event_kind").getAsInt();
                int api_event_id = api_data.get("api_event_id").getAsInt();
                String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);

                if (api_data.has("api_eventmap")) {
                    currentEventMapRank = KcaApiData.getEventMapDifficulty(currentMapNo);
                } else {
                    currentEventMapRank = 0;
                }

                if (startHeavyDamageExist != HD_NONE) {
                    JsonObject battleResultInfo = new JsonObject();
                    battleResultInfo.addProperty("data", startHeavyDamageExist);
                    bundle = new Bundle();
                    bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                    bundle.putString("data", gson.toJson(battleResultInfo));
                    sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }

                JsonObject nodeInfo = api_data;
                nodeInfo.addProperty("api_url", url);
                nodeInfo.add("api_dc_used", damecon_used);
                nodeInfo.add("api_deck_port", deckportdata);
                nodeInfo.addProperty("api_heavy_damaged", startHeavyDamageExist);
                setCurrentApiData(nodeInfo);
                helper.putValue(DB_KEY_BATTLENODE, nodeInfo.toString());

                JsonObject qtrackData = new JsonObject();
                qtrackData.addProperty("world", currentMapArea);
                qtrackData.addProperty("map", currentMapNo);
                qtrackData.addProperty("node", currentNode);
                qtrackData.addProperty("isboss", isBossReached);
                qtrackData.addProperty("isstart", true);
                qtrackData.add("deck_port", deckportdata);
                helper.putValue(DB_KEY_QTRACKINFO, qtrackData.toString());

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_NODE);
                bundle.putString("data", nodeInfo.toString());
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_MAP_NEXT)) {
                Bundle bundle;
                Message sMsg;

                ship_ke = null;
                ship_ke_combined = null;

                int checkcbresult = checkCombinedHeavyDamagedExist();
                Log.e("KCA", "hd: " + String.valueOf(checkcbresult));
                damecon_used = new JsonArray();

                currentMapArea = api_data.get("api_maparea_id").getAsInt();
                currentMapNo = api_data.get("api_mapinfo_no").getAsInt();
                currentNode = api_data.get("api_no").getAsInt();
                int api_event_kind = api_data.get("api_event_kind").getAsInt();
                int api_event_id = api_data.get("api_event_id").getAsInt();
                if (api_event_id == API_NODE_EVENT_ID_BOSS) { // Reach Booss (event id = 5)
                    isBossReached = true;
                }
                isEndReached = (api_data.get("api_next").getAsInt() == 0);
                String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);
                currentEnemyFormation = -1;

                JsonObject battleResultInfo = new JsonObject();
                battleResultInfo.addProperty("data", checkcbresult);
                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                bundle.putString("data", gson.toJson(battleResultInfo));
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);

                JsonObject nodeInfo = api_data;
                nodeInfo.addProperty("api_url", url);
                nodeInfo.add("api_dc_used", damecon_used);
                nodeInfo.add("api_deck_port", deckportdata);
                nodeInfo.add("api_escape", escapelist);
                nodeInfo.add("api_escape_combined", escapecblist);
                nodeInfo.addProperty("api_heavy_damaged", checkcbresult);
                setCurrentApiData(nodeInfo);
                helper.putValue(DB_KEY_BATTLENODE, nodeInfo.toString());

                JsonObject qtrackData = new JsonObject();
                qtrackData.addProperty("world", currentMapArea);
                qtrackData.addProperty("map", currentMapNo);
                qtrackData.addProperty("node", currentNode);
                qtrackData.addProperty("isboss", isBossReached);
                qtrackData.addProperty("isstart", false);
                qtrackData.add("deck_port", deckportdata);
                helper.putValue(DB_KEY_QTRACKINFO, qtrackData.toString());

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_NODE);
                bundle.putString("data", nodeInfo.toString());
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_SORTIE_BATTLE) || url.equals(API_REQ_PRACTICE_BATTLE)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 분식항공전 Stage 3
                if (isKeyExist(api_data, "api_injection_kouku")) { // Check Null for old data
                    calculateAirBattle(api_data.getAsJsonObject("api_injection_kouku"));
                }

                // 기항대 Stage 3
                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                // 지원함대
                if (isKeyExist(api_data, "api_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_support_info");
                    calculateSupportDamage(support_info);
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    if (isKeyExist(opening_taisen, "api_df_list")) {
                        calculateHougekiDamage(opening_taisen);
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    calculateRaigekiDamage(openingattack);
                }

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        calculateHougekiDamage(hougeki);
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    calculateRaigekiDamage(raigeki);
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_deck_port", deckportdata);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);

                if (url.equals(API_REQ_PRACTICE_BATTLE)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT)
                    || url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT_SP)
                    || url.equals(API_REQ_PRACTICE_MIDNIGHT_BATTLE)) {

                if (url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT_SP)) {
                    ship_ke = api_data.getAsJsonArray("api_ship_ke");
                    currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                    String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                    String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                    friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                    enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                }

                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                // 야간지원함대
                if (isKeyExist(api_data, "api_n_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_n_support_info");
                    calculateSupportDamage(support_info);
                }

                if (isKeyExist(api_data, "api_friendly_battle")) {
                    JsonObject friend_battle = api_data.getAsJsonObject("api_friendly_battle");
                    JsonObject friend_hougeki = friend_battle.getAsJsonObject("api_hougeki");
                    if (isKeyExist(friend_hougeki, "api_df_list")) {
                        calculateFriendSupportFleetHougekiDamage(friend_hougeki);
                    }
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    if (isKeyExist(hougeki, "api_df_list")) {
                        calculateHougekiDamage(hougeki);
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_deck_port", deckportdata);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                if (url.equals(API_REQ_PRACTICE_MIDNIGHT_BATTLE)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            // 아웃레인지, 공습
            if (url.equals(API_REQ_SORTIE_AIRBATTLE) || url.equals(API_REQ_SORTIE_LDAIRBATTLE)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 기지항공대 Stage 3
                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                if (url.equals(API_REQ_SORTIE_AIRBATTLE)) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku2"));
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            // 레이더사격
            if (url.equals(API_REQ_SORTIE_LDSHOOTING)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 기지항공대 Stage 3
                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        calculateHougekiDamage(hougeki);
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_SORTIE_BATTLE_RESULT) || url.equals(API_REQ_PRACTICE_BATTLE_RESULT)) {
                Bundle bundle;
                Message sMsg;
                friendNowHps = KcaUtils.parseJson(friendAfterHps.toString()).getAsJsonArray();
                int checkresult = HD_NONE;

                JsonObject qtrackData = new JsonObject();
                qtrackData.addProperty("result", api_data.get("api_win_rank").getAsString());
                if (url.equals(API_REQ_SORTIE_BATTLE_RESULT)) {
                    if (!isEndReached) {
                        checkresult = checkHeavyDamagedExist();
                        Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkresult));
                        Log.e("KCA", String.valueOf(KcaApiData.checkUserItemMax()) + String.valueOf(KcaApiData.checkUserShipMax()));
                        Log.e("KCA", String.valueOf(KcaApiData.checkUserPortEnough()));

                        JsonObject heavyDamagedInfo = new JsonObject();
                        if (checkresult != HD_NONE) {
                            heavyDamagedInfo.addProperty("data", checkresult);
                            bundle = new Bundle();
                            bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                            bundle.putString("data", heavyDamagedInfo.toString());
                            sMsg = sHandler.obtainMessage();
                            sMsg.setData(bundle);
                            sHandler.sendMessage(sMsg);
                        }

                        if (isKeyExist(api_data, "api_escape_flag")) {
                            int api_escape_flag = api_data.get("api_escape_flag").getAsInt();
                            Log.e("KCA", "api_escape_flag: " + String.valueOf(api_escape_flag));
                            if (api_escape_flag == 1) {
                                escapedata = api_data.getAsJsonObject("api_escape");
                                Log.e("KCA", "api_escape: " + escapedata.toString());
                            } else {
                                escapedata = null;
                            }
                        }
                    }

                    JsonObject dropInfo = new JsonObject();
                    dropInfo.addProperty("world", currentMapArea);
                    dropInfo.addProperty("map", currentMapNo);
                    dropInfo.addProperty("node", currentNode);
                    dropInfo.addProperty("isboss", isBossReached);
                    dropInfo.addProperty("quest_name", api_data.get("api_quest_name").getAsString());
                    dropInfo.addProperty("enemy_name", api_data.getAsJsonObject("api_enemy_info").get("api_deck_name").getAsString());
                    dropInfo.addProperty("rank", api_data.get("api_win_rank").getAsString());
                    dropInfo.addProperty("maprank", currentEventMapRank);
                    if (api_data.has("api_get_ship")) {
                        int api_ship_id = api_data.getAsJsonObject("api_get_ship").get("api_ship_id").getAsInt();
                        dropInfo.addProperty("result", api_ship_id);
                        dropInfo.addProperty("inventory", KcaApiData.countUserShipById(api_ship_id));
                    } else {
                        dropInfo.addProperty("result", 0);
                        dropInfo.addProperty("inventory", 0);
                    }
                    JsonObject enemyInfo = new JsonObject();
                    enemyInfo.addProperty("formation", currentEnemyFormation);
                    if (ship_ke != null) enemyInfo.add("ships", ship_ke);
                    dropInfo.add("enemy", enemyInfo);

                    bundle = new Bundle();
                    bundle.putString("url", KCA_API_NOTI_BATTLE_DROPINFO);
                    bundle.putString("data", gson.toJson(dropInfo));
                    sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);

                    qtrackData.addProperty("world", currentMapArea);
                    qtrackData.addProperty("map", currentMapNo);
                    qtrackData.addProperty("node", currentNode);
                    qtrackData.addProperty("isboss", isBossReached);
                    qtrackData.add("ship_ke", ship_ke);
                    qtrackData.add("deck_port", deckportdata);
                    qtrackData.add("afterhps_f", friendAfterHps);
                    qtrackData.add("afterhps_e", enemyAfterHps);
                    if (ship_ke_combined != null) {
                        qtrackData.addProperty("combined_flag", true);
                        qtrackData.add("ship_ke_combined", ship_ke_combined);
                        qtrackData.add("aftercbhps_f", friendCbAfterHps);
                        qtrackData.add("aftercbhps_e", enemyCbAfterHps);
                    } else {
                        qtrackData.addProperty("combined_flag", false);
                    }
                }
                helper.putValue(DB_KEY_QTRACKINFO, qtrackData.toString());
                helper.updateExpScore(api_data.get("api_get_exp").getAsInt());
                // ship_ke, afterhp, battle_result

                if (api_data.has("api_get_useitem")) {
                    int useitem_id = api_data.getAsJsonObject("api_get_useitem").get("api_useitem_id").getAsInt();
                    KcaApiData.addUseitemCount(useitem_id);
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                if (url.equals(API_REQ_PRACTICE_BATTLE_RESULT)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }
                battleResultInfo.addProperty("api_heavy_damaged", checkresult);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_SORTIE_GOBACKPORT)) {
                if (escapedata == null) {
                    Log.e("KCA", "escapedata is null");
                } else {
                    JsonArray api_escape_idx = escapedata.getAsJsonArray("api_escape_idx");
                    int api_escape_target = api_escape_idx.get(0).getAsInt(); // only first

                    if (!escapelist.contains(new JsonPrimitive(api_escape_target))) {
                        escapelist.add(api_escape_target);
                    }
                    Log.e("KCA", KcaUtils.format("Escape: %d", api_escape_target));
                }

                JsonObject battleResultInfo = helper.getJsonObjectValue(DB_KEY_BATTLEINFO);
                battleResultInfo.add("api_escape", escapelist);
                setCurrentApiData(battleResultInfo);

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE) || url.equals(API_REQ_COMBINED_BATTLE_WATER)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                int combined_type = 0;
                if (url.equals(API_REQ_COMBINED_BATTLE)) combined_type = COMBINED_A;
                else if (url.equals(API_REQ_COMBINED_BATTLE_WATER)) combined_type = COMBINED_W;
                //else if(url.equals(API_REQ_COMBINED_BATTLE	))	combined_type = COMBINED_D;

                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                String friendCbMaxHpsData = api_data.getAsJsonArray("api_f_maxhps_combined").toString();
                String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();

                friendCbMaxHps = KcaUtils.parseJson(friendCbMaxHpsData).getAsJsonArray();
                friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 분식항공전 Stage 3
                if (isKeyExist(api_data, "api_injection_kouku")) { // Check Null for old data
                    calculateAirBattle(api_data.getAsJsonObject("api_injection_kouku"));
                }

                // 기지항공대 Stage 3
                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                // 지원함대
                if (isKeyExist(api_data, "api_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_support_info");
                    calculateSupportDamage(support_info);
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    if (isKeyExist(opening_taisen, "api_df_list")) {
                        calculateCombinedHougekiDamage(opening_taisen, 0, PHASE_3);
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    calculateRaigekiDamage(openingattack);
                }

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        if (isKeyExist(hougeki, "api_df_list")) {
                            calculateCombinedHougekiDamage(hougeki, combined_type, n);
                        }
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    calculateRaigekiDamage(raigeki);
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined", enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);

            }

            if (url.equals(API_REQ_COMBINED_BATTLE_EC) || url.equals(API_REQ_COMBINED_BATTLE_EACH) || url.equals(API_REQ_COMBINED_BATTLE_EACH_WATER)) {

                int combined_type = 0;
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");

                if (url.equals(API_REQ_COMBINED_BATTLE_EACH)) combined_type = COMBINED_A;
                else if (url.equals(API_REQ_COMBINED_BATTLE_EACH_WATER)) combined_type = COMBINED_W;

                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                if (isKeyExist(api_data, "api_f_maxhps_combined")) {
                    String friendCbMaxHpsData = api_data.getAsJsonArray("api_f_maxhps_combined").toString();
                    String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();

                    friendCbMaxHps = KcaUtils.parseJson(friendCbMaxHpsData).getAsJsonArray();
                    friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                    friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                }

                String enemyCbMaxHpsData = api_data.getAsJsonArray("api_e_maxhps_combined").toString();
                String enemyCbNowHpsData = api_data.getAsJsonArray("api_e_nowhps_combined").toString();

                enemyCbMaxHps = KcaUtils.parseJson(enemyCbMaxHpsData).getAsJsonArray();
                enemyCbNowHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();
                enemyCbAfterHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 분식항공전 Stage 3
                if (isKeyExist(api_data, "api_injection_kouku")) { // Check Null for old data
                    calculateAirBattle(api_data.getAsJsonObject("api_injection_kouku"));
                }

                // 기지항공대 Stage 3
                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                // 지원함대
                if (isKeyExist(api_data, "api_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_support_info");
                    calculateSupportDamage(support_info);
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    if (isKeyExist(opening_taisen, "api_df_list")) {
                        calculateEnemyCombinedHougekiDamage(opening_taisen, combined_type > 0, PHASE_3);
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    calculateRaigekiDamage(openingattack);
                }

                // 포격전
                int[] phase_list = {0, 0, 0, 0};
                if (combined_type == COMBINED_A) {
                    phase_list[1] = PHASE_1;
                    phase_list[2] = PHASE_2;
                    phase_list[3] = PHASE_3;
                } else if (combined_type == COMBINED_W) {
                    phase_list[1] = PHASE_1;
                    phase_list[2] = PHASE_3;
                    phase_list[3] = PHASE_2;
                } else {
                    phase_list[1] = PHASE_2;
                    phase_list[2] = PHASE_1;
                    phase_list[3] = PHASE_3;
                }

                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(KcaUtils.format("api_hougeki%d", n));
                        if (isKeyExist(hougeki, "api_df_list")) {
                            calculateEnemyCombinedHougekiDamage(hougeki, combined_type > 0, phase_list[n]);
                        }
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    calculateRaigekiDamage(raigeki);
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined", enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            // 아웃레인지, 공습
            if (url.equals(API_REQ_COMBINED_AIRBATTLE) || url.equals(API_REQ_COMBINED_LDAIRBATTLE)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                String friendCbMaxHpsData = api_data.getAsJsonArray("api_f_maxhps_combined").toString();
                String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();

                friendCbMaxHps = KcaUtils.parseJson(friendCbMaxHpsData).getAsJsonArray();
                friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 기지항공대 Stage 3
                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                // 제1항공전 Stage 3
                calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                if (url.equals(API_REQ_COMBINED_AIRBATTLE)) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku2"));
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined", enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT) || url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_SP)) {
                if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_SP)) {
                    ship_ke = api_data.getAsJsonArray("api_ship_ke");

                    String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                    String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                    String friendCbMaxHpsData = api_data.getAsJsonArray("api_f_maxhps_combined").toString();

                    friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                    enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                    friendCbMaxHps = KcaUtils.parseJson(friendCbMaxHpsData).getAsJsonArray();
                }

                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();

                friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();

                // 야간지원함대
                if (isKeyExist(api_data, "api_n_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_n_support_info");
                    calculateSupportDamage(support_info);
                }

                if (isKeyExist(api_data, "api_friendly_battle")) {
                    JsonObject friend_battle = api_data.getAsJsonObject("api_friendly_battle");
                    JsonObject friend_hougeki = friend_battle.getAsJsonObject("api_hougeki");
                    if (isKeyExist(friend_hougeki, "api_df_list")) {
                        calculateFriendSupportFleetHougekiDamage(friend_hougeki);
                    }
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    if (isKeyExist(hougeki, "api_df_list")) {
                        calculateCombinedHougekiDamage(hougeki, 0, 0);
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined", enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_EC)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");
                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                JsonArray activeDeckData = api_data.getAsJsonArray("api_active_deck");
                int[] activedeck = {0, 0};
                for (int i = 0; i < activeDeckData.size(); i++) {
                    activedeck[i] = activeDeckData.get(i).getAsInt();
                }

                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                if (isKeyExist(api_data, "api_f_nowhps_combined")) {
                    String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();
                    friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                    friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                }

                String enemyCbNowHpsData = api_data.getAsJsonArray("api_e_nowhps_combined").toString();
                enemyCbNowHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();
                enemyCbAfterHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();

                // 야간지원함대
                if (isKeyExist(api_data, "api_n_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_n_support_info");
                    calculateSupportDamage(support_info);
                }

                if (isKeyExist(api_data, "api_friendly_battle")) {
                    JsonObject friend_battle = api_data.getAsJsonObject("api_friendly_battle");
                    JsonObject friend_hougeki = friend_battle.getAsJsonObject("api_hougeki");
                    if (isKeyExist(friend_hougeki, "api_df_list")) {
                        calculateFriendSupportFleetHougekiDamage(friend_hougeki);
                    }
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    if (isKeyExist(hougeki, "api_df_list")) {
                        calculateEnemyCombinedNightDamage(hougeki, activedeck);
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined", enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE_EC_NIGHTTODAY)) {
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");

                currentEnemyFormation = api_data.getAsJsonArray("api_formation").get(1).getAsInt();

                String friendMaxHpsData = api_data.getAsJsonArray("api_f_maxhps").toString();
                String friendNowHpsData = api_data.getAsJsonArray("api_f_nowhps").toString();
                String enemyMaxHpsData = api_data.getAsJsonArray("api_e_maxhps").toString();
                String enemyNowHpsData = api_data.getAsJsonArray("api_e_nowhps").toString();

                friendMaxHps = KcaUtils.parseJson(friendMaxHpsData).getAsJsonArray();
                friendNowHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();
                friendAfterHps = KcaUtils.parseJson(friendNowHpsData).getAsJsonArray();

                enemyMaxHps = KcaUtils.parseJson(enemyMaxHpsData).getAsJsonArray();
                enemyNowHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();
                enemyAfterHps = KcaUtils.parseJson(enemyNowHpsData).getAsJsonArray();

                if (isKeyExist(api_data, "api_f_maxhps_combined")) {
                    String friendCbMaxHpsData = api_data.getAsJsonArray("api_f_maxhps_combined").toString();
                    String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();

                    friendCbMaxHps = KcaUtils.parseJson(friendCbMaxHpsData).getAsJsonArray();
                    friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                    friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                }

                String enemyCbMaxHpsData = api_data.getAsJsonArray("api_e_maxhps_combined").toString();
                String enemyCbNowHpsData = api_data.getAsJsonArray("api_e_nowhps_combined").toString();

                enemyCbMaxHps = KcaUtils.parseJson(enemyCbMaxHpsData).getAsJsonArray();
                enemyCbNowHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();
                enemyCbAfterHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();

                if (isKeyExist(api_data, "api_n_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_n_support_info");
                    calculateSupportDamage(support_info);
                }

                for (int n = 1; n <= 2; n++) {
                    String api_name = KcaUtils.format("api_n_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        if (isKeyExist(hougeki, "api_df_list")) {
                            calculateEnemyCombinedHougekiDamage(hougeki, true, PHASE_3);
                        }
                    }
                }

                // 주간전 이행: 일반함대

                // 기항대분식항공전 Stage 3
                if (isKeyExist(api_data, "api_air_base_injection")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_air_base_injection"));
                }

                // 분식항공전 Stage 3
                if (isKeyExist(api_data, "api_injection_kouku")) { // Check Null for old data
                    calculateAirBattle(api_data.getAsJsonObject("api_injection_kouku"));
                }

                if (isKeyExist(api_data, "api_air_base_attack")) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        calculateAirBattle(airbase_attack.get(i).getAsJsonObject());
                    }
                }

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                // 지원함대
                if (isKeyExist(api_data, "api_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_support_info");
                    calculateSupportDamage(support_info);
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    if (isKeyExist(opening_taisen, "api_df_list")) {
                        calculateHougekiDamage(opening_taisen);
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    calculateRaigekiDamage(openingattack);
                }

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        if (isKeyExist(hougeki, "api_df_list")) {
                            calculateHougekiDamage(hougeki);
                        }
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    calculateRaigekiDamage(raigeki);
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined", enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLERESULT)) {
                friendNowHps = KcaUtils.parseJson(friendAfterHps.toString()).getAsJsonArray();
                if (friendCbAfterHps != null)
                    friendCbNowHps = KcaUtils.parseJson(friendCbAfterHps.toString()).getAsJsonArray();

                Bundle bundle;
                Message sMsg;

                int checkresult = HD_NONE;
                if (!isEndReached) {
                    checkresult = checkCombinedHeavyDamagedExist();
                    Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkresult));
                    if (checkresult != HD_NONE) {
                        JsonObject heavyDamagedInfo = new JsonObject();
                        heavyDamagedInfo.addProperty("data", checkresult);
                        bundle = new Bundle();
                        bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                        bundle.putString("data", gson.toJson(heavyDamagedInfo));
                        sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }

                    if (isKeyExist(api_data, "api_escape_flag")) {
                        int api_escape_flag = api_data.get("api_escape_flag").getAsInt();
                        Log.e("KCA", "api_escape_flag: " + String.valueOf(api_escape_flag));
                        if (api_escape_flag == 1) {
                            escapedata = api_data.getAsJsonObject("api_escape");
                            Log.e("KCA", "api_escape: " + escapedata.toString());
                        } else {
                            escapedata = null;
                        }
                    }
                }

                JsonObject dropInfo = new JsonObject();
                dropInfo.addProperty("world", currentMapArea);
                dropInfo.addProperty("map", currentMapNo);
                dropInfo.addProperty("node", currentNode);
                dropInfo.addProperty("isboss", isBossReached);
                dropInfo.addProperty("quest_name", api_data.get("api_quest_name").getAsString());
                dropInfo.addProperty("enemy_name", api_data.getAsJsonObject("api_enemy_info").get("api_deck_name").getAsString());
                dropInfo.addProperty("rank", api_data.get("api_win_rank").getAsString());
                dropInfo.addProperty("maprank", currentEventMapRank);
                if (api_data.has("api_get_ship")) {
                    int api_ship_id = api_data.getAsJsonObject("api_get_ship").get("api_ship_id").getAsInt();
                    dropInfo.addProperty("result", api_ship_id);
                    dropInfo.addProperty("inventory", KcaApiData.countUserShipById(api_ship_id));
                } else {
                    dropInfo.addProperty("result", 0);
                    dropInfo.addProperty("inventory", 0);
                }
                JsonObject enemyInfo = new JsonObject();
                enemyInfo.addProperty("formation", currentEnemyFormation);
                if (ship_ke != null) enemyInfo.add("ships", ship_ke);
                if (ship_ke_combined != null) enemyInfo.add("ships2", ship_ke_combined);
                dropInfo.add("enemy", enemyInfo);

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_DROPINFO);
                bundle.putString("data", gson.toJson(dropInfo));
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);

                JsonObject qtrackData = new JsonObject();
                qtrackData.addProperty("world", currentMapArea);
                qtrackData.addProperty("map", currentMapNo);
                qtrackData.addProperty("node", currentNode);
                qtrackData.addProperty("isboss", isBossReached);
                qtrackData.add("ship_ke", ship_ke);
                qtrackData.add("deck_port", deckportdata);
                qtrackData.add("afterhps_f", friendAfterHps);
                qtrackData.add("afterhps_e", enemyAfterHps);
                if (isCombinedFleetInSortie()) {
                    qtrackData.add("aftercbhps_f", friendCbAfterHps);
                }
                if (ship_ke_combined != null) {
                    qtrackData.addProperty("combined_flag", true);
                    qtrackData.add("ship_ke_combined", ship_ke_combined);
                    qtrackData.add("aftercbhps_e", enemyCbAfterHps);
                } else {
                    qtrackData.addProperty("combined_flag", false);
                }
                qtrackData.addProperty("result", api_data.get("api_win_rank").getAsString());
                helper.putValue(DB_KEY_QTRACKINFO, qtrackData.toString());
                helper.updateExpScore(api_data.get("api_get_exp").getAsInt());

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_dc_used", damecon_used);
                battleResultInfo.addProperty("api_heavy_damaged", checkresult);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_GOBACKPORT)) {
                if (escapedata == null) {
                    Log.e("KCA", "escapedata is null");
                } else {
                    JsonArray api_escape_idx = escapedata.getAsJsonArray("api_escape_idx");
                    JsonArray api_tow_idx = escapedata.getAsJsonArray("api_tow_idx");

                    int api_escape_target = api_escape_idx.get(0).getAsInt(); // only first
                    int api_tow_target = api_tow_idx.get(0).getAsInt(); // only first

                    if (api_escape_target > 6) {
                        if (!escapecblist.contains(new JsonPrimitive(api_escape_target - 6))) {
                            escapecblist.add(api_escape_target - 6);
                        }
                    } else {
                        if (!escapelist.contains(new JsonPrimitive(api_escape_target))) {
                            escapelist.add(api_escape_target);
                        }
                    }

                    if (!escapecblist.contains(new JsonPrimitive(api_tow_target - 6))) {
                        escapecblist.add(api_tow_target - 6);
                    }

                    JsonObject battleResultInfo = helper.getJsonObjectValue(DB_KEY_BATTLEINFO);
                    battleResultInfo.add("api_escape", escapelist);
                    battleResultInfo.add("api_escape_combined", escapecblist);
                    setCurrentApiData(battleResultInfo);

                    Log.e("KCA", KcaUtils.format("Escape: %d with %d", api_escape_target, api_tow_target));
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                    bundle.putString("data", battleResultInfo.toString());
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
            }
        } catch (Exception e) {
            JsonObject errorInfo = new JsonObject();
            String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);
            errorInfo.addProperty("api_data", api_data.toString());
            errorInfo.addProperty("api_url", url);
            errorInfo.addProperty("api_node", KcaUtils.format("%d-%d-%s", currentMapArea, currentMapNo, currentNodeAlphabet));
            errorInfo.addProperty("api_error", getStringFromException(e));

            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_PROCESS_BATTLE_FAILED);
            bundle.putString("data", gson.toJson(errorInfo));
            Message sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
    }


    public static void test() {
        JsonObject data = gson.fromJson("{\"api_deck_id\":1,\"api_formation\":[12,14,2],\"api_f_nowhps\":[85,20,88,62,18,52],\"api_f_maxhps\":[93,62,99,62,63,65],\"api_f_nowhps_combined\":[47,33,65,34,70,31],\"api_f_maxhps_combined\":[51,35,65,37,70,31],\"api_fParam\":[[114,0,80,104],[76,87,74,75],[99,36,70,95],[40,0,74,62],[75,80,47,81],[34,0,72,65]],\"api_fParam_combined\":[[53,80,88,67],[47,71,68,53],[77,84,106,78],[73,93,59,52],[75,84,60,82],[37,71,55,40]],\"api_ship_ke\":[1842,1752,1698,1707,1595,1595],\"api_ship_lv\":[1,1,1,1,1,1],\"api_ship_ke_combined\":[1555,1592,1741,1741,1741,1741],\"api_ship_lv_combined\":[1,1,1,1,1,1],\"api_e_nowhps\":[900,440,530,490,88,88],\"api_e_maxhps\":[900,440,530,490,88,88],\"api_e_nowhps_combined\":[57,66,66,66,66,66],\"api_e_maxhps_combined\":[57,66,66,66,66,66],\"api_eSlot\":[[587,587,559,586,-1],[547,548,549,532,-1],[509,509,512,529,-1],[563,563,515,564,-1],[505,505,515,525,-1],[505,505,515,525,-1]],\"api_eSlot_combined\":[[506,525,542,543,-1],[550,550,545,525,-1],[501,501,559,-1,-1],[501,501,559,-1,-1],[501,501,559,-1,-1],[501,501,559,-1,-1]],\"api_eParam\":[[330,0,150,285],[190,0,140,149],[195,0,88,168],[170,90,86,189],[79,74,79,89],[79,74,79,89]],\"api_eParam_combined\":[[48,80,30,39],[64,92,96,68],[79,99,79,59],[79,99,79,59],[79,99,79,59],[79,99,79,59]],\"api_flavor_info\":[{\"api_boss_ship_id\":\"1842\",\"api_type\":\"1\",\"api_voice_id\":\"425184010\",\"api_class_name\":\"戦艦\",\"api_ship_name\":\"欧州水姫\",\"api_message\":\"キサマ…タチハ……。ココデッ…シズムノッ……！<br>シズムノ…ヨォッ！\",\"api_pos_x\":\"-6\",\"api_pos_y\":\"-31\",\"api_data\":\"\"}],\"api_midnight_flag\":1,\"api_search\":[1,1],\"api_air_base_attack\":[{\"api_base_id\":2,\"api_stage_flag\":[1,1,1],\"api_plane_from\":[null,[1,2,4,5,6,7,8]],\"api_squadron_plane\":[{\"api_mst_id\":223,\"api_count\":14},{\"api_mst_id\":186,\"api_count\":18},{\"api_mst_id\":186,\"api_count\":18},{\"api_mst_id\":186,\"api_count\":18}],\"api_stage1\":{\"api_f_count\":68,\"api_f_lostcount\":19,\"api_e_count\":279,\"api_e_lostcount\":34,\"api_disp_seiku\":3,\"api_touch_plane\":[-1,586]},\"api_stage2\":{\"api_f_count\":37,\"api_f_lostcount\":6,\"api_e_count\":0,\"api_e_lostcount\":0},\"api_stage3\":{\"api_erai_flag\":[0,0,0,0,1,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_edam\":[0,0,0,0,34,0]},\"api_stage3_combined\":{\"api_erai_flag\":[0,0,0,1,0,1],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,1,0,0],\"api_edam\":[0,0,0,187,0,0]}},{\"api_base_id\":2,\"api_stage_flag\":[1,1,1],\"api_plane_from\":[null,[1,2,4,5,6,7,8]],\"api_squadron_plane\":[{\"api_mst_id\":223,\"api_count\":14},{\"api_mst_id\":186,\"api_count\":18},{\"api_mst_id\":186,\"api_count\":18},{\"api_mst_id\":186,\"api_count\":18}],\"api_stage1\":{\"api_f_count\":68,\"api_f_lostcount\":19,\"api_e_count\":245,\"api_e_lostcount\":34,\"api_disp_seiku\":3,\"api_touch_plane\":[-1,586]},\"api_stage2\":{\"api_f_count\":39,\"api_f_lostcount\":6,\"api_e_count\":0,\"api_e_lostcount\":0},\"api_stage3\":{\"api_erai_flag\":[0,0,1,0,0,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_edam\":[0,0,16,0,0,0]},\"api_stage3_combined\":{\"api_erai_flag\":[0,1,1,0,0,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_edam\":[0,51,79,0,0,0]}},{\"api_base_id\":3,\"api_stage_flag\":[1,1,1],\"api_plane_from\":[null,[1,2,4,5,6,7,8]],\"api_squadron_plane\":[{\"api_mst_id\":225,\"api_count\":17},{\"api_mst_id\":187,\"api_count\":18},{\"api_mst_id\":180,\"api_count\":18},{\"api_mst_id\":170,\"api_count\":18}],\"api_stage1\":{\"api_f_count\":71,\"api_f_lostcount\":15,\"api_e_count\":211,\"api_e_lostcount\":51,\"api_disp_seiku\":0,\"api_touch_plane\":[-1,564]},\"api_stage2\":{\"api_f_count\":43,\"api_f_lostcount\":5,\"api_e_count\":0,\"api_e_lostcount\":0},\"api_stage3\":{\"api_erai_flag\":[0,0,1,0,1,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_edam\":[0,0,81,0,37,0]},\"api_stage3_combined\":{\"api_erai_flag\":[1,0,0,0,0,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_edam\":[104,0,0,0,0,0]}},{\"api_base_id\":3,\"api_stage_flag\":[1,1,1],\"api_plane_from\":[null,[1,2,4,5,6,8]],\"api_squadron_plane\":[{\"api_mst_id\":225,\"api_count\":17},{\"api_mst_id\":187,\"api_count\":18},{\"api_mst_id\":180,\"api_count\":18},{\"api_mst_id\":170,\"api_count\":18}],\"api_stage1\":{\"api_f_count\":71,\"api_f_lostcount\":12,\"api_e_count\":158,\"api_e_lostcount\":36,\"api_disp_seiku\":0,\"api_touch_plane\":[-1,564]},\"api_stage2\":{\"api_f_count\":45,\"api_f_lostcount\":11,\"api_e_count\":0,\"api_e_lostcount\":0},\"api_stage3\":{\"api_erai_flag\":[1,0,0,1,0,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_edam\":[112,0,0,0.1,0,0]},\"api_stage3_combined\":{\"api_erai_flag\":[0,1,0,0,0,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,1,0,0,0,0],\"api_edam\":[0,153,0,0,0,0]}}],\"api_stage_flag\":[1,1,1],\"api_kouku\":{\"api_plane_from\":[[2,4,6],[1,2]],\"api_stage1\":{\"api_f_count\":108,\"api_f_lostcount\":8,\"api_e_count\":113,\"api_e_lostcount\":37,\"api_disp_seiku\":2,\"api_touch_plane\":[102,-1]},\"api_stage2\":{\"api_f_count\":53,\"api_f_lostcount\":11,\"api_e_count\":48,\"api_e_lostcount\":36,\"api_air_fire\":{\"api_idx\":8,\"api_kind\":10,\"api_use_items\":[135,131,89]}},\"api_stage3\":{\"api_frai_flag\":[0,0,0,0,0,1],\"api_erai_flag\":[0,0,1,0,0,0],\"api_fbak_flag\":[0,0,0,0,0,0],\"api_ebak_flag\":[0,0,0,0,0,1],\"api_fcl_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_fdam\":[0,0,0,0,0,0],\"api_edam\":[0,0,41,0,0,11]},\"api_stage3_combined\":{\"api_frai_flag\":[0,0,0,0,0,0],\"api_erai_flag\":[0,0,0,0,1,0],\"api_fbak_flag\":[0,0,0,0,0,0],\"api_ebak_flag\":[0,0,0,0,0,0],\"api_fcl_flag\":[0,0,0,0,0,0],\"api_ecl_flag\":[0,0,0,0,0,0],\"api_fdam\":[0,0,0,0,0,0],\"api_edam\":[0,0,0,0,0,0]}},\"api_support_flag\":0,\"api_support_info\":null,\"api_opening_taisen_flag\":0,\"api_opening_taisen\":null,\"api_opening_flag\":1,\"api_opening_atack\":{\"api_frai\":[-1,-1,-1,-1,-1,-1,3,-1,-1,-1,-1,-1],\"api_fcl\":[0,0,0,0,0,0,1,0,0,0,0,0],\"api_fdam\":[0,0,0,0,0,0,0,0,0,0,0,0],\"api_fydam\":[0,0,0,0,0,0,65,0,0,0,0,0],\"api_erai\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"api_ecl\":[0,0,0,0,0,0,0,0,0,0,0,0],\"api_edam\":[0,0,0,65,0,0,0,0,0,0,0,0],\"api_eydam\":[0,0,0,0,0,0,0,0,0,0,0,0]},\"api_hourai_flag\":[1,1,1,1],\"api_hougeki1\":{\"api_at_eflag\":[0,1,0,1,0,1,0,1,0],\"api_at_list\":[0,0,2,3,5,2,1,4,4],\"api_at_type\":[100,0,6,0,7,0,0,0,0],\"api_df_list\":[[5,1,2],[1],[3],[3],[2],[2],[3],[4],[3]],\"api_si_list\":[[-1],[587],[\"59\",\"103\",\"161\"],[563],[\"206\",\"306\",\"94\"],[509],[50],[505],[123]],\"api_cl_list\":[[1,2,1],[1],[1],[2],[1],[0],[1],[0],[1]],\"api_damage\":[[359.1,602,69],[12],[194],[40],[30],[0],[21],[0],[13]]},\"api_hougeki2\":{\"api_at_eflag\":[0,1,0,1,0,1,0,0],\"api_at_list\":[0,0,1,2,2,3,4,5],\"api_at_type\":[6,0,0,0,2,0,2,7],\"api_df_list\":[[4],[3],[11],[3],[11,11],[2],[3,3],[3]],\"api_si_list\":[[\"239\",\"137\",\"300\"],[587],[50],[512],[\"103\",\"161\"],[563],[\"123\",\"50\"],[\"206\",\"306\",\"94\"]],\"api_cl_list\":[[1],[1],[1],[0],[1,1],[0],[1,1],[1]],\"api_damage\":[[266],[11],[5],[0],[163,172],[0],[25,22],[10]]},\"api_hougeki3\":{\"api_at_eflag\":[0,1,0,0,0,0,0],\"api_at_list\":[8,10,10,6,7,11,9],\"api_at_type\":[0,0,0,0,0,0,0],\"api_df_list\":[[10],[8],[10],[10],[10],[10],[10]],\"api_si_list\":[[135],[501],[-1],[134],[122],[-1],[-1]],\"api_cl_list\":[[1],[0],[0],[1],[0],[1],[1]],\"api_damage\":[[33],[0],[0],[16],[0],[1],[23]]},\"api_raigeki\":{\"api_frai\":[-1,-1,-1,-1,-1,-1,0,0,2,3,0,0],\"api_fcl\":[0,0,0,0,0,0,2,0,1,1,1,0],\"api_fdam\":[0,0,0,0,0,0,0,0,0,0,0,0],\"api_fydam\":[0,0,0,0,0,0,77,0,34,8,97,0],\"api_erai\":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],\"api_ecl\":[0,0,0,0,0,0,0,0,0,0,0,0],\"api_edam\":[174,0,34,8,0,0,0,0,0,0,0,0],\"api_eydam\":[0,0,0,0,0,0,0,0,0,0,0,0]},\"api_name\":\"each_battle_water\"}", JsonObject.class);
        processData(null, API_REQ_COMBINED_BATTLE_EACH_WATER, data);

        Log.e("KCA", enemyMaxHps.toString());
        Log.e("KCA", enemyCbMaxHps.toString());

        Log.e("KCA", enemyAfterHps.toString());
        Log.e("KCA", enemyCbAfterHps.toString());
    }
}
