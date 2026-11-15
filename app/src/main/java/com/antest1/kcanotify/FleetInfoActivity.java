package com.antest1.kcanotify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.antest1.kcanotify.viewmodel.FleetInfoViewModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaUtils.doVibrate;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.LocaleUtils.getResourceLocaleCode;

public class FleetInfoActivity extends BaseActivity {

    Toolbar toolbar;

    static boolean is_portrait = true;
    TextView fleetlist_name, fleetlist_fp, fleetlist_seek, fleetlist_loading;
    ImageView fleetlist_select;
    GridView fleetlist_ships;
    KcaFleetInfoItemAdapter adapter;
    Vibrator vibrator;

    boolean is_popup_on;
    View export_popup, export_exit;
    TextView export_clipboard, export_openpage_noro6, export_openpage_jervisor, export_openpage2;

    FleetInfoViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fleetlist);
        viewModel = new ViewModelProvider(this).get(FleetInfoViewModel.class);
        viewModel.init();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.action_fleetlist));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        vibrator = KcaUtils.getVibrator(this);

        final String[] deck_list = {"1", "2", "3", "4", getString(R.string.fleetview_combined)};

        fleetlist_select = findViewById(R.id.fleetlist_select);
        fleetlist_select.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.black), PorterDuff.Mode.MULTIPLY);

        fleetlist_select.setOnClickListener(v -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(FleetInfoActivity.this);
            dialog.setSingleChoiceItems(deck_list, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int n) {
                    viewModel.setCurrentFleet(n);
                    loadShipItem();
                    dialog.dismiss();
                }
            });
            dialog.show();
        });

        fleetlist_name = findViewById(R.id.fleetlist_name);
        fleetlist_ships = findViewById(R.id.fleetlist_ships);
        fleetlist_fp = findViewById(R.id.fleetlist_fp);
        fleetlist_seek = findViewById(R.id.fleetlist_seek);
        fleetlist_loading = findViewById(R.id.fleetlist_loading);
        for (int i = 1; i <= 7; i++) {
            KcaFleetInfoItemAdapter.alv_format[i] = getString(getId(KcaUtils.format("alv_%d", i), R.string.class));
        }

        adapter = new KcaFleetInfoItemAdapter();
        fleetlist_ships.setAdapter(adapter);

        is_portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (is_portrait) {
            fleetlist_ships.setNumColumns(1);
        } else {
            fleetlist_ships.setNumColumns(2);
        }

        export_popup = findViewById(R.id.export_popup);
        ((TextView) export_popup.findViewById(R.id.export_title))
                .setText(getString(R.string.fleetinfo_export_title));
        export_popup.setVisibility(View.GONE);
        is_popup_on = false;

        export_exit = export_popup.findViewById(R.id.export_exit);
        ((ImageView) export_exit).setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);

        export_popup.findViewById(R.id.export_bar).setOnClickListener(v -> {
            is_popup_on = false;
            export_popup.setVisibility(View.GONE);
        });

        TextView content = findViewById(R.id.export_content);

        export_clipboard = export_popup.findViewById(R.id.export_clipboard);
        export_clipboard.setText(getString(R.string.fleetinfo_export_clipboard));
        export_clipboard.setOnClickListener(v -> {
            CharSequence text = content.getText();
            ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText("text", text));
            doVibrate(vibrator, 100);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
        });

        export_openpage_noro6 = export_popup.findViewById(R.id.export_openpage_noro6);
        export_openpage_noro6.setText(getString(R.string.fleetinfo_export_openpage_noro6));
        export_openpage_noro6.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://noro6.github.io/kc-web/#/aircalc"));
            startActivity(bIntent);
        });

        export_openpage_jervisor = export_popup.findViewById(R.id.export_openpage_jervisor);
        export_openpage_jervisor.setText(getString(R.string.fleetinfo_export_openpage_jervisor));
        export_openpage_jervisor.setOnClickListener(v -> {
            Intent bIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://jervis.vercel.app/"));
            startActivity(bIntent);
        });

        export_openpage2 = export_popup.findViewById(R.id.export_openpage2);
        export_openpage2.setText(getString(R.string.fleetinfo_export_openpage2));
        export_openpage2.setOnClickListener(v -> {
            String data = viewModel.imageBuilderData();
            String locale_code = getResourceLocaleCode();
            if (locale_code.equals("ko")) locale_code = "kr";
            if (locale_code.equals("tcn")) locale_code = "jp";
            try {
                JsonObject add_data = JsonParser.parseString(data).getAsJsonObject();
                add_data.addProperty("lang", locale_code);
                add_data.addProperty("theme", "dark");
                data = add_data.toString();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "json parsing error", Toast.LENGTH_SHORT).show();
            }

            try {
                String encoded =  URLEncoder.encode(data, "utf-8");
                Intent bIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://kancolleimgbuilder.web.app/builder#".concat(encoded)));
                startActivity(bIntent);
            } catch (UnsupportedEncodingException e) {
                Toast.makeText(getApplicationContext(), "parsing error", Toast.LENGTH_SHORT).show();
            }
        });

        loadShipItem();
    }

    private void loadShipItem() {
        Handler handler = new Handler(Looper.getMainLooper());
        fleetlist_loading.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            FleetInfoViewModel.FleetData result = viewModel.loadFleetData(is_portrait);
            handler.post(() -> {
                fleetlist_name.setText(result.fleetName);
                if (!result.passed) {
                    findViewById(R.id.fleetlist_content).setVisibility(View.GONE);
                    fleetlist_fp.setText("");
                    fleetlist_seek.setText("");
                } else {
                    KcaFleetInfoItemAdapter.is_combined = result.isCombined;
                    adapter.setListViewItemList(result.data);
                    findViewById(R.id.fleetlist_content).setVisibility(View.VISIBLE);
                    fleetlist_fp.setText(result.fp);
                    fleetlist_seek.setText(result.seek);
                    adapter.notifyDataSetChanged();
                }
                fleetlist_loading.setVisibility(View.GONE);
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fleetinfo, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_fleet_export:
                String data = viewModel.deckBuilderData();
                ((TextView) export_popup.findViewById(R.id.export_content)).setText(data);
                is_popup_on = true;
                export_popup.setVisibility(View.VISIBLE);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        is_portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        if (is_portrait) {
            fleetlist_ships.setNumColumns(1);
        } else {
            fleetlist_ships.setNumColumns(2);
        }
        fleetlist_ships.invalidateViews();
    }
}
