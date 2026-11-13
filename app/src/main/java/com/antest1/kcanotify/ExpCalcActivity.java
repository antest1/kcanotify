package com.antest1.kcanotify;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.antest1.kcanotify.viewmodel.ExpCalcViewModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;


public class ExpCalcActivity extends BaseActivity {
    Toolbar toolbar;

    boolean userIsInteracting;
    boolean shipselect_current_flag = false;
    boolean cal_visible = true;

    View cal_area;
    Spinner value_ship, value_current_lv, value_target_lv, value_map, value_rank;
    TextView value_current_exp, value_target_exp, value_mapexp, value_counter, value_remainexp;
    EditText value_base_exp;
    CheckBox chkbox_flagship, chkbox_mvp;
    ArrayAdapter<String> ship_adapter, current_lv_adapter, target_lv_adapter, map_adapter, rank_adapter;
    ImageView cal_hide_bar, map_base_exp, btn_base_exp_help;
    FloatingActionButton add_button;
    LinearLayout listview;

    ExpCalcViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ExpCalcViewModel.class);
        if (!viewModel.init()) {
            Toast.makeText(getApplicationContext(), "Error loading data.", Toast.LENGTH_LONG).show();
            finish();
        }
        setUI();
    }

    private void setUI() {
        setContentView(R.layout.activity_expcalc);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_expcalc));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewModel.resetForUi();

        cal_area = findViewById(R.id.cal_area);
        cal_hide_bar = findViewById(R.id.cal_hide_bar);
        cal_hide_bar.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.grey), PorterDuff.Mode.SRC_ATOP);
        cal_hide_bar.setOnClickListener(v -> {
            if (cal_visible) {
                cal_hide_bar.setImageResource(R.drawable.ic_arrow_down);
                cal_area.setVisibility(View.GONE);
            } else {
                cal_hide_bar.setImageResource(R.drawable.ic_arrow_up);
                cal_area.setVisibility(View.VISIBLE);
            }
            cal_visible = !cal_visible;
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
        add_button.setOnClickListener(v -> {
            if (viewModel.getCurrentShipData() != null) {
                JsonObject data = KcaUtils.getJsonObjectCopy(viewModel.getCurrentShipData());
                data.addProperty("current_lv", String.valueOf(value_current_lv.getSelectedItem()));
                data.addProperty("target_lv", String.valueOf(value_target_lv.getSelectedItem()));
                data.addProperty("map", String.valueOf(value_map.getSelectedItem()));
                data.addProperty("rank", String.valueOf(value_rank.getSelectedItem()));
                data.addProperty("is_flagship", chkbox_flagship.isChecked());
                data.addProperty("is_mvp", chkbox_mvp.isChecked());
                data.addProperty("current_exp", value_current_exp.getText().toString());
                data.addProperty("target_exp", value_target_exp.getText().toString());
                data.addProperty("counter", value_counter.getText().toString());
                data.addProperty("mapexp", viewModel.getMapExp(value_base_exp.getText().toString()));
                makeFilterItem(data);
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

        final JsonObject exp_ship_data = viewModel.getExpShipData();

        final String[] sortie_map = viewModel.getSortieMap();

        value_base_exp.setText("1");

        map_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortie_map);
        map_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_map.setAdapter(map_adapter);
        value_map.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                viewModel.setMapState(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        String[] rank_list = ExpCalcViewModel.getRankList();
        rank_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rank_list);
        rank_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_rank.setAdapter(rank_adapter);
        value_rank.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                viewModel.setRank(position);
                setScreen();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        String[] level_list = viewModel.getLevelList();
        current_lv_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, level_list);
        current_lv_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_current_lv.setAdapter(current_lv_adapter);
        value_current_lv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (!shipselect_current_flag && userIsInteracting && exp_ship_data != null) {
                    String value = String.valueOf(position + 1);
                    JsonArray exp_data = exp_ship_data.getAsJsonArray(value);
                    viewModel.setCurrentExp(exp_data.get(1).getAsInt());
                    viewModel.setCurrentLvState(position);
                    setScreen();
                }
                shipselect_current_flag = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        target_lv_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, level_list);
        target_lv_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_target_lv.setAdapter(target_lv_adapter);
        value_target_lv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (exp_ship_data != null) {
                    String value = String.valueOf(position + 1);
                    JsonArray exp_data = exp_ship_data.getAsJsonArray(value);
                    viewModel.setTargetExp(exp_data.get(1).getAsInt());
                    viewModel.setTargetLvState(position);
                    setScreen();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        final List<JsonObject> shipItemList = viewModel.getShipItemList();

        String[] ship_label_list = viewModel.getShipLabelList();

        ship_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ship_label_list);
        ship_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_ship.setAdapter(ship_adapter);
        value_ship.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                viewModel.setCurrentShipData(shipItemList.get(position));
                JsonObject current_ship_data = viewModel.getCurrentShipData();
                int lv = current_ship_data.get("api_lv").getAsInt();
                int ship_id = current_ship_data.get("api_ship_id").getAsInt();
                JsonObject kc_ship_data = getKcShipDataById(ship_id, "afterlv");
                int ship_afterlv;
                if (kc_ship_data != null) {
                    ship_afterlv = kc_ship_data.get("afterlv").getAsInt();
                } else {
                    ship_afterlv = 0;
                }

                shipselect_current_flag = true;
                if (!viewModel.isLoadFlag()) {
                    if (current_lv_adapter != null) {
                        value_current_lv.setSelection(Math.min(lv - 1, ExpCalcViewModel.LEVEL_MAX - 1));
                    }
                    if (target_lv_adapter != null) {
                        if (ship_afterlv <= 0 || lv >= ship_afterlv)
                            value_target_lv.setSelection(Math.min(lv, ExpCalcViewModel.LEVEL_MAX - 1));
                        else value_target_lv.setSelection(Math.min(ship_afterlv - 1, ExpCalcViewModel.LEVEL_MAX - 1));
                    }
                } else {
                    viewModel.setLoadFlag(false);
                }

                viewModel.setCurrentExp(current_ship_data.getAsJsonArray("api_exp").get(0).getAsInt());
                viewModel.setShipState(position);
                setScreen();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        chkbox_flagship.setOnCheckedChangeListener((compoundButton, b) -> {
            viewModel.setFlagship(b);
            setScreen();
        });

        chkbox_mvp.setOnCheckedChangeListener((compoundButton, b) -> {
            viewModel.setMvp(b);
            setScreen();
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

        for (JsonObject item : viewModel.getRestoredTrackItems()) {
            makeFilterItem(item);
        }

        loadCurrentState();
    }

    private void loadCurrentState() {
        JsonObject current_state = viewModel.getCurrentState();
        viewModel.setLoadFlag(current_state.has("current_lv") || current_state.has("target_lv"));
        if (current_state.has("ship")) {
            safeSetSelection(value_ship, current_state.get("ship").getAsInt());
        }
        if (current_state.has("current_lv")) {
            safeSetSelection(value_current_lv, current_state.get("current_lv").getAsInt());
        }
        if (current_state.has("target_lv")) {
            safeSetSelection(value_target_lv, current_state.get("target_lv").getAsInt());
        }
        if (current_state.has("map")) {
            safeSetSelection(value_map, current_state.get("map").getAsInt());
        }
        if (current_state.has("rank")) {
            safeSetSelection(value_rank, current_state.get("rank").getAsInt());
        }
        if (current_state.has("flagship")) {
            chkbox_flagship.setChecked(current_state.get("flagship").getAsBoolean());
        }
        if (current_state.has("mvp")) {
            chkbox_mvp.setChecked(current_state.get("mvp").getAsBoolean());

        }
    }

    private void setScreen() {
        ExpCalcViewModel.ScreenResult result = viewModel.calcScreen(value_base_exp.getText().toString());
        value_current_exp.setText(String.valueOf(viewModel.getCurrentExp()));
        value_target_exp.setText(String.valueOf(viewModel.getTargetExp()));

        value_mapexp.setText(String.valueOf(result.mapExp));
        value_remainexp.setText(String.valueOf(result.remainExp));
        value_counter.setText(String.valueOf(result.counter));
    }

    private void makeFilterItem(JsonObject data) {
        int target = viewModel.addTrack(data);

        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.listview_expcalc_item, null);
        v.setTag(target);

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
            ship_name_value = kc_ship_data.get("name").getAsString();
            ship_name_value = KcaApiData.getShipTranslation(ship_name_value, ship_id, false);
        }

        ship_name.setText(ship_name_value);
        level_before.setText(data.get("current_lv").getAsString());
        level_after.setText(data.get("target_lv").getAsString());
        exp_left.setText(String.valueOf(Math.max(0, data.get("target_exp").getAsInt() - data.get("current_exp").getAsInt())));
        label_battle.setText(getString(R.string.expcalc_battle));
        value_battle.setText(data.get("counter").getAsString());
        stat_area.setText(data.get("map").getAsString());
        stat_rank.setText(data.get("rank").getAsString());
        stat_flagship.setText(getString(R.string.expcalc_flagship));
        stat_mvp.setText(getString(R.string.expcalc_mvp));

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
        remove_btn.setOnClickListener(view -> removeViewByTag(target));
        listview.addView(v);
    }

    private void removeViewByTag(int tag) {
        View target = listview.findViewWithTag(tag);
        listview.removeView(target);
        viewModel.removeTrack(tag);
    }

    private void safeSetSelection(Spinner spinner, int index) {
        if (spinner == null || spinner.getAdapter() == null) return;
        int count = spinner.getAdapter().getCount();
        if (count == 0) return;
        spinner.setSelection(Math.max(0, Math.min(index, count - 1)));
    }

    @Override
    protected void onDestroy() {
        viewModel.saveTracks();
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setUI();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }
}
