package com.antest1.kcanotify;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KcaResCheckItemAdpater extends BaseAdapter {
    public static final String RESCHK_KEY = "RESCHK_URL";
    public List<JsonObject> data = new ArrayList<>();
    private Context context;
    private Handler handler;

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
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
            v = inflater.inflate(R.layout.listview_rescheck, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.reschk_name = v.findViewById(R.id.reschk_name);
            holder.reschk_desc = v.findViewById(R.id.reschk_desc);
            holder.reschk_ver = v.findViewById(R.id.reschk_version);
            holder.reschk_download = v.findViewById(R.id.reschk_download);
            v.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) v.getTag();
        if (pos < data.size()) {
            JsonObject item = data.get(pos);
            holder.reschk_name.setText(item.get("name").getAsString());
            holder.reschk_desc.setText(item.get("desc").getAsString());
            holder.reschk_ver.setText(item.get("version_str").getAsString());
            holder.reschk_download.setText("Download");
            holder.reschk_download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString(RESCHK_KEY, item.toString());
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            });
            if (item.get("highlight").getAsBoolean()) {
                holder.reschk_ver.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPanelWarning));
                holder.reschk_ver.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                holder.reschk_ver.setBackground(null);
                holder.reschk_ver.setTextColor(ContextCompat.getColor(context, R.color.grey));
            }
        }
        return v;
    }

    static class ViewHolder {
        TextView reschk_name, reschk_desc, reschk_ver, reschk_download;
    }

    public void setListItem(List<JsonObject> data) {
        this.data = data;
        ItemComparator cmp = new ItemComparator();
        Collections.sort(this.data, cmp);
    }
    public void setContext(Context context) { this.context = context; }
    public void setHandler(Handler handler) { this.handler = handler; }

    private class ItemComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String v1 = o1.get("version").getAsString();
            String v2 = o2.get("version").getAsString();
            String n1 = o1.get("name").getAsString();
            String n2 = o2.get("name").getAsString();

            if (v1.equals(v2)) return n1.compareTo(n2);
            else return KcaUtils.compareVersion(v1, v2) ? -1 : 1;
        }
    }
}
