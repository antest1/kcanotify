package com.antest1.kcanotify;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Calendar;

import static com.antest1.kcanotify.KcaApiData.getAirForceResultString;
import static com.antest1.kcanotify.KcaApiData.getCurrentNodeAlphabet;
import static com.antest1.kcanotify.KcaApiData.getEngagementString;
import static com.antest1.kcanotify.KcaApiData.getFormationString;
import static com.antest1.kcanotify.KcaApiData.getNodeFullInfo;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaConstants.*;

public class KcaBattleViewService extends Service {
    LayoutInflater mInflater;
    private BroadcastReceiver refreshreceiver, hdmgreceiver;
    public static boolean active;
    public static JsonObject api_data;
    public static String currentNodeInfo = "";
    public static int[] startNowHps;

    JsonArray api_formation;
    JsonObject api_kouku;

    private View mView;
    private WindowManager mManager;
    private View mViewBackup;

    private int[] shipViewList = {0,
            R.id.fm_1, R.id.fm_2, R.id.fm_3, R.id.fm_4, R.id.fm_5, R.id.fm_6,
            R.id.em_1, R.id.em_2, R.id.em_3, R.id.em_4, R.id.em_5, R.id.em_6
    };

    private int[] shipNameViewList = {0,
            R.id.fm_1_name, R.id.fm_2_name, R.id.fm_3_name, R.id.fm_4_name, R.id.fm_5_name, R.id.fm_6_name,
            R.id.em_1_name, R.id.em_2_name, R.id.em_3_name, R.id.em_4_name, R.id.em_5_name, R.id.em_6_name
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
        return String.format("HP: %d/%d", currenthp, maxhp);
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

    public Drawable getProgressDrawable(Context context, int value) {
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
                currentNodeInfo = getNodeFullInfo(getApplicationContext(), currentNode, api_event_id, api_event_type, true);
                currentNodeInfo = currentNodeInfo.replaceAll("[()]", "");
                ((TextView) battleview.findViewById(R.id.battle_node)).setText(currentNodeInfo);
                ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");

                switch (api_color_no) {
                    case 2:
                    case 6:
                    case 9:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
                        break;
                    case 3:
                        battleview.findViewById(R.id.battle_node)
                                .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorVortex));
                        break;
                    case 4:
                        if (api_event_id == API_NODE_EVENT_ID_NOEVENT) {
                            battleview.findViewById(R.id.battle_node)
                                    .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorNone));
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

            if (is_practice) {
                ((TextView) battleview.findViewById(R.id.battle_node)).setText(getString(R.string.node_info_practice));
                ((TextView) battleview.findViewById(R.id.battle_result)).setText("");
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_engagement)).setText("");
                ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).setText("");
                ((TextView) battleview.findViewById(R.id.battle_airpower)).setText("");
                battleview.findViewById(R.id.battle_node)
                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorItem));
            }

            if (api_data.has("api_deck_port")) { // common sortie, practice
                JsonObject deckportdata = api_data.getAsJsonObject("api_deck_port");
                if (deckportdata != null) {
                    JsonArray deckData = deckportdata.getAsJsonArray("api_deck_data");
                    JsonArray portData = deckportdata.getAsJsonArray("api_ship_data");
                    for (int i = 7; i < 13; i++) {
                        battleview.findViewById(shipViewList[i]).setVisibility(View.INVISIBLE);
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
                                    int maxhp = data.get("api_maxhp").getAsInt();
                                    int nowhp = data.get("api_nowhp").getAsInt();
                                    int level = data.get("api_lv").getAsInt();
                                    JsonObject kcShipData = KcaApiData.getKcShipDataById(data.get("api_ship_id").getAsInt(), "name");
                                    String kcname = getShipTranslation(kcShipData.get("name").getAsString());
                                    ((TextView) battleview.findViewById(shipNameViewList[j + 1])).setText(kcname);
                                    ((TextView) battleview.findViewById(shipLevelViewList[j + 1])).setText(makeLvString(level));
                                    ((TextView) battleview.findViewById(shipHpTxtViewList[j + 1])).setText(makeHpString(nowhp, maxhp));

                                    int hpPercent = nowhp * VIEW_HP_MAX / maxhp;
                                    ((ProgressBar) battleview.findViewById(shipHpBarViewList[j + 1])).setProgress(hpPercent);
                                    ((ProgressBar) battleview.findViewById(shipHpBarViewList[j + 1])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));

                                    battleview.findViewById(shipViewList[j + 1]).setVisibility(View.VISIBLE);
                                }
                            }
                        } else if (i == 1) {
                            // TODO: Combined Fleet
                        }
                    }
                }
            }

            if (api_data.has("api_ship_ke")) { // Battle (Common)
                Log.e("KCA", "BATTLE");
                JsonArray api_ship_ke = api_data.getAsJsonArray("api_ship_ke");
                JsonArray api_ship_lv = api_data.getAsJsonArray("api_ship_lv");

                boolean start_flag = api_data.has("api_formation");
                if (start_flag) { // day/sp_night Battle Process
                    api_formation = api_data.getAsJsonArray("api_formation");
                    // air power show
                    if (api_data.has("api_kouku")) {
                        api_kouku = api_data.getAsJsonObject("api_kouku");
                    } else {
                        api_kouku = null;
                    }
                }

                // fleet formation and engagement show
                ((TextView) battleview.findViewById(R.id.friend_fleet_formation)).
                        setText(getFormationString(getApplicationContext(), api_formation.get(0).getAsInt()));
                ((TextView) battleview.findViewById(R.id.enemy_fleet_formation)).
                        setText(getFormationString(getApplicationContext(), api_formation.get(1).getAsInt()));
                ((TextView) battleview.findViewById(R.id.battle_engagement)).
                        setText(getEngagementString(getApplicationContext(), api_formation.get(2).getAsInt()));

                if (api_kouku != null && !api_kouku.get("api_stage1").isJsonNull()) {
                    JsonObject api_stage1 = api_kouku.getAsJsonObject("api_stage1");
                    int api_disp_seiku = api_stage1.get("api_disp_seiku").getAsInt();
                    ((TextView) battleview.findViewById(R.id.battle_airpower))
                            .setText(getAirForceResultString(getApplicationContext(), api_disp_seiku));
                }

                if (KcaBattle.currentEnemyDeckName.length() > 0) {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(KcaBattle.currentEnemyDeckName);
                } else {
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_name)).
                            setText(getString(R.string.enemy_fleet_name));
                }

                for (int i = 1; i < api_ship_ke.size(); i++) {
                    if (api_ship_ke.get(i).getAsInt() == -1) {
                        battleview.findViewById(shipViewList[getEnemyIdx(i)]).setVisibility(View.INVISIBLE);
                    } else {
                        int level = api_ship_lv.get(i).getAsInt();
                        JsonObject kcShipData = KcaApiData.getKcShipDataById(api_ship_ke.get(i).getAsInt(), "name,yomi");
                        String kcname = getShipTranslation(kcShipData.get("name").getAsString());
                        String kcyomi = getShipTranslation(kcShipData.get("yomi").getAsString());

                        ((TextView) battleview.findViewById(shipNameViewList[getEnemyIdx(i)])).setText(kcname);
                        if(!is_practice) {
                            if (kcyomi.equals(getString(R.string.yomi_elite))) {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(kcyomi);
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)]))
                                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorElite));
                            } else if (kcyomi.equals(getString(R.string.yomi_flagship))) {
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)])).setText(kcyomi);
                                ((TextView) battleview.findViewById(shipYomiViewList[getEnemyIdx(i)]))
                                        .setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFlagship));
                            }
                        }
                        ((TextView) battleview.findViewById(shipLevelViewList[getEnemyIdx(i)])).setText(makeLvString(level));
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
                        int hpPercent = afterhp * VIEW_HP_MAX / maxhp;
                        ((TextView) battleview.findViewById(shipHpTxtViewList[i])).setText(makeHpString(afterhp, maxhp));
                        ((ProgressBar) battleview.findViewById(shipHpBarViewList[i])).setProgress(hpPercent);
                        ((ProgressBar) battleview.findViewById(shipHpBarViewList[i])).setProgressDrawable(getProgressDrawable(getApplicationContext(), hpPercent));
                    }
                }
                int[] nowhps = new int[13];
                int[] afterhps = new int[13];

                for (int i = 0; i < api_nowhps.size(); i++) {
                    nowhps[i] = api_nowhps.get(i).getAsInt();
                    afterhps[i] = api_afterhps.get(i).getAsInt();
                }

                if (start_flag) startNowHps = nowhps;
                Log.e("KCA", Arrays.toString(startNowHps));
                JsonObject rankData = KcaBattle.calculateRankSS(afterhps, startNowHps);

                if(rankData.has("fnowhpsum")) {
                    int friendNowSum = rankData.get("fnowhpsum").getAsInt();
                    int friendAfterSum = rankData.get("fafterhpsum").getAsInt();
                    int friendDamageRate = rankData.get("fdmgrate").getAsInt();
                    String dmgshow = String.format("%d/%d (%d%%)", friendAfterSum, friendNowSum, friendDamageRate);
                    ((TextView) battleview.findViewById(R.id.friend_fleet_damage)).setText(dmgshow);
                }

                if(rankData.has("enowhpsum")) {
                    int enemyNowSum = rankData.get("enowhpsum").getAsInt();
                    int enemyAfterSum = rankData.get("eafterhpsum").getAsInt();
                    int enemyDamageRate = rankData.get("edmgrate").getAsInt();
                    String dmgshow = String.format("%d/%d (%d%%)", enemyAfterSum, enemyNowSum, enemyDamageRate);
                    ((TextView) battleview.findViewById(R.id.enemy_fleet_damage)).setText(dmgshow);
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
                //battleresult.setText(api_data.getAsJsonArray("api_ship_ke").toString());

            } else {
                Log.e("KCA", api_data.entrySet().toString());
            }
        } else {
            Log.e("KCA", "api_data is null");
        }
    }

    public void setView() {
        //if(mViewBackup != null) mView = mViewBackup;
        battleview = (ScrollView) mView.findViewById(R.id.battleview);
        battleview.setOnTouchListener(mViewTouchListener);
        api_data = KcaViewButtonService.getCurrentApiData();
        setBattleview();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        active = true;
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.view_sortie_battle, null);
        setView();
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.CENTER;

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);

        hdmgreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e("KCA", "=> Received Intent");
                //mViewBackup = mView;
                //mManager.removeView(mView);
                ((LinearLayout) mView.findViewById(R.id.battleviewpanel))
                        .setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorHeavyDmgState));
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
                setView();
                if (KcaViewButtonService.getClickCount() == 0) {
                    mView.setVisibility(View.GONE);
                }
                //mManager.addView(mView, mParams);
                mView.invalidate();
                mManager.updateViewLayout(mView, mParams);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver((hdmgreceiver), new IntentFilter(KCA_MSG_BATTLE_VIEW_HDMG));
        LocalBroadcastManager.getInstance(this).registerReceiver((refreshreceiver), new IntentFilter(KCA_MSG_BATTLE_VIEW_REFRESH));
    }

    @Override
    public void onDestroy() {
        active = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hdmgreceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshreceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mView.setVisibility(View.VISIBLE);
        return super.onStartCommand(intent, flags, startId);
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private long prevClickTime = -1;
        private long startClickTime = -1;
        private long clickDuration;
        ;
        private int clickcount = 2;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    if (prevClickTime == -1) prevClickTime = startClickTime;
                    break;

                case MotionEvent.ACTION_UP:
                    clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION && (startClickTime - prevClickTime) < MAX_CLICK_DURATION) {
                        clickcount -= 1;
                        if (clickcount <= 0) {
                            clickcount = 2;
                            mView.setVisibility(View.GONE);
                            //stopSelf();
                        }
                    } else {
                        clickcount = 1;
                    }
                    prevClickTime = startClickTime;
                    break;
            }
            return true;
        }
    };

}