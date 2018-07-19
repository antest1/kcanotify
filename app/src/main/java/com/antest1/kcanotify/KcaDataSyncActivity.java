package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.antest1.kcanotify.KcaApiData.kcQuestInfoData;
import static com.antest1.kcanotify.KcaApiData.loadQuestTrackDataFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaUtils.doVibrate;


public class KcaDataSyncActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    ImageView questCodeScanBtn, questCodeClearBtn;
    TextView resultText, questCurrentCode, questCurrentCodeLabel, questSyncModeText;
    TextView questCheckBtn, questSyncBtn, questCopyBtn;
    KcaDBHelper dbHelper;
    KcaQuestTracker questTracker;
    EditText questCodeInput;
    SwitchCompat questSyncModeSwitch;

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

        loadTranslationData(getApplicationContext());
        loadQuestTrackDataFromStorage(dbHelper, getApplicationContext());
        ((TextView) findViewById(R.id.questsync_title)).setText(R.string.datasync_quest_title);

        questCodeScanBtn = findViewById(R.id.questsync_btn);
        questCodeClearBtn = findViewById(R.id.questsync_clear);
        questCheckBtn = findViewById(R.id.questsync_button_check);
        questSyncBtn = findViewById(R.id.questsync_button_sync);
        questCopyBtn = findViewById(R.id.questsync_button_copy);

        resultText = findViewById(R.id.questsync_result);
        questCodeInput = findViewById(R.id.questsync_input);
        questSyncModeSwitch = findViewById(R.id.mode_switch);
        questSyncModeText = findViewById(R.id.mode_text);
        questCurrentCodeLabel = findViewById(R.id.questsync_current_label);
        questCurrentCode = findViewById(R.id.questsync_code);

        questSyncModeText.setText(getStringWithLocale(R.string.datasync_mode_add));
        questCurrentCodeLabel.setText(getStringWithLocale(R.string.datasync_current_label));

        String current_code = getCurrentQuestCode();
        questCurrentCode.setText(current_code.length() > 0 ? current_code : "-");

        questCodeClearBtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.SRC_ATOP);
        questCodeClearBtn.setOnClickListener(v -> {
            questCodeInput.getText().clear();
        });

        questCodeScanBtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.SRC_ATOP);
        questCodeScanBtn.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(KcaDataSyncActivity.this);
            integrator.setCaptureActivity( ZxingActivity.class );
            integrator.setOrientationLocked(false);
            integrator.setBeepEnabled(false);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.initiateScan();
        });

        questSyncModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                questSyncModeText.setText(getStringWithLocale(R.string.datasync_mode_overwrite));
                questSyncModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPanelWarning));
            } else {
                questSyncModeText.setText(getStringWithLocale(R.string.datasync_mode_add));
                questSyncModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
            }
            String code = questCodeInput.getText().toString().toUpperCase();
            setResultText(code);
        });

        questCheckBtn.setOnClickListener(v -> {
            String code = questCodeInput.getText().toString().toUpperCase();
            setResultText(code);
        });

        questCopyBtn.setOnClickListener(v -> {
            String code = questCurrentCode.getText().toString();
            if (code.length() > 0) {
                ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText("text", code));
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getCurrentQuestCode() {
        List<String> all_code = new ArrayList<>();
        JsonArray api_list = dbHelper.getCurrentQuestList();
        for (int i = 0; i < api_list.size(); i++) {
            JsonObject api_list_item = api_list.get(i).getAsJsonObject();
            String api_no = api_list_item.get("api_no").getAsString();
            all_code.add(KcaQuestCode.convert_to_code(api_no));
        }
        JsonArray tracked_quest = questTracker.getQuestTrackerData();
        for (int i = 0; i < tracked_quest.size(); i++) {
            JsonObject item = tracked_quest.get(i).getAsJsonObject();
            String id = item.get("id").getAsString();
            boolean active = item.get("active").getAsBoolean();
            JsonArray cond = item.getAsJsonArray("cond");
            int id_index = all_code.indexOf(KcaQuestCode.convert_to_code(id));
            String new_code =  KcaQuestCode.convert_to_code(id, cond, active);
            if (id_index != -1) {
                all_code.set(id_index, new_code);
            } else {
                all_code.add(new_code);
            }
        }
        return KcaUtils.joinStr(all_code, "");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        String result = "";
        if (scanningResult != null) {
            if (scanningResult.getContents() != null) {
                result = scanningResult.getContents();
            }
        }
        if (result.length() > 0) {
            questCodeInput.setText(result, TextView.BufferType.EDITABLE);
            setResultText(result);
        }
    }

    private void setResultText(String code) {
        resultText.setText(KcaQuestCode.getFormattedCodeInfo(code, questSyncModeSwitch.isChecked(), questTracker));
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
