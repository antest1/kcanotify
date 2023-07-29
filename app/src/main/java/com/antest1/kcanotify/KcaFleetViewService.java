package com.antest1.kcanotify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static com.antest1.kcanotify.KcaAkashiViewService.SHOW_AKASHIVIEW_ACTION;
import static com.antest1.kcanotify.KcaApiData.getSlotItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getUseItemNameById;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.isItemAircraft;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_USEITEMS;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_FLEETVIEW;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_FIX_VIEW_LOC;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_1;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_2;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_3;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_ITEM_4;
import static com.antest1.kcanotify.KcaConstants.PREF_FV_MENU_ORDER;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_SEEK_CN;
import static com.antest1.kcanotify.KcaConstants.PREF_VIEW_YLOC;
import static com.antest1.kcanotify.KcaConstants.SEEK_PURE;
import static com.antest1.kcanotify.KcaQuestViewService.SHOW_QUESTVIEW_ACTION_NEW;
import static com.antest1.kcanotify.KcaUseStatConstant.CLOSE_FLEETVIEW;
import static com.antest1.kcanotify.KcaUseStatConstant.FV_BTN_PRESS;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_FLEETVIEW;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static org.apache.commons.lang3.StringUtils.split;

public class KcaFleetViewService extends Service {
    public static final String SHOW_FLEETVIEW_ACTION = "show_fleetview_action";
    public static final String REFRESH_FLEETVIEW_ACTION = "update_fleetview_action";
    public static final String CLOSE_FLEETVIEW_ACTION = "close_fleetview_action";
    public static final String[] fleetview_menu_keys = {"quest", "excheck", "develop", "construction", "docking", "maphp", "fchk", "labinfo", "akashi"};
    public static final String DECKINFO_REQ_LIST = "id,ship_id,lv,exp,slot,slot_ex,onslot,cond,maxhp,nowhp,sally_area";
    public static final String KC_DECKINFO_REQ_LIST = "name,maxeq,stype";

    public static final int FLEET_COMBINED_ID = 4;
    final int fleetview_menu_margin = 40;

    private static final int HQINFO_TOTAL = 6;
    private static final int HQINFO_EXPVIEW = 0;
    private static final int HQINFO_SECOUNT = 1;
    private static final int HQINFO_ITEMCNT1 = 2;
    private static final int HQINFO_ITEMCNT2 = 3;
    private static final int HQINFO_ITEMCNT3 = 4;
    private static final int HQINFO_ITEMCNT4 = 5;


    Context contextWithLocale;
    LayoutInflater mInflater;
    SharedPreferences prefs;
    public KcaDBHelper dbHelper;
    public KcaDeckInfo deckInfoCalc;
    int seekcn_internal = -1;
    int switch_status = 1;
    Handler mHandler;
    Runnable timer;
    String fleetCalcInfoText = "";
    boolean isAkashiTimerActive = false;
    ScheduledExecutorService timeScheduler = null;

    static int view_status = 0;
    static boolean error_flag = false;
    boolean active;
    private View mView, itemView, fleetHqInfoView;
    private TextView fleetInfoTitle, fleetInfoLine, fleetCnChangeBtn, fleetSwitchBtn, fleetAkashiTimerBtn;
    private WindowManager mManager;
    private ScrollView fleetMenu, fleetShipArea;
    private ImageView fleetMenuArrowUp, fleetMenuArrowDown;
    private static boolean isReady;
    private static int[] hqinfoItems = {-1, -1, -1, -1};
    private static int hqinfoState = 0;
    private JsonObject gunfitData;

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

    public int setView() {
        try {
            Log.e("KCA-FV", String.valueOf(selected));
            setHqInfo();
            fleetInfoTitle.setVisibility(View.VISIBLE);
            updateSelectedView(selected);
            processDeckInfo(selected, isCombinedFlag(selected));
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            sendReport(e, 0);
            return 1;
        }
    }

    private void setNextState() {
        hqinfoState = (hqinfoState + 1) % HQINFO_TOTAL;
        if (hqinfoState>=HQINFO_ITEMCNT1 && hqinfoItems[hqinfoState-HQINFO_ITEMCNT1] == -1) setNextState();
    }

    private void setHqInfoViewVisibility() {
        if (hqinfoState == HQINFO_EXPVIEW) {
            fleetHqInfoView.findViewById(R.id.fleetview_exp).setVisibility(View.VISIBLE);
            fleetHqInfoView.findViewById(R.id.fleetview_cnt).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_item_cnt).setVisibility(View.GONE);
        } else if (hqinfoState == HQINFO_SECOUNT) {
            fleetHqInfoView.findViewById(R.id.fleetview_exp).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_cnt).setVisibility(View.VISIBLE);
            fleetHqInfoView.findViewById(R.id.fleetview_item_cnt).setVisibility(View.GONE);
        } else {
            fleetHqInfoView.findViewById(R.id.fleetview_exp).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_cnt).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_item_cnt).setVisibility(View.VISIBLE);
        }
    }

    private void setHqInfo() {
        String[] pref_items = {PREF_FV_ITEM_1, PREF_FV_ITEM_2, PREF_FV_ITEM_3, PREF_FV_ITEM_4};
        for (int i = 0; i < pref_items.length; i++) {
            hqinfoItems[i] = Integer.parseInt(getStringPreferences(getApplicationContext(), pref_items[i]));
        }
        setHqInfoViewVisibility();
        switch (hqinfoState) {
            case HQINFO_EXPVIEW:
                TextView expview = fleetHqInfoView.findViewById(R.id.fleetview_exp);
                float[] exp_score = dbHelper.getExpScore();
                expview.setText(KcaUtils.format(
                        getStringWithLocale(R.string.fleetview_expview),
                        exp_score[0], exp_score[1]));
                break;
            case HQINFO_SECOUNT:
                TextView shipcntview = fleetHqInfoView.findViewById(R.id.fleetview_cnt1);
                TextView equipcntview = fleetHqInfoView.findViewById(R.id.fleetview_cnt2);
                ImageView shipcntviewicon = fleetHqInfoView.findViewById(R.id.fleetview_cnt1_icon);
                ImageView equipcntviewicon = fleetHqInfoView.findViewById(R.id.fleetview_cnt2_icon);

                shipcntview.setText(KcaUtils.format("%d/%d", KcaApiData.getShipSize(), KcaApiData.getUserMaxShipCount()));
                if (KcaApiData.checkEventUserShip()){
                    shipcntview.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHqCheckEventCondFailed));
                    shipcntviewicon.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.colorHqCheckEventCondFailed), PorterDuff.Mode.MULTIPLY);
                } else {
                    shipcntview.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    shipcntviewicon.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.white), PorterDuff.Mode.MULTIPLY);
                }

                equipcntview.setText(KcaUtils.format("%d/%d", KcaApiData.getItemSize(), KcaApiData.getUserMaxItemCount()));
                if (KcaApiData.checkEventUserItem()){
                    equipcntview.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHqCheckEventCondFailed));
                    equipcntviewicon.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.colorHqCheckEventCondFailed), PorterDuff.Mode.MULTIPLY);
                } else {
                    equipcntview.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    equipcntviewicon.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.white), PorterDuff.Mode.MULTIPLY);
                }
                break;
            case HQINFO_ITEMCNT1:
            case HQINFO_ITEMCNT2:
            case HQINFO_ITEMCNT3:
            case HQINFO_ITEMCNT4:
                int item_id = hqinfoItems[hqinfoState-2];
                int item_count = 0;
                String item_name = getUseItemNameById(item_id);

                ImageView item_icon = fleetHqInfoView.findViewById(R.id.fleetview_item_cnt_icon);
                if (item_id == 68 || item_id == 93) {
                    item_icon.setImageResource(R.mipmap.ic_saury);
                } else {
                    item_icon.setImageResource(R.mipmap.ic_gift);
                }
                item_icon.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                        KcaUtils.getId("colorFleetViewItem" + (hqinfoState-1), R.color.class)), PorterDuff.Mode.SRC_ATOP);

                TextView itemcntview = fleetHqInfoView.findViewById(R.id.fleetview_item_cnt_value);
                itemcntview.setTextColor(ContextCompat.getColor(getApplicationContext(),
                        KcaUtils.getId("colorFleetViewItem" + (hqinfoState-1), R.color.class)));
                itemcntview.setText(KcaUtils.format("[%s] %02d", item_name, item_count));

                JsonArray useitem_data = dbHelper.getJsonArrayValue(DB_KEY_USEITEMS);
                if (useitem_data != null) {
                    for (int i = 0; i < useitem_data.size(); i++) {
                        JsonObject item = useitem_data.get(i).getAsJsonObject();
                        if (item.get("api_id").getAsInt() == item_id) {
                            item_count = item.get("api_count").getAsInt();
                        }
                    }
                    itemcntview.setText(KcaUtils.format("[%s] %02d", item_name, item_count));
                }
                break;
            default:
                break;
        }
    }

    void runTimer() {
        timeScheduler = Executors.newSingleThreadScheduledExecutor();
        timeScheduler.scheduleAtFixedRate(timer, 0, 1, TimeUnit.SECONDS);
    }

    void stopTimer() {
        if (timeScheduler != null) {
            timeScheduler.shutdown();
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
            switch_status = 1;
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            view_status = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_VIEW_YLOC));
            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            dbHelper.updateExpScore(0);
            KcaApiData.setDBHelper(dbHelper);

            contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
            deckInfoCalc = new KcaDeckInfo(getApplicationContext(), contextWithLocale);
            gunfitData = loadGunfitData(getAssets());

            //mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInflater = LayoutInflater.from(contextWithLocale);
            initView();

            fleetInfoLine.setText(getStringWithLocale(R.string.kca_init_content));
            itemView = mInflater.inflate(R.layout.view_battleview_items, null);
            mHandler = new Handler();
            timer = new Runnable() {
                @Override
                public void run() {
                    updateFleetInfoLine();
                }
            };
            runTimer();

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
        stopTimer();
        if (mView != null) {
            if (mView.getParent() != null) mManager.removeViewImmediate(mView);
        }
        if (itemView != null) {
            if (itemView.getParent() != null) mManager.removeViewImmediate(itemView);
        }
        super.onDestroy();
    }

    private void setFleetMenu() {
        LinearLayout fleetMenuArea = mView.findViewById(R.id.viewbutton_area);
        List<TextView> menuBtnList = new ArrayList<>();
        for (int i = 0; i < fleetview_menu_keys.length; i++) {
            String key = fleetview_menu_keys[i];
            TextView tv = mView.findViewById(KcaUtils.getId(KcaUtils.format("viewbutton_%s", key), R.id.class));
            tv.setText(getStringWithLocale(KcaUtils.getId(KcaUtils.format("viewmenu_%s", key), R.string.class)));
            tv.setOnTouchListener(mViewTouchListener);
            menuBtnList.add(tv);
            ((ViewGroup) tv.getParent()).removeView(tv);
        }

        String order_data = getStringPreferences(getApplicationContext(), PREF_FV_MENU_ORDER);
        if (order_data.length() > 0) {
            JsonArray order = JsonParser.parseString(order_data).getAsJsonArray();
            for (int i = 0; i < order.size(); i++) {
                fleetMenuArea.addView(menuBtnList.get(order.get(i).getAsInt()));
            }
        } else {
            for (TextView tv: menuBtnList) {
                fleetMenuArea.addView(tv);
            }
        }
    }

    private void initView() {
        mView = mInflater.inflate(R.layout.view_fleet_list, null);
        mView.setVisibility(GONE);
        KcaUtils.resizeFullWidthView(getApplicationContext(), mView);
        mView.findViewById(R.id.fleetview_shiparea).setOnTouchListener(mViewTouchListener);
        mView.findViewById(R.id.fleetview_tool).setOnTouchListener(mViewTouchListener);
        mView.findViewById(R.id.fleetview_head).setOnTouchListener(mViewTouchListener);
        mView.findViewById(R.id.fleetview_cn_change).setOnTouchListener(mViewTouchListener);
        mView.findViewById(R.id.fleetview_fleetswitch).setOnTouchListener(mViewTouchListener);
        mView.findViewById(R.id.fleetview_hqinfo).setOnTouchListener(mViewTouchListener);

        for (int i = 0; i < 5; i++) {
            mView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
        }
        for (int i = 0; i < 12; i++) {
//            mView.findViewById(getId("fleetview_item_".concat(String.valueOf(i + 1)), R.id.class)).setOnTouchListener(mViewTouchListener);
            getFleetViewItem(i).setOnTouchListener(mViewTouchListener);
        }

        setFleetMenu();
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = KcaUtils.getGravity(view_status);
        setPreferences(getApplicationContext(), PREF_VIEW_YLOC, view_status);

        fleetInfoLine = mView.findViewById(R.id.fleetview_infoline);
        fleetInfoLine.setOnTouchListener(mViewTouchListener);

        fleetInfoTitle = mView.findViewById(R.id.fleetview_title);
        fleetHqInfoView = mView.findViewById(R.id.fleetview_hqinfo);
        fleetCnChangeBtn = mView.findViewById(R.id.fleetview_cn_change);
        fleetAkashiTimerBtn = mView.findViewById(R.id.fleetview_akashi_timer);
        fleetSwitchBtn = mView.findViewById(R.id.fleetview_fleetswitch);
        fleetSwitchBtn.setVisibility(View.GONE);
        fleetShipArea = mView.findViewById(R.id.fleetview_shiparea);
        fleetMenu = mView.findViewById(R.id.fleetview_menu);
        fleetMenuArrowUp = mView.findViewById(R.id.fleetview_menu_up);
        fleetMenuArrowDown = mView.findViewById(R.id.fleetview_menu_down);
        fleetMenu.setOnTouchListener((view, motionEvent) -> {
            ViewTreeObserver observer = fleetMenu.getViewTreeObserver();
            observer.addOnScrollChangedListener(fleetMenuScrollChangeListener);
            return false;
        });
    }

    final ViewTreeObserver.OnScrollChangedListener fleetMenuScrollChangeListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            if (fleetMenu != null && fleetMenuArrowUp != null && fleetMenuArrowDown != null) {
                if (fleetMenu.getScrollY() < fleetview_menu_margin) {
                    fleetMenuArrowUp.setVisibility(View.GONE);
                    fleetMenuArrowDown.setAlpha(0.9f);
                } else if (fleetMenu.getChildAt(0).getBottom() - (fleetMenu.getHeight() + fleetMenu.getScrollY()) < fleetview_menu_margin) {
                    fleetMenuArrowDown.setVisibility(View.GONE);
                    fleetMenuArrowUp.setAlpha(0.9f);
                } else {
                    fleetMenuArrowUp.setVisibility(View.VISIBLE);
                    fleetMenuArrowDown.setVisibility(View.VISIBLE);
                    fleetMenuArrowUp.setAlpha(0.6f);
                    fleetMenuArrowDown.setAlpha(0.6f);
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(SHOW_FLEETVIEW_ACTION)) {
                if (mView != null && mView.getVisibility() != View.VISIBLE) {
                    if (seekcn_internal == -1) seekcn_internal = getSeekCn();
                    fleetCnChangeBtn.setText(getSeekType());
                    int setViewResult = setView();
                    if (setViewResult == 0) {
                        mView.setVisibility(View.VISIBLE);
                        if (mView.getParent() != null) {
                            mManager.removeViewImmediate(mView);
                        }
                        mManager.addView(mView, mParams);
                    }
                    sendUserAnalytics(getApplicationContext(), OPEN_FLEETVIEW, null);
                }
            }
            if (intent.getAction().equals(REFRESH_FLEETVIEW_ACTION)) {
                int setViewResult = setView();
                if (setViewResult == 0) {
                    if (mView != null && mView.getParent() != null) {
                        mView.invalidate();
                        mManager.updateViewLayout(mView, mParams);
                    }
                }
            }
            if (intent.getAction().equals(CLOSE_FLEETVIEW_ACTION)) {
                if (mView != null) {
                    mView.setVisibility(GONE);
                    if (mView.getParent() != null) {
                        mManager.removeViewImmediate(mView);
                    }
                    itemView.setVisibility(GONE);
                    if (itemView.getParent() != null) {
                        mManager.removeViewImmediate(itemView);
                    }
                    JsonObject statProperties = new JsonObject();
                    statProperties.addProperty("manual", false);
                    sendUserAnalytics(getApplicationContext(), CLOSE_FLEETVIEW, statProperties);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;
        private float mBeforeY, mAfterY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean view_fix = getBooleanPreferences(getApplicationContext(), PREF_FIX_VIEW_LOC);
            WindowManager.LayoutParams itemViewParams;
            int xMargin = (int) getResources().getDimension(R.dimen.item_popup_xmargin);

            Intent qintent;
            int id = v.getId();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e("KCA-FV", "ACTION_DOWN");
                    mBeforeY = event.getRawY();
                    mAfterY = event.getRawY();
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    if (id == fleetInfoLine.getId()) {
                        fleetInfoLine.setSelected(true);
                    }
                    for (int i = 0; i < 12; i++) {
//                        String item_id = "fleetview_item_".concat(String.valueOf(i + 1));
//                        if (id == mView.findViewById(getId(item_id, R.id.class)).getId()) {
                        if (id == getFleetViewItem(i).getId()) {
                            JsonArray data;
                            JsonObject udata, kcdata;

                            if (isCombinedFlag(selected)) {
                                if (i < 6) {
                                    data = deckInfoCalc.getDeckListInfo(dbHelper.getJsonArrayValue(DB_KEY_DECKPORT), 0, DECKINFO_REQ_LIST, KC_DECKINFO_REQ_LIST);
                                } else {
                                    data = deckInfoCalc.getDeckListInfo(dbHelper.getJsonArrayValue(DB_KEY_DECKPORT), 1, DECKINFO_REQ_LIST, KC_DECKINFO_REQ_LIST);
                                }
                                udata = data.get(i % 6).getAsJsonObject().getAsJsonObject("user");
                                kcdata = data.get(i % 6).getAsJsonObject().getAsJsonObject("kc");
                            } else {
                                data = deckInfoCalc.getDeckListInfo(dbHelper.getJsonArrayValue(DB_KEY_DECKPORT), selected, DECKINFO_REQ_LIST, KC_DECKINFO_REQ_LIST);
                                udata = data.get(i).getAsJsonObject().getAsJsonObject("user");
                                kcdata = data.get(i).getAsJsonObject().getAsJsonObject("kc");
                            }

                            String ship_id = udata.get("ship_id").getAsString();
                            int ship_married = udata.get("lv").getAsInt() >= 100 ? 1 : 0;
                            JsonObject itemdata = new JsonObject();
                            itemdata.add("api_slot", udata.get("slot"));
                            itemdata.add("api_slot_ex", udata.get("slot_ex"));
                            itemdata.add("api_onslot", udata.get("onslot"));
                            itemdata.add("api_maxslot", kcdata.get("maxeq"));

                            setItemViewLayout(itemdata, ship_id, ship_married);
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
//                        String item_id = "fleetview_item_".concat(String.valueOf(i + 1));
//                        if (id == mView.findViewById(getId(item_id, R.id.class)).getId()) {
                        if (id == getFleetViewItem(i).getId()) {
                            itemView.setVisibility(GONE);
                            break;
                        }
                    }

                    int y_direction = (int) (mAfterY - mBeforeY);
                    if (!view_fix && Math.abs(y_direction) > 400) {
                        int status_change = y_direction > 0 ? 1 : -1;
                        view_status = Math.min(Math.max(view_status + status_change, -1), 1);
                        mParams.gravity = KcaUtils.getGravity(view_status);
                        mManager.updateViewLayout(mView, mParams);
                        setPreferences(getApplicationContext(), PREF_VIEW_YLOC, view_status);
                    }

                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == mView.findViewById(R.id.fleetview_head).getId()) {
                            JsonObject statProperties = new JsonObject();
                            statProperties.addProperty("manual", true);
                            if (mView != null) mView.setVisibility(GONE);
                            if (itemView != null) itemView.setVisibility(GONE);
                        } else if (id == mView.findViewById(R.id.fleetview_tool).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Tools"), null);
                            if (mView != null) mView.setVisibility(GONE);
                            if (itemView != null) itemView.setVisibility(GONE);
                            Intent toolIntent = new Intent(KcaFleetViewService.this, ToolsActivity.class);
                            toolIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(toolIntent);
                        } else if (id == mView.findViewById(R.id.fleetview_cn_change).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("CnChange"), null);
                            changeInternalSeekCn();
                            fleetCnChangeBtn.setText(getSeekType());
                            processDeckInfo(selected, isCombinedFlag(selected));
                        } else if (id == mView.findViewById(R.id.fleetview_fleetswitch).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("FleetChange"), null);
                            if (switch_status == 1) {
                                switch_status = 2;
                                mView.findViewById(R.id.fleet_list_main).setVisibility(View.GONE);
                                mView.findViewById(R.id.fleet_list_combined).setVisibility(View.VISIBLE);
                                ((TextView) mView.findViewById(R.id.fleetview_fleetswitch)).setText(getStringWithLocale(R.string.fleetview_switch_2));
                            } else {
                                switch_status = 1;
                                mView.findViewById(R.id.fleet_list_main).setVisibility(View.VISIBLE);
                                mView.findViewById(R.id.fleet_list_combined).setVisibility(View.GONE);
                                ((TextView) mView.findViewById(R.id.fleetview_fleetswitch)).setText(getStringWithLocale(R.string.fleetview_switch_1));
                            }
                        } else if (id == mView.findViewById(R.id.fleetview_hqinfo).getId()) {
                            setNextState();
                            JsonObject statProperties = new JsonObject();
                            statProperties.addProperty("state", hqinfoState);
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("HqInfo"), statProperties);
                            setHqInfo();
                        } else if (id == mView.findViewById(R.id.viewbutton_quest).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Quest"), null);
                            qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                            qintent.setAction(SHOW_QUESTVIEW_ACTION_NEW);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_akashi).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Akashi"), null);
                            qintent = new Intent(getBaseContext(), KcaAkashiViewService.class);
                            qintent.setAction(SHOW_AKASHIVIEW_ACTION);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_develop).getId()) {
                            if (isGameDataLoaded()) {
                                sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Develop"), null);
                                qintent = new Intent(getBaseContext(), KcaDevelopPopupService.class);
                                startService(qintent);
                            }
                        } else if (id == mView.findViewById(R.id.viewbutton_construction).getId()) {
                            if (isGameDataLoaded()) {
                                sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Constr"), null);
                                qintent = new Intent(getBaseContext(), KcaConstructPopupService.class);
                                qintent.setAction(KcaConstructPopupService.CONSTR_DATA_ACTION);
                                startService(qintent);
                            }
                        } else if (id == mView.findViewById(R.id.viewbutton_docking).getId()) {
                            if (isGameDataLoaded()) {
                                sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Docking"), null);
                                qintent = new Intent(getBaseContext(), KcaDockingPopupService.class);
                                qintent.setAction(KcaDockingPopupService.DOCKING_DATA_ACTION);
                                startService(qintent);
                            }
                        } else if (id == mView.findViewById(R.id.viewbutton_maphp).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("MapHP"), null);
                            qintent = new Intent(getBaseContext(), KcaMapHpPopupService.class);
                            qintent.setAction(KcaMapHpPopupService.MAPHP_SHOW_ACTION);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_fchk).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("FleetCheck"), null);
                            qintent = new Intent(getBaseContext(), KcaFleetCheckPopupService.class);
                            qintent.setAction(KcaFleetCheckPopupService.FCHK_SHOW_ACTION);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_labinfo).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("LandAirBaseInfo"), null);
                            qintent = new Intent(getBaseContext(), KcaLandAirBasePopupService.class);
                            qintent.setAction(KcaLandAirBasePopupService.LAB_DATA_ACTION);
                            startService(qintent);
                        } else if (id == mView.findViewById(R.id.viewbutton_excheck).getId()) {
                            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("ExpeditionCheck"), null);
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
                case MotionEvent.ACTION_MOVE:
                    mAfterY = event.getRawY();
                    break;
            }
            if (id == mView.findViewById(R.id.fleetview_shiparea).getId()) return false;
            else return true;
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
        boolean is_combined = idx == FLEET_COMBINED_ID;
        boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        JsonArray deckportdata = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (!isReady) {
            fleetCalcInfoText = "";
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            fleetInfoLine.setText(getStringWithLocale(R.string.kca_init_content));
            mView.findViewById(R.id.fleet_list_main).setVisibility(INVISIBLE);
            mView.findViewById(R.id.fleet_list_combined).setVisibility(is_landscape? INVISIBLE : View.GONE);
            fleetSwitchBtn.setVisibility(View.GONE);
            return;
        }

        if (idx != FLEET_COMBINED_ID && idx >= deckportdata.size()) {
            fleetCalcInfoText = "Not Opened";
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            fleetInfoLine.setText(fleetCalcInfoText);
            mView.findViewById(R.id.fleet_list_main).setVisibility(INVISIBLE);
            mView.findViewById(R.id.fleet_list_combined).setVisibility(is_landscape? INVISIBLE : View.GONE);
            fleetSwitchBtn.setVisibility(View.GONE);
            return;
        }

        if(is_landscape) {
            fleetSwitchBtn.setVisibility(View.GONE);
            mView.findViewById(R.id.fleet_list_main).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.fleet_list_combined).setVisibility(is_combined ? View.VISIBLE : INVISIBLE);
        } else {
            boolean switch_is_one = switch_status == 1;
            fleetSwitchBtn.setVisibility(is_combined ? View.VISIBLE : View.GONE);
            mView.findViewById(R.id.fleet_list_main).setVisibility((!is_combined || switch_is_one) ? View.VISIBLE : View.GONE);
            mView.findViewById(R.id.fleet_list_combined).setVisibility((is_combined && !switch_is_one) ? View.VISIBLE : View.GONE);
        }

        int cn = seekcn_internal;
        List<String> infoList = new ArrayList<>();

        String airPowerValue = "";
        if (isCombined) {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(deckportdata, 0, KcaBattle.getEscapeFlag());
        } else {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(deckportdata, idx, null);
        }
        if (airPowerValue.length() > 0) {
            infoList.add(airPowerValue);
        }

        double seekValue = 0;
        String seekStringValue = "";
        if (isCombined) {
            seekValue = deckInfoCalc.getSeekValue(deckportdata, "0,1", cn, KcaBattle.getEscapeFlag());
        } else {
            seekValue = deckInfoCalc.getSeekValue(deckportdata, String.valueOf(idx), cn, null);
        }
        if (cn == SEEK_PURE) {
            seekStringValue = KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_d), (int) seekValue);
        } else {
            seekStringValue = KcaUtils.format(getStringWithLocale(R.string.fleetview_seekvalue_f), seekValue);
        }
        infoList.add(seekStringValue);

        String speedStringValue = "";
        if (isCombined) {
            speedStringValue = deckInfoCalc.getSpeedString(deckportdata, "0,1", KcaBattle.getEscapeFlag());
        } else {
            speedStringValue = deckInfoCalc.getSpeedString(deckportdata, String.valueOf(idx), null);
        }
        infoList.add(speedStringValue);

        String tpValue = "";
        if (isCombined) {
            tpValue = deckInfoCalc.getTPString(deckportdata, "0,1", KcaBattle.getEscapeFlag());
        } else {
            tpValue = deckInfoCalc.getTPString(deckportdata, String.valueOf(idx), null);
        }
        infoList.add(tpValue);

        for (int i = 0; i < 12; i++) {
            getFleetViewItem(i).setVisibility(INVISIBLE);
        }

        int sum_level = 0;
        if (isCombined) {

            sum_level += setFleetInfo(deckInfoCalc.getDeckList(deckportdata, 0), 0);
            sum_level += setFleetInfo(deckInfoCalc.getDeckList(deckportdata, 1), 1);
        } else {

            int[] ship_id_list = deckInfoCalc.getDeckList(deckportdata, idx);
            if (ship_id_list.length > 6) { // if need to show combined fleet (maybe 遊撃艦隊)
                mView.findViewById(R.id.fleet_list_combined).setVisibility(View.VISIBLE);
            }

            sum_level += setFleetInfo(ship_id_list, 0);
        }

        isAkashiTimerActive = deckInfoCalc.checkAkashiFlagship(deckportdata).size() > 0;

        infoList.add("LV ".concat(String.valueOf(sum_level)));
        fleetCalcInfoText = joinStr(infoList, " / ");
        long moraleCompleteTime;
        if (selected == FLEET_COMBINED_ID) {
            moraleCompleteTime = Math.max(KcaMoraleInfo.getMoraleCompleteTime(0),
                    KcaMoraleInfo.getMoraleCompleteTime(1));
        } else {
            moraleCompleteTime = KcaMoraleInfo.getMoraleCompleteTime(selected);
        }
        if (selected < 4 && KcaExpedition2.isInExpedition(selected)) {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoExpedition));
        } else if (moraleCompleteTime > 0) {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNotGoodStatus));
        } else {
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNormal));
        }
        updateFleetInfoLine(moraleCompleteTime);
    }

    /**
     *
     * @param list member ship id list
     * @param base_view_id 0 for 1st-4th fleet and 1st combined fleet, 1 for 2nd combined fleet
     * @return sum of level
     */
    private int setFleetInfo(int[] list, int base_view_id) {
        int sum_level = 0;
        int ships_in_fleet = list.length;
        // 1-6 for normal fleet and combined fleet
        // 7 for the striking force fleet (遊撃艦隊)
        for (int i = 0; i < Math.max(6, ships_in_fleet); i++) {
            int view_id = base_view_id * 6 + i;
            KcaFleetViewListItem ship = getFleetViewItem(view_id);

            if (i >= ships_in_fleet) {
                getFleetViewItem(view_id).setVisibility(INVISIBLE);
            } else {
                getFleetViewItem(view_id).setContent(list[i]);

                sum_level += ship.getShipInfo().lv;
            }
        }
        return sum_level;
    }

    /**
     *
     * @param index 0 <= index < 12
     * @return fleetview_item_$index
     */
    private KcaFleetViewListItem getFleetViewItem(int index) {
        if (index < 6) {
            ViewGroup main = mView.findViewById(R.id.fleet_list_main);
            return (KcaFleetViewListItem) main.getChildAt(index);
        } else {
            ViewGroup combined = mView.findViewById(R.id.fleet_list_combined);
            return (KcaFleetViewListItem) combined.getChildAt(index - 6);
        }
    }

    public void updateFleetInfoLine() {
        updateFleetInfoLine(-2);
    }

    public void updateFleetInfoLine(long moraleCompleteTime) {
        boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        final String displayText;
        if (KcaService.isPortAccessed) {
            if (moraleCompleteTime < -1) {
                if (selected == FLEET_COMBINED_ID) {
                    moraleCompleteTime = Math.max(KcaMoraleInfo.getMoraleCompleteTime(0),
                            KcaMoraleInfo.getMoraleCompleteTime(1));
                } else {
                    moraleCompleteTime = KcaMoraleInfo.getMoraleCompleteTime(selected);
                }
            }
            if (moraleCompleteTime > 0) {
                int diff = Math.max(0, (int)(moraleCompleteTime - System.currentTimeMillis()) / 1000);
                String moraleTimeText = KcaUtils.getTimeStr(diff);
                displayText = moraleTimeText.concat(" | ").concat(fleetCalcInfoText);
            } else {
                displayText = fleetCalcInfoText;
            }
        } else {
            displayText = "";
        }

        final String akashi_timer_text = KcaUtils.getTimeStr(KcaAkashiRepairInfo.getAkashiElapsedTimeInSecond());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isReady && displayText.length() > 0) {
                    if (!displayText.contentEquals(fleetInfoLine.getText())) {
                        fleetInfoLine.setText(displayText);
                    }

                    long akashiTimerValue = KcaAkashiRepairInfo.getAkashiTimerValue();
                    boolean isAkashiActive = akashiTimerValue >= 0 && isAkashiTimerActive;
                    for (int i = 0; i < 12; i++) {
                        getFleetViewItem(i).setAkashiTimer(isAkashiActive);
                    }

                    if (akashiTimerValue < 0) {
                        fleetAkashiTimerBtn.setVisibility(View.GONE);
                    } else {
                        if (isAkashiTimerActive) {
                            fleetAkashiTimerBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetAkashiTimerBtnActive));
                        } else {
                            fleetAkashiTimerBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetAkashiTimerBtnDeactive));
                        }
                        fleetAkashiTimerBtn.setText(akashi_timer_text);
                        fleetAkashiTimerBtn.setVisibility(View.VISIBLE);
                    }
                } else {
                    fleetInfoLine.setText(getStringWithLocale(R.string.kca_init_content));
                    fleetAkashiTimerBtn.setVisibility(View.GONE);
                }
            }
        });
    }


    public void setItemViewLayout(JsonObject data, String ship_id, int married) {
        Log.e("KCA", data.toString());
        JsonObject item_fit = gunfitData.getAsJsonObject("e_idx");
        JsonObject ship_fit = gunfitData.getAsJsonObject("s_idx");
        JsonObject fit_data = gunfitData.getAsJsonObject("f_idx");

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
                itemView.findViewById(getId("item".concat(String.valueOf(i + 1)), R.id.class)).setVisibility(GONE);
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(GONE);
            } else {
                slot_count += 1;
                JsonObject kcItemData;
                int lv = 0;
                int alv = -1;
                if (onslot != null) {
                    Log.e("KCA", "item_id: " + String.valueOf(item_id));
                    kcItemData = getUserItemStatusById(item_id, "level,alv", "id,type,name");
                    if (kcItemData == null) continue;
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
                        itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(GONE);
                    }

                    if (alv > 0) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                                .setText(getStringWithLocale(getId(KcaUtils.format("alv_%d", alv), R.string.class)));
                        int alvColorId = (alv <= 3) ? 1 : 2;
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), getId(KcaUtils.format("itemalv%d", alvColorId), R.color.class)));
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(GONE);
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
                        itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(INVISIBLE);
                    }
                } else {
                    kcItemData = getKcItemStatusById(item_id, "id,type,name");
                    itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(GONE);
                    itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(GONE);
                    itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(GONE);
                }

                String kcItemId = kcItemData.get("id").getAsString();
                String kcItemName = getSlotItemTranslation(kcItemData.get("name").getAsString());

                int fit_state = -1;
                if (item_fit.has(kcItemId) && ship_fit.has(ship_id)) {
                    int ship_idx = ship_fit.get(ship_id).getAsInt();
                    int item_idx = item_fit.get(kcItemId).getAsInt();
                    String key = KcaUtils.format("%d_%d", item_idx, ship_idx);
                    if (fit_data.has(key)) {
                        fit_state = fit_data.getAsJsonArray(key).get(married).getAsInt() + 2;
                    }
                }

                int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();
                int typeres = 0;
                try {
                    typeres = getId(KcaUtils.format("item_%d", type), R.mipmap.class);
                } catch (Exception e) {
                    typeres = R.mipmap.item_0;
                }
                ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class))).setText(kcItemName);
                if (fit_state == -1) {
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                } else {
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), KcaUtils.getId(KcaUtils.format("colorGunfit%d", fit_state), R.color.class)));
                }

                ((ImageView) itemView.findViewById(getId(KcaUtils.format("item%d_icon", i + 1), R.id.class))).setImageResource(typeres);
                itemView.findViewById(getId("item".concat(String.valueOf(i + 1)), R.id.class)).setVisibility(View.VISIBLE);
            }
        }

        if (slot_ex != 0) {
            // EX_SLOT
            if (slot_ex > 0) {
                slot_count += 1;
                JsonObject kcItemData = getUserItemStatusById(slot_ex, "level", "type,name");
                if (kcItemData != null) {
                    String kcItemName = getSlotItemTranslation(kcItemData.get("name").getAsString());
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
                    itemView.findViewById(R.id.item_ex_icon).setVisibility(View.VISIBLE);
                    if (lv > 0) {
                        ((TextView) itemView.findViewById(R.id.item_ex_level))
                                .setText(getStringWithLocale(R.string.lv_star).concat(String.valueOf(lv)));
                        itemView.findViewById(R.id.item_ex_level).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(R.id.item_ex_level).setVisibility(GONE);
                    }
                } else {
                    ((TextView) itemView.findViewById(R.id.item_ex_name)).setText("???");
                    itemView.findViewById(R.id.view_slot_ex).setVisibility(INVISIBLE);
                }
            } else {
                ((TextView) itemView.findViewById(R.id.item_ex_name)).setText(getStringWithLocale(R.string.slot_empty));
                ((ImageView) itemView.findViewById(R.id.item_ex_icon)).setImageResource(R.mipmap.item_0);
                itemView.findViewById(R.id.item_ex_level).setVisibility(GONE);
                itemView.findViewById(R.id.item_ex_slot_list).setVisibility(INVISIBLE);
            }
            itemView.findViewById(R.id.view_slot_ex).setVisibility(View.VISIBLE);
        } else {
            itemView.findViewById(R.id.view_slot_ex).setVisibility(GONE);
        }

        if (onslot_count == 0) {
            for (int i = 0; i < slot.size(); i++) {
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(GONE);
            }
            itemView.findViewById(R.id.item_slot_list).setVisibility(GONE);
            itemView.findViewById(R.id.item_ex_slot_list).setVisibility(GONE);
        } else {
            itemView.findViewById(R.id.item_slot_list).setVisibility(View.VISIBLE);
        }

        if (slot_count == 0) {
            ((TextView) itemView.findViewById(R.id.item1_name)).setText(getStringWithLocale(R.string.slot_empty));
            ((TextView) itemView.findViewById(R.id.item1_name)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            ((ImageView) itemView.findViewById(R.id.item1_icon)).setImageResource(R.mipmap.item_0);
            itemView.findViewById(R.id.item1_level).setVisibility(GONE);
            itemView.findViewById(R.id.item1_alv).setVisibility(GONE);
            itemView.findViewById(R.id.item1_slot).setVisibility(GONE);
            itemView.findViewById(R.id.item1).setVisibility(View.VISIBLE);
        }
        itemView.setVisibility(View.VISIBLE);
    }

    private int getSeekCn() {
        return Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    private String getSeekType() {
        String seekType = "";
        switch (seekcn_internal) {
            case 1:
                seekType = getStringWithLocale(R.string.seek_type_1);
                break;
            case 2:
                seekType = getStringWithLocale(R.string.seek_type_2);
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
        seekcn_internal += 1;
        seekcn_internal %= 5;
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
                } else if (i < 4 && KcaMoraleInfo.getMoraleCompleteTime(i) > 0) {
                    mView.findViewById(view_id).setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNotGoodStatusBtn));
                } else if (i == FLEET_COMBINED_ID &&
                        (KcaMoraleInfo.getMoraleCompleteTime(0) > 0 || KcaMoraleInfo.getMoraleCompleteTime(1) > 0)) { // Combined Morale
                    mView.findViewById(view_id).setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNotGoodStatusBtn));
                } else {
                    mView.findViewById(view_id).setBackgroundColor(
                            ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                }
            }
        }
    }


    public static JsonObject loadGunfitData(AssetManager am) {
        try {
            AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) am.open("gunfit.json");
            byte[] bytes = ByteStreams.toByteArray(ais);
            return JsonParser.parseString(new String(bytes)).getAsJsonObject();
        } catch (IOException e) {
            return new JsonObject();
        }
    }

    private void sendReport(Exception e, int type) {
        error_flag = true;
        String data_str;
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        if (data == null) {
            data_str = "data is null";
        } else {
            data_str = data.toString();
        }
        if (mView != null) mView.setVisibility(GONE);

        KcaDBHelper helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(ERROR_TYPE_FLEETVIEW, "fleetview", "FV_".concat(String.valueOf(type)), data_str, getStringFromException(e));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            mInflater = LayoutInflater.from(contextWithLocale);
            int visibllity = mView.getVisibility();
            if (mManager != null) {
                if (mView.getParent() != null) mManager.removeViewImmediate(mView);
                initView();
                mManager.addView(mView, mParams);

                int setViewResult = setView();
                if (setViewResult == 0) {
                    if (mView.getParent() != null) {
                        mView.invalidate();
                        mManager.updateViewLayout(mView, mParams);
                    }
                }
                mView.setVisibility(visibllity);
            }
        }
    }
}
