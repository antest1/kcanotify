package com.antest1.kcanotify;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by Gyeong Bok Lee on 2017-01-07.
 */

public class KcaUtils {
    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ").replaceAll("\t", "");
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }

    public static void writeCacheData(Context ctx, byte[] data, String filename) throws IOException {
        FileOutputStream fos = ctx.openFileOutput(filename, ctx.MODE_PRIVATE);
        fos.write(data);
        fos.close();
    }

    public static String getStringPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", ctx.MODE_PRIVATE);
        return pref.getString(key, "");
    }

    public static Boolean getBooleanPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", ctx.MODE_PRIVATE);
        return pref.getBoolean(key, false);
    }

    // 값 저장하기
    public static void setPreferences(Context ctx, String key, Object value) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else {
            editor.putString(key, value.toString());
        }
        editor.commit();
    }

    public static JsonObject readCacheData(Context ctx, String filename) {
        try {
            FileInputStream fis = ctx.openFileInput(filename);
            byte[] cache_data = new byte[fis.available()];
            //Toast.makeText(getApplicationContext(), String.format("Loading Cached Data %d", fis.available()), Toast.LENGTH_LONG).show();
            while (fis.read(cache_data) != -1) {
                ;
            }
            String cache_data_str = new String(cache_data, 0, cache_data.length);
            return new JsonParser().parse(cache_data_str).getAsJsonObject();
        } catch (FileNotFoundException e) {
            //new retrieveApiStartData().execute("", "down", "");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
