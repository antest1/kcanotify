package com.antest1.kcanotify.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.LocaleUtils;
import com.antest1.kcanotify.R;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromStorage;

/**
 * Business logic and state for {@link com.antest1.kcanotify.ExpeditionTableActivity}.
 */
public class ExpeditionTableViewModel extends AndroidViewModel {
    private final KcaDBHelper dbHelper;
    private final Context localizedContext;
    private JsonArray expeditionData = new JsonArray();
    private int worldIdx = 0;
    private boolean greatSuccess;
    private int daihatsuCount = 0;

    public ExpeditionTableViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
        localizedContext = LocaleUtils.getLocalizedContext(application);
    }

    public void init() {
        KcaApiData.setDBHelper(dbHelper);
        KcaUtils.setDefaultGameData(getApplication(), dbHelper);
        loadSimpleExpeditionInfoFromStorage(getApplication());
        expeditionData = KcaUtils.getJsonArrayFromStorage(getApplication(), "expedition.json", dbHelper);
    }

    public List<JsonObject> getExpeditionItems() {
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        List<JsonObject> items = new Gson().fromJson(expeditionData, listType);
        if (items == null) return new ArrayList<>();
        return new ArrayList<>(Collections2.filter(items,
                item -> worldIdx == 0 || item.get("area").getAsInt() == worldIdx));
    }

    public int getAreaColorRes(int area) {
        if (area < 1 || area > 7) area = 99;
        return KcaUtils.getId(KcaUtils.format("colorExpeditionTable%d", area), R.color.class);
    }

    public int calcResourcesBonus(int val) {
        if (greatSuccess) {
            return (int) ((val * (1.0 + (0.05 * daihatsuCount)) * 3.0) / 2);
        } else {
            return (int) (val * (1.0 + (0.05 * daihatsuCount)));
        }
    }

    public int getWorldIdx() {
        return worldIdx;
    }

    public void setWorldIdx(int worldIdx) {
        this.worldIdx = worldIdx;
    }

    public boolean isGreatSuccess() {
        return greatSuccess;
    }

    public void toggleGreatSuccess() {
        greatSuccess = !greatSuccess;
    }

    public String getGreatSuccessText() {
        return localizedContext.getString(greatSuccess ? R.string.expdtable_btn_s2 : R.string.expdtable_btn_s1);
    }

    public int getDaihatsuCount() {
        return daihatsuCount;
    }

    public void cycleDaihatsuCount() {
        daihatsuCount = (daihatsuCount + 1) % 5;
    }

    public static String getDaihatsuCountString(int val) {
        return "×".concat(String.valueOf(val));
    }
}
