package com.antest1.kcanotify;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.checkUserPortEnough;
import static com.antest1.kcanotify.KcaApiData.getAirForceResultString;
import static com.antest1.kcanotify.KcaApiData.getCurrentNodeAlphabet;
import static com.antest1.kcanotify.KcaApiData.getEngagementString;
import static com.antest1.kcanotify.KcaApiData.getFormationString;
import static com.antest1.kcanotify.KcaApiData.getItemString;
import static com.antest1.kcanotify.KcaApiData.getItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getNodeFullInfo;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getUserItemStatusById;
import static com.antest1.kcanotify.KcaApiData.isItemAircraft;
import static com.antest1.kcanotify.KcaConstants.*;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;


public class KcaBattleViewService extends Service {
    Context contextWithLocale;
    LayoutInflater mInflater;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver refreshreceiver, hdmgreceiver;
    public static boolean active;
    public static JsonObject api_data;
    public static String currentNodeInfo = "";
    public static int[] startNowHps;
    public static int[] startNowHpsCombined;

    public static JsonArray friendShipData, friendCombinedShipData;
    public static JsonArray enemyShipData, enemyCombinedShipData;

    static boolean error_flag = false;
    boolean fc_flag = false;
    boolean ec_flag = false;

    JsonArray api_formation;
    JsonObject api_kouku;

    private View mView, itemView;
    private WindowManager mManager;

    int displayWidth = 0;

    public static final int ERORR_INIT = 0;
    public static final int ERORR_VIEW = 1;
    public static final int ERORR_ITEMVIEW = 2;

    private int[] shipViewList = {0,
            R.id.fm_1, R.id.fm_2, R.id.fm_3, R.id.fm_4, R.id.fm_5, R.id.fm_6,
            R.id.em_1, R.id.em_2, R.id.em_3, R.id.em_4, R.id.em_5, R.id.em_6
    };

    private int[] shipNameViewList = {0,
            R.id.fm_1_name, R.id.fm_2_name, R.id.fm_3_name, R.id.fm_4_name, R.id.fm_5_name, R.id.fm_6_name,
            R.id.em_1_name, R.id.em_2_name, R.id.em_3_name, R.id.em_4_name, R.id.em_5_name, R.id.em_6_name
    };

    private int[] shipNameAreaViewList = {0,
            R.id.fm_1_name, R.id.fm_2_name, R.id.fm_3_name, R.id.fm_4_name, R.id.fm_5_name, R.id.fm_6_name,
            R.id.em_1_name_area, R.id.em_2_name_area, R.id.em_3_name_area, R.id.em_4_name_area, R.id.em_5_name_area, R.id.em_6_name_area
    };

    private int[] shipLevelViewList = {0,
            R.id.fm_1_lv, R.id.fm_2_lv, R.id.fm_3_lv, R.id.fm_4_lv, R.id.fm_5_lv, R.id.fm_6_lv,
            R.id.em_1_lv, R.id.em_2_lv, R.id.em_3_lv, R.id.em_4_lv, R.id.em_5_lv, R.id.em_6_lv
    };

    private int[] shipHpTxtViewList = {0,
            R.id.fm_1_hp_txt, R.id.fm_2_hp_txt, R.id.fm_3_hp_txt, R.id.fm_4_hp_txt, R.id.fm_5_hp_txt, R.id.fm_6_hp_txt,
            R.id.em_1_hp_txt, R.id.em_2_hp_txt, R.id.em_3_hp_txt, R.id.em_4_hp_txt, R.id.em_5_hp_txt, R.id.em_6_hp_txt
    };

    private int[] shipHpBarViewList = {0,
            R.id.fm_1_hp_bar, R.id.fm_2_hp_bar, R.id.fm_3_hp_bar, R.id.fm_4_hp_bar, R.id.fm_5_hp_bar, R.id.fm_6_hp_bar,
            R.id.em_1_hp_bar, R.id.em_2_hp_bar, R.id.em_3_hp_bar, R.id.em_4_hp_bar, R.id.em_5_hp_bar, R.id.em_6_hp_bar
    };

    private int[] shipYomiViewList = {0, 0, 0, 0, 0, 0, 0,
            R.id.em_1_yomi, R.id.em_2_yomi, R.id.em_3_yomi, R.id.em_4_yomi, R.id.em_5_yomi, R.id.em_6_yomi
    };

    private int[] shipCombinedViewList = {0,
            R.id.fs_1, R.id.fs_2, R.id.fs_3, R.id.fs_4, R.id.fs_5, R.id.fs_6,
            R.id.es_1, R.id.es_2, R.id.es_3, R.id.es_4, R.id.es_5, R.id.es_6
    };

    private int[] shipNameCombinedViewList = {0,
            R.id.fs_1_name, R.id.fs_2_name, R.id.fs_3_name, R.id.fs_4_name, R.id.fs_5_name, R.id.fs_6_name,
            R.id.es_1_name, R.id.es_2_name, R.id.es_3_name, R.id.es_4_name, R.id.es_5_name, R.id.es_6_name
    };

    private int[] shipNameAreaCombinedViewList = {0,
            R.id.fs_1_name, R.id.fs_2_name, R.id.fs_3_name, R.id.fs_4_name, R.id.fs_5_name, R.id.fs_6_name,
            R.id.es_1_name_area, R.id.es_2_name_area, R.id.es_3_name_area, R.id.es_4_name_area, R.id.es_5_name_area, R.id.es_6_name_area
    };

    private int[] shipLevelCombinedViewList = {0,
            R.id.fs_1_lv, R.id.fs_2_lv, R.id.fs_3_lv, R.id.fs_4_lv, R.id.fs_5_lv, R.id.fs_6_lv,
            R.id.es_1_lv, R.id.es_2_lv, R.id.es_3_lv, R.id.es_4_lv, R.id.es_5_lv, R.id.es_6_lv
    };

    private int[] shipHpTxtCombinedViewList = {0,
            R.id.fs_1_hp_txt, R.id.fs_2_hp_txt, R.id.fs_3_hp_txt, R.id.fs_4_hp_txt, R.id.fs_5_hp_txt, R.id.fs_6_hp_txt,
            R.id.es_1_hp_txt, R.id.es_2_hp_txt, R.id.es_3_hp_txt, R.id.es_4_hp_txt, R.id.es_5_hp_txt, R.id.es_6_hp_txt
    };

    private int[] shipHpBarCombinedViewList = {0,
            R.id.fs_1_hp_bar, R.id.fs_2_hp_bar, R.id.fs_3_hp_bar, R.id.fs_4_hp_bar, R.id.fs_5_hp_bar, R.id.fs_6_hp_bar,
            R.id.es_1_hp_bar, R.id.es_2_hp_bar, R.id.es_3_hp_bar, R.id.es_4_hp_bar, R.id.es_5_hp_bar, R.id.es_6_hp_bar
    };

    private int[] shipYomiCombinedViewList = {0, 0, 0, 0, 0, 0, 0,
            R.id.es_1_yomi, R.id.es_2_yomi, R.id.es_3_yomi, R.id.es_4_yomi, R.id.es_5_yomi, R.id.es_6_yomi
    };

    private int[] slotViewList = {R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5};

    WindowManager.LayoutParams mParams;
    ScrollView battleview;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean getStatus() {
        return active;
    }

    private static String makeHpString(int currenthp, int maxhp) {
        return String.format("HP %d/%d", currenthp, maxhp);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    private static String makeLvString(int level) {
        return String.format("Lv %d", level);
    }

    public static int getFriendIdx(int i) {
        return i;
    }

    public static int getEnemyIdx(int i) {
        return i + 6;
    }

    public static int getFriendCbIdx(int i) {
        return i - 6;
    }

    public static int getEnemyCbIdx(int i) {
        return i;
    }

    public static int getStatus(int value) {
        if (value > 75) return STATE_NORMAL;
        else if (value > 50) return STATE_LIGHTDMG;
        else if (value > 25) return STATE_MODERATEDMG;
        else return STATE_HEAVYDMG;
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

    public void setBattleview() {
        if (api_data != null) {
            boolean is_practice = api_data.has("api_practice_flag");

            if (api_data.has("api_maparea_id")) { // start, next
                Log.e("KCA", "START/NEXT");
                //Log.e("KCA", api_data.toString());
                int api_maparea_id = api_data.get("api_maparea_id").getAsInt();
                int api_mapinfo_no = api_data.get("api_mapinfo_no").getAsInt();
                int api_no = api_data.get("api_no").getAsInt();
                String currentNode = getCurrentNodeAlphabet(api_maparea_id, api_mapinfo_no, api_no);
                int api_event_id = api_data.get("api_event_id").getAsInt();
                int api_event_type = api_data.get("api_event_kind").getAsInt();
                int api_color_no = api_data.get("api_color_no").getAsInt();
                currentNodeInfo = getNodeFullInfo(contextWithLocale, currentNode, api_event_id, api_event_type, true);
                currentNodeInfo = currentNodeInfo.replaceAll("[()]", "");

                // View Settings
                fc_flag = KcaBattle.isCombined;
                ec_flag = (api_event_type == API_NODE_EVENT_KIND_ECBATTLE);
                setViewLayout(fc_flag, false);

                ((TextView) battleview.findViewById(R.id.battle_node)).setText(currentNodeInfo);
                ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");

                if (!getBooleanPreferences(contextWithLocale, PREF_SHOWDROP_SETTING)) {
                    battleview.findViewById(R.id.battle_getship_row).setVisibility(View.GONE);
                } else {
                    if (checkUserPortEnough()) {
                        ((TextView) battleview.findViewById(R.id.battle_getship)).setText("");
                    } else {
                        ((TextView) battleview.findViewById(R.id.battle_getship)).setText(getStringWithLocale(R.string.getship_max));
                    }
                }

                if (api_event_id == API_NODE_EVENT_ID_OBTAIN) {
                    JsonArray api_itemget = api_data.getAsJsonArray("api_itemget");
                    List<String> itemTextList = new ArrayList<String>();
                    for (int i = 0; i < api_itemget.size(); i++) {
                        JsonObject itemdata = api_itemget.get(i).getAsJsonObject();
                        String itemname = getItemString(contextWithLocale, itemdata.get("api_id").getAsInt());
                        int itemgetcount = itemdata.get("api_getcount").getAsInt();
                        itemTextList.add(String.format("%s +%d", itemname, itemgetcount));
                    }
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(KcaUtils.joinStr(itemTextList, " / "));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                } else if (api_event_id == API_NODE_EVENT_ID_AIR) {
                    JsonObject itemdata = api_data.getAsJsonObject("api_itemget");
                    String itemname = getItemString(contextWithLocale, itemdata.get("api_id").getAsInt());
                    int itemgetcount = itemdata.get("api_getcount").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(String.format("%s +%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItemSpecial));
                } else if (api_event_id == API_NODE_EVENT_ID_LOSS) {
                    JsonObject api_happening = api_data.getAsJsonObject("api_happening");
                    String itemname = getItemString(contextWithLocale, api_happening.get("api_mst_id").getAsInt());
                    int itemgetcount = api_happening.get("api_count").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(String.format("%s -%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorVortex));
                } else if (api_event_id == API_NODE_EVENT_ID_SENDAN) {
                    ((LinearLayout) mView.findViewById(R.id.battleviewpanel))
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    JsonObject api_itemget_eo_comment = api_data.getAsJsonObject("api_itemget_eo_comment");
                    String itemname = getItemString(contextWithLocale, api_itemget_eo_comment.get("api_id").getAsInt());
                    int itemgetcount = api_itemget_eo_comment.get("api_getcount").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText(String.format("%s +%d", itemname, itemgetcount));
                    ((TextView) battleview.findViewById(R.id.battle_result))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                }

                if (api_data.has("api_destruction_battle")) {
                    JsonObject api_destruction_battle = api_data.getAsJsonObject("api_destruction_battle");
                    int api_lost_kind = api_destruction_battle.get("api_lost_kind").getAsInt();
                    switch (api_lost_kind) {
                        case RAID_LOST_TYPE_1:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getStringWithLocale(R.string.raid_type1));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidDamaged));
                            break;
                        case RAID_LOST_TYPE_2:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getStringWithLocale(R.string.raid_type2));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidHeavyDamaged));
                            break;
                        case RAID_LOST_TYPE_3:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getStringWithLocale(R.string.raid_type3));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidDamaged));
                            break;
                        case RAID_LOST_TYPE_4:
                            ((TextView) battleview.findViewById(R.id.battle_result)).setText(getStringWithLocale(R.string.raid_type4));
                            ((TextView) battleview.findViewById(R.id.battle_result))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRaidNoDamaged));
                            break;
                        default:
                            break;
                    }
                    JsonObject api_air_based_attack = api_destruction_battle.getAsJsonObject("api_air_base_attack");
                    int api_disp_seiku = api_air_based_attack.getAsJsonObject("api_stage1").get("api_disp_seiku").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_airpower))
                            .setText(getAirForceResultString(contextWithLocale, api_disp_seiku));
                }

                switch (api_color_no) {
                    case 2:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                        break;
                    case 6:
                    case 9:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItemSpecial));
                        break;
                    case 3:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorVortex));
                        break;
                    case 4:
                        if (api_event_id == API_NODE_EVENT_ID_NOEVENT) {
                            if (api_event_type == API_NODE_EVENT_KIND_SELECTABLE) { // selectable
                                battleview.findViewById(R.id.battle_node)
                                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSelectable));
                            } else {
                                battleview.findViewById(R.id.battle_node)
                                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                            }

                        } else {
                            battleview.findViewById(R.id.battle_node)
                                    .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBattle));
                        }
                        break;
                    case 5:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBossBattle));
                        break;
                    case 7:
                    case 10:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAirBattle));
                        break;
                    case 8:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
                        break;
                    default:
                        break;
                }
            }

            if (api_data.has("api_deck_port")) { // common sortie, practice
                setViewLayout(fc_flag, ec_flag);

                if (is_practice) {
                    ((TextView) battleview.findViewById(R.id.battle_node)).setText(getStringWithLocale(R.string.node_info_practice));
                    ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                    ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                    ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                    ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");
                    battleview.findViewById(R.id.battle_getship_row).setVisibility(View.GONE);
                    battleview.findViewById(R.id.battle_node)
                            .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                }

                JsonObject deckportdata = api_data.getAsJsonObject("api_deck_port");

                JsonArray api_escape = new JsonArray();
                JsonArray api_escape_combined = new JsonArray();
                if (api_data.has("api_escape")) {
                    api_escape = api_data.getAsJsonArray("api_escape");
                    api_escape_combined = api_data.getAsJsonArray("api_escape_combined");
                }

                if (deckportdata != null) {
                    JsonArray deckData = deckportdata.getAsJsonArray("api_deck_data");
                    JsonArray portData = deckportdata.getAsJsonArray("api_ship_data");
                    friendShipData = new JsonArray();
                    friendCombinedShipData = new JsonArray();

                    for (int i = 1; i < 13; i++) {
                        battleview.findViewById(shipViewList[i]).setVisibility(View.INVISIBLE);
                        battleview.findViewById(shipCombinedViewList[i]).setVisibility(View.INVISIBLE);
                    }
                    for (int i = 0; i < deckData.size(); i++) {
                        if (i == 0) {
                            JsonObject mainDeckData = deckData.get(i).getAsJsonObject();
                            ((TextView) battleview.findViewById(R.id.friend_fleet_name)).
                                    setText(mainDeckData.get("api_name").getAsString());
                            JsonArray mainDeck = mainDeckData.getAsJsonArray("api_ship");

                            JsonObject shipData = new JsonObject();
                            //Log.e("KCA", String.valueOf(portData.size()));
                            for (int j = 0; j < portData.size(); j++) {
                                JsonObject data = portData.get(j).getAsJsonObject();
                                shipData.add(String.valueOf(data.get("api_id").getAsInt()), data);
                            }

                            for (int j = 0; j < mainDeck.size(); j++) {
                                if (mainDeck.get(j).getAsInt() == -1) {
                                    //Log.e("KCA", String.format("%d: invisible", j + 1));
                                    battleview.findViewById(shipViewList[j + 1]).setVisibility(View.INVISIBLE);
                                } else {
                                    //Log.e("KCA", String.format("%d: visible", j + 1));
                                    JsonObject data = shipData.getAsJsonObject(String.valueOf(mainDeck.get(j)));
                                    JsonObject kcdata = getKcShipDataById(data.get("api_ship_id").getAsInt(), "maxeq");
                                    JsonObject itemdata = new JsonObject();
                                    itemdata.add("api_slot", data.get("api_slot"));
                                    itemdata.add("api_slot_ex", data.get("api_slot_ex"));
                                    itemdata.add("api_onslot", data.get("api_onslot"));
                                    itemdata.add("api_maxslot", kcdata.get("maxeq"));
                                    friendShipData.add(itemdata);
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    JsonObject kcShipData = KcaApiData.getKcShipDataById(data.get("api_ship_id").getAsInt(), "name");
                                    String kcname = getShipTranslation(kcShipData.get("name").getAsString(), false);
                                    ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setText(kcname);
                                    ((TextView) battleview.findViewById(shipNameViewList[j + 1]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                                    } else {
                                        ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                                    }

                                    ((TextView) battleview.findViewById(shipLevelViewList[j + 1])).setText(makeLvString(level));
                                    if(!((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).getText().toString()
                                            .contains(getStringWithLocale(R.string.battleview_text_retreated))) {
                                        ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setText(makeHpString(nowhp, maxhp));
                                    }
                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(shipLevelViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                                        ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                                    } else {
                                        ((TextView) battleview.findViewById(shipLevelViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                                        ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                                    }

                                    float hpPercent = nowhp * VIEW_HP_MAX / (float) maxhp;
                                    ((ProgressBar) battleview.findViewById(shipHpBarViewList[j + 1])).setProgress(Math.round(hpPercent));
                                    ((ProgressBar) battleview.findViewById(shipHpBarViewList[j + 1])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));

                                    battleview.findViewById(shipViewList[j + 1]).setVisibility(View.VISIBLE);
                                }
                            }

                            for (int j = 0; j < api_escape.size(); j++) {
                                int idx = api_escape.get(j).getAsInt();
                                ((TextView) battleview.findViewById(shipHpTxtViewList[idx])).setText(getStringWithLocale(R.string.battleview_text_retreated));
                                ((TextView) battleview.findViewById(shipHpTxtViewList[idx])).setGravity(Gravity.CENTER_HORIZONTAL);
                                battleview.findViewById(shipHpTxtViewList[idx])
                                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRetreated));
                            }
                            Log.e("KCA", "FSD: " + String.valueOf(friendShipData.size()));

                        } else if (i == 1) { // TODO: CHECK NEEDED
                            JsonObject combinedDeckData = deckData.get(i).getAsJsonObject();
                            ((TextView) battleview.findViewById(R.id.friend_combined_fleet_name)).
                                    setText(combinedDeckData.get("api_name").getAsString());
                            JsonArray combinedDeck = combinedDeckData.getAsJsonArray("api_ship");
                            JsonObject shipData = new JsonObject();
                            //Log.e("KCA", String.valueOf(portData.size()));
                            for (int j = 0; j < portData.size(); j++) {
                                JsonObject data = portData.get(j).getAsJsonObject();
                                shipData.add(String.valueOf(data.get("api_id").getAsInt()), data);
                            }

                            for (int j = 0; j < combinedDeck.size(); j++) {
                                if (combinedDeck.get(j).getAsInt() == -1) {
                                    //Log.e("KCA", String.format("%d: invisible", j + 1));
                                    battleview.findViewById(shipCombinedViewList[j + 1]).setVisibility(View.INVISIBLE);
                                } else {
                                    //Log.e("KCA", String.format("%d: visible", j + 1));
                                    JsonObject data = shipData.getAsJsonObject(String.valueOf(combinedDeck.get(j)));
                                    JsonObject kcdata = getKcShipDataById(data.get("api_ship_id").getAsInt(), "maxeq");
                                    JsonObject itemdata = new JsonObject();
                                    itemdata.add("api_slot", data.get("api_slot"));
                                    itemdata.add("api_slot_ex", data.get("api_slot_ex"));
                                    itemdata.add("api_onslot", data.get("api_onslot"));
                                    itemdata.add("api_maxslot", kcdata.get("maxeq"));
                                    friendCombinedShipData.add(itemdata);
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    JsonObject kcShipData = KcaApiData.getKcShipDataById(data.get("api_ship_id").getAsInt(), "name");
                                    String kcname = getShipTranslation(kcShipData.get("name").getAsString(), false);
                                    ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1])).setText(kcname);
                                    ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                                    ((TextView) battleview.findViewById(shipNameCombinedViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                                    ((TextView) battleview.findViewById(shipLevelCombinedViewList[j + 1])).setText(makeLvString(level));
                                    if(!((TextView) battleview.findViewById(shipHpTxtCombinedViewList[j + 1])).getText().toString()
                                            .contains(getStringWithLocale(R.string.battleview_text_retreated))) {
                                        ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[j + 1])).setText(makeHpString(nowhp, maxhp));
                                    }
                                    ((TextView) battleview.findViewById(shipLevelCombinedViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                                    ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[j + 1])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);

                                    float hpPercent = nowhp * VIEW_HP_MAX / (float) maxhp;
                                    ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[j + 1])).setProgress(Math.round(hpPercent));
                                    ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[j + 1])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));

                                    battleview.findViewById(shipCombinedViewList[j + 1]).setVisibility(View.VISIBLE);
                                }
                            }

                            for (int j = 0; j < api_escape_combined.size(); j++) {
                                int idx = api_escape_combined.get(j).getAsInt();
                                ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[idx])).setText(getStringWithLocale(R.string.battleview_text_retreated));
                                ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[idx])).setGravity(Gravity.CENTER_HORIZONTAL);
                                battleview.findViewById(shipHpTxtCombinedViewList[idx])
                                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRetreated));
                            }

                            Log.e("KCA", "FCSD: " + String.valueOf(friendCombinedShipData.size()));
                        }
                    }
                }
            }

            if (api_data.has("api_ship_ke")) { // Battle (Common)
                Log.e("KCA", "BATTLE");
                enemyShipData = new JsonArray();
                enemyCombinedShipData = new JsonArray();
                JsonArray api_ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray api_ship_lv = api_data.getAsJsonArray("api_ship_lv");
                Log.e("KCA", api_data.toString());
                int[] nowhps = new int[13];
                int[] afterhps = new int[13];
                int[] nowhps_combined = new int[13];
                int[] afterhps_combined = new int[13];

                Arrays.fill(nowhps, -1);
                Arrays.fill(afterhps, -1);
                Arrays.fill(nowhps_combined, -1);
                Arrays.fill(afterhps_combined, -1);

                JsonArray eSlot = api_data.getAsJsonArray("api_eSlot");
                for (int i = 0; i < eSlot.size(); i++) {
                    JsonObject itemdata = new JsonObject();
                    itemdata.add("api_slot", eSlot.get(i));
                    enemyShipData.add(itemdata);
                }
                Log.e("KCA", "ESD: " + String.valueOf(enemyShipData.size()));
                boolean start_flag = api_data.has("api_formation");
                if (start_flag) { // day/sp_night Battle Process
                    api_formation = api_data.getAsJsonArray("api_formation");
                    // air power show
                    if (api_data.has("api_kouku") && !api_data.get("api_kouku").isJsonNull()) {
                        api_kouku = api_data.getAsJsonObject("api_kouku");
                    } else {
                        api_kouku = null;
                    }
                }

                // fleet formation and engagement show
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).
                        setText(getFormationString(contextWithLocale, api_formation.get(0).getAsInt(), fc_flag));
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).
                        setText(getFormationString(contextWithLocale, api_formation.get(1).getAsInt(), fc_flag));
                ((TextView) battleview.findViewById(R.id.battle_engagement)).
                        setText(getEngagementString(contextWithLocale, api_formation.get(2).getAsInt()));

                // airforce result
                if (api_kouku != null && !api_kouku.get("api_stage1").isJsonNull()) {
                    JsonObject api_stage1 = api_kouku.getAsJsonObject("api_stage1");
                    int api_disp_seiku = api_stage1.get("api_disp_seiku").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_airpower))
                            .setText(getAirForceResultString(contextWithLocale, api_disp_seiku));
                }

                if (KcaBattle.currentEnemyDeckName.length() > 0) {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(KcaBattle.currentEnemyDeckName);
                } else {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(getStringWithLocale(R.string.enemy_fleet_name));
                }

                for (int i = 1; i < api_ship_ke.size(); i++) {
                    if (api_ship_ke.get(i).getAsInt() == -1) {
                        battleview.findViewById(shipViewList[getEnemyIdx(i)]).setVisibility(View.INVISIBLE);
                    } else {
                        int level = api_ship_lv.get(i).getAsInt();
                        JsonObject kcShipData = KcaApiData.getKcShipDataById(api_ship_ke.get(i).getAsInt(), "name,yomi");
                        String kcname = getShipTranslation(kcShipData.get("name").getAsString(), true);
                        String kcyomi = kcShipData.get("yomi").getAsString();

                        ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setText(kcname);
                        ((TextView) battleview.findViewById(shipLevelViewList[getEnemyIdx(i)])).setText(makeLvString(level));
                        if (fc_flag || ec_flag) {
                            ((TextView) battleview.findViewById(shipLevelViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                            ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                        } else {
                            ((TextView) battleview.findViewById(shipLevelViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                            ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                        }
                        if (!is_practice) {
                            if (fc_flag || ec_flag) {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                            } else {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                            }
                            if (kcname.contains(getStringWithLocale(R.string.ship_name_class))) {
                                if (kcyomi.equals(getStringWithLocale(R.string.yomi_elite))) {
                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(getStringWithLocale(R.string.yomi_elite_short));
                                    } else {
                                        ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(kcyomi);
                                    }
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                                } else if (kcyomi.equals(getStringWithLocale(R.string.yomi_flagship))) {
                                    if (fc_flag || ec_flag) {
                                        ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(getStringWithLocale(R.string.yomi_flagship_short));
                                    } else {
                                        ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(kcyomi);
                                    }
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                                } else {
                                    ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText("");
                                }
                            } else {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText("");
                            }
                        }
                        battleview.findViewById(shipViewList[getEnemyIdx(i)]).setVisibility(View.VISIBLE);
                    }
                }

                JsonArray api_maxhps = api_data.getAsJsonArray("api_maxhps");
                JsonArray api_nowhps = api_data.getAsJsonArray("api_nowhps");
                JsonArray api_afterhps = api_data.getAsJsonArray("api_afterhps");
                for (int i = 1; i < api_maxhps.size(); i++) {
                    int maxhp = api_maxhps.get(i).getAsInt();
                    int afterhp = api_afterhps.get(i).getAsInt();
                    if (maxhp == -1) continue;
                    else {
                        float hpPercent = afterhp * VIEW_HP_MAX / (float) maxhp;
                        if(!((TextView) battleview.findViewById(shipHpTxtViewList[i])).getText().toString()
                                .contains(getStringWithLocale(R.string.battleview_text_retreated))) {
                            ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setText(makeHpString(afterhp, maxhp));
                        }
                        ((ProgressBar) battleview.findViewById(shipHpBarViewList[i])).setProgress(Math.round(hpPercent));
                        ((ProgressBar) battleview.findViewById(shipHpBarViewList[i])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                        if (fc_flag || ec_flag) {
                            ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                        } else {
                            ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                        }

                    }
                    nowhps[i] = api_nowhps.get(i).getAsInt();
                    afterhps[i] = api_afterhps.get(i).getAsInt();
                }

                if (api_data.has("api_maxhps_combined")) {
                    JsonArray api_maxhps_combined = api_data.getAsJsonArray("api_maxhps_combined");
                    JsonArray api_nowhps_combined = api_data.getAsJsonArray("api_nowhps_combined");
                    JsonArray api_afterhps_combined = api_data.getAsJsonArray("api_afterhps_combined");

                    for (int i = 1; i < api_maxhps_combined.size(); i++) {
                        int maxhp = api_maxhps_combined.get(i).getAsInt();
                        int afterhp = api_afterhps_combined.get(i).getAsInt();
                        if (maxhp == -1) continue;
                        else {
                            float hpPercent = afterhp * VIEW_HP_MAX / (float) maxhp;
                            if(!((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).getText().toString()
                                    .contains(getStringWithLocale(R.string.battleview_text_retreated))) {
                                ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setText(makeHpString(afterhp, maxhp));
                            }
                            ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[i])).setProgress(Math.round(hpPercent));
                            ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[i])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                            if (fc_flag || ec_flag) {
                                ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                            } else {
                                ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                            }

                        }
                        nowhps_combined[i] = api_nowhps_combined.get(i).getAsInt();
                        afterhps_combined[i] = api_afterhps_combined.get(i).getAsInt();
                    }
                }

                // For enemy combined fleet
                if (api_data.has("api_ship_ke_combined")) {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(getStringWithLocale(R.string.enemy_main_fleet_name));
                    ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name)).
                            setText(getStringWithLocale(R.string.enemy_combined_fleet_name));

                    JsonArray eSlotCombined = api_data.getAsJsonArray("api_eSlot_combined");
                    for (int i = 0; i < eSlotCombined.size(); i++) {
                        JsonObject itemdata = new JsonObject();
                        itemdata.add("api_slot", eSlotCombined.get(i));
                        enemyCombinedShipData.add(itemdata);
                    }
                    Log.e("KCA", "ECSD: " + String.valueOf(enemyCombinedShipData.size()));
                    JsonArray api_ship_ke_combined = api_data.getAsJsonArray("api_ship_ke_combined");
                    for (int i = 1; i < api_ship_ke_combined.size(); i++) {
                        if (api_ship_ke_combined.get(i).getAsInt() == -1) {
                            battleview.findViewById(shipCombinedViewList[getEnemyIdx(i)]).setVisibility(View.INVISIBLE);
                        } else {
                            int level = api_ship_lv.get(i).getAsInt();
                            JsonObject kcShipData = KcaApiData.getKcShipDataById(api_ship_ke_combined.get(i).getAsInt(), "name,yomi");
                            String kcname = getShipTranslation(kcShipData.get("name").getAsString(), true);
                            String kcyomi = kcShipData.get("yomi").getAsString();

                            ((TextView) battleview.findViewById(shipNameCombinedViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
                            ((TextView) battleview.findViewById(shipNameCombinedViewList[getEnemyIdx(i)])).setText(kcname);

                            ((TextView) battleview.findViewById(shipLevelCombinedViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                            ((TextView) battleview.findViewById(shipLevelCombinedViewList[getEnemyIdx(i)])).setText(makeLvString(level));

                            ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                            if (kcname.contains(getStringWithLocale(R.string.ship_name_class))) {
                                if (kcyomi.equals(getStringWithLocale(R.string.yomi_elite))) {
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)])).setText(getStringWithLocale(R.string.yomi_elite_short));
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                                } else if (kcyomi.equals(getStringWithLocale(R.string.yomi_flagship))) {
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)])).setText(getStringWithLocale(R.string.yomi_flagship_short));
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)]))
                                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                                } else {
                                    ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)])).setText("");
                                }
                            } else {
                                ((TextView) battleview.findViewById(shipYomiCombinedViewList[getEnemyIdx(i)])).setText("");
                            }
                            battleview.findViewById(shipCombinedViewList[getEnemyIdx(i)]).setVisibility(View.VISIBLE);
                        }
                    }

                    JsonArray api_maxhps_combined = api_data.getAsJsonArray("api_maxhps_combined");
                    JsonArray api_nowhps_combined = api_data.getAsJsonArray("api_nowhps_combined");
                    JsonArray api_afterhps_combined = api_data.getAsJsonArray("api_afterhps_combined");
                    for (int i = 7; i < api_maxhps_combined.size(); i++) {
                        int maxhp_combined = api_maxhps_combined.get(i).getAsInt();
                        int afterhp_combined = api_afterhps_combined.get(i).getAsInt();
                        if (maxhp_combined == -1) continue;
                        else {
                            float hpPercent = afterhp_combined * VIEW_HP_MAX / (float) maxhp_combined;
                            ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setText(makeHpString(afterhp_combined, maxhp_combined));
                            ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[i])).setProgress(Math.round(hpPercent));
                            ((ProgressBar) battleview.findViewById(shipHpBarCombinedViewList[i])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                            ((TextView) battleview.findViewById(shipHpTxtCombinedViewList[i])).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                        }
                        nowhps_combined[i] = api_nowhps_combined.get(i).getAsInt();
                        afterhps_combined[i] = api_afterhps_combined.get(i).getAsInt();
                    }
                }

                // Rank Data
                if (start_flag) {
                    startNowHps = nowhps;
                    startNowHpsCombined = nowhps_combined;
                    if (api_data.has("api_ship_ke_combined")) {
                        boolean midnightflag = KcaBattle.isMainFleetInNight(afterhps_combined, startNowHpsCombined);
                        if (midnightflag) {
                            ((TextView) battleview.findViewById(R.id.enemy_fleet_name))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNightTargetFleet));
                        } else {
                            ((TextView) battleview.findViewById(R.id.enemy_combined_fleet_name))
                                    .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNightTargetFleet));
                        }
                    }
                }

                Log.e("KCA", Arrays.toString(startNowHps));
                Log.e("KCA", Arrays.toString(startNowHpsCombined));

                String api_url = api_data.get("api_url").getAsString();
                JsonObject rankData;
                if (api_url.equals(API_REQ_SORTIE_LDAIRBATTLE) || api_url.equals(API_REQ_COMBINED_LDAIRBATTLE)) {
                    rankData = KcaBattle.calculateLdaRank(afterhps, startNowHps, afterhps_combined, startNowHpsCombined);
                } else {
                    rankData = KcaBattle.calculateRank(afterhps, startNowHps, afterhps_combined, startNowHpsCombined);
                }

                if (rankData.has("fnowhpsum")) {
                    int friendNowSum = rankData.get("fnowhpsum").getAsInt();
                    int friendAfterSum = rankData.get("fafterhpsum").getAsInt();
                    int friendDamageRate = rankData.get("fdmgrate").getAsInt();
                    String dmgshow = String.format("%d/%d (%d%%)", friendAfterSum, friendNowSum, friendDamageRate);
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText(dmgshow);
                } else {
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                }

                if (rankData.has("enowhpsum")) {
                    int enemyNowSum = rankData.get("enowhpsum").getAsInt();
                    int enemyAfterSum = rankData.get("eafterhpsum").getAsInt();
                    int enemyDamageRate = rankData.get("edmgrate").getAsInt();
                    String dmgshow = String.format("%d/%d (%d%%)", enemyAfterSum, enemyNowSum, enemyDamageRate);
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText(dmgshow);
                } else {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                }

                int rank = rankData.get("rank").getAsInt();
                switch (rank) {
                    case JUDGE_SS:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_ss));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankS));
                        break;
                    case JUDGE_S:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_s));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankS));
                        break;
                    case JUDGE_A:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_a));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankA));
                        break;
                    case JUDGE_B:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_b));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankB));
                        break;
                    case JUDGE_C:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_c));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankC));
                        break;
                    case JUDGE_D:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_d));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankD));
                        break;
                    case JUDGE_E:
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setText(getStringWithLocale(R.string.rank_e));
                        ((TextView) battleview.findViewById(R.id.battle_result))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRankE));
                        break;
                    default:
                        break;
                }
            } else if (api_data.has("api_win_rank")) { // Battle Result
                int mvp_idx = api_data.get("api_mvp").getAsInt();
                if (mvp_idx != -1) {
                    ((TextView) battleview.findViewById(shipNameViewList[mvp_idx]))
                            .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                }
                if (api_data.has("api_mvp_combined") && !api_data.get("api_mvp_combined").isJsonNull()) {
                    int mvp_idx_combined = api_data.get("api_mvp_combined").getAsInt();
                    if (mvp_idx_combined != -1) {
                        ((TextView) battleview.findViewById(shipNameCombinedViewList[mvp_idx_combined]))
                                .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorMVP));
                    }
                }
                if (api_data.has("api_get_ship")) {
                    String ship_name = api_data.getAsJsonObject("api_get_ship").get("api_ship_name").getAsString();
                    ((TextView) battleview.findViewById(R.id.battle_getship))
                            .setText(getShipTranslation(ship_name, false));
                } else if (!is_practice && checkUserPortEnough()) {
                    ((TextView) battleview.findViewById(R.id.battle_getship))
                            .setText(getStringWithLocale(R.string.getship_none));
                }
            } else {
                Log.e("KCA", api_data.entrySet().toString());
            }
        } else {
            Log.e("KCA", "api_data is null");
        }
    }

    public void setViewLayout(boolean fc_flag, boolean ec_flag) {
        LinearLayout friend_main_fleet = (LinearLayout) battleview.findViewById(R.id.friend_main_fleet);
        LinearLayout friend_combined_fleet = (LinearLayout) battleview.findViewById(R.id.friend_combined_fleet);
        LinearLayout enemy_main_fleet = (LinearLayout) battleview.findViewById(R.id.enemy_main_fleet);
        LinearLayout enemy_combined_fleet = (LinearLayout) battleview.findViewById(R.id.enemy_combined_fleet);
        Log.e("KCA", String.valueOf(fc_flag) + "-" + String.valueOf(ec_flag));

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

    public void setItemViewLayout(int id) {
        if (api_data == null || id == -1) return;
        int realID = id - 1;
        JsonObject data;

        Log.e("KCA", String.valueOf(realID));
        boolean friendflag = false;
        if (realID < 20) { // Main
            if (realID < 6) {
                friendflag = true;
                data = friendShipData.get(realID).getAsJsonObject();
            } else {
                friendflag = false;
                data = enemyShipData.get(realID - 6).getAsJsonObject();
            }
        } else { // Combined
            realID = realID % 20;
            if (realID < 6) {
                friendflag = true;
                data = friendCombinedShipData.get(realID).getAsJsonObject();
            } else {
                friendflag = false;
                data = enemyCombinedShipData.get(realID - 6).getAsJsonObject();
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
                itemView.findViewById(getId(String.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            } else {
                slot_count += 1;
                JsonObject kcItemData;
                int lv = 0;
                int alv = -1;
                if (onslot != null) {
                    Log.e("KCA", "item_id: " + String.valueOf(item_id));
                    kcItemData = getUserItemStatusById(item_id, "level,alv", "type,name");
                    Log.e("KCA", kcItemData.toString());
                    lv = kcItemData.get("level").getAsInt();
                    if (kcItemData.has("alv")) {
                        alv = kcItemData.get("alv").getAsInt();
                    }

                    if (lv > 0) {
                        ((TextView) itemView.findViewById(getId(String.format("item%d_level", i + 1), R.id.class)))
                                .setText(getStringWithLocale(R.string.lv_star).concat(String.valueOf(lv)));
                        itemView.findViewById(getId(String.format("item%d_level", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(String.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                    }

                    if (alv > 0) {
                        ((TextView) itemView.findViewById(getId(String.format("item%d_alv", i + 1), R.id.class)))
                                .setText(getStringWithLocale(getId(String.format("alv_%d", alv), R.string.class)));
                        int alvColorId = (alv <= 3) ? 1 : 2;
                        itemView.findViewById(getId(String.format("item%d_alv", i + 1), R.id.class))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), getId(String.format("itemalv%d", alvColorId), R.color.class)));
                        itemView.findViewById(getId(String.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(String.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                    }

                    int itemtype = kcItemData.getAsJsonArray("type").get(2).getAsInt();
                    if (isItemAircraft(itemtype)) {
                        onslot_count += 1;
                        Log.e("KCA", "ID: " + String.valueOf(itemtype));
                        int nowSlotValue = onslot.get(i).getAsInt();
                        int maxSlotValue = maxslot.get(i).getAsInt();
                        ((TextView) itemView.findViewById(getId(String.format("item%d_slot", i + 1), R.id.class)))
                                .setText(String.format("[%02d/%02d]", nowSlotValue, maxSlotValue));
                        itemView.findViewById(getId(String.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.VISIBLE);
                    } else {
                        itemView.findViewById(getId(String.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.INVISIBLE);
                    }
                } else {
                    kcItemData = getKcItemStatusById(item_id, "type,name");
                    itemView.findViewById(getId(String.format("item%d_level", i + 1), R.id.class)).setVisibility(View.GONE);
                    itemView.findViewById(getId(String.format("item%d_alv", i + 1), R.id.class)).setVisibility(View.GONE);
                    itemView.findViewById(getId(String.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
                }

                String kcItemName = getItemTranslation(kcItemData.get("name").getAsString());
                int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();

                int typeres = 0;
                try {
                    typeres = getId(String.format("item_%d", type), R.mipmap.class);
                } catch (Exception e) {
                    typeres = R.mipmap.item_0;
                }
                ((TextView) itemView.findViewById(getId(String.format("item%d_name", i + 1), R.id.class))).setText(kcItemName);
                ((ImageView) itemView.findViewById(getId(String.format("item%d_icon", i + 1), R.id.class))).setImageResource(typeres);
                itemView.findViewById(slotViewList[i]).setVisibility(View.VISIBLE);
            }
        }

        if (onslot_count == 0) {
            for (int i = 0; i < slot.size(); i++) {
                itemView.findViewById(getId(String.format("item%d_slot", i + 1), R.id.class)).setVisibility(View.GONE);
            }
        }

        if (slot_ex > 0) {
            // EX_SLOT
            slot_count += 1;
            JsonObject kcItemData = getUserItemStatusById(slot_ex, "level", "type,name");
            String kcItemName = getItemTranslation(kcItemData.get("name").getAsString());
            int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();
            int lv = kcItemData.get("level").getAsInt();
            int typeres = 0;
            try {
                typeres = getId(String.format("item_%d", type), R.mipmap.class);
            } catch (Exception e) {
                typeres = R.mipmap.item_0;
            }
            ((TextView) itemView.findViewById(R.id.item_ex_name)).setText(kcItemName);
            ((ImageView) itemView.findViewById(R.id.item_ex_icon)).setImageResource(typeres);
            if (lv > 0) {
                ((TextView) itemView.findViewById(R.id.item_ex_level))
                        .setText(getStringWithLocale(R.string.lv_star).concat(String.valueOf(lv)));
                itemView.findViewById(R.id.item_ex_level).setVisibility(View.VISIBLE);
            } else {
                itemView.findViewById(R.id.item_ex_level).setVisibility(View.GONE);
            }
            itemView.findViewById(R.id.view_slot_ex).setVisibility(View.VISIBLE);
        } else {
            itemView.findViewById(R.id.view_slot_ex).setVisibility(View.GONE);
        }

        if (slot_count == 0) {
            ((TextView) itemView.findViewById(R.id.item1_name)).setText(getStringWithLocale(R.string.slot_empty));
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
            api_data = KcaViewButtonService.getCurrentApiData();
            setBattleview();
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
        try {
            active = true;
            contextWithLocale = getContextWithLocale(getApplicationContext(), getBaseContext());
            broadcaster = LocalBroadcastManager.getInstance(this);
            //mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInflater = LayoutInflater.from(contextWithLocale);
            mView = mInflater.inflate(R.layout.view_sortie_battle, null);
            battleview = (ScrollView) mView.findViewById(R.id.battleview);
            itemView = mInflater.inflate(R.layout.view_battleview_items, null);
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

            hdmgreceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.e("KCA", "=> Received Intent");
                    //mViewBackup = mView;
                    //mManager.removeView(mView);
                    String value = intent.getExtras().getString(KCA_MSG_DATA, "0");
                    if (value.contains("1")) {
                        ((LinearLayout) mView.findViewById(R.id.battleviewpanel))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHeavyDmgStatePanel));
                    } else {
                        ((LinearLayout) mView.findViewById(R.id.battleviewpanel))
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    }

                    //mManager.addView(mView, mParams);
                    if (KcaViewButtonService.getClickCount() == 0) {
                        mView.setVisibility(View.GONE);
                    }
                    mView.invalidate();
                    mManager.updateViewLayout(mView, mParams);
                }
            };
            refreshreceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.e("KCA", "=> Received Intent");
                    //mViewBackup = mView;
                    //mManager.removeView(mView);
                    int setViewResult = setView();
                    if (setViewResult == 0) {
                        if (KcaViewButtonService.getClickCount() == 0) {
                            mView.setVisibility(View.GONE);
                        }
                        //mManager.addView(mView, mParams);
                        mView.invalidate();
                        mManager.updateViewLayout(mView, mParams);
                    }
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver((hdmgreceiver), new IntentFilter(KCA_MSG_BATTLE_VIEW_HDMG));
            LocalBroadcastManager.getInstance(this).registerReceiver((refreshreceiver), new IntentFilter(KCA_MSG_BATTLE_VIEW_REFRESH));
        } catch (Exception e) {
            active = false;
            error_flag = true;
            sendReport(e, ERORR_INIT);
            stopSelf();
        }
    }


    @Override
    public void onDestroy() {
        active = false;
        mView.setVisibility(View.GONE);
        itemView.setVisibility(View.GONE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hdmgreceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!error_flag) {
            mView.setVisibility(View.VISIBLE);
            battleview.setOnTouchListener(mViewTouchListener);
            for (int i = 1; i < shipViewList.length; i++) {
                battleview.findViewById(shipNameAreaViewList[i]).setOnTouchListener(shipViewTouchListener);
                battleview.findViewById(shipLevelViewList[i]).setOnTouchListener(shipViewTouchListener);
            }
            for (int i = 1; i < shipCombinedViewList.length; i++) {
                battleview.findViewById(shipNameAreaCombinedViewList[i]).setOnTouchListener(shipViewTouchListener);
                battleview.findViewById(shipLevelCombinedViewList[i]).setOnTouchListener(shipViewTouchListener);
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
                        itemView.setVisibility(View.GONE);
                    }
                    break;
            }
            return false;
        }
    };

    private View.OnTouchListener shipViewTouchListener = new View.OnTouchListener() {
        WindowManager.LayoutParams itemViewParams;
        int xMargin = 200;
        boolean isTouchDown = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isTouchDown) {
                        isTouchDown = true;
                        try {
                            int selected = getshipidx(v.getId());
                            api_data.addProperty("api_touched_idx", selected);
                            setItemViewLayout(selected);
                            itemViewParams = new WindowManager.LayoutParams(
                                    WindowManager.LayoutParams.WRAP_CONTENT,
                                    WindowManager.LayoutParams.WRAP_CONTENT,
                                    WindowManager.LayoutParams.TYPE_PHONE,
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                    PixelFormat.TRANSLUCENT);
                            itemViewParams.x = (int) (event.getRawX() + xMargin);
                            itemViewParams.y = (int) event.getRawY();
                            itemViewParams.gravity = Gravity.TOP | Gravity.LEFT;
                            if (itemView.getParent() != null) {
                                mManager.updateViewLayout(itemView, itemViewParams);
                            } else {
                                mManager.addView(itemView, itemViewParams);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            sendReport(e, KcaBattleViewService.ERORR_ITEMVIEW);
                        }
                    }
                    Log.e("KCA", "ACTION_DOWN");
                    return true;
                case MotionEvent.ACTION_UP:
                    itemView.setVisibility(View.GONE);
                    isTouchDown = false;
                    Log.e("KCA", "ACTION_UP");
                    return false;
                default:
                    return false;
            }
        }
    };

    private int getshipidx(int rid) {
        for (int i = 1; i < shipNameAreaViewList.length; i++) {
            if (rid == shipNameAreaViewList[i] || rid == shipLevelViewList[i]) return i;
        }
        for (int i = 1; i < shipNameAreaCombinedViewList.length; i++) {
            if (rid == shipNameAreaCombinedViewList[i] || rid == shipLevelCombinedViewList[i])
                return 20 + i;
        }
        return -1;
    }

    private void sendReport(Exception e, int type) {
        error_flag = true;
        mView.setVisibility(View.GONE);
        String app_version = BuildConfig.VERSION_NAME;
        String token = "df1629d6820907e7a09ea1e98d3041c2";
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
        if(type == ERORR_ITEMVIEW) {
            api_data.add("api_fs_data", friendShipData);
            api_data.add("api_fsc_data", friendCombinedShipData);
            api_data.add("api_es_data", enemyShipData);
            api_data.add("api_esc_data", enemyCombinedShipData);
        }
        sendData.addProperty("data", api_data.toString());
        sendData.addProperty("error", getStringFromException(e));
        String sendDataString = sendData.toString();

        AQuery aq = new AQuery(KcaBattleViewService.this);
        cb.header("Referer", "app:/KCA/");
        cb.header("Content-Type", "application/x-www-form-urlencoded");
        HttpEntity entity = new ByteArrayEntity(sendDataString.getBytes());
        cb.param(AQuery.POST_ENTITY, entity);
        aq.ajax(dataSendUrl, String.class, cb);
    }

}