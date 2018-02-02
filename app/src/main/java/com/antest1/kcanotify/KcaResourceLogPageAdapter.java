package com.antest1.kcanotify;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonObject;

public class KcaResourceLogPageAdapter extends FragmentStatePagerAdapter {
    private final static int tabCount = 2;
    JsonObject data = new JsonObject();
    public KcaResourceLogPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Log.e("KCA", "getItem "+position);
        JsonObject data = new JsonObject();
        data.addProperty("0", "resource_data");
        data.addProperty("1", "consumable_data");

        KcaResoureLogFragment f = KcaResoureLogFragment.create(data, position);
        switch(position) {
            case 0:
                return f;
            case 1:
                return f;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return tabCount;
    }

    public void setData(JsonObject d) {
        data = d;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
