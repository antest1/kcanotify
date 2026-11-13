package com.antest1.kcanotify.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaDropLogger;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.antest1.kcanotify.KcaUtils.getId;

/**
 * Query state and business logic for {@link com.antest1.kcanotify.DropLogActivity}.
 */
public class DropLogViewModel extends AndroidViewModel {
    public static final long DAY_MILLISECOND = 86400000;

    public static final int RANK_S = 32;
    public static final int RANK_A = 16;
    public static final int RANK_B = 8;
    public static final int RANK_C = 4;
    public static final int RANK_D = 2;
    public static final int RANK_E = 1;

    private final KcaDBHelper dbHelper;
    private final KcaDropLogger dropLogger;
    private final JsonObject conditionData = new JsonObject();
    private final String[] maprankInfo = new String[5];
    private String[] worldList = {"*", "1", "2", "3", "4", "5", "6", "7"};
    private String[] mapList = {};
    private String[] nodeList = {};
    private int currentWorld;
    private int currentMap;
    private int currentNode;
    private int rankFlag = RANK_S | RANK_A | RANK_B;

    public DropLogViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
        dropLogger = new KcaDropLogger(application, null, KcaConstants.KCANOTIFY_DROPLOG_VERSION);
    }

    public void init() {
        KcaApiData.setDBHelper(dbHelper);
        KcaUtils.setDefaultGameData(getApplication(), dbHelper);

        // downloaded file first (falls back to the bundled asset)
        JsonArray appWorldListData = KcaUtils.getJsonArrayFromStorage(getApplication(), "world_list.json", dbHelper);
        if (appWorldListData != null) {
            worldList = new String[appWorldListData.size()];
            for (int i = 0; i < worldList.length; i++) {
                worldList[i] = appWorldListData.get(i).getAsString();
            }
        }

        maprankInfo[0] = "-";
        for (int i = 1; i < maprankInfo.length; i++) {
            maprankInfo[i] = getApplication().getString(getId("maprank_" + i, R.string.class));
        }
        KcaDropLogger.maprank_info = maprankInfo;
        KcaDropLogger.ship_none = getApplication().getString(R.string.droplog_ship_none);
        KcaDropLogger.ship_full = getApplication().getString(R.string.droplog_ship_full);

        long currentTime = KcaUtils.getCurrentDateTimestamp(System.currentTimeMillis());
        currentWorld = 0;
        currentMap = 0;
        currentNode = 0;

        conditionData.addProperty("startdate", currentTime - DAY_MILLISECOND * 7); // 7 days before
        conditionData.addProperty("enddate", currentTime + DAY_MILLISECOND - 1);
        conditionData.addProperty("isboss", 0);
        conditionData.addProperty("isdesc", 1);
        conditionData.addProperty("maprank", 0);
        conditionData.addProperty("rank", "B,A,S");
    }

    public JsonObject getConditionData() {
        return conditionData;
    }

    public String[] getMaprankInfo() {
        return maprankInfo;
    }

    public String[] getWorldList() {
        return worldList;
    }

    public String[] getMapList() {
        return mapList;
    }

    public String[] getNodeList() {
        return nodeList;
    }

    public String getStartDateText() {
        return convertMillsToDate(conditionData.get("startdate").getAsLong());
    }

    public String getEndDateText() {
        return convertMillsToDate(conditionData.get("enddate").getAsLong());
    }

    public void setConditionData(String key, int value) {
        conditionData.addProperty(key, value);
    }

    public void setConditionData(String key, String value) {
        conditionData.addProperty(key, value);
    }

    public void setMapRank(int position) {
        conditionData.addProperty("maprank", position);
    }

    /**
     * Selects a world. Returns true when the map spinner should be enabled.
     */
    public boolean selectWorld(int position) {
        String value = worldList[position];
        boolean mapEnabled;
        if (position == 0) {
            currentWorld = 0;
            currentMap = 0;
            currentNode = 0;
            conditionData.remove("world");
            conditionData.remove("map");
            conditionData.remove("node");
            mapList = new String[0];
            mapEnabled = false;
        } else {
            currentWorld = Integer.parseInt(value);
            conditionData.addProperty("world", currentWorld);
            mapList = dropLogger.getMapList(currentWorld);
            mapEnabled = true;
        }
        nodeList = new String[0];
        return mapEnabled;
    }

    /**
     * Selects a map. Returns true when the node spinner should be enabled.
     */
    public boolean selectMap(int position) {
        String value = mapList[position];
        boolean nodeEnabled;
        if (position == 0) {
            currentMap = 0;
            currentNode = 0;
            conditionData.remove("map");
            conditionData.remove("node");
            nodeList = new String[0];
            nodeEnabled = false;
        } else {
            currentMap = Integer.parseInt(value);
            conditionData.addProperty("map", currentMap);
            nodeList = dropLogger.getNodeList(currentWorld, currentMap);
            nodeEnabled = true;
        }
        return nodeEnabled;
    }

    public void selectNode(int position) {
        String value = nodeList[position];
        if (position == 0) {
            currentNode = 0;
            conditionData.remove("node");
        } else {
            currentNode = Integer.parseInt(value.split("\\(")[0]);
            conditionData.addProperty("node", currentNode);
        }
    }

    public void updateRankFlag(int viewId, boolean checked) {
        if (viewId == R.id.droplog_rank_s) {
            rankFlag = rank_bitop(rankFlag, RANK_S, checked);
        } else if (viewId == R.id.droplog_rank_a) {
            rankFlag = rank_bitop(rankFlag, RANK_A, checked);
        } else if (viewId == R.id.droplog_rank_b) {
            rankFlag = rank_bitop(rankFlag, RANK_B, checked);
        } else if (viewId == R.id.droplog_rank_x) {
            rankFlag = rank_bitop(rankFlag, RANK_C | RANK_D | RANK_E, checked);
        }
        setConditionData("rank", convertRankFlagToText(rankFlag));
    }

    /**
     * Applies a date chosen in the date picker.
     *
     * @return the selected timestamp when the range is still valid, otherwise {@code null}.
     */
    public Long applyDateSelection(boolean isStart, int year, int monthOfYear, int dayOfMonth) {
        String text = KcaUtils.format("%02d-%02d-%04d", monthOfYear + 1, dayOfMonth, year);
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
            long timestamp = dateFormat.parse(text).getTime();
            if (isStart) {
                if (timestamp < conditionData.get("enddate").getAsLong()) {
                    conditionData.addProperty("startdate", timestamp);
                    return timestamp;
                }
            } else {
                long newValue = timestamp + (DAY_MILLISECOND - 1);
                if (newValue > conditionData.get("startdate").getAsLong()) {
                    conditionData.addProperty("enddate", newValue);
                    return timestamp;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<JsonObject> getDropLog() {
        return dropLogger.getDropLogWithCondition(conditionData);
    }

    public void clearDropLog() {
        dropLogger.clearDropLog();
    }

    public static int rank_bitop(int source, int target, boolean b) {
        if (b) return source | target;
        else return source & ~target;
    }

    public static String convertRankFlagToText(int r) {
        List<String> data = new ArrayList<>();
        String[] rank_list = {"E", "D", "C", "B", "A", "S"};
        for (int i = 0; i < 6; i++) {
            if (r % 2 == 1) data.add(rank_list[i]);
            r /= 2;
        }
        return KcaUtils.joinStr(data, ",");
    }

    public static String convertMillsToDate(long timestamp) {
        Date d = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        return dateFormat.format(d);
    }

    /**
     * Writes the drop log to a CSV file in the app's external files directory.
     */
    public ExportResult exportCsv() {
        int result;
        File savedir = new File(getApplication().getExternalFilesDir(null), "export_data");
        if (!savedir.exists()) savedir.mkdirs();
        String exportPath = savedir.getPath();

        String label_date = getApplication().getString(R.string.droplog_item_label_time);
        String label_area = getApplication().getString(R.string.droplog_item_label_area);
        String label_isboss = getApplication().getString(R.string.droplog_label_isboss);
        String label_rank = getApplication().getString(R.string.droplog_label_rank);
        String label_name = getApplication().getString(R.string.droplog_item_label_name);

        String label_line = KcaUtils.format("%s,%s,%s,%s,%s", label_date, label_area, label_isboss, label_rank, label_name);

        List<String> loglist = dropLogger.getFullStringDropLog();
        File file = null;
        if (!loglist.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String timetext = dateFormat.format(new Date());

            String filename = KcaUtils.format("/kca_droplog_%s.csv", timetext);
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
