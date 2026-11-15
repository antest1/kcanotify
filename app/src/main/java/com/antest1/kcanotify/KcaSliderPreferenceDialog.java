package com.antest1.kcanotify;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.slider.Slider;

// The dialog of KcaSliderPreference: the description, the value and a slider; OK saves the value.
public class KcaSliderPreferenceDialog extends PreferenceDialogFragmentCompat {
    private static final String SAVE_STATE_VALUE = "KcaSliderPreferenceDialog.value";

    private int value;

    public static KcaSliderPreferenceDialog newInstance(String key) {
        KcaSliderPreferenceDialog fragment = new KcaSliderPreferenceDialog();
        Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    private KcaSliderPreference getSliderPreference() {
        return (KcaSliderPreference) getPreference();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        value = savedInstanceState == null
                ? getSliderPreference().getValue()
                : savedInstanceState.getInt(SAVE_STATE_VALUE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_VALUE, value);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        KcaSliderPreference preference = getSliderPreference();
        TextView valueView = view.findViewById(R.id.pref_slider_value);
        Slider slider = view.findViewById(R.id.pref_slider);
        slider.setValueFrom(preference.getMin());
        slider.setValueTo(preference.getMax());
        slider.setStepSize(1);
        slider.setValue(Math.max(preference.getMin(), Math.min(preference.getMax(), value)));
        valueView.setText(String.valueOf(value));
        slider.addOnChangeListener((s, v, fromUser) -> {
            value = Math.round(v);
            valueView.setText(String.valueOf(value));
        });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) return;
        KcaSliderPreference preference = getSliderPreference();
        if (preference.callChangeListener(value)) {
            preference.setValue(value);
        }
    }
}
