package com.antest1.kcanotify;

/**
 * Created by alias on 2016-12-18.
 */

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

@ReportsCrashes(
        resToastText = R.string.error_text,
        mailTo = "kcanotify@gmail.com"
)

public class KcaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        String language, country;

        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String[] pref_locale = pref.getString(PREF_KCA_LANGUAGE, "").split("-");

        if (pref_locale.length == 2) {
            language = pref_locale[0];
            country = pref_locale[1];
        } else {
            language = Locale.getDefault().getLanguage();
            country = Locale.getDefault().getCountry();
        }

        Log.e("KCA", "Locale: " + language + "-" + country);
        LocaleUtils.setLocale(new Locale(language, country));
        LocaleUtils.updateConfig(this, getBaseContext().getResources().getConfiguration());
        ACRA.init(this);
    }
}
