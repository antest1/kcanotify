package com.antest1.kcanotify;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import static com.antest1.kcanotify.KcaResourcelogItemAdpater.resourceData;

public class KcaResourceLogPageAdapter extends FragmentStateAdapter {
    private final static int tabCount = 2;
    public KcaResourceLogPageAdapter(FragmentManager fragmentManager, Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position < tabCount) {
            return KcaResoureLogFragment.create(resourceData, position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return KcaResoureLogFragment.getStateId() + position;
    }

    @Override
    public boolean containsItem(long itemId) {
        return KcaResoureLogFragment.getStateId() == itemId;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
