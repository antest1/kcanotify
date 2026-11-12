package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaApiData.AKASHI_TIMER_20MIN;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

// Anchorage repair by repair ships (AR): Akashi, Akashi Kai and Asahi Kai.
// - The flagship AR repairs the ships of its fleet from the 1st ship, unless it is moderately damaged or worse.
// - 1 AR: as many ships as its ship repair facilities. Akashi counts 2 more as built in;
//   Asahi Kai has none, so she repairs only with facilities equipped (up to 3 ships).
// - 2 AR: Asahi Kai and Akashi as the 1st and 2nd ships (either one the flagship), both lightly damaged or better.
//   2 + the facilities equipped on both (up to 7 ships for a striking force), and each repair takes 5/6 of the time.
public class KcaAkashiRepairInfo {
    public static final int SHIP_ID_AKASHI = 182;
    public static final int SHIP_ID_AKASHI_KAI = 187;
    public static final int AKASHI_BUILT_IN_FACILITIES = 2;
    public static final double TWO_AR_REPAIR_MODIFIER = 5.0 / 6.0;

    public static Map<Integer, Integer> akashi_data = new HashMap<>();
    public static Map<Integer, Double> akashi_modifier = new HashMap<>();

    public static long akashi_register_time = -1;

    public static long getAkashiRepairTime() {
        if (akashi_register_time < 0) return 0;
        return akashi_register_time + AKASHI_TIMER_20MIN * 1000;
    }

    public static void initAkashiTimer() { setAkashiTimer(-1); akashi_data.clear(); akashi_modifier.clear(); }

    public static void setAkashiData(JsonArray value) {
        akashi_data.clear();
        akashi_modifier.clear();
        for (JsonElement item: value) {
            JsonObject obj = item.getAsJsonObject();
            int id = obj.get("id").getAsInt();
            akashi_data.put(id, obj.get("count").getAsInt());
            akashi_modifier.put(id, obj.get("modifier").getAsDouble());
        }
    }

    public static int getBuiltInFacilities(int kcShipId) {
        return (kcShipId == SHIP_ID_AKASHI || kcShipId == SHIP_ID_AKASHI_KAI) ? AKASHI_BUILT_IN_FACILITIES : 0;
    }

    // first, second: {equipped, total, usable} of the 1st and 2nd ships when they are ARs, otherwise null.
    // total: repair facilities including the built-in ones of Akashi
    // usable: 1 when the ship is lightly damaged or better, 0 when moderately damaged or worse
    public static boolean isTwoRepairShips(int[] first, int[] second) {
        return first != null && second != null && first[2] == 1 && second[2] == 1;
    }

    // ships repaired from the 1st ship, or 0 when the fleet cannot do anchorage repair
    public static int getRepairReach(int[] first, int[] second) {
        if (first == null || first[2] == 0) return 0;
        // with Akashi in the pair, her 2 built-in facilities are the base 2
        if (isTwoRepairShips(first, second)) return Math.min(first[1] + second[1], 7);
        return Math.min(first[1], 6);
    }

    public static double getRepairModifier(int[] first, int[] second) {
        return isTwoRepairShips(first, second) ? TWO_AR_REPAIR_MODIFIER : 1.0;
    }

    public static void setAkashiTimer() { setAkashiTimer(System.currentTimeMillis()); }

    public static void setAkashiTimer(long time) { akashi_register_time = time; }

    public static boolean getAkashiInAnyFlagship() {
        return !akashi_data.isEmpty();
    }

    public static int getAkashiAvailableCount(int index) {
        if (akashi_data.containsKey(index)) {
            Integer result = akashi_data.get(index);
            if (result != null) return result;
        }
        return 0;
    }

    public static double getAkashiRepairModifier(int index) {
        Double result = akashi_modifier.get(index);
        return result != null ? result : 1.0;
    }

    public static long getAkashiTimerValue() { return akashi_register_time; }

    public static int getAkashiElapsedTimeInSecond() { return (int) ((System.currentTimeMillis() - akashi_register_time) / 1000); }

}
