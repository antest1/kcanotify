package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.antest1.kcanotify.KcaApiData.getCurrentNodeAlphabet;
import static com.antest1.kcanotify.R.id.droplog_item;
import static com.antest1.kcanotify.R.id.ship_id;

public class KcaDroplogItemAdpater extends BaseAdapter {
    private List<JsonObject> listViewItemList = new ArrayList<>();
    public static int color_normal, color_none, color_item, color_item_acc;

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
            v = inflater.inflate(R.layout.listview_droplist_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.droplog_item = v.findViewById(droplog_item);
            holder.item_time = v.findViewById(R.id.droplog_item_time);
            holder.item_area = v.findViewById(R.id.droplog_item_area);
            holder.item_isboss = v.findViewById(R.id.droplog_item_isboss);
            holder.item_rank = v.findViewById(R.id.droplog_item_rank);
            holder.item_name = v.findViewById(R.id.droplog_item_name);
            v.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) v.getTag();
        JsonObject item = listViewItemList.get(pos);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        String timetext = dateFormat.format(new Date(item.get("timestamp").getAsLong()));
        holder.item_time.setText(timetext);

        int world = item.get("world").getAsInt();
        int map = item.get("map").getAsInt();
        int node = item.get("node").getAsInt();
        int maprank = item.get("maprank").getAsInt();
        String node_alpha = KcaApiData.getCurrentNodeAlphabet(world, map, node);
        if (maprank > 0) {
            holder.item_area.setText(KcaUtils.format("%d-%d[%s]-%s", world, map, KcaDropLogger.maprank_info[maprank], node_alpha));
        } else {
            holder.item_area.setText(KcaUtils.format("%d-%d-%s", world, map, node_alpha));
        }

        if (item.get("isboss").getAsInt() > 0) holder.item_isboss.setText("✓");
        else holder.item_isboss.setText("");

        holder.item_rank.setText(item.get("rank").getAsString());

        int ship_id = item.get("ship_id").getAsInt();
        if (ship_id <= 0) {
            if (ship_id == -1) holder.item_name.setText(KcaDropLogger.ship_full);
            else if (ship_id == 0) holder.item_name.setText(KcaDropLogger.ship_none);
            holder.item_name.setTextColor(color_none);
        }
        else {
            JsonObject kc_data = KcaApiData.getKcShipDataById(ship_id, "name");
            if (kc_data != null) {
                String kc_name = kc_data.get("name").getAsString();
                holder.item_name.setText(KcaApiData.getShipTranslation(kc_name, false));
            }
            holder.item_name.setTextColor(color_normal);
        }

        if ((pos + 1) % 5 == 0) {
            holder.droplog_item.setBackgroundColor(color_item_acc);
        } else {
            holder.droplog_item.setBackgroundColor(color_item);
        }

        return v;
    }

    static class ViewHolder {
        View droplog_item;
        TextView item_time, item_area, item_isboss, item_rank, item_name;
    }

    public void setListViewItemList(List<JsonObject> data, int sort_key) {
        listViewItemList = data;
    }

    private class StatComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            int o1_id = o1.get("api_id").getAsInt();
            int o2_id = o2.get("api_id").getAsInt();

            int o1_type_2 = o1.getAsJsonArray("api_type").get(2).getAsInt();
            int o2_type_2 = o2.getAsJsonArray("api_type").get(2).getAsInt();

            if (o1_type_2 != o2_type_2) return o1_type_2 - o2_type_2;
            return o1_id - o2_id;
        }
    }
}
