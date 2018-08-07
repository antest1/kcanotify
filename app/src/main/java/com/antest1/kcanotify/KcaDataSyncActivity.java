package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.antest1.kcanotify.KcaApiData.helper;
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
    boolean is_checked = false;

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

        KcaApiData.setDBHelper(dbHelper);
        loadTranslationData(getApplicationContext());
        loadQuestTrackDataFromStorage(dbHelper, getApplicationContext());
        ((TextView) findViewById(R.id.questsync_title)).setText(R.string.datasync_quest_title);

        findViewById(R.id.questsync_url).setOnClickListener(v -> {
            Uri uri = Uri.parse(getString(R.string.datasync_quest_url));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

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

        questCodeInput.addTextChangedListener(new KcaTextWatcher(questCodeInput));

        String current_code = dbHelper.getCurrentQuestCode();
        questCurrentCode.setText(current_code.length() > 0 ? current_code : "-");

        questCodeClearBtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.SRC_ATOP);
        questCodeClearBtn.setOnClickListener(v -> {
            resetCheck();
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
            resultText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPanelWarning));
            is_checked = true;
        });

        questCopyBtn.setOnClickListener(v -> {
            String code = questCurrentCode.getText().toString();
            if (code.length() > 0) {
                ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText("text", code));
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
            }
        });

        questSyncBtn.setOnClickListener(v -> {
            if (!is_checked) return;
            String code = questCodeInput.getText().toString().toUpperCase();
            boolean result = false;
            if (code.length() > 0) result = dbHelper.loadQuestDataFromCode(code, questSyncModeSwitch.isChecked(), System.currentTimeMillis());
            if (result) {
                String updated_code = dbHelper.getCurrentQuestCode();
                questCurrentCode.setText(updated_code.length() > 0 ? updated_code : "-");
                setResultText(updated_code);
                questCodeInput.setText(updated_code, TextView.BufferType.EDITABLE);
                resetCheck();
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.datasync_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.datasync_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetCheck() {
        resultText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
        is_checked = false;
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
        resultText.setText(getFormattedCodeInfo(code, questSyncModeSwitch.isChecked()));
    }



    public String getFormattedCodeInfo(String code, boolean mode) {
        StringBuilder data = new StringBuilder();
        if (KcaQuestCode.validate_code(code)) {
            JsonArray decoded_result = KcaQuestCode.decode_code(code);
            if (decoded_result == null) return "ERROR";
            for (int i = 0; i < decoded_result.size(); i++) {
                JsonObject item = decoded_result.get(i).getAsJsonObject();
                String key = item.get("code").getAsString();
                boolean active = item.get("active").getAsBoolean();
                if (kcQuestInfoData.has(key)) {
                    JsonObject quest_data = kcQuestInfoData.getAsJsonObject(key);
                    String quest_code = quest_data.get("code").getAsString();
                    String quest_name = quest_data.get("name").getAsString();
                    JsonArray quest_cond = item.getAsJsonArray("cond");
                    if (quest_cond.size() > 0) {
                        List<String> quest_cond_list = new ArrayList<>();
                        JsonArray quest_trackinfo = questTracker.getQuestTrackInfo(key);

                        for (int j = 0; j < quest_cond.size(); j++) {
                            String v = quest_cond.get(j).getAsString();
                            String u = "";
                            if (!mode) {
                                if (quest_trackinfo.size() > 0) {
                                    u = quest_trackinfo.get(j).getAsString() + "+";
                                } else {
                                    u = "0+";
                                }
                            }
                            quest_cond_list.add(u + v);
                        }
                        String active_str = active ? "" : "*";
                        String quest_cond_str = KcaUtils.joinStr(quest_cond_list, ", ");
                        data.append(KcaUtils.format("[%s%s] %s (%s)\n", quest_code, active_str, quest_name, quest_cond_str));
                    } else {
                        data.append(KcaUtils.format("[%s] %s\n", quest_code, quest_name));
                    }
                } else {
                    data.append("quest data not available");
                }
            }
            return data.toString().trim();
        } else {
            return "INVALID";
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

    public class KcaTextWatcher implements TextWatcher {
        private EditText mEditText;
        public KcaTextWatcher(EditText editText) {
            mEditText = editText;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            resetCheck();
        }
    }
}
