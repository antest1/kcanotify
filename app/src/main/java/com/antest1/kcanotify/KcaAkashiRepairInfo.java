package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaApiData.AKASHI_TIMER_20MIN;

public class KcaAkashiRepairInfo {
    public static boolean akashi_exist = false;

    public static long akashi_register_time = -1;

    public static long getAkashiRepairTime() {
        if (akashi_register_time < 0) return 0;
        return akashi_register_time + AKASHI_TIMER_20MIN * 1000;
    }

    public static void initAkashiTimer() { setAkashiTimer(-1); akashi_exist = false; }

    public static void setAkashiExist(boolean value) { akashi_exist = value; }

    public static void setAkashiTimer() { setAkashiTimer(System.currentTimeMillis()); }

    public static void setAkashiTimer(long time) { akashi_register_time = time; }

    public static boolean getAkashiInFlasship() { return akashi_exist; }

    public static long getAkashiTimerValue() { return akashi_register_time; }

    public static int getAkashiElapsedTimeInSecond() { return (int) ((System.currentTimeMillis() - akashi_register_time) / 1000); }


}
