package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_MATERIALS;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_RESOURCELOG_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_FILTERLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STAR_CHECKED;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getJapanCalendarInstance;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;


public class AkashiActivity extends BaseActivity {
    Toolbar toolbar;
    JsonObject akashiData, akashiDay;
    int akashiDataLoadingFlag = 0;
    ListView listview;
    int currentClicked = 0;

    TextView dmat_count, smat_count;
    TextView current_date;

    MaterialButton unsafeButton, safeButton;
    Button starButton, filterButton;
    boolean isStarChecked, isSafeChecked = false;
    ArrayList<KcaAkashiListViewItem> listViewItemList;

    KcaDBHelper dbHelper;
    KcaResourceLogger resourceLogger;
    KcaAkashiListViewAdpater adapter;
    UpdateHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akashi_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_akashi));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        resourceLogger = new KcaResourceLogger(getApplicationContext(), null, KCANOTIFY_RESOURCELOG_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();

        Calendar calendar = getJapanCalendarInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
        currentClicked = dayOfWeek;
        listViewItemList = new ArrayList<>();

        starButton = findViewById(R.id.akashi_btn_star);

        unsafeButton = findViewById(R.id.akashi_btn_unsafe);
        safeButton = findViewById(R.id.akashi_btn_safe);
        unsafeButton.setChecked(true);

        filterButton = findViewById(R.id.akashi_btn_filter);
        isStarChecked = getBooleanPreferences(getApplicationContext(), PREF_AKASHI_STAR_CHECKED);
        setStarButton();

        Date date = calendar.getTime();
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        current_date = findViewById(R.id.current_date);
        current_date.setText(KcaUtils.format("%s (%s)", date_format.format(date),
                getString(getId("akashi_term_day_" + dayOfWeek, R.string.class))));

        dmat_count = findViewById(R.id.count_dmat);
        smat_count = findViewById(R.id.count_smat);
        JsonArray material_data = dbHelper.getJsonArrayValue(DB_KEY_MATERIALS);
        if (material_data != null) {
            JsonElement dmat_value = material_data.get(6);
            String dmat_str = dmat_value.isJsonPrimitive() ? dmat_value.getAsString() : dmat_value.getAsJsonObject().get("api_value").getAsString();
            dmat_count.setText(String.valueOf(dmat_str));

            JsonElement smat_value = material_data.get(7);
            String smat_str = smat_value.isJsonPrimitive() ? smat_value.getAsString() : smat_value.getAsJsonObject().get("api_value").getAsString();
            smat_count.setText(String.valueOf(smat_str));
        }

        handler = new UpdateHandler(this);
        adapter = new KcaAkashiListViewAdpater();
        adapter.setHandler(handler);
        AkashiFilterActivity.setHandler(handler);

        akashiDataLoadingFlag = getAkashiDataFromStorage();
        if (akashiDataLoadingFlag != 1) {
            Toast.makeText(getApplicationContext(), "Error Loading Akashi Data", Toast.LENGTH_LONG).show();
        } else if (KcaApiData.getKcItemStatusById(2, "name") == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.kca_toast_get_data_at_settings_2), Toast.LENGTH_LONG).show();
        } else {
            loadAkashiList(currentClicked, isSafeChecked);
            adapter.setListViewItemList(listViewItemList);
            adapter.setSafeCheckedStatus(isSafeChecked);
            listview = findViewById(R.id.akashi_listview);
            listview.setAdapter(adapter);

            MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroup);
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                String name = getResources().getResourceEntryName(checkedId);
                currentClicked = name.charAt(name.length() - 1) - (int)'0';
                loadAkashiList(currentClicked, isSafeChecked);
                resetListView(true);
            });
            toggleGroup.check(KcaUtils.getId(KcaUtils.format("akashi_day_%d", currentClicked), R.id.class));

            unsafeButton.setOnClickListener(v -> {
                isSafeChecked = false;
                adapter.setSafeCheckedStatus(isSafeChecked);
                loadAkashiList(currentClicked, isSafeChecked);
                resetListView(false);
            });
            safeButton.setOnClickListener(v -> {
                isSafeChecked = true;
                adapter.setSafeCheckedStatus(isSafeChecked);
                loadAkashiList(currentClicked, isSafeChecked);
                resetListView(false);
            });

            starButton.setOnClickListener(v -> {
                isStarChecked = !isStarChecked;
                setStarButton();
                loadAkashiList(currentClicked, isSafeChecked);
                resetListView(true);
            });

            filterButton.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), AkashiFilterActivity.class);
                startActivity(intent);
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

    private int getAkashiDataFromStorage() {
        JsonObject data = KcaUtils.getJsonObjectFromStorage(getApplicationContext(), "akashi_data.json", dbHelper);
        if (data != null) {
            akashiData = data;
        } else {
            return -1;
        }

        data = KcaUtils.getJsonObjectFromStorage(getApplicationContext(), "akashi_day.json", dbHelper);
        if (data != null) {
            akashiDay = data;
        } else {
            return -1;
        }

        JsonArray itemData = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "akashi_reqitems.json", dbHelper);
        if (itemData != null) {
            AkashiDetailActivity.setRequiredItemTranslation(itemData);
        } else {
            return -1;
        }

        showDataLoadErrorToast(getApplicationContext(), getString(R.string.download_check_error));
        return 1;
    }

    private void setStarButton() {
        if (isStarChecked) {
            starButton.setText("★");
        } else {
            starButton.setText("☆");
        }
        setPreferences(getApplicationContext(), PREF_AKASHI_STAR_CHECKED, isStarChecked);
    }

    private void resetListView(boolean isTop) {
        adapter.setListViewItemList(listViewItemList);
        adapter.notifyDataSetChanged();
        if (isTop) listview.setAdapter(adapter);
    }

    private void loadAkashiList(int day, boolean checked) {
        final int TYPE_MUL = 1000;
        String starList = getStringPreferences(getApplicationContext(), PREF_AKASHI_STARLIST);
        String filterList = getStringPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST);

        if (!filterList.equals("|")) {
            filterButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            filterButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
        } else {
            filterButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
            filterButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnText));
        }

        listViewItemList.clear();
        List<Integer> keylist = new ArrayList<>();
        JsonArray equipList = akashiDay.getAsJsonArray(String.valueOf(day));
        for (int i = 0; i < equipList.size(); i++) {
            int equipId = equipList.get(i).getAsInt();
            JsonObject kcItemData = KcaApiData.getKcItemStatusById(equipId, "type");
            if (kcItemData != null) {
                int type2 = kcItemData.getAsJsonArray("type").get(2).getAsInt();
                int type3 = kcItemData.getAsJsonArray("type").get(3).getAsInt();
                if (!checkFiltered(filterList, type3)) {
                    keylist.add(type2 * TYPE_MUL + equipId);
                }
            }
        }
        Collections.sort(keylist);

        for (int equipId : keylist) {
            equipId = equipId % TYPE_MUL;
            if (isStarChecked && !checkStarred(starList, equipId)) continue;
            KcaAkashiListViewItem item = new KcaAkashiListViewItem();
            item.setEquipDataById(equipId);
            item.setEquipImprovementData(akashiData.getAsJsonObject(String.valueOf(equipId)));
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
            super(Looper.getMainLooper());
            mActivity = new WeakReference<>(activity);
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

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }
}
