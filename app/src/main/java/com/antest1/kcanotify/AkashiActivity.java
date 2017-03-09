package com.antest1.kcanotify;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;


public class AkashiActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    JsonObject akashiData, akashiDay;
    int akashiDataLoadingFlag = 0;
    ListView listview;
    int currentClicked = 0;

    Button safeButton;
    boolean isSafeChecked = false;
    ArrayList<KcaAkashiListViewItem> listViewItemList;
    KcaAkashiListViewAdpater adapter;

    public AkashiActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akashi_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_akashi));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
        currentClicked = dayOfWeek;
        listViewItemList = new ArrayList<>();
        safeButton = (Button) findViewById(R.id.akashi_btn_safe);
        adapter = new KcaAkashiListViewAdpater();
        akashiDataLoadingFlag = getAkashiDataFromAssets();
        if (akashiDataLoadingFlag != 1) {
            Toast.makeText(getApplicationContext(), "Error Loading Akashi Data", Toast.LENGTH_LONG).show();
        } else {
            loadAkashiList(currentClicked, isSafeChecked);
            adapter.setListViewItemList(listViewItemList);
            adapter.setSafeCheckedStatus(isSafeChecked);
            listview = (ListView) findViewById(R.id.akashi_listview);
            listview.setAdapter(adapter);
        }

        for (int i = 0; i < 7; i++) {
            final int week = i;
            TextView tv = (TextView) findViewById(KcaUtils.getId(String.format("akashi_week_%d", i), R.id.class));
            if (week == currentClicked) {
                tv.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            }
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentClicked != week) {
                        TextView tv_prev = (TextView) findViewById(KcaUtils.getId(String.format("akashi_week_%d", currentClicked), R.id.class));
                        tv_prev.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
                        tv_prev.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                        v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                        ((TextView) v).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                        currentClicked = week;
                        loadAkashiList(currentClicked, isSafeChecked);
                        resetListView(true);
                    }
                }
            });
        }

        safeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSafeChecked) {
                    ((Button) v).setText(getStringWithLocale(R.string.aa_btn_safe_state1));
                    ((Button) v).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                } else {
                    ((Button) v).setText(getStringWithLocale(R.string.aa_btn_safe_state0));
                    ((Button) v).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                    v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
                }
                isSafeChecked = !isSafeChecked;
                adapter.setSafeCheckedStatus(isSafeChecked);
                loadAkashiList(currentClicked, isSafeChecked);
                resetListView(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        akashiData = null;
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private int getAkashiDataFromAssets() {
        try {
            AssetManager.AssetInputStream ais;
            JsonElement data;

            ais = (AssetManager.AssetInputStream) getAssets().open("akashi_data.json");
            data = new JsonParser().parse(new String(ByteStreams.toByteArray(ais)));
            ais.close();
            if (data.isJsonObject()) {
                akashiData = data.getAsJsonObject();
            } else {
                return -1;
            }

            ais = (AssetManager.AssetInputStream) getAssets().open("akashi_day.json");
            data = new JsonParser().parse(new String(ByteStreams.toByteArray(ais)));
            ais.close();
            if (data.isJsonObject()) {
                akashiDay = data.getAsJsonObject();
            } else {
                return -1;
            }

            return 1;
        } catch (IOException e) {
            return 0;
        }
    }



    private void resetListView(boolean isTop) {
        adapter.setListViewItemList(listViewItemList);
        adapter.notifyDataSetChanged();
        if (isTop) listview.setAdapter(adapter);
    }

    private void loadAkashiList(int day, boolean checked) {
        final int TYPE_MUL = 1000;
        listViewItemList.clear();
        List<Integer> keylist = new ArrayList<Integer>();
        JsonArray equipList = akashiDay.getAsJsonArray(String.valueOf(day));
        for (int i = 0; i < equipList.size(); i++) {
            int equipid = equipList.get(i).getAsInt();
            JsonObject kcItemData = KcaApiData.getKcItemStatusById(equipid, "type");
            int type2 = kcItemData.getAsJsonArray("type").get(2).getAsInt();
            keylist.add(type2 * TYPE_MUL + equipid);
        }
        Collections.sort(keylist);

        for (int equipid : keylist) {
            equipid = equipid % TYPE_MUL;
            KcaAkashiListViewItem item = new KcaAkashiListViewItem();
            item.setEquipDataById(equipid);
            JsonArray improvmentData = akashiData.getAsJsonObject(String.valueOf(equipid)).getAsJsonArray("improvment");
            item.setEquipImprovementData(improvmentData, day, checked);
            listViewItemList.add(item);
        }
    }
}
