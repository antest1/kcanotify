package com.antest1.kcanotify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;

public class KcaApplication extends MultiDexApplication {
    private static WeakReference<KcaApplication> mInstance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = new WeakReference<>(this);
        LocaleUtils.setLocaleFromPreference(this);
        FirebaseAnalytics.getInstance(this);
    }

    public static @NonNull KcaApplication getInstance() {
        return mInstance.get();
    }
}
