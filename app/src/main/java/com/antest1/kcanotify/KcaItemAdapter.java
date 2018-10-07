package com.antest1.kcanotify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_FLAG;
import static com.antest1.kcanotify.KcaFairySelectActivity.FAIRY_SPECIAL_PREFIX;
import static com.antest1.kcanotify.KcaUtils.getId;

public class KcaItemAdapter extends BaseAdapter {
    public static final int IMAGE_SIZE = 128;
    Context context;
    int layout;
    int rescale = -1;
    List<String> item = new ArrayList<>();
    LayoutInflater inf;
    private int prevactive = -1;

    public KcaItemAdapter(Context context, int layout, List<String> data) {
        this.context = context;
        this.layout = layout;
        this.item = data;
        inf = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setPrevActive(int v) {
        prevactive = v;
    }

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

    public void setRescaleDp(int value) {
        rescale = value;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView==null)
            convertView = inf.inflate(layout, null);
        ImageView iv = (ImageView) convertView.findViewById(R.id.setting_image_pic);
        KcaUtils.setFairyImageFromStorage(context, item.get(position), iv, IMAGE_SIZE);
        if (position == prevactive) iv.setBackground(ContextCompat.getDrawable(context, R.drawable.imagebtn_on));
        else iv.setBackground(ContextCompat.getDrawable(context, R.drawable.imagebtn_off));
        if (rescale > 0) {
            iv.getLayoutParams().width = rescale;
            iv.getLayoutParams().height = rescale;
        }
        return convertView;
    }
}