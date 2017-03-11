package com.antest1.kcanotify;

/**
 * Created by Gyeong Bok Lee on 2017-02-18.
 */

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import static com.antest1.kcanotify.KcaUtils.adjustAlpha;


public class KcaCustomToast extends Toast {
    Context mContext;

    public KcaCustomToast(Context context) {
        super(context);
        mContext = context;
    }

    public void showToast(String body, int duration, int color) {
        LayoutInflater inflater;
        View v;

        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.toast_layout, null);
        TextView text = (TextView) v.findViewById(R.id.toast_text);
        text.setText(body);
        text.setBackgroundColor(adjustAlpha(color, 0.8f));
        show(this, v, duration);
    }

    private void show(Toast toast, View v, int duration) {
        //toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(duration);
        toast.setView(v);
        toast.show();
    }
}

