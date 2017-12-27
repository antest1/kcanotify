package com.antest1.kcanotify;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.R.id.list;
import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.getAirForceResultString;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getShipTypeAbbr;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;

public class KcaShipListViewAdpater extends BaseAdapter {
    private long exp_sum = 0L;
    private List<JsonObject> listViewItemList = new ArrayList<>();

    public long getTotalExp() { return exp_sum; }

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
            v = inflater.inflate(R.layout.listview_shiplist_item, parent, false);
            ViewHolder holder = new ViewHolder();

            holder.ship_stat_2_0 = v.findViewById(R.id.ship_stat_2_0);
            holder.ship_id = v.findViewById(R.id.ship_id);
            holder.ship_name = v.findViewById(R.id.ship_name);
            holder.ship_karyoku = v.findViewById(R.id.ship_karyoku);
            holder.ship_raisou = v.findViewById(R.id.ship_raisou);
            holder.ship_taiku = v.findViewById(R.id.ship_taiku);
            holder.ship_soukou = v.findViewById(R.id.ship_soukou);
            holder.ship_stype = v.findViewById(R.id.ship_stype);
            holder.ship_lv = v.findViewById(R.id.ship_lv);
            holder.ship_hp = v.findViewById(R.id.ship_hp);
            holder.ship_cond = v.findViewById(R.id.ship_cond);
            holder.ship_exp = v.findViewById(R.id.ship_exp);
            holder.ship_kaihi = v.findViewById(R.id.ship_kaihi);
            holder.ship_sakuteki = v.findViewById(R.id.ship_sakuteki);
            holder.ship_luck = v.findViewById(R.id.ship_luck);
            holder.ship_yasen = v.findViewById(R.id.ship_yasen);
            holder.ship_taisen = v.findViewById(R.id.ship_taisen);
            holder.ship_sally_area = v.findViewById(R.id.ship_sally_area);
            holder.ship_equip_slot = new TextView[4];
            holder.ship_equip_icon = new ImageView[5];
            for (int i = 0; i < 4; i++) {
                holder.ship_equip_slot[i] = v.findViewById(KcaUtils.getId(KcaUtils.format("ship_equip_%d_slot", i + 1), R.id.class));
            }
            for (int i = 0; i < 5; i++) {
                holder.ship_equip_icon[i] = v.findViewById(KcaUtils.getId(KcaUtils.format("ship_equip_%d_icon", i + 1), R.id.class));
            }
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);
        int kc_ship_id = item.get("api_ship_id").getAsInt();
        JsonObject kcShipData = getKcShipDataById(kc_ship_id, "name,stype,houg,raig,tyku,souk,tais,luck,afterlv");
        String ship_name = kcShipData.get("name").getAsString();
        int ship_stype = kcShipData.get("stype").getAsInt();
        int ship_init_ka = kcShipData.getAsJsonArray("houg").get(0).getAsInt();
        int ship_init_ra = kcShipData.getAsJsonArray("raig").get(0).getAsInt();
        int ship_init_ta = kcShipData.getAsJsonArray("tyku").get(0).getAsInt();
        int ship_init_so = kcShipData.getAsJsonArray("souk").get(0).getAsInt();
        int ship_init_lk = kcShipData.getAsJsonArray("luck").get(0).getAsInt();
        int ship_afterlv = kcShipData.get("afterlv").getAsInt();

        JsonArray ship_slot = item.getAsJsonArray("api_slot");
        JsonArray ship_onslot = item.getAsJsonArray("api_onslot");
        int ship_slot_ex = item.get("api_slot_ex").getAsInt();
        int ship_ex_item_icon = 0;

        int slot_sum = 0;
        JsonArray ship_item_icon = new JsonArray();
        for (int j = 0; j < ship_slot.size(); j++) {
            int item_id = ship_slot.get(j).getAsInt();
            if (item_id > 0) {
                JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "type");
                if (itemData != null) {
                    int item_type = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                    ship_item_icon.add(item_type);
                }
            } else {
                ship_item_icon.add(0);
            }
        }
        if (ship_slot_ex > 0) {
            JsonObject ex_item_data = getUserItemStatusById(ship_slot_ex, "level,alv", "type");
            if (ex_item_data != null) {
                ship_ex_item_icon = ex_item_data.get("type").getAsJsonArray().get(3).getAsInt();
            }
        }

        for (int j = 0; j < ship_onslot.size(); j++) {
            slot_sum += ship_onslot.get(j).getAsInt();
        }

        ViewHolder holder = (ViewHolder) v.getTag();
        holder.ship_id.setText(item.get("api_id").getAsString());
        holder.ship_stype.setText(KcaApiData.getShipTypeAbbr(ship_stype));
        holder.ship_name.setText(KcaApiData.getShipTranslation(ship_name, false));

        int ship_lv = item.get("api_lv").getAsInt();
        holder.ship_lv.setText(KcaUtils.format("LV %d", ship_lv));

        if (ship_afterlv != 0 && ship_lv >= ship_afterlv) {
            holder.ship_stat_2_0.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatRemodel));
            holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_stat_2_0.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            if (ship_lv >= 100) {
                holder.ship_name.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
                holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
                holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
            } else {
                holder.ship_name.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
                holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
                holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
            }
        }

        holder.ship_exp.setText(KcaUtils.format("Next: %d", item.getAsJsonArray("api_exp").get(1).getAsInt()));

        int nowhp = item.get("api_nowhp").getAsInt();
        int maxhp = item.get("api_maxhp").getAsInt();
        if (maxhp >= 100) {
            holder.ship_hp.setText(KcaUtils.format("%d/%d", nowhp, maxhp));
        } else {
            holder.ship_hp.setText(KcaUtils.format("%d / %d", nowhp, maxhp));
        }
        if (nowhp * 4 <= maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorHeavyDmgState));
        } else if (nowhp * 2 <= maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorModerateDmgState));
        } else if (nowhp * 4 <= maxhp * 3) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightDmgState));
        } else if (nowhp != maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNormalState));
        } else {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFullState));
        }

        JsonArray ship_kyouka = item.getAsJsonArray("api_kyouka");

        JsonArray ship_ka = item.getAsJsonArray("api_karyoku");
        JsonArray ship_ra = item.getAsJsonArray("api_raisou");
        JsonArray ship_ta = item.getAsJsonArray("api_taiku");
        JsonArray ship_so = item.getAsJsonArray("api_soukou");
        JsonArray ship_ts = item.getAsJsonArray("api_taisen");
        JsonArray ship_kh = item.getAsJsonArray("api_kaihi");
        JsonArray ship_st = item.getAsJsonArray("api_sakuteki");
        JsonArray ship_lk = item.getAsJsonArray("api_lucky");

        if (ship_init_ka + ship_kyouka.get(0).getAsInt() == ship_ka.get(1).getAsInt()) {
            holder.ship_karyoku.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatKaryoku));
            holder.ship_karyoku.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_karyoku.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_karyoku.setTextColor(ContextCompat.getColor(context, R.color.colorStatKaryoku));
        }
        holder.ship_karyoku.setText(ship_ka.get(0).getAsString());

        if (ship_init_ra + ship_kyouka.get(1).getAsInt() == ship_ra.get(1).getAsInt()) {
            holder.ship_raisou.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatRaisou));
            holder.ship_raisou.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_raisou.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_raisou.setTextColor(ContextCompat.getColor(context, R.color.colorStatRaisou));
        }
        holder.ship_raisou.setText(ship_ra.get(0).getAsString());

        holder.ship_yasen.setText(KcaUtils.format("%d", ship_ka.get(0).getAsInt() + ship_ra.get(0).getAsInt()));

        if (ship_init_ta + ship_kyouka.get(2).getAsInt() == ship_ta.get(1).getAsInt()) {
            holder.ship_taiku.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatTaiku));
            holder.ship_taiku.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_taiku.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_taiku.setTextColor(ContextCompat.getColor(context, R.color.colorStatTaiku));
        }
        holder.ship_taiku.setText(ship_ta.get(0).getAsString());

        if (ship_init_so + ship_kyouka.get(3).getAsInt() == ship_so.get(1).getAsInt()) {
            holder.ship_soukou.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatSoukou));
            holder.ship_soukou.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_soukou.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_soukou.setTextColor(ContextCompat.getColor(context, R.color.colorStatSoukou));
        }
        holder.ship_soukou.setText(ship_so.get(0).getAsString());

        if (ship_ts.get(0).getAsInt() >= 100) {
            holder.ship_taisen.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatTaisen));
            holder.ship_taisen.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_taisen.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_taisen.setTextColor(ContextCompat.getColor(context, R.color.colorStatTaisen));
        }
        holder.ship_taisen.setText(ship_ts.get(0).getAsString());

        holder.ship_kaihi.setText(ship_kh.get(0).getAsString());
        holder.ship_sakuteki.setText(ship_st.get(0).getAsString());

        if (ship_init_lk + ship_kyouka.get(4).getAsInt() == ship_lk.get(1).getAsInt()) {
            holder.ship_luck.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatLuck));
            holder.ship_luck.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_luck.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            holder.ship_luck.setTextColor(ContextCompat.getColor(context, R.color.colorStatLuck));
        }
        holder.ship_luck.setText(ship_lk.get(0).getAsString());

        for (int i = 0; i < 4; i++) {
            int item_id = ship_item_icon.get(i).getAsInt();
            if (item_id == 0) {
                holder.ship_equip_icon[i].setVisibility(View.INVISIBLE);
            } else {
                holder.ship_equip_icon[i].setImageResource(
                        KcaUtils.getId(KcaUtils.format("item_%d", item_id), R.mipmap.class));
                holder.ship_equip_icon[i].setVisibility(View.VISIBLE);
            }
        }

        if (ship_slot_ex == 0) {
            holder.ship_equip_icon[4].setVisibility(View.INVISIBLE);
        } else {
            holder.ship_equip_icon[4].setImageResource(
                    KcaUtils.getId(KcaUtils.format("item_%d", ship_ex_item_icon), R.mipmap.class));
            holder.ship_equip_icon[4].setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < 4; i++) {
            if (slot_sum == 0) {
                holder.ship_equip_slot[i].setVisibility(View.INVISIBLE);
            } else {
                holder.ship_equip_slot[i].setText(ship_onslot.get(i).getAsString());
                holder.ship_equip_slot[i].setVisibility(View.VISIBLE);
            }
        }

        if (item.has("api_sally_area")) {
            holder.ship_sally_area.setBackgroundColor(ContextCompat.getColor(context,
                    KcaUtils.getId(KcaUtils.format("colorStatSallyArea%d", item.get("api_sally_area").getAsInt()), R.color.class)));
            holder.ship_sally_area.setVisibility(View.VISIBLE);
        } else {
            holder.ship_sally_area.setVisibility(View.GONE);
        }
        return v;
    }

    static class ViewHolder {
        LinearLayout ship_stat_2_0;
        TextView ship_id, ship_stype, ship_name, ship_lv, ship_exp, ship_hp, ship_cond;
        TextView ship_karyoku, ship_raisou, ship_taiku, ship_soukou;
        TextView ship_yasen, ship_taisen, ship_kaihi, ship_sakuteki, ship_luck;
        TextView[] ship_equip_slot;
        ImageView[] ship_equip_icon;
        ImageView ship_sally_area;
    }

    public void setListViewItemList(JsonArray ship_list, String sort_key, boolean is_desc) {
        exp_sum = 0;
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
        StatComparator cmp = new StatComparator(sort_key, is_desc);
        Collections.sort(listViewItemList, cmp);
        for (int i = 0; i < listViewItemList.size(); i++) {
            exp_sum += listViewItemList.get(i).getAsJsonArray("api_exp").get(0).getAsLong();
        }
    }

    public class StatComparator implements Comparator<JsonObject> {
        String sort_key;
        boolean is_desc;
        public StatComparator(String key, boolean desc) {
            sort_key = key;
            is_desc = desc;
        }
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String key = "api_id";
            if (o1.has(sort_key)) key = sort_key;
            int val1 = o1.get(key).getAsInt();
            int val2 = o2.get(key).getAsInt();
            if (is_desc) return val2 - val1;
            else return val1 - val2;
        }
    }
}
