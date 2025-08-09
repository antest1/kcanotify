package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_DOWN_FLAG;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KcaFairyDownloader {
    private final static String FAIRY_INFO_FILENAME = "icon_info.json";
    private final static String FAIRY_DOWNLOAD_PATH = "fairy";

    boolean fairyWait;
    boolean finishWhenDoneFlag;
    int totalFiles;
    int successedFiles;
    int failedFiles;

    BaseActivity activity;
    Handler handler;
    KcaDBHelper dbHelper;
    PowerManager pm;
    PowerManager.WakeLock mWakeLock;
    ProgressDialog mProgressDialog;
    JsonArray downloadData;

    public KcaFairyDownloader(BaseActivity act) {
        activity = act;
        pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        dbHelper = new KcaDBHelper(activity, null, KCANOTIFY_DB_VERSION);
        PRDownloader.initialize(activity);
    }

    public void run(boolean flag) {
        if (mWakeLock != null) return;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, activity.getClass().getName());
        mWakeLock.acquire(10*60*1000L /*10 minutes*/);

        finishWhenDoneFlag = flag;
        fairyWait = false;
        totalFiles = 0;
        successedFiles = 0;
        failedFiles = 0;
        handler = new Handler(Looper.getMainLooper());
        downloadData = new JsonArray();

        mProgressDialog = new ProgressDialog();
        mProgressDialog.setMessage(activity.getString(R.string.download_progress));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressNumberFormat("%1d file(s)");
        mProgressDialog.show(activity);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            JsonArray fairy_data = KcaUtils.getJsonArrayFromStorage(activity, FAIRY_INFO_FILENAME, dbHelper);
            for (int i = 0; i < fairy_data.size(); i++) {
                JsonObject fairy_item = fairy_data.get(i).getAsJsonObject();
                if (!KcaUtils.checkFairyImageFileFromStorage(activity, fairy_item.get("name").getAsString())) {
                    downloadData.add(fairy_item);
                }
            }
            startDownloadProgress();
        });
    }

    private void startDownloadProgress() {
        fairyWait = true;
        publishProgress(0);
        totalFiles = downloadData.size();
        if (handler != null) {
            handler.post(() -> mProgressDialog.setMax(totalFiles));
            for (int i = 0; i < downloadData.size(); i++) {
                JsonObject item = downloadData.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                String url = item.get("url").getAsString();
                downloadFile(url, name);
            }
        }
    }

    private void workFinished()  {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        handler = null;
        Log.e("KCA-FS", KcaUtils.format("%d %d %d", totalFiles, successedFiles, failedFiles));
        setPreferences(activity, PREF_FAIRY_DOWN_FLAG, true);
        if (!activity.isFinishing() && mProgressDialog != null) {
            mProgressDialog.dismissAllowingStateLoss();
        }
        if (totalFiles == 0) {
            Toast.makeText(activity, "no file to download", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity, KcaUtils.format("Download Completed: %d", successedFiles), Toast.LENGTH_LONG).show();
            if (finishWhenDoneFlag) activity.finish();
        }
    }

    private void downloadFile(String url, String name) {
        ContextWrapper cw = new ContextWrapper(activity.getApplicationContext());
        final File root_dir = cw.getDir(FAIRY_DOWNLOAD_PATH, Context.MODE_PRIVATE);
        final File data = new File(root_dir, name);
        if (data.exists()) data.delete();

        PRDownloader.download(url, root_dir.getPath(), name)
                .build()
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        successedFiles += 1;
                        if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                        dbHelper.putResVer(name, 0);
                    }

                    @Override
                    public void onError(Error error) {
                        failedFiles += 1;
                        if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
                    }
                });
    }

    private void publishProgress(Integer progress) {
        if (handler != null) {
            handler.post(() -> {
                mProgressDialog.setIndeterminate(!fairyWait);
                mProgressDialog.setMax(totalFiles);
                mProgressDialog.setProgress(progress);

                if (totalFiles == 0 || progress == totalFiles) {
                    workFinished();
                }
            });
        }
    }
}
