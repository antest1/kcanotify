package com.antest1.kcanotify;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.antest1.kcanotify.viewmodel.ExpeditionTableViewModel;
import com.google.android.material.chip.Chip;

import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;


public class ExpeditionTableActivity extends BaseActivity {

    Toolbar toolbar;
    ListView listview;

    Chip greatscbtn;
    Chip daihatsubtn;

    KcaExpeditionTableViewAdpater adapter;
    ExpeditionTableViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expeditiontable_list);
        viewModel = new ViewModelProvider(this).get(ExpeditionTableViewModel.class);
        viewModel.init();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_expdtable));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new KcaExpeditionTableViewAdpater(getBaseContext());
        adapter.setGreatSuccess(viewModel.isGreatSuccess());
        adapter.setDaihatsuCount(viewModel.getDaihatsuCount());

        for (int i = 0; i <= 7; i++) {
            final int target = i;
            TextView w_btn = findViewById(getId(KcaUtils.format("btn_w%d", i), R.id.class));
            w_btn.setOnClickListener(view -> {
                viewModel.setWorldIdx(target);
                adapter.setListViewItemList(viewModel.getExpeditionData(), viewModel.getWorldIdx());
                listview = findViewById(R.id.expeditiontable_listiview);
                listview.setAdapter(adapter);
            });
        }

        greatscbtn = findViewById(R.id.btn_success_type);
        greatscbtn.setText(viewModel.getGreatSuccessText());
        greatscbtn.setOnClickListener(view -> {
            Chip chip = (Chip) view;
            viewModel.toggleGreatSuccess();
            chip.setText(viewModel.getGreatSuccessText());
            adapter.setGreatSuccess(viewModel.isGreatSuccess());
            adapter.notifyDataSetChanged();
        });

        daihatsubtn = findViewById(R.id.btn_daihatsu);
        daihatsubtn.setText(ExpeditionTableViewModel.getDaihatsuCountString(viewModel.getDaihatsuCount()));
        daihatsubtn.setOnClickListener(view -> {
            viewModel.cycleDaihatsuCount();
            ((Chip) view).setText(ExpeditionTableViewModel.getDaihatsuCountString(viewModel.getDaihatsuCount()));
            adapter.setDaihatsuCount(viewModel.getDaihatsuCount());
            adapter.notifyDataSetChanged();
        });

        showDataLoadErrorToast(getApplicationContext(), getString(R.string.download_check_error));
        adapter.setListViewItemList(viewModel.getExpeditionData(), viewModel.getWorldIdx());
        listview = findViewById(R.id.expeditiontable_listiview);
        listview.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && adapter != null && requestCode > 0) {
            // Filter task here
            adapter.notifyDataSetChanged();
            listview.setAdapter(adapter);
        }
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
