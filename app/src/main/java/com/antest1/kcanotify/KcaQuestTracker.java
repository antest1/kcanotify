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

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static android.R.attr.id;
import static android.R.attr.key;
import static android.R.attr.value;
import static com.antest1.kcanotify.KcaApiData.STYPE_AP;
import static com.antest1.kcanotify.KcaApiData.STYPE_BB;
import static com.antest1.kcanotify.KcaApiData.STYPE_BBV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CA;
import static com.antest1.kcanotify.KcaApiData.STYPE_CL;
import static com.antest1.kcanotify.KcaApiData.STYPE_CV;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVB;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVL;
import static com.antest1.kcanotify.KcaApiData.STYPE_DD;
import static com.antest1.kcanotify.KcaApiData.STYPE_SS;
import static com.antest1.kcanotify.KcaApiData.STYPE_SSV;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaApiData.kcShipData;
import static com.antest1.kcanotify.KcaConstants.API_REQ_MAP_START;

public class KcaQuestTracker extends SQLiteOpenHelper {
    private static final String qt_db_name = "quest_track_db";
    private static final String qt_table_name = "quest_track_table";

    public KcaQuestTracker(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, qt_db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE ".concat(qt_table_name).concat(" ( "));
        sb.append(" KEY INTEGER PRIMARY KEY, ");
        sb.append(" ACTIVE INTEGER, ");
        sb.append(" CND0 INTEGER, ");
        sb.append(" CND1 INTEGER, ");
        sb.append(" CND2 INTEGER, ");
        sb.append(" CND3 INTEGER, ");
        sb.append(" TIME TEXT ) ");
        db.execSQL(sb.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + qt_table_name);
        onCreate(db);
    }

    public void addQuestTrack(int id) {
        Date currentTime = Calendar.getInstance(Locale.JAPAN).getTime();
        SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd-HH");
        String time = df.format(currentTime);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ACTIVE", 1);
        Cursor c = db.query(qt_table_name, null, "KEY=?", new String[]{String.valueOf(id)}, null, null, null, null);
        if (c.moveToFirst()) {
            JsonObject questInfo = KcaApiData.getQuestTrackInfo(String.valueOf(id));
            int questType = questInfo.get("type").getAsInt();
            String questTime = c.getString(c.getColumnIndex("TIME"));
            if (checkQuestValid(questType, id, questTime)) {
                db.update(qt_table_name, values, "KEY=?", new String[]{String.valueOf(id)});
            } else {
                db.delete(qt_table_name, "KEY=?", new String[]{String.valueOf(id)});
                c.close();
                addQuestTrack(id);
                return;
            }
        } else {
            values.put("KEY", id);
            for (int i = 0; i < 4; i++) values.put("CND".concat(String.valueOf(i)), 0);
            values.put("TIME", time);
            db.insert(qt_table_name, null, values);
        }
        c.close();
        test();
    }

    public void removeQuestTrack(int id, boolean hard) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (hard) {
            db.delete(qt_table_name, "KEY=?", new String[]{String.valueOf(id)});
        } else {
            ContentValues values = new ContentValues();
            values.put("ACTIVE", 0);
            Cursor c = db.query(qt_table_name, null, "KEY=?", new String[]{String.valueOf(id)}, null, null, null, null);
            if (c.moveToFirst()) {
                db.update(qt_table_name, values, "KEY=?", new String[]{String.valueOf(id)});
            }
            c.close();
        }
        test();
    }

    public void deleteQuestTrackWithRange(int startId, int endId) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (startId == -1) {
            db.delete(qt_table_name, "KEY < ?", new String[]{String.valueOf(endId)});
        } else if (endId == -1) {
            db.delete(qt_table_name, "KEY > ?", new String[]{String.valueOf(startId)});
        } else {
            db.delete(qt_table_name, "KEY > ? AND KEY < ?",
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
        Date currentTime = Calendar.getInstance(Locale.JAPAN).getTime();
        SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd-HH");
        String[] current_time = df.format(currentTime).split("-");
        String[] quest_time = time.split("-");
        boolean reset_passed = Integer.parseInt(current_time[3]) >= 5;
        switch (type) {
            case 1: // Daily
                if (!quest_time[1].equals(current_time[1]) || !quest_time[2].equals(current_time[2])) {
                    valid_flag = false;
                }
                break;
            case 2: // Weekly
                SimpleDateFormat dateFormat = new SimpleDateFormat("yy MM dd");
                try {
                    Date date1 = dateFormat.parse(String.format("%s %s %s", quest_time[0], quest_time[1], quest_time[2]));
                    Date date2 = dateFormat.parse(String.format("%s %s %s", current_time[0], current_time[1], current_time[2]));
                    long diff = date2.getTime() - date1.getTime();
                    long datediff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                    if (datediff >= 7 && reset_passed) {
                        valid_flag = false;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case 3: // Monthly
                if (!quest_time[1].equals(current_time[1]) && reset_passed) {
                    valid_flag = false;
                }
                break;
            case 5: // Quarterly, Else
                if (id == 822 || id == 854 || id == 637 || id == 643) { // Bq1, Bq2, F35, F39 (Quarterly)
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
        return valid_flag;
    }

    private boolean isWinRank(String rank) {
        return (rank.equals("S") || rank.equals("A") || rank.equals("B"));
    }

    private boolean isGoodRank(String rank) {
        return (rank.equals("S") || rank.equals("A"));
    }

    public JsonArray getQuestTrackInfo(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        JsonArray info = new JsonArray();
        Cursor c = db.rawQuery("SELECT KEY, CND0, CND1, CND2, CND3 from "
                .concat(qt_table_name)
                .concat(" WHERE KEY=?"), new String[]{id});
        if (c.moveToFirst()) {
            info.add(c.getInt(c.getColumnIndex("CND0")));
            if (id.equals("214") || id.equals("854")) {
                for (int i = 1; i < 4; i++) {
                    info.add(c.getInt(c.getColumnIndex("CND".concat(String.valueOf(i)))));
                }
            }
        }
        c.close();
        return info;
    }

    public JsonObject updateBattleTracker(JsonObject data) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (data.has("api_url") && data.get("api_url").getAsString().equals(API_REQ_MAP_START)) { // Sortie Count
            int cond0 = 0;
            Cursor c = db.rawQuery("SELECT KEY, CND0 from "
                    .concat(qt_table_name)
                    .concat(" WHERE KEY=214 AND ACTIVE=1"), null);
            if (c.moveToFirst()) {
                ContentValues values = new ContentValues();
                cond0 = 1 + c.getInt(c.getColumnIndex("CND0"));
                values.put("CND0", cond0);
                db.update(qt_table_name, values, "KEY=214 AND ACTIVE=1", null);
            }
            c.close();
            JsonObject targetData = new JsonObject();
            targetData.addProperty("214", cond0);
            return targetData;
        }

        int world = data.get("world").getAsInt();
        int map = data.get("map").getAsInt();
        boolean isboss = data.get("isboss").getAsBoolean();
        String rank = data.get("result").getAsString();
        JsonArray ship_ke = data.getAsJsonArray("ship_ke");
        JsonArray deck_data = data.getAsJsonObject("deck_port").getAsJsonArray("api_deck_data");
        JsonArray afterhp = data.getAsJsonArray("afterhp");
        JsonArray ship_ke_combined = null;
        JsonArray aftercbhp = null;

        int cvcount = 0; // carrier
        int apcount = 0; // wa-class
        int sscount = 0; // submarine

        for (int i = 1; i < ship_ke.size(); i++) {
            int ship_id = ship_ke.get(i).getAsInt();
            if (ship_id == -1) break;
            JsonObject kcShipData = KcaApiData.getKcShipDataById(ship_ke.get(i).getAsInt(), "stype");
            int stype = kcShipData.get("stype").getAsInt();
            boolean isSunk = afterhp.get(i + 6).getAsInt() <= 0;
            if ((stype == STYPE_CV || stype == STYPE_CVL || stype == STYPE_CVB) && isSunk)
                cvcount += 1;
            if (stype == STYPE_AP && isSunk) apcount += 1;
            if ((stype == STYPE_SS || stype == STYPE_SSV) && isSunk) sscount += 1;
        }

        if (data.has("ship_ke_combined")) {
            ship_ke_combined = data.getAsJsonArray("ship_ke_combined");
            aftercbhp = data.getAsJsonArray("aftercbhp");
            for (int i = 1; i < ship_ke_combined.size(); i++) {
                JsonObject kcShipData = KcaApiData.getKcShipDataById(ship_ke_combined.get(i).getAsInt(), "stype");
                int stype = kcShipData.get("stype").getAsInt();
                boolean isSunk = aftercbhp.get(i + 6).getAsInt() <= 0;
                if ((stype == STYPE_CV || stype == STYPE_CVL || stype == STYPE_CVB) && isSunk)
                    cvcount += 1;
                if (stype == STYPE_AP && isSunk) apcount += 1;
                if ((stype == STYPE_SS || stype == STYPE_SSV) && isSunk) sscount += 1;
            }
        }

        Cursor c = db.rawQuery("SELECT KEY, CND0, CND1, CND2, CND3 from "
                .concat(qt_table_name).concat(" WHERE ACTIVE=1 ORDER BY KEY"), null);

        int dupflag = 0;
        int requiredShip = 0;
        JsonObject updateTarget = new JsonObject();
        while (c.moveToNext()) {
            boolean wflag = false;
            boolean fleetflag = true;
            String key = c.getString(c.getColumnIndex("KEY"));
            int cond0 = c.getInt(c.getColumnIndex("CND0"));
            int cond1 = c.getInt(c.getColumnIndex("CND1"));
            int cond2 = c.getInt(c.getColumnIndex("CND2"));
            int cond3 = c.getInt(c.getColumnIndex("CND3"));
            JsonArray targetData;
            switch (key) {
                case "210":
                    updateTarget.addProperty(key, cond0 + 1);
                    break;
                case "201":
                case "216":
                    if (isWinRank(rank)) {
                        updateTarget.addProperty(key, cond0 + 1);
                    }
                    break;
                case "211":
                case "220":
                    updateTarget.addProperty(key, cond0 + cvcount);
                    break;
                case "218":
                case "213":
                case "221":
                    updateTarget.addProperty(key, cond0 + apcount);
                    if (!key.equals("221")) dupflag += 1;
                    break;
                case "230":
                case "228":
                    updateTarget.addProperty(key, cond0 + sscount);
                    break;
                case "214":
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
                case "226":
                    wflag = world == 2 && isboss && isWinRank(rank);
                    break;
                case "229":
                    wflag = world == 4 && isboss && isWinRank(rank);
                    break;
                case "241":
                    wflag = world == 3 && map >= 3 && isboss && isWinRank(rank);
                    break;
                case "242":
                    wflag = world == 4 && map == 4 && isboss && isGoodRank(rank);
                    break;
                case "243":
                    wflag = world == 5 && map == 2 && isboss && rank.equals("S");
                    break;
                case "261":
                case "265":
                    wflag = world == 1 && map == 5 && isboss && isGoodRank(rank);
                    break;
                case "249": // 5전대
                    requiredShip = 3;
                    for (int i = 0; i < deck_data.size(); i++) {
                        int shipId = getUserShipDataById(deck_data.get(i).getAsInt(),
                                "ship_id").get("ship_id").getAsInt();
                        int kcShipId = getKcShipDataById(shipId, "id").get("id").getAsInt();
                        if (kcShipId == 62 || kcShipId == 265 || kcShipId == 319) requiredShip -= 1;
                        if (kcShipId == 63 || kcShipId == 192 || kcShipId == 266) requiredShip -= 1;
                        if (kcShipId == 65 || kcShipId == 194 || kcShipId == 268) requiredShip -= 1;
                        wflag = (world == 2 && map == 5 && isboss && rank.equals("S") && requiredShip == 0);
                    }
                    break;
                case "256": // 잠수함대
                    wflag = world == 6 && map == 1 && isboss && rank.equals("S");
                    break;
                case "257": // 수뢰전대
                    requiredShip = 0;
                    for (int i = 0; i < deck_data.size(); i++) {
                        int shipId = getUserShipDataById(deck_data.get(i).getAsInt(),
                                "ship_id").get("ship_id").getAsInt();
                        int kcShipType = getKcShipDataById(shipId, "stype").get("stype").getAsInt();
                        if ((i == 0 && kcShipType != STYPE_CL) || (kcShipType != STYPE_DD && kcShipType != STYPE_CL)) {
                            fleetflag = false;
                        }
                        if (kcShipType == STYPE_CL) requiredShip += 1;
                    }
                    wflag = world == 1 && map == 4 && isboss && rank.equals("S") && fleetflag && requiredShip <= 3;
                    break;
                case "259": // 수상타격
                    requiredShip = 0;
                    for (int i = 0; i < deck_data.size(); i++) {
                        int shipId = getUserShipDataById(deck_data.get(i).getAsInt(),
                                "ship_id").get("ship_id").getAsInt();
                        int kcShipType = getKcShipDataById(shipId, "stype").get("stype").getAsInt();
                        if (kcShipType == STYPE_BB || kcShipType == STYPE_BBV) requiredShip += 10;
                        if (kcShipType == STYPE_CL) requiredShip += 1;
                    }
                    wflag = world == 5 && map == 1 && isboss && rank.equals("S") && requiredShip == 31;
                    break;
                case "264": // 공모기동
                    requiredShip = 0;
                    for (int i = 0; i < deck_data.size(); i++) {
                        int shipId = getUserShipDataById(deck_data.get(i).getAsInt(),
                                "ship_id").get("ship_id").getAsInt();
                        int kcShipType = getKcShipDataById(shipId, "stype").get("stype").getAsInt();
                        if (kcShipType == STYPE_CV || kcShipType == STYPE_CVL || kcShipType == STYPE_CVB)
                            requiredShip += 10;
                        if (kcShipType == STYPE_DD) requiredShip += 1;
                    }
                    wflag = world == 4 && map == 2 && isboss && rank.equals("S") && requiredShip == 22;
                    break;
                case "266": // 수상반격
                    requiredShip = 0;
                    for (int i = 0; i < deck_data.size(); i++) {
                        int shipId = getUserShipDataById(deck_data.get(i).getAsInt(),
                                "ship_id").get("ship_id").getAsInt();
                        int kcShipType = getKcShipDataById(shipId, "stype").get("stype").getAsInt();
                        if ((i == 0 && kcShipType != STYPE_DD) || (kcShipType != STYPE_DD || kcShipType != STYPE_CL || kcShipType != STYPE_CA)) {
                            fleetflag = false;
                            break;
                        }
                        if (kcShipType == STYPE_CA) requiredShip += 100;
                        if (kcShipType == STYPE_CV) requiredShip += 10;
                        if (kcShipType == STYPE_DD) requiredShip += 1;
                    }
                    wflag = world == 2 && map == 5 && isboss && rank.equals("S") && fleetflag && requiredShip == 114;
                    break;
                case "822": // 오키노시마
                    wflag = world == 2 && map == 4 && isboss && rank.equals("S");
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
                    if (world == 6 && map == 4 && isboss && rank.equals("S"))
                        targetData.set(3, new JsonPrimitive(1));
                    updateTarget.add(key, targetData);
                    break;
                default:
                    break;
            }

            if (wflag) updateTarget.addProperty(key, cond0 + 1);

            if (dupflag == 2) {
                updateTarget.addProperty("213", updateTarget.get("213").getAsInt() + apcount);
                updateTarget.addProperty("218", updateTarget.get("218").getAsInt() + apcount);
            }
        }

        for (Map.Entry<String, JsonElement> entry : updateTarget.entrySet()) {
            String entryKey = entry.getKey();
            JsonElement entryValue = entry.getValue();
            if (entryValue.isJsonArray()) {
                JsonArray entryValueArray = entryValue.getAsJsonArray();
                ContentValues values = new ContentValues();
                for (int i = 0; i < 4; i++) {
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
        return updateTarget;
    }

    public void test() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor c = db.query(qt_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndex("KEY"));
            String active = c.getString(c.getColumnIndex("ACTIVE"));
            String cond0 = c.getString(c.getColumnIndex("CND0"));
            String time = c.getString(c.getColumnIndex("TIME"));
            Log.e("KCA-QT", String.format("%s -> %s %s %s", key, active, cond0, time));
            count += 1;
        }
        Log.e("KCA-QT", "Total: " + String.valueOf(count));
        c.close();
    }
}
