package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaApiData.AKASHI_TIMER_20MIN;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class KcaAkashiRepairInfo {
    public static Map<Integer, Integer> akashi_data = new HashMap<>();

    public static long akashi_register_time = -1;

    public static long getAkashiRepairTime() {
        if (akashi_register_time < 0) return 0;
        return akashi_register_time + AKASHI_TIMER_20MIN * 1000;
    }

    public static void initAkashiTimer() { setAkashiTimer(-1); akashi_data.clear(); }

    public static void setAkashiData(JsonArray value) {
        akashi_data.clear();
        for (JsonElement item: value) {
            JsonObject obj = item.getAsJsonObject();
            akashi_data.put(obj.get("id").getAsInt(), obj.get("count").getAsInt());
        }
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

    public static long getAkashiTimerValue() { return akashi_register_time; }

    public static int getAkashiElapsedTimeInSecond() { return (int) ((System.currentTimeMillis() - akashi_register_time) / 1000); }

}
