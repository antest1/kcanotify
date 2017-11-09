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

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;

public class KcaBattle {
    public static JsonObject deckportdata = null;
    public static JsonObject currentApiData = null;

    public static JsonArray ship_ke = null;
    public static JsonArray ship_ke_combined = null;

    public static int[] maxhps = new int[13];
    public static int[] nowhps = new int[13];
    public static int[] afterhps = new int[13];

    public static int[] maxcbhps = new int[13];
    public static int[] nowcbhps = new int[13];
    public static int[] aftercbhps = new int[13];

    public static JsonObject escapedata = null;
    public static List<Integer> escapelist = new ArrayList<Integer>();
    public static List<Integer> escapecblist = new ArrayList<Integer>();

    public static boolean[] dameconflag = new boolean[7];
    public static boolean[] dameconcbflag = new boolean[7];

    private static Gson gson = new Gson();
    public static Handler sHandler;

    public static int currentMapArea = -1;
    public static int currentMapNo = -1;
    public static int currentNode = -1;
    public static int currentEventMapRank = 0;

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
        for (int i = 1; i < maxhps.length; i++) {
            if (maxhps[i] == -1 || i > 6) {
                return status;
            } else if (nowhps[i] * 4 <= maxhps[i]) {
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
        for (int i = 1; i < maxhps.length; i++) {
            if (maxhps[i] == -1 || i > 6) {
                break;
            } else if (nowhps[i] * 4 <= maxhps[i] && !escapelist.contains(i)) {
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
        for (int i = 1; i < maxcbhps.length; i++) {
            if (maxcbhps[i] == -1 || i > 6) {
                break;
            } else if (i == 1) {
                continue; // first ship never sunk
            } else if (nowcbhps[i] * 4 <= maxcbhps[i] && !escapecblist.contains(i)) {
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

    public static void cleanData() {
        for (int i = 0; i < maxhps.length; i++) {
            maxhps[i] = -1;
        }
        for (int i = 0; i < nowhps.length; i++) {
            nowhps[i] = -1;
        }
        for (int i = 0; i < afterhps.length; i++) {
            afterhps[i] = -1;
        }
    }

    public static void cleanCbData() {
        for (int i = 0; i < maxcbhps.length; i++) {
            maxcbhps[i] = -1;
        }
        for (int i = 0; i < nowcbhps.length; i++) {
            nowcbhps[i] = -1;
        }
        for (int i = 0; i < aftercbhps.length; i++) {
            aftercbhps[i] = -1;
        }
    }

    public static int getFriendIdx(int i) {
        return i;
    }

    public static int getEnemyIdx(int i) {
        return i + 6;
    }

    public static int getFriendCbIdx(int i) {
        return i - 6;
    }

    public static int getEnemyCbIdx(int i) {
        return i;
    }

    public static int cnv(JsonElement value) {
        Float f = value.getAsFloat();
        return f.intValue();
    }

    public static void cleanEscapeList() {
        escapedata = null;
        escapelist.clear();
        escapecblist.clear();
    }

    public static boolean[] getEscapeFlag() {
        boolean[] flag = new boolean[13];
        for (int i = 0; i < escapelist.size(); i++) {
            flag[escapelist.get(i)] = true;
        }
        for (int i = 0; i < escapecblist.size(); i++) {
            flag[escapecblist.get(i) + 6] = true;
        }
        return flag;
    }

    public static boolean isKeyExist(JsonObject data, String key) {
        return (data.has(key) && !data.get(key).isJsonNull());
    }

    public static boolean isMainFleetInNight(int[] afterhps, int[] nowhps, int[] aftercbhps, int[] nowcbhps) {
        int[] enemyAfterHps = Ints.concat(Arrays.copyOfRange(afterhps, 7, 13), Arrays.copyOfRange(aftercbhps, 7, 13));
        int[] enemyNowHps = Ints.concat(Arrays.copyOfRange(nowhps, 7, 13), Arrays.copyOfRange(nowcbhps, 7, 13));
        boolean[] enemySunk = new boolean[12];

        int enemyMainCount = 6;
        int enemyCbCount = 6;

        int enemyMainSunkCount = 0;
        int enemyCbSunkCount = 0;
        int enemyCbGoodHealth = 0;

        Log.e("KCA", "nb-enemyAfterHps " + Arrays.toString(enemyAfterHps));
        Log.e("KCA", "nb-enemyNowHps " + Arrays.toString(enemyNowHps));

        for (int i = 0; i < 6; i++) {
            if (enemyNowHps[i] == -1) {
                enemyMainCount -= 1;
            } else {
                enemySunk[i] = (enemyAfterHps[i] <= 0);
                if (enemySunk[i]) {
                    enemyMainSunkCount += 1;
                }
            }
        }

        for (int i = 6; i < 12; i++) {
            if (enemyNowHps[i] == -1) {
                enemyCbCount -= 1;
            } else {
                enemySunk[i] = (enemyAfterHps[i] <= 0);
                if (enemySunk[i]) {
                    enemyCbSunkCount += 1;
                } else {
                    if (enemyAfterHps[i] * 2 > enemyNowHps[i]) {
                        enemyCbGoodHealth += 1;
                    }
                }
            }
        }

        Log.e("KCA", "nb-enemyCbCount " + String.valueOf(enemyCbCount));
        Log.e("KCA", "nb-enemyCbSunkCount " + String.valueOf(enemyCbSunkCount));
        Log.e("KCA", "nb-enemyCbGoodHealth " + String.valueOf(enemyCbGoodHealth));

        if (!enemySunk[7] && enemyCbGoodHealth >= 2) {
            return false;
        } else if (enemyCbGoodHealth >= 3) {
            return false;
        } else return !(enemyMainSunkCount == enemyMainCount && enemyCbSunkCount != enemyCbCount);

    }

    public static JsonObject calculateRank(int[] afterhps, int[] nowhps, int[] aftercbhps, int[] nowcbhps) {
        JsonObject result = new JsonObject();

        int[] friendAfterHps = Ints.concat(Arrays.copyOfRange(afterhps, 1, 7), Arrays.copyOfRange(aftercbhps, 1, 7));
        int[] friendNowHps = Ints.concat(Arrays.copyOfRange(nowhps, 1, 7), Arrays.copyOfRange(nowcbhps, 1, 7));
        int[] enemyAfterHps = Ints.concat(Arrays.copyOfRange(afterhps, 7, 13), Arrays.copyOfRange(aftercbhps, 7, 13));
        int[] enemyNowHps = Ints.concat(Arrays.copyOfRange(nowhps, 7, 13), Arrays.copyOfRange(nowcbhps, 7, 13));

        boolean[] friendSunk = new boolean[12];
        boolean[] enemySunk = new boolean[12];

        int friendCount = 12;
        int enemyCount = 12;
        int friendSunkCount = 0;
        int enemySunkCount = 0;
        int friendNowSum = 0;
        int enemyNowSum = 0;
        int friendAfterSum = 0;
        int enemyAfterSum = 0;

        Log.e("KCA", "friendAfterHps " + Arrays.toString(friendAfterHps));
        Log.e("KCA", "friendNowHps " + Arrays.toString(friendNowHps));
        Log.e("KCA", "enemyAfterHps " + Arrays.toString(enemyAfterHps));
        Log.e("KCA", "enemyNowHps " + Arrays.toString(enemyNowHps));

        for (int i = 0; i < 12; i++) {
            if (friendNowHps[i] == -1) {
                friendCount -= 1;
            } else {
                friendSunk[i] = (friendAfterHps[i] <= 0);
                if (friendSunk[i]) friendSunkCount += 1;
                friendNowSum += friendNowHps[i];
                friendAfterSum += Math.max(0, friendAfterHps[i]);
            }
        }
        for (int i = 0; i < 12; i++) {
            if (enemyNowHps[i] == -1) {
                enemyCount -= 1;
            } else {
                enemySunk[i] = (enemyAfterHps[i] <= 0);
                if (enemySunk[i]) enemySunkCount += 1;
                enemyNowSum += enemyNowHps[i];
                enemyAfterSum += Math.max(0, enemyAfterHps[i]);
            }
        }

        int friendDamageRate = (friendNowSum - friendAfterSum) * 100 / friendNowSum;
        int enemyDamageRate = (enemyNowSum - enemyAfterSum) * 100 / enemyNowSum;

        Log.e("KCA", "friendCount " + String.valueOf(friendCount));
        Log.e("KCA", "enemyCount " + String.valueOf(enemyCount));
        Log.e("KCA", "friendSunkCount " + String.valueOf(friendSunkCount));
        Log.e("KCA", "enemySunkCount " + String.valueOf(enemySunkCount));

        Log.e("KCA", "friendDamageRate " + String.valueOf(friendDamageRate));
        Log.e("KCA", "enemyDamageRate " + String.valueOf(enemyDamageRate));

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
        if (!result.has("rank") && enemySunk[0] && friendSunkCount < enemySunkCount) {
            result.addProperty("rank", JUDGE_B);
        }
        if (!result.has("rank") && (friendCount == 1) && (friendAfterHps[0] * 4 <= friendNowHps[0])) {
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

    public static JsonObject calculateLdaRank(int[] afterhps, int[] nowhps, int[] aftercbhps, int[] nowcbhps) {
        JsonObject result = new JsonObject();

        int[] friendAfterHps = Ints.concat(Arrays.copyOfRange(afterhps, 1, 7), Arrays.copyOfRange(aftercbhps, 1, 7));
        int[] friendNowHps = Ints.concat(Arrays.copyOfRange(nowhps, 1, 7), Arrays.copyOfRange(nowcbhps, 1, 7));

        boolean[] friendSunk = new boolean[12];
        int friendCount = 12;
        int friendSunkCount = 0;
        int friendNowSum = 0;
        int friendAfterSum = 0;

        for (int i = 0; i < 12; i++) {
            if (friendNowHps[i] == -1) {
                friendCount -= 1;
            } else {
                friendSunk[i] = (friendAfterHps[i] <= 0);
                if (friendSunk[i]) friendSunkCount += 1;
                friendNowSum += friendNowHps[i];
                friendAfterSum += Math.max(0, friendAfterHps[i]);
            }
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
        cleanData();
        cleanCbData();
        deckportdata = api_data;
        JsonArray apiDeckData = deckportdata.getAsJsonArray("api_deck_data");
        JsonArray apiShipData = deckportdata.getAsJsonArray("api_ship_data");

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
            maxhps[i + 1] = deckMaxHpData.get(firstDeck.get(i).getAsString()).getAsInt();
            nowhps[i + 1] = deckNowHpData.get(firstDeck.get(i).getAsString()).getAsInt();
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
                maxcbhps[i + 1] = deckMaxHpData.get(secondDeck.get(i).getAsString()).getAsInt();
                nowcbhps[i + 1] = deckNowHpData.get(secondDeck.get(i).getAsString()).getAsInt();
            }
        }

        Log.e("KCA", Arrays.toString(maxhps));
        Log.e("KCA", Arrays.toString(nowhps));
        Log.e("KCA", Arrays.toString(afterhps));
        Log.e("KCA", Arrays.toString(maxcbhps));
        Log.e("KCA", Arrays.toString(nowcbhps));
        Log.e("KCA", Arrays.toString(aftercbhps));
    }

    public static void calculateAirBattle(JsonObject data) {
        if (isKeyExist(data, "api_stage3")) {
            JsonObject api_stage3 = data.getAsJsonObject("api_stage3");
            if (isKeyExist(api_stage3, "api_fdam")) {
                JsonArray api_fdam = api_stage3.getAsJsonArray("api_fdam");
                for (int i = 1; i < api_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    afterhps[f_idx] -= cnv(api_fdam.get(i));
                }
            }
            if (isKeyExist(api_stage3, "api_edam")) {
                JsonArray api_edam = api_stage3.getAsJsonArray("api_edam");
                for (int i = 1; i < api_edam.size(); i++) {
                    int e_idx = getEnemyIdx(i);
                    afterhps[e_idx] -= cnv(api_edam.get(i));
                }
            }
        }
        if (isKeyExist(data, "api_stage3_combined")) {
            JsonObject api_stage3_combined = data.getAsJsonObject("api_stage3_combined");
            if (isKeyExist(api_stage3_combined, "api_fdam")) {
                JsonArray api_fdam = api_stage3_combined.getAsJsonArray("api_fdam");
                for (int i = 1; i < api_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    aftercbhps[f_idx] -= cnv(api_fdam.get(i));
                }
            }
            if (isKeyExist(api_stage3_combined, "api_edam")) {
                JsonArray api_edam = api_stage3_combined.getAsJsonArray("api_edam");
                for (int i = 1; i < api_edam.size(); i++) {
                    int e_idx = getEnemyIdx(i);
                    aftercbhps[e_idx] -= cnv(api_edam.get(i));
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

                JsonObject battleResultInfo = new JsonObject();
                battleResultInfo.addProperty("data", checkcbresult);
                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                bundle.putString("data", gson.toJson(battleResultInfo));
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);

                JsonObject nodeInfo = api_data;
                JsonArray api_escape = (JsonArray) new JsonParser().parse(gson.toJson(escapelist));
                JsonArray api_escape_combined = (JsonArray) new JsonParser().parse(gson.toJson(escapecblist));

                nodeInfo.addProperty("api_url", url);
                nodeInfo.add("api_deck_port", deckportdata);
                nodeInfo.add("api_escape", api_escape);
                nodeInfo.add("api_escape_combined", api_escape_combined);
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
                cleanData();
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

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
                    for (int d = 1; d < damage.size(); d++) {
                        int e_idx = getEnemyIdx(d);
                        afterhps[e_idx] -= cnv(damage.get(d));
                    }
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list").getAsJsonArray();
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage").getAsJsonArray();
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                        }
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack").getAsJsonObject();
                    JsonArray openingattack_fdam = openingattack.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = openingattack.getAsJsonArray("api_edam");
                    for (int i = 1; i < openingattack_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(openingattack_fdam.get(i));
                        afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                    }
                }

                //Log.e("KCA", "hpInfo (start): " + Arrays.toString(afterhps));

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                        for (int i = 1; i < df_list.size(); i++) {
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                                //Log.e("KCA", KcaUtils.format("hpInfo (hougeki to %d, %d): ",cnv(target.get(t)), cnv(target_dmg.get(t))) + Arrays.toString(afterhps));
                            }
                        }
                    }
                    //Log.e("KCA", "hpInfo (hougeki): " + Arrays.toString(afterhps));
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki").getAsJsonObject();
                    JsonArray openingattack_fdam = raigeki.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = raigeki.getAsJsonArray("api_edam");
                    for (int i = 1; i < openingattack_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(openingattack_fdam.get(i));
                        afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                    }
                }

                String hpInfo = Arrays.toString(afterhps);
                Log.e("KCA", "hpInfo: " + hpInfo);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_deck_port", deckportdata);
                battleResultInfo.add("api_afterhps", api_afterhps);
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
                }

                cleanData();
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");
                JsonArray afterhpsData = api_data.getAsJsonArray("api_nowhps");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = cnv(afterhpsData.get(i));
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            if (target_idx > 0) {
                                afterhps[target_idx] -= cnv(target_dmg.get(t));
                            }
                        }
                    }
                }
                String hpInfo = Arrays.toString(afterhps);
                Log.e("KCA", "hpInfo: " + hpInfo);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_deck_port", deckportdata);
                battleResultInfo.add("api_afterhps", api_afterhps);
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
                cleanData();
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }

                if (url.equals(API_REQ_SORTIE_AIRBATTLE)) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku2"));
                }

                String hpInfo = Arrays.toString(afterhps);
                Log.e("KCA", "hpInfo: " + hpInfo);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
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
                nowhps = afterhps;
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
                            bundle.putString("data", gson.toJson(heavyDamagedInfo));
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
                    qtrackData.add("afterhp", new JsonArray());
                    for (int v: afterhps) qtrackData.getAsJsonArray("afterhp").add(v);
                    if (ship_ke_combined != null) {
                        qtrackData.addProperty("combined_flag", true);
                        qtrackData.add("ship_ke_combined", ship_ke_combined);
                        qtrackData.add("aftercbhp", new JsonArray());
                        for (int v: aftercbhps) qtrackData.getAsJsonArray("aftercbhp").add(v);
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
                cleanData();
                cleanCbData();
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                int combined_type = 0;
                if (url.equals(API_REQ_COMBINED_BATTLE)) combined_type = COMBINED_A;
                else if (url.equals(API_REQ_COMBINED_BATTLE_WATER)) combined_type = COMBINED_W;
                //else if(url.equals(API_REQ_COMBINED_BATTLE	))	combined_type = COMBINED_D;

                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                JsonArray maxcbhpsData = api_data.getAsJsonArray("api_maxhps_combined");
                JsonArray nowcbhpsData = api_data.getAsJsonArray("api_nowhps_combined");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < maxcbhpsData.size(); i++) {
                    maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                    nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                    aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
                }

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
                    for (int d = 1; d < damage.size(); d++) {
                        int e_idx = getEnemyIdx(d);
                        afterhps[e_idx] -= cnv(damage.get(d));
                    }
                }

                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list");
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            afterhps[target_idx] -= cnv(target_dmg.get(t));
                        }
                    }
                }

                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    JsonArray openingattack_fdam = openingattack.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = openingattack.getAsJsonArray("api_edam");
                    for (int i = 1; i < openingattack_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                        afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                    }
                }

                // 포격전
                for (int n = 1; n <= 3; n++) {
                    String api_name = KcaUtils.format("api_hougeki%d", n);
                    if (isKeyExist(api_data, api_name)) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                        for (int i = 1; i < df_list.size(); i++) {
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                int target_idx = target.get(t).getAsInt();
                                if (combined_type == COMBINED_A) {
                                    if (n == 1) {
                                        if (target_idx <= 6) {
                                            aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                        } else {
                                            afterhps[target_idx] -= cnv(target_dmg.get(t));
                                        }
                                    } else {
                                        afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                                    }
                                } else if (combined_type == COMBINED_W) {
                                    if (n == 3) {
                                        if (target_idx <= 6) {
                                            aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                        } else {
                                            afterhps[target_idx] -= cnv(target_dmg.get(t));
                                        }
                                    } else {
                                        afterhps[target_idx] -= cnv(target_dmg.get(t));
                                    }
                                }
                            }
                        }
                        String hpInfo1 = Arrays.toString(afterhps);
                        String hpInfo2 = Arrays.toString(aftercbhps);
                        Log.e("KCA", "hpInfo: " + hpInfo1 + " / " + hpInfo2);
                    }
                }

                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    JsonArray openingattack_fdam = raigeki.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = raigeki.getAsJsonArray("api_edam");
                    for (int i = 1; i < openingattack_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                        afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                    }
                }

                String hpInfo1 = Arrays.toString(afterhps);
                String hpInfo2 = Arrays.toString(aftercbhps);
                Log.e("KCA", "hpInfo: " + hpInfo1 + " / " + hpInfo2);

                //String hpInfo1 = Arrays.toString(afterhps);
                //String hpInfo2 = Arrays.toString(aftercbhps);
                //Log.e("KCA", "hpInfo: " + hpInfo1 + " / " + hpInfo2);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                JsonArray api_afterhps_combined = (JsonArray) new JsonParser().parse(gson.toJson(aftercbhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);
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
                //else if(url.equals(API_REQ_COMBINED_BATTLE	))	combined_type = COMBINED_D;

                cleanData();
                cleanCbData();

                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                JsonArray maxcbhpsData = api_data.getAsJsonArray("api_maxhps_combined");
                JsonArray nowcbhpsData = api_data.getAsJsonArray("api_nowhps_combined");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < maxcbhpsData.size(); i++) {
                    maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                    nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                    aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
                }

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
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 항공전 Stage 3
                if (isKeyExist(api_data, "api_kouku")) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));

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
                    for (int d = 1; d < damage.size(); d++) {
                        int e_idx;
                        if (d <= 6) {
                            e_idx = getEnemyIdx(d);
                            afterhps[e_idx] -= cnv(damage.get(d));
                        } else {
                            e_idx = getEnemyCbIdx(d);
                            aftercbhps[e_idx] -= cnv(damage.get(d));
                        }
                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 선제대잠
                if (isKeyExist(api_data, "api_opening_taisen")) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray at_eflag = opening_taisen.getAsJsonArray("api_at_eflag");
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list");
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            if (eflag == 0) {
                                int e_idx = -1;
                                if (target_idx <= 6) {
                                    e_idx = getEnemyIdx(target_idx);
                                    afterhps[e_idx] -= cnv(target_dmg.get(t));
                                } else {
                                    e_idx = getEnemyCbIdx(target_idx);
                                    aftercbhps[e_idx] -= cnv(target_dmg.get(t));
                                }
                            } else {
                                int f_idx = -1;
                                if (target_idx <= 6) {
                                    f_idx = getFriendIdx(target_idx);
                                    afterhps[f_idx] -= cnv(target_dmg.get(t));
                                } else {
                                    f_idx = getFriendCbIdx(target_idx);
                                    aftercbhps[f_idx] -= cnv(target_dmg.get(t));
                                }
                            }
                        }
                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 개막뇌격
                if (isKeyExist(api_data, "api_opening_atack")) {
                    JsonObject openingattack = api_data.getAsJsonObject("api_opening_atack");
                    JsonArray openingattack_fdam = openingattack.getAsJsonArray("api_fdam");
                    JsonArray openingattack_edam = openingattack.getAsJsonArray("api_edam");
                    for (int i = 1; i < openingattack_fdam.size(); i++) {
                        if (i <= 6) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(openingattack_fdam.get(i));
                            afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                        } else {
                            int f_idx = getFriendIdx(i - 6);
                            int e_idx = getEnemyIdx(i - 6);
                            aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                            aftercbhps[e_idx] -= cnv(openingattack_edam.get(i));
                        }

                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
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

                        for (int i = 1; i < df_list.size(); i++) {
                            int eflag = at_eflag.get(i).getAsInt();
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                int target_idx = target.get(t).getAsInt();
                                int damage_value = target_dmg.get(t).getAsInt();
                                if (n == first_phase) {
                                    if (eflag == 0) {
                                        target_idx = getEnemyIdx(target_idx);
                                    } else {
                                        target_idx = getFriendIdx(target_idx);
                                    }
                                    afterhps[target_idx] -= damage_value;
                                } else if (n == second_phase) {
                                    if (eflag == 0) {
                                        target_idx = getEnemyCbIdx(target_idx);
                                        aftercbhps[target_idx] -= damage_value;
                                    } else {
                                        if (combined_type == COMBINED_A || combined_type == COMBINED_W) {
                                            target_idx = getFriendCbIdx(target_idx);
                                            aftercbhps[target_idx] -= damage_value;
                                        } else {
                                            target_idx = getFriendIdx(target_idx);
                                            afterhps[target_idx] -= damage_value;
                                        }
                                    }
                                } else if (n == all_phase) {
                                    if (eflag == 0) {
                                        if (target_idx > 6) {
                                            target_idx = getEnemyCbIdx(target_idx);
                                            aftercbhps[target_idx] -= damage_value;
                                        } else if (target_idx != -1) {
                                            target_idx = getEnemyIdx(target_idx);
                                            afterhps[target_idx] -= damage_value;
                                        }
                                    } else {
                                        if (target_idx > 6) {
                                            target_idx = getFriendCbIdx(target_idx);
                                            aftercbhps[target_idx] -= damage_value;
                                        } else if (target_idx != -1) {
                                            target_idx = getFriendIdx(target_idx);
                                            afterhps[target_idx] -= damage_value;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                }
                //Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 폐막뇌격
                if (isKeyExist(api_data, "api_raigeki")) {
                    JsonObject raigeki = api_data.getAsJsonObject("api_raigeki");
                    JsonArray raigeki_fdam = raigeki.getAsJsonArray("api_fdam");
                    JsonArray raigeki_edam = raigeki.getAsJsonArray("api_edam");

                    for (int i = 1; i < raigeki_fdam.size(); i++) {
                        if (i <= 6) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(raigeki_fdam.get(i));
                            afterhps[e_idx] -= cnv(raigeki_edam.get(i));
                        } else {
                            int f_idx = getFriendIdx(i - 6);
                            int e_idx = getEnemyIdx(i - 6);
                            aftercbhps[f_idx] -= cnv(raigeki_fdam.get(i));
                            aftercbhps[e_idx] -= cnv(raigeki_edam.get(i));
                        }
                    }
                }

                String hpInfo1 = Arrays.toString(afterhps);
                String hpInfo2 = Arrays.toString(aftercbhps);
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                JsonArray api_afterhps_combined = (JsonArray) new JsonParser().parse(gson.toJson(aftercbhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);
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
                cleanData();
                cleanCbData();
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                JsonArray maxcbhpsData = api_data.getAsJsonArray("api_maxhps_combined");
                JsonArray nowcbhpsData = api_data.getAsJsonArray("api_nowhps_combined");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < maxcbhpsData.size(); i++) {
                    maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                    nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                    aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
                }

                // 제1항공전 Stage 3
                calculateAirBattle(api_data.getAsJsonObject("api_kouku"));
                if (url.equals(API_REQ_COMBINED_AIRBATTLE)) {
                    calculateAirBattle(api_data.getAsJsonObject("api_kouku2"));
                }
                String hpInfo1 = Arrays.toString(afterhps);
                String hpInfo2 = Arrays.toString(aftercbhps);
                Log.e("KCA", "hpInfo: " + hpInfo1 + hpInfo2);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                JsonArray api_afterhps_combined = (JsonArray) new JsonParser().parse(gson.toJson(aftercbhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);
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
                // reset maxhps_combined data
                if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT)) {
                    for (int i = 0; i < api_data.getAsJsonArray("api_maxhps_combined").size(); i++) {
                        api_data.getAsJsonArray("api_maxhps_combined").set(i, new JsonPrimitive(maxcbhps[i]));
                    }
                }

                if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_SP)) {
                    ship_ke = api_data.getAsJsonArray("api_ship_ke");
                }

                cleanData();
                cleanCbData();

                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");
                JsonArray maxcbhpsData = api_data.getAsJsonArray("api_maxhps_combined");
                JsonArray nowcbhpsData = api_data.getAsJsonArray("api_nowhps_combined");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < nowhpsData.size(); i++) {
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < maxcbhpsData.size(); i++) {
                    maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < nowcbhpsData.size(); i++) {
                    nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                    aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
                }
                String hpInfob = Arrays.toString(afterhps);
                String hpcbInfob = Arrays.toString(aftercbhps);
                Log.e("KCA", "hpInfo: " + hpInfob);
                Log.e("KCA", "hpcbInfo: " + hpcbInfob);

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            if (target_idx > 6) {
                                afterhps[target_idx] -= cnv(target_dmg.get(t));
                            } else if (target_idx != -1) {
                                aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                            }
                        }
                    }
                }

                String hpInfo = Arrays.toString(afterhps);
                String hpcbInfo = Arrays.toString(aftercbhps);
                Log.e("KCA", "hpInfo: " + hpInfo);
                Log.e("KCA", "hpcbInfo: " + hpcbInfo);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                JsonArray api_afterhps_combined = (JsonArray) new JsonParser().parse(gson.toJson(aftercbhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);
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
                for (int i = 0; i < api_data.getAsJsonArray("api_maxhps_combined").size(); i++) {
                    api_data.getAsJsonArray("api_maxhps_combined").set(i, new JsonPrimitive(maxcbhps[i]));
                }

                cleanData();
                cleanCbData();
                ship_ke = api_data.getAsJsonArray("api_ship_ke");
                ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");

                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                JsonArray maxcbhpsData = api_data.getAsJsonArray("api_maxhps_combined");
                JsonArray nowcbhpsData = api_data.getAsJsonArray("api_nowhps_combined");

                JsonArray activeDeckData = api_data.getAsJsonArray("api_active_deck");
                int[] activedeck = {0, 0};
                for (int i = 0; i < activeDeckData.size(); i++) {
                    activedeck[i] = activeDeckData.get(i).getAsInt();
                }

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                for (int i = 0; i < maxcbhpsData.size(); i++) {
                    maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                    nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                    aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
                }

                if (isKeyExist(api_data, "api_hougeki")) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            int damage = cnv(target_dmg.get(t));
                            if (target_idx != -1) {
                                if (target_idx <= 6) {
                                    if (KcaBattle.isCombined) aftercbhps[target_idx] -= damage;
                                    else afterhps[target_idx] -= damage;
                                } else {
                                    if (activedeck[1] == 1) afterhps[target_idx] -= damage;
                                    else aftercbhps[target_idx] -= damage;
                                }
                            }
                        }
                    }
                }

                String hpInfo1 = Arrays.toString(afterhps);
                String hpInfo2 = Arrays.toString(aftercbhps);
                Log.e("KCA", "hpInfo: " + hpInfo1 + "/" + hpInfo2);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                JsonArray api_afterhps_combined = (JsonArray) new JsonParser().parse(gson.toJson(aftercbhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);
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
                nowhps = afterhps;
                nowcbhps = aftercbhps;

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
                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_DROPINFO);
                bundle.putString("data", gson.toJson(dropInfo));
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);

                JsonObject qtrackData = new JsonObject();
                qtrackData.addProperty("world", currentMapArea);
                qtrackData.addProperty("map", currentMapNo);
                qtrackData.addProperty("isboss", isBossReached);
                qtrackData.add("ship_ke", ship_ke);
                qtrackData.add("deck_port", deckportdata);
                qtrackData.add("afterhp", new JsonArray());
                for (int v: afterhps) qtrackData.getAsJsonArray("afterhp").add(v);
                if (ship_ke_combined != null) {
                    qtrackData.addProperty("combined_flag", true);
                    qtrackData.add("ship_ke_combined", ship_ke_combined);
                    qtrackData.add("aftercbhp", new JsonArray());
                    for (int v: aftercbhps) qtrackData.getAsJsonArray("aftercbhp").add(v);
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
                        if (!escapecblist.contains(api_escape_target - 6)) {
                            escapecblist.add(api_escape_target - 6);
                        }
                    } else {
                        if (!escapelist.contains(api_escape_target)) {
                            escapelist.add(api_escape_target);
                        }
                    }

                    if (!escapecblist.contains(api_tow_target - 6)) {
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
