package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
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
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_VIEW_LIST;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaViewButtonService.REFRESH_QUESTVIEW_ACTION;
import static com.antest1.kcanotify.KcaViewButtonService.SHOW_QUESTVIEW_ACTION;

public class KcaQuestViewService extends Service {
    Context contextWithLocale;
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver;
    public static boolean active;
    public static JsonObject api_data;

    static boolean error_flag = false;

    private View mView;
    private WindowManager mManager;

    int displayWidth = 0;

    WindowManager.LayoutParams mParams;
    ScrollView questview;
    ImageView exitbtn;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean getStatus() {
        return active;
    }

    public static void setApiData(JsonObject data) {
        api_data = data;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @SuppressLint("DefaultLocale")
    public void setQuestview() {
        if (api_data != null) {
            int api_page_count = api_data.get("api_page_count").getAsInt();
            int api_disp_page = api_data.get("api_disp_page").getAsInt();
            ((TextView) questview.findViewById(R.id.quest_page))
                    .setText(String.format(getStringWithLocale(R.string.questview_page), api_disp_page, api_page_count));

            JsonArray api_list = api_data.getAsJsonArray("api_list");
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

                    String api_title = item.get("api_title").getAsString();
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
        }
    }


    public int setView() {
        try {
            error_flag = false;
            //api_data = KcaViewButtonService.getCurrentApiData();
            Log.e("KCA", api_data.toString());
            setQuestview();
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
        try {
            active = true;
            contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
            broadcaster = LocalBroadcastManager.getInstance(this);
            //mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInflater = LayoutInflater.from(contextWithLocale);
            mView = mInflater.inflate(R.layout.view_quest_list, null);
            questview = (ScrollView) mView.findViewById(R.id.questview);
            questview.findViewById(R.id.quest_head).setOnTouchListener(mViewTouchListener);

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


    @Override
    public void onDestroy() {
        active = false;
        mView.setVisibility(View.GONE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals(REFRESH_QUESTVIEW_ACTION)) {
                int setViewResult = setView();
                if (setViewResult == 0) {
                    if (KcaViewButtonService.getClickCount() == 0) {
                        mView.setVisibility(View.GONE);
                    }
                    mView.invalidate();
                    mManager.updateViewLayout(mView, mParams);
                }
            } else if (intent.getAction().equals(SHOW_QUESTVIEW_ACTION)) {
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
                        mView.setVisibility(View.GONE);
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
