package com.antest1.kcanotify;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;

import java.util.Locale;

import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;

@AcraCore(
        buildConfigClass = BuildConfig.class,
        sharedPreferencesName = "pref"
)
@AcraMailSender(
        mailTo = "kcanotify@gmail.com",
        reportAsFile = false
)

public class KcaApplication extends MultiDexApplication {
    public static Locale defaultLocale;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        ACRA.init(this);
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
                LocaleUtils.setLocale(defaultLocale);
            } else {
                language = pref_locale[0];
                country = pref_locale[1];
                LocaleUtils.setLocale(new Locale(language, country));
            }
        } else {
            pref.edit().remove(PREF_KCA_LANGUAGE).apply();
            LocaleUtils.setLocale(defaultLocale);
        }

        LocaleUtils.updateConfig(this, getBaseContext().getResources().getConfiguration());
        ACRA.init(this);
    }
}
