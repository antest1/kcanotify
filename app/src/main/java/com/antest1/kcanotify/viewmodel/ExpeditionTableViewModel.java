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
import com.google.gson.JsonArray;

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

    public JsonArray getExpeditionData() {
        return expeditionData;
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
