package com.antest1.kcanotify;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

// A number setting picked with a slider in a dialog (see KcaSliderPreferenceDialog), so it cannot be mistyped.
// The setting shows its value, and the dialog shows the description (android:dialogMessage).
// The value is stored as a string, like the text field it replaces, so saved values keep working.
public class KcaSliderPreference extends DialogPreference {
    private final int min;
    private final int max;
    private int value;

    public KcaSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KcaSliderPreference);
        min = a.getInt(R.styleable.KcaSliderPreference_sliderMin, 0);
        max = a.getInt(R.styleable.KcaSliderPreference_sliderMax, 100);
        a.recycle();
        setDialogLayoutResource(R.layout.pref_dialog_slider);
        setSummaryProvider(preference -> String.valueOf(value));
    }

    public int getMin() { return min; }

    public int getMax() { return max; }

    public int getValue() { return value; }

    public void setValue(int newValue) {
        value = clamp(newValue);
        persistString(String.valueOf(value));
        notifyChanged();
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        String saved = getPersistedString(defaultValue == null ? null : defaultValue.toString());
        int parsed = min;
        if (saved != null) {
            try {
                parsed = Integer.parseInt(saved.trim());
            } catch (NumberFormatException e) {
                // keep the minimum
            }
        }
        value = clamp(parsed);
    }
}
