package com.antest1.kcanotify;

import android.app.DatePickerDialog;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.antest1.kcanotify.viewmodel.ResourceLogViewModel;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.DropLogActivity.convertMillsToDate;

public class ResourceLogActivity extends BaseActivity {
    Toolbar toolbar;

    static boolean is_hidden = false;
    boolean is_exporting = false;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    KcaResourceLogPageAdapter pageAdapter;

    TextView start_date, end_date;
    TextView interval_d, interval_w, interval_m;
    Spinner interval_select;
    ImageView showhide_btn;
    int tab_position = 0;

    ResourceLogViewModel viewModel;

    public static boolean getChartHiddenState() {
        return is_hidden;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ResourceLogViewModel.class);
        viewModel.init();
        setUI(getResources().getConfiguration().orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pageAdapter.notifyItemChanged(tab_position);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setUI(newConfig.orientation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.reslog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_reslog_clear:
                AlertDialog.Builder alert = new AlertDialog.Builder(ResourceLogActivity.this);
                alert.setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
                    viewModel.clearResourceLog();
                    updateSelectedDate(viewModel.getStartTimestamp(), viewModel.getEndTimestamp());
                    dialog.dismiss();
                }).setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.cancel());
                alert.setMessage(getString(R.string.reslog_clear_dialog_message));
                alert.show();
                return true;
            case R.id.action_reslog_export:
                saveLogData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUI(int orientation) {
        setContentView(R.layout.activity_resourcelog);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_reslog));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tabLayout = findViewById(R.id.reslog_tab);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            tabLayout.addTab(tabLayout.newTab()
                    .setText(getString(R.string.reslog_label_resource_initial)));
            tabLayout.addTab(tabLayout.newTab()
                    .setText(getString(R.string.reslog_label_consumable_initial)));
        } else {
            tabLayout.addTab(tabLayout.newTab()
                    .setText(getString(R.string.reslog_label_resource)));
            tabLayout.addTab(tabLayout.newTab()
                    .setText(getString(R.string.reslog_label_consumable)));
        }
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab_position = tab.getPosition();
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                tab_position = tab.getPosition();
                viewPager.setCurrentItem(tab.getPosition());
            }
        });

        pageAdapter = new KcaResourceLogPageAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager = findViewById(R.id.reslog_pager);
        viewPager.setAdapter(pageAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                tab_position = position;
                tabLayout.setScrollPosition(position, positionOffset, true);
                Log.e("KCA", "onPageScrolled " + position);
            }
        });

        showhide_btn = findViewById(R.id.reslog_showhide);
        showhide_btn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);
        showhide_btn.setImageResource(R.drawable.ic_arrow_down);
        showhide_btn.setOnClickListener(view -> {
            is_hidden = !is_hidden;
            if (is_hidden) {
                showhide_btn.setImageResource(R.drawable.ic_arrow_up);
            } else {
                showhide_btn.setImageResource(R.drawable.ic_arrow_down);
            }
            pageAdapter.notifyItemChanged(tab_position);
        });

        start_date = findViewById(R.id.reslog_date_start);
        start_date.setOnClickListener(dateViewListener);
        end_date = findViewById(R.id.reslog_date_end);
        end_date.setOnClickListener(dateViewListener);

        interval_d = findViewById(R.id.reslog_interval_day);
        interval_d.setOnClickListener(view -> {
            if (viewModel.getIntervalValue() != ResourceLogViewModel.INTERVAL_1D) {
                viewModel.setIntervalValue(ResourceLogViewModel.INTERVAL_1D);
                setFragmentChartInfo(viewModel.getIntervalValue());
                interval_select.setSelection(ResourceLogViewModel.INTERVAL_1D);
            } else {
                pageAdapter.notifyItemChanged(tab_position);
            }
        });
        interval_w = findViewById(R.id.reslog_interval_week);
        interval_w.setOnClickListener(view -> {
            if (viewModel.getIntervalValue() != ResourceLogViewModel.INTERVAL_1W) {
                viewModel.setIntervalValue(ResourceLogViewModel.INTERVAL_1W);
                setFragmentChartInfo(viewModel.getIntervalValue());
                interval_select.setSelection(ResourceLogViewModel.INTERVAL_1W);
            } else {
                pageAdapter.notifyItemChanged(tab_position);
            }
        });
        interval_m = findViewById(R.id.reslog_interval_month);
        interval_m.setOnClickListener(view -> {
            if (viewModel.getIntervalValue() != ResourceLogViewModel.INTERVAL_1M) {
                viewModel.setIntervalValue(ResourceLogViewModel.INTERVAL_1M);
                setFragmentChartInfo(viewModel.getIntervalValue());
                interval_select.setSelection(ResourceLogViewModel.INTERVAL_1M);
            } else {
                pageAdapter.notifyItemChanged(tab_position);
            }
        });

        interval_select = findViewById(R.id.reslog_date_interval);
        final ArrayAdapter<CharSequence> interval_adapter = ArrayAdapter.createFromResource(this,
                R.array.time_interval_array, R.layout.spinner_item_14dp);
        interval_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_14dp);
        interval_select.setAdapter(interval_adapter);
        interval_select.setSelection(viewModel.getIntervalValue());
        interval_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Log.e("KCA-RL", "onItemSelected");
                viewModel.setIntervalValue(position);
                setFragmentChartInfo(viewModel.getIntervalValue());
                KcaResourcelogItemAdpater.setListViewItemList(viewModel.convertData(viewModel.getResourceLog()));
                pageAdapter.notifyItemChanged(tab_position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        start_date.setText(convertMillsToDate(viewModel.getStartTimestamp()));
        end_date.setText(convertMillsToDate(viewModel.getEndTimestamp()));
        updateSelectedDate(viewModel.getStartTimestamp(), viewModel.getEndTimestamp());
    }

    private void updateSelectedDate(long start_timestamp, long end_timestamp) {
        if (pageAdapter != null) {
            viewModel.loadResourceLogInRange(start_timestamp, end_timestamp);
            KcaResoureLogFragment.setDateInfo(start_timestamp, end_timestamp);
            KcaResourcelogItemAdpater.setListViewItemList(viewModel.convertData(viewModel.getResourceLog()));
            pageAdapter.notifyItemChanged(tab_position);
        }
    }

    public void setFragmentChartInfo(int interval_type) {
        long interval_timestamp = viewModel.getInterval(interval_type) / 8;
        switch (interval_type) {
            case ResourceLogViewModel.INTERVAL_1H:
            case ResourceLogViewModel.INTERVAL_3H:
            case ResourceLogViewModel.INTERVAL_6H:
            case ResourceLogViewModel.INTERVAL_12H:
                KcaResourcelogItemAdpater.setTimeFormat("HH:mm");
                KcaResoureLogFragment.setChartInfo(1000, 50, interval_timestamp, "HH:mm");
                break;
            case ResourceLogViewModel.INTERVAL_1D:
            case ResourceLogViewModel.INTERVAL_3D:
                KcaResourcelogItemAdpater.setTimeFormat("MM/dd");
                KcaResoureLogFragment.setChartInfo(1000, 50, interval_timestamp, "MM/dd");
                break;
            case ResourceLogViewModel.INTERVAL_1W:
            case ResourceLogViewModel.INTERVAL_2W:
                KcaResourcelogItemAdpater.setTimeFormat("MM/dd");
                KcaResoureLogFragment.setChartInfo(2000, 100, interval_timestamp, "MM/dd");
                break;
            case ResourceLogViewModel.INTERVAL_1M:
                KcaResourcelogItemAdpater.setTimeFormat("yy/MM");
                KcaResoureLogFragment.setChartInfo(5000, 200, interval_timestamp, "yy/MM");
                break;
            default:
                break;
        }
    }

    private void saveLogData() {
        Handler handler = new Handler(Looper.getMainLooper());
        TextView export_msg = findViewById(R.id.export_msg);

        is_exporting = true;
        export_msg.setText(getString(R.string.action_save_msg));
        export_msg.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ResourceLogViewModel.ExportResult exportResult = viewModel.exportCsv();

            handler.post(() -> {
                is_exporting = false;
                export_msg.setVisibility(View.GONE);
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

    View.OnClickListener dateViewListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            DatePickerDialog.OnDateSetListener listener = (v, year, monthOfYear, dayOfMonth) -> {
                boolean valid_flag = true;
                String text = KcaUtils.format("%02d-%02d-%04d", monthOfYear + 1, dayOfMonth, year);
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                    long timestamp = dateFormat.parse(text).getTime();
                    long new_value = timestamp;
                    if (view.getId() == R.id.reslog_date_start) {
                        if (new_value < viewModel.getEndTimestamp()) {
                            viewModel.setStartTimestamp(new_value);
                        } else {
                            valid_flag = false;
                        }
                    } else if (view.getId() == R.id.reslog_date_end) {
                        new_value = timestamp + (ResourceLogViewModel.DAY_MILLISECOND - 1);
                        long time_limit = KcaUtils.getCurrentDateTimestamp(System.currentTimeMillis()) + ResourceLogViewModel.DAY_MILLISECOND;
                        if (new_value > viewModel.getStartTimestamp() && new_value < time_limit) {
                            viewModel.setEndTimestamp(new_value);
                        } else {
                            valid_flag = false;
                        }
                    }
                    if (valid_flag) {
                        ((TextView) view).setText(convertMillsToDate(timestamp));
                        updateSelectedDate(viewModel.getStartTimestamp(), viewModel.getEndTimestamp());
                    }
                } catch (ParseException e) {
                    viewModel.recordDateParseError(e);
                    e.printStackTrace();
                }
            };

            long current_time = System.currentTimeMillis();
            current_time = KcaUtils.getCurrentDateTimestamp(current_time);
            if (view.getId() == R.id.droplog_date_end) current_time += (ResourceLogViewModel.DAY_MILLISECOND - 1);

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(current_time));
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dialog = new DatePickerDialog(ResourceLogActivity.this, listener, year, month, day);
            dialog.show();
        }
    };
}
