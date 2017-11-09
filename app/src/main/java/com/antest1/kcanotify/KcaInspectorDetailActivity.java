package com.antest1.kcanotify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.Arrays;

import static android.R.attr.key;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_ARRAY;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_BOOLEAN_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_ARRAY;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

public class KcaInspectorDetailActivity extends AppCompatActivity {
    final String PREF_PREFIX = "PREF ";
    final String DB_PREFIX = "DB ";
    final String loading_message = "Loading value...";

    String key, value_text= "";
    boolean is_formatted = true;
    Toolbar toolbar;
    TextView view_key, view_value, view_format;
    KcaDBHelper dbHelper;

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
        view_value.setText(loading_message);

        if (type_key.startsWith(DB_PREFIX)) {
            value_text = dbHelper.getValue(key);
            if (value_text == null) value_text = "<null>";
        } else if (type_key.startsWith(PREF_PREFIX)) {
            if (PREFS_BOOLEAN_LIST.contains(key)) {
                value_text = String.valueOf(getBooleanPreferences(getApplicationContext(), key));
            } else {
                value_text = getStringPreferences(getApplicationContext(), key);
            }
        }
        setText();
    }

    public void setText() {
        if (value_text == null || value_text.length() == 0) {
            view_value.setText(loading_message);
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