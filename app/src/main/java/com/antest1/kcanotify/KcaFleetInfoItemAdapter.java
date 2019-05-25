package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaUtils.getId;

public class KcaFleetInfoItemAdapter extends BaseAdapter {
    public static String[] alv_format = {"", "", "", "", "", "", "", ""};
    JsonArray item = new JsonArray();
    public static boolean is_combined = false;

    @Override
    public int getCount() {
        return item.size();
    }

    @Override
    public Object getItem(int position) {
        return item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_fleetlist_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.ship_name = v.findViewById(R.id.fship_name);
            holder.ship_karyoku = v.findViewById(R.id.fship_karyoku);
            holder.ship_raisou = v.findViewById(R.id.fship_raisou);
            holder.ship_taiku = v.findViewById(R.id.fship_taiku);
            holder.ship_soukou = v.findViewById(R.id.fship_soukou);
            holder.ship_stype = v.findViewById(R.id.fship_stype);
            holder.ship_lv = v.findViewById(R.id.fship_lv);
            holder.ship_maxhp = v.findViewById(R.id.fship_maxhp);
            holder.ship_speed = v.findViewById(R.id.fship_speed);
            holder.ship_cb_indicator = v.findViewById(R.id.fship_cb_indicator);
            holder.ship_luck = v.findViewById(R.id.fship_luck);
            holder.ship_taisen = v.findViewById(R.id.fship_taisen);
            holder.ship_layout = v.findViewById(R.id.fship_layout);
            holder.ship_equip_holder = new View[5];
            holder.ship_equip_slot = new TextView[5];
            holder.ship_equip_name = new TextView[5];
            holder.ship_equip_lv = new TextView[5];
            holder.ship_equip_alv = new TextView[5];
            holder.ship_equip_icon = new ImageView[5];

            for (int i = 0; i < 5; i++) {
                holder.ship_equip_holder[i] = v.findViewById(getId(KcaUtils.format("fship_equip_%d", i + 1), R.id.class));
                holder.ship_equip_slot[i] = v.findViewById(getId(KcaUtils.format("fship_equip_%d_slot", i + 1), R.id.class));
                holder.ship_equip_name[i] = v.findViewById(getId(KcaUtils.format("fship_equip_%d_name", i + 1), R.id.class));
                holder.ship_equip_icon[i] = v.findViewById(getId(KcaUtils.format("fship_equip_%d_icon", i + 1), R.id.class));
                holder.ship_equip_lv[i] = v.findViewById(getId(KcaUtils.format("fship_equip_%d_lv", i + 1), R.id.class));
                holder.ship_equip_alv[i] = v.findViewById(getId(KcaUtils.format("fship_equip_%d_alv", i + 1), R.id.class));

            }
            holder.ship_equip_ex_area = v.findViewById(R.id.fship_equip_ex_area);
            holder.ship_equip_slot_ex = v.findViewById(R.id.fship_equip_ex_slot);
            holder.ship_equip_icon_ex = v.findViewById(R.id.fship_equip_ex_icon);
            holder.ship_equip_name_ex = v.findViewById(R.id.fship_equip_ex_name);
            holder.ship_equip_lv_ex = v.findViewById(R.id.fship_equip_ex_lv);
            v.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) v.getTag();
        JsonObject data = item.get(position).getAsJsonObject();
        if (!data.has("kc") || data.get("kc") == null) {
            holder.ship_layout.setVisibility(View.INVISIBLE);
            return v;
        }
        holder.ship_layout.setVisibility(View.VISIBLE);
        if (data == null || data.isJsonNull()) return v;
        JsonObject kcdata = data.getAsJsonObject("kc");
        JsonObject userdata = data.getAsJsonObject("user");

        holder.ship_name.setText(KcaApiData.getShipTranslation(kcdata.get("name").getAsString(), false));
        holder.ship_stype.setText(KcaApiData.getShipTypeAbbr(kcdata.get("stype").getAsInt()));
        holder.ship_lv.setText("Lv ".concat(userdata.get("api_lv").getAsString()));
        holder.ship_maxhp.setText("HP ".concat(userdata.get("api_maxhp").getAsString()));
        holder.ship_speed.setText(KcaApiData.getSpeedString(context, userdata.get("api_soku").getAsInt()));

        if (is_combined) {
            holder.ship_cb_indicator.setVisibility(View.VISIBLE);
            if (data.has("cb_flag")) {
                holder.ship_cb_indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatSallyArea2));
            } else {
                holder.ship_cb_indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.colorStatSallyArea1));
            }
        } else {
            holder.ship_cb_indicator.setVisibility(View.GONE);
        }

        int ship_stype = kcdata.get("stype").getAsInt();
        int ship_init_lk = kcdata.getAsJsonArray("luck").get(0).getAsInt();
        int ship_slot_num = kcdata.get("slot_num").getAsInt();

        JsonArray ship_slot = userdata.getAsJsonArray("api_slot");
        JsonArray ship_onslot = userdata.getAsJsonArray("api_onslot");
        int ship_slot_ex = userdata.get("api_slot_ex").getAsInt();
        String ship_ex_item_name = "";
        int ship_ex_item_icon = 0;
        int ship_ex_item_lv = 0;

        int slot_sum = 0;
        boolean flag_931 = false;
        JsonArray ship_item = new JsonArray();

        for (int j = 0; j < ship_slot.size(); j++) {
            int item_id = ship_slot.get(j).getAsInt();
            if (item_id > 0) {
                JsonObject itemData = getUserItemStatusById(item_id, "level,alv", "id,type,name");
                if (itemData != null) {
                    int item_kc_id = itemData.get("id").getAsInt();
                    if (item_kc_id == 82 || item_kc_id == 83) flag_931 = true;
                    int item_type = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                    itemData.addProperty("icon", item_type);
                    itemData.addProperty("name", KcaApiData.getItemTranslation(itemData.get("name").getAsString()));
                    ship_item.add(itemData);
                }
            } else {
                ship_item.add(new JsonObject());
            }
        }
        if (ship_slot_ex > 0) {
            JsonObject ex_item_data = getUserItemStatusById(ship_slot_ex, "level,alv", "name,type");
            if (ex_item_data != null) {
                ship_ex_item_name = KcaApiData.getItemTranslation(ex_item_data.get("name").getAsString());
                ship_ex_item_icon = ex_item_data.get("type").getAsJsonArray().get(3).getAsInt();
                ship_ex_item_lv = ex_item_data.get("level").getAsInt();
            }
        }

        for (int j = 0; j < ship_onslot.size(); j++) {
            slot_sum += ship_onslot.get(j).getAsInt();
        }


        int kc_ship_id = userdata.get("api_ship_id").getAsInt();

        JsonArray ship_kyouka = userdata.getAsJsonArray("api_kyouka");

        JsonArray ship_ka = userdata.getAsJsonArray("api_karyoku");
        JsonArray ship_ra = userdata.getAsJsonArray("api_raisou");
        JsonArray ship_ta = userdata.getAsJsonArray("api_taiku");
        JsonArray ship_so = userdata.getAsJsonArray("api_soukou");
        JsonArray ship_ts = userdata.getAsJsonArray("api_taisen");
        JsonArray ship_kh = userdata.getAsJsonArray("api_kaihi");
        JsonArray ship_st = userdata.getAsJsonArray("api_sakuteki");
        JsonArray ship_lk = userdata.getAsJsonArray("api_lucky");

        holder.ship_karyoku.setText(ship_ka.get(0).getAsString());
        holder.ship_raisou.setText(ship_ra.get(0).getAsString());
        holder.ship_taiku.setText(ship_ta.get(0).getAsString());
        holder.ship_soukou.setText(ship_so.get(0).getAsString());

        int taisen_value = ship_ts.get(0).getAsInt();
        if (taisen_value >= 100 || (ship_stype == 1 && taisen_value >= 60) ||
                kc_ship_id == 141 || ((kc_ship_id == 529 || kc_ship_id == 536) && taisen_value >= 65) ||
                ((kc_ship_id == 380 || kc_ship_id == 526 || kc_ship_id == 381 || kc_ship_id == 534) && taisen_value >= 65 && flag_931)) {
            holder.ship_taisen.setTextColor(ContextCompat.getColor(context, R.color.colorStatTaisen));
        } else {
            holder.ship_taisen.setTextColor(ContextCompat.getColor(context, R.color.grey));
        }
        holder.ship_taisen.setText(ship_ts.get(0).getAsString());

        if (ship_init_lk + ship_kyouka.get(4).getAsInt() >= 50) {
            holder.ship_luck.setTextColor(ContextCompat.getColor(context, R.color.colorStatLuck));
        } else {
            holder.ship_luck.setTextColor(ContextCompat.getColor(context, R.color.grey));
        }
        holder.ship_luck.setText(ship_lk.get(0).getAsString());

        for (int i = 0; i < ship_onslot.size(); i++) {
            if (i >= holder.ship_equip_slot.length || i >= ship_slot.size()) break;
            if (slot_sum == 0) {
                holder.ship_equip_slot[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else if (i >= ship_slot_num) {
                holder.ship_equip_slot[i].setVisibility(i == 4 ? View.GONE : View.INVISIBLE);
            } else {
                holder.ship_equip_slot[i].setText(KcaApiData.getItemTranslation(ship_onslot.get(i).getAsString()));
                holder.ship_equip_slot[i].setVisibility(View.VISIBLE);
            }
        }

        for (int i = 0; i < 4; i++) holder.ship_equip_holder[i].setVisibility(View.INVISIBLE);
        holder.ship_equip_holder[4].setVisibility(View.GONE);

        for (int i = 0; i < ship_item.size(); i++) {
            if (i >= holder.ship_equip_icon.length) break;
            JsonObject item_data = ship_item.get(i).getAsJsonObject();
            if (item_data.has("icon")) {
                int item_icon = item_data.get("icon").getAsInt();
                String item_name = item_data.get("name").getAsString();
                holder.ship_equip_name[i].setText(item_name);
                holder.ship_equip_icon[i].setImageResource(
                        getId(KcaUtils.format("item_%d", item_icon), R.mipmap.class));
                holder.ship_equip_icon[i].setVisibility(View.VISIBLE);

                if (item_data.has("level")) {
                    int lv = item_data.get("level").getAsInt();
                    if (lv > 0) {
                        holder.ship_equip_lv[i].setText(KcaUtils.format("★%d", lv));
                    } else {
                        holder.ship_equip_lv[i].setText("");
                    }
                } else {
                    holder.ship_equip_lv[i].setText("");
                }

                if (item_data.has("alv")) {
                    int alv = item_data.get("alv").getAsInt();
                    if (alv > 0) {
                        int alvColorId = (alv <= 3) ? 1 : 2;
                        holder.ship_equip_alv[i].setTextColor(ContextCompat.getColor(context, getId(KcaUtils.format("itemalv%d", alvColorId), R.color.class)));
                        holder.ship_equip_alv[i].setText(KcaUtils.format(alv_format[alv], alv));
                    } else {
                        holder.ship_equip_alv[i].setText("");
                    }
                } else {
                    holder.ship_equip_alv[i].setText("");
                }
                holder.ship_equip_holder[i].setVisibility(View.VISIBLE);
            }
        }

        if (ship_slot_ex == 0) {
            holder.ship_equip_ex_area.setVisibility(View.INVISIBLE);
        } else {
            holder.ship_equip_name_ex.setText(ship_ex_item_name);
            holder.ship_equip_icon_ex.setImageResource(
                    getId(KcaUtils.format("item_%d", ship_ex_item_icon), R.mipmap.class));
            holder.ship_equip_ex_area.setVisibility(View.VISIBLE);

            if (ship_ex_item_lv > 0) {
                holder.ship_equip_lv_ex.setText(KcaUtils.format("★%d", ship_ex_item_lv));
            } else {
                holder.ship_equip_lv_ex.setText("");
            }
        }

        return v;
    }

    static class ViewHolder {
        TextView ship_stype, ship_name, ship_lv, ship_maxhp, ship_speed;
        TextView ship_karyoku, ship_raisou, ship_taiku, ship_soukou;
        TextView ship_taisen, ship_luck;
        ImageView ship_cb_indicator;
        View[] ship_equip_holder;
        TextView[] ship_equip_slot, ship_equip_name, ship_equip_lv, ship_equip_alv;
        ImageView[] ship_equip_icon;
        TextView ship_equip_slot_ex, ship_equip_name_ex, ship_equip_lv_ex;
        ImageView ship_equip_icon_ex;
        View ship_layout, ship_equip_ex_area;
    }

    public void setListViewItemList(JsonArray ship_list) {
        item = ship_list;
    }
}