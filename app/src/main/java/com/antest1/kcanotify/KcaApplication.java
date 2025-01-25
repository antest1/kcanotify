package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.antest1.kcanotify.remote_capture.Utils;
import com.antest1.kcanotify.remote_capture.model.MatchList;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.getDecryptionListFromFile;
import static com.antest1.kcanotify.KcaConstants.PREF_DECRYPTION_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;

public class KcaApplication extends MultiDexApplication {
    public static Locale defaultLocale;
    private static WeakReference<KcaApplication> mInstance;
    private MatchList mDecryptionList;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = new WeakReference<>(this);

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

    public static @NonNull KcaApplication getInstance() {
        return mInstance.get();
    }

    @SuppressLint("ApplySharedPref")
    public MatchList getDecryptionList() {
        if(mDecryptionList == null) {
            SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
            String list = getDecryptionListFromFile(getApplicationContext());
            pref.edit().putString(PREF_DECRYPTION_LIST, list).commit();
            mDecryptionList = new MatchList(getApplicationContext(), PREF_DECRYPTION_LIST);
        }
        return mDecryptionList;
    }
}
