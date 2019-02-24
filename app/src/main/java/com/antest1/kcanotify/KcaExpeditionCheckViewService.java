package com.antest1.kcanotify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.antest1.kcanotify.KcaApiData.T2_FLYING_BOAT;
import static com.antest1.kcanotify.KcaApiData.T2_SEA_BOMBER;
import static com.antest1.kcanotify.KcaApiData.T2_SEA_FIGHTER;
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

public class KcaExpeditionCheckViewService extends Service {
    public static final String SHOW_EXCHECKVIEW_ACTION = "show_excheckview";
    final int[] expedition_list = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 35, 36, 37, 38, 39, 40,
            100, 101, 102, 110, 111, 133, 134};
    public static final double[][] toku_bonus = {
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {4.0, 4.0, 4.0, 4.0, 4.0},
            {5.0, 5.0, 5.2, 5.4, 5.4},
            {5.4, 5.6, 5.8, 5.9, 6.0}
    };

    public static boolean active;
    static boolean error_flag = false;
    Context contextWithLocale;
    int displayWidth = 0;
    public KcaDBHelper dbHelper;
    private View mView, itemView;
    LayoutInflater mInflater;
    private WindowManager mManager;
    WindowManager.LayoutParams mParams;

    String locale;
    int selected = 1;
    JsonArray deckdata;
    List<JsonObject> ship_data;
    Map<String, JsonObject> checkdata;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    private void showInfoView(MotionEvent paramMotionEvent, int selected) {
        setItemViewLayout(KcaApiData.getExpeditionInfo(selected, locale));
        itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = itemView.getMeasuredWidth();
        int height = itemView.getMeasuredHeight();

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        localLayoutParams.x = ((int) (125.0F + paramMotionEvent.getRawX()));
        localLayoutParams.y = ((int) paramMotionEvent.getRawY());
        if (selected >= 100) {
            if (selected >= 111) localLayoutParams.x -= (125 + width);
            localLayoutParams.y -= (height + 25);
        } else {
            if ((selected - 1) % 8 >= 4) localLayoutParams.x -= (125 + width);
            if (selected > 24) localLayoutParams.y -= (height + 25);
        }

        localLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        if (itemView.getParent() != null) {
            mManager.removeViewImmediate(itemView);
        }
        mManager.addView(itemView, localLayoutParams);
    }

    private void updateSelectedView(int idx) {
        for (int i = 1; i < 4; i++) {
            int view_id = getId("fleet_".concat(String.valueOf(i + 1)), R.id.class);
            if (idx == i) {
                mView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            } else {
                mView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
            }
        }
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }

        try {
            active = true;
            locale = LocaleUtils.getResourceLocaleCode(KcaUtils.getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
            ship_data = new ArrayList<>();
            checkdata = new HashMap<>();
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            contextWithLocale = KcaUtils.getContextWithLocale(getApplicationContext(), getBaseContext());
            mInflater = LayoutInflater.from(contextWithLocale);
            mView = mInflater.inflate(R.layout.view_excheck_list, null);
            KcaUtils.resizeFullWidthView(getApplicationContext(), mView);
            mView.setVisibility(View.GONE);
            mView.findViewById(R.id.excheckview_head).setOnTouchListener(mViewTouchListener);
            for (int i = 1; i < 4; i++) {
                mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
            }

            for (int no: expedition_list) {
                mView.findViewById(KcaUtils.getId("expedition_btn_".concat(String.valueOf(no)), R.id.class)).setOnTouchListener(mViewTouchListener);
            }

            itemView = mInflater.inflate(R.layout.view_excheck_detail, null);
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            mParams.gravity = Gravity.CENTER;

            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            displayWidth = size.x;
        } catch (Exception e) {
            active = false;
            error_flag = true;
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        active = false;
        if (mView != null) {
            if (mView.getParent() != null) mManager.removeViewImmediate(mView);
        }
        if (itemView != null) {
            if (itemView.getParent() != null) mManager.removeViewImmediate(itemView);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().startsWith(SHOW_EXCHECKVIEW_ACTION)) {
                deckdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                if (deckdata != null) {
                    int selected_new = Integer.parseInt(intent.getAction().split("/")[1]);
                    if (selected_new < 1) selected_new = 1;
                    else if (selected_new > 3) selected_new = 2;
                    if (selected_new < deckdata.size()) {
                        selected = selected_new;
                    }
                    int setViewResult = setView();
                    if (setViewResult == 0) {
                        if (mView.getParent() != null) {
                            mManager.removeViewImmediate(mView);
                        }
                        mManager.addView(mView, mParams);
                    }
                    Log.e("KCA", "show_excheckview_action " + String.valueOf(setViewResult));
                    mView.setVisibility(View.VISIBLE);
                } else {
                    stopSelf();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private JsonObject checkFleetCondition(String data) {
        boolean total_pass = false;
        Map<String, Integer> stypedata = new HashMap<>();
        for (JsonObject obj : ship_data) {
            String stype = String.valueOf(obj.get("stype").getAsInt());
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

        JsonObject data = KcaApiData.getExpeditionInfo(exp_no, locale);

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
            if (ship_data.size() > 0) {
                int flag_conv_value = ship_data.get(0).get("stype").getAsInt();
                int flag_cond = data.get("flag-cond").getAsInt();
                result.addProperty("flag-cond", flag_conv_value == flag_cond);
                total_pass = total_pass && (flag_conv_value == flag_cond);
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
                switch (type_t2) {
                    case T2_SEA_BOMBER:
                    case T2_SEA_FIGHTER:
                    case T2_FLYING_BOAT:
                        plane_tais_value += tais;
                        break;
                    default:
                        break;
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
            int total_asw_value = 0;
            for (JsonObject obj : ship_data) {
                total_asw_value += (obj.get("taisen").getAsInt());
            }
            int total_asw = data.get("total-asw").getAsInt();
            result.addProperty("total-asw", total_asw_value >= (total_asw - plane_tais_value));
            total_pass = total_pass && (total_asw_value >= (total_asw - plane_tais_value));
        }

        result.addProperty("total-fp", true);
        if (has_total_fp) {
            int total_fp_value = 0;
            for (JsonObject obj : ship_data) {
                total_fp_value += (obj.get("taiku").getAsInt());
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
            }
            int total_los = data.get("total-los").getAsInt();
            result.addProperty("total-los", total_los_value >= total_los);
            total_pass = total_pass && (total_los_value >= total_los);
        }

        result.addProperty("total-firepower", true);
        if (has_total_firepower) {
            int total_firepower_value = 0;
            for (JsonObject obj : ship_data) {
                total_firepower_value += obj.get("karyoku").getAsInt();
            }
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
        int drum_count = 0;
        int bonus_count = 0;
        int daihatsu_count = 0;
        int tank_count = 0;
        int amp_count = 0;
        int toku_count = 0;
        double bonus_level = 0.0;

        int total_asw = 0;
        int total_los = 0;
        JsonObject result = new JsonObject();

        for (JsonObject obj : ship_data) {
            if (obj.get("ship_id").getAsInt() == 487) { // Kinu Kai Ni
                bonus += 5.0;
                kinu_exist = true;
            }
            total_asw += obj.get("taisen").getAsInt();
            total_los += obj.get("sakuteki").getAsInt();
            for (JsonElement itemobj : obj.getAsJsonArray("item")) {
                int slotitem_id = itemobj.getAsJsonObject().get("slotitem_id").getAsInt();
                int level = itemobj.getAsJsonObject().get("level").getAsInt();
                switch (slotitem_id) {
                    case 75:
                        drum_count += 1;
                        break;
                    case 68:
                        bonus += 5.0;
                        bonus_level += level;
                        bonus_count += 1.0;
                        daihatsu_count += 1;
                        break;
                    case 166:
                        bonus += 2.0;
                        bonus_level += level;
                        bonus_count += 1.0;
                        tank_count += 1;
                        break;
                    case 167:
                        bonus += 1.0;
                        bonus_level += level;
                        bonus_count += 1.0;
                        amp_count += 1;
                        break;
                    case 193:
                        bonus += 5.0;
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
        int left_count = bonus_count - toku_count;
        if (left_count > 4) left_count = 4;
        if (bonus > 20) bonus = 20.0;
        bonus += (100.0 + 0.2 * bonus_level);
        if (toku_count > 0) {
            int toku_idx;
            if (toku_count > 4) toku_idx = 3;
            else toku_idx = toku_count - 1;
            bonus += toku_bonus[toku_idx][left_count];
        }
        result.addProperty("kinu", kinu_exist);
        result.addProperty("drum", drum_count);
        result.addProperty("daihatsu", daihatsu_count);
        result.addProperty("tank", tank_count);
        result.addProperty("amp", amp_count);
        result.addProperty("toku", toku_count);
        result.addProperty("bonus", bonus);
        result.addProperty("asw", total_asw);
        result.addProperty("los", total_los);
        return result;
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

    public void setItemViewLayout(JsonObject data) {
        String no = data.get("no").getAsString();
        String name = data.get("name").getAsString();
        int no_value = Integer.parseInt(no);
        String title = getExpeditionHeader(no_value).concat(name);
        int time = data.get("time").getAsInt() * 60;
        int total_num = data.get("total-num").getAsInt();

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
        ((TextView) itemView.findViewById(R.id.view_excheck_time))
                .setText(getTimeStr(time));

        ((TextView) itemView.findViewById(R.id.view_excheck_fleet_total_num))
                .setText(KcaUtils.format(getStringWithLocale(R.string.excheckview_total_num_format), total_num));
        setItemTextViewColorById(R.id.view_excheck_fleet_total_num,
                check.get("total-num").getAsBoolean(), false);

        setItemViewVisibilityById(R.id.view_excheck_flagship, has_flag_info);
        if (has_flag_info) {
            setItemViewVisibilityById(R.id.view_excheck_flagship_lv, has_flag_lv);
            if (has_flag_lv) {
                int flag_lv = data.get("flag-lv").getAsInt();
                setItemTextViewById(R.id.view_excheck_flagship_lv,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_flag_lv_format), flag_lv));
                setItemTextViewColorById(R.id.view_excheck_flagship_lv,
                        check.get("flag-lv").getAsBoolean(), false);
            }
            setItemViewVisibilityById(R.id.view_excheck_flagship_cond, has_flag_cond);
            if (has_flag_cond) {
                int flag_cond = data.get("flag-cond").getAsInt();
                setItemTextViewById(R.id.view_excheck_flagship_cond,
                        getShipTypeAbbr(flag_cond));
                setItemTextViewColorById(R.id.view_excheck_flagship_cond,
                        check.get("flag-cond").getAsBoolean(), false);
            }
        }

        setItemViewVisibilityById(R.id.view_excheck_fleet_total_lv, has_total_lv);
        if (has_total_lv) {
            int total_lv = data.get("total-lv").getAsInt();
            setItemTextViewById(R.id.view_excheck_fleet_total_lv,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_lv_format), total_lv));
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
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_drum_ship_format), drum_ship));
                setItemTextViewColorById(R.id.view_excheck_drum_ship,
                        check.get("drum-ship").getAsBoolean(), false);
            }
            setItemViewVisibilityById(R.id.view_excheck_drum_count, has_drum_num || has_drum_num_optional);
            if (has_drum_num) {
                int drum_num = data.get("drum-num").getAsInt();
                setItemTextViewById(R.id.view_excheck_drum_count,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_drum_num_format), drum_num));
                setItemTextViewColorById(R.id.view_excheck_drum_count,
                        check.get("drum-num").getAsBoolean(), false);
            } else if (has_drum_num_optional) {
                int drum_num = data.get("drum-num-optional").getAsInt();
                setItemTextViewById(R.id.view_excheck_drum_count,
                        KcaUtils.format(getStringWithLocale(R.string.excheckview_drum_num_format), drum_num));
                setItemTextViewColorById(R.id.view_excheck_drum_count,
                        check.get("drum-num").getAsBoolean(), true);
            }
        }

        setItemViewVisibilityById(R.id.view_excheck_asw, has_total_asw);
        if (has_total_asw) {
            int total_asw = data.get("total-asw").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_asw,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_asw));
            setItemTextViewColorById(R.id.view_excheck_total_asw,
                    check.get("total-asw").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_fp, has_total_asw);
        if (has_total_fp) {
            int total_fp = data.get("total-fp").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_fp,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_fp));
            setItemTextViewColorById(R.id.view_excheck_total_fp,
                    check.get("total-fp").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_los, has_total_los);
        if (has_total_los) {
            int total_los = data.get("total-los").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_los,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_los));
            setItemTextViewColorById(R.id.view_excheck_total_los,
                    check.get("total-los").getAsBoolean(), false);
        }

        setItemViewVisibilityById(R.id.view_excheck_firepower, has_total_firepower);
        if (has_total_firepower) {
            int total_firepower = data.get("total-firepower").getAsInt();
            setItemTextViewById(R.id.view_excheck_total_firepower,
                    KcaUtils.format(getStringWithLocale(R.string.excheckview_total_format), total_firepower));
            setItemTextViewColorById(R.id.view_excheck_total_firepower,
                    check.get("total-firepower").getAsBoolean(), false);
        }

        itemView.setVisibility(View.VISIBLE);
    }

    public int setView() {
        try {
            JsonArray api_ship = deckdata.get(selected)
                    .getAsJsonObject().getAsJsonArray("api_ship");
            ship_data.clear();
            if (checkUserShipDataLoaded()) {
                for (int i = 0; i < api_ship.size(); i++) {
                    int id = api_ship.get(i).getAsInt();
                    if (id > 0) {
                        JsonObject data = new JsonObject();
                        JsonObject usershipinfo = getUserShipDataById(id, "ship_id,lv,slot,cond,karyoku,taisen,taiku,sakuteki");
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
                for (int i: expedition_list) {
                    String key = String.valueOf(i);
                    checkdata.put(key, checkCondition(i));
                    if (checkdata.get(key).get("pass").getAsBoolean()) {
                        mView.findViewById(getId("expedition_btn_".concat(String.valueOf(i)), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnGoodBack));
                        ((TextView) mView.findViewById(getId("expedition_btn_".concat(String.valueOf(i)), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnGoodText));
                    } else {
                        mView.findViewById(getId("expedition_btn_".concat(String.valueOf(i)), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnFailBack));
                        ((TextView) mView.findViewById(getId("expedition_btn_".concat(String.valueOf(i)), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorExpeditionBtnFailText));
                    }
                }
                updateSelectedView(selected);

                JsonObject bonus_info = getBonusInfo();
                List<String> bonus_info_text = new ArrayList<>();

                int total_asw = bonus_info.get("asw").getAsInt();
                bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_asw), total_asw));

                int total_los = bonus_info.get("los").getAsInt();
                bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_los), total_los));

                int drum_count = bonus_info.get("drum").getAsInt();
                if (drum_count > 0) {
                    bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_drum), drum_count));
                } else {
                    bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_drum), 0));
                }
                if (bonus_info.get("kinu").getAsBoolean())
                    bonus_info_text.add(getStringWithLocale(R.string.excheckview_bonus_kinu));
                int daihatsu_count = bonus_info.get("daihatsu").getAsInt();
                if (daihatsu_count > 0)
                    bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_dlc), daihatsu_count));
                int tank_count = bonus_info.get("tank").getAsInt();
                if (tank_count > 0)
                    bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_tank), tank_count));
                int amp_count = bonus_info.get("amp").getAsInt();
                if (amp_count > 0)
                    bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_amp), amp_count));
                int toku_count = bonus_info.get("toku").getAsInt();
                if (toku_count > 0)
                    bonus_info_text.add(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_toku), toku_count));
                String bonus_info_content = joinStr(bonus_info_text, " / ");
                double bonus_value = bonus_info.get("bonus").getAsDouble() - 100.0;
                if (bonus_value > 0.0) {
                    bonus_info_content = bonus_info_content.concat(KcaUtils.format(getStringWithLocale(R.string.excheckview_bonus_result), bonus_value));
                }
                ((TextView) mView.findViewById(R.id.excheck_info)).setText(bonus_info_content);
                mView.findViewById(R.id.excheck_info).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
            } else {
                ((TextView) mView.findViewById(R.id.excheck_info)).setText(getStringWithLocale(R.string.kca_init_content));
                mView.findViewById(R.id.excheck_info).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            }
            return 0;
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), getStringFromException(e), Toast.LENGTH_LONG).show();
            return 1;
        }
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e("KCA-FV", "ACTION_DOWN");
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    if (checkUserShipDataLoaded()) {
                        for (int no: expedition_list) {
                            if (id == mView.findViewById(getId("expedition_btn_".concat(String.valueOf(no)), R.id.class)).getId()) {
                                setItemViewLayout(KcaApiData.getExpeditionInfo(no, locale));
                                showInfoView(event, no);
                                break;
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("KCA-FV", "ACTION_UP");
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    itemView.setVisibility(View.GONE);

                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == mView.findViewById(R.id.excheckview_head).getId()) {
                            stopSelf();
                        } else {
                            for (int i = 1; i < 4; i++) {
                                if (id == mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).getId()) {
                                    if (i < deckdata.size()) {
                                        selected = i;
                                    }
                                    setView();
                                    break;
                                }
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    };
}
