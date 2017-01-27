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
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        LocaleUtils.setLocale(new Locale(pref.getString(PREF_KCA_LANGUAGE, "en")));
        LocaleUtils.updateConfig(this, getBaseContext().getResources().getConfiguration());
        ACRA.init(this);
    }
}
