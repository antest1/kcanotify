package com.antest1.kcanotify;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import static com.antest1.kcanotify.KcaConstants.DB_KEY_EXPSHIP;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_EXPSORTIE;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class ExpCalcActivity extends AppCompatActivity {
    public static final int LEVEL_MAX = 165;
    private static final int RANK_S = 0;
    private static final int RANK_A = 1;
    private static final int RANK_B = 2;
    private static final int RANK_C = 3;
    private static final int RANK_D = 4;

    Toolbar toolbar;
    KcaDBHelper dbHelper;

    boolean is_flagship = false;
    boolean is_mvp = false;
    int sortie_val = -1;
    int rank_val = RANK_S;
    int current_exp = 0;
    int target_exp = 0;

    private boolean userIsInteracting;
    boolean shipselect_current_flag = false;

    Spinner value_ship, value_current_lv, value_target_lv, value_map, value_rank;
    TextView value_current_exp, value_target_exp, value_mapexp, value_counter, value_remainexp;
    CheckBox chkbox_flagship, chkbox_mvp;
    ArrayAdapter<String> ship_adapter, current_lv_adapter, target_lv_adapter, map_adapter, rank_adapter;

    public ExpCalcActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expcalc);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_expcalc));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        value_ship = findViewById(R.id.value_ship);
        value_current_lv = findViewById(R.id.value_current_lv);
        value_target_lv = findViewById(R.id.value_target_lv);
        value_map = findViewById(R.id.value_map);
        value_rank = findViewById(R.id.value_rank);

        value_current_exp = findViewById(R.id.value_current_exp);
        value_target_exp = findViewById(R.id.value_target_exp);
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
        sortie_val = exp_sortie_data.get(sortie_map[0]).getAsInt();

        map_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortie_map);
        map_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        value_map.setAdapter(map_adapter);
        value_map.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        sortie_val = exp_sortie_data.get(sortie_map[position]).getAsInt();
                        setScreen();
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
                        if (!shipselect_current_flag && userIsInteracting) {
                            String value = String.valueOf(position + 1);
                            JsonArray exp_data = exp_ship_data.getAsJsonArray(value);
                            current_exp = exp_data.get(1).getAsInt();
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
                        String value = String.valueOf(position + 1);
                        JsonArray exp_data = exp_ship_data.getAsJsonArray(value);
                        target_exp = exp_data.get(1).getAsInt();
                        setScreen();
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
                        int lv = shipItemList.get(position).get("api_lv").getAsInt();
                        int ship_id = shipItemList.get(position).get("api_ship_id").getAsInt();
                        JsonObject kc_ship_data = KcaApiData.getKcShipDataById(ship_id, "afterlv");
                        int ship_afterlv = kc_ship_data.get("afterlv").getAsInt();

                        shipselect_current_flag = true;
                        if (current_lv_adapter != null) {
                            value_current_lv.setSelection(lv - 1);
                        }
                        if (target_lv_adapter != null) {
                            if (lv < ship_afterlv) value_target_lv.setSelection(ship_afterlv - 1);
                            else value_target_lv.setSelection(lv);
                        }

                        current_exp = shipItemList.get(position).getAsJsonArray("api_exp").get(0).getAsInt();
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
                setScreen();
            }
        });

        chkbox_mvp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                is_mvp = b;
                setScreen();
            }
        });
    }

    private void setScreen() {
        int mapexp = sortie_val;
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
        value_current_exp.setText(String.valueOf(current_exp));
        value_target_exp.setText(String.valueOf(target_exp));

        value_mapexp.setText(String.valueOf(mapexp));
        int remainexp = Math.max(0, target_exp - current_exp);
        value_remainexp.setText(String.valueOf(remainexp));
        int left_count = (int) Math.ceil((double) remainexp / mapexp);
        value_counter.setText(String.valueOf(left_count));
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
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
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }
}
