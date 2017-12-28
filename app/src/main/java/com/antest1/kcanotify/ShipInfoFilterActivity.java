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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.dataLoadTriggered;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;


public class ShipInfoFilterActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    LinearLayout listview;
    public static int count;
    static TextView debug;
    public static SparseArray<String> sort_values = new SparseArray<>();

    KcaDBHelper dbHelper;

    public ShipInfoFilterActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    public static String makeStatPrefValue(int idx, int op, String val) {
        return KcaUtils.format("%d,%d,%s", idx, op, val);
    }

    public static String makeStatPrefValue(JsonObject data) {
        return KcaUtils.format("%d,%d,%s", data.get("idx").getAsInt(),
                data.get("op").getAsInt(), data.get("val").getAsString());
    }

    public static JsonObject unpackPrefValue(String pref) {
        JsonObject obj = new JsonObject();
        String[] data = pref.split(",", 3);
        obj.addProperty("idx", data[0]);
        obj.addProperty("op", data[1]);
        obj.addProperty("val", data[2]);
        return obj;
    }

    private String makeStatFiltData() {
        return makeStatFiltData(getApplicationContext());
    }

    private static String makeStatFiltData(Context context) {
        if (sort_values.size() <= 1) return "|";
        List<String> data = new ArrayList<>();
        for (int i = 0; i < sort_values.size(); i++) {
            int k = sort_values.keyAt(i);
            if (k < count) {
                String val = sort_values.get(k);
                if (!val.endsWith(",")) data.add(sort_values.get(k));
            }
        }
        if (data.size() == 0) return "|";
        return KcaUtils.format("|%s|", KcaUtils.joinStr(data, "|"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadTranslationData(getApplicationContext());
        setContentView(R.layout.activity_shipinfo_sort_filter);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.shipinfo_btn_filter));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        debug = findViewById(R.id.debug);

        listview = findViewById(R.id.ship_stat_sort_list);
        String pref_sort_list = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_FILTCOND);
        String[] sort_keys = pref_sort_list.split("\\|");
        count = 0;
        sort_values = new SparseArray<>();

        for (String key_desc: sort_keys) {
            if (key_desc.length() > 0) {
                String[] key_desc_list = key_desc.split(",", 3);
                int key = Integer.valueOf(key_desc_list[0]);
                int op = Integer.valueOf(key_desc_list[1]);
                String value = "";
                if (key_desc_list.length > 2) value = key_desc_list[2];
                makeAndFilterItem(key, op, value, false);
            }
        }
        makeAndFilterItem();
    }

    private void makeAndFilterItem() {
        makeAndFilterItem(0, 0, "", true);
    }

    private void makeAndFilterItem(int key, int op, String value, boolean add_flag) {
        count += 1;
        sort_values.put(count, makeStatPrefValue(key, op, value));

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.listview_shipinfo_filt_and, null);
        if (add_flag) v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorStatSortCurrentBack));
        v.setTag(count);
        final int target = count;

        Spinner sp_target = v.findViewById(R.id.ship_stat_spinner);
        Spinner sp_op = v.findViewById(R.id.ship_stat_operator);
        final EditText condition_val = v.findViewById(R.id.ship_stat_value);
        final Spinner sp_val = v.findViewById(R.id.ship_stat_select);
        ImageView add_remove_btn = v.findViewById(R.id.ship_stat_add_remove_btn);

        condition_val.setInputType(InputType.TYPE_CLASS_NUMBER);
        condition_val.setTag(target);
        condition_val.addTextChangedListener(new KcaTextWatcher(getApplicationContext(), condition_val));

        ArrayAdapter<CharSequence> adapter_target = ArrayAdapter.createFromResource(this,
                R.array.ship_stat_array, android.R.layout.simple_spinner_item);
        adapter_target.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp_target.setAdapter(adapter_target);
        sp_target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String data = sort_values.get(target);
                JsonObject obj = ShipInfoFilterActivity.unpackPrefValue(data);
                obj.addProperty("idx", position);
                ShipInfoFilterActivity.sort_values.put(target, ShipInfoFilterActivity.makeStatPrefValue(obj));
                if (KcaShipListViewAdpater.isList(position)) {
                    condition_val.setVisibility(View.GONE);
                    sp_val.setVisibility(View.VISIBLE);
                    setupSpinner(sp_val, target, "val", position, null);
                } else {
                    sp_val.setVisibility(View.GONE);
                    condition_val.setVisibility(View.VISIBLE);
                    if (KcaShipListViewAdpater.isNumeric(position)) {
                        condition_val.setInputType(InputType.TYPE_CLASS_NUMBER);
                    } else {
                        condition_val.setInputType(InputType.TYPE_CLASS_TEXT);
                    }
                    condition_val.setText("");
                }
                debug.setText(makeStatFiltData());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ArrayAdapter<CharSequence> adapter_op = ArrayAdapter.createFromResource(this,
                R.array.ship_operations, android.R.layout.simple_spinner_item);
        adapter_op.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp_op.setAdapter(adapter_op);
        sp_op.setOnItemSelectedListener(getListener(target, "op", 0));

        if (key != -1) sp_target.setSelection(key);
        if (op != -1) sp_op.setSelection(op);

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
                    makeAndFilterItem();
                } else {
                    if (sort_values.size() > 1) {
                        removeViewByTag(target);
                        sort_values.delete(target);
                    }
                }
                debug.setText(makeStatFiltData());
            }
        });

        listview.addView(v);
        if (value.length() > 0) {
            Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), String.valueOf(target), Toast.LENGTH_LONG).show();
            if (KcaShipListViewAdpater.isList(key)) {
                setupSpinner(((Spinner) listview.findViewWithTag(target).findViewById(R.id.ship_stat_select)),
                        target, "val", key, Integer.valueOf(value));
            } else {
                ((TextView) listview.findViewWithTag(target).findViewById(R.id.ship_stat_value)).setText(value);
            }
        }

        debug.setText(makeStatFiltData());
    }

    TextWatcher tw = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // Nothing to do
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // Nothing to do
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void removeViewByTag(int tag) {
        View target = listview.findViewWithTag(tag);
        listview.removeView(target);
    }

    private AdapterView.OnItemSelectedListener getListener(final int target, final String key, final int fnc) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String data = sort_values.get(target);
                JsonObject obj = ShipInfoFilterActivity.unpackPrefValue(data);
                switch (fnc) {
                    case 1:
                        obj.addProperty(key, position + 1);
                        break;
                    case 2:
                        obj.addProperty(key, 5 * (position + 1));
                        break;
                    default:
                        obj.addProperty(key, position);
                        break;
                }
                ShipInfoFilterActivity.sort_values.put(target, ShipInfoFilterActivity.makeStatPrefValue(obj));
                debug.setText(makeStatFiltData());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        };
    }

    private void setupSpinner(Spinner sp_val, int target, String key, int position, Integer value) {
        int fnc = 0;
        if (position == 2) fnc = 1;
        if (position == 12) fnc = 2;
        sp_val.setAdapter(getStypeAdapter());
        sp_val.setOnItemSelectedListener(getListener(target, "val", fnc));
        if (value != null) sp_val.setSelection(getStypePosition(value));
    }

    private ArrayAdapter<String> getStypeAdapter() {
        List<String> stype_list = new ArrayList<>();
        for (int i = 1; i < KcaApiData.getShipTypeSize(); i++) {
            stype_list.add(KcaApiData.getShipTypeAbbr(i));
        }
        String[] stype_arr = new String[stype_list.size()];
        stype_arr = stype_list.toArray(stype_arr);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stype_arr);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }

    private int getStypePosition(int i) { return i - 1; }

    private ArrayAdapter<String> getSpeedAdapter() {
        List<String> speed_list = new ArrayList<>();
        speed_list.add(getStringWithLocale(R.string.speed_slow));
        speed_list.add(getStringWithLocale(R.string.speed_fast));
        speed_list.add(getStringWithLocale(R.string.speed_fastplus));
        speed_list.add(getStringWithLocale(R.string.speed_superfast));

        String[] speed_arr = new String[speed_list.size()];
        speed_arr = speed_list.toArray(speed_arr);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, speed_arr);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        return adapter;
    }

    private int getSpeedPosition(int i) { return (i - 1) / 5; }

    private void setValueAndFinish() {
        setPreferences(getApplicationContext(), PREF_SHIPINFO_FILTCOND, makeStatFiltData());
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

    public static class KcaTextWatcher implements TextWatcher {
        private EditText mEditText;
        private Context mContext;
        public KcaTextWatcher(Context ctx, EditText editText) {
            mEditText = editText;
            mContext = ctx;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            Toast.makeText(mContext, "Editable: "+editable.toString(), Toast.LENGTH_LONG).show();
            int target = (Integer) mEditText.getTag();
            String data = sort_values.get(target);
            JsonObject obj = ShipInfoFilterActivity.unpackPrefValue(data);
            obj.addProperty("val", editable.toString());
            ShipInfoFilterActivity.sort_values.put(target, ShipInfoFilterActivity.makeStatPrefValue(obj));
            debug.setText(makeStatFiltData(mContext));
        }
    }
}
