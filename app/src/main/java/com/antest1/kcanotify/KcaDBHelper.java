package com.antest1.kcanotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gyeong Bok Lee on 2017-04-26.
 */

public class KcaDBHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String db_name = "kcanotify_db";
    private static final String table_name = "kca_userdata";
    private static final String slotitem_table_name = "kca_slotitem";
    private static final String questlist_table_name = "kca_questlist";
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

        sb = new StringBuffer();
        sb.append(" CREATE TABLE ".concat(questlist_table_name).concat(" ( "));
        sb.append(" KEY INTEGER PRIMARY KEY, ");
        sb.append(" VALUE TEXT, ");
        sb.append(" TIME TEXT ) "); // YY-MM-DD-HH
        db.execSQL(sb.toString());
        Log.e("KCA", "table generated");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + table_name);
        db.execSQL("drop table if exists " + slotitem_table_name);
        db.execSQL("drop table if exists " + questlist_table_name);
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
        if (c.moveToFirst()) {
            db.update(table_name, values, "KEY=?", new String[]{key});
        } else {
            db.insert(table_name, null, values);
        }
        c.close();
    }

    // for kca_slotitem
    public int getItemCount() {
        int result = 0;
        db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT KEY from ".concat(slotitem_table_name), null);
        result = c.getCount();
        c.close();
        return result;
    }

    public HashSet<Integer> getItemKeyList() {
        HashSet<Integer> set = new HashSet<>();
        db = this.getReadableDatabase();
        Cursor c = db.query(slotitem_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            set.add(c.getInt(c.getColumnIndex("KEY")));
        }
        c.close();
        return set;
    }

    public String getItemValue(int key) {
        db = this.getReadableDatabase();
        Cursor c = db.query(slotitem_table_name, null, "KEY=?", new String[]{String.valueOf(key)}, null, null, null, null);
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
        if (c.moveToFirst()) {
            db.update(slotitem_table_name, values, "KEY=?", new String[]{String.valueOf(key)});
        } else {
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

    public JsonArray getCurrentQuestList() {
        db = this.getReadableDatabase();
        JsonArray data = new JsonArray();
        Cursor c = db.rawQuery("SELECT VALUE FROM ".concat(questlist_table_name), null);
        while (c.moveToNext()) {
            data.add(new JsonParser().parse(c.getString(c.getColumnIndex("VALUE"))).getAsJsonObject());
        }
        return data;
    }

    // for kca_questlist
    public void checkValidQuest(int page, int lastpage, JsonArray api_list) {
        Date currentTime = Calendar.getInstance(Locale.JAPAN).getTime();
        SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd-HH");
        String[] current_time = df.format(currentTime).split("-");

        List<Integer> questIdList = new ArrayList<>();
        db = this.getWritableDatabase();
        if (api_list == null) {
            db.delete(slotitem_table_name, "", null);
        } else {
            for (int i = 0; i < api_list.size(); i++) {
                int index = i + 1;
                JsonElement api_list_item = api_list.get(i);
                if (api_list_item.isJsonObject()) {
                    JsonObject item = api_list_item.getAsJsonObject();
                    questIdList.add(item.get("api_no").getAsInt());
                }
            }

            // remove invalid quest
            if (page == 1) {
                db.delete(questlist_table_name, "KEY < ?", new String[]{String.valueOf(questIdList.get(0))});
            } else if (page == lastpage) {
                db.delete(questlist_table_name, "KEY > ?", new String[]{String.valueOf(questIdList.get(questIdList.size() - 1))});
            }
            for (int i = 0; i < questIdList.size() - 1; i++) {
                db.delete(questlist_table_name, "KEY > ? AND KEY < ?",
                        new String[]{String.valueOf(questIdList.get(i)), String.valueOf(questIdList.get(i + 1))});

                int quest_id = questIdList.get(i);
                String quest_str = getQuest(quest_id);
                if (quest_str != null) {
                    JsonObject quest_data = new JsonParser().parse(getQuest(quest_id)).getAsJsonObject();
                    int quest_type = quest_data.get("api_type").getAsInt();

                    String[] quest_time = getQuestDate(quest_id).split("-");
                    boolean reset_passed = Integer.parseInt(current_time[3]) >= 5;
                    switch(quest_type) {
                        case 1: // Daily
                            if (!quest_time[1].equals(current_time[1]) || !quest_time[2].equals(current_time[2])) {
                                db.delete(questlist_table_name, "KEY = ?", new String[]{String.valueOf(quest_id)});
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
                                    db.delete(questlist_table_name, "KEY = ?", new String[]{String.valueOf(quest_id)});
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;
                        case 3: // Monthly
                            if (!quest_time[1].equals(quest_time[2]) && reset_passed) {
                                db.delete(questlist_table_name, "KEY = ?", new String[]{String.valueOf(quest_id)});
                            }
                            break;
                        case 5: // Quarterly, Else
                            if (quest_id == 822 || quest_id == 854 || quest_id == 637 || quest_id == 643) { // Bq1, Bq2, F35, F39 (Quarterly)
                                int quest_month = Integer.parseInt(quest_time[1]);
                                int quest_quarter = quest_month - quest_month % 3;
                                int current_month = Integer.parseInt(current_time[1]);
                                int current_quarter = current_month - current_month % 3;
                                if (quest_quarter != current_quarter && reset_passed) {
                                    db.delete(questlist_table_name, "KEY = ?", new String[]{String.valueOf(quest_id)});
                                }
                            } else if (!quest_time[1].equals(current_time[1]) || !quest_time[2].equals(current_time[2])) {
                                db.delete(questlist_table_name, "KEY = ?", new String[]{String.valueOf(quest_id)});
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

            for (int i = 0; i < api_list.size(); i++) {
                JsonElement api_list_item = api_list.get(i);
                if (api_list_item.isJsonObject()) {
                    JsonObject item = api_list_item.getAsJsonObject();
                    int api_no = item.get("api_no").getAsInt();
                    int api_state = item.get("api_state").getAsInt();
                    if (api_state == 2 || api_state == 3) {
                        putQuest(api_no, item.toString());
                    } else {
                        removeQuest(api_no);
                    }
                }
            }
        }
        test3();
    }

    public String getQuest(int key) {
        db = this.getReadableDatabase();
        Cursor c = db.query(questlist_table_name, null, "KEY=?", new String[]{String.valueOf(key)}, null, null, null, null);
        if (c.moveToFirst()) {
            return c.getString(c.getColumnIndex("VALUE"));
        } else {
            return null;
        }
    }

    public String getQuestDate(int key) {
        db = this.getReadableDatabase();
        Cursor c = db.query(questlist_table_name, null, "KEY=?", new String[]{String.valueOf(key)}, null, null, null, null);
        if (c.moveToFirst()) {
            return c.getString(c.getColumnIndex("TIME"));
        } else {
            return null;
        }
    }

    public void putQuest(int key, String value) {
        Date currentTime = Calendar.getInstance(Locale.JAPAN).getTime();
        SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd-HH");
        String time = df.format(currentTime);

        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("KEY", key);
        values.put("VALUE", value);
        values.put("TIME", time);

        Cursor c = db.query(questlist_table_name, null, "KEY=?", new String[]{String.valueOf(key)}, null, null, null, null);
        if (c.moveToFirst()) {
            db.update(questlist_table_name, values, "KEY=?", new String[]{String.valueOf(key)});
        } else {
            db.insert(questlist_table_name, null, values);
        }
        c.close();
    }

    public void removeQuest(int key) {
        db = this.getWritableDatabase();
        db.delete(questlist_table_name, "KEY=?", new String[]{String.valueOf(key)});
    }

    // test code
    public void test() {
        db = this.getReadableDatabase();
        Cursor c = db.query(table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
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
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndex("KEY"));
            String value = c.getString(c.getColumnIndex("VALUE"));
            Log.e("KCA", String.format("%s -> %s (%d)", key, value.substring(0, Math.min(50, value.length())), value.length()));
            count += 1;
        }
        Log.e("KCA", "Total: " + String.valueOf(count));
        c.close();
    }

    public void test3() {
        db = this.getReadableDatabase();
        int count = 0;
        Cursor c = db.query(questlist_table_name, null, null, null, null, null, null);
        while (c.moveToNext()) {
            String key = c.getString(c.getColumnIndex("KEY"));
            String value = c.getString(c.getColumnIndex("VALUE"));
            String time = c.getString(c.getColumnIndex("TIME"));
            Log.e("KCA", String.format("%s\t%s\t%s", key, value.substring(0, Math.min(50, value.length())), time));
            count += 1;
        }
        Log.e("KCA", "Total: " + String.valueOf(count));
        c.close();
    }
}
