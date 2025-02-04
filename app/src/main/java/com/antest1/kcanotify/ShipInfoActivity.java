package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;

import static android.widget.Toast.makeText;
import static com.antest1.kcanotify.KcaApiData.getNationalityData;
import static com.antest1.kcanotify.KcaApiData.getSpecialEquipmentInfo;
import static com.antest1.kcanotify.KcaApiData.loadShipExpInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipFilterDataFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadShipNationalityDataFromStorage;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_SHIPIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_FILTCOND;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SHIPNAT;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SHIPSTAT;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SORTKEY;
import static com.antest1.kcanotify.KcaConstants.PREF_SHIPINFO_SPEQUIPS;
import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class ShipInfoActivity extends BaseActivity {
    static final int SHIPINFO_GET_SORT_KEY = 1;
    static final int SHIPINFO_GET_FILTER_RESULT = 2;
    static final int SHIPINFO_SET_QUERY = 3;

    Toolbar toolbar;
    ListView listview;
    TextView totalcountview, totalexpview;

    KcaDBHelper dbHelper;
    Button sortButton, filterButton, searchButton;
    KcaShipListViewAdpater adapter;
    EditText searchEditText;
    Vibrator vibrator;

    String export_kanmusu_list = "";
    String export_seikuuken = "";

    boolean is_popup_on;
    boolean is_search_on;
    View export_popup, export_exit;
    Button export_clipboard_1, export_openpage_1;
    Button export_clipboard_2, export_openpage_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipinfo_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_shipinfo));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadShipFilterDataFromStorage(getApplicationContext());
        loadShipNationalityDataFromStorage(getApplicationContext());

        AssetManager assetManager = getAssets();
        int loadExpShipInfoResult = loadShipExpInfoFromAssets(assetManager);
        if (loadExpShipInfoResult != 1) {
            makeText(this, "Error loading Exp Ship Info", Toast.LENGTH_LONG).show();
            finish();
        }

        adapter = new KcaShipListViewAdpater();
        totalcountview = findViewById(R.id.shipinfo_count);
        totalexpview = findViewById(R.id.shipinfo_total_exp);
        sortButton = findViewById(R.id.shipinfo_btn_sort);
        filterButton = findViewById(R.id.shipinfo_btn_filter);
        searchButton = findViewById(R.id.shipinfo_btn_search);
        searchEditText = findViewById(R.id.shipinfo_search);

        is_popup_on = false;
        is_search_on = false;

        sortButton.setOnClickListener(view -> {
            if (!is_popup_on) {
                Intent aIntent = new Intent(ShipInfoActivity.this, ShipInfoSortActivity.class);
                startActivityForResult(aIntent, SHIPINFO_GET_SORT_KEY);
            }
        });

        filterButton.setOnClickListener(view -> {
            if (!is_popup_on) {
                Intent aIntent = new Intent(ShipInfoActivity.this, ShipInfoFilterActivity.class);
                startActivityForResult(aIntent, SHIPINFO_GET_FILTER_RESULT);
            }
        });

        searchButton.setOnClickListener(view -> {
            is_search_on = !is_search_on;
            setButtonStyle(searchButton, is_search_on);
            if (is_search_on) {
                findViewById(R.id.shipinfo_search_area).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.shipinfo_search_area).setVisibility(View.GONE);
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSearchResult(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_SHIPIFNO);
        if (data == null) data = new JsonArray();
        JsonArray deckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (deckdata == null) deckdata = new JsonArray();

        String sortkey = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SORTKEY);
        String filtcond = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_FILTCOND);
        String special_equip = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SPEQUIPS);
        String ship_status = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SHIPSTAT);
        String ship_nat = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SHIPNAT);
        setButtonStyle(filterButton, filtcond.length() > 1
                || !special_equip.trim().isEmpty()
                || !ship_status.trim().isEmpty()
                || !ship_nat.trim().isEmpty());
        adapter.setSpecialEquipment(getSpecialEquipmentInfo());
        adapter.setNationality(getNationalityData());
        adapter.setListViewItemList(data, deckdata, sortkey, filtcond, special_equip, ship_status, ship_nat);

        totalcountview.setText(KcaUtils.format(getString(R.string.shipinfo_btn_total_format), adapter.getCount()));
        totalexpview.setText(KcaUtils.format(getString(R.string.shipinfo_btn_total_exp_format), adapter.getTotalExp()));
        setButtonStyle(searchButton, is_search_on);
        findViewById(R.id.shipinfo_search_area).setVisibility(View.GONE);

        export_popup = findViewById(R.id.export_popup);
        ((TextView) export_popup.findViewById(R.id.export_title))
                .setText(getString(R.string.shipinfo_export_title));
        export_popup.setVisibility(View.GONE);

        export_exit = export_popup.findViewById(R.id.export_exit);
        ((ImageView) export_exit).setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);

        export_popup.findViewById(R.id.export_bar).setOnClickListener(v -> {
            is_popup_on = false;
            export_popup.setVisibility(View.GONE);
        });

        export_clipboard_1 = export_popup.findViewById(R.id.export_clipboard_1);
        export_clipboard_1.setText(getString(R.string.shipinfo_export_clipboard));
        export_clipboard_1.setOnClickListener(v -> {
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("text", export_kanmusu_list));
            doVibrate(vibrator, 100);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        });

        export_clipboard_2 = export_popup.findViewById(R.id.export_clipboard_2);
        export_clipboard_2.setText(getString(R.string.shipinfo_export_clipboard));
        export_clipboard_2.setOnClickListener(v -> {
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("text", export_seikuuken));
            doVibrate(vibrator, 100);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        });

        export_openpage_1 = export_popup.findViewById(R.id.export_openpage_1);
        export_openpage_1.setText(getString(R.string.shipinfo_export_openpage));
        export_openpage_1.setOnClickListener(v -> {
            String encoded = KcaUtils.encode64(export_kanmusu_list);
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://kancolle-calc.net/kanmusu_list.html?data=".concat(encoded)));
            startActivity(bIntent);
        });

        export_openpage_2 = export_popup.findViewById(R.id.export_openpage_2);
        export_openpage_2.setText(getString(R.string.shipinfo_export_openpage));
        export_openpage_2.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://noro6.github.io/kc-web/#/manager"));
            startActivity(bIntent);
        });

        listview = findViewById(R.id.shipinfo_listview);
        listview.setAdapter(adapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && adapter != null && requestCode > 0) {
            setList(requestCode);
        }
    }

    private void setList(int requestCode) {
        String sortkey = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SORTKEY);
        String filtcond = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_FILTCOND);
        String special_equip = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SPEQUIPS);
        String ship_status = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SHIPSTAT);
        String ship_nat = getStringPreferences(getApplicationContext(), PREF_SHIPINFO_SHIPNAT);
        export_popup.setVisibility(View.GONE);
        if (requestCode == SHIPINFO_GET_SORT_KEY) {
            adapter.resortListViewItem(sortkey);
        }
        else {
            JsonArray deckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
            JsonArray shipdata = dbHelper.getJsonArrayValue(DB_KEY_SHIPIFNO);
            if (deckdata == null) deckdata = new JsonArray();
            if (shipdata == null) shipdata = new JsonArray();

            adapter.setListViewItemList(shipdata, deckdata, sortkey, filtcond, special_equip, ship_status, ship_nat);
            if (requestCode == SHIPINFO_GET_FILTER_RESULT) {
                setButtonStyle(filterButton, filtcond.length() > 1
                        || !special_equip.trim().isEmpty()
                        || !ship_status.trim().isEmpty()
                        || !ship_nat.trim().isEmpty());
            }
        }
        adapter.notifyDataSetChanged();
        listview.setAdapter(adapter);
        totalcountview.setText(KcaUtils.format(getString(R.string.shipinfo_btn_total_format), adapter.getCount()));
        totalexpview.setText(KcaUtils.format(getString(R.string.shipinfo_btn_total_exp_format), adapter.getTotalExp()));
    }

    private void setSearchResult(String query) {
        adapter.setSearchQuery(query);
        setList(SHIPINFO_SET_QUERY);
    }

    private void setButtonStyle(Button button, boolean is_active) {
        if (is_active) {
            button.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
            button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            button.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnText));
            button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shipinfo, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ship_export:
                export_kanmusu_list = adapter.getKanmusuListText();
                export_seikuuken = adapter.getSeikuukenSimulatorText();
                ((TextView) export_popup.findViewById(R.id.export_content_1)).setText(export_kanmusu_list);
                ((TextView) export_popup.findViewById(R.id.export_content_2)).setText(export_seikuuken);
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

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }
}
