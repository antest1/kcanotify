package com.antest1.kcanotify;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.DropLogActivity.convertMillsToDate;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_RESLOG;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_RESOURCELOG_VERSION;

public class ResourceLogActivity extends AppCompatActivity {
    Toolbar toolbar;
    private final String FILE_PATH = "/export_data";
    public static final long DAY_MILLISECOND = 86400000;

    public static final int INTERVAL_1H = 0;
    public static final int INTERVAL_3H = 1;
    public static final int INTERVAL_6H = 2;
    public static final int INTERVAL_12H = 3;
    public static final int INTERVAL_1D = 4;
    public static final int INTERVAL_3D = 5;
    public static final int INTERVAL_1W = 6;
    public static final int INTERVAL_2W = 7;
    public static final int INTERVAL_1M = 8;

    static boolean is_hidden = false;
    boolean is_exporting = false;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    KcaResourceLogPageAdapter pageAdapter;

    KcaDBHelper dbHelper;
    KcaResourceLogger resourceLogger;
    List<JsonObject> resourceLog;
    TextView start_date, end_date;
    TextView interval_d, interval_w, interval_m;
    TextView export_msg;
    Spinner interval_select;
    ImageView showhide_btn;
    int tab_position = 0;
    long start_timestamp, end_timestamp;

    public int interval_value = INTERVAL_1D;

    public ResourceLogActivity() {
        LocaleUtils.updateConfig(this);
    }

    public static boolean getChartHiddenState() {
        return is_hidden;
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

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        resourceLogger = new KcaResourceLogger(getApplicationContext(), null, KCANOTIFY_RESOURCELOG_VERSION);
        KcaApiData.setDBHelper(dbHelper);

        tabLayout = findViewById(R.id.reslog_tab);
        tabLayout.addTab(tabLayout.newTab().setText(getStringWithLocale(R.string.reslog_label_resource)));
        tabLayout.addTab(tabLayout.newTab().setText(getStringWithLocale(R.string.reslog_label_consumable)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        export_msg = findViewById(R.id.export_msg);
        viewPager = findViewById(R.id.reslog_pager);

        pageAdapter = new KcaResourceLogPageAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pageAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                tab_position = position;
                tabLayout.setScrollPosition(position, positionOffset, true);
                Log.e("KCA", "onPageScrolled " + position);
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

        showhide_btn = findViewById(R.id.reslog_showhide);
        showhide_btn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);
        showhide_btn.setImageResource(R.mipmap.ic_arrow_down);
        showhide_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_hidden = !is_hidden;
                if (is_hidden) {
                    showhide_btn.setImageResource(R.mipmap.ic_arrow_up);
                } else {
                    showhide_btn.setImageResource(R.mipmap.ic_arrow_down);
                }
                pageAdapter.notifyDataSetChanged();
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
                if (interval_value != INTERVAL_1D) {
                    interval_value = INTERVAL_1D;
                    setFragmentChartInfo(interval_value);
                    interval_select.setSelection(INTERVAL_1D);
                } else {
                    pageAdapter.notifyDataSetChanged();
                }
            }
        });
        interval_w = findViewById(R.id.reslog_interval_week);
        interval_w.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (interval_value != INTERVAL_1W) {
                    interval_value = INTERVAL_1W;
                    setFragmentChartInfo(interval_value);
                    interval_select.setSelection(INTERVAL_1W);
                } else {
                    pageAdapter.notifyDataSetChanged();
                }
            }
        });
        interval_m = findViewById(R.id.reslog_interval_month);
        interval_m.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (interval_value != INTERVAL_1M) {
                    interval_value = INTERVAL_1M;
                    setFragmentChartInfo(interval_value);
                    interval_select.setSelection(INTERVAL_1M);
                } else {
                    pageAdapter.notifyDataSetChanged();
                }
            }
        });

        interval_select = findViewById(R.id.reslog_date_interval);
        final ArrayAdapter<CharSequence> interval_adapter = ArrayAdapter.createFromResource(this,
                R.array.time_interval_array, R.layout.spinner_item_14dp);
        interval_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_14dp);
        interval_select.setAdapter(interval_adapter);
        interval_select.setSelection(INTERVAL_1D);
        interval_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Log.e("KCA-RL", "onItemSelected");
                interval_value = position;
                setFragmentChartInfo(interval_value);
                KcaResourcelogItemAdpater.setListViewItemList(convertData(resourceLog));
                pageAdapter.notifyDataSetChanged();
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
        start_timestamp = current_time - DAY_MILLISECOND * KcaUtils.getLastDay(year, month == 1 ? 12 : month - 1);
        end_timestamp = current_time + DAY_MILLISECOND - 1;

        start_date.setText(convertMillsToDate(start_timestamp));
        end_date.setText(convertMillsToDate(end_timestamp));

        resourceLog = resourceLogger.getResourceLogInRange(start_timestamp, end_timestamp);
        KcaResourcelogItemAdpater.setListViewItemList(convertData(resourceLog));
        pageAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pageAdapter.notifyDataSetChanged();
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
                alert.setPositiveButton(getStringWithLocale(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resourceLogger.clearResoureLog();
                        resourceLog = resourceLogger.getResourceLogInRange(start_timestamp, end_timestamp);
                        KcaResourcelogItemAdpater.setListViewItemList(convertData(resourceLog));
                        pageAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                }).setNegativeButton(getStringWithLocale(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.setMessage(getStringWithLocale(R.string.reslog_clear_dialog_message));
                alert.show();
                return true;
            case R.id.action_reslog_export:
                new LogSaveTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public long getInterval(int interval_value) {
        switch (interval_value) {
            case INTERVAL_1H:
                return DAY_MILLISECOND / 24;
            case INTERVAL_3H:
                return DAY_MILLISECOND / 8;
            case INTERVAL_6H:
                return DAY_MILLISECOND / 4;
            case INTERVAL_12H:
                return DAY_MILLISECOND / 2;
            case INTERVAL_1D:
                return DAY_MILLISECOND;
            case INTERVAL_3D:
                return DAY_MILLISECOND * 3;
            case INTERVAL_1W:
                return DAY_MILLISECOND * 7;
            case INTERVAL_2W:
                return DAY_MILLISECOND * 12;
            case INTERVAL_1M:
                return DAY_MILLISECOND * 30;
            default:
                return DAY_MILLISECOND;
        }
    }

    public void setFragmentChartInfo(int interval_type) {
        long interval_timestamp = getInterval(interval_type) / 8;
        switch (interval_type) {
            case INTERVAL_1H:
            case INTERVAL_3H:
            case INTERVAL_6H:
            case INTERVAL_12H:
                KcaResourcelogItemAdpater.setTimeFormat("HH:mm");
                KcaResoureLogFragment.setChartInfo(1000, 50, interval_timestamp, "HH:mm");
                break;
            case INTERVAL_1D:
            case INTERVAL_3D:
                KcaResourcelogItemAdpater.setTimeFormat("MM/dd");
                KcaResoureLogFragment.setChartInfo(1000, 50, interval_timestamp, "MM/dd");
                break;
            case INTERVAL_1W:
            case INTERVAL_2W:
                KcaResourcelogItemAdpater.setTimeFormat("MM/dd");
                KcaResoureLogFragment.setChartInfo(2000, 100, interval_timestamp, "MM/dd");
                break;
            case INTERVAL_1M:
                KcaResourcelogItemAdpater.setTimeFormat("yy/MM");
                KcaResoureLogFragment.setChartInfo(5000, 200, interval_timestamp, "yy/MM");
                break;
            default:
                break;
        }
    }

    private class LogSaveTask extends AsyncTask<String, String, Integer> {
        File file;

        @Override
        protected void onPreExecute() {
            is_exporting = true;
            export_msg.setText(getStringWithLocale(R.string.action_save_msg));
            export_msg.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String[] params) {
            File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(FILE_PATH));
            if (!savedir.exists()) savedir.mkdirs();
            String exportPath = savedir.getPath();

            String label_date = getStringWithLocale(R.string.reslog_label_time);
            String label_1 = getStringWithLocale(R.string.item_fuel);
            String label_2 = getStringWithLocale(R.string.item_ammo);
            String label_3 = getStringWithLocale(R.string.item_stel);
            String label_4 = getStringWithLocale(R.string.item_baux);
            String label_5 = getStringWithLocale(R.string.item_bgtz);
            String label_6 = getStringWithLocale(R.string.item_brnr);
            String label_7 = getStringWithLocale(R.string.item_mmat);
            String label_8 = getStringWithLocale(R.string.item_kmat);

            String label_line = KcaUtils.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    label_date, label_1, label_2, label_3, label_4, label_5, label_6, label_7, label_8);

            List<String> loglist = resourceLogger.getFullStringResourceLog();
            if (loglist.size() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String timetext = dateFormat.format(new Date());

                String filename = KcaUtils.format("/kca_resourcelog_%s.csv", timetext);
                file = new File(exportPath.concat(filename));
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
                    bw.write(label_line);
                    bw.write("\r\n");
                    for (String line : loglist) {
                        bw.write(line);
                        bw.write("\r\n");
                    }
                    bw.close();
                    return 0;
                } catch (IOException e) {
                    e.printStackTrace();
                    return 2;
                }
            } else {
                return 1;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            is_exporting = false;
            export_msg.setVisibility(View.GONE);
            switch (result) {
                case 0:
                    Toast.makeText(getApplicationContext(), "Exported to ".concat(file.getPath()), Toast.LENGTH_LONG).show();
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
        }
    }

    public List<JsonObject> convertData(List<JsonObject> data) {
        List<JsonObject> new_data = new ArrayList<>();
        if (data.size() == 0) return new_data;

        List<Long> timestamp_list = new ArrayList<>();
        long start = KcaUtils.getCurrentDateTimestamp(start_timestamp);
        long end = KcaUtils.getCurrentDateTimestamp(end_timestamp);
        long interval = getInterval(interval_value);
        for (JsonObject item: data) {
            timestamp_list.add(item.get("timestamp").getAsLong());
        }

        int count = 0;
        long time = start;
        while (time < end) {
            int recent_idx = 0;
            for (int k = 0; k < timestamp_list.size(); k++) {
                long ts = timestamp_list.get(k);
                if (ts >= time) {
                    break;
                } else {
                    recent_idx = k;
                }
            }
            JsonObject item = KcaUtils.getJsonObjectCopy(data.get(recent_idx));
            item.addProperty("timestamp", time);
            new_data.add(item);

            time += interval;
            count += 1;
        }
        JsonObject last_item = KcaUtils.getJsonObjectCopy(data.get(data.size() - 1));
        last_item.addProperty("timestamp", end);
        new_data.add(last_item);
        return new_data;
    }


    View.OnClickListener dateViewListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {

                @Override
                public void onDateSet(DatePicker v, int year, int monthOfYear, int dayOfMonth) {
                    boolean valid_flag = true;
                    String text = KcaUtils.format("%02d-%02d-%04d", monthOfYear + 1, dayOfMonth, year);
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                        long timestamp = dateFormat.parse(text).getTime();
                        long new_value = timestamp;
                        if (view.getId() == R.id.reslog_date_start) {
                            if (new_value < end_timestamp) {
                                start_timestamp = new_value;
                            } else {
                                valid_flag = false;
                            }
                        } else if (view.getId() == R.id.reslog_date_end) {
                            new_value = timestamp + (DAY_MILLISECOND - 1);
                            long time_limit = KcaUtils.getCurrentDateTimestamp(System.currentTimeMillis()) + DAY_MILLISECOND;
                            if (new_value > start_timestamp && new_value < time_limit) {
                                end_timestamp = new_value;
                            } else {
                                valid_flag = false;
                            }
                        }
                        if (valid_flag) {
                            ((TextView) view).setText(convertMillsToDate(timestamp));
                            resourceLog = resourceLogger.getResourceLogInRange(start_timestamp, end_timestamp);
                            KcaResourcelogItemAdpater.setListViewItemList(convertData(resourceLog));
                            pageAdapter.notifyDataSetChanged();
                        }
                    } catch (ParseException e) {
                        dbHelper.recordErrorLog(ERROR_TYPE_RESLOG, "", "", "", KcaUtils.getStringFromException(e));
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