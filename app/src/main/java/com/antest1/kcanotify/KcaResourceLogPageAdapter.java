package com.antest1.kcanotify;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaResourcelogItemAdpater.resourceData;

public class KcaResourceLogPageAdapter extends FragmentStatePagerAdapter {
    private final static int tabCount = 2;
    public KcaResourceLogPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Log.e("KCA", "getItem "+position);

        KcaResoureLogFragment f = KcaResoureLogFragment.create(resourceData, position);
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

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
