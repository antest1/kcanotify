package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaBackupItemAdpater.BACKUP_KEY;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KcaBackupActivity extends AppCompatActivity {
    private final String FILE_PATH = "/backup";
    static Gson gson = new Gson();

    Toolbar toolbar;
    List<String> db_paths = new ArrayList<>();
    boolean is_exporting = false;

    TextView exportButton;
    TextView exportMessage;
    TextView backup_load;
    ListView backup_list;

    KcaBackupItemAdpater adapter = new KcaBackupItemAdpater();
    UpdateHandler handler;

    public KcaBackupActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_appbackup));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView) findViewById(R.id.backup_export_label)).setText(getStringWithLocale(R.string.backup_export_label));
        ((TextView) findViewById(R.id.backup_export_desc)).setText(getStringWithLocale(R.string.backup_export_desc));
        ((TextView) findViewById(R.id.backup_export_button)).setText(getStringWithLocale(R.string.backup_export_button));
        ((TextView) findViewById(R.id.backup_list_label)).setText(getStringWithLocale(R.string.backup_list_label));

        backup_load = findViewById(R.id.backup_loading);
        backup_list = findViewById(R.id.backup_list);
        backup_list.setAdapter(adapter);

        exportButton = findViewById(R.id.backup_export_button);
        exportButton.setOnClickListener(v -> {
            new BackupSaveTask().execute();
        });

        handler = new UpdateHandler(this);
        adapter.setHandler(handler);

        exportMessage = findViewById(R.id.backup_result_message);
        exportMessage.setText("");

        db_paths.add(KcaDBHelper.getName());
        db_paths.add(KcaQuestTracker.getName());
        db_paths.add(KcaDropLogger.getName());
        db_paths.add(KcaResourceLogger.getName());

        for (String name: db_paths) {
            Log.e("KCA", getDatabasePath(name).getAbsolutePath());
        }

        adapter.setListItem(getBackupDataList());
        adapter.notifyDataSetChanged();

        backup_load.setVisibility(View.GONE);
        backup_list.setVisibility(View.VISIBLE);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class BackupSaveTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            is_exporting = true;
            exportButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
            exportButton.setClickable(false);
        }

        @Override
        protected String doInBackground(String[] params) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String exportDirPath = KcaUtils.format("/kca_backup_%s", dateFormat.format(new Date()));

            File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(FILE_PATH).concat(exportDirPath));
            if (!savedir.exists()) savedir.mkdirs();

            String exportPath = savedir.getPath();
            File exportFolder = new File(exportPath);

            try {
                for (String name: db_paths) {
                    File src = new File(getDatabasePath(name).getAbsolutePath());
                    File dst = new File(exportPath.concat("/").concat(name));
                    if (src.exists()) {
                        KcaUtils.copyFile(new FileInputStream(src), new FileOutputStream(dst));
                    }
                }

                ZipFile zipFile = new ZipFile(exportPath.concat(".zip"));
                zipFile.addFolder(exportFolder);

                for (File file: exportFolder.listFiles()) {
                    if (!file.isDirectory())
                        file.delete();
                }
                exportFolder.delete();

                return zipFile.getFile().getPath();

            } catch (IOException e) {
                e.printStackTrace();
                return KcaUtils.getStringFromException(e);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            is_exporting = false;
            exportButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            exportButton.setClickable(true);
            if ("Error".equals(result)) {
                exportMessage.setText(getStringWithLocale(R.string.backup_msg_export_error).concat(result));
            } else if (result != null) {
                exportMessage.setText(getStringWithLocale(R.string.backup_msg_export_done).concat(result));
                adapter.setListItem(getBackupDataList());
                adapter.notifyDataSetChanged();
            }
        }
    }

    private class BackupLoadTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String[] params) {
            String backup_fn = params[0];
            File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(FILE_PATH));

            try {
                File exportFolder = new File(savedir.getAbsolutePath().concat("/").concat(backup_fn).replace(".zip", ""));
                if (!exportFolder.exists()) exportFolder.mkdirs();
                new ZipFile(savedir.getAbsolutePath().concat("/").concat(backup_fn)).extractAll(savedir.getAbsolutePath());

                for (String name: db_paths) {
                    File src = new File(exportFolder.getAbsolutePath().concat("/").concat(name));
                    File dst = new File(getDatabasePath(name).getAbsolutePath());
                    if (src.exists()) {
                        KcaUtils.copyFile(new FileInputStream(src), new FileOutputStream(dst));
                    }
                }

                for (File file: exportFolder.listFiles()) {
                    if (!file.isDirectory())
                        file.delete();
                }
                exportFolder.delete();
                return backup_fn;

            } catch (IOException e) {
                e.printStackTrace();
                return KcaUtils.getStringFromException(e);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            is_exporting = false;
            exportButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            exportButton.setClickable(true);
            if ("Error".equals(result)) {
                exportMessage.setText(getStringWithLocale(R.string.backup_msg_import_error).concat(result));
            } else if (result != null) {
                exportMessage.setText(getStringWithLocale(R.string.backup_msg_import_done).concat(result));
            }
        }
    }

    private List<JsonObject> getBackupDataList() {
        List<JsonObject> list = new ArrayList<>();
        File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(FILE_PATH));
        if (savedir.isDirectory()) {
            File[] files = savedir.listFiles();
            for (File f: files) {
                if (!f.getName().endsWith(".zip")) continue;
                JsonObject item = new JsonObject();
                item.addProperty("name", f.getName());
                item.addProperty("size", f.length());
                list.add(item);
                Log.e("KCA", item.toString());
            }
        }
        return list;
    }

    private static class UpdateHandler extends Handler {
        private final WeakReference<KcaBackupActivity> mActivity;

        UpdateHandler(KcaBackupActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            KcaBackupActivity activity = mActivity.get();
            if (activity != null) {
                Bundle bundle = msg.getData();
                String data = bundle.getString(BACKUP_KEY);
                activity.handleUpdateMessage(data);
            }
        }
    }

    public void handleUpdateMessage(String data) {
        JsonObject obj = gson.fromJson(data, JsonObject.class);
        String backup_fn = obj.get("name").getAsString();
        String action = obj.get("action").getAsString();
        File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(FILE_PATH));

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(KcaBackupActivity.this);
        if (action.equals("restore")) {
            mBuilder.setTitle(getStringWithLocale(R.string.backup_dialog_import_title));
            mBuilder.setMessage(KcaUtils.format(getStringWithLocale(R.string.backup_dialog_import_msg), backup_fn));
            mBuilder.setPositiveButton(getStringWithLocale(R.string.dialog_ok), (dialog, which) -> {
                new BackupLoadTask().execute(backup_fn);
            });
            mBuilder.setNegativeButton(getStringWithLocale(R.string.dialog_cancel), ((dialog, which) -> {
                dialog.dismiss();
            }));
            AlertDialog mDialog = mBuilder.create();
            mDialog.show();
        } else if (action.equals("delete")) {
            mBuilder.setTitle(getStringWithLocale(R.string.backup_dialog_delete_title));
            mBuilder.setMessage(KcaUtils.format(getStringWithLocale(R.string.backup_dialog_delete_msg), backup_fn));
            mBuilder.setPositiveButton(getStringWithLocale(R.string.dialog_ok), (dialog, which) -> {
                File file = new File(savedir.getAbsolutePath().concat("/").concat(backup_fn));
                file.delete();
                exportMessage.setText("Deleted ".concat(backup_fn));
                adapter.setListItem(getBackupDataList());
                adapter.notifyDataSetChanged();
            });
            mBuilder.setNegativeButton(getStringWithLocale(R.string.dialog_cancel), ((dialog, which) -> {
                dialog.dismiss();
            }));
            AlertDialog mDialog = mBuilder.create();
            mDialog.show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }
}
