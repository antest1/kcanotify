package com.antest1.kcanotify;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
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

import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DROPLOG_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class DropLogActivity extends AppCompatActivity {
    private final String FILE_PATH = "/export_data";
    public static final long DAY_MILLISECOND = 86400000;
    public static final String[] world_list = {"*", "1", "2", "3", "4", "5", "6", "7", "41", "42", "43", "44"};

    public static final int RANK_S = 32;
    public static final int RANK_A = 16;
    public static final int RANK_B = 8;
    public static final int RANK_C = 4;
    public static final int RANK_D = 2;
    public static final int RANK_E = 1;

    Toolbar toolbar;
    KcaDBHelper dbHelper;
    KcaDropLogger dropLogger;
    KcaDroplogItemAdpater adapter;
    String[] maprank_info = new String[5];
    String[] map_list = {};
    String[] node_list = {};

    JsonObject condition_data = new JsonObject();
    TextView start_date, end_date, row_count;
    Spinner sp_world, sp_map, sp_node, sp_maprank;
    ImageView showhide_btn;
    CheckBox chkbox_desc, chkbox_boss, chkbox_s, chkbox_a, chkbox_b, chkbox_x;
    boolean is_hidden = false;
    boolean is_exporting = false;
    int rank_flag = RANK_S | RANK_A | RANK_B;
    int current_world, current_map, current_node;
    Button btn_search;
    ListView droplog_listview;

    public DropLogActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_droplog_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_droplog));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        dropLogger = new KcaDropLogger(getApplicationContext(), null, KCANOTIFY_DROPLOG_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadTranslationData(getApplicationContext());

        showhide_btn = findViewById(R.id.droplog_showhide);
        showhide_btn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);
        showhide_btn.setImageResource(R.mipmap.ic_arrow_up);
        showhide_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_hidden = !is_hidden;
                if (is_hidden) {
                    showhide_btn.setImageResource(R.mipmap.ic_arrow_down);
                    findViewById(R.id.droplog_filter_area).setVisibility(View.GONE);
                } else {
                    showhide_btn.setImageResource(R.mipmap.ic_arrow_up);
                    findViewById(R.id.droplog_filter_area).setVisibility(View.VISIBLE);
                }
            }
        });

        maprank_info[0] = "-";
        for (int i = 1; i < maprank_info.length; i++) {
            maprank_info[i] = getStringWithLocale(KcaUtils.getId("maprank_" + String.valueOf(i), R.string.class));
        }
        KcaDropLogger.maprank_info = maprank_info;
        KcaDropLogger.ship_none = getStringWithLocale(R.string.droplog_ship_none);
        KcaDropLogger.ship_full = getStringWithLocale(R.string.droplog_ship_full);
        KcaDroplogItemAdpater.color_normal = ContextCompat.getColor(getApplicationContext(), R.color.black);
        KcaDroplogItemAdpater.color_none = ContextCompat.getColor(getApplicationContext(), R.color.grey);
        KcaDroplogItemAdpater.color_item = ContextCompat.getColor(getApplicationContext(), R.color.colorListItemBack);
        KcaDroplogItemAdpater.color_item_acc = ContextCompat.getColor(getApplicationContext(), R.color.colorListItemBackAccent);

        start_date = findViewById(R.id.droplog_date_start);
        start_date.setOnClickListener(dateViewListener);
        end_date = findViewById(R.id.droplog_date_end);
        end_date.setOnClickListener(dateViewListener);

        row_count = findViewById(R.id.droplog_result_info);
        row_count.setText(KcaUtils.format(getStringWithLocale(R.string.droplog_total_format), 0));

        sp_world = findViewById(R.id.droplog_world);
        sp_map = findViewById(R.id.droplog_map);
        sp_node = findViewById(R.id.droplog_node);

        ArrayAdapter<CharSequence> sp_world_adapter =
                new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, world_list);
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
                condition_data.addProperty("maprank", position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        droplog_listview = findViewById(R.id.droplog_listview);
        adapter = new KcaDroplogItemAdpater();

        btn_search = findViewById(R.id.droplog_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setListView();
            }
        });

        chkbox_s = findViewById(R.id.droplog_rank_s);
        chkbox_s.setOnCheckedChangeListener(chkboxListener);
        chkbox_a = findViewById(R.id.droplog_rank_a);
        chkbox_a.setOnCheckedChangeListener(chkboxListener);
        chkbox_b = findViewById(R.id.droplog_rank_b);
        chkbox_b.setOnCheckedChangeListener(chkboxListener);
        chkbox_x = findViewById(R.id.droplog_rank_x);
        chkbox_x.setOnCheckedChangeListener(chkboxListener);
        chkbox_boss = findViewById(R.id.droplog_isboss);
        chkbox_boss.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setConditionData("isboss", b ? 1 : 0);
            }
        });
        chkbox_desc = findViewById(R.id.droplog_isdesc);
        chkbox_desc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setConditionData("isdesc", b ? 1 : 0);
                chkbox_desc.setText(getStringWithLocale(b ? R.string.droplog_sort_desc : R.string.droplog_sort_asc));
            }
        });
        chkbox_desc.setChecked(true);

        btn_search = findViewById(R.id.droplog_search);

        long current_time = System.currentTimeMillis();
        current_time = KcaUtils.getCurrentDateTimestamp(current_time);

        start_date.setText(convertMillsToDate(current_time - DAY_MILLISECOND * 7));
        end_date.setText(convertMillsToDate(current_time + DAY_MILLISECOND - 1));

        current_world = 0;
        current_map = 0;
        current_node = 0;

        condition_data.addProperty("startdate", current_time - DAY_MILLISECOND * 7); // 7 days before
        condition_data.addProperty("enddate", current_time + DAY_MILLISECOND - 1);
        condition_data.addProperty("isboss", 0);
        condition_data.addProperty("isdesc", 1);
        condition_data.addProperty("maprank", 0);
        condition_data.addProperty("rank", "B,A,S");
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                alert.setPositiveButton(getStringWithLocale(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dropLogger.clearDropLog();
                        setListView();
                        dialog.dismiss();
                    }
                }).setNegativeButton(getStringWithLocale(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.setMessage(getStringWithLocale(R.string.droplog_clear_dialog_message));
                alert.show();
                return true;
            case R.id.action_droplog_export:
                new LogSaveTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class LogSaveTask extends AsyncTask<String, String, Integer> {
        File file;

        @Override
        protected void onPreExecute() {
            is_exporting = true;
            row_count.setText(getStringWithLocale(R.string.action_save_msg));
            row_count.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPanelWarning));
        }

        @Override
        protected Integer doInBackground(String[] params) {
            File savedir = new File(getExternalFilesDir(null).getAbsolutePath().concat(FILE_PATH));
            if (!savedir.exists()) savedir.mkdirs();
            String exportPath = savedir.getPath();

            String label_date = getStringWithLocale(R.string.droplog_item_label_time);
            String label_area = getStringWithLocale(R.string.droplog_item_label_area);
            String label_isboss = getStringWithLocale(R.string.droplog_label_isboss);
            String label_rank = getStringWithLocale(R.string.droplog_label_rank);
            String label_name = getStringWithLocale(R.string.droplog_item_label_name);

            String label_line = KcaUtils.format("%s,%s,%s,%s,%s", label_date, label_area, label_isboss, label_rank, label_name);

            List<String> loglist = dropLogger.getFullStringDropLog();
            if (loglist.size() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String timetext = dateFormat.format(new Date());

                String filename = KcaUtils.format("/kca_droplog_%s.csv", timetext);
                file = new File(exportPath.concat(filename));
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
                    bw.write(label_line);
                    bw.write("\r\n");
                    for(String line: loglist) {
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
            row_count.setText(KcaUtils.format(getStringWithLocale(R.string.droplog_total_format), adapter.getCount()));
            row_count.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
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

    public void setListView() {
        //Toast.makeText(getApplicationContext(), condition_data.toString(), Toast.LENGTH_LONG).show();
        adapter.setListViewItemList(dropLogger.getDropLogWithCondition(condition_data), 0);
        droplog_listview.setAdapter(adapter);
        if (!is_exporting) row_count.setText(KcaUtils.format(getStringWithLocale(R.string.droplog_total_format), adapter.getCount()));
        findViewById(R.id.droplog_infoline).setVisibility(View.VISIBLE);
    }

    public void setSpinnerEnable(Spinner sp, boolean b) {
        if (sp != null) {
            sp.setEnabled(b);
        }
    }

    public void setConditionData(String key, int value) {
        condition_data.addProperty(key, value);
    }

    public void setConditionData(String key, String value) {
        condition_data.addProperty(key, value);
    }

    public static int rank_bitop(int source, int target, boolean b) {
        if (b) return source | target;
        else return source & ~target;
    }

    public String convertRankFlagToText(int r) {
        List<String> data = new ArrayList<>();
        String[] rank_list = {"E", "D", "C", "B", "A", "S"};
        for (int i = 0; i < 6; i++) {
            if (r % 2 == 1) data.add(rank_list[i]);
            r /= 2;
        }
        return KcaUtils.joinStr(data, ",");
    }

    public static String convertMillsToDate(long timestamp) {
        Date d = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        return dateFormat.format(d);
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
                        if (view.getId() == R.id.droplog_date_start) {
                            if (new_value < condition_data.get("enddate").getAsLong()) {
                                condition_data.addProperty("startdate", timestamp);
                            } else {
                                valid_flag = false;
                            }
                        } else if (view.getId() == R.id.droplog_date_end) {
                            new_value = timestamp + (DAY_MILLISECOND - 1);
                            if (new_value > condition_data.get("startdate").getAsLong()) {
                                condition_data.addProperty("enddate", timestamp + (DAY_MILLISECOND - 1));
                            } else {
                                valid_flag = false;
                            }
                        }
                        if (valid_flag) ((TextView) view).setText(convertMillsToDate(timestamp));
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
            DatePickerDialog dialog = new DatePickerDialog(DropLogActivity.this, listener, year, month, day);
            dialog.show();
        }
    };

    CompoundButton.OnCheckedChangeListener chkboxListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            int id = compoundButton.getId();
            switch (id) {
                case R.id.droplog_rank_s:
                    rank_flag = rank_bitop(rank_flag, RANK_S, b);
                    break;
                case R.id.droplog_rank_a:
                    rank_flag = rank_bitop(rank_flag, RANK_A, b);
                    break;
                case R.id.droplog_rank_b:
                    rank_flag = rank_bitop(rank_flag, RANK_B, b);
                    break;
                case R.id.droplog_rank_x:
                    rank_flag = rank_bitop(rank_flag, RANK_C | RANK_D | RANK_E, b);
                    break;
                default:
                    break;
            }
            setConditionData("rank", convertRankFlagToText(rank_flag));
        }
    };


    AdapterView.OnItemSelectedListener sp_world_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            String value = world_list[position];
            if (position == 0) {
                current_world = 0;
                current_map = 0;
                current_node = 0;
                condition_data.remove("world");
                condition_data.remove("map");
                condition_data.remove("node");
                setSpinnerEnable(sp_map, false);
                sp_map.setAdapter(null);
            } else {
                current_world = Integer.parseInt(value);
                condition_data.addProperty("world", current_world);
                map_list = dropLogger.getMapList(current_world);
                ArrayAdapter<CharSequence> sp_map_adapter =
                        new ArrayAdapter<CharSequence>(getBaseContext(),
                                android.R.layout.simple_spinner_item, map_list);
                sp_map_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                sp_map.setAdapter(sp_map_adapter);
                setSpinnerEnable(sp_map, true);
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
            String value = map_list[position];
            if (position == 0) {
                current_map = 0;
                current_node = 0;
                condition_data.remove("map");
                condition_data.remove("node");
                sp_node.setAdapter(null);
                setSpinnerEnable(sp_node, false);
            } else {
                current_map = Integer.parseInt(value);
                condition_data.addProperty("map", current_map);
                node_list = dropLogger.getNodeList(current_world, current_map);
                ArrayAdapter<CharSequence> sp_node_adapter =
                        new ArrayAdapter<CharSequence>(getBaseContext(),
                                android.R.layout.simple_spinner_item, node_list);
                sp_node_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                sp_node.setAdapter(sp_node_adapter);
                setSpinnerEnable(sp_node, true);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    AdapterView.OnItemSelectedListener sp_node_listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            String value = node_list[position];
            if (position == 0) {
                current_node = 0;
                condition_data.remove("node");
            } else {
                current_node = Integer.parseInt(value.split("\\(")[0]);
                condition_data.addProperty("node", current_node);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

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
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }
}
