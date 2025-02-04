package com.antest1.kcanotify;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.lang.ref.WeakReference;

import static com.antest1.kcanotify.KcaConstants.PREF_PACKAGE_ALLOW;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;


public class PackageFilterActivity extends BaseActivity {
    Toolbar toolbar;
    ListView listview;
    TextView countview;

    KcaPackageListViewAdpater adapter;
    static UpdateHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_list);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.setting_menu_sniffer_title_package_allow));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView notice = findViewById(R.id.package_notice);
        notice.setText(getString(R.string.packagefilter_restart));

        handler = new UpdateHandler(this);

        final PackageManager pm = getPackageManager();
        adapter = new KcaPackageListViewAdpater();
        adapter.setList(pm);
        adapter.setHandler(handler);
        listview = findViewById(R.id.package_listview);
        listview.setAdapter(adapter);

        countview = findViewById(R.id.package_count);
        setAllowCount();

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

    private static class UpdateHandler extends Handler {
        private final WeakReference<PackageFilterActivity> mActivity;

        UpdateHandler(PackageFilterActivity activity) {
            mActivity = new WeakReference<PackageFilterActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PackageFilterActivity activity = mActivity.get();
            if (activity != null) {
                activity.setAllowCount();
            }
        }
    }

    public void setAllowCount() {
        JsonArray data = JsonParser.parseString(getStringPreferences(getApplicationContext(), PREF_PACKAGE_ALLOW)).getAsJsonArray();
        countview.setText(KcaUtils.format(getString(R.string.packagefilter_count_format), data.size()));
    }
}
