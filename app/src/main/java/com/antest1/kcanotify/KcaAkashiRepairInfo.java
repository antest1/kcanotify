package com.antest1.kcanotify;

public class KcaAkashiRepairInfo {
    public static long akashi_register_time = -1;

    public static void initAkashiTimer() { setAkashiTimer(-1); }

    public static void setAkashiTimer() { setAkashiTimer(System.currentTimeMillis()); }

    public static void setAkashiTimer(long time) { akashi_register_time = time; }

    public static long getAkashiTimerValue() { return akashi_register_time; }

    public static int getAkashiElapsedTimeInSecond() { return (int) ((System.currentTimeMillis() - akashi_register_time) / 1000); }


}
