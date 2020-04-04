package com.antest1.kcanotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Map;
import java.util.regex.Pattern;

import static com.antest1.kcanotify.KcaConstants.PREF_VPN_BYPASS_ADDRESS;

public class NestedPreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, androidx.preference.Preference.OnPreferenceChangeListener {
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Map<String, ?> allEntries = getPreferenceManager().getSharedPreferences().getAll();
        //SharedPreferences prefs = this.getActivity().getSharedPreferences("pref", MODE_PRIVATE);
        for (String key : allEntries.keySet()) {
            Preference pref = findPreference(key);
            if (pref == null) continue;

            pref.setOnPreferenceChangeListener(this);
            if (pref instanceof ListPreference) {
                ListPreference etp = (ListPreference) pref;
                pref.setSummary(etp.getEntry());
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName("pref");
        int pref_key = getArguments().getInt(NESTED_TAG);
        Log.e("KCA", "PREF_KEY " + pref_key);
        switch (pref_key) {
            case FRAGMENT_ADV_NETWORK:
                setPreferencesFromResource(R.xml.advance_network_settings, rootKey);
                getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
                ((AppCompatActivity) getActivity()).getSupportActionBar()
                        .setTitle(getStringWithLocale(R.string.setting_menu_kand_title_adv_network));
                break;
            default:
                break;
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (PREF_VPN_BYPASS_ADDRESS.equals(key)) {
            String value = (String) newValue;
            if (value.equals(""))
                return true;
            Pattern cidrPattern = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])/(\\d|[1-2]\\d|3[0-2])$");
            String[] cidrs = (value).split(",");
            for (String cidr : cidrs) {
                if (!cidrPattern.matcher(cidr.trim()).find()) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.sa_bypass_list_invalid), Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        return true;
    }
}
