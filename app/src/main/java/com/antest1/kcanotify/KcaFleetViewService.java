package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.antest1.kcanotify.KcaAkashiViewService.SHOW_AKASHIVIEW_ACTION;
import static com.antest1.kcanotify.KcaApiData.getItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getShipTypeAbbr;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.isItemAircraft;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_FLEETVIEW;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.SEEK_PURE;
import static com.antest1.kcanotify.KcaQuestViewService.SHOW_QUESTVIEW_ACTION_NEW;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaFleetViewService extends Service {
    public static final String SHOW_FLEETVIEW_ACTION = "show_fleetview_action";
    public static final String REFRESH_FLEETVIEW_ACTION = "update_fleetview_action";
    public static final String CLOSE_FLEETVIEW_ACTION = "close_fleetview_action";
    public static final int FLEET_COMBINED_ID = 4;

    Context contextWithLocale;
    LayoutInflater mInflater;
    public KcaDBHelper helper;
    public KcaDeckInfo deckInfoCalc;
    KcaDeckInfo deckInfoObject;
    int seekcn_internal = -1;

    static boolean error_flag = false;
    boolean active;
    private View mView, itemView;
    private TextView fleetInfoTitle, fleetInfoLine, fleetExpView, fleetCnChangeBtn;
    private WindowManager mManager;
    private static boolean isReady;

    int displayWidth = 0;

    WindowManager.LayoutParams mParams;

    int selected;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    public static void setReadyFlag(boolean flag) {
        isReady = flag;
    }

    private static String makeLvString(int level) {
        return KcaUtils.format("Lv %d", level);
    }

    private static String makeSimpleExpString(float e1, float e2) {
        return KcaUtils.format("%.2f/%.2f", e1, e2);
    }

    private static String makeHpString(int currenthp, int maxhp) {
        return KcaUtils.format("HP %d/%d", currenthp, maxhp);
    }

    @SuppressLint("DefaultLocale")

    public int setView() {
        try {
            Log.e("KCA-FV", String.valueOf(selected));
            boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            float[] exp_score = helper.getExpScore();

            if (is_landscape) {
                fleetExpView.setText(KcaUtils.format(
                        getStringWithLocale(R.string.fleetview_expview),
                        exp_score[0], exp_score[1]));
                fleetInfoTitle.setVisibility(View.VISIBLE);
            } else {
                fleetExpView.setText(makeSimpleExpString(exp_score[0], exp_score[1]));
                fleetInfoTitle.setVisibility(View.GONE);
            }
            updateSelectedView(selected);

            if (seekcn_internal == -1) seekcn_internal = getSeekCn();
            processDeckInfo(selected, isCombinedFlag(selected));
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            sendReport(e, 0);
            return 1;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }
        try {
            active = true;
            deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());
            helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            helper.updateExpScore(0);
            if (helper.getJsonArrayValue(DB_KEY_DECKPORT) == null) {
                stopSelf();
            }

            contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
            //mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInflater = LayoutInflater.from(contextWithLocale);
            mView = mInflater.inflate(R.layout.view_fleet_list, null);
            mView.setVisibility(View.GONE);
            mView.findViewById(R.id.fleetview_head).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.fleetview_cn_change).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.viewbutton_quest).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.viewbutton_akashi).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.viewbutton_develop).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.viewbutton_construction).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.viewbutton_maphp).setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.viewbutton_excheck).setOnTouchListener(mViewTouchListener);
            for (int i = 0; i < 5; i++) {
                mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
            }
            for (int i = 0; i < 12; i++) {
                mView.findViewById(getId("fleetview_item_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
            }

            ((TextView) mView.findViewById(R.id.viewbutton_quest)).setText(getStringWithLocale(R.string.viewmenu_quest));
            ((TextView) mView.findViewById(R.id.viewbutton_akashi)).setText(getStringWithLocale(R.string.viewmenu_akashi));
            fleetInfoLine = mView.findViewById(R.id.fleetview_infoline);
            fleetInfoLine.setText(getStringWithLocale(R.string.kca_init_content));
            fleetInfoLine.setOnTouchListener(mViewTouchListener);

            fleetInfoTitle = mView.findViewById(R.id.fleetview_title);
            fleetExpView = mView.findViewById(R.id.fleetview_exp);
            fleetCnChangeBtn = mView.findViewById(R.id.fleetview_cn_change);

            itemView = mInflater.inflate(R.layout.view_battleview_items, null);

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
            e.printStackTrace();
            active = false;
            error_flag = true;
            sendReport(e, 1);
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
            if (intent.getAction().equals(SHOW_FLEETVIEW_ACTION)) {
                int setViewResult = setView();
                if (setViewResult == 0) {
                    mView.setVisibility(View.VISIBLE);
                    if (mView.getParent() != null) {
                        mManager.removeViewImmediate(mView);
                    }
                    mManager.addView(mView, mParams);
                }
            }
            if (intent.getAction().equals(REFRESH_FLEETVIEW_ACTION)) {
                int setViewResult = setView();
                if (setViewResult == 0) {
                    if (mView.getParent() != null) {
                        mView.invalidate();
                        mManager.updateViewLayout(mView, mParams);
                    }
                }
            }
            if (intent.getAction().equals(CLOSE_FLEETVIEW_ACTION)) {
                mView.setVisibility(View.GONE);
                if (mView.getParent() != null) {
                    mManager.removeViewImmediate(mView);
                }
                itemView.setVisibility(View.GONE);
                if (itemView.getParent() != null) {
                    mManager.removeViewImmediate(itemView);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            WindowManager.LayoutParams itemViewParams;
            int xMargin = 200;

            Intent qintent;
            int id = v.getId();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e("KCA-FV", "ACTION_DOWN");
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    if (id == fleetInfoLine.getId()) {
                        fleetInfoLine.setSelected(true);
                    }
                    for (int i = 0; i < 12; i++) {
                        String item_id = "fleetview_item_".concat(String.valueOf(i + 1));
                        if (id == mView.findViewById(getId(item_id, R.id.class)).getId()) {
                            JsonArray data;
                            if (isCombinedFlag(selected)) {
                                if (i < 6)
                                    data = deckInfoCalc.getDeckListInfo(helper.getJsonArrayValue(DB_KEY_DECKPORT), 0);
                                else
                                    data = deckInfoCalc.getDeckListInfo(helper.getJsonArrayValue(DB_KEY_DECKPORT), 1);
                            } else {
                                data = deckInfoCalc.getDeckListInfo(helper.getJsonArrayValue(DB_KEY_DECKPORT), selected);
                            }

                            JsonObject udata = data.get(i % 6).getAsJsonObject().getAsJsonObject("user");
                            JsonObject kcdata = data.get(i % 6).getAsJsonObject().getAsJsonObject("kc");
                            JsonObject itemdata = new JsonObject();
                            itemdata.add("api_slot", udata.get("slot"));
                            itemdata.add("api_slot_ex", udata.get("slot_ex"));
                            itemdata.add("api_onslot", udata.get("onslot"));
                            itemdata.add("api_maxslot", kcdata.get("maxeq"));

                            setItemViewLayout(itemdata);
                            itemViewParams = new WindowManager.LayoutParams(
                                    WindowManager.LayoutParams.WRAP_CONTENT,
                                    WindowManager.LayoutParams.WRAP_CONTENT,
                                    getWindowLayoutType(),
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                    PixelFormat.TRANSLUCENT);
                            itemViewParams.x = (int) (event.getRawX() + xMargin);
                            itemViewParams.y = (int) event.getRawY();
                            itemViewParams.gravity = Gravity.TOP | Gravity.LEFT;
                            if (itemView.getParent() != null) {
                                mManager.removeViewImmediate(itemView);
                            }
                            mManager.addView(itemView, itemViewParams);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("KCA-FV", "ACTION_UP");
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (id == fleetInfoLine.getId()) {
                        fleetInfoLine.setSelected(false);
                    }

                    for (int i = 0; i < 12; i++) {
                        String item_id = "fleetview_item_".concat(String.valueOf(i + 1));
                        if (id == mView.findViewById(getId(item_id, R.id.class)).getId()) {
                            itemView.setVisibility(View.GONE);
                            break;
                        }
                    }

                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == mView.findViewById(R.id.fleetview_head).getId()) {
                            if (mView != null) mView.setVisibility(View.GONE);
                            if (itemView != null) itemView.setVisibility(View.GONE);
                        } else if (id == mView.findViewById(R.id.fleetview_cn_change).getId()) {
                            changeInternalSeekCn();
                            fleetCnChangeBtn.setText(getSeekType());
                            processDeckInfo(selected, isCombinedFlag(selected));
                        } else if (id == mView.findViewById(R.id.viewbutton_quest).getId()) {
                            qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                            qintent.setAction(SHOW_QUESTVIEW_ACTION_NEW);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_akashi).getId()) {
                            qintent = new Intent(getBaseContext(), KcaAkashiViewService.class);
                            qintent.setAction(SHOW_AKASHIVIEW_ACTION);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_develop).getId()) {
                            if (isGameDataLoaded()) {
                                qintent = new Intent(getBaseContext(), KcaDevelopPopupService.class);
                                startService(qintent);
                            }
                        } else if (id == mView.findViewById(R.id.viewbutton_construction).getId()) {
                            if (isGameDataLoaded()) {
                                qintent = new Intent(getBaseContext(), KcaConstructPopupService.class);
                                qintent.setAction(KcaConstructPopupService.CONSTR_DATA_ACTION);
                                startService(qintent);
                            }
                        } else if (id == mView.findViewById(R.id.viewbutton_maphp).getId()) {
                            qintent = new Intent(getBaseContext(), KcaMapHpPopupService.class);
                            qintent.setAction(KcaMapHpPopupService.MAPHP_SHOW_ACTION);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_excheck).getId()) {
                            qintent = new Intent(getBaseContext(), KcaExpeditionCheckViewService.class);
                            qintent.setAction(KcaExpeditionCheckViewService.SHOW_EXCHECKVIEW_ACTION.concat("/").concat(String.valueOf(selected)));
                            startService(qintent);
                        } else {
                            for (int i = 0; i < 5; i++) {
                                if (id == mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).getId()) {
                                    selected = i;
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

    private View.OnTouchListener mPopupTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    };

    private boolean isCombinedFlag(int idx) {
        return idx == FLEET_COMBINED_ID;
    }

    private void processDeckInfo(int idx, boolean isCombined) {
        JsonArray data = helper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (!isReady) {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            fleetInfoLine.setText(getStringWithLocale(R.string.kca_init_content));
            mView.findViewById(R.id.fleet_list_main).setVisibility(View.INVISIBLE);
            mView.findViewById(R.id.fleet_list_combined).setVisibility(View.INVISIBLE);
            return;
        }

        if (idx != FLEET_COMBINED_ID && idx >= data.size()) {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            fleetInfoLine.setText("Not Opened");
            return;
        }

        mView.findViewById(R.id.fleet_list_main).setVisibility(View.VISIBLE);
        if (idx == FLEET_COMBINED_ID) {
            mView.findViewById(R.id.fleet_list_combined).setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.fleet_list_combined).setVisibility(View.INVISIBLE);
        }

        int cn = seekcn_internal;
        List<String> infoList = new ArrayList<>();

        String airPowerValue = "";
        if (isCombined) {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(data, 0, KcaBattle.getEscapeFlag());
        } else {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(data, idx, null);
        }
        if (airPowerValue.length() > 0) {
            infoList.add(airPowerValue);
        }

        double seekValue = 0;
        String seekStringValue = "";
        if (isCombined) {
            seekValue = deckInfoCalc.getSeekValue(data, "0,1", cn, KcaBattle.getEscapeFlag());
        } else {
            seekValue = deckInfoCalc.getSeekValue(data, String.valueOf(idx), cn, null);
        }
        if (cn == SEEK_PURE) {
            seekStringValue = KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_d), (int) seekValue);
        } else {
            seekStringValue = KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_f), seekValue);
        }
        infoList.add(seekStringValue);

        String speedStringValue = "";
        if (isCombined) {
            speedStringValue = deckInfoCalc.getSpeedString(data, "0,1", KcaBattle.getEscapeFlag());
        } else {
            speedStringValue = deckInfoCalc.getSpeedString(data, String.valueOf(idx), null);
        }
        infoList.add(speedStringValue);

        String tpValue = "";
        if (isCombined) {
            tpValue = deckInfoCalc.getTPString(data, "0,1", KcaBattle.getEscapeFlag());
        } else {
            tpValue = deckInfoCalc.getTPString(data, String.valueOf(idx), null);
        }
        infoList.add(tpValue);

        int sum_level = 0;
        if (isCombined) {
            for (int n = 0; n < 2; n++) {
                JsonArray maindata = deckInfoCalc.getDeckListInfo(data, n);
                for (int i = 0; i < 6; i++) {
                    int v = n * 6 + i + 1;
                    if (i >= maindata.size()) {
                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class)).setVisibility(View.INVISIBLE);
                    } else {
                        JsonObject userData = maindata.get(i).getAsJsonObject().getAsJsonObject("user");
                        JsonObject kcData = maindata.get(i).getAsJsonObject().getAsJsonObject("kc");
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_name", v), R.id.class)))
                                .setText(getShipTranslation(kcData.get("name").getAsString(), false));
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_stype", v), R.id.class)))
                                .setText(getShipTypeAbbr(kcData.get("stype").getAsInt()));
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_lv", v), R.id.class)))
                                .setText(makeLvString(userData.get("lv").getAsInt()));
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                .setText(makeHpString(userData.get("nowhp").getAsInt(), userData.get("maxhp").getAsInt()));
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_cond", v), R.id.class)))
                                .setText(userData.get("cond").getAsString());
                        int condition = userData.get("cond").getAsInt();
                        if (condition > 49) {
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipKira));
                        } else if (condition / 10 >= 3) {
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                        } else if (condition / 10 == 2) {
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue1));
                        } else {
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue2));
                        }

                        if (userData.get("nowhp").getAsInt() * 4 <= userData.get("maxhp").getAsInt()) {
                            mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                    .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetWarning));
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHeavyDmgState));
                        } else if (userData.get("nowhp").getAsInt() * 2 <= userData.get("maxhp").getAsInt()) {
                            mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                    .setBackgroundColor(Color.TRANSPARENT);
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorModerateDmgState));
                        } else if (userData.get("nowhp").getAsInt() * 4 <= userData.get("maxhp").getAsInt() * 3) {
                            mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                    .setBackgroundColor(Color.TRANSPARENT);
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLightDmgState));
                        } else if (userData.get("nowhp").getAsInt() != userData.get("maxhp").getAsInt()) {
                            mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                    .setBackgroundColor(Color.TRANSPARENT);
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNormalState));
                        } else {
                            mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                    .setBackgroundColor(Color.TRANSPARENT);
                            ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFullState));
                        }


                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class)).setVisibility(View.VISIBLE);

                        sum_level += userData.get("lv").getAsInt();
                    }
                }
            }

        } else {
            JsonArray maindata = deckInfoCalc.getDeckListInfo(data, idx);
            for (int i = 0; i < 6; i++) {
                int v = i + 1;
                if (i >= maindata.size()) {
                    mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class)).setVisibility(View.INVISIBLE);
                } else {
                    JsonObject userData = maindata.get(i).getAsJsonObject().getAsJsonObject("user");
                    JsonObject kcData = maindata.get(i).getAsJsonObject().getAsJsonObject("kc");
                    ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_name", v), R.id.class)))
                            .setText(getShipTranslation(kcData.get("name").getAsString(), false));
                    ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_stype", v), R.id.class)))
                            .setText(getShipTypeAbbr(kcData.get("stype").getAsInt()));
                    ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_lv", v), R.id.class)))
                            .setText(makeLvString(userData.get("lv").getAsInt()));
                    ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                            .setText(makeHpString(userData.get("nowhp").getAsInt(), userData.get("maxhp").getAsInt()));
                    ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_cond", v), R.id.class)))
                            .setText(userData.get("cond").getAsString());
                    int condition = userData.get("cond").getAsInt();
                    if (condition > 49) {
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipKira));
                    } else if (condition / 10 >= 3) {
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                    } else if (condition / 10 == 2) {
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue1));
                    } else {
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_condmark", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue2));
                    }

                    if (userData.get("nowhp").getAsInt() * 4 <= userData.get("maxhp").getAsInt()) {
                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetWarning));
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHeavyDmgState));
                    } else if (userData.get("nowhp").getAsInt() * 2 <= userData.get("maxhp").getAsInt()) {
                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                .setBackgroundColor(Color.TRANSPARENT);
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorModerateDmgState));
                    } else if (userData.get("nowhp").getAsInt() * 4 <= userData.get("maxhp").getAsInt() * 3) {
                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                .setBackgroundColor(Color.TRANSPARENT);
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLightDmgState));
                    } else if (userData.get("nowhp").getAsInt() != userData.get("maxhp").getAsInt()) {
                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                .setBackgroundColor(Color.TRANSPARENT);
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNormalState));
                    } else {
                        mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class))
                                .setBackgroundColor(Color.TRANSPARENT);
                        ((TextView) mView.findViewById(getId(KcaUtils.format("fleetview_item_%d_hp", v), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFullState));
                    }

                    mView.findViewById(getId(KcaUtils.format("fleetview_item_%d", v), R.id.class)).setVisibility(View.VISIBLE);

                    sum_level += userData.get("lv").getAsInt();
                }
            }
        }

        infoList.add("LV ".concat(String.valueOf(sum_level)));
        fleetInfoLine.setText(joinStr(infoList, " / "));
        if (selected < 4 && KcaExpedition2.isInExpedition(selected)) {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
        } else {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNormal));
        }

    }

    public void setItemViewLayout(JsonObject data) {
        Log.e("KCA", data.toString());
        JsonArray slot = data.getAsJsonArray("api_slot");
        JsonArray onslot = null;
        JsonArray maxslot = null;
        if (data.has("api_onslot")) {
            onslot = data.getAsJsonArray("api_onslot");
            maxslot = data.getAsJsonArray("api_maxslot");
        }
        int slot_count = 0;
        int onslot_count = 0;
        int slot_ex = 0;
        if (data.has("api_slot_ex")) {
            slot_ex = data.get("api_slot_ex").getAsInt();
        }
        for (int i = 0; i < slot.size(); i++) {
            int item_id = slot.get(i).getAsInt();
            if (item_id == -1) {
                itemView.findViewById(getId("item".concat(String.valueOf(i + 1)), R.id.class)).setVisibility(View.GONE);
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            } else {
                slot_count += 1;
                JsonObject kcItemData;
                int lv = 0;
                int alv = -1;
                if (onslot != null) {
                    Log.e("KCA", "item_id: " + String.valueOf(item_id));
                    kcItemData = getUserItemStatusById(item_id, "level,alv", "type,name");
                    Log.e("KCA", kcItemData.toString());
                    lv = kcItemData.get("level").getAsInt();
                    if (kcItemData.has("alv")) {
                        alv = kcItemData.get("alv").getAsInt();
                    }

                    if (lv > 0) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)))
                                .setText(getStringWithLocale(R.string.lv_star).concat(String.valueOf(lv)));
                        itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                    }

                    if (alv > 0) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                                .setText(getStringWithLocale(getId(KcaUtils.format("alv_%d", alv), R.string.class)));
                        int alvColorId = (alv <= 3) ? 1 : 2;
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), getId(KcaUtils.format("itemalv%d", alvColorId), R.color.class)));
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                    }

                    int itemtype = kcItemData.getAsJsonArray("type").get(2).getAsInt();
                    if (isItemAircraft(itemtype)) {
                        onslot_count += 1;
                        Log.e("KCA", "ID: " + String.valueOf(itemtype));
                        int nowSlotValue = onslot.get(i).getAsInt();
                        int maxSlotValue = maxslot.get(i).getAsInt();
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)))
                                .setText(KcaUtils.format("[%02d/%02d]", nowSlotValue, maxSlotValue));
                        itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                    }
                } else {
                    kcItemData = getKcItemStatusById(item_id, "type,name");
                    itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                    itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                    itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
                }

                String kcItemName = getItemTranslation(kcItemData.get("name").getAsString());
                int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();

                int typeres = 0;
                try {
                    typeres = getId(KcaUtils.format("item_%d", type), R.mipmap.class);
                } catch (Exception e) {
                    typeres = R.mipmap.item_0;
                }
                ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class))).setText(kcItemName);
                ((ImageView) itemView.findViewById(getId(KcaUtils.format("item%d_icon", i + 1), R.id.class))).setImageResource(typeres);
                itemView.findViewById(getId("item".concat(String.valueOf(i + 1)), R.id.class)).setVisibility(View.VISIBLE);
            }
        }

        if (onslot_count == 0) {
            for (int i = 0; i < slot.size(); i++) {
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            }
        }

        if (slot_ex > 0) {
            // EX_SLOT
            slot_count += 1;
            JsonObject kcItemData = getUserItemStatusById(slot_ex, "level", "type,name");
            String kcItemName = getItemTranslation(kcItemData.get("name").getAsString());
            int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();
            int lv = kcItemData.get("level").getAsInt();
            int typeres = 0;
            try {
                typeres = getId(KcaUtils.format("item_%d", type), R.mipmap.class);
            } catch (Exception e) {
                typeres = R.mipmap.item_0;
            }
            ((TextView) itemView.findViewById(R.id.item_ex_name)).setText(kcItemName);
            ((ImageView) itemView.findViewById(R.id.item_ex_icon)).setImageResource(typeres);
            if (lv > 0) {
                ((TextView) itemView.findViewById(R.id.item_ex_level))
                        .setText(getStringWithLocale(R.string.lv_star).concat(String.valueOf(lv)));
                itemView.findViewById(R.id.item_ex_level).setVisibility(View.VISIBLE);
            } else {
                itemView.findViewById(R.id.item_ex_level).setVisibility(View.GONE);
            }
            itemView.findViewById(R.id.view_slot_ex).setVisibility(View.VISIBLE);
        } else {
            itemView.findViewById(R.id.view_slot_ex).setVisibility(View.GONE);
        }

        if (slot_count == 0) {
            ((TextView) itemView.findViewById(R.id.item1_name)).setText(getStringWithLocale(R.string.slot_empty));
            ((ImageView) itemView.findViewById(R.id.item1_icon)).setImageResource(R.mipmap.item_0);
            itemView.findViewById(R.id.item1_level).setVisibility(View.GONE);
            itemView.findViewById(R.id.item1_alv).setVisibility(View.GONE);
            itemView.findViewById(R.id.item1_slot).setVisibility(View.GONE);
            itemView.findViewById(R.id.item1).setVisibility(View.VISIBLE);
        }
        itemView.setVisibility(View.VISIBLE);
    }

    private int getSeekCn() {
        return Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    private String getSeekType() {
        String seekType = "";
        switch (seekcn_internal) {
            case 1:
                seekType = getStringWithLocale(R.string.seek_type_1);
                break;
            case 3:
                seekType = getStringWithLocale(R.string.seek_type_3);
                break;
            case 4:
                seekType = getStringWithLocale(R.string.seek_type_4);
                break;
            default:
                seekType = getStringWithLocale(R.string.seek_type_0);
                break;
        }
        return seekType;
    }

    private void changeInternalSeekCn() {
        int new_value = seekcn_internal;
        switch (seekcn_internal) {
            case 0:
                new_value = 1;
                break;
            case 1:
                new_value = 3;
                break;
            case 3:
                new_value = 4;
                break;
            case 4:
                new_value = 0;
                break;
            default:
                break;
        }
        seekcn_internal = new_value;
    }

    private void updateSelectedView(int idx) {
        for (int i = 0; i < 5; i++) {
            int view_id = getId("fleet_".concat(String.valueOf(i + 1)), R.id.class);
            if (idx == i) {
                mView.findViewById(view_id).setBackgroundColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            } else {
                if (i < 4 && KcaExpedition2.isInExpedition(i)) {
                    mView.findViewById(view_id).setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpeditionBtn));
                } else {
                    mView.findViewById(view_id).setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                }
            }
        }
    }

    private void sendReport(Exception e, int type) {
        error_flag = true;
        String data_str;
        JsonArray data = helper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (data == null) {
            data_str = "data is null";
        } else {
            data_str = data.toString();
        }
        if (mView != null) mView.setVisibility(View.GONE);

        KcaDBHelper helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(ERROR_TYPE_FLEETVIEW, "fleetview", "FV_".concat(String.valueOf(type)), data_str, getStringFromException(e));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean is_landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        float[] exp_score = helper.getExpScore();

        if (is_landscape) {
            fleetExpView.setText(KcaUtils.format(
                    getStringWithLocale(R.string.fleetview_expview),
                    exp_score[0], exp_score[1]));
            fleetInfoTitle.setVisibility(View.VISIBLE);
        } else {
            fleetExpView.setText(makeSimpleExpString(exp_score[0], exp_score[1]));
            fleetInfoTitle.setVisibility(View.GONE);
        }
        super.onConfigurationChanged(newConfig);
    }
}
