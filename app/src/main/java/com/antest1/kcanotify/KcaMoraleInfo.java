package com.antest1.kcanotify;

import static java.lang.System.currentTimeMillis;

public class KcaMoraleInfo {
    public static final long WAIT_UNIT = 179000;
    public static long[] complete_time_check = {-1, -1, -1, -1};
    public static int[] dock_morale_value = {-1, -1, -1, -1};
    public static long[] registered_time_check = {-1, -1, -1, -1};
    public static int deck_count = 0;
    public static int min_morale = 40;
    public static int itemuse_deck = -1;

    public static int getItemUseDeckAndReset() { int ret = itemuse_deck; itemuse_deck = -1; return ret; }
    public static void setItemUseDeck(int value) { itemuse_deck = value; }
    public static void setMinMorale(int value) { min_morale = value; }
    public static int getDeckCount() { return deck_count; }
    public static void setDeckCount(int value) { deck_count = value; }

    public static void initMoraleValue(int minval) {
        min_morale = minval;
        for (int i = 0; i < dock_morale_value.length; i++) {
            dock_morale_value[i] = -1;
            complete_time_check[i] = -1;
            registered_time_check[i] = -1;
        }
    }
    public static long getMoraleCompleteTime(int dock) { return complete_time_check[dock]; }

    // rflag: noti reset flag, cflag = change flag (use registered time check)
    public static boolean setMoraleValue(int dock, int value, boolean flag, boolean cflag) {
        flag = flag || (value < dock_morale_value[dock]);
        dock_morale_value[dock] = value;
        if (value < min_morale) {
            int count = (int) Math.ceil((float)(min_morale - value) / 3.0);
            long completion_time = WAIT_UNIT * count;
            if (cflag && registered_time_check[dock] > 0) {
                completion_time += registered_time_check[dock];
            } else {
                long current_time = System.currentTimeMillis();
                completion_time += current_time;
                registered_time_check[dock] = current_time;
            }

            if (!flag && complete_time_check[dock] != -1) {
                complete_time_check[dock] = Math.min(complete_time_check[dock], completion_time);
                flag = (complete_time_check[dock] == completion_time);
            } else {
                complete_time_check[dock] = completion_time;
            }
        } else {
            flag = complete_time_check[dock] != -1;
            complete_time_check[dock] = -1;
        }
        return flag;
    }
}
