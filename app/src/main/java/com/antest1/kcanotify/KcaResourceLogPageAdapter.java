package com.antest1.kcanotify;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.Log;

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
