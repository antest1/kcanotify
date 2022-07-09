package com.antest1.kcanotify;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KcaBackupItemAdpater extends BaseAdapter {
    public static final String BACKUP_KEY = "BACKUP_URL";
    public List<JsonObject> data = new ArrayList<>();
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
            v = inflater.inflate(R.layout.listview_backup, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.backup_name = v.findViewById(R.id.backup_name);
            holder.backup_restore = v.findViewById(R.id.backup_restore);
            holder.backup_restore.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            holder.backup_delete = v.findViewById(R.id.backup_delete);
            holder.backup_delete.setColorFilter(ContextCompat.getColor(context, R.color.colorPanelWarning), PorterDuff.Mode.SRC_ATOP);
            v.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) v.getTag();
        if (pos < data.size()) {
            JsonObject item = data.get(pos);
            holder.backup_name.setText(item.get("name").getAsString());
            holder.backup_restore.setOnClickListener(v1 -> {
                item.addProperty("action", "restore");
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString(BACKUP_KEY, item.toString());
                msg.setData(bundle);
                handler.sendMessage(msg);
            });

            holder.backup_delete.setOnClickListener(v1 -> {
                Message msg = handler.obtainMessage();
                item.addProperty("action", "delete");
                Bundle bundle = new Bundle();
                bundle.putString(BACKUP_KEY, item.toString());
                msg.setData(bundle);
                handler.sendMessage(msg);
            });
        }
        return v;
    }

    static class ViewHolder {
        TextView backup_name;
        ImageView backup_restore, backup_delete;
    }

    public void setListItem(List<JsonObject> data) {
        this.data = data;
        ItemComparator cmp = new ItemComparator();
        Collections.sort(this.data, cmp);
    }

    public void setHandler(Handler handler) { this.handler = handler; }

    private class ItemComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String n1 = o1.get("name").getAsString();
            String n2 = o2.get("name").getAsString();
            return n1.compareTo(n2);
        }
    }
}
