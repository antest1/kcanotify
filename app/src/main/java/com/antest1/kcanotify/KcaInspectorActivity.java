package com.antest1.kcanotify;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

import static com.antest1.kcanotify.KcaConstants.DB_KEY_ARRAY;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_SETTING;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_PACKETLOG_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREFS_BOOLEAN_LIST;
import static com.antest1.kcanotify.KcaConstants.PREFS_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_PACKET_LOG;
import static com.antest1.kcanotify.KcaConstants.PREF_SVC_ENABLED;
import static com.antest1.kcanotify.KcaConstants.PREF_VPN_ENABLED;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class KcaInspectorActivity extends AppCompatActivity {
    final String SPREF_PREFIX = "SPREF ";
    final String PREF_PREFIX = "PREF ";
    final String DB_PREFIX = "DB ";
    final String DQ_PREFIX = "DQ ";
    final String QT_PREFIX = "QT ";

    Toolbar toolbar;
    static Gson gson = new Gson();
    ListView listview;
    KcaDBHelper dbHelper;
    KcaQuestTracker questTracker;
    ArrayList<Map.Entry<String, String>> listViewItemList;
    KcaInspectViewAdpater adapter;
    CheckBox packetlogEnable;
    Button packletlogClear, packetlogDump;
    KcaPacketLogger packetLogger;

    public KcaInspectorActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_inspector));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listViewItemList = new ArrayList<>();
        adapter = new KcaInspectViewAdpater();
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);
        packetLogger = new KcaPacketLogger(getApplicationContext(), null, KCANOTIFY_PACKETLOG_VERSION);
        for (String db_key: DB_KEY_ARRAY) {
            String db_value = dbHelper.getValue(db_key);
            if (db_value == null) db_value = "<null>";
            else if (db_value.length() > 100) {
                db_value = db_value.substring(0, 100).concat(KcaUtils.format("... (%d)", db_value.length()));
            }
            listViewItemList.add(new AbstractMap.SimpleEntry<> (DB_PREFIX.concat(db_key), db_value));
        }

        String questlist_data = dbHelper.getQuestListData();
        String questlist_view = questlist_data.replace("\n", ", ");
        if (questlist_view.length() > 100) {
            questlist_view = questlist_view.substring(0, 100).concat(KcaUtils.format("... (%d)", questlist_data.length()));
        }
        listViewItemList.add(new AbstractMap.SimpleEntry<> (DQ_PREFIX.concat("quest_data"), questlist_view));

        String questtrack_data = questTracker.getQuestTrackerDump();
        listViewItemList.add(new AbstractMap.SimpleEntry<> (QT_PREFIX.concat("tracked_data"), questtrack_data));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listViewItemList.add(new AbstractMap.SimpleEntry<>(SPREF_PREFIX.concat(PREF_VPN_ENABLED), String.valueOf(prefs.getBoolean(PREF_VPN_ENABLED, false))));
        listViewItemList.add(new AbstractMap.SimpleEntry<>(SPREF_PREFIX.concat(PREF_SVC_ENABLED), String.valueOf(prefs.getBoolean(PREF_SVC_ENABLED, false))));
        for (String pref_key: PREFS_LIST) {
            String pref_value = "";
            try {
                if (PREFS_BOOLEAN_LIST.contains(pref_key)) {
                    pref_value = String.valueOf(getBooleanPreferences(getApplicationContext(), pref_key));
                } else {
                    pref_value = getStringPreferences(getApplicationContext(), pref_key);
                }
            } catch (Exception e) {
                dbHelper.recordErrorLog(ERROR_TYPE_SETTING, "pref", "", "", getStringFromException(e));
            }
            listViewItemList.add(new AbstractMap.SimpleEntry<> (PREF_PREFIX.concat(pref_key), pref_value));
        }

        adapter.setListViewItemList(listViewItemList);
        listview = findViewById(R.id.inspector_listview);
        listview.setAdapter(adapter);

        packetlogEnable = findViewById(R.id.packetlog_enable);
        packetlogEnable.setChecked(getBooleanPreferences(getApplicationContext(), PREF_PACKET_LOG));
        packetlogEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                KcaUtils.setPreferences(getApplicationContext(), PREF_PACKET_LOG, isChecked);
            }
        });

        packletlogClear = findViewById(R.id.packetlog_clear);
        packletlogClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                packetLogger.clear();
            }
        });

        packetlogDump = findViewById(R.id.packetlog_dump);
        packetlogDump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = packetLogger.dump(getApplicationContext());
                Toast.makeText(getApplicationContext(), String.valueOf(result), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
