package com.antest1.kcanotify;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;

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

    public static byte[] gzipcompress(String value) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                new BufferedOutputStream(byteArrayOutputStream));
        gzipOutStream.write(value.getBytes());
        gzipOutStream.finish();
        gzipOutStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gzipdecompress(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ByteStreams.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private static int bytetoint(byte[] arr) {
        int csize = 0;
        for (int i = 0; i < arr.length; i++) {
            csize = csize << 4;
            if (arr[i] >= 0x30 && arr[i] <= 0x39) {
                csize += arr[i] - 0x30; // (0x30 = '0')
            } else if (arr[i] >= 0x61 && arr[i] <= 0x66) {
                csize += arr[i] - 0x61 + 0x0a; // (0x61 = 'a')
            } else if (arr[i] >= 0x41 && arr[i] <= 0x46) {
                csize += arr[i] - 0x41 + 0x0a; // (0x41 = 'A')
            }
        }
        return csize;
    }

    public static byte[] unchunkdata(byte[] contentBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int startIdx = 0;
        for (int i = 0; i < contentBytes.length - 1; i++) {
            if (contentBytes[i] == '\r' && contentBytes[i + 1] == '\n') {
                int size = bytetoint(Arrays.copyOfRange(contentBytes, startIdx, i));
                if (size == 0) break;
                int dataStart = i + 2;
                out.write(Arrays.copyOfRange(contentBytes, dataStart, dataStart + size));
                startIdx = dataStart + size + 2; // \r\n padding
                i = startIdx - 1;
            }
        }
        return out.toByteArray();
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (final byte b : a)
            sb.append(String.format("%02x ", b & 0xff));
        return sb.toString();
    }

    public static boolean[] makeExcludeFlag(int[] list) {
        boolean[] flag = {false,false,false,false,false,false};
        for (int i = 0; i < list.length; i++) {
            flag[list[i]] = true;
        }
        return flag;
    }

    public static boolean isPackageExist(Context context, String name) {
        boolean isExist = false;

        PackageManager pkgMgr = context.getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(name)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    public static Intent getKcIntent(Context context) {
        Intent kcIntent;
        if (isPackageExist(context, KC_PACKAGE_NAME)) {
            kcIntent = context.getPackageManager().getLaunchIntentForPackage(KC_PACKAGE_NAME);
            kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return kcIntent;
        } else {
            return null;
        }
    }

    public static String getLocaleInArray(Context context, String locale) {
        List<String> localeList = Arrays.asList(context.getResources().getStringArray(R.array.languageOptionValue));
        if(localeList.contains(locale)) {
            return locale;
        } else {
            return "en";
        }
    }

    public static Context getContextWithLocale(Context ac, Context bc) {
        Locale locale = new Locale(getStringPreferences(ac, PREF_KCA_LANGUAGE));
        Configuration configuration = new Configuration(ac.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            return bc.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            DisplayMetrics metrics = new DisplayMetrics();
            bc.getResources().updateConfiguration(configuration, bc.getResources().getDisplayMetrics());
            return bc;
        }
    }

    public static String getStringWithLocale(Context ac, Context bc, int id) {
        Locale locale = new Locale(getStringPreferences(ac, PREF_KCA_LANGUAGE));
        Configuration configuration = new Configuration(ac.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            return bc.createConfigurationContext(configuration).getResources().getString(id);
        } else {
            configuration.locale = locale;
            DisplayMetrics metrics = new DisplayMetrics();
            bc.getResources().updateConfiguration(configuration, bc.getResources().getDisplayMetrics());
            return bc.getResources().getString(id);
        }
    }
}
