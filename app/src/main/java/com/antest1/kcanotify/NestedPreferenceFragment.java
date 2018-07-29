package com.antest1.kcanotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.Map;
import java.util.regex.Pattern;

import static com.antest1.kcanotify.KcaConstants.PREF_VPN_BYPASS_ADDRESS;

public class NestedPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String NESTED_TAG = "NESTED_TAG";
    public final static int FRAGMENT_ADV_NETWORK = 701;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getActivity().getApplicationContext(), getActivity().getBaseContext(), id);
    }

    public static NestedPreferenceFragment newInstance(int key) {
        NestedPreferenceFragment fragment = new NestedPreferenceFragment();
        Bundle args = new Bundle();
        args.putInt(NESTED_TAG, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("pref");

        int pref_key = getArguments().getInt(NESTED_TAG);

        Log.e("KCA", "PREF_KEY " + pref_key);
        switch (pref_key) {
            case FRAGMENT_ADV_NETWORK:
                addPreferencesFromResource(R.xml.advance_network_settings);
                ((AppCompatActivity) getActivity()).getSupportActionBar()
                        .setTitle(getStringWithLocale(R.string.setting_menu_kand_title_adv_network));
                break;
            default:
                break;
        }

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Map<String, ?> allEntries = getPreferenceManager().getSharedPreferences().getAll();
        //SharedPreferences prefs = this.getActivity().getSharedPreferences("pref", MODE_PRIVATE);
        for (String key : allEntries.keySet()) {
            Preference pref = findPreference(key);
            if (key.equals(PREF_VPN_BYPASS_ADDRESS)) {
                pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.equals(""))
                            return true;
                        Pattern cidrPattern = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])/(\\d|[1-2]\\d|3[0-2])$");
                        String[] cidrs = ((String) newValue).split(",");
                        for (String cidr : cidrs) {
                            if (!cidrPattern.matcher(cidr.trim()).find()) {
                                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.sa_bypass_list_invalid), Toast.LENGTH_LONG).show();
                                return false;
                            }
                        }
                        return true;
                    }
                });
            } else if (pref instanceof ListPreference) {
                ListPreference etp = (ListPreference) pref;
                pref.setSummary(etp.getEntry());
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
            }
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference etp = (ListPreference) pref;
            pref.setSummary(etp.getEntry());
        } else if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            pref.setSummary(etp.getText());
        }
    }
}
