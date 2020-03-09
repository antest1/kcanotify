package com.antest1.kcanotify;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Calendar;

public class KcaInfoActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView app_gpl, app_source;
    public static final String TAG = "KCA";

    public KcaInfoActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.setting_menu_app_info));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app_gpl = (TextView) findViewById(R.id.app_gpl);

        int year = Calendar.getInstance().get(Calendar.YEAR);
        TextView app_license = findViewById(R.id.AppLicense);
        app_license.setText(KcaUtils.format(getString(R.string.ia_license), year));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            app_gpl.setText(Html.fromHtml(KcaUtils.format(getString(R.string.ia_gpl), getString(R.string.app_brand)), Html.FROM_HTML_MODE_LEGACY));
        } else {
            app_gpl.setText(Html.fromHtml((KcaUtils.format(getString(R.string.ia_gpl), getString(R.string.app_brand)))));
        }
        app_gpl.setMovementMethod(LinkMovementMethod.getInstance());
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