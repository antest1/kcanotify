package com.antest1.kcanotify;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.R.attr.fragment;
import static android.view.View.GONE;
import static com.antest1.kcanotify.KcaResourcelogItemAdpater.resourceData;

public class KcaResoureLogFragment extends Fragment {
    public static final long DAY_MILLISECOND = 86400000;
    public int position;

    final int[][] color_data = {
            { R.color.colorResourceFuel, R.color.colorResourceAmmo, R.color.colorResourceSteel, R.color.colorResourceBauxite},
            { R.color.colorConsumableBucket, R.color.colorConsumableTorch, R.color.colorConsumableDevmat, R.color.colorConsumableScrew}};
    final String[][] data_key = {
            { "res_fuel", "res_ammo", "res_steel", "res_bauxite" },
            { "con_bucket", "con_torch", "con_devmat", "con_screw"}};

    final static int[] maximum = {300000, 3000};

    boolean[] is_draw_enabled = { true, true, true, true };
    static int[] interval = {5000, 100};
    static long xaxis_interval = DAY_MILLISECOND;
    static String xaxis_format = "MM/dd";
    KcaResourcelogItemAdpater adapter = new KcaResourcelogItemAdpater();

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getContext(), getActivity(), id);
    }

    public static KcaResoureLogFragment create(List<JsonObject> data, int pos) {
        Log.e("KCA", "create " + pos);
        resourceData = data;
        KcaResoureLogFragment fragment = new KcaResoureLogFragment();
        Bundle b = new Bundle();
        b.putInt("position", pos);
        fragment.setArguments(b);
        return fragment;
    }

    public static void setChartInfo(int res, int con, long xint, String xfmt) {
        interval[0] = res;
        interval[1] = con;
        xaxis_interval = xint;
        xaxis_format = xfmt;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_resourcelog, container, false);
        position = getArguments().getInt("position");
        adapter.setPosition(position);
        v = setView(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public View setView(final View v) {
        Log.e("KCA", "setView " + position);
        v.setTag("fragment_view");
        v.findViewById(KcaUtils.getId(KcaUtils.format("reslog_chart_filter_box_%d", position), R.id.class)).setVisibility(View.VISIBLE);
        v.findViewById(KcaUtils.getId(KcaUtils.format("reslog_chart_filter_box_%d", 1 - position), R.id.class)).setVisibility(GONE);

        for (int i = 0; i < 4; i++) {
            final int k = i;
            AppCompatCheckBox box = v.findViewById(KcaUtils.getId(KcaUtils.format("reslog_chart_filter_%d_%d", position, i), R.id.class));
            box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    is_draw_enabled[k] = b;
                    setChartDataVisibility(v, k);
                }
            });
            box.setChecked(is_draw_enabled[k]);
        }

        ((TextView) v.findViewById(R.id.reslog_item_label_0)).setText(getStringWithLocale(R.string.reslog_label_date));
        if (position == 0) {
            ((TextView) v.findViewById(R.id.reslog_item_label_1)).setText(getStringWithLocale(R.string.item_fuel));
            ((TextView) v.findViewById(R.id.reslog_item_label_2)).setText(getStringWithLocale(R.string.item_ammo));
            ((TextView) v.findViewById(R.id.reslog_item_label_3)).setText(getStringWithLocale(R.string.item_stel));
            ((TextView) v.findViewById(R.id.reslog_item_label_4)).setText(getStringWithLocale(R.string.item_baux));
        } else {
            ((TextView) v.findViewById(R.id.reslog_item_label_1)).setText(getStringWithLocale(R.string.item_bgtz));
            ((TextView) v.findViewById(R.id.reslog_item_label_2)).setText(getStringWithLocale(R.string.item_brnr));
            ((TextView) v.findViewById(R.id.reslog_item_label_3)).setText(getStringWithLocale(R.string.item_mmat));
            ((TextView) v.findViewById(R.id.reslog_item_label_4)).setText(getStringWithLocale(R.string.item_kmat));
        }

        ListView resource_data = v.findViewById(R.id.reslog_listview);
        resource_data.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        drawChart(v);
        return v;
    }

    public void drawChart(View v) {
        View chart_area = v.findViewById(R.id.reslog_chart_area);
        if (ResourceLogActivity.getChartHiddenState()) {
            chart_area.setVisibility(View.GONE);
            return;
        } else {
            chart_area.setVisibility(View.VISIBLE);
        }

        if (resourceData.size() <= 0) return;

        LineChart chart = v.findViewById(R.id.reslog_chart);
        final int[] color_table = color_data[position];
        final String[] key_list = data_key[position];
        int interval_value = interval[position];
        int y_count = 7;
        List<ILineDataSet> lines = new ArrayList<>();
        int max_value = 0;
        int min_value = maximum[position];

        for (int k = 0; k < 4; k++) {
            String key = key_list[k];
            List<Entry> data_entries = new ArrayList<>();
            for (JsonObject data: resourceData) {
                long time = data.get("timestamp").getAsLong();
                int value = data.get(key).getAsInt();
                data_entries.add(new Entry(time, value));
                if (max_value < value) max_value = value;
                if (min_value > value) min_value = value;
            }

            Collections.sort(data_entries, new EntryXComparator());
            LineDataSet dataset = new LineDataSet(data_entries, "data1");
            dataset.setDrawValues(false);
            int color = ContextCompat.getColor(getContext(), color_table[k]);
            dataset.setColor(color);
            dataset.setCircleColor(color);
            dataset.setVisible(is_draw_enabled[k]);
            dataset.setHighlightEnabled(false);
            lines.add(dataset);
        }

        max_value = (int) Math.ceil(max_value / (float) interval_value) * interval_value;
        min_value = (int) Math.floor(min_value / (float) interval_value) * interval_value;

        int range = max_value - min_value;
        while (range % (y_count - 1) != 0) y_count -= 1;

        Log.e("KCA", KcaUtils.format("%d~%d / %d", min_value, max_value, range));

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(xaxis_format, Locale.US);
                return dateFormat.format(new Date((long)value));
            }
        };

        LineData data = new LineData(lines);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(formatter);
        //xAxis.setGranularity(xaxis_interval);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setLabelCount(8, true);

        setChartYRange(chart, max_value, min_value, y_count, interval_value);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public void setChartYRange(LineChart chart, int max_value, int min_value, int y_count, int interval) {
        int margin = interval;
        if (max_value + margin < maximum[position]) max_value  += margin;
        if (min_value - margin > 0) min_value -= margin;
        chart.getAxisLeft().setAxisMaximum(max_value);
        chart.getAxisLeft().setAxisMinimum(min_value);
        chart.getAxisLeft().setLabelCount(y_count, false);
        chart.getAxisRight().setAxisMaximum(max_value);
        chart.getAxisRight().setAxisMinimum(min_value);
        chart.getAxisRight().setLabelCount(y_count, false);
    }

    public void setChartDataVisibility(View v, int k) {
        int interval_value = interval[position];
        int y_count = 7;
        LineChart chart = v.findViewById(R.id.reslog_chart);
        if (chart != null && chart.getLineData() != null) {
            ILineDataSet data = chart.getLineData().getDataSetByIndex(k);
            data.setVisible(is_draw_enabled[k]);
            int max_value = 0;
            int min_value = maximum[position];
            for (int i = 0; i < 4; i++) {
                if (is_draw_enabled[i]) {
                    max_value = Math.max((int) chart.getLineData().getDataSetByIndex(i).getYMax(), max_value);
                    min_value = Math.min((int) chart.getLineData().getDataSetByIndex(i).getYMin(), min_value);
                }
            }
            max_value = (int) (Math.ceil(max_value / (float) interval_value) * interval_value);
            min_value = (int) (Math.floor(min_value / (float) interval_value) * interval_value);
            int range = max_value - min_value;
            while (range % (y_count - 1) != 0) y_count -= 1;
            setChartYRange(chart, max_value, min_value, y_count, interval_value);
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }
}
