package com.antest1.kcanotify;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.antest1.kcanotify.viewmodel.AkashiDetailViewModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getJapanCalendarInstance;


public class AkashiDetailActivity extends BaseActivity {
    Toolbar toolbar;
    TextView itemNameTextView, itemImprovDefaultShipTextView, getItemImprovStat;
    TextView dmat_count, smat_count;
    TextView current_date;

    AkashiDetailViewModel viewModel;

    public static void setRequiredItemTranslation(JsonArray data) {
        AkashiDetailViewModel.setRequiredItemTranslation(data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akashi_detail);
        viewModel = new ViewModelProvider(this).get(AkashiDetailViewModel.class);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_akashi_detail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Calendar calendar = getJapanCalendarInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
        Date date = calendar.getTime();
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        current_date = findViewById(R.id.current_date);
        current_date.setText(KcaUtils.format("%s (%s)", date_format.format(date),
                getString(getId("akashi_term_day_" + dayOfWeek, R.string.class))));

        dmat_count = findViewById(R.id.count_dmat);
        smat_count = findViewById(R.id.count_smat);
        String dmat_str = viewModel.getMaterialCount(6);
        if (dmat_str != null) {
            dmat_count.setText(String.valueOf(dmat_str));
            smat_count.setText(String.valueOf(viewModel.getMaterialCount(7)));
        }

        viewModel.loadUserItems();

        if (!getIntent().hasExtra("item_id")) {
            finish();
        } else {
            itemNameTextView = findViewById(R.id.akashi_detail_name);
            itemImprovDefaultShipTextView = findViewById(R.id.akashi_improv_default_list);

            int itemid = getIntent().getIntExtra("item_id", 0);
            String itemImprovmetInfo = getIntent().getStringExtra("item_info");
            if (itemid != 0) {
                itemNameTextView.setText(viewModel.getItemName(itemid));
            } else {
                Toast.makeText(this, "??", Toast.LENGTH_LONG).show();
            }

            getItemImprovStat = findViewById(R.id.akashi_improv_status_list);
            getItemImprovStat.setText(viewModel.getImprovedCountText(itemid));

            if (itemImprovmetInfo != null && !itemImprovmetInfo.isEmpty()) {
                JsonObject itemImprovementData = viewModel.parseImprovementData(itemImprovmetInfo);
                JsonArray itemImprovementDetail = itemImprovementData.getAsJsonArray("improvement");
                JsonArray itemDefaultEquippedOn = itemImprovementData.getAsJsonArray("default_equipped_on");
                boolean itemConvertException = itemImprovementData.has("convert_exception");

                for (int i = 0; i < 3; i++) {
                    int detail_id = getId("akashi_improv_detail_" + (i + 1), R.id.class);
                    if (i < itemImprovementDetail.size()) {
                        findViewById(detail_id).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(detail_id).setVisibility(View.GONE);
                    }
                }

                for (int i = 0; i < itemImprovementDetail.size(); i++) {
                    JsonObject data = itemImprovementDetail.get(i).getAsJsonObject();
                    JsonArray resources = data.getAsJsonArray("resource");

                    JsonObject item_count = viewModel.buildItemCountForResources(resources);

                    for (int j = 1; j <= 3; j++) {
                        String[] mse_string = viewModel.getMseString(resources.get(j).getAsJsonArray(), item_count);
                        ((TextView) findViewById(getId("akashi_improv_detail_m".concat(String.valueOf(j))
                                .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(mse_string[0]);
                        ((TextView) findViewById(getId("akashi_improv_detail_s".concat(String.valueOf(j))
                                .concat("_").concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(mse_string[1]);
                        if (j == 3 && data.has("require_item")) {
                            JsonArray require_items = data.getAsJsonArray("require_item");
                            String e3 = viewModel.getRequiredItemsText(require_items, "", mse_string[2]);
                            ((TextView) findViewById(getId(KcaUtils.format("akashi_improv_detail_e%d_%d", j, i + 1), R.id.class)))
                                    .setText(e3);
                        } else if (j == 2 && data.has("require_item2")) {
                            JsonArray require_item2 = data.getAsJsonArray("require_item2");
                            String e3 = viewModel.getRequiredItems2Text(require_item2, mse_string[2]);
                            ((TextView) findViewById(getId(KcaUtils.format("akashi_improv_detail_e%d_%d", j, i + 1), R.id.class)))
                                    .setText(e3);
                        } else {
                            ((TextView) findViewById(getId(KcaUtils.format("akashi_improv_detail_e%d_%d", j, i + 1), R.id.class)))
                                    .setText(mse_string[2]);
                        }

                    }

                    ((TextView) findViewById(getId("akashi_improv_consume_".concat(String.valueOf(i + 1)), R.id.class)))
                            .setText(viewModel.getConcatString(resources.get(0).getAsJsonArray()));

                    String upgradeText = viewModel.getUpgradeText(data);
                    if (upgradeText != null) {
                        ((TextView) findViewById(getId("akashi_improv_detail_upgrade_".concat(String.valueOf(i + 1)), R.id.class)))
                                .setText(upgradeText);
                    } else {
                        findViewById(getId("akashi_improv_detail_row3_".concat(String.valueOf(i + 1)), R.id.class)).
                                setVisibility(View.GONE);
                    }

                    JsonArray req = data.get("req").getAsJsonArray();
                    ((TextView) findViewById(getId("akashi_improv_support_".concat(String.valueOf(i + 1)), R.id.class)))
                            .setText(viewModel.getSupportString(req, itemConvertException));
                }
                itemImprovDefaultShipTextView.setText(viewModel.getDefaultEquippedText(itemDefaultEquippedOn));

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
