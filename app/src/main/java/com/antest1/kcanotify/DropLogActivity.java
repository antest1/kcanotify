package com.antest1.kcanotify;

import android.app.DatePickerDialog;
import android.graphics.PorterDuff;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.antest1.kcanotify.viewmodel.DropLogViewModel;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DropLogActivity extends BaseActivity {

    Toolbar toolbar;
    KcaDroplogItemAdpater adapter;
    String[] maprank_info = new String[5];

    TextView start_date, end_date, row_count;
    Spinner sp_world, sp_map, sp_node, sp_maprank;
    ImageView showhide_btn;
    CheckBox chkbox_desc, chkbox_boss, chkbox_s, chkbox_a, chkbox_b, chkbox_x;
    boolean is_hidden = false;
    boolean is_exporting = false;
    Button btn_search;
    ListView droplog_listview;
    String[] world_list = {"*", "1", "2", "3", "4", "5", "6", "7"};

    DropLogViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_droplog_list);
        viewModel = new ViewModelProvider(this).get(DropLogViewModel.class);
        viewModel.init();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_droplog));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        world_list = viewModel.getWorldList();

        showhide_btn = findViewById(R.id.droplog_showhide);
        showhide_btn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);
        showhide_btn.setImageResource(R.drawable.ic_arrow_up);
        showhide_btn.setOnClickListener(view -> {
            is_hidden = !is_hidden;
            if (is_hidden) {
                showhide_btn.setImageResource(R.drawable.ic_arrow_down);
                findViewById(R.id.droplog_filter_area).setVisibility(View.GONE);
            } else {
                showhide_btn.setImageResource(R.drawable.ic_arrow_up);
                findViewById(R.id.droplog_filter_area).setVisibility(View.VISIBLE);
            }
        });

        maprank_info = viewModel.getMaprankInfo();
        KcaDroplogItemAdpater.color_normal = ContextCompat.getColor(getApplicationContext(), R.color.black);
        KcaDroplogItemAdpater.color_none = ContextCompat.getColor(getApplicationContext(), R.color.grey);
        KcaDroplogItemAdpater.color_item = ContextCompat.getColor(getApplicationContext(), R.color.colorListItemBack);
        KcaDroplogItemAdpater.color_item_acc = ContextCompat.getColor(getApplicationContext(), R.color.colorListItemBackAccent);

        start_date = findViewById(R.id.droplog_date_start);
        start_date.setOnClickListener(dateViewListener);
        end_date = findViewById(R.id.droplog_date_end);
        end_date.setOnClickListener(dateViewListener);

        row_count = findViewById(R.id.droplog_result_info);
        row_count.setText(KcaUtils.format(getString(R.string.droplog_total_format), 0));

        sp_world = findViewById(R.id.droplog_world);
        sp_map = findViewById(R.id.droplog_map);
        sp_node = findViewById(R.id.droplog_node);

        ArrayAdapter<CharSequence> sp_world_adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, world_list);
        sp_world_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp_world.setAdapter(sp_world_adapter);

        setSpinnerEnable(sp_world, true);
        setSpinnerEnable(sp_map, false);
        setSpinnerEnable(sp_node, false);
        sp_world.setOnItemSelectedListener(sp_world_listener);
        sp_map.setOnItemSelectedListener(sp_map_listener);
        sp_node.setOnItemSelectedListener(sp_node_listener);

        sp_maprank = findViewById(R.id.droplog_maprank);
        ArrayAdapter<CharSequence> sp_maprank_adapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, maprank_info);
        sp_maprank_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sp_maprank.setAdapter(sp_maprank_adapter);
        sp_maprank.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                viewModel.setMapRank(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        droplog_listview = findViewById(R.id.droplog_listview);
        adapter = new KcaDroplogItemAdpater();

        btn_search = findViewById(R.id.droplog_search);
        btn_search.setOnClickListener(view -> setListView());

        chkbox_s = findViewById(R.id.droplog_rank_s);
        chkbox_s.setOnCheckedChangeListener(chkboxListener);
        chkbox_a = findViewById(R.id.droplog_rank_a);
        chkbox_a.setOnCheckedChangeListener(chkboxListener);
        chkbox_b = findViewById(R.id.droplog_rank_b);
        chkbox_b.setOnCheckedChangeListener(chkboxListener);
        chkbox_x = findViewById(R.id.droplog_rank_x);
        chkbox_x.setOnCheckedChangeListener(chkboxListener);
        chkbox_boss = findViewById(R.id.droplog_isboss);
        chkbox_boss.setOnCheckedChangeListener((compoundButton, b) -> viewModel.setConditionData("isboss", b ? 1 : 0));
        chkbox_desc = findViewById(R.id.droplog_isdesc);
        chkbox_desc.setOnCheckedChangeListener((compoundButton, b) -> {
            viewModel.setConditionData("isdesc", b ? 1 : 0);
            chkbox_desc.setText(getString(b ? R.string.droplog_sort_desc : R.string.droplog_sort_asc));
        });
        chkbox_desc.setChecked(true);

        btn_search = findViewById(R.id.droplog_search);

        start_date.setText(viewModel.getStartDateText());
        end_date.setText(viewModel.getEndDateText());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.droplog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_droplog_clear:
                AlertDialog.Builder alert = new AlertDialog.Builder(DropLogActivity.this);
                alert.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
                    viewModel.clearDropLog();
                    setListView();
                    dialog.dismiss();
                }).setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.cancel());
                alert.setMessage(getString(R.string.droplog_clear_dialog_message));
                alert.show();
                return true;
            case R.id.action_droplog_export:
                saveLogData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveLogData() {
        Handler handler = new Handler(Looper.getMainLooper());

        is_exporting = true;
        row_count.setText(getString(R.string.action_save_msg));
        row_count.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPanelWarning));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            DropLogViewModel.ExportResult exportResult = viewModel.exportCsv();

            handler.post(() -> {
                is_exporting = false;
                row_count.setText(KcaUtils.format(getString(R.string.droplog_total_format), adapter.getCount()));
                row_count.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                switch (exportResult.status) {
                    case 0:
                        Toast.makeText(getApplicationContext(), "Exported to ".concat(exportResult.path), Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        Toast.makeText(getApplicationContext(), "No log to export.", Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(), "An error occurred when exporting drop log.", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            });
        });
    }

    public void setListView() {
        adapter.setListViewItemList(viewModel.getDropLog(), 0);
        droplog_listview.setAdapter(adapter);
        if (!is_exporting) row_count.setText(KcaUtils.format(getString(R.string.droplog_total_format), adapter.getCount()));
        findViewById(R.id.droplog_infoline).setVisibility(View.VISIBLE);
    }

    public void setSpinnerEnable(Spinner sp, boolean b) {
        if (sp != null) {
            sp.setEnabled(b);
        }
    }

    public static int rank_bitop(int source, int target, boolean b) {
        return DropLogViewModel.rank_bitop(source, target, b);
    }

    public static String convertMillsToDate(long timestamp) {
        return DropLogViewModel.convertMillsToDate(timestamp);
    }

    View.OnClickListener dateViewListener = view -> {
        DatePickerDialog.OnDateSetListener listener = (v, year, monthOfYear, dayOfMonth) -> {
            Long timestamp = viewModel.applyDateSelection(view.getId() == R.id.droplog_date_start,
                    year, monthOfYear, dayOfMonth);
            if (timestamp != null) {
                ((TextView) view).setText(DropLogViewModel.convertMillsToDate(timestamp));
            }
        };

        long current_time = System.currentTimeMillis();
        current_time = KcaUtils.getCurrentDateTimestamp(current_time);
        if (view.getId() == R.id.droplog_date_end) current_time += (DropLogViewModel.DAY_MILLISECOND - 1);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(current_time));
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(DropLogActivity.this, listener, year, month, day);
        dialog.show();
    };

    CompoundButton.OnCheckedChangeListener chkboxListener = (compoundButton, b) ->
            viewModel.updateRankFlag(compoundButton.getId(), b);


    AdapterView.OnItemSelectedListener sp_world_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            boolean mapEnabled = viewModel.selectWorld(position);
            if (mapEnabled) {
                ArrayAdapter<CharSequence> sp_map_adapter =
                        new ArrayAdapter<CharSequence>(getBaseContext(),
                                android.R.layout.simple_spinner_item, viewModel.getMapList());
                sp_map_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                sp_map.setAdapter(sp_map_adapter);
                setSpinnerEnable(sp_map, true);
            } else {
                setSpinnerEnable(sp_map, false);
                sp_map.setAdapter(null);
            }
            sp_node.setAdapter(null);
            setSpinnerEnable(sp_node, false);
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    AdapterView.OnItemSelectedListener sp_map_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            boolean nodeEnabled = viewModel.selectMap(position);
            if (nodeEnabled) {
                ArrayAdapter<CharSequence> sp_node_adapter =
                        new ArrayAdapter<CharSequence>(getBaseContext(),
                                android.R.layout.simple_spinner_item, viewModel.getNodeList());
                sp_node_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                sp_node.setAdapter(sp_node_adapter);
                setSpinnerEnable(sp_node, true);
            } else {
                sp_node.setAdapter(null);
                setSpinnerEnable(sp_node, false);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    AdapterView.OnItemSelectedListener sp_node_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            viewModel.selectNode(position);
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
}
