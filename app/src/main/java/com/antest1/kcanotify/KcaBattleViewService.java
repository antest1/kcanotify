package com.antest1.kcanotify;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.checkUserPortEnough;
import static com.antest1.kcanotify.KcaApiData.getAirForceResultString;
import static com.antest1.kcanotify.KcaApiData.getCurrentNodeAlphabet;
import static com.antest1.kcanotify.KcaApiData.getCurrentNodeSubExist;
import static com.antest1.kcanotify.KcaApiData.getEngagementString;
import static com.antest1.kcanotify.KcaApiData.getFormationString;
import static com.antest1.kcanotify.KcaApiData.getItemString;
import static com.antest1.kcanotify.KcaApiData.getSlotItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getNodeColor;
import static com.antest1.kcanotify.KcaApiData.getNodeFullInfo;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getUseitemCount;
import static com.antest1.kcanotify.KcaApiData.getUseItemNameById;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isItemAircraft;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaFleetViewService.SHOW_FLEETVIEW_ACTION;
import static com.antest1.kcanotify.KcaQuestViewService.SHOW_QUESTVIEW_ACTION_NEW;
import static com.antest1.kcanotify.KcaUseStatConstant.BV_BTN_PRESS;
import static com.antest1.kcanotify.KcaUseStatConstant.CLOSE_BATTEVIEW;
import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_BATTEVIEW;
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


public class KcaBattleViewService extends BaseService {
    public static final String SHOW_BATTLEVIEW_ACTION = "show_battleview";
    public static final String HIDE_BATTLEVIEW_ACTION = "hide_battleview";

    public static final int[] contact_bonus = {112, 112, 117, 120};
    
    KcaDBHelper dbHelper;
    KcaDeckInfo deckInfoCalc;
    LayoutInflater mInflater;
    private BroadcastReceiver refreshreceiver;
    SharedPreferences prefs;

    public static boolean active;
    public static int view_status = 0;
    public static JsonObject api_data;
    public static String currentNodeInfo = "";

    static JsonArray api_f_maxhps;
    static JsonArray api_f_nowhps;
    static JsonArray api_f_afterhps;

    static JsonArray api_e_maxhps;
    static JsonArray api_e_nowhps;
    static JsonArray api_e_afterhps;

    static JsonArray api_f_maxhps_combined;
    static JsonArray api_f_nowhps_combined;
    static JsonArray api_f_afterhps_combined;

    static JsonArray api_e_maxhps_combined;
    static JsonArray api_e_nowhps_combined;
    static JsonArray api_e_afterhps_combined;

    static JsonArray api_f_starthps;
    static JsonArray api_e_starthps;
    static JsonArray api_f_starthps_combined;
    static JsonArray api_e_starthps_combined;

    static JsonObject fleetcheckdata;
    public static JsonObject deckportdata = null;
    public static JsonArray friendShipData, friendCombinedShipData;
    public static JsonArray enemyShipData, enemyCombinedShipData;

    static boolean error_flag = false;
    boolean fc_flag = false;
    boolean ec_flag = false;

    JsonArray api_formation;
    JsonObject api_kouku;

    private DraggableOverlayLayout battleViewLayout;
    private View itemView, acView, menuView;
    private WindowManager windowManager;
    KcaCustomToast customToast;

    private SnapIndicator snapIndicator;

    JsonArray deckData, portData;

    private int fleetViewHeight = 0;


    public static final int ERORR_INIT = 0;
    public static final int ERORR_VIEW = 1;
    public static final int ERORR_ITEMVIEW = 2;

    private int[] slotViewList = {R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5};

    private WindowManager.LayoutParams layoutParams, acViewParams;
    private ViewGroup battleview;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static String makeHpString(int currenthp, int maxhp) {
        return KcaUtils.format("HP %d/%d", currenthp, maxhp);
    }

    private static String makeHpString(int currenthp, int maxhp, boolean damecon_flag) {
        String data = KcaUtils.format(" %d/%d", currenthp, maxhp);
        if (damecon_flag) return data;
        else return "HP".concat(data);
    }

    private static String makeLvString(int level) {
        return KcaUtils.format("Lv %d", level);
    }

    private static String makeExpString(int exp, boolean flag) {
        if (flag) return String.valueOf(exp);
        else return KcaUtils.format("next: %d", exp);
    }

    public Drawable getProgressDrawable(Context context, float value) {
        if (value > 75) {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_normal);
        } else if (value > 50) {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_lightdmg);
        } else if (value > 25) {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_moderatedmg);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.progress_bar_heavydmg);
        }
    }

    public boolean checkItemPairExist(JsonArray data, int key1, int key2) {
        String key = KcaUtils.format("%d_%d", key1, key2);
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getAsString().equals(key)) return true;
        }
        return false;
    }

    private void setBattleView() {
        int textsize_n_large = getResources().getDimensionPixelSize(R.dimen.battleview_text_n_large);
        int textsize_n_medium = getResources().getDimensionPixelSize(R.dimen.battleview_text_n_medium);
        int textsize_n_small = getResources().getDimensionPixelSize(R.dimen.battleview_text_n_small);

        int textsize_c_large = getResources().getDimensionPixelSize(R.dimen.battleview_text_c_large);
        int textsize_c_medium = getResources().getDimensionPixelSize(R.dimen.battleview_text_c_medium);
        int textsize_c_small = getResources().getDimensionPixelSize(R.dimen.battleview_text_c_small);
        int textsize_c_xsmall = getResources().getDimensionPixelSize(R.dimen.battleview_text_c_xsmall);

        if (api_data != null) {
            boolean is_practice = api_data.has("api_practice_flag");

            if (api_data.has("api_maparea_id")) { // start, next
                Log.e("KCA", "START/NEXT");
                //Log.e("KCA", api_data.toString());
                int api_maparea_id = api_data.get("api_maparea_id").getAsInt();
                int api_mapinfo_no = api_data.get("api_mapinfo_no").getAsInt();
                int api_no = api_data.get("api_no").getAsInt();
                String current_node = getCurrentNodeAlphabet(api_maparea_id, api_mapinfo_no, api_no);
                boolean sub_exist = getCurrentNodeSubExist(api_maparea_id, api_mapinfo_no, api_no);
                int api_event_id = api_data.get("api_event_id").getAsInt();
                int api_event_kind = api_data.get("api_event_kind").getAsInt();
                int api_color_no = api_data.get("api_color_no").getAsInt();
                currentNodeInfo = getNodeFullInfo(this, current_node, api_event_id, api_event_kind, api_color_no, true);
                currentNodeInfo = currentNodeInfo.replaceAll("[()]", "");

                // View Settings
                fc_flag = KcaBattle.isCombinedFleetInSortie();
                ec_flag = api_event_id != API_NODE_EVENT_ID_NOEVENT &&
                        (api_event_kind == API_NODE_EVENT_KIND_ECBATTLE || api_event_kind == API_NODE_EVENT_KIND_NIGHTDAYBATTLE_EC);
                setViewLayout(fc_flag, false);

                ((TextView) battleview.findViewById(R.id.battle_node)).setText(currentNodeInfo);
                ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name))
                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name))
                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_getitem)).setText("");

                battleview.findViewById(R.id.fm_mid_item1).setVisibility(View.GONE);
                battleview.findViewById(R.id.fm_mid_item2).setVisibility(View.GONE);
                battleview.findViewById(R.id.fm_mid_item3).setVisibility(View.GONE);
                battleview.findViewById(R.id.fs_mid_item1).setVisibility(View.GONE);
                battleview.findViewById(R.id.fs_mid_item2).setVisibility(View.GONE);
                battleview.findViewById(R.id.fs_mid_item3).setVisibility(View.GONE);
                battleview.findViewById(R.id.em_mid_item1).setVisibility(View.GONE);
                battleview.findViewById(R.id.em_mid_item2).setVisibility(View.GONE);
                battleview.findViewById(R.id.em_mid_item3).setVisibility(View.GONE);

                battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) )
                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) ))
                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

                if (!getBooleanPreferences(this, PREF_SHOWDROP_SETTING)) {
                    battleview.findViewById(R.id.battle_getship_row).setVisibility(View.GONE);
                } else {
                    battleview.findViewById(R.id.battle_getship_row).setVisibility(View.VISIBLE);
                    if (checkUserPortEnough()) {
                        ((TextView) battleview.findViewById(R.id.battle_getship)).setText("");
                    } else {
                        ((TextView) battleview.findViewById(R.id.battle_getship)).setText(getString(R.string.getship_max));
                    }
                }

                if (api_event_id == API_NODE_EVENT_ID_OBTAIN) {
                    JsonArray api_itemget = api_data.getAsJsonArray("api_itemget");
                    List<String> itemTextList = new ArrayList<String>();
                    for (int i = 0; i < api_itemget.size(); i++) {
                        JsonObject itemdata = api_itemget.get(i).getAsJsonObject();
                        String itemname = getItemString(this, itemdata.get("api_id").getAsInt());
                        int itemgetcount = itemdata.get("api_getcount").getAsInt();
                        itemTextList.add(KcaUtils.format("%s +%d", itemname, itemgetcount));
                    }
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(KcaUtils.joinStr(itemTextList, " / "));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                } else if (api_event_id == API_NODE_EVENT_ID_AIR && api_event_kind == API_NODE_EVENT_KIND_AIRSEARCH) {
                    JsonObject api_airsearch = api_data.getAsJsonObject("api_airsearch");
                    int airsearch_result = api_airsearch.get("api_result").getAsInt();
                    if (airsearch_result == 0) {
                        ((TextView) battleview.findViewById(R.id.battle_result)).setText(getString(R.string.recon_failed));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorReconFailed));
                    } else {
                        JsonObject itemdata = api_data.getAsJsonObject("api_itemget");
                        String itemname = getItemString(this, itemdata.get("api_id").getAsInt());
                        int itemgetcount = itemdata.get("api_getcount").getAsInt();
                        ((TextView) battleview.findViewById(R.id.battle_result)).setText(KcaUtils.format("%s +%d", itemname, itemgetcount));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItemSpecial));
                    }
                } else if (api_event_id == API_NODE_EVENT_ID_LOSS) {
                    JsonObject api_happening = api_data.getAsJsonObject("api_happening");
                    String itemname = getItemString(this, api_happening.get("api_mst_id").getAsInt());
                    int itemgetcount = api_happening.get("api_count").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(KcaUtils.format("%s -%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorVortex));
                } else if (api_event_id == API_NODE_EVENT_ID_SENDAN) {
                    battleViewLayout.findViewById(R.id.battleviewpanel)
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    JsonObject api_itemget_eo_comment = api_data.getAsJsonObject("api_itemget_eo_comment");
                    String itemname = getItemString(this, api_itemget_eo_comment.get("api_id").getAsInt());
                    int itemgetcount = api_itemget_eo_comment.get("api_getcount").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(KcaUtils.format("%s +%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                }

                if (api_data.has("api_destruction_battle")) {
                    JsonObject api_destruction_battle = api_data.getAsJsonObject("api_destruction_battle");
                    int api_lost_kind = api_destruction_battle.get("api_lost_kind").getAsInt();
                    switch (api_lost_kind) {
                        case RAID_LOST_TYPE_1:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getString(R.string.raid_type1));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidDamaged));
                            break;
                        case RAID_LOST_TYPE_2:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getString(R.string.raid_type2));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidHeavyDamaged));
                            break;
                        case RAID_LOST_TYPE_3:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getString(R.string.raid_type3));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidDamaged));
                            break;
                        case RAID_LOST_TYPE_4:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getString(R.string.raid_type4));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidNoDamaged));
                            break;
                        default:
                            break;
                    }
                    JsonObject api_air_based_attack = api_destruction_battle.getAsJsonObject("api_air_base_attack");
                    int api_disp_seiku = api_air_based_attack.getAsJsonObject("api_stage1").get("api_disp_seiku").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_airpower))
                            .setText(getAirForceResultString(this, api_disp_seiku));
                }

                for (int i = 0; i < 7; i++) ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_exp_count", i + 1), R.id.class))).setText("");
                for (int i = 0; i < 6; i++) ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_exp_count", i + 1), R.id.class))).setText("");

                battleview.findViewById(R.id.battle_node)
                        .setBackgroundColor(getNodeColor(getApplicationContext(), api_event_id, api_event_kind, api_color_no));
                if (is_practice) battleview.findViewById(R.id.battle_node_ss).setVisibility(View.GONE);
                else battleview.findViewById(R.id.battle_node_ss).setVisibility(sub_exist ? View.VISIBLE : View.GONE);
            }

            if (api_data.has("api_deck_port")) { // common sortie, practice
                boolean midnight_flag = api_data.get("api_url").getAsString().contains("midnight");
                if (is_practice && !midnight_flag) {
                    ((TextView) battleview.findViewById(R.id.battle_node)).setText(getString(R.string.node_info_practice));
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                    ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                    ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                    ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) ))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) ))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    battleview.findViewById(R.id.battle_getship_row).setVisibility(View.GONE);
                    battleview.findViewById(R.id.battle_node)
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                }

                deckportdata = api_data.getAsJsonObject("api_deck_port");
                if (deckportdata != null) {
                    deckData = deckportdata.getAsJsonArray("api_deck_data");
                    portData = deckportdata.getAsJsonArray("api_ship_data");
                    friendShipData = new JsonArray();
                    friendCombinedShipData = new JsonArray();

                    for (int i = 0; i < 6; i++) {
                        battleview.findViewById(getId(KcaUtils.format("fm_%d", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                        battleview.findViewById(getId(KcaUtils.format("fs_%d", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                        battleview.findViewById(getId(KcaUtils.format("em_%d", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                        battleview.findViewById(getId(KcaUtils.format("es_%d", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                    }
                    battleview.findViewById(R.id.fm_7).setVisibility(View.INVISIBLE);

                    for (int i = 0; i < deckData.size(); i++) {
                        if (i == 0) { // Main Deck
                            JsonObject mainDeckData = deckData.get(i).getAsJsonObject();
                            ((TextView) battleview.findViewById(R.id.friend_fleet_name)).
                                    setText(mainDeckData.get("api_name").getAsString());
                            JsonArray mainDeck = mainDeckData.getAsJsonArray("api_ship");

                            JsonObject shipData = new JsonObject();
                            for (int j = 0; j < portData.size(); j++) {
                                JsonObject data = portData.get(j).getAsJsonObject();
                                shipData.add(String.valueOf(data.get("api_id").getAsInt()), data);
                            }

                            for (int j = 0; j < mainDeck.size(); j++) {
                                if (fc_flag || ec_flag) {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_name", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_large);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_xsmall);
                                } else {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_name", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_medium);
                                }

                                if (fc_flag || ec_flag) {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_lv", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_exp", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_xsmall);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_exp_count", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                                } else {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_lv", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_medium);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_exp", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_small);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_exp_count", j + 1), R.id.class)))
                                            .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                                }

                                if (mainDeck.get(j).getAsInt() == -1) {
                                    battleview.findViewById(getId(KcaUtils.format("fm_%d", j + 1), R.id.class)).setVisibility(View.INVISIBLE);
                                } else {
                                    JsonObject data = shipData.getAsJsonObject(String.valueOf(mainDeck.get(j)));
                                    int ship_id = data.get("api_ship_id").getAsInt();
                                    JsonObject kcdata = getKcShipDataById(data.get("api_ship_id").getAsInt(), "name,maxeq");
                                    JsonObject itemdata = new JsonObject();
                                    itemdata.add("api_slot", data.get("api_slot"));
                                    itemdata.add("api_slot_ex", data.get("api_slot_ex"));
                                    itemdata.add("api_onslot", data.get("api_onslot"));
                                    itemdata.add("api_maxslot", kcdata.get("maxeq"));
                                    friendShipData.add(itemdata);
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    int condition = data.get("api_cond").getAsInt();
                                    int exp_left = data.getAsJsonArray("api_exp").get(1).getAsInt();

                                    String kcname = getShipTranslation(kcdata.get("name").getAsString(), ship_id, false);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_name", j + 1), R.id.class))).setText(kcname);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_name", j + 1), R.id.class)))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class))).setText(String.valueOf(condition));
                                    if (condition > 49) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipKira));
                                    } else if (condition / 10 >= 3) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipNormal));
                                    } else if (condition / 10 == 2) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue1));
                                    } else {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue2));
                                    }

                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_lv", j + 1), R.id.class)))
                                            .setText(makeLvString(level));
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_exp", j + 1), R.id.class)))
                                            .setText(makeExpString(exp_left, fc_flag));
                                    if (!((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", j + 1), R.id.class)))
                                            .getText().toString()
                                            .contains(getString(R.string.battleview_text_retreated))) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", j + 1), R.id.class)))
                                                .setText(makeHpString(nowhp, maxhp));
                                    }

                                    float hpPercent = nowhp * VIEW_HP_MAX / (float) maxhp;
                                    ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_bar", j + 1), R.id.class)))
                                            .setProgress(Math.round(hpPercent));
                                    ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_bar", j + 1), R.id.class)))
                                            .setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                                    battleview.findViewById(getId(KcaUtils.format("fm_%d", j + 1), R.id.class)).setVisibility(View.VISIBLE);
                                }
                            }
                            if (mainDeck.size() <= 6) {
                                battleview.findViewById(R.id.fm_7).setVisibility(View.GONE);
                            } else if (mainDeck.size() == 7 && mainDeck.get(6).getAsInt() == -1) {
                                battleview.findViewById(R.id.fm_7).setVisibility(View.GONE);
                            }


                            Log.e("KCA", "FSD: " + friendShipData.size());

                        } else if (i == 1) {
                            JsonObject combinedDeckData = deckData.get(i).getAsJsonObject();
                            ((TextView) battleview.findViewById(R.id.friend_combined_fleet_name)).
                                    setText(combinedDeckData.get("api_name").getAsString());
                            JsonArray combinedDeck = combinedDeckData.getAsJsonArray("api_ship");
                            JsonObject shipData = new JsonObject();
                            for (int j = 0; j < portData.size(); j++) {
                                JsonObject data = portData.get(j).getAsJsonObject();
                                shipData.add(String.valueOf(data.get("api_id").getAsInt()), data);
                            }

                            for (int j = 0; j < combinedDeck.size(); j++) {
                                ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_name", j + 1), R.id.class)))
                                        .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_large);
                                ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_lv", j + 1), R.id.class)))
                                        .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                                ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", j + 1), R.id.class)))
                                        .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);

                                if (combinedDeck.get(j).getAsInt() == -1) {
                                    battleview.findViewById(getId(KcaUtils.format("fs_%d", j + 1), R.id.class)).setVisibility(View.INVISIBLE);
                                } else {
                                    JsonObject data = shipData.getAsJsonObject(String.valueOf(combinedDeck.get(j)));

                                    int ship_id = data.get("api_ship_id").getAsInt();

                                    JsonObject kcdata = getKcShipDataById(ship_id, "maxeq,name");
                                    JsonObject itemdata = new JsonObject();
                                    itemdata.add("api_slot", data.get("api_slot"));
                                    itemdata.add("api_slot_ex", data.get("api_slot_ex"));
                                    itemdata.add("api_onslot", data.get("api_onslot"));
                                    itemdata.add("api_maxslot", kcdata.get("maxeq"));
                                    friendCombinedShipData.add(itemdata);
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    int condition = data.get("api_cond").getAsInt();
                                    int exp_left = data.getAsJsonArray("api_exp").get(1).getAsInt();

                                    String kcname = getShipTranslation(kcdata.get("name").getAsString(), ship_id, false);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_name", j + 1), R.id.class))).setText(kcname);
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_name", j + 1), R.id.class)))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_cond", j + 1), R.id.class))).setText(String.valueOf(condition));
                                    if (condition > 49) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipKira));
                                    } else if (condition / 10 >= 3) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipNormal));
                                    } else if (condition / 10 == 2) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue1));
                                    } else {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_cond", j + 1), R.id.class)))
                                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFleetShipFatigue2));
                                    }
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_lv", j + 1), R.id.class))).setText(makeLvString(level));
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_exp", j + 1), R.id.class))).setText(makeExpString(exp_left, true));
                                    if (!((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", j + 1), R.id.class))).getText().toString()
                                            .contains(getString(R.string.battleview_text_retreated))) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", j + 1), R.id.class)))
                                                .setText(makeHpString(nowhp, maxhp));
                                    }
                                    float hpPercent = nowhp * VIEW_HP_MAX / (float) maxhp;
                                    ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_bar", j + 1), R.id.class)))
                                            .setProgress(Math.round(hpPercent));
                                    ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_bar", j + 1), R.id.class)))
                                            .setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                                    battleview.findViewById(getId(KcaUtils.format("fs_%d", j + 1), R.id.class)).setVisibility(View.VISIBLE);
                                }
                            }

                            Log.e("KCA", "FCSD: " + friendCombinedShipData.size());
                        }
                    }
                }
            }

            if (api_data.has("api_escape") && api_data.get("api_escape").isJsonArray()) {
                JsonArray api_escape = api_data.getAsJsonArray("api_escape");
                for (int j = 0; j < api_escape.size(); j++) {
                    int idx = api_escape.get(j).getAsInt();
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", idx), R.id.class)))
                            .setText(getString(R.string.battleview_text_retreated));
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", idx), R.id.class)))
                            .setGravity(Gravity.CENTER_HORIZONTAL);
                    battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", idx), R.id.class))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRetreated));
                }
            }

            if (api_data.has("api_escape_combined") && api_data.get("api_escape_combined").isJsonArray()) {
                JsonArray api_escape_combined = api_data.getAsJsonArray("api_escape_combined");
                for (int j = 0; j < api_escape_combined.size(); j++) {
                    int idx = api_escape_combined.get(j).getAsInt();
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", idx), R.id.class)))
                            .setText(getString(R.string.battleview_text_retreated));
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", idx), R.id.class)))
                            .setGravity(Gravity.CENTER_HORIZONTAL);
                    battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", idx), R.id.class))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRetreated));
                }
            }

            if (api_data.has("api_ship_ke")) { // Battle (Common)
                Log.e("KCA", "BATTLE");
                enemyShipData = new JsonArray();
                enemyCombinedShipData = new JsonArray();
                JsonArray api_ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray api_ship_lv = api_data.getAsJsonArray("api_ship_lv");
                Log.e("KCA", api_data.toString());
                setViewLayout(fc_flag, ec_flag);

                JsonArray eSlot = api_data.getAsJsonArray("api_eSlot");
                for (int i = 0; i < eSlot.size(); i++) {
                    JsonObject itemdata = new JsonObject();
                    itemdata.add("api_slot", eSlot.get(i));
                    enemyShipData.add(itemdata);
                }
                Log.e("KCA", "ESD: " + String.valueOf(enemyShipData.size()));
                boolean start_flag = checkStart(api_data.get("api_url").getAsString());
                if (start_flag) { // day/sp_night Battle Process
                    api_formation = api_data.getAsJsonArray("api_formation");
                    // air power show
                    if (api_data.has("api_kouku") && !api_data.get("api_kouku").isJsonNull()) {
                        api_kouku = api_data.getAsJsonObject("api_kouku");
                    } else {
                        api_kouku = null;
                    }

                    // fleet formation and engagement show
                    ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).
                            setText(getFormationString(this, api_formation.get(0).getAsInt(), fc_flag));
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).
                            setText(getFormationString(this, api_formation.get(1).getAsInt(), fc_flag));
                    ((TextView) battleview.findViewById(R.id.battle_engagement)).
                            setText(getEngagementString(this, api_formation.get(2).getAsInt()));

                    // airforce result
                    if (api_kouku != null && !api_kouku.get("api_stage1").isJsonNull()) {
                        JsonObject api_stage1 = api_kouku.getAsJsonObject("api_stage1");
                        int api_disp_seiku = api_stage1.get("api_disp_seiku").getAsInt();
                        ((TextView) battleview.findViewById(R.id.battle_airpower))
                                .setText(getAirForceResultString(this, api_disp_seiku));
                    }

                    if (!KcaBattle.currentEnemyDeckName.isEmpty()) {
                        ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                                setText(KcaBattle.currentEnemyDeckName);
                    } else {
                        ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                                setText(getString(R.string.enemy_fleet_name));
                    }
                }

                for (int i = 0; i < api_ship_ke.size(); i++) {
                    if (fc_flag || ec_flag) {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_lv", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_name", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_large);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                    } else {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_lv", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_medium);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_name", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_small);
                    }

                    if (api_ship_ke.get(i).getAsInt() == -1) {
                        battleview.findViewById(getId(KcaUtils.format("em_%d", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                    } else {
                        int level = api_ship_lv.get(i).getAsInt();
                        int ship_ke_id = api_ship_ke.get(i).getAsInt();
                        JsonObject kcShipData = KcaApiData.getKcShipDataById(ship_ke_id, "name,yomi");
                        String kcname = getShipTranslation(kcShipData.get("name").getAsString(), ship_ke_id, true);
                        String kcyomi = kcShipData.get("yomi").getAsString();

                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_name", i + 1), R.id.class))).setText(kcname);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_lv", i + 1), R.id.class))).setText(makeLvString(level));

                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setText("");
                        if (!is_practice) {
                            if (kcname.contains(getString(R.string.ship_name_class))) {
                                if (kcyomi.equals(getString(R.string.yomi_elite))) {
                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setText(getString(R.string.yomi_elite_short));
                                    } else {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setText(kcyomi);
                                    }
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class)))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                                } else if (kcyomi.equals(getString(R.string.yomi_flagship))) {
                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setText(getString(R.string.yomi_flagship_short));
                                    } else {
                                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setText(kcyomi);
                                    }
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class)))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                                }
                            }
                        }
                        battleview.findViewById(getId(KcaUtils.format("em_%d", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    }
                }

                if (is_practice) battleview.findViewById(R.id.battle_node_ss).setVisibility(View.GONE);

                api_f_maxhps = api_data.getAsJsonArray("api_f_maxhps");
                api_f_nowhps = api_data.getAsJsonArray("api_f_nowhps");
                api_f_afterhps = api_data.getAsJsonArray("api_f_afterhps");

                api_e_maxhps = api_data.getAsJsonArray("api_e_maxhps");
                api_e_nowhps = api_data.getAsJsonArray("api_e_nowhps");
                api_e_afterhps = api_data.getAsJsonArray("api_e_afterhps");

                JsonArray api_dc_used = api_data.getAsJsonArray("api_dc_used");

                for (int i = 0; i < api_f_maxhps.size(); i++) {
                    if (fc_flag || ec_flag) {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", i + 1), R.id.class)))
                                .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                    } else {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", i + 1), R.id.class)))
                                .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                    }

                    int maxhp = api_f_maxhps.get(i).getAsInt();
                    int afterhp = api_f_afterhps.get(i).getAsInt();
                    if (maxhp == -1) continue;
                    else {
                        float hpPercent = afterhp * VIEW_HP_MAX / (float) maxhp;
                        boolean damecon_flag = checkItemPairExist(api_dc_used, 0, i);
                        battleview.findViewById(getId(KcaUtils.format("fm_%d_dcflag", i + 1), R.id.class))
                                .setVisibility(damecon_flag ? View.VISIBLE : View.GONE);
                        if (!((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", i + 1), R.id.class)))
                                .getText().toString().contains(getString(R.string.battleview_text_retreated))) {
                            ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_txt", i + 1), R.id.class)))
                                    .setText(makeHpString(afterhp, maxhp, damecon_flag));
                        }
                        ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_bar", i + 1), R.id.class)))
                                .setProgress(Math.round(hpPercent));
                        ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fm_%d_hp_bar", i + 1), R.id.class)))
                                .setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                    }
                }

                for (int i = 0; i < api_e_maxhps.size(); i++) {
                    if (fc_flag || ec_flag) {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_hp_txt", i + 1), R.id.class)))
                                .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                    } else {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_hp_txt", i + 1), R.id.class)))
                                .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                    }

                    String hp_str = api_e_maxhps.get(i).getAsString();
                    float hpPercent = 0;
                    if (hp_str.contains("N")) { // N/A
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_hp_txt", i + 1), R.id.class)))
                                .setText("N/A");
                    } else {
                        int maxhp = api_e_maxhps.get(i).getAsInt();
                        int afterhp = api_e_afterhps.get(i).getAsInt();
                        if (maxhp == -1) continue;
                        else {
                            hpPercent = afterhp * VIEW_HP_MAX / (float) maxhp;
                            ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_hp_txt", i + 1), R.id.class)))
                                    .setText(makeHpString(afterhp, maxhp));
                        }
                    }
                    ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("em_%d_hp_bar", i + 1), R.id.class)))
                            .setProgress(Math.round(hpPercent));
                    ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("em_%d_hp_bar", i + 1), R.id.class)))
                            .setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                }

                if (api_data.has("api_f_maxhps_combined")) {
                    api_f_maxhps_combined = api_data.getAsJsonArray("api_f_maxhps_combined");
                    api_f_nowhps_combined = api_data.getAsJsonArray("api_f_nowhps_combined");
                    api_f_afterhps_combined = api_data.getAsJsonArray("api_f_afterhps_combined");

                    for (int i = 0; i < api_f_maxhps_combined.size(); i++) {
                        if (fc_flag || ec_flag) {
                            ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", i + 1), R.id.class)))
                                    .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                        } else {
                            ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", i + 1), R.id.class)))
                                    .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_n_large);
                        }

                        int maxhp = api_f_maxhps_combined.get(i).getAsInt();
                        int afterhp = api_f_afterhps_combined.get(i).getAsInt();
                        if (maxhp == -1) continue;
                        else {
                            float hpPercent = afterhp * VIEW_HP_MAX / (float) maxhp;
                            boolean damecon_flag = checkItemPairExist(api_dc_used, 1, i);
                            battleview.findViewById(getId(KcaUtils.format("fs_%d_dcflag", i + 1), R.id.class))
                                    .setVisibility(damecon_flag ? View.VISIBLE : View.GONE);
                            if (!((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", i + 1), R.id.class)))
                                    .getText().toString().contains(getString(R.string.battleview_text_retreated))) {
                                ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_txt", i + 1), R.id.class)))
                                        .setText(makeHpString(afterhp, maxhp, damecon_flag));
                            }
                            ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_bar", i + 1), R.id.class))).setProgress(Math.round(hpPercent));
                            ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("fs_%d_hp_bar", i + 1), R.id.class)))
                                    .setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                        }
                    }
                } else {
                    api_f_maxhps_combined =  new JsonArray();
                    api_f_nowhps_combined = new JsonArray();
                    api_f_afterhps_combined = new JsonArray();
                }

                // For enemy combined fleet
                if (api_data.has("api_ship_ke_combined")) {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(getString(R.string.enemy_main_fleet_name));
                    ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name)).
                            setText(getString(R.string.enemy_combined_fleet_name));

                    JsonArray eSlotCombined = api_data.getAsJsonArray("api_eSlot_combined");
                    for (int i = 0; i < eSlotCombined.size(); i++) {
                        JsonObject itemdata = new JsonObject();
                        itemdata.add("api_slot", eSlotCombined.get(i));
                        enemyCombinedShipData.add(itemdata);
                    }
                    Log.e("KCA", "ECSD: " + String.valueOf(enemyCombinedShipData.size()));
                    JsonArray api_ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");
                    for (int i = 0; i < api_ship_ke_combined.size(); i++) {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_name", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_large);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_lv", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("em_%d_yomi", i + 1), R.id.class))).setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);

                        if (api_ship_ke_combined.get(i).getAsInt() == -1) {
                            battleview.findViewById(getId(KcaUtils.format("es_%d", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                        } else {
                            int level = api_ship_lv.get(i).getAsInt();
                            int ship_ke_id = api_ship_ke_combined.get(i).getAsInt();
                            JsonObject kcShipData = KcaApiData.getKcShipDataById(ship_ke_id, "name,yomi");
                            String kcname = getShipTranslation(kcShipData.get("name").getAsString(), ship_ke_id, true);
                            String kcyomi = kcShipData.get("yomi").getAsString();

                            ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_name", i + 1), R.id.class))).setText(kcname);
                            ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_lv", i + 1), R.id.class))).setText(makeLvString(level));

                            if (kcname.contains(getString(R.string.ship_name_class))) {
                                if (kcyomi.equals(getString(R.string.yomi_elite))) {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_yomi", i + 1), R.id.class))).setText(getString(R.string.yomi_elite_short));
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_yomi", i + 1), R.id.class)))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                                } else if (kcyomi.equals(getString(R.string.yomi_flagship))) {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_yomi", i + 1), R.id.class))).setText(getString(R.string.yomi_flagship_short));
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_yomi", i + 1), R.id.class)))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                                } else {
                                    ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_yomi", i + 1), R.id.class))).setText("");
                                }
                            } else {
                                ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_yomi", i + 1), R.id.class))).setText("");
                            }
                            battleview.findViewById(getId(KcaUtils.format("es_%d", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                        }
                    }

                    api_e_maxhps_combined = api_data.getAsJsonArray("api_e_maxhps_combined");
                    api_e_nowhps_combined = api_data.getAsJsonArray("api_e_nowhps_combined");
                    api_e_afterhps_combined = api_data.getAsJsonArray("api_e_afterhps_combined");
                    for (int i = 0; i < api_e_maxhps_combined.size(); i++) {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_hp_txt", i + 1), R.id.class)))
                                .setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize_c_medium);

                        int maxhp_combined = api_e_maxhps_combined.get(i).getAsInt();
                        int afterhp_combined = api_e_afterhps_combined.get(i).getAsInt();
                        if (maxhp_combined != -1) {
                            float hpPercent = afterhp_combined * VIEW_HP_MAX / (float) maxhp_combined;
                            ((TextView) battleview.findViewById(getId(KcaUtils.format("es_%d_hp_txt", i + 1), R.id.class)))
                                    .setText(makeHpString(afterhp_combined, maxhp_combined));
                            ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("es_%d_hp_bar", i + 1), R.id.class))).setProgress(Math.round(hpPercent));
                            ((ProgressBar) battleview.findViewById(getId(KcaUtils.format("es_%d_hp_bar", i + 1), R.id.class))).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                        }
                    }
                } else {
                    api_e_maxhps_combined =  new JsonArray();
                    api_e_nowhps_combined = new JsonArray();
                    api_e_afterhps_combined = new JsonArray();
                }

                if (api_data.has("api_touch_check")) {
                    if (api_data.get("api_touch_check").getAsBoolean()) {
                        battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) )
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) ))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    } else {
                        battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) )
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_1_name"), R.id.class) ))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    }
                }

                if (api_data.has("api_searchlight")) {
                    JsonArray api_searchlight = api_data.getAsJsonArray("api_searchlight");
                    if (api_searchlight.get(0).getAsBoolean()) {
                        if (fc_flag) {
                            battleview.findViewById(R.id.fs_mid_item1).setVisibility(View.VISIBLE);
                        } else {
                            battleview.findViewById(R.id.fm_mid_item1).setVisibility(View.VISIBLE);
                        }
                    } else {
                        battleview.findViewById(R.id.fm_mid_item1).setVisibility(View.GONE);
                        battleview.findViewById(R.id.fs_mid_item1).setVisibility(View.GONE);
                    }
                    if (api_searchlight.get(1).getAsBoolean()) {
                        battleview.findViewById(R.id.em_mid_item1).setVisibility(View.VISIBLE);
                    } else {
                        battleview.findViewById(R.id.em_mid_item1).setVisibility(View.GONE);
                    }
                }

                if (api_data.has("api_flare_pos")) {
                    JsonArray api_flare = api_data.getAsJsonArray("api_flare_pos");
                    if (api_flare.get(0).getAsInt() > -1) {
                        if (fc_flag) {
                            battleview.findViewById(R.id.fs_mid_item2).setVisibility(View.VISIBLE);
                        } else {
                            battleview.findViewById(R.id.fm_mid_item2).setVisibility(View.VISIBLE);
                        }
                    } else {
                        battleview.findViewById(R.id.fm_mid_item2).setVisibility(View.GONE);
                        battleview.findViewById(R.id.fs_mid_item2).setVisibility(View.GONE);
                    }
                    if (api_flare.get(1).getAsInt() > -1) {
                        battleview.findViewById(R.id.em_mid_item2).setVisibility(View.VISIBLE);
                    } else {
                        battleview.findViewById(R.id.em_mid_item2).setVisibility(View.GONE);
                    }

                }

                if (api_data.has("api_touch_plane")) {
                    JsonArray api_touch_plane = api_data.getAsJsonArray("api_touch_plane");
                    if (api_touch_plane.get(0).getAsInt() > -1) {
                        if (fc_flag) {
                            battleview.findViewById(R.id.fs_mid_item3).setVisibility(View.VISIBLE);
                        } else {
                            battleview.findViewById(R.id.fm_mid_item3).setVisibility(View.VISIBLE);
                        }
                    } else {
                        battleview.findViewById(R.id.fm_mid_item3).setVisibility(View.GONE);
                        battleview.findViewById(R.id.fs_mid_item3).setVisibility(View.GONE);
                    }
                    if (api_touch_plane.get(1).getAsInt() > -1) {
                        battleview.findViewById(R.id.em_mid_item3).setVisibility(View.VISIBLE);
                    } else {
                        battleview.findViewById(R.id.em_mid_item3).setVisibility(View.GONE);
                    }
                }

                // Rank Data
                if (start_flag) {
                    api_f_starthps = KcaUtils.parseJson(api_f_nowhps.toString()).getAsJsonArray();
                    api_e_starthps = KcaUtils.parseJson(api_e_nowhps.toString()).getAsJsonArray();
                    api_f_starthps_combined = KcaUtils.parseJson(api_f_nowhps_combined.toString()).getAsJsonArray();
                    api_e_starthps_combined = KcaUtils.parseJson(api_e_nowhps_combined.toString()).getAsJsonArray();

                    JsonObject enemydata = new JsonObject();
                    enemydata.add("e_after", api_e_afterhps);
                    enemydata.add("e_start", api_e_starthps);
                    enemydata.add("e_after_cb", api_e_afterhps_combined);
                    enemydata.add("e_start_cb", api_e_starthps_combined);
                    if (api_data.has("api_ship_ke_combined")) {
                        boolean midnightflag = KcaBattle.isMainFleetInNight(enemydata);
                        if (midnightflag) {
                            ((TextView) battleview.findViewById(R.id.enemy_fleet_name))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNightTargetFleet));
                        } else {
                            ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNightTargetFleet));
                        }
                    }
                }

                fleetcheckdata = new JsonObject();
                fleetcheckdata.add("f_after", api_f_afterhps);
                fleetcheckdata.add("e_after", api_e_afterhps);
                fleetcheckdata.add("f_after_cb", api_f_afterhps_combined);
                fleetcheckdata.add("e_after_cb", api_e_afterhps_combined);
                fleetcheckdata.add("f_start", api_f_starthps);
                fleetcheckdata.add("e_start", api_e_starthps);
                fleetcheckdata.add("f_start_cb", api_f_starthps_combined);
                fleetcheckdata.add("e_start_cb", api_e_starthps_combined);

                String api_url = api_data.get("api_url").getAsString();
                JsonObject rankData;
                if (api_url.equals(API_REQ_SORTIE_LDAIRBATTLE) || api_url.equals(API_REQ_COMBINED_LDAIRBATTLE) || api_url.equals(API_REQ_SORTIE_LDSHOOTING)) {
                    rankData = KcaBattle.calculateLdaRank(fleetcheckdata);
                } else {
                    rankData = KcaBattle.calculateRank(fleetcheckdata);
                }

                if (rankData.has("fnowhpsum")) {
                    int friendNowSum = rankData.get("fnowhpsum").getAsInt();
                    int friendAfterSum = rankData.get("fafterhpsum").getAsInt();
                    int friendDamageRate = rankData.get("fdmgrate").getAsInt();
                    String dmgshow = KcaUtils.format("%d/%d (%d%%)", friendAfterSum, friendNowSum, friendDamageRate);
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText(dmgshow);
                } else {
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                }

                if (rankData.has("enowhpsum")) {
                    int enemyNowSum = rankData.get("enowhpsum").getAsInt();
                    int enemyAfterSum = rankData.get("eafterhpsum").getAsInt();
                    int enemyDamageRate = rankData.get("edmgrate").getAsInt();
                    String dmgshow = KcaUtils.format("%d/%d (%d%%)", enemyAfterSum, enemyNowSum, enemyDamageRate);
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText(dmgshow);
                } else {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                }

                int rank = rankData.get("rank").getAsInt();
                switch (rank) {
                    case JUDGE_SS:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_ss));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankS));
                        break;
                    case JUDGE_S:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_s));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankS));
                        break;
                    case JUDGE_A:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_a));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankA));
                        break;
                    case JUDGE_B:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_b));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankB));
                        break;
                    case JUDGE_C:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_c));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankC));
                        break;
                    case JUDGE_D:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_d));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankD));
                        break;
                    case JUDGE_E:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getString(R.string.rank_e));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankE));
                        break;
                    default:
                        break;
                }
            } else if (api_data.has("api_win_rank")) { // Battle Result
                int mvp_idx = api_data.get("api_mvp").getAsInt();
                if (mvp_idx != -1) {
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_name", mvp_idx), R.id.class) ))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                    ((TextView) battleview.findViewById(getId(KcaUtils.format("fm_%d_name", mvp_idx), R.id.class) ))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                }
                if (api_data.has("api_mvp_combined") && !api_data.get("api_mvp_combined").isJsonNull()) {
                    int mvp_idx_combined = api_data.get("api_mvp_combined").getAsInt();
                    if (mvp_idx_combined != -1) {
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_name", mvp_idx), R.id.class) ))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                        ((TextView) battleview.findViewById(getId(KcaUtils.format("fs_%d_name", mvp_idx_combined), R.id.class)))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                    }
                }
                if (api_data.has("api_get_ship")) {
                    int ship_id = api_data.getAsJsonObject("api_get_ship").get("api_ship_id").getAsInt();
                    String ship_name = api_data.getAsJsonObject("api_get_ship").get("api_ship_name").getAsString();
                    ((TextView) battleview.findViewById(R.id.battle_getship))
                            .setText(getShipTranslation(ship_name, ship_id, false));
                } else if (!is_practice && checkUserPortEnough()) {
                    ((TextView) battleview.findViewById(R.id.battle_getship))
                            .setText(getString(R.string.getship_none));
                }
                if (api_data.has("api_get_useitem")) {
                    int useitem_id = api_data.getAsJsonObject("api_get_useitem").get("api_useitem_id").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_getitem))
                            .setText(KcaUtils.format("%s (%d)", getUseItemNameById(useitem_id), getUseitemCount(useitem_id)));
                }

                if (!is_practice) {
                    JsonArray exp_data = api_data.getAsJsonArray("api_get_exp_lvup");
                    JsonArray exp_add = api_data.getAsJsonArray("api_get_ship_exp");
                    JsonArray exp_data_combined = new JsonArray();
                    JsonArray exp_add_combined = new JsonArray();
                    if (api_data.has("api_get_exp_lvup_combined") && api_data.get("api_get_exp_lvup_combined").isJsonArray()) {
                        exp_data_combined = api_data.getAsJsonArray("api_get_exp_lvup_combined");
                        exp_add_combined = api_data.getAsJsonArray("api_get_ship_exp_combined");
                    }

                    String map_text = KcaUtils.format("%d-%d", KcaBattle.currentMapArea, KcaBattle.currentMapNo);
                    JsonObject leveling_data = new JsonObject();
                    JsonArray leveling_track = dbHelper.getJsonArrayValue(DB_KEY_EXPCALTRK);
                    if (leveling_track != null) {
                        for (int i = 0; i < leveling_track.size(); i++) {
                            JsonObject item = leveling_track.get(i).getAsJsonObject();
                            leveling_data.add(item.get("api_id").getAsString(), item);
                        }
                    }
                    for (int i = 0; i < deckData.size(); i++) {
                        JsonObject deck = deckData.get(i).getAsJsonObject();
                        JsonArray ship_list = deck.getAsJsonArray("api_ship");
                        String view_id_format = "";

                        for (int j = 0; j < ship_list.size(); j++) {
                            String ship_id = ship_list.get(j).getAsString();
                            int current_exp = 0;
                            if (i == 0) view_id_format = KcaUtils.format("fm_%d_exp_count", j+1);
                            else if (i == 1) view_id_format = KcaUtils.format("fs_%d_exp_count", j+1);

                            if (leveling_data.has(ship_id)) {
                                JsonObject ship_data = leveling_data.getAsJsonObject(ship_id);
                                int target_exp = ship_data.get("target_exp").getAsInt();
                                int mapexp = ship_data.get("mapexp").getAsInt();
                                if (i == 0) {
                                    current_exp = exp_data.get(j).getAsJsonArray().get(0).getAsInt()
                                            + Math.max(0, exp_add.get(j + 1).getAsInt());
                                } else if (i == 1) {
                                    current_exp = exp_data_combined.get(j).getAsJsonArray().get(0).getAsInt()
                                            + Math.max(0, exp_add_combined.get(j + 1).getAsInt());
                                }
                                int remainexp = Math.max(0, target_exp - current_exp);
                                int left_count = (int) Math.ceil((double) remainexp / mapexp);
                                ((TextView) battleview.findViewById(getId(view_id_format, R.id.class))).setText(String.valueOf(left_count));
                            } else {
                                ((TextView) battleview.findViewById(getId(view_id_format, R.id.class))).setText("");
                            }
                        }
                    }
                }
            } else {
                Log.e("KCA", api_data.entrySet().toString());
            }
        } else {
            Log.e("KCA", "api_data is null");
        }
    }

    public void setViewLayout(boolean fc_flag, boolean ec_flag) {
        LinearLayout friend_main_fleet = battleview.findViewById(R.id.friend_main_fleet);
        LinearLayout friend_combined_fleet = battleview.findViewById(R.id.friend_combined_fleet);
        LinearLayout enemy_main_fleet = battleview.findViewById(R.id.enemy_main_fleet);
        LinearLayout enemy_combined_fleet = battleview.findViewById(R.id.enemy_combined_fleet);
        Log.e("KCA", fc_flag + "-" + ec_flag);

        friend_combined_fleet.setVisibility(fc_flag ? View.VISIBLE : View.GONE);
        enemy_combined_fleet.setVisibility(ec_flag ? View.VISIBLE : View.GONE);

        if (fc_flag && ec_flag) {
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
            friend_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
            enemy_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.25f));
        } else if (fc_flag) {
            enemy_combined_fleet.setVisibility(View.GONE);
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.35f));
            friend_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
        } else if (ec_flag) {
            friend_combined_fleet.setVisibility(View.GONE);
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.35f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
            enemy_combined_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.325f));
        } else {
            friend_combined_fleet.setVisibility(View.GONE);
            enemy_combined_fleet.setVisibility(View.GONE);
            friend_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.5f));
            enemy_main_fleet.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.5f));
        }
    }

    private boolean isShipIdAvailable(int id) {
        int realID = id - 1;
        if (realID < 100) { // Main
            if (realID < 20) {
                return friendShipData.size() > realID;
            } else {
                return enemyShipData.size() > realID - 20;
            }
        } else { // Combined
            realID = realID % 100;
            if (realID < 20) {
                return friendCombinedShipData.size() >realID;
            } else {
                return enemyCombinedShipData.size() >realID - 20;
            }
        }
    }

    private void setItemViewLayout(int id) {
        if (api_data == null || id == -1) return;
        int realID = id - 1;
        JsonObject data;
        Log.e("KCA", String.valueOf(realID));
        if (realID < 100) { // Main
            if (realID < 20) {
                data = friendShipData.get(realID).getAsJsonObject();
            } else {
                data = enemyShipData.get(realID - 20).getAsJsonObject();
            }
        } else { // Combined
            realID = realID % 100;
            if (realID < 20) {
                data = friendCombinedShipData.get(realID).getAsJsonObject();
            } else {
                data = enemyCombinedShipData.get(realID - 20).getAsJsonObject();
            }
        }
        Log.e("KCA", data.toString());
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
                itemView.findViewById(slotViewList[i]).setVisibility(View.GONE);
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            } else {
                slot_count += 1;
                JsonObject kcItemData;
                int lv = 0;
                int alv = -1;
                if (onslot != null) {
                    Log.e("KCA", "item_id: " + String.valueOf(item_id));
                    kcItemData = getUserItemStatusById(item_id, "level,alv", "type,name");
                    if (kcItemData == null) continue;
                    Log.e("KCA", kcItemData.toString());
                    lv = kcItemData.get("level").getAsInt();
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
                        itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                    }
                } else {
                    kcItemData = getKcItemStatusById(item_id, "type,name");
                    itemView.findViewById(getId(KcaUtils.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                    itemView.findViewById(getId(KcaUtils.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                    itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
                }

                if (kcItemData != null) {
                    String kcItemName = getSlotItemTranslation(kcItemData.get("name").getAsString());
                    int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();
                    int typeres = KcaApiData.getTypeRes(type);
                    ((TextView) itemView.findViewById(getId(KcaUtils.format("item%d_name", i + 1), R.id.class))).setText(kcItemName);
                    ((ImageView) itemView.findViewById(getId(KcaUtils.format("item%d_icon", i + 1), R.id.class))).setImageResource(typeres);
                    itemView.findViewById(slotViewList[i]).setVisibility(View.VISIBLE);
                }
            }
        }

        if (onslot_count == 0) {
            for (int i = 0; i < slot.size(); i++) {
                itemView.findViewById(getId(KcaUtils.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            }
        }

        if (slot_ex > 0) {
            // EX_SLOT
            slot_count += 1;
            JsonObject kcItemData = getUserItemStatusById(slot_ex, "level", "type,name");
            if (kcItemData != null) {
                String kcItemName = getSlotItemTranslation(kcItemData.get("name").getAsString());
                int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();
                int lv = kcItemData.get("level").getAsInt();
                int typeres = KcaApiData.getTypeRes(type);
                ((TextView) itemView.findViewById(R.id.item_ex_name)).setText(kcItemName);
                ((ImageView) itemView.findViewById(R.id.item_ex_icon)).setImageResource(typeres);
                if (lv > 0) {
                    ((TextView) itemView.findViewById(R.id.item_ex_level))
                            .setText(getString(R.string.lv_star).concat(String.valueOf(lv)));
                    itemView.findViewById(R.id.item_ex_level).setVisibility(View.VISIBLE);
                } else {
                    itemView.findViewById(R.id.item_ex_level).setVisibility(View.GONE);
                }
                itemView.findViewById(R.id.view_slot_ex).setVisibility(View.VISIBLE);
            } else {
                itemView.findViewById(R.id.view_slot_ex).setVisibility(View.INVISIBLE);
            }
        } else {
            itemView.findViewById(R.id.view_slot_ex).setVisibility(View.GONE);
        }

        if (slot_count == 0) {
            ((TextView) itemView.findViewById(R.id.item1_name)).setText(getString(R.string.slot_empty));
            ((ImageView) itemView.findViewById(R.id.item1_icon)).setImageResource(R.mipmap.item_0);
            itemView.findViewById(R.id.item1_level).setVisibility(View.GONE);
            itemView.findViewById(R.id.item1_alv).setVisibility(View.GONE);
            itemView.findViewById(R.id.item1_slot).setVisibility(View.GONE);
            itemView.findViewById(slotViewList[0]).setVisibility(View.VISIBLE);
        }
        itemView.setVisibility(View.VISIBLE);
    }

    public int setView() {
        try {
            error_flag = false;
            setBattleView();
            setAirCombatView();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            sendReport(e, ERORR_VIEW);
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
                active = true;
                view_status = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_VIEW_YLOC));
                dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
                deckInfoCalc = new KcaDeckInfo(getBaseContext());
                customToast = new KcaCustomToast(getApplicationContext());
                prefs = PreferenceManager.getDefaultSharedPreferences(this);

                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                mInflater = LayoutInflater.from(this);
                battleViewLayout = (DraggableOverlayLayout) mInflater.inflate(R.layout.view_sortie_battle, null);
                snapIndicator = new SnapIndicator(this, windowManager, mInflater);

                KcaUtils.resizeFullWidthView(getApplicationContext(), battleViewLayout);
                battleViewLayout.setVisibility(View.GONE);
                battleview = battleViewLayout.findViewById(R.id.battleview);
                battleview.findViewById(R.id.battleviewpanel).setOnTouchListener(draggableLayoutTouchListener);
                battleview.findViewById(R.id.battleviewpanel).setOnClickListener(onClickListener);
                battleview.findViewById(R.id.battle_node_area).setOnTouchListener(infoListViewTouchListener);
                itemView = mInflater.inflate(R.layout.view_battleview_items, null);
                acView = mInflater.inflate(R.layout.view_battleview_aircombat, null);
                acView.findViewById(R.id.view_ac_head).setOnTouchListener(acViewTouchListener);
                acView.findViewById(R.id.view_ac_phase1_0_f).setOnTouchListener(acViewTouchListener);
                acView.findViewById(R.id.view_ac_phase1_0_e).setOnTouchListener(acViewTouchListener);
                acView.findViewById(R.id.view_ac_phase2_0_f).setOnTouchListener(acViewTouchListener);
                acView.findViewById(R.id.view_ac_phase2_0_e).setOnTouchListener(acViewTouchListener);
                ((TextView) acView.findViewById(R.id.view_ac_title)).setText(getString(R.string.battleview_menu0));
                menuView = mInflater.inflate(R.layout.view_battleview_menu, null);
                menuView.setVisibility(View.GONE);
                menuView.findViewById(R.id.view_head).setOnClickListener(battleViewMenuListener);
                menuView.findViewById(R.id.view_item0).setOnClickListener(battleViewMenuListener);
                menuView.findViewById(R.id.view_item1).setOnClickListener(battleViewMenuListener);
                menuView.findViewById(R.id.view_item2).setOnClickListener(battleViewMenuListener);
                ((TextView) menuView.findViewById(R.id.view_bm_title)).setText(getString(R.string.battleview_menu_head));

                layoutParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        getWindowLayoutType(),
                        getWindowLayoutParamsFlags(getResources().getConfiguration()),
                        PixelFormat.TRANSLUCENT);
                layoutParams.gravity = Gravity.TOP;
                // Hide at bottom before the fleetView is first rendered
                updateScreenSize();
                layoutParams.y = screenHeight;
                setPreferences(getApplicationContext(), PREF_VIEW_YLOC, view_status);

                battleViewLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (bottom - top == oldBottom - oldTop) return;
                    fleetViewHeight = bottom - top;
                    updateScreenSize();
                    layoutParams.y = (screenHeight - fleetViewHeight) * (view_status + 1) / 2;
                    if ((battleViewLayout.getParent() != null)) {
                        windowManager.updateViewLayout(battleViewLayout, layoutParams);
                    }
                });
                windowManager.addView(battleViewLayout, layoutParams);

                refreshreceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.e("KCA", "=> Received Intent");
                        api_data = KcaBattle.getCurrentApiData();
                        if (api_data != null && api_data.has("api_heavy_damaged")) {
                            int value = api_data.get("api_heavy_damaged").getAsInt();
                            if (value == HD_DANGER) {
                                battleViewLayout.findViewById(R.id.battleviewpanel)
                                        .setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.colorHeavyDmgStateTransparentPanel)));
                            } else {
                                battleViewLayout.findViewById(R.id.battleviewpanel)
                                        .setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryTransparentDark)));
                            }
                        }
                        int setViewResult = setView();
                        if (setViewResult == 0) {
                            if (KcaViewButtonService.getClickCount() == 0) {
                                battleViewLayout.setVisibility(View.GONE);
                            }
                            battleViewLayout.invalidate();
                            windowManager.updateViewLayout(battleViewLayout, layoutParams);
                        }
                    }
                };
                LocalBroadcastManager.getInstance(this).registerReceiver(refreshreceiver, new IntentFilter(KCA_MSG_BATTLE_VIEW_REFRESH));
            } catch (Exception e) {
                active = false;
                error_flag = true;
                sendReport(e, ERORR_INIT);
                stopSelf();
            }
        }
    }

    private void setBattleViewMenu() {
        int idx = KcaBattle.currentFleet;
        if (menuView == null || idx == -1) return;

        List<String> infoList = new ArrayList<>();
        float[] exp_score = dbHelper.getExpScore();
        String exp_str = KcaUtils.format(getString(R.string.battleview_expview),
                exp_score[0], exp_score[1]);
        infoList.add(exp_str);

        JsonArray data = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
        int cn = getSeekCn();
        boolean isCombined = KcaBattle.isCombined;
        String airPowerValue = "";
        if (isCombined) {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(data, 0, KcaBattle.getEscapeFlag());
        } else {
            airPowerValue = deckInfoCalc.getAirPowerRangeString(data, idx, null);
        }
        if (!airPowerValue.isEmpty()) {
            infoList.add(airPowerValue);
        }

        double seekValue = 0;
        String seekStringValue = "";
        if (isCombined) {
            seekValue = deckInfoCalc.getSeekValue(data, "0,1", cn, KcaBattle.getEscapeFlag());
        } else {
            seekValue = deckInfoCalc.getSeekValue(data, String.valueOf(idx), cn, null);
        }
        if (cn == SEEK_PURE) {
            seekStringValue = KcaUtils.format(getString(R.string.kca_toast_seekvalue_d), deckInfoCalc.getSeekType(cn), (int) seekValue);
        } else {
            seekStringValue = KcaUtils.format(getString(R.string.kca_toast_seekvalue_f), deckInfoCalc.getSeekType(cn), seekValue);
        }
        infoList.add(seekStringValue);

        String speedStringValue = "";
        if (isCombined) {
            speedStringValue = deckInfoCalc.getSpeedString(data, "0,1", KcaBattle.getEscapeFlag());
        } else {
            speedStringValue = deckInfoCalc.getSpeedString(data, String.valueOf(idx), null);
        }
        infoList.add(speedStringValue.concat(getString(R.string.speed_postfix)));

        String tpValue = "";
        if (isCombined) {
            tpValue = deckInfoCalc.getTPString(data, "0,1", KcaBattle.getEscapeFlag());
        } else {
            tpValue = deckInfoCalc.getTPString(data, String.valueOf(idx), null);
        }
        infoList.add(tpValue);
        ((TextView) menuView.findViewById(R.id.view_menu_fleetinfo)).setText(joinStr(infoList, "\n"));
        sendUserAnalytics(getApplicationContext(), BV_BTN_PRESS, null);
    }

    private void setAirCombatContact(String prefix, JsonObject data) {
        ((TextView) acView.findViewById(getId(prefix.concat("0_msg"), R.id.class))).setText(
                getString(R.string.contact_term));
        TextView friendContactText = acView.findViewById(getId(prefix.concat("0_f"), R.id.class));
        TextView enemyContactText = acView.findViewById(getId(prefix.concat("0_e"), R.id.class));
        ImageView friendContactIcon = acView.findViewById(getId(prefix.concat("0_f_icon"), R.id.class));
        ImageView enemyContactIcon = acView.findViewById(getId(prefix.concat("0_e_icon"), R.id.class));
        TextView friendContactDm = acView.findViewById(getId(prefix.concat("0_f_dm"), R.id.class));
        TextView enemyContactDm = acView.findViewById(getId(prefix.concat("0_e_dm"), R.id.class));

        if (!data.get("api_stage1").isJsonNull()) {
            JsonObject stage1_data = data.getAsJsonObject("api_stage1");
            JsonArray touch_plane = stage1_data.getAsJsonArray("api_touch_plane");
            if (touch_plane != null && !touch_plane.isJsonNull()) {
                int friend_contact = touch_plane.get(0).getAsInt();
                if (friend_contact != -1) {
                    JsonObject f_data = getKcItemStatusById(friend_contact, "name,type,houm");
                    if (f_data != null) {
                        friendContactText.setText(getSlotItemTranslation(f_data.get("name").getAsString()));
                        int type = f_data.getAsJsonArray("type").get(3).getAsInt();
                        int typeres = KcaApiData.getTypeRes(type);
                        friendContactIcon.setImageResource(typeres);
                        int houm_val = Math.min(f_data.get("houm").getAsInt(), 3);
                        int dm_value = contact_bonus[houm_val];
                        friendContactDm.setText(KcaUtils.format(getString(R.string.contact_dm), dm_value));
                    } else {
                        friendContactText.setText("???");
                        friendContactDm.setText("???");
                        friendContactIcon.setImageResource(R.mipmap.item_0);
                    }
                    friendContactIcon.setVisibility(View.VISIBLE);
                    friendContactDm.setVisibility(View.VISIBLE);
                    friendContactText.setSelected(true);
                } else {
                    friendContactText.setText(getString(R.string.contact_none));
                    friendContactDm.setVisibility(View.GONE);
                    friendContactIcon.setVisibility(View.GONE);
                }

                int enemy_contact = touch_plane.get(1).getAsInt();
                if (enemy_contact != -1) {
                    JsonObject e_data = getKcItemStatusById(enemy_contact, "name,type,houm");
                    if (e_data != null) {
                        enemyContactText.setText(getSlotItemTranslation(e_data.get("name").getAsString()));
                        int type = e_data.getAsJsonArray("type").get(3).getAsInt();
                        int typeres = KcaApiData.getTypeRes(type);
                        enemyContactIcon.setImageResource(typeres);
                        int houm_val = Math.min(e_data.get("houm").getAsInt(), 3);
                        int dm_value = contact_bonus[houm_val];
                        enemyContactDm.setText(KcaUtils.format(getString(R.string.contact_dm), dm_value));
                    } else {
                        enemyContactText.setText("???");
                        enemyContactDm.setText("???");
                        enemyContactIcon.setImageResource(R.mipmap.item_0);
                    }
                    enemyContactIcon.setVisibility(View.VISIBLE);
                    enemyContactDm.setVisibility(View.VISIBLE);
                    enemyContactText.setSelected(true);
                } else {
                    enemyContactText.setText(getString(R.string.contact_none));
                    enemyContactDm.setVisibility(View.GONE);
                    enemyContactIcon.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setAirCombatTextView(String prefix, JsonObject data) {
        String countformat = "%d/%d (-%d)";
        for (int n = 1; n <= 2; n++) {
            String n_str = String.valueOf(n);
            TextView friendView = acView.findViewById(getId(prefix.concat(n_str).concat("_f"), R.id.class));
            TextView enemyView = acView.findViewById(getId(prefix.concat(n_str).concat("_e"), R.id.class));
            TextView msgView = acView.findViewById(getId(prefix.concat(n_str).concat("_msg"), R.id.class));
            if (!data.get("api_stage".concat(n_str)).isJsonNull()) {
                JsonObject stage_data = data.getAsJsonObject("api_stage".concat(String.valueOf(n)));
                int f_count = stage_data.get("api_f_count").getAsInt();
                int f_lostcount = stage_data.get("api_f_lostcount").getAsInt();
                int e_count = stage_data.get("api_e_count").getAsInt();
                int e_lostcount = stage_data.get("api_e_lostcount").getAsInt();

                friendView.setText(KcaUtils.format(countformat, f_count - f_lostcount, f_count, f_lostcount));
                if (f_count > 0 && f_count - f_lostcount <= 0) {
                    friendView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPlaneWipedText));
                } else {
                    friendView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }

                enemyView.setText(KcaUtils.format(countformat, e_count - e_lostcount, e_count, e_lostcount));
                if (e_count > 0 && e_count - e_lostcount <= 0) {
                    enemyView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPlaneWipedText));
                } else {
                    enemyView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }

                if (n == 1) {
                    if (stage_data.has("api_disp_seiku")) {
                        msgView.setText(getAirForceResultString(this,
                                stage_data.get("api_disp_seiku").getAsInt()));
                    } else {
                        msgView.setText("-");
                    }
                } else if (n == 2) {
                    if (stage_data.has("api_air_fire")) {
                        String airfireformat = "#%d";
                        msgView.setText(KcaUtils.format(airfireformat,
                                stage_data.getAsJsonObject("api_air_fire").get("api_idx").getAsInt() + 1));
                    } else {
                        msgView.setText("-");
                    }
                }
            } else {
                friendView.setText("");
                enemyView.setText("");
                msgView.setText("");
            }
        }
    }

    private void setAirCombatView() {
        if (api_data != null && !api_data.has("api_enemy_info") && !api_data.has("api_flare_pos")) {
            if (api_data.has("api_plane_from")) {
                if (api_data.get("api_plane_from").isJsonNull()) {
                    acView.findViewById(R.id.view_ac_phase1).setVisibility(View.GONE);
                }
            }

            if (api_data.has("api_kouku")) {
                JsonObject kdata = api_data.getAsJsonObject("api_kouku");
                ((TextView) acView.findViewById(R.id.view_ac_phase1_title))
                        .setText(KcaUtils.format(getString(R.string.phase_term), 1));
                setAirCombatTextView("view_ac_phase1_", kdata);
                setAirCombatContact("view_ac_phase1_", kdata);
                acView.findViewById(R.id.view_ac_phase1).setVisibility(View.VISIBLE);
            } else {
                acView.findViewById(R.id.view_ac_phase1).setVisibility(View.GONE);
            }

            if (api_data.has("api_kouku2")) {
                JsonObject kdata = api_data.getAsJsonObject("api_kouku2");
                ((TextView) acView.findViewById(R.id.view_ac_phase2_title))
                        .setText(KcaUtils.format(getString(R.string.phase_term), 2));
                setAirCombatTextView("view_ac_phase2_", kdata);
                setAirCombatContact("view_ac_phase2_", kdata);
                acView.findViewById(R.id.view_ac_phase2).setVisibility(View.VISIBLE);
            } else {
                acView.findViewById(R.id.view_ac_phase2).setVisibility(View.GONE);
            }

            if (api_data.has("api_air_base_injection")) {
                JsonObject kdata = api_data.getAsJsonObject("api_air_base_injection");
                ((TextView) acView.findViewById(R.id.view_ac_abinj_title))
                        .setText(getString(R.string.injection_term)
                                .concat("/").concat(getString(R.string.airbase_term)));
                setAirCombatTextView("view_ac_abinj_", kdata);
                acView.findViewById(R.id.view_ac_abinj).setVisibility(View.VISIBLE);
            } else {
                acView.findViewById(R.id.view_ac_abinj).setVisibility(View.GONE);
            }

            if (api_data.has("api_injection_kouku")) {
                JsonObject kdata = api_data.getAsJsonObject("api_injection_kouku");
                ((TextView) acView.findViewById(R.id.view_ac_inj_title))
                        .setText(getString(R.string.injection_term));
                setAirCombatTextView("view_ac_inj_", kdata);
                acView.findViewById(R.id.view_ac_inj).setVisibility(View.VISIBLE);
            } else {
                acView.findViewById(R.id.view_ac_inj).setVisibility(View.GONE);
            }

            if (api_data.has("api_destruction_battle")) {
                String viewidformat = "view_ac_aba_%d";
                JsonObject kdata = api_data.getAsJsonObject("api_destruction_battle").getAsJsonObject("api_air_base_attack");
                ((TextView) acView.findViewById(R.id.view_ac_aba_0_title))
                        .setText(getString(R.string.airbase_term).concat(" ")
                                .concat(KcaUtils.format(getString(R.string.phase_term), 1)));
                setAirCombatTextView("view_ac_aba_0_", kdata);
                acView.findViewById(R.id.view_ac_aba_0).setVisibility(View.VISIBLE);
                for (int i = 1; i < 6; i++) {
                    String viewid = KcaUtils.format(viewidformat, i);
                    acView.findViewById(getId(viewid, R.id.class)).setVisibility(View.GONE);
                }
                acView.findViewById(R.id.view_ac_aba).setVisibility(View.VISIBLE);
            } else if (api_data.has("api_air_base_attack")) {
                JsonArray kdata_list = api_data.getAsJsonArray("api_air_base_attack");
                String viewidformat = "view_ac_aba_%d";
                for (int i = 0; i < 6; i++) {
                    String viewid = KcaUtils.format(viewidformat, i);
                    if (i >= kdata_list.size()) {
                        acView.findViewById(getId(viewid, R.id.class)).setVisibility(View.GONE);
                        Log.e("KCA-BV", viewid + " not available");
                    } else {
                        JsonObject kdata = kdata_list.get(i).getAsJsonObject();
                        ((TextView) acView.findViewById(getId(viewid.concat("_title"), R.id.class)))
                                .setText(getString(R.string.airbase_term).concat(" ")
                                        .concat(KcaUtils.format(getString(R.string.phase_term), i + 1)));
                        setAirCombatTextView(viewid.concat("_"), kdata);
                        acView.findViewById(getId(viewid, R.id.class)).setVisibility(View.VISIBLE);
                        Log.e("KCA-BV", viewid + " " + kdata.toString());
                    }
                }
                acView.findViewById(R.id.view_ac_aba).setVisibility(View.VISIBLE);
            } else {
                acView.findViewById(R.id.view_ac_aba).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroy() {
        active = false;
        deckportdata = null;
        if (battleViewLayout != null) {
            if (battleViewLayout.getParent() != null) windowManager.removeViewImmediate(battleViewLayout);
        }
        if (itemView != null) {
            if (itemView.getParent() != null) windowManager.removeViewImmediate(itemView);
        }
        if (menuView != null) {
            if (menuView.getParent() != null) windowManager.removeViewImmediate(menuView);
        }
        if (snapIndicator != null) {
            snapIndicator.remove();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(SHOW_BATTLEVIEW_ACTION)) {
                if (!error_flag) {
                    battleview.findViewById(R.id.fleetview).setOnTouchListener(fleetViewTouchListener);
                    if (battleViewLayout != null) battleViewLayout.setVisibility(View.VISIBLE);
                    sendUserAnalytics(getApplicationContext(), OPEN_BATTEVIEW, null);
                }
            }
            if (intent.getAction().equals(HIDE_BATTLEVIEW_ACTION)) {
                if (battleViewLayout != null) battleViewLayout.setVisibility(View.GONE);
                if (itemView != null) itemView.setVisibility(View.GONE);
                JsonObject statProperties = new JsonObject();
                statProperties.addProperty("manual", true);
                sendUserAnalytics(getApplicationContext(), CLOSE_BATTEVIEW, statProperties);
                snapIndicator.remove();
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
            int maxY = screenHeight - battleViewLayout.getHeight();
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
                        battleViewLayout.cancelAnimations();
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
                        battleViewLayout.animateTo(layoutParams.x, layoutParams.y,
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

                        windowManager.updateViewLayout(battleViewLayout, layoutParams);
                        snapIndicator.update(finalY, maxY);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    battleViewLayout.cancelAnimations();
                    snapIndicator.remove();
                    layoutParams.x = startViewX;
                    layoutParams.y = startViewY;
                    windowManager.updateViewLayout(battleViewLayout, layoutParams);
                    break;
            }
            return false;
        }
    };


    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (abs(layoutParams.x - startViewX) < 20 && abs(layoutParams.y - startViewY) < 20) {
                if (battleViewLayout != null) battleViewLayout.setVisibility(View.GONE);
                if (itemView != null) itemView.setVisibility(View.GONE);
                JsonObject statProperties = new JsonObject();
                statProperties.addProperty("manual", false);
                sendUserAnalytics(getApplicationContext(), CLOSE_BATTEVIEW, statProperties);
            }
        }
    };

    private int screenWidth, screenHeight;
    private int popupWidth, popupHeight;
    private float acTouchX, acTouchY;
    private int acViewX, acViewY;

    private void initAcViewParams() {
        acView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWidth = acView.getMeasuredWidth();
        popupHeight = acView.getMeasuredHeight();
        acViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        acViewParams.gravity = Gravity.TOP | Gravity.START;
        updateScreenSize();

        acViewParams.x = (screenWidth - popupWidth) / 2;
        acViewParams.y = (screenHeight - popupHeight) / 2;
    }

    private final View.OnTouchListener acViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.getId() == acView.findViewById(R.id.view_ac_head).getId()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        acTouchX = event.getRawX();
                        acTouchY = event.getRawY();
                        acViewX = acViewParams.x;
                        acViewY = acViewParams.y;
                        Log.e("KCA", "ACTION_DOWN");
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        break;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            if (v.getId() == acView.findViewById(R.id.view_ac_head).getId()) {
                                acView.setVisibility(View.GONE);
                                windowManager.removeViewImmediate(acView);
                            }
                        }
                        Log.e("KCA", "ACTION_UP");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int x = (int) (event.getRawX() - acTouchX);
                        int y = (int) (event.getRawY() - acTouchY);

                        acViewParams.x = acViewX + x;
                        acViewParams.y = acViewY + y;
                        if (acViewParams.x < 0) acViewParams.x = 0;
                        else if (acViewParams.x > screenWidth - popupWidth)
                            acViewParams.x = screenWidth - popupWidth;
                        if (acViewParams.y < 0) acViewParams.y = 0;
                        else if (acViewParams.y > screenHeight - popupHeight)
                            acViewParams.y = screenHeight - popupHeight;
                        windowManager.updateViewLayout(acView, acViewParams);
                        break;

                    default:
                        break;
                }
            } else if (v instanceof TextView){
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    String val = (String) ((TextView) v).getText();
                    showCustomToast(customToast, val, Toast.LENGTH_LONG, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
            }
            return true;
        }
    };

    private final View.OnTouchListener fleetViewTouchListener = new View.OnTouchListener() {
        private int getShipId(float x, float y) {
            for (int i = 1; i <= 7; i++) {
                if (isInsideView(battleview.findViewById(getId(KcaUtils.format("fm_%d", i), R.id.class)), x, y)) {
                    api_data.addProperty("api_touched_idx", i);
                    if (friendShipData == null || i > friendShipData.size()) return -1;
                    return i;
                }
            }

            for (int i = 1; i <= 6; i++) {
                if (isInsideView(battleview.findViewById(getId(KcaUtils.format("fs_%d", i), R.id.class)), x, y)) {
                    api_data.addProperty("api_touched_idx", i + 100);
                    if (friendCombinedShipData == null || i > friendCombinedShipData.size()) return -1;
                    return i + 100;
                }
            }

            for (int i = 1; i <= 6; i++) {
                if (isInsideView(battleview.findViewById(getId(KcaUtils.format("em_%d", i), R.id.class)), x, y)) {
                    api_data.addProperty("api_touched_idx", i + 20);
                    if (enemyShipData == null || i > enemyShipData.size()) return -1;
                    return i + 20;
                }
            }

            for (int i = 1; i <= 6; i++) {
                if (isInsideView(battleview.findViewById(getId(KcaUtils.format("es_%d", i), R.id.class)), x, y)) {
                    api_data.addProperty("api_touched_idx", i + 120);
                    if (enemyCombinedShipData == null || i > enemyCombinedShipData.size()) return -1;
                    return i + 120;
                }
            }
            return -1;
        }
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

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (menuView.getParent() != null) {
                return false;
            }
            int xmargin = (int) getResources().getDimension(R.dimen.item_popup_xmargin);
            int ymargin = (int) getResources().getDimension(R.dimen.item_popup_ymargin);
            float x = event.getRawX();
            float y = event.getRawY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_DOWN:
                    if (api_data != null) {
                        try {
                            int newSelected = getShipId(x, y);
                            if (!isShipIdAvailable(newSelected)) {
                                return false;
                            }
                            if (selected != newSelected) {
                                setItemViewLayout(newSelected);
                            }
                            updateScreenSize();
                            if (event.getRawX() + itemView.getWidth() > screenWidth) {
                                itemViewParams.x = (int) (event.getRawX() - xmargin - itemView.getWidth());
                            } else {
                                itemViewParams.x = (int) (event.getRawX() + xmargin);
                            }
                            itemViewParams.y = (int) (event.getRawY() - ymargin - itemView.getHeight());
                            itemViewParams.gravity = Gravity.TOP | Gravity.START;
                            if (itemView.getParent() != null) {
                                if (newSelected == -1) {
                                    itemView.setVisibility(View.GONE);
                                } else if (selected == -1 || selected != newSelected) {
                                    // Selection changed
                                    windowManager.removeViewImmediate(itemView);
                                    windowManager.addView(itemView, itemViewParams);
                                } else {
                                    windowManager.updateViewLayout(itemView, itemViewParams);
                                }
                            } else if (newSelected != -1) {
                                windowManager.addView(itemView, itemViewParams);
                            }
                            selected = newSelected;

                        } catch (Exception e) {
                            e.printStackTrace();
                            sendReport(e, KcaBattleViewService.ERORR_ITEMVIEW);
                        }
                    }
                    Log.e("KCA", "ACTION_DOWN");
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    itemView.setVisibility(View.GONE);
                    selected = -1;
                    Log.e("KCA", "ACTION_UP");
                    break;
            }
            return false;
        }
    };

    private final View.OnTouchListener infoListViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (acView.getParent() != null) return true;
                    WindowManager.LayoutParams infoViewParams;
                    infoViewParams = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            getWindowLayoutType(),
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT);
                    setBattleViewMenu();
                    if (menuView.getParent() != null) {
                        windowManager.updateViewLayout(menuView, infoViewParams);
                    } else {
                        windowManager.addView(menuView, infoViewParams);
                        menuView.setVisibility(View.VISIBLE);
                    }
                    return true;
                default:
                    return false;
            }
        }
    };

    private final View.OnClickListener battleViewMenuListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent qintent;
            Log.e("KCA-BV", getResources().getResourceEntryName(v.getId()));
            JsonObject statProperties = new JsonObject();

            switch (v.getId()) {
                case R.id.view_item0:
                    statProperties.addProperty("type", "air_calulation");
                    sendUserAnalytics(getApplicationContext(), BV_BTN_PRESS.concat("Menu"), statProperties);
                    initAcViewParams();
                    if (acView != null && acView.getParent() != null) {
                        windowManager.updateViewLayout(acView, acViewParams);
                    } else {
                        acView.setVisibility(View.VISIBLE);
                        windowManager.addView(acView, acViewParams);
                    }
                    break;
                case R.id.view_item1:
                    statProperties.addProperty("type", "quest_view");
                    sendUserAnalytics(getApplicationContext(), BV_BTN_PRESS.concat("Menu"), statProperties);
                    qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                    qintent.setAction(SHOW_QUESTVIEW_ACTION_NEW);
                    startService(qintent);
                    break;
                case R.id.view_item2:
                    statProperties.addProperty("type", "enter_fleetview");
                    sendUserAnalytics(getApplicationContext(), BV_BTN_PRESS.concat("Menu"), statProperties);
                    qintent = new Intent(getBaseContext(), KcaFleetViewService.class);
                    qintent.setAction(SHOW_FLEETVIEW_ACTION);
                    startService(qintent);
                    break;
                case R.id.view_head:
                    break;
                default:
                    break;
            }
            menuView.setVisibility(View.GONE);
            if (menuView.getParent() != null) {
                windowManager.removeViewImmediate(menuView);
            }
        }
    };

    private void sendReport(Exception e, int type) {
        error_flag = true;
        if (battleViewLayout != null) battleViewLayout.setVisibility(View.GONE);
        if (itemView != null) itemView.setVisibility(View.GONE);
        if (menuView != null) menuView.setVisibility(View.GONE);

        Toast.makeText(getApplicationContext(), getString(R.string.battleview_error), Toast.LENGTH_SHORT).show();

        String api_url = "url";
        JsonObject sendData = new JsonObject();
        if (api_data == null) {
            api_data = new JsonObject();
            api_data.addProperty("api_data", "api_data is null");
        }
        if (type == ERORR_ITEMVIEW) {
            api_url = api_data.get("api_url").getAsString();
            api_data.add("api_deckport", deckportdata);
            api_data.add("api_fs_data", friendShipData);
            api_data.add("api_fsc_data", friendCombinedShipData);
            api_data.add("api_es_data", enemyShipData);
            api_data.add("api_esc_data", enemyCombinedShipData);
        }
        sendData.addProperty("data", api_data.toString());
        sendData.addProperty("error", getStringFromException(e));

        KcaDBHelper helper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        helper.recordErrorLog(ERROR_TYPE_BATTLEVIEW, api_url, "BV", api_data.toString(), getStringFromException(e));
    }

    private int getSeekCn() {
        return Integer.valueOf(getStringPreferences(getApplicationContext(), PREF_KCA_SEEK_CN));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreenSize();
        int maxY = screenHeight - battleViewLayout.getMeasuredHeight();
        if (battleViewLayout != null) {
            battleViewLayout.cancelAnimations();
            snapIndicator.remove();
            layoutParams.x = 0;
            // Snap to either top, center, or bottom
            if (view_status == -1) {
                layoutParams.y = 0;
            } else if (view_status == 0) {
                layoutParams.y = maxY / 2;
            } else {
                layoutParams.y = maxY;
            }
            layoutParams.flags = getWindowLayoutParamsFlags(newConfig);
            snapIndicator = new SnapIndicator(getApplicationContext(), windowManager, mInflater);
            windowManager.updateViewLayout(battleViewLayout, layoutParams);
        }
    }

    public void showCustomToast(KcaCustomToast toast, String body, int duration, int color) {
        KcaUtils.showCustomToast(getApplicationContext(), getBaseContext(), toast, body, duration, color);
    }

    private boolean checkStart(String url) {
        if (url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT)) return false;
        if (url.equals(API_REQ_PRACTICE_MIDNIGHT_BATTLE)) return false;
        if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT)) return false;
        if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_EC)) return false;
        return true;
    }

    private void updateScreenSize() {
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: " + screenWidth + " " + screenHeight);
    }
}