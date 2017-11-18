package com.antest1.kcanotify;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.primitives.Booleans;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaConstants.*;
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

    public static String currentEnemyDeckName = "";

    public static int checkHeavyDamagedExist() {
        int status = HD_NONE;
        for (int i = 0; i < friendMaxHps.size(); i++) {
            if (friendNowHps.get(i).getAsInt() * 4 <= friendMaxHps.get(i).getAsInt()) {
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
        for (int i = 0; i < friendCbMaxHps.size(); i++) {
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

    public static void reduce_value(JsonArray target, int idx, int amount) {
        if (idx < target.size()) {
            int before_value = target.get(idx).getAsInt();
            int after_value = before_value - amount;
            target.set(idx, new JsonPrimitive(after_value));
        }
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
            friendCount += 1;
            if (api_f_afterhps.get(i).getAsInt() <= 0) {
                friendSunkCount += 1;
            }
            friendNowSum += api_f_nowhps.get(i).getAsInt();
            friendAfterSum += Math.max(0, api_f_afterhps.get(i).getAsInt());
        }

        for (int i = 0; i < api_f_nowhps_combined.size(); i++) {
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
                    reduce_value(friendAfterHps, i, cnv(api_fdam.get(i)));
                }
            }
            if (isKeyExist(api_stage3, "api_edam")) {
                JsonArray api_edam = api_stage3.getAsJsonArray("api_edam");
                for (int i = 0; i < api_edam.size(); i++) {
                    reduce_value(enemyAfterHps, i, cnv(api_edam.get(i)));
                }
            }
        }
        if (isKeyExist(data, "api_stage3_combined")) {
            JsonObject api_stage3_combined = data.getAsJsonObject("api_stage3_combined");
            if (isKeyExist(api_stage3_combined, "api_fdam")) {
                JsonArray api_fdam = api_stage3_combined.getAsJsonArray("api_fdam");
                for (int i = 0; i < api_fdam.size(); i++) {
                    reduce_value(friendCbAfterHps, i, cnv(api_fdam.get(i)));
                }
            }
            if (isKeyExist(api_stage3_combined, "api_edam")) {
                JsonArray api_edam = api_stage3_combined.getAsJsonArray("api_edam");
                for (int i = 0; i < api_edam.size(); i++) {
                    reduce_value(enemyCbAfterHps, i, cnv(api_edam.get(i)));
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
                bundle.putString("data", "");
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
                bundle.putString("data", "");
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

                // Log.e("KCA", "hpInfo (kouku): " + Arrays.toString(afterhps));

                // 지원함대
                if (isKeyExist(api_data, "api_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_support_info");
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
                        reduce_value(enemyAfterHps, d, cnv(damage.get(d)));
                    }
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray at_eflag = opening_taisen.getAsJsonArray("api_at_eflag").getAsJsonArray();
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list").getAsJsonArray();
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage").getAsJsonArray();
                    for (int i = 0; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                        }
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    JsonArray openingattack_fdam = openingattack.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = openingattack.getAsJsonArray("api_edam");
                    for (int i = 0; i < friendAfterHps.size(); i++) {
                        reduce_value(friendAfterHps, i, cnv(openingattack_fdam.get(i)));
                    }
                    for (int i = 0; i < enemyAfterHps.size(); i++) {
                        reduce_value(enemyAfterHps, i, cnv(openingattack_edam.get(i)));
                    }
                }

                //Log.e("KCA", "hpInfo (start): " + Arrays.toString(afterhps));

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        JsonArray at_eflag = hougeki.getAsJsonArray("api_at_eflag");
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                        for (int i = 0; i < df_list.size(); i++) {
                            int eflag = at_eflag.get(i).getAsInt();
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            }
                        }
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    JsonArray raigeki_fdam = raigeki.getAsJsonArray("api_fdam");
                    JsonArray raigeki_edam = raigeki.getAsJsonArray("api_edam");
                    for (int i = 0; i < friendAfterHps.size(); i++) {
                        reduce_value(friendAfterHps, i, cnv(raigeki_fdam.get(i)));
                    }
                    for (int i = 0; i < enemyAfterHps.size(); i++) {
                        reduce_value(enemyAfterHps, i, cnv(raigeki_edam.get(i)));
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
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
                bundle.putString("data", "");
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
                    JsonArray damage = new JsonArray();
                    if (!support_info.get("api_support_airatack").isJsonNull()) {
                        // 항공지원
                        JsonObject support_airattack = support_info.getAsJsonObject("api_support_airatack");
                        damage = support_airattack.getAsJsonObject("api_stage3").getAsJsonArray("api_edam");
                    } else if (!support_info.get("api_support_hourai").isJsonNull()) {
                        JsonObject support_hourai = support_info.getAsJsonObject("api_support_hourai");
                        damage = support_hourai.getAsJsonArray("api_damage");
                    }
                    for (int d = 0; d < damage.size(); d++) {
                        reduce_value(enemyAfterHps, d, cnv(damage.get(d)));
                    }
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray at_eflag = hougeki.getAsJsonArray("api_at_eflag");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 0; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                        }
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
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
                bundle.putString("data", "");
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

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                if (url.equals(API_REQ_SORTIE_AIRBATTLE)) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku2"));
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
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
                    if(!isEndReached) {
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
                    }

                    if (KcaApiData.checkUserPortEnough()) {
                        JsonObject dropInfo = new JsonObject();
                        dropInfo.addProperty("world", currentMapArea);
                        dropInfo.addProperty("map", currentMapNo);
                        dropInfo.addProperty("node", currentNode);
                        dropInfo.addProperty("rank", api_data.get("api_win_rank").getAsString());
                        dropInfo.addProperty("maprank", currentEventMapRank);
                        if (api_data.has("api_get_ship")) {
                            int api_ship_id = api_data.getAsJsonObject("api_get_ship").get("api_ship_id").getAsInt();
                            dropInfo.addProperty("result", api_ship_id);
                            dropInfo.addProperty("inventory", KcaApiData.countUserShipById(api_ship_id));
                            KcaApiData.addShipCountInBattle();
                            KcaApiData.addItemCountInBattle(api_ship_id);
                        } else {
                            dropInfo.addProperty("result", 0);
                            dropInfo.addProperty("inventory", 0);
                        }
                        JsonObject enemyInfo = new JsonObject();
                        enemyInfo.addProperty("formation", currentEnemyFormation);
                        if (ship_ke != null) {
                            JsonArray enemyinfo_ship = new JsonArray();
                            for (int i = 0; i < 6; i++) {
                                if (i < ship_ke.size()) {
                                    int ship_value = ship_ke.get(i).getAsInt();
                                    enemyinfo_ship.add(ship_value);
                                } else {
                                    enemyinfo_ship.add(0);
                                }
                            }
                            enemyInfo.add("ships", enemyinfo_ship);
                        }
                        dropInfo.add("enemy", enemyInfo);

                        bundle = new Bundle();
                        bundle.putString("url", KCA_API_NOTI_BATTLE_DROPINFO);
                        bundle.putString("data", gson.toJson(dropInfo));
                        sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }

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
                    JsonArray damage = new JsonArray();
                    if (!support_info.get("api_support_airatack").isJsonNull()) {
                        // 항공지원
                        JsonObject support_airattack = support_info.getAsJsonObject("api_support_airatack");
                        damage = support_airattack.getAsJsonObject("api_stage3").getAsJsonArray("api_edam");
                    } else if (!support_info.get("api_support_hourai").isJsonNull()) {
                        JsonObject support_hourai = support_info.getAsJsonObject("api_support_hourai");
                        damage = support_hourai.getAsJsonArray("api_damage");
                    }
                    for (int d = 0; d < damage.size(); d++) {
                        reduce_value(enemyAfterHps, d, cnv(damage.get(d)));
                    }
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray at_eflag = opening_taisen.getAsJsonArray("api_at_eflag");
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list");
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage");
                    for (int i = 0; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                        }
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    JsonArray openingattack_fdam = openingattack.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = openingattack.getAsJsonArray("api_edam");
                    for (int i = 0; i < friendCbAfterHps.size(); i++) {
                        reduce_value(friendCbAfterHps, i, cnv(openingattack_fdam.get(i)));
                    }
                    for (int i = 0; i < enemyAfterHps.size(); i++) {
                        reduce_value(enemyAfterHps, i, cnv(openingattack_edam.get(i)));
                    }
                }

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        JsonArray at_eflag = hougeki.getAsJsonArray("api_at_eflag");
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                        for (int i = 0; i < df_list.size(); i++) {
                            int eflag = at_eflag.get(i).getAsInt();
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                if (combined_type == COMBINED_A) {
                                    if (n == 1) {
                                        if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        else reduce_value(friendCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                    } else {
                                        if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                    }
                                } else if (combined_type == COMBINED_W) {
                                    if (n == 3) {
                                        if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        else reduce_value(friendCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                    } else {
                                        if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                    }
                                }
                            }
                        }
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    JsonArray raigeki_fdam = raigeki.getAsJsonArray("api_fdam");
                    JsonArray raigeki_edam = raigeki.getAsJsonArray("api_edam");
                    for (int i = 0; i < friendAfterHps.size(); i++) {
                        reduce_value(friendCbAfterHps, i, cnv(raigeki_fdam.get(i)));
                    }
                    for (int i = 0; i < enemyAfterHps.size(); i++) {
                        reduce_value(enemyAfterHps, i, cnv(raigeki_edam.get(i)));
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined",enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
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

                String friendCbMaxHpsData = api_data.getAsJsonArray("api_f_maxhps_combined").toString();
                String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();
                String enemyCbMaxHpsData = api_data.getAsJsonArray("api_e_maxhps_combined").toString();
                String enemyCbNowHpsData = api_data.getAsJsonArray("api_e_nowhps_combined").toString();

                friendCbMaxHps = KcaUtils.parseJson(friendCbMaxHpsData).getAsJsonArray();
                friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();

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

                // 지원함대 TODO: NEEDS VALIDATED
                if (isKeyExist(api_data, "api_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_support_info");
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
                        if (d < 6) {
                            reduce_value(enemyAfterHps, d, cnv(damage.get(d)));
                        } else {
                            reduce_value(enemyCbAfterHps, d - 6, cnv(damage.get(d)));
                        }
                   }
                }

                // 선제대잠 TODO: NEEDS VALIDATED
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray at_eflag = opening_taisen.getAsJsonArray("api_at_eflag");
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list");
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage");
                    for (int i = 0; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            if (eflag == 0) {
                                if (target_idx < 6) {
                                    reduce_value(enemyAfterHps, target_idx, cnv(target_dmg.get(t)));
                                } else {
                                    reduce_value(enemyCbAfterHps, target_idx - 6, cnv(target_dmg.get(t)));
                                }
                            } else {
                                if (target_idx < 6) {
                                    reduce_value(friendAfterHps, target_idx, cnv(target_dmg.get(t)));
                                } else {
                                    reduce_value(friendCbAfterHps, target_idx - 6, cnv(target_dmg.get(t)));
                                }
                            }
                        }
                    }
                }

                // 개막뇌격 TODO: NEEDS VALIDATED
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    JsonArray openingattack_fdam = openingattack.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = openingattack.getAsJsonArray("api_edam");
                    for (int i = 0; i < openingattack_fdam.size(); i++) {
                        if (i < 6) reduce_value(friendAfterHps, i, cnv(openingattack_fdam.get(i)));
                        else reduce_value(friendCbAfterHps, i - 6, cnv(openingattack_fdam.get(i)));
                    }
                    for (int i = 0; i < openingattack_edam.size(); i++) {
                        if (i < 6) reduce_value(enemyAfterHps, i, cnv(openingattack_edam.get(i)));
                        else reduce_value(enemyCbAfterHps, i - 6, cnv(openingattack_edam.get(i)));
                    }
                }

                // 포격전
                int all_phase = 0;
                int first_phase = 0;
                int second_phase = 0;
                if (combined_type == COMBINED_A) {
                    first_phase = 1;
                    second_phase = 2;
                    all_phase = 3;
                } else if (combined_type == COMBINED_W) {
                    first_phase = 1;
                    all_phase = 2;
                    second_phase = 3;
                } else {
                    second_phase = 1;
                    first_phase = 2;
                    all_phase = 3;
                }

                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(KcaUtils.format("api_hougeki%d", n));
                        JsonArray at_eflag = hougeki.getAsJsonArray("api_at_eflag");
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");

                        for (int i = 0; i < df_list.size(); i++) {
                            int eflag = at_eflag.get(i).getAsInt();
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                int target_idx = target.get(t).getAsInt();
                                int damage_value = target_dmg.get(t).getAsInt();
                                if (n == first_phase) {
                                    if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                    else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                } else if (n == second_phase) {
                                    if (eflag == 0)
                                        reduce_value(enemyCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                    else {
                                        if (combined_type == COMBINED_A || combined_type == COMBINED_W) {
                                            reduce_value(friendCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        } else {
                                            reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        }
                                    }
                                } else if (n == all_phase) {
                                    if (eflag == 0) {
                                        if (target_idx > 6) {
                                            reduce_value(enemyCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        } else if (target_idx != -1) {
                                            reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        }
                                    } else {
                                        if (target_idx > 6) {
                                            reduce_value(friendCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        } else if (target_idx != -1) {
                                            reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 폐막뇌격 TODO: NEEDS VALIDATED
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    JsonArray raigeki_fdam = raigeki.getAsJsonArray("api_fdam");
                    JsonArray raigeki_edam = raigeki.getAsJsonArray("api_edam");
                    for (int i = 0; i < raigeki_fdam.size(); i++) {
                        if (i < 6) reduce_value(friendAfterHps, i, cnv(raigeki_fdam.get(i)));
                        else reduce_value(friendCbAfterHps, i - 6, cnv(raigeki_fdam.get(i)));
                    }
                    for (int i = 0; i < raigeki_edam.size(); i++) {
                        if (i < 6) reduce_value(enemyAfterHps, i, cnv(raigeki_edam.get(i)));
                        else reduce_value(enemyCbAfterHps, i - 6, cnv(raigeki_edam.get(i)));
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined",enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
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

                // 제1항공전 Stage 3
                calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                if (url.equals(API_REQ_COMBINED_AIRBATTLE)) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku2"));
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined",enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
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
                    JsonArray damage = new JsonArray();
                    if (!support_info.get("api_support_airatack").isJsonNull()) {
                        // 항공지원
                        JsonObject support_airattack = support_info.getAsJsonObject("api_support_airatack");
                        damage = support_airattack.getAsJsonObject("api_stage3").getAsJsonArray("api_edam");
                    } else if (!support_info.get("api_support_hourai").isJsonNull()) {
                        JsonObject support_hourai = support_info.getAsJsonObject("api_support_hourai");
                        damage = support_hourai.getAsJsonArray("api_damage");
                    }
                    for (int d = 0; d < damage.size(); d++) {
                        reduce_value(enemyAfterHps, d, cnv(damage.get(d)));
                    }
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray at_eflag = hougeki.getAsJsonArray("api_at_eflag");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 0; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            if (eflag == 0) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            else reduce_value(friendCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                        }
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined",enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
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

                String friendCbNowHpsData = api_data.getAsJsonArray("api_f_nowhps_combined").toString();
                String enemyCbNowHpsData = api_data.getAsJsonArray("api_e_nowhps_combined").toString();

                friendCbNowHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();
                friendCbAfterHps = KcaUtils.parseJson(friendCbNowHpsData).getAsJsonArray();

                enemyCbNowHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();
                enemyCbAfterHps = KcaUtils.parseJson(enemyCbNowHpsData).getAsJsonArray();

                // 야간지원함대
                if (isKeyExist(api_data, "api_n_support_info")) {
                    JsonObject support_info = api_data.getAsJsonObject("api_n_support_info");
                    JsonArray damage = new JsonArray();
                    if (!support_info.get("api_support_airatack").isJsonNull()) {
                        // 항공지원
                        JsonObject support_airattack = support_info.getAsJsonObject("api_support_airatack");
                        damage = support_airattack.getAsJsonObject("api_stage3").getAsJsonArray("api_edam");
                    } else if (!support_info.get("api_support_hourai").isJsonNull()) {
                        JsonObject support_hourai = support_info.getAsJsonObject("api_support_hourai");
                        damage = support_hourai.getAsJsonArray("api_damage");
                    }
                    for (int d = 0; d < damage.size(); d++) {
                        if (activedeck[1] == 1) reduce_value(enemyAfterHps, d, cnv(damage.get(d)));
                        else reduce_value(enemyCbAfterHps, d, cnv(damage.get(d)));
                    }
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray at_eflag = hougeki.getAsJsonArray("api_at_eflag");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 0; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            if (eflag == 0) {
                                if (activedeck[1] == 1) reduce_value(enemyAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                else reduce_value(enemyCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            }
                            else {
                                if (KcaBattle.isCombined) reduce_value(friendCbAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                                else reduce_value(friendAfterHps, cnv(target.get(t)), cnv(target_dmg.get(t)));
                            }
                        }
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_f_afterhps", friendAfterHps);
                battleResultInfo.add("api_f_afterhps_combined", friendCbAfterHps);
                battleResultInfo.add("api_e_afterhps", enemyAfterHps);
                battleResultInfo.add("api_e_afterhps_combined",enemyCbAfterHps);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLERESULT)) {
                friendNowHps = KcaUtils.parseJson(friendAfterHps.toString()).getAsJsonArray();
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
                dropInfo.addProperty("rank", api_data.get("api_win_rank").getAsString());
                dropInfo.addProperty("maprank", currentEventMapRank);
                if (api_data.has("api_get_ship")) {
                    int api_ship_id = api_data.getAsJsonObject("api_get_ship").get("api_ship_id").getAsInt();
                    dropInfo.addProperty("result", api_ship_id);
                    dropInfo.addProperty("inventory", KcaApiData.countUserShipById(api_ship_id));
                    KcaApiData.addShipCountInBattle();
                    KcaApiData.addItemCountInBattle(api_ship_id);
                } else {
                    dropInfo.addProperty("result", 0);
                    dropInfo.addProperty("inventory", 0);
                }
                JsonObject enemyInfo = new JsonObject();
                enemyInfo.addProperty("formation", currentEnemyFormation);
                if (ship_ke != null) {
                    JsonArray enemyinfo_ship = new JsonArray();
                    for (int i = 0; i < 6; i++) {
                        if (i < ship_ke.size()) {
                            int ship_value = ship_ke.get(i).getAsInt();
                            enemyinfo_ship.add(ship_value);
                        } else {
                            enemyinfo_ship.add(0);
                        }
                    }
                    enemyInfo.add("ships", enemyinfo_ship);
                }
                if (ship_ke_combined != null) {
                    JsonArray enemyinfo_ship_cb = new JsonArray();
                    for (int i = 0; i < 6; i++) {
                        if (i < ship_ke_combined.size()) {
                            int ship_value = ship_ke_combined.get(i).getAsInt();
                            enemyinfo_ship_cb.add(ship_value);
                        } else {
                            enemyinfo_ship_cb.add(0);
                        }
                    }
                    enemyInfo.add("ships2", enemyinfo_ship_cb);
                }
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
                if (ship_ke_combined != null) {
                    qtrackData.addProperty("combined_flag", true);
                    qtrackData.add("ship_ke_combined", ship_ke_combined);
                    qtrackData.add("aftercbhps_f", friendCbAfterHps);
                    qtrackData.add("aftercbhps_e", enemyCbAfterHps);
                } else {
                    qtrackData.addProperty("combined_flag", false);
                }
                qtrackData.addProperty("result", api_data.get("api_win_rank").getAsString());
                helper.putValue(DB_KEY_QTRACKINFO, qtrackData.toString());
                helper.updateExpScore(api_data.get("api_get_exp").getAsInt());

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.addProperty("api_heavy_damaged", checkresult);
                setCurrentApiData(battleResultInfo);
                helper.putValue(DB_KEY_BATTLEINFO, battleResultInfo.toString());

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", "");
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

                    Log.e("KCA", KcaUtils.format("Escape: %d with %d", api_escape_target, api_tow_target));
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
}
