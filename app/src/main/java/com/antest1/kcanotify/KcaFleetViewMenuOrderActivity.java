package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;

import static com.antest1.kcanotify.KcaConstants.PREF_FV_MENU_ORDER;
import static com.antest1.kcanotify.KcaFleetViewService.fleetview_menu_keys;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaFleetViewMenuOrderActivity extends AppCompatActivity {
    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();
    DragListView listview;
    KcaFleetViewMenuOrderAdpater adapter;
    JsonArray order_data = new JsonArray();

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public KcaFleetViewMenuOrderActivity() {
        LocaleUtils.updateConfig(this);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_mbtn_order);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.setting_menu_kand_title_fleetview_button_order));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayList<JsonObject> data = new ArrayList<>();
        String pref_value = getStringPreferences(getApplicationContext(), PREF_FV_MENU_ORDER);
        if (pref_value.length() > 0) {
            order_data = new JsonParser().parse(pref_value).getAsJsonArray();
            for (int i = 0; i < order_data.size(); i++) {
                JsonObject item = new JsonObject();
                int key = order_data.get(i).getAsInt();
                item.addProperty("key", key);
                item.addProperty("value", fleetview_menu_keys[key]);
                item.addProperty("label", getStringWithLocale(KcaUtils.getId(
                        KcaUtils.format("viewmenu_%s", fleetview_menu_keys[key]), R.string.class)));
                data.add(item);
            }
        } else {
            for (int i = 0; i < fleetview_menu_keys.length; i++) {
                JsonObject item = new JsonObject();
                item.addProperty("key", i);
                item.addProperty("value", fleetview_menu_keys[i]);
                item.addProperty("label", getStringWithLocale(KcaUtils.getId(
                        KcaUtils.format("viewmenu_%s", fleetview_menu_keys[i]), R.string.class)));
                data.add(item);
                order_data.add(i);
            }
            setPreferences(getApplicationContext(), PREF_FV_MENU_ORDER, order_data.toString());
        }

        adapter = new KcaFleetViewMenuOrderAdpater(data, R.layout.listview_mbtn_item, R.id.setting_mbtn_selector, false);
        listview = findViewById(R.id.mbtn_list);
        listview.setDragListListener(new DragListView.DragListListener() {
            @Override
            public void onItemDragStarted(int position) {

            }

            @Override
            public void onItemDragging(int itemPosition, float x, float y) {

            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                order_data = new JsonArray();
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    order_data.add(adapter.getUniqueItemOrder(i));
                }
                setPreferences(getApplicationContext(), PREF_FV_MENU_ORDER, order_data.toString());
            }
        });

        listview.setLayoutManager(new LinearLayoutManager(this));
        listview.setAdapter(adapter, true);
        listview.setCanDragHorizontally(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}