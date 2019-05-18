package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.widget.Toast.makeText;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SORTKEY;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;


public class ShipInfoSortActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    LinearLayout listview;
    TextView listcounter;
    public int count;
    public List<Integer> sort_items = new ArrayList<>();
    public SparseArray<String> sort_values = new SparseArray<>();

    KcaDBHelper dbHelper;

    public ShipInfoSortActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    private String makeStatPrefValue(int idx, boolean is_desc) {
        return KcaUtils.format("%d,%b", idx, is_desc);
    }

    private String makeStatPrefData() {
        if (sort_items.size() == 1) return "|";
        List<String> data = new ArrayList<>();
        for (Integer v: sort_items) {
            if (v < count && sort_values.indexOfKey(v) >= 0) {
                data.add(sort_values.get(v));
            }
        }
        return KcaUtils.format("|%s|", KcaUtils.joinStr(data, "|"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipinfo_sort);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.shipinfo_btn_sort));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        listview = findViewById(R.id.ship_stat_sort_list);
        listcounter = findViewById(R.id.ship_stat_count);
        String pref_sort_list = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SORTKEY);
        String[] sort_keys = pref_sort_list.split("\\|");
        count = 0;
        for (String key_desc: sort_keys) {
            if (key_desc.length() > 0) {
                String[] key_desc_list = key_desc.split(",");
                int key = Integer.valueOf(key_desc_list[0]);
                boolean is_desc = Boolean.valueOf(key_desc_list[1]);
                makeSortItem(key, is_desc, false);
            }
        }
        makeSortItem(0, false, true);
    }

    private void makeSortItem(int key, boolean is_desc, boolean add_flag) {
        count += 1;
        sort_items.add(count);

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.listview_shipinfo_stat, null);
        if (add_flag) v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorStatSortCurrentBack));
        v.setTag(count);
        final int target = count;

        Spinner sp = v.findViewById(R.id.ship_stat_spinner);
        ImageView add_remove_btn = v.findViewById(R.id.ship_stat_add_remove_btn);
        final CheckBox desc_check = v.findViewById(R.id.ship_stat_isdesc);
        desc_check.setText(getStringWithLocale(R.string.shipinfo_sort_asc));
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ship_stat_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                position = KcaShipListViewAdpater.getSortKeyIndex(position);
                sort_values.put(target, makeStatPrefValue(position, desc_check.isChecked()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (key != -1) {
            sp.setSelection(KcaShipListViewAdpater.getSortIndexByKey(key));
        }

        desc_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    compoundButton.setText(getStringWithLocale(R.string.shipinfo_sort_desc));
                } else {
                    compoundButton.setText(getStringWithLocale(R.string.shipinfo_sort_asc));
                }
                if (sort_values.indexOfKey(target) >= 0) {
                    int value = Integer.valueOf(sort_values.get(target).split(",")[0]);
                    sort_values.put(target, makeStatPrefValue(value, checked));
                    listcounter.setText(KcaUtils.format(getStringWithLocale(R.string.shipinfo_criteria_count), sort_values.size() - 1));
                }
            }
        });
        if(add_flag) {
            add_remove_btn.setImageResource(R.mipmap.ic_add_circle);
            add_remove_btn.setTag(true);
        } else {
            add_remove_btn.setImageResource(R.mipmap.ic_remove_circle);
            add_remove_btn.setTag(false);
        }

        add_remove_btn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorBtnText), PorterDuff.Mode.MULTIPLY);


        add_remove_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listview.findViewWithTag(target).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                ImageView im = (ImageView) view;
                boolean is_add = (Boolean) im.getTag();
                if (is_add) {
                    im.setImageResource(R.mipmap.ic_remove_circle);
                    im.setTag(false);
                    makeSortItem(0, false, true);
                } else {
                    if (sort_items.size() > 2) {
                        removeViewByTag(target);
                        sort_items.remove(Integer.valueOf(target));
                        sort_values.delete(target);
                        listcounter.setText(KcaUtils.format(getStringWithLocale(R.string.shipinfo_criteria_count), sort_values.size() - 1));
                    }
                }
            }
        });
        listview.addView(v);
        listcounter.setText(KcaUtils.format(getStringWithLocale(R.string.shipinfo_criteria_count), sort_items.size() - 1));
        CheckBox cb = listview.findViewWithTag(target).findViewById(R.id.ship_stat_isdesc);
        cb.setChecked(is_desc);
    }

    private void removeViewByTag(int tag) {
        View target = listview.findViewWithTag(tag);
        listview.removeView(target);
    }

    private void setValueAndFinish() {
        setPreferences(getApplicationContext(), PREF_SHIPINFO_SORTKEY, makeStatPrefData());
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setValueAndFinish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setValueAndFinish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        loadTranslationData(getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}
