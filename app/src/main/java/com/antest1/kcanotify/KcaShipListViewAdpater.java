package com.antest1.kcanotify;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.antest1.kcanotify.viewmodel.ShipInfoViewModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaUtils.getId;

public class KcaShipListViewAdpater extends BaseAdapter {
    private ShipInfoViewModel viewModel;

    public void setViewModel(ShipInfoViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public static int getSortKeyIndex(int position) {
        return ShipInfoViewModel.getSortKeyIndex(position);
    }

    public static int getSortIndexByKey(int key) {
        return ShipInfoViewModel.getSortIndexByKey(key);
    }

    public static int getFilterKeyIndex(int position) {
        return ShipInfoViewModel.getFilterKeyIndex(position);
    }

    public static int getFilterIndexByKey(int key) {
        return ShipInfoViewModel.getFilterIndexByKey(key);
    }

    public static boolean isList(int idx) {
        return ShipInfoViewModel.isList(idx);
    }

    public static boolean isBoolean(int idx) {
        return ShipInfoViewModel.isBoolean(idx);
    }

    public static boolean isNumeric(int idx) {
        return ShipInfoViewModel.isNumeric(idx);
    }

    @Override
    public int getCount() {
        return viewModel.getItems().size();
    }

    @Override
    public Object getItem(int position) {
        return viewModel.getItems().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public long getTotalExp() {
        return viewModel.getTotalExp();
    }

    public void setSearchQuery(String query) {
        viewModel.setSearchQuery(query);
    }

    public void setSpecialEquipment(JsonObject data) {
        viewModel.setSpecialEquipment(data);
    }

    public void setNationality(JsonObject data) {
        viewModel.setNationality(data);
    }

    public void setListViewItemList(JsonArray ship_list, JsonArray deck_list, String sort_key, String special_equip, String ship_status, String ship_nat) {
        viewModel.setListViewItemList(ship_list, deck_list, sort_key, special_equip, ship_status, ship_nat);
    }

    public void setListViewItemList(JsonArray ship_list, JsonArray deck_list, String sort_key, final String filter, String special_equip, String ship_status, String ship_nat) {
        viewModel.setListViewItemList(ship_list, deck_list, sort_key, filter, special_equip, ship_status, ship_nat);
    }

    public void resortListViewItem(String sort_key) {
        viewModel.resortListViewItem(sort_key);
    }

    public String getSeikuukenSimulatorText() {
        return viewModel.getSeikuukenSimulatorText();
    }

    public String getKanmusuListText() {
        return viewModel.getKanmusuListText();
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

        JsonObject item = viewModel.getItems().get(position);
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
        holder.ship_name.setText(KcaApiData.getShipTranslation(ship_name, kc_ship_id, false));

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
                try {
                    holder.ship_equip_icon[i].setImageResource(
                            getId(KcaUtils.format("item_%d", item_id), R.mipmap.class));
                } catch (Exception e) {
                    holder.ship_equip_icon[i].setImageResource(R.mipmap.item_0);
                }
                holder.ship_equip_icon[i].setVisibility(View.VISIBLE);
            }
        }

        if (ship_slot_ex == 0) {
            holder.ship_equip_icon_ex.setVisibility(View.INVISIBLE);
        } else {
            try {
                holder.ship_equip_icon_ex.setImageResource(
                        getId(KcaUtils.format("item_%d", ship_ex_item_icon), R.mipmap.class));
            } catch (Exception e) {
                holder.ship_equip_icon_ex.setImageResource(R.mipmap.item_0);
            }
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
}
