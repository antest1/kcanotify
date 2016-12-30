package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static com.antest1.kcanotify.KcaConstants.*;

public class KcaExpedition implements Runnable {
    public int mission_no;
    public String mission_krname;
    public int kantai_idx;
    public String kantai_name;
    public int left_time;
    public Handler sHandler;
    public static Map<Integer, HashMap<String, String>> expeditionData;
    public static String[] left_time_str = {null, null, null};
    public static long[] complete_time_check = {-1, -1, -1};
    public static boolean[] canceled_flag = {false, false, false};
    public static int start_delay = 2;
    public static int server_delay = 60;
    public Handler mHandler = null;
    private Gson gson = new Gson();

    private boolean check_canceled(int idx) {
        if (canceled_flag[idx]) {
            canceled_flag[idx] = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean isHandlerLive() {
        return mHandler != null;
    }

    public KcaExpedition(int no, int kidx, String name, long time, Handler h) {
        mission_no = no;
        mission_krname = expeditionData.get(mission_no).get("krname");
        kantai_idx = kidx;
        kantai_name = name;

        left_time = -1;
        sHandler = h;

        complete_time_check[kantai_idx] = time;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (!KcaService.isServiceOn) return;

                int div = msg.what;
                //// Log.e("KCA", "From "+String.valueOf(div));

                int sec, min, hour;
                sec = left_time;
                min = sec / 60;
                hour = min / 60;
                sec = sec % 60;
                min = min % 60;

                String strTime = String.format("%02d:%02d:%02d", hour, min, sec);
                //// Log.e("KCA", strTime);
                if (Thread.currentThread().isInterrupted()) {
                    //// Log.e("KCA", String.valueOf(div)+" Interrupted");
                }

                left_time = (int) ((complete_time_check[kantai_idx] - System.currentTimeMillis()) / 1000) - start_delay;

                if (complete_time_check[kantai_idx] != -1) {
                    if (left_time >= server_delay) {
                        /*
						JSONObject leftExpInfo = new JSONObject();
						leftExpInfo.put("idx", kantai_idx);
						// String krname_space = new String(new char[10 -
						// mission_krname.length()]).replace("\0", " ");
						// leftExpInfo.put("str", String.format("[%d] %s%s %s",
						// mission_no, mission_krname, krname_space, strTime));
						leftExpInfo.put("str", String.format("[%02d] %s", mission_no, strTime));
						Bundle bundle = new Bundle();
						bundle.putString("url", KCA_API_NOTI_EXP_LEFT);
						bundle.putString("data", leftExpInfo.toJSONString());
						Message sMsg = sHandler.obtainMessage();
						sMsg.setData(bundle);
						sHandler.sendMessage(sMsg);
						*/
                        //Log.e("KCA", String.valueOf(mission_no));
                        JsonObject leftExpInfo = new JsonObject();
                        left_time_str[kantai_idx] = String.format("[%02d] %s", mission_no, strTime);
                        Bundle bundle = new Bundle();
                        bundle.putString("url", KCA_API_NOTI_EXP_LEFT);
                        bundle.putString("data", gson.toJson(leftExpInfo));
                        Message sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                        this.sendEmptyMessageDelayed(div, 1000);
                    } else {
                        left_time_str[kantai_idx] = null;
                        complete_time_check[kantai_idx] = -1;

                        if (!check_canceled(kantai_idx)) {
                            JsonObject endExpInfo = new JsonObject();

                            endExpInfo.addProperty("kantai_idx", kantai_idx);
                            endExpInfo.addProperty("kantai_name", kantai_name);
                            endExpInfo.addProperty("mission_no", mission_no);
                            endExpInfo.addProperty("mission_krname", mission_krname);

                            Bundle bundle = new Bundle();
                            bundle.putString("url", KCA_API_NOTI_EXP_FIN);
                            bundle.putString("data", gson.toJson(endExpInfo));
                            Message sMsg = sHandler.obtainMessage();
                            sMsg.setData(bundle);

                            sHandler.sendMessage(sMsg);
                            // Log.e("KCA", "Expedition " + String.valueOf(mission_no) +
                            // "Finished");
                        }
                    }
                }
            }
        };
    }

    @Override
    public void run() {
        int max_left_time = Integer.parseInt(expeditionData.get(mission_no).get("time")) * 60 - start_delay; // Minutes
        // ->
        // Seconds
        int calculated_left_time = (int) ((complete_time_check[kantai_idx] - System.currentTimeMillis()) / 1000) - start_delay;
        left_time = Math.min(max_left_time, calculated_left_time);
        // Log.e("KCA", "Expedition " + String.valueOf(mission_no) + ": " +
        // String.valueOf(left_time));
        mHandler.sendEmptyMessage(mission_no);
    }

    public void canceled(long arrive_time) {
        JsonObject endExpInfo = new JsonObject();

        endExpInfo.addProperty("kantai_idx", kantai_idx);
        endExpInfo.addProperty("kantai_name", kantai_name);
        endExpInfo.addProperty("mission_no", mission_no);
        endExpInfo.addProperty("mission_krname", mission_krname);

        complete_time_check[kantai_idx] = arrive_time;
        canceled_flag[kantai_idx] = true;

        Bundle bundle = new Bundle();
        bundle.putString("url", KCA_API_NOTI_EXP_CANCELED);
        bundle.putString("data", gson.toJson(endExpInfo));
        Message sMsg = sHandler.obtainMessage();
        sMsg.setData(bundle);

        sHandler.sendMessage(sMsg);
    }

    public void stopHandler() {
        mHandler = null;
    }
}
