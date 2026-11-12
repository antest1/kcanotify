package com.antest1.kcanotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaApiData.AKASHI_TIMER_20MIN;
import static com.antest1.kcanotify.KcaApiData.STYPE_AR;
import static com.antest1.kcanotify.KcaApiData.STYPE_AS;
import static com.antest1.kcanotify.KcaApiData.STYPE_BB;
import static com.antest1.kcanotify.KcaApiData.STYPE_BBV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CA;
import static com.antest1.kcanotify.KcaApiData.STYPE_CAV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVB;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVL;
import static com.antest1.kcanotify.KcaApiData.STYPE_DE;
import static com.antest1.kcanotify.KcaApiData.STYPE_FBB;
import static com.antest1.kcanotify.KcaApiData.STYPE_SS;

public class KcaDocking {
    public static JsonArray dockdata = new JsonArray();
    public static long[] complete_time_check = {-1, -1, -1, -1};
    public static int[] dock_ship_id = {0, 0, 0, 0};

    public static JsonArray getDockData() { return dockdata; }
    public static void setDockData(JsonArray v) { dockdata = v; }
    public static long getCompleteTime(int dock) { return complete_time_check[dock]; }
    public static void setCompleteTime(int dock, long time) {
        complete_time_check[dock] = time;
    }
    public static int getShipId(int dock) { return dock_ship_id[dock]; }
    public static void setShipId(int dock, int id) { dock_ship_id[dock] = id; }
    public static boolean checkShipInDock(int id) {
        if (id <= 0) return false;
        for (int sid : dock_ship_id) {
            if (sid == id) return true;
        }
        return false;
    }
    public static int getDockingTime(int hp_loss, int level, int stype) {
        if (hp_loss > 0) {
            int repair_time = 0;
            double multiplier = getMultiplier(stype);
            if (level <= 11) {
                repair_time = 30 + (int) (hp_loss * (level * 10) * multiplier);
            } else {
                repair_time = 30 + (int) (hp_loss * ((level * 5) + Math.floor(Math.sqrt(level - 11)) * 10 + 50) * multiplier);
            }
            return repair_time;
        } else {
            return 0;
        }
    }
    private static double getMultiplier(int type) {
        if (type == STYPE_BB || type == STYPE_BBV || type == STYPE_CV || type == STYPE_CVB || type == STYPE_AR) return 2.0;
        else if (type == STYPE_CA || type == STYPE_CAV || type == STYPE_FBB || type == STYPE_CVL || type == STYPE_AS ) return 1.5;
        else if (type == STYPE_SS || type == STYPE_DE ) return 0.5;
        else return 1.0;
    }

    private static double getRepair1HpTime(int level, int stype) {
        double mult = getMultiplier(stype);
        if (level <= 11) {
            return level * 10 * mult;
        } else {
            double amp = Math.floor(Math.sqrt(level - 11)) * 10 + 50;
            return (level * 5 + amp) * mult;
        }
    }

    /**
     *
     * @param level level of the ship to be repaired
     * @param stype ship type of the ship
     * @param modifier repair time modifier (5/6 with two repair ships)
     * @param elapsed elapsed seconds
     * @return
     */
    public static int getRepairedHp(int level, int stype, double modifier, int elapsed) {
        // the expected repair so far: all of it is applied at a port visit once 20 minutes have passed,
        // and from 20 minutes it is at least 1 HP.
        // the epsilon keeps exact multiples of a fractional time (with the 5/6 modifier) from rounding down
        int repaired = (int) Math.floor(elapsed / (getRepair1HpTime(level, stype) * modifier) + 1e-6);
        return elapsed >= AKASHI_TIMER_20MIN ? Math.max(1, repaired) : repaired;

        // 10秒たった
        // 3.1秒で1回復
        // maybe = 10 / 3.1 = 3.22 ~~ 3すでに回復しているはず
        // でもceil(3.1 * 3) = 10

        // 極端な例の検証不足
        // TODO: exhaustive check is needed
    }

    /**
     *
     * @param level level of the ship to be repaired
     * @param stype ship type of the ship
     * @param modifier repair time modifier (5/6 with two repair ships)
     * @param elapsed elapsed seconds
     * @return
     */
    public static int getNextRepair(int level, int stype, double modifier, int elapsed) {
        double repair1HpTime = getRepair1HpTime(level, stype) * modifier;
        int repaired = getRepairedHp(level, stype, modifier, elapsed);
        int need = (int) Math.ceil(repair1HpTime * (repaired + 1) - 1e-6);
        // the first HP comes at 20 minutes at the latest
        if (repaired == 0 && elapsed < AKASHI_TIMER_20MIN) need = Math.min(need, AKASHI_TIMER_20MIN);
        return need - elapsed;
    }
}
