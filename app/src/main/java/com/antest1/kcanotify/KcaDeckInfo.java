package com.antest1.kcanotify;

import android.content.Context;
import android.support.annotation.IntegerRes;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.*;
import static com.antest1.kcanotify.KcaApiData.helper;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_DEFENSE;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_SORTIE;
import static com.antest1.kcanotify.KcaConstants.PREF_HDNOTI_LOCKED;
import static com.antest1.kcanotify.KcaConstants.PREF_HDNOTI_MINLEVEL;
import static com.antest1.kcanotify.KcaConstants.SEEK_PURE;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.KcaUtils.setDefaultGameData;


public class KcaDeckInfo {
    private static final int SPEEDFLAG_SLOW = 1 << 3;
    private static final int SPEEDFLAG_FAST = 1 << 2;
    private static final int SPEEDFLAG_FASTPLUS = 1 << 1;
    private static final int SPEEDFLAG_SUPERFAST = 1 << 0;
    private KcaDBHelper helper;
    private Context ac, bc;

    public KcaDeckInfo(Context a, Context b) {
        this.ac = a;
        this.bc = b;
        helper = new KcaDBHelper(ac, null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(helper);
        setDefaultGameData(a, helper);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(ac, bc, id);
    }

    // Formula 33 (2016.12.26)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/deckbuilder.html
    public JsonObject getEachSeekValue(JsonArray deckShipIdList, int id, int Cn, JsonObject exclude_flag) {
        JsonObject data = new JsonObject();
        double pureTotalSeek = 0.0;
        double totalEquipSeek = 0.0;
        double totalShipSeek = 0.0;
        int noShipCount = 6;

        boolean excludeflagexist = (exclude_flag != null);
        JsonArray excludeflagdata = null;
        if (excludeflagexist) {
            if (id == 0) excludeflagdata = exclude_flag.getAsJsonArray("escape");
            if (id == 1) excludeflagdata = exclude_flag.getAsJsonArray("escapecb");
        }

        for (int i = 0; i < deckShipIdList.size(); i++) {
            if (excludeflagdata != null && excludeflagdata.contains(new JsonPrimitive(i + 1)))
                continue;

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
                            JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "name,type,saku");
                            if (itemData == null) continue;
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
                                    totalEquipSeek += itemSeek + 1.2 * Math.sqrt(itemLevel);
                                    break;
                                case T2_SCOUT_II:
                                    totalEquipSeek += itemSeek + 1.2 * Math.sqrt(itemLevel);
                                    break;
                                case T2_SEA_SCOUT:
                                    totalEquipSeek += 1.2 * (itemSeek + 1.2 * Math.sqrt(itemLevel));
                                    break;
                                case T2_SEA_BOMBER:
                                    totalEquipSeek += 1.1 * (itemSeek + 1.15 * Math.sqrt(itemLevel));
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
        data.addProperty("ship", totalShipSeek);
        data.addProperty("equip", totalEquipSeek);
        data.addProperty("pure", pureTotalSeek);
        data.addProperty("nscount", noShipCount);
        return data;
    }

    public double getSeekValue(JsonArray deckPortData, String deckid_list, int Cn, JsonObject exclude_flag) {
        double pureTotalSeek = 0.0;
        double totalSeek = 0.0;

        double totalShipSeek = 0.0;
        double totalEquipSeek = 0.0;
        double hqPenalty = 0.0;
        double noShipBonus = 0;
        int noShipCount = 0;

        int userLevel = getAdmiralLevel();
        hqPenalty = Math.ceil(0.4 * userLevel);

        String[] decklist = deckid_list.split(",");
        for (int n = 0; n < decklist.length; n++) {
            int deckid = Integer.parseInt(decklist[n]);
            JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
            JsonObject seekData = getEachSeekValue(deckShipIdList, deckid, Cn, exclude_flag);
            pureTotalSeek += seekData.get("pure").getAsDouble();
            totalEquipSeek += seekData.get("equip").getAsDouble();
            totalShipSeek += seekData.get("ship").getAsDouble();
            noShipCount += seekData.get("nscount").getAsInt();
        }

        if (Cn == SEEK_PURE) {
            return pureTotalSeek;
        } else {
            noShipBonus = 2 * noShipCount;
            totalSeek = totalShipSeek + Cn * totalEquipSeek - hqPenalty + noShipBonus;
            return Math.floor(totalSeek * 100) / 100;
        }
    }

    public String getSeekType(int cn) {
        String seekType = "";
        switch (cn) {
            case 1:
                seekType = getStringWithLocale(R.string.seek_type_1);
                break;
            case 3:
                seekType = getStringWithLocale(R.string.seek_type_3);
                break;
            case 4:
                seekType = getStringWithLocale(R.string.seek_type_4);
                break;
            default:
                seekType = getStringWithLocale(R.string.seek_type_0);
                break;
        }
        return seekType;
    }

    public String getConditionStatus(JsonArray deckPortData, int deckid) {
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
        if(conditionList.size() == 0) {
            getConditionInfo = "";
        } else {
            getConditionInfo = joinStr(conditionList, "/");
        }
        return getConditionInfo;

    }

    // Air Power Calculation (2016.12.27)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/aircrafts.html
    private double calcBasicAAC(int type, double aac, int carry) {
        if (Arrays.binarySearch(T2LIST_FIGHT_AIRCRAFTS, type) < 0) {
            return 0;
        } else {
            return Math.sqrt(carry) * aac;
        }
    }

    private double calcReinforcedAAC(int type, int aac, int reinforce) {
        switch (type) {
            case T2_FIGHTER:
            case T2_SEA_FIGHTER:
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

    private double[] calcSlotAACFromMastery(int type, int mastery, int mode) {
        int minMastery = mastery;
        if (mode == 1) {
            minMastery = 0;
        }
        double[] rangeAAC = {0.0, 0.0};
        switch (type) {
            case T2_FIGHTER:
            case T2_SEA_FIGHTER:
            case T2_ITCP_FIGHTER:
                rangeAAC[0] += FIGHTER_MASTERY_BONUS[minMastery];
                rangeAAC[1] += FIGHTER_MASTERY_BONUS[mastery];
                rangeAAC[0] += Math.sqrt(BASIC_MASTERY_MIN_BONUS[minMastery] / 10.0);
                rangeAAC[1] += Math.sqrt(BASIC_MASTERY_MAX_BONUS[mastery] / 10.0);
                break;
            case T2_BOMBER:
            case T2_TORPEDO_BOMBER:
            case T2_JET_BOMBER:
            case T2_LBA_AIRCRAFT:
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

    public int[] getAirPowerRange(JsonArray deckPortData, int deckid, JsonObject exclude_flag) {
        int[] totalRangeAAC = {0, 0};
        boolean excludeflagexist = (exclude_flag != null);
        JsonArray excludeflagdata = null;
        if (excludeflagexist) {
            if (deckid == 0) excludeflagdata = exclude_flag.getAsJsonArray("escape");
            if (deckid == 1) excludeflagdata = exclude_flag.getAsJsonArray("escapecb");
        }

        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
                if (excludeflagdata != null && excludeflagdata.contains(new JsonPrimitive(i + 1)))
                    continue;

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
                        JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "id,name,type,tyku");
                        if (itemData == null) continue;
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

    public String getAirPowerRangeString(JsonArray deckPortData, int deckid, JsonObject exclude_flag) {
        String airPowerValue = "";
        int[] airPowerRange = getAirPowerRange(deckPortData, deckid, exclude_flag);
        if (airPowerRange[1] > 0) {
            airPowerValue = KcaUtils.format(getStringWithLocale(R.string.kca_toast_airpower), airPowerRange[0], airPowerRange[1]);
        }
        return airPowerValue;
    }

    public JsonObject getContactProb(JsonArray deckPortData, String deckid_list, JsonObject exclude_flag) {
        JsonObject return_data = new JsonObject();
        JsonArray stage1_prob = new JsonArray();
        JsonArray stage2_prob = new JsonArray();

        SortedMap<Integer, Double> stage2_prob_data = new TreeMap<>();

        boolean excludeflagexist = (exclude_flag != null);
        String[] decklist = deckid_list.split(",");

        double stage1_prob_sum[] = {0.0, 0.0};
        double stage2_prob_sum[] = {0.0, 0.0};

        for (int n = 0; n < decklist.length; n++) {
            int deckid = Integer.parseInt(decklist[n]);
            JsonArray excludeflagdata = null;
            if (excludeflagexist) {
                if (deckid == 0) excludeflagdata = exclude_flag.getAsJsonArray("escape");
                if (deckid == 1) excludeflagdata = exclude_flag.getAsJsonArray("escapecb");
            }
            JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().get("api_ship").getAsJsonArray();
            for (int i = 0; i < deckShipIdList.size(); i++) {
                if (excludeflagdata != null && excludeflagdata.contains(new JsonPrimitive(i + 1)))
                    continue;

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
                            JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "id,name,type,saku,houm");
                            if (itemData == null) continue;
                            String itemName = itemData.get("name").getAsString();
                            int itemLevel = itemData.get("level").getAsInt();
                            int itemMastery = 0;
                            if (itemData.has("alv")) itemMastery = itemData.get("alv").getAsInt();
                            int itemAcc = itemData.get("houm").getAsInt();
                            int itemSeek = itemData.get("saku").getAsInt();
                            int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                            if (itemType == T2_SEA_SCOUT || itemType == T2_SCOUT || itemType == T2_SCOUT_II || itemType == T2_FLYING_BOAT) {
                                stage1_prob_sum[0] += Math.sqrt(slot) * itemSeek * 4.0 / 100;
                                stage1_prob_sum[1] += Math.sqrt(slot) * itemSeek * 2.4 / 100; // 0.6 * 4.0 / 100
                                Integer key = (10 - itemAcc) * 1000 + n * 100 + i * 10 + j;
                                stage2_prob_data.put(key, (double) itemSeek);
                            } else if (itemType == T2_TORPEDO_BOMBER) {
                                stage1_prob_sum[0] += 0.00001;
                                Integer key = (10 - itemAcc) * 1000 + n * 100 + i * 10 + j;
                                stage2_prob_data.put(key, (double) itemSeek);
                            }

                        }
                    }
                }
            }
        }

        double[] remain_percentage = {1.0, 1.0};
        for (Double item: stage2_prob_data.values()) {
            stage2_prob_sum[0] += remain_percentage[0] * item * 0.07; // AS++
            remain_percentage[0] = 1.0 - stage2_prob_sum[0];
            stage2_prob_sum[1] += remain_percentage[1] * item * 0.06; // AS
            remain_percentage[1] = 1.0 - stage2_prob_sum[1];
        }

        stage1_prob.add(stage1_prob_sum[0]);
        stage1_prob.add(stage1_prob_sum[1]);

        stage2_prob.add(Math.min(stage1_prob_sum[0], 1.0) * stage2_prob_sum[0]);
        stage2_prob.add(Math.min(stage1_prob_sum[1], 1.0) * stage2_prob_sum[1]);

        return_data.add("stage1", stage1_prob);
        return_data.add("stage2", stage2_prob);
        return return_data;
    }

    public int getSpeedFlagValue(boolean s, boolean f, boolean fp, boolean sf) {
        int value = 0;
        if (s) value += SPEEDFLAG_SLOW;
        if (f) value += SPEEDFLAG_FAST;
        if (fp) value += SPEEDFLAG_FASTPLUS;
        if (sf) value += SPEEDFLAG_SUPERFAST;
        return value;
    }

    public int getSpeed(JsonArray deckPortData, String deckid_list, JsonObject exclude_flag) {
        boolean is_slow_flag = false;
        boolean is_fast_flag = false;
        boolean is_fastplus_flag = false;
        boolean is_superfast_flag = false;

        boolean excludeflagexist = (exclude_flag != null);
        String[] decklist = deckid_list.split(",");

        for (int n = 0; n < decklist.length; n++) {
            int deckid = Integer.parseInt(decklist[n]);
            JsonArray excludeflagdata = null;
            if (excludeflagexist) {
                if (deckid == 0) excludeflagdata = exclude_flag.getAsJsonArray("escape");
                if (deckid == 1) excludeflagdata = exclude_flag.getAsJsonArray("escapecb");
            }
            JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().get("api_ship").getAsJsonArray();

            // Retrieve Speed (soku) Information
            for (int i = 0; i < deckShipIdList.size(); i++) {
                if (excludeflagdata != null && excludeflagdata.contains(new JsonPrimitive(i + 1)))
                    continue;

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

    public String getSpeedString(JsonArray deckPortData, String deckid_list, JsonObject exclude_flag) {
        int speedValue = getSpeed(deckPortData, deckid_list, exclude_flag);
        return KcaApiData.getSpeedString(bc, speedValue);
    }

    public int[] getTPValue(JsonArray deckPortData, String deckid_list, JsonObject exclude_flag) {
        double totalTP = 0.0;

        boolean excludeflagexist = (exclude_flag != null);
        String[] decklist = deckid_list.split(",");

        for (int n = 0; n < decklist.length; n++) {
            int deckid = Integer.parseInt(decklist[n]);
            JsonArray excludeflagdata = null;
            if (excludeflagexist) {
                if (deckid == 0) excludeflagdata = exclude_flag.getAsJsonArray("escape");
                if (deckid == 1) excludeflagdata = exclude_flag.getAsJsonArray("escapecb");
            }

            JsonArray deckShipIdList = ((JsonObject) deckPortData.get(deckid)).getAsJsonArray("api_ship");
            for (int i = 0; i < deckShipIdList.size(); i++) {
                if (excludeflagdata != null && excludeflagdata.contains(new JsonPrimitive(i + 1)))
                    continue;

                int shipId = deckShipIdList.get(i).getAsInt();
                if (shipId != -1) {
                    JsonObject shipData = getUserShipDataById(shipId, "slot,ship_id,nowhp,maxhp");
                    int kcShipId = shipData.get("ship_id").getAsInt();
                    int nowhp = shipData.get("nowhp").getAsInt();
                    int maxhp = shipData.get("maxhp").getAsInt();
                    if (nowhp * 4 <= maxhp) continue;

                    JsonObject kcShipData = getKcShipDataById(kcShipId, "stype");
                    if (kcShipData == null) {
                        int[] dummy = {-1, -1};
                        return dummy;
                    }
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
                            JsonObject itemData = getUserItemStatusById(item_id, "level", "type");
                            if (itemData == null) continue;
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

    public String getTPString(JsonArray deckPortData, String deckid_list, JsonObject exclude_flag) {
        int[] tp = getTPValue(deckPortData, deckid_list, exclude_flag);
        return KcaUtils.format(getStringWithLocale(R.string.kca_view_tpvalue), tp[1], tp[0]);
    }

    public int[] getKcShipList(JsonArray deckPortData, int deckid) {
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

    public void debugPortInfo(JsonArray deckPortData, int deckid) {
        JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "ship_id,lv,slot,onslot");
                int shipKcId = shipData.get("ship_id").getAsInt();
                int shipLv = shipData.get("lv").getAsInt();
                JsonObject shipKcData = getKcShipDataById(shipKcId, "name");
                String shipName = shipKcData.get("name").getAsString();
                Log.e("KCA", KcaUtils.format("%s (%s)", shipName, shipLv));
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                JsonArray shipSlotCount = (JsonArray) shipData.get("onslot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    int slot = shipSlotCount.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "name,type,tyku");
                        if (itemData == null) continue;
                        String itemName = itemData.get("name").getAsString();
                        int itemLevel = itemData.get("level").getAsInt();
                        int itemMastery = 0;
                        if (itemData.has("alv")) {
                            itemMastery = itemData.get("alv").getAsInt();
                        }
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        Log.e("KCA", KcaUtils.format("- %s %d %d %d", itemName, itemLevel, itemMastery, slot));
                    }
                }
            }
        }
    }

    public JsonArray getDeckListInfo(JsonArray deckPortData, int deckid, String request_list, String kc_request_list) {
        JsonArray deckListInfo = new JsonArray();
        int deckSize = deckPortData.size();
        if (deckid < deckSize) {
            JsonArray deckShipIdList = (JsonArray) ((JsonObject) deckPortData.get(deckid)).get("api_ship");
            for (int i = 0; i < deckShipIdList.size(); i++) {
                JsonObject data = new JsonObject();
                int shipId = deckShipIdList.get(i).getAsInt();
                if (shipId != -1) {
                    int shipKcId = -1;
                    JsonObject shipData = getUserShipDataById(shipId, request_list);
                    data.add("user", shipData);
                    if (shipData.has("api_ship_id")) {
                        shipKcId = shipData.get("api_ship_id").getAsInt();
                    } else if (shipData.has("ship_id")) {
                        shipKcId = shipData.get("ship_id").getAsInt();
                    }
                    if (shipKcId != -1) {
                        data.add("kc", getKcShipDataById(shipKcId, kc_request_list));
                        deckListInfo.add(data);
                    }
                }
            }
        }
        return deckListInfo;
    }

    public int checkHeavyDamageExist(JsonArray deckPortData, int deckid) {
        int[] status = {0, 0, 0, 0, 0, 0, 0};
        boolean check_locked = getBooleanPreferences(ac, PREF_HDNOTI_LOCKED);
        int min_level = Integer.parseInt(getStringPreferences(ac, PREF_HDNOTI_MINLEVEL));
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                if (KcaDocking.checkShipInDock(shipId)) continue;

                JsonObject shipData = getUserShipDataById(shipId, "nowhp,maxhp,slot,slot_ex,lv,locked,locked_equip");
                int level = shipData.get("lv").getAsInt();
                int locked = shipData.get("locked").getAsInt();
                int locked_eq = shipData.get("locked_equip").getAsInt();

                if (check_locked && (locked == 0 && locked_eq == 0)) continue;
                if (min_level >= level && locked_eq == 0) continue;

                int shipNowHp = shipData.get("nowhp").getAsInt();
                int shipMaxHp = shipData.get("maxhp").getAsInt();
                if (shipNowHp * 4 <= shipMaxHp) {
                    status[i] = 2;
                }

                JsonArray shipItem = (JsonArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(item_id, "id", "type");
                        if (itemData == null) continue;
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        if (itemType == T2_DAMECON && status[i] != 0) {
                            status[i] = 1;
                        }
                    }
                }
                int ex_item_id = shipData.get("slot_ex").getAsInt();
                if (ex_item_id != 0 && ex_item_id != -1) {
                    Log.e("KCA", String.valueOf(ex_item_id));
                    JsonObject itemData = getUserItemStatusById(ex_item_id, "id", "type");
                    if (itemData == null) continue;
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

    public boolean[] getHeavyDmgCheckStatus(JsonArray deckPortData, int deckid) {
        boolean check_locked = getBooleanPreferences(ac, PREF_HDNOTI_LOCKED);
        int min_level = Integer.parseInt(getStringPreferences(ac, PREF_HDNOTI_MINLEVEL));
        boolean[] heavyDmgCheckStatus = {true, true, true, true, true, true, true};
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "lv,locked,locked_equip");
                int level = shipData.get("lv").getAsInt();
                int locked = shipData.get("locked").getAsInt();
                int locked_eq = shipData.get("locked_equip").getAsInt();

                if (check_locked && (locked == 0 && locked_eq == 0)) heavyDmgCheckStatus[i] = false;
                if (min_level >= level && locked_eq == 0) heavyDmgCheckStatus[i] = false;
            }
        }
        return heavyDmgCheckStatus;
    }

    public boolean[] getDameconStatus(JsonArray deckPortData, int deckid) {
        boolean[] dameconStatus = {false, false, false, false, false, false, false};
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "slot,slot_ex");
                JsonArray shipItem = (JsonArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = shipItem.get(j).getAsInt();
                    if (item_id != -1) {
                        JsonObject itemData = getUserItemStatusById(item_id, "id", "type");
                        if (itemData == null) continue;
                        int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                        if (itemType == T2_DAMECON) {
                            dameconStatus[i] = true;
                        }
                    }
                }
                int ex_item_id = shipData.get("slot_ex").getAsInt();
                if (ex_item_id != 0 && ex_item_id != -1) {
                    JsonObject itemData = getUserItemStatusById(ex_item_id, "id", "type");
                    if (itemData == null) continue;
                    int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                    if (itemType == T2_DAMECON) {
                        dameconStatus[i] = true;
                    }
                }
            }
        }
        return dameconStatus;
    }

    public boolean checkNotSuppliedExist(JsonArray deckPortData, int deckid) {
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

    public int checkShipInDeck(JsonArray deckPortData, int target) { // return -1 or 0~3
        for (int i = 0; i < deckPortData.size(); i++) {
            JsonArray deckShipIdList = deckPortData.get(i).getAsJsonObject().getAsJsonArray("api_ship");
            for (int j = 0; j < deckShipIdList.size(); j++) {
                int shipid = deckShipIdList.get(j).getAsInt();
                if (shipid == target) return i;
            }
        }
        return -1;
    }

    public int checkMinimumMorale(JsonArray deckPortData, int deckid) {
        JsonArray deckShipIdList = deckPortData.get(deckid).getAsJsonObject().getAsJsonArray("api_ship");
        int min_cond_value = 100;
        for (int i = 0; i < deckShipIdList.size(); i++) {
            int shipId = deckShipIdList.get(i).getAsInt();
            if (shipId != -1) {
                JsonObject shipData = getUserShipDataById(shipId, "cond");
                int cond = shipData.get("cond").getAsInt();
                min_cond_value = Math.min(cond, min_cond_value);
            }
        }
        return min_cond_value;
    }

    public JsonArray checkAkashiFlagship(JsonArray deckPortData) {
        JsonArray deck_id_list = new JsonArray();
        for (int i = 0; i < deckPortData.size(); i++) {
            JsonArray deckShipIdList = deckPortData.get(i).getAsJsonObject().getAsJsonArray("api_ship");
            int flagship = deckShipIdList.get(0).getAsInt();
            if (flagship != -1) {
                JsonObject shipData = getUserShipDataById(flagship, "ship_id");
                int kc_ship_id = shipData.get("ship_id").getAsInt();
                if (kc_ship_id == 182 || kc_ship_id == 187) deck_id_list.add(i);
            }
        }
        return deck_id_list;
    }

    // Reference: http://kancolle.wikia.com/wiki/Land_Base_Aerial_Support#Fighter_Power_Calculations
    public int getAirPowerInAirBase(int status, JsonArray plane_info) {
        int air_power = 0;
        if (status == LAB_STATUS_SORTIE) {
            for (int i = 0; i < plane_info.size(); i++) {
                JsonObject item = plane_info.get(i).getAsJsonObject();
                int state = item.get("api_state").getAsInt();
                if (state != 1) continue;

                int count = item.get("api_count").getAsInt();
                int slotid = item.get("api_slotid").getAsInt();
                JsonObject itemData = getUserItemStatusById(slotid, "level,alv", "type,tyku,houk");
                if (itemData == null) continue;
                int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                int itemLevel = itemData.get("level").getAsInt();
                int itemMastery = 0;
                if (itemData.has("alv")) {
                    itemMastery = itemData.get("alv").getAsInt();
                }
                double profiencyBonus = calcSlotAACFromMastery(itemType, itemMastery, 0)[0];
                int itemAAC = itemData.get("tyku").getAsInt();
                int itemITC = itemData.get("houk").getAsInt();
                int realAAC = (int) Math.floor(Math.sqrt(count) * (1.5 * itemITC + calcReinforcedAAC(itemType, itemAAC, itemLevel)) + profiencyBonus);
                air_power += realAAC;
            }
        } else if (status == LAB_STATUS_DEFENSE) {
            for (int i = 0; i < plane_info.size(); i++) {
                JsonObject item = plane_info.get(i).getAsJsonObject();
                int state = item.get("api_state").getAsInt();
                if (state != 1) continue;

                int count = item.get("api_count").getAsInt();
                int slotid = item.get("api_slotid").getAsInt();
                JsonObject itemData = getUserItemStatusById(slotid, "level,alv", "type,tyku,houk,houm,saku");
                if (itemData == null) continue;
                int itemType = itemData.get("type").getAsJsonArray().get(2).getAsInt();
                int itemLevel = itemData.get("level").getAsInt();
                int itemMastery = 0;
                if (itemData.has("alv")) {
                    itemMastery = itemData.get("alv").getAsInt();
                }
                double profiencyBonus = calcSlotAACFromMastery(itemType, itemMastery, 0)[0];
                int itemAAC = itemData.get("tyku").getAsInt();
                int itemITC = itemData.get("houk").getAsInt();
                int itemAB = 0;
                if (itemType == T2_ITCP_FIGHTER) itemAB += itemData.get("houm").getAsInt();
                int itemSeek = itemData.get("saku").getAsInt();
                int realAAC = (int) Math.floor(Math.sqrt(count) * (itemITC + itemAB * 2.0 + calcReinforcedAAC(itemType, itemAAC, itemLevel)) + profiencyBonus);

                double recon_bonus = 1.0;
                if (itemType == T2_SCOUT || itemType == T2_SCOUT_II) {
                    if(itemSeek >= 9) recon_bonus = 1.3;
                    else recon_bonus = 1.2;
                } else if (itemType == T2_SEA_SCOUT) {
                    if (itemSeek >= 9) recon_bonus = 1.16;
                    else if (itemSeek == 8) recon_bonus = 1.13;
                    else recon_bonus = 1.1;
                }
                air_power += (int) Math.floor(realAAC * recon_bonus);
            }
        }
        return air_power;
    }
}
