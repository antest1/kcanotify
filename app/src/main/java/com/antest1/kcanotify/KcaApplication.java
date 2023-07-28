package com.antest1.kcanotify;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;

import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;

public class KcaApplication extends MultiDexApplication {
    public static Locale defaultLocale;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String language, country;
        defaultLocale = Locale.getDefault();
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String[] pref_locale = pref.getString(PREF_KCA_LANGUAGE, "").split("-");

        if (pref_locale.length == 2) {
            if (pref_locale[0].equals("default")) {
                LocaleUtils.setLocale(getBaseContext(), defaultLocale);
            } else {
                language = pref_locale[0];
                country = pref_locale[1];
                LocaleUtils.setLocale(getBaseContext(), new Locale(language, country));
            }
        } else {
            pref.edit().remove(PREF_KCA_LANGUAGE).apply();
            LocaleUtils.setLocale(getBaseContext(), defaultLocale);
        }

        FirebaseAnalytics.getInstance(this);
    }
}
