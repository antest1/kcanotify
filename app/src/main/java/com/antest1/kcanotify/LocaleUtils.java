package com.antest1.kcanotify;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.ContextThemeWrapper;

import java.util.Locale;

public class LocaleUtils {

    private static Locale sLocale;

    public static Locale getLocale() {
        return sLocale;
    }

    public static void setLocale(Locale locale) {
        sLocale = locale;
        if(sLocale != null) {
            Locale.setDefault(sLocale);
        }
    }

    public static void updateConfig(ContextThemeWrapper wrapper) {
        if(sLocale != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration configuration = new Configuration();
            configuration.setLocale(sLocale);
            wrapper.applyOverrideConfiguration(configuration);
        }
    }

    public static void updateConfig(Application app, Configuration configuration) {
        if(sLocale != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //Wrapping the configuration to avoid Activity endless loop
            Configuration config = new Configuration(configuration);
            config.locale = sLocale;
            Resources res = app.getBaseContext().getResources();
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }

    public static String getLocaleCode() {
        String language = sLocale.getLanguage();
        String country = sLocale.getCountry();
        String pref = language.concat("-").concat(country);

        if (pref.startsWith("ko")) {
            return "ko";
        } else if (pref.startsWith("en")) {
            return "en";
        } else if (pref.startsWith("zh")) {
            if (pref.equals("zh-CN") || pref.equals("zh-SG")) {
                return "scn";
            } else {
                return "tcn";
            }
        } else if (pref.startsWith("ja")) {
            return "jp";
        } else {
            return "en";
        }
    }

    public static String getResourceLocaleCode() {
        String locale = getLocaleCode();
        if (locale.equals("es")) locale = "en";
        return locale;
    }
}