package com.antest1.kcanotify;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.antest1.kcanotify.KcaApiData.STYPE_CVE;
import static com.antest1.kcanotify.KcaApiData.T2_ANTISUB_PATROL;
import static com.antest1.kcanotify.KcaApiData.T2_AUTOGYRO;
import static com.antest1.kcanotify.KcaApiData.T2_DEPTH_CHARGE;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_LARGE_II;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_MEDIUM;
import static com.antest1.kcanotify.KcaApiData.T2_GUN_SMALL;
import static com.antest1.kcanotify.KcaApiData.T2_RADAR_LARGE;
import static com.antest1.kcanotify.KcaApiData.T2_RADAR_SMALL;
import static com.antest1.kcanotify.KcaApiData.T2_RADER_LARGE_II;
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

import static java.lang.Math.*;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KcaExpeditionCheckViewService extends Service {
    public static final String SHOW_EXCHECKVIEW_ACTION = "show_excheckview";
    private static final int[] EXPEDITION_LIST = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 35, 36, 37, 38, 39, 40,
            100, 101, 102, 110, 111, 203, 204
    };
    private static final double[][] TOKU_BONUS = {
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {4.0, 4.0, 4.0, 4.0, 4.0},
            {5.0, 5.0, 5.2, 5.4, 5.4},
            {5.4, 5.6, 5.8, 5.9, 6.0}
    };

    public static boolean active;
    static boolean error_flag = false;
    Context contextWithLocale;
    int displayWidth = 0;
    private View mView;
    private WindowManager mManager;
    WindowManager.LayoutParams mParams;
    private final Gson gson = new Gson();

    private String locale;
    private int selected_fleet = 1; // selected fleet
    private int selected_world = 1; // selected world, 1 <= n <= 7, exclude 6
    private int selected_expd = 0; // selected expedition
    private boolean isGreatSuccess = false;
    private JsonArray deckdata;
    private final List<JsonObject> ship_data = new ArrayList<>();
    private final List<Integer> expedition_data = new ArrayList<>();
    private JsonObject bonus_info;
    private final Map<Integer, JsonObject> checkdata = new HashMap<>();

    // region utilities
    private String getResName(@AnyRes int res) {
        try {
            return getResources().getResourceEntryName(res);
        } catch (Resources.NotFoundException ignored) {
            return String.valueOf(res);
        }
    }

    private static int clamp(int x, int min, int max) {
        return min(max(x, min), max);
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

    private void setViewVisibilityGone(@IdRes int... id) {
        for (int view_id : id) {
            setViewVisibilityById(view_id, false);
        }
    }
    // endregion

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        Log.d("KCA", "onCreate");
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
        findViewById(R.id.expd_reward).setOnClickListener(mRewardClickListener);
        for (int id : this.<Flow>findViewById(R.id.fleet_tab).getReferencedIds()) {
            findViewById(id).setOnClickListener(mFleetTabClickListener);
        }
        for (int id : this.<Flow>findViewById(R.id.expd_worlds).getReferencedIds()) {
            findViewById(id).setOnClickListener(mExpdWorldClickListener);
        }
        for (int id : this.<Flow>findViewById(R.id.expd_table).getReferencedIds()) {
            findViewById(id).setOnClickListener(mExpdBtnClickListener);
        }
        mView.setVisibility(GONE);
        clearExpdDetailLayout();

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.CENTER;

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("KCA", "onStartCommand: " + intent.toString());

        if (intent == null || intent.getAction() == null
                || !intent.getAction().startsWith(SHOW_EXCHECKVIEW_ACTION)) {
            return super.onStartCommand(intent, flags, startId);
        }

        try (KcaDBHelper db = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION)) {
            deckdata = db.getJsonArrayValue(DB_KEY_DECKPORT);
        }

        if (deckdata == null || deckdata.size() < 2) {
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        Log.d("KCA", "Loaded: " + checkLoaded());
        setSelectedFleet(Integer.parseInt(intent.getAction().split("/")[1]));
//            int setViewResult = updateLayout();
//            if (setViewResult == 0) {
//                if (mView.getParent() != null) {
//                    mManager.removeViewImmediate(mView);
//                }
//                mManager.addView(mView, mParams);
//            }
//            Log.e("KCA", "show_excheckview_action " + setViewResult);

        mView.setVisibility(VISIBLE);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("KCA", "onDestroy");
        active = false;
        if (mView != null && mView.getParent() != null) {
            mManager.removeViewImmediate(mView);
        }
        super.onDestroy();
    }

    private boolean checkLoaded() {
        if (!checkUserShipDataLoaded()) {
            setViewContentById(R.id.expd_fleet_info, R.string.kca_init_content);
            findViewById(R.id.expd_fleet_info).setBackgroundColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            return false;
        }

        return true;
    }

    // region set and update selection
    private void setSelectedFleet(int newValue) {
        if (!checkLoaded()) return;
        newValue = clamp(newValue, 1, deckdata.size()); // 1 <= i < size
        selected_fleet = newValue;

        Log.d("KCA", "fleet: " + newValue);

        updateFleet();
        setSelectedWorld(7);
    }

    private void updateFleet() {
        for (int id : this.<Flow>findViewById(R.id.fleet_tab).getReferencedIds()) {
            View v = findViewById(id);
            v.setBackgroundColor(selected_fleet + 1 == Integer.parseInt((String) v.getTag())
                    ? ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)
                    : ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
        }

        ship_data.clear();
        JsonArray api_ship = deckdata.get(selected_fleet)
                .getAsJsonObject().getAsJsonArray("api_ship");
        for (int id : gson.fromJson(api_ship, int[].class)) {
            if (id <= 0) continue;

            JsonObject data = new JsonObject();
            JsonObject userData = getUserShipDataById(id, "ship_id,lv,slot,cond,karyoku,taisen,taiku,sakuteki");
            int kc_ship_id = userData.get("ship_id").getAsInt();
            JsonObject kcData = getKcShipDataById(kc_ship_id, "stype");
            if (kcData == null) {
                Log.e("KCA", "cannot find kc data for ship_id: " + kc_ship_id);
                continue;
            }

            data.addProperty("ship_id", userData.get("ship_id").getAsInt());
            data.addProperty("lv", userData.get("lv").getAsInt());
            data.addProperty("cond", userData.get("cond").getAsInt());
            data.addProperty("stype", kcData.get("stype").getAsInt());
            data.addProperty("karyoku", userData.getAsJsonArray("karyoku").get(0).getAsInt());
            data.addProperty("taisen", userData.getAsJsonArray("taisen").get(0).getAsInt());
            data.addProperty("taiku", userData.getAsJsonArray("taiku").get(0).getAsInt());
            data.addProperty("sakuteki", userData.getAsJsonArray("sakuteki").get(0).getAsInt());
            data.add("item", new JsonArray());
            for (int itemid : gson.fromJson(userData.getAsJsonArray("slot"), int[].class)) {
                if (itemid <= 0) continue;

                JsonObject iteminfo = getUserItemStatusById(itemid, "slotitem_id,level", "type,tais");
                if (iteminfo != null) data.getAsJsonArray("item").add(iteminfo);
            }

            ship_data.add(data);
        }

        bonus_info = getBonusInfo();
        setFleetInfoView();

        Log.d("KCA", "ship_data: " + ship_data);
        Log.d("KCA", "bonus_info: " + bonus_info);
    }

    private void setSelectedWorld(int newValue) {
        if (!checkLoaded()) return;
        newValue = clamp(newValue, 1, 7); // 1 <= i <= 7, exclude 6
        selected_world = newValue;

        Log.d("KCA", "world: " + newValue);

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

        expedition_data.clear();
        expedition_data.addAll(KcaApiData.getExpeditionNumByWorld(selected_world));
        Log.d("KCA", "expedition_data: " + expedition_data);

        int[] view_ids = this.<Flow>findViewById(R.id.expd_table).getReferencedIds();
        for (int i = 0; i < view_ids.length; i++) {
            int id = view_ids[i];

            if (i >= expedition_data.size()) {
                findViewById(id).setVisibility(INVISIBLE);
            } else {

                setViewContentById(id, KcaExpedition2.getExpeditionStr(expedition_data.get(i)));
                findViewById(id).setVisibility(VISIBLE);
            }
        }
    }

    private void updateCheckData() {
        checkdata.clear();
        for (int expd : expedition_data) {
            checkdata.put(expd, checkCondition(KcaApiData.getExpeditionInfo(expd, locale)));
        }

        Log.d("KCA", "checkdata: " + checkdata);

        int[] view_ids = this.<Flow>findViewById(R.id.expd_table).getReferencedIds();
        for (int i = 0; i < expedition_data.size(); i++) {
            int id = view_ids[i];

            JsonObject check_item = checkdata.get(expedition_data.get(i));
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

    private void setSelectedExpd(int newValue) {
        if (!checkLoaded()) return;
        newValue = clamp(newValue, 0, expedition_data.size() - 1); // 1 <= i <= 7, exclude 6
        selected_expd = newValue;
        Log.d("KCA", "expd: " + newValue);

        updateExpd();
    }

    private void updateExpd() {
        setExpdDetailLayout();
    }
    // endregion

    private JsonObject checkCondition(JsonObject expd_data) {
        boolean total_pass = true;
        JsonObject result = new JsonObject();
        if (expd_data == null) return null;

        boolean has_flag_lv = expd_data.has("flag-lv");
        boolean has_flag_cond = expd_data.has("flag-cond");
        boolean has_total_lv = expd_data.has("total-lv");
        boolean has_total_cond = expd_data.has("total-cond");
        boolean has_drum_ship = expd_data.has("drum-ship");
        boolean has_drum_num = expd_data.has("drum-num");
        boolean has_drum_num_optional = expd_data.has("drum-num-optional");
        boolean has_total_asw = expd_data.has("total-asw");
        boolean has_total_fp = expd_data.has("total-fp");
        boolean has_total_los = expd_data.has("total-los");
        boolean has_total_firepower = expd_data.has("total-firepower");

        int total_num = expd_data.get("total-num").getAsInt();
        result.addProperty("total-num", ship_data.size() >= total_num);
        total_pass = total_pass && (ship_data.size() >= total_num);

        result.addProperty("flag-lv", true);
        if (has_flag_lv) {
            if (ship_data.size() > 0) {
                int flag_lv_value = ship_data.get(0).get("lv").getAsInt();
                int flag_lv = expd_data.get("flag-lv").getAsInt();
                result.addProperty("flag-lv", flag_lv_value >= flag_lv);
                total_pass = total_pass && (flag_lv_value >= flag_lv);
            } else {
                result.addProperty("flag-cond", false);
                total_pass = false;
            }
        }

        result.addProperty("flag-cond", true);
        if (has_flag_cond) {
            if (ship_data.size() > 0) {
                boolean is_flag_passed = false;
                int flag_ship_id = ship_data.get(0).get("ship_id").getAsInt();
                int flag_conv_value = ship_data.get(0).get("stype").getAsInt();
                if (KcaApiData.isShipCVE(flag_ship_id)) flag_conv_value = STYPE_CVE;
                String[] flag_cond = expd_data.get("flag-cond").getAsString().split("/");
                for (int i = 0; i < flag_cond.length; i++) {
                    if (flag_conv_value == Integer.parseInt(flag_cond[i])) {
                        is_flag_passed = true;
                        break;
                    }
                }
                result.addProperty("flag-cond", is_flag_passed);
                total_pass = total_pass && is_flag_passed;
            } else {
                result.addProperty("flag-cond", false);
                total_pass = false;
            }
        }

        result.addProperty("total-lv", true);
        if (has_total_lv) {
            int total_lv_value = 0;
            for (JsonObject obj : ship_data) {
                total_lv_value += obj.get("lv").getAsInt();
            }
            int total_lv = expd_data.get("total-lv").getAsInt();
            result.addProperty("total-lv", total_lv_value >= total_lv);
            total_pass = total_pass && (total_lv_value >= total_lv);
        }

        if (has_total_cond) {
            String total_cond = expd_data.get("total-cond").getAsString();
            JsonObject total_cond_result = checkFleetCondition(total_cond);
            result.add("total-cond", total_cond_result.getAsJsonArray("value"));
            total_pass = total_pass && total_cond_result.get("pass").getAsBoolean();
        }

        // Drum: 75
        int drum_ship_value = 0;
        int drum_num_value = 0;
        int plane_tais_value = 0;

        for (JsonObject obj : ship_data) {
            int count = 0;
            for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                int slotitem_id = itemobj.getAsJsonObject().get("slotitem_id").getAsInt();
                int level = itemobj.getAsJsonObject().get("level").getAsInt();
                int type_t2 = itemobj.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                int tais = itemobj.getAsJsonObject().get("tais").getAsInt();
                if (KcaApiData.isItemAircraft(type_t2) || type_t2 == T2_AUTOGYRO || type_t2 == T2_ANTISUB_PATROL) {
                    plane_tais_value += tais;
                }
                if (slotitem_id == 75) {
                    drum_num_value += 1;
                    count += 1;
                }
            }
            if (count > 0) drum_ship_value += 1;
        }

        result.addProperty("drum-ship", true);
        result.addProperty("drum-num", true);
        result.addProperty("drum-num-optional", true);

        if (has_drum_ship) {
            int drum_ship = expd_data.get("drum-ship").getAsInt();
            result.addProperty("drum-ship", drum_ship_value >= drum_ship);
            total_pass = total_pass && (drum_ship_value >= drum_ship);
        }
        if (has_drum_num) {
            int drum_num = expd_data.get("drum-num").getAsInt();
            result.addProperty("drum-num", drum_num_value >= drum_num);
            total_pass = total_pass && (drum_num_value >= drum_num);
        } else if (has_drum_num_optional) {
            int drum_num = expd_data.get("drum-num-optional").getAsInt();
            result.addProperty("drum-num-optional", drum_num_value >= drum_num);
        }

        result.addProperty("total-asw", true);
        if (has_total_asw) {
            double total_asw_value = 0;
            double total_asw_bonus = 0;
            for (JsonObject obj : ship_data) {
                total_asw_value += (obj.get("taisen").getAsInt());
                for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                    int level = itemobj.getAsJsonObject().get("level").getAsInt();
                    int type_t2 = itemobj.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                    if (type_t2 == T2_SONAR || type_t2 == T2_SONAR_LARGE || type_t2 == T2_DEPTH_CHARGE) {
                        total_asw_bonus += sqrt(level);
                    }
                }
            }
            total_asw_value = floor(total_asw_value - plane_tais_value + total_asw_bonus * 2 / 3);
            int total_asw = expd_data.get("total-asw").getAsInt();
            result.addProperty("total-asw", total_asw_value >= total_asw);
            total_pass = total_pass && (total_asw_value >= total_asw);
        }

        result.addProperty("total-fp", true);
        if (has_total_fp) {
            int total_fp_value = 0;
            for (JsonObject obj : ship_data) {
                total_fp_value += (obj.get("taiku").getAsInt());
                for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                    int level = itemobj.getAsJsonObject().get("level").getAsInt();
                    int type_t3 = itemobj.getAsJsonObject().getAsJsonArray("type").get(3).getAsInt();
                    if (type_t3 == 15) total_fp_value += sqrt(level);
                    if (type_t3 == 16) total_fp_value += 0.3 * sqrt(level);
                }
            }
            int total_fp = expd_data.get("total-fp").getAsInt();
            result.addProperty("total-fp", total_fp_value >= total_fp);
            total_pass = total_pass && (total_fp_value >= total_fp);
        }

        result.addProperty("total-los", true);
        if (has_total_los) {
            int total_los_value = 0;
            for (JsonObject obj : ship_data) {
                total_los_value += obj.get("sakuteki").getAsInt();
                for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                    int level = itemobj.getAsJsonObject().get("level").getAsInt();
                    int type_t2 = itemobj.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                    if (type_t2 == T2_RADAR_SMALL || type_t2 == T2_RADAR_LARGE || type_t2 == T2_RADER_LARGE_II) {
                        total_los_value += sqrt(level);
                    }
                }
            }
            int total_los = expd_data.get("total-los").getAsInt();
            result.addProperty("total-los", total_los_value >= total_los);
            total_pass = total_pass && (total_los_value >= total_los);
        }

        result.addProperty("total-firepower", true);
        if (has_total_firepower) {
            double total_firepower_value = 0;
            for (JsonObject obj : ship_data) {
                total_firepower_value += obj.get("karyoku").getAsInt();
                for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                    int level = itemobj.getAsJsonObject().get("level").getAsInt();
                    int type_t2 = itemobj.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                    if (type_t2 == T2_GUN_MEDIUM || type_t2 == T2_GUN_LARGE || type_t2 == T2_GUN_LARGE_II) {
                        total_firepower_value += sqrt(level);
                    } else if (type_t2 == T2_GUN_SMALL) {
                        total_firepower_value += 0.5 * sqrt(level);
                    } else if (type_t2 == T2_SUB_GUN) {
                        total_firepower_value += 0.15 * sqrt(level);
                    }
                }
            }
            total_firepower_value = floor(total_firepower_value);
            int total_firepower = expd_data.get("total-firepower").getAsInt();
            result.addProperty("total-firepower", total_firepower_value >= total_firepower);
            total_pass = total_pass && (total_firepower_value >= total_firepower);
        }

        result.addProperty("pass", total_pass);
        return result;
    }

    private JsonObject checkFleetCondition(String total_cond) {
        boolean total_pass = false;
        String cve_key = String.valueOf(STYPE_CVE);
        Map<String, Integer> stypedata = new HashMap<>();
        for (JsonObject obj : ship_data) {
            int ship_id = obj.get("ship_id").getAsInt();
            String stype = String.valueOf(obj.get("stype").getAsInt());
            if (KcaApiData.isShipCVE(ship_id)) {
                if (stypedata.containsKey(cve_key)) {
                    stypedata.put(cve_key, stypedata.get(cve_key) + 1);
                }
                stypedata.put(cve_key, 1);
            }
            if (stypedata.containsKey(stype)) {
                stypedata.put(stype, stypedata.get(stype) + 1);
            } else {
                stypedata.put(stype, 1);
            }
        }

        JsonArray value = new JsonArray();
        String[] conds = total_cond.split("/");
        for (String cond : conds) {
            boolean partial_pass = true;
            JsonObject cond_check = new JsonObject();
            String[] shipcond = cond.split("\\|");
            for (String sc : shipcond) {
                String[] ship_count = sc.split("\\-");
                String[] ship = ship_count[0].split(",");
                int count = Integer.parseInt(ship_count[1]);
                List<String> ship_list = new ArrayList<>();
                for (String s : ship) {
                    if (stypedata.containsKey(s)) {
                        count -= stypedata.get(s);
                    }
                }
                cond_check.addProperty(ship_count[0], count <= 0);
                partial_pass = partial_pass && (count <= 0);
            }
            total_pass = total_pass || partial_pass;
            value.add(cond_check);
        }
        JsonObject result = new JsonObject();
        result.addProperty("pass", total_pass);
        result.add("value", value);
        return result;
    }

    // Drum: 75, Daihatsu: 68, 89Tank: 166, Amp: 167, Toku-Daihatsu: 193
    // TODO: add anti-air
    private JsonObject getBonusInfo() {
        boolean kinu_exist = false;

        int drum_count = 0;
        int bonus_count = 0;
        int daihatsu_count = 0;
        int tank_count = 0;
        int amp_count = 0;
        int toku_count = 0;
        double bonus_level = 0.0;

        float total_firepower = 0;
        float total_asw_value = 0;
        float total_asw_bonus = 0;
        float total_aa_bonus = 0;
        float plane_tais_value = 0;
        float plane_saku_value = 0;
        int total_los = 0;
        JsonObject result = new JsonObject();

        for (JsonObject ship : ship_data) {
            if (ship.get("ship_id").getAsInt() == 487) { // Kinu Kai Ni
                kinu_exist = true;
            }

            total_firepower += ship.get("karyoku").getAsInt();
            total_asw_value += ship.get("taisen").getAsInt();
            total_aa_bonus += ship.get("taiku").getAsInt(); // TODO
            total_los += ship.get("sakuteki").getAsInt();
            for (JsonElement itemobj : ship.getAsJsonArray("item")) {
                int type_t2 = itemobj.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                int slotitem_id = itemobj.getAsJsonObject().get("slotitem_id").getAsInt();
                int level = itemobj.getAsJsonObject().get("level").getAsInt();
                if (KcaApiData.isItemAircraft(type_t2) || type_t2 == T2_AUTOGYRO || type_t2 == T2_ANTISUB_PATROL) {
                    plane_tais_value += itemobj.getAsJsonObject().get("tais").getAsInt();
                }

                if (type_t2 == T2_GUN_MEDIUM || type_t2 == T2_GUN_LARGE || type_t2 == T2_GUN_LARGE_II) {
                    total_firepower += sqrt(level);
                } else if (type_t2 == T2_GUN_SMALL) {
                    total_firepower += 0.5 * sqrt(level);
                } else if (type_t2 == T2_SUB_GUN) {
                    total_firepower += 0.15 * sqrt(level);
                }

                if (type_t2 == T2_SONAR || type_t2 == T2_SONAR_LARGE || type_t2 == T2_DEPTH_CHARGE) {
                    total_asw_bonus += sqrt(level);
                }

                switch (slotitem_id) {
                    case 75:
                        drum_count += 1;
                        break;
                    case 68:
                        bonus_level += level;
                        bonus_count += 1.0;
                        daihatsu_count += 1;
                        break;
                    case 166:
                        bonus_level += level;
                        bonus_count += 1.0;
                        tank_count += 1;
                        break;
                    case 167:
                        bonus_level += level;
                        bonus_count += 1.0;
                        amp_count += 1;
                        break;
                    case 193:
                        bonus_level += level;
                        bonus_count += 1.0;
                        toku_count += 1;
                        break;
                    default:
                        break;
                }
            }
        }
        if (bonus_count > 0) bonus_level /= bonus_count;

        result.addProperty("kinu", kinu_exist);
        result.addProperty("drum", drum_count);
        result.addProperty("daihatsu", daihatsu_count);
        result.addProperty("tank", tank_count);
        result.addProperty("amp", amp_count);
        result.addProperty("toku", toku_count);
        result.addProperty("level", bonus_level);
        result.addProperty("firepower", (int) total_firepower);
        result.addProperty("asw", floor(total_asw_value - plane_tais_value + total_asw_bonus * 2 / 3));
        result.addProperty("aa", total_los);
        result.addProperty("los", total_los);
        return result;
    }

    private int applyBonus(int value, boolean isGreatSuccess) {
        int kinu_count = bonus_info.get("kinu").getAsBoolean() ? 1 : 0;
        int daihatsu_count = bonus_info.get("daihatsu").getAsInt();
        int tank_count = bonus_info.get("tank").getAsInt();
        int amp_count = bonus_info.get("amp").getAsInt();
        int toku_count = bonus_info.get("toku").getAsInt();
        double bonus_level = bonus_info.get("level").getAsDouble();

        int dlc_count = kinu_count + daihatsu_count + toku_count;
        int ntoku_count = min(4, daihatsu_count + tank_count + amp_count);
        double bonus_default = min(dlc_count * 0.05 + tank_count * 0.02 + amp_count * 0.01, 0.2);
        bonus_default += 0.002 * bonus_level;
        double bonus_toku = 0.0;
        if (toku_count > 0) {
            int toku_idx;
            if (toku_count > 4) toku_idx = 3;
            else toku_idx = toku_count - 1;
            bonus_toku = TOKU_BONUS[toku_idx][ntoku_count] * 0.01;
        }

        int value_bonus = 0;
        if (isGreatSuccess) {
            value_bonus = (int) (value * 3 * (1 + bonus_default) / 2) + (int) (value * 3 * bonus_toku / 2);
        } else {
            value_bonus = (int) (value * (1 + bonus_default)) + (int) (value * bonus_toku);
        }
        return value_bonus;
    }

    private String convertTotalCond(String str) {
        String[] ship_count = str.split("-");
        String ship_concat;
        if (ship_count[0].equals("7,11,16,18")) {
            ship_concat = getStringWithLocale(R.string.excheckview_ship_cvs);
        } else {
            String[] ship = ship_count[0].split(",");
            List<String> ship_list = new ArrayList<>();
            for (String s : ship) {
                ship_list.add(getShipTypeAbbr(Integer.parseInt(s)));
            }
            ship_concat = joinStr(ship_list, "/");
        }
        return ship_concat.concat(":").concat(ship_count[1]);
    }

    private List<View> generateConditionView(String data, JsonArray check) {
        int textsize = getResources().getDimensionPixelSize(R.dimen.popup_text_normal);
        List<View> views = new ArrayList<>();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 14, 0);

        String[] conds = data.split("/");
        int count = 0;
        for (String cond : conds) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            String[] shipcond = cond.split("\\|");
            for (String sc : shipcond) {
                TextView scView = new TextView(this);
                scView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize);
                scView.setText(convertTotalCond(sc));
                if (check.get(count).getAsJsonObject().get(sc.split("-")[0]).getAsBoolean()) {
                    scView.setTextColor(ContextCompat
                            .getColor(getApplicationContext(), R.color.colorExpeditionBtnGoodBack));
                } else {
                    scView.setTextColor(ContextCompat
                            .getColor(getApplicationContext(), R.color.colorExpeditionBtnFailBack));
                }
                rowLayout.addView(scView, params);
            }
            views.add(rowLayout);
            count += 1;
        }
        return views;
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
        setViewVisibilityGone(R.id.expd_flagship_label, R.id.expd_flagship_lv, R.id.expd_flagship_cond);
        setViewVisibilityGone(R.id.expd_fleet_label, R.id.expd_fleet_total_num, R.id.expd_fleet_total_lv);
        this.<LinearLayout>findViewById(R.id.expd_fleet_cond).removeAllViews();
        setViewVisibilityGone(
                R.id.expd_firepower_label, R.id.expd_total_firepower, R.id.expd_fleet_firepower,
                R.id.expd_asw_label, R.id.expd_total_asw, R.id.expd_fleet_asw,
                R.id.expd_aa_label, R.id.expd_total_aa, R.id.expd_fleet_aa,
                R.id.expd_los_label, R.id.expd_total_los, R.id.expd_fleet_los);
        setViewVisibilityGone(R.id.expd_drum_label, R.id.expd_drum_ships, R.id.expd_drum_count, R.id.expd_drum_optional);
    }

    public void setExpdDetailLayout() {
        int expd_value = expedition_data.get(selected_expd);
        Log.e("KCA", "expd: " + selected_expd + ", data: " + expd_value);
        JsonObject data = KcaApiData.getExpeditionInfo(expd_value, locale);
        Log.d("KCA", "data: " + data);

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
        if (features.contains("flagship_lv")) {
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
            Log.e("KCA", "Item image not found for item: " + reward1);
        }

        try {
            if (reward2 > 0) {
                setViewContentById(R.id.expd_reward_item1, getId("common_itemicons_id_" + reward2, R.mipmap.class));
            }
        } catch (Exception e) {
            Log.e("KCA", "Item image not found for item: " + reward2);
        }
        // endregion

        JsonObject check = checkdata.get(data.get("no").getAsInt());
        if (check == null) return;
        Log.d("KCA", "check: " + check);

        setViewTextColorById(R.id.expd_title, getColorByCond(check.get("pass").getAsBoolean()));

        // region flagship cond
        if (data.has("flag-lv")) {
            setViewVisibilityById(R.id.expd_flagship_label, true);
            setViewContentById(R.id.expd_flagship_lv,
                    format(R.string.excheckview_flag_lv_format, data.get("flag-lv").getAsInt()),
                    getColorByCond(check.get("flag-lv").getAsBoolean()));
        }

        if (data.has("flag-cond")) {
            setViewVisibilityById(R.id.expd_flagship_label, true);

            List<String> abbr_text_list = Lists.transform(
                    Arrays.asList(data.get("flag-cond").getAsString().split("/")),
                    (str) -> getShipTypeAbbr(Integer.parseInt(str)));

            setViewContentById(R.id.expd_flagship_cond,
                    KcaUtils.joinStr(abbr_text_list, "/"),
                    getColorByCond(check.get("flag-cond").getAsBoolean()));
        }
        // endregion

        // region fleet cond
        if (data.has("total-num")) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewContentById(R.id.expd_fleet_total_num,
                    format(R.string.excheckview_total_num_format, data.get("total-num").getAsInt()),
                    getColorByCond(check.get("total-num").getAsBoolean()));
        }

        if (data.has("total-lv")) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewContentById(R.id.expd_fleet_total_lv,
                    format(R.string.excheckview_total_lv_format, data.get("total-lv").getAsInt()),
                    getColorByCond(check.get("total-lv").getAsBoolean()));
        }

        if (data.has("total-cond")) {
            setViewVisibilityById(R.id.expd_fleet_label, true);
            setViewVisibilityById(R.id.expd_fleet_cond, true);
            String total_cond = data.get("total-cond").getAsString();
            LinearLayout layout = findViewById(R.id.expd_fleet_cond);
            for (View v : generateConditionView(total_cond, check.getAsJsonArray("total-cond"))) {
                layout.addView(v);
            }
        }
        // endregion

        // region drum cond
        if (data.has("drum-ship")) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_ships,
                    format(R.string.excheckview_drum_ship_format, data.get("drum-ship").getAsInt()),
                    getColorByCond(check.get("drum-ship").getAsBoolean()));
        }

        if (data.has("drum-num")) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_count,
                    format(R.string.excheckview_drum_num_format, data.get("drum-num").getAsInt()),
                    getColorByCond(check.get("drum-num").getAsBoolean()));
        }

        if (data.has("drum-num-optional")) {
            setViewVisibilityById(R.id.expd_drum_label, true);
            setViewContentById(R.id.expd_drum_optional,
                    format(R.string.excheckview_drum_num_format, data.get("drum-num-optional").getAsInt()),
                    getColorByCond(check.get("drum-num-optional").getAsBoolean(), true)); // TODO: add check for 'drum-num-optional'
        }
        // endregion

        boolean isCombat2 = features.contains("combat_2");
        // TODO: add status% when combat II

        // region status
        if (data.has("total-asw")) {
            setViewVisibilityById(R.id.expd_asw_label, true);
            setViewContentById(R.id.expd_total_asw,
                    format(R.string.excheckview_total_format, data.get("total-asw").getAsInt()),
                    getColorByCond(check.get("total-asw").getAsBoolean(), false));
        }

        if (data.has("total-fp")) {
            setViewVisibilityById(R.id.expd_aa_label, true);
            setViewContentById(R.id.expd_total_aa,
                    format(R.string.excheckview_total_format, data.get("total-fp").getAsInt()),
                    getColorByCond(check.get("total-fp").getAsBoolean(), false));
        }

        if (data.has("total-los")) {
            setViewVisibilityById(R.id.expd_los_label, true);
            setViewContentById(R.id.expd_total_los,
                    format(R.string.excheckview_total_format, data.get("total-los").getAsInt()),
                    getColorByCond(check.get("total-los").getAsBoolean(), false));
        }

        if (data.has("total-firepower")) {
            setViewVisibilityById(R.id.expd_firepower_label, true);
            setViewContentById(R.id.expd_total_firepower,
                    format(R.string.excheckview_total_format, data.get("total-firepower").getAsInt()),
                    getColorByCond(check.get("total-firepower").getAsBoolean(), false));
        }
        // endregion
    }

    public int updateLayout() {
        try {
            if (!checkLoaded()) return 1;

            updateFleet();
            updateWorld();
            updateCheckData();
            updateExpd();
            return 0;
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), getStringFromException(e), Toast.LENGTH_LONG).show();
            return 1;
        }
    }

    private void setFleetInfoView() {
        List<String> info = new ArrayList<>();

        info.add(format(R.string.excheckview_bonus_firepower, bonus_info.get("firepower").getAsInt()));
        info.add(format(R.string.excheckview_bonus_asw, bonus_info.get("asw").getAsInt()));
        info.add(format(R.string.excheckview_bonus_aa, bonus_info.get("aa").getAsInt()));
        info.add(format(R.string.excheckview_bonus_los, bonus_info.get("los").getAsInt()));

        info.add(format(R.string.excheckview_bonus_drum, bonus_info.get("drum").getAsInt()));

        if (bonus_info.get("kinu").getAsBoolean())
            info.add(getStringWithLocale(R.string.excheckview_bonus_kinu));

        int daihatsu_count = bonus_info.get("daihatsu").getAsInt();
        if (daihatsu_count > 0)
            info.add(format(R.string.excheckview_bonus_dlc, daihatsu_count));

        int tank_count = bonus_info.get("tank").getAsInt();
        if (tank_count > 0)
            info.add(format(R.string.excheckview_bonus_tank, tank_count));

        int amp_count = bonus_info.get("amp").getAsInt();
        if (amp_count > 0)
            info.add(format(R.string.excheckview_bonus_amp, amp_count));

        int toku_count = bonus_info.get("toku").getAsInt();
        if (toku_count > 0)
            info.add(format(R.string.excheckview_bonus_toku, toku_count));

        String bonus_text = joinStr(info, " / ");
        double resource_bonus = (double) applyBonus(100, false);
        if (resource_bonus > 100.0) {
            bonus_text += format(R.string.excheckview_bonus_result, resource_bonus - 100.0);
        }
        setViewContentById(R.id.expd_fleet_info, bonus_text);
        findViewById(R.id.expd_fleet_info)
                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
    }

    private final View.OnClickListener mRewardClickListener = v -> {
        if (!checkUserShipDataLoaded()) return;
        isGreatSuccess = !isGreatSuccess;
        setExpdDetailLayout();
    };

    private final View.OnClickListener mFleetTabClickListener = v -> {
        if (!checkUserShipDataLoaded()) return;
        try {
            int fleet = Integer.parseInt((String) v.getTag()) - 1 /* fleet id is 0(main) <= id <= 3 */;
            if (fleet != selected_fleet) setSelectedFleet(fleet);
        } catch (ClassCastException | NumberFormatException e) {
            Log.e("KCA", getResName(v.getId()), new IllegalStateException("tag is not set or is overwritten, tag: " + v.getTag()));
        }
    };

    private final View.OnClickListener mExpdWorldClickListener = v -> {
        if (!checkUserShipDataLoaded()) return;
        try {
            int world = Integer.parseInt((String) v.getTag());
            if (world != selected_world) setSelectedWorld(world);
        } catch (ClassCastException | NumberFormatException e) {
            Log.e("KCA", getResName(v.getId()), new IllegalStateException("tag is not set or is overwritten, tag: " + v.getTag()));
        }
    };

    private final View.OnClickListener mExpdBtnClickListener = v -> {
        if (!checkUserShipDataLoaded()) return;
        Flow table = findViewById(R.id.expd_table);
        int expd = Ints.indexOf(table.getReferencedIds(), v.getId());
        if (expd == -1) {
            Log.e("KCA", getResName(v.getId()), new IllegalStateException("can not find clicked view"));
            return;
        }
        if (expd != selected_expd) setSelectedExpd(expd);
    };
}
