package com.antest1.kcanotify;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.*;
import static com.antest1.kcanotify.KcaConstants.SEEK_PURE;
import static com.antest1.kcanotify.KcaUtils.joinStr;


public class KcaDeckInfo {
    public static final int SPEEDFLAG_SLOW = 1 << 3;
    public static final int SPEEDFLAG_FAST = 1 << 2;
    public static final int SPEEDFLAG_FASTPLUS = 1 << 1;
    public static final int SPEEDFLAG_SUPERFAST = 1 << 0;
    public static KcaDBHelper helper;

    public static void setDBHelper(KcaDBHelper hp) {
        helper = hp;
    }

    // Formula 33 (2016.12.26)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/deckbuilder.html
    public static double getSeekValue(JsonArray deckPortData, int deckid, int Cn, boolean[] exclude_flag) {
        double pureTotalSeek = 0.0;
        double totalSeek = 0.0;

        double totalShipSeek = 0.0;
        double totalEquipSeek = 0.0;
        double hqPenalty = 0.0;
        double noShipBonus = 0;

        int userLevel = getLevel();
        hqPenalty = Math.ceil(0.4 * userLevel);

        int noShipCount = 6;
        boolean excludeflagexist = (exclude_flag != null);
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            if (excludeflagexist && exclude_flag[i]) continue;
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                noShipCount -= 1;
                JsonObject shipData = getUserShipDataById(shipId, "slot,sakuteki");
                int shipSeek = shipData.get("sakuteki").getAsJsonArray().get(0).getAsInt();
                pureTotalSeek += shipSeek;
                if (Cn != SEEK_PURE) {
                    JsonArray shipItem = (JsonArray) shipData.get("slot");
                    for (int j = 0; j < shipItem.size(); j++) {
                        int item_id = shipItem.get(j).getAsInt();
                        if (item_id != -1) {
                            JsonObject itemData = getUserItemStatusById(helper, item_id, "level,alv", "name,type,saku");
                            if (itemData == null) continue; // TODO: will be removed after item null issue resolved
                            String itemName = itemData.get("name").getAsString();
                            int itemLevel = itemData.get("level").getAsInt();
                            int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                            int itemSeek = itemData.get("saku").getAsInt();
                            shipSeek -= itemSeek;

                            switch (itemType) {
                                case T2_TORPEDO_BOMBER:
                                    totalEquipSeek += 0.8 * itemSeek;
                                    break;
                                case T2_SCOUT:
                                    totalEquipSeek += 1 * itemSeek;
                                    break;
                                case T2_SCOUT_II:
                                    totalEquipSeek += 1 * itemSeek;
                                    break;
                                case T2_SEA_SCOUT:
                                    totalEquipSeek += 1.2 * (itemSeek + 1.2 * Math.sqrt(itemLevel));
                                    break;
                                case T2_SEA_BOMBER:
                                    totalEquipSeek += 1.1 * itemSeek;
                                    break;
                                case T2_RADAR_LARGE:
                                    totalEquipSeek += 0.6 * (itemSeek + 1.4 * Math.sqrt(itemLevel));
                                    break;
                                case T2_RADAR_SMALL:
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
        }

        if (Cn == SEEK_PURE) {
            return pureTotalSeek;
        } else {
            noShipBonus = 2 * noShipCount;
            totalSeek = totalShipSeek + Cn * totalEquipSeek - hqPenalty + noShipBonus;

            return Math.floor(totalSeek * 100) / 100;
        }
    }

    public static String getConditionStatus(JsonArray deckPortData, int deckid) {
        String getConditionInfo = "";
        List<String> conditionList = new ArrayList<String>();
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "cond");
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
    private static double calcBasicAAC(int type, double aac, int carry) {
        if (Arrays.binarySearch(T2LIST_FIGHT_AIRCRAFTS, type) < 0) {
            return 0;
        } else {
            return Math.sqrt(carry) * aac;
        }
    }

    private static double calcReinforcedAAC(int type, int aac, int reinforce) {
        switch (type) {
            case T2_FIGHTER:
                return aac + 0.2 * reinforce;
            case T2_BOMBER:
            case T2_JET_BOMBER:
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
            case T2_FIGHTER:
            case T2_SEA_FIGHTER:
                rangeAAC[0] += FIGHTER_MASTERY_BONUS[minMastery];
                rangeAAC[1] += FIGHTER_MASTERY_BONUS[mastery];
                rangeAAC[0] += Math.sqrt(BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case T2_BOMBER:
            case T2_TORPEDO_BOMBER:
            case T2_JET_BOMBER:
                rangeAAC[0] += Math.sqrt(BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case T2_SEA_BOMBER:
                rangeAAC[0] += SEA_BOMBER_MASTERY_BONUS[minMastery];
                rangeAAC[1] += SEA_BOMBER_MASTERY_BONUS[mastery];
                rangeAAC[0] += Math.sqrt(BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case T2_SEA_SCOUT:
            case T2_SCOUT:
            default:
                break;
        }
        return rangeAAC;
    }

    public static int[] getAirPowerRange(JsonArray deckPortData, int deckid, boolean[] exclude_flag) {
        int[] totalRangeAAC = {0, 0};
        boolean excludeflagexist = (exclude_flag != null);
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int excludeflagid = deckid * 6 + i + 1; // 1 ~ 13
            if (excludeflagexist && exclude_flag[excludeflagid]) continue;
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "ship_id,lv,slot,onslot");
                int shipKcId = shipData.get("ship_id").getAsInt();
                int shipLv = shipData.get("lv").getAsInt();
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                JsonArray shipSlotCount = (JsonArray) shipData.get("onslot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    int slot = shipSlotCount.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(helper, item_id, "level,alv", "name,type,tyku");
                        if (itemData == null) continue; // TODO: will be removed after item null issue resolved
                        String itemName = itemData.get("name").getAsString();
                        int itemLevel = itemData.get("level").getAsInt();
                        int itemMastery = 0;
                        if (itemData.has("alv")) {
                            itemMastery = itemData.get("alv").getAsInt();
                        }
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        int itemAAC = itemData.get("tyku").getAsInt();
                        double baseAAC = calcBasicAAC(itemType, calcReinforcedAAC(itemType, itemAAC, itemLevel), slot);
                        double[] masteryAAC = calcSlotAACFromMastery(itemType, itemMastery, 0);

                        totalRangeAAC[0] += (int) Math.floor(baseAAC + masteryAAC[0]);
                        totalRangeAAC[1] += (int) Math.floor(baseAAC + masteryAAC[1]);
                    }
                }
            }
        }
        return totalRangeAAC;
    }

    public static int getSpeedFlagValue(boolean s, boolean f, boolean fp, boolean sf) {
        int value = 0;
        if (s) value += SPEEDFLAG_SLOW;
        if (f) value += SPEEDFLAG_FAST;
        if (fp) value += SPEEDFLAG_FASTPLUS;
        if (sf) value += SPEEDFLAG_SUPERFAST;
        return value;
    }


    public static int getSpeed(JsonArray deckPortData, String deckid_list, boolean[] exclude_flag) {
        boolean is_slow_flag = false;
        boolean is_fast_flag = false;
        boolean is_fastplus_flag = false;
        boolean is_superfast_flag = false;

        boolean excludeflagexist = (exclude_flag != null);

        String[] decklist = deckid_list.split(",");

        for (int n = 0; n < decklist.length; n++) {
            int deckid = Integer.parseInt(decklist[n]);
            JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().get("api_ship").getAsJsonArray();

            // Retrieve Speed (soku) Information
            for (int i = 0; i < deckShipIdList.size(); i++) {
                int excludeflagid = deckid * 6 + i + 1; // 1 ~ 13
                if (excludeflagexist && exclude_flag[excludeflagid]) continue;
                int shipId = deckShipIdList.get(i).getAsInt();
                if (shipId != -1) {
                    JsonObject shipData = getUserShipDataById(shipId, "soku");
                    int soku = shipData.get("soku").getAsInt();
                    if (soku == SPEED_SLOW) {
                        is_slow_flag = true;
                    } else if (soku == SPEED_FAST) {
                        is_fast_flag = true;
                    } else if (soku == SPEED_FASTPLUS) {
                        is_fastplus_flag = true;
                    } else if (soku == SPEED_SUPERFAST) {
                        is_superfast_flag = true;
                    }
                }
            }
        }


        int flag = getSpeedFlagValue(is_slow_flag, is_fast_flag, is_fastplus_flag, is_superfast_flag);
        if (flag > SPEEDFLAG_SLOW) {
            return SPEED_MIXED_NORMAL;
        } else if (flag == SPEEDFLAG_SLOW) {
            return SPEED_SLOW;
        } else if (flag > SPEEDFLAG_FAST) {
            return SPEED_MIXED_FAST;
        } else if (flag == SPEEDFLAG_FAST) {
            return SPEED_FAST;
        } else if (flag > SPEEDFLAG_FASTPLUS) {
            return SPEED_MIXED_FASTPLUS;
        } else if (flag == SPEEDFLAG_FASTPLUS) {
            return SPEED_FASTPLUS;
        } else if (flag == SPEEDFLAG_SUPERFAST) {
            return SPEED_SUPERFAST;
        } else {
            return SPEED_NONE; // Unreachable
        }
    }

    public static int[] getTPValue(JsonArray deckPortData, String deckid_list, boolean[] exclude_flag) {
        double totalTP = 0.0;

        boolean excludeflagexist = (exclude_flag != null);

        String[] decklist = deckid_list.split(",");

        for (int n = 0; n < decklist.length; n++) {
            int deckid = Integer.parseInt(decklist[n]);
            JsonArray deckShipIdList = ((JsonObject) deckPortData.get(deckid)).getAsJsonArray("api_ship");
            for (int i = 0; i < deckShipIdList.size(); i++) {
                int excludeflagid = deckid * 6 + i + 1; // 1 ~ 13
                if (excludeflagexist && exclude_flag[excludeflagid]) continue;
                int shipId = deckShipIdList.get(i).getAsInt();
                if (shipId != -1) {
                    JsonObject shipData = getUserShipDataById(shipId, "slot,ship_id");
                    int kcShipId = shipData.get("ship_id").getAsInt();
                    JsonObject kcShipData = getKcShipDataById(kcShipId, "stype");
                    int stype = kcShipData.get("stype").getAsInt();

                    switch (stype) {
                        case STYPE_DD:
                            totalTP += 5;
                            break;
                        case STYPE_CL:
                            totalTP += 2;
                            break;
                        case STYPE_CAV:
                            totalTP += 4;
                            break;
                        case STYPE_BBV:
                            totalTP += 7;
                            break;
                        case STYPE_AV:
                            totalTP += 9;
                            break;
                        case STYPE_LHA:
                            totalTP += 12;
                            break;
                        case STYPE_AS:
                            totalTP += 7;
                            break;
                        case STYPE_CT:
                            totalTP += 6;
                            break;
                        case STYPE_AO:
                            totalTP += 15;
                            break;
                        case STYPE_SSV:
                            totalTP += 1;
                        default:
                            break;
                    }

                    if (kcShipId == 487) { // kinu kai ni
                        totalTP += 8;
                    }

                    JsonArray shipItem = (JsonArray) shipData.get("slot");
                    for (int j = 0; j < shipItem.size(); j++) {
                        int item_id = shipItem.get(j).getAsInt();
                        if (item_id != -1) {
                            JsonObject itemData = getUserItemStatusById(helper, item_id, "level", "type");
                            if (itemData == null) continue; // TODO: will be removed after item null issue resolved
                            int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();

                            switch (itemType) {
                                case T2_DRUM_CAN:
                                    totalTP += 5.0;
                                    break;
                                case T2_LANDING_CRAFT:
                                    totalTP += 8.0;
                                    break;
                                case T2_AMP_TANK:
                                    totalTP += 2.0;
                                    break;
                                case T2_COMBAT_FOOD:
                                    totalTP += 1.0;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        int TP_s = (int) Math.floor(totalTP);
        int TP_a = (int) Math.floor(TP_s * 0.7f);
        int[] estimatedTP = {TP_s, TP_a};
        return estimatedTP;
    }

    public static int[] getKcShipList(JsonArray deckPortData, int deckid) {
        int[] kcShipList = new int[6];
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "ship_id");
                kcShipList[i] = shipData.get("ship_id").getAsInt();
            }
        }
        return kcShipList;
    }

    public static void debugPortInfo(JsonArray deckPortData, int deckid) {
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "ship_id,lv,slot,onslot");
                int shipKcId = shipData.get("ship_id").getAsInt();
                int shipLv = shipData.get("lv").getAsInt();
                JsonObject shipKcData = getKcShipDataById(shipKcId, "name");
                String shipName = shipKcData.get("name").getAsString();
                Log.e("KCA", String.format("%s (%s)", shipName, shipLv));
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                JsonArray shipSlotCount = (JsonArray) shipData.get("onslot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    int slot = shipSlotCount.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(helper, item_id, "level,alv", "name,type,tyku");
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
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "nowhp,maxhp,slot,slot_ex");
                int shipNowHp = shipData.get("nowhp").getAsInt();
                int shipMaxHp = shipData.get("maxhp").getAsInt();
                if (shipNowHp * 4 <= shipMaxHp) {
                    status[i] = 2;
                }

                JsonArray shipItem = (JsonArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(helper, item_id, "id", "type");
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        if (itemType == T2_DAMECON && status[i] != 0) {
                            status[i] = 1;
                        }
                    }
                }
                int ex_item_id = shipData.get("slot_ex").getAsInt();
                if (ex_item_id != 0 && ex_item_id != -1) {
                    Log.e("KCA", String.valueOf(ex_item_id));
                    JsonObject itemData = getUserItemStatusById(helper, ex_item_id, "id", "type");
                    int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                    if (itemType == T2_DAMECON && status[i] != 0) {
                        status[i] = 1;
                    }
                }
            }
        }

        int heavyExist = 0;
        for (int i = 0; i < status.length; i++) {
            heavyExist = Math.max(heavyExist, status[i]);
        }
        return heavyExist;
    }

    public static boolean[] getDameconStatus(JsonArray deckPortData, int deckid) {
        boolean[] dameconStatus = {false, false, false, false, false, false, false};
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int idx = i + 1;
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "slot,slot_ex");
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(helper, item_id, "id", "type");
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        if (itemType == T2_DAMECON) {
                            dameconStatus[idx] = true;
                        }
                    }
                }
                int ex_item_id = shipData.get("slot_ex").getAsInt();
                if (ex_item_id != 0 && ex_item_id != -1) {
                    JsonObject itemData = getUserItemStatusById(helper, ex_item_id, "id", "type");
                    int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                    if (itemType == T2_DAMECON) {
                        dameconStatus[idx] = true;
                    }
                }
            }
        }
        return dameconStatus;
    }

    public static boolean checkNotSuppliedExist(JsonArray deckPortData, int deckid) {
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "ship_id,fuel,bull");
                int fuel = shipData.get("fuel").getAsInt();
                int bull = shipData.get("bull").getAsInt();
                int kcShipId = shipData.get("ship_id").getAsInt();
                JsonObject kcShipData = getKcShipDataById(kcShipId, "fuel_max,bull_max");
                int fuel_max = kcShipData.get("fuel_max").getAsInt();
                int bull_max = kcShipData.get("bull_max").getAsInt();
                if (fuel_max != fuel || bull_max != bull) {
                    return true;
                }
            }
        }
        return false;
    }
}
