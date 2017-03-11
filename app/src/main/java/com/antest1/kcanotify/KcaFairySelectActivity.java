package com.antest1.kcanotify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static com.antest1.kcanotify.KcaConstants.FAIRY_TOTAL_COUNT;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_FAIRY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaFairySelectActivity extends AppCompatActivity {
    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public KcaFairySelectActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_fairy);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.setting_menu_kand_title_fairy_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        for(int i = 0; i<FAIRY_TOTAL_COUNT; i++) {
            int btnId = getId("setting_fairy_".concat(String.valueOf(i)), R.id.class);
            ImageView btnView = (ImageView) findViewById(btnId);
            final String value = String.valueOf(i);
            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setPreferences(getApplicationContext(), PREF_FAIRY_ICON, value);
                    setFairyButton();
                    if(KcaService.getServiceStatus()) {
                        JsonObject data = new JsonObject();
                        data.addProperty("id", value);
                        Bundle bundle = new Bundle();
                        bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
                        bundle.putString("data", data.toString());
                        Message sMsg = sHandler.obtainMessage();
                        sMsg.setData(bundle);
                        sHandler.sendMessage(sMsg);
                    }
                }
            });
        }
        setFairyButton();
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

    private void setFairyButton() {
        for(int i = 0; i<FAIRY_TOTAL_COUNT; i++) {
            int btnId = getId("setting_fairy_".concat(String.valueOf(i)), R.id.class);
            ImageView btnView = (ImageView) findViewById(btnId);
            if(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON).equals(String.valueOf(i))) {
                btnView.setBackground(ContextCompat.getDrawable(this, R.drawable.imagebtn_on));
            } else {
                btnView.setBackground(ContextCompat.getDrawable(this, R.drawable.imagebtn_off));
            }
        }
    }
}
