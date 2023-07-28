package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.antest1.kcanotify.KcaConstants.NOTICE_CHK_CODE;
import static com.antest1.kcanotify.KcaConstants.PREF_NOTICE_CHK_FLAG;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaNoticeActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static final String TAG = "KCA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_notice);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CheckBox noshow = findViewById(R.id.notice_noshow);
        noshow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setPreferences(getApplicationContext(), PREF_NOTICE_CHK_FLAG, NOTICE_CHK_CODE);
            } else {
                setPreferences(getApplicationContext(), PREF_NOTICE_CHK_FLAG, "");
            }
        });

        TextView ok = findViewById(R.id.notice_confirmed);
        ok.setOnClickListener(v -> {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
            finish();
        });
    }
}