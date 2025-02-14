package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_QUESTVIEW;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_QUESTVIEW;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;


public class KcaQuestViewService extends BaseService {
    public static final String REFRESH_QUESTVIEW_ACTION = "refresh_questview";
    public static final String SHOW_QUESTVIEW_ACTION = "show_questview";
    public static final String SHOW_QUESTVIEW_ACTION_NEW = "show_questview_new";
    public static final String CLOSE_QUESTVIEW_ACTION = "close_questview";
    private static final int PAGE_SIZE = 5;

    public static boolean active = false;
    public static JsonObject api_data;
    private static boolean isamenuvisible = false;
    private static boolean isquestlist = false;
    private static boolean error_flag = false;

    Context contextWithTheme;
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver;
    private int currentPage = 1;
    final int[] pageIndexList = {R.id.quest_page_1, R.id.quest_page_2, R.id.quest_page_3, R.id.quest_page_4, R.id.quest_page_5};
    final int[] filterCategoryList = {2, 3, 4, 6, 7};
    int currentFilterState = -1;
    JsonArray currentQuestList = new JsonArray();

    public KcaDBHelper helper;
    private KcaQuestTracker questTracker;
    private View layoutView;
    private WindowManager windowManager;

    int displayWidth = 0;

    WindowManager.LayoutParams layoutParams;
    ScrollView questView;
    View questDescPopupView;
    ListView questList;
    KcaQuestListAdpater adapter;
    ImageView questMenuButton;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean getQuestMode() {
        return isquestlist;
    }

    public static void setQuestMode(boolean v) {
        isquestlist = v;
    }

    public static void setApiData(JsonObject data) {
        api_data = data;
    }

    @SuppressLint("DefaultLocale")
    public void setQuestView(JsonArray api_list, boolean checkValid, int filter) {
        adapter.setListViewItemList(api_list, filter);
        adapter.notifyDataSetChanged();
        questList.setAdapter(adapter);
        scrollListView(0);
        setTopBottomNavigation(1, adapter.getCount());
    }

    public int setView(boolean isquestlist, boolean checkValid, int tab_id, int filter_id) {
        try {
            Log.e("KCA", "QuestView setView " + String.valueOf(isquestlist));
            error_flag = false;

            int api_count = 0;
            if (isquestlist && api_data != null) {
                Log.e("KCA-Q", api_data.toString());
                if(api_data.has("api_list")) {
                    if (api_data.get("api_list").isJsonArray()) {
                        currentQuestList = api_data.getAsJsonArray("api_list");
                        api_count = api_data.get("api_count").getAsInt();
                    } else {
                        currentQuestList = new JsonArray();
                        api_count = 0;
                    }
                }
            } else {
                currentQuestList = helper.getCurrentQuestList();
                api_count = currentQuestList.size();
            }
            Log.e("KCA", currentQuestList.toString());
            if (checkValid) {
                questTracker.clearInvalidQuestTrack();
                helper.checkValidQuest(currentQuestList, tab_id);
            }
            int filter = -1;
            if (filter_id > -1) filter = filterCategoryList[filter_id];
            setQuestView(currentQuestList, checkValid, filter);
            questDescPopupView.setVisibility(View.GONE);
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
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            try {
                helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
                questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);

                contextWithTheme = new ContextThemeWrapper(this, R.style.AppTheme);
                broadcaster = LocalBroadcastManager.getInstance(this);
                mInflater = LayoutInflater.from(contextWithTheme);
                layoutView = mInflater.inflate(R.layout.view_quest_list_v2, null);
                KcaUtils.resizeFullWidthView(getApplicationContext(), layoutView.findViewById(R.id.questviewpanel));
                layoutView.setVisibility(View.GONE);

                questView = layoutView.findViewById(R.id.questview);
                questView.findViewById(R.id.quest_head).setOnTouchListener(mViewTouchListener);
                questDescPopupView = layoutView.findViewById(R.id.quest_desc_popup);
                questDescPopupView.setVisibility(View.GONE);
                questDescPopupView.findViewById(R.id.view_qd_head).setOnTouchListener(popupViewTouchListener);
                ((TextView) questDescPopupView.findViewById(R.id.view_qd_rewards_hd))
                        .setText(getString(R.string.questview_reward));

                adapter = new KcaQuestListAdpater(KcaQuestViewService.this, questTracker);
                questList = layoutView.findViewById(R.id.quest_list);
                questList.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        TextView page_title = questView.findViewById(R.id.quest_page);
                        page_title.setText(getString(R.string.questview_page)
                                .replace("%d/%d", "???"));
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    }
                });

                isamenuvisible = false;
                questView.findViewById(R.id.quest_amenu).setVisibility(View.GONE);
                questMenuButton = questView.findViewById(R.id.quest_amenu_btn);
                questMenuButton.setOnTouchListener(mViewTouchListener);
                questMenuButton.setImageResource(R.drawable.ic_arrow_up);

                questView.findViewById(R.id.quest_page_top).setOnTouchListener(mViewTouchListener);
                questView.findViewById(R.id.quest_page_bottom).setOnTouchListener(mViewTouchListener);
                for (int i = 0; i < pageIndexList.length; i++) {
                    TextView view = questView.findViewById(pageIndexList[i]);
                    view.setText(String.valueOf(i + 1));
                    view.setOnTouchListener(mViewTouchListener);
                }

                for (int i = 1; i <= 5; i++) {
                    TextView view = questView.findViewById(getId(KcaUtils.format("quest_class_%d", i), R.id.class));
                    view.setText(getString(getId(KcaUtils.format("quest_class_%d", i), R.string.class)));
                    view.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                    view.setOnTouchListener(mViewTouchListener);
                }

                layoutParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        getWindowLayoutType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                layoutParams.gravity = Gravity.CENTER;

                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                displayWidth = KcaUtils.getDefaultDisplaySizeInsets(this).size.x;

            } catch (Exception e) {
                active = false;
                error_flag = true;
                sendReport(e, 1);
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        active = false;
        if (layoutView != null) {
            layoutView.setVisibility(View.GONE);
            if (layoutView.getParent() != null) windowManager.removeView(layoutView);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        layoutView = null;
        windowManager = null;
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

    private void updateView(int setViewResult, boolean isreset) {
        if (windowManager != null && setViewResult == 0) {
            if(layoutView.getParent() != null) {
                if (isreset) {
                    windowManager.removeViewImmediate(layoutView);
                    windowManager.addView(layoutView, layoutParams);
                } else {
                    layoutView.invalidate();
                    windowManager.updateViewLayout(layoutView, layoutParams);
                }
            } else {
                windowManager.addView(layoutView, layoutParams);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        JsonObject statProperties = new JsonObject();
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (!KcaService.getServiceStatus()) {
            stopSelf();
        } else if (intent != null && intent.getAction() != null && layoutView != null) {
            if (intent.getAction().equals(REFRESH_QUESTVIEW_ACTION)) {
                int extra = intent.getIntExtra("tab_id", -1);
                updateView(setView(isquestlist, true, extra, currentFilterState), false);
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION)) {
                currentPage = 1;
                updateView(setView(isquestlist, false, 0, currentFilterState), false);
                layoutView.setVisibility(View.VISIBLE);
                active = true;
                statProperties.addProperty("type", "no_reset");
                sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION_NEW)) {
                currentPage = 1;
                updateView(setView(isquestlist, false, 0, currentFilterState), true);
                layoutView.setVisibility(View.VISIBLE);
                active = true;
                statProperties.addProperty("type", "new");
                sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
            } else if (intent.getAction().equals(CLOSE_QUESTVIEW_ACTION)) {
                if (layoutView.getParent() != null) {
                    layoutView.setVisibility(View.GONE);
                    windowManager.removeViewImmediate(layoutView);
                    active = false;
                    statProperties.addProperty("manual", false);
                    sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private View.OnTouchListener popupViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            JsonObject statProperties = new JsonObject();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        int id = v.getId();
                        if (id == R.id.view_qd_head) {
                            questDescPopupView.setVisibility(View.GONE);
                        }
                    }
                    break;
            }
            return true;
        }
    };

    private final View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime = -1;
        private long clickDuration;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            JsonObject statProperties = new JsonObject();
            statProperties.addProperty("manual", true);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        int id = v.getId();
                        if (id == questView.findViewById(R.id.quest_head).getId()) {
                            active = false;
                            layoutView.setVisibility(View.GONE);
                            windowManager.removeViewImmediate(layoutView);
                            sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
                        } else if (id == questMenuButton.getId()) {
                            if (isamenuvisible) {
                                questView.findViewById(R.id.quest_amenu).setVisibility(View.GONE);
                                questMenuButton.setImageResource(R.drawable.ic_arrow_up);
                            } else {
                                questView.findViewById(R.id.quest_amenu).setVisibility(View.VISIBLE);
                                questMenuButton.setImageResource(R.drawable.ic_arrow_down);
                            }
                            isamenuvisible = !isamenuvisible;
                        } else if (id ==  questView.findViewById(R.id.quest_clear).getId()) {
                            questTracker.clearQuestTrack();
                        } else {
                            int total_size = adapter.getCount();
                            for (int i = 0; i < pageIndexList.length; i++) {
                                if (id == questView.findViewById(pageIndexList[i]).getId()) {
                                    int current_page = Integer.parseInt(((TextView) v).getText().toString());
                                    int pos = (current_page - 1) * PAGE_SIZE;
                                    scrollListView(pos);
                                    setTopBottomNavigation(current_page, total_size);
                                }
                            }
                            if (id == questView.findViewById(R.id.quest_page_top).getId()) {
                                scrollListView(0);
                                setTopBottomNavigation(1, total_size);
                            } else if (id == questView.findViewById(R.id.quest_page_bottom).getId()) {
                                int total_page = (total_size - 1) / PAGE_SIZE + 1;
                                int last_idx = Math.max(total_size - PAGE_SIZE, 0);
                                setTopBottomNavigation(total_page, total_size);
                                scrollListView(last_idx);
                            }


                            for (int i = 0; i < 5; i++) {
                                if (id == KcaUtils.getId("quest_class_" + (i+1), R.id.class)) {
                                    for (int j = 0; j < 5; j++) {
                                        ((Chip)questView.findViewById(KcaUtils.getId("quest_class_" + (j+1), R.id.class)))
                                                .setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
                                    }
                                    if (currentFilterState != i) {
                                        // Selection Changed
                                        ((Chip)v).setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(),
                                                KcaUtils.getId(KcaUtils.format("colorQuestCategory%d", filterCategoryList[i]), R.color.class))));
                                        setQuestView(currentQuestList, false, filterCategoryList[i]);
                                        currentFilterState = i;
                                    } else {
                                        setQuestView(currentQuestList, false, -1);
                                        currentFilterState = -1;
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    };

    private void scrollListView(int pos) {
        questList.smoothScrollBy(0, 0);
        questList.smoothScrollToPosition(pos);
        questList.setSelection(pos);
    }

    private void setTopBottomNavigation(int centerPage, int totalItemSize) {
        int totalPage = (totalItemSize - 1) / PAGE_SIZE + 1;
        int startPage = centerPage - 2;

        if (totalPage <= 5) startPage = 1;
        else if (centerPage <= 3) startPage = 1;
        else if (centerPage > totalPage - 4) startPage = totalPage - 4;

        TextView page_title = questView.findViewById(R.id.quest_page);
        page_title.setText(KcaUtils.format(getString(R.string.questview_page), centerPage, totalPage));

        for (int i = 0; i < 5; i++) {
            questView.findViewById(pageIndexList[i])
                    .setVisibility(totalPage > i ? View.VISIBLE : View.INVISIBLE);
            ((TextView) questView.findViewById(pageIndexList[i]))
                    .setText(String.valueOf(startPage + i));
            if (startPage + i == centerPage) {
                ((TextView) questView.findViewById(pageIndexList[i])).setTextColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
            } else {
                ((TextView) questView.findViewById(pageIndexList[i])).setTextColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.white));
            }
        }
    }

    public void setAndShowPopup(JsonObject data) {
        ((TextView) questDescPopupView.findViewById(R.id.view_qd_title))
                .setText(data.get("title").getAsString());
        ((TextView) questDescPopupView.findViewById(R.id.view_qd_text))
                .setText(data.get("detail").getAsString());

        String memo = data.get("memo").getAsString();
        TextView memoView = questDescPopupView.findViewById(R.id.view_qd_memo);
        if (!memo.isEmpty()) {
            memoView.setText(memo);
            memoView.setVisibility(View.VISIBLE);
        } else {
            memoView.setVisibility(View.GONE);
        }

        String rewards = data.get("rewards").getAsString();
        if (!rewards.isEmpty()) {
            ((TextView) questDescPopupView.findViewById(R.id.view_qd_rewards)).setText(rewards);
            questDescPopupView.findViewById(R.id.view_qd_rewards_layout).setVisibility(View.VISIBLE);
        } else {
            questDescPopupView.findViewById(R.id.view_qd_rewards_layout).setVisibility(View.GONE);
        }

        JsonArray materials = data.getAsJsonArray("materials");
        for (int i = 0; i < materials.size(); i++) {
            int value = materials.get(i).getAsInt();
            String view_name = "view_qd_materials_" + (i+1);
            ((TextView) questDescPopupView.findViewById(getId(view_name, R.id.class)))
                    .setText(String.valueOf(value));
        }

        questDescPopupView.setVisibility(View.VISIBLE);
    }

    private void sendReport(Exception e, int type) {
        error_flag = true;
        String data = "";
        if (layoutView != null) layoutView.setVisibility(View.GONE);
        if (api_data == null) data = "[api data is null]";
        else data = api_data.toString();
        KcaDBHelper helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(ERROR_TYPE_QUESTVIEW, "questview", "QV", data, getStringFromException(e));
    }
}
