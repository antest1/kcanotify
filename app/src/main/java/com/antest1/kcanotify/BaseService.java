package com.antest1.kcanotify;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BaseService extends Service {
    private final static String TAG = "BaseService";

    @Override
    public void onCreate() {
        super.onCreate();
        updateLocaleConfig();
    }

    protected void updateLocaleConfig() {
        Configuration config = new Configuration();
        config.locale = LocaleUtils.getLocale();
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
