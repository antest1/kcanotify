package com.antest1.kcanotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.R.id.ship_id;

public class KcaResourceLogger extends SQLiteOpenHelper {
    private static final String resourcelog_db_name = "resourcelogger_db";
    private static final String resourcelog_table_name = "resoucrelog_table";

    public KcaResourceLogger(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, resourcelog_db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE IF NOT EXISTS ".concat(resourcelog_table_name).concat(" ( "));
        sb.append(" key INTEGER PRIMARY KEY, ");
        sb.append(" timestamp INTEGER, ");
        sb.append(" res_fuel INTEGER, ");
        sb.append(" res_ammo INTEGER, ");
        sb.append(" res_steel INTEGER, ");
        sb.append(" res_bauxite INTEGER, ");
        sb.append(" con_bucket INTEGER, ");
        sb.append(" con_torch INTEGER, ");
        sb.append(" con_devmat INTEGER, ");
        sb.append(" con_screw INTEGER) ");
        db.execSQL(sb.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + resourcelog_table_name);
        onCreate(db);
    }

    public void recordResourceLog(JsonArray data, boolean is_object) {
        int[] new_data = {0, 0, 0, 0, 0, 0, 0, 0};

        long current_time = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            if (is_object) new_data[i] = data.get(i).getAsJsonObject().get("api_value").getAsInt();
            else new_data[i] = data.get(i).getAsInt();
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timestamp", current_time);
        values.put("res_fuel", new_data[0]);
        values.put("res_ammo", new_data[1]);
        values.put("res_steel", new_data[2]);
        values.put("res_bauxite", new_data[3]);
        values.put("con_torch", new_data[4]);
        values.put("con_bucket", new_data[5]);
        values.put("con_devmat", new_data[6]);
        values.put("con_screw", new_data[7]);

        db.insert(resourcelog_table_name, null, values);
    }

    public List<JsonObject> getResourceLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<JsonObject> result = new ArrayList<>();

        Cursor c = db.rawQuery("SELECT * from "
                .concat(resourcelog_table_name)
                .concat(" ORDER BY timestamp"), null);

        while (c.moveToNext()) {
            result.add(retrieveDataFromCursor(c));
        }
        c.close();
        return result;
    }


    public JsonObject getLatestResourceLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * from "
                .concat(resourcelog_table_name)
                .concat(" ORDER BY timestamp DESC LIMIT 1"), null);
        JsonObject result = new JsonObject();
        while (c.moveToNext()) {
            result = retrieveDataFromCursor(c);
        }
        c.close();
        return result;
    }

    public List<JsonObject> getResourceLogInRange(long start, long end) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<JsonObject> result = new ArrayList<>();

        String where_str = KcaUtils.format(" WHERE timestamp >= %d and timestamp <= %d", start, end);
        Cursor c = db.rawQuery("SELECT * from "
                .concat(resourcelog_table_name)
                .concat(where_str).concat(" ORDER BY timestamp"), null);

        while (c.moveToNext()) {
            result.add(retrieveDataFromCursor(c));
        }
        c.close();
        return result;
    }

    public JsonObject retrieveDataFromCursor(Cursor c) {
        JsonObject data = new JsonObject();
        String[] key_list = {"timestamp", "res_fuel", "res_ammo", "res_steel", "res_bauxite", "con_bucket", "con_torch", "con_devmat", "con_screw"};
        for (String key: key_list) {
            if (key.equals("timestamp")) {
                data.addProperty(key, c.getLong(c.getColumnIndex(key)));
            } else {
                data.addProperty(key, c.getInt(c.getColumnIndex(key)));
            }
        }
        return data;
    }

    public List<String> getFullStringResourceLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> result = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * from "
                .concat(resourcelog_table_name)
                .concat(" ORDER BY timestamp"), null);
        while (c.moveToNext()) {
            JsonObject item = retrieveDataFromCursor(c);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
            String timetext = dateFormat.format(new Date(item.get("timestamp").getAsLong()));
            List<String> row = new ArrayList<>();
            for (int i = 2; i < c.getColumnCount(); i++) {
                row.add(String.valueOf(c.getInt(i)));
            }
            String str_item = KcaUtils.format("%s,%s", timetext, KcaUtils.joinStr(row, ","));
            result.add(str_item);
        }
        c.close();
        return result;
    }


    public void clearResoureLog() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(resourcelog_table_name, null, null);
    }
}
