package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.SEEK_33CN1;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;

public class FleetInfoActivity extends BaseActivity {
    static final String KC_REQ_LIST = "id,name,stype,houg,raig,tyku,souk,tais,luck,afterlv,slot_num";

    Toolbar toolbar;
    KcaDBHelper dbHelper;

    static int current_fleet = 0;
    static boolean is_portrait = true;
    TextView fleetlist_name, fleetlist_fp, fleetlist_seek, fleetlist_loading;
    ImageView fleetlist_select;
    GridView fleetlist_ships;
    KcaFleetInfoItemAdapter adapter;
    KcaDeckInfo deckInfoCalc;
    Vibrator vibrator;

    boolean is_popup_on;
    View export_popup, export_exit;
    TextView export_clipboard, export_openpage_noro6, export_openpage_jervisor, export_openpage2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fleetlist);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_fleetlist));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        vibrator = KcaUtils.getVibrator(this);

        deckInfoCalc = new KcaDeckInfo(this);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();

        final String[] deck_list = {"1", "2", "3", "4", getString(R.string.fleetview_combined)};

        fleetlist_select = findViewById(R.id.fleetlist_select);
        fleetlist_select.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);

        fleetlist_select.setOnClickListener(v -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(FleetInfoActivity.this);
            dialog.setSingleChoiceItems(deck_list, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int n) {
                    current_fleet = n;
                    loadShipItem();
                    dialog.dismiss();
                }
            });
            dialog.show();
        });

        fleetlist_name = findViewById(R.id.fleetlist_name);
        fleetlist_ships = findViewById(R.id.fleetlist_ships);
        fleetlist_fp = findViewById(R.id.fleetlist_fp);
        fleetlist_seek = findViewById(R.id.fleetlist_seek);
        fleetlist_loading = findViewById(R.id.fleetlist_loading);
        for (int i = 1; i <= 7; i++) {
            KcaFleetInfoItemAdapter.alv_format[i] = getString(getId(KcaUtils.format("alv_%d", i), R.string.class));
        }

        adapter = new KcaFleetInfoItemAdapter();
        fleetlist_ships.setAdapter(adapter);

        is_portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (is_portrait) {
            fleetlist_ships.setNumColumns(1);
        } else {
            fleetlist_ships.setNumColumns(2);
        }

        export_popup = findViewById(R.id.export_popup);
        ((TextView) export_popup.findViewById(R.id.export_title))
                .setText(getString(R.string.fleetinfo_export_title));
        export_popup.setVisibility(View.GONE);
        is_popup_on = false;

        export_exit = export_popup.findViewById(R.id.export_exit);
        ((ImageView) export_exit).setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);

        export_popup.findViewById(R.id.export_bar).setOnClickListener(v -> {
            is_popup_on = false;
            export_popup.setVisibility(View.GONE);
        });

        TextView content = findViewById(R.id.export_content);

        export_clipboard = export_popup.findViewById(R.id.export_clipboard);
        export_clipboard.setText(getString(R.string.fleetinfo_export_clipboard));
        export_clipboard.setOnClickListener(v -> {
            CharSequence text = content.getText();
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("text", text));
            doVibrate(vibrator, 100);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        });

        export_openpage_noro6 = export_popup.findViewById(R.id.export_openpage_noro6);
        export_openpage_noro6.setText(getString(R.string.fleetinfo_export_openpage_noro6));
        export_openpage_noro6.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://noro6.github.io/kc-web/#/aircalc"));
            startActivity(bIntent);
        });

        export_openpage_jervisor = export_popup.findViewById(R.id.export_openpage_jervisor);
        export_openpage_jervisor.setText(getString(R.string.fleetinfo_export_openpage_jervisor));
        export_openpage_jervisor.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://jervis.vercel.app/"));
            startActivity(bIntent);
        });

        export_openpage2 = export_popup.findViewById(R.id.export_openpage2);
        export_openpage2.setText(getString(R.string.fleetinfo_export_openpage2));
        export_openpage2.setOnClickListener(v -> {
            String data = imageBuilderData();
            String locale_code = getResourceLocaleCode();
            if (locale_code.equals("ko")) locale_code = "kr";
            if (locale_code.equals("tcn")) locale_code = "jp";
            try {
                JsonObject add_data = JsonParser.parseString(data).getAsJsonObject();
                add_data.addProperty("lang", locale_code);
                add_data.addProperty("theme", "dark");
                data = add_data.toString();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "json parsing error", Toast.LENGTH_SHORT).show();
            }

            try {
                String encoded =  URLEncoder.encode(data, "utf-8");
                Intent bIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://kancolleimgbuilder.web.app/builder#".concat(encoded)));
                startActivity(bIntent);
            } catch (UnsupportedEncodingException e) {
                Toast.makeText(getApplicationContext(), "parsing error", Toast.LENGTH_SHORT).show();
            }
        });

        loadShipItem();
        // Toast.makeText(getApplicationContext(), deckBuilderData(), Toast.LENGTH_LONG).show();
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    private void loadShipItem() {
        Handler handler = new Handler(Looper.getMainLooper());
        final String[] fleet_name = {""};
        fleetlist_loading.setVisibility(View.VISIBLE);

        JsonArray deck_data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean passed = true;
            JsonArray data;
            if (deck_data == null) {
                fleet_name[0] = "-";
                passed = false;
            } else {
                if (current_fleet == 4) {
                    fleet_name[0] = getString(R.string.fleetlist_combined_fleet);
                    if (deck_data.size() < 2) {
                        passed = false;
                    } else {
                        KcaFleetInfoItemAdapter.is_combined = true;
                        data = deckInfoCalc.getDeckListInfo(deck_data, 0, "all", KC_REQ_LIST);
                        JsonArray data_c = deckInfoCalc.getDeckListInfo(deck_data, 1, "all", KC_REQ_LIST);
                        while (!is_portrait && data.size() < 6) data.add(new JsonObject());

                        for (int i = 0; i < data_c.size(); i++) {
                            JsonObject item_c = data_c.get(i).getAsJsonObject();
                            item_c.addProperty("cb_flag", true);
                            data.add(item_c);
                        }
                        while (!is_portrait && data.size() < 12) data.add(new JsonObject());
                        adapter.setListViewItemList(data);
                    }
                } else {
                    if (deck_data.size() <= current_fleet) {
                        fleet_name[0] = String.valueOf(current_fleet + 1);
                        passed = false;
                    } else {
                        KcaFleetInfoItemAdapter.is_combined = false;
                        JsonObject fleet_data = deck_data.get(current_fleet).getAsJsonObject();
                        fleet_name[0] = fleet_data.get("api_name").getAsString();
                        data = deckInfoCalc.getDeckListInfo(deck_data, current_fleet, "all", KC_REQ_LIST);
                        adapter.setListViewItemList(data);
                    }
                }
            }

            final boolean passedFinal = passed;
            handler.post(() -> {
                fleetlist_name.setText(fleet_name[0]);
                if (!passedFinal) {
                    findViewById(R.id.fleetlist_content).setVisibility(View.GONE);
                    fleetlist_fp.setText("");
                    fleetlist_seek.setText("");
                } else {
                    if (current_fleet == 4) {
                        findViewById(R.id.fleetlist_content).setVisibility(View.VISIBLE);
                        fleetlist_fp.setText(deckInfoCalc.getAirPowerRangeString(deck_data, 0, null));
                        fleetlist_seek.setText(KcaUtils.format(getString(R.string.fleetview_seekvalue_f),
                                deckInfoCalc.getSeekValue(deck_data, "0,1", SEEK_33CN1, null)));

                    } else {
                        findViewById(R.id.fleetlist_content).setVisibility(View.VISIBLE);
                        fleetlist_fp.setText(deckInfoCalc.getAirPowerRangeString(deck_data, current_fleet, null));
                        fleetlist_seek.setText(KcaUtils.format(getString(R.string.fleetview_seekvalue_f),
                                deckInfoCalc.getSeekValue(deck_data, String.valueOf(current_fleet), SEEK_33CN1, null)));
                    }
                    adapter.notifyDataSetChanged();
                }
                fleetlist_loading.setVisibility(View.GONE);
            });
        });
    }

    public String deckBuilderData() {
        JsonObject deckbuilder = new JsonObject();
        deckbuilder.addProperty("version", 4);
        deckbuilder.addProperty("hqlv", KcaApiData.getAdmiralLevel());
        JsonArray deck_data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (deck_data != null) {
            for (int i = 0; i < deck_data.size(); i++) {
                JsonObject fleet = new JsonObject();
                JsonArray item = deckInfoCalc.getDeckListInfo(deck_data, i, "all", KC_REQ_LIST);
                for (int j = 0; j < item.size(); j++) {
                    JsonObject sitem = new JsonObject();

                    JsonObject ship = item.get(j).getAsJsonObject();
                    JsonObject user = ship.getAsJsonObject("user");
                    sitem.addProperty("id", user.get("api_ship_id").getAsString());
                    sitem.addProperty("lv", user.get("api_lv").getAsInt());
                    sitem.addProperty("luck", user.getAsJsonArray("api_lucky").get(0).getAsInt());
                    sitem.addProperty("hp", user.get("api_nowhp").getAsInt());
                    JsonObject items = new JsonObject();

                    int taisen_equip = 0;
                    JsonArray api_slot = user.getAsJsonArray("api_slot");
                    for (int k = 0; k < api_slot.size(); k++) {
                        JsonObject item_s = new JsonObject();
                        int slotid = api_slot.get(k).getAsInt();
                        if (slotid <= 0) continue;
                        JsonObject item_data = getUserItemStatusById(slotid, "slotitem_id,level,alv", "tais");
                        if (item_data != null) {
                            taisen_equip += item_data.get("tais").getAsInt();
                            int slotitme_id = item_data.get("slotitem_id").getAsInt();
                            item_s.addProperty("id", slotitme_id);
                            if (item_data.has("level")) {
                                int level = item_data.get("level").getAsInt();
                                if (level > 0) item_s.addProperty("rf", level);
                            }
                            if (item_data.has("alv")) {
                                int alv = item_data.get("alv").getAsInt();
                                if (alv > 0) item_s.addProperty("mas", alv);
                            }
                            items.add(KcaUtils.format("i%d", k + 1), item_s);
                        }
                    }

                    int ship_slot_ex = user.get("api_slot_ex").getAsInt();
                    if (ship_slot_ex > 0) {
                        JsonObject item_s = new JsonObject();
                        JsonObject ex_item_data = getUserItemStatusById(ship_slot_ex, "slotitem_id,level,alv", "tais");
                        if (ex_item_data != null) {
                            taisen_equip += ex_item_data.get("tais").getAsInt();
                            int slotitme_id = ex_item_data.get("slotitem_id").getAsInt();
                            item_s.addProperty("id", slotitme_id);
                            if (ex_item_data.has("level")) {
                                int level = ex_item_data.get("level").getAsInt();
                                if (level > 0) item_s.addProperty("rf", level);
                            }
                            if (ex_item_data.has("alv")) {
                                int alv = ex_item_data.get("alv").getAsInt();
                                if (alv > 0) item_s.addProperty("mas", alv);
                            }
                            items.add("ix", item_s);
                        }
                    }


                    sitem.addProperty("asw", user.getAsJsonArray("api_taisen").get(0).getAsInt() - taisen_equip);
                    sitem.add("items", items);
                    fleet.add(KcaUtils.format("s%d", j + 1), sitem);
                }
                deckbuilder.add(KcaUtils.format("f%d", i + 1), fleet);
            }
        }
        return deckbuilder.toString();
    }

    public String imageBuilderData() {
        JsonObject builder = new JsonObject();
        builder.addProperty("hqlv", KcaApiData.getAdmiralLevel());

        JsonArray deck_data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (deck_data != null) {
            for (int i = 0; i < deck_data.size(); i++) {
                JsonObject fleet = new JsonObject();
                JsonArray item = deckInfoCalc.getDeckListInfo(deck_data, i, "all", KC_REQ_LIST);
                for (int j = 0; j < item.size(); j++) {
                    JsonObject sitem = new JsonObject();

                    JsonObject ship = item.get(j).getAsJsonObject();
                    JsonObject user = ship.getAsJsonObject("user");
                    sitem.addProperty("id", user.get("api_ship_id").getAsString());
                    sitem.addProperty("lv", user.get("api_lv").getAsInt());
                    sitem.addProperty("luck", user.getAsJsonArray("api_lucky").get(0).getAsInt());
                    sitem.addProperty("hp", user.get("api_nowhp").getAsInt());
                    JsonObject items = new JsonObject();

                    JsonArray api_slot = user.getAsJsonArray("api_slot");
                    for (int k = 0; k < api_slot.size(); k++) {
                        JsonObject item_s = new JsonObject();
                        int slotid = api_slot.get(k).getAsInt();
                        if (slotid <= 0) continue;
                        JsonObject item_data = getUserItemStatusById(slotid, "slotitem_id,level,alv", "");
                        if (item_data != null) {
                            int slotitme_id = item_data.get("slotitem_id").getAsInt();
                            item_s.addProperty("id", slotitme_id);
                            if (item_data.has("level")) {
                                int level = item_data.get("level").getAsInt();
                                if (level > 0) item_s.addProperty("rf", level);
                            }
                            if (item_data.has("alv")) {
                                int alv = item_data.get("alv").getAsInt();
                                if (alv > 0) item_s.addProperty("mas", alv);
                            }
                            items.add(KcaUtils.format("i%d", k + 1), item_s);
                        }
                    }

                    int ship_slot_ex = user.get("api_slot_ex").getAsInt();
                    if (ship_slot_ex > 0) {
                        JsonObject item_s = new JsonObject();
                        JsonObject ex_item_data = getUserItemStatusById(ship_slot_ex, "slotitem_id,level,alv", "");
                        if (ex_item_data != null) {
                            int slotitme_id = ex_item_data.get("slotitem_id").getAsInt();
                            item_s.addProperty("id", slotitme_id);
                            if (ex_item_data.has("level")) {
                                int level = ex_item_data.get("level").getAsInt();
                                if (level > 0) item_s.addProperty("rf", level);
                            }
                            if (ex_item_data.has("alv")) {
                                int alv = ex_item_data.get("alv").getAsInt();
                                if (alv > 0) item_s.addProperty("mas", alv);
                            }
                            items.add("ix", item_s);
                        }
                    }

                    sitem.addProperty("asw", user.getAsJsonArray("api_taisen").get(0).getAsInt());
                    sitem.add("items", items);
                    sitem.addProperty("fp", user.getAsJsonArray("api_karyoku").get(0).getAsInt());
                    sitem.addProperty("tp", user.getAsJsonArray("api_raisou").get(0).getAsInt());
                    sitem.addProperty("aa", user.getAsJsonArray("api_taiku").get(0).getAsInt());
                    sitem.addProperty("ar", user.getAsJsonArray("api_soukou").get(0).getAsInt());
                    sitem.addProperty("ev", user.getAsJsonArray("api_kaihi").get(0).getAsInt());
                    sitem.addProperty("los", user.getAsJsonArray("api_sakuteki").get(0).getAsInt());
                    fleet.add(KcaUtils.format("s%d", j + 1), sitem);
                }
                builder.add(KcaUtils.format("f%d", i + 1), fleet);
            }
        }
        return builder.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fleetinfo, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_fleet_export:
                String data = deckBuilderData();
                ((TextView) export_popup.findViewById(R.id.export_content)).setText(data);
                is_popup_on = true;
                export_popup.setVisibility(View.VISIBLE);
                return true;
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
        is_portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        if (is_portrait) {
            fleetlist_ships.setNumColumns(1);
        } else {
            fleetlist_ships.setNumColumns(2);
        }
        fleetlist_ships.invalidateViews();
    }
}
