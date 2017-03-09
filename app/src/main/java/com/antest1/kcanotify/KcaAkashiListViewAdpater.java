package com.antest1.kcanotify;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;

public class KcaAkashiListViewAdpater extends BaseAdapter {
    private boolean isSafeChecked = false;
    private int screwColor;
    private ArrayList<KcaAkashiListViewItem> listViewItemList = new ArrayList<KcaAkashiListViewItem>();

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

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_equip_item, parent, false);

            ImageView iconView = (ImageView) convertView.findViewById(R.id.akashi_improv_icon);
            TextView nameView = (TextView) convertView.findViewById(R.id.akashi_improv_name);
            TextView screwView = (TextView) convertView.findViewById(R.id.akashi_improv_screws);
            TextView supportView = (TextView) convertView.findViewById(R.id.akashi_improv_support);
            TextView starView = (TextView) convertView.findViewById(R.id.akashi_improv_star);

            KcaAkashiListViewItem item = listViewItemList.get(position);
            iconView.setImageResource(item.getEquipIconMipmap());
            nameView.setText(item.getEquipName());
            screwView.setText(item.getEquipScrews());
            supportView.setText(item.getEquipSupport());
            screwView.setTextColor(ContextCompat.getColor(context, getScrewTextColor(isSafeChecked)));

            starView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Save Equipment start data
                    TextView tv = (TextView) v;
                    if(tv.getText().equals("☆")) {
                        tv.setText("★");
                    } else {
                        tv.setText("☆");
                    }
                }
            });

            return convertView;
        }

        return null;
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
