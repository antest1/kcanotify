package com.antest1.kcanotify;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.antest1.kcanotify.DropLogActivity.convertMillsToDate;

public class ResourceLogActivity extends AppCompatActivity {
    Toolbar toolbar;
    public static final long DAY_MILLISECOND = 86400000;
    public static final int INTERVAL_6H = 0;
    public static final int INTERVAL_12H = 1;
    public static final int INTERVAL_1D = 2;
    public static final int INTERVAL_3D = 3;
    public static final int INTERVAL_1W = 4;
    public static final int INTERVAL_2W = 5;
    public static final int INTERVAL_1M = 6;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    KcaResourceLogPageAdapter pageAdapter;
    TextView start_date, end_date;
    TextView interval_d, interval_w, interval_m;
    Spinner interval_select;

    public int interval_value = INTERVAL_1D;

    public ResourceLogActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resourcelog);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_reslog));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tabLayout = findViewById(R.id.reslog_tab);
        tabLayout.addTab(tabLayout.newTab().setText(getStringWithLocale(R.string.reslog_label_resource)));
        tabLayout.addTab(tabLayout.newTab().setText(getStringWithLocale(R.string.reslog_label_consumable)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        viewPager = findViewById(R.id.reslog_pager);

        pageAdapter = new KcaResourceLogPageAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pageAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                tabLayout.setScrollPosition(position, positionOffset, true);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        start_date = findViewById(R.id.reslog_date_start);
        start_date.setOnClickListener(dateViewListener);
        end_date = findViewById(R.id.reslog_date_end);
        end_date.setOnClickListener(dateViewListener);

        interval_d = findViewById(R.id.reslog_interval_day);
        interval_d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                interval_value = INTERVAL_1D;
            }
        });
        interval_w = findViewById(R.id.reslog_interval_week);
        interval_w.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                interval_value = INTERVAL_1W;
            }
        });
        interval_m = findViewById(R.id.reslog_interval_month);
        interval_m.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                interval_value = INTERVAL_1M;
            }
        });

        interval_select = findViewById(R.id.reslog_date_interval);
        ArrayAdapter<CharSequence> interval_adapter = ArrayAdapter.createFromResource(this,
                R.array.time_interval_array, android.R.layout.simple_spinner_item);
        interval_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        interval_select.setAdapter(interval_adapter);
        interval_select.setSelection(INTERVAL_1D);
        interval_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                interval_value = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        long current_time = System.currentTimeMillis();
        current_time = KcaUtils.getCurrentDateTimestamp(current_time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        String[] time_list = dateFormat.format(new Date(current_time)).split("\\-");
        int year = Integer.parseInt(time_list[2]);
        int month = Integer.parseInt(time_list[0]);
        start_date.setText(convertMillsToDate(current_time - DAY_MILLISECOND * KcaUtils.getLastDay(year, month == 1 ? 12 : month - 1)));
        end_date.setText(convertMillsToDate(current_time + DAY_MILLISECOND - 1));

    }

    @Override
    protected void onResume() {
        super.onResume();
        pageAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    View.OnClickListener dateViewListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker v, int year, int monthOfYear, int dayOfMonth) {
                    String text = KcaUtils.format("%02d-%02d-%04d", monthOfYear + 1, dayOfMonth, year);
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                        long timestamp = dateFormat.parse(text).getTime();
                        if (view.getId() == R.id.droplog_date_start) {
                            //condition_data.addProperty("startdate", timestamp);
                        } else if (view.getId() == R.id.droplog_date_end) {
                            //condition_data.addProperty("enddate", timestamp + (DAY_MILLISECOND - 1));
                        }
                        ((TextView) view).setText(convertMillsToDate(timestamp));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            };

            long current_time = System.currentTimeMillis();
            current_time = KcaUtils.getCurrentDateTimestamp(current_time);
            if (view.getId() == R.id.droplog_date_end) current_time += (DAY_MILLISECOND - 1);

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