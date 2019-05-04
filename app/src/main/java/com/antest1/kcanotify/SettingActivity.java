package com.antest1.kcanotify;

import android.app.FragmentManager;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.util.Locale;

import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.NestedPreferenceFragment.NESTED_TAG;

public class SettingActivity extends AppCompatActivity implements MainPreferenceFragment.Callback {
    public static String currentVersion = BuildConfig.VERSION_NAME;
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_USAGESTAT_PERMISSION = 3;

    Toolbar toolbar;
    public SettingActivity() {
        LocaleUtils.updateConfig(this);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_settings));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            FragmentManager fm = getFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new MainPreferenceFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNestedPreferenceSelected(int key) {
        Log.e("KCA", "onNestedPreferenceSelected");
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, NestedPreferenceFragment.newInstance(key), NESTED_TAG)
                .addToBackStack(NESTED_TAG).commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }

    public static String getDefaultValue(String prefKey) {
        switch (prefKey) {
            case PREF_KCA_SEEK_CN:
                return String.valueOf(SEEK_33CN1);
            case PREF_OPENDB_API_USE:
            case PREF_POIDB_API_USE:
            case PREF_KCAQSYNC_USE:
            case PREF_AKASHI_STAR_CHECKED:
            case PREF_KCA_SET_PRIORITY:
            case PREF_DISABLE_CUSTOMTOAST:
            case PREF_FAIRY_AUTOHIDE:
            case PREF_KCA_NOTI_AKASHI:
            case PREF_SHOW_CONSTRSHIP_NAME:
            case PREF_DATALOAD_ERROR_FLAG:
            case PREF_FIX_VIEW_LOC:
            case PREF_PACKET_LOG:
            case PREF_RES_USELOCAL:
            case PREF_FAIRY_DOWN_FLAG:
            case PREF_ALLOW_EXTFILTER:
            case PREF_HDNOTI_LOCKED:
                return "boolean_false";
            case PREF_KCA_EXP_VIEW:
            case PREF_KCA_NOTI_NOTIFYATSVCOFF:
            case PREF_KCA_NOTI_DOCK:
            case PREF_KCA_NOTI_EXP:
            case PREF_KCA_NOTI_MORALE:
            case PREF_KCA_BATTLEVIEW_USE:
            case PREF_KCA_BATTLENODE_USE:
            case PREF_KCA_QUESTVIEW_USE:
            case PREF_KCA_NOTI_V_HD:
            case PREF_KCA_NOTI_V_NS:
            case PREF_SHOWDROP_SETTING:
            case PREF_FAIRY_NOTI_LONGCLICK:
            case PREF_KCA_NOTI_QUEST_FAIRY_GLOW:
            case PREF_KCA_ACTIVATE_DROPLOG:
            case PREF_KCA_ACTIVATE_RESLOG:
            case PREF_CHECK_UPDATE_START:
                return "boolean_true";
            case PREF_KCA_LANGUAGE:
                return "R.string.default_locale";
            case PREF_KCA_NOTI_SOUND_KIND:
                return "R.string.sound_kind_value_vibrate";
            case PREF_KCA_NOTI_RINGTONE:
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();
            case PREF_APK_DOWNLOAD_SITE:
                return "R.string.app_download_link_playstore";
            case PREF_AKASHI_STARLIST:
            case PREF_AKASHI_FILTERLIST:
            case PREF_SHIPINFO_FILTCOND:
                return "|";
            case PREF_SHIPINFO_SORTKEY:
                return "|1,true|";
            case PREF_FAIRY_ICON:
            case PREF_KCA_EXP_TYPE:
            case PREF_VIEW_YLOC:
            case PREF_LAST_UPDATE_CHECK:
            case PREF_SNIFFER_MODE:
            case PREF_KCARESOURCE_VERSION:
            case PREF_LAST_QUEST_CHECK:
            case PREF_FAIRY_REV:
            case PREF_HDNOTI_MINLEVEL:
                return "0";
            case PREF_ALARM_DELAY:
                return "61";
            case PREF_KCA_MORALE_MIN:
                return "40";
            case PREF_EQUIPINFO_FILTCOND:
                return "all";
            case PREF_KCA_DATA_VERSION:
                return "R.string.default_gamedata_version";
            case PREF_UPDATE_SERVER:
                return "R.string.server_nova";
            case PREF_TIMER_WIDGET_STATE:
                return "{}";
            case PREF_PACKAGE_ALLOW:
                return "[]";
            case PREF_KC_PACKAGE:
                return KC_PACKAGE_NAME;
            default:
                return "";
        }
    }
}