package com.antest1.kcanotify;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import com.google.gson.JsonObject;

public class KcaResoureLogFragment extends Fragment {
    public static JsonObject resourceData;
    public int position;

    public static void setData(JsonObject data) {

    }

    public static KcaResoureLogFragment create(JsonObject data, int pos) {
        resourceData = data;
        KcaResoureLogFragment fragment = new KcaResoureLogFragment();
        Bundle b = new Bundle();
        b.putInt("position", pos);;
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_resourcelog, container, false);
        position = getArguments().getInt("position");
        v = setView(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public View setView(View v) {
        Log.e("KCA", "setView " + position);
        v.setTag("fragment_view");
        TextView tv = v.findViewById(R.id.test);
        tv.setText(resourceData.get(String.valueOf(position)).toString());
        v.findViewById(KcaUtils.getId(KcaUtils.format("reslog_chart_filter_box_%d", position), R.id.class)).setVisibility(View.VISIBLE);
        v.findViewById(KcaUtils.getId(KcaUtils.format("reslog_chart_filter_box_%d", 1 - position), R.id.class)).setVisibility(View.GONE);
        return v;
    }



}
