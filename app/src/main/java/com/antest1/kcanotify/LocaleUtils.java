package com.antest1.kcanotify;

import static android.content.Context.MODE_PRIVATE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Locale;

public class LocaleUtils {
    private static final String TAG = "LocaleUtils";

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

    public static void setLocaleFromPreference(Context context) {
        String language, country;
        Locale defaultLocale = Locale.getDefault();
        SharedPreferences pref = context.getSharedPreferences("pref", MODE_PRIVATE);
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
        Log.d(TAG, "setLocaleFromPreference: " + LocaleUtils.getLocale().toLanguageTag());
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