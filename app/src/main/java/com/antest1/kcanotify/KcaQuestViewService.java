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
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.R.attr.id;
import static com.antest1.kcanotify.KcaApiData.getQuestTrackInfo;
import static com.antest1.kcanotify.KcaApiData.isQuestTrackable;
import static com.antest1.kcanotify.KcaApiData.kcQuestInfoData;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_QUESTVIEW;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_QTDB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCAQSYNC_USE;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.joinStr;


public class KcaQuestViewService extends Service {
    public static final String REFRESH_QUESTVIEW_ACTION = "refresh_questview";
    public static final String SHOW_QUESTVIEW_ACTION = "show_questview";
    public static final String SHOW_QUESTVIEW_ACTION_NEW = "show_questview_new";
    public static final String CLOSE_QUESTVIEW_ACTION = "close_questview";

    public static final float PROGRESS_1 = 0.5f;
    public static final float PROGRESS_2 = 0.8f;

    Context contextWithLocale;
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver;
    public static boolean active;
    public static JsonObject api_data;
    private static boolean isamenuvisible = false;
    private static boolean isquestlist = false;
    private static int currentPage = 1;
    private static int prevpagelastno = -1;
    private static final int maxPage = 2; // Max 6 quest at parallel
    public KcaDBHelper helper;
    private KcaQuestTracker questTracker;

    static boolean error_flag = false;

    private View mView;
    private WindowManager mManager;

    int displayWidth = 0;

    WindowManager.LayoutParams mParams;
    ScrollView questview;
    TextView questprev, questnext;
    ImageView questclear, questamenubtn;
    ImageView exitbtn;

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

    public static void setPrevPageLastNo(int no) {
        prevpagelastno = no;
    }

    public static int getPrevPageLastNo() {
        return prevpagelastno;
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
    public void setQuestView(int api_disp_page, int api_page_count, JsonArray api_list, boolean checkValid) {
        if (api_page_count > 0 && api_list.size() > 0) {
            ((TextView) questview.findViewById(R.id.quest_page))
                    .setText(KcaUtils.format(getStringWithLocale(R.string.questview_page), api_disp_page, api_page_count));
            for (int i = 0; i < api_list.size(); i++) {
                int index = i + 1;
                JsonElement api_list_item = api_list.get(i);
                if (api_list_item.isJsonObject()) {
                    JsonObject item = api_list_item.getAsJsonObject();
                    String api_no = item.get("api_no").getAsString();
                    int api_category = item.get("api_category").getAsInt();
                    int api_type = item.get("api_type").getAsInt();

                    int api_progress = item.get("api_progress_flag").getAsInt();
                    int api_state = item.get("api_state").getAsInt();

                    String api_title = KcaUtils.format("[%s] %s", api_no, item.get("api_title").getAsString());
                    String api_detail = item.get("api_detail").getAsString();
                    if (kcQuestInfoData.has(api_no)) {
                        String code = kcQuestInfoData.getAsJsonObject(api_no).get("code").getAsString();
                        String name = kcQuestInfoData.getAsJsonObject(api_no).get("name").getAsString();
                        api_title = KcaUtils.format("[%s] %s", code, name);
                        api_detail = kcQuestInfoData.getAsJsonObject(api_no).get("desc").getAsString();
                        if (kcQuestInfoData.getAsJsonObject(api_no).has("memo")) { // Temporary
                            String memo = kcQuestInfoData.getAsJsonObject(api_no).get("memo").getAsString();
                            api_detail = api_detail.concat(" (").concat(memo).concat(")");
                        }
                    }

                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_category", index), R.id.class)))
                            .setText(getStringWithLocale(getId(KcaUtils.format("quest_category_%d", api_category), R.string.class)));
                    questview.findViewById(getId(KcaUtils.format("quest%d_category", index), R.id.class))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                    getId(KcaUtils.format("colorQuestCategory%d", api_category), R.color.class)));

                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_type", index), R.id.class)))
                            .setText(getStringWithLocale(getId(KcaUtils.format("quest_type_%d", api_type), R.string.class)));
                    questview.findViewById(getId(KcaUtils.format("quest%d_type", index), R.id.class))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                    getId(KcaUtils.format("colorQuestType%d", api_type), R.color.class)));

                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_name", index), R.id.class))).setText(api_title);
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_desc", index), R.id.class))).setText(api_detail);
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_desc", index), R.id.class))).setMovementMethod(new ScrollingMovementMethod());

                    if (api_progress != 0) {
                        ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class)))
                                .setText(getStringWithLocale(getId(KcaUtils.format("quest_progress_%d", api_progress), R.string.class)));
                        questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                        getId(KcaUtils.format("colorQuestProgress%d", api_progress), R.color.class)));
                        questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class)).setVisibility(View.GONE);
                    }

                    if (isQuestTrackable(api_no)) {
                        boolean noshowflag = false;
                        questTracker.test();
                        List<String> trackinfo_list = new ArrayList<>();
                        String trackinfo_text = "";
                        JsonObject questTrackInfo = getQuestTrackInfo(api_no);
                        if (questTrackInfo != null) {
                            JsonArray trackData = questTracker.getQuestTrackInfo(api_no);
                            JsonArray trackCond = questTrackInfo.getAsJsonArray("cond");

                            if (trackData.size() == 1) {
                                JsonArray updatevalue = new JsonArray();
                                if (api_progress == 1 && trackData.get(0).getAsFloat() < trackCond.get(0).getAsFloat() * PROGRESS_1) {
                                    updatevalue.add((int) (Math.ceil(trackCond.get(0).getAsFloat() * PROGRESS_1)));
                                } else if (api_progress == 2) {
                                    int calculated_value = (int) Math.ceil(trackCond.get(0).getAsFloat() * PROGRESS_2);
                                    if (calculated_value >= trackCond.get(0).getAsFloat()) {
                                        updatevalue.add(trackCond.get(0).getAsInt() - 1);
                                    } else if (trackData.get(0).getAsFloat() < trackCond.get(0).getAsFloat() * PROGRESS_2) {
                                        updatevalue.add(calculated_value);
                                    }
                                } else if (api_state == 3) {
                                    updatevalue.add(trackCond.get(0).getAsInt());
                                }
                                if (updatevalue.size() > 0) {
                                    questTracker.updateQuestTrackValueWithId(api_no, updatevalue);
                                    trackData = questTracker.getQuestTrackInfo(api_no);
                                }
                            }

                            for (int n = 0; n < trackData.size(); n++) {
                                int cond = trackCond.get(n).getAsInt() - KcaQuestTracker.getInitialCondValue(api_no);
                                int val = trackData.get(n).getAsInt() - KcaQuestTracker.getInitialCondValue(api_no);
                                Log.e("KCA-QV", api_no + " " + String.valueOf(val) + " " + String.valueOf(cond));
                                trackinfo_list.add(KcaUtils.format("%d/%d", Math.min(val, cond), cond));
                            }
                            if (trackinfo_list.size() > 0) {
                                trackinfo_text = joinStr(trackinfo_list, ", ");
                            } else {
                                noshowflag = true;
                            }

                            ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_progress_track", index), R.id.class))).setText(trackinfo_text);
                            questview.findViewById(getId(KcaUtils.format("quest%d_progress_track", index), R.id.class)).setVisibility(View.VISIBLE);
                        }
                        if (noshowflag) {
                            questview.findViewById(getId(KcaUtils.format("quest%d_progress_track", index), R.id.class)).setVisibility(View.GONE);
                        }
                    } else {
                        questview.findViewById(getId(KcaUtils.format("quest%d_progress_track", index), R.id.class)).setVisibility(View.GONE);
                    }


                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_name", index), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(),
                                    getId(KcaUtils.format("colorQuestState%d", api_state), R.color.class)));

                    questview.findViewById(getId(KcaUtils.format("quest%d", index), R.id.class)).setVisibility(View.VISIBLE);
                } else {
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_category", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_type", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_name", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_desc", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class))).setText("");
                    questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class)).setVisibility(View.GONE);
                    questview.findViewById(getId(KcaUtils.format("quest%d", index), R.id.class)).setVisibility(View.INVISIBLE);
                }
            }
        } else {
            ((TextView) questview.findViewById(R.id.quest_page))
                    .setText(getStringWithLocale(R.string.questview_nopage));
            for (int i = 0; i < 5; i++) {
                int index = i + 1;
                ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_category", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_type", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_name", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_desc", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class))).setText("");
                questview.findViewById(getId(KcaUtils.format("quest%d_progress", index), R.id.class)).setVisibility(View.GONE);
                questview.findViewById(getId(KcaUtils.format("quest%d", index), R.id.class)).setVisibility(View.INVISIBLE);
            }
        }
    }

    public int setView(boolean isquestlist, boolean checkValid, int tab_id) {
        try {
            Log.e("KCA", "QuestView setView " + String.valueOf(isquestlist));
            error_flag = false;
            //api_data = KcaViewButtonService.getCurrentApiData();
            int prevnextVisibility = isquestlist ? View.GONE : View.VISIBLE;
            questprev.setVisibility(prevnextVisibility);
            questnext.setVisibility(prevnextVisibility);
            TextView page_title = questview.findViewById(R.id.quest_page);
            if (KcaUtils.getBooleanPreferences(getApplicationContext(), PREF_KCAQSYNC_USE)) {
                if (helper.checkQuestListValid()) {
                    page_title.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorQuestCheckedTrue));
                } else {
                    page_title.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorQuestCheckedFalse));
                }
            } else {
                page_title.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            }

            int api_page_count, api_disp_page;
            JsonArray api_list = new JsonArray();
            if (isquestlist && api_data != null) {
                Log.e("KCA-Q", api_data.toString());
                api_page_count = api_data.get("api_page_count").getAsInt();
                api_disp_page = api_data.get("api_disp_page").getAsInt();
                if(api_data.has("api_list") && api_data.get("api_list").isJsonArray()) {
                    api_list = api_data.getAsJsonArray("api_list");
                }
            } else {
                JsonArray raw_api_list = helper.getCurrentQuestList();
                api_disp_page = currentPage;
                if (raw_api_list.size() > 0) api_page_count = (raw_api_list.size() - 1) / 5 + 1;
                else api_page_count = 0;
                for (int i = (api_disp_page - 1) * 5; i < Math.min(api_disp_page * 5, raw_api_list.size()); i++) {
                    api_list.add(raw_api_list.get(i).getAsJsonObject());
                }
                if (api_list.size() < 5) {
                    for (int i = 0; api_list.size() < 5; i++) {
                        api_list.add(-1);
                    }
                }
                questprev.setVisibility(api_disp_page == 1 ? View.GONE : View.VISIBLE);
                questnext.setVisibility(api_page_count == 0 || api_disp_page == api_page_count ? View.GONE : View.VISIBLE);
            }
            Log.e("KCA", api_list.toString());
            if (checkValid) {
                questTracker.clearInvalidQuestTrack();
                helper.checkValidQuest(api_disp_page, api_page_count, api_list, tab_id);
            }
            setQuestView(api_disp_page, api_page_count, api_list, checkValid);
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
                mView = mInflater.inflate(R.layout.view_quest_list, null);
                KcaUtils.resizeFullWidthView(getApplicationContext(), mView);
                mView.setVisibility(View.GONE);

                questview = (ScrollView) mView.findViewById(R.id.questview);
                questview.findViewById(R.id.quest_head).setOnTouchListener(mViewTouchListener);

                questprev = (TextView) questview.findViewById(R.id.quest_prev);
                questprev.setOnTouchListener(mViewTouchListener);
                questprev.setVisibility(View.GONE);

                questnext = (TextView) questview.findViewById(R.id.quest_next);
                questnext.setOnTouchListener(mViewTouchListener);
                questnext.setVisibility(View.GONE);

                questclear = questview.findViewById(R.id.quest_clear);
                questclear.setOnTouchListener(mViewTouchListener);

                isamenuvisible = false;
                questview.findViewById(R.id.quest_amenu).setVisibility(View.GONE);
                questamenubtn = questview.findViewById(R.id.quest_amenu_btn);
                questamenubtn.setOnTouchListener(mViewTouchListener);
                questamenubtn.setImageResource(R.mipmap.ic_arrow_up);

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
        if (intent != null && intent.getAction() != null && mView != null) {
            if (intent.getAction().equals(REFRESH_QUESTVIEW_ACTION)) {
                int extra = intent.getIntExtra("tab_id", -1);
                boolean checkValid = extra % 9 == 0;
                updateView(setView(isquestlist, true, extra), false);
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION)) {
                currentPage = 1;
                updateView(setView(isquestlist, false, 0), false);
                mView.setVisibility(View.VISIBLE);
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION_NEW)) {
                currentPage = 1;
                updateView(setView(isquestlist, false, 0), true);
                mView.setVisibility(View.VISIBLE);
            } else if (intent.getAction().equals(CLOSE_QUESTVIEW_ACTION)) {
                if (mView.getParent() != null) {
                    mView.setVisibility(View.GONE);
                    mManager.removeViewImmediate(mView);
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
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        int id = v.getId();
                        if (id == questview.findViewById(R.id.quest_head).getId()) {
                            mView.setVisibility(View.GONE);
                            mManager.removeViewImmediate(mView);
                        } else if (id == questamenubtn.getId()) {
                            if (isamenuvisible) {
                                questview.findViewById(R.id.quest_amenu).setVisibility(View.GONE);
                                questamenubtn.setImageResource(R.mipmap.ic_arrow_up);
                            } else {
                                questview.findViewById(R.id.quest_amenu).setVisibility(View.VISIBLE);
                                questamenubtn.setImageResource(R.mipmap.ic_arrow_down);
                            }
                            isamenuvisible = !isamenuvisible;
                        } else if (id == questclear.getId()) {
                            questTracker.clearQuestTrack();
                        } else if (id == questprev.getId() || id == questnext.getId()) {
                            if (id == questprev.getId() && currentPage > 1) {
                                currentPage -= 1;
                            } else if (id == questnext.getId() && currentPage < maxPage) {
                                currentPage += 1;
                            }
                            int setViewResult = setView(isquestlist, false, 0);
                            if (setViewResult == 0) {
                                mView.invalidate();
                                mManager.updateViewLayout(mView, mParams);
                            }
                        }
                    }
                    break;
            }
            return true;
        }
    };


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
