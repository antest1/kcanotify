package com.antest1.kcanotify;

import java.util.Locale;

import static com.antest1.kcanotify.KcaAlarmService.ALARM_DELAY;

public class KcaExpedition2 {
    public static int[] mission_no = {-1, -1, -1, -1};
    public static String[] deck_name = {null, "", "", ""};
    public static long[] complete_time_check = {-1, -1, -1, -1};
    public static boolean[] canceled_flag = {false, false, false, false};

    public static boolean isMissionExist() {
        for (int i = 1; i < 4; i++) {
            if (complete_time_check[i] != -1) {
                return true;
            }
        }
        return false;
    }

    public static void setMissionData(int idx, String deck, int mission, long arrive_time) {
        mission_no[idx] = mission;
        deck_name[idx] = deck;
        complete_time_check[idx] = arrive_time;
        canceled_flag[idx] = false;
    }

    public static void clearMissionData(int idx) {
        mission_no[idx] = -1;
        deck_name[idx] = "";
        complete_time_check[idx] = -1;
        canceled_flag[idx] = false;
    }

    public static boolean isInMission(int idx) {
        return complete_time_check[idx] != -1;
    }

    public static boolean isCanceled(int idx) {
        return canceled_flag[idx];
    }

    public static String getDeckName(int idx) {
        return deck_name[idx];
    }

    public static int getIdxByMissionNo(int mission) {
        for (int i = 1; i < mission_no.length; i++) {
            if (mission_no[i] == mission) {
                return i;
            }
        }
        return -1;
    }

    public static void cancel(int idx, long arrive_time) {
        canceled_flag[idx] = true;
        complete_time_check[idx] = arrive_time;
    }

    public static long getArriveTime(int idx) {
        return complete_time_check[idx];
    }

    public static String getLeftTimeStr(int idx) {
        if (complete_time_check[idx] == -1) return "";
        else {
            int left_time = (int) (complete_time_check[idx] - System.currentTimeMillis() - ALARM_DELAY) / 1000;
            int sec, min, hour;
            sec = left_time;
            min = sec / 60;
            hour = min / 60;
            sec = sec % 60;
            min = min % 60;

            return String.format("[%02d] %02d:%02d:%02d", mission_no[idx], hour, min, sec);
        }
    }
}
