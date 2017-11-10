package com.antest1.kcanotify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.R.attr.key;
import static android.R.attr.y;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_ARRAY;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_BOOLEAN_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_ARRAY;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

public class KcaInspectorDetailActivity extends AppCompatActivity {
    final String PREF_PREFIX = "PREF ";
    final String DB_PREFIX = "DB ";
    final String DQ_PREFIX = "DQ ";
    final String QT_PREFIX = "QT ";

    String key, value_text= "";
    boolean is_formatted = true;
    Toolbar toolbar;
    TextView view_key, view_value, view_format;
    View view_holder;
    ScrollView sv;
    KcaDBHelper dbHelper;
    KcaQuestTracker questTracker;

    int scroll_h_total = 0;
    int scroll_h_layout = 0;
    int scroll_touch_count = 0;
    ScheduledExecutorService autoScrollScheduler;

    public KcaInspectorDetailActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector_detail);

        Bundle bundle = getIntent().getExtras();
        String type_key = bundle.getString("key");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(type_key);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);

        sv = findViewById(R.id.inspect_data_scrollview);
        sv.setSmoothScrollingEnabled(false);

        view_holder = findViewById(R.id.inspect_data_view);
        view_key = findViewById(R.id.inspect_data_key);
        view_value = findViewById(R.id.inspect_data_value);
        view_format = findViewById(R.id.inspect_data_format);

        view_format.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (value_text == null || value_text.length() > 0) {
                    is_formatted = !is_formatted;
                    setText();
                }
            }
        });

        String[] type_key_list = type_key.split(" ");
        key = type_key_list[1];
        view_key.setText(key);

        if (type_key.startsWith(DB_PREFIX)) {
            value_text = dbHelper.getValue(key);
            if (value_text == null) value_text = "<null>";
        } else if (type_key.startsWith(PREF_PREFIX)) {
            if (PREFS_BOOLEAN_LIST.contains(key)) {
                value_text = String.valueOf(getBooleanPreferences(getApplicationContext(), key));
            } else {
                value_text = getStringPreferences(getApplicationContext(), key);
            }
        } else if (type_key.startsWith(QT_PREFIX)) {
            value_text = questTracker.getQuestTrackerData();
        } else if (type_key.startsWith(DQ_PREFIX)) {
            value_text = dbHelper.getQuestListData();
        }
        setText();
        sv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int s_y = sv.getScrollY();
            }
        });

        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                scroll_h_total = sv.getMeasuredHeight();
                scroll_h_layout = sv.getHeight();

                sv.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                int y = (int) motionEvent.getY();
                                int direction = 1;
                                if (y < scroll_h_layout / 2) direction = -1;
                                scroll_touch_count = 0;
                                autoScrollScheduler = Executors.newSingleThreadScheduledExecutor();
                                autoScrollScheduler.scheduleAtFixedRate(auto_scroll(direction), 800, 200, TimeUnit.MILLISECONDS);
                                break;
                            case MotionEvent.ACTION_UP:
                                autoScrollScheduler.shutdown();
                                break;
                        }
                        return false;
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void setText() {
        if (value_text == null || value_text.length() == 0) {
            view_value.setText("");
        } else {
            Gson gson;
            if (is_formatted) gson = new GsonBuilder().setPrettyPrinting().create();
            else gson = new GsonBuilder().create();

            try {
                JsonElement data = gson.fromJson(value_text, JsonElement.class);
                value_text = gson.toJson(data);
            } catch (JsonSyntaxException ex) {
                // Nothing to do
            } finally {
                view_value.setText(value_text);
            }
        }
    }

    private Runnable auto_scroll(final int direction){
        Runnable aRunnable = new Runnable(){
            public void run(){
                int modifier = 1;
                scroll_touch_count += 1;
                if (scroll_touch_count >= 10) {
                    modifier = 4;
                } else if (scroll_touch_count >= 15) {
                    modifier = 32;
                }
                sv.scrollBy(0, scroll_h_layout * direction * modifier);
            }
        };
        return aRunnable;
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
}