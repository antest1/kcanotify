package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.Log;
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

import static com.antest1.kcanotify.KcaApiData.getSlotItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_LABSIFNO;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_DEFENSE;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_REST;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_RETREAT;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_SORTIE;
import static com.antest1.kcanotify.KcaConstants.LAB_STATUS_STANDBY;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getWindowLayoutType;

import java.util.Calendar;

public class KcaLandAirBasePopupService extends BaseService {
    public final static String LAB_DATA_ACTION = "lab_data_action";

    private View layoutView, itemView;
    private WindowManager windowManager;
    private KcaDBHelper dbHelper;
    KcaDeckInfo deckInfoCalc;

    WindowManager.LayoutParams layoutParams, itemViewParams;

    public static int type;
    public static int clickcount;
    public static boolean active = false;
    public static int visibility = View.GONE;

    public static boolean isActive() {
        return active;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            active = true;
            clickcount = 0;
            dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
            deckInfoCalc = new KcaDeckInfo(getBaseContext());
            KcaApiData.setDBHelper(dbHelper);
            setDefaultGameData();

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA-LAB", "onStartCommand");
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(LAB_DATA_ACTION)) {
                // check item data exist in db
                setPopupLayout();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        active = false;
        super.onDestroy();
    }

    private void setPopupLayout() {
        if (checkLayoutExist()) return;

        LayoutInflater mInflater = LayoutInflater.from(this);
        layoutView = mInflater.inflate(R.layout.view_labinfo_view, null);
        itemView = mInflater.inflate(R.layout.view_battleview_items, null);

        // mView.setOnTouchListener(mViewTouchListener);
        layoutView.findViewById(R.id.view_lab_head).setOnTouchListener(mViewTouchListener);
        layoutView.setVisibility(View.GONE);
        ((TextView) layoutView.findViewById(R.id.view_lab_title)).setText(getString(R.string.viewmenu_airbase_title));
        layoutView.findViewById(R.id.view_lab_title).setOnClickListener(view -> {
            if (layoutView != null) layoutView.setVisibility(View.GONE);
            if (itemView != null) itemView.setVisibility(View.GONE);
            stopPopup();
        });

        layoutParams = getLayoutParams(getResources().getConfiguration());
        windowManager.addView(layoutView, layoutParams);

        itemViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        if (dbHelper != null && dbHelper.getItemCount() > 0) {
            updatePopupContent();
        }
    }

    @SuppressLint("HandlerLeak")
    final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            setPopupContent();
            windowManager.updateViewLayout(layoutView, layoutParams);
            layoutView.setVisibility(View.VISIBLE);
        }
    };

    private void updatePopupContent() {
        Log.e("KCA-LAB", "updatePopup");
        handler.sendEmptyMessage(0);
    }

    private void setPopupContent() {
        LinearLayout view_list = layoutView.findViewById(R.id.view_lab_list);
        LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view_list.removeAllViews();
        JsonArray api_air_base = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
        try {
            if (api_air_base != null && !api_air_base.isEmpty()) {
                for (int i = 0; i < api_air_base.size(); i++) {
                    JsonObject item = api_air_base.get(i).getAsJsonObject();
                    View v = vi.inflate(R.layout.listview_lab, null);

                    JsonObject distance_info = item.getAsJsonObject("api_distance");
                    int distance_base = distance_info.get("api_base").getAsInt();
                    int distance_bonus = distance_info.get("api_bonus").getAsInt();
                    ((TextView) v.findViewById(R.id.lab_dist)).setText(
                            KcaUtils.format(getString(R.string.labinfoview_dist_format), distance_base + distance_bonus));

                    TextView titleView = v.findViewById(R.id.lab_title);
                    titleView.setText(KcaUtils.format("[%d-%d] %s",
                            item.get("api_area_id").getAsInt(), item.get("api_rid").getAsInt(), item.get("api_name").getAsString()));

                    TextView statusView = v.findViewById(R.id.lab_status);
                    TextView fpView = v.findViewById(R.id.lab_fp);
                    int fp_value = 0;
                    int action_status = item.get("api_action_kind").getAsInt();
                    switch (action_status) {
                        case LAB_STATUS_STANDBY:
                            statusView.setText(getString(R.string.labinfoview_status_standby));
                            statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusStandby));
                            fpView.setText("");
                            fpView.setVisibility(View.GONE);
                            break;
                        case LAB_STATUS_SORTIE:
                            statusView.setText(getString(R.string.labinfoview_status_sortie));
                            statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusSortie));
                            fp_value = deckInfoCalc.getAirPowerInAirBase(LAB_STATUS_SORTIE, item.getAsJsonArray("api_plane_info"));
                            fpView.setText(KcaUtils.format(getString(R.string.labinfoview_fp_format), fp_value));
                            fpView.setVisibility(View.VISIBLE);
                            break;
                        case LAB_STATUS_DEFENSE:
                            statusView.setText(getString(R.string.labinfoview_status_defense));
                            statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusDefense));
                            fp_value = deckInfoCalc.getAirPowerInAirBase(LAB_STATUS_DEFENSE, item.getAsJsonArray("api_plane_info"));
                            fpView.setText(KcaUtils.format(getString(R.string.labinfoview_fp_format), fp_value));
                            fpView.setVisibility(View.VISIBLE);
                            break;
                        case LAB_STATUS_RETREAT:
                            statusView.setText(getString(R.string.labinfoview_status_retreat));
                            statusView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLabsStatusRetreat));
                            fpView.setText("");
                            fpView.setVisibility(View.GONE);
                            break;
                        case LAB_STATUS_REST:
                            statusView.setText(getString(R.string.labinfoview_status_rest));
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
                            int slotid = plane.get("api_slotid").getAsInt();
                            int typeres = R.mipmap.item_0;;
                            int cond = 1;
                            if (plane.has("api_cond")) cond = plane.get("api_cond").getAsInt();

                            JsonObject itemData = getUserItemStatusById(slotid, "id", "type");
                            if (itemData != null) {
                                int itemType = itemData.get("type").getAsJsonArray().get(3).getAsInt();
                                try {
                                    typeres = getId(KcaUtils.format("item_%d", itemType), R.mipmap.class);
                                } catch (Exception e) {
                                    // do not change default value
                                }
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
                layoutView.findViewById(R.id.view_lab_test).setVisibility(View.GONE);
            } else {
                layoutView.findViewById(R.id.view_lab_test).setVisibility(View.VISIBLE);
                ((TextView) layoutView.findViewById(R.id.view_lab_test)).setText("No Data");
            }
        } catch (Exception e) {
            layoutView.findViewById(R.id.view_lab_test).setVisibility(View.VISIBLE);
            ((TextView) layoutView.findViewById(R.id.view_lab_test)).setText("Error while processing data");
            dbHelper.putValue(DB_KEY_LABSIFNO, (new JsonArray()).toString());
        }
    }

    public void setItemViewLayout(JsonObject data) {
        itemView.setVisibility(View.GONE);
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
                            .setText(getString(R.string.lv_star).concat(String.valueOf(lv)));
                    itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                } else {
                    itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                }

                if (alv > 0) {
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)))
                            .setText(getString(getId(KcaUtils.format("alv_%d", alv), R.string.class)));
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

                String kcItemName = getSlotItemTranslation(kcItemData.get("name").getAsString());
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
        itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    }

    private final View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            int xmargin = (int) getResources().getDimension(R.dimen.item_popup_xmargin);
            int ymargin = (int) getResources().getDimension(R.dimen.item_popup_ymargin);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    int tag = -1;
                    if (v.getTag() instanceof Integer) {
                        tag = (Integer) v.getTag();
                    } if (tag != -1) {
                        JsonArray api_air_base = dbHelper.getJsonArrayValue(DB_KEY_LABSIFNO);
                        if (tag < api_air_base.size()) {
                            if (itemView.getParent() != null) windowManager.removeViewImmediate(itemView);
                            JsonObject item = api_air_base.get(tag).getAsJsonObject();
                            setItemViewLayout(item);
                            itemViewParams.x = Math.max(0, (int) (event.getRawX() - xmargin - itemView.getMeasuredWidth()));
                            itemViewParams.y = (int) (event.getRawY() - ymargin - itemView.getMeasuredHeight());
                            itemViewParams.gravity = Gravity.TOP | Gravity.START;
                            windowManager.addView(itemView, itemViewParams);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("KCA", "Callback Canceled");
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        if (id == R.id.view_lab_head) {
                            stopPopup();
                        }
                    }
                    itemView.setVisibility(View.GONE);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (itemView.getParent() != null) {
                        itemViewParams.x = Math.max(0, (int) (event.getRawX() - xmargin - itemView.getMeasuredWidth()));
                        itemViewParams.y = (int) (event.getRawY() - ymargin - itemView.getMeasuredHeight());
                        windowManager.updateViewLayout(itemView, itemViewParams);
                    }
                    break;
            }
            return true;
        }
    };

    private void stopPopup() {
        if (layoutView != null) {
            if (layoutView.getParent() != null) windowManager.removeViewImmediate(layoutView);
        }
        if (itemView != null) {
            if (itemView.getParent() != null) windowManager.removeViewImmediate(itemView);
        }
        stopSelf();
    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    private WindowManager.LayoutParams getLayoutParams(Configuration config) {
        int orientation = config.orientation;
        int layoutWidth = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                WindowManager.LayoutParams.WRAP_CONTENT : WindowManager.LayoutParams.MATCH_PARENT;

        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                layoutWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.CENTER;
        return mParams;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        visibility = layoutView.getVisibility();
        if (windowManager != null && checkLayoutExist()) {
            windowManager.removeViewImmediate(layoutView);
        }
    }

    private boolean checkLayoutExist() {
        return layoutView != null && layoutView.getParent() != null;
    }
}