package com.antest1.kcanotify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_ARRAY;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_BOOLEAN_LIST;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_SVC_ENABLED;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class KcaDataSyncActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    Button questSyncBtn;
    TextView resultText;
    KcaDBHelper dbHelper;
    KcaQuestTracker questTracker;

    public KcaDataSyncActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datasync);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_datasync));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);

        questSyncBtn = findViewById(R.id.questsync_btn);
        resultText = findViewById(R.id.questsync_result);

        questSyncBtn.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(KcaDataSyncActivity.this);
            integrator.setCaptureActivity( ZxingActivity.class );
            integrator.setOrientationLocked(false);
            integrator.setBeepEnabled(false);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        String result = "";
        String format = "";
        if (scanningResult != null) {
            if (scanningResult.getContents() != null) {
                result = scanningResult.getContents();
                format = scanningResult.getFormatName();
            }
            if (result.length() > 0) {
                String decoded_result = KcaQuestCode.decode_code(result).toString();
                resultText.setText(KcaUtils.format("%s\n(%s)", decoded_result, result));
            } else {
                resultText.setText("");
            }
        } else {
            resultText.setText(KcaUtils.format("nothing scanned"));
        }
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
}
