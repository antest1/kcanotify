package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;

import static com.antest1.kcanotify.KcaApiData.kcQuestInfoData;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_VIEW_LIST;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;


public class KcaQuestViewService extends Service {
    public static final String REFRESH_QUESTVIEW_ACTION = "refresh_questview";
    public static final String SHOW_QUESTVIEW_ACTION = "show_questview";
    public static final String SHOW_QUESTVIEW_ACTION_CURRENT = "show_questview_current";

    Context contextWithLocale;
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver;
    public static boolean active;
    public static JsonObject api_data;
    private static boolean isquestlist = false;
    private static int currentPage = 1;
    private static int prevpagelastno = -1;
    private static final int maxPage = 2; // Max 6 quest at parallel
    public KcaDBHelper helper;

    static boolean error_flag = false;

    private View mView;
    private WindowManager mManager;

    int displayWidth = 0;

    WindowManager.LayoutParams mParams;
    ScrollView questview;
    TextView questprev, questnext;
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

    public static void setPrevPageLastNo(int no) { prevpagelastno = no; }

    public static int getPrevPageLastNo() { return prevpagelastno; }

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
        if (api_page_count > 0) {
            ((TextView) questview.findViewById(R.id.quest_page))
                    .setText(String.format(getStringWithLocale(R.string.questview_page), api_disp_page, api_page_count));
            if (checkValid) helper.checkValidQuest(api_disp_page, api_page_count, api_list);
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

                    String api_title = String.format("[%s] %s", api_no, item.get("api_title").getAsString());
                    String api_detail = item.get("api_detail").getAsString();
                    if (kcQuestInfoData.has(api_no)) {
                        String code = kcQuestInfoData.getAsJsonObject(api_no).get("code").getAsString();
                        String name = kcQuestInfoData.getAsJsonObject(api_no).get("name").getAsString();
                        api_title = String.format("[%s] %s", code, name);
                        api_detail = kcQuestInfoData.getAsJsonObject(api_no).get("desc").getAsString();
                        if (kcQuestInfoData.getAsJsonObject(api_no).has("memo")) { // Temporary
                            String memo = kcQuestInfoData.getAsJsonObject(api_no).get("memo").getAsString();
                            api_detail = api_detail.concat(" (").concat(memo).concat(")");
                        }
                    }

                    ((TextView) questview.findViewById(getId(String.format("quest%d_category", index), R.id.class)))
                            .setText(getStringWithLocale(getId(String.format("quest_category_%d", api_category), R.string.class)));
                    questview.findViewById(getId(String.format("quest%d_category", index), R.id.class))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                    getId(String.format("colorQuestCategory%d", api_category), R.color.class)));

                    ((TextView) questview.findViewById(getId(String.format("quest%d_type", index), R.id.class)))
                            .setText(getStringWithLocale(getId(String.format("quest_type_%d", api_type), R.string.class)));
                    questview.findViewById(getId(String.format("quest%d_type", index), R.id.class))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                    getId(String.format("colorQuestType%d", api_type), R.color.class)));

                    ((TextView) questview.findViewById(getId(String.format("quest%d_name", index), R.id.class))).setText(api_title);
                    ((TextView) questview.findViewById(getId(String.format("quest%d_desc", index), R.id.class))).setText(api_detail);
                    ((TextView) questview.findViewById(getId(String.format("quest%d_desc", index), R.id.class))).setMovementMethod(new ScrollingMovementMethod());

                    if (api_progress != 0) {
                        ((TextView) questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class)))
                                .setText(getStringWithLocale(getId(String.format("quest_progress_%d", api_progress), R.string.class)));
                        questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                                        getId(String.format("colorQuestProgress%d", api_progress), R.color.class)));
                        questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class)).setVisibility(View.GONE);
                    }

                    ((TextView) questview.findViewById(getId(String.format("quest%d_name", index), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(),
                                    getId(String.format("colorQuestState%d", api_state), R.color.class)));

                    questview.findViewById(getId(String.format("quest%d", index), R.id.class)).setVisibility(View.VISIBLE);
                } else {
                    ((TextView) questview.findViewById(getId(String.format("quest%d_category", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(String.format("quest%d_type", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(String.format("quest%d_name", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(String.format("quest%d_desc", index), R.id.class))).setText("");
                    ((TextView) questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class))).setText("");
                    questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class)).setVisibility(View.GONE);
                    questview.findViewById(getId(String.format("quest%d", index), R.id.class)).setVisibility(View.INVISIBLE);
                }
            }
        } else {
            ((TextView) questview.findViewById(R.id.quest_page))
                    .setText(getStringWithLocale(R.string.questview_nopage));
            for (int i = 0; i < 5; i++) {
                int index = i + 1;
                ((TextView) questview.findViewById(getId(String.format("quest%d_category", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(String.format("quest%d_type", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(String.format("quest%d_name", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(String.format("quest%d_desc", index), R.id.class))).setText("");
                ((TextView) questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class))).setText("");
                questview.findViewById(getId(String.format("quest%d_progress", index), R.id.class)).setVisibility(View.GONE);
                questview.findViewById(getId(String.format("quest%d", index), R.id.class)).setVisibility(View.INVISIBLE);
            }
        }
    }

    public int setView(boolean isquestlist, boolean checkValid) {
        try {
            Log.e("KCA", "QuestView setView " + String.valueOf(isquestlist));
            error_flag = false;
            //api_data = KcaViewButtonService.getCurrentApiData();
            int prevnextVisibility = isquestlist ? View.GONE : View.VISIBLE;
            questprev.setVisibility(prevnextVisibility);
            questnext.setVisibility(prevnextVisibility);

            int api_page_count, api_disp_page;
            JsonArray api_list = new JsonArray();
            if (isquestlist && api_data != null) {
                api_page_count = api_data.get("api_page_count").getAsInt();
                api_disp_page = api_data.get("api_disp_page").getAsInt();
                api_list = api_data.getAsJsonArray("api_list");
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
                questnext.setVisibility(api_page_count == 0 || api_disp_page == api_page_count  ? View.GONE : View.VISIBLE);

            }
            Log.e("KCA", api_list.toString());
            setQuestView(api_disp_page, api_page_count, api_list, checkValid);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            //sendReport(e, 0);
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
                contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
                broadcaster = LocalBroadcastManager.getInstance(this);
                //mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                mInflater = LayoutInflater.from(contextWithLocale);
                mView = mInflater.inflate(R.layout.view_quest_list, null);
                mView.setVisibility(View.GONE);

                questview = (ScrollView) mView.findViewById(R.id.questview);
                questview.findViewById(R.id.quest_head).setOnTouchListener(mViewTouchListener);

                questprev = (TextView) questview.findViewById(R.id.quest_prev);
                questprev.setOnTouchListener(mViewTouchListener);
                questprev.setVisibility(View.GONE);

                questnext = (TextView) questview.findViewById(R.id.quest_next);
                questnext.setOnTouchListener(mViewTouchListener);
                questnext.setVisibility(View.GONE);

                mParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
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
                //sendReport(e, 1);
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        active = false;
        if(mView != null) mView.setVisibility(View.GONE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(REFRESH_QUESTVIEW_ACTION)) {
                boolean checkValid = intent.getIntExtra("tab_id", -1) % 9 == 0;
                int setViewResult = setView(isquestlist, checkValid);
                if (setViewResult == 0) {
                    mView.invalidate();
                    mManager.updateViewLayout(mView, mParams);
                }
                Log.e("KCA", String.valueOf(intent.getIntExtra("tab_id", -2)));

            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION)) {
                currentPage = 1;
                if (!isquestlist) {
                    int setViewResult = setView(isquestlist, false);
                    if (setViewResult == 0) {
                        mView.invalidate();
                        mManager.updateViewLayout(mView, mParams);
                    }
                }
                mView.setVisibility(View.VISIBLE);
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
                        } else if (id == questprev.getId() || id == questnext.getId()) {
                            if (id == questprev.getId() && currentPage > 1) {
                                currentPage -= 1;
                            } else if (id == questnext.getId() && currentPage < maxPage) {
                                currentPage += 1;
                            }
                            int setViewResult = setView(isquestlist, false);
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


    /*
    private void sendReport(Exception e, int type) {
        error_flag = true;
        mView.setVisibility(View.GONE);
        String app_version = BuildConfig.VERSION_NAME;
        String token = "not_used_now";
        String kca_url = "";
        try {
            kca_url = URLEncoder.encode(KCA_MSG_BATTLE_VIEW_REFRESH, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.battleview_error), Toast.LENGTH_SHORT).show();
        String dataSendUrl = String.format(getStringWithLocale(R.string.errorlog_battle_link), token, kca_url, "BV", app_version);
        AjaxCallback<String> cb = new AjaxCallback<String>() {
            @Override
            public void callback(String url, String data, AjaxStatus status) {
                // do nothing
            }
        };
        JsonObject sendData = new JsonObject();
        sendData.addProperty("data", api_data.toString());
        sendData.addProperty("error", getStringFromException(e));
        String sendDataString = sendData.toString();

        AQuery aq = new AQuery(KcaQuestViewService.this);
        cb.header("Referer", "app:/KCA/");
        cb.header("Content-Type", "application/x-www-form-urlencoded");
        HttpEntity entity = new ByteArrayEntity(sendDataString.getBytes());
        cb.param(AQuery.POST_ENTITY, entity);
        aq.ajax(dataSendUrl, String.class, cb);
    }
    */
}
