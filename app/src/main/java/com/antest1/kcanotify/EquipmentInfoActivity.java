package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
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

import com.antest1.kcanotify.viewmodel.EquipmentInfoViewModel;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getId;


public class EquipmentInfoActivity extends BaseActivity {
    static final int EQUIPINFO_GET_FILTER_RESULT = 2;

    Toolbar toolbar;
    ListView listview;
    Button filterBtn;
    EditText searchEditText;
    TextView totalcountview, totalstarview;

    KcaEquipListViewAdpater adapter;

    boolean is_popup_on;
    View export_popup, export_exit;
    Button export_clipboard, export_openpage2;
    Vibrator vibrator;

    EquipmentInfoViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_list);
        viewModel = new ViewModelProvider(this).get(EquipmentInfoViewModel.class);
        viewModel.init();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_equipmentinfo));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        vibrator = KcaUtils.getVibrator(this);

        is_popup_on = false;

        JsonObject itemStatTranslation = new JsonObject();
        for (String key: KcaEquipListViewAdpater.STAT_KEYS) {
            itemStatTranslation.addProperty(key, getString(getId("text_"+key, R.string.class)));
        }
        itemStatTranslation.addProperty("api_houm2", getString(R.string.text_api_houm2));
        itemStatTranslation.addProperty("api_houk2", getString(R.string.text_api_houk2));
        for (int i = 1; i <= 5; i++) {
            itemStatTranslation.addProperty("api_leng"+i, getString(getId("text_api_leng_"+i, R.string.class)));
        }

        totalcountview = findViewById(R.id.equipinfo_count);
        totalstarview = findViewById(R.id.equipinfo_total_star);

        adapter = new KcaEquipListViewAdpater();
        adapter.setViewModel(viewModel);
        adapter.setSummaryFormat(getString(R.string.equipinfo_summary));
        adapter.setStatTranslation(itemStatTranslation);
        viewModel.loadList();

        listview = findViewById(R.id.equipment_listview);
        listview.setAdapter(adapter);
        setSummary();

        filterBtn = findViewById(R.id.equipment_btn_filter);
        filterBtn.setOnClickListener(view -> {
            Intent aIntent = new Intent(EquipmentInfoActivity.this, EquipmentListFilterActivity.class);
            startActivityForResult(aIntent, EQUIPINFO_GET_FILTER_RESULT);
        });
        setFilterBtn(!viewModel.getFilterCond().equals("all"));

        searchEditText = findViewById(R.id.equipinfo_search);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
                viewModel.loadList();
                adapter.notifyDataSetChanged();
                listview.setAdapter(adapter);
                listview.invalidateViews();
                setSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        export_popup = findViewById(R.id.export_popup);
        ((TextView) export_popup.findViewById(R.id.export_title))
                .setText(getString(R.string.equipinfo_export_title));
        export_popup.setVisibility(View.GONE);

        export_exit = export_popup.findViewById(R.id.export_exit);
        ((ImageView) export_exit).setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);

        export_popup.findViewById(R.id.export_bar).setOnClickListener(v -> {
            is_popup_on = false;
            export_popup.setVisibility(View.GONE);
        });

        export_clipboard = export_popup.findViewById(R.id.export_clipboard);
        export_clipboard.setText(getString(R.string.equipinfo_export_clipboard));
        export_clipboard.setOnClickListener(v -> {
            CharSequence text = ((TextView) findViewById(R.id.export_content)).getText();
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("text", text));
            doVibrate(vibrator, 100);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        });

        export_openpage2 = export_popup.findViewById(R.id.export_openpage2);
        export_openpage2.setText(getString(R.string.equipinfo_export_openpage2));
        export_openpage2.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://noro6.github.io/kc-web/#/manager"));
            startActivity(bIntent);
        });
    }

    private void setSummary() {
        totalcountview.setText(KcaUtils.format(getString(R.string.equipinfo_btn_total_format), adapter.getTotalCount()));
        totalstarview.setText(KcaUtils.format(getString(R.string.equipinfo_btn_total_star_format), adapter.getStarCount()));
    }

    private void setFilterBtn(boolean is_active) {
        if (is_active) {
            filterBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnTextAccent));
            filterBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        } else {
            filterBtn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtnText));
            filterBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBtn));
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && adapter != null && requestCode > 0) {
            if (requestCode == EQUIPINFO_GET_FILTER_RESULT) {
                viewModel.loadList();
                setFilterBtn(!viewModel.getFilterCond().equals("all"));
            }
            adapter.notifyDataSetChanged();
            listview.setAdapter(adapter);
            setSummary();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.equipinfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_equip_export:
                String data = viewModel.getSeikuukenData().toString();
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
}
