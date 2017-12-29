package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaUtils.getId;

public class KcaExpeditionTableViewAdpater extends BaseAdapter {
    private long exp_sum = 0L;
    private List<JsonObject> listViewItemList = new ArrayList<>();

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
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
            v = inflater.inflate(R.layout.listview_expedtable_item, parent, false);
            ViewHolder holder = new ViewHolder();

            holder.value_no= v.findViewById(R.id.value_no);
            holder.value_name= v.findViewById(R.id.value_name);
            holder.value_hq_exp= v.findViewById(R.id.value_hq_exp);
            holder.value_ship_exp= v.findViewById(R.id.value_ship_exp);
            holder.value_fuel= v.findViewById(R.id.value_fuel);
            holder.value_ammo= v.findViewById(R.id.value_ammo);
            holder.value_steel= v.findViewById(R.id.value_steel);
            holder.value_bauxite= v.findViewById(R.id.value_bauxite);
            holder.value_spitem1_type= v.findViewById(R.id.value_spitem1_type);
            holder.value_spitem2_type= v.findViewById(R.id.value_spitem2_type);
            holder.value_spitem1_count= v.findViewById(R.id.value_spitem1_count);
            holder.value_spitem2_count= v.findViewById(R.id.value_spitem2_count);
            holder.value_condition= v.findViewById(R.id.value_condition);
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);

        ViewHolder holder = (ViewHolder) v.getTag();


        return v;
    }

    static class ViewHolder {
        TextView value_no, value_name, value_hq_exp, value_ship_exp;
        TextView value_fuel, value_ammo, value_steel, value_bauxite;
        ImageView value_spitem1_type, value_spitem2_type;
        TextView value_spitem1_count, value_spitem2_count;
        TextView value_condition;
    }

    public void setListViewItemList(JsonArray ship_list, int area_no) {
        exp_sum = 0;
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
    }
}
