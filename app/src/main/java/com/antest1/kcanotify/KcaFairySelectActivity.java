package com.antest1.kcanotify;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_FAIRY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_DOWN_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_REV;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaFairySelectActivity extends AppCompatActivity {
    public final static String FAIRY_INFO_FILENAME = "icon_info.json";
    public final static boolean FAIRY_SPECIAL_FLAG = false;
    public final static int FAIRY_SPECIAL_PREFIX = 900;
    public final static int FAIRY_SPECIAL_COUNT = 8;

    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();
    KcaDBHelper dbHelper;
    GridView gv;
    ProgressDialog mProgressDialog;
    Fetch fetch;
    JsonArray download_data = new JsonArray();
    PowerManager pm;
    PowerManager.WakeLock mWakeLock;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public KcaFairySelectActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_fairy);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.setting_menu_kand_title_fairy_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, KcaFairySelectActivity.class.getName());

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(getApplicationContext())
                .setDownloadConcurrentLimit(24)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        mProgressDialog = new ProgressDialog(KcaFairySelectActivity.this);
        mProgressDialog.setMessage(getStringWithLocale(R.string.download_progress));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressNumberFormat("%1d file(s)");

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        JsonArray icon_info = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "icon_info.json", dbHelper);
        Log.e("KCA-FS", icon_info.toString());
        List<String> fairy_id = new ArrayList<>();

        boolean fairy_downloaded = getBooleanPreferences(getApplicationContext(), PREF_FAIRY_DOWN_FLAG);
        int fairy_size = fairy_downloaded ? icon_info.size() : 1;

        if (FAIRY_SPECIAL_FLAG) {
            for (int i = 0; i < FAIRY_SPECIAL_COUNT; i++) {
                fairy_id.add("noti_icon_".concat(String.valueOf(i + FAIRY_SPECIAL_PREFIX)));
            }
        }

        for (int i = 0; i < fairy_size; i++) {
            fairy_id.add("noti_icon_".concat(String.valueOf(i)));
        }

        final KcaItemAdapter adapter = new KcaItemAdapter(getApplicationContext(),
                R.layout.listview_image_item, fairy_id);

        String pref_value = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        if (pref_value.length() > 0) {
            adapter.setPrevActive(Integer.parseInt(pref_value));
        }

        gv = (GridView)findViewById(R.id.fairy_gridview);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int value = Integer.parseInt(fairy_id.get(position).replace("noti_icon_", ""));
                setPreferences(getApplicationContext(), PREF_FAIRY_REV, 0);
                setPreferences(getApplicationContext(), PREF_FAIRY_ICON, String.valueOf(value));
                if (KcaService.getServiceStatus()) {
                    JsonObject data = new JsonObject();
                    data.addProperty("id", value);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
                    bundle.putString("data", data.toString());
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
                adapter.setPrevActive(position);
                gv.invalidateViews();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fairyselect, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_fairy_down:
                new KcaResourceDownloader().execute();
                return true;
            case R.id.action_fairy_rev:
                int current_rev = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_REV));
                setPreferences(getApplicationContext(), PREF_FAIRY_REV, 1 - current_rev);
                if (KcaService.getServiceStatus()) {
                    int current_id = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
                    JsonObject data = new JsonObject();
                    data.addProperty("id", current_id);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
                    bundle.putString("data", data.toString());
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class KcaResourceDownloader extends AsyncTask<Integer, Integer, Integer> {
        boolean fairy_wait = false;
        int update_version = 0;
        int totalFiles = 0;
        int successedFiles = 0;
        int failedFiles = 0;
        int download_result = 0;

        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWakeLock.acquire(10*60*1000L /*10 minutes*/);
            mProgressDialog.show();
        }

        private void workFinished()  {
            if (mWakeLock.isHeld()) mWakeLock.release();
            Log.e("KCA-FS", KcaUtils.format("%d %d %d", totalFiles, successedFiles, failedFiles));
            setPreferences(getApplicationContext(), PREF_FAIRY_DOWN_FLAG, true);
            if (!KcaFairySelectActivity.this.isFinishing() && mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (totalFiles == 0) {
                Toast.makeText(getApplicationContext(), "no file to download", Toast.LENGTH_LONG).show();
            } else {
                finish();
            }
        }

        private void downloadFile(String folder, String url, String name, int version) {
            final File root_dir = cw.getDir(folder, Context.MODE_PRIVATE);
            final File data = new File(root_dir, name);
            if (data.exists()) data.delete();

            final Request request = new Request(url, data.getPath());
            fetch.enqueue(request, updatedRequest -> {
                dbHelper.putResVer(name, version);
            }, error -> {
                failedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            });
        }


        FetchListener fetchDownloadListener = new FetchListener() {
            @Override
            public void onDeleted(Download download) {}

            @Override
            public void onRemoved(Download download) {}

            @Override
            public void onResumed(Download download) {}

            @Override
            public void onPaused(Download download) {}

            @Override
            public void onProgress(Download download, long l, long l1) {}

            @Override
            public void onQueued(Download download, boolean b) {}

            @Override
            public void onCancelled(Download download) {}

            @Override
            public void onError(Download download) {
                failedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            }

            @Override
            public void onCompleted(@NotNull Download download) {
                successedFiles += 1;
                if (totalFiles > 0) publishProgress((successedFiles + failedFiles));
            }
        };

        private void startDownloadProgress() {
            fairy_wait = true;
            publishProgress(0);
            fetch.addListener(fetchDownloadListener);
            totalFiles = download_data.size();
            mProgressDialog.setMax(totalFiles);
            for (int i = 0; i < download_data.size(); i++) {
                JsonObject item = download_data.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                String url = item.get("url").getAsString();
                downloadFile("fairy", url, name, 0);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(!fairy_wait);
            mProgressDialog.setMax(totalFiles);
            mProgressDialog.setProgress(progress[0]);

            if (totalFiles == 0 || progress[0] == totalFiles) {
                workFinished();
            }
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            JsonArray fairy_data = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), FAIRY_INFO_FILENAME, dbHelper);
            for (int i = 0; i < fairy_data.size(); i++) {
                JsonObject fairy_item = fairy_data.get(i).getAsJsonObject();
                if (!KcaUtils.checkFairyImageFileFromStorage(getApplicationContext(), fairy_item.get("name").getAsString())) {
                    download_data.add(fairy_item);
                }
            }
            startDownloadProgress();

            //downloadFairyIcon();
            return download_result;
        }

        @Override
        protected void onPostExecute(Integer s) {
            super.onPostExecute(s);

        }
    }
}