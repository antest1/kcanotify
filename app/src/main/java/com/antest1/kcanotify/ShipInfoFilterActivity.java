package com.antest1.kcanotify;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.TAG_COUNT;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SPEQUIPS;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;


public class ShipInfoFilterActivity extends AppCompatActivity {
    public final static int SPECIAL_EQUIPMENT_COUNT = 5;

    Toolbar toolbar;
    static Gson gson = new Gson();
    List<CheckBox> filterSpecialEquipment = new ArrayList<>();
    TextView listcounter;
    LinearLayout listview;
    public int count;
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
        setContentView(R.layout.activity_shipinfo_filter);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.shipinfo_btn_filter));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String pref_special_equips = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SPEQUIPS);
        List<String> specialEquipsFilterList = new ArrayList<String>(Arrays.asList(pref_special_equips.split(",")));

        for (int i = 0; i < SPECIAL_EQUIPMENT_COUNT; i++) {
            final String key = KcaUtils.format("stype%d", i+1);
            Log.e("KCA", KcaUtils.format("equip_%s", key));
            filterSpecialEquipment.add(findViewById(
                    KcaUtils.getId(KcaUtils.format("equip_%s", key), R.id.class)
            ));
            CheckBox item = filterSpecialEquipment.get(i);
            item.setText(getStringWithLocale(
                    KcaUtils.getId(KcaUtils.format("ship_stat_equip_%s", key), R.string.class)
            ));
            item.setChecked(specialEquipsFilterList.contains(key));
            item.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) specialEquipsFilterList.add(key);
                else specialEquipsFilterList.remove(key);
                setPreferences(getApplicationContext(), PREF_SHIPINFO_SPEQUIPS,
                        KcaUtils.joinStr(specialEquipsFilterList, ","));
            });
        }

        listview = findViewById(R.id.ship_stat_sort_list);
        listcounter = findViewById(R.id.ship_stat_count);
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

        final View selector1 = v.findViewById(R.id.ship_stat_selector1);
        final View selector2 = v.findViewById(R.id.ship_stat_selector2);

        Spinner sp_target = v.findViewById(R.id.ship_stat_spinner);
        Spinner sp_op = v.findViewById(R.id.ship_stat_operator);
        final EditText condition_val = v.findViewById(R.id.ship_stat_value);
        final TextView sp_val = v.findViewById(R.id.ship_stat_select);
        ImageView add_remove_btn = v.findViewById(R.id.ship_stat_add_remove_btn);
        final CheckBox cb_target = v.findViewById(R.id.ship_stat_checked);

        condition_val.setInputType(InputType.TYPE_CLASS_NUMBER);
        condition_val.setTag(target);
        condition_val.addTextChangedListener(new KcaTextWatcher(condition_val));

        ArrayAdapter<CharSequence> adapter_target = ArrayAdapter.createFromResource(this,
                R.array.ship_filt_array, android.R.layout.simple_spinner_item);
        adapter_target.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp_target.setAdapter(adapter_target);
        sp_target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String data = sort_values.get(target);
                JsonObject obj = ShipInfoFilterActivity.unpackPrefValue(data);
                position = KcaShipListViewAdpater.getFilterKeyIndex(position);
                int prev_position = obj.get("idx").getAsInt();
                obj.addProperty("idx", position);

                sort_values.put(target, makeStatPrefValue(obj));
                if (KcaShipListViewAdpater.isBoolean(position)) {
                    selector1.setVisibility(View.GONE);
                    selector2.setVisibility(View.VISIBLE);
                } else {
                    selector1.setVisibility(View.VISIBLE);
                    selector2.setVisibility(View.GONE);
                    if (KcaShipListViewAdpater.isList(position)) {
                        condition_val.setVisibility(View.GONE);
                        sp_val.setVisibility(View.VISIBLE);
                        if(prev_position != position) {
                            setupListSelect(sp_val, target, "val", position, null);
                            sp_val.setText(getStringWithLocale(R.string.shipinfo_filt_list_dialog_title));
                        }
                    } else {
                        sp_val.setVisibility(View.GONE);
                        condition_val.setVisibility(View.VISIBLE);
                        if (KcaShipListViewAdpater.isNumeric(position)) {
                            condition_val.setInputType(InputType.TYPE_CLASS_NUMBER);
                        } else {
                            condition_val.setInputType(InputType.TYPE_CLASS_TEXT);
                        }
                        if(prev_position != position) condition_val.setText("");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        cb_target.setOnCheckedChangeListener((compoundButton, b) -> {
            String data = sort_values.get(target);
            JsonObject obj = ShipInfoFilterActivity.unpackPrefValue(data);
            obj.addProperty("op", b ? 3 : 0);
            obj.addProperty("val", 0);
            sort_values.put(target, makeStatPrefValue(obj));
        });

        ArrayAdapter<CharSequence> adapter_op = ArrayAdapter.createFromResource(this,
                R.array.ship_operations, android.R.layout.simple_spinner_item);
        adapter_op.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp_op.setAdapter(adapter_op);
        sp_op.setOnItemSelectedListener(getListener(target, "op", 0));

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
                        listcounter.setText(KcaUtils.format(getStringWithLocale(R.string.shipinfo_criteria_count), sort_values.size() - 1));
                    }
                }
            }
        });

        listview.addView(v);
        listcounter.setText(KcaUtils.format(getStringWithLocale(R.string.shipinfo_criteria_count), sort_values.size() - 1));
        if (key != -1) ((Spinner) listview.findViewWithTag(target).findViewById(R.id.ship_stat_spinner))
                .setSelection(KcaShipListViewAdpater.getFilterIndexByKey(key));
        if (op != -1) ((Spinner) listview.findViewWithTag(target).findViewById(R.id.ship_stat_operator)).setSelection(op);
        if (value.length() > 0) {
            if (KcaShipListViewAdpater.isBoolean(key)) {
                ((CheckBox) listview.findViewWithTag(target).findViewById(R.id.ship_stat_checked)).setChecked(op > 0);
            } else if (KcaShipListViewAdpater.isList(key)) {
                setupListSelect(((TextView) listview.findViewWithTag(target).findViewById(R.id.ship_stat_select)),
                        target, "val", key, value.split("_").length);
            } else {
                ((TextView) listview.findViewWithTag(target).findViewById(R.id.ship_stat_value)).setText(value);
            }
        }
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
                sort_values.put(target, makeStatPrefValue(obj));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        };
    }

    private void setupListSelect(TextView sp_val, int target, String key, int position, Integer value) {
        String[] adapter = {};
        int fnc = 0;
        if (position == 2) {
            adapter = getStypeArray();
            fnc = 1;
        } else if (position == 5) {
            adapter = getFleetArray();
        } else if (position == 7) {
            adapter = getHPArray();
        } else if (position == 20) {
            adapter = getSpeedArray();
            fnc = 2;
        } else if (position == 22) {
            adapter = getTagArray();
        }

        final AlertDialog dialog = makeDialog(sp_val, target, key, fnc, adapter);
        sp_val.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });
        if (value != null) {
            sp_val.setText(KcaUtils.format("%d/%d", value, adapter.length));
        }
    }

        private AlertDialog makeDialog(final TextView sp_val, final int target, final String key, final int fnc, final String[] arr) {
        final List<Integer> selectedItems = new ArrayList<>();
        boolean[] selected_arr = new boolean[arr.length];
        Arrays.fill(selected_arr, false);
        String data = sort_values.get(target);
        final JsonObject obj = unpackPrefValue(data);
        String[] val = obj.get("val").getAsString().split("_");

        for (String v: val) {
            if (v.length() > 0) {
                int idx = getRealPosition(fnc, Integer.valueOf(v));
                if (idx < selected_arr.length) {
                    selected_arr[idx] = true;
                    selectedItems.add(idx);
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getStringWithLocale(R.string.shipinfo_filt_list_dialog_title))
                .setMultiChoiceItems(arr, selected_arr, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedItems.add(indexSelected);
                        } else if (selectedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            selectedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                }).setPositiveButton(getStringWithLocale(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        sp_val.setText(KcaUtils.format("%d/%d", selectedItems.size(), arr.length));

                        List<String> vals = new ArrayList<String>();
                        for (int selected: selectedItems) {
                            switch (fnc) {
                                case 1:
                                    vals.add(String.valueOf(selected + 1));
                                    break;
                                case 2:
                                    vals.add(String.valueOf((selected + 1) * 5));
                                    break;
                                default:
                                    vals.add(String.valueOf(selected));
                                    break;
                            }
                        }
                        obj.addProperty(key, KcaUtils.joinStr(vals, "_"));
                        sort_values.put(target, makeStatPrefValue(obj));
                    }
                }).setNegativeButton(getStringWithLocale(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }).create();
        return dialog;
    }

    private String[] getStypeArray() {
        List<String> stype_list = new ArrayList<>();
        for (int i = 1; i < KcaApiData.getShipTypeSize(); i++) {
            stype_list.add(KcaApiData.getShipTypeAbbr(i));
        }
        String[] stype_arr = new String[stype_list.size()];
        stype_arr = stype_list.toArray(stype_arr);
        return stype_arr;
    }

    private String[] getSpeedArray() {
        List<String> speed_list = new ArrayList<>();
        speed_list.add(getStringWithLocale(R.string.speed_slow));
        speed_list.add(getStringWithLocale(R.string.speed_fast));
        speed_list.add(getStringWithLocale(R.string.speed_fastplus));
        speed_list.add(getStringWithLocale(R.string.speed_superfast));

        String[] speed_arr = new String[speed_list.size()];
        speed_arr = speed_list.toArray(speed_arr);
        return speed_arr;
    }

    private String[] getTagArray() {
        List<String> tag_list = new ArrayList<>();
        for (int i = 0; i <= TAG_COUNT; i++) {
            tag_list.add(getStringWithLocale(getId(KcaUtils.format("ship_tag_%d", i), R.string.class)));
        }

        String[] tag_arr = new String[tag_list.size()];
        tag_arr = tag_list.toArray(tag_arr);
        return tag_arr;
    }

    private String[] getFleetArray() {
        List<String> tag_list = new ArrayList<>();
        tag_list.add(getStringWithLocale(R.string.ship_fleet_0));
        for (int i = 0; i < 4; i++) {
            tag_list.add(KcaUtils.format(getStringWithLocale(R.string.fleet_format), i+1));
        }

        String[] tag_arr = new String[tag_list.size()];
        tag_arr = tag_list.toArray(tag_arr);
        return tag_arr;
    }

    private String[] getHPArray() {
        List<String> tag_list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tag_list.add(getStringWithLocale(KcaUtils.getId(KcaUtils.format("ship_hp_%d", i), R.string.class)));
        }

        String[] tag_arr = new String[tag_list.size()];
        tag_arr = tag_list.toArray(tag_arr);
        return tag_arr;
    }

    private int getStypePosition(int i) { return i - 1; }
    private int getSpeedPosition(int i) { return (i - 1) / 5; }
    private int getRealPosition(int fnc, int i) {
        switch (fnc) {
            case 1:
                return getStypePosition(i);
            case 2:
                return getSpeedPosition(i);
            default:
                return i;
        }
    }

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
        public KcaTextWatcher(EditText editText) {
            mEditText = editText;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            int target = (Integer) mEditText.getTag();
            String data = sort_values.get(target);
            JsonObject obj = ShipInfoFilterActivity.unpackPrefValue(data);
            obj.addProperty("val", editable.toString());
            sort_values.put(target, makeStatPrefValue(obj));
        }
    }
}
