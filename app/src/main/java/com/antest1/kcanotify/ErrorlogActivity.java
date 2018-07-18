package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class ErrorlogActivity extends AppCompatActivity{
    private final int SHOW_LIMIT = 50;
    private final String LOG_PATH = "/logs";
    Vibrator vibrator;
    KcaDBHelper dbHelper;
    Toolbar toolbar;
    Button loadbtn, clearbtn, exportbtn;
    TextView text, exportPathView;
    String exportPath;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_errorlog);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.action_errorlog);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);

        text = findViewById(R.id.errorlogview);
        text.setText("");
        text.setLongClickable(true);
        text.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText("text", ((TextView) v).getText()));
                doVibrate(vibrator, 100);
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
                return false;
            }
        });

        loadbtn = findViewById(R.id.error_load);
        loadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> loglist = dbHelper.getErrorLog(SHOW_LIMIT, false);
                if (loglist.size() > 0) {
                    text.setText(joinStr(loglist, "\n"));
                } else {
                    text.setText("No Error Log");
                }
            }
        });

        clearbtn = findViewById(R.id.error_clear);
        clearbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(ErrorlogActivity.this);
                alert.setPositiveButton(getStringWithLocale(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearErrorLog();
                        text.setText("No Error Log");
                        Toast.makeText(getApplicationContext(), "Cleared", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                }).setNegativeButton(getStringWithLocale(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.setMessage(getStringWithLocale(R.string.errlog_dialog_message));
                alert.show();
            }
        });

        File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(LOG_PATH));
        if (!savedir.exists()) savedir.mkdirs();
        exportPath = savedir.getPath();
        exportPathView = findViewById(R.id.error_path);
        exportPathView.setText(exportPath);

        exportbtn = findViewById(R.id.error_export);
        exportbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> loglist = dbHelper.getErrorLog(-1, true);
                if (loglist.size() > 0) {
                    String filename = KcaUtils.format("/log_%s_%s.txt", BuildConfig.VERSION_NAME, String.valueOf(System.currentTimeMillis()));
                    File file = new File(exportPath.concat(filename));
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
                        for(String line: loglist) {
                            bw.write(line);
                            bw.write("\r\n");
                        }
                        bw.close();
                        Toast.makeText(getApplicationContext(), "Exported to ".concat(file.getPath()), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "An error occurred when exporting error log.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No log to export.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
