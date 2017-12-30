package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.w3c.dom.Text;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.name;
import static com.antest1.kcanotify.KcaApiData.getExpeditionInfo;
import static com.antest1.kcanotify.KcaApiData.getShipTypeAbbr;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaExpeditionTableViewAdpater extends BaseAdapter {
    private String locale;
    private int daihatsu_cnt = 0;
    private boolean is_great_success = false;
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private Context application_context, base_context;
    private List<Integer> active = new ArrayList<>();

    public KcaExpeditionTableViewAdpater(Context ac, Context bc, String loc) {
        application_context = ac;
        base_context = bc;
        locale = loc;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(application_context, base_context, id);
    }

    public void setDaihatsuCount(int val) {
        daihatsu_cnt = val;
    }

    public void setGreatSuccess(boolean val) {
        is_great_success = val;
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
            v = inflater.inflate(R.layout.listview_expedtable_item, parent, false);
            ViewHolder holder = new ViewHolder();

            holder.view_abs = v.findViewById(R.id.expedition_item_abstract);
            holder.view_full = v.findViewById(R.id.expedition_item_full);

            holder.label_no_abs = v.findViewById(R.id.label_no_abs);
            holder.label_total_num = v.findViewById(R.id.label_total_num);
            holder.label_flagship_lv = v.findViewById(R.id.label_flagship_lv);

            holder.value_name_abs = v.findViewById(R.id.value_name_abs);
            holder.value_time_abs = v.findViewById(R.id.value_time_abs);
            holder.value_resources_abs = v.findViewById(R.id.value_resources_abs);
            holder.value_spitem1_abs = v.findViewById(R.id.value_spitem1_abs);
            holder.value_spitem2_abs = v.findViewById(R.id.value_spitem2_abs);

            holder.value_no = v.findViewById(R.id.value_no);
            holder.value_name = v.findViewById(R.id.value_name);
            holder.value_time =  v.findViewById(R.id.value_time);
            holder.value_hq_exp = v.findViewById(R.id.value_hq_exp);
            holder.value_ship_exp = v.findViewById(R.id.value_ship_exp);
            holder.value_fuel = v.findViewById(R.id.value_fuel);
            holder.value_ammo = v.findViewById(R.id.value_ammo);
            holder.value_steel = v.findViewById(R.id.value_steel);
            holder.value_bauxite = v.findViewById(R.id.value_bauxite);
            holder.value_spitem1_type = v.findViewById(R.id.value_spitem1_type);
            holder.value_spitem2_type = v.findViewById(R.id.value_spitem2_type);
            holder.value_spitem1_count = v.findViewById(R.id.value_spitem1_count);
            holder.value_spitem2_count = v.findViewById(R.id.value_spitem2_count);
            holder.value_condition = v.findViewById(R.id.value_condition);

            holder.background_labels = new View[7];
            holder.background_labels[0] = holder.label_no_abs;
            holder.background_labels[1] = v.findViewById(R.id.label_no);
            holder.background_labels[2] = v.findViewById(R.id.label_item_abs);
            holder.background_labels[3] = v.findViewById(R.id.label_item1);
            holder.background_labels[4] = v.findViewById(R.id.label_item2);
            holder.background_labels[5] = v.findViewById(R.id.label_condition);
            holder.background_labels[6] = v.findViewById(R.id.item_bottom);

            holder.greatsuccess_values = new View[7];
            holder.greatsuccess_values[0] = holder.value_resources_abs;
            holder.greatsuccess_values[1] = holder.value_fuel;
            holder.greatsuccess_values[2] = holder.value_ammo;
            holder.greatsuccess_values[3] = holder.value_steel;
            holder.greatsuccess_values[4] = holder.value_bauxite;
            holder.greatsuccess_values[5] = holder.value_hq_exp;
            holder.greatsuccess_values[6] = holder.value_ship_exp;
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);
        final ViewHolder holder = (ViewHolder) v.getTag();

        int no = item.get("no").getAsInt();
        int area = item.get("area").getAsInt();
        int area_color = getAreaColor(area);

        final int target = no;

        holder.value_no.setTextColor(ContextCompat.getColor(context, area_color));
        holder.value_name.setTextColor(ContextCompat.getColor(context, area_color));
        holder.value_name_abs.setTextColor(ContextCompat.getColor(context, area_color));

        for (View gsv: holder.greatsuccess_values) {
            if (is_great_success) ((TextView) gsv).setTextColor(ContextCompat.getColor(context, R.color.colorExpeditionGreatSuccess));
            else ((TextView) gsv).setTextColor(ContextCompat.getColor(context, R.color.black));
        }

        for (View bv: holder.background_labels) {
            bv.setBackgroundColor(ContextCompat.getColor(context, area_color));
        }

        String no_str = KcaExpedition2.getExpeditionStr(no);
        holder.value_no.setText(no_str);
        holder.label_no_abs.setText(no_str);
        JsonObject name = item.getAsJsonObject("name");
        String name_locale = "";
        if (name.has(locale)) {
            name_locale = name.get(locale).getAsString();
        } else {
            name_locale = name.get("jp").getAsString();
        }

        holder.value_name_abs.setText(name_locale);
        holder.value_name.setText(name_locale);

        String time_str = KcaUtils.getTimeStr(item.get("time").getAsInt(), true);
        holder.value_time.setText(time_str);
        holder.value_time_abs.setText(time_str);

        holder.label_total_num.setText(item.get("total-num").getAsString());

        if (item.has("flag-lv")) {
            String flagship_lv = item.get("flag-lv").getAsString();
            holder.label_flagship_lv.setText(KcaUtils.format("Lv ".concat(flagship_lv)));
        } else {
            holder.label_flagship_lv.setText("");
        }

        JsonArray exp_info = item.getAsJsonArray("exp");
        int hq_exp = exp_info.get(0).getAsInt();
        int ship_exp = exp_info.get(1).getAsInt();
        holder.value_hq_exp.setText(String.valueOf(hq_exp * (is_great_success ? 2 : 1)));
        holder.value_ship_exp.setText(String.valueOf(ship_exp * (is_great_success ? 2 : 1)));

        JsonArray resource = item.getAsJsonArray("resource");

        int fuel = resource.get(0).getAsInt();
        int ammo = resource.get(1).getAsInt();
        int steel = resource.get(2).getAsInt();
        int bauxite = resource.get(3).getAsInt();

        holder.value_fuel.setText(String.valueOf(calcResourcesBonus(fuel)));
        holder.value_ammo.setText(String.valueOf(calcResourcesBonus(ammo)));
        holder.value_steel.setText(String.valueOf(calcResourcesBonus(steel)));
        holder.value_bauxite.setText(String.valueOf(calcResourcesBonus(bauxite)));
        List<String> resource_str_list = new ArrayList<>();
        for(int i = 0; i < resource.size(); i++) {
            resource_str_list.add(String.valueOf(calcResourcesBonus(resource.get(i).getAsInt())));
        }
        holder.value_resources_abs.setText(joinStr(resource_str_list, "/"));

        JsonArray reward = item.getAsJsonArray("reward");
        JsonArray reward1 = reward.get(0).getAsJsonArray();
        JsonArray reward2 = reward.get(1).getAsJsonArray();

        setRewardData(holder.value_spitem1_type, reward1.get(0).getAsInt(), holder.value_spitem1_count, reward1.get(1).getAsInt());
        setRewardData(holder.value_spitem2_type, reward2.get(0).getAsInt(), holder.value_spitem2_count, reward2.get(1).getAsInt());
        setRewardData(holder.value_spitem1_abs, reward1.get(0).getAsInt(), null, 0);
        setRewardData(holder.value_spitem2_abs, reward2.get(0).getAsInt(), null, 0);

        holder.view_abs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                active.add(target);
                holder.view_full.setVisibility(View.VISIBLE);
            }
        });

        holder.view_full.setVisibility(View.GONE);
        holder.view_full.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                active.remove(Integer.valueOf(target));
                holder.view_abs.setVisibility(View.VISIBLE);
            }
        });

        holder.value_condition.setTag(target);
        v.findViewById(R.id.expedition_item_abstract).setVisibility(active.contains(target) ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.expedition_item_full).setVisibility(active.contains(target) ? View.VISIBLE : View.GONE);

        setConditionContent(v.findViewWithTag(target), getExpeditionInfo(target, locale));
        return v;
    }

    private void setRewardData(ImageView type_view, int type, TextView count_view, int count) {
        type_view.setVisibility(type > 0 ? View.VISIBLE : View.INVISIBLE);
        switch(type) {
            case 1:
                type_view.setImageResource(R.mipmap.icon_instant_repair);
                break;
            case 2:
                type_view.setImageResource(R.mipmap.icon_instant_construction);
                break;
            case 3:
                type_view.setImageResource(R.mipmap.icon_development_material);
                break;
            case 10:
                type_view.setImageResource(R.mipmap.icon_furniture_box_small);
                break;
            case 11:
                type_view.setImageResource(R.mipmap.icon_furniture_box_medium);
                break;
            case 12:
                type_view.setImageResource(R.mipmap.icon_furniture_box_large);
                break;
            default:
                type_view.setImageResource(R.color.transparent);
                break;
        }

        if (count_view != null) {
            count_view.setText(KcaUtils.format("x%d", count));
            count_view.setVisibility(type > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setConditionContent(View root_view, JsonObject data) {
        int total_num = data.get("total-num").getAsInt();
        boolean has_flag_lv = data.has("flag-lv");
        boolean has_flag_cond = data.has("flag-cond");
        boolean has_flag_info = has_flag_lv || has_flag_cond;
        boolean has_total_lv = data.has("total-lv");
        boolean has_total_cond = data.has("total-cond");
        boolean has_drum_ship = data.has("drum-ship");
        boolean has_drum_num = data.has("drum-num");
        boolean has_drum_num_optional = data.has("drum-num-optional");
        boolean has_drum_info = has_drum_ship || has_drum_num || has_drum_num_optional;
        boolean has_total_asw = data.has("total-asw");
        boolean has_total_fp = data.has("total-fp");
        boolean has_total_los = data.has("total-los");
        boolean has_total_firepower = data.has("total-firepower");

        ((TextView) root_view.findViewById(R.id.view_excheck_fleet_total_num))
                .setText(KcaUtils.format(getStringWithLocale(R.string.excheckview_total_num_format), total_num));

        setItemViewVisibilityById(root_view, R.id.view_excheck_flagship, has_flag_info);
        if (has_flag_info) {
            setItemViewVisibilityById(root_view, R.id.view_excheck_flagship_lv, has_flag_lv);
            if (has_flag_lv) {
                int flag_lv = data.get("flag-lv").getAsInt();
                setItemTextViewById(root_view, R.id.view_excheck_flagship_lv,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_flag_lv_format), flag_lv));
            }
            setItemViewVisibilityById(root_view, R.id.view_excheck_flagship_cond, has_flag_cond);
            if (has_flag_cond) {
                int flag_cond = data.get("flag-cond").getAsInt();
                setItemTextViewById(root_view, R.id.view_excheck_flagship_cond,
                        getShipTypeAbbr(flag_cond));
            }
        }

        setItemViewVisibilityById(root_view, R.id.view_excheck_fleet_total_lv, has_total_lv);
        if (has_total_lv) {
            int total_lv = data.get("total-lv").getAsInt();
            setItemTextViewById(root_view, R.id.view_excheck_fleet_total_lv,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_lv_format), total_lv));
        }

        ((LinearLayout) root_view.findViewById(R.id.view_excheck_fleet_condition)).removeAllViews();
        setItemViewVisibilityById(root_view, R.id.view_excheck_fleet_condition, has_total_cond);
        if (has_total_cond) {
            String total_cond = data.get("total-cond").getAsString();
            for (View v : generateConditionView(total_cond)) {
                ((LinearLayout) root_view.findViewById(R.id.view_excheck_fleet_condition)).addView(v);
            }
        }

        setItemViewVisibilityById(root_view, R.id.view_excheck_drum, has_drum_info);
        if (has_drum_info) {
            setItemViewVisibilityById(root_view, R.id.view_excheck_drum_ship, has_drum_ship);
            if (has_drum_ship) {
                int drum_ship = data.get("drum-ship").getAsInt();
                setItemTextViewById(root_view, R.id.view_excheck_drum_ship,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_drum_ship_format), drum_ship));
            }
            setItemViewVisibilityById(root_view, R.id.view_excheck_drum_count, has_drum_num || has_drum_num_optional);
            if (has_drum_num) {
                int drum_num = data.get("drum-num").getAsInt();
                setItemTextViewById(root_view, R.id.view_excheck_drum_count,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_drum_num_format), drum_num));
            } else if (has_drum_num_optional) {
                int drum_num = data.get("drum-num-optional").getAsInt();
                setItemTextViewById(root_view, R.id.view_excheck_drum_count,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_drum_num_format), drum_num));
            }
        }

        setItemViewVisibilityById(root_view, R.id.view_excheck_asw, has_total_asw);
        if (has_total_asw) {
            int total_asw = data.get("total-asw").getAsInt();
            setItemTextViewById(root_view, R.id.view_excheck_total_asw,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_asw));
        }

        setItemViewVisibilityById(root_view, R.id.view_excheck_fp, has_total_asw);
        if (has_total_fp) {
            int total_fp = data.get("total-fp").getAsInt();
            setItemTextViewById(root_view, R.id.view_excheck_total_fp,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_fp));
        }

        setItemViewVisibilityById(root_view, R.id.view_excheck_los, has_total_los);
        if (has_total_los) {
            int total_los = data.get("total-los").getAsInt();
            setItemTextViewById(root_view, R.id.view_excheck_total_los,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_los));
        }

        setItemViewVisibilityById(root_view, R.id.view_excheck_firepower, has_total_firepower);
        if (has_total_firepower) {
            int total_firepower = data.get("total-firepower").getAsInt();
            setItemTextViewById(root_view, R.id.view_excheck_total_firepower,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_firepower));
        }
    }

    private void setItemTextViewById(View root_view, int id, String value) {
        ((TextView) root_view.findViewById(id)).setText(value);
    }

    private void setItemViewVisibilityById(View root_view, int id, boolean visible) {
        int visible_value = visible ? View.VISIBLE : View.GONE;
        root_view.findViewById(id).setVisibility(visible_value);
    }

    private List<View> generateConditionView(String data) {
        List<View> views = new ArrayList<>();
        String[] conds = data.split("/");
        for (String cond : conds) {
            List<String> cond_value = new ArrayList<>();
            TextView cond_tv = new TextView(application_context);
            cond_tv.setTextColor(ContextCompat.getColor(application_context, R.color.black));
            String[] shipcond = cond.split("\\|");
            for (String sc : shipcond) {
                cond_value.add(convertTotalCond(sc));
            }
            cond_tv.setText("- ".concat(KcaUtils.joinStr(cond_value, " ")));
            views.add(cond_tv);
        }
        return views;
    }

    private String convertTotalCond(String str) {
        String[] ship_count = str.split("\\-");
        String[] ship = ship_count[0].split(",");
        List<String> ship_list = new ArrayList<>();
        for (String s : ship) {
            ship_list.add(getShipTypeAbbr(Integer.parseInt(s)));
        }
        String ship_concat = joinStr(ship_list, "/");
        return ship_concat.concat(":").concat(ship_count[1]);
    }

    private static class ViewHolder {
        LinearLayout view_abs, view_full;

        TextView label_no_abs, value_name_abs, value_time_abs, value_resources_abs;
        TextView label_total_num, label_flagship_lv;
        ImageView value_spitem1_abs, value_spitem2_abs;

        TextView value_no, value_name, value_time, value_hq_exp, value_ship_exp;
        TextView value_fuel, value_ammo, value_steel, value_bauxite;
        ImageView value_spitem1_type, value_spitem2_type;
        TextView value_spitem1_count, value_spitem2_count;
        View value_condition;

        View[] background_labels;
        View[] greatsuccess_values;
    }

    public void setListViewItemList(JsonArray ship_list, final int area_no) {
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
        listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, new Predicate<JsonObject>() {
            @Override
            public boolean apply(JsonObject input) {
                return (area_no == 0 || input.get("area").getAsInt() == area_no);
            }
        }));
    }

    public int getAreaColor(int area) {
        if (area < 1 || area > 5) area = 99;
        return KcaUtils.getId(KcaUtils.format("colorExpeditionTable%d", area), R.color.class);
    }

    public int calcResourcesBonus(int val) {
        double mult;
        if (is_great_success) {
            return (int) ((val * (1.0 + (0.05 * daihatsu_cnt)) * 3.0) / 2);
        } else {
            return (int) (val * (1.0 + (0.05 * daihatsu_cnt)));
        }
    }
}
