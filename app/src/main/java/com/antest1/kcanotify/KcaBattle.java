package com.antest1.kcanotify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class KcaBattle {
    public static final String API_REQ_MAP_START = "/api_req_map/start";
    public static final String API_REQ_MAP_NEXT = "/api_req_map/next";
    public static final String API_REQ_SORTIE_BATTLE = "/api_req_sortie/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT = "/api_req_battle_midnight/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT_SP = "/api_req_battle_midnight/sp_midnight";
    public static final String API_REQ_SORTIE_AIRBATTLE = "/api_req_sortie/airbattle";
    public static final String API_REQ_SORTIE_LDAIRBATTLE = "/api_req_sortie/ld_airbattle";
    public static final String API_REQ_SORTIE_BATTLE_RESULT = "/api_req_sortie/battleresult";

    public static final String API_REQ_COMBINED_BATTLE = "/api_req_combined_battle/battle"; // 기동
    public static final String API_REQ_COMBINED_BATTLE_WATER = "/api_req_combined_battle/battle_water"; // 수상
    public static final String API_REQ_COMBINED_AIRBATTLE = "/api_req_combined_battle/airbattle"; // 아웃레인지
    public static final String API_REQ_COMBINED_LDAIRBATTLE = "/api_req_combined_battle/ld_airbattle"; // 공습
    public static final String API_REQ_COMBINED_BATTLE_EC = "/api_req_combined_battle/ec_battle"; // 단일-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH = "/api_req_combined_battle/each_battle"; // 기동-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH_WATER = "/api_req_combined_battle/each_battle_water"; // 수상-연합

    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT = "/api_req_combined_battle/midnight_battle";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_SP = "/api_req_combined_battle/sp_midnight";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_EC = "/api_req_combined_battle/ec_midnight_battle"; // 단대연 야전


    public static final String API_REQ_COMBINED_BATTLERESULT = "/api_req_combined_battle/battleresult";
    public static final String API_REQ_COMBINED_GOBACKPORT = "/api_req_combined_battle/goback_port"; // 퇴피

    public static final String KCA_API_NOTI_HEAVY_DMG = "/kca_api/noti_heavy_dmg";
    public static final String KCA_API_NOTI_GOBAKCPORT = "/kca_api/noti_gobackport";

    public static final int COMBINED_A = 1;
    public static final int COMBINED_W = 2;

    public static int[] maxhps = new int[13];
    public static int[] nowhps = new int[13];
    public static int[] afterhps = new int[13];

    public static int[] maxcbhps = new int[13];
    public static int[] nowcbhps = new int[13];
    public static int[] aftercbhps = new int[13];

    public static List<Integer> escapelist = new ArrayList<Integer>();
    public static List<Integer> escapecblist = new ArrayList<Integer>();


    public static Handler sHandler;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public static boolean checkHeavyDamagedExist() {
        for (int i = 1; i < afterhps.length; i++) {
            if (maxhps[i] == -1 || i > 6) {
                return false;
            } else if (afterhps[i] * 4 <= maxhps[i]) {
                return true;
            }
        }
        return false;
    }

    public static void cleanData() {
        for (int i = 0; i < maxhps.length; i++) {
            maxhps[i] = 0;
        }
        for (int i = 0; i < nowhps.length; i++) {
            nowhps[i] = 0;
        }
        for (int i = 0; i < afterhps.length; i++) {
            afterhps[i] = 0;
        }
    }

    public static void cleanCbData() {
        for (int i = 0; i < maxcbhps.length; i++) {
            maxcbhps[i] = 0;
        }
        for (int i = 0; i < nowcbhps.length; i++) {
            nowcbhps[i] = 0;
        }
        for (int i = 0; i < aftercbhps.length; i++) {
            aftercbhps[i] = 0;
        }
    }

    public static boolean checkCombinedHeavyDamagedExist() {
        for (int i = 1; i < afterhps.length; i++) {
            if (maxhps[i] == -1 || i > 6) {
                break;
            } else if (afterhps[i] * 4 <= maxhps[i] && !escapelist.contains(i)) {
                return true;
            }
        }
        for (int i = 1; i < aftercbhps.length; i++) {
            if (maxcbhps[i] == -1 || i > 6) {
                break;
            } else if (aftercbhps[i] * 4 <= maxcbhps[i] && !escapecblist.contains(i)) {
                return true;
            }
        }
        return false;
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

    public static int cnv(Object o) {
        if (o instanceof Long) {
            return ((Long) o).intValue();
        } else if (o instanceof Float) {
            return ((Float) o).intValue();
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else {
            //Log.e("KCA", "KcaBattle/cnv: Unrecognized " + o.toString());
            Float f = (Float.parseFloat(o.toString()));
            return f.intValue();
        }
    }

    public static void processData(String url, JSONObject api_data) {
        // Log.e("KCA", "processData: "+url );
        if (url.equals(API_REQ_MAP_START)) {
            escapelist.clear();
            escapecblist.clear();
            // TODO: Toast Next Point
        }

        if (url.equals(API_REQ_MAP_NEXT)) {
            // TODO: Toast Next Point
        }

        if (url.equals(API_REQ_SORTIE_BATTLE)) {
            cleanData();
            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            // 분식항공전 Stage 3
            JSONObject inj_kouku = (JSONObject) api_data.get("api_injection_kouku");
            if (inj_kouku != null) { // Check Null for old data
                JSONObject inj_kouku_stage3 = (JSONObject) inj_kouku.get("api_stage3");
                if (inj_kouku_stage3 != null) {
                    JSONArray inj_kouku_fdam = (JSONArray) inj_kouku_stage3.get("api_fdam");
                    JSONArray inj_kouku_edam = (JSONArray) inj_kouku_stage3.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                    }
                }
            }
            // 항공전 Stage 3
            JSONObject kouku = (JSONObject) api_data.get("api_kouku");
            JSONObject kouku_stage3 = (JSONObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JSONArray kouku_fdam = (JSONArray) kouku_stage3.get("api_fdam");
                JSONArray kouku_edam = (JSONArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }

            // Log.e("KCA", "hpInfo (kouku): " + Arrays.toString(afterhps));

            // 지원함대
            JSONObject support_info = (JSONObject) api_data.get("api_support_info");
            if (support_info != null) {
                JSONObject support_airattack = (JSONObject) support_info.get("api_support_airatack");
                JSONObject support_hourai = (JSONObject) support_info.get("api_support_hourai");

                JSONArray damage = new JSONArray();
                if (support_airattack != null) {
                    // 항공지원
                    damage = (JSONArray) ((JSONObject) support_airattack.get("api_stage3")).get("api_edam");
                } else if (support_hourai != null) {
                    damage = (JSONArray) support_hourai.get("api_damage");
                }
                for (int d = 1; d < damage.size(); d++) {
                    int e_idx = getEnemyIdx(d);
                    afterhps[e_idx] -= cnv(damage.get(d));
                }
            }

            // 선제대잠
            JSONObject opening_taisen = (JSONObject) api_data.get("api_opening_taisen");
            if (opening_taisen != null) {
                JSONArray df_list = (JSONArray) opening_taisen.get("api_df_list");
                JSONArray df_damage = (JSONArray) opening_taisen.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JSONArray target = (JSONArray) df_list.get(i);
                    JSONArray target_dmg = (JSONArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        for (int d = 0; d < target_dmg.size(); d++) {
                            afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(d));
                        }
                    }
                }
            }

            // 개막뇌격
            JSONObject openingattack = (JSONObject) api_data.get("api_opening_atack");
            if (openingattack != null) {
                JSONArray openingattack_fdam = (JSONArray) openingattack.get("api_fdam");
                JSONArray openingattack_edam = (JSONArray) openingattack.get("api_edam");
                for (int i = 1; i < openingattack_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(openingattack_fdam.get(i));
                    afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                }
            }

            //Log.e("KCA", "hpInfo (start): " + Arrays.toString(afterhps));

            // 포격전
            for (int n = 1; n <= 3; n++) {
                JSONObject hougeki = (JSONObject) api_data.get(String.format("api_hougeki%d", n));
                if (hougeki != null) {
                    JSONArray df_list = (JSONArray) hougeki.get("api_df_list");
                    JSONArray df_damage = (JSONArray) hougeki.get("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JSONArray target = (JSONArray) df_list.get(i);
                        JSONArray target_dmg = (JSONArray) df_damage.get(i);
                        for (int t = 0; t < target.size(); t++) {
                            afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                            //Log.e("KCA", String.format("hpInfo (hougeki to %d, %d): ",cnv(target.get(t)), cnv(target_dmg.get(t))) + Arrays.toString(afterhps));
                        }
                    }
                }
                //Log.e("KCA", "hpInfo (hougeki): " + Arrays.toString(afterhps));
            }

            // 폐막뇌격
            JSONObject raigeki = (JSONObject) api_data.get("api_raigeki");
            if (raigeki != null) {
                JSONArray openingattack_fdam = (JSONArray) raigeki.get("api_fdam");
                JSONArray openingattack_edam = (JSONArray) raigeki.get("api_edam");
                for (int i = 1; i < openingattack_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(openingattack_fdam.get(i));
                    afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                }
            }

            String hpInfo = Arrays.toString(afterhps);
            Log.e("KCA", "hpInfo: " + hpInfo);
            /*
			 * JSONObject battleResultInfo = new JSONObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toJSONString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT) || url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT_SP)) {
            cleanData();
            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");
            JSONArray afterhpsData = (JSONArray) api_data.get("api_nowhps");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(afterhpsData.get(i));
            }

            JSONObject hougeki = (JSONObject) api_data.get("api_hougeki");
            if (hougeki != null) {
                JSONArray df_list = (JSONArray) hougeki.get("api_df_list");
                JSONArray df_damage = (JSONArray) hougeki.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JSONArray target = (JSONArray) df_list.get(i);
                    JSONArray target_dmg = (JSONArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                    }
                }
            }

            String hpInfo = Arrays.toString(afterhps);
            Log.e("KCA", "hpInfo: " + hpInfo);
			/*
			 * JSONObject battleResultInfo = new JSONObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toJSONString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        // 아웃레인지, 공습
        if (url.equals(API_REQ_SORTIE_AIRBATTLE) || url.equals(API_REQ_SORTIE_LDAIRBATTLE)) {
            cleanData();
            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            // 제1항공전 Stage 3
            JSONObject kouku = (JSONObject) api_data.get("api_kouku");
            JSONObject kouku_stage3 = (JSONObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JSONArray kouku_fdam = (JSONArray) kouku_stage3.get("api_fdam");
                JSONArray kouku_edam = (JSONArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }

            if (url.equals(API_REQ_SORTIE_AIRBATTLE)) {
                // 제2항공전 Stage 3
                JSONObject kouku2 = (JSONObject) api_data.get("api_kouku2");
                JSONObject kouku2_stage3 = (JSONObject) kouku2.get("api_stage3");
                if (kouku_stage3 != null) {
                    JSONArray kouku2_fdam = (JSONArray) kouku2_stage3.get("api_fdam");
                    JSONArray kouku2_edam = (JSONArray) kouku2_stage3.get("api_edam");
                    for (int i = 1; i < kouku2_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(kouku2_fdam.get(i));
                        afterhps[e_idx] -= cnv(kouku2_edam.get(i));
                    }
                }

            }
            String hpInfo = Arrays.toString(afterhps);
            Log.e("KCA", "hpInfo: " + hpInfo);
        }

        if (url.equals(API_REQ_SORTIE_BATTLE_RESULT)) {
            JSONObject battleResultInfo = new JSONObject();
            Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkHeavyDamagedExist()));
            if (checkHeavyDamagedExist()) {
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                bundle.putString("data", battleResultInfo.toJSONString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }
        }

        if (url.equals(API_REQ_COMBINED_BATTLE) || url.equals(API_REQ_COMBINED_BATTLE_WATER)) {
            cleanData();
            cleanCbData();
            int combined_type = 0;
            if (url.equals(API_REQ_COMBINED_BATTLE)) combined_type = COMBINED_A;
            else if (url.equals(API_REQ_COMBINED_BATTLE_WATER)) combined_type = COMBINED_W;
            //else if(url.equals(API_REQ_COMBINED_BATTLE	))	combined_type = COMBINED_D;

            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            JSONArray maxcbhpsData = (JSONArray) api_data.get("api_maxhps_combined");
            JSONArray nowcbhpsData = (JSONArray) api_data.get("api_nowhps_combined");

            JSONArray escapeIdx = (JSONArray) api_data.get("api_escape_idx");
            JSONArray escapeIdxCb = (JSONArray) api_data.get("api_escape_idx_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = cnv(maxcbhpsData.get(i));
                nowcbhps[i] = cnv(nowcbhpsData.get(i));
                aftercbhps[i] = cnv(nowcbhpsData.get(i));
            }

            if (escapeIdx != null) {
                for (int i = 0; i < escapeIdx.size(); i++) {
                    if (!escapelist.contains(escapeIdx.get(i))) {
                        escapelist.add(i);
                    }
                }
            }

            if (escapeIdxCb != null) {
                for (int i = 0; i < escapeIdxCb.size(); i++) {
                    if (!escapecblist.contains(escapeIdxCb.get(i))) {
                        escapecblist.add(i);
                    }
                }
            }

            // 분식항공전 Stage 3
            JSONObject inj_kouku = (JSONObject) api_data.get("api_injection_kouku");
            if (inj_kouku != null) { // Check Null for old data
                JSONObject inj_kouku_stage3 = (JSONObject) inj_kouku.get("api_stage3");
                if (inj_kouku_stage3 != null) {
                    JSONArray inj_kouku_fdam = (JSONArray) inj_kouku_stage3.get("api_fdam");
                    JSONArray inj_kouku_edam = (JSONArray) inj_kouku_stage3.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                    }
                }
                JSONObject inj_kouku_stage3_combined = (JSONObject) inj_kouku.get("api_stage3_combined");
                if (inj_kouku_stage3_combined != null) {
                    JSONArray inj_kouku_fdam_combined = (JSONArray) inj_kouku_stage3_combined.get("api_fdam");
                    JSONArray inj_kouku_edam_combined = (JSONArray) inj_kouku_stage3_combined.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam_combined.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        aftercbhps[f_idx] -= cnv(inj_kouku_fdam_combined.get(i));
                        aftercbhps[e_idx] -= cnv(inj_kouku_edam_combined.get(i));
                    }
                }
            }

            // 기지항공대 Stage 3
            JSONArray airbase_attack = (JSONArray) api_data.get("api_air_base_attack");
            if (airbase_attack != null) {
                for (int i = 0; i < airbase_attack.size(); i++) {
                    JSONObject airbase_attack_info = (JSONObject) airbase_attack.get(i);
                    JSONObject airbase_attack_stage3 = (JSONObject) airbase_attack_info.get("api_stage3");
                    JSONArray airbase_attack_edam = (JSONArray) airbase_attack_stage3.get("api_edam");
                    for (int j = 1; j < airbase_attack_edam.size(); j++) {
                        int e_idx = getEnemyIdx(j);
                        afterhps[e_idx] -= cnv(airbase_attack_edam.get(j));
                    }
                }
            }

            // 항공전 Stage 3
            JSONObject kouku = (JSONObject) api_data.get("api_kouku");
            JSONObject kouku_stage3 = (JSONObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JSONArray kouku_fdam = (JSONArray) kouku_stage3.get("api_fdam");
                JSONArray kouku_edam = (JSONArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }
            JSONObject kouku_stage3_combined = (JSONObject) kouku.get("api_stage3_combined");
            if (kouku_stage3_combined != null) {
                JSONArray kouku_fdam_combined = (JSONArray) kouku_stage3_combined.get("api_fdam");
                for (int i = 1; i < kouku_fdam_combined.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    aftercbhps[f_idx] -= cnv(kouku_fdam_combined.get(i));
                }
            }

            // 지원함대
            JSONObject support_info = (JSONObject) api_data.get("api_support_info");
            if (support_info != null) {
                JSONObject support_airattack = (JSONObject) support_info.get("api_support_airatack");
                JSONObject support_hourai = (JSONObject) support_info.get("api_support_hourai");

                JSONArray damage = new JSONArray();
                if (support_airattack != null) {
                    // 항공지원
                    damage = (JSONArray) ((JSONObject) support_airattack.get("api_stage3")).get("api_edam");
                } else if (support_hourai != null) {
                    damage = (JSONArray) support_hourai.get("api_damage");
                }
                for (int d = 1; d < damage.size(); d++) {
                    int e_idx = getEnemyIdx(d);
                    afterhps[e_idx] -= cnv(damage.get(d));
                }
            }

            // 선제대잠
            JSONObject opening_taisen = (JSONObject) api_data.get("api_opening_taisen");
            if (opening_taisen != null) {
                JSONArray df_list = (JSONArray) opening_taisen.get("api_df_list");
                JSONArray df_damage = (JSONArray) opening_taisen.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JSONArray target = (JSONArray) df_list.get(i);
                    JSONArray target_dmg = (JSONArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                    }
                }
            }

            // 개막뇌격
            JSONObject openingattack = (JSONObject) api_data.get("api_opening_atack");
            if (openingattack != null) {
                JSONArray openingattack_fdam = (JSONArray) openingattack.get("api_fdam");
                JSONArray openingattack_edam = (JSONArray) openingattack.get("api_edam");
                for (int i = 1; i < openingattack_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                    afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                }
            }

            // 포격전
            for (int n = 1; n <= 3; n++) {
                JSONObject hougeki = (JSONObject) api_data.get(String.format("api_hougeki%d", n));
                if (hougeki != null) {
                    JSONArray df_list = (JSONArray) hougeki.get("api_df_list");
                    JSONArray df_damage = (JSONArray) hougeki.get("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JSONArray target = (JSONArray) df_list.get(i);
                        JSONArray target_dmg = (JSONArray) df_damage.get(i);
                        for (int t = 0; t < target.size(); t++) {

                            if (combined_type == COMBINED_A) {
                                if (n == 1) {
                                    int target_idx = cnv(target.get(t));
                                    if (target_idx <= 6) {
                                        aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                    } else {
                                        afterhps[target_idx] -= cnv(target_dmg.get(t));
                                    }
                                } else {
                                    afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                                }
                            } else if (combined_type == COMBINED_W) {
                                if (n == 3) {
                                    int target_idx = cnv(target.get(t));
                                    if (target_idx <= 6) {
                                        aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                    } else {
                                        afterhps[target_idx] -= cnv(target_dmg.get(t));
                                    }
                                } else {
                                    afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                                }
                            }

                        }
                    }
                }
            }

            // 폐막뇌격
            JSONObject raigeki = (JSONObject) api_data.get("api_raigeki");
            if (raigeki != null) {
                JSONArray openingattack_fdam = (JSONArray) raigeki.get("api_fdam");
                JSONArray openingattack_edam = (JSONArray) raigeki.get("api_edam");
                for (int i = 1; i < openingattack_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                    afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                }
            }

            String hpInfo1 = Arrays.toString(afterhps);
            String hpInfo2 = Arrays.toString(aftercbhps);
            Log.e("KCA", "hpInfo: " + hpInfo1 + " / " + hpInfo2);

            //String hpInfo1 = Arrays.toString(afterhps);
            //String hpInfo2 = Arrays.toString(aftercbhps);
            //Log.e("KCA", "hpInfo: " + hpInfo1 + " / " + hpInfo2);
			/*
			 * JSONObject battleResultInfo = new JSONObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toJSONString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_COMBINED_GOBACKPORT)) {
            JSONObject battleResultInfo = new JSONObject();
            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_NOTI_GOBAKCPORT);
            bundle.putString("data", battleResultInfo.toJSONString());
            Message sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }

        if (url.equals(API_REQ_COMBINED_BATTLE_EC) || url.equals(API_REQ_COMBINED_BATTLE_EACH) || url.equals(API_REQ_COMBINED_BATTLE_EACH_WATER)) {
            int combined_type = 0;
            if (url.equals(API_REQ_COMBINED_BATTLE_EACH)) combined_type = COMBINED_A;
            else if (url.equals(API_REQ_COMBINED_BATTLE_EACH_WATER)) combined_type = COMBINED_W;
            //else if(url.equals(API_REQ_COMBINED_BATTLE	))	combined_type = COMBINED_D;

            cleanData();
            cleanCbData();

            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            JSONArray maxcbhpsData = (JSONArray) api_data.get("api_maxhps_combined");
            JSONArray nowcbhpsData = (JSONArray) api_data.get("api_nowhps_combined");

            JSONArray escapeIdx = (JSONArray) api_data.get("api_escape_idx");
            JSONArray escapeIdxCb = (JSONArray) api_data.get("api_escape_idx_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = cnv(maxcbhpsData.get(i));
                nowcbhps[i] = cnv(nowcbhpsData.get(i));
                aftercbhps[i] = cnv(nowcbhpsData.get(i));
            }

            if (escapeIdx != null) {
                for (int i = 0; i < escapeIdx.size(); i++) {
                    if (!escapelist.contains(escapeIdx.get(i))) {
                        escapelist.add(i);
                    }
                }
            }

            if (escapeIdxCb != null) {
                for (int i = 0; i < escapeIdxCb.size(); i++) {
                    if (!escapecblist.contains(escapeIdxCb.get(i))) {
                        escapecblist.add(i);
                    }
                }
            }

            // 분식항공전 Stage 3
            JSONObject inj_kouku = (JSONObject) api_data.get("api_injection_kouku");
            if (inj_kouku != null) { // Check Null for old data
                JSONObject inj_kouku_stage3 = (JSONObject) inj_kouku.get("api_stage3");
                if (inj_kouku_stage3 != null) {
                    JSONArray inj_kouku_fdam = (JSONArray) inj_kouku_stage3.get("api_fdam");
                    JSONArray inj_kouku_edam = (JSONArray) inj_kouku_stage3.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                    }
                }
                JSONObject inj_kouku_stage3_combined = (JSONObject) inj_kouku.get("api_stage3_combined");
                if (inj_kouku_stage3_combined != null) {
                    JSONArray inj_kouku_fdam_combined = (JSONArray) inj_kouku_stage3_combined.get("api_fdam");
                    JSONArray inj_kouku_edam_combined = (JSONArray) inj_kouku_stage3_combined.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam_combined.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        aftercbhps[f_idx] -= cnv(inj_kouku_fdam_combined.get(i));
                        aftercbhps[e_idx] -= cnv(inj_kouku_edam_combined.get(i));
                    }
                }
            }

            // 기지항공대 Stage 3
            JSONArray airbase_attack = (JSONArray) api_data.get("api_air_base_attack");
            if (airbase_attack != null) {
                for (int i = 0; i < airbase_attack.size(); i++) {
                    JSONObject airbase_attack_info = (JSONObject) airbase_attack.get(i);
                    JSONObject airbase_attack_stage3 = (JSONObject) airbase_attack_info.get("api_stage3");
                    if(airbase_attack_stage3 != null) {
                        JSONArray airbase_attack_edam = (JSONArray) airbase_attack_stage3.get("api_edam");
                        for (int j = 1; j < airbase_attack_edam.size(); j++) {
                            int e_idx = getEnemyIdx(j);
                            afterhps[e_idx] -= cnv(airbase_attack_edam.get(j));
                        }
                    }
                    JSONObject airbase_attack_stage3_combined = (JSONObject) airbase_attack_info.get("api_stage3_combined");
                    if(airbase_attack_stage3_combined != null) {
                        JSONArray airbase_attack_edam_combined = (JSONArray) airbase_attack_stage3_combined.get("api_edam");
                        for (int j = 1; j < airbase_attack_edam_combined.size(); j++) {
                            int e_idx = getEnemyIdx(j);
                            aftercbhps[e_idx] -= cnv(airbase_attack_edam_combined.get(j));
                        }
                    }
                }
            }

            // 항공전 Stage 3
            JSONObject kouku = (JSONObject) api_data.get("api_kouku");
            JSONObject kouku_stage3 = (JSONObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JSONArray kouku_fdam = (JSONArray) kouku_stage3.get("api_fdam");
                JSONArray kouku_edam = (JSONArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }
            JSONObject kouku_stage3_combined = (JSONObject) kouku.get("api_stage3_combined");
            if (kouku_stage3_combined != null) {
                JSONArray kouku_fdam_combined = (JSONArray) kouku_stage3_combined.get("api_fdam");
                JSONArray kouku_edam_combined = (JSONArray) kouku_stage3_combined.get("api_edam");
                for (int i = 1; i < kouku_fdam_combined.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    aftercbhps[f_idx] -= cnv(kouku_fdam_combined.get(i));
                    aftercbhps[e_idx] -= cnv(kouku_edam_combined.get(i));
                }
            }

            // 지원함대
            JSONObject support_info = (JSONObject) api_data.get("api_support_info");
            if (support_info != null) {
                JSONObject support_airattack = (JSONObject) support_info.get("api_support_airatack");
                JSONObject support_hourai = (JSONObject) support_info.get("api_support_hourai");

                JSONArray damage = new JSONArray();
                if (support_airattack != null) {
                    // 항공지원
                    damage = (JSONArray) ((JSONObject) support_airattack.get("api_stage3")).get("api_edam");
                } else if (support_hourai != null) {
                    damage = (JSONArray) support_hourai.get("api_damage");
                }
                for (int d = 1; d < damage.size(); d++) {
                    int e_idx;
                    if (d <= 6) {
                        e_idx = getEnemyIdx(d);
                        afterhps[e_idx] -= cnv(damage.get(d));
                    } else {
                        e_idx = getEnemyIdx(d - 6);
                        aftercbhps[e_idx] -= cnv(damage.get(d));
                    }
                }
            }

            // 선제대잠
            JSONObject opening_taisen = (JSONObject) api_data.get("api_opening_taisen");
            if (opening_taisen != null) {
                JSONArray df_list = (JSONArray) opening_taisen.get("api_df_list");
                JSONArray df_damage = (JSONArray) opening_taisen.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JSONArray target = (JSONArray) df_list.get(i);
                    JSONArray target_dmg = (JSONArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        aftercbhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                    }
                }
            }

            // 개막뇌격
            JSONObject openingattack = (JSONObject) api_data.get("api_opening_atack");
            if (openingattack != null) {
                JSONArray openingattack_fdam = (JSONArray) openingattack.get("api_fdam");
                JSONArray openingattack_edam = (JSONArray) openingattack.get("api_edam");
                for (int i = 1; i < openingattack_fdam.size(); i++) {
                    if (i <= 6) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(openingattack_fdam.get(i));
                        afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                    } else {
                        int f_idx = getFriendIdx(i - 6);
                        int e_idx = getEnemyIdx(i - 6);
                        aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                        aftercbhps[e_idx] -= cnv(openingattack_edam.get(i));
                    }

                }
            }

            // 포격전
            for (int n = 1; n <= 3; n++) {
                JSONObject hougeki = (JSONObject) api_data.get(String.format("api_hougeki%d", n));
                if (hougeki != null) {
                    JSONArray at_eflag = (JSONArray) hougeki.get("api_at_eflag");
                    JSONArray df_list = (JSONArray) hougeki.get("api_df_list");
                    JSONArray df_damage = (JSONArray) hougeki.get("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        int eflag = ((Long) at_eflag.get(i)).intValue();
                        JSONArray target = (JSONArray) df_list.get(i);
                        JSONArray target_dmg = (JSONArray) df_damage.get(i);
                        for (int t = 0; t < target.size(); t++) {

                            int all_phase = 0;
                            int first_phase = 0;
                            int second_phase = 0;
                            if (combined_type == COMBINED_A) {
                                first_phase = 1;
                                second_phase = 2;
                                all_phase = 3;
                            } else if (combined_type == COMBINED_W) {
                                first_phase = 1;
                                all_phase = 2;
                                second_phase = 3;
                            } else {
                                second_phase = 1;
                                first_phase = 2;
                                all_phase = 3;
                            }

                            int target_idx = -1;
                            if (n == first_phase) {
                                if (eflag == 0) {
                                    target_idx = getEnemyIdx(cnv(target.get(t)));
                                } else {
                                    target_idx = getFriendIdx(cnv(target.get(t)));
                                }
                                afterhps[target_idx] -= cnv(target_dmg.get(t));
                            } else if (n == second_phase) {
                                if (eflag == 0) {
                                    target_idx = getEnemyCbIdx(cnv(target.get(t)));
                                    aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                } else {
                                    if (combined_type == COMBINED_A || combined_type == COMBINED_W) {
                                        target_idx = getFriendCbIdx(cnv(target.get(t)));
                                        aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                                    } else {
                                        target_idx = getFriendIdx(cnv(target.get(t)));
                                        afterhps[target_idx] -= cnv(target_dmg.get(t));
                                    }
                                }
                            } else if (n == all_phase) {
                                target_idx = cnv(target.get(t));
                                if (eflag == 0) {
                                    if (target_idx > 6) {
                                        aftercbhps[getEnemyCbIdx(target_idx)] -= cnv(target_dmg.get(t));
                                    } else {
                                        afterhps[getEnemyIdx(target_idx)] -= cnv(target_dmg.get(t));
                                    }
                                } else {
                                    if (target_idx > 6) {
                                        aftercbhps[getFriendCbIdx(target_idx)] -= cnv(target_dmg.get(t));
                                    } else {
                                        afterhps[getFriendIdx(target_idx)] -= cnv(target_dmg.get(t));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 폐막뇌격
            JSONObject raigeki = (JSONObject) api_data.get("api_raigeki");
            if (raigeki != null) {
                JSONArray raigeki_fdam = (JSONArray) raigeki.get("api_fdam");
                JSONArray raigeki_edam = (JSONArray) raigeki.get("api_edam");

                for (int i = 1; i < raigeki_fdam.size(); i++) {
                    if (i <= 6) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(raigeki_fdam.get(i));
                        afterhps[e_idx] -= cnv(raigeki_edam.get(i));
                    } else {
                        int f_idx = getFriendIdx(i - 6);
                        int e_idx = getEnemyIdx(i - 6);
                        aftercbhps[f_idx] -= cnv(raigeki_fdam.get(i));
                        aftercbhps[e_idx] -= cnv(raigeki_edam.get(i));
                    }
                }
            }

            String hpInfo1 = Arrays.toString(afterhps);
            String hpInfo2 = Arrays.toString(aftercbhps);
            Log.e("KCA", "hpInfo: " + hpInfo1 + " / " + hpInfo2);
			/*
			 * JSONObject battleResultInfo = new JSONObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toJSONString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        // 아웃레인지, 공습
        if (url.equals(API_REQ_COMBINED_AIRBATTLE) || url.equals(API_REQ_COMBINED_LDAIRBATTLE)) {
            cleanData();
            cleanCbData();

            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            JSONArray maxcbhpsData = (JSONArray) api_data.get("api_maxhps_combined");
            JSONArray nowcbhpsData = (JSONArray) api_data.get("api_nowhps_combined");

            JSONArray escapeIdx = (JSONArray) api_data.get("api_escape_idx");
            JSONArray escapeIdxCb = (JSONArray) api_data.get("api_escape_idx_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = cnv(maxcbhpsData.get(i));
                nowcbhps[i] = cnv(nowcbhpsData.get(i));
                aftercbhps[i] = cnv(nowcbhpsData.get(i));
            }

            if (escapeIdx != null) {
                for (int i = 0; i < escapeIdx.size(); i++) {
                    if (!escapelist.contains(escapeIdx.get(i))) {
                        escapelist.add(i);
                    }
                }
            }

            if (escapeIdxCb != null) {
                for (int i = 0; i < escapeIdxCb.size(); i++) {
                    if (!escapecblist.contains(escapeIdxCb.get(i))) {
                        escapecblist.add(i);
                    }
                }
            }
            // 제1항공전 Stage 3
            JSONObject kouku = (JSONObject) api_data.get("api_kouku");
            JSONObject kouku_stage3 = (JSONObject) kouku.get("api_stage3");
            JSONObject kouku_stage3_combined = (JSONObject) kouku.get("api_stage3_combined");

            if (kouku_stage3 != null) {
                JSONArray kouku_fdam = (JSONArray) kouku_stage3.get("api_fdam");
                JSONArray kouku_edam = (JSONArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }
            if (kouku_stage3_combined != null) {
                JSONArray kouku_fdam = (JSONArray) kouku_stage3_combined.get("api_fdam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    aftercbhps[f_idx] -= cnv(kouku_fdam.get(i));
                }
            }

            if (url.equals(API_REQ_COMBINED_AIRBATTLE)) {
                // 제2항공전 Stage 3
                JSONObject kouku2 = (JSONObject) api_data.get("api_kouku2");
                JSONObject kouku2_stage3 = (JSONObject) kouku2.get("api_stage3");
                JSONObject kouku2_stage3_combined = (JSONObject) kouku2.get("api_stage3_combined");
                if (kouku_stage3 != null) {
                    JSONArray kouku2_fdam = (JSONArray) kouku2_stage3.get("api_fdam");
                    JSONArray kouku2_edam = (JSONArray) kouku2_stage3.get("api_edam");
                    for (int i = 1; i < kouku2_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(kouku2_fdam.get(i));
                        afterhps[e_idx] -= cnv(kouku2_edam.get(i));
                    }
                }

                if (kouku2_stage3_combined != null) {
                    JSONArray kouku_fdam = (JSONArray) kouku2_stage3_combined.get("api_fdam");
                    for (int i = 1; i < kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        aftercbhps[f_idx] -= cnv(kouku_fdam.get(i));
                    }
                }
            }
            String hpInfo1 = Arrays.toString(afterhps);
            String hpInfo2 = Arrays.toString(aftercbhps);
            Log.e("KCA", "hpInfo: " + hpInfo1 + hpInfo2);
        }

        if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT) || url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_SP)) {
            cleanData();
            cleanCbData();

            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            JSONArray maxcbhpsData = (JSONArray) api_data.get("api_maxhps_combined");
            JSONArray nowcbhpsData = (JSONArray) api_data.get("api_nowhps_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = cnv(maxcbhpsData.get(i));
                nowcbhps[i] = cnv(nowcbhpsData.get(i));
                aftercbhps[i] = cnv(nowcbhpsData.get(i));
            }

            JSONObject hougeki = (JSONObject) api_data.get("api_hougeki");
            if (hougeki != null) {
                JSONArray df_list = (JSONArray) hougeki.get("api_df_list");
                JSONArray df_damage = (JSONArray) hougeki.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JSONArray target = (JSONArray) df_list.get(i);
                    JSONArray target_dmg = (JSONArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        if (i > 6) {
                            afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                        } else {
                            aftercbhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                        }
                    }
                }
            }

            String hpInfo = Arrays.toString(afterhps);
            Log.e("KCA", "hpInfo: " + hpInfo);
			/*
			 * JSONObject battleResultInfo = new JSONObject();
			 * battleResultInfo.put("msg", hpInfo);
			 * 
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toJSONString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 * 
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_EC)) {
            JSONArray maxhpsData = (JSONArray) api_data.get("api_maxhps");
            JSONArray nowhpsData = (JSONArray) api_data.get("api_nowhps");

            JSONArray maxcbhpsData = (JSONArray) api_data.get("api_maxhps_combined");
            JSONArray nowcbhpsData = (JSONArray) api_data.get("api_nowhps_combined");

            JSONArray activeDeckData = (JSONArray) api_data.get("api_active_deck");
            int[] activedeck = {0, 0};
            for (int i = 0; i < activeDeckData.size(); i++) {
                activedeck[i] = cnv(activeDeckData.get(i));
            }

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = cnv(maxhpsData.get(i));
                nowhps[i] = cnv(nowhpsData.get(i));
                afterhps[i] = cnv(nowhpsData.get(i));
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = cnv(maxcbhpsData.get(i));
                nowcbhps[i] = cnv(nowcbhpsData.get(i));
                aftercbhps[i] = cnv(nowcbhpsData.get(i));
            }

            JSONObject hougeki = (JSONObject) api_data.get("api_hougeki");
            if (hougeki != null) {
                JSONArray df_list = (JSONArray) hougeki.get("api_df_list");
                JSONArray df_damage = (JSONArray) hougeki.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JSONArray target = (JSONArray) df_list.get(i);
                    JSONArray target_dmg = (JSONArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        int target_idx = cnv(target.get(t));
                        if (activedeck[1] == 1) {
                            afterhps[target_idx] -= cnv(target_dmg.get(t));
                        } else {
                            aftercbhps[target_idx] -= cnv(target_dmg.get(t));
                        }
                    }
                }
            }

            String hpInfo1 = Arrays.toString(afterhps);
            String hpInfo2 = Arrays.toString(aftercbhps);
            Log.e("KCA", "hpInfo: " + hpInfo1 + "/" + hpInfo2);
			/*
			 * JSONObject battleResultInfo = new JSONObject();
			 * battleResultInfo.put("msg", hpInfo);
			 * 
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toJSONString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 * 
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_COMBINED_BATTLERESULT)) {
            JSONObject battleResultInfo = new JSONObject();
            Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkCombinedHeavyDamagedExist()));
            if (checkCombinedHeavyDamagedExist()) {
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                bundle.putString("data", battleResultInfo.toJSONString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }
        }

    }
}
