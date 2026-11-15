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
        return (long) KcaResoureLogFragment.getStateId() + position;
    }

    @Override
    public boolean containsItem(long itemId) {
        // must accept every id returned by getItemId, or the adapter drops the bound fragment
        long position = itemId - KcaResoureLogFragment.getStateId();
        return position >= 0 && position < tabCount;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
