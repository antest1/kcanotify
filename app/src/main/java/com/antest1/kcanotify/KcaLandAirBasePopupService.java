package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;

import static android.R.attr.value;
import static com.antest1.kcanotify.KcaApiData.getItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isItemAircraft;
import static com.antest1.kcanotify.KcaApiData.kcItemData;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_LABSIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_DEFENSE;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_REST;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_RETREAT;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_SORTIE;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_STANDBY;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

public class KcaLandAirBasePopupService extends Service {
    public final static String LAB_DATA_ACTION = "lab_data_action";

    private View mView, itemView;
    ;
    private WindowManager mManager;
    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private KcaDBHelper dbHelper;
    KcaDeckInfo deckInfoCalc;

    WindowManager.LayoutParams mParams, itemViewParams;

    public static int type;
    public static int clickcount;
    public static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            active = true;
            clickcount = 0;
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            deckInfoCalc = new KcaDeckInfo(getApplicationContext(), getBaseContext());
            KcaApiData.setDBHelper(dbHelper);
            setDefaultGameData();

            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = mInflater.inflate(R.layout.view_labinfo_view, null);
            itemView = mInflater.inflate(R.layout.view_battleview_items, null);

            mView.setOnTouchListener(mViewTouchListener);
            mView.findViewById(R.id.view_lab_head).setOnTouchListener(mViewTouchListener);
            mView.setVisibility(View.GONE);
            ((TextView) mView.findViewById(R.id.view_lab_title)).setText(getStringWithLocale(R.string.viewmenu_airbase_title));

            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWidth = mView.getMeasuredWidth();
            popupHeight = mView.getMeasuredHeight();

            // Button (Fairy) Settings
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            mParams.gravity = Gravity.TOP | Gravity.START;
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

            mParams.x = (screenWidth - popupWidth) / 2;
            mParams.y = (screenHeight - popupHeight) / 2;
            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            itemViewParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-LAB", "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(LAB_DATA_ACTION)) {
                // check item data exist in db
                if (dbHelper != null && dbHelper.getItemCount() > 0) {
                    updatePopup();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mView != null) {
            if (mView.getParent() != null) mManager.removeViewImmediate(mView);
        }
        if (itemView != null) {
            if (itemView.getParent() != null) mManager.removeViewImmediate(itemView);
        }
        super.onDestroy();
    }

    @SuppressLint("HandlerLeak")
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            LinearLayout view_list = mView.findViewById(R.id.view_lab_list);
            LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view_list.removeAllViews();
            JsonArray api_air_base = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
            String value = "";
            try {
                if (api_air_base != null && api_air_base.size() > 0) {
                    for (int i = 0; i < api_air_base.size(); i++) {
                        JsonObject item = api_air_base.get(i).getAsJsonObject();
                        View v = vi.inflate(R.layout.listivew_lab, null);

                        int ori_info = getResources().getConfiguration().orientation;
                        if (ori_info == Configuration.ORIENTATION_PORTRAIT) {
                            ((LinearLayout) v.findViewById(R.id.lab_row)).setOrientation(LinearLayout.VERTICAL);
                        } else {
                            ((LinearLayout) v.findViewById(R.id.lab_row)).setOrientation(LinearLayout.HORIZONTAL);
                        }

                        JsonObject distance_info = item.getAsJsonObject("api_distance");
                        int distance_base = distance_info.get("api_base").getAsInt();
                        int distance_bonus = distance_info.get("api_bonus").getAsInt();
                        ((TextView) v.findViewById(R.id.lab_dist)).setText(
                                KcaUtils.format(getStringWithLocale(R.string.labinfoview_dist_format), distance_base + distance_bonus));

                        TextView titleView = v.findViewById(R.id.lab_title);
                        titleView.setText(KcaUtils.format("[%d-%d] %s",
                                item.get("api_area_id").getAsInt(), item.get("api_rid").getAsInt(), item.get("api_name").getAsString()));

                        TextView statusView = v.findViewById(R.id.lab_status);
                        TextView fpView = v.findViewById(R.id.lab_fp);
                        int fp_value = 0;
                        int action_status = item.get("api_action_kind").getAsInt();
                        switch (action_status) {
                            case LAB_STATUS_STANDBY:
                                statusView.setText(getStringWithLocale(R.string.labinfoview_status_standby));
                                statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusStandby));
                                fpView.setText("");
                                fpView.setVisibility(View.GONE);
                                break;
                            case LAB_STATUS_SORTIE:
                                statusView.setText(getStringWithLocale(R.string.labinfoview_status_sortie));
                                statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusSortie));
                                fp_value = deckInfoCalc.getAirPowerInAirBase(LAB_STATUS_SORTIE, item.getAsJsonArray("api_plane_info"));
                                fpView.setText(KcaUtils.format(getStringWithLocale(R.string.labinfoview_fp_format), fp_value));
                                fpView.setVisibility(View.VISIBLE);
                                break;
                            case LAB_STATUS_DEFENSE:
                                statusView.setText(getStringWithLocale(R.string.labinfoview_status_defense));
                                statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusDefense));
                                fp_value = deckInfoCalc.getAirPowerInAirBase(LAB_STATUS_DEFENSE, item.getAsJsonArray("api_plane_info"));
                                fpView.setText(KcaUtils.format(getStringWithLocale(R.string.labinfoview_fp_format), fp_value));
                                fpView.setVisibility(View.VISIBLE);
                                break;
                            case LAB_STATUS_RETREAT:
                                statusView.setText(getStringWithLocale(R.string.labinfoview_status_retreat));
                                statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusRetreat));
                                fpView.setText("");
                                fpView.setVisibility(View.GONE);
                                break;
                            case LAB_STATUS_REST:
                                statusView.setText(getStringWithLocale(R.string.labinfoview_status_rest));
                                statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusRest));
                                fpView.setText("");
                                fpView.setVisibility(View.GONE);
                                break;
                            default:
                                statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                                fpView.setText("");
                                fpView.setVisibility(View.GONE);
                                statusView.setText("");
                        }
                        int empty_count = 0;
                        JsonArray plane_info = item.getAsJsonArray("api_plane_info");
                        for (int j = 0; j < plane_info.size(); j++) {
                            JsonObject plane = plane_info.get(j).getAsJsonObject();
                            int state = plane.get("api_state").getAsInt();
                            ImageView iconView = v.findViewById(getId(KcaUtils.format("lab_icon%d", j + 1), R.id.class));
                            if (state > 0) {
                                int typeres = 0;
                                int cond = 1;
                                if (plane.has("api_cond")) plane.get("api_cond").getAsInt();
                                int slotid = plane.get("api_slotid").getAsInt();
                                JsonObject itemData = getUserItemStatusById(slotid, "id", "type");
                                int itemType = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                                try {
                                    typeres = getId(KcaUtils.format("item_%d", itemType), R.mipmap.class);
                                } catch (Exception e) {
                                    typeres = R.mipmap.item_0;
                                }
                                iconView.setImageResource(typeres);
                                if (state == 2) {
                                    iconView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsPlaneInChange), PorterDuff.Mode.OVERLAY);
                                } else if (cond == 3) {
                                    iconView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue2), PorterDuff.Mode.OVERLAY);
                                } else if (cond == 2) {
                                    iconView.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue1), PorterDuff.Mode.OVERLAY);
                                } else {
                                    iconView.clearColorFilter();
                                }
                            } else {
                                empty_count += 1;
                                iconView.clearColorFilter();
                                iconView.setImageResource(R.mipmap.item_0);
                            }
                        }
                        LinearLayout lab_plane_layout = v.findViewById(R.id.lab_plane);
                        lab_plane_layout.setTag(i);
                        if (empty_count < 4) {
                            lab_plane_layout.setOnTouchListener(mViewTouchListener);
                            lab_plane_layout.setVisibility(View.VISIBLE);
                        } else {
                            lab_plane_layout.setVisibility(View.GONE);
                        }
                        view_list.addView(v);
                    }
                    mView.findViewById(R.id.view_lab_test).setVisibility(View.GONE);
                } else {
                    mView.findViewById(R.id.view_lab_test).setVisibility(View.VISIBLE);
                    ((TextView) mView.findViewById(R.id.view_lab_test)).setText("No Data");
                }
            } catch (Exception e) {
                mView.findViewById(R.id.view_lab_test).setVisibility(View.VISIBLE);
                ((TextView) mView.findViewById(R.id.view_lab_test)).setText("Error while processing data");
                dbHelper.putValue(DB_KEY_LABSIFNO, (new JsonArray()).toString());
            }

            mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWidth = mView.getMeasuredWidth();
            popupHeight = mView.getMeasuredHeight();
            mParams.x = (screenWidth - popupWidth) / 2;
            mParams.y = (screenHeight - popupHeight) / 2;
            mManager.updateViewLayout(mView, mParams);
            mView.setVisibility(View.VISIBLE);
        }
    };

    private void updatePopup() {
        Log.e("KCA-LAB", "updatePopup");
        handler.sendEmptyMessage(0);
    }

    public void setItemViewLayout(JsonObject data) {
        JsonArray plane_info = data.getAsJsonArray("api_plane_info");
        for (int i = 0; i < plane_info.size(); i++) {
            JsonObject item = plane_info.get(i).getAsJsonObject();
            int state = item.get("api_state").getAsInt();
            if (state == 0) {
                ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                        .setText("-");
                ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                ((ImageView) itemView.findViewById(getId(KcaUtils.format("item%d_icon", i + 1), R.id.class))).setImageResource(R.mipmap.item_0);
                itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            } else {
                int item_id = item.get("api_slotid").getAsInt();
                JsonObject kcItemData = getUserItemStatusById(item_id, "level,alv", "type,name");
                if (kcItemData == null) continue;
                int lv = kcItemData.get("level").getAsInt();
                int alv = 0;
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
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), getId(KcaUtils.format("itemalv%d", alvColorId), R.color.class)));
                    itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                } else {
                    itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                }

                if (state == 1) {
                    int nowSlotValue = item.get("api_count").getAsInt();
                    int maxSlotValue = item.get("api_max_count").getAsInt();
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)))
                            .setText(KcaUtils.format("[%02d/%02d]", nowSlotValue, maxSlotValue));
                    itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                } else { // state == 2
                    itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsPlaneInChange));
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

                if (item.has("api_cond")) {
                    int cond = item.get("api_cond").getAsInt();
                    if (cond == 2) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue1));
                    }
                    else if (cond == 3) {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue2));
                    } else {
                        ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    }
                }
                itemView.findViewById(getId("item".concat(String.valueOf(i + 1)), R.id.class)).setVisibility(View.VISIBLE);
            }
        }

        itemView.findViewById(R.id.view_slot_ex).setVisibility(View.GONE);
        itemView.setVisibility(View.VISIBLE);
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY, itemViewX, itemViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private static final int LONG_CLICK_DURATION = 800;
        int itemViewWidth, itemViewHeight;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            int xMargin = (int) getResources().getDimension(R.dimen.item_popup_xmargin);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = mParams.x;
                    mViewY = mParams.y;
                    itemViewX = itemViewParams.x;
                    itemViewY = itemViewParams.y;
                    Log.e("KCA", KcaUtils.format("mView: %d %d", mViewX, mViewY));
                    startClickTime = Calendar.getInstance().getTimeInMillis();

                    int tag = -1;
                    if (v.getTag() instanceof Integer) {
                        tag = (Integer) v.getTag();
                    }
                    if (tag != -1) {
                        JsonArray api_air_base = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
                        if (tag < api_air_base.size()) {
                            JsonObject item = api_air_base.get(tag).getAsJsonObject();
                            setItemViewLayout(item);
                            itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                            itemViewWidth = itemView.getMeasuredWidth();
                            itemViewHeight = itemView.getMeasuredHeight();

                            itemViewParams.x = (int) (event.getRawX() - xMargin - itemViewWidth);
                            itemViewParams.y = (int) (event.getRawY() - itemViewHeight / 2);
                            itemViewParams.gravity = Gravity.TOP | Gravity.LEFT;
                            if (itemView.getParent() != null) {
                                mManager.removeViewImmediate(itemView);
                            }
                            mManager.addView(itemView, itemViewParams);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    Log.e("KCA", "Callback Canceled");
                    itemView.setVisibility(View.GONE);

                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == R.id.view_lab_head) {
                            if (mView != null) mView.setVisibility(View.GONE);
                            if (itemView != null) itemView.setVisibility(View.GONE);
                            stopPopup();
                        }
                    }

                    int[] locations = new int[2];
                    mView.getLocationOnScreen(locations);
                    int xx = locations[0];
                    int yy = locations[1];
                    Log.e("KCA", KcaUtils.format("Coord: %d %d", xx, yy));
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;
                    if (mParams.x < 0) mParams.x = 0;
                    else if (mParams.x > screenWidth - popupWidth)
                        mParams.x = screenWidth - popupWidth;
                    if (mParams.y < 0) mParams.y = 0;
                    else if (mParams.y > screenHeight - popupHeight)
                        mParams.y = screenHeight - popupHeight;
                    mManager.updateViewLayout(mView, mParams);

                    if (itemView.getParent() != null) {
                        itemViewParams.x = (int) (event.getRawX() - xMargin - itemViewWidth);
                        itemViewParams.y = (int) (event.getRawY() - itemViewHeight / 2);
                        mManager.updateViewLayout(itemView, itemViewParams);
                    }
                    break;
            }

            return true;
        }
    };

    private void stopPopup() {
        active = false;
        stopSelf();
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: " + String.valueOf(screenWidth) + " " + String.valueOf(screenHeight));

        if (mParams != null) {
            if (mParams.x < 0) mParams.x = 0;
            else if (mParams.x > screenWidth - popupWidth) mParams.x = screenWidth - popupWidth;
            if (mParams.y < 0) mParams.y = 0;
            else if (mParams.y > screenHeight - popupHeight) mParams.y = screenHeight - popupHeight;
        }

        super.onConfigurationChanged(newConfig);
    }
}