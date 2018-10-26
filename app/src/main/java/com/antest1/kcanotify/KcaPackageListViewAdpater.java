package com.antest1.kcanotify;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.PREF_PACKAGE_ALLOW;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaPackageListViewAdpater extends BaseAdapter {
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private Handler handler;

    public void setHandler(Handler h) {
        handler = h;
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
        PackageManager pm = context.getPackageManager();
        JsonArray list = new JsonParser().parse(getStringPreferences(context, PREF_PACKAGE_ALLOW)).getAsJsonArray();

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_package_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.itemView = v.findViewById(R.id.app_item);
            holder.iconView = v.findViewById(R.id.app_icon);
            holder.nameView = v.findViewById(R.id.app_name);
            holder.packageView = v.findViewById(R.id.app_package);
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);
        String name = item.get("name").getAsString();
        String pkgname = item.get("package").getAsString();
        ViewHolder holder = (ViewHolder) v.getTag();
        try {
            holder.iconView.setImageDrawable(pm.getApplicationIcon(pkgname));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.nameView.setText(name); // pm.getApplicationLabel(item.applicationInfo));
        holder.nameView.setTextColor(ContextCompat.getColor(context,
                getCheckedTextColor(list.contains(new JsonPrimitive(pkgname)))));
        holder.packageView.setText(pkgname);

        final String package_name = pkgname;
        holder.itemView.setOnClickListener(v1 -> {
            JsonArray data = new JsonParser().parse(getStringPreferences(context, PREF_PACKAGE_ALLOW)).getAsJsonArray();
            JsonPrimitive pkg = new JsonPrimitive(package_name);
            if (data.contains(pkg)) {
                data.remove(pkg);
                holder.nameView.setTextColor(ContextCompat.getColor(context,getCheckedTextColor(false)));
            }
            else {
                data.add(pkg);
                holder.nameView.setTextColor(ContextCompat.getColor(context,getCheckedTextColor(true)));
            }
            setPreferences(context, PREF_PACKAGE_ALLOW, data.toString());
            if (handler != null) {
                handler.sendEmptyMessage(0);
            }
        });
        return v;
    }

    static class ViewHolder {
        View itemView;
        ImageView iconView;
        TextView nameView, packageView;
    }

    public void setList(PackageManager pm) {
        List<PackageInfo> data = pm.getInstalledPackages(0);
        listViewItemList = new ArrayList<>();
        for (PackageInfo p: data) {
            JsonObject item = new JsonObject();
            item.addProperty("name", pm.getApplicationLabel(p.applicationInfo).toString());
            item.addProperty("package", p.packageName);
            listViewItemList.add(item);
        }
        PackageNameCompare cmp = new PackageNameCompare(pm);
        Collections.sort(listViewItemList, cmp);
    }

    private int getCheckedTextColor(boolean checked) {
        if (checked) return R.color.colorAccent;
        else return R.color.black;
    }

    private class PackageNameCompare implements Comparator<JsonObject> {
        PackageManager pm;
        private PackageNameCompare(PackageManager pm) {
            this.pm = pm;
        }
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String p1 = o1.get("name").getAsString();
            String p2 = o2.get("name").getAsString();
            return p1.compareTo(p2);
        }
    }
}
