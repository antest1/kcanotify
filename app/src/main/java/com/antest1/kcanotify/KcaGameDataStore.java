package com.antest1.kcanotify;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

// Game data (the whole api_start2 response) in a file. It is about 2MB, more than a DB row can be read at
// once (the cursor window is 2MB on most devices), and it is always read and written as a whole.
// Its version stays in PREF_KCA_DATA_VERSION.
public class KcaGameDataStore {
    private static final String TAG = "KcaGameDataStore";
    private static final String DIR_NAME = "gamedata";
    public static final String FILE_NAME = "api_start2.json";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final Object writeLock = new Object();

    private static File getFile(Context context) {
        return new File(new File(context.getFilesDir(), DIR_NAME), FILE_NAME);
    }

    public static boolean exists(Context context) {
        return getFile(context).isFile();
    }

    // size in bytes, or -1 if there is no file
    public static long length(Context context) {
        File file = getFile(context);
        return file.isFile() ? file.length() : -1;
    }

    public static boolean save(Context context, String data) {
        return write(context, writer -> writer.write(data));
    }

    // writes the object directly, without making the whole JSON string first
    public static boolean save(Context context, JsonObject data) {
        return write(context, writer -> new Gson().toJson(data, writer));
    }

    // the whole response, or null if there is no file or it cannot be read
    public static JsonObject load(Context context) {
        File file = getFile(context);
        if (!file.isFile()) return null;
        try (JsonReader reader = new JsonReader(openReader(file))) {
            reader.setLenient(true);
            JsonElement element = JsonParser.parseReader(reader);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            Log.w(TAG, "load: " + e);
            return null;
        }
    }

    // api_data of the response, or null
    public static JsonObject loadApiData(Context context) {
        JsonObject data = load(context);
        if (data == null || !data.has("api_data") || !data.get("api_data").isJsonObject()) return null;
        return data.getAsJsonObject("api_data");
    }

    // the raw text, for the inspector
    public static String readText(Context context) {
        File file = getFile(context);
        if (!file.isFile()) return null;
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[16 * 1024];
        try (Reader reader = openReader(file)) {
            int n;
            while ((n = reader.read(buffer)) != -1) builder.append(buffer, 0, n);
            return builder.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static void delete(Context context) {
        synchronized (writeLock) {
            File file = getFile(context);
            file.delete();
            new File(file.getParentFile(), FILE_NAME + TEMP_SUFFIX).delete();
        }
    }

    private interface Content {
        void writeTo(Writer writer) throws IOException;
    }

    // writes to a temporary file and replaces the current one only when it is complete,
    // so a failed write keeps the previous data; readers always see a whole file
    private static boolean write(Context context, Content content) {
        synchronized (writeLock) {
            File file = getFile(context);
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return false;
            File temp = new File(dir, FILE_NAME + TEMP_SUFFIX);
            try (Writer writer = new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(temp)), StandardCharsets.UTF_8)) {
                content.writeTo(writer);
            } catch (IOException e) {
                Log.w(TAG, "save: " + e);
                temp.delete();
                return false;
            }
            if (!temp.renameTo(file)) {
                temp.delete();
                return false;
            }
            return true;
        }
    }

    private static Reader openReader(File file) throws IOException {
        return new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), StandardCharsets.UTF_8);
    }
}
