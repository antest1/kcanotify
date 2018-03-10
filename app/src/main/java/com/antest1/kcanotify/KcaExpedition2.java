package com.antest1.kcanotify;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.text.TextUtils.concat;
import static com.antest1.kcanotify.KcaAlarmService.ALARM_DELAY;
import static com.antest1.kcanotify.KcaUtils.getTimeStr;

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

    public static boolean isInExpedition(int idx) {
        return mission_no[idx] != -1;
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
            if (left_time < 0) return "";
            String mission_no_head = getExpeditionHeader(mission_no[idx]);
            return mission_no_head.concat(getTimeStr(left_time));
        }
    }

    public static String getSpentTimeStr(int idx) {
        if (complete_time_check[idx] == -1) return "";
        else {
            long current_time = System.currentTimeMillis();
            long arrive_time = complete_time_check[idx];
            int current_mission_no = mission_no[idx];
            long duration = KcaApiData.getExpeditionDuration(current_mission_no);
            long start_time = arrive_time - duration;

            int pass_time = (int) (System.currentTimeMillis() - start_time) / 1000;
            String mission_no_head = getExpeditionHeader(mission_no[idx]);
            return mission_no_head.concat(getTimeStr(pass_time));
        }
    }

    public static String getEndTimeStr(int idx) {
        if (complete_time_check[idx] == -1) return "";
        else {
            long arrive_time = complete_time_check[idx] - ALARM_DELAY;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

            String mission_no_head = getExpeditionHeader(mission_no[idx]);
            return mission_no_head.concat(sdf.format(new Date(arrive_time)));
        }
    }

    public static String getTimeInfoStr(int idx, int type) {
        switch (type) {
            case 2:
                return getSpentTimeStr(idx);
            case 1:
                return getEndTimeStr(idx);
            default:
                return getLeftTimeStr(idx);
        }
    }

    public static String getExpeditionStr(int mission_no_value) {
        String mission_no_str = "";
        if (mission_no_value >= 100) {
            if ( mission_no_value < 110) mission_no_str = KcaUtils.format("A%d", (mission_no_value + 1) - 100);
            else if ( mission_no_value < 120) mission_no_str = KcaUtils.format("B%d", (mission_no_value + 1) - 110);
            else if (mission_no_value % 2 == 1) mission_no_str = "S1";
            else mission_no_str = "S2";
        } else {
            mission_no_str = String.valueOf(mission_no_value);
        }
        return mission_no_str;
    }

    public static String getExpeditionHeader(int mission_no) {
        String mission_no_head = "";
        String mission_no_value = getExpeditionStr(mission_no);
        if (mission_no_value.contains("A") || mission_no_value.contains("B") || mission_no_value.contains("S")) {
            mission_no_head = "[".concat(mission_no_value).concat("] ");
        } else {
            mission_no_head = KcaUtils.format("[%02d] ", mission_no);
        }
        return mission_no_head;
    }
}
