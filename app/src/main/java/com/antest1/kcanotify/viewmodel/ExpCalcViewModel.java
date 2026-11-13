package com.antest1.kcanotify.viewmodel;

import android.app.Application;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;

/**
 * Business logic for {@link com.antest1.kcanotify.ExpCalcActivity}.
 *
 * <p>Owns the experience-calculation state and all of the data shaping that used to live in the
 * activity. It contains no references to views so the same logic can be reused by a Compose UI.
 */
public class ExpCalcViewModel extends AndroidViewModel {
    public static final int LEVEL_MAX = 188;
    public static final int RANK_S = 0;
    public static final int RANK_A = 1;
    public static final int RANK_B = 2;
    public static final int RANK_C = 3;
    public static final int RANK_D = 4;

    private KcaDBHelper dbHelper;
    private JsonObject expShipData;
    private JsonObject expSortieData;
    private String[] sortieMap = new String[0];
    private List<JsonObject> shipItemList = new ArrayList<>();
    private String[] shipLabelList = new String[0];

    private boolean isFlagship = false;
    private boolean isMvp = false;
    private int rankVal = RANK_S;
    private int currentExp = 0;
    private int targetExp = 0;
    private int count;
    private boolean loadFlag;
    private JsonObject currentShipData;
    private final SparseArray<String> trackValues = new SparseArray<>();
    private final JsonObject currentState = new JsonObject();

    public ExpCalcViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Loads (or reloads) the data used by the screen.
     *
     * @return false when the bundled experience data could not be read.
     */
    public boolean init() {
        dbHelper = new KcaDBHelper(getApplication(), null, KcaConstants.KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        KcaUtils.setDefaultGameData(getApplication(), dbHelper);

        int expLoadResult = KcaApiData.loadShipExpInfoFromAssets(getApplication().getAssets());
        int expSortieResult = KcaApiData.loadSortieExpInfoFromAssets(getApplication().getAssets());
        if (expLoadResult != 1 || expSortieResult != 1) {
            return false;
        }

        currentShipData = null;
        trackValues.clear();

        expShipData = dbHelper.getJsonObjectValue(KcaConstants.DB_KEY_EXPSHIP);
        expSortieData = dbHelper.getJsonObjectValue(KcaConstants.DB_KEY_EXPSORTIE);
        if (expSortieData != null) {
            sortieMap = new String[expSortieData.size()];
            int m = 0;
            for (Map.Entry<String, JsonElement> data : expSortieData.entrySet()) {
                sortieMap[m] = data.getKey();
                m++;
            }
        } else {
            sortieMap = new String[0];
        }

        JsonArray shipData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_SHIPIFNO);
        if (shipData == null) shipData = new JsonArray();
        Type listType = new TypeToken<List<JsonObject>>() {
        }.getType();
        shipItemList = new Gson().fromJson(shipData, listType);
        if (shipItemList == null) shipItemList = new ArrayList<>();
        Collections.sort(shipItemList, new StatComparator());

        shipLabelList = new String[shipItemList.size()];
        for (int i = 0; i < shipLabelList.length; i++) {
            int lv = shipItemList.get(i).get("api_lv").getAsInt();
            int shipId = shipItemList.get(i).get("api_ship_id").getAsInt();
            String name = "???";
            JsonObject kcShipData = KcaApiData.getKcShipDataById(shipId, "name");
            if (kcShipData != null) {
                name = getShipTranslation(kcShipData.get("name").getAsString(), shipId, false);
            }
            shipLabelList[i] = KcaUtils.format("[Lv.%d] %s", lv, name);
        }
        return true;
    }

    public JsonObject getExpShipData() {
        return expShipData;
    }

    public String[] getSortieMap() {
        return sortieMap;
    }

    public List<JsonObject> getShipItemList() {
        return shipItemList;
    }

    public String[] getShipLabelList() {
        return shipLabelList;
    }

    public String[] getLevelList() {
        String[] levelList = new String[LEVEL_MAX];
        for (int i = 0; i < LEVEL_MAX; i++) {
            levelList[i] = String.valueOf(i + 1);
        }
        return levelList;
    }

    public static String[] getRankList() {
        return new String[]{"S", "A", "B", "C", "D"};
    }

    public void setRank(int rank) {
        rankVal = rank;
        currentState.addProperty("rank", rank);
    }

    public void setFlagship(boolean flagship) {
        isFlagship = flagship;
        currentState.addProperty("flagship", flagship);
    }

    public void setMvp(boolean mvp) {
        isMvp = mvp;
        currentState.addProperty("mvp", mvp);
    }

    public void setCurrentExp(int exp) {
        currentExp = exp;
    }

    public void setTargetExp(int exp) {
        targetExp = exp;
    }

    public int getCurrentExp() {
        return currentExp;
    }

    public int getTargetExp() {
        return targetExp;
    }

    public void setCurrentShipData(JsonObject data) {
        currentShipData = data;
    }

    public JsonObject getCurrentShipData() {
        return currentShipData;
    }

    public void setLoadFlag(boolean value) {
        loadFlag = value;
    }

    public boolean isLoadFlag() {
        return loadFlag;
    }

    /**
     * Resets the per-screen state that the activity used to recreate in its {@code setUI()}.
     */
    public void resetForUi() {
        currentShipData = null;
        trackValues.clear();
    }

    public JsonObject getCurrentState() {
        return currentState;
    }

    public void setCurrentLvState(int position) {
        currentState.addProperty("current_lv", position);
    }

    public void setTargetLvState(int position) {
        currentState.addProperty("target_lv", position);
    }

    public void setShipState(int position) {
        currentState.addProperty("ship", position);
    }

    public void setMapState(int position) {
        currentState.addProperty("map", position);
    }

    /**
     * Applies the MVP / flagship / rank multipliers to the base experience.
     */
    public int getMapExp(String baseText) {
        if (baseText == null) baseText = "";
        if (baseText.length() > 9) baseText = baseText.substring(0, 9);
        int mapexp = 1;
        if (!baseText.isEmpty()) mapexp = Integer.parseInt(baseText);
        if (mapexp == 0) mapexp = 1;

        if (isMvp) mapexp *= 2;
        if (isFlagship) mapexp = (mapexp * 3) / 2;
        switch (rankVal) {
            case RANK_S:
                mapexp = mapexp * 6 / 5;
                break;
            case RANK_C:
                mapexp = mapexp * 8 / 10;
                break;
            case RANK_D:
                mapexp = mapexp * 7 / 10;
                break;
            default:
                break;
        }
        return mapexp;
    }

    /**
     * Computes the values shown on screen for the current inputs.
     */
    public ScreenResult calcScreen(String baseText) {
        int mapexp = getMapExp(baseText);
        int remainexp = Math.max(0, targetExp - currentExp);
        int leftCount = (int) Math.ceil((double) remainexp / mapexp);
        return new ScreenResult(mapexp, remainexp, leftCount);
    }

    /**
     * Adds a tracked calculation and returns the tag used for its row view.
     */
    public int addTrack(JsonObject data) {
        count += 1;
        trackValues.append(count, data.toString());
        return count;
    }

    public void removeTrack(int tag) {
        trackValues.remove(tag);
    }

    /**
     * Rebuilds the rows restored from the database. Mirrors the values the activity used to
     * compute while creating the list.
     */
    public List<JsonObject> getRestoredTrackItems() {
        List<JsonObject> result = new ArrayList<>();
        JsonArray shipData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_SHIPIFNO);
        if (shipData == null) shipData = new JsonArray();

        JsonObject shipIndex = new JsonObject();
        for (int i = 0; i < shipData.size(); i++) {
            JsonObject item = shipData.get(i).getAsJsonObject();
            String key = item.get("api_id").getAsString();
            shipIndex.add(key, item);
        }

        JsonArray savedData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_EXPCALTRK);
        if (savedData != null) {
            for (int i = 0; i < savedData.size(); i++) {
                JsonObject item = savedData.get(i).getAsJsonObject();
                String key = item.get("api_id").getAsString();
                if (shipIndex.has(key)) {
                    int mapexp = item.get("mapexp").getAsInt();
                    int currentLv = shipIndex.getAsJsonObject(key).get("api_lv").getAsInt();
                    int currentExp = shipIndex.getAsJsonObject(key).getAsJsonArray("api_exp").get(0).getAsInt();
                    int targetExp = item.get("target_exp").getAsInt();
                    int remainexp = Math.max(0, targetExp - currentExp);
                    item.addProperty("current_lv", currentLv);
                    item.addProperty("current_exp", currentExp);
                    item.addProperty("counter", (int) Math.ceil((double) remainexp / mapexp));
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * Persists the tracked calculations.
     */
    public void saveTracks() {
        JsonArray data = new JsonArray();
        for (int i = 0; i < trackValues.size(); i++) {
            int k = trackValues.keyAt(i);
            if (k <= count) {
                JsonObject val = JsonParser.parseString(trackValues.get(k)).getAsJsonObject();
                data.add(val);
            }
        }
        dbHelper.putValue(KcaConstants.DB_KEY_EXPCALTRK, data.toString());
    }

    public static class ScreenResult {
        public final int mapExp;
        public final int remainExp;
        public final int counter;

        public ScreenResult(int mapExp, int remainExp, int counter) {
            this.mapExp = mapExp;
            this.remainExp = remainExp;
            this.counter = counter;
        }
    }

    private static class StatComparator implements Comparator<JsonObject> {
        @Override
        public int compare(JsonObject o1, JsonObject o2) {
            String sortKey = "api_lv";
            return o2.get(sortKey).getAsInt() - o1.get(sortKey).getAsInt();
        }
    }
}
