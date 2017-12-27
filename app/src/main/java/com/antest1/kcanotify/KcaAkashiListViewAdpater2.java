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

import java.util.ArrayList;

public class KcaAkashiListViewAdpater2 extends BaseAdapter {
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
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_akashi_equip_item2, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.itemView = (LinearLayout) v.findViewById(R.id.akashi_improv2_item_area);
            holder.iconView = (ImageView) v.findViewById(R.id.akashi_improv2_icon);
            holder.nameView = (TextView) v.findViewById(R.id.akashi_improv2_name);
            holder.materialView = (TextView) v.findViewById(R.id.akashi_improv2_materials);
            holder.screwView = (TextView) v.findViewById(R.id.akashi_improv2_screws);
            holder.supportView = (TextView) v.findViewById(R.id.akashi_improv2_support);
            v.setTag(holder);
        }

        KcaAkashiListViewItem item = listViewItemList.get(position);
        final int itemId = item.getEquipId();
        final String itemImprovementData = item.getEquipImprovementData().toString();

        ViewHolder holder = (ViewHolder) v.getTag();
        holder.iconView.setImageResource(item.getEquipIconMipmap());
        holder.nameView.setText(item.getEquipName());
        holder.screwView.setText(item.getEquipScrews());
        holder.materialView.setText(item.getEquipMaterials());
        holder.supportView.setText(item.getEquipSupport());
        holder.screwView.setTextColor(ContextCompat.getColor(context, getScrewTextColor(isSafeChecked)));
        holder.materialView.setTextColor(ContextCompat.getColor(context, getScrewTextColor(isSafeChecked)));
        return v;
    }

    static class ViewHolder {
        LinearLayout itemView;
        ImageView iconView;
        TextView nameView;
        TextView screwView;
        TextView materialView;
        TextView supportView;
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
}
