package com.antest1.kcanotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.antest1.kcanotify.KcaApiData.STYPE_AO;
import static com.antest1.kcanotify.KcaApiData.STYPE_AP;
import static com.antest1.kcanotify.KcaApiData.STYPE_AV;
import static com.antest1.kcanotify.KcaApiData.STYPE_BBV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CA;
import static com.antest1.kcanotify.KcaApiData.STYPE_CL;
import static com.antest1.kcanotify.KcaApiData.STYPE_CV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVB;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVL;
import static com.antest1.kcanotify.KcaApiData.STYPE_DD;
import static com.antest1.kcanotify.KcaApiData.STYPE_DE;
import static com.antest1.kcanotify.KcaApiData.STYPE_SS;
import static com.antest1.kcanotify.KcaApiData.STYPE_SSV;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaUtils.getJapanCalendarInstance;
import static com.antest1.kcanotify.KcaUtils.getJapanSimpleDataFormat;

public class KcaQuestTracker extends SQLiteOpenHelper {
    private static final String qt_db_name = "quest_track_db";
    private static final String qt_table_name = "quest_track_table";
    private final static int[] non_quarterly_quest_id = {211, 212};

    private final static int[] long_daily_practice_quests = {311, 318, 330, 337, 339, 342};
    private final static int[] quest_cont_quest_id = {411, 607, 608};
    private static boolean ap_dup_flag = false;

    public static String getName() {
        return qt_db_name;
    }

    public void setApDupFlag() {
        ap_dup_flag = true;
    }

    public void clearApDupFlag() {
        ap_dup_flag = false;
    }

    public static int getInitialCondValue(String id) {
        if (Arrays.binarySearch(quest_cont_quest_id, Integer.parseInt(id)) >= 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public KcaQuestTracker(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, qt_db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sb = " CREATE TABLE ".concat(qt_table_name).concat(" ( ") +
                " KEY INTEGER PRIMARY KEY, " +
                " ACTIVE INTEGER, " +
                " CND0 INTEGER, " +
                " CND1 INTEGER, " +
                " CND2 INTEGER, " +
                " CND3 INTEGER, " +
                " CND4 INTEGER, " +
                " CND5 INTEGER, " +
                " TYPE INTEGER, " +
                " TIME TEXT ) ";
        db.execSQL(sb);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 2) {
            db.execSQL("alter table ".concat(qt_table_name).concat(" add column CND4 INTEGER"));
            db.execSQL("alter table ".concat(qt_table_name).concat(" add column CND5 INTEGER"));
        } else {
            db.execSQL("drop table if exists " + qt_table_name);
            onCreate(db);
        }
    }

    public void addQuestTrack(int id) {
        if (!KcaApiData.isQuestTrackable(String.valueOf(id))) return;
        Date currentTime = getJapanCalendarInstance().getTime();
        SimpleDateFormat df = getJapanSimpleDataFormat("yy-MM-dd-HH");
        String time = df.format(currentTime);
        String id_str = String.valueOf(id);
        int quest_type = 0;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ACTIVE", 1);
        JsonObject questInfo = KcaApiData.getQuestTrackInfo(id_str);
        if (questInfo != null) quest_type = questInfo.get("type").getAsInt();
        Cursor c = db.query(qt_table_name, null, "KEY=?", new String[]{id_str}, null, null, null, null);
        if (c.moveToFirst()) {
            if (questInfo != null) {
                String questTime = c.getString(c.getColumnIndexOrThrow("TIME"));
                if (checkQuestValid(quest_type, id, questTime)) {
                    db.update(qt_table_name, values, "KEY=?", new String[]{id_str});
                } else {
                    db.delete(qt_table_name, "KEY=?", new String[]{id_str});
                    c.close();
                    addQuestTrack(id);
                    return;
                }
            }
        } else {
            values.put("KEY", id);
            values.put("CND0", getInitialCondValue(id_str));
            for (int i = 1; i < 4; i++) values.put("CND".concat(String.valueOf(i)), 0);
            values.put("TIME", time);
            values.put("TYPE", quest_type);
            db.insert(qt_table_name, null, values);
        }
        c.close();
        test();
    }

    public void removeQuestTrack(int id, boolean hard) {
        String id_str = String.valueOf(id);
        SQLiteDatabase db = this.getWritableDatabase();
        if (hard) {
            db.delete(qt_table_name, "KEY=?", new String[]{id_str});
        } else {
            ContentValues values = new ContentValues();
            values.put("ACTIVE", 0);
            Cursor c = db.query(qt_table_name, null, "KEY=?", new String[]{id_str}, null, null, null, null);
            if (c.moveToFirst()) {
                db.update(qt_table_name, values, "KEY=?", new String[]{id_str});
            }
            c.close();
        }
        test();
    }

    public void clearQuestTrack() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(qt_table_name, null, null);
        test();
    }

    public void deleteQuestTrackWithRange(int startId, int endId, String type_cond) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (startId == -1) {
            db.delete(qt_table_name, "KEY < ? AND ACTIVE=1".concat(type_cond), new String[]{String.valueOf(endId)});
        } else if (endId == -1) {
            db.delete(qt_table_name, "KEY > ? AND ACTIVE=1".concat(type_cond), new String[]{String.valueOf(startId)});
        } else {
            db.delete(qt_table_name, "KEY > ? AND KEY < ? AND ACTIVE=1".concat(type_cond),
                    new String[]{String.valueOf(startId), String.valueOf(endId)});
        }
    }

    public void updateQuestTrackValueWithId(String id, JsonArray updatevalue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        for (int i = 0; i < updatevalue.size(); i++) {
            values.put("CND".concat(String.valueOf(i)), updatevalue.get(i).getAsString());
        }
        db.update(qt_table_name, values, "KEY=?", new String[]{id});
    }

    public boolean checkQuestValid(int type, int id, String time) {
        boolean valid_flag = true;
        Date currentTime = getJapanCalendarInstance().getTime();
        SimpleDateFormat df = getJapanSimpleDataFormat("yy-MM-dd-HH");
        String[] current_time = df.format(currentTime).split("-");
        String[] quest_time = time.split("-");
        boolean reset_passed = Integer.parseInt(current_time[3]) >= 5;
        if (Arrays.binarySearch(long_daily_practice_quests, id) >= 0) { // Monthly/Quarterly Practice Quest
            if (!quest_time[1].equals(current_time[1]) || !quest_time[2].equals(current_time[2])) {
                valid_flag = false;
            }
        } else {
            switch (type) {
                case 1: // Daily
                    if (!quest_time[1].equals(current_time[1]) || !quest_time[2].equals(current_time[2])) {
                        valid_flag = false;
                    } else if (Integer.parseInt(quest_time[3]) < 5 && reset_passed) {
                        valid_flag = false;
                    }
                    break;
                case 2: // Weekly
                    SimpleDateFormat dateFormat = getJapanSimpleDataFormat("yy MM dd HH");
                    try {
                        JsonObject week_data = KcaUtils.getCurrentWeekData();
                        long start_time = week_data.get("start").getAsLong();
                        long end_time = week_data.get("end").getAsLong();
                        Date date1 = dateFormat.parse(KcaUtils.format("%s %s %s %s", quest_time[0], quest_time[1], quest_time[2], quest_time[3]));
                        if (date1 != null) {
                            long quest_timestamp = date1.getTime();
                            if (quest_timestamp < start_time || quest_timestamp >= end_time) {
                                valid_flag = false;
                            }
                        }
                    } catch (ParseException e) {
                        // do nothing
                    }
                    break;
                case 3: // Monthly
                    if (!quest_time[1].equals(current_time[1]) && reset_passed) {
                        valid_flag = false;
                    }
                    break;
                case 5: // Quarterly, Else
                    if (Arrays.binarySearch(non_quarterly_quest_id, id) < 0) {
                        int quest_month = Integer.parseInt(quest_time[1]);
                        int quest_quarter = quest_month - quest_month % 3;
                        int current_month = Integer.parseInt(current_time[1]);
                        int current_quarter = current_month - current_month % 3;
                        if (quest_quarter != current_quarter && reset_passed) {
                            valid_flag = false;
                        }
                    } else if (!quest_time[1].equals(current_time[1]) || !quest_time[2].equals(current_time[2])) {
                        valid_flag = false;
                    }
                    break;
                default:
                    break;
            }
        }
        return valid_flag;
    }

    private boolean isSRank(String rank) { return rank.equals("S"); }
    private boolean isGoodRank(String rank) {
        return (rank.equals("S") || rank.equals("A"));
    }
    private boolean isWinRank(String rank) { return isGoodRank(rank) || rank.equals("B"); }

    public JsonArray getQuestTrackInfo(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        JsonArray info = new JsonArray();
        Cursor c = db.rawQuery("SELECT KEY, CND0, CND1, CND2, CND3, CND4, CND5 from "
                .concat(qt_table_name)
                .concat(" WHERE KEY=?"), new String[]{id});
        if (c.moveToFirst()) {
            JsonObject questTrackInfo = KcaApiData.getQuestTrackInfo(id);
            if (questTrackInfo != null) {
                int cond_count = questTrackInfo.getAsJsonArray("cond").size();
                for (int i = 0; i < cond_count; i++) {
                    info.add(c.getInt(c.getColumnIndexOrThrow("CND".concat(String.valueOf(i)))));
                }
            }
        }
        c.close();
        return info;
    }

    public void updateIdCountTracker(String id) {
        updateIdCountTracker(id, 0);
    }

    public void updateIdCountTracker(String id, int idx) {
        SQLiteDatabase db = this.getWritableDatabase();
        String cond = "CND" + idx;
        Cursor c = db.rawQuery(KcaUtils.format("SELECT KEY, %s from ", cond)
                .concat(qt_table_name)
                .concat(" WHERE KEY=? AND ACTIVE=1"), new String[]{id});
        if (c.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put(cond, c.getInt(c.getColumnIndexOrThrow(cond)) + 1);
            db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{id});
        }
        c.close();
    }

    public void updateNodeTracker(JsonObject data) {
        SQLiteDatabase db = this.getWritableDatabase();
        int world = data.get("world").getAsInt();
        int map = data.get("map").getAsInt();
        int node = data.get("node").getAsInt();
        boolean is_start = data.get("isstart").getAsBoolean();
        JsonArray deck_data = data.getAsJsonObject("deck_port").getAsJsonArray("api_deck_data");
        JsonArray fleet_data = deck_data.get(0).getAsJsonObject().getAsJsonArray("api_ship");

        Cursor c = db.rawQuery("SELECT KEY, CND0, CND1, CND2, CND3, CND4, CND5 from "
                .concat(qt_table_name).concat(" WHERE ACTIVE=1 ORDER BY KEY"), null);

        int requiredShip;
        JsonObject updateTarget = new JsonObject();
        while (c.moveToNext()) {
            boolean wflag = false;
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            int cond0 = c.getInt(c.getColumnIndexOrThrow("CND0"));
            int cond1 = c.getInt(c.getColumnIndexOrThrow("CND1"));
            int cond2 = c.getInt(c.getColumnIndexOrThrow("CND2"));
            int cond3 = c.getInt(c.getColumnIndexOrThrow("CND3"));

            JsonArray targetData;
            switch (key) {
                case "214": // 아호
                    targetData = new JsonArray();
                    if (is_start) {
                        targetData.add(cond0 + 1);
                    } else {
                        targetData.add(cond0);
                    }
                    targetData.add(cond1);
                    targetData.add(cond2);
                    targetData.add(cond3);
                    updateTarget.add(key, targetData);
                    break;
                case "861": // 1-6 분기퀘
                    boolean isend = (node == 14 || node == 17);
                    requiredShip = 0;
                    int requiredBBV = 0;
                    int requiredAO = 0;
                    for (int i = 0; i < fleet_data.size(); i++) {
                        int item = fleet_data.get(i).getAsInt();
                        if (item == -1) break;
                        JsonObject shipData = getUserShipDataById(item, "ship_id");
                        if (shipData != null) {
                            int shipId = shipData.get("ship_id").getAsInt();
                            JsonObject kcShipData = getKcShipDataById(shipId, "stype");
                            if (kcShipData != null) {
                                int kcShipType = kcShipData.get("stype").getAsInt();
                                if (kcShipType == STYPE_BBV) requiredBBV += 1;
                                if (kcShipType == STYPE_AO) requiredAO += 1;
                            }
                        }
                    }
                    if (requiredBBV >= 2) requiredShip += 1;
                    if (requiredAO >= 2) requiredShip += 1;
                    wflag = world == 1 && map == 6 && isend && requiredShip > 0;
                    break;
                default:
                    break;
            }
            if (wflag) updateTarget.addProperty(key, cond0 + 1);
        }

        for (Map.Entry<String, JsonElement> entry : updateTarget.entrySet()) {
            String entryKey = entry.getKey();
            JsonElement entryValue = entry.getValue();
            if (entryValue.isJsonArray()) {
                JsonArray entryValueArray = entryValue.getAsJsonArray();
                ContentValues values = new ContentValues();
                for (int i = 0; i < entryValueArray.size(); i++) {
                    values.put("CND".concat(String.valueOf(i)), String.valueOf(entryValueArray.get(i)));
                }
                db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{entryKey});
            } else {
                int entryValueData = entryValue.getAsInt();
                ContentValues values = new ContentValues();
                values.put("CND0", entryValueData);
                if (ap_dup_flag && checkApQuestActive() && entryKey.equals("212") || entryKey.equals("218")) {
                    db.update(qt_table_name, values, "KEY=?", new String[]{entryKey});
                } else {
                    db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{entryKey});
                }
            }
        }
        c.close();
    }

    public void updateQuestTracker(JsonObject data) {
        SQLiteDatabase db = this.getWritableDatabase();
        String rank = data.get("result").getAsString();

        JsonArray deck_data = data.getAsJsonObject("deck_port").getAsJsonArray("api_deck_data");
        JsonArray fleet_data = deck_data.get(0).getAsJsonObject().getAsJsonArray("api_ship");

        Cursor c = db.rawQuery("SELECT KEY, CND0 from "
                .concat(qt_table_name).concat(" WHERE ACTIVE=1 ORDER BY KEY"), null);

        int requiredShip;
        JsonObject updateTarget = new JsonObject();

        ArrayList<Integer> kcShipIdList = new ArrayList<>();
        ArrayList<Integer> kcShipTypeList = new ArrayList<>();

        for (int i = 0; i < fleet_data.size(); i++) {
            int item = fleet_data.get(i).getAsInt();
            if (item == -1) break;
            JsonObject shipData = getUserShipDataById(item, "ship_id");
            if (shipData != null) {
                int shipId = shipData.get("ship_id").getAsInt();
                JsonObject kcShipData = getKcShipDataById(shipId, "id,stype");
                if (kcShipData != null) {
                    kcShipIdList.add(kcShipData.get("id").getAsInt());
                    kcShipTypeList.add(kcShipData.get("stype").getAsInt());
                }
            }
        }
        int flagshipKcShipType = kcShipTypeList.get(0);

        while (c.moveToNext()) {
            boolean wflag = false;
            boolean fleetflag = true;
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            int cond0 = c.getInt(c.getColumnIndexOrThrow("CND0"));

            switch (key) {
                case "303":
                    wflag = true;
                    break;
                case "304":
                case "302":
                case "311":
                    wflag = isWinRank(rank);
                    break;
                case "318":
                    requiredShip = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CL) requiredShip += 1;
                    }
                    wflag = (requiredShip == 2) && isWinRank(rank);
                    break;
                case "330":
                    requiredShip = 0;
                    if (flagshipKcShipType != STYPE_CV && flagshipKcShipType != STYPE_CVL && flagshipKcShipType != STYPE_CVB) {
                        fleetflag = false;
                    } else {
                        for (int kcShipType: kcShipTypeList) {
                            if (kcShipType == STYPE_CV || kcShipType == STYPE_CVL || kcShipType == STYPE_CVB) requiredShip += 1;
                        }
                    }
                    wflag = fleetflag && (requiredShip == 2) && isWinRank(rank);
                    break;
                case "337":
                    requiredShip = 4;
                    for (int kcShipId: kcShipIdList) {
                        if (kcShipId == 49 || kcShipId == 253 || kcShipId == 464 || kcShipId == 470) requiredShip -= 1;
                        if (kcShipId == 48 || kcShipId == 198 || kcShipId == 252) requiredShip -= 1;
                        if (kcShipId == 17 || kcShipId == 225 || kcShipId == 566) requiredShip -= 1;
                        if (kcShipId == 18 || kcShipId == 226 || kcShipId == 567) requiredShip -= 1;
                    }
                    wflag = (requiredShip == 0) && isSRank(rank);
                    break;
                case "339":
                    requiredShip = 4;
                    for (int kcShipId: kcShipIdList) {
                        if (kcShipId == 12 || kcShipId == 206 || kcShipId == 666) requiredShip -= 1;
                        if (kcShipId == 368 || kcShipId == 486 || kcShipId == 647) requiredShip -= 1;
                        if (kcShipId == 13 || kcShipId == 195 || kcShipId == 207) requiredShip -= 1;
                        if (kcShipId == 14 || kcShipId == 208 || kcShipId == 627) requiredShip -= 1;
                    }
                    wflag = (requiredShip == 0) && isSRank(rank);
                    break;
                case "342":
                    requiredShip = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CL) requiredShip += 1;
                        if (kcShipType == STYPE_DD || kcShipType == STYPE_DE) requiredShip += 10;
                    }
                    wflag = (requiredShip >= 40 || requiredShip == 31) && isGoodRank(rank);
                    break;
                default:
                    break;
            }
            if (wflag) updateTarget.addProperty(key, cond0 + 1);
        }

        for (Map.Entry<String, JsonElement> entry : updateTarget.entrySet()) {
            String entryKey = entry.getKey();
            JsonElement entryValue = entry.getValue();
            if (entryValue.isJsonArray()) {
                JsonArray entryValueArray = entryValue.getAsJsonArray();
                ContentValues values = new ContentValues();
                for (int i = 0; i < entryValueArray.size(); i++) {
                    values.put("CND".concat(String.valueOf(i)), String.valueOf(entryValueArray.get(i)));
                }
                db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{entryKey});
            } else {
                int entryValueData = entryValue.getAsInt();
                ContentValues values = new ContentValues();
                values.put("CND0", entryValueData);
                if (ap_dup_flag && checkApQuestActive() && entryKey.equals("212") || entryKey.equals("218")) {
                    db.update(qt_table_name, values, "KEY=?", new String[]{entryKey});
                } else {
                    db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{entryKey});
                }
            }
        }

        c.close();
    }

    public void updateBattleTracker(JsonObject data) {
        SQLiteDatabase db = this.getWritableDatabase();
        int world = data.get("world").getAsInt();
        int map = data.get("map").getAsInt();
        int node = data.get("node").getAsInt();
        boolean isboss = data.get("isboss").getAsBoolean();
        String rank = data.get("result").getAsString();

        JsonElement ship_ke_data = data.get("ship_ke");
        JsonArray ship_ke = new JsonArray();
        JsonArray afterhps_e = new JsonArray();
        if (!ship_ke_data.isJsonNull()) {
            ship_ke = data.getAsJsonArray("ship_ke");
            afterhps_e = data.getAsJsonArray("afterhps_e");
        }
        JsonArray deck_data = data.getAsJsonObject("deck_port").getAsJsonArray("api_deck_data");
        JsonArray fleet_data = deck_data.get(0).getAsJsonObject().getAsJsonArray("api_ship");

        JsonArray ship_ke_combined;
        JsonArray aftercbhps_e;

        int cvcount = 0; // carrier
        int apcount = 0; // wa-class
        int sscount = 0; // submarine

        for (int i = 0; i < ship_ke.size(); i++) {
            int ship_id = ship_ke.get(i).getAsInt();
            if (ship_id == -1) break;
            JsonObject kcShipData = KcaApiData.getKcShipDataById(ship_ke.get(i).getAsInt(), "stype");
            if (kcShipData != null) {
                int stype = kcShipData.get("stype").getAsInt();
                String hp_str = afterhps_e.get(i).getAsString();
                if (hp_str.contains("N")) continue;
                boolean isSunk = (Integer.parseInt(hp_str) <= 0);
                if ((stype == STYPE_CV || stype == STYPE_CVL || stype == STYPE_CVB) && isSunk)
                    cvcount += 1;
                if (stype == STYPE_AP && isSunk) apcount += 1;
                if ((stype == STYPE_SS || stype == STYPE_SSV) && isSunk) sscount += 1;
            }
        }

        if (data.has("ship_ke_combined")) {
            ship_ke_combined = data.getAsJsonArray("ship_ke_combined");
            aftercbhps_e = data.getAsJsonArray("aftercbhps_e");
            for (int i = 0; i < ship_ke_combined.size(); i++) {
                int ship_id = ship_ke_combined.get(i).getAsInt();
                if (ship_id == -1) break;
                JsonObject kcShipData = KcaApiData.getKcShipDataById(ship_id, "stype");
                if (kcShipData != null) {
                    int stype = kcShipData.get("stype").getAsInt();
                    boolean isSunk = aftercbhps_e.get(i).getAsInt() <= 0;
                    if ((stype == STYPE_CV || stype == STYPE_CVL || stype == STYPE_CVB) && isSunk)
                        cvcount += 1;
                    if (stype == STYPE_AP && isSunk) apcount += 1;
                    if ((stype == STYPE_SS || stype == STYPE_SSV) && isSunk) sscount += 1;
                }
            }
        }

        Cursor c = db.rawQuery("SELECT KEY, CND0, CND1, CND2, CND3, CND4, CND5 from "
                .concat(qt_table_name).concat(" WHERE ACTIVE=1 ORDER BY KEY"), null);

        int requiredShip;
        JsonObject updateTarget = new JsonObject();

        ArrayList<Integer> kcShipIdList = new ArrayList<>();
        ArrayList<Integer> kcShipTypeList = new ArrayList<>();

        for (int i = 0; i < fleet_data.size(); i++) {
            int item = fleet_data.get(i).getAsInt();
            if (item == -1) break;
            JsonObject shipData = getUserShipDataById(item, "ship_id");
            if (shipData != null) {
                int shipId = shipData.get("ship_id").getAsInt();
                JsonObject kcShipData = getKcShipDataById(shipId, "id,stype");
                if (kcShipData != null) {
                    kcShipIdList.add(kcShipData.get("id").getAsInt());
                    kcShipTypeList.add(kcShipData.get("stype").getAsInt());
                }
            }
        }
        int flagshipKcShipId = kcShipIdList.get(0);
        int flagshipKcShipType = kcShipTypeList.get(0);

        while (c.moveToNext()) {
            boolean wflag = false;
            boolean fleetflag = true;

            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            int cond0 = c.getInt(c.getColumnIndexOrThrow("CND0"));
            int cond1 = c.getInt(c.getColumnIndexOrThrow("CND1"));
            int cond2 = c.getInt(c.getColumnIndexOrThrow("CND2"));
            int cond3 = c.getInt(c.getColumnIndexOrThrow("CND3"));
            int cond4 = c.getInt(c.getColumnIndexOrThrow("CND4"));

            JsonArray targetData;
            switch (key) {
                case "210": // 출격10회
                    updateTarget.addProperty(key, cond0 + 1);
                    break;
                case "201": // 출격일퀘
                case "216": // 주력격멸
                    if (isWinRank(rank)) {
                        updateTarget.addProperty(key, cond0 + 1);
                    }
                    break;
                case "211": // 항모3회
                case "220": // 이호
                    updateTarget.addProperty(key, cond0 + cvcount);
                    break;
                case "218": // 보급3회
                case "212": // 보급5회
                    int mult = 1;
                    if (ap_dup_flag) mult = 2;
                    updateTarget.addProperty(key, cond0 + Math.min(apcount * mult, 4));
                    break;
                case "213": // 통상파괴
                case "221": // 로호
                    updateTarget.addProperty(key, cond0 + apcount);
                    break;
                case "230": // 잠수일퀘
                case "228": // 해상호위
                    updateTarget.addProperty(key, cond0 + sscount);
                    break;
                case "214": // 아호
                    targetData = new JsonArray();
                    targetData.add(cond0);
                    if (isboss) targetData.add(cond1 + 1);
                    else targetData.add(cond1);

                    if (isboss && isWinRank(rank)) targetData.add(cond2 + 1);
                    else targetData.add(cond2);

                    if (rank.equals("S")) {
                        targetData.add(cond3 + 1);
                    } else {
                        targetData.add(cond3);
                    }
                    updateTarget.add(key, targetData);
                    break;
                case "226": // 남서
                    wflag = world == 2 && isboss && isWinRank(rank);
                    break;
                case "229": // 동방
                    wflag = world == 4 && isboss && isWinRank(rank);
                    break;
                case "241": // 북방
                    wflag = world == 3 && map >= 3 && isboss && isWinRank(rank);
                    break;
                case "242": // 중추
                    wflag = world == 4 && map == 4 && isboss && isGoodRank(rank);
                    break;
                case "243": // 산호
                    wflag = world == 5 && map == 2 && isboss && isSRank(rank);
                    break;
                case "261": // 해상안전
                case "265": // 호위월간
                    wflag = world == 1 && map == 5 && isboss && isGoodRank(rank);
                    break;
                case "249": // 5전대
                    requiredShip = 3;
                    for (int kcShipId: kcShipIdList) {
                        if (kcShipId == 62 || kcShipId == 265 || kcShipId == 319) requiredShip -= 1;
                        if (kcShipId == 63 || kcShipId == 192 || kcShipId == 266) requiredShip -= 1;
                        if (kcShipId == 65 || kcShipId == 194 || kcShipId == 268) requiredShip -= 1;
                    }
                    wflag = (world == 2 && map == 5 && isboss && isSRank(rank) && requiredShip == 0);
                    break;
                case "256": // 잠수함대
                    wflag = world == 6 && map == 1 && isboss && isSRank(rank);
                    break;
                case "257": // 수뢰전대
                    requiredShip = 0;
                    if (flagshipKcShipType != STYPE_CL) {
                        fleetflag = false;
                    } else {
                        for (int kcShipType: kcShipTypeList) {
                            if (kcShipType != STYPE_DD && kcShipType != STYPE_CL) {
                                fleetflag = false;
                                break;
                            } else if (kcShipType == STYPE_CL) {
                                requiredShip += 1;
                            }
                        }
                    }
                    wflag = world == 1 && map == 4 && isboss && isSRank(rank) && fleetflag && requiredShip <= 3;
                    break;
                case "259": // 수상타격
                    requiredShip = 0;
                    // required_bb: 이세급, 후소급, 나가토급, 야마토급
                    int[] bb_ise_class = {77, 82, 87, 88, 553, 554};
                    int[] bb_fusou_class = {26, 27, 286, 287, 411, 412};
                    int[] bb_nagato_class = {80, 81, 275, 276, 541, 573};
                    int[] bb_yamato_class = {131, 136, 143, 148, 546, 911, 916};
                    for (int kcShipId: kcShipIdList) {
                        if (Arrays.binarySearch(bb_ise_class, kcShipId) >= 0) requiredShip += 10;
                        if (Arrays.binarySearch(bb_fusou_class, kcShipId) >= 0) requiredShip += 10;
                        if (Arrays.binarySearch(bb_nagato_class, kcShipId) >= 0) requiredShip += 10;
                        if (Arrays.binarySearch(bb_yamato_class, kcShipId) >= 0) requiredShip += 10;
                    }
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CL) requiredShip += 1;
                    }
                    wflag = world == 5 && map == 1 && isboss && isSRank(rank) && requiredShip == 31;
                    break;
                case "264": // 공모기동
                    requiredShip = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CV || kcShipType == STYPE_CVL || kcShipType == STYPE_CVB)
                            requiredShip += 10;
                        if (kcShipType == STYPE_DD) requiredShip += 1;
                    }
                    wflag = world == 4 && map == 2 && isboss && isSRank(rank) && requiredShip == 22;
                    break;
                case "266": // 수상반격
                    requiredShip = 0;
                    if (flagshipKcShipType != STYPE_DD) {
                        fleetflag = false;
                    } else {
                        for (int kcShipType: kcShipTypeList) {
                            if ((kcShipType != STYPE_DD && kcShipType != STYPE_CL && kcShipType != STYPE_CA)) {
                                fleetflag = false;
                                break;
                            }
                            if (kcShipType == STYPE_CA) requiredShip += 100;
                            if (kcShipType == STYPE_CL) requiredShip += 10;
                            if (kcShipType == STYPE_DD) requiredShip += 1;
                        }
                    }
                    wflag = world == 2 && map == 5 && isboss && isSRank(rank) && fleetflag && requiredShip == 114;
                    break;
                case "280": // 병참선
                    requiredShip = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CVL) requiredShip += 10;
                        if (kcShipType == STYPE_CL) requiredShip += 10;
                        if (kcShipType == STYPE_DD) requiredShip += 1;
                        if (kcShipType == STYPE_DE) requiredShip += 1;
                    }

                    if (requiredShip % 10 >= 3 && requiredShip / 10 >= 1) {
                        targetData = new JsonArray();
                        targetData.add(cond0);
                        targetData.add(cond1);
                        targetData.add(cond2);
                        targetData.add(cond3);
                        if (world == 1 && map == 2 && isboss && isSRank(rank))
                            targetData.set(0, new JsonPrimitive(1));
                        if (world == 1 && map == 3 && isboss && isSRank(rank))
                            targetData.set(1, new JsonPrimitive(1));
                        if (world == 1 && map == 4 && isboss && isSRank(rank))
                            targetData.set(2, new JsonPrimitive(1));
                        if (world == 2 && map == 1 && isboss && isSRank(rank))
                            targetData.set(3, new JsonPrimitive(1));
                        updateTarget.add(key, targetData);
                    }
                    break;
                case "822": // 오키노시마
                    wflag = world == 2 && map == 4 && isboss && isSRank(rank);
                    break;
                case "854": // Z전단
                    targetData = new JsonArray();
                    targetData.add(cond0);
                    targetData.add(cond1);
                    targetData.add(cond2);
                    targetData.add(cond3);
                    if (world == 2 && map == 4 && isboss && isGoodRank(rank))
                        targetData.set(0, new JsonPrimitive(1));
                    if (world == 6 && map == 1 && isboss && isGoodRank(rank))
                        targetData.set(1, new JsonPrimitive(1));
                    if (world == 6 && map == 3 && isboss && isGoodRank(rank))
                        targetData.set(2, new JsonPrimitive(1));
                    if (world == 6 && map == 4 && isboss && isSRank(rank))
                        targetData.set(3, new JsonPrimitive(1));
                    updateTarget.add(key, targetData);
                    break;
                case "872": // Z후단
                    targetData = new JsonArray();
                    targetData.add(cond0);
                    targetData.add(cond1);
                    targetData.add(cond2);
                    targetData.add(cond3);
                    if (world == 7 && map == 2 && node == 15 && isSRank(rank))
                        targetData.set(0, new JsonPrimitive(1));
                    if (world == 5 && map == 5 && isboss && isSRank(rank))
                        targetData.set(1, new JsonPrimitive(1));
                    if (world == 6 && map == 2 && isboss && isSRank(rank))
                        targetData.set(2, new JsonPrimitive(1));
                    if (world == 6 && map == 5 && isboss && isSRank(rank))
                        targetData.set(3, new JsonPrimitive(1));
                    updateTarget.add(key, targetData);
                    break;
                case "862": // 6-3 분기퀘
                    requiredShip = 0;
                    int requiredCL = 0;
                    int requiredAV = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CL) requiredCL += 1;
                        if (kcShipType == STYPE_AV) requiredAV += 1;
                    }
                    if (requiredCL >= 2) requiredShip += 1;
                    if (requiredAV >= 1) requiredShip += 1;
                    wflag = world == 6 && map == 3 && isboss && isGoodRank(rank) && requiredShip == 2;
                    break;
                case "873": // 3해역 분기퀘
                    targetData = new JsonArray();
                    targetData.add(cond0);
                    targetData.add(cond1);
                    targetData.add(cond2);
                    if (world == 3 && map == 1 && isboss && isGoodRank(rank))
                        targetData.set(0, new JsonPrimitive(1));
                    if (world == 3 && map == 2 && isboss && isGoodRank(rank))
                        targetData.set(1, new JsonPrimitive(1));
                    if (world == 3 && map == 3 && isboss && isGoodRank(rank))
                        targetData.set(2, new JsonPrimitive(1));
                    updateTarget.add(key, targetData);
                    break;
                case "845": // 4해역 분기퀘
                    targetData = new JsonArray();
                    targetData.add(cond0);
                    targetData.add(cond1);
                    targetData.add(cond2);
                    targetData.add(cond3);
                    targetData.add(cond4);
                    if (world == 4 && map == 1 && isboss && isSRank(rank))
                        targetData.set(0, new JsonPrimitive(1));
                    if (world == 4 && map == 2 && isboss && isSRank(rank))
                        targetData.set(1, new JsonPrimitive(1));
                    if (world == 4 && map == 3 && isboss && isSRank(rank))
                        targetData.set(2, new JsonPrimitive(1));
                    if (world == 4 && map == 4 && isboss && isSRank(rank))
                        targetData.set(3, new JsonPrimitive(1));
                    if (world == 4 && map == 5 && isboss && isSRank(rank))
                        targetData.set(4, new JsonPrimitive(1));
                    updateTarget.add(key, targetData);
                    break;
                case "875": // 5-4 분기퀘
                    requiredShip = 0;
                    int[] naganami_ids = {543}; // 나가나미 개2
                    int[] other_dd31_ids = {344, 345, 359, 569, 578, 649}; // 타카나미改/오키나미改/아사시모改
                    for (int kcShipId: kcShipIdList) {
                        if (Arrays.binarySearch(naganami_ids, kcShipId) >= 0) requiredShip += 10;
                        if (Arrays.binarySearch(other_dd31_ids, kcShipId) >= 0) requiredShip += 1;
                    }
                    wflag = world == 5 && map == 4 && isboss && isSRank(rank) && requiredShip >= 11;
                    break;
                case "893": // 정박지 전과퀘 (1-5, 7-1, 7-2-1, 7-2-2)
                    targetData = new JsonArray();
                    targetData.add(cond0);
                    targetData.add(cond1);
                    targetData.add(cond2);
                    targetData.add(cond3);
                    if (world == 1 && map == 5 && isboss && isSRank(rank))
                        targetData.set(0, new JsonPrimitive(1));
                    if (world == 7 && map == 1 && isboss && isSRank(rank))
                        targetData.set(1, new JsonPrimitive(1));
                    if (world == 7 && map == 2 && node == 7 && isSRank(rank))
                        targetData.set(2, new JsonPrimitive(1));
                    if (world == 7 && map == 2 && node == 15 && isSRank(rank))
                        targetData.set(3, new JsonPrimitive(1));
                    updateTarget.add(key, targetData);
                    break;
                case "894": // 항모병참선 분기퀘 (1-3, 1-4, 2-1, 2-2, 2-3)
                    requiredShip = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CV) requiredShip += 1;
                        if (kcShipType == STYPE_CVL) requiredShip += 1;
                        if (kcShipType == STYPE_CVB) requiredShip += 1;
                    }
                    if (requiredShip > 0) {
                        targetData = new JsonArray();
                        targetData.add(cond0);
                        targetData.add(cond1);
                        targetData.add(cond2);
                        targetData.add(cond3);
                        targetData.add(cond4);
                        if (world == 1 && map == 3 && isboss && isSRank(rank))
                            targetData.set(0, new JsonPrimitive(1));
                        if (world == 1 && map == 4 && isboss && isSRank(rank))
                            targetData.set(1, new JsonPrimitive(1));
                        if (world == 2 && map == 1 && isboss && isSRank(rank))
                            targetData.set(2, new JsonPrimitive(1));
                        if (world == 2 && map == 2 && isboss && isSRank(rank))
                            targetData.set(3, new JsonPrimitive(1));
                        if (world == 2 && map == 3 && isboss && isSRank(rank))
                            targetData.set(4, new JsonPrimitive(1));
                        updateTarget.add(key, targetData);
                    }
                    break;
                case "284": // 남서제도 분기퀘 (1-4, 2-1, 2-2, 2-3)
                    requiredShip = 0;
                    for (int kcShipType: kcShipTypeList) {
                        if (kcShipType == STYPE_CL) requiredShip += 10;
                        if (kcShipType == STYPE_CVL) requiredShip += 10;
                        if (kcShipType == STYPE_DD) requiredShip += 1;
                        if (kcShipType == STYPE_DE) requiredShip += 1;
                    }
                    if (requiredShip / 10 == 1 && requiredShip % 10 >= 3) { // 경순or경항모 1척, 구축or해방 3척 이상
                        targetData = new JsonArray();
                        targetData.add(cond0);
                        targetData.add(cond1);
                        targetData.add(cond2);
                        targetData.add(cond3);
                        if (world == 1 && map == 4 && isboss && isSRank(rank))
                            targetData.set(0, new JsonPrimitive(1));
                        if (world == 2 && map == 1 && isboss && isSRank(rank))
                            targetData.set(1, new JsonPrimitive(1));
                        if (world == 2 && map == 2 && isboss && isSRank(rank))
                            targetData.set(2, new JsonPrimitive(1));
                        if (world == 2 && map == 3 && isboss && isSRank(rank))
                            targetData.set(3, new JsonPrimitive(1));
                        updateTarget.add(key, targetData);
                    }
                    break;
                case "888": // 미카와 분기퀘: 쵸카이, 아오바, 키누가사, 카코, 후루타카, 텐류, 유바리 중 4택
                    requiredShip = 0;
                    int[] required_mikawa = {51, 59, 60, 61, 69, 115, 123, 142, 213, 262, 263, 264, 272, 293, 295, 416, 417, 427, 477, 622, 623, 624};
                    for (int kcShipId: kcShipIdList) {
                        if (Arrays.binarySearch(required_mikawa, kcShipId) >= 0) requiredShip += 4;
                    }
                    if (requiredShip >= 4) {
                        targetData = new JsonArray();
                        targetData.add(cond0);
                        targetData.add(cond1);
                        targetData.add(cond2);
                        if (world == 5 && map == 1 && isboss && isSRank(rank))
                            targetData.set(0, new JsonPrimitive(1));
                        if (world == 5 && map == 3 && isboss && isSRank(rank))
                            targetData.set(1, new JsonPrimitive(1));
                        if (world == 5 && map == 4 && isboss && isSRank(rank))
                            targetData.set(2, new JsonPrimitive(1));
                        updateTarget.add(key, targetData);
                    }
                    break;
                case "903": // 6수전 분기퀘 (유바리개2 기함, 유라개2 혹은 6수뢰구축 2척 이상)
                    requiredShip = 0;
                    int[] yuubari_kaini = {622, 623, 624};
                    int[] rokusuisen_dd = {1, 2, 30, 31, 164, 254, 255, 259, 261, 308, 434, 435};

                    if (Arrays.binarySearch(yuubari_kaini, flagshipKcShipId) >= 0) requiredShip += 10;
                    for (int kcShipId: kcShipIdList) {
                        if (kcShipId == 488) requiredShip += 2; // 유라 개2
                        else if (Arrays.binarySearch(rokusuisen_dd, kcShipId) >= 0) requiredShip += 1; // 6수전
                    }

                    if (requiredShip >= 12) {
                        targetData = new JsonArray();
                        targetData.add(cond0);
                        targetData.add(cond1);
                        targetData.add(cond2);
                        targetData.add(cond3);
                        if (world == 5 && map == 1 && isboss && isSRank(rank))
                            targetData.set(0, new JsonPrimitive(1));
                        if (world == 5 && map == 4 && isboss && isSRank(rank))
                            targetData.set(1, new JsonPrimitive(1));
                        if (world == 6 && map == 4 && isboss && isSRank(rank))
                            targetData.set(2, new JsonPrimitive(1));
                        if (world == 6 && map == 5 && isboss && isSRank(rank))
                            targetData.set(3, new JsonPrimitive(1));
                        updateTarget.add(key, targetData);
                    }
                    break;
                default:
                    break;
            }
            if (wflag) updateTarget.addProperty(key, cond0 + 1);
        }

        for (Map.Entry<String, JsonElement> entry : updateTarget.entrySet()) {
            String entryKey = entry.getKey();
            JsonElement entryValue = entry.getValue();
            if (entryValue.isJsonArray()) {
                JsonArray entryValueArray = entryValue.getAsJsonArray();
                ContentValues values = new ContentValues();
                for (int i = 0; i < entryValueArray.size(); i++) {
                    values.put("CND".concat(String.valueOf(i)), String.valueOf(entryValueArray.get(i)));
                }
                db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{entryKey});
            } else {
                int entryValueData = entryValue.getAsInt();
                ContentValues values = new ContentValues();
                values.put("CND0", entryValueData);
                db.update(qt_table_name, values, "KEY=? AND ACTIVE=1", new String[]{entryKey});
            }
        }
        c.close();
    }

    public boolean checkApQuestActive() {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean result = false;
        Cursor c = db.rawQuery("SELECT KEY from "
                .concat(qt_table_name)
                .concat(" WHERE ACTIVE=1 AND (KEY=212 OR KEY=218)"), null);
        if (c.moveToFirst()) result = true;
        c.close();
        return result;
    }

    public boolean check_quest_completed() {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean result = false;
        Cursor c = db.rawQuery("SELECT * from "
                .concat(qt_table_name)
                .concat(" WHERE ACTIVE=1"), null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            String cond0 = c.getString(c.getColumnIndexOrThrow("CND0"));
            String cond1 = c.getString(c.getColumnIndexOrThrow("CND1"));
            String cond2 = c.getString(c.getColumnIndexOrThrow("CND2"));
            String cond3 = c.getString(c.getColumnIndexOrThrow("CND3"));
            String cond4 = c.getString(c.getColumnIndexOrThrow("CND4"));
            String cond5 = c.getString(c.getColumnIndexOrThrow("CND5"));
            String time = c.getString(c.getColumnIndexOrThrow("TIME"));
            String[] cond_value = {cond0, cond1, cond2, cond3, cond4, cond5};
            JsonObject questTrackInfo = KcaApiData.getQuestTrackInfo(key);
            if (questTrackInfo != null) {
                int counter = 0;
                JsonArray cond = questTrackInfo.getAsJsonArray("cond");
                int type = questTrackInfo.get("type").getAsInt();
                for (int i = 0; i < cond.size(); i++) {
                    if (cond_value[i] == null || cond_value[i].isEmpty()) continue;
                    if(Integer.parseInt(cond_value[i]) >= Integer.parseInt(cond.get(i).getAsString())) {
                        if (!checkQuestValid(type, Integer.parseInt(key), time)) {
                            db.delete(qt_table_name, "KEY=?", new String[]{key});
                        } else {
                            counter += 1;
                        }
                    }
                }
                if (counter == cond.size()) {
                    result = true;
                }
            }
        }
        c.close();
        return result;
    }

    public String getQuestTrackerDump() {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder sb = new StringBuilder();
        Cursor c = db.query(qt_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            String active = c.getString(c.getColumnIndexOrThrow("ACTIVE"));
            int cond0 = c.getInt(c.getColumnIndexOrThrow("CND0"));
            int cond1 = c.getInt(c.getColumnIndexOrThrow("CND1"));
            int cond2 = c.getInt(c.getColumnIndexOrThrow("CND2"));
            int cond3 = c.getInt(c.getColumnIndexOrThrow("CND3"));
            int cond4 = c.getInt(c.getColumnIndexOrThrow("CND4"));
            int cond5 = c.getInt(c.getColumnIndexOrThrow("CND5"));
            int type = c.getInt(c.getColumnIndexOrThrow("TYPE"));
            String time = c.getString(c.getColumnIndexOrThrow("TIME"));
            sb.append(KcaUtils.format("[%s] A:%s C:%02d,%02d,%02d,%02d,%02d,%02d K:%d T:%s\n",
                    key, active, cond0, cond1, cond2, cond3, cond4, cond5, type, time));
        }
        c.close();
        return sb.toString().trim();
    }

    public void clearInvalidQuestTrack() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(qt_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            String time = c.getString(c.getColumnIndexOrThrow("TIME"));
            JsonObject questTrackInfo = KcaApiData.getQuestTrackInfo(key);
            if (questTrackInfo != null) {
                int type = questTrackInfo.get("type").getAsInt();
                if (!checkQuestValid(type, Integer.parseInt(key), time)) {
                    db.delete(qt_table_name, "KEY=?", new String[]{key});
                }
            }
        }
        c.close();
    }

    public JsonArray getQuestTrackerData() {
        clearInvalidQuestTrack();
        JsonArray data = new JsonArray();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(qt_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            boolean active = c.getInt(c.getColumnIndexOrThrow("ACTIVE")) == 1;
            int cond0 = c.getInt(c.getColumnIndexOrThrow("CND0"));
            int cond1 = c.getInt(c.getColumnIndexOrThrow("CND1"));
            int cond2 = c.getInt(c.getColumnIndexOrThrow("CND2"));
            int cond3 = c.getInt(c.getColumnIndexOrThrow("CND3"));
            int cond4 = c.getInt(c.getColumnIndexOrThrow("CND4"));
            int cond5 = c.getInt(c.getColumnIndexOrThrow("CND5"));

            int[] cond_value = {cond0, cond1, cond2, cond3, cond4, cond5};
            JsonObject questItem = new JsonObject();
            questItem.addProperty("id", key);
            questItem.addProperty("active", active);
            JsonArray quest_count = new JsonArray();
            JsonObject questTrackInfo = KcaApiData.getQuestTrackInfo(key);
            if (questTrackInfo != null) {
                JsonArray cond_info = questTrackInfo.getAsJsonArray("cond");
                for (int i = 0; i < cond_info.size(); i++) {
                    if(cond_value[i] >= cond_info.get(i).getAsInt()) {
                        cond_value[i] = cond_info.get(i).getAsInt();
                    }
                    quest_count.add(cond_value[i] - getInitialCondValue(key));
                }
            }
            questItem.add("cond", quest_count);
            data.add(questItem);
        }
        c.close();
        return data;
    }

    public void test() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor c = db.query(qt_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndexOrThrow("KEY"));
            String active = c.getString(c.getColumnIndexOrThrow("ACTIVE"));
            String cond0 = c.getString(c.getColumnIndexOrThrow("CND0"));
            String time = c.getString(c.getColumnIndexOrThrow("TIME"));
            Log.e("KCA-QT", KcaUtils.format("%s -> %s %s %s", key, active, cond0, time));
            count += 1;
        }
        Log.e("KCA-QT", "Total: " + count);
        c.close();
    }
}
