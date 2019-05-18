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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.T2_ITCP_FIGHTER;

public class KcaEquipListViewAdpater extends BaseAdapter {
    public static final String[] STAT_KEYS = {"api_houg", "api_raig", "api_tyku", "api_souk", "api_tais",  "api_baku", "api_houk", "api_saku", "api_houm", "api_leng", "api_cost", "api_distance"};
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private JsonObject shipEquipInfo = new JsonObject();
    private Map<String, AtomicInteger> countInfo = new HashMap<>();
    private List<Integer> active = new ArrayList<>();
    private String summary_format = "";
    private String searchQuery = "";
    private JsonObject shipStatTranslation = new JsonObject();

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

    public void setSearchQuery(String query) {
        searchQuery = query;
    }

    public void setStatTranslation(JsonObject data) {
        shipStatTranslation = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_equipment_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.equipment_item = v.findViewById(R.id.equipment_item);
            holder.equipment_id = v.findViewById(R.id.equipment_id);
            holder.equipment_icon = v.findViewById(R.id.equipment_icon);
            holder.equipment_name = v.findViewById(R.id.equipment_name);
            holder.equipment_count = v.findViewById(R.id.equipment_count);
            holder.equipment_item_detail = v.findViewById(R.id.equipment_item_detail);
            holder.equipment_item_detail_list = v.findViewById(R.id.equipment_item_detail_list);
            holder.equipment_item_detail_summary = v.findViewById(R.id.equipment_item_detail_summary);
            holder.equipment_item_detail_line = v.findViewById(R.id.equipment_item_detail_line);
            holder.equipment_stat = v.findViewById(R.id.equipment_stat);
            holder.equipment_labs_info = v.findViewById(R.id.equipment_labs_area);
            holder.equipment_cost = v.findViewById(R.id.equipment_cost);
            holder.equipment_dist = v.findViewById(R.id.equipment_dist);
            v.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) v.getTag();

        JsonObject item = listViewItemList.get(pos);
        int item_id = item.get("api_id").getAsInt();
        int item_type_2 = item.getAsJsonArray("api_type").get(2).getAsInt();
        int item_type_3 = item.getAsJsonArray("api_type").get(3).getAsInt();
        boolean is_item_aircraft = KcaApiData.isItemAircraft(item_type_2);
        String item_name = KcaApiData.getItemTranslation(item.get("api_name").getAsString());
        int item_count = getTotalCount(countInfo, item_id);
        final Integer target = item_id;

        holder.equipment_id.setText(String.valueOf(item_id));
        holder.equipment_icon.setImageResource(KcaUtils.getId(KcaUtils.format("item_%d", item_type_3), R.mipmap.class));
        holder.equipment_name.setText(item_name);
        holder.equipment_count.setText(KcaUtils.format("x%d", item_count));

        LayoutInflater inflater = LayoutInflater.from(context);
        holder.equipment_item_detail_list.removeAllViews();
        int item_equipped_count = 0;
        for (int lv = 0; lv <= 10; lv++) {
            String key_noalv = getItemKey(item_id, lv, -1);
            if (countInfo.containsKey(key_noalv)) {
                if (shipEquipInfo.has(key_noalv)) item_equipped_count += shipEquipInfo.getAsJsonArray(key_noalv).size();
                LinearLayout ship_equip_item = getEquipmentShipDetailView(context, inflater, holder.equipment_item_detail_list, lv, -1, key_noalv, is_item_aircraft);
                holder.equipment_item_detail_list.addView(ship_equip_item);
            }
            for (int alv = 0; alv <= 7; alv++) {
                String key_alv = getItemKey(item_id, lv, alv);
                if (countInfo.containsKey(key_alv)){
                    if (shipEquipInfo.has(key_alv)) item_equipped_count += shipEquipInfo.getAsJsonArray(key_alv).size();
                    LinearLayout ship_equip_item = getEquipmentShipDetailView(context, inflater, holder.equipment_item_detail_list, lv, alv, key_alv, is_item_aircraft);
                    holder.equipment_item_detail_list.addView(ship_equip_item);
                }
            }

        }
        holder.equipment_item_detail_summary.setText(KcaUtils.format(summary_format,
                item_count, item_equipped_count, item_count - item_equipped_count));

        holder.equipment_item_detail.setVisibility(active.contains(target) ? View.VISIBLE : View.GONE);
        holder.equipment_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.equipment_item_detail.getVisibility() == View.GONE) {
                    active.add(target);
                    holder.equipment_item_detail.setVisibility(View.VISIBLE);
                } else {
                    active.remove(target);
                    holder.equipment_item_detail.setVisibility(View.GONE);
                }
            }
        });
        holder.equipment_item_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                active.remove(target);
                holder.equipment_item_detail.setVisibility(View.GONE);
            }
        });

        List<String> stat_list = new ArrayList<>();
        for (String key: STAT_KEYS) {
            if (key.equals("api_cost") || key.equals("api_distance")) continue;
            if (item.has(key) && item.get(key).getAsInt() != 0) {
                int value = item.get(key).getAsInt();
                if (item_type_2 == T2_ITCP_FIGHTER && (key.equals("api_houm") || key.equals("api_houk"))) {
                    stat_list.add(KcaUtils.format("%s %d", shipStatTranslation.get(key.concat("2")).getAsString(), value));
                } else if (key.equals("api_leng")) {
                    stat_list.add(KcaUtils.format("%s %s", shipStatTranslation.get(key).getAsString(),
                            shipStatTranslation.get(key+value).getAsString()));
                } else {
                    stat_list.add(KcaUtils.format("%s %d", shipStatTranslation.get(key).getAsString(), value));
                }
            }
        }
        holder.equipment_stat.setText(KcaUtils.joinStr(stat_list, " | "));

        if (item.has("api_distance")) {
            holder.equipment_cost.setText(item.get("api_cost").getAsString());
            holder.equipment_dist.setText(item.get("api_distance").getAsString());
            holder.equipment_labs_info.setVisibility(View.VISIBLE);
        } else {
            holder.equipment_cost.setText("");
            holder.equipment_dist.setText("");
            holder.equipment_labs_info.setVisibility(View.GONE);
        }

        return v;
    }

    private JsonObject getEquipmentShipDetail(String key) {
        JsonObject ret_data = new JsonObject();
        JsonArray data = shipEquipInfo.has(key) ? shipEquipInfo.getAsJsonArray(key) : new JsonArray();
        SparseArray<AtomicInteger> counter = new SparseArray<>();
        int count_sum = 0;
        for (int k = 0; k < data.size(); k++) {
            JsonObject ship_info = data.get(k).getAsJsonObject();
            int ship_id = ship_info.get("id").getAsInt();
            if(counter.indexOfKey(ship_id) < 0) {
                counter.put(ship_id,  new AtomicInteger(1));
            } else {
                counter.get(ship_id).incrementAndGet();
            }
        }
        List<String> equipment_ship_info_list = new ArrayList<>();
        for (int k = 0; k < data.size(); k++) {
            JsonObject ship_info = data.get(k).getAsJsonObject();
            int ship_id = ship_info.get("id").getAsInt();
            int ship_lv = ship_info.get("lv").getAsInt();
            String ship_name = KcaApiData.getShipTranslation(ship_info.get("name").getAsString(), false);
            int ship_count = counter.get(ship_id).get();
            String val = KcaUtils.format("%s (Lv %d) x%d", ship_name, ship_lv, ship_count);
            if (!equipment_ship_info_list.contains(val)) {
                equipment_ship_info_list.add(val);
                count_sum += ship_count;
            }
        }

        String content = KcaUtils.joinStr(equipment_ship_info_list, ", ");
        ret_data.addProperty("ship", (content.length() > 0) ? content : "-");
        ret_data.addProperty("count", count_sum);
        return ret_data;
    }

    private LinearLayout getEquipmentShipDetailView(Context context, LayoutInflater inflater, LinearLayout root, int lv, int alv, String key, boolean is_aircraft) {
        LinearLayout ship_equip_item = (LinearLayout)inflater.inflate(R.layout.listview_equipment_count, root, false);
        TextView ship_equip_item_lv = ship_equip_item.findViewById(R.id.equipment_lv);
        TextView ship_equip_item_alv = ship_equip_item.findViewById(R.id.equipment_alv);
        TextView ship_equip_item_ships = ship_equip_item.findViewById(R.id.equipment_ships);
        TextView ship_equip_item_cnt_sum = ship_equip_item.findViewById(R.id.equipment_cnt_sum);
        JsonObject ship_equip_detail = getEquipmentShipDetail(key);

        ship_equip_item_lv.setText(getLvText(lv));
        if (lv == 0) ship_equip_item_lv.setTextColor(ContextCompat.getColor(context, R.color.grey));
        else ship_equip_item_lv.setTextColor(ContextCompat.getColor(context, R.color.itemlevel));
        if (is_aircraft) {
            ship_equip_item_alv.setVisibility(View.VISIBLE);
            ship_equip_item_alv.setText(getAlvText(alv));
            if (alv < 0) ship_equip_item_alv.setTextColor(ContextCompat.getColor(context, R.color.grey));
            else if (alv < 4) ship_equip_item_alv.setTextColor(ContextCompat.getColor(context, R.color.itemalv1));
            else ship_equip_item_alv.setTextColor(ContextCompat.getColor(context, R.color.itemalv2));
        } else {
            ship_equip_item_alv.setVisibility(View.GONE);
            ship_equip_item_alv.setText("");
        }

        int item_count = countInfo.get(key).get();
        int item_equip_count = ship_equip_detail.get("count").getAsInt();
        ship_equip_item_cnt_sum.setText(KcaUtils.format(summary_format,
                item_count, item_equip_count, item_count - item_equip_count));
        ship_equip_item_ships.setText(ship_equip_detail.get("ship").getAsString());
        return ship_equip_item;
    }

    private String getLvText(int value) {
        return KcaUtils.format("★+%d", value);
    }

    private String getAlvText(int value) {
        return KcaUtils.format("+%d", value > 0 ? value : 0);
    }

    private String getItemKey(int id, int lv, int alv) {
        if (alv == -1) return KcaUtils.format("%d_%d_n", id, lv);
        else return KcaUtils.format("%d_%d_%d", id, lv, alv);
    }

    static class ViewHolder {
        ImageView equipment_icon, equipment_item_detail_line;
        TextView equipment_id, equipment_name, equipment_count, equipment_item_detail_summary;
        TextView equipment_stat, equipment_cost, equipment_dist;
        LinearLayout equipment_item, equipment_item_detail, equipment_item_detail_list, equipment_labs_info;
    }

    public void setSummaryFormat(String value) {
        summary_format = value;
    }

    public void setListViewItemList(JsonArray total_equip_list, Map<String, AtomicInteger> counter, JsonObject ship_equip_info, final String filtcond) {
        shipEquipInfo = ship_equip_info;
        countInfo = counter;
        if (filtcond.length() > 0) {
            Type listType = new TypeToken<List<JsonObject>>() {}.getType();
            listViewItemList = new Gson().fromJson(total_equip_list, listType);
            listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, new Predicate<JsonObject>() {
                @Override
                public boolean apply(JsonObject input) {
                    int key = input.get("api_id").getAsInt();
                    String item_name = KcaApiData.getItemTranslation(input.get("api_name").getAsString());
                    if (!item_name.contains(searchQuery)) return false;
                    if (filtcond.equals("all")) {
                        return checkCountExist(countInfo, key);
                    } else {
                        Integer type_3 = input.getAsJsonArray("api_type").get(3).getAsInt();
                        String[] position_list = filtcond.split(",");
                        List<Integer> filt_list = new ArrayList<Integer>();
                        for (String p: position_list) {
                            filt_list.add(Integer.valueOf(p) + 1);
                        }
                        return filt_list.contains(type_3) && checkCountExist(countInfo, key);
                    }
                }}));

            StatComparator cmp = new StatComparator();
            Collections.sort(listViewItemList, cmp);
        } else {
            listViewItemList.clear();
        }
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

    private int getTotalCount(Map<String, AtomicInteger> count, int equip_id) {
        int cnt = 0;
        for (String key: count.keySet()) {
            String[] key_split = key.split("_");
            int target_equip_id = Integer.parseInt(key_split[0]);
            if (target_equip_id == equip_id) cnt += count.get(key).get();
        }
        return cnt;
    }

    private boolean checkCountExist(Map<String, AtomicInteger> count, int equip_id) {
        for (String key: count.keySet()) {
            String[] key_split = key.split("_");
            int target_equip_id = Integer.parseInt(key_split[0]);
            if (target_equip_id == equip_id) return true;
        }
        return false;
    }
}
