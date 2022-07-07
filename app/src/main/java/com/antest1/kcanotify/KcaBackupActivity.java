package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KcaBackupActivity extends AppCompatActivity {
    private final String FILE_PATH = "/backup";

    Toolbar toolbar;
    List<String> db_paths = new ArrayList();
    boolean is_exporting = false;

    TextView exportButton;
    TextView exportMessage;

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

        exportButton = findViewById(R.id.backup_export_button);
        exportButton.setOnClickListener(v -> {
            new BackupSaveTask().execute();
        });

        exportMessage = findViewById(R.id.backup_export_result_message);
        exportMessage.setText("");

        db_paths.add(KcaDBHelper.getName());
        db_paths.add(KcaQuestTracker.getName());
        db_paths.add(KcaDropLogger.getName());
        db_paths.add(KcaResourceLogger.getName());
        // Toast.makeText(getApplicationContext(), getDatabasePath().getAbsolutePath(), Toast.LENGTH_LONG).show();

        for (String name: db_paths) {
            Log.e("KCA", getDatabasePath(name).getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.droplog, menu);
        return true;
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
        File file;

        @Override
        protected void onPreExecute() {
            is_exporting = true;
            exportButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
            exportButton.setClickable(false);
            //row_count.setText(getStringWithLocale(R.string.action_save_msg));
            //row_count.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPanelWarning));
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
                    KcaUtils.copyFile(new FileInputStream(src), new FileOutputStream(dst));
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
                return "Error";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            is_exporting = false;
            exportButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            exportButton.setClickable(true);
            if ("Error".equals(result)) {
                exportMessage.setText("An error occurred when exporting backup file.");
            } else if (result != null) {
                exportMessage.setText("Exported to: ".concat(result));
            }
        }
    }

    public void setListView() {

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
