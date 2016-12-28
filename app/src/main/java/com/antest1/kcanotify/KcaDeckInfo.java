package com.antest1.kcanotify;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import java.util.ArrayList;
import java.util.List;

public class KcaDeckInfo {
    // Formula 33 (2016.12.26)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/deckbuilder.html
    public static double getSeekValue(JsonArray deckPortData, int deckid, int Cn) {
        double pureTotalSeek = 0.0;
        double totalSeek = 0.0;

        double totalShipSeek = 0.0;
        double totalEquipSeek = 0.0;
        double hqPenalty = 0.0;
        double noShipBonus = 0;

        int userLevel = KcaApiData.getLevel();
        hqPenalty = Math.ceil(0.4*userLevel);

        int noShipCount = 6;
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                noShipCount -= 1;
                JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "slot,sakuteki");
                int shipSeek = shipData.get("sakuteki").getAsJsonArray().get(0).getAsInt();
                pureTotalSeek += shipSeek;

                JsonArray shipItem = (JsonArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = KcaApiData.getUserItemStatusById (item_id, "level,alv", "name,type,saku");
                        String itemName = itemData.get("name").getAsString();
                        int itemLevel = itemData.get("level").getAsInt();
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        int itemSeek = itemData.get("saku").getAsInt();
                        shipSeek -= itemSeek;

                        switch (itemType) {
                            case KcaApiData.T2_TORPEDO_BOMBER:
                                totalEquipSeek += 0.8 * itemSeek;
                                break;
                            case KcaApiData.T2_SCOUT:
                                totalEquipSeek += 1 * itemSeek;
                                break;
                            case KcaApiData.T2_SCOUT_II:
                                totalEquipSeek += 1 * itemSeek;
                                break;
                            case KcaApiData.T2_SEA_SCOUT:
                                totalEquipSeek += 1.2 * (itemSeek + 1.2 * Math.sqrt(itemLevel));
                                break;
                            case KcaApiData.T2_SEA_BOMBER:
                                totalEquipSeek += 1.1 * itemSeek;
                                break;
                            case KcaApiData.T2_RADAR_LARGE:
                                totalEquipSeek += 0.6 * (itemSeek + 1.4 * Math.sqrt(itemLevel));
                                break;
                            case KcaApiData.T2_RADAR_SMALL:
                                totalEquipSeek += 0.6 * (itemSeek + 1.25 * Math.sqrt(itemLevel));
                                break;
                            default:
                                totalEquipSeek += 0.6 * itemSeek;
                                break;
                        }
                    }
                }
                totalShipSeek += Math.sqrt(shipSeek);
            }
        }
        noShipBonus =  2*noShipCount;
        totalSeek = totalShipSeek + Cn * totalEquipSeek - hqPenalty + noShipBonus;
        return Math.floor(totalSeek*100)/100;
    }

    public static String getConditionStatus(JsonArray deckPortData, int deckid) {
        String getConditionInfo = "";
        List<String> conditionList = new ArrayList<String>();
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "cond");
                int shipCondition = shipData.get("cond").getAsInt();
                conditionList.add(String.valueOf(shipCondition));
            }
        }
        getConditionInfo = joinStr(conditionList, "/");
        return getConditionInfo;

    }

    // Air Power Calculation (2016.12.27)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/aircrafts.html
    private static double calcBasicAAC(double aac, int carry) {
        return Math.sqrt(carry) * aac;
    }

    private static double calcReinforcedAAC(int type, int aac, int reinforce) {
        switch (type) {
            case KcaApiData.T2_FIGHTER:
                return aac + 0.2 * reinforce;
            case KcaApiData.T2_BOMBER:
            case KcaApiData.T2_JET_BOMBER:
                if (aac > 0) {
                    return aac + 0.25 * reinforce;
                } else {
                    return 0;
                }
        }
        return aac;
    }

    private static double[] calcSlotAACFromMastery(int type, int mastery, int mode) {
        int minMastery = mastery;
        if (mode == 1) {
            minMastery = 0;
        }
        double[] rangeAAC = {0.0, 0.0};
        switch (type) {
            case KcaApiData.T2_FIGHTER:
            case KcaApiData.T2_SEA_FIGHTER:
                rangeAAC[0] += KcaApiData.FIGHTER_MASTERY_BONUS[minMastery];
                rangeAAC[1] += KcaApiData.FIGHTER_MASTERY_BONUS[mastery];
                rangeAAC[0] += Math.sqrt(KcaApiData.BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(KcaApiData.BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case KcaApiData.T2_BOMBER:
            case KcaApiData.T2_TORPEDO_BOMBER:
            case KcaApiData.T2_JET_BOMBER:
                rangeAAC[0] += Math.sqrt(KcaApiData.BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(KcaApiData.BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case KcaApiData.T2_SEA_BOMBER:
                rangeAAC[0] += KcaApiData.SEA_BOMBER_MASTERY_BONUS[minMastery];
                rangeAAC[1] += KcaApiData.SEA_BOMBER_MASTERY_BONUS[mastery];
                rangeAAC[0] += Math.sqrt(KcaApiData.BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(KcaApiData.BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case KcaApiData.T2_SEA_SCOUT:
            case KcaApiData.T2_SCOUT:
            default:
                break;
        }
        return rangeAAC;
    }

    public static int[] getAirPowerRange(JsonArray deckPortData, int deckid) {
        int[] totalRangeAAC = {0, 0};

        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "ship_id,lv,slot,onslot");
                int shipKcId = shipData.get("ship_id").getAsInt();
                int shipLv = shipData.get("lv").getAsInt();
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                JsonArray shipSlotCount = (JsonArray) shipData.get("onslot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    int slot = shipSlotCount.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = KcaApiData.getUserItemStatusById(item_id, "level,alv", "name,type,tyku");
                        String itemName = itemData.get("name").getAsString();
                        int itemLevel = itemData.get("level").getAsInt();
                        int itemMastery = 0;
                        if (itemData.has("alv")) {
                            itemMastery = itemData.get("alv").getAsInt();
                        }
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        int itemAAC = itemData.get("tyku").getAsInt();
                        double baseAAC = calcBasicAAC(calcReinforcedAAC(itemType, itemAAC, itemLevel), slot);
                        double[] masteryAAC = calcSlotAACFromMastery(itemType, itemMastery, 0);

                        totalRangeAAC[0] += (int)Math.floor(baseAAC + masteryAAC[0]);
                        totalRangeAAC[1] += (int)Math.floor(baseAAC + masteryAAC[1]);
                    }
                }
            }
        }
        return totalRangeAAC;
    }

    public static int getSpeed(JsonArray deckPortData, int deckid) {
        boolean is_fast_flag = true;
        boolean is_slow_flag = true;

        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().get("api_ship").getAsJsonArray();
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "ship_id");
                int shipKcId = shipData.get("ship_id").getAsInt();
                JsonObject shipKcData = KcaApiData.getKcShipDataById(shipKcId, "soku");
                int soku = shipKcData.get("soku").getAsInt();
                if (soku == KcaApiData.SPEED_FAST) {
                    is_slow_flag = false;
                } else if (soku == KcaApiData.SPEED_SLOW) {
                    is_fast_flag = false;
                }
                if (!is_slow_flag && !is_fast_flag) {
                    return KcaApiData.SPEED_MIXED;
                }
            }
        }
        if(is_fast_flag) {
            return KcaApiData.SPEED_FAST;
        } else if(is_slow_flag) {
            return KcaApiData.SPEED_SLOW;
        }
        return KcaApiData.SPEED_NONE; // Unreachable
    }

    public static void debugPortInfo(JsonArray deckPortData, int deckid) {
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "ship_id,lv,slot,onslot");
                int shipKcId = shipData.get("ship_id").getAsInt();
                int shipLv = shipData.get("lv").getAsInt();
                JsonObject shipKcData = KcaApiData.getKcShipDataById(shipKcId, "name");
                String shipName = shipKcData.get("name").getAsString();
                Log.e("KCA", String.format("%s (%s)", shipName, shipLv));
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                JsonArray shipSlotCount = (JsonArray) shipData.get("onslot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    int slot = shipSlotCount.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = KcaApiData.getUserItemStatusById(item_id, "level,alv", "name,type,tyku");
                        String itemName = itemData.get("name").getAsString();
                        int itemLevel = itemData.get("level").getAsInt();
                        int itemMastery = 0;
                        if (itemData.has("alv")) {
                            itemMastery = itemData.get("alv").getAsInt();
                        }
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        Log.e("KCA", String.format("- %s %d %d %d", itemName, itemLevel, itemMastery, slot));
                    }
                }
            }
        }
    }

    public static int checkHeavyDamageExist(JsonArray deckPortData, int deckid) {
        int[] status = {0, 0, 0, 0, 0, 0};
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = KcaApiData.getUserShipDataById(shipId, "nowhp,maxhp,slot,slot_ex");
                int shipNowHp = shipData.get("nowhp").getAsInt();
                int shipMaxHp = shipData.get("maxhp").getAsInt();
                if (shipNowHp * 4 <= shipMaxHp) {
                    status[i] = 2;
                }

                JsonArray shipItem = (JsonArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = KcaApiData.getUserItemStatusById(item_id, "id", "type");
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        if (itemType == KcaApiData.T2_DAMECON && status[i] != 0) {
                            status[i] = 1;
                        }
                    }
                }
                int ex_item_id = shipData.get("slot_ex").getAsInt();
                if(ex_item_id != 0) {
                    JsonObject itemData = KcaApiData.getUserItemStatusById(ex_item_id, "id", "type");
                    int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                    if (itemType == KcaApiData.T2_DAMECON && status[i] != 0) {
                        status[i] = 1;
                    }
                }
            }
        }

        int heavyExist = 0;
        for(int i=0; i<status.length; i++) {
            heavyExist = Math.max(heavyExist, status[i]);
        }
        return heavyExist;
    }

    // TODO: Damecon Check

    private static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }

}
