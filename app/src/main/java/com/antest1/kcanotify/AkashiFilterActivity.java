package com.antest1.kcanotify;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaApiData.T3LIST_IMPROVABLE;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_FILTERLIST;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.KcaUtils.setPreferences;


public class AkashiFilterActivity extends AppCompatActivity {
    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();
    TextView itemNameTextView, itemImproveDefaultShipTextView;
    Button selectAllButton, selectNoneButton, selectReverseButton;
    JsonObject itemImprovementData;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public AkashiFilterActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akashi_filter);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.action_akashi_filter));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final String filter = getStringPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST);
        for(final int type: T3LIST_IMPROVABLE) {
            int btnId = getId("akashi_filter_equip_btn_".concat(String.valueOf(type)), R.id.class);
            final ImageView btnView = (ImageView) findViewById(btnId);
            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String filter = getStringPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST);
                    if(checkFiltered(filter, type)) {
                        setPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST, deleteFiltered(filter, type));
                        btnView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.imagebtn_on));
                    } else {
                        setPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST, addFiltered(filter, type));
                        btnView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.imagebtn_off));
                    }
                    sHandler.sendEmptyMessage(0);
                }
            });
        }
        setEquipButton();

        selectAllButton = (Button) findViewById(R.id.akashi_filter_btn_all);
        selectNoneButton = (Button) findViewById(R.id.akashi_filter_btn_none);
        selectReverseButton = (Button) findViewById(R.id.akashi_filter_btn_reverse);

        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST, "|");
                setEquipButton();
                sHandler.sendEmptyMessage(0);
            }
        });

        selectNoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> typelist = new ArrayList<String>();
                for(int type: T3LIST_IMPROVABLE) {
                    typelist.add(String.valueOf(type));
                }
                setPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST,
                        "|".concat(joinStr(typelist, "|")).concat("|"));
                setEquipButton();
                sHandler.sendEmptyMessage(0);
            }
        });

        selectReverseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filter = getStringPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST);
                List<String> typelist = new ArrayList<String>();
                for(int type: T3LIST_IMPROVABLE) {
                    if(!checkFiltered(filter, type)) typelist.add(String.valueOf(type));
                }
                if(typelist.size() > 0) {
                    setPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST,
                            "|".concat(joinStr(typelist, "|")).concat("|"));
                } else {
                    setPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST, "|");
                }
                setEquipButton();
                sHandler.sendEmptyMessage(0);
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

    private boolean checkFiltered(String data, int id) {
        return data.contains(KcaUtils.format("|%d|",id));
    }

    private String addFiltered(String data, int id) {
        return data.concat(String.valueOf(id)).concat("|");
    }

    private String deleteFiltered(String data, int id) {
        return data.replace(KcaUtils.format("|%d|",id), "|");
    }

    private void setEquipButton() {
        String filter = getStringPreferences(getApplicationContext(), PREF_AKASHI_FILTERLIST);
        for(int type: T3LIST_IMPROVABLE) {
            int btnId = getId("akashi_filter_equip_btn_".concat(String.valueOf(type)), R.id.class);
            ImageView btnView = (ImageView) findViewById(btnId);
            if(checkFiltered(filter, type)) {
                btnView.setBackground(ContextCompat.getDrawable(this, R.drawable.imagebtn_off));
            } else {
                btnView.setBackground(ContextCompat.getDrawable(this, R.drawable.imagebtn_on));
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }
}
