package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaConstants.KCA_API_NOTI_DOCK_FIN;

public class KcaDocking implements Runnable {
    public static int maxNdock;
    public static long[] complete_time_check = {-1, -1, -1, -1};
    public static Handler sHandler;
    public static int start_delay = 2;
    public static int server_delay = 60;

    int dock_no = 0;
    int ship_id = 0;
    Handler mHandler;
    int left_time = -1;
    public static Gson gson = new Gson();

    public KcaDocking(int no, int ship, long time, Handler h) {
        dock_no = no;
        ship_id = ship;

        sHandler = h;

        complete_time_check[no] = time;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (!KcaService.isServiceOn) return;

                int sec, min, hour;
                sec = left_time;
                min = sec / 60;
                hour = min / 60;
                sec = sec % 60;
                min = min % 60;

                String strTime = String.format("%02d:%02d:%02d", hour, min, sec);
                //// Log.e("KCA", strTime);
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                left_time = (int) ((complete_time_check[dock_no] - System.currentTimeMillis()) / 1000) - start_delay;
                if (complete_time_check[dock_no] != -1) {
                    if (left_time >= server_delay) {
                        this.sendEmptyMessageDelayed(msg.what, 1000);
                    } else {
                        sendDockingFinished(false);
                    }
                }
            }
        };
    }

    public void sendDockingFinished(boolean isSpeedUp) {
        if (!isSpeedUp) {
            JsonObject endDockInfo = new JsonObject();
            endDockInfo.addProperty("dock_no", dock_no);
            if(isGameDataLoaded()) {
                JsonObject shipData = KcaApiData.getUserShipDataById(ship_id, "ship_id");
                JsonObject kcShipData = KcaApiData.getKcShipDataById(shipData.get("ship_id").getAsInt(), "name");
                String shipName = kcShipData.get("name").getAsString();
                endDockInfo.addProperty("ship_name", shipName);
            } else {
                endDockInfo.addProperty("ship_name", "");
            }

            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_NOTI_DOCK_FIN);
            bundle.putString("data", gson.toJson(endDockInfo));
            Message sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
        Log.e("KCA", String.format("ndock %d Finished", dock_no));
        complete_time_check[dock_no] = -1;
    }

    public void setCompleteTime(int dock, long time) {
        complete_time_check[dock] = time;
    }

    @Override
    public void run() {
        left_time = (int) ((complete_time_check[dock_no] - System.currentTimeMillis()) / 1000) - start_delay;
        // Log.e("KCA", "Expedition " + String.valueOf(mission_no) + ": " +
        // String.valueOf(left_time));
        mHandler.sendEmptyMessage(dock_no);
    }

    public void stopHandler() {
        mHandler = null;
    }
}
