package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import static com.antest1.kcanotify.KcaExpedition2.getExpeditionHeader;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getTimeStr;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaExpeditionCheckViewService extends BaseService {
    public static final String SHOW_EXCHECKVIEW_ACTION = "show_excheckview";
    final int[] expedition_list = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 35, 36, 37, 38, 39, 40,
            100, 101, 102, 110, 111, 203, 204};
    public static final double[][] toku_bonus = {
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {4.0, 4.0, 4.0, 4.0, 4.0},
            {5.0, 5.0, 5.2, 5.4, 5.4},
            {5.4, 5.6, 5.8, 5.9, 6.0}
    };

    static boolean error_flag = false;
    int displayWidth = 0;
    public KcaDBHelper dbHelper;
    private View layoutView, itemView;
    private WindowManager windowManager;
    WindowManager.LayoutParams layoutParams;

    int intentSelection = 1;
    int selected = 1;
    int world = 1;
    int button = 0;
    boolean isGreatSuccess = false;
    JsonArray deckdata;
    List<JsonObject> ship_data;
    List<Integer> expedition_data = new ArrayList<>();
    Map<String, JsonObject> checkdata;

    public static boolean active = false;
    public static boolean isActive() {
        return active;
    }

    private void updateSelectedView(int idx, int world) {
        for (int i = 0; i < 15; i++) {
            int view_id = getId("expd_btn_".concat(String.valueOf(i)), R.id.class);
            if (i < expedition_data.size()) {
                int value = expedition_data.get(i);
                ((TextView) layoutView.findViewById(view_id)).setText(KcaExpedition2.getExpeditionStr(value));
                layoutView.findViewById(view_id).setVisibility(View.VISIBLE);
            } else {
                layoutView.findViewById(view_id).setVisibility(View.INVISIBLE);
            }
        }

        for (int i = 1; i <= 7; i++) {
            int view_id = getId("expd_world_" + i, R.id.class);
            if (world == i) {
                ((Chip) layoutView.findViewById(view_id)).setChipBackgroundColorResource(getId("colorExpeditionTable" + i, R.color.class));
            } else {
                ((Chip) layoutView.findViewById(view_id)).setChipBackgroundColorResource(R.color.transparent);
            }
        }

        for (int i = 1; i < 4; i++) {
            int view_id = getId("fleet_".concat(String.valueOf(i + 1)), R.id.class);
            if (idx == i) {
                layoutView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            } else {
                layoutView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
            }
        }
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            try {
                active = true;
                ship_data = new ArrayList<>();
                checkdata = new HashMap<>();
                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            } catch (Exception e) {
                e.printStackTrace();
                active = false;
                error_flag = true;
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        active = false;
        if (windowManager != null && checkLayoutExist()) {
            windowManager.removeViewImmediate(layoutView);
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && checkLayoutExist()) {
            windowManager.removeViewImmediate(layoutView);
        }
    }

    private boolean checkLayoutExist() {
        return layoutView != null && layoutView.getParent() != null;
    }

    private void setViewLayout() {
        if (checkLayoutExist()) return;

        Context context = new ContextThemeWrapper(this, R.style.AppTheme);
        LayoutInflater mInflater = LayoutInflater.from(context);

        layoutView = mInflater.inflate(R.layout.view_excheck_list, null);
        itemView = layoutView.findViewById(R.id.view_excheck_detail);
        layoutView.findViewById(R.id.excheckview_head).setOnClickListener(mViewClickListener);
        layoutView.findViewById(R.id.excheck_detail_reward).setOnClickListener(mViewClickListener);
        KcaUtils.resizeFullWidthView(getApplicationContext(), layoutView.findViewById(R.id.excheckviewpanel));
        layoutView.setVisibility(View.GONE);

        setPopupContent();
        displayWidth = KcaUtils.getDefaultDisplaySizeInsets(this).size.x;

        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.CENTER;
        }
        windowManager.addView(layoutView, layoutParams);
    }

    private void setPopupContent() {
        if (intentSelection < 1) intentSelection = 1;
        else if (intentSelection > 3) intentSelection = 2;
        if (intentSelection < deckdata.size()) {
            selected = intentSelection;
        }

        clearItemViewLayout();
        int setViewResult = setView();

        Log.e("KCA", "show_excheckview_action " + setViewResult);
        for (int i = 1; i < 4; i++) {
            layoutView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).setOnClickListener(fleetChipOnClickListener);
        }
        for (int i = 1; i <= 7; i++) {
            layoutView.findViewById(getId("expd_world_".concat(String.valueOf(i)), R.id.class)).setOnClickListener(mViewClickListener);
        }
        for (int i = 0; i < 15; i++) {
            layoutView.findViewById(KcaUtils.getId("expd_btn_".concat(String.valueOf(i)), R.id.class)).setOnClickListener(mViewClickListener);
        }
        layoutView.setVisibility(View.VISIBLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().startsWith(SHOW_EXCHECKVIEW_ACTION)) {
                deckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                if (deckdata != null && deckdata.size() >= 2) {
                    intentSelection = Integer.parseInt(intent.getAction().split("/")[1]);
                    setViewLayout();
                } else stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private JsonObject checkFleetCondition(String data) {
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
        String[] conds = data.split("/");
        for (String cond : conds) {
            boolean partial_pass = true;
            JsonObject cond_check = new JsonObject();
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            String[] shipcond = cond.split("\\|");
            for (String sc : shipcond) {
                String[] ship_count = sc.split("\\-");
                String[] ship = ship_count[0].split(",");
                int count = Integer.valueOf(ship_count[1]);
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

    private JsonObject checkCondition(int exp_no) {
        boolean total_pass = true;
        JsonObject result = new JsonObject();

        JsonObject data = KcaApiData.getExpeditionInfo(exp_no);
        if (data == null) return null;

        boolean has_flag_lv = data.has("flag-lv");
        boolean has_flag_cond = data.has("flag-cond");
        boolean has_total_lv = data.has("total-lv");
        boolean has_total_cond = data.has("total-cond");
        boolean has_drum_ship = data.has("drum-ship");
        boolean has_drum_num = data.has("drum-num");
        boolean has_drum_num_optional = data.has("drum-num-optional");
        boolean has_total_asw = data.has("total-asw");
        boolean has_total_fp = data.has("total-fp");
        boolean has_total_los = data.has("total-los");
        boolean has_total_firepower = data.has("total-firepower");

        int total_num = data.get("total-num").getAsInt();
        result.addProperty("total-num", ship_data.size() >= total_num);
        total_pass = total_pass && (ship_data.size() >= total_num);

        result.addProperty("flag-lv", true);
        if (has_flag_lv) {
            if (ship_data.size() > 0) {
                int flag_lv_value = ship_data.get(0).get("lv").getAsInt();
                int flag_lv = data.get("flag-lv").getAsInt();
                result.addProperty("flag-lv", flag_lv_value >= flag_lv);
                total_pass = total_pass && (flag_lv_value >= flag_lv);
            } else {
                result.addProperty("flag-cond", false);
                total_pass = false;
            }
        }

        result.addProperty("flag-cond", true);
        if (has_flag_cond) {
            if (!ship_data.isEmpty()) {
                boolean is_flag_passed = false;
                int flag_ship_id = ship_data.get(0).get("ship_id").getAsInt();
                int flag_conv_value = ship_data.get(0).get("stype").getAsInt();
                if (KcaApiData.isShipCVE(flag_ship_id)) flag_conv_value = STYPE_CVE;
                String[] flag_cond = data.get("flag-cond").getAsString().split("/");
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
            int total_lv = data.get("total-lv").getAsInt();
            result.addProperty("total-lv", total_lv_value >= total_lv);
            total_pass = total_pass && (total_lv_value >= total_lv);
        }

        if (has_total_cond) {
            String total_cond = data.get("total-cond").getAsString();
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
            int drum_ship = data.get("drum-ship").getAsInt();
            result.addProperty("drum-ship", drum_ship_value >= drum_ship);
            total_pass = total_pass && (drum_ship_value >= drum_ship);
        }
        if (has_drum_num) {
            int drum_num = data.get("drum-num").getAsInt();
            result.addProperty("drum-num", drum_num_value >= drum_num);
            total_pass = total_pass && (drum_num_value >= drum_num);
        } else if (has_drum_num_optional) {
            int drum_num = data.get("drum-num-optional").getAsInt();
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
                        total_asw_bonus += Math.sqrt(level);
                    }
                }
            }
            total_asw_value = Math.floor(total_asw_value - plane_tais_value + total_asw_bonus * 2 / 3);
            int total_asw = data.get("total-asw").getAsInt();
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
                    if (type_t3 == 15) total_fp_value += Math.sqrt(level);
                    if (type_t3 == 16) total_fp_value += 0.3 * Math.sqrt(level);
                }
            }
            int total_fp = data.get("total-fp").getAsInt();
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
                        total_los_value += Math.sqrt(level);
                    }
                }
            }
            int total_los = data.get("total-los").getAsInt();
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
                        total_firepower_value += Math.sqrt(level);
                    } else if (type_t2 == T2_GUN_SMALL) {
                        total_firepower_value += 0.5 * Math.sqrt(level);
                    } else if (type_t2 == T2_SUB_GUN) {
                        total_firepower_value += 0.15 * Math.sqrt(level);
                    }
                }
            }
            total_firepower_value = Math.floor(total_firepower_value);
            int total_firepower = data.get("total-firepower").getAsInt();
            result.addProperty("total-firepower", total_firepower_value >= total_firepower);
            total_pass = total_pass && (total_firepower_value >= total_firepower);
        }

        result.addProperty("pass", total_pass);
        return result;
    }

    // Drum: 75, Daihatsu: 68, 89Tank: 166, Amp: 167, Toku-Daihatsu: 193
    private JsonObject getBonusInfo() {
        double bonus = 0.0;
        boolean kinu_exist = false;

        int bonus_default = 0;
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
        float plane_tais_value = 0;
        int total_los = 0;
        JsonObject result = new JsonObject();

        for (JsonObject obj : ship_data) {
            if (obj.get("ship_id").getAsInt() == 487) { // Kinu Kai Ni
                kinu_exist = true;
            }

            total_firepower += obj.get("karyoku").getAsInt();
            total_asw_value += obj.get("taisen").getAsInt();
            total_los += obj.get("sakuteki").getAsInt();
            for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                int type_t2 = itemobj.getAsJsonObject().getAsJsonArray("type").get(2).getAsInt();
                int slotitem_id = itemobj.getAsJsonObject().get("slotitem_id").getAsInt();
                int level = itemobj.getAsJsonObject().get("level").getAsInt();
                int tais = itemobj.getAsJsonObject().get("tais").getAsInt();
                if (KcaApiData.isItemAircraft(type_t2) || type_t2 == T2_AUTOGYRO || type_t2 == T2_ANTISUB_PATROL)
                    plane_tais_value += tais;

                if (type_t2 == T2_GUN_MEDIUM || type_t2 == T2_GUN_LARGE || type_t2 == T2_GUN_LARGE_II) {
                    total_firepower += Math.sqrt(level);
                } else if (type_t2 == T2_GUN_SMALL) {
                    total_firepower += 0.5 * Math.sqrt(level);
                } else if (type_t2 == T2_SUB_GUN) {
                    total_firepower += 0.15 * Math.sqrt(level);
                }

                if (type_t2 == T2_SONAR || type_t2 == T2_SONAR_LARGE || type_t2 == T2_DEPTH_CHARGE) {
                    total_asw_bonus += Math.sqrt(level);
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
        result.addProperty("asw", Math.floor(total_asw_value - plane_tais_value + total_asw_bonus * 2 / 3));
        result.addProperty("los", total_los);
        return result;
    }

    private int calculateBonusValue(int value, JsonObject bonus_info, boolean is_gs) {
        int kinu_count = bonus_info.get("kinu").getAsBoolean() ? 1 : 0;
        int daihatsu_count = bonus_info.get("daihatsu").getAsInt();
        int tank_count = bonus_info.get("tank").getAsInt();
        int amp_count = bonus_info.get("amp").getAsInt();
        int toku_count = bonus_info.get("toku").getAsInt();
        double bonus_level = bonus_info.get("level").getAsDouble();

        int dlc_count = kinu_count + daihatsu_count + toku_count;
        int ntoku_count = Math.min(4, daihatsu_count + tank_count + amp_count);
        double bonus_default = Math.min(dlc_count * 0.05 + tank_count * 0.02 + amp_count * 0.01, 0.2);
        bonus_default += 0.002 * bonus_level;
        double bonus_toku = 0.0;
        if (toku_count > 0) {
            int toku_idx;
            if (toku_count > 4) toku_idx = 3;
            else toku_idx = toku_count - 1;
            bonus_toku = toku_bonus[toku_idx][ntoku_count] * 0.01;
        }

        int value_bonus = 0;
        if (is_gs) {
            value_bonus = (int) (value * 3 * (1 + bonus_default) / 2) + (int) (value * 3 * bonus_toku / 2);
        } else {
            value_bonus = (int) (value * (1 + bonus_default)) + (int) (value * bonus_toku);
        }
        return value_bonus;
    }

    private void setItemViewVisibilityById(int id, boolean visible) {
        int visible_value = visible ? View.VISIBLE : View.GONE;
        itemView.findViewById(id).setVisibility(visible_value);
    }

    private void setItemTextViewById(int id, String value) {
        ((TextView) itemView.findViewById(id)).setText(value);
    }

    private void setItemTextViewColorById(int id, boolean value, boolean is_option) {
        if (value) {
            ((TextView) itemView.findViewById(id)).setTextColor(ContextCompat
                    .getColor(getApplicationContext(), R.color.colorExpeditionBtnGoodBack));
        } else if (is_option) {
            ((TextView) itemView.findViewById(id)).setTextColor(ContextCompat
                    .getColor(getApplicationContext(), R.color.grey));
        } else {
            ((TextView) itemView.findViewById(id)).setTextColor(ContextCompat
                    .getColor(getApplicationContext(), R.color.colorExpeditionBtnFailBack));
        }
    }


    private String convertTotalCond(String str) {
        String[] ship_count = str.split("\\-");
        String ship_concat;
        if (ship_count[0].equals("7,11,16,18")) {
            ship_concat = getString(R.string.excheckview_ship_cvs);
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
                if (check.get(count).getAsJsonObject().get(sc.split("\\-")[0]).getAsBoolean()) {
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

    private void clearItemViewLayout() {
        ((TextView) itemView.findViewById(R.id.view_excheck_title)).setText("");
        ((TextView) itemView.findViewById(R.id.view_excheck_time)).setText("");
        setItemViewVisibilityById(R.id.view_excheck_flagship, false);
        setItemViewVisibilityById(R.id.view_excheck_flagship_lv, false);
        setItemViewVisibilityById(R.id.view_excheck_flagship_cond, false);
        setItemViewVisibilityById(R.id.view_excheck_fleet, false);
        setItemViewVisibilityById(R.id.view_excheck_fleet_total_lv, false);
        setItemViewVisibilityById(R.id.view_excheck_fleet_condition, false);
        setItemViewVisibilityById(R.id.view_excheck_drum, false);
        setItemViewVisibilityById(R.id.view_excheck_drum_ship, false);
        setItemViewVisibilityById(R.id.view_excheck_drum_count, false);
        setItemViewVisibilityById(R.id.view_excheck_asw, false);
        setItemViewVisibilityById(R.id.view_excheck_fp, false);
        setItemViewVisibilityById(R.id.view_excheck_los, false);
        setItemViewVisibilityById(R.id.view_excheck_firepower, false);
    }

    public void setItemViewLayout(int index, JsonObject bonus_info) {
        if (index >= expedition_data.size()) {
            return;
        }
        int expd_value = expedition_data.get(index);
        Log.e("KCA", index + " " + expd_value);
        JsonObject data = KcaApiData.getExpeditionInfo(expd_value);

        String no = data.get("no").getAsString();
        String name = data.get("name").getAsString();
        int no_value = Integer.parseInt(no);
        String title = getExpeditionHeader(no_value).concat(name);
        int time = data.get("time").getAsInt() * 60;
        int total_num = data.get("total-num").getAsInt();
        JsonArray resource = data.getAsJsonArray("resource");
        int fuel = calculateBonusValue(resource.get(0).getAsInt(), bonus_info, isGreatSuccess);
        int ammo = calculateBonusValue(resource.get(1).getAsInt(), bonus_info, isGreatSuccess);
        int steel = calculateBonusValue(resource.get(2).getAsInt(), bonus_info, isGreatSuccess);
        int bauxite = calculateBonusValue(resource.get(3).getAsInt(), bonus_info, isGreatSuccess);
        String resource_text = KcaUtils.format("%d/%d/%d/%d", fuel, ammo, steel, bauxite);
        ((TextView) itemView.findViewById(R.id.excheck_reward_res)).setText(resource_text);
        if (isGreatSuccess) {
            ((TextView) itemView.findViewById(R.id.excheck_reward_res))
                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionGreatSuccess));
        } else {
            ((TextView) itemView.findViewById(R.id.excheck_reward_res))
                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        }

        JsonArray reward = data.getAsJsonArray("reward");
        int reward1 = reward.get(0).getAsJsonArray().get(0).getAsInt();
        int reward2 = reward.get(1).getAsJsonArray().get(0).getAsInt();
        try {
            if (reward1 > 0) {
                ((ImageView) itemView.findViewById(R.id.excheck_reward_item1))
                        .setImageResource(getId("common_itemicons_id_" + reward1, R.mipmap.class));
            } else {
                ((ImageView) itemView.findViewById(R.id.excheck_reward_item1)).setImageDrawable(null);
            }
        } catch (Exception e) {
            ((ImageView) itemView.findViewById(R.id.excheck_reward_item1)).setImageDrawable(null);
        }
        try {
            if (reward2 > 0) {
                ((ImageView) itemView.findViewById(R.id.excheck_reward_item2))
                        .setImageResource(getId("common_itemicons_id_" + reward2, R.mipmap.class));
            } else {
                ((ImageView) itemView.findViewById(R.id.excheck_reward_item2)).setImageDrawable(null);
            }
        } catch (Exception e) {
            ((ImageView) itemView.findViewById(R.id.excheck_reward_item2)).setImageDrawable(null);
        }

        JsonObject check = checkdata.get(no);
        if (check == null) return;

        boolean has_flag_lv = data.has("flag-lv");
        boolean has_flag_cond = data.has("flag-cond");
        boolean has_flag_info = has_flag_lv || has_flag_cond;
        boolean has_total_lv = data.has("total-lv");
        boolean has_total_cond = data.has("total-cond");
        boolean has_drum_ship = data.has("drum-ship");
        boolean has_drum_num = data.has("drum-num");
        boolean has_drum_num_optional = data.has("drum-num-optional");
        boolean has_drum_info = has_drum_ship || has_drum_num || has_drum_num_optional;
        boolean has_total_asw = data.has("total-asw");
        boolean has_total_fp = data.has("total-fp");
        boolean has_total_los = data.has("total-los");
        boolean has_total_firepower = data.has("total-firepower");

        ((LinearLayout) itemView.findViewById(R.id.view_excheck_fleet_condition)).removeAllViews();

        ((TextView) itemView.findViewById(R.id.view_excheck_title))
                .setText(title);

        initExpeditionButtonStyle();
        if (check.get("pass").getAsBoolean()) {
            ((TextView) itemView.findViewById(R.id.view_excheck_title))
                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnGoodBack));
            ((Chip) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(index)), R.id.class)))
                    .setChipBackgroundColorResource(R.color.colorExpeditionBtnGoodBack);
        } else {
            ((TextView) itemView.findViewById(R.id.view_excheck_title))
                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnFailBack));
            ((Chip) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(index)), R.id.class)))
                    .setChipBackgroundColorResource(R.color.colorExpeditionBtnFailBack);
        }
        ((Chip) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(index)), R.id.class)))
                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

        ((TextView) itemView.findViewById(R.id.view_excheck_time))
                .setText(getTimeStr(time));

        ((TextView) itemView.findViewById(R.id.view_excheck_fleet_total_num))
                .setText(KcaUtils.format(getString(R.string.excheckview_total_num_format), total_num));
        setItemTextViewColorById(R.id.view_excheck_fleet_total_num,
                check.get("total-num").getAsBoolean(), false);

        setItemViewVisibilityById(R.id.view_excheck_flagship, has_flag_info);
        if (has_flag_info) {
            setItemViewVisibilityById(R.id.view_excheck_flagship_lv, has_flag_lv);
            if (has_flag_lv) {
                int flag_lv = data.get("flag-lv").getAsInt();
                setItemTextViewById(R.id.view_excheck_flagship_lv,
                        KcaUtils.format(getString(R.string.excheckview_flag_lv_format), flag_lv));
                setItemTextViewColorById(R.id.view_excheck_flagship_lv,
                        check.get("flag-lv").getAsBoolean(), false);
            }
            setItemViewVisibilityById(R.id.view_excheck_flagship_cond, has_flag_cond);
            if (has_flag_cond) {
                String[] flag_cond = data.get("flag-cond").getAsString().split("/");
                List<String> abbr_text_list = new ArrayList<>();
                for (int i = 0; i < flag_cond.length; i++) {
                    abbr_text_list.add(getShipTypeAbbr(Integer.parseInt(flag_cond[i])));
                }
                setItemTextViewById(R.id.view_excheck_flagship_cond, KcaUtils.joinStr(abbr_text_list, "/"));
                setItemTextViewColorById(R.id.view_excheck_flagship_cond,
                        check.get("flag-cond").getAsBoolean(), false);
            }
        }

        setItemViewVisibilityById(R.id.view_excheck_fleet, true);
        setItemViewVisibilityById(R.id.view_excheck_fleet_total_lv, has_total_lv);
        if (has_total_lv) {
            int total_lv = data.get("total-lv").getAsInt();
            setItemTextViewById(R.id.view_excheck_fleet_total_lv,
                    KcaUtils.format(getString(R.string.excheckview_total_lv_format), total_lv));
            setItemTextViewColorById(R.id.view_excheck_fleet_total_lv,
                    check.get("total-lv").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_fleet_condition, has_total_cond);
        if (has_total_cond) {
            String total_cond = data.get("total-cond").getAsString();
            for (View v : generateConditionView(total_cond, check.getAsJsonArray("total-cond"))) {
                ((LinearLayout) itemView.findViewById(R.id.view_excheck_fleet_condition)).addView(v);
            }
        }

        setItemViewVisibilityById(R.id.view_excheck_drum, has_drum_info);
        if (has_drum_info) {
            setItemViewVisibilityById(R.id.view_excheck_drum_ship, has_drum_ship);
            if (has_drum_ship) {
                int drum_ship = data.get("drum-ship").getAsInt();
                setItemTextViewById(R.id.view_excheck_drum_ship,
                        KcaUtils.format(getString(R.string.excheckview_drum_ship_format), drum_ship));
                setItemTextViewColorById(R.id.view_excheck_drum_ship,
                        check.get("drum-ship").getAsBoolean(), false);
            }
            setItemViewVisibilityById(R.id.view_excheck_drum_count, has_drum_num || has_drum_num_optional);
            if (has_drum_num) {
                int drum_num = data.get("drum-num").getAsInt();
                setItemTextViewById(R.id.view_excheck_drum_count,
                        KcaUtils.format(getString(R.string.excheckview_drum_num_format), drum_num));
                setItemTextViewColorById(R.id.view_excheck_drum_count,
                        check.get("drum-num").getAsBoolean(), false);
            } else if (has_drum_num_optional) {
                int drum_num = data.get("drum-num-optional").getAsInt();
                setItemTextViewById(R.id.view_excheck_drum_count,
                        KcaUtils.format(getString(R.string.excheckview_drum_num_format), drum_num));
                setItemTextViewColorById(R.id.view_excheck_drum_count,
                        check.get("drum-num").getAsBoolean(), true);
            }
        }

        setItemViewVisibilityById(R.id.view_excheck_asw, has_total_asw);
        if (has_total_asw) {
            int total_asw = data.get("total-asw").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_asw,
                    KcaUtils.format(getString(R.string.excheckview_total_format), total_asw));
            setItemTextViewColorById(R.id.view_excheck_total_asw,
                    check.get("total-asw").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_fp, has_total_fp);
        if (has_total_fp) {
            int total_fp = data.get("total-fp").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_fp,
                    KcaUtils.format(getString(R.string.excheckview_total_format), total_fp));
            setItemTextViewColorById(R.id.view_excheck_total_fp,
                    check.get("total-fp").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_los, has_total_los);
        if (has_total_los) {
            int total_los = data.get("total-los").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_los,
                    KcaUtils.format(getString(R.string.excheckview_total_format), total_los));
            setItemTextViewColorById(R.id.view_excheck_total_los,
                    check.get("total-los").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_firepower, has_total_firepower);
        if (has_total_firepower) {
            int total_firepower = data.get("total-firepower").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_firepower,
                    KcaUtils.format(getString(R.string.excheckview_total_format), total_firepower));
            setItemTextViewColorById(R.id.view_excheck_total_firepower,
                    check.get("total-firepower").getAsBoolean(), false);
        }

        itemView.setVisibility(View.VISIBLE);
    }

    public int setView() {
        try {
            expedition_data = KcaApiData.getExpeditionNumByWorld(world);
            JsonArray api_ship = deckdata.get(selected)
                    .getAsJsonObject().getAsJsonArray("api_ship");
            ship_data.clear();
            if (checkUserShipDataLoaded()) {
                for (int i = 0; i < api_ship.size(); i++) {
                    int id = api_ship.get(i).getAsInt();
                    if (id > 0) {
                        JsonObject data = new JsonObject();
                        JsonObject usershipinfo = getUserShipDataById(id, "ship_id,lv,slot,cond,karyoku,taisen,taiku,sakuteki");
                        if (usershipinfo != null) {
                            JsonObject kcshipinfo = getKcShipDataById(usershipinfo.get("ship_id").getAsInt(), "stype");

                            data.addProperty("ship_id", usershipinfo.get("ship_id").getAsInt());
                            data.addProperty("lv", usershipinfo.get("lv").getAsInt());
                            data.addProperty("cond", usershipinfo.get("cond").getAsInt());
                            data.addProperty("stype", kcshipinfo.get("stype").getAsInt());
                            data.addProperty("karyoku", usershipinfo.getAsJsonArray("karyoku").get(0).getAsInt());
                            data.addProperty("taisen", usershipinfo.getAsJsonArray("taisen").get(0).getAsInt());
                            data.addProperty("taiku", usershipinfo.getAsJsonArray("taiku").get(0).getAsInt());
                            data.addProperty("sakuteki", usershipinfo.getAsJsonArray("sakuteki").get(0).getAsInt());
                            data.add("item", new JsonArray());
                            JsonArray shipslot = usershipinfo.getAsJsonArray("slot");
                            for (int j = 0; j < shipslot.size(); j++) {
                                int itemid = shipslot.get(j).getAsInt();
                                if (itemid > 0) {
                                    JsonObject iteminfo = getUserItemStatusById(itemid, "slotitem_id,level", "type,tais");
                                    if (iteminfo != null) data.getAsJsonArray("item").add(iteminfo);
                                }
                            }
                            ship_data.add(data);
                        }
                    }
                }

                checkdata = new HashMap<>();
                for (int i = 0; i < 15; i++) {
                    if (i < expedition_data.size()) {
                        int expd = expedition_data.get(i);
                        checkdata.put(String.valueOf(expd), checkCondition(expd));
                    }
                }
                initExpeditionButtonStyle();
                updateSelectedView(selected, world);

                JsonObject bonus_info = getBonusInfo();
                List<String> bonus_info_text = new ArrayList<>();

                int total_firepower = bonus_info.get("firepower").getAsInt();
                bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_firepower), total_firepower));

                int total_asw = bonus_info.get("asw").getAsInt();
                bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_asw), total_asw));

                int total_los = bonus_info.get("los").getAsInt();
                bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_los), total_los));

                int drum_count = bonus_info.get("drum").getAsInt();
                if (drum_count > 0) {
                    bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_drum), drum_count));
                } else {
                    bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_drum), 0));
                }
                if (bonus_info.get("kinu").getAsBoolean())
                    bonus_info_text.add(getString(R.string.excheckview_bonus_kinu));
                int daihatsu_count = bonus_info.get("daihatsu").getAsInt();
                if (daihatsu_count > 0)
                    bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_dlc), daihatsu_count));
                int tank_count = bonus_info.get("tank").getAsInt();
                if (tank_count > 0)
                    bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_tank), tank_count));
                int amp_count = bonus_info.get("amp").getAsInt();
                if (amp_count > 0)
                    bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_amp), amp_count));
                int toku_count = bonus_info.get("toku").getAsInt();
                if (toku_count > 0)
                    bonus_info_text.add(KcaUtils.format(getString(R.string.excheckview_bonus_toku), toku_count));
                String bonus_info_content = joinStr(bonus_info_text, " / ");
                double bonus_value = (double) calculateBonusValue(100, bonus_info, false);
                if (bonus_value > 100.0) {
                    bonus_info_content = bonus_info_content.concat(KcaUtils.format(getString(R.string.excheckview_bonus_result), bonus_value - 100.0));
                }
                ((TextView) layoutView.findViewById(R.id.excheck_info)).setText(bonus_info_content);
                layoutView.findViewById(R.id.excheck_info).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
                setItemViewLayout(button, bonus_info);
            } else {
                ((TextView) layoutView.findViewById(R.id.excheck_info)).setText(getString(R.string.kca_init_content));
                layoutView.findViewById(R.id.excheck_info).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            }
            return 0;
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), getStringFromException(e), Toast.LENGTH_LONG).show();
            return 1;
        }
    }

    private void initExpeditionButtonStyle() {
        for (int i = 0; i < 15; i++) {
            if (i < expedition_data.size()) {
                int expd = expedition_data.get(i);
                String key = String.valueOf(expd);
                JsonObject check_item = checkdata.get(key);
                if (check_item != null) {
                    ((Chip) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(i)), R.id.class)))
                            .setChipBackgroundColorResource(R.color.transparent);
                    if (check_item.get("pass").getAsBoolean()) {
                        ((Chip) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(i)), R.id.class)))
                                .setChipStrokeColorResource(R.color.colorExpeditionBtnGoodBack);
                        ((TextView) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(i)), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnGoodText));
                    } else {
                        ((Chip) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(i)), R.id.class)))
                                .setChipStrokeColorResource(R.color.colorExpeditionBtnFailBack);
                        ((TextView) layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(i)), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnFailText));
                    }
                }
            }
        }
    }

    private final View.OnClickListener mViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == layoutView.findViewById(R.id.excheckview_head).getId()) {
                stopSelf();
            } else if (checkUserShipDataLoaded()) {
                JsonObject bonus_info = getBonusInfo();
                if (id == layoutView.findViewById(R.id.excheck_detail_reward).getId()) {
                    isGreatSuccess = !isGreatSuccess;
                    setItemViewLayout(button, bonus_info);
                    return;
                }

                for (int i = 1; i <= 7; i++) {
                    if (id == layoutView.findViewById(getId("expd_world_".concat(String.valueOf(i)), R.id.class)).getId()) {
                        world = i;
                        button = 0;
                        clearItemViewLayout();
                        setView();
                        return;
                    }
                }
                for (int i = 0; i < 15; i++) {
                    if (id == layoutView.findViewById(getId("expd_btn_".concat(String.valueOf(i)), R.id.class)).getId()) {
                        button = i;
                        setItemViewLayout(button, bonus_info);
                        break;
                    }
                }
            }
        }
    };

    private final View.OnClickListener fleetChipOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            for (int i = 1; i < 4; i++) {
                if (id == layoutView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).getId()) {
                    updateFleetChips(i);
                    if (i < deckdata.size()) {
                        selected = i;
                    }
                    if (checkUserShipDataLoaded()) {
                        JsonObject bonus_info = getBonusInfo();
                        setView();
                        return;
                    }
                }
            }
        }
    };

    private void updateFleetChips(int newSelected) {
        for (int i = 1; i < 4; i++) {
            Chip chip = layoutView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class));
            if (newSelected == i) {
                chip.setChipStrokeColorResource(R.color.colorAccent);
            } else {
                chip.setChipStrokeColorResource(R.color.colorFleetInfoNormalBtn);
            }
        }
    }
}
