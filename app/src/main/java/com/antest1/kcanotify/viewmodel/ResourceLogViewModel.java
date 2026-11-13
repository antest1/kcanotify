package com.antest1.kcanotify.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaResourceLogger;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.LocaleUtils;
import com.antest1.kcanotify.R;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Date range, interval, data conversion and export logic for
 * {@link com.antest1.kcanotify.ResourceLogActivity}.
 */
public class ResourceLogViewModel extends AndroidViewModel {
    public static final long DAY_MILLISECOND = 86400000;

    public static final int INTERVAL_1H = 0;
    public static final int INTERVAL_3H = 1;
    public static final int INTERVAL_6H = 2;
    public static final int INTERVAL_12H = 3;
    public static final int INTERVAL_1D = 4;
    public static final int INTERVAL_3D = 5;
    public static final int INTERVAL_1W = 6;
    public static final int INTERVAL_2W = 7;
    public static final int INTERVAL_1M = 8;

    private final KcaDBHelper dbHelper;
    private final KcaResourceLogger resourceLogger;
    private final Context localizedContext;
    private List<JsonObject> resourceLog = new ArrayList<>();
    private long startTimestamp;
    private long endTimestamp;
    private int intervalValue = INTERVAL_1D;

    public ResourceLogViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
        resourceLogger = new KcaResourceLogger(application, null, KcaConstants.KCANOTIFY_RESOURCELOG_VERSION);
        localizedContext = LocaleUtils.getLocalizedContext(application);
    }

    public void init() {
        KcaApiData.setDBHelper(dbHelper);
        setCurrentTimestamp();
    }

    private void setCurrentTimestamp() {
        long current_time = System.currentTimeMillis();
        current_time = KcaUtils.getCurrentDateTimestamp(current_time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        String[] time_list = dateFormat.format(new Date(current_time)).split("\\-");
        int year = Integer.parseInt(time_list[2]);
        int month = Integer.parseInt(time_list[0]);
        startTimestamp = current_time - DAY_MILLISECOND * KcaUtils.getLastDay(year, month == 1 ? 12 : month - 1);
        endTimestamp = current_time + DAY_MILLISECOND - 1;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getIntervalValue() {
        return intervalValue;
    }

    public void setIntervalValue(int intervalValue) {
        this.intervalValue = intervalValue;
    }

    public List<JsonObject> getResourceLog() {
        return resourceLog;
    }

    public void loadResourceLogInRange(long startTimestamp, long endTimestamp) {
        resourceLog = resourceLogger.getResourceLogInRange(startTimestamp, endTimestamp);
    }

    public void clearResourceLog() {
        resourceLogger.clearResoureLog();
    }

    public long getInterval(int interval_value) {
        switch (interval_value) {
            case INTERVAL_1H:
                return DAY_MILLISECOND / 24;
            case INTERVAL_3H:
                return DAY_MILLISECOND / 8;
            case INTERVAL_6H:
                return DAY_MILLISECOND / 4;
            case INTERVAL_12H:
                return DAY_MILLISECOND / 2;
            case INTERVAL_1D:
                return DAY_MILLISECOND;
            case INTERVAL_3D:
                return DAY_MILLISECOND * 3;
            case INTERVAL_1W:
                return DAY_MILLISECOND * 7;
            case INTERVAL_2W:
                return DAY_MILLISECOND * 12;
            case INTERVAL_1M:
                return DAY_MILLISECOND * 30;
            default:
                return DAY_MILLISECOND;
        }
    }

    public List<JsonObject> convertData(List<JsonObject> data) {
        List<JsonObject> new_data = new ArrayList<>();
        if (data.isEmpty()) return new_data;

        List<Long> timestamp_list = new ArrayList<>();
        long start = KcaUtils.getCurrentDateTimestamp(startTimestamp);
        long end = KcaUtils.getCurrentDateTimestamp(endTimestamp);
        long interval = getInterval(intervalValue);
        for (JsonObject item : data) {
            timestamp_list.add(item.get("timestamp").getAsLong());
        }

        long time = start;
        while (time < end) {
            int recent_idx = 0;
            for (int k = 0; k < timestamp_list.size(); k++) {
                long ts = timestamp_list.get(k);
                if (ts >= time) {
                    break;
                } else {
                    recent_idx = k;
                }
            }
            JsonObject item = KcaUtils.getJsonObjectCopy(data.get(recent_idx));
            item.addProperty("timestamp", time);
            new_data.add(item);

            time += interval;
        }
        JsonObject last_item = KcaUtils.getJsonObjectCopy(data.get(data.size() - 1));
        last_item.addProperty("timestamp", end);
        new_data.add(last_item);
        return new_data;
    }

    public void recordDateParseError(Exception e) {
        dbHelper.recordErrorLog(KcaConstants.ERROR_TYPE_RESLOG, "", "", "", KcaUtils.getStringFromException(e));
    }

    /**
     * Writes the full resource log to a CSV file in the app's external files directory.
     */
    public ExportResult exportCsv() {
        int result;
        File savedir = new File(getApplication().getExternalFilesDir(null), "export_data");
        if (!savedir.exists()) savedir.mkdirs();
        String exportPath = savedir.getPath();

        String label_date = localizedContext.getString(R.string.reslog_label_time);
        String label_1 = localizedContext.getString(R.string.item_fuel);
        String label_2 = localizedContext.getString(R.string.item_ammo);
        String label_3 = localizedContext.getString(R.string.item_stel);
        String label_4 = localizedContext.getString(R.string.item_baux);
        String label_5 = localizedContext.getString(R.string.item_bgtz);
        String label_6 = localizedContext.getString(R.string.item_brnr);
        String label_7 = localizedContext.getString(R.string.item_mmat);
        String label_8 = localizedContext.getString(R.string.item_kmat);

        String label_line = KcaUtils.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                label_date, label_1, label_2, label_3, label_4, label_5, label_6, label_7, label_8);

        List<String> loglist = resourceLogger.getFullStringResourceLog();
        File file = null;
        if (!loglist.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String timetext = dateFormat.format(new Date());

            String filename = KcaUtils.format("/kca_resourcelog_%s.csv", timetext);
            file = new File(exportPath.concat(filename));
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
                bw.write(label_line);
                bw.write("\r\n");
                for (String line : loglist) {
                    bw.write(line);
                    bw.write("\r\n");
                }
                bw.close();
                result = 0;
            } catch (IOException e) {
                e.printStackTrace();
                result = 2;
            }
        } else {
            result = 1;
        }
        return new ExportResult(result, file == null ? null : file.getPath());
    }

    public static class ExportResult {
        public final int status;
        public final String path;

        public ExportResult(int status, String path) {
            this.status = status;
            this.path = path;
        }
    }
}
