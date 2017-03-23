package com.antest1.kcanotify;

public class KcaDocking {
    public static long[] complete_time_check = {-1, -1, -1, -1};
    public static int[] dock_ship_id = {0, 0, 0, 0};

    public static long getCompleteTime(int dock) { return complete_time_check[dock]; }
    public static void setCompleteTime(int dock, long time) {
        complete_time_check[dock] = time;
    }
    public static int getShipId(int dock) { return dock_ship_id[dock]; }
    public static void setShipId(int dock, int id) { dock_ship_id[dock] = id; }
}
