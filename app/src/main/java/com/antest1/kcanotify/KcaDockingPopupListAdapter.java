package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.STATE_HEAVYDMG;
import static com.antest1.kcanotify.KcaConstants.STATE_LIGHTDMG;
import static com.antest1.kcanotify.KcaConstants.STATE_MODERATEDMG;

public class KcaDockingPopupListAdapter extends BaseAdapter {
    private List<JsonObject> itemList = new ArrayList<>();

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
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
            v = inflater.inflate(R.layout.listview_dock, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.shipname = (TextView) v.findViewById(R.id.dock_ship);
            holder.repairtime = (TextView) v.findViewById(R.id.dock_time);
            v.setTag(holder);
        }

        final JsonObject item = itemList.get(position).getAsJsonObject();

        ViewHolder holder = (ViewHolder) v.getTag();
        holder.shipname.setText(item.get("name").getAsString());
        holder.repairtime.setText(item.get("time").getAsString());
        int state = item.get("state").getAsInt();
        boolean in_dock = item.get("dock").getAsBoolean();
        if (in_dock) {
            holder.shipname.setTextColor(ContextCompat.getColor(context, R.color.colorNormalState));
        } else {
            holder.shipname.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
        switch (state) {
            case STATE_HEAVYDMG:
                holder.repairtime.setBackgroundColor(ContextCompat.getColor(context, R.color.colorHeavyDmgState));
                break;
            case STATE_MODERATEDMG:
                holder.repairtime.setBackgroundColor(ContextCompat.getColor(context, R.color.colorModerateDmgState));
                break;
            case STATE_LIGHTDMG:
                holder.repairtime.setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightDmgState));
                break;
            default:
                holder.repairtime.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
                break;
        }

        return v;
    }

    static class ViewHolder {
        TextView shipname;
        TextView repairtime;
    }

    public void setItemList(List<JsonObject> list) {
        itemList = list;
    }
}
