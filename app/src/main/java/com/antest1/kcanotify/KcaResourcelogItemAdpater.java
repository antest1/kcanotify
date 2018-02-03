package com.antest1.kcanotify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.R.id.droplog_item;

public class KcaResourcelogItemAdpater extends BaseAdapter {
    public static List<JsonObject> resourceData = new ArrayList<>();
    public static String time_format = "MM/dd";
    public static int color_item, color_item_acc;

    public int data_idx;

    @Override
    public int getCount() {
        return resourceData.size();
    }

    @Override
    public Object getItem(int position) {
        return resourceData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_resourcelist_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.reslog_item = v.findViewById(R.id.reslog_item);
            holder.item_0 = v.findViewById(R.id.reslog_item_0);
            holder.item_1 = v.findViewById(R.id.reslog_item_1);
            holder.item_2 = v.findViewById(R.id.reslog_item_2);
            holder.item_3 = v.findViewById(R.id.reslog_item_3);
            holder.item_4 = v.findViewById(R.id.reslog_item_4);
            v.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) v.getTag();
        JsonObject item = resourceData.get(pos);

        SimpleDateFormat dateFormat = new SimpleDateFormat(time_format, Locale.US);
        String timetext = dateFormat.format(new Date(item.get("timestamp").getAsLong()));
        holder.item_0.setText(timetext);

        if (data_idx == 0) {
            holder.item_1.setText(item.get("res_fuel").getAsString());
            holder.item_2.setText(item.get("res_ammo").getAsString());
            holder.item_3.setText(item.get("res_steel").getAsString());
            holder.item_4.setText(item.get("res_bauxite").getAsString());
        } else {
            holder.item_1.setText(item.get("con_bucket").getAsString());
            holder.item_2.setText(item.get("con_torch").getAsString());
            holder.item_3.setText(item.get("con_devmat").getAsString());
            holder.item_4.setText(item.get("con_screw").getAsString());
        }

        if ((pos + 1) % 5 == 0) {
            holder.reslog_item.setBackgroundColor(color_item_acc);
        } else {
            holder.reslog_item.setBackgroundColor(color_item);
        }

        return v;
    }

    static class ViewHolder {
        View reslog_item;
        TextView item_0, item_1, item_2, item_3, item_4;
    }

    public static void setListViewItemList(List<JsonObject> data) {
        resourceData = data;
    }

    public static void setTimeFormat(String fmt) {
        time_format = fmt;
    }

    public void setPosition(int idx) {
        data_idx = idx;
    }
}
