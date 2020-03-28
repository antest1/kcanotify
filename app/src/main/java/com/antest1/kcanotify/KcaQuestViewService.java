package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.Display;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_QUESTVIEW;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_QUESTVIEW;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;


public class KcaQuestViewService extends Service {
    public static final String REFRESH_QUESTVIEW_ACTION = "refresh_questview";
    public static final String SHOW_QUESTVIEW_ACTION = "show_questview";
    public static final String SHOW_QUESTVIEW_ACTION_NEW = "show_questview_new";
    public static final String CLOSE_QUESTVIEW_ACTION = "close_questview";

    Context contextWithLocale;
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver;
    public static boolean active;
    public static JsonObject api_data;
    private static boolean isamenuvisible = false;
    private static boolean isquestlist = false;
    private int currentPage = 1;
    static boolean error_flag = false;
    final int[] pageIndexList = {R.id.quest_page_1, R.id.quest_page_2, R.id.quest_page_3, R.id.quest_page_4, R.id.quest_page_5};
    final int[] filterCategoryList = {2, 3, 4, 6, 7};
    int currentFilterState = -1;
    JsonArray currentQuestList = new JsonArray();

    public KcaDBHelper helper;
    private KcaQuestTracker questTracker;
    private View mView;
    private WindowManager mManager;

    int displayWidth = 0;

    WindowManager.LayoutParams mParams;
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

    public static boolean getStatus() {
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

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @SuppressLint("DefaultLocale")
    public void setQuestView(JsonArray api_list, boolean checkValid, int filter) {
        questDescPopupView.setVisibility(View.GONE);
        adapter.setListViewItemList(api_list, filter);
        adapter.notifyDataSetChanged();
        questList.setAdapter(adapter);
        scrollListView(0);
        setTopBottomNavigation(1, adapter.getCount());
    }

    public int setView(boolean isquestlist, boolean checkValid, int tab_id) {
        return setView(isquestlist, checkValid, tab_id, 0);
    }

    public int setView(boolean isquestlist, boolean checkValid, int tab_id, int filter_id) {
        try {
            Log.e("KCA", "QuestView setView " + String.valueOf(isquestlist));
            error_flag = false;

            int api_count = 0;
            if (isquestlist && api_data != null) {
                Log.e("KCA-Q", api_data.toString());
                if(api_data.has("api_list") && api_data.get("api_list").isJsonArray()) {
                    currentQuestList = api_data.getAsJsonArray("api_list");
                    api_count = api_data.get("api_count").getAsInt();
                }
            } else {
                currentQuestList = helper.getCurrentQuestList();
                api_count = currentQuestList.size();
            }
            Log.e("KCA", currentQuestList.toString());
            if (checkValid) {
                questTracker.clearInvalidQuestTrack();
                // helper.checkValidQuest(api_disp_page, api_page_count, api_list, tab_id);
            }
            setQuestView(currentQuestList, checkValid, filter_id);
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
        } else {
            try {
                active = true;
                helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
                questTracker = new KcaQuestTracker(getApplicationContext(), null, KCANOTIFY_QTDB_VERSION);
                contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
                broadcaster = LocalBroadcastManager.getInstance(this);
                //mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                mInflater = LayoutInflater.from(contextWithLocale);
                mView = mInflater.inflate(R.layout.view_quest_list_v2, null);
                KcaUtils.resizeFullWidthView(getApplicationContext(), mView);
                mView.setVisibility(View.GONE);

                questView = mView.findViewById(R.id.questview);
                questView.findViewById(R.id.quest_head).setOnTouchListener(mViewTouchListener);
                questDescPopupView = mView.findViewById(R.id.quest_desc_popup);
                questDescPopupView.setVisibility(View.GONE);
                questDescPopupView.findViewById(R.id.view_qd_head).setOnTouchListener(popupViewTouchListener);

                adapter = new KcaQuestListAdpater(KcaQuestViewService.this, questTracker);
                questList = mView.findViewById(R.id.quest_list);
                questList.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        TextView page_title = questView.findViewById(R.id.quest_page);
                        page_title.setText(getStringWithLocale(R.string.questview_page)
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
                questMenuButton.setImageResource(R.mipmap.ic_arrow_up);

                questView.findViewById(R.id.quest_page_top).setOnTouchListener(mViewTouchListener);
                questView.findViewById(R.id.quest_page_bottom).setOnTouchListener(mViewTouchListener);
                for (int i = 0; i < pageIndexList.length; i++) {
                    TextView view = questView.findViewById(pageIndexList[i]);
                    view.setText(String.valueOf(i + 1));
                    view.setOnTouchListener(mViewTouchListener);
                }

                for (int i = 1; i <= 5; i++) {
                    TextView view = questView.findViewById(getId(KcaUtils.format("quest_class_%d", i), R.id.class));
                    view.setText(getStringWithLocale(getId(KcaUtils.format("quest_class_%d", i), R.string.class)));
                    view.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                    view.setOnTouchListener(mViewTouchListener);
                }

                mParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        getWindowLayoutType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                mParams.gravity = Gravity.CENTER;

                mManager = (WindowManager) getSystemService(WINDOW_SERVICE);

                Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                displayWidth = size.x;

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
        if (mView != null) {
            mView.setVisibility(View.GONE);
            if (mView.getParent() != null) mManager.removeView(mView);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        mView = null;
        mManager = null;
        super.onDestroy();
    }

    private void updateView(int setViewResult, boolean isreset) {
        if (mManager != null && setViewResult == 0) {
            if(mView.getParent() != null) {
                if (isreset) {
                    mManager.removeViewImmediate(mView);
                    mManager.addView(mView, mParams);
                } else {
                    mView.invalidate();
                    mManager.updateViewLayout(mView, mParams);
                }
            } else {
                mManager.addView(mView, mParams);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        JsonObject statProperties = new JsonObject();
        if (intent != null && intent.getAction() != null && mView != null) {
            if (intent.getAction().equals(REFRESH_QUESTVIEW_ACTION)) {
                int extra = intent.getIntExtra("tab_id", -1);
                updateView(setView(isquestlist, true, extra), false);
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION)) {
                currentPage = 1;
                updateView(setView(isquestlist, false, 0), false);
                mView.setVisibility(View.VISIBLE);
                statProperties.addProperty("type", "no_reset");
                sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION_NEW)) {
                currentPage = 1;
                updateView(setView(isquestlist, false, 0), true);
                mView.setVisibility(View.VISIBLE);
                statProperties.addProperty("type", "new");
                sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
            } else if (intent.getAction().equals(CLOSE_QUESTVIEW_ACTION)) {
                if (mView.getParent() != null) {
                    mView.setVisibility(View.GONE);
                    mManager.removeViewImmediate(mView);
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

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
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
                            mView.setVisibility(View.GONE);
                            mManager.removeViewImmediate(mView);
                            sendUserAnalytics(getApplicationContext(), OPEN_QUESTVIEW, statProperties);
                        } else if (id == questMenuButton.getId()) {
                            if (isamenuvisible) {
                                questView.findViewById(R.id.quest_amenu).setVisibility(View.GONE);
                                questMenuButton.setImageResource(R.mipmap.ic_arrow_up);
                            } else {
                                questView.findViewById(R.id.quest_amenu).setVisibility(View.VISIBLE);
                                questMenuButton.setImageResource(R.mipmap.ic_arrow_down);
                            }
                            isamenuvisible = !isamenuvisible;
                        } else if (id ==  questView.findViewById(R.id.quest_clear).getId()) {
                            questTracker.clearQuestTrack();
                        } else {
                            int total_size = adapter.getCount();
                            for (int i = 0; i < pageIndexList.length; i++) {
                                if (id == questView.findViewById(pageIndexList[i]).getId()) {
                                    int current_page = Integer.parseInt(((TextView) v).getText().toString());
                                    int pos = (current_page - 1) * 5;
                                    scrollListView(pos);
                                    setTopBottomNavigation(current_page, total_size);
                                }
                            }
                            if (id == questView.findViewById(R.id.quest_page_top).getId()) {
                                scrollListView(0);
                                setTopBottomNavigation(1, total_size);
                            } else if (id == questView.findViewById(R.id.quest_page_bottom).getId()) {
                                int total_page = (total_size - 1) / 5 + 1;
                                int last_idx = Math.max(total_size - 5, 0);
                                setTopBottomNavigation(total_page, total_size);
                                scrollListView(last_idx);
                            }


                            for (int i = 0; i < 5; i++) {
                                if (id == KcaUtils.getId("quest_class_" + (i+1), R.id.class)) {
                                    for (int j = 0; j < 5; j++) {
                                        questView.findViewById(KcaUtils.getId("quest_class_" + (j+1), R.id.class))
                                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetInfoBtn));
                                    }
                                    if (currentFilterState != i) {
                                        v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                                KcaUtils.getId(KcaUtils.format("colorQuestCategory%d", filterCategoryList[i]), R.color.class)));
                                        setQuestView(currentQuestList, false, filterCategoryList[i]);
                                        currentFilterState = i;
                                    } else {
                                        setQuestView(currentQuestList, false, 0);
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
        int totalPage = (totalItemSize - 1) / 5 + 1;
        int startPage = centerPage - 2;
        if (centerPage <= 3) startPage = 1;
        else if (centerPage > totalPage - 4) startPage = totalPage - 4;

        TextView page_title = questView.findViewById(R.id.quest_page);
        page_title.setText(KcaUtils.format(getStringWithLocale(R.string.questview_page), centerPage, totalPage));

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

    public void setAndShowPopup(String title, String content) {
        TextView qdTitle = questDescPopupView.findViewById(R.id.view_qd_title);
        TextView qdContent = questDescPopupView.findViewById(R.id.view_qd_text);
        qdTitle.setText(title);
        qdContent.setText(content);
        questDescPopupView.setVisibility(View.VISIBLE);
    }

    private void sendReport(Exception e, int type) {
        error_flag = true;
        String data = "";
        if (mView != null) mView.setVisibility(View.GONE);
        if (api_data == null) data = "[api data is null]";
        else data = api_data.toString();
        KcaDBHelper helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(ERROR_TYPE_QUESTVIEW, "questview", "QV", data, getStringFromException(e));
    }
}
