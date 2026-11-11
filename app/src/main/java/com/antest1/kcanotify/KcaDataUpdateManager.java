package com.antest1.kcanotify;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_DATA_UPDATE_CHECKED;
import static com.antest1.kcanotify.KcaConstants.PREF_DATA_UPDATE_PENDING;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

// Downloads game data and resource files one at a time, independent of any screen, so a download
// finishes (and its version is recorded) even if the update screen is closed.
// Each file goes to a temporary file first and replaces the current one only after it is checked.
public class KcaDataUpdateManager {
    private static final String TAG = "KcaDataUpdate";
    public static final String GAME_DATA_KEY = "api_start2";
    private static final String TEMP_SUFFIX = ".download";
    private static final long[] RETRY_DELAYS_MS = {1000, 3000};

    public enum State { QUEUED, DOWNLOADING, DONE, FAILED, NOT_READY }

    public interface Listener {
        // called on the main thread; progress is 0-100, or -1 if unknown
        void onItemChanged(String key, State state, int progress);

        // called on the main thread when the queue becomes empty
        void onFinished(Summary summary);
    }

    public static class Summary {
        public final int done;
        public final int failed;
        // files the server or CDN does not have yet (game data version not uploaded, or an older
        // CDN copy of a resource); retrying a few minutes later fixes them
        public final int notReady;

        Summary(int done, int failed, int notReady) {
            this.done = done;
            this.failed = failed;
            this.notReady = notReady;
        }
    }

    private static KcaDataUpdateManager instance;

    public static synchronized KcaDataUpdateManager getInstance(Context context) {
        if (instance == null) instance = new KcaDataUpdateManager(context.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final OkHttpClient client;
    private final KcaDownloader downloader;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> pending = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<String> batch = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean cancelled = false;
    private volatile Call currentCall;
    private volatile retrofit2.Call<String> currentGameDataCall;
    // counters of the current batch, used on the worker thread only
    private int doneCount, failedCount, notReadyCount;
    private boolean translationInstalled;

    private KcaDataUpdateManager(Context context) {
        this.context = context;
        this.client = KcaUtils.buildUpdateHttpClient();
        this.downloader = KcaUtils.getInfoDownloader(context);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public State getState(String key) {
        return states.get(key);
    }

    public boolean isRunning() {
        return !pending.isEmpty();
    }

    public void enqueueGameData(String version) {
        enqueue(GAME_DATA_KEY, () -> downloadGameData(version));
    }

    // listLastModified: Last-Modified of the list.json response that announced this version
    public void enqueueResource(String name, String url, int version, String listLastModified) {
        enqueue(name, () -> downloadResource(name, url, version, listLastModified));
    }

    // stops the current download and drops the queued ones; finished files are kept
    public void cancelAll() {
        cancelled = true;
        Call call = currentCall;
        if (call != null) call.cancel();
        retrofit2.Call<String> gameDataCall = currentGameDataCall;
        if (gameDataCall != null) gameDataCall.cancel();
    }

    // keys of the downloads requested since the queue was last empty, for the progress of the batch
    public List<String> getBatchKeys() {
        synchronized (batch) {
            return new ArrayList<>(batch);
        }
    }

    // forgets the results of finished downloads (shown as "done" or "failed") before a new check
    public void clearFinishedStates() {
        if (isRunning()) return;
        states.clear();
        batch.clear();
    }

    private void enqueue(String key, Runnable task) {
        synchronized (pending) {
            if (pending.contains(key)) return; // already queued or downloading
            if (pending.isEmpty()) batch.clear();
            pending.add(key);
        }
        batch.add(key);
        cancelled = false;
        setState(key, State.QUEUED, 0);
        worker.execute(() -> {
            if (cancelled) {
                states.remove(key);
                notifyItem(key, null, 0);
            } else {
                task.run();
            }
            pending.remove(key);
            if (pending.isEmpty()) finishBatch();
        });
    }

    private void finishBatch() {
        Summary summary = new Summary(doneCount, failedCount, notReadyCount);
        doneCount = failedCount = notReadyCount = 0;
        boolean reloadTranslation = translationInstalled;
        translationInstalled = false;
        mainHandler.post(() -> {
            // translation files are loaded together, so once after the batch
            if (reloadTranslation) KcaApiData.loadTranslationData(context, true);
            if (listeners.isEmpty()) {
                // the update screen is closed: tell the result once
                String message = getSummaryText(context, summary);
                if (message != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            } else {
                for (Listener listener : listeners) listener.onFinished(summary);
            }
        });
    }

    // saves the result of an update check: when it was done and how many updates were found
    public static void saveCheckResult(Context context, int pendingCount) {
        setPreferences(context, PREF_DATA_UPDATE_CHECKED, String.valueOf(System.currentTimeMillis()));
        setPreferences(context, PREF_DATA_UPDATE_PENDING, Math.max(0, pendingCount));
    }

    public static int getPendingCount(Context context) {
        try {
            return Integer.parseInt(getStringPreferences(context, PREF_DATA_UPDATE_PENDING));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 0 if never checked
    public static long getLastCheckTime(Context context) {
        try {
            return Long.parseLong(getStringPreferences(context, PREF_DATA_UPDATE_CHECKED));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // an update counted by the last check is installed
    private void onUpdateInstalled() {
        int count = getPendingCount(context);
        if (count > 0) setPreferences(context, PREF_DATA_UPDATE_PENDING, count - 1);
    }

    public static String getSummaryText(Context context, Summary summary) {
        if (summary.done + summary.failed + summary.notReady == 0) return null;
        if (summary.failed == 0 && summary.notReady == 0) {
            return KcaUtils.format(context.getString(R.string.data_update_result_done), summary.done);
        }
        if (summary.failed == 0) return context.getString(R.string.data_update_result_not_ready);
        return KcaUtils.format(context.getString(R.string.data_update_result_failed),
                summary.done, summary.failed + summary.notReady);
    }

    private void setState(String key, State state, int progress) {
        states.put(key, state);
        notifyItem(key, state, progress);
    }

    private void notifyItem(String key, State state, int progress) {
        mainHandler.post(() -> {
            for (Listener listener : listeners) listener.onItemChanged(key, state, progress);
        });
    }

    private void downloadGameData(String version) {
        setState(GAME_DATA_KEY, State.DOWNLOADING, -1);
        for (int attempt = 0; ; attempt++) {
            try {
                retrofit2.Call<String> call = downloader.getGameData(version);
                currentGameDataCall = call;
                retrofit2.Response<String> response = call.execute();
                if (response.code() == 404) {
                    // this version is not on the server yet
                    notReadyCount += 1;
                    setState(GAME_DATA_KEY, State.NOT_READY, 0);
                    return;
                }
                String serverVersion = response.headers().get(KcaGameDataChecker.HEADER_VERSION);
                String startData = response.body();
                if (!response.isSuccessful() || startData == null
                        || serverVersion == null || !serverVersion.trim().equals(version)) {
                    throw new IOException("unexpected response: " + response.code() + " " + serverVersion);
                }
                JsonObject apiData = JsonParser.parseString(startData).getAsJsonObject().getAsJsonObject("api_data");
                if (apiData == null) throw new IOException("no api_data");

                // save the data and its version together, then apply it on the main thread,
                // where the game data in memory is used
                if (!KcaGameDataStore.save(context, startData)) throw new IOException("cannot save game data");
                setPreferences(context, PREF_KCA_DATA_VERSION, version);
                mainHandler.post(() -> {
                    KcaApiData.getKcGameData(apiData);
                    KcaApiData.setDataLoadTriggered();
                });
                doneCount += 1;
                onUpdateInstalled();
                setState(GAME_DATA_KEY, State.DONE, 100);
                return;
            } catch (Exception e) {
                Log.w(TAG, "game data " + version + ": " + e);
                if (cancelled || !waitBeforeRetry(attempt)) {
                    failedCount += 1;
                    setState(GAME_DATA_KEY, State.FAILED, 0);
                    recordError("gamedata_download", version, e);
                    return;
                }
            } finally {
                currentGameDataCall = null;
            }
        }
    }

    private void downloadResource(String name, String url, int version, String listLastModified) {
        File dir = context.getDir("data", Context.MODE_PRIVATE);
        File temp = new File(dir, name + TEMP_SUFFIX);
        for (int attempt = 0; ; attempt++) {
            try {
                setState(name, State.DOWNLOADING, 0);
                String fileLastModified = fetchToFile(
                        KcaUtils.format("%s?t=%d", url, System.currentTimeMillis() / 1000), temp, name);
                if (isOlderDeploy(fileLastModified, listLastModified)) {
                    // GitHub Pages ignores "?t=" and may still serve the previous deploy for up to
                    // 10 minutes: keep the current file and its version, and try again later
                    temp.delete();
                    notReadyCount += 1;
                    setState(name, State.NOT_READY, 0);
                    return;
                }
                if (!isValidJson(temp)) throw new IOException("invalid json");
                if (!temp.renameTo(new File(dir, name))) throw new IOException("rename failed");
                KcaDBHelper helper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
                helper.putResVer(name, version);
                helper.close();
                mainHandler.post(() -> KcaApiData.reloadResource(context, name));
                doneCount += 1;
                onUpdateInstalled();
                if (isTranslationFile(name)) translationInstalled = true;
                setState(name, State.DONE, 100);
                return;
            } catch (Exception e) {
                temp.delete();
                Log.w(TAG, name + ": " + e);
                if (cancelled || !waitBeforeRetry(attempt)) {
                    failedCount += 1;
                    setState(name, State.FAILED, 0);
                    recordError("resource_download", name, e);
                    return;
                }
            }
        }
    }

    // downloads url into file and returns the Last-Modified header of the response
    private String fetchToFile(String url, File file, String key) throws IOException {
        Request request = new Request.Builder().url(url).header("Cache-Control", "no-cache").build();
        Call call = client.newCall(request);
        currentCall = call;
        try (Response response = call.execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) throw new IOException("http " + response.code());
            long total = body.contentLength();
            int lastProgress = -1;
            try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[16 * 1024];
                long read = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    read += n;
                    if (total > 0) {
                        int progress = (int) (read * 100 / total);
                        if (progress >= lastProgress + 5) {
                            lastProgress = progress;
                            notifyItem(key, State.DOWNLOADING, progress);
                        }
                    }
                }
            }
            return response.header("Last-Modified");
        } finally {
            currentCall = null;
        }
    }

    // ship, item, quest... names of a language (e.g. ships-ko.json)
    public static boolean isTranslationFile(String name) {
        return name.matches("^(items|quests|ships|stype|useitems)-[a-z]+\\.json$");
    }

    // true if the file comes from an older deploy than the list (both are Last-Modified headers,
    // which GitHub Pages sets to the deploy time for every file)
    static boolean isOlderDeploy(String fileLastModified, String listLastModified) {
        Date file = parseHttpDate(fileLastModified);
        Date list = parseHttpDate(listLastModified);
        return file != null && list != null && file.before(list);
    }

    private static Date parseHttpDate(String value) {
        if (value == null) return null;
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private static boolean isValidJson(File file) {
        try (Reader reader = new FileReader(file)) {
            JsonParser.parseReader(reader);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // waits before the next attempt; false if there is no attempt left
    private boolean waitBeforeRetry(int attempt) {
        if (attempt >= RETRY_DELAYS_MS.length) return false;
        try {
            Thread.sleep(RETRY_DELAYS_MS[attempt]);
        } catch (InterruptedException e) {
            return false;
        }
        return !cancelled;
    }

    private void recordError(String type, String target, Exception e) {
        KcaDBHelper helper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(KcaConstants.ERROR_TYPE_SETTING, type, target, "", KcaUtils.getStringFromException(e));
        helper.close();
    }
}
