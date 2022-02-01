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
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
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

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    // region constants
    public static final String TAG = "KCA/EXPD";
    public static final String SHOW_EXCHECKVIEW_ACTION = "show_excheckview";

    private static final String USER_SHIP_REQ_LIST = "ship_id,lv,slot,cond,karyoku,taisen,taiku,sakuteki";
    private static final String KC_SHIP_REQ_LIST = "stype";
    private static final String USER_EQUIP_REQ_LIST = "slotitem_id,level";
    private static final String KC_EQUIP_REQ_LIST = "type,tais,tyku,saku";

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
    private static final String HAS_AIRCRAFT = "has-aircraft";
    private static final String GS_RATE_NORMAL = "gs-normal";
    private static final String GS_RATE_DRUM_1 = "gs-drum"; // when < drum-num-optional
    private static final String GS_RATE_DRUM_2 = "gs-drum-2"; // when >= drum-num-optional
    private static final String GS_RATE_FLAGSHIP = "gs-flagship";

    /**
     * the last selected world and expedition
     * format: <selected_world>[;<world>,<selected_expd_id>...]
     */
    private static final String EXPD_SELECTION_KEY = KcaConstants.PREF_EXPD_SELECTION;
    // endregion

    private String locale;
    private View mView;
    private final Gson gson = new Gson();

    private int selected_fleet = 1;
    private int selected_world = 1;
    /**
     * selected expd index of each world
     */
    private final SparseIntArray selected_expd = new SparseIntArray();
    private boolean isGreatSuccess = false;

    /**
     * ship id list of each fleet
     */
    private final List<List<Integer>> fleet_list = new ArrayList<>(); // set by onStartCommand
    private final List<JsonObject> ship_list = new ArrayList<>(); // set by updateFleet
    /**
     * expd id list in current world
     */
    private final List<Integer> expd_list = new ArrayList<>(); // set by updateWorld
    private JsonObject fleet_info; // set by updateFleetInfo
    /**
     * expd id to check result map
     */
    private final SparseArray<JsonObject> check_data = new SparseArray<>(); // set by updateCheckData

    // region lifecycle
    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            Log.e(TAG, "Can not draw overlays");
            stopSelf();
        }

        try {
            locale = LocaleUtils.getResourceLocaleCode(KcaUtils.getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));

            initView();

            // region load selection
            String selection = KcaUtils.getStringPreferences(this, EXPD_SELECTION_KEY);
            Log.d(TAG, "restore selection: " + selection);
            Iterator<String> data = Splitter.on(CharMatcher.anyOf(";,")).split(selection).iterator();
            if (data.hasNext()) {
                selected_world = parseIntOrDefault(data.next(), selected_world);

                while (data.hasNext()) {
                    int key = parseIntOrDefault(data.next(), -1);
                    if (!data.hasNext()) break; // lack
                    int expd = parseIntOrDefault(data.next(), -1);
                    if (key == -1 || expd == -1) continue; // invalid value
                    selected_expd.put(key, expd);
                }
            }
            // endregion
        } catch (Exception e) {
            Log.e(TAG, "exception while onCreate", e);
            Toast.makeText(getApplicationContext(), getStringFromException(e), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @SuppressLint("InflateParams")
    public void initView() {
        Context contextWithLocale = KcaUtils.getContextWithLocale(getApplicationContext(), getBaseContext());
        mView = LayoutInflater.from(contextWithLocale).inflate(R.layout.view_excheck_list, null);
        KcaUtils.resizeFullWidthView(getApplicationContext(), mView);

        findViewById(R.id.window_head).setOnClickListener((v) -> stopSelf());
        findViewById(R.id.window_exit).setOnClickListener((v) -> stopSelf());
        for (int id : this.<Flow>findViewById(R.id.expd_fleet_tab).getReferencedIds())
            findViewById(id).setOnClickListener(this::onClickFleetTab);
        for (int id : this.<Flow>findViewById(R.id.expd_worlds).getReferencedIds())
            findViewById(id).setOnClickListener(this::onClickWorldTab);
        for (int id : this.<Flow>findViewById(R.id.expd_table).getReferencedIds())
            findViewById(id).setOnClickListener(this::onClickExpdButton);
        findViewById(R.id.expd_reward).setOnClickListener(this::onToggleRewardButton);

        clearExpdDetailLayout();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        mView.setLayoutParams(params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);

        // when onStartCommand returns START_NOT_STICKY, intent must not be null.
//        if (intent == null || intent.getAction() == null
//                || !intent.getAction().startsWith(SHOW_EXCHECKVIEW_ACTION)) {
//            return super.onStartCommand(intent, flags, startId);
//        }

        try (KcaDBHelper db = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION)) {

            fleet_list.clear();
            for (JsonElement fleet : db.getJsonArrayValue(DB_KEY_DECKPORT)) {
                int[] ships = gson.fromJson(fleet.getAsJsonObject().get("api_ship"), int[].class);
                fleet_list.add(Ints.asList(ships));
            }

            if (fleet_list.size() <= 1) {
                stopSelf();
                throw new IllegalArgumentException("at least 2nd fleets is required, fleet_list: " + fleet_list);
            }

            if (intent.getAction() != null) {
                String fleet = Iterables.get(Splitter.on('/').split(intent.getAction()), 1, "1");
                selected_fleet = parseIntOrDefault(fleet, selected_fleet);
            }

            updateFleet();
            updateWorld();
            updateCheckData(true);
            setSelectedExpd(selected_expd.get(selected_world, 0));

            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (mView.getParent() != null) {
                wm.removeViewImmediate(mView);
            }
            wm.addView(mView, mView.getLayoutParams());

        } catch (Exception e) {
            Log.e(TAG, "exception while onStartCommand", e);
            Toast.makeText(getApplicationContext(), getStringFromException(e), Toast.LENGTH_LONG).show();
            stopSelf();
        }

        return START_NOT_STICKY; // this service does not have to be restarted.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mView != null && mView.getParent() != null) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.removeViewImmediate(mView);
        }

        // region save selection
        List<String> data = Lists.newArrayList(String.valueOf(selected_world));
        for (int i = 0, size = selected_expd.size(); i < size; i++) {
            data.add(KcaUtils.format("%d,%d", selected_expd.keyAt(i), selected_expd.valueAt(i)));
        }
        Log.d(TAG, "save selection: " + Joiner.on(';').join(data));
        KcaUtils.setPreferences(this, EXPD_SELECTION_KEY, Joiner.on(';').join(data));
        // endregion

        super.onDestroy();
    }

    private void onClickExpdButton(View v) {
        Flow table = findViewById(R.id.expd_table);
        int expd = Ints.indexOf(table.getReferencedIds(), v.getId());
        if (expd == -1) {
            Log.e(TAG, "can not find clicked view, id: " + getResName(v.getId()));
            return;
        }
        if (expd != selected_expd.get(selected_world, -1)) setSelectedExpd(expd);
    }

    private void onClickWorldTab(View v) {
        try {
            int world = Integer.parseInt((String) v.getTag());
            if (world != selected_world) setSelectedWorld(world);
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG, "can not find clicked view, id: " + getResName(v.getId()) + ", tag: " + v.getTag(), e);
        }
    }

    private void onClickFleetTab(View v) {
        try {
            int fleet = Integer.parseInt((String) v.getTag()) - 1 /* fleet id is 0(main) <= id <= 3 */;
            if (fleet != selected_fleet) setSelectedFleet(fleet);
        } catch (ClassCastException | NumberFormatException e) {
            Log.e(TAG, "can not find clicked view, id: " + getResName(v.getId()) + ", tag: " + v.getTag(), e);
        }
    }

    private void onToggleRewardButton(View v) {
        isGreatSuccess = !isGreatSuccess;
        setExpdDetailLayout();
    }
    // endregion lifecycle

    // region data update
    private void setSelectedFleet(int newValue) {
        newValue = clamp(newValue, 1, fleet_list.size()); // 1 <= i < size
        selected_fleet = newValue;

        Log.d(TAG, "fleet: " + newValue);

        updateFleet();
        updateCheckData(true);
        setExpdDetailLayout();
    }

    private void updateFleet() {
        selected_fleet = clamp(selected_fleet, 1, fleet_list.size()); // 1 <= i < size

        for (int id : this.<Flow>findViewById(R.id.expd_fleet_tab).getReferencedIds()) {
            View v = findViewById(id);
            v.setBackgroundColor(selected_fleet + 1 == Integer.parseInt((String) v.getTag())
                    ? ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)
                    : ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
        }

        if (!checkUserShipDataLoaded())
            return; // view will be updated even when the load isn't completed

        updateFleetInfo();
        setFleetInfoView();

        Log.d(TAG, "ship_list[" + ship_list.size() + "]: " + ship_list);
        Log.d(TAG, "fleet_info: " + fleet_info);
    }

    private void updateFleetInfo() {
        ship_list.clear();
        for (int id : fleet_list.get(selected_fleet)) {
            if (id <= 0) continue;

            JsonObject data = new JsonObject();
            JsonObject userData = getUserShipDataById(id, USER_SHIP_REQ_LIST);
            int kc_ship_id = userData.get("ship_id").getAsInt();
            JsonObject kcData = getKcShipDataById(kc_ship_id, KC_SHIP_REQ_LIST);

            data.add("ship_id", userData.get("ship_id"));
            data.add("lv", userData.get("lv"));
            data.add("cond", userData.get("cond"));
            data.add("stype", kcData == null ? new JsonPrimitive(0) : kcData.get("stype"));
            data.add("karyoku", userData.getAsJsonArray("karyoku").get(0));
            data.add("taisen", userData.getAsJsonArray("taisen").get(0));
            data.add("taiku", userData.getAsJsonArray("taiku").get(0));
            data.add("sakuteki", userData.getAsJsonArray("sakuteki").get(0));
            data.add("item", new JsonArray());
            for (int item_id : gson.fromJson(userData.get("slot"), int[].class)) {
                if (item_id <= 0) continue;

                JsonObject itemData = getUserItemStatusById(item_id, USER_EQUIP_REQ_LIST, KC_EQUIP_REQ_LIST);
                if (itemData != null) data.getAsJsonArray("item").add(itemData);
            }

            if (kcData == null) {
                Log.e(TAG, "cannot find kc data for ship_id: " + kc_ship_id);
                continue;
            }

            ship_list.add(data);
        }

        JsonObject result = new JsonObject();
        getStatusInfo(ship_list, result);
        getGSInfo(ship_list, result);
        getBonusInfo(ship_list, result);
        fleet_info = result;
    }

    /**
     * <a href="https://wikiwiki.jp/kancolle/%E9%81%A0%E5%BE%81#about_stat">wikiwiki</a>
     * <a href="https://kitongame.com/%E3%80%90%E8%89%A6%E3%81%93%E3%82%8C%E3%80%91%E3%83%9E%E3%83%B3%E3%82%B9%E3%83%AA%E3%83%BC%E9%81%A0%E5%BE%81%E3%81%AE%E6%88%90%E5%8A%9F%E6%9D%A1%E4%BB%B6%E3%81%A8%E7%8D%B2%E5%BE%97%E8%B3%87%E6%9D%90/">kiton</a>
     */
    private static void getStatusInfo(List<JsonObject> ship_list, JsonObject result) {
        int flag_lv = ship_list.isEmpty() ? -1 : ship_list.get(0).get("lv").getAsInt();

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

        result.addProperty(FLAG_LV, flag_lv);
        result.addProperty(FLEET_LV, total_lv);
        result.addProperty(TOTAL_FIRE, total_fire);
        result.addProperty(TOTAL_ASW, total_asw);
        result.addProperty(TOTAL_AA, total_aa);
        result.addProperty(TOTAL_LOS, total_los);
        result.addProperty(HAS_AIRCRAFT, has_aircraft);
    }

    /**
     * <a href="https://twitter.com/jo_swaf/status/1283626757367590913/photo/2">twitter @jo_swaf: great success rate</a>
     * <a href="https://docs.google.com/spreadsheets/d/1bKwHBxtQhNfxE4PAzYaFlbiO_soQTFi3dG1U4nh9njs/edit#gid=330967444">Spreadsheet</a>
     */
    private static void getGSInfo(List<JsonObject> ship_list, JsonObject result) {
        if (ship_list.isEmpty()) { // if fleet is empty, set all rate to 0, return
            result.addProperty(GS_RATE_NORMAL, 0);
            result.addProperty(GS_RATE_DRUM_1, 0);
            result.addProperty(GS_RATE_DRUM_2, 0);
            result.addProperty(GS_RATE_FLAGSHIP, 0);
            return;
        }

        int flag_lv = ship_list.get(0).get("lv").getAsInt();
        int sparkles = 0;
        boolean all_sparkle = true;
        for (JsonObject ship : ship_list) {
            if (ship.get("cond").getAsInt() >= 50) sparkles += 1;
            else all_sparkle = false;
        }

        int base = 20 + sparkles * 15 + 1;
        int flagship = -5 + (int) floor(sqrt(flag_lv) + flag_lv / 10.0);
        result.addProperty(GS_RATE_NORMAL, all_sparkle ? base : 0);
        result.addProperty(GS_RATE_DRUM_1, base - 15);
        result.addProperty(GS_RATE_DRUM_2, base + 20);
        result.addProperty(GS_RATE_FLAGSHIP, base + flagship);
    }

    /**
     * <a href="https://wikiwiki.jp/kancolle/%E6%94%B9%E4%BF%AE%E5%B7%A5%E5%BB%A0#abf2415c">wikiwiki: daihatsu bonus</a>
     * <a href="https://wikiwiki.jp/kancolle/%E7%89%B9%E5%A4%A7%E7%99%BA%E5%8B%95%E8%89%87/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%881#:~:text=%E7%89%B9%E5%A4%A7%E7%99%BA%E8%A3%9C%E6%AD%A3%E3%81%A7,%E9%87%91)%2018%3A10%3A58">wikiwiki: toku daihatsu bonus</a>
     */
    private static void getBonusInfo(List<JsonObject> ship_list, JsonObject result) {
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

        result.addProperty(KINU_EXISTS, kinu_exist);
        result.addProperty(DRUM_COUNT, drum_count);
        result.addProperty(DRUM_SHIPS, drum_ships);
        result.addProperty(DLCS_COUNT, all_dlcs_count);
        result.addProperty(DLCS_BONUS, sum_of_bonus + 0.01f * sum_of_bonus * avg_of_level);
        result.addProperty(TOKU_BONUS, toku_bonus);
    }

    private void setSelectedWorld(int newValue) {
        newValue = clamp(newValue, 1, 7); // 1 <= i <= 7, exclude 6
        selected_world = newValue;

        Log.d(TAG, "world: " + newValue);

        updateWorld();
        updateCheckData(false);
        setSelectedExpd(selected_expd.get(newValue, 0)); // restore the last selection or the first expd of the world
    }

    private void updateWorld() {
        selected_world = clamp(selected_world, 1, 7); // 1 <= i <= 7, exclude 6

        for (int id : this.<Flow>findViewById(R.id.expd_worlds).getReferencedIds()) {
            View v = findViewById(id);
            v.setBackgroundColor(selected_world == Integer.parseInt((String) v.getTag())
                    ? ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)
                    : ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
        }

        if (!KcaApiData.isExpeditionDataLoaded())
            return; // view will be updated even when the load isn't completed

        expd_list.clear();
        expd_list.addAll(KcaApiData.getExpeditionNumByWorld(selected_world));
        Log.d(TAG, "expd_list[" + expd_list.size() + "]: " + expd_list);

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

    private void updateCheckData(boolean isFleetChanged) {
        if (!checkUserShipDataLoaded() || fleet_info == null) return;

        // if fleet wasn't changed, don't delete, just append.
        if (isFleetChanged) check_data.clear();

        for (int expd : expd_list) {
            check_data.put(expd, checkCondition(KcaApiData.getExpeditionInfo(expd, locale)));
        }

        Log.d(TAG, "check_data[" + check_data.size() + "]: " + check_data);

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
        if (expd_data == null || fleet_info == null) return null;

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
            int total = fleet_info.get(FLEET_LV).getAsInt();
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
            int ships = fleet_info.get(DRUM_SHIPS).getAsInt();
            result.addProperty(DRUM_SHIPS, ships >= req);
            total_pass = total_pass && (ships >= req);
        }

        result.addProperty(DRUM_COUNT, true);
        if (expd_data.has(DRUM_COUNT)) {
            int req = expd_data.get(DRUM_COUNT).getAsInt();
            int count = fleet_info.get(DRUM_COUNT).getAsInt();
            result.addProperty(DRUM_COUNT, count >= req);
            total_pass = total_pass && (count >= req);
        }

        result.addProperty(DRUM_OPTIONAL, true);
        if (expd_data.has(DRUM_OPTIONAL)) {
            int req = expd_data.get(DRUM_OPTIONAL).getAsInt();
            int count = fleet_info.get(DRUM_COUNT).getAsInt();
            result.addProperty(DRUM_OPTIONAL, count >= req);
        }
        // endregion

        // region status check
        result.addProperty(TOTAL_FIRE, true);
        if (expd_data.has(TOTAL_FIRE)) {
            int req = expd_data.get(TOTAL_FIRE).getAsInt();
            int total = fleet_info.get(TOTAL_FIRE).getAsInt();
            result.addProperty(TOTAL_FIRE, total >= req);
            total_pass = total_pass && total >= req;
        }

        result.addProperty(TOTAL_ASW, true);
        if (expd_data.has(TOTAL_ASW)) {
            int req = expd_data.get(TOTAL_ASW).getAsInt();
            int total = fleet_info.get(TOTAL_ASW).getAsInt();
            result.addProperty(TOTAL_ASW, total >= req);
            total_pass = total_pass && total >= req;
        }

        result.addProperty(TOTAL_AA, true);
        if (expd_data.has(TOTAL_AA)) {
            int req = expd_data.get(TOTAL_AA).getAsInt();
            int total = fleet_info.get(TOTAL_AA).getAsInt();
            result.addProperty(TOTAL_AA, total >= req);
            total_pass = total_pass && total >= req;
        }

        result.addProperty(TOTAL_LOS, true);
        if (expd_data.has(TOTAL_LOS)) {
            int req = expd_data.get(TOTAL_LOS).getAsInt();
            int total = fleet_info.get(TOTAL_LOS).getAsInt();
            result.addProperty(TOTAL_LOS, total >= req);
            total_pass = total_pass && total >= req;
        }
        // endregion

        result.addProperty("pass", total_pass);
        return result;
    }

    private String[] parseTotalCond(String total_cond) {
        return total_cond.split("/");
    }

    private List<Pair<String, Integer>> parseFleetCond(String fleet_cond) {
        return transform(Arrays.asList(fleet_cond.split("\\|")), stype_cond -> {
            String[] cond = stype_cond.split("-");
            return new Pair<>(cond[0], Integer.parseInt(cond[1]));
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
        for (String fleet_cond : parseTotalCond(total_cond)) {
            JsonObject cond_check = new JsonObject();
            total_check.add(cond_check);

            boolean partial_pass = true;
            for (Pair<String, Integer> cond : parseFleetCond(fleet_cond)) {

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
        newValue = clamp(newValue, 0, expd_list.size() - 1); // 1 <= i <= 7, exclude 6
        selected_expd.put(selected_world, newValue);

        Log.d(TAG, "expd: " + newValue);

        setExpdDetailLayout();
    }
    // endregion data update

    // region layout update
    private void clearExpdDetailLayout() {
        // clear detail header
        setViewContentById(R.id.expd_id, "");
        setViewContentById(R.id.expd_title, "");
        setViewVisibilityById(R.id.expd_type, false);
        setViewVisibilityById(R.id.expd_ctype, false);
        setViewVisibilityById(R.id.expd_gs_rate, false);
        setViewContentById(R.id.expd_time, "");
        setViewContentById(R.id.expd_reward_res, "0/0/0/0");
        setViewTextColorById(R.id.expd_reward_res, R.color.white);
        setViewContentById(R.id.expd_reward_item1, (Drawable) null);
        setViewContentById(R.id.expd_reward_item2, (Drawable) null);

        // clear conditions
        setViewsGone(R.id.expd_flagship_label, R.id.expd_flagship_lv, R.id.expd_flagship_cond);
        setViewsGone(R.id.expd_fleet_label, R.id.expd_fleet_total_num, R.id.expd_fleet_total_lv);
        this.<LinearLayout>findViewById(R.id.expd_fleet_cond).removeAllViews();
        setViewsGone(R.id.expd_fire_label, R.id.expd_total_fire, R.id.expd_fleet_fire,
                R.id.expd_asw_label, R.id.expd_total_asw, R.id.expd_fleet_asw,
                R.id.expd_aa_label, R.id.expd_total_aa, R.id.expd_fleet_aa,
                R.id.expd_los_label, R.id.expd_total_los, R.id.expd_fleet_los);
        setViewsGone(R.id.expd_drum_label, R.id.expd_drum_ships, R.id.expd_drum_count, R.id.expd_drum_optional);
    }

    public void setExpdDetailLayout() {
        if (!KcaApiData.isExpeditionDataLoaded()) return;

        int expd_id = expd_list.get(selected_expd.get(selected_world, 0));
        JsonObject data = KcaApiData.getExpeditionInfo(expd_id, locale);
        Log.d(TAG, "data: " + data);

        clearExpdDetailLayout();

        // region header
        setViewContentById(R.id.expd_id, getExpeditionHeader(data.get("no").getAsInt()));
        setViewContentById(R.id.expd_title, data.get("name").getAsString());

        List<String> features = data.has("features")
                ? Arrays.asList(gson.fromJson(data.get("features"), String[].class))
                : Collections.emptyList();

        setViewVisibilityById(R.id.expd_type, true);
        if (features.contains("drum")) {
            setViewContentById(R.id.expd_type, R.string.expd_type_drum);
        } else if (features.contains("flag_lv")) {
            setViewContentById(R.id.expd_type, R.string.expd_type_flagship_lv);
        } else {
            setViewContentById(R.id.expd_type, R.string.expd_type_normal);
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

//        if (!checkUserShipDataLoaded() || fleet_info == null) return;

        JsonObject check = check_data.get(data.get("no").getAsInt());
        boolean checked = check != null;
//        if (check == null) return;
        Log.d(TAG, "check: " + check);

        if (checked) {
            setViewTextColorById(R.id.expd_id, getColorByCond(check.get("pass").getAsBoolean()));
            setViewTextColorById(R.id.expd_title, getColorByCond(check.get("pass").getAsBoolean()));
        }

        if (checked && fleet_info != null && check.get("pass").getAsBoolean()) {
            int gs_rate;
            if (features.contains("drum")) {
                gs_rate = check.get(DRUM_OPTIONAL).getAsBoolean()
                        ? fleet_info.get(GS_RATE_DRUM_2).getAsInt()
                        : fleet_info.get(GS_RATE_DRUM_1).getAsInt();
            } else if (features.contains("flagship")) {
                gs_rate = fleet_info.get(GS_RATE_FLAGSHIP).getAsInt();
            } else {
                gs_rate = fleet_info.get(GS_RATE_NORMAL).getAsInt();
            }

            if (gs_rate > 0) {

                setViewVisibilityById(R.id.expd_gs_rate, true);
                setViewContentById(R.id.expd_gs_rate, format(R.string.expd_gs_rate, gs_rate));
            }
        }

        // region flagship cond
        if (data.has(FLAG_LV)) {
            setViewVisibilityById(R.id.expd_flagship_label, true);
            setViewContentById(R.id.expd_flagship_lv,
                    format(R.string.excheckview_flag_lv_format, data.get(FLAG_LV).getAsInt()),
                    getColorByCheck(check, FLAG_LV));
        }

        if (data.has(FLAG_COND)) {
            setViewVisibilityById(R.id.expd_flagship_label, true);

            setViewContentById(R.id.expd_flagship_cond,
                    getSshipTypeAbbrList(data.get(FLAG_COND).getAsString().split("/")),
                    getColorByCheck(check, FLAG_COND));
        }
        // endregion

        // region fleet cond
        if (data.has(FLEET_NUM)) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewContentById(R.id.expd_fleet_total_num,
                    format(R.string.excheckview_total_num_format, data.get(FLEET_NUM).getAsInt()),
                    getColorByCheck(check, FLEET_NUM));
        }

        if (data.has(FLEET_LV)) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewContentById(R.id.expd_fleet_total_lv,
                    format(R.string.excheckview_total_lv_format, data.get(FLEET_LV).getAsInt()),
                    getColorByCheck(check, FLEET_LV));
        }

        if (data.has(FLEET_COND)) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewVisibilityById(R.id.expd_fleet_cond, true);
            addFleetCondView(findViewById(R.id.expd_fleet_cond),
                    data.get(FLEET_COND).getAsString(),
                    checked ? check.getAsJsonArray(FLEET_COND) : null);
        }
        // endregion

        // region drum cond
        if (data.has(DRUM_SHIPS)) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_ships,
                    format(R.string.excheckview_drum_ship_format, data.get(DRUM_SHIPS).getAsInt()),
                    getColorByCheck(check, DRUM_SHIPS));
        }

        if (data.has(DRUM_COUNT)) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_count,
                    format(R.string.excheckview_drum_num_format, data.get(DRUM_COUNT).getAsInt()),
                    getColorByCheck(check, DRUM_COUNT));
        }

        if (data.has(DRUM_OPTIONAL)) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_optional,
                    format(R.string.excheckview_drum_num_format, data.get(DRUM_OPTIONAL).getAsInt()),
                    getColorByCheck(check, DRUM_COUNT, true));
        }
        // endregion

        // region status cond
        boolean isCombat2 = features.contains("combat_2");

        setStatusInfoFor(TOTAL_FIRE, data, check, isCombat2,
                R.id.expd_fire_label,
                R.id.expd_total_fire,
                R.id.expd_fleet_fire);

        setStatusInfoFor(TOTAL_AA, data, check, isCombat2,
                R.id.expd_aa_label,
                R.id.expd_total_aa,
                R.id.expd_fleet_aa);

        setStatusInfoFor(TOTAL_ASW, data, check, isCombat2,
                R.id.expd_asw_label,
                R.id.expd_total_asw,
                R.id.expd_fleet_asw);

        setStatusInfoFor(TOTAL_LOS, data, check, isCombat2,
                R.id.expd_los_label,
                R.id.expd_total_los,
                R.id.expd_fleet_los);
        // endregion

        // code for save view bitmap into view.png (used to take screenshot)
//        mView.setDrawingCacheEnabled(true);
//        mView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//        mView.setDrawingCacheBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
//        mView.post(() -> AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
//            deleteFile("view.png");
//            try (BufferedOutputStream out = new BufferedOutputStream(openFileOutput("view.png", MODE_PRIVATE))) {
//
//                mView.getDrawingCache().compress(Bitmap.CompressFormat.JPEG, 90, out);
//                Log.d(TAG, "saved");
//            } catch (Exception e) {
//                Log.e(TAG, "", e);
//            }
//        }));
    }

    private void setStatusInfoFor(
            String key,
            @NonNull JsonObject data,
            @Nullable JsonObject check,
            boolean isCombat2,
            @IdRes int labelId,
            @IdRes int reqId,
            @IdRes int fleetId) {

        if (data.has(key)) {
            int req = data.get(key).getAsInt();

            setViewVisibilityById(labelId, true);
            setViewContentById(reqId,
                    format(R.string.excheckview_total_format, req),
                    getColorByCheck(check, key));

            if (check == null || fleet_info == null) return;

            boolean pass = check.get(key).getAsBoolean();
            float fleet = fleet_info.get(key).getAsFloat();

            if (!pass) {
                setViewContentById(fleetId,
                        format(R.string.excheckview_cur_total_format, fleet),
                        getColorByCond(false));
            }

            if (pass && isCombat2) {
                setViewContentById(fleetId,
                        format(R.string.excheckview_per_format, fleet / req * 100),
                        getColorByCond(fleet >= req * 2.17, true));
            }
        }
    }

    private void addFleetCondView(
            LinearLayout container,
            @NonNull String data,
            @Nullable JsonArray check) {

        LinearLayout.LayoutParams row_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        int margin4dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getApplicationContext().getResources().getDisplayMetrics());
        text_params.setMargins(0, 0, margin4dp, 0);

        int i = 0;
        for (String fleet_cond : parseTotalCond(data)) {
            JsonObject fleet_check = check != null ? check.get(i++).getAsJsonObject() : null;

            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(row_params);
            row.setOrientation(LinearLayout.HORIZONTAL);

            for (Pair<String, Integer> stype_cond : parseFleetCond(fleet_cond)) {

                String stype_abbr = stype_cond.first.equals(CARRIERS)
                        ? getStringWithLocale(R.string.excheckview_ship_cvs)
                        : getSshipTypeAbbrList(stype_cond.first.split(","));

                int color = fleet_check == null
                        ? R.color.white
                        : getColorByCond(fleet_check.get(stype_cond.first).getAsBoolean());

                row.addView(generateTextView(stype_abbr, stype_cond.second, color), text_params);
            }

            container.addView(row);
        }
    }

    private TextView generateTextView(String stype, int count, @ColorRes int color) {
        TextView tv = new TextView(this);
        tv.setText(format(R.string.excheckview_ship_cond, stype, count));
        tv.setTextAppearance(this, R.style.FloatingWindow_TextAppearance_Normal);
        tv.setTextColor(ContextCompat.getColor(getApplicationContext(), color));
        return tv;
    }

    private void setFleetInfoView() {
        // actually never, but just in case
        if (!checkUserShipDataLoaded() || fleet_info == null) return;

        List<String> info = new ArrayList<>();

        info.add(format(R.string.excheckview_bonus_lv, fleet_info.get(FLEET_LV).getAsInt()));
//        info.add(format(R.string.excheckview_bonus_firepower, fleet_info.get(TOTAL_FIRE).getAsInt()));
//        info.add(format(R.string.excheckview_bonus_asw, fleet_info.get(TOTAL_ASW).getAsInt()));
//        info.add(format(R.string.excheckview_bonus_aa, fleet_info.get(TOTAL_AA).getAsInt()));
//        info.add(format(R.string.excheckview_bonus_los, fleet_info.get(TOTAL_LOS).getAsInt()));

        int drum_count = fleet_info.get(DRUM_COUNT).getAsInt();
        int drum_ships = fleet_info.get(DRUM_SHIPS).getAsInt();
        if (drum_count >= 1) {
            info.add(format(R.string.excheckview_bonus_drum_carrying, drum_count, drum_ships));
        } else {
            info.add(format(R.string.excheckview_bonus_drum, 0));
        }

        if (fleet_info.get(KINU_EXISTS).getAsBoolean()) {
            info.add(getStringWithLocale(R.string.excheckview_bonus_kinu));
        }

        int dlcs_count = fleet_info.get(DLCS_COUNT).getAsInt();
        if (dlcs_count > 0) {
            info.add(format(R.string.excheckview_bonus_dlcs, dlcs_count));
        }

        String bonus_text = joinStr(info, " / ");
        float dlcs_bonus = fleet_info.get(DLCS_BONUS).getAsFloat();
        float toku_bonus = fleet_info.get(TOKU_BONUS).getAsFloat();
        if (dlcs_bonus + toku_bonus > 0f) {
            bonus_text += format(R.string.excheckview_bonus_result, (dlcs_bonus + toku_bonus) * 100f);
        }

        setViewContentById(R.id.expd_fleet_info, bonus_text);
        findViewById(R.id.expd_fleet_info)
                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
    }
    // endregion layout update

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

    private String getSshipTypeAbbrList(String[] stype_list) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = Iterators.forArray(stype_list);
        while (itr.hasNext()) {
            sb.append(KcaApiData.getShipTypeAbbr(Integer.parseInt(itr.next())));
            if (itr.hasNext()) sb.append("/");
        }
        return sb.toString();
    }

    private int applyBonus(int value, boolean isGreatSuccess) {
        if (!checkUserShipDataLoaded() || fleet_info == null) return value;

        float dlcs_bonus = fleet_info.get(DLCS_BONUS).getAsFloat();
        float toku_bonus = fleet_info.get(TOKU_BONUS).getAsFloat();

        float gs = isGreatSuccess ? 1.5f : 1f;
        return (int) (value * gs * (1f + dlcs_bonus)) + (int) (value * gs * toku_bonus);
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

    @ColorRes private int getColorByCheck(@Nullable JsonObject check, String key, boolean is_option) {
        return check == null
                ? R.color.white
                : getColorByCond(check.get(key).getAsBoolean(), is_option);
    }

    @ColorRes private int getColorByCheck(@Nullable JsonObject check, String key) {
        return getColorByCheck(check, key, false);
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
    private boolean checkUserShipDataLoaded() {
        if (!KcaApiData.checkUserShipDataLoaded()) {
            setViewContentById(R.id.expd_fleet_info, R.string.kca_init_content);
            findViewById(R.id.expd_fleet_info).setBackgroundColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            return false;
        }

        return true;
    }

    private static int parseIntOrDefault(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.d(TAG, String.format("failed to parse '%s' into int; use default value %d", s, defaultValue), e);
            return defaultValue;
        }
    }

    private static int clamp(int x, int min, int max) {
        return min(max(x, min), max);
    }

    private static double floor01(double x) {
        return floor(x * 10f) / 10f;
    }
    // endregion
}
