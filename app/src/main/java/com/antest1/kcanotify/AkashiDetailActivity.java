package com.antest1.kcanotify;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.findRemodelLv;
import static com.antest1.kcanotify.KcaApiData.getItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaApiData.removeKai;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;


public class AkashiDetailActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    TextView itemNameTextView, itemImprovDefaultShipTextView;
    JsonObject itemImprovementData;

    public AkashiDetailActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akashi_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_akashi_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().hasExtra("item_id")) {
            finish();
        } else {
            itemNameTextView = (TextView) findViewById(R.id.akashi_detail_name);
            itemImprovDefaultShipTextView = (TextView) findViewById(R.id.akashi_improv_default_list);

            int itemid = getIntent().getIntExtra("item_id", 0);
            String itemImprovmetInfo = getIntent().getStringExtra("item_info");
            String kcItemName = "";
            if (itemid != 0) {
                JsonObject kcItemData = getKcItemStatusById(itemid, "type,name");
                kcItemName = getItemTranslation(kcItemData.get("name").getAsString());
                itemNameTextView.setText(kcItemName);
            } else {
                Toast.makeText(this, "??", Toast.LENGTH_LONG).show();
            }

            if (itemImprovmetInfo.length() > 0) {
                itemImprovementData = new JsonParser().parse(itemImprovmetInfo).getAsJsonObject();
                JsonArray itemImprovementDetail = itemImprovementData.getAsJsonArray("improvement");
                JsonArray itemDefaultEquippedOn = itemImprovementData.getAsJsonArray("default_equipped_on");
                boolean itemConvertException = itemImprovementData.has("convert_exception");
                if (itemImprovementDetail.size() < 2) {
                    findViewById(R.id.akashi_improv_detail_2).setVisibility(View.GONE);
                }
                for (int i = 0; i < itemImprovementDetail.size(); i++) {
                    JsonObject data = itemImprovementDetail.get(i).getAsJsonObject();
                    JsonArray resources = data.getAsJsonArray("resource");

                    for (int j = 1; j <= 3; j++) {
                        String[] mse_string = getMseString(resources.get(j).getAsJsonArray());
                        ((TextView) findViewById(getId("akashi_improv_detail_m".concat(String.valueOf(j))
                                .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(mse_string[0]);
                        ((TextView) findViewById(getId("akashi_improv_detail_s".concat(String.valueOf(j))
                                .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(mse_string[1]);
                        if (j == 3 && data.has("require_item")) {
                            String e3 = mse_string[2];
                            JsonArray require_item = data.getAsJsonArray("require_item");
                            String require_item_name;
                            int require_item_count = require_item.get(1).getAsInt();
                            switch (require_item.get(0).getAsInt()) {
                                case 1:
                                    require_item_name = getStringWithLocale(R.string.item_engine);
                                    break;
                                case 2:
                                    require_item_name = getStringWithLocale(R.string.item_gmi_material);
                                    break;
                                case 3:
                                    require_item_name = getStringWithLocale(R.string.item_skilled_crew);
                                    break;
                                default:
                                    require_item_name = "";
                                    break;
                            }

                            e3 = KcaUtils.format("%s\n%s x %d", e3, require_item_name, require_item_count);
                            ((TextView) findViewById(getId("akashi_improv_detail_e".concat(String.valueOf(j))
                                    .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                    .setText(e3);
                        } else {
                            ((TextView) findViewById(getId("akashi_improv_detail_e".concat(String.valueOf(j))
                                    .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                    .setText(mse_string[2]);
                        }

                    }

                    ((TextView) findViewById(getId("akashi_improv_consume_".concat(String.valueOf(i + 1)), R.id.class)))
                            .setText(getConcatString(resources.get(0).getAsJsonArray()));

                    JsonElement upgrade = data.get("upgrade");
                    if (upgrade.isJsonArray()) {
                        int upgradeItemId = upgrade.getAsJsonArray().get(0).getAsInt();
                        int upgradeItemStar = upgrade.getAsJsonArray().get(1).getAsInt();
                        String upgradeItemName = getItemTranslation(getKcItemStatusById(upgradeItemId, "name").get("name").getAsString());
                        String upgradeStarString = "";
                        if (upgradeItemStar > 0) {
                            upgradeStarString = upgradeStarString.concat(" ").concat("★").concat(String.valueOf(upgradeItemStar));
                        }
                        ((TextView) findViewById(getId("akashi_improv_detail_upgrade_".concat(String.valueOf(i + 1)), R.id.class)))
                                .setText("→ ".concat(upgradeItemName.concat(upgradeStarString)));
                    } else {
                        findViewById(getId("akashi_improv_detail_row3_".concat(String.valueOf(i + 1)), R.id.class)).
                                setVisibility(View.GONE);
                    }

                    JsonArray req = data.get("req").getAsJsonArray();
                    ((TextView) findViewById(getId("akashi_improv_support_".concat(String.valueOf(i + 1)), R.id.class)))
                            .setText(getSupportString(req, itemConvertException));
                }
                List<String> shipList = new ArrayList<>();
                List<String> shipNameList = new ArrayList<>();
                JsonObject shipName = new JsonObject();

                if (itemDefaultEquippedOn != null) {
                    for (JsonElement element : itemDefaultEquippedOn) {
                        String shipId = element.getAsString();
                        shipList.add(shipId);
                        JsonObject kcShipData = getKcShipDataById(Integer.parseInt(shipId), "name");
                        shipName.addProperty(shipId, getShipTranslation(kcShipData.get("name").getAsString(), false));
                    }
                    if (shipList.size() > 0) {
                        JsonObject shipRemovelLv = findRemodelLv(joinStr(shipList, ","));
                        for (int i = 0; i < shipList.size(); i++) {
                            String id = shipList.get(i);
                            shipNameList.add(KcaUtils.format("%s(%d)", shipName.get(id).getAsString(), shipRemovelLv.get(id).getAsInt()));
                        }
                        itemImprovDefaultShipTextView.setText(joinStr(shipNameList, " / "));
                    } else {
                        itemImprovDefaultShipTextView.setText("-");
                    }
                } else {
                    itemImprovDefaultShipTextView.setText("-");
                }

            } else {
                Toast.makeText(this, "??", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
       finish();
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

    private String getConcatString(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(e.getAsString());
        }
        return joinStr(list, " / ");
    }

    private String[] getMseString(JsonArray array) {
        String[] value_list = new String[6];
        for (int i = 0; i < array.size(); i++) {
            value_list[i] = array.get(i).getAsString();
            if (i >= 4 && value_list[i].equals("0")) {
                value_list[i] = "-";
            } else if (value_list[i].equals("-1")) {
                value_list[i] = "?";
            }
        }

        if (!value_list[4].equals("-")) {
            String equipName = getItemTranslation(getKcItemStatusById(Integer.parseInt(value_list[4]), "name")
                    .get("name").getAsString());
            value_list[4] = equipName;
        }

        String material_data = value_list[0].concat(" / ").concat(value_list[1]);
        String screw_data = value_list[2].concat(" / ").concat(value_list[3]);
        String equip_data = "";
        if (value_list[4].equals("-")) {
            equip_data = value_list[4];
        } else {
            equip_data = value_list[4].concat("×").concat(value_list[5]);
        }
        String[] result = {material_data, screw_data, equip_data};
        return result;
    }


    private String getSupportString(JsonArray array, boolean exception) {
        String supportString = "";
        for (int i = 0; i < array.size(); i++) {
            List<String> daylist = new ArrayList<>();
            JsonArray item = array.get(i).getAsJsonArray();
            if (item.get(1).isJsonArray()) {
                JsonArray day = item.get(0).getAsJsonArray();
                for (int j = 0; j < day.size(); j++) {
                    if (day.get(j).getAsBoolean()) {
                        daylist.add(getStringWithLocale(getId("akashi_term_day_".concat(String.valueOf(j)), R.string.class)));
                    }
                }
                String daytext = joinStr(daylist, ",");

                int[] shiplist = removeKai(item.get(1).getAsJsonArray(), exception);
                for (int j = 0; j < shiplist.length; j++) {
                    JsonObject kcShipData = getKcShipDataById(shiplist[j], "name");
                    String shipname = getShipTranslation(kcShipData.get("name").getAsString(), false);
                    supportString = supportString.concat(KcaUtils.format("%s(%s)", shipname, daytext)).concat("\n");
                }
            } else {
                supportString = "-";
            }

        }
        return supportString.trim();
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
            LocaleUtils.setLocale(KcaApplication.defaultLocale);
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        loadTranslationData(getAssets(), getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}
