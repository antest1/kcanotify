package com.antest1.kcanotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.R.attr.id;
import static android.R.attr.value;
import static android.view.View.X;
import static org.apache.commons.lang3.StringUtils.split;

public class KcaDropLogger extends SQLiteOpenHelper {
    private static final String droplog_db_name = "droplogger_db";
    private static final String droplog_table_name = "droplog_table";
    private static final String[] droplog_condition_key = {"startdate", "enddate", "world", "map", "node", "maprank", "isboss", "rank"};

    public static String[] maprank_info;
    public static String ship_none, ship_full;

    public KcaDropLogger(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, droplog_db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE IF NOT EXISTS ".concat(droplog_table_name).concat(" ( "));
        sb.append(" key INTEGER PRIMARY KEY, ");
        sb.append(" timestamp INTEGER, ");
        sb.append(" world INTEGER, ");
        sb.append(" map INTEGER, ");
        sb.append(" node INTEGER, ");
        sb.append(" maprank INTEGER, ");
        sb.append(" isboss INTEGER, ");
        sb.append(" rank TEXT, ");
        sb.append(" enemy TEXT, ");
        sb.append(" ship_id INTEGER) ");
        db.execSQL(sb.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + droplog_table_name);
        onCreate(db);
    }

    public void recordDropLog(JsonObject data, boolean is_full) {
        long current_time = System.currentTimeMillis();
        int world = data.get("world").getAsInt();
        int map = data.get("map").getAsInt();
        int node = data.get("node").getAsInt();
        int difficulty = data.get("maprank").getAsInt();
        int is_boss = data.get("isboss").getAsBoolean() ? 1 : 0;
        String rank = data.get("rank").getAsString();
        String enemyinfo = data.get("enemy").toString();
        int ship_id = data.get("result").getAsInt();

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timestamp", current_time);
        values.put("world", world);
        values.put("map", map);
        values.put("node", node);
        values.put("maprank", difficulty);
        values.put("isboss", is_boss);
        values.put("rank", rank);
        values.put("enemy", enemyinfo);
        values.put("ship_id", is_full ? -1 : ship_id);

        db.insert(droplog_table_name, null, values);
    }

    public List<JsonObject> getDropLogWithCondition(JsonObject condition) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<JsonObject> result = new ArrayList<>();

        List<String> where_conds = new ArrayList<>();
        for (String key: droplog_condition_key) {
            if (condition.has(key)) {
                String value = condition.get(key).getAsString();
                if (condition.has(key)) {
                    if (key.equals("startdate")) {
                        where_conds.add(KcaUtils.format("%s>=%s", "timestamp", value));
                    } else if (key.equals("enddate")) {
                        where_conds.add(KcaUtils.format("%s<=%s", "timestamp", value));
                    } else if (key.equals("isboss")) {
                        where_conds.add(KcaUtils.format("%s>=%s", key, value));
                    } else if (key.equals("rank")) {
                        List<String> rankcond = new ArrayList<>();
                        String[] rank_list = value.split(",");
                        for (String rank: rank_list) {
                            rankcond.add(KcaUtils.format("%s=\"%s\"", key, rank));
                        }
                        where_conds.add(KcaUtils.format("(%s)", KcaUtils.joinStr(rankcond, " OR ")));
                    } else if (key.equals("maprank")) {
                        if (Integer.parseInt(value) > 0) {
                            where_conds.add(KcaUtils.format("%s=%s", key, value));
                        }
                    } else {
                        where_conds.add(KcaUtils.format("%s=%s", key, value));
                    }
                }
            }
        }
        String where_str = " WHERE ".concat(KcaUtils.joinStr(where_conds, " AND "));
        boolean is_desc = condition.get("isdesc").getAsInt() > 0;
        String order_str = is_desc ? "DESC" : "";
        Cursor c = db.rawQuery("SELECT * from "
                .concat(droplog_table_name)
                .concat(where_str)
                .concat(" ORDER BY key "
                .concat(order_str).trim()), null);

        while (c.moveToNext()) {
            JsonObject item = retrieveDataFromCursor(c);
            result.add(item);
        }
        c.close();
        return result;
    }

    public List<String> getFullStringDropLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> result = new ArrayList<>();

        Cursor c = db.rawQuery("SELECT * from "
                .concat(droplog_table_name)
                .concat(" ORDER BY key "), null);

        while (c.moveToNext()) {
            JsonObject item = retrieveDataFromCursor(c);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
            String timetext = dateFormat.format(new Date(item.get("timestamp").getAsLong()));

            int world = item.get("world").getAsInt();
            int map = item.get("map").getAsInt();
            int node = item.get("node").getAsInt();
            int maprank = item.get("maprank").getAsInt();

            String node_alpha = KcaApiData.getCurrentNodeAlphabet(world, map, node);
            String node_text = "";
            if (maprank > 0) node_text = KcaUtils.format("%d-%d[%s]-%s", world, map, maprank_info[maprank], node_alpha);
            else node_text = KcaUtils.format("%d-%d-%s", world, map, node_alpha);

            String isboss_text = "";
            if (item.get("isboss").getAsInt() > 0) isboss_text = "O";
            else isboss_text = "-";

            String rank_text = item.get("rank").getAsString();

            String ship_name_text = "";
            int ship_id = item.get("ship_id").getAsInt();
            if (ship_id <= 0) {
                if (ship_id == -1) ship_name_text = ship_full;
                else if (ship_id == 0) ship_name_text = ship_none;
            }
            else {
                JsonObject kc_data = KcaApiData.getKcShipDataById(ship_id, "name");
                if (kc_data != null) {
                    String kc_name = kc_data.get("name").getAsString();
                    ship_name_text = KcaApiData.getShipTranslation(kc_name, false);
                }
            }
            String str_item = KcaUtils.format("%s,%s,%s,%s,%s", timetext, node_text, isboss_text, rank_text, ship_name_text);
            result.add(str_item);
        }
        c.close();
        return result;
    }

    public String[] getMapList(int world) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> result = new ArrayList<>();

        Cursor c = db.rawQuery("SELECT map from "
                .concat(droplog_table_name)
                .concat(KcaUtils.format(" WHERE world=%d", world)), null);

        result.add("*");
        while (c.moveToNext()) {
            String value = String.valueOf(c.getInt(c.getColumnIndex("map")));
            if (!result.contains(value)) result.add(value);
        }

        Collections.sort(result, new IntStrComparator());
        c.close();

        String[] result_arr = new String[result.size()];
        result_arr = result.toArray(result_arr);

        return result_arr;
    }

    public String[] getNodeList(int world, int map) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> result = new ArrayList<>();

        Cursor c = db.rawQuery("SELECT node from "
                .concat(droplog_table_name)
                .concat(KcaUtils.format(" WHERE world=%d AND map=%d", world, map)), null);

        result.add("*");
        while (c.moveToNext()) {
            String value = String.valueOf(c.getInt(c.getColumnIndex("node")));
            String node_alpha = KcaApiData.getCurrentNodeAlphabet(world, map, Integer.parseInt(value));
            if (!value.equals(node_alpha)) value = KcaUtils.format("%s(%s)", value, node_alpha);
            if (!result.contains(value)) result.add(value);
        }

        Collections.sort(result, new IntStrComparator());
        c.close();

        String[] result_arr = new String[result.size()];
        result_arr = result.toArray(result_arr);

        return result_arr;
    }

    public JsonObject retrieveDataFromCursor(Cursor c) {
        JsonObject data = new JsonObject();
        String[] key_list = {"key", "timestamp", "world", "map", "node", "maprank", "isboss", "rank", "enemy", "ship_id"};
        for (String key: key_list) {
            if (key.equals("rank") || key.equals("enemy")) {
                data.addProperty(key, c.getString(c.getColumnIndex(key)));
            } else if (key.equals("timestamp")) {
                data.addProperty(key, c.getLong(c.getColumnIndex(key)));
            } else {
                data.addProperty(key, c.getInt(c.getColumnIndex(key)));
            }
        }
        return data;
    }

    public void clearDropLog() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(droplog_table_name, null, null);
    }

    public class IntStrComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2)
        {
            if (o1.equals(o2)) return 0;
            else if (o1.equals("*")) return -100;
            else if (o2.equals("*")) return 100;
            return Integer.parseInt(o1.split("\\(")[0]) - (Integer.parseInt(o2.split("\\(")[0]));
        }
    }

    public void test() {
    }
}
