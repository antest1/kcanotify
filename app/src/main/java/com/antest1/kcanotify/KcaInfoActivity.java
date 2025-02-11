package com.antest1.kcanotify;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Calendar;

public class KcaInfoActivity extends BaseActivity {
    Toolbar toolbar;
    TextView app_gpl, app_source;
    public static final String TAG = "KCA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.setting_menu_app_info));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app_gpl = findViewById(R.id.app_gpl);

        int year = Calendar.getInstance().get(Calendar.YEAR);
        TextView app_license = findViewById(R.id.AppLicense);
        app_license.setText(KcaUtils.format(getString(R.string.ia_license), year));

        app_gpl.setText(Html.fromHtml(KcaUtils.format(getString(R.string.ia_gpl), getString(R.string.app_brand)), Html.FROM_HTML_MODE_LEGACY));
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