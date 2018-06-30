package com.antest1.kcanotify;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.Request;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_MAIN;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;

public class InitStartActivity extends Activity {
    public final static int delay_time = 1000;

    boolean download_finished = false;
    Handler handler = new Handler();
    Runnable r;

    KcaDBHelper dbHelper;
    ProgressDialog mProgressDialog;
    PowerManager pm;
    PowerManager.WakeLock mWakeLock;
    TextView appname, version;

    int requiredFiles = 0;
    JsonArray download_data = new JsonArray();
    Fetch fetch;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_download);
        Log.e("KCA-DA", "created");
        // instantiate it within the onCreate method
        mProgressDialog = new ProgressDialog(InitStartActivity.this);
        mProgressDialog.setMessage("Downloading..");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, InitStartActivity.class.getName());

        appname = findViewById(R.id.app_title);
        appname.setText(getStringWithLocale(R.string.app_name));

        version = findViewById(R.id.app_version);
        version.setText(getString(R.string.app_version));

        loadTranslationData(getApplicationContext());

        KcaResourceInfoDownloader resource_loader = KcaUtils.getResourceDownloader(getApplicationContext());
        resource_loader.getIconInfo();
        final Call<String> icon_info = resource_loader.getIconInfo();
        icon_info.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                try {
                    if (response.body() != null) {
                        JsonArray fairy_index = new JsonParser().parse(response.body()).getAsJsonArray();
                        Log.e("KCA", fairy_index.toString());
                        Log.e("KCA", " " + fairy_index.size());

                        for (int i = 0; i < fairy_index.size(); i++) {
                            JsonObject item = fairy_index.get(i).getAsJsonObject();
                            String name = item.get("name").getAsString();
                            if (!KcaUtils.checkFairyImageInStorage(getApplicationContext(), name)) {
                                download_data.add(item);
                            }
                        }

                        requiredFiles = download_data.size();
                        if (requiredFiles == 0) {
                            startMainActivity();
                        } else {
                            final KcaResourceDownloader downloadTask = new KcaResourceDownloader();
                            downloadTask.execute();
                            Log.e("KCA-DA", "execute");

                            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    downloadTask.cancel(true);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "fairy_info", "", "", getStringFromException(e));
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            KcaUtils.format(getStringWithLocale(R.string.sa_getupdate_servererror), t.getMessage()),
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "fairy_info", "", "", t.getMessage());
                }
            }
        });
    }

    private void startMainActivity() {
        r = new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        };
        handler.postDelayed(r, delay_time);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (download_finished && r != null) handler.postDelayed(r, delay_time);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (download_finished && r != null) handler.removeCallbacks(r);
    }

    @Override
    protected void onDestroy() {
        Log.e("KCA-DA", "destroy");
        super.onDestroy();
    }



    class KcaResourceDownloader extends AsyncTask<String, Integer, String> {
        int totalFiles = 0;
        int successedFiles = 0;
        int failedFiles = 0;
        JsonArray fairy_index = new JsonArray();
        private List<Target> save_list = new ArrayList<>();
        Fetch fetch;

        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        public KcaResourceDownloader() {
            FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(getApplicationContext())
                    .setDownloadConcurrentLimit(10)
                    .build();
            fetch = Fetch.Impl.getInstance(fetchConfiguration);
        }

        @Override
        protected void onPreExecute() {
            save_list = new ArrayList<>();
            super.onPreExecute();
            download_finished = false;
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        private void workFinished() {
            mWakeLock.release();
            download_finished = true;
            mProgressDialog.dismiss();
            startMainActivity();
        }

        private void downloadFairy(String url, String name) {
            final File root_dir = cw.getDir("fairy", Context.MODE_PRIVATE);
            final File myImageFile = new File(root_dir, name);

            final Request request = new Request(url, myImageFile.getPath());
            fetch.enqueue(request, updatedRequest -> {
                successedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            }, error -> {
                failedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            });
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(totalFiles);
            mProgressDialog.setProgress(progress[0]);

            if (progress[0] == totalFiles) {
                workFinished();
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            totalFiles = download_data.size();
            downloadFairyIcon();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //mProgressDialog.dismiss();
            // save_list.clear();
            // finish();
        }

        private void downloadFairyIcon() {
            for (int i = 0; i < download_data.size(); i++) {
                JsonObject item = download_data.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                String url = item.get("url").getAsString();
                downloadFairy(url, name);
            }
        }
    }
}

