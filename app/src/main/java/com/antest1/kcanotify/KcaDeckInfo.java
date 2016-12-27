package com.antest1.kcanotify;

import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KcaDeckInfo {
    // Formula 33 (2016.12.26)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/deckbuilder.html
    public static double getSeekValue(JSONArray deckPortData, int deckid, int Cn) {
        double pureTotalSeek = 0.0;
        double totalSeek = 0.0;

        double totalShipSeek = 0.0;
        double totalEquipSeek = 0.0;
        double hqPenalty = 0.0;
        double noShipBonus = 0;

        int userLevel = KcaApiData.getLevel();
        hqPenalty = Math.ceil(0.4*userLevel);

        int noShipCount = 6;
        JSONArray deckShipIdList = (JSONArray) ((JSONObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = intv(deckShipIdList.get(i));
            if (shipId != -1) {
                noShipCount -= 1;
                JSONObject shipData = KcaApiData.getUserShipDataById(shipId, "slot,sakuteki");
                int shipSeek = intv(((JSONArray) shipData.get("sakuteki")).get(0));
                pureTotalSeek += shipSeek;

                JSONArray shipItem = (JSONArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = intv(shipItem.get(j));
                    if (item_id != -1) {
                        JSONObject itemData = KcaApiData.getUserItemStatusById (item_id, "level,alv", "name,type,saku");
                        String itemName = (String)itemData.get("name");
                        int itemLevel = intv(itemData.get("level"));
                        int itemType = intv(((JSONArray) itemData.get("type")).get(2));
                        int itemSeek = intv(itemData.get("saku"));
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

    public static String getConditionStatus(JSONArray deckPortData, int deckid) {
        String getConditionInfo = "";
        List<String> conditionList = new ArrayList<String>();
        JSONArray deckShipIdList = (JSONArray) ((JSONObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = intv(deckShipIdList.get(i));
            if (shipId != -1) {
                JSONObject shipData = KcaApiData.getUserShipDataById(shipId, "cond");
                int shipCondition = intv(shipData.get("cond"));
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

    public static int[] getAirPowerRange(JSONArray deckPortData, int deckid) {
        int[] totalRangeAAC = {0, 0};

        JSONArray deckShipIdList = (JSONArray) ((JSONObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = intv(deckShipIdList.get(i));
            if (shipId != -1) {
                JSONObject shipData = KcaApiData.getUserShipDataById(shipId, "slot,onslot");

                JSONArray shipItem = (JSONArray) shipData.get("slot");
                JSONArray shipSlotCount = (JSONArray) shipData.get("onslot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = intv(shipItem.get(j));
                    int slot = intv(shipSlotCount.get(j));
                    if (item_id != -1) {
                        JSONObject itemData = KcaApiData.getUserItemStatusById(item_id, "level,alv", "name,type,tyku");
                        String itemName = (String) itemData.get("name");
                        int itemLevel = intv(itemData.get("level"));
                        int itemMastery = 0;
                        if (itemData.containsKey("alv")) {
                            itemMastery = intv(itemData.get("alv"));
                        }
                        int itemType = intv(((JSONArray) itemData.get("type")).get(2));
                        int itemAAC = intv(itemData.get("tyku"));

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

    public static int getSpeed(JSONArray deckPortData, int deckid) {
        boolean is_fast_flag = true;
        boolean is_slow_flag = true;

        JSONArray deckShipIdList = (JSONArray) ((JSONObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = intv(deckShipIdList.get(i));
            if (shipId != -1) {
                JSONObject shipData = KcaApiData.getUserShipDataById(shipId, "ship_id");
                int shipKcId = intv(shipData.get("ship_id"));
                JSONObject shipKcData = KcaApiData.getKcShipDataById(shipKcId, "soku");
                int soku = intv(shipKcData.get("soku"));
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

    private static Integer intv(Object o) {
        return ((Long) o).intValue();
    }

}
