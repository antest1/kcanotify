package com.antest1.kcanotify;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private final static String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(LocaleUtils.getLocale());
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }
}
