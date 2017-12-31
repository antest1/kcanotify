package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class KcaItemMultipleAdapter extends BaseAdapter {
    Context context;
    int layout;
    int rescale = -1;
    List<Integer> item = new ArrayList<>();
    LayoutInflater inf;
    private List<Integer> selected = new ArrayList<>();

    public KcaItemMultipleAdapter(Context context, int layout, List<Integer> data) {
        this.context = context;
        this.layout = layout;
        this.item = data;
        inf = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }

    public void selectAll() {
        for (int i = 0; i < item.size(); i++) {
            if (!selected.contains(i)) selected.add(i);
        }
    }

    public void unselectAll() {
        selected.clear();
    }

    public void reverseSelect() {
        List<Integer> reversed = new ArrayList<>();
        for (int i = 0; i < item.size(); i++) {
            if (!selected.contains(i)) reversed.add(i);
        }
        selected = reversed;
    }

    public void setSelected(Integer position) {
        selected.add(position);
    }

    public void removeSelected(Integer position) {
        selected.remove(position);
    }

    public void reverseSelected(int position) {
        if (selected.contains(position)) removeSelected(position);
        else setSelected(position);
    }

    public String getPreference() {
        if (selected.size() == item.size()) return "all";
        else {
            List<String> val = new ArrayList<>();
            for (Integer i: selected) {
                val.add(String.valueOf(i));
            }
            return KcaUtils.joinStr(val, ",");
        }
    }

    public void loadFromPreference(String pref) {
        if (pref.equals("all")) selectAll();
        else if (pref.length() == 0) unselectAll();
        else {
            String[] pref_list = pref.split(",");
            for (String p: pref_list) {
                setSelected(Integer.valueOf(p));
            }
        }
    }

    public void setRescaleDp(int value) {
        rescale = value;
    }

    @Override
    public int getCount() {
        return item.size();
    }

    @Override
    public Object getItem(int position) {
        return item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView==null)
            convertView = inf.inflate(layout, null);
        ImageView iv = (ImageView) convertView.findViewById(R.id.setting_image_pic);
        iv.setImageResource(item.get(position));
        if (selected.contains(position)) iv.setBackground(ContextCompat.getDrawable(context, R.drawable.imagebtn_on));
        else iv.setBackground(ContextCompat.getDrawable(context, R.drawable.imagebtn_off));
        if (rescale > 0) {
            iv.getLayoutParams().width = rescale;
            iv.getLayoutParams().height = rescale;
        }
        return convertView;
    }
}