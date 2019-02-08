package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.media.CamcorderProfile.get;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.loadShipExpInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadSortieExpInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaApiData.setDataLoadTriggered;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_EXPCALTRK;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_EXPSHIP;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_EXPSORTIE;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class ExpCalcActivity extends AppCompatActivity {
    public static final int LEVEL_MAX = 175;
    private static final int RANK_S = 0;
    private static final int RANK_A = 1;
    private static final int RANK_B = 2;
    private static final int RANK_C = 3;
    private static final int RANK_D = 4;

    Toolbar toolbar;
    KcaDBHelper dbHelper;

    public int count;
    boolean is_flagship = false;
    boolean is_mvp = false;
    int rank_val = RANK_S;
    int current_exp = 0;
    int target_exp = 0;

    private boolean userIsInteracting;
    boolean shipselect_current_flag = false;
    boolean cal_visible = true;
    JsonObject current_ship_data;
    public static SparseArray<String> track_values = new SparseArray<>();

    View cal_area;
    Spinner value_ship, value_current_lv, value_target_lv, value_map, value_rank;
    TextView value_current_exp, value_target_exp, value_mapexp, value_counter, value_remainexp;
    EditText value_base_exp;
    CheckBox chkbox_flagship, chkbox_mvp;
    ArrayAdapter<String> ship_adapter, current_lv_adapter, target_lv_adapter, map_adapter, rank_adapter;
    ImageView cal_hide_bar, map_base_exp, btn_base_exp_help;
    FloatingActionButton add_button;
    LinearLayout listview;

    boolean load_flag = false;
    JsonObject current_state = new JsonObject();

    public ExpCalcActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadTranslationData(getApplicationContext());
        int exp_load_result = loadShipExpInfoFromAssets(getAssets());
        int exp_sortie_result = loadSortieExpInfoFromAssets(getAssets());
        if (exp_load_result != 1 || exp_sortie_result != 1) {
            Toast.makeText(getApplicationContext(), "Error loading data.", Toast.LENGTH_LONG).show();
            finish();
        }
        setUI();
    }

    private void setUI() {
        setContentView(R.layout.activity_expcalc);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_expcalc));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        current_ship_data = null;
        track_values = new SparseArray<>();

        cal_area = findViewById(R.id.cal_area);
        cal_hide_bar = findViewById(R.id.cal_hide_bar);
        cal_hide_bar.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.grey), PorterDuff.Mode.SRC_ATOP);
        cal_hide_bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cal_visible) {
                    cal_hide_bar.setImageResource(R.mipmap.ic_arrow_down);
                    cal_area.setVisibility(View.GONE);
                } else {
                    cal_hide_bar.setImageResource(R.mipmap.ic_arrow_up);
                    cal_area.setVisibility(View.VISIBLE);
                }
                cal_visible = !cal_visible;
            }
        });

        map_base_exp = findViewById(R.id.map_base_exp);
        map_base_exp.setVisibility(View.GONE);
        map_base_exp.setOnClickListener(v -> map_base_exp.setVisibility(View.GONE));

        btn_base_exp_help = findViewById(R.id.btn_base_exp_help);
        btn_base_exp_help.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.SRC_ATOP);
        btn_base_exp_help.setOnClickListener(v -> map_base_exp.setVisibility(View.VISIBLE));

        listview = findViewById(R.id.ship_leveling_list);
        add_button = findViewById(R.id.add_exp_track);
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current_ship_data != null) {
                    JsonObject data = KcaUtils.getJsonObjectCopy(current_ship_data);
                    data.addProperty("current_lv", String.valueOf(value_current_lv.getSelectedItem()));
                    data.addProperty("target_lv", String.valueOf(value_target_lv.getSelectedItem()));
                    data.addProperty("map", String.valueOf(value_map.getSelectedItem()));
                    data.addProperty("rank", String.valueOf(value_rank.getSelectedItem()));
                    data.addProperty("is_flagship", chkbox_flagship.isChecked());
                    data.addProperty("is_mvp", chkbox_mvp.isChecked());
                    data.addProperty("current_exp", value_current_exp.getText().toString());
                    data.addProperty("target_exp", value_target_exp.getText().toString());
                    data.addProperty("counter", value_counter.getText().toString());
                    data.addProperty("mapexp", getMapExp());
                    makeFilterItem(data);
                }
            }
        });

        value_ship = findViewById(R.id.value_ship);
        value_current_lv = findViewById(R.id.value_current_lv);
        value_target_lv = findViewById(R.id.value_target_lv);
        value_map = findViewById(R.id.value_map);
        value_rank = findViewById(R.id.value_rank);

        value_current_exp = findViewById(R.id.value_current_exp);
        value_target_exp = findViewById(R.id.value_target_exp);
        value_base_exp = findViewById(R.id.value_base_exp);
        value_mapexp = findViewById(R.id.value_mapexp);
        value_counter = findViewById(R.id.value_counter);
        value_remainexp = findViewById(R.id.value_remainexp);

        chkbox_flagship = findViewById(R.id.chkbox_flagship);
        chkbox_mvp = findViewById(R.id.chkbox_mvp);

        final JsonObject exp_ship_data = dbHelper.getJsonObjectValue(DB_KEY_EXPSHIP);

        final JsonObject exp_sortie_data = dbHelper.getJsonObjectValue(DB_KEY_EXPSORTIE);
        final String[] sortie_map = new String[exp_sortie_data.size()];
        int m = 0;
        for (Map.Entry<String, JsonElement> data : exp_sortie_data.entrySet()) {
            sortie_map[m] = data.getKey();
            m += 1;
        }

        value_base_exp.setText("1");

        map_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortie_map);
        map_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_map.setAdapter(map_adapter);
        value_map.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        //value_base_exp.setText(exp_sortie_data.get(sortie_map[position]).getAsString());
                        current_state.addProperty("map", position);
                        //setScreen();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                }
        );

        String[] rank_list = {"S", "A", "B", "C", "D"};
        rank_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rank_list);
        rank_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_rank.setAdapter(rank_adapter);
        value_rank.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        rank_val = position;
                        current_state.addProperty("rank", position);
                        setScreen();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                }
        );

        String[] level_list = new String[LEVEL_MAX];
        for (int i = 0; i < LEVEL_MAX; i++) {
            level_list[i] = String.valueOf(i + 1);
        }
        current_lv_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, level_list);
        current_lv_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_current_lv.setAdapter(current_lv_adapter);
        value_current_lv.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        if (!shipselect_current_flag && userIsInteracting && exp_ship_data != null) {
                            String value = String.valueOf(position + 1);
                            JsonArray exp_data = exp_ship_data.getAsJsonArray(value);
                            current_exp = exp_data.get(1).getAsInt();
                            current_state.addProperty("current_lv", position);
                            setScreen();
                        }
                        shipselect_current_flag = false;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                }
        );

        target_lv_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, level_list);
        target_lv_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_target_lv.setAdapter(target_lv_adapter);
        value_target_lv.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        if (exp_ship_data != null) {
                            String value = String.valueOf(position + 1);
                            JsonArray exp_data = exp_ship_data.getAsJsonArray(value);
                            target_exp = exp_data.get(1).getAsInt();
                            current_state.addProperty("target_lv", position);
                            setScreen();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                }
        );

        JsonArray ship_data = dbHelper.getJsonArrayValue(DB_KEY_SHIPIFNO);
        if (ship_data == null) ship_data = new JsonArray();
        Type listType = new TypeToken<List<JsonObject>>() {
        }.getType();
        final List<JsonObject> shipItemList = new Gson().fromJson(ship_data, listType);
        StatComparator cmp = new StatComparator();
        Collections.sort(shipItemList, cmp);

        String[] ship_label_list = new String[shipItemList.size()];
        for (int i = 0; i < ship_label_list.length; i++) {
            int lv = shipItemList.get(i).get("api_lv").getAsInt();
            int ship_id = shipItemList.get(i).get("api_ship_id").getAsInt();
            String name = "???";
            JsonObject kc_ship_data = KcaApiData.getKcShipDataById(ship_id, "name");
            if (kc_ship_data != null) {
                name = getShipTranslation(kc_ship_data.get("name").getAsString(), false);
            }
            ship_label_list[i] = KcaUtils.format("[Lv.%d] %s", lv, name);
        }

        ship_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ship_label_list);
        ship_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_ship.setAdapter(ship_adapter);
        value_ship.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        current_ship_data = shipItemList.get(position);
                        int lv = current_ship_data.get("api_lv").getAsInt();
                        int ship_id = current_ship_data.get("api_ship_id").getAsInt();
                        JsonObject kc_ship_data = KcaApiData.getKcShipDataById(ship_id, "afterlv");
                        int ship_afterlv;
                        if (kc_ship_data != null) {
                            ship_afterlv = kc_ship_data.get("afterlv").getAsInt();
                        } else {
                            ship_afterlv = 0;
                        }

                        shipselect_current_flag = true;
                        if (!load_flag) {
                            if (current_lv_adapter != null) {
                                value_current_lv.setSelection(lv - 1);
                            }
                            if (target_lv_adapter != null) {
                                if (ship_afterlv <= 0 || lv >= ship_afterlv) value_target_lv.setSelection(Math.min(lv, LEVEL_MAX - 1));
                                else value_target_lv.setSelection(ship_afterlv - 1);
                            }
                        } else {
                            load_flag = false;
                        }

                        current_exp = current_ship_data.getAsJsonArray("api_exp").get(0).getAsInt();
                        current_state.addProperty("ship", position);
                        setScreen();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                }
        );

        chkbox_flagship.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                is_flagship = b;
                current_state.addProperty("flagship", b);
                setScreen();
            }
        });

        chkbox_mvp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                is_mvp = b;
                current_state.addProperty("mvp", b);
                setScreen();
            }
        });

        value_base_exp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                setScreen();
            }
        });

        JsonObject ship_index = new JsonObject();
        for (int i = 0; i < ship_data.size(); i++) {
            JsonObject item = ship_data.get(i).getAsJsonObject();
            String key = item.get("api_id").getAsString();
            ship_index.add(key, item);
        }

        JsonArray saved_data = dbHelper.getJsonArrayValue(DB_KEY_EXPCALTRK);
        if (saved_data != null) {
            for (int i = 0; i < saved_data.size(); i++) {
                JsonObject item = saved_data.get(i).getAsJsonObject();
                String key = item.get("api_id").getAsString();
                if (ship_index.has(key)) {
                    int mapexp = item.get("mapexp").getAsInt();
                    int current_lv = ship_index.getAsJsonObject(key)
                            .get("api_lv").getAsInt();
                    int current_exp = ship_index.getAsJsonObject(key)
                            .getAsJsonArray("api_exp").get(0).getAsInt();
                    int target_exp = item.get("target_exp").getAsInt();
                    int remainexp = Math.max(0, target_exp - current_exp);
                    item.addProperty("current_lv", current_lv);
                    item.addProperty("current_exp", current_exp);
                    item.addProperty("counter", (int) Math.ceil((double) remainexp/ mapexp));
                    makeFilterItem(item);
                }
            }
        }

        loadCurrentState();
    }

    private void loadCurrentState() {
        load_flag = current_state.has("current_lv") || current_state.has("target_lv");
        if (current_state.has("ship")) {
            value_ship.setSelection(current_state.get("ship").getAsInt());
        }
        if (current_state.has("current_lv")) {
            value_current_lv.setSelection(current_state.get("current_lv").getAsInt());
        }
        if (current_state.has("target_lv")) {
            value_target_lv.setSelection(current_state.get("target_lv").getAsInt());
        }
        if (current_state.has("map")) {
            value_map.setSelection(current_state.get("map").getAsInt());
        }
        if (current_state.has("rank")) {
            value_rank.setSelection(current_state.get("rank").getAsInt());
        }
        if (current_state.has("flagship")) {
            chkbox_flagship.setChecked(current_state.get("flagship").getAsBoolean());
        }
        if (current_state.has("mvp")) {
            chkbox_mvp.setChecked(current_state.get("mvp").getAsBoolean());

        }
    }

    private int getMapExp() {
        String base_text = value_base_exp.getText().toString();
        if (base_text.length() > 9) base_text = base_text.substring(0, 9);
        int mapexp = 1;
        if (base_text.length() > 0) mapexp = Integer.parseInt(base_text);
        if (mapexp == 0) mapexp = 1;

        if (is_mvp) mapexp *= 2;
        if (is_flagship) mapexp = (mapexp * 3) / 2;
        switch (rank_val) {
            case RANK_S:
                mapexp = mapexp * 6 / 5;
                break;
            case RANK_C:
                mapexp = mapexp * 8 / 10;
                break;
            case RANK_D:
                mapexp = mapexp * 7 / 10;
                break;
            default:
                break;
        }
        return mapexp;
    }

    private void setScreen() {
        int mapexp = getMapExp();
        value_current_exp.setText(String.valueOf(current_exp));
        value_target_exp.setText(String.valueOf(target_exp));

        value_mapexp.setText(String.valueOf(mapexp));
        int remainexp = Math.max(0, target_exp - current_exp);
        value_remainexp.setText(String.valueOf(remainexp));
        int left_count = (int) Math.ceil((double) remainexp / mapexp);
        value_counter.setText(String.valueOf(left_count));
    }

    private void makeFilterItem(JsonObject data) {
        count += 1;
        track_values.append(count, data.toString());

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.listview_expcalc_item, null);
        v.setTag(count);
        final int target = count;

        TextView ship_name = v.findViewById(R.id.ship_name);
        TextView level_before = v.findViewById(R.id.level_before);
        TextView level_after = v.findViewById(R.id.level_after);
        TextView exp_left = v.findViewById(R.id.exp_left);
        TextView label_battle = v.findViewById(R.id.label_battle);
        TextView value_battle = v.findViewById(R.id.value_battle);
        TextView stat_area = v.findViewById(R.id.stat_area);
        TextView stat_rank = v.findViewById(R.id.stat_rank);
        TextView stat_flagship = v.findViewById(R.id.stat_flagship);
        TextView stat_mvp = v.findViewById(R.id.stat_mvp);

        int ship_id = data.get("api_ship_id").getAsInt();
        String ship_name_value = "???";
        JsonObject kc_ship_data = KcaApiData.getKcShipDataById(ship_id, "name");
        if (kc_ship_data != null) {
            ship_name_value = getShipTranslation(kc_ship_data.get("name").getAsString(), false);
        }

        ship_name.setText(ship_name_value);
        level_before.setText(data.get("current_lv").getAsString());
        level_after.setText(data.get("target_lv").getAsString());
        exp_left.setText(String.valueOf(Math.max(0, data.get("target_exp").getAsInt() - data.get("current_exp").getAsInt())));
        label_battle.setText(getStringWithLocale(R.string.expcalc_battle));
        value_battle.setText(data.get("counter").getAsString());
        stat_area.setText(data.get("map").getAsString());
        stat_rank.setText(data.get("rank").getAsString());
        stat_flagship.setText(getStringWithLocale(R.string.expcalc_flagship));
        stat_mvp.setText(getStringWithLocale(R.string.expcalc_mvp));

        if (data.get("is_flagship").getAsBoolean()) {
            stat_flagship.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpCalcFlagship));
        } else {
            stat_flagship.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.lightgray));
        }

        if (data.get("is_mvp").getAsBoolean()) {
            stat_mvp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpCalcMVP));
        } else {
            stat_mvp.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.lightgray));
        }

        ImageView remove_btn = v.findViewById(R.id.ship_remove);
        remove_btn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorBtnText), PorterDuff.Mode.SRC_ATOP);
        remove_btn.setOnClickListener(view -> {
            removeViewByTag(target);
        });
        listview.addView(v);
    }

    private void removeViewByTag(int tag) {
        View target = listview.findViewWithTag(tag);
        listview.removeView(target);
        track_values.remove(tag);
    }

    private void setValueAndFinish() {
        JsonArray data = new JsonArray();
        for (int i = 0; i < track_values.size(); i++) {
            int k = track_values.keyAt(i);
            if (k <= count) {
                JsonObject val = new JsonParser().parse(track_values.get(k)).getAsJsonObject();
                data.add(val);
            }
        }
        dbHelper.putValue(DB_KEY_EXPCALTRK, data.toString());
    }


    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    @Override
    protected void onDestroy() {
        setValueAndFinish();
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

    private class StatComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String sort_key = "api_lv";
            return o2.get(sort_key).getAsInt() - o1.get(sort_key).getAsInt();
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
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }

        super.onConfigurationChanged(newConfig);
        setUI();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }
}
