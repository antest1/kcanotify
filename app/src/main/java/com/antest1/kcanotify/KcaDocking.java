package com.antest1.kcanotify;

import com.google.gson.JsonArray;

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
}
