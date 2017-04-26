package com.antest1.kcanotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Gyeong Bok Lee on 2017-04-26.
 */

public class KcaDBHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String db_name = "kcanotify_db";
    private static final String table_name = "kca_userdata";
    private static final String slotitem_table_name = "kca_slotitem";
    SQLiteDatabase db;

    public KcaDBHelper(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, db_name, factory, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE ".concat(table_name).concat(" ( "));
        sb.append(" KEY TEXT PRIMARY KEY, ");
        sb.append(" VALUE TEXT ) ");
        db.execSQL(sb.toString());

        sb = new StringBuffer();
        sb.append(" CREATE TABLE ".concat(slotitem_table_name).concat(" ( "));
        sb.append(" KEY INTEGER PRIMARY KEY, ");
        sb.append(" VALUE TEXT ) ");
        db.execSQL(sb.toString());
        Log.e("KCA", "table generated");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "drop table if exists "+table_name;
        db.execSQL(sql);
        sql = "drop table if exists "+slotitem_table_name;
        db.execSQL(sql);
        onCreate(db);
    }

    // for kca_userdata
    public String getValue(String key) {
        db = this.getReadableDatabase();
        Cursor c = db.query(table_name, null, "KEY=?", new String[]{key}, null, null, null, null);
        if (c.moveToFirst()) {
            return c.getString(c.getColumnIndex("VALUE"));
        } else {
            return null;
        }
    }

    public void putValue(String key, String value) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("KEY", key);
        values.put("VALUE", value);
        Cursor c = db.query(table_name, null, "KEY=?", new String[]{key}, null, null, null, null);
        if(c.moveToFirst()) {
            db.update(table_name, values, "KEY=?", new String[]{key});
        }
        else {
            db.insert(table_name, null, values);
        }
        c.close();
    }

    // for kca_slotitem
    public HashSet<Integer> getItemKeyList() {
        HashSet<Integer> set = new HashSet<>();
        db = this.getReadableDatabase();
        Cursor c = db.query(slotitem_table_name, null, null, null, null, null, null);
        while(c.moveToNext()) {
             set.add(c.getInt(c.getColumnIndex("KEY")));
        }
        c.close();
        return set;
    }

    public String getItemValue(int key) {
        db = this.getReadableDatabase();
        Cursor c = db.query(table_name, null, "KEY=?", new String[]{String.valueOf(key)}, null, null, null, null);
        if (c.moveToFirst()) {
            return c.getString(c.getColumnIndex("VALUE"));
        } else {
            return null;
        }
    }

    public void putItemValue(int key, String value) {
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("KEY", key);
        values.put("VALUE", value);
        Cursor c = db.query(slotitem_table_name, null, "KEY=?", new String[]{String.valueOf(key)}, null, null, null, null);
        if(c.moveToFirst()) {
            db.update(slotitem_table_name, values, "KEY=?", new String[]{String.valueOf(key)});
        }
        else {
            db.insert(slotitem_table_name, null, values);
        }
        c.close();
    }

    public void removeItemValue(String[] keys) {
        db = this.getWritableDatabase();
        String condition = "";
        for (int i = 0; i < keys.length; i++) {
            condition = condition.concat("KEY=").concat(keys[i]);
            if (i != keys.length - 1) {
                condition = condition.concat(" OR ");
            }
        }
        int result = db.delete(slotitem_table_name, condition, null);
        Log.e("KCA", condition + " " + String.valueOf(result));

    }

    public void test() {
        db = this.getReadableDatabase();
        Cursor c = db.query(table_name, null, null, null, null, null, null);
        while(c.moveToNext()) {
            String key = c.getString(c.getColumnIndex("KEY"));
            String value = c.getString(c.getColumnIndex("VALUE"));
            Log.e("KCA", String.format("%s -> %s (%d)", key, value.substring(0, Math.min(50, value.length())), value.length()));
        }
        /*
        c = db.query(slotitem_table_name, null, null, null, null, null, null);
        while(c.moveToNext()) {
            String key = c.getString(c.getColumnIndex("KEY"));
            String value = c.getString(c.getColumnIndex("VALUE"));
            Log.e("KCA", String.format("%s -> %s (%d)", key, value.substring(0, Math.min(50, value.length())), value.length()));
        }
        */
        c.close();
    }

    public void test2() {
        db = this.getReadableDatabase();
        int count = 0;
        Cursor c = db.query(slotitem_table_name, null, null, null, null, null, null);
        while(c.moveToNext()) {
            String key = c.getString(c.getColumnIndex("KEY"));
            String value = c.getString(c.getColumnIndex("VALUE"));
            Log.e("KCA", String.format("%s -> %s (%d)", key, value.substring(0, Math.min(50, value.length())), value.length()));
            count += 1;
        }
        Log.e("KCA", "Total: "+String.valueOf(count));
        c.close();
    }
}
