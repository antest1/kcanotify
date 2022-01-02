package com.antest1.kcanotify;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVE;
import static com.antest1.kcanotify.KcaApiData.T2_ANTISUB_PATROL;
import static com.antest1.kcanotify.KcaApiData.T2_AP_SHELL;
import static com.antest1.kcanotify.KcaApiData.T2_AUTOGYRO;
import static com.antest1.kcanotify.KcaApiData.T2_DEPTH_CHARGE;
import static com.antest1.kcanotify.KcaApiData.T2_DRUM_CAN;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE_II;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_MEDIUM;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_SMALL;
import static com.antest1.kcanotify.KcaApiData.T2_MACHINE_GUN;
import static com.antest1.kcanotify.KcaApiData.T2_RADAR_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_RADAR_SMALL;
import static com.antest1.kcanotify.KcaApiData.T2_RADER_LARGE_II;
import static com.antest1.kcanotify.KcaApiData.T2_SANSHIKIDAN;
import static com.antest1.kcanotify.KcaApiData.T2_SEA_SCOUT;
import static com.antest1.kcanotify.KcaApiData.T2_SONAR;
import static com.antest1.kcanotify.KcaApiData.T2_SONAR_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_SUB_GUN;
import static com.antest1.kcanotify.KcaApiData.checkUserShipDataLoaded;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getShipTypeAbbr;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaExpedition2.getExpeditionHeader;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getTimeStr;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.google.common.collect.Lists.transform;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AnyRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.core.content.ContextCompat;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Drum: 21, 44, 37, 38, E2, 24, 40
// Flag Lv: A2, A3, A4, A5, A6,
//      B3, B4, B5, B6,
//      41, 43, 45, 46,
//      32, D1, D2, D3, E1
// Monthly: A4, A5, A6,
//      B2, B3, B4, B5, B6,
//      42, 43, 44, 46,
//      D2, D3, E1, E2
// Combat I: A5, B4, 43, D2
// Combat II: A6, B5, B6, 46, D3, E1, E2

/*
cat 'app/src/main/assets/expedition.json' | jq \
--argjson drum '["21", "44", "37", "38", "E2", "24", "40"]' \
--argjson flag '["A2", "A3", "A4", "A5", "A6", "B3", "B4", "B5", "B6", "41", "43", "45", "46", "32", "D1", "D2", "D3", "E1"]' \
--argjson c1 '["A5", "B4", "43", "D2"]' \
--argjson c2 '["A6", "B5", "B6", "46", "D3", "E1", "E2"]' \
--argjson month '["A4", "A5", "A6", "B2", "B3", "B4", "B5", "B6", "42", "43", "44", "46", "D2", "D3", "E1", "E2"]' \
'def containedIn($arr): . as $x | $arr | any(. == $x); del(.[].features)
| map(if .code | containedIn($drum) then .features += ["drum"] else . end
| if .code | containedIn($flag) then .features += ["flag_lv"] else . end
| if .code | containedIn($c1) then .features += ["combat_1"] else . end
| if .code | containedIn($c2) then .features += ["combat_2"] else . end
| if .code | containedIn($month) then .features += ["monthly"] else . end)'
*/
public class KcaExpeditionCheckViewService extends Service {
    public static final String TAG = "KCA/EXPD";
    public static final String SHOW_EXCHECKVIEW_ACTION = "show_excheckview";
    private static final float[][] TOKU_BONUS_DATA = {
            {0.02f, 0.02f, 0.02f, 0.02f, 0.02f},
            {0.04f, 0.04f, 0.04f, 0.04f, 0.04f},
            {0.05f, 0.05f, 0.052f, 0.054f, 0.054f},
            {0.05f, 0.056f, 0.058f, 0.059f, 0.06f}
    };

    private static final SparseArray<Float> DLCS_BONUS_DATA = new SparseArray<>();

    static {
        DLCS_BONUS_DATA.put(68, 0.05f); // Daihatsu / 大発動艇
        DLCS_BONUS_DATA.put(166, 0.02f); // 89Tank / 大発動艇(八九式中戦車&陸戦隊)
        DLCS_BONUS_DATA.put(167, 0.01f); // Amp. Tank / 特二式内火艇
        DLCS_BONUS_DATA.put(193, 0.05f); // Toku Daihatsu / 特大発動艇
        DLCS_BONUS_DATA.put(230, 0.00f); // 11th Tank Regiment / 特大発動艇+戦車第11連隊
        DLCS_BONUS_DATA.put(355, 0.00f); // M4A1 DD
        DLCS_BONUS_DATA.put(408, 0.02f); // Armored Boat / 装甲艇(AB艇)
        DLCS_BONUS_DATA.put(409, 0.03f); // Armed Daihatsu / 武装大発
        DLCS_BONUS_DATA.put(436, 0.02f); // Panzer II / 大発動艇(II号戦車/北アフリカ仕様)
        DLCS_BONUS_DATA.put(449, 0.02f); // Type 1 Gun Tank / 特大発動艇＋一式砲戦車
    }

    /**
     * @see KcaApiData#STYPE_CVL
     * @see KcaApiData#STYPE_CV
     * @see KcaApiData#STYPE_AV
     * @see KcaApiData#STYPE_CVB
     */
    private static final String CARRIERS = "7,11,16,18";

    public static boolean active;
    static boolean error_flag = false;
    private String locale;
    Context contextWithLocale;
    int displayWidth = 0;
    private View mView;
    private WindowManager mManager;
    WindowManager.LayoutParams mParams;
    private final Gson gson = new Gson();

    private int selected_fleet = 1; // selected fleet
    private int selected_world = 1; // selected world, 1 <= n <= 7, exclude 6
    private int selected_expd = 0; // selected expedition
    private boolean isGreatSuccess = false;

    private final List<List<Integer>> fleet_list = new ArrayList<>(); // set by onStartCommand
    private final List<JsonObject> ship_list = new ArrayList<>(); // set by updateFleet
    private final List<Integer> expd_list = new ArrayList<>(); // set by updateWorld
    private JsonObject status_info; // set by updateFleet
    private JsonObject bonus_info; // set by updateFleet
    private final SparseArray<JsonObject> check_data = new SparseArray<>(); // set by updateCheckData

    private static final String FLAG_LV = "flag-lv";
    private static final String FLAG_COND = "flag-cond";
    private static final String FLEET_LV = "total-lv";
    private static final String FLEET_NUM = "total-num";
    private static final String FLEET_COND = "total-cond";
    private static final String DRUM_SHIPS = "drum-ship";
    private static final String DRUM_COUNT = "drum-num";
    private static final String DRUM_OPTIONAL = "drum-num-optional";
    private static final String TOTAL_FIRE = "total-firepower";
    private static final String TOTAL_ASW = "total-asw";
    private static final String TOTAL_AA = "total-fp";
    private static final String TOTAL_LOS = "total-los";
    private static final String KINU_EXISTS = "kinu";
    private static final String DLCS_COUNT = "daihatsu";
    private static final String DLCS_BONUS = "dlc-bonus";
    private static final String TOKU_BONUS = "toku-bonus";

    // region lifecycle
    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }

        try {
            active = true;
            locale = LocaleUtils.getResourceLocaleCode(KcaUtils.getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
            contextWithLocale = KcaUtils.getContextWithLocale(getApplicationContext(), getBaseContext());

            initView();

            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            displayWidth = size.x;
        } catch (Exception e) {
            e.printStackTrace();
            active = false;
            error_flag = true;
            stopSelf();
        }
    }

    @SuppressLint("InflateParams")
    public void initView() {
        mView = LayoutInflater.from(contextWithLocale).inflate(R.layout.view_excheck_list, null);
        KcaUtils.resizeFullWidthView(getApplicationContext(), mView);

        findViewById(R.id.window_head).setOnClickListener((v) -> stopSelf());
        findViewById(R.id.window_exit).setOnClickListener((v) -> stopSelf());
        for (int id : this.<Flow>findViewById(R.id.fleet_tab).getReferencedIds()) {
            findViewById(id).setOnClickListener(this::onClickFleetTab);
        }
        for (int id : this.<Flow>findViewById(R.id.expd_worlds).getReferencedIds()) {
            findViewById(id).setOnClickListener(this::onClickWorldTab);
        }
        for (int id : this.<Flow>findViewById(R.id.expd_table).getReferencedIds()) {
            findViewById(id).setOnClickListener(this::onClickExpdButton);
        }
        findViewById(R.id.expd_reward).setOnClickListener(this::onToggleRewardButton);
        clearExpdDetailLayout();

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.CENTER;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        if (intent == null || intent.getAction() == null
                || !intent.getAction().startsWith(SHOW_EXCHECKVIEW_ACTION)) {
            return super.onStartCommand(intent, flags, startId);
        }

        try (KcaDBHelper db = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION)) {
            Type type = new TypeToken<List<Integer>>() {
            }.getType();
            fleet_list.clear();
            for (JsonElement fleet : db.getJsonArrayValue(DB_KEY_DECKPORT)) {
                fleet_list.add(gson.fromJson(fleet.getAsJsonObject().get("api_ship"), type));
            }
        }

        if (fleet_list.size() <= 1) {
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        try {
            if (!checkLoaded()) throw new IllegalStateException("Userdata is not loaded");

            setSelectedFleet(Integer.parseInt(intent.getAction().split("/")[1]));

            if (mView.getParent() != null) {
                mManager.removeViewImmediate(mView);
            }
            mManager.addView(mView, mParams);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), getStringFromException(e), Toast.LENGTH_LONG).show();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        active = false;
        if (mView != null && mView.getParent() != null) {
            mManager.removeViewImmediate(mView);
        }
        super.onDestroy();
    }

    private void onClickExpdButton(View v) {
        if (!checkUserShipDataLoaded()) return;
        Flow table = findViewById(R.id.expd_table);
        int expd = Ints.indexOf(table.getReferencedIds(), v.getId());
        if (expd == -1) {
            Log.e(TAG, getResName(v.getId()), new IllegalStateException("can not find clicked view"));
            return;
        }
        if (expd != selected_expd) setSelectedExpd(expd);
    }

    private void onClickWorldTab(View v) {
        if (!checkUserShipDataLoaded()) return;
        try {
            int world = Integer.parseInt((String) v.getTag());
            if (world != selected_world) setSelectedWorld(world);
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG, getResName(v.getId()), new IllegalStateException("tag is not set or is overwritten, tag: " + v.getTag()));
        }
    }

    private void onClickFleetTab(View v) {
        if (!checkUserShipDataLoaded()) return;
        try {
            int fleet = Integer.parseInt((String) v.getTag()) - 1 /* fleet id is 0(main) <= id <= 3 */;
            if (fleet != selected_fleet) setSelectedFleet(fleet);
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG, getResName(v.getId()), new IllegalStateException("tag is not set or is overwritten, tag: " + v.getTag()));
        }
    }

    private void onToggleRewardButton(View v) {
        if (!checkUserShipDataLoaded()) return;
        isGreatSuccess = !isGreatSuccess;
        setExpdDetailLayout();
    }
    // endregion

    private void setSelectedFleet(int newValue) {
        if (!checkLoaded()) return;
        newValue = clamp(newValue, 1, fleet_list.size()); // 1 <= i < size
        selected_fleet = newValue;

        Log.d(TAG, "fleet: " + newValue);

        updateFleet();
        updateCheckData();
        setSelectedWorld(1);
    }

    private void updateFleet() {
        for (int id : this.<Flow>findViewById(R.id.fleet_tab).getReferencedIds()) {
            View v = findViewById(id);
            v.setBackgroundColor(selected_fleet + 1 == Integer.parseInt((String) v.getTag())
                    ? ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)
                    : ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
        }

        ship_list.clear();
        for (int id : fleet_list.get(selected_fleet)) {
            if (id <= 0) continue;

            JsonObject data = new JsonObject();
            JsonObject userData = getUserShipDataById(id, "ship_id,lv,slot,cond,karyoku,taisen,taiku,sakuteki");
            int kc_ship_id = userData.get("ship_id").getAsInt();
            JsonObject kcData = getKcShipDataById(kc_ship_id, "stype");
            if (kcData == null) {
                Log.e(TAG, "cannot find kc data for ship_id: " + kc_ship_id);
                continue;
            }

            data.add("ship_id", userData.get("ship_id"));
            data.add("lv", userData.get("lv"));
            data.add("cond", userData.get("cond"));
            data.add("stype", kcData.get("stype"));
            data.add("karyoku", userData.getAsJsonArray("karyoku").get(0));
            data.add("taisen", userData.getAsJsonArray("taisen").get(0));
            data.add("taiku", userData.getAsJsonArray("taiku").get(0));
            data.add("sakuteki", userData.getAsJsonArray("sakuteki").get(0));
            data.add("item", new JsonArray());
            for (int item_id : gson.fromJson(userData.get("slot"), int[].class)) {
                if (item_id <= 0) continue;

                JsonObject itemData = getUserItemStatusById(item_id, "slotitem_id,level", "type,tais");
                if (itemData != null) data.getAsJsonArray("item").add(itemData);
            }

            ship_list.add(data);
        }

        updateStatusInfo();
        updateBonusInfo();
        setFleetInfoView();

        Log.d(TAG, "ship_list: " + ship_list);
        Log.d(TAG, "status_info: " + status_info);
        Log.d(TAG, "bonus_info: " + bonus_info);
    }

    /**
     * <a href="https://wikiwiki.jp/kancolle/%E9%81%A0%E5%BE%81#about_stat">wikiwiki</a>
     * <a href="https://kitongame.com/%E3%80%90%E8%89%A6%E3%81%93%E3%82%8C%E3%80%91%E3%83%9E%E3%83%B3%E3%82%B9%E3%83%AA%E3%83%BC%E9%81%A0%E5%BE%81%E3%81%AE%E6%88%90%E5%8A%9F%E6%9D%A1%E4%BB%B6%E3%81%A8%E7%8D%B2%E5%BE%97%E8%B3%87%E6%9D%90/">kiton</a>
     */
    private void updateStatusInfo() {
        int total_lv = 0;
        float total_fire = 0;
        float total_asw = 0;
        float total_aa = 0;
        float total_los = 0;

        boolean has_aircraft = false;

        for (JsonObject ship : ship_list) {

            total_lv += ship.get("lv").getAsInt();
            total_fire += ship.get("karyoku").getAsInt();
            total_asw += ship.get("taisen").getAsInt();
            total_aa += ship.get("taiku").getAsInt();
            total_los += ship.get("sakuteki").getAsInt();

            for (JsonElement itemData : ship.getAsJsonArray("item")) {
                int type2 = itemData.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                int type3 = itemData.getAsJsonObject().getAsJsonArray("type").get(3).getAsInt();
                int level = itemData.getAsJsonObject().get("level").getAsInt();

                if (type2 == T2_GUN_SMALL || type2 == T2_SANSHIKIDAN || type2 == T2_AP_SHELL)
                    total_fire += floor01(0.5 * sqrt(level));

                if (type2 == T2_GUN_MEDIUM)
                    total_fire += floor01(sqrt(level));

                if (type2 == T2_GUN_LARGE || type2 == T2_GUN_LARGE_II)
                    total_fire += floor01(0.97 * sqrt(level));

                if (type2 == T2_SUB_GUN)
                    total_fire += floor01(0.15 * level);

                if (type3 == 16 /* green aa gun */)
                    total_aa += floor01(0.3 * level);

                if (type2 == T2_MACHINE_GUN)
                    total_aa += floor01(sqrt(level));

                if (type2 == T2_SONAR || type2 == T2_SONAR_LARGE || type2 == T2_DEPTH_CHARGE)
                    total_asw += floor01(sqrt(level));

                if (type2 == T2_SEA_SCOUT || type2 == T2_RADAR_LARGE || type2 == T2_RADER_LARGE_II)
                    total_los += floor01(0.95 * sqrt(level));

                if (type2 == T2_RADAR_SMALL)
                    total_los += floor01(sqrt(level));

                if (KcaApiData.isItemAircraft(type2) || type2 == T2_AUTOGYRO || type2 == T2_ANTISUB_PATROL) {
                    has_aircraft = true;
                    total_asw -= itemData.getAsJsonObject().get("tais").getAsInt();
                    total_aa -= itemData.getAsJsonObject().get("tyku").getAsInt();
                    total_los -= itemData.getAsJsonObject().get("saku").getAsInt();
                }
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty(FLEET_LV, total_lv);
        result.addProperty(TOTAL_FIRE, total_fire);
        result.addProperty(TOTAL_ASW, total_asw);
        result.addProperty(TOTAL_AA, total_aa);
        result.addProperty(TOTAL_LOS, total_los);
        result.addProperty("has-aircraft", has_aircraft);
        status_info = result;
    }

    /**
     * <a href="https://wikiwiki.jp/kancolle/%E6%94%B9%E4%BF%AE%E5%B7%A5%E5%BB%A0#abf2415c">wikiwiki: daihatsu bonus</a>
     * <a href="https://wikiwiki.jp/kancolle/%E7%89%B9%E5%A4%A7%E7%99%BA%E5%8B%95%E8%89%87/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%881#:~:text=%E7%89%B9%E5%A4%A7%E7%99%BA%E8%A3%9C%E6%AD%A3%E3%81%A7,%E9%87%91)%2018%3A10%3A58">wikiwiki: toku daihatsu bonus</a>
     */
    private void updateBonusInfo() {
        boolean kinu_exist = false;
        int drum_count = 0;
        int drum_ships = 0;
        SparseIntArray dlcs_count = new SparseIntArray();
        int all_dlcs_count = 0;
        int sum_of_level = 0;
        int bonus_count = 0;

        for (JsonObject ship : ship_list) {
            if (ship.get("ship_id").getAsInt() == 487) { // Kinu Kai Ni
                kinu_exist = true;
            }

            boolean has_drum = false;
            for (JsonElement itemData : ship.getAsJsonArray("item")) {
                int item_id = itemData.getAsJsonObject().get("slotitem_id").getAsInt();
                int type2 = itemData.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                int level = itemData.getAsJsonObject().get("level").getAsInt();

                if (type2 == T2_DRUM_CAN) {
                    has_drum = true;
                    drum_count += 1;
                }

                if (DLCS_BONUS_DATA.indexOfKey(item_id) >= 0) {
                    all_dlcs_count += 1;
                    dlcs_count.put(item_id, dlcs_count.get(item_id, 0) + 1);

                    if (DLCS_BONUS_DATA.get(item_id, 0f) > 0f) {
                        sum_of_level += level;
                        bonus_count += 1;
                    }
                }
            }

            if (has_drum) drum_ships += 1;
        }

        float avg_of_level = 0f;
        if (bonus_count > 0) avg_of_level = ((float) sum_of_level) / bonus_count;

        float sum_of_bonus = kinu_exist ? 0.05f : 0f;
        for (int i = 0; i < dlcs_count.size(); i++) {
            int item_id = dlcs_count.keyAt(i);
            sum_of_bonus += DLCS_BONUS_DATA.get(item_id) * dlcs_count.get(item_id);
        }
        sum_of_bonus = min(sum_of_bonus, 0.2f);

        float toku_bonus = 0f;
        if (dlcs_count.get(193, 0) >= 1) {
            int toku_idx = min(dlcs_count.get(193) - 1, 3);
            int dlc_idx = min(dlcs_count.get(68), 4);
            toku_bonus = TOKU_BONUS_DATA[toku_idx][dlc_idx];
        }

        JsonObject result = new JsonObject();
        result.addProperty(KINU_EXISTS, kinu_exist);
        result.addProperty(DRUM_COUNT, drum_count);
        result.addProperty(DRUM_SHIPS, drum_ships);
        result.addProperty(DLCS_COUNT, all_dlcs_count);
        result.addProperty(DLCS_BONUS, sum_of_bonus + 0.01f * sum_of_bonus * avg_of_level);
        result.addProperty(TOKU_BONUS, toku_bonus);
        bonus_info = result;
    }

    private int applyBonus(int value, boolean isGreatSuccess) {
        float dlcs_bonus = bonus_info.get(DLCS_BONUS).getAsFloat();
        float toku_bonus = bonus_info.get(TOKU_BONUS).getAsFloat();

        float gs = isGreatSuccess ? 1.5f : 1f;
        return (int) (value * gs * (1f + dlcs_bonus)) + (int) (value * gs * toku_bonus);
    }

    private void setSelectedWorld(int newValue) {
        if (!checkLoaded()) return;
        newValue = clamp(newValue, 1, 7); // 1 <= i <= 7, exclude 6
        selected_world = newValue;

        Log.d(TAG, "world: " + newValue);

        updateWorld();
        updateCheckData();
        setSelectedExpd(0);
    }

    private void updateWorld() {
        for (int id : this.<Flow>findViewById(R.id.expd_worlds).getReferencedIds()) {
            View v = findViewById(id);
            v.setBackgroundColor(selected_world == Integer.parseInt((String) v.getTag())
                    ? ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)
                    : ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
        }

        expd_list.clear();
        expd_list.addAll(KcaApiData.getExpeditionNumByWorld(selected_world));
        Log.d(TAG, "expd_list: " + expd_list);

        int[] view_ids = this.<Flow>findViewById(R.id.expd_table).getReferencedIds();
        for (int i = 0; i < view_ids.length; i++) {
            int id = view_ids[i];

            if (i >= expd_list.size()) {
                findViewById(id).setVisibility(INVISIBLE);
            } else {

                setViewContentById(id, KcaExpedition2.getExpeditionStr(expd_list.get(i)));
                findViewById(id).setVisibility(VISIBLE);
            }
        }
    }

    private void updateCheckData() {

        check_data.clear();
        for (int expd : expd_list) {
            check_data.put(expd, checkCondition(KcaApiData.getExpeditionInfo(expd, locale)));
        }

        Log.d(TAG, "check_data: " + check_data);

        int[] view_ids = this.<Flow>findViewById(R.id.expd_table).getReferencedIds();
        for (int i = 0; i < expd_list.size(); i++) {
            int id = view_ids[i];

            JsonObject check_item = check_data.get(expd_list.get(i));
            if (check_item != null) {
                boolean pass = check_item.get("pass").getAsBoolean();
                setViewTextColorById(id,
                        pass ? R.color.colorExpeditionBtnGoodText : R.color.colorExpeditionBtnFailText,
                        pass ? R.color.colorExpeditionBtnGoodBack : R.color.colorExpeditionBtnFailBack);
            } else {
                setViewTextColorById(id, R.color.white, R.color.grey);
            }
        }
    }

    private JsonObject checkCondition(JsonObject expd_data) {
        if (expd_data == null || status_info == null || bonus_info == null) return null;

        boolean any_ship = ship_list.size() >= 1;
        boolean total_pass = any_ship;
        JsonObject result = new JsonObject();

        // region flagship & fleet check
        result.addProperty(FLAG_LV, any_ship);
        if (any_ship && expd_data.has(FLAG_LV)) {
            int lv = ship_list.get(0).get("lv").getAsInt();
            int req = expd_data.get(FLAG_LV).getAsInt();
            result.addProperty(FLAG_LV, lv >= req);
            // noinspection ConstantConditions
            total_pass = total_pass && (lv >= req);
        }

        result.addProperty(FLAG_COND, any_ship);
        if (any_ship && expd_data.has(FLAG_COND)) {
            int stype = ship_list.get(0).get("stype").getAsInt();
            boolean isCVE = KcaApiData.isShipCVE(ship_list.get(0).get("ship_id").getAsInt());

            boolean flag_pass = false;
            for (String stype_str : expd_data.get(FLAG_COND).getAsString().split("/")) {
                int req = Integer.parseInt(stype_str);

                if (stype == req || (stype == STYPE_CVE && isCVE)) {
                    flag_pass = true;
                    break;
                }
            }

            result.addProperty(FLAG_COND, flag_pass);
            total_pass = total_pass && flag_pass;
        }

        result.addProperty(FLEET_LV, any_ship);
        if (expd_data.has(FLEET_LV)) {
            int req = expd_data.get(FLEET_LV).getAsInt();
            int total = status_info.get(FLEET_LV).getAsInt();
            result.addProperty(FLEET_LV, total >= req);
            total_pass = total_pass && (total >= req);
        }

        result.addProperty(FLEET_NUM, true);
        if (expd_data.has(FLEET_NUM)) {
            int req = expd_data.get(FLEET_NUM).getAsInt();
            int total = ship_list.size();
            result.addProperty(FLEET_NUM, total >= req);
            total_pass = total_pass && (total >= req);
        }

        result.addProperty(FLEET_COND, true);
        if (expd_data.has(FLEET_COND)) {
            JsonObject res = checkFleetCondition(expd_data.get(FLEET_COND).getAsString());
            result.add(FLEET_COND, res.get("value"));
            total_pass = total_pass && res.get("pass").getAsBoolean();
        }
        // endregion

        // region drum check
        result.addProperty(DRUM_SHIPS, true);
        if (expd_data.has(DRUM_SHIPS)) {
            int req = expd_data.get(DRUM_SHIPS).getAsInt();
            int ships = bonus_info.get(DRUM_SHIPS).getAsInt();
            result.addProperty(DRUM_SHIPS, ships >= req);
            total_pass = total_pass && (ships >= req);
        }

        result.addProperty(DRUM_COUNT, true);
        if (expd_data.has(DRUM_COUNT)) {
            int req = expd_data.get(DRUM_COUNT).getAsInt();
            int count = bonus_info.get(DRUM_COUNT).getAsInt();
            result.addProperty(DRUM_COUNT, count >= req);
            total_pass = total_pass && (count >= req);
        }

        result.addProperty(DRUM_OPTIONAL, true);
        if (expd_data.has(DRUM_OPTIONAL)) {
            int req = expd_data.get(DRUM_OPTIONAL).getAsInt();
            int count = bonus_info.get(DRUM_COUNT).getAsInt();
            result.addProperty(DRUM_OPTIONAL, count >= req);
        }
        // endregion

        // region status check
        result.addProperty(TOTAL_FIRE, true);
        if (expd_data.has(TOTAL_FIRE)) {
            int req = expd_data.get(TOTAL_FIRE).getAsInt();
            int total = status_info.get(TOTAL_FIRE).getAsInt();
            result.addProperty(TOTAL_FIRE, total >= req);
            total_pass = total_pass && total >= req;
        }

        result.addProperty(TOTAL_ASW, true);
        if (expd_data.has(TOTAL_ASW)) {
            int req = expd_data.get(TOTAL_ASW).getAsInt();
            int total = status_info.get(TOTAL_ASW).getAsInt();
            result.addProperty(TOTAL_ASW, total >= req);
            total_pass = total_pass && total >= req;
        }

        result.addProperty(TOTAL_AA, true);
        if (expd_data.has(TOTAL_AA)) {
            int req = expd_data.get(TOTAL_AA).getAsInt();
            int total = status_info.get(TOTAL_AA).getAsInt();
            result.addProperty(TOTAL_AA, total >= req);
            total_pass = total_pass && total >= req;
        }

        result.addProperty(TOTAL_LOS, true);
        if (expd_data.has(TOTAL_LOS)) {
            int req = expd_data.get(TOTAL_LOS).getAsInt();
            int total = status_info.get(TOTAL_LOS).getAsInt();
            result.addProperty(TOTAL_LOS, total >= req);
            total_pass = total_pass && total >= req;
        }
        // endregion

        result.addProperty("pass", total_pass);
        return result;
    }

    /**
     * @return {@code List<List<[stype, count]>>}
     */
    private List<List<Pair<String, Integer>>> parseFleetCond(String cond_data) {

        return transform(Arrays.asList(cond_data.split("/")), cond_list -> {
            return transform(Arrays.asList(cond_list.split("\\|")), cond_str -> {
                String[] ship_cond = cond_str.split("-");

                return new Pair<>(ship_cond[0], Integer.parseInt(ship_cond[1]));
            });
        });
    }

    private JsonObject checkFleetCondition(String total_cond) {

        // stype to count map
        SparseIntArray stype_data = new SparseIntArray();

        for (JsonObject ship : ship_list) {
            int ship_id = ship.get("ship_id").getAsInt();
            int stype = ship.get("stype").getAsInt();

            if (KcaApiData.isShipCVE(ship_id)) {
                stype_data.put(STYPE_CVE, stype_data.get(STYPE_CVE, 0) + 1);
            }

            stype_data.put(stype, stype_data.get(stype, 0) + 1);
        }

        boolean total_pass = false;
        JsonArray total_check = new JsonArray();
        for (List<Pair<String, Integer>> cond_list : parseFleetCond(total_cond)) {
            JsonObject cond_check = new JsonObject();
            total_check.add(cond_check);

            boolean partial_pass = true;
            for (Pair<String, Integer> cond : cond_list) {
                String[] stype_list = cond.first.split(",");
                int count = 0;

                for (String stype : stype_list) {
                    count += stype_data.get(Integer.parseInt(stype), 0);
                }

                cond_check.addProperty(cond.first, cond.second <= count);
                partial_pass = partial_pass && cond.second <= count;
            }

            total_pass = total_pass || partial_pass;
        }

        JsonObject result = new JsonObject();
        result.addProperty("pass", total_pass);
        result.add("value", total_check);
        return result;
    }

    private void setSelectedExpd(int newValue) {
        if (!checkLoaded()) return;
        newValue = clamp(newValue, 0, expd_list.size() - 1); // 1 <= i <= 7, exclude 6
        selected_expd = newValue;

        Log.d(TAG, "expd: " + newValue);

        setExpdDetailLayout();
    }

    private void clearExpdDetailLayout() {
        // clear detail header
        setViewContentById(R.id.expd_title, "");
        setViewVisibilityById(R.id.expd_type, false);
        setViewVisibilityById(R.id.expd_ctype, false);
        setViewContentById(R.id.expd_time, "");
        setViewContentById(R.id.expd_reward_res, "0/0/0/0");
        setViewTextColorById(R.id.expd_reward_res, R.color.white);
        setViewContentById(R.id.expd_reward_item1, (Drawable) null);
        setViewContentById(R.id.expd_reward_item2, (Drawable) null);

        // clear conditions
        setViewsGone(R.id.expd_flagship_label, R.id.expd_flagship_lv, R.id.expd_flagship_cond);
        setViewsGone(R.id.expd_fleet_label, R.id.expd_fleet_total_num, R.id.expd_fleet_total_lv);
        this.<LinearLayout>findViewById(R.id.expd_fleet_cond).removeAllViews();
        setViewsGone(R.id.expd_firepower_label, R.id.expd_total_firepower, R.id.expd_fleet_firepower,
                R.id.expd_asw_label, R.id.expd_total_asw, R.id.expd_fleet_asw,
                R.id.expd_aa_label, R.id.expd_total_aa, R.id.expd_fleet_aa,
                R.id.expd_los_label, R.id.expd_total_los, R.id.expd_fleet_los);
        setViewsGone(R.id.expd_drum_label, R.id.expd_drum_ships, R.id.expd_drum_count, R.id.expd_drum_optional);
    }

    public void setExpdDetailLayout() {
        JsonObject data = KcaApiData.getExpeditionInfo(expd_list.get(selected_expd), locale);
        Log.d(TAG, "data: " + data);

        clearExpdDetailLayout();

        // region header
        setViewContentById(R.id.expd_title,
                getExpeditionHeader(data.get("no").getAsInt()) + data.get("name").getAsString());

        List<String> features = data.has("features")
                ? Arrays.asList(gson.fromJson(data.get("features"), String[].class))
                : Collections.emptyList();

        if (features.contains("drum")) {
            setViewVisibilityById(R.id.expd_type, true);
            setViewContentById(R.id.expd_type, R.string.expd_type_drum);
        }
        if (features.contains("flag_lv")) {
            setViewVisibilityById(R.id.expd_type, true);
            setViewContentById(R.id.expd_type, R.string.expd_type_flagship_lv);
        }
        if (features.contains("combat_1")) {
            setViewVisibilityById(R.id.expd_ctype, true);
            setViewContentById(R.id.expd_ctype, R.string.expd_ctype_1);
            setViewTextColorById(R.id.expd_ctype, R.color.colorExpeditionCombatI);
        }
        if (features.contains("combat_2")) {
            setViewVisibilityById(R.id.expd_ctype, true);
            setViewContentById(R.id.expd_ctype, R.string.expd_ctype_2);
            setViewTextColorById(R.id.expd_ctype, R.color.colorExpeditionCombatII);
        }
//        if (features.contains("support")) {}
//        if (features.contains("monthly")) {}

        setViewContentById(R.id.expd_time, getTimeStr(data.get("time").getAsInt() * 60));

        JsonArray resource = data.getAsJsonArray("resource");
        int fuel = applyBonus(resource.get(0).getAsInt(), isGreatSuccess);
        int ammo = applyBonus(resource.get(1).getAsInt(), isGreatSuccess);
        int steel = applyBonus(resource.get(2).getAsInt(), isGreatSuccess);
        int bauxite = applyBonus(resource.get(3).getAsInt(), isGreatSuccess);

        String resource_text = KcaUtils.format("%d/%d/%d/%d", fuel, ammo, steel, bauxite);
        setViewContentById(R.id.expd_reward_res, resource_text);
        if (isGreatSuccess) {
            setViewTextColorById(R.id.expd_reward_res, R.color.colorExpeditionGreatSuccess);
        }

        JsonArray reward = data.getAsJsonArray("reward");
        int reward1 = reward.get(0).getAsJsonArray().get(0).getAsInt();
        int reward2 = reward.get(1).getAsJsonArray().get(0).getAsInt();
        try {
            if (reward1 > 0) {
                setViewContentById(R.id.expd_reward_item1, getId("common_itemicons_id_" + reward1, R.mipmap.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "Item image not found for item: " + reward1);
        }

        try {
            if (reward2 > 0) {
                setViewContentById(R.id.expd_reward_item1, getId("common_itemicons_id_" + reward2, R.mipmap.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "Item image not found for item: " + reward2);
        }
        // endregion

        JsonObject check = check_data.get(data.get("no").getAsInt());
        if (check == null) return;
        Log.d(TAG, "check: " + check);

        setViewTextColorById(R.id.expd_title, getColorByCond(check.get("pass").getAsBoolean()));

        // region flagship cond
        if (data.has(FLAG_LV)) {
            setViewVisibilityById(R.id.expd_flagship_label, true);
            setViewContentById(R.id.expd_flagship_lv,
                    format(R.string.excheckview_flag_lv_format, data.get(FLAG_LV).getAsInt()),
                    getColorByCond(check.get(FLAG_LV).getAsBoolean()));
        }

        if (data.has(FLAG_COND)) {
            setViewVisibilityById(R.id.expd_flagship_label, true);

            List<String> stype_list = transform(
                    Arrays.asList(data.get(FLAG_COND).getAsString().split("/")),
                    (str) -> getShipTypeAbbr(Integer.parseInt(str)));

            setViewContentById(R.id.expd_flagship_cond,
                    KcaUtils.joinStr(stype_list, "/"),
                    getColorByCond(check.get(FLAG_COND).getAsBoolean()));
        }
        // endregion

        // region fleet cond
        if (data.has(FLEET_NUM)) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewContentById(R.id.expd_fleet_total_num,
                    format(R.string.excheckview_total_num_format, data.get(FLEET_NUM).getAsInt()),
                    getColorByCond(check.get(FLEET_NUM).getAsBoolean()));
        }

        if (data.has(FLEET_LV)) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewContentById(R.id.expd_fleet_total_lv,
                    format(R.string.excheckview_total_lv_format, data.get(FLEET_LV).getAsInt()),
                    getColorByCond(check.get(FLEET_LV).getAsBoolean()));
        }

        if (data.has(FLEET_COND)) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewVisibilityById(R.id.expd_fleet_cond, true);
            String total_cond = data.get(FLEET_COND).getAsString();
            LinearLayout layout = findViewById(R.id.expd_fleet_cond);
            for (View v : generateCondView(total_cond, check.getAsJsonArray(FLEET_COND))) {
                layout.addView(v);
            }
        }
        // endregion

        // region drum cond
        if (data.has(DRUM_SHIPS)) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_ships,
                    format(R.string.excheckview_drum_ship_format, data.get(DRUM_SHIPS).getAsInt()),
                    getColorByCond(check.get(DRUM_SHIPS).getAsBoolean()));
        }

        if (data.has(DRUM_COUNT)) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_count,
                    format(R.string.excheckview_drum_num_format, data.get(DRUM_COUNT).getAsInt()),
                    getColorByCond(check.get(DRUM_COUNT).getAsBoolean()));
        }

        if (data.has(DRUM_OPTIONAL)) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_optional,
                    format(R.string.excheckview_drum_num_format, data.get(DRUM_OPTIONAL).getAsInt()),
                    getColorByCond(check.get(DRUM_OPTIONAL).getAsBoolean(), true));
        }
        // endregion

        // region status cond
        boolean isCombat2 = features.contains("combat_2");

        if (data.has(TOTAL_FIRE)) {
            int req = data.get(TOTAL_FIRE).getAsInt();
            boolean pass = check.get(TOTAL_FIRE).getAsBoolean();
            float fleet = status_info.get(TOTAL_FIRE).getAsFloat();

            setViewVisibilityById(R.id.expd_firepower_label, true);
            setViewContentById(R.id.expd_total_firepower,
                    format(R.string.excheckview_total_format, req),
                    getColorByCond(pass));

            if (!pass) {
                setViewContentById(R.id.expd_fleet_firepower,
                        format(R.string.excheckview_cur_total_format, fleet),
                        getColorByCond(false));
            }

            if (pass && isCombat2) {
                setViewContentById(R.id.expd_fleet_firepower,
                        format(R.string.excheckview_per_format, fleet / req * 100),
                        getColorByCond(fleet >= req * 2.17, true));
            }
        }

        if (data.has(TOTAL_ASW)) {
            int req = data.get(TOTAL_ASW).getAsInt();
            boolean pass = check.get(TOTAL_ASW).getAsBoolean();
            float fleet = status_info.get(TOTAL_ASW).getAsFloat();

            setViewVisibilityById(R.id.expd_asw_label, true);
            setViewContentById(R.id.expd_total_asw,
                    format(R.string.excheckview_total_format, req),
                    getColorByCond(pass));

            if (!pass) {
                setViewContentById(R.id.expd_fleet_asw,
                        format(R.string.excheckview_cur_total_format, fleet),
                        getColorByCond(false));
            }

            if (pass && isCombat2) {
                setViewContentById(R.id.expd_fleet_asw,
                        format(R.string.excheckview_per_format, fleet / req * 100),
                        getColorByCond(fleet >= req * 2.17, true));
            }
        }

        if (data.has(TOTAL_AA)) {
            int req = data.get(TOTAL_AA).getAsInt();
            boolean pass = check.get(TOTAL_AA).getAsBoolean();
            float fleet = status_info.get(TOTAL_AA).getAsFloat();

            setViewVisibilityById(R.id.expd_aa_label, true);
            setViewContentById(R.id.expd_total_aa,
                    format(R.string.excheckview_total_format, req),
                    getColorByCond(pass));

            if (!pass) {
                setViewContentById(R.id.expd_fleet_aa,
                        format(R.string.excheckview_cur_total_format, fleet),
                        getColorByCond(false));
            }

            if (pass && isCombat2) {
                setViewContentById(R.id.expd_fleet_aa,
                        format(R.string.excheckview_per_format, fleet / req * 100),
                        getColorByCond(fleet >= req * 2.17, true));
            }
        }

        if (data.has(TOTAL_LOS)) {
            int req = data.get(TOTAL_LOS).getAsInt();
            boolean pass = check.get(TOTAL_LOS).getAsBoolean();
            float fleet = status_info.get(TOTAL_LOS).getAsFloat();

            setViewVisibilityById(R.id.expd_los_label, true);
            setViewContentById(R.id.expd_total_los,
                    format(R.string.excheckview_total_format, req),
                    getColorByCond(pass));

            if (!pass) {
                setViewContentById(R.id.expd_fleet_los,
                        format(R.string.excheckview_cur_total_format, fleet),
                        getColorByCond(false));
            }

            if (pass && isCombat2) {
                setViewContentById(R.id.expd_fleet_los,
                        format(R.string.excheckview_per_format, fleet / req * 100),
                        getColorByCond(fleet >= req * 2.17, true));
            }
        }
        // endregion
    }

    /**
     * @param cond_data {@code "1-2|2-3/1,2,3-4"} means ( DE*2 + DD*3 ) or ( (DE+DD+CL)*4 )
     */
    private List<List<JsonObject>> convertTotalCond(String cond_data, JsonArray check_data) {

        List<List<JsonObject>> condList = Lists.newArrayList();

        int i = 0;
        for (String org_cond_str : cond_data.split("/")) {
            List<JsonObject> org_cond = Lists.newArrayList();
            condList.add(org_cond);

            for (String ship_cond_str : org_cond_str.split("\\|")) {
                JsonObject ship_cond = new JsonObject();
                org_cond.add(ship_cond);

                String stypes = ship_cond_str.split("-")[0];
                int count = Integer.parseInt(ship_cond_str.split("-")[1]);

                String str;
                if (stypes.equals(CARRIERS)) {
                    str = getStringWithLocale(R.string.excheckview_ship_cvs);
                } else {
                    List<String> abbr = transform(Arrays.asList(stypes.split(",")),
                            stype -> getShipTypeAbbr(Integer.parseInt(stype)));
                    str = joinStr(abbr, "/");
                }

                ship_cond.addProperty("pass", check_data.get(i).getAsJsonObject().get(stypes).getAsBoolean());
                ship_cond.addProperty("text", format(R.string.excheckview_ship_cond, str, count));
            }

            i++;
        }

        return condList;
    }

    private TextView getTextViewFor(String text, boolean is_pass) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.FloatingWindow_TextAppearance_Normal);
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(), getColorByCond(is_pass)));
        return tv;
    }

    private List<View> generateCondView(String data, JsonArray check) {

        LinearLayout.LayoutParams row_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        int margin4dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getApplicationContext().getResources().getDisplayMetrics());
        text_params.setMargins(0, 0, margin4dp, 0);

        return transform(convertTotalCond(data, check),
                cond_list -> {
                    LinearLayout row = new LinearLayout(this);
                    row.setLayoutParams(row_params);
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    for (JsonObject cond : cond_list) {
                        String text = cond.get("text").getAsString();
                        boolean pass = cond.get("pass").getAsBoolean();
                        row.addView(getTextViewFor(text, pass), text_params);
                    }

                    return row;
                });
    }

    private void setFleetInfoView() {
        List<String> info = new ArrayList<>();

        info.add(format(R.string.excheckview_bonus_lv, status_info.get(FLEET_LV).getAsInt()));
        info.add(format(R.string.excheckview_bonus_firepower, status_info.get(TOTAL_FIRE).getAsInt()));
        info.add(format(R.string.excheckview_bonus_asw, status_info.get(TOTAL_ASW).getAsInt()));
        info.add(format(R.string.excheckview_bonus_aa, status_info.get(TOTAL_AA).getAsInt()));
        info.add(format(R.string.excheckview_bonus_los, status_info.get(TOTAL_LOS).getAsInt()));

        int drum_count = bonus_info.get(DRUM_COUNT).getAsInt();
        int drum_ships = bonus_info.get(DRUM_SHIPS).getAsInt();
        if (drum_count >= 1) {
            info.add(format(R.string.excheckview_bonus_drum_carrying, drum_count, drum_ships));
        } else {
            info.add(format(R.string.excheckview_bonus_drum, 0));
        }

        if (bonus_info.get(KINU_EXISTS).getAsBoolean()) {
            info.add(getStringWithLocale(R.string.excheckview_bonus_kinu));
        }

        int dlcs_count = bonus_info.get(DLCS_COUNT).getAsInt();
        if (dlcs_count > 0) {
            info.add(format(R.string.excheckview_bonus_dlcs, dlcs_count));
        }

        String bonus_text = joinStr(info, " / ");
        float dlcs_bonus = bonus_info.get(DLCS_BONUS).getAsFloat();
        float toku_bonus = bonus_info.get(TOKU_BONUS).getAsFloat();
        if (dlcs_bonus + toku_bonus > 0f) {
            bonus_text += format(R.string.excheckview_bonus_result, (dlcs_bonus + toku_bonus) * 100f);
        }

        setViewContentById(R.id.expd_fleet_info, bonus_text);
        findViewById(R.id.expd_fleet_info)
                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
    }

    // region utilities
    private String getResName(@AnyRes int res) {
        try {
            return getResources().getResourceEntryName(res);
        } catch (Resources.NotFoundException ignored) {
            return String.valueOf(res);
        }
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    private String format(@StringRes int formatResId, Object... args) {
        return KcaUtils.format(getStringWithLocale(formatResId), args);
    }

    @NonNull
    private <T extends View> T findViewById(@IdRes int id) {
        try {
            return Objects.requireNonNull(mView.findViewById(id));
        } catch (NullPointerException e) {
            throw new NullPointerException("View not found, id: " + getResName(id));
        }
    }

    private void setViewVisibilityById(@IdRes int id, boolean visibility) {
        int visible_value = visibility ? VISIBLE : GONE;
        mView.findViewById(id).setVisibility(visible_value);
    }

    private void setViewContentById(@IdRes int id, String text) {
        this.<TextView>findViewById(id).setText(text);
    }

    private void setViewContentById(@IdRes int id, @Nullable Drawable drawable) {
        this.<ImageView>findViewById(id).setImageDrawable(drawable);
    }

    private void setViewContentById(@IdRes int id, @StringRes @DrawableRes int resId) {
        View v = findViewById(id);
        if (v instanceof ImageView) ((ImageView) v).setImageResource(resId);
        if (v instanceof TextView) ((TextView) v).setText(getStringWithLocale(resId));
    }

    private void setViewTextColorById(@IdRes int id, @ColorRes int resId) {
        this.<TextView>findViewById(id)
                .setTextColor(ContextCompat.getColor(getApplicationContext(), resId));
    }

    private void setViewTextColorById(@IdRes int id, @ColorRes int fgId, @ColorRes int bgId) {
        this.<TextView>findViewById(id)
                .setTextColor(ContextCompat.getColor(getApplicationContext(), fgId));
        this.<TextView>findViewById(id)
                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), bgId));
    }

    @ColorRes private int getColorByCond(boolean is_pass, boolean is_option) {
        return is_pass ? R.color.colorExpeditionBtnGoodBack
                : is_option ? R.color.grey
                : R.color.colorExpeditionBtnFailBack;
    }

    @ColorRes private int getColorByCond(boolean is_pass) {
        return getColorByCond(is_pass, false);
    }

    private void setViewContentById(@IdRes int id, String content, @ColorRes int color) {
        setViewVisibilityById(id, true);
        setViewContentById(id, content);
        setViewTextColorById(id, color);
    }

    private void setViewsGone(@IdRes int... id) {
        for (int view_id : id) {
            setViewVisibilityById(view_id, false);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkLoaded() {
        if (!checkUserShipDataLoaded()) {
            setViewContentById(R.id.expd_fleet_info, R.string.kca_init_content);
            findViewById(R.id.expd_fleet_info).setBackgroundColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            return false;
        }

        return true;
    }

    private static int clamp(int x, int min, int max) {
        return min(max(x, min), max);
    }

    private static double floor01(double x) {
        return floor(x * 10f) / 10f;
    }
    // endregion
}
