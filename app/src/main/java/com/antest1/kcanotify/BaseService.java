package com.antest1.kcanotify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BaseService extends Service {
    private final static String TAG = "BaseService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(LocaleUtils.getLocale());
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
