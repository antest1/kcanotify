package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.ContextThemeWrapper;
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

import com.google.android.material.chip.Chip;
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
import static android.view.View.VISIBLE;
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
import static com.antest1.kcanotify.KcaUseStatConstant.CLOSE_FLEETVIEW;
import static com.antest1.kcanotify.KcaUseStatConstant.FV_BTN_PRESS;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_FLEETVIEW;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutParamsFlags;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;
import static com.antest1.kcanotify.KcaUtils.setPreferences;
import static java.lang.Math.abs;
import static java.lang.Math.max;

public class KcaFleetViewService extends BaseService {
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

    private int screenWidth;
    private int screenHeight;

    Context contextWithTheme;
    LayoutInflater mInflater;
    SharedPreferences prefs;
    public KcaDBHelper dbHelper;
    public KcaDeckInfo deckInfoCalc;
    int seekcn_internal = -1;
    int switch_status = 1;
    Handler mHandler;
    Runnable timer;
    String fleetCalcInfoText = "";
    int akashiAvailableCount = 0;
    ScheduledExecutorService timeScheduler = null;

    static int view_status = 0;
    static boolean error_flag = false;
    boolean active;

    private DraggableOverlayLayout fleetView;
    private int fleetViewHeight = 0;
    private WindowManager.LayoutParams layoutParams;

    private View itemView, fleetHqInfoView;
    private TextView fleetInfoTitle, fleetInfoLine, fleetCnChangeBtn, fleetSwitchBtn, fleetAkashiTimerBtn;
    private WindowManager windowManager;
    private ScrollView fleetMenu;
    private ImageView fleetMenuArrowUp, fleetMenuArrowDown;
    private static boolean isReady;
    private static int[] hqinfoItems = {-1, -1, -1, -1};
    private static int hqinfoState = 0;
    private JsonObject gunfitData;

    int displayWidth = 0;

    private SnapIndicator snapIndicator;

    int selectedFleetIndex;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void setReadyFlag(boolean flag) {
        isReady = flag;
    }

    public boolean setView() {
        try {
            Log.e("KCA-FV", String.valueOf(selectedFleetIndex));
            setHqInfo();
            fleetInfoTitle.setVisibility(VISIBLE);
            updateSelectedFleetView(selectedFleetIndex);
            processDeckInfo(selectedFleetIndex, isCombinedFlag(selectedFleetIndex));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            sendReport(e, 0);
            return false;
        }
    }

    private void setNextState() {
        hqinfoState = (hqinfoState + 1) % HQINFO_TOTAL;
        if (hqinfoState>=HQINFO_ITEMCNT1 && hqinfoItems[hqinfoState-HQINFO_ITEMCNT1] == -1) setNextState();
    }

    private void setHqInfoViewVisibility() {
        if (hqinfoState == HQINFO_EXPVIEW) {
            fleetHqInfoView.findViewById(R.id.fleetview_exp).setVisibility(VISIBLE);
            fleetHqInfoView.findViewById(R.id.fleetview_cnt).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_item_cnt).setVisibility(View.GONE);
        } else if (hqinfoState == HQINFO_SECOUNT) {
            fleetHqInfoView.findViewById(R.id.fleetview_exp).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_cnt).setVisibility(VISIBLE);
            fleetHqInfoView.findViewById(R.id.fleetview_item_cnt).setVisibility(View.GONE);
        } else {
            fleetHqInfoView.findViewById(R.id.fleetview_exp).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_cnt).setVisibility(View.GONE);
            fleetHqInfoView.findViewById(R.id.fleetview_item_cnt).setVisibility(VISIBLE);
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
                        getString(R.string.fleetview_expview),
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
                    item_icon.setImageResource(R.drawable.ic_saury);
                } else {
                    item_icon.setImageResource(R.drawable.ic_gift);
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
        timeScheduler.scheduleWithFixedDelay(timer, 0, 1, TimeUnit.SECONDS);
    }

    void stopTimer() {
        if (timeScheduler != null) {
            timeScheduler.shutdown();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }
        updateScreenSize();
        try {
            active = true;
            switch_status = 1;
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            view_status = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_VIEW_YLOC));
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            dbHelper.updateExpScore(0);
            KcaApiData.setDBHelper(dbHelper);

            contextWithTheme = new ContextThemeWrapper(this, R.style.AppTheme);
            deckInfoCalc = new KcaDeckInfo(contextWithTheme);
            gunfitData = loadGunfitData(getAssets());

            mInflater = LayoutInflater.from(contextWithTheme);
            initView();

            fleetInfoLine.setText(getString(R.string.kca_init_content));
            itemView = mInflater.inflate(R.layout.view_battleview_items, null);
            mHandler = new Handler();
            timer = this::updateFleetInfoLine;
            runTimer();

            windowManager.addView(fleetView, layoutParams);
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
        if (fleetView != null) {
            if (fleetView.getParent() != null) windowManager.removeViewImmediate(fleetView);
        }
        if (snapIndicator != null) {
            snapIndicator.remove();
        }
        if (itemView != null) {
            if (itemView.getParent() != null) windowManager.removeViewImmediate(itemView);
        }
        super.onDestroy();
    }

    private void setFleetMenu() {
        LinearLayout fleetMenuArea = fleetView.findViewById(R.id.viewbutton_area);
        List<TextView> menuBtnList = new ArrayList<>();
        for (String key : fleetview_menu_keys) {
            TextView tv = fleetView.findViewById(KcaUtils.getId(KcaUtils.format("viewbutton_%s", key), R.id.class));
            tv.setText(getString(KcaUtils.getId(KcaUtils.format("viewmenu_%s", key), R.string.class)));
            menuBtnList.add(tv);
            ((ViewGroup) tv.getParent()).removeView(tv);
        }

        String order_data = getStringPreferences(getApplicationContext(), PREF_FV_MENU_ORDER);
        if (!order_data.isEmpty()) {
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

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        fleetView = (DraggableOverlayLayout) mInflater.inflate(R.layout.view_fleet_list, null);
        fleetView.setVisibility(GONE);
        KcaUtils.resizeFullWidthView(getApplicationContext(), fleetView);

        snapIndicator = new SnapIndicator(this, windowManager, mInflater);

        fleetView.findViewById(R.id.fleetview_head).setOnTouchListener(draggableLayoutTouchListener);

        fleetView.findViewById(R.id.fleetview_head).setOnClickListener(v -> {
            if (abs(layoutParams.x - startViewX) < 20 && abs(layoutParams.y - startViewY) < 20) {
                JsonObject statProperties = new JsonObject();
                statProperties.addProperty("manual", true);
                if (fleetView != null) fleetView.setVisibility(GONE);
                if (itemView != null) itemView.setVisibility(GONE);
            }
        });

        fleetView.findViewById(R.id.viewbutton_quest).setOnClickListener(
                v -> startPopupService(KcaQuestViewService.class,
                KcaQuestViewService.SHOW_QUESTVIEW_ACTION_NEW,
                "Quest"));
        fleetView.findViewById(R.id.viewbutton_excheck).setOnClickListener(v -> {
            String action = KcaExpeditionCheckViewService.SHOW_EXCHECKVIEW_ACTION
                    .concat("/").concat(String.valueOf(selectedFleetIndex));
            startPopupService(KcaExpeditionCheckViewService.class, action, "ExpeditionCheck");
        });
        fleetView.findViewById(R.id.viewbutton_develop).setOnClickListener(
                v -> startPopupService(KcaDevelopPopupService.class,
                        null,
                        "Develop"));
        fleetView.findViewById(R.id.viewbutton_construction).setOnClickListener(
                v -> startPopupService(KcaConstructPopupService.class,
                KcaConstructPopupService.CONSTR_DATA_ACTION,
                "Constr"));
        fleetView.findViewById(R.id.viewbutton_docking).setOnClickListener(
                v -> startPopupService(KcaDockingPopupService.class,
                KcaDockingPopupService.DOCKING_DATA_ACTION,
                "Docking"));
        fleetView.findViewById(R.id.viewbutton_maphp).setOnClickListener(
                v -> startPopupService(KcaMapHpPopupService.class,
                KcaMapHpPopupService.MAPHP_SHOW_ACTION,
                "MapHP"));
        fleetView.findViewById(R.id.viewbutton_fchk).setOnClickListener(
                v -> startPopupService(KcaFleetCheckPopupService.class,
                KcaFleetCheckPopupService.FCHK_SHOW_ACTION,
                "FleetCheck"));
        fleetView.findViewById(R.id.viewbutton_labinfo).setOnClickListener(
                v -> startPopupService(KcaLandAirBasePopupService.class,
                KcaLandAirBasePopupService.LAB_DATA_ACTION,
                "LandAirBaseInfo"));
        fleetView.findViewById(R.id.viewbutton_akashi).setOnClickListener(
                v -> startPopupService(KcaAkashiViewService.class,
                KcaAkashiViewService.SHOW_AKASHIVIEW_ACTION,
                "Akashi"));

        fleetView.findViewById(R.id.fleetview_tool).setOnClickListener(v -> {
            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("Tools"), null);
            if (fleetView != null) fleetView.setVisibility(GONE);
            if (itemView != null) itemView.setVisibility(GONE);
            Intent toolIntent = new Intent(KcaFleetViewService.this, MainActivity.class);
            toolIntent.setAction(MainActivity.ACTION_OPEN_TOOL);
            toolIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(toolIntent);
        });
        fleetView.findViewById(R.id.fleetview_cn_change).setOnClickListener(v -> {
            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("CnChange"), null);
            changeInternalSeekCn();
            fleetCnChangeBtn.setText(getSeekType());
            processDeckInfo(selectedFleetIndex, isCombinedFlag(selectedFleetIndex));
        });
        fleetView.findViewById(R.id.fleetview_fleetswitch).setOnClickListener(v -> {
            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("FleetChange"), null);
            if (switch_status == 1) {
                switch_status = 2;
                fleetView.findViewById(R.id.fleet_list_main).setVisibility(View.GONE);
                fleetView.findViewById(R.id.fleet_list_combined).setVisibility(VISIBLE);
                ((TextView) fleetView.findViewById(R.id.fleetview_fleetswitch)).setText(getString(R.string.fleetview_switch_2));
            } else {
                switch_status = 1;
                fleetView.findViewById(R.id.fleet_list_main).setVisibility(VISIBLE);
                fleetView.findViewById(R.id.fleet_list_combined).setVisibility(View.GONE);
                ((TextView) fleetView.findViewById(R.id.fleetview_fleetswitch)).setText(getString(R.string.fleetview_switch_1));
            }
        });
        fleetView.findViewById(R.id.fleetview_hqinfo).setOnClickListener(v -> {
            setNextState();
            JsonObject statProperties = new JsonObject();
            statProperties.addProperty("state", hqinfoState);
            sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat("HqInfo"), statProperties);
            setHqInfo();
        });
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            fleetView.findViewById(getId("fleet_".concat(String.valueOf(i + 1)), R.id.class)).setOnClickListener(v -> {
                selectedFleetIndex = finalI;
                setView();
            });
        }

        for (int i = 0; i < 12; i++) {
            getFleetViewItem(i).setOnTouchListener(fleetViewItemTouchListener);
        }

        setFleetMenu();
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                getWindowLayoutParamsFlags(getResources().getConfiguration()),
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP;

        fleetView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom - top == oldBottom - oldTop) return;
            fleetViewHeight = bottom - top;
            layoutParams.y = (screenHeight - fleetViewHeight) * (view_status + 1) / 2;
            if ((fleetView.getParent() != null)) {
                windowManager.updateViewLayout(fleetView, layoutParams);
            }
        });
        // Hide at bottom before the fleetView is first rendered
        layoutParams.y = screenHeight;
        setPreferences(getApplicationContext(), PREF_VIEW_YLOC, view_status);

        fleetInfoLine = fleetView.findViewById(R.id.fleetview_infoline);
        fleetInfoTitle = fleetView.findViewById(R.id.fleetview_title);
        fleetHqInfoView = fleetView.findViewById(R.id.fleetview_hqinfo);
        fleetCnChangeBtn = fleetView.findViewById(R.id.fleetview_cn_change);
        fleetAkashiTimerBtn = fleetView.findViewById(R.id.fleetview_akashi_timer);
        fleetSwitchBtn = fleetView.findViewById(R.id.fleetview_fleetswitch);
        fleetMenu = fleetView.findViewById(R.id.fleetview_menu);
        fleetMenuArrowUp = fleetView.findViewById(R.id.fleetview_menu_up);
        fleetMenuArrowDown = fleetView.findViewById(R.id.fleetview_menu_down);

        fleetAkashiTimerBtn.setVisibility(View.GONE);
        fleetSwitchBtn.setVisibility(View.GONE);
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
                    fleetMenuArrowUp.setVisibility(VISIBLE);
                    fleetMenuArrowDown.setVisibility(VISIBLE);
                    fleetMenuArrowUp.setAlpha(0.6f);
                    fleetMenuArrowDown.setAlpha(0.6f);
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(SHOW_FLEETVIEW_ACTION)) {
                if (fleetView != null && fleetView.getVisibility() != VISIBLE) {
                    if (seekcn_internal == -1) seekcn_internal = getSeekCn();
                    fleetCnChangeBtn.setText(getSeekType());
                    if (setView()) {
                        fleetView.setVisibility(VISIBLE);
                        if (fleetView.getParent() != null) {
                            windowManager.removeViewImmediate(fleetView);
                        }
                        windowManager.addView(fleetView, layoutParams);
                    }
                    sendUserAnalytics(getApplicationContext(), OPEN_FLEETVIEW, null);
                }
            }
            if (intent.getAction().equals(REFRESH_FLEETVIEW_ACTION)) {
                if (setView()) {
                    if (fleetView != null && fleetView.getParent() != null) {
                        fleetView.invalidate();
                        windowManager.updateViewLayout(fleetView, layoutParams);
                    }
                }
            }
            if (intent.getAction().equals(CLOSE_FLEETVIEW_ACTION)) {
                if (fleetView != null) {
                    fleetView.setVisibility(GONE);
                    if (fleetView.getParent() != null) {
                        windowManager.removeViewImmediate(fleetView);
                    }
                    snapIndicator.remove();
                    itemView.setVisibility(GONE);
                    if (itemView.getParent() != null) {
                        windowManager.removeViewImmediate(itemView);
                    }
                    JsonObject statProperties = new JsonObject();
                    statProperties.addProperty("manual", false);
                    sendUserAnalytics(getApplicationContext(), CLOSE_FLEETVIEW, statProperties);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    private int startViewX, startViewY; // Starting view x y
    private final View.OnTouchListener draggableLayoutTouchListener = new View.OnTouchListener() {
        private float startRawX, startRawY; // Starting finger x y
        private final float[] lastX = new float[3];
        private final float[] lastY = new float[3];
        private final long[] lastT = new long[3];
        private int curr = 0;
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean isViewLocked = getBooleanPreferences(getApplicationContext(), PREF_FIX_VIEW_LOC);
            int maxY = screenHeight - fleetView.getHeight();
            float finalX, finalY;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRawX = event.getRawX();
                    startRawY = event.getRawY();
                    lastX[curr] = startRawX;
                    lastY[curr] = startRawY;
                    lastT[curr] = Calendar.getInstance().getTimeInMillis();
                    curr = (curr + 1) % 3;
                    startViewX = layoutParams.x;
                    startViewY = layoutParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", startViewX, startViewY));
                    if (!isViewLocked) {
                        snapIndicator.show(layoutParams.y, maxY, fleetViewHeight);
                        fleetView.cancelAnimations();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (!isViewLocked) {
                        float dx = event.getRawX() - lastX[(curr + 1) % 3];
                        float dy = event.getRawY() - lastY[(curr + 1) % 3];
                        long dt = Calendar.getInstance().getTimeInMillis() - lastT[(curr + 1) % 3];
                        float finalXUncap = layoutParams.x + dx / dt * 400;
                        float finalYUncap = layoutParams.y + dy / dt * 400;
                        finalX = 0f;
                        // Snap to either top, center, or bottom
                        if (finalYUncap < maxY / 4f) {
                            finalY = 0;
                            view_status = -1;
                        } else if (finalYUncap < maxY / 4f * 3f) {
                            finalY = maxY / 2f;
                            view_status = 0;
                        } else {
                            finalY = maxY;
                            view_status = 1;
                        }
                        fleetView.animateTo(layoutParams.x, layoutParams.y,
                                0, (int) finalY,
                                finalXUncap == finalX ? 0 : max(2f, abs(dx / dt) / 2f), finalYUncap == finalY ? 0 : max(2f, abs(dy / dt) / 2f),
                                500, windowManager, layoutParams);
                        snapIndicator.remove();
                    }

                    // Save new preference to DB
                    setPreferences(getApplicationContext(), PREF_VIEW_YLOC, view_status);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isViewLocked) {
                        int x = (int) (event.getRawX() - startRawX);
                        int y = (int) (event.getRawY() - startRawY);
                        Log.e("KCA", KcaUtils.format("Coord: %d %d", x, y));

                        lastX[curr] = event.getRawX();
                        lastY[curr] = event.getRawY();
                        lastT[curr] = Calendar.getInstance().getTimeInMillis();
                        curr = (curr + 1) % 3;
                        finalX = startViewX + x;
                        finalY = startViewY + y;

                        layoutParams.x = (int) finalX;
                        layoutParams.y = (int) finalY;

                        windowManager.updateViewLayout(fleetView, layoutParams);
                        snapIndicator.update(finalY, maxY);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    fleetView.cancelAnimations();
                    snapIndicator.remove();
                    layoutParams.x = startViewX;
                    layoutParams.y = startViewY;
                    windowManager.updateViewLayout(fleetView, layoutParams);
                    break;
            }
            return false;
        }
    };

    private final View.OnTouchListener fleetViewItemTouchListener = new View.OnTouchListener() {
        private boolean isInsideView(View view, float x, float y) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);

            float viewLeft = location[0];
            float viewTop = location[1];
            float viewRight = viewLeft + view.getWidth();
            float viewBottom = viewTop + view.getHeight();

            return (x >= viewLeft && x <= viewRight && y >= viewTop && y <= viewBottom);
        }

        final WindowManager.LayoutParams itemViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        int selected = -1;
        @SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int xmargin = (int) getResources().getDimension(R.dimen.item_popup_xmargin);
            int ymargin = (int) getResources().getDimension(R.dimen.item_popup_ymargin);

            float x = event.getRawX();
            float y = event.getRawY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_DOWN:
                    Log.e("KCA-FV", "ACTION_DOWN");
                    for (int i = 0; i < 12; i++) {
                        // Check the x, y position is inside the view, instead of id
                        // because the ACTION_MOVE event is fired on the original start-touch item, not the item current touching
                        if (isInsideView(getFleetViewItem(i), x , y)) {
                            if (selected != i) {
                                // Reload data if selecting another ship
                                JsonArray data;
                                int shipIndex;
                                if (isCombinedFlag(selectedFleetIndex)) {
                                    if (i < 6) {
                                        data = deckInfoCalc.getDeckListInfo(dbHelper.getJsonArrayValue(DB_KEY_DECKPORT), 0, DECKINFO_REQ_LIST, KC_DECKINFO_REQ_LIST);
                                    } else {
                                        data = deckInfoCalc.getDeckListInfo(dbHelper.getJsonArrayValue(DB_KEY_DECKPORT), 1, DECKINFO_REQ_LIST, KC_DECKINFO_REQ_LIST);
                                    }
                                    shipIndex = i % 6;
                                } else {
                                    data = deckInfoCalc.getDeckListInfo(dbHelper.getJsonArrayValue(DB_KEY_DECKPORT), selectedFleetIndex, DECKINFO_REQ_LIST, KC_DECKINFO_REQ_LIST);
                                    shipIndex = i;
                                }
                                if (shipIndex < data.size()) {
                                    JsonObject udata = data.get(shipIndex).getAsJsonObject().getAsJsonObject("user");
                                    JsonObject kcdata = data.get(shipIndex).getAsJsonObject().getAsJsonObject("kc");

                                    String ship_id = udata.get("ship_id").getAsString();
                                    int ship_married = udata.get("lv").getAsInt() >= 100 ? 1 : 0;
                                    JsonObject itemData = new JsonObject();
                                    itemData.add("api_slot", udata.get("slot"));
                                    itemData.add("api_slot_ex", udata.get("slot_ex"));
                                    itemData.add("api_onslot", udata.get("onslot"));
                                    itemData.add("api_maxslot", kcdata.get("maxeq"));
                                    setItemViewLayout(itemData, ship_id, ship_married);
                                }
                            }

                            if (event.getRawX() + itemView.getWidth() > screenWidth) {
                                itemViewParams.x = (int) (event.getRawX() - xmargin - itemView.getWidth());
                            } else {
                                itemViewParams.x = (int) (event.getRawX() + xmargin);
                            }
                            itemViewParams.y = (int) (event.getRawY() - ymargin - itemView.getHeight());
                            itemViewParams.gravity = Gravity.TOP | Gravity.START;

                            if (itemView.getParent() != null) {
                                if (selected == -1 || selected != i) {
                                    // Selection changed
                                    windowManager.removeViewImmediate(itemView);
                                    windowManager.addView(itemView, itemViewParams);
                                } else {
                                    windowManager.updateViewLayout(itemView, itemViewParams);
                                }
                            } else {
                                windowManager.addView(itemView, itemViewParams);
                            }
                            selected = i;
                            return true;
                        }
                    }
                case MotionEvent.ACTION_UP:
                    Log.e("KCA-FV", "ACTION_UP");
                    itemView.setVisibility(GONE);
                    selected = -1;
                    break;
            }
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
        int deck_count = (deckportdata != null) ? deckportdata.size() : 0;

        if (!isReady) {
            fleetCalcInfoText = "";
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            fleetInfoLine.setText(getString(R.string.kca_init_content));
            fleetView.findViewById(R.id.fleet_list_main).setVisibility(INVISIBLE);
            fleetView.findViewById(R.id.fleet_list_combined).setVisibility(is_landscape? INVISIBLE : View.GONE);
            fleetSwitchBtn.setVisibility(View.GONE);
            return;
        }

        boolean not_opened_flag = idx == FLEET_COMBINED_ID ? deck_count < 2 : idx >= deck_count;
        if (not_opened_flag) {
            fleetCalcInfoText = "Not Opened";
            fleetInfoLine.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoNoShip));
            fleetInfoLine.setText(fleetCalcInfoText);
            fleetView.findViewById(R.id.fleet_list_main).setVisibility(INVISIBLE);
            fleetView.findViewById(R.id.fleet_list_combined).setVisibility(is_landscape? INVISIBLE : View.GONE);
            fleetSwitchBtn.setVisibility(View.GONE);
            return;
        }

        if (is_landscape) {
            fleetSwitchBtn.setVisibility(View.GONE);
            fleetView.findViewById(R.id.fleet_list_main).setVisibility(VISIBLE);
            fleetView.findViewById(R.id.fleet_list_combined).setVisibility(is_combined ? VISIBLE : INVISIBLE);
        } else {
            boolean switch_is_one = switch_status == 1;
            fleetSwitchBtn.setVisibility(is_combined ? VISIBLE : View.GONE);
            fleetView.findViewById(R.id.fleet_list_main).setVisibility((!is_combined || switch_is_one) ? VISIBLE : View.GONE);
            fleetView.findViewById(R.id.fleet_list_combined).setVisibility((is_combined && !switch_is_one) ? VISIBLE : View.GONE);
        }

        int cn = seekcn_internal;
        List<String> infoList = new ArrayList<>();

        String airPowerValue;
        if (isCombined) {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(deckportdata, 0, KcaBattle.getEscapeFlag());
        } else {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(deckportdata, idx, null);
        }
        if (!airPowerValue.isEmpty()) {
            infoList.add(airPowerValue);
        }

        double seekValue;
        String seekStringValue;
        if (isCombined) {
            seekValue = deckInfoCalc.getSeekValue(deckportdata, "0,1", cn, KcaBattle.getEscapeFlag());
        } else {
            seekValue = deckInfoCalc.getSeekValue(deckportdata, String.valueOf(idx), cn, null);
        }
        if (cn == SEEK_PURE) {
            seekStringValue = KcaUtils.format(getString(R.string.fleetview_seekvalue_d), (int) seekValue);
        } else {
            seekStringValue = KcaUtils.format(getString(R.string.fleetview_seekvalue_f), seekValue);
        }
        infoList.add(seekStringValue);

        String speedStringValue;
        if (isCombined) {
            speedStringValue = deckInfoCalc.getSpeedString(deckportdata, "0,1", KcaBattle.getEscapeFlag());
        } else {
            speedStringValue = deckInfoCalc.getSpeedString(deckportdata, String.valueOf(idx), null);
        }
        infoList.add(speedStringValue);

        String tpValue;
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
                fleetView.findViewById(R.id.fleet_list_combined).setVisibility(VISIBLE);
            }
            sum_level += setFleetInfo(ship_id_list, 0);
        }

        akashiAvailableCount = KcaAkashiRepairInfo.getAkashiAvailableCount(idx);

        infoList.add("LV ".concat(String.valueOf(sum_level)));
        fleetCalcInfoText = joinStr(infoList, " / ");
        long moraleCompleteTime;
        if (selectedFleetIndex == FLEET_COMBINED_ID) {
            moraleCompleteTime = Math.max(KcaMoraleInfo.getMoraleCompleteTime(0),
                    KcaMoraleInfo.getMoraleCompleteTime(1));
        } else {
            moraleCompleteTime = KcaMoraleInfo.getMoraleCompleteTime(selectedFleetIndex);
        }
        if (selectedFleetIndex < 4 && KcaExpedition2.isInExpedition(selectedFleetIndex)) {
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
            ViewGroup main = fleetView.findViewById(R.id.fleet_list_main);
            return (KcaFleetViewListItem) main.getChildAt(index);
        } else {
            ViewGroup combined = fleetView.findViewById(R.id.fleet_list_combined);
            return (KcaFleetViewListItem) combined.getChildAt(index - 6);
        }
    }

    public void updateFleetInfoLine() {
        updateFleetInfoLine(-2);
    }

    public void updateFleetInfoLine(long moraleCompleteTime) {
        final String displayText;
        if (KcaService.isPortAccessed) {
            if (moraleCompleteTime < -1) {
                if (selectedFleetIndex == FLEET_COMBINED_ID) {
                    moraleCompleteTime = Math.max(KcaMoraleInfo.getMoraleCompleteTime(0),
                            KcaMoraleInfo.getMoraleCompleteTime(1));
                } else {
                    moraleCompleteTime = KcaMoraleInfo.getMoraleCompleteTime(selectedFleetIndex);
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

        mHandler.post(() -> {
            if (isReady && !displayText.isEmpty()) {
                if (!displayText.contentEquals(fleetInfoLine.getText())) {
                    fleetInfoLine.setText(displayText);
                }

                long akashiTimerValue = KcaAkashiRepairInfo.getAkashiTimerValue();
                boolean isAkashiActive = akashiTimerValue >= 0 && akashiAvailableCount > 0;
                for (int i = 0; i < 6; i++) {
                    if (i < akashiAvailableCount) {
                        getFleetViewItem(i).setAkashiTimer(isAkashiActive);
                    } else {
                        getFleetViewItem(i).setAkashiTimer(false);
                    }
                }

                if (akashiTimerValue < 0) {
                    fleetAkashiTimerBtn.setVisibility(View.GONE);
                } else {
                    if (akashiAvailableCount > 0) {
                        fleetAkashiTimerBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetAkashiTimerBtnActive));
                    } else {
                        fleetAkashiTimerBtn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetAkashiTimerBtnDeactive));
                    }
                    if (!akashi_timer_text.contentEquals(fleetAkashiTimerBtn.getText())) {
                        fleetAkashiTimerBtn.setText(akashi_timer_text);
                    }
                    fleetAkashiTimerBtn.setVisibility(VISIBLE);
                }
            } else {
                if (!getString(R.string.kca_init_content).contentEquals(fleetInfoLine.getText())) {
                    fleetInfoLine.setText(getString(R.string.kca_init_content));
                }
                fleetAkashiTimerBtn.setVisibility(View.GONE);
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
                    Log.e("KCA", "item_id: " + item_id);
                    kcItemData = getUserItemStatusById(item_id, "level,alv", "id,type,name");
                    if (kcItemData == null) continue;
                    Log.e("KCA", kcItemData.toString());
                    lv = kcItemData.get("level").getAsInt();
                    if (kcItemData.has("alv")) {
                        alv = kcItemData.get("alv").getAsInt();
                    }

                    if (lv > 0) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)))
                                .setText(getString(R.string.lv_star).concat(String.valueOf(lv)));
                        itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(VISIBLE);
                    } else {
                        itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(GONE);
                    }

                    if (alv > 0) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                                .setText(getString(getId(KcaUtils.format("alv_%d", alv), R.string.class)));
                        int alvColorId = (alv <= 3) ? 1 : 2;
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), getId(KcaUtils.format("itemalv%d", alvColorId), R.color.class)));
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(VISIBLE);
                    } else {
                        itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(GONE);
                    }

                    int itemtype = kcItemData.getAsJsonArray("type").get(2).getAsInt();
                    if (isItemAircraft(itemtype)) {
                        onslot_count += 1;
                        Log.e("KCA", "ID: " + itemtype);
                        int nowSlotValue = onslot.get(i).getAsInt();
                        int maxSlotValue = maxslot.get(i).getAsInt();
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)))
                                .setText(KcaUtils.format("[%02d/%02d]", nowSlotValue, maxSlotValue));
                        itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(VISIBLE);
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
                int typeres;
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
                itemView.findViewById(getId("item".concat(String.valueOf(i + 1)), R.id.class)).setVisibility(VISIBLE);
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
                    int typeres;
                    try {
                        typeres = getId(KcaUtils.format("item_%d", type), R.mipmap.class);
                    } catch (Exception e) {
                        typeres = R.mipmap.item_0;
                    }
                    ((TextView) itemView.findViewById(R.id.item_ex_name)).setText(kcItemName);
                    ((ImageView) itemView.findViewById(R.id.item_ex_icon)).setImageResource(typeres);
                    itemView.findViewById(R.id.item_ex_icon).setVisibility(VISIBLE);
                    if (lv > 0) {
                        ((TextView) itemView.findViewById(R.id.item_ex_level))
                                .setText(getString(R.string.lv_star).concat(String.valueOf(lv)));
                        itemView.findViewById(R.id.item_ex_level).setVisibility(VISIBLE);
                    } else {
                        itemView.findViewById(R.id.item_ex_level).setVisibility(GONE);
                    }
                } else {
                    ((TextView) itemView.findViewById(R.id.item_ex_name)).setText("???");
                    itemView.findViewById(R.id.view_slot_ex).setVisibility(INVISIBLE);
                }
            } else {
                ((TextView) itemView.findViewById(R.id.item_ex_name)).setText(getString(R.string.slot_empty));
                ((ImageView) itemView.findViewById(R.id.item_ex_icon)).setImageResource(R.mipmap.item_0);
                itemView.findViewById(R.id.item_ex_level).setVisibility(GONE);
                itemView.findViewById(R.id.item_ex_slot_list).setVisibility(INVISIBLE);
            }
            itemView.findViewById(R.id.view_slot_ex).setVisibility(VISIBLE);
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
            itemView.findViewById(R.id.item_slot_list).setVisibility(VISIBLE);
        }

        if (slot_count == 0) {
            ((TextView) itemView.findViewById(R.id.item1_name)).setText(getString(R.string.slot_empty));
            ((TextView) itemView.findViewById(R.id.item1_name)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            ((ImageView) itemView.findViewById(R.id.item1_icon)).setImageResource(R.mipmap.item_0);
            itemView.findViewById(R.id.item1_level).setVisibility(GONE);
            itemView.findViewById(R.id.item1_alv).setVisibility(GONE);
            itemView.findViewById(R.id.item1_slot).setVisibility(GONE);
            itemView.findViewById(R.id.item1).setVisibility(VISIBLE);
        }
        itemView.setVisibility(VISIBLE);
    }

    private int getSeekCn() {
        return Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    private String getSeekType() {
        String seekType;
        switch (seekcn_internal) {
            case 1:
                seekType = getString(R.string.seek_type_1);
                break;
            case 2:
                seekType = getString(R.string.seek_type_2);
                break;
            case 3:
                seekType = getString(R.string.seek_type_3);
                break;
            case 4:
                seekType = getString(R.string.seek_type_4);
                break;
            default:
                seekType = getString(R.string.seek_type_0);
                break;
        }
        return seekType;
    }

    private void changeInternalSeekCn() {
        seekcn_internal += 1;
        seekcn_internal %= 5;
    }

    private void updateSelectedFleetView(int selected) {
        for (int i = 0; i < 5; i++) {
            int view_id = getId("fleet_".concat(String.valueOf(i + 1)), R.id.class);
            Chip chip = fleetView.findViewById(view_id);
            chip.setChecked(selected == i);
            if (selected == i) {
                chip.setChipStrokeColorResource(R.color.colorAccent);
            } else {
                if (i < 4 && KcaExpedition2.isInExpedition(i)) {
                    chip.setChipStrokeColorResource(R.color.colorFleetInfoExpeditionBtn);
                } else if (i < 4 && KcaMoraleInfo.getMoraleCompleteTime(i) > 0) {
                    chip.setChipStrokeColorResource(R.color.colorFleetInfoNotGoodStatusBtn);
                } else if (i == FLEET_COMBINED_ID &&
                        (KcaMoraleInfo.getMoraleCompleteTime(0) > 0 || KcaMoraleInfo.getMoraleCompleteTime(1) > 0)) { // Combined Morale
                    chip.setChipStrokeColorResource(R.color.colorFleetInfoNotGoodStatusBtn);
                } else {
                    chip.setChipStrokeColorResource(R.color.colorFleetInfoNormalBtn);
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
        if (fleetView != null) fleetView.setVisibility(GONE);

        KcaDBHelper helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(ERROR_TYPE_FLEETVIEW, "fleetview", "FV_".concat(String.valueOf(type)), data_str, getStringFromException(e));
    }

    private void startPopupService(Class<?> target, String action) {
        startPopupService(target, action, null);
    }
    private void startPopupService(Class<?> target, String action, String label) {
        boolean checkGameData = false;
        switch (target.getSimpleName()) {
            case "KcaConstructPopupService":
            case "KcaDevelopPopupService":
            case "KcaDockingPopupService":
                checkGameData = true;
                break;
            default:
                break;
        }

        if (checkGameData && !isGameDataLoaded()) return;
        if (label != null) sendUserAnalytics(getApplicationContext(), FV_BTN_PRESS.concat(label), null);
        Intent popupIntent = new Intent(getBaseContext(), target);
        if (action != null) popupIntent.setAction(action);
        startService(popupIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateScreenSize();
        super.onConfigurationChanged(newConfig);

        contextWithTheme = new ContextThemeWrapper(this, R.style.AppTheme);
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            mInflater = LayoutInflater.from(contextWithTheme);
            int visibility = fleetView.getVisibility();
            if (windowManager != null) {
                if (fleetView.getParent() != null) windowManager.removeViewImmediate(fleetView);
                initView();
                layoutParams.flags = getWindowLayoutParamsFlags(newConfig);
                windowManager.addView(fleetView, layoutParams);

                if (setView()) {
                    if (fleetView.getParent() != null) {
                        fleetView.invalidate();
                        windowManager.updateViewLayout(fleetView, layoutParams);
                    }
                }
                fleetView.setVisibility(visibility);
            }

            // reopen popups
            if (KcaQuestViewService.isActive()) {
                startPopupService(KcaQuestViewService.class, KcaQuestViewService.SHOW_QUESTVIEW_ACTION_NEW);
            }
            if (KcaExpeditionCheckViewService.isActive()) {
                String action = KcaExpeditionCheckViewService.SHOW_EXCHECKVIEW_ACTION
                        .concat("/").concat(String.valueOf(selectedFleetIndex));
                startPopupService(KcaExpeditionCheckViewService.class, action);
            }
            if (KcaDevelopPopupService.isActive()) {
                startPopupService(KcaDevelopPopupService.class, null);
            }
            if (KcaConstructPopupService.isActive()) {
                startPopupService(KcaConstructPopupService.class, KcaConstructPopupService.CONSTR_DATA_ACTION);
            }
            if (KcaDockingPopupService.isActive()) {
                startPopupService(KcaDockingPopupService.class, KcaDockingPopupService.DOCKING_DATA_ACTION);
            }
            if (KcaMapHpPopupService.isActive()) {
                startPopupService(KcaMapHpPopupService.class, KcaMapHpPopupService.MAPHP_SHOW_ACTION);
            }
            if (KcaFleetCheckPopupService.isActive()) {
                startPopupService(KcaFleetCheckPopupService.class, KcaFleetCheckPopupService.FCHK_SHOW_ACTION);
            }
            if (KcaLandAirBasePopupService.isActive()) {
                startPopupService(KcaLandAirBasePopupService.class, KcaLandAirBasePopupService.LAB_DATA_ACTION);
            }
            if (KcaAkashiViewService.isActive()) {
                startPopupService(KcaAkashiViewService.class, SHOW_AKASHIVIEW_ACTION);
            }
        }
    }

    private void updateScreenSize() {
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
    }
}
