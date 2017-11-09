package com.antest1.kcanotify;

import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_FILTERLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STAR_CHECKED;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;


public class AkashiActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    JsonObject akashiData, akashiDay;
    int akashiDataLoadingFlag = 0;
    ListView listview;
    int currentClicked = 0;

    Button starButton, safeButton, filterButton;
    boolean isStarChecked, isSafeChecked = false;
    ArrayList<KcaAkashiListViewItem> listViewItemList;
    KcaAkashiListViewAdpater adapter;
    UpdateHandler handler;

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

        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
        currentClicked = dayOfWeek;
        listViewItemList = new ArrayList<>();
        starButton = (Button) findViewById(R.id.akashi_btn_star);
        safeButton = (Button) findViewById(R.id.akashi_btn_safe);
        filterButton = (Button) findViewById(R.id.akashi_btn_filter);
        isStarChecked = getBooleanPreferences(getApplicationContext(), PREF_AKASHI_STAR_CHECKED);
        setStarButton();

        handler = new UpdateHandler(this);
        adapter = new KcaAkashiListViewAdpater();
        adapter.setHandler(handler);
        AkashiFilterActivity.setHandler(handler);

        akashiDataLoadingFlag = getAkashiDataFromAssets();
        if (akashiDataLoadingFlag != 1) {
            Toast.makeText(getApplicationContext(), "Error Loading Akashi Data", Toast.LENGTH_LONG).show();
        } else if (KcaApiData.getKcItemStatusById(2, "name") == null) {
            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.kca_toast_get_data_at_settings_2), Toast.LENGTH_LONG).show();
        } else {
            loadAkashiList(currentClicked, isSafeChecked);
            adapter.setListViewItemList(listViewItemList);
            adapter.setSafeCheckedStatus(isSafeChecked);
            listview = (ListView) findViewById(R.id.akashi_listview);
            listview.setAdapter(adapter);

            for (int i = 0; i < 7; i++) {
                final int week = i;
                TextView tv = (TextView) findViewById(KcaUtils.getId(KcaUtils.format("akashi_day_%d", i), R.id.class));
                if (week == currentClicked) {
                    tv.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                    tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentClicked != week) {
                            TextView tv_prev = (TextView) findViewById(KcaUtils.getId(KcaUtils.format("akashi_day_%d", currentClicked), R.id.class));
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
                    isSafeChecked = !isSafeChecked;
                    setSafeButton();
                    adapter.setSafeCheckedStatus(isSafeChecked);
                    loadAkashiList(currentClicked, isSafeChecked);
                    resetListView(false);
                }
            });

            starButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isStarChecked = !isStarChecked;
                    setStarButton();
                    loadAkashiList(currentClicked, isSafeChecked);
                    resetListView(true);
                }
            });

            filterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), AkashiFilterActivity.class);
                    startActivity(intent);
                }
            });
        }
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

    private void setStarButton() {
        if (isStarChecked) {
            starButton.setText("★");
            starButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            starButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            starButton.setText("☆");
            starButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            starButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
        }
        setPreferences(getApplicationContext(), PREF_AKASHI_STAR_CHECKED, isStarChecked);
    }

    private void setSafeButton() {
        if (isSafeChecked) {
            safeButton.setText(getStringWithLocale(R.string.aa_btn_safe_state1));
            safeButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            safeButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            safeButton.setText(getStringWithLocale(R.string.aa_btn_safe_state0));
            safeButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            safeButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
        }
    }

    private void resetListView(boolean isTop) {
        adapter.setListViewItemList(listViewItemList);
        adapter.notifyDataSetChanged();
        if (isTop) listview.setAdapter(adapter);
    }

    private void loadAkashiList(int day, boolean checked) {
        final int TYPE_MUL = 1000;
        String starlist = getStringPreferences(getApplicationContext(), PREF_AKASHI_STARLIST);
        String filterlist = getStringPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST);

        if (!filterlist.equals("|")) {
            filterButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            filterButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        } else {
            filterButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
            filterButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        }

        listViewItemList.clear();
        List<Integer> keylist = new ArrayList<Integer>();
        JsonArray equipList = akashiDay.getAsJsonArray(String.valueOf(day));
        for (int i = 0; i < equipList.size(); i++) {
            int equipid = equipList.get(i).getAsInt();
            JsonObject kcItemData = KcaApiData.getKcItemStatusById(equipid, "type");
            int type2 = kcItemData.getAsJsonArray("type").get(2).getAsInt();
            int type3 = kcItemData.getAsJsonArray("type").get(3).getAsInt();
            if (!checkFiltered(filterlist, type3)) {
                keylist.add(type2 * TYPE_MUL + equipid);
            }
        }
        Collections.sort(keylist);

        for (int equipid : keylist) {
            equipid = equipid % TYPE_MUL;
            if (isStarChecked && !checkStarred(starlist, equipid)) continue;
            KcaAkashiListViewItem item = new KcaAkashiListViewItem();
            item.setEquipDataById(equipid);
            Log.e("KCA", String.valueOf(equipid));
            item.setEquipImprovementData(akashiData.getAsJsonObject(String.valueOf(equipid)));
            item.setEquipImprovementElement(day, checked);
            listViewItemList.add(item);
        }
    }

    private boolean checkStarred(String data, int id) {
        return data.contains(KcaUtils.format("|%d|", id));
    }

    private boolean checkFiltered(String data, int id) {
        return data.contains(KcaUtils.format("|%d|", id));
    }

    private static class UpdateHandler extends Handler {
        private final WeakReference<AkashiActivity> mActivity;

        UpdateHandler(AkashiActivity activity) {
            mActivity = new WeakReference<AkashiActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AkashiActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleUpdateMessage();
            }
        }
    }

    public void handleUpdateMessage() {
        loadAkashiList(currentClicked, isSafeChecked);
        resetListView(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        loadTranslationData(getAssets(), getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}
