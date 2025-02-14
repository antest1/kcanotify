package com.antest1.kcanotify;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_AKASHI_STARLIST;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getJapanCalendarInstance;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;
import static com.antest1.kcanotify.KcaUtils.showDataLoadErrorToast;


public class KcaAkashiViewService extends BaseService {
    public static final String REFRESH_AKASHIVIEW_ACTION = "refresh_akashiview";
    public static final String SHOW_AKASHIVIEW_ACTION = "show_akashiview";
    public static final String SHOW_AKASHIVIEW_ACTION_CURRENT = "show_akashiview_current";

    LayoutInflater mInflater;
    public static JsonObject api_data;
    public static JsonObject akashiData, akashiDay;
    private static boolean isSafeChecked = false;
    private static boolean isStarChecked = false;
    private static int currentPage = 1;
    private static final int maxPage = 2; // Max 6 quest at parallel
    public KcaDBHelper dbHelper;

    static boolean error_flag = false;

    private View akashiLayoutView;
    private WindowManager windowManager;

    int displayWidth = 0;

    WindowManager.LayoutParams mParams;
    TextView akashiview_gtd, akashiview_star;
    ListView akashiview_list;
    KcaAkashiListViewAdpater2 adapter;
    ArrayList<KcaAkashiListViewItem> listViewItemList;

    public static boolean active = false;
    public static boolean isActive() {
        return active;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int setView() {
        try {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
            ((TextView) akashiLayoutView.findViewById(R.id.akashiview_day))
                    .setText(getString(getId("akashi_term_day_".concat(String.valueOf(day)), R.string.class)));
            listViewItemList = new ArrayList<>();
            int akashiDataLoadingFlag = getAkashiDataFromStorage();
            if (akashiDataLoadingFlag != 1) {
                Toast.makeText(getApplicationContext(), "Error Loading Akashi Data", Toast.LENGTH_LONG).show();
            } else if (KcaApiData.getKcItemStatusById(2, "name") == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.kca_toast_get_data_at_settings_2), Toast.LENGTH_LONG).show();
            } else {
                loadTodayAkashiList(isSafeChecked);
                resetListView(true);
            }
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
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        }
        try {
            active = true;
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);

            mInflater = LayoutInflater.from(this);
            akashiLayoutView = mInflater.inflate(R.layout.view_akashi_list, null);
            KcaUtils.resizeFullWidthView(getApplicationContext(), akashiLayoutView.findViewById(R.id.akashiviewpanel));
            akashiLayoutView.setVisibility(View.GONE);
            akashiLayoutView.findViewById(R.id.akashiview_head).setOnTouchListener(mViewTouchListener);

            akashiview_gtd = akashiLayoutView.findViewById(R.id.akashiview_gtd);
            akashiview_gtd.setOnTouchListener(mViewTouchListener);
            akashiview_gtd.setText(getString(R.string.aa_btn_safe_state0));
            akashiview_gtd.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

            akashiview_star = akashiLayoutView.findViewById(R.id.akashiview_star);
            akashiview_star.setText(getString(R.string.aa_btn_star0));
            akashiview_star.setOnTouchListener(mViewTouchListener);

            akashiview_list = akashiLayoutView.findViewById(R.id.akashiview_list);

            adapter = new KcaAkashiListViewAdpater2();
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            mParams.gravity = Gravity.CENTER;

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            displayWidth = KcaUtils.getDefaultDisplaySizeInsets(this).size.x;
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
        akashiLayoutView.setVisibility(View.GONE);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowManager != null && checkLayoutExist()) {
            windowManager.removeViewImmediate(akashiLayoutView);
        }
    }

    private boolean checkLayoutExist() {
        return akashiLayoutView != null && akashiLayoutView.getParent() != null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(SHOW_AKASHIVIEW_ACTION)) {
                active = true;
                int setViewResult = setView();
                if (setViewResult == 0) {
                    if (akashiLayoutView.getParent() != null) {
                        windowManager.removeViewImmediate(akashiLayoutView);
                    }
                    windowManager.addView(akashiLayoutView, mParams);
                }
                Log.e("KCA", "show_akashiview_action " + String.valueOf(setViewResult));
                akashiLayoutView.setVisibility(View.VISIBLE);
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
                        if (id == akashiLayoutView.findViewById(R.id.akashiview_head).getId()) {
                            active = false;
                            akashiLayoutView.setVisibility(View.GONE);
                            windowManager.removeViewImmediate(akashiLayoutView);
                        } else if (id == akashiLayoutView.findViewById(R.id.akashiview_star).getId()) {
                            if (isStarChecked) akashiview_star.setText(getString(R.string.aa_btn_star0));
                            else akashiview_star.setText(getString(R.string.aa_btn_star1));
                            isStarChecked = !isStarChecked;
                            loadTodayAkashiList(isSafeChecked);
                            resetListView(true);
                        } else if (id == akashiLayoutView.findViewById(R.id.akashiview_gtd).getId()) {
                            if (isSafeChecked) {
                                akashiview_gtd.setText(getString(R.string.aa_btn_safe_state0));
                                akashiview_gtd.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                            }
                            else {
                                akashiview_gtd.setText(getString(R.string.aa_btn_safe_state1));
                                akashiview_gtd.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAkashiGtdScrew));
                            }
                            isSafeChecked = !isSafeChecked;
                            loadTodayAkashiList(isSafeChecked);
                            resetListView(false);
                        }
                    }
                    break;
            }
            return true;
        }
    };

    private int getAkashiDataFromStorage() {
        JsonObject data;
        data = KcaUtils.getJsonObjectFromStorage(getApplicationContext(), "akashi_data.json", dbHelper);
        if (data != null) {
            akashiData = data;
        } else {
            return -1;
        }

        data = KcaUtils.getJsonObjectFromStorage(getApplicationContext(), "akashi_day.json", dbHelper);
        if (data != null) {
            akashiDay = data;
        } else {
            return -1;
        }

        JsonArray itemData = KcaUtils.getJsonArrayFromStorage(getApplicationContext(), "akashi_reqitems.json", dbHelper);
        if (itemData != null) {
            AkashiDetailActivity.setRequiredItemTranslation(itemData);
        } else {
            return -1;
        }

        showDataLoadErrorToast(getApplicationContext(), getString(R.string.download_check_error));
        return 1;
    }

    private void resetListView(boolean isTop) {
        adapter.setSafeCheckedStatus(isSafeChecked);
        adapter.setListViewItemList(listViewItemList);
        adapter.notifyDataSetChanged();
        if (isTop) akashiview_list.setAdapter(adapter);
    }

    private void loadTodayAkashiList(boolean checked) {
        Calendar calendar = getJapanCalendarInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0(Sun) ~ 6(Sat)
        loadAkashiList(day, checked);
    }

    private void loadAkashiList(int day, boolean checked) {
        final int TYPE_MUL = 1000;
        String starlist = getStringPreferences(getApplicationContext(), PREF_AKASHI_STARLIST);

        listViewItemList.clear();
        List<Integer> keylist = new ArrayList<Integer>();
        JsonArray equipList = akashiDay.getAsJsonArray(String.valueOf(day));
        for (int i = 0; i < equipList.size(); i++) {
            int equipid = equipList.get(i).getAsInt();
            JsonObject kcItemData = KcaApiData.getKcItemStatusById(equipid, "type");
            int type2 = kcItemData.getAsJsonArray("type").get(2).getAsInt();
            int type3 = kcItemData.getAsJsonArray("type").get(3).getAsInt();
            keylist.add(type2 * TYPE_MUL + equipid);
        }
        Collections.sort(keylist);

        for (int equipid : keylist) {
            equipid = equipid % TYPE_MUL;
            if (isStarChecked && !checkStarred(starlist, equipid)) continue;
            KcaAkashiListViewItem item = new KcaAkashiListViewItem();
            item.setEquipDataById(equipid);
            Log.e("KCA", String.valueOf(equipid));
            item.setEquipImprovementData(akashiData.getAsJsonObject(String.valueOf(equipid)));
            item.setEquipImprovementElement(day, checked);
            listViewItemList.add(item);
        }
    }

    private boolean checkStarred(String data, int id) {
        return data.contains(KcaUtils.format("|%d|", id));
    }
}
