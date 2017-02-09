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

import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;

public class KcaBattle {
    public static JsonObject deckportdata = null;

    public static int[] maxhps = new int[13];
    public static int[] nowhps = new int[13];
    public static int[] afterhps = new int[13];

    public static int[] maxcbhps = new int[13];
    public static int[] nowcbhps = new int[13];
    public static int[] aftercbhps = new int[13];

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

    public static boolean isCombined = false;
    public static boolean isEndReached = false;
    public static boolean isBossReached = false;
    public static int startHeavyDamageExist;

    public static void setHandler(Handler h) {
        sHandler = h;
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

    public static boolean isMainFleetInNight(int[] aftercbhps, int[] nowcbhps) {
        int[] enemyAfterHps = Arrays.copyOfRange(aftercbhps, 7, 13);
        int[] enemyNowHps = Arrays.copyOfRange(nowcbhps, 7, 13);

        int enemyCount = 6;
        int enemySunkCount = 0;
        for (int i = 0; i < 6; i++) {
            if(enemyNowHps[i] == -1) {
                enemyCount -= 1;
            } else {
                if(enemyAfterHps[i] <= 0)
                    enemySunkCount += 1;
            }
        }

        if (enemyCount > 1 && (enemySunkCount >= (int)Math.floor(0.7*enemyCount))) { // A~SS
            return true;
        } else {
            return false;
        }
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
            if(friendNowHps[i] == -1) {
                friendCount -= 1;
            } else {
                friendSunk[i] = (friendAfterHps[i] <= 0);
                if (friendSunk[i]) friendSunkCount += 1;
                friendNowSum += friendNowHps[i];
                friendAfterSum += Math.max(0, friendAfterHps[i]);
            }
        }
        for (int i = 0; i < 12; i++) {
            if(enemyNowHps[i] == -1) {
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

        Log.e("KCA", "friendCount "+String.valueOf(friendCount));
        Log.e("KCA", "enemyCount "+String.valueOf(enemyCount));
        Log.e("KCA", "friendSunkCount "+String.valueOf(friendSunkCount));
        Log.e("KCA", "enemySunkCount "+String.valueOf(enemySunkCount));

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
                if ( friendAfterSum >= friendNowSum ) result.addProperty("rank", JUDGE_SS);
                else result.addProperty("rank", JUDGE_S);
            } else if (enemyCount > 1 && (enemySunkCount >= (int)Math.floor(0.7*enemyCount))) {
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
        if(!result.has("rank")) {
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
            if(friendNowHps[i] == -1) {
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
        
        if(friendAfterSum >= friendNowSum) result.addProperty("rank", JUDGE_SS);
        else if (friendDamageRate < 10 ) result.addProperty("rank", JUDGE_A);
        else if (friendDamageRate < 20 ) result.addProperty("rank", JUDGE_B);
        else if (friendDamageRate < 50 ) result.addProperty("rank", JUDGE_C);
        else if (friendDamageRate < 80 ) result.addProperty("rank", JUDGE_D);
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
        for (JsonElement element : apiDeckData.get(0).getAsJsonObject().getAsJsonArray("api_ship")) {
            if (element.getAsInt() != -1) {
                firstDeckSize += 1;
            } else {
                break;
            }
        }

        for (int i = 0; i < firstDeckSize; i++) {
            JsonObject shipData = apiShipData.get(i).getAsJsonObject();
            maxhps[i + 1] = shipData.get("api_maxhp").getAsInt();
            nowhps[i + 1] = shipData.get("api_nowhp").getAsInt();
        }
        if (apiDeckData.size() == 2) {
            int secondDeckSize = 0;
            for (JsonElement element : apiDeckData.get(1).getAsJsonObject().getAsJsonArray("api_ship")) {
                if (element.getAsInt() != -1) {
                    secondDeckSize += 1;
                } else {
                    break;
                }
            }
            for (int i = firstDeckSize; i < firstDeckSize + secondDeckSize; i++) {
                JsonObject shipData = apiShipData.get(i).getAsJsonObject();
                maxcbhps[i - firstDeckSize + 1] = shipData.get("api_maxhp").getAsInt();
                nowcbhps[i - firstDeckSize + 1] = shipData.get("api_nowhp").getAsInt();
            }
        }

        Log.e("KCA", Arrays.toString(maxhps));
        Log.e("KCA", Arrays.toString(nowhps));
        Log.e("KCA", Arrays.toString(afterhps));
        Log.e("KCA", Arrays.toString(maxcbhps));
        Log.e("KCA", Arrays.toString(nowcbhps));
        Log.e("KCA", Arrays.toString(aftercbhps));
    }

    public static void processData(String url, JsonObject api_data) {
        //
        try {
            // Log.e("KCA", "processData: "+url );
            if (url.equals(API_REQ_MAP_START)) {
                KcaApiData.resetShipCountInBattle();
                KcaApiData.resetItemCountInBattle();
                int checkresult = startHeavyDamageExist;
                if (checkresult != HD_NONE) {
                    JsonObject battleResultInfo = new JsonObject();
                    battleResultInfo.addProperty("data", checkresult);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                    bundle.putString("data", gson.toJson(battleResultInfo));
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }

                escapelist.clear();
                escapecblist.clear();
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

                JsonObject nodeInfo = api_data;
                nodeInfo.addProperty("api_url", url);
                nodeInfo.add("api_deck_port", deckportdata);
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_NODE);
                bundle.putString("data", gson.toJson(nodeInfo));
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_MAP_NEXT)) {
                int checkresult = checkHeavyDamagedExist();
                int checkcbresult = checkCombinedHeavyDamagedExist();
                if (checkresult != HD_NONE || checkcbresult != HD_NONE) {
                    Log.e("KCA", String.valueOf(checkresult) + " " + String.valueOf(checkcbresult));
                    JsonObject battleResultInfo = new JsonObject();
                    battleResultInfo.addProperty("data", checkresult);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                    bundle.putString("data", gson.toJson(battleResultInfo));
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }

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

                JsonObject nodeInfo = api_data;
                nodeInfo.addProperty("api_url", url);
                nodeInfo.add("api_deck_port", deckportdata);
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_NODE);
                bundle.putString("data", gson.toJson(nodeInfo));
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_SORTIE_BATTLE) || url.equals(API_REQ_PRACTICE_BATTLE)) {
                cleanData();
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                // 분식항공전 Stage 3
                if (api_data.has("api_injection_kouku") && !api_data.get("api_injection_kouku").isJsonNull()) { // Check Null for old data
                    JsonObject inj_kouku = api_data.getAsJsonObject("api_injection_kouku");
                    if (!inj_kouku.get("api_stage3").isJsonNull()) {
                        JsonObject inj_kouku_stage3 = inj_kouku.getAsJsonObject("api_stage3");
                        JsonArray inj_kouku_fdam = inj_kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray inj_kouku_edam = inj_kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                        }
                    }
                }
                // 항공전 Stage 3
                if (!api_data.get("api_kouku").isJsonNull()) {
                    JsonObject kouku = api_data.getAsJsonObject("api_kouku");
                    if (!kouku.get("api_stage3").isJsonNull()) {
                        JsonObject kouku_stage3 = kouku.getAsJsonObject("api_stage3");
                        JsonArray kouku_fdam = kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray kouku_edam = kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(kouku_edam.get(i));
                        }
                    }
                }

                // Log.e("KCA", "hpInfo (kouku): " + Arrays.toString(afterhps));

                // 지원함대
                if (api_data.has("api_support_info") && !api_data.get("api_support_info").isJsonNull()) {
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
                if (api_data.has("api_opening_taisen") && !api_data.get("api_opening_taisen").isJsonNull()) {
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
                if (!api_data.get("api_opening_atack").isJsonNull()) {
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
                    String api_name = String.format("api_hougeki%d", n);
                    if (!api_data.get(api_name).isJsonNull()) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                        for (int i = 1; i < df_list.size(); i++) {
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {
                                afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                                //Log.e("KCA", String.format("hpInfo (hougeki to %d, %d): ",cnv(target.get(t)), cnv(target_dmg.get(t))) + Arrays.toString(afterhps));
                            }
                        }
                    }
                    //Log.e("KCA", "hpInfo (hougeki): " + Arrays.toString(afterhps));
                }

                // 폐막뇌격
                if (!api_data.get("api_raigeki").isJsonNull()) {
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
                if(url.equals(API_REQ_PRACTICE_BATTLE)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }

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
                cleanData();
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");
                JsonArray afterhpsData = api_data.getAsJsonArray("api_nowhps");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = cnv(afterhpsData.get(i));
                }

                if (!api_data.get("api_hougeki").isJsonNull()) {
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
                if(url.equals(API_REQ_PRACTICE_MIDNIGHT_BATTLE)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }

            // 아웃레인지, 공습
            if (url.equals(API_REQ_SORTIE_AIRBATTLE) || url.equals(API_REQ_SORTIE_LDAIRBATTLE)) {
                cleanData();
                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                for (int i = 0; i < maxhpsData.size(); i++) {
                    maxhps[i] = maxhpsData.get(i).getAsInt();
                    nowhps[i] = nowhpsData.get(i).getAsInt();
                    afterhps[i] = nowhpsData.get(i).getAsInt();
                }

                // 항공전 Stage 3
                if (!api_data.get("api_kouku").isJsonNull()) {
                    JsonObject kouku = api_data.getAsJsonObject("api_kouku");
                    if (!kouku.get("api_stage3").isJsonNull()) {
                        JsonObject kouku_stage3 = kouku.getAsJsonObject("api_stage3");
                        JsonArray kouku_fdam = kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray kouku_edam = kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(kouku_edam.get(i));
                        }
                    }
                }

                if (url.equals(API_REQ_SORTIE_AIRBATTLE)) {
                    // 제2항공전 Stage 3
                    JsonObject kouku2 = api_data.getAsJsonObject("api_kouku2");
                    if (!kouku2.get("api_stage3").isJsonNull()) {
                        JsonObject kouku2_stage3 = kouku2.getAsJsonObject("api_stage3");
                        JsonArray kouku2_fdam = kouku2_stage3.getAsJsonArray("api_fdam");
                        JsonArray kouku2_edam = kouku2_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku2_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(kouku2_fdam.get(i));
                            afterhps[e_idx] -= cnv(kouku2_edam.get(i));
                        }
                    }

                }
                String hpInfo = Arrays.toString(afterhps);
                Log.e("KCA", "hpInfo: " + hpInfo);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);

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

                nowhps = afterhps;

                if(!isEndReached && url.equals(API_REQ_SORTIE_BATTLE_RESULT)) {
                    JsonObject heavyDamagedInfo = new JsonObject();
                    int checkresult = checkHeavyDamagedExist();
                    Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkresult));

                    if (checkresult != HD_NONE) {
                        heavyDamagedInfo.addProperty("data", checkresult);
                        bundle = new Bundle();
                        bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                        bundle.putString("data", gson.toJson(heavyDamagedInfo));
                        sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }

                    Log.e("KCA", String.valueOf(KcaApiData.checkUserItemMax()) + String.valueOf(KcaApiData.checkUserShipMax()));
                    Log.e("KCA", String.valueOf(KcaApiData.checkUserPortEnough()));

                    if (KcaApiData.checkUserPortEnough()) {
                        JsonObject dropInfo = new JsonObject();
                        dropInfo.addProperty("world", currentMapArea);
                        dropInfo.addProperty("map", currentMapNo);
                        dropInfo.addProperty("node", currentNode);
                        dropInfo.addProperty("rank", api_data.get("api_win_rank").getAsString());
                        dropInfo.addProperty("maprank", 0);
                        if (api_data.has("api_get_ship")) {
                            int api_ship_id = api_data.getAsJsonObject("api_get_ship").get("api_ship_id").getAsInt();
                            dropInfo.addProperty("result", api_ship_id);
                            KcaApiData.addShipCountInBattle();
                            KcaApiData.addItemCountInBattle(api_ship_id);
                        } else {
                            dropInfo.addProperty("result", 0);
                        }
                        bundle = new Bundle();
                        bundle.putString("url", KCA_API_NOTI_BATTLE_DROPINFO);
                        bundle.putString("data", gson.toJson(dropInfo));
                        sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }
                }

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                if(url.equals(API_REQ_PRACTICE_BATTLE_RESULT)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }

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

                if (api_data.has("api_escape_idx") && !api_data.get("api_escape_idx").isJsonNull()) {
                    JsonArray escapeIdx = api_data.getAsJsonArray("api_escape_idx");
                    for (int i = 0; i < escapeIdx.size(); i++) {
                        if (!escapelist.contains(cnv(escapeIdx.get(i)))) {
                            escapelist.add(i);
                        }
                    }
                }

                if (api_data.has("api_escape_idx_combined") && !api_data.get("api_escape_idx_combined").isJsonNull()) {
                    JsonArray escapeIdxCb = api_data.getAsJsonArray("api_escape_idx_combined");
                    for (int i = 0; i < escapeIdxCb.size(); i++) {
                        if (!escapecblist.contains(cnv(escapeIdxCb.get(i)))) {
                            escapecblist.add(i);
                        }
                    }
                }

                // 분식항공전 Stage 3
                if (api_data.has("api_injection_kouku") && !api_data.get("api_injection_kouku").isJsonNull()) { // Check Null for old data
                    JsonObject inj_kouku = api_data.getAsJsonObject("api_injection_kouku").getAsJsonObject();
                    if (!inj_kouku.get("api_stage3").isJsonNull()) {
                        JsonObject inj_kouku_stage3 = inj_kouku.getAsJsonObject("api_stage3");
                        JsonArray inj_kouku_fdam = inj_kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray inj_kouku_edam = inj_kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                        }
                    }
                    if (!inj_kouku.get("api_stage3_combined").isJsonNull()) {
                        JsonObject inj_kouku_stage3_combined = inj_kouku.get("api_stage3_combined").getAsJsonObject();
                        JsonArray inj_kouku_fdam_combined = inj_kouku_stage3_combined.getAsJsonArray("api_fdam");
                        JsonArray inj_kouku_edam_combined = inj_kouku_stage3_combined.getAsJsonArray("api_edam");
                        for (int i = 1; i < inj_kouku_fdam_combined.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            aftercbhps[f_idx] -= cnv(inj_kouku_fdam_combined.get(i));
                            aftercbhps[e_idx] -= cnv(inj_kouku_edam_combined.get(i));
                        }
                    }
                }

                // 기지항공대 Stage 3
                if (api_data.has("api_air_base_attack") && !api_data.get("api_air_base_attack").isJsonNull()) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        JsonObject airbase_attack_info = airbase_attack.get(i).getAsJsonObject();
                        JsonObject airbase_attack_stage3 = airbase_attack_info.getAsJsonObject("api_stage3");
                        JsonArray airbase_attack_edam = airbase_attack_stage3.getAsJsonArray("api_edam");
                        for (int j = 1; j < airbase_attack_edam.size(); j++) {
                            int e_idx = getEnemyIdx(j);
                            afterhps[e_idx] -= cnv(airbase_attack_edam.get(j));
                        }
                    }
                }

                // 항공전 Stage 3
                if (!api_data.get("api_kouku").isJsonNull()) {
                    JsonObject kouku = api_data.getAsJsonObject("api_kouku");
                    if (!kouku.get("api_stage3").isJsonNull()) {
                        JsonObject kouku_stage3 = kouku.getAsJsonObject("api_stage3");
                        JsonArray kouku_fdam = kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray kouku_edam = kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(kouku_edam.get(i));
                        }
                    }
                    if (!kouku.get("api_stage3_combined").isJsonNull()) {
                        JsonObject kouku_stage3_combined = kouku.get("api_stage3_combined").getAsJsonObject();
                        JsonArray kouku_fdam_combined = kouku_stage3_combined.getAsJsonArray("api_fdam");
                        for (int i = 1; i < kouku_fdam_combined.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            aftercbhps[f_idx] -= cnv(kouku_fdam_combined.get(i));
                        }
                    }
                }

                // 지원함대
                if (!api_data.get("api_support_info").isJsonNull()) {
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
                if (!api_data.get("api_opening_taisen").isJsonNull()) {
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
                if (!api_data.get("api_opening_atack").isJsonNull()) {
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
                    String api_name = String.format("api_hougeki%d", n);
                    if (!api_data.get(api_name).isJsonNull()) {
                        JsonObject hougeki = api_data.getAsJsonObject(api_name);
                        JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                        JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                        for (int i = 1; i < df_list.size(); i++) {
                            JsonArray target = df_list.get(i).getAsJsonArray();
                            JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                            for (int t = 0; t < target.size(); t++) {

                                if (combined_type == COMBINED_A) {
                                    if (n == 1) {
                                        int target_idx = cnv(target.get(t));
                                        if (target_idx <= 6) {
                                            aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                        } else {
                                            afterhps[target_idx] -= cnv(target_dmg.get(t));
                                        }
                                    } else {
                                        afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                                    }
                                } else if (combined_type == COMBINED_W) {
                                    int target_idx = target.get(t).getAsInt();
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
                    }
                }

                // 폐막뇌격
                if (!api_data.get("api_raigeki").isJsonNull()) {
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
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);

            }

            if (url.equals(API_REQ_COMBINED_GOBACKPORT)) {
                JsonObject battleResultInfo = new JsonObject();
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_GOBACKPORT);
                bundle.putString("data", gson.toJson(battleResultInfo));
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE_EC) || url.equals(API_REQ_COMBINED_BATTLE_EACH) || url.equals(API_REQ_COMBINED_BATTLE_EACH_WATER)) {
                int combined_type = 0;
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

                if (api_data.has("api_escape_idx") && !api_data.get("api_escape_idx").isJsonNull()) {
                    JsonArray escapeIdx = api_data.getAsJsonArray("api_escape_idx");
                    for (int i = 0; i < escapeIdx.size(); i++) {
                        if (!escapelist.contains(cnv(escapeIdx.get(i)))) {
                            escapelist.add(i);
                        }
                    }
                }

                if (api_data.has("api_escape_idx_combined") && !api_data.get("api_escape_idx_combined").isJsonNull()) {
                    JsonArray escapeIdxCb = api_data.getAsJsonArray("api_escape_idx_combined");
                    for (int i = 0; i < escapeIdxCb.size(); i++) {
                        if (!escapecblist.contains(cnv(escapeIdxCb.get(i)))) {
                            escapecblist.add(i);
                        }
                    }
                }

                // 분식항공전 Stage 3
                if (api_data.has("api_injection_kouku") && !api_data.get("api_injection_kouku").isJsonNull()) { // Check Null for old data
                    JsonObject inj_kouku = api_data.getAsJsonObject("api_injection_kouku");
                    if (!inj_kouku.get("api_stage3").isJsonNull()) {
                        JsonObject inj_kouku_stage3 = inj_kouku.getAsJsonObject("api_stage3");
                        JsonArray inj_kouku_fdam = inj_kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray inj_kouku_edam = inj_kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                        }
                    }
                    if (!inj_kouku.get("api_stage3_combined").isJsonNull()) {
                        JsonObject inj_kouku_stage3_combined = inj_kouku.getAsJsonObject("api_stage3_combined");
                        JsonArray inj_kouku_fdam_combined = inj_kouku_stage3_combined.getAsJsonArray("api_fdam");
                        JsonArray inj_kouku_edam_combined = inj_kouku_stage3_combined.getAsJsonArray("api_edam");
                        for (int i = 1; i < inj_kouku_fdam_combined.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            aftercbhps[f_idx] -= cnv(inj_kouku_fdam_combined.get(i));
                            aftercbhps[e_idx] -= cnv(inj_kouku_edam_combined.get(i));
                        }
                    }
                }
                
                // 기지항공대 Stage 3
                if (api_data.has("api_air_base_attack") && !api_data.get("api_air_base_attack").isJsonNull()) {
                    JsonArray airbase_attack = api_data.getAsJsonArray("api_air_base_attack");
                    for (int i = 0; i < airbase_attack.size(); i++) {
                        JsonObject airbase_attack_info = airbase_attack.get(i).getAsJsonObject();
                        if (!airbase_attack_info.get("api_stage3").isJsonNull()) {
                            JsonObject airbase_attack_stage3 = airbase_attack_info.getAsJsonObject("api_stage3");
                            JsonArray airbase_attack_edam = airbase_attack_stage3.getAsJsonArray("api_edam");
                            for (int j = 1; j < airbase_attack_edam.size(); j++) {
                                int e_idx = getEnemyIdx(j);
                                afterhps[e_idx] -= cnv(airbase_attack_edam.get(j));
                            }
                        }
                        if (!airbase_attack_info.get("api_stage3_combined").isJsonNull()) {
                            JsonObject airbase_attack_stage3_combined = airbase_attack_info.getAsJsonObject("api_stage3_combined");
                            JsonArray airbase_attack_edam_combined = airbase_attack_stage3_combined.getAsJsonArray("api_edam");
                            for (int j = 1; j < airbase_attack_edam_combined.size(); j++) {
                                int e_idx = getEnemyIdx(j);
                                aftercbhps[e_idx] -= cnv(airbase_attack_edam_combined.get(j));
                            }
                        }
                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 항공전 Stage 3
                if (!api_data.get("api_kouku").isJsonNull()) {
                    JsonObject kouku = api_data.getAsJsonObject("api_kouku");
                    if (!kouku.get("api_stage3").isJsonNull()) {
                        JsonObject kouku_stage3 = kouku.getAsJsonObject("api_stage3");
                        JsonArray kouku_fdam = kouku_stage3.getAsJsonArray("api_fdam");
                        JsonArray kouku_edam = kouku_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                            afterhps[e_idx] -= cnv(kouku_edam.get(i));
                        }
                    }
                    if (!kouku.get("api_stage3_combined").isJsonNull()) {
                        JsonObject kouku_stage3_combined = kouku.getAsJsonObject("api_stage3_combined");
                        JsonArray kouku_fdam_combined = kouku_stage3_combined.getAsJsonArray("api_fdam");
                        JsonArray kouku_edam_combined = kouku_stage3_combined.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku_fdam_combined.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            aftercbhps[f_idx] -= cnv(kouku_fdam_combined.get(i));
                            aftercbhps[e_idx] -= cnv(kouku_edam_combined.get(i));
                        }
                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 지원함대
                if (!api_data.get("api_support_info").isJsonNull()) {
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
                        int e_idx;
                        if (d <= 6) {
                            e_idx = getEnemyIdx(d);
                            afterhps[e_idx] -= cnv(damage.get(d));
                        } else {
                            e_idx = getEnemyIdx(d - 6);
                            aftercbhps[e_idx] -= cnv(damage.get(d));
                        }
                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 선제대잠
                if (api_data.has("api_opening_taisen") && !api_data.get("api_opening_taisen").isJsonNull()) {
                    JsonObject opening_taisen = api_data.getAsJsonObject("api_opening_taisen");
                    JsonArray df_list = opening_taisen.getAsJsonArray("api_df_list");
                    JsonArray df_damage = opening_taisen.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                        }
                    }
                }
                Log.e("KCA", "hpInfo: " + Arrays.toString(afterhps) + " / " + Arrays.toString(aftercbhps));
                // 개막뇌격
                if (api_data.has("api_opening_atack") && !api_data.get("api_opening_atack").isJsonNull()) {
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
                    String api_name = String.format("api_hougeki%d", n);
                    if (!api_data.get(api_name).isJsonNull()) {
                        JsonObject hougeki = api_data.getAsJsonObject(String.format("api_hougeki%d", n));
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
                                        } else {
                                            target_idx = getEnemyIdx(target_idx);
                                            afterhps[target_idx] -= damage_value;
                                        }
                                    } else {
                                        if (target_idx > 6) {
                                            target_idx = getFriendCbIdx(target_idx);
                                            aftercbhps[target_idx] -= damage_value;
                                        } else {
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
                if (api_data.has("api_raigeki") && !api_data.get("api_raigeki").isJsonNull()) {
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

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }

            // 아웃레인지, 공습
            if (url.equals(API_REQ_COMBINED_AIRBATTLE) || url.equals(API_REQ_COMBINED_LDAIRBATTLE)) {
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

                if (api_data.has("api_escape_idx") && !api_data.get("api_escape_idx").isJsonNull()) {
                    JsonArray escapeIdx = api_data.getAsJsonArray("api_escape_idx");
                    for (int i = 0; i < escapeIdx.size(); i++) {
                        if (!escapelist.contains(cnv(escapeIdx.get(i)))) {
                            escapelist.add(i);
                        }
                    }
                }

                if (api_data.has("api_escape_idx_combined") && !api_data.get("api_escape_idx_combined").isJsonNull()) {
                    JsonArray escapeIdxCb = api_data.getAsJsonArray("api_escape_idx_combined");
                    for (int i = 0; i < escapeIdxCb.size(); i++) {
                        if (!escapecblist.contains(cnv(escapeIdxCb.get(i)))) {
                            escapecblist.add(i);
                        }
                    }
                }
                // 제1항공전 Stage 3
                JsonObject kouku = api_data.getAsJsonObject("api_kouku");
                if (!kouku.get("api_stage3").isJsonNull()) {
                    JsonObject kouku_stage3 = kouku.getAsJsonObject("api_stage3");
                    JsonArray kouku_fdam = kouku_stage3.getAsJsonArray("api_fdam");
                    JsonArray kouku_edam = kouku_stage3.getAsJsonArray("api_edam");
                    for (int i = 1; i < kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(kouku_edam.get(i));
                    }
                }
                if (!kouku.get("api_stage3_combined").isJsonNull()) {
                    JsonObject kouku_stage3_combined = kouku.getAsJsonObject("api_stage3_combined");
                    JsonArray kouku_fdam = kouku_stage3_combined.getAsJsonArray("api_fdam");
                    for (int i = 1; i < kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        aftercbhps[f_idx] -= cnv(kouku_fdam.get(i));
                    }
                }

                if (url.equals(API_REQ_COMBINED_AIRBATTLE)) {
                    // 제2항공전 Stage 3
                    JsonObject kouku2 = api_data.getAsJsonObject("api_kouku2");

                    if (!kouku2.get("api_stage3").isJsonNull()) {
                        JsonObject kouku2_stage3 = kouku2.getAsJsonObject("api_stage3");
                        JsonArray kouku2_fdam = kouku2_stage3.getAsJsonArray("api_fdam");
                        JsonArray kouku2_edam = kouku2_stage3.getAsJsonArray("api_edam");
                        for (int i = 1; i < kouku2_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            int e_idx = getEnemyIdx(i);
                            afterhps[f_idx] -= cnv(kouku2_fdam.get(i));
                            afterhps[e_idx] -= cnv(kouku2_edam.get(i));
                        }
                    }

                    if (!kouku2.get("api_stage3_combined").isJsonNull()) {
                        JsonObject kouku2_stage3_combined = kouku2.getAsJsonObject("api_stage3_combined");
                        JsonArray kouku_fdam = kouku2_stage3_combined.getAsJsonArray("api_fdam");
                        for (int i = 1; i < kouku_fdam.size(); i++) {
                            int f_idx = getFriendIdx(i);
                            aftercbhps[f_idx] -= cnv(kouku_fdam.get(i));
                        }
                    }
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

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT) || url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_SP)) {
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

                if (!api_data.get("api_hougeki").isJsonNull()) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = target.get(t).getAsInt();
                            if (i > 6) {
                                afterhps[target_idx] -= cnv(target_dmg.get(t));
                            } else {
                                aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                            }
                        }
                    }
                }

                String hpInfo = Arrays.toString(afterhps);
                Log.e("KCA", "hpInfo: " + hpInfo);

                JsonObject battleResultInfo = api_data;
                JsonArray api_afterhps = (JsonArray) new JsonParser().parse(gson.toJson(afterhps));
                JsonArray api_afterhps_combined = (JsonArray) new JsonParser().parse(gson.toJson(aftercbhps));
                battleResultInfo.addProperty("api_url", url);
                battleResultInfo.add("api_afterhps", api_afterhps);
                battleResultInfo.add("api_afterhps_combined", api_afterhps_combined);

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_EC)) {
                cleanData();
                cleanCbData();

                JsonArray maxhpsData = api_data.getAsJsonArray("api_maxhps");
                JsonArray nowhpsData = api_data.getAsJsonArray("api_nowhps");

                JsonArray maxcbhpsData = api_data.getAsJsonArray("api_maxhps_combined");
                JsonArray nowcbhpsData = api_data.getAsJsonArray("api_nowhps_combined");

                JsonArray activeDeckData = api_data.getAsJsonArray("api_active_deck");
                int[] activedeck = {0, 0};
                for (int i = 0; i < activeDeckData.size(); i++) {
                    activedeck[i] = cnv(activeDeckData.get(i));
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

                if (!api_data.get("api_hougeki").isJsonNull()) {
                    JsonObject hougeki = api_data.getAsJsonObject("api_hougeki");
                    JsonArray df_list = hougeki.getAsJsonArray("api_df_list");
                    JsonArray df_damage = hougeki.getAsJsonArray("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = df_list.get(i).getAsJsonArray();
                        JsonArray target_dmg = df_damage.get(i).getAsJsonArray();
                        for (int t = 0; t < target.size(); t++) {
                            int target_idx = cnv(target.get(t));
                            if (activedeck[1] == 1) {
                                afterhps[target_idx] -= cnv(target_dmg.get(t));
                            } else {
                                aftercbhps[target_idx] -= cnv(target_dmg.get(t));
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

                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);
            }

            if (url.equals(API_REQ_COMBINED_BATTLERESULT)) {
                nowhps = afterhps;
                nowcbhps = aftercbhps;

                Bundle bundle;
                Message sMsg;

                if (!isEndReached) {
                    int checkresult = checkCombinedHeavyDamagedExist();
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
                    KcaApiData.addShipCountInBattle();
                    KcaApiData.addItemCountInBattle(api_ship_id);
                } else {
                    dropInfo.addProperty("result", 0);
                }
                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_DROPINFO);
                bundle.putString("data", gson.toJson(dropInfo));
                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);
                sHandler.sendMessage(sMsg);

                JsonObject battleResultInfo = api_data;
                battleResultInfo.addProperty("api_url", url);
                if(url.equals(API_REQ_PRACTICE_BATTLE_RESULT)) {
                    battleResultInfo.addProperty("api_practice_flag", true);
                }

                bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_BATTLE_INFO);
                bundle.putString("data", battleResultInfo.toString());

                sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }
        } catch (Exception e) {
            JsonObject errorInfo = new JsonObject();
            String currentNodeAlphabet = KcaApiData.getCurrentNodeAlphabet(currentMapArea, currentMapNo, currentNode);
            try {
                errorInfo.addProperty("api_data", api_data.toString());
                errorInfo.addProperty("api_url", URLEncoder.encode(url, "utf-8"));
                errorInfo.addProperty("api_node", URLEncoder.encode(String.format("%d-%d-%s", currentMapArea, currentMapNo, currentNodeAlphabet), "utf-8"));
                errorInfo.addProperty("api_error", getStringFromException(e));
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_PROCESS_BATTLE_FAILED);
            bundle.putString("data", gson.toJson(errorInfo));
            Message sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);

        }
    }
}
