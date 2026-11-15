package com.antest1.kcanotify;

import static java.lang.System.currentTimeMillis;

public class KcaMoraleInfo {
    public static final long WAIT_UNIT = 179000;
    public static final int NATURAL_MORALE_CAP = 49;
    public static final int APF_MORALE_CAP = 54;
    public static final long APF_GAIN_INTERVAL = 15 * 60 * 1000L;

    public static long[] complete_time_check = {-1, -1, -1, -1};
    public static int[] dock_morale_value = {-1, -1, -1, -1};
    public static long[] registered_time_check = {-1, -1, -1, -1};
    public static int[] dock_apf_gain = {0, 0, 0, 0};
    public static long[] dock_apf_tick = {-1, -1, -1, -1};
    public static int deck_count = 0;
    public static int min_morale = 40;
    public static int itemuse_deck = -1;

    public static int getItemUseDeckAndReset() { int ret = itemuse_deck; itemuse_deck = -1; return ret; }
    public static void setItemUseDeck(int value) { itemuse_deck = value; }
    public static void setMinMorale(int value) { min_morale = Math.max(1, Math.min(APF_MORALE_CAP, value)); }
    public static int getDeckCount() { return deck_count; }
    public static void setDeckCount(int value) { deck_count = value; }

    public static void initMoraleValue(int minval) {
        min_morale = Math.max(1, Math.min(APF_MORALE_CAP, minval));
        for (int i = 0; i < dock_morale_value.length; i++) {
            dock_morale_value[i] = -1;
            complete_time_check[i] = -1;
            registered_time_check[i] = -1;
            dock_apf_gain[i] = 0;
            dock_apf_tick[i] = -1;
        }
    }
    public static long getMoraleCompleteTime(int dock) { return complete_time_check[dock]; }

    // flag: force reschedule, cflag: use the registered time check
    // apfGain: passive morale gain per 15 minutes (0 when no APF is present)
    // apfNextTick: absolute time of the next passive morale gain tick
    public static boolean setMoraleValue(int dock, int value, int apfGain, long apfNextTick, boolean flag, boolean cflag) {
        flag = flag || (value < dock_morale_value[dock])
                || apfGain != dock_apf_gain[dock]
                || (apfGain > 0 && apfNextTick != dock_apf_tick[dock]);
        dock_morale_value[dock] = value;
        dock_apf_gain[dock] = apfGain;
        dock_apf_tick[dock] = apfNextTick;

        if (value < min_morale) {
            long current_time = currentTimeMillis();
            long completion_time = calculateMoraleCompleteTime(value, apfGain, apfNextTick, current_time);
            if (completion_time < 0) {
                // The target cannot be reached with natural recovery and the current APF bonus.
                flag = complete_time_check[dock] != -1;
                complete_time_check[dock] = -1;
            } else {
                if (cflag && registered_time_check[dock] > 0) {
                    completion_time = registered_time_check[dock] + (completion_time - current_time);
                } else {
                    registered_time_check[dock] = current_time;
                }

                if (!flag && complete_time_check[dock] != -1) {
                    complete_time_check[dock] = Math.min(complete_time_check[dock], completion_time);
                    flag = (complete_time_check[dock] == completion_time);
                } else {
                    boolean changed = complete_time_check[dock] != completion_time;
                    complete_time_check[dock] = completion_time;
                    flag = flag || changed;
                }
            }
        } else {
            flag = complete_time_check[dock] != -1;
            complete_time_check[dock] = -1;
        }
        return flag;
    }

    // Simulates natural regeneration (+3 up to 49) and APF passive gain (up to 54).
    // Returns an absolute timestamp, or -1 when the target is unreachable.
    public static long calculateMoraleCompleteTime(int value, int apfGain, long apfNextTick, long now) {
        if (value >= min_morale) return now;
        if (min_morale > APF_MORALE_CAP) return -1;
        if (min_morale > NATURAL_MORALE_CAP && apfGain <= 0) return -1;

        long naturalTick = now + WAIT_UNIT;
        long apfTick = Long.MAX_VALUE;
        if (apfGain > 0) {
            apfTick = apfNextTick > now ? apfNextTick : now + APF_GAIN_INTERVAL;
        }

        int morale = value;
        long completionTime = now;
        while (morale < min_morale) {
            boolean natural = morale < NATURAL_MORALE_CAP;
            if (!natural && apfGain <= 0) return -1;
            if (morale >= APF_MORALE_CAP) return -1;

            if (natural && naturalTick <= apfTick) {
                morale = Math.min(NATURAL_MORALE_CAP, morale + 3);
                completionTime = naturalTick;
                naturalTick += WAIT_UNIT;
            } else {
                morale = Math.min(APF_MORALE_CAP, morale + apfGain);
                completionTime = apfTick;
                apfTick += APF_GAIN_INTERVAL;
            }
        }
        return completionTime;
    }
}
