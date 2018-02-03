package com.antest1.kcanotify;

import android.support.v4.util.LogWriter;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.R.id.list;
import static com.antest1.kcanotify.KcaFleetViewService.fleetview_menu_keys;

public class KcaFleetViewMenuOrderAdpater extends DragItemAdapter<JsonObject, KcaFleetViewMenuOrderAdpater.ViewHolder> {
    private int mLayoutId, mGrabHandleId;
    private boolean mDragOnLongPress;

    KcaFleetViewMenuOrderAdpater(ArrayList<JsonObject> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setItemList(list);
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).get("key").getAsLong();
    }

    public String getUniqueItemValue(int position) {
        return mItemList.get(position).get("value").getAsString();
    }

    public int getUniqueItemOrder(int position) {
        String value = mItemList.get(position).get("value").getAsString();
        for (int i = 0; i < fleetview_menu_keys.length; i++) {
            if (fleetview_menu_keys[i].equals(value)) return i;
        }
        return -1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        String text = mItemList.get(position).get("label").getAsString();
        holder.mText.setText(text);
        holder.itemView.setTag(mItemList.get(position));
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        TextView mText;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            mText = itemView.findViewById(R.id.setting_mbtn_text);
        }

        @Override
        public void onItemClicked(View view) {

        }

        @Override
        public boolean onItemLongClicked(View view) {
            return true;
        }
    }
}
