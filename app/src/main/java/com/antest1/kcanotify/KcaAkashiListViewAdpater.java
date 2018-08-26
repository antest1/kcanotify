package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaAkashiListViewAdpater extends BaseAdapter {
    private boolean isSafeChecked = false;
    private static Handler sHandler;
    private ArrayList<KcaAkashiListViewItem> listViewItemList = new ArrayList<KcaAkashiListViewItem>();

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
        // format: |1|23|55|260|
        String starlistData = getStringPreferences(context, PREF_AKASHI_STARLIST);
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_akashi_equip_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.itemView = (LinearLayout) v.findViewById(R.id.akashi_improv_item_area);
            holder.iconView = (ImageView) v.findViewById(R.id.akashi_improv_icon);
            holder.nameView = (TextView) v.findViewById(R.id.akashi_improv_name);
            holder.materialView = (TextView) v.findViewById(R.id.akashi_improv_material);
            holder.screwView = (TextView) v.findViewById(R.id.akashi_improv_screws);
            holder.supportView = (TextView) v.findViewById(R.id.akashi_improv_support);
            holder.starView = (TextView) v.findViewById(R.id.akashi_improv_star);
            v.setTag(holder);
        }

        KcaAkashiListViewItem item = listViewItemList.get(position);
        final int itemId = item.getEquipId();
        final String itemImprovementData = item.getEquipImprovementData().toString();

        ViewHolder holder = (ViewHolder) v.getTag();
        holder.iconView.setImageResource(item.getEquipIconMipmap());
        holder.nameView.setText(item.getEquipName());
        holder.materialView.setText(item.getEquipMaterials());
        holder.screwView.setText(item.getEquipScrews());
        holder.supportView.setText(item.getEquipSupport());
        holder.materialView.setTextColor(ContextCompat.getColor(context, getMaterialTextColor(isSafeChecked)));
        holder.screwView.setTextColor(ContextCompat.getColor(context, getScrewTextColor(isSafeChecked)));
        if (checkStarred(starlistData, itemId)) {
            holder.starView.setText(context.getString(R.string.aa_btn_star1));
        } else {
            holder.starView.setText(context.getString(R.string.aa_btn_star0));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AkashiDetailActivity.class);
                intent.putExtra("item_id", itemId);
                intent.putExtra("item_info", itemImprovementData);
                context.startActivity(intent);
            }
        });

        holder.starView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String starred = getStringPreferences(context, PREF_AKASHI_STARLIST);
                TextView tv = (TextView) v;
                if (tv.getText().equals(context.getString(R.string.aa_btn_star0))) {
                    setPreferences(context, PREF_AKASHI_STARLIST,
                            addStarred(starred, itemId));
                    tv.setText(context.getString(R.string.aa_btn_star1));
                } else {
                    setPreferences(context, PREF_AKASHI_STARLIST,
                            deleteStarred(starred, itemId));
                    tv.setText(context.getString(R.string.aa_btn_star0));
                }
                Log.e("KCA", starred);
                sHandler.sendEmptyMessage(0);
            }
        });

        return v;
    }

    static class ViewHolder {
        LinearLayout itemView;
        ImageView iconView;
        TextView nameView;
        TextView materialView;
        TextView screwView;
        TextView supportView;
        TextView starView;
    }

    public void setListViewItemList(ArrayList<KcaAkashiListViewItem> list) {
        listViewItemList = list;
    }

    public void setSafeCheckedStatus(boolean checked) {
        isSafeChecked = checked;
    }

    private int getScrewTextColor(boolean checked) {
        if (checked) return R.color.colorAkashiGtdScrew;
        else return R.color.colorAkashiNormalScrew;
    }

    private int getMaterialTextColor(boolean checked) {
        if (checked) return R.color.colorAkashiGtdMaterial;
        else return R.color.colorAkashiNormalMaterial;
    }

    private boolean checkStarred(String data, int id) {
        return data.contains(KcaUtils.format("|%d|", id));
    }

    private String addStarred(String data, int id) {
        return data.concat(String.valueOf(id)).concat("|");
    }

    private String deleteStarred(String data, int id) {
        return data.replace(KcaUtils.format("|%d|", id), "|");
    }
}
