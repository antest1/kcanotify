package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.PREF_DATALOAD_ERROR_FLAG;
import static com.antest1.kcanotify.KcaConstants.PREF_KR_NOTICE_CHK;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaKrNoticeActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static final String TAG = "KCA";

    public KcaKrNoticeActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_ipblock_kr);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CheckBox noshow = findViewById(R.id.ipblock_kr_noshow);
        noshow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setPreferences(getApplicationContext(), PREF_KR_NOTICE_CHK, "chk001");
            } else {
                setPreferences(getApplicationContext(), PREF_KR_NOTICE_CHK, "");
            }
        });

        TextView ok = findViewById(R.id.kr_ipblock_confirmed);
        ok.setOnClickListener(v -> {
            Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mainIntent);
            finish();
        });
    }
}