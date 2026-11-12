package com.antest1.kcanotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class KcaNosakiInfo {
    // Nosaki / Nosaki Kai have ship type "Food Supply Ship" (APF).
    // These are the master data api_id values (not the encyclopedia/sort numbers 596/602).
    public static final int SHIP_ID_NOSAKI = 996;
    public static final int SHIP_ID_NOSAKI_KAI = 1002;
    public static final int TIMER_SEC = 900;
    public static final int MIN_MORALE = 30;
    private static final long TIMER_MS = TIMER_SEC * 1000L;

    // fleets with an APF as the flagship or 2nd ship that meets the conditions -> morale gain
    public static Map<Integer, Integer> nosaki_data = new HashMap<>();
    public static long nosaki_register_time = -1;

    public static void initNosakiTimer() {
        setNosakiTimer(-1);
        nosaki_data.clear();
    }

    public static void setNosakiData(JsonArray value) {
        nosaki_data.clear();
        for (JsonElement item : value) {
            JsonObject obj = item.getAsJsonObject();
            nosaki_data.put(obj.get("id").getAsInt(), obj.get("gain").getAsInt());
        }
    }

    public static void setNosakiTimer() { setNosakiTimer(System.currentTimeMillis()); }

    public static void setNosakiTimer(long time) { nosaki_register_time = time; }

    public static boolean isNosakiTimerOver() {
        return nosaki_register_time >= 0 && getNosakiElapsedTimeInSecond() >= TIMER_SEC;
    }

    public static boolean getNosakiInAnyFlagship() {
        return !nosaki_data.isEmpty();
    }

    // conditions of the APF, as for a repair ship: fully supplied, better than lightly damaged (HP over 75%),
    // morale 30 or more, and not on expedition or in docking
    public static boolean isNosakiEffective(int nowHp, int maxHp, int cond, int fuel, int fuelMax, int bull, int bullMax,
                                            boolean onExpedition, boolean inDock) {
        return nowHp * 4 > maxHp * 3
                && cond >= MIN_MORALE
                && fuel >= fuelMax && bull >= bullMax
                && !onExpedition && !inDock;
    }

    public static int getNosakiMoraleGain(int index) {
        Integer result = nosaki_data.get(index);
        return result != null ? result : 0;
    }

    public static long getNosakiTimerValue() { return nosaki_register_time; }

    public static int getNosakiElapsedTimeInSecond() {
        return (int) ((System.currentTimeMillis() - nosaki_register_time) / 1000);
    }

    // The absolute time of the next passive morale gain tick. Past ticks are
    // advanced to the next future cycle. Returns -1 when the timer is not running.
    public static long getNextNosakiTickTime() {
        if (nosaki_register_time < 0) return -1;
        long next = nosaki_register_time + TIMER_MS;
        long now = System.currentTimeMillis();
        if (next <= now) {
            next += ((now - next) / TIMER_MS + 1) * TIMER_MS;
        }
        return next;
    }
}
