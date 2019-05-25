package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaUtils.searchStringFromStart;
import static com.antest1.kcanotify.KcaUtils.getId;

public class KcaShipListViewAdpater extends BaseAdapter {
    private long exp_sum = 0L;
    private JsonArray deckInfo = new JsonArray();
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private String searchQuery = "";
    private JsonObject specialEquipment = new JsonObject();

    private static final String[] total_key_list = {
            "api_id", "api_lv", "api_stype", "api_cond", "api_locked",
            "api_deck_id", "api_docking", "api_damage", "api_repair", "api_mission", "api_exslot",
            "api_karyoku", "api_raisou", "api_taiku", "api_soukou", "api_yasen",
            "api_taisen", "api_kaihi", "api_sakuteki", "api_lucky", "api_soku", "api_sort_id", "api_sally_area"};

    public long getTotalExp() { return exp_sum; }

    private static int[] sort_table = {0, 1, 2, 3, 5, 7, 8, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static int[] filt_table = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 22};

    public static int getSortKeyIndex(int position) {
        return sort_table[position];
    }

    public static int getSortIndexByKey(int key) {
        return Arrays.binarySearch(sort_table, key);
    }

    public static int getFilterKeyIndex(int position) {
        return filt_table[position];
    }

    public static int getFilterIndexByKey(int key) {
        return Arrays.binarySearch(filt_table, key);
    }

    public static boolean isList(int idx) {
        int[] list = {2, 5, 7, 20, 22};  // ship_filt_array
        return (Arrays.binarySearch(list, idx) >= 0);
    }

    public static boolean isBoolean(int idx) {
        int[] list = {4, 6, 9, 10}; // ship_filt_array
        return (Arrays.binarySearch(list, idx) >= 0);
    }

    public static boolean isNumeric(int idx) {
        return !isList(idx) && !isBoolean(idx);
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

    public void setSearchQuery(String query) {
        searchQuery = query;
    }

    public void setSpecialEquipment(JsonObject data) { specialEquipment = data; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_shiplist_item, parent, false);
            ViewHolder holder = new ViewHolder();

            holder.ship_back = v.findViewById(R.id.ship_back);
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
            holder.ship_fleet = v.findViewById(R.id.ship_fleet);
            holder.ship_cond = v.findViewById(R.id.ship_cond);
            holder.ship_exp = v.findViewById(R.id.ship_exp);
            holder.ship_kaihi = v.findViewById(R.id.ship_kaihi);
            holder.ship_sakuteki = v.findViewById(R.id.ship_sakuteki);
            holder.ship_luck = v.findViewById(R.id.ship_luck);
            holder.ship_yasen = v.findViewById(R.id.ship_yasen);
            holder.ship_taisen = v.findViewById(R.id.ship_taisen);
            holder.ship_sally_area = v.findViewById(R.id.ship_sally_area);
            holder.ship_equip_slot = new TextView[5];
            holder.ship_equip_icon = new ImageView[5];
            for (int i = 0; i < 5; i++) {
                holder.ship_equip_slot[i] = v.findViewById(getId(KcaUtils.format("ship_equip_%d_slot", i + 1), R.id.class));
            }
            for (int i = 0; i < 5; i++) {
                holder.ship_equip_icon[i] = v.findViewById(getId(KcaUtils.format("ship_equip_%d_icon", i + 1), R.id.class));
            }
            holder.ship_equip_slot_ex = v.findViewById(R.id.ship_equip_ex_slot);
            holder.ship_equip_icon_ex = v.findViewById(R.id.ship_equip_ex_icon);
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);
        int kc_ship_id = item.get("api_ship_id").getAsInt();
        JsonObject kcShipData = getKcShipDataById(kc_ship_id, "name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num");
        if (kcShipData == null) return v;

        String ship_name = kcShipData.get("name").getAsString();
        int ship_stype = kcShipData.get("stype").getAsInt();
        int ship_init_ka = kcShipData.getAsJsonArray("houg").get(0).getAsInt();
        int ship_init_ra = kcShipData.getAsJsonArray("raig").get(0).getAsInt();
        int ship_init_ta = kcShipData.getAsJsonArray("tyku").get(0).getAsInt();
        int ship_init_so = kcShipData.getAsJsonArray("souk").get(0).getAsInt();
        int ship_init_lk = kcShipData.getAsJsonArray("luck").get(0).getAsInt();
        int ship_afterlv = kcShipData.get("afterlv").getAsInt();
        int ship_slot_num = kcShipData.get("slot_num").getAsInt();

        JsonArray ship_slot = item.getAsJsonArray("api_slot");
        JsonArray ship_onslot = item.getAsJsonArray("api_onslot");
        int ship_slot_ex = item.get("api_slot_ex").getAsInt();
        int ship_ex_item_icon = 0;
        int ship_locked = item.get("api_locked").getAsInt();

        int slot_sum = 0;
        boolean flag_931 = false;
        JsonArray ship_item_icon = new JsonArray();
        for (int j = 0; j < ship_slot.size(); j++) {
            int item_id = ship_slot.get(j).getAsInt();
            if (item_id > 0) {
                JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "id,type");
                if (itemData != null) {
                    int item_kc_id = itemData.get("id").getAsInt();
                    if (item_kc_id == 82 || item_kc_id == 83) flag_931 = true;
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

        int in_docking = item.get("api_docking").getAsInt();
        int in_expedition = item.get("api_mission").getAsInt();

        ViewHolder holder = (ViewHolder) v.getTag();
        if (in_docking > 0) {
            holder.ship_back.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatInDock));
        } else if (in_expedition > 0) {
            holder.ship_back.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatInExpedition));
        } else {
            holder.ship_back.setBackgroundColor(ContextCompat.getColor(context, R.color.colorListItemBack));
        }

        int fleet = item.get("api_deck_id").getAsInt();
        if (fleet > 0 ) {
            holder.ship_fleet.setText(String.valueOf(fleet));
            holder.ship_fleet.setVisibility(View.VISIBLE);
        } else {
            holder.ship_fleet.setVisibility(View.GONE);
        }

        holder.ship_id.setText(item.get("api_id").getAsString());
        if (ship_locked > 0) {
            holder.ship_id.setTextColor(ContextCompat.getColor(context, R.color.colorStatLocked));
        } else {
            holder.ship_id.setTextColor(ContextCompat.getColor(context, R.color.colorStatNotLocked));
        }

        holder.ship_stype.setText(KcaApiData.getShipTypeAbbr(ship_stype));
        holder.ship_name.setText(KcaApiData.getShipTranslation(ship_name, false));

        int cond = item.get("api_cond").getAsInt();
        holder.ship_cond.setText(String.valueOf(cond));
        if (cond > 49) {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipKira));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        } else if (cond / 10 >= 3) {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipNormal));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        } else if (cond / 10 == 2) {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipFatigue1));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_cond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFleetShipFatigue2));
            holder.ship_cond.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        int ship_lv = item.get("api_lv").getAsInt();
        holder.ship_lv.setText(KcaUtils.format("Lv %d", ship_lv));

        if (ship_lv >= 100) {
            holder.ship_name.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
        } else {
            holder.ship_name.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        }

        if (ship_afterlv != 0 && ship_lv >= ship_afterlv) {
            holder.ship_stat_2_0.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatRemodel));
            holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_stat_2_0.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            if (ship_lv >= 100) {
                holder.ship_lv.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
                holder.ship_exp.setTextColor(ContextCompat.getColor(context, R.color.colorStatMarried));
            } else {

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
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else if (nowhp * 2 <= maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorModerateDmgState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else if (nowhp * 4 <= maxhp * 3) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorLightDmgState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.colorStatNormal));
        } else if (nowhp != maxhp) {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNormalState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.ship_hp.setBackgroundColor(ContextCompat.getColor(context, R.color.colorFullState));
            holder.ship_hp.setTextColor(ContextCompat.getColor(context, R.color.white));
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

        int taisen_value = ship_ts.get(0).getAsInt();
        if (taisen_value >= 100 || (ship_stype == 1 && taisen_value >= 60) ||
                kc_ship_id == 141 || (kc_ship_id == 529 && taisen_value >= 65) ||
                ((kc_ship_id == 380 || kc_ship_id == 526) && taisen_value >= 65 && flag_931)) {
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

        for (int i = 0; i < ship_item_icon.size(); i++) {
            if (i >= holder.ship_equip_icon.length) break;
            int item_id = ship_item_icon.get(i).getAsInt();
            if (item_id == 0) {
                holder.ship_equip_icon[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else {
                holder.ship_equip_icon[i].setImageResource(
                        getId(KcaUtils.format("item_%d", item_id), R.mipmap.class));
                holder.ship_equip_icon[i].setVisibility(View.VISIBLE);
            }
        }

        if (ship_slot_ex == 0) {
            holder.ship_equip_icon_ex.setVisibility(View.INVISIBLE);
        } else {
            holder.ship_equip_icon_ex.setImageResource(
                    getId(KcaUtils.format("item_%d", ship_ex_item_icon), R.mipmap.class));
            holder.ship_equip_icon_ex.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < ship_onslot.size(); i++) {
            if (i >= holder.ship_equip_slot.length || i >= ship_slot.size()) break;
            if (slot_sum == 0) {
                holder.ship_equip_slot[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else if (i >= ship_slot_num) {
                holder.ship_equip_slot[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else {
                holder.ship_equip_slot[i].setText(ship_onslot.get(i).getAsString());
                holder.ship_equip_slot[i].setVisibility(View.VISIBLE);
            }
        }

        if (item.has("api_sally_area")) {
            holder.ship_sally_area.setBackgroundColor(ContextCompat.getColor(context,
                    getId(KcaUtils.format("colorStatSallyArea%d", item.get("api_sally_area").getAsInt()), R.color.class)));
            holder.ship_sally_area.setVisibility(View.VISIBLE);
        } else {
            holder.ship_sally_area.setVisibility(View.GONE);
        }
        return v;
    }

    static class ViewHolder {
        LinearLayout ship_back, ship_stat_2_0;
        TextView ship_id, ship_stype, ship_name, ship_lv, ship_exp, ship_hp, ship_fleet, ship_cond;
        TextView ship_karyoku, ship_raisou, ship_taiku, ship_soukou;
        TextView ship_yasen, ship_taisen, ship_kaihi, ship_sakuteki, ship_luck;
        TextView[] ship_equip_slot;
        ImageView[] ship_equip_icon;
        TextView ship_equip_slot_ex;
        ImageView ship_equip_icon_ex;
        ImageView ship_sally_area;
    }

    public void setListViewItemList(JsonArray ship_list, JsonArray deck_list, String sort_key, String special_equip) {
        setListViewItemList(ship_list, deck_list, sort_key, "|", special_equip);
    }

    public String getKanmusuListText() {
        JsonObject data = KcaApiData.buildShipUpdateData();
        JsonObject frombefore = data.getAsJsonObject("frombefore");
        JsonObject fromafter = data.getAsJsonObject("fromafter");
        JsonObject afterlv = data.getAsJsonObject("afterlv");

        List<String> items = new ArrayList<>();
        Map<String, List<String>> ship_base = new LinkedHashMap<>();
        items.add(".2");
        if (listViewItemList != null) {
            for (JsonObject v: listViewItemList) {
                String lv = v.get("api_lv").getAsString();
                String shipId = v.get("api_ship_id").getAsString();

                String targetId = shipId;
                String baseId = shipId;
                int minlv = 100;
                int k = 0;
                while (fromafter.has(targetId)) {
                    baseId = fromafter.get(targetId).getAsString();
                    String key = KcaUtils.format("%s->%s", baseId, targetId);
                    minlv = afterlv.get(key).getAsInt();
                    targetId = baseId;
                    k += 1;
                }

                targetId = baseId;
                String afterId = baseId;
                int l = 0;
                while (frombefore.has(targetId)) {
                    afterId = frombefore.get(targetId).getAsString();
                    String key = KcaUtils.format("%s->%s", targetId, afterId);
                    if (Integer.parseInt(lv) < afterlv.get(key).getAsInt()) break;
                    targetId = afterId;
                    l += 1;
                }

                String postfix = (k != l) ? "." + String.valueOf(k+1) : "";
                if (!ship_base.containsKey(baseId)) ship_base.put(baseId, new ArrayList<>());
                ship_base.get(baseId).add(lv + postfix);
            }
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> item: ship_base.entrySet()) {
            result.put(item.getKey(), KcaUtils.joinStr(item.getValue(), ","));
        }
        List<Map.Entry<String, String>> list = new ArrayList<>(result.entrySet());
        Collections.sort(list, (o1, o2) -> o1.getKey().equals(o2.getKey()) ? 0 :
                Integer.parseInt(o1.getKey()) < Integer.parseInt(o2.getKey()) ? -1 : 1);
        for (Map.Entry<String, String> item: list) {
            items.add(item.getKey() + ":" + item.getValue());
        }
        return KcaUtils.joinStr(items, "|");
    }

    /*
    *
    * else if (key.equals("api_yasen")) {
            return getintvalue(o, "api_karyoku") + getintvalue(o, "api_raisou");
        } else {
            int kc_ship_id = o.get("api_ship_id").getAsInt();
            JsonObject kcShipData = getKcShipDataById(kc_ship_id, "api_name,api_stype");
            if (kcShipData != null && kcShipData.has(key)) {
                return kcShipData.get(key).getAsInt();
            }
        }
    *
    * */

    private List<JsonObject> addShipInformation(List<JsonObject> data, String sp_eqlist) {
        List<JsonObject> filteredShipInformation = new ArrayList<>();
        JsonObject deck_ship_info = new JsonObject();
        for (int i = 0; i < deckInfo.size(); i++) {
            JsonObject fleet_item = deckInfo.get(i).getAsJsonObject();
            int api_id = fleet_item.get("api_id").getAsInt();
            JsonArray api_ship = fleet_item.getAsJsonArray("api_ship");
            int api_mission = fleet_item.getAsJsonArray("api_mission").get(1).getAsInt();
            for (int j = 0; j < api_ship.size(); j++) {
                int ship_id = api_ship.get(j).getAsInt();
                JsonObject ship_item = new JsonObject();
                if (ship_id > 0) {
                    ship_item.addProperty("deck_id", api_id);
                    ship_item.addProperty("mission", api_mission);
                }
                deck_ship_info.add(String.valueOf(ship_id), ship_item);
            }
        }

        for (int i = 0; i < data.size(); i++) {
            JsonObject item  = data.get(i);
            String ship_id = item.get("api_id").getAsString();
            int kc_ship_id = item.get("api_ship_id").getAsInt();
            item.addProperty("api_yasen",
                    item.getAsJsonArray("api_karyoku").get(0).getAsInt() +
                            item.getAsJsonArray("api_raisou").get(0).getAsInt());
            JsonObject kcShipData = getKcShipDataById(kc_ship_id, "api_name,api_yomi,api_stype,api_sort_id");
            String name = kcShipData != null ? kcShipData.get("api_name").getAsString() : "";
            name = KcaApiData.getShipTranslation(name, false);
            String yomi = kcShipData != null ? kcShipData.get("api_yomi").getAsString() : "";
            int stype = kcShipData != null ? kcShipData.get("api_stype").getAsInt() : 0;

            boolean name_matched = searchStringFromStart(name, searchQuery, false);
            boolean yomi_matched = searchStringFromStart(yomi, searchQuery, false);
            if (!name_matched && !yomi_matched) continue;

            if (sp_eqlist.trim().length() > 0) {
                String[] special_eq_list = sp_eqlist.split(",");
                boolean found_flag = false;
                for (String key: special_eq_list) {
                    if (specialEquipment.has(key)) {
                        JsonObject se_item = specialEquipment.getAsJsonObject(key);
                        boolean match_stype = se_item.getAsJsonArray("stype").contains(new JsonPrimitive(stype));
                        boolean match_special = se_item.getAsJsonArray("special").contains(new JsonPrimitive(kc_ship_id));
                        if (match_stype || match_special) {
                            found_flag = true;
                            break;
                        }
                    }
                }
                if (!found_flag) continue;
            }

            int max_hp = item.get("api_maxhp").getAsInt();
            int now_hp = item.get("api_nowhp").getAsInt();
            item.addProperty("api_exslot", item.get("api_slot_ex").getAsInt() != 0 ? 1 : 0);
            item.addProperty("api_stype", stype);
            item.addProperty("api_docking", KcaDocking.checkShipInDock(Integer.parseInt(ship_id)) ? 1 : 0);
            item.addProperty("api_damage", KcaApiData.getStatus(now_hp * 100 / max_hp));
            item.addProperty("api_damage_value", (max_hp - now_hp) * 100 / max_hp);
            item.addProperty("api_repair", KcaDocking.getDockingTime( max_hp - now_hp, item.get("api_lv").getAsInt(), stype));
            if (deck_ship_info.has(ship_id)) {
                JsonObject ship_deck_data = deck_ship_info.getAsJsonObject(ship_id);
                item.addProperty("api_deck_id", ship_deck_data.get("deck_id").getAsInt());
                item.addProperty("api_mission", ship_deck_data.get("mission").getAsInt());
            } else {
                item.addProperty("api_deck_id", 0);
                item.addProperty("api_mission", 0);
            }
            item.addProperty("api_sort_id", kcShipData != null ? kcShipData.get("api_sort_id").getAsInt() : 0);
            filteredShipInformation.add(item);
        }
        return filteredShipInformation;
    }

    public void setListViewItemList(JsonArray ship_list, JsonArray deck_list, String sort_key, final String filter, String special_equip) {
        exp_sum = 0;
        deckInfo = deck_list;
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
        if (listViewItemList == null) listViewItemList = new ArrayList<>();
        listViewItemList = addShipInformation(listViewItemList, special_equip);
        if (!filter.equals("|") && listViewItemList.size() > 1) {
            listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, input -> {
                String[] filter_list = filter.split("\\|");
                for (String key_op_val: filter_list) {
                    if (key_op_val.length() != 0) {
                        String[] kov_split = key_op_val.split(",");
                        int idx = Integer.valueOf(kov_split[0]);
                        String key = total_key_list[idx];
                        int op = Integer.valueOf(kov_split[1]);

                        String v1 = KcaShipListViewAdpater.getstrvalue(input, key);
                        String v2 = kov_split[2].trim();
                        String[] v2_list = v2.split("_");

                        int v1_int = KcaShipListViewAdpater.getintvalue(input, key);

                        boolean flag = false;
                        switch (op) {
                            case 0:
                                for (String v2_val: v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int == v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 1:
                                for (String v2_val: v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int != v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 2:
                                for (String v2_val: v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int < v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 3:
                                for (String v2_val: v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int > v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 4:
                                for (String v2_val: v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int <= v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            case 5:
                                for (String v2_val: v2_list) {
                                    int v2_int = Integer.valueOf(v2_val);
                                    if (v1_int >= v2_int) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag) return false;
                                break;
                            default:
                                break;
                        }
                    }
                }
                return true;
            }));
        }

        StatComparator cmp = new StatComparator(sort_key);
        Collections.sort(listViewItemList, cmp);
        Log.e("KCA", "list size: " + listViewItemList.size());
        for (int i = 0; i < listViewItemList.size(); i++) {
            exp_sum += listViewItemList.get(i).getAsJsonArray("api_exp").get(0).getAsLong();
        }
    }

    private static int getintvalue(JsonObject o, String key) {
        if (o.has(key)) {
            if (o.get(key).isJsonArray()) {
                return o.getAsJsonArray(key).get(0).getAsInt();
            } else {
                return o.get(key).getAsInt();
            }
        }
        return 0;
    }

    private static String getstrvalue(JsonObject o, String key) {
        if (o.has(key)) {
            if (o.get(key).isJsonPrimitive()) return o.get(key).getAsString();
            else return o.get(key).toString();
        }
        else return "";
    }

    public void resortListViewItem(String sort_key) {
        StatComparator cmp = new StatComparator(sort_key);
        Collections.sort(listViewItemList, cmp);
    }

    private class StatComparator implements Comparator<JsonObject> {
        String sort_key;
        private StatComparator(String key) {
            sort_key = key;
        }

        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String[] sort_key_list = sort_key.split("\\|");
            for (String key_idx: sort_key_list) {
                if (key_idx.length() != 0) {
                    int idx = Integer.valueOf(key_idx.split(",")[0]);
                    boolean is_desc = Boolean.valueOf(key_idx.split(",")[1]);
                    String key = total_key_list[idx];
                    if (key.equals("api_lv")) key = "api_exp";
                    if (key.equals("api_damage")) key = "api_damage_value";
                    int val1 = KcaShipListViewAdpater.getintvalue(o1, key);
                    int val2 = KcaShipListViewAdpater.getintvalue(o2, key);
                    if (key.equals("api_deck_id")) {
                        if (val1 == 0) val1 = is_desc ? -10 : 10;
                        if (val2 == 0) val2 = is_desc ? -10 : 10;
                    }
                    if (val1 != val2) {
                        if (is_desc) return val2 - val1;
                        else return val1 - val2;
                    }
                }
            }
            return 0;
        }
    }
}
