package com.antest1.kcanotify;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mariten.kanatools.KanaConverter;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_STARTDATA;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_DATALOAD;
import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_DATALOAD_ERROR_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_DISABLE_CUSTOMTOAST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KC_PACKAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_RES_USELOCAL;
import static com.antest1.kcanotify.KcaConstants.PREF_UPDATE_SERVER;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_FLAG;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_PREFIX;
import static com.mariten.kanatools.KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA;


public class KcaUtils {
    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ").replaceAll("\t", "");
    }

    public static String format(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    public static JsonElement parseJson(String v) {
        return new JsonParser().parse(v);
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        if (list.size() > 0) {
            int i;
            for (i = 0; i < list.size() - 1; i++) {
                resultStr = resultStr.concat(list.get(i));
                resultStr = resultStr.concat(delim);
            }
            resultStr = resultStr.concat(list.get(i));
        }
        return resultStr;
    }

    public static String getStringPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        try {
            return String.valueOf(pref.getInt(key, 0));
        } catch (Exception e) {
            // Nothing to do
        }
        return pref.getString(key, "");
    }

    public static Boolean getBooleanPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        return pref.getBoolean(key, false);
    }

    // 값 저장하기
    public static void setPreferences(Context ctx, String key, Object value) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putString(key, String.valueOf(value));
        } else {
            editor.putString(key, value.toString());
        }
        editor.commit();
    }

    public static String getUpdateServer(Context ctx) {
        return getStringPreferences(ctx, PREF_UPDATE_SERVER);
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
        byte[] unchunkedData = null;
        byte[] buffer = new byte[1024];
        ByteArrayInputStream bis = new ByteArrayInputStream(contentBytes);
        ChunkedInputStream cis = new ChunkedInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int read = -1;
        while ((read = cis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        unchunkedData = bos.toByteArray();
        bos.close();

        return unchunkedData;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (final byte b : a)
            sb.append(KcaUtils.format("%02x ", b & 0xff));
        return sb.toString();
    }

    public static boolean[] makeExcludeFlag(int[] list) {
        boolean[] flag = {false, false, false, false, false, false};
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
        String package_name = getStringPreferences(context, PREF_KC_PACKAGE);
        Intent kcIntent = context.getPackageManager().getLaunchIntentForPackage(package_name);
        return kcIntent;
    }

    public static Context getContextWithLocale(Context ac, Context bc) {
        Locale locale;
        String[] pref_locale = getStringPreferences(ac, PREF_KCA_LANGUAGE).split("-");
        if (pref_locale[0].toLowerCase().equals("default") || pref_locale.length < 2) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(pref_locale[0], pref_locale[1]);
        }
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
        return getContextWithLocale(ac, bc).getString(id);
    }

    public static JsonObject getJsonObjectCopy(JsonObject data) {
        return new JsonParser().parse(data.toString()).getAsJsonObject();
    }

    public static int setDefaultGameData(Context context, KcaDBHelper helper) {
        boolean valid_data = false;
        String current_version = getStringPreferences(context, PREF_KCA_DATA_VERSION);
        String default_version = context.getString(R.string.default_gamedata_version);

        if (helper.getJsonObjectValue(DB_KEY_STARTDATA) != null && KcaUtils.compareVersion(current_version, default_version)) {
            if (KcaApiData.isGameDataLoaded()) return 1;
            JsonObject start_data = helper.getJsonObjectValue(DB_KEY_STARTDATA);
            if (start_data.has("api_data") && start_data.get("api_data").isJsonObject()) {
                KcaApiData.getKcGameData(start_data.getAsJsonObject("api_data"));
                valid_data = true;
            }
        }

        if (!valid_data) {
            try {
                AssetManager assetManager = context.getAssets();
                AssetManager.AssetInputStream ais =
                        (AssetManager.AssetInputStream) assetManager.open("api_start2");
                byte[] bytes = KcaUtils.gzipdecompress(ByteStreams.toByteArray(ais));
                helper.putValue(DB_KEY_STARTDATA, new String(bytes));
                JsonElement data = new JsonParser().parse(new String(bytes));
                JsonObject api_data = new Gson().fromJson(data, JsonObject.class).getAsJsonObject("api_data");
                KcaApiData.getKcGameData(api_data);
                setPreferences(context, PREF_KCA_VERSION, default_version);
                setPreferences(context, PREF_KCA_DATA_VERSION, default_version);
            } catch (Exception e) {
                return 0;
            }
            return 1;
        } else {
            return 1;
        }
    }

    public static Uri getContentUri(@NonNull Context context, @NonNull Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (uri.toString().startsWith("file")) {
                File file = new File(uri.getPath());
                return MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
            } else {
                return uri;
            }
        }
        return uri;
    }

    public static void playNotificationSound(MediaPlayer mediaPlayer, Context context, Uri uri) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            if (uri != null && !Uri.EMPTY.equals(uri)) {
                mediaPlayer.setDataSource(context, uri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attr = new AudioAttributes.Builder()
                            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                            .build();
                    mediaPlayer.setAudioAttributes(attr);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                }
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public static int getNotificationId(int type, int n) {
        return n + 1000 * type;
    }

    public static String getName(Context context, int resid) {
        return context.getResources().getResourceEntryName(resid);
    }

    public static int getId(String resourceName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resourceName);
            return idField.getInt(idField);
        } catch (Exception e) {
            throw new RuntimeException("No resource ID found for: "
                    + resourceName + " / " + c, e);
        }
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    // True: latest, False: need to update
    public static boolean compareVersion(String version_current, String version_default) {
        if (version_current != null && version_current.length() == 0) return false;
        if (version_current.equals(version_default)) return true;
        String[] current_split = version_current.replace("r", ".0.").split("\\.");
        String[] default_split = version_default.replace("r", ".0.").split("\\.");
        int min_length = Math.min(current_split.length, default_split.length);
        for (int i = 0; i < min_length; i++) {
            if (current_split[i].trim().length() > 0 && default_split[i].trim().length() > 0) {
                if (Integer.parseInt(current_split[i]) > Integer.parseInt(default_split[i])) {
                    return true;
                } else if (Integer.parseInt(current_split[i]) < Integer.parseInt(default_split[i])) {
                    return false;
                }
            }
        }
        return current_split.length > default_split.length;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static int getWindowLayoutType() {
        int windowLayoutType = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowLayoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return windowLayoutType;
    }

    public static NotificationCompat.Builder createBuilder(Context context, String channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(context, channel);
        } else {
            return new NotificationCompat.Builder(context);
        }
    }

    public static String getTimeStr(int left_time, boolean is_min) {
        int sec, min, hour;
        sec = left_time;
        min = sec / 60;
        hour = min / 60;
        sec = sec % 60;
        min = min % 60;
        if (is_min) return KcaUtils.format("%02d:%02d", hour * 60 + min, sec);
        else return KcaUtils.format("%02d:%02d:%02d", hour, min, sec);
    }

    public static String getTimeStr(int left_time) {
        return getTimeStr(left_time, false);
    }

    public static Calendar getJapanCalendarInstance() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        return c;
    }

    public static Calendar getJapanCalendarInstance(long timestamp) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTimeInMillis(timestamp);
        return c;
    }

    public static SimpleDateFormat getJapanSimpleDataFormat(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return dateFormat;
    }

    public static long getCurrentDateTimestamp (long current_time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        String timetext = dateFormat.format(new Date(current_time));
        long timestamp = 0;
        try {
            timestamp = dateFormat.parse(timetext).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    public static int getLastDay(int year, int month) {
        int[] day31 = {1, 3, 5, 7, 8, 10, 12};
        if (month == 2) {
            if (year % 100 != 0 && year % 4 == 0) return 29;
            else return 28;
        } else {
            return Arrays.binarySearch(day31, month) >= 0 ? 31 : 30;
        }
    }

    public static void doVibrate(Vibrator v, int time) {
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(time);
        }
    }

    public static void showCustomToast(Context a, Context b, KcaCustomToast toast, String body, int duration, int color) {
        if (getBooleanPreferences(a, PREF_DISABLE_CUSTOMTOAST)) {
            JsonObject data = new JsonObject();
            data.addProperty("text", body);
            data.addProperty("duration", duration);
            data.addProperty("color", color);
            Intent toastIntent = new Intent(b, KcaCustomToastService.class);
            toastIntent.setAction(KcaCustomToastService.TOAST_SHOW_ACTION);
            toastIntent.putExtra("data", data.toString());
            a.startService(toastIntent);
            //Toast.makeText(ctx, body, duration).show();
        } else {
            toast.showToast(body, duration, color);
        }
    }

    public static String getOrientationPrefix(int value) {
        if (value == Configuration.ORIENTATION_PORTRAIT) {
            return "ori_v_";
        } else {
            return "ori_h_";
        }
    }

    public static boolean searchStringFromStart(String name, String query, boolean match_case) {
        if (name == null) return false;
        if (query.trim().length() == 0) return true;
        // katakana to hiragana
        name = KanaConverter.convertKana(name, OP_ZEN_KATA_TO_ZEN_HIRA);
        query = KanaConverter.convertKana(query, OP_ZEN_KATA_TO_ZEN_HIRA);
        if (match_case) {
            return name.trim().startsWith(query.trim());
        } else {
            return name.trim().toLowerCase().startsWith(query.trim().toLowerCase());
        }
    }

    public static boolean searchStringContains(String name, String query, boolean match_case) {
        if (name == null) return false;
        if (query.trim().length() == 0) return true;
        // katakana to hiragana
        name = KanaConverter.convertKana(name, OP_ZEN_KATA_TO_ZEN_HIRA);
        query = KanaConverter.convertKana(query, OP_ZEN_KATA_TO_ZEN_HIRA);
        if (match_case) {
            return name.trim().contains(query.trim());
        } else {
            return name.trim().toLowerCase().contains(query.trim().toLowerCase());
        }
    }

    public static KcaDownloader getInfoDownloader(Context context){
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.MINUTES);
        builder.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("User-Agent", "Kcanotify/".concat(BuildConfig.VERSION_NAME).replace("r", "."))
                    .method(original.method(), original.body()).build();
            return chain.proceed(request);
        });

        OkHttpClient okHttpClient = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KcaUtils.getUpdateServer(context))
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        return retrofit.create(KcaDownloader.class);
    }

    public static KcaDownloader getResDownloader(Context context){
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.MINUTES);
        builder.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("User-Agent", "Kcanotify/".concat(BuildConfig.VERSION_NAME).replace("r", "."))
                    .method(original.method(), original.body()).build();
            return chain.proceed(request);
        });

        OkHttpClient okHttpClient = builder.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com/antest1/kcanotify-gamedata/master/files/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(KcaDownloader.class);
    }

    public static boolean checkFairyImageInStorage(Context context, String name) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("fairy", Context.MODE_PRIVATE);
        File image = new File(directory, name);
        return image.exists();
    }

    public static JsonObject getJsonObjectFromStorage(Context context, String name, KcaDBHelper helper) {
        if (getBooleanPreferences(context, PREF_RES_USELOCAL)) {
            return getJsonObjectFromAsset(context, name, helper);
        } else {
            ContextWrapper cw = new ContextWrapper(context);
            File directory = cw.getDir("data", Context.MODE_PRIVATE);
            File jsonFile = new File(directory, name);
            JsonObject data = null;
            try {
                Reader reader = new FileReader(jsonFile);
                data = new JsonParser().parse(reader).getAsJsonObject();
                reader.close();
            } catch (IOException | IllegalStateException | JsonSyntaxException e) {
                e.printStackTrace();
                setPreferences(context, PREF_DATALOAD_ERROR_FLAG, true);
                if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, name, "getJsonObjectFromStorage", "0", getStringFromException(e));
                data = getJsonObjectFromAsset(context, name, helper);
            }
            return data;
        }
    }

    public static JsonObject getJsonObjectFromAsset(Context context, String name, KcaDBHelper helper) {
        ContextWrapper cw = new ContextWrapper(context);
        JsonObject data = null;
        AssetManager am = cw.getAssets();
        try {
            AssetManager.AssetInputStream ais =
                    (AssetManager.AssetInputStream) am.open(name);
            byte[] bytes = ByteStreams.toByteArray(ais);
            data = new JsonParser().parse(new String(bytes)).getAsJsonObject();
            ais.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, name, "getJsonObjectFromStorage", "1", getStringFromException(e1));
        }
        return data;
    }

    public static JsonArray getJsonArrayFromStorage(Context context, String name, KcaDBHelper helper) {

        if (getBooleanPreferences(context, PREF_RES_USELOCAL)) {
            return getJsonArrayFromAsset(context, name, helper);
        } else {
            ContextWrapper cw = new ContextWrapper(context);
            File directory = cw.getDir("data", Context.MODE_PRIVATE);
            File jsonFile = new File(directory, name);
            JsonArray data = new JsonArray();
            try {
                Reader reader = new FileReader(jsonFile);
                data = new JsonParser().parse(reader).getAsJsonArray();
                reader.close();
            } catch (IOException | IllegalStateException | JsonSyntaxException e ) {
                e.printStackTrace();
                setPreferences(context, PREF_DATALOAD_ERROR_FLAG, true);
                if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, name, "getJsonArrayFromStorage", "0", getStringFromException(e));
                data = getJsonArrayFromAsset(context, name, helper);
            }
            return data;
        }
    }

    public static JsonArray getJsonArrayFromAsset(Context context, String name, KcaDBHelper helper) {
        ContextWrapper cw = new ContextWrapper(context);
        JsonArray data = new JsonArray();
        AssetManager am = cw.getAssets();
        try {
            AssetManager.AssetInputStream ais =
                    (AssetManager.AssetInputStream) am.open(name);
            byte[] bytes = ByteStreams.toByteArray(ais);
            data = new JsonParser().parse(new String(bytes)).getAsJsonArray();
            ais.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, name, "getJsonArrayFromStorage", "1", getStringFromException(e1));
        }
        return data;
    }

    public static boolean checkFairyImageFileFromStorage(Context context, String name) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("fairy", Context.MODE_PRIVATE);
        File myImageFile = new File(directory, KcaUtils.format("%s", name));
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            InputStream is = new FileInputStream(myImageFile);
            bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();
        } catch (IOException e) {
            // Log.e("KCA", getStringFromException(e));
            return false;
        }
        if (bitmap == null) {
            return false;
        }
        return true;
    }

    public static Bitmap getFairyImageFromStorage(Context context, String name, KcaDBHelper helper) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("fairy", Context.MODE_PRIVATE);
        File myImageFile = new File(directory, KcaUtils.format("%s.png", name));

        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        int fairy_id = Integer.parseInt(name.replace("noti_icon_", ""));
        if (fairy_id == 0) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.noti_icon_0);
        } else if (FAIRY_SPECIAL_FLAG && fairy_id >= FAIRY_SPECIAL_PREFIX) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), getId(name, R.mipmap.class));
        } else {
            try {
                bitmap = BitmapFactory.decodeStream(new FileInputStream(myImageFile), null, options);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setPreferences(context, PREF_DATALOAD_ERROR_FLAG, true);
                if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, name, "getFairyImageFromStorage", "0", getStringFromException(e));
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.noti_icon_0);
            }
            if (bitmap == null) {
                setPreferences(context, PREF_DATALOAD_ERROR_FLAG, true);
                if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, name, "getFairyImageFromStorage", "0", "bitmap==null");
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.noti_icon_0);
            }
        }
        return bitmap;
    }

    public static void setFairyImageFromStorage(Context context, String name, ImageView view, int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int fairy_id = Integer.parseInt(name.replace("noti_icon_", ""));
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("fairy", Context.MODE_PRIVATE);
        File myImageFile = new File(directory, KcaUtils.format("%s.png", name));
        if (myImageFile.exists()) {
            if (px > 0) {
                GlideApp.with(context).load(myImageFile.getPath())
                        .dontAnimate().override(px, px).into(view);
            } else {
                GlideApp.with(context).load(myImageFile.getPath()).into(view);
            }
        } else if (FAIRY_SPECIAL_FLAG && fairy_id >= FAIRY_SPECIAL_PREFIX) {
            view.setImageResource(getId(name, R.mipmap.class));
        } else {
            view.setImageResource(R.mipmap.noti_icon_0);
        }
    }

    public static void showDataLoadErrorToast(Context ac, Context bc, String text) {
        if (getBooleanPreferences(ac, PREF_DATALOAD_ERROR_FLAG)) {
            KcaCustomToast customToast = new KcaCustomToast(ac);
            showCustomToast(ac, bc, customToast, text, Toast.LENGTH_LONG, ContextCompat.getColor(ac, R.color.colorHeavyDmgStatePanel));
        }
    }

    public static void showDataLoadErrorToast(Context context, String text) {
        if (getBooleanPreferences(context, PREF_DATALOAD_ERROR_FLAG)) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean checkOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    public static int getGravity(int status) {
        int value;
        switch (status) {
            case 1:
                value = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                break;
            case 0:
                value = Gravity.CENTER;
                break;
            case -1:
                value = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                break;
            default:
                value = Gravity.CENTER;
                break;
        }
        return value;
    }

    // Image Downscale Functions from Android Reference
    // https://developer.android.com/topic/performance/graphics/load-bitmap.html

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static boolean validateResourceFiles(Context context, KcaDBHelper helper) {
        int count = 0;
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("data", Context.MODE_PRIVATE);
        for (final File entry : directory.listFiles()) {
            try {
                Reader reader = new FileReader(entry);
                new JsonParser().parse(reader);
                count += 1;
            } catch (FileNotFoundException | IllegalStateException | JsonSyntaxException e ) {
                e.printStackTrace();
                if (helper != null) helper.recordErrorLog(ERROR_TYPE_DATALOAD, entry.getName(), "validateResourceFiles", "2", getStringFromException(e));
                setPreferences(context, PREF_DATALOAD_ERROR_FLAG, true);
                return false;
            }
        }
        return count > 0;
    }

    public static JsonObject getCurrentWeekData() {
        JsonObject week_data = new JsonObject();
        Calendar cal = getJapanCalendarInstance();
        boolean is_passed = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) && (cal.get(Calendar.HOUR_OF_DAY) < 5);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_WEEK, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 5);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start_timestamp = cal.getTimeInMillis();
        if (is_passed) start_timestamp -= 604800000;
        long end_timestamp = start_timestamp + 604800000;
        week_data.addProperty("start", start_timestamp);
        week_data.addProperty("end", end_timestamp);
        return week_data;
    }

    public static int convertDpToPixel (float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    // Fix width for specific device has notch design:
    // Reference: https://stackoverflow.com/questions/53579164/check-if-device-has-notch-in-service
    public static void resizeFullWidthView(Context context, View v) {
        if (v == null) return;
        int statusBarHeight = 0;
        int defaultHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            Log.e("KCA", "" + statusBarHeight + " " + convertDpToPixel(defaultHeight));
            if (statusBarHeight > convertDpToPixel(defaultHeight)) {
                int orientation = context.getResources().getConfiguration().orientation;
                if (orientation == ORIENTATION_LANDSCAPE) {
                    final float scale = context.getResources().getDisplayMetrics().density;
                    int padding_px_width = (int) (28 * scale + 0.5f);
                    v.setPadding(padding_px_width, 0, padding_px_width, 0);
                }
            }
        }
    }

    public static KcaQSyncAPI getQuestSync(Context context){
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.MINUTES)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.app_kcaqsync_link))
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        return retrofit.create(KcaQSyncAPI.class);
    }

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static Map<String, String> getKcaQSyncHeaderMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Referer", "app:/KCA/");
        map.put("Content-Type", "application/x-www-form-urlencoded");
        map.put("User-Agent", KcaUtils.format("Kcanotify/%s ", BuildConfig.VERSION_NAME));
        return map;
    }

    public static String getRSAEncodedString(Context context, String value) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
        /*
        Example:
        try {
            JsonObject data = new JsonObject();
            data.addProperty("userid", 20181234);
            data.addProperty("data", "416D341GX141JI0318W");
            String encoded = KcaUtils.getRSAEncodedString(getApplicationContext(), data.toString());
            Log.e("KCA", encoded);
            data.remove("data");
            encoded = KcaUtils.getRSAEncodedString(getApplicationContext(), data.toString());
            Log.e("KCA", data.toString());
            Log.e("KCA", encoded);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        List<String> value_list = new ArrayList<>();
        for (int i = 0; i < (int) Math.ceil(value.length() / 96.0); i++) {
            value_list.add(value.substring(i*96, Math.min((i+1)*96, value.length()) ));
        }

        AssetManager am = context.getAssets();
        AssetManager.AssetInputStream ais =
                (AssetManager.AssetInputStream) am.open("kcaqsync_pubkey.txt");
        byte[] bytes = ByteStreams.toByteArray(ais);
        String publicKeyContent = new String(bytes)
                .replaceAll("\\n", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").trim();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(Base64.decode(publicKeyContent, Base64.DEFAULT));
        Key encryptionKey = keyFactory.generatePublic(pubSpec);
        Cipher rsa = Cipher.getInstance("RSA/None/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, encryptionKey);

        byte[] data_all = {};
        for (String item : value_list) {
            byte[] item_byte = rsa.doFinal(item.getBytes("utf-8"));
            data_all = addAll(data_all, item_byte);
        }

        String result = Base64.encodeToString(rsa.doFinal(value.getBytes("utf-8")), Base64.DEFAULT).replace("\n", "");
        return result;
    }


    // Customized base64 encoding: http://kancolle-calc.net/data/share.js
    static String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-";
    static String[] CODE = { "0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111",
            "1000", "1001", "1010", "1011", "1100", "1101" };


    public static String encode64(String dataString) {
        StringBuilder buff = new StringBuilder();
        StringBuilder outputString = new StringBuilder();
        for (int j = 0; j < dataString.length(); j++) {
            char c = dataString.charAt(j);
            int pos;
            if (c == ',') pos = 10;
            else if (c == '|') pos = 11;
            else if (c == '.') pos = 12;
            else if (c == ':') pos = 13;
            else pos = Integer.parseInt(String.valueOf(c));

            buff.append(CODE[pos]);
            if (buff.length() >= 6) {
                String seg = buff.substring(0, 6);
                outputString.append(BASE64.charAt(Integer.parseInt(seg, 2)));
                buff = new StringBuilder(buff.substring(6));
            }
        }
        if (buff.length() > 0) {
            while (buff.length() < 6) {
                buff.append('1');
            }
            outputString.append(BASE64.charAt(Integer.parseInt(buff.toString(), 2)));
        }
        return outputString.toString();
    }

    public static String decode64(String inputString) {
        List<String> codeList = new ArrayList<String>(Arrays.asList(CODE));
        StringBuilder dataString = new StringBuilder();
        StringBuilder buff = new StringBuilder();
        for (int j = 0; j < inputString.length(); j++) {
            StringBuilder inp = new StringBuilder(Integer.toBinaryString(BASE64.indexOf(inputString.charAt(j))));
            while (inp.length() < 6) {
                inp.insert(0, '0');
            }
            buff.append(inp);
            while (buff.length() >= 4) {
                String seg = buff.substring(0, 4);
                int pos = codeList.indexOf(seg);
                if (pos == -1); // Padding, do nothing
                else if (pos == 10) {
                    dataString.append(',');
                } else if (pos == 11) {
                    dataString.append('|');
                } else if (pos == 12) {
                    dataString.append('.');
                } else if (pos == 13) {
                    dataString.append(':');
                } else {
                    dataString.append(pos);
                }
                buff = new StringBuilder(buff.substring(4));
            }
        }
        return dataString.toString();
    }
}
