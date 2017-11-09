package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

public class KcaInspectViewAdpater extends BaseAdapter {
    private static Handler sHandler;
    private ArrayList<Map.Entry<String, String>> listViewItemList = new ArrayList<>();

    public void setHandler(Handler h) {
        sHandler = h;
    }

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
            v = inflater.inflate(R.layout.listivew_inspector, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.nameView = (TextView) v.findViewById(R.id.inspector_name);
            holder.valueView = (TextView) v.findViewById(R.id.inspector_value);
            v.setTag(holder);
        }

        final Map.Entry<String, String> item = listViewItemList.get(position);

        ViewHolder holder = (ViewHolder) v.getTag();
        holder.nameView.setText(item.getKey());
        holder.valueView.setText(item.getValue());

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, KcaInspectorDetailActivity.class);
                intent.putExtra("key", item.getKey());
                context.startActivity(intent);
            }
        });

        return v;
    }

    static class ViewHolder {
        TextView nameView;
        TextView valueView;
    }

    public void setListViewItemList(ArrayList<Map.Entry<String, String>> list) {
        listViewItemList = list;
    }
}
