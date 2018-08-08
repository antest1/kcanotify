package com.antest1.kcanotify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KcaPacketLogger extends SQLiteOpenHelper {
    private static final String LOG_PATH = "/logs";
    private static final String packetlog_db_name = "packetlogger_db";
    private static final String packetlog_table_name = "packetlogger_table";

    private static final String REQUEST = "0";
    private static final String RESPONSE = "1";

    public KcaPacketLogger(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, packetlog_db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE IF NOT EXISTS ".concat(packetlog_table_name).concat(" ( "));
        sb.append(" timestamp INTEGER, ");
        sb.append(" url TEXT, ");
        sb.append(" type INTEGER, ");
        sb.append(" data TEXT)");
        db.execSQL(sb.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + packetlog_table_name);
        onCreate(db);
    }


    public void log(String url, String req, String resp) {
        SQLiteDatabase db = null;
        SQLiteStatement statement;
        long timestamp;
        try {
            db = getWritableDatabase();
            db.beginTransaction();
            statement = db.compileStatement("INSERT INTO ".concat(packetlog_table_name).concat(" (timestamp, url, type, data) values (?, ?, ?, ?)"));

            timestamp = System.currentTimeMillis();
            statement.bindAllArgsAsStrings(new String[]{String.valueOf(timestamp), url, REQUEST, req});
            statement.execute();

            timestamp = System.currentTimeMillis();
            statement.bindAllArgsAsStrings(new String[]{String.valueOf(timestamp), url, RESPONSE, resp});
            statement.execute();

            statement.close();
            db.setTransactionSuccessful();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }

    public void clear() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(packetlog_table_name, "", null);
    }

    public boolean dump(Context context){
        File savedir = new File(context.getExternalFilesDir(null).getAbsolutePath().concat(LOG_PATH));
        if (!savedir.exists()) savedir.mkdirs();
        String exportPath = savedir.getPath();

        File data = Environment.getDataDirectory();
        FileChannel source, destination;
        String currentDBPath = "/data/"+ BuildConfig.APPLICATION_ID +"/databases/" + packetlog_db_name;
        String backupDBPath = packetlog_db_name;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(exportPath, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
