package com.antest1.kcanotify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

    public static int cnv(JsonElement value) {
        Float f = value.getAsFloat();
        return f.intValue();
    }

    public static void processData(String url, JsonObject api_data) {
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
            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            // 분식항공전 Stage 3
            JsonObject inj_kouku = (JsonObject) api_data.get("api_injection_kouku");
            if (inj_kouku != null) { // Check Null for old data
                JsonObject inj_kouku_stage3 = (JsonObject) inj_kouku.get("api_stage3");
                if (inj_kouku_stage3 != null) {
                    JsonArray inj_kouku_fdam = (JsonArray) inj_kouku_stage3.get("api_fdam");
                    JsonArray inj_kouku_edam = (JsonArray) inj_kouku_stage3.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                    }
                }
            }
            // 항공전 Stage 3
            JsonObject kouku = (JsonObject) api_data.get("api_kouku");
            JsonObject kouku_stage3 = (JsonObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JsonArray kouku_fdam = (JsonArray) kouku_stage3.get("api_fdam");
                JsonArray kouku_edam = (JsonArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }

            // Log.e("KCA", "hpInfo (kouku): " + Arrays.toString(afterhps));

            // 지원함대
            JsonObject support_info = (JsonObject) api_data.get("api_support_info");
            if (support_info != null) {
                JsonObject support_airattack = (JsonObject) support_info.get("api_support_airatack");
                JsonObject support_hourai = (JsonObject) support_info.get("api_support_hourai");

                JsonArray damage = new JsonArray();
                if (support_airattack != null) {
                    // 항공지원
                    damage = (JsonArray) ((JsonObject) support_airattack.get("api_stage3")).get("api_edam");
                } else if (support_hourai != null) {
                    damage = (JsonArray) support_hourai.get("api_damage");
                }
                for (int d = 1; d < damage.size(); d++) {
                    int e_idx = getEnemyIdx(d);
                    afterhps[e_idx] -= cnv(damage.get(d));
                }
            }

            // 선제대잠
            JsonObject opening_taisen = (JsonObject) api_data.get("api_opening_taisen");
            if (opening_taisen != null) {
                JsonArray df_list = (JsonArray) opening_taisen.get("api_df_list");
                JsonArray df_damage = (JsonArray) opening_taisen.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JsonArray target = (JsonArray) df_list.get(i);
                    JsonArray target_dmg = (JsonArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        for (int d = 0; d < target_dmg.size(); d++) {
                            afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(d));
                        }
                    }
                }
            }

            // 개막뇌격
            JsonObject openingattack = (JsonObject) api_data.get("api_opening_atack");
            if (openingattack != null) {
                JsonArray openingattack_fdam = (JsonArray) openingattack.get("api_fdam");
                JsonArray openingattack_edam = (JsonArray) openingattack.get("api_edam");
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
                JsonObject hougeki = (JsonObject) api_data.get(String.format("api_hougeki%d", n));
                if (hougeki != null) {
                    JsonArray df_list = (JsonArray) hougeki.get("api_df_list");
                    JsonArray df_damage = (JsonArray) hougeki.get("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = (JsonArray) df_list.get(i);
                        JsonArray target_dmg = (JsonArray) df_damage.get(i);
                        for (int t = 0; t < target.size(); t++) {
                            afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                            //Log.e("KCA", String.format("hpInfo (hougeki to %d, %d): ",cnv(target.get(t)), cnv(target_dmg.get(t))) + Arrays.toString(afterhps));
                        }
                    }
                }
                //Log.e("KCA", "hpInfo (hougeki): " + Arrays.toString(afterhps));
            }

            // 폐막뇌격
            JsonObject raigeki = (JsonObject) api_data.get("api_raigeki");
            if (raigeki != null) {
                JsonArray openingattack_fdam = (JsonArray) raigeki.get("api_fdam");
                JsonArray openingattack_edam = (JsonArray) raigeki.get("api_edam");
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
			 * JsonObject battleResultInfo = new JsonObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT) || url.equals(API_REQ_SORTIE_BATTLE_MIDNIGHT_SP)) {
            cleanData();
            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();
            JsonArray afterhpsData = api_data.get("api_nowhps").getAsJsonArray();

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = cnv(afterhpsData.get(i));
            }

            JsonObject hougeki = (JsonObject) api_data.get("api_hougeki");
            if (hougeki != null) {
                JsonArray df_list = (JsonArray) hougeki.get("api_df_list");
                JsonArray df_damage = (JsonArray) hougeki.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JsonArray target = (JsonArray) df_list.get(i);
                    JsonArray target_dmg = (JsonArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                    }
                }
            }

            String hpInfo = Arrays.toString(afterhps);
            Log.e("KCA", "hpInfo: " + hpInfo);
			/*
			 * JsonObject battleResultInfo = new JsonObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        // 아웃레인지, 공습
        if (url.equals(API_REQ_SORTIE_AIRBATTLE) || url.equals(API_REQ_SORTIE_LDAIRBATTLE)) {
            cleanData();
            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            // 제1항공전 Stage 3
            JsonObject kouku = (JsonObject) api_data.get("api_kouku");
            JsonObject kouku_stage3 = (JsonObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JsonArray kouku_fdam = (JsonArray) kouku_stage3.get("api_fdam");
                JsonArray kouku_edam = (JsonArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }

            if (url.equals(API_REQ_SORTIE_AIRBATTLE)) {
                // 제2항공전 Stage 3
                JsonObject kouku2 = (JsonObject) api_data.get("api_kouku2");
                JsonObject kouku2_stage3 = (JsonObject) kouku2.get("api_stage3");
                if (kouku_stage3 != null) {
                    JsonArray kouku2_fdam = (JsonArray) kouku2_stage3.get("api_fdam");
                    JsonArray kouku2_edam = (JsonArray) kouku2_stage3.get("api_edam");
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
            JsonObject battleResultInfo = new JsonObject();
            Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkHeavyDamagedExist()));
            if (checkHeavyDamagedExist()) {
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                bundle.putString("data", battleResultInfo.toString());
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

            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            JsonArray maxcbhpsData = (JsonArray) api_data.get("api_maxhps_combined");
            JsonArray nowcbhpsData = (JsonArray) api_data.get("api_nowhps_combined");

            JsonArray escapeIdx = (JsonArray) api_data.get("api_escape_idx");
            JsonArray escapeIdxCb = (JsonArray) api_data.get("api_escape_idx_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
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
            JsonObject inj_kouku = (JsonObject) api_data.get("api_injection_kouku");
            if (inj_kouku != null) { // Check Null for old data
                JsonObject inj_kouku_stage3 = (JsonObject) inj_kouku.get("api_stage3");
                if (inj_kouku_stage3 != null) {
                    JsonArray inj_kouku_fdam = (JsonArray) inj_kouku_stage3.get("api_fdam");
                    JsonArray inj_kouku_edam = (JsonArray) inj_kouku_stage3.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                    }
                }
                JsonObject inj_kouku_stage3_combined = (JsonObject) inj_kouku.get("api_stage3_combined");
                if (inj_kouku_stage3_combined != null) {
                    JsonArray inj_kouku_fdam_combined = (JsonArray) inj_kouku_stage3_combined.get("api_fdam");
                    JsonArray inj_kouku_edam_combined = (JsonArray) inj_kouku_stage3_combined.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam_combined.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        aftercbhps[f_idx] -= cnv(inj_kouku_fdam_combined.get(i));
                        aftercbhps[e_idx] -= cnv(inj_kouku_edam_combined.get(i));
                    }
                }
            }

            // 기지항공대 Stage 3
            JsonArray airbase_attack = (JsonArray) api_data.get("api_air_base_attack");
            if (airbase_attack != null) {
                for (int i = 0; i < airbase_attack.size(); i++) {
                    JsonObject airbase_attack_info = (JsonObject) airbase_attack.get(i);
                    JsonObject airbase_attack_stage3 = (JsonObject) airbase_attack_info.get("api_stage3");
                    JsonArray airbase_attack_edam = (JsonArray) airbase_attack_stage3.get("api_edam");
                    for (int j = 1; j < airbase_attack_edam.size(); j++) {
                        int e_idx = getEnemyIdx(j);
                        afterhps[e_idx] -= cnv(airbase_attack_edam.get(j));
                    }
                }
            }

            // 항공전 Stage 3
            JsonObject kouku = (JsonObject) api_data.get("api_kouku");
            JsonObject kouku_stage3 = (JsonObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JsonArray kouku_fdam = (JsonArray) kouku_stage3.get("api_fdam");
                JsonArray kouku_edam = (JsonArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }
            JsonObject kouku_stage3_combined = (JsonObject) kouku.get("api_stage3_combined");
            if (kouku_stage3_combined != null) {
                JsonArray kouku_fdam_combined = (JsonArray) kouku_stage3_combined.get("api_fdam");
                for (int i = 1; i < kouku_fdam_combined.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    aftercbhps[f_idx] -= cnv(kouku_fdam_combined.get(i));
                }
            }

            // 지원함대
            JsonObject support_info = (JsonObject) api_data.get("api_support_info");
            if (support_info != null) {
                JsonObject support_airattack = (JsonObject) support_info.get("api_support_airatack");
                JsonObject support_hourai = (JsonObject) support_info.get("api_support_hourai");

                JsonArray damage = new JsonArray();
                if (support_airattack != null) {
                    // 항공지원
                    damage = (JsonArray) ((JsonObject) support_airattack.get("api_stage3")).get("api_edam");
                } else if (support_hourai != null) {
                    damage = (JsonArray) support_hourai.get("api_damage");
                }
                for (int d = 1; d < damage.size(); d++) {
                    int e_idx = getEnemyIdx(d);
                    afterhps[e_idx] -= cnv(damage.get(d));
                }
            }

            // 선제대잠
            JsonObject opening_taisen = (JsonObject) api_data.get("api_opening_taisen");
            if (opening_taisen != null) {
                JsonArray df_list = (JsonArray) opening_taisen.get("api_df_list");
                JsonArray df_damage = (JsonArray) opening_taisen.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JsonArray target = (JsonArray) df_list.get(i);
                    JsonArray target_dmg = (JsonArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        afterhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                    }
                }
            }

            // 개막뇌격
            JsonObject openingattack = (JsonObject) api_data.get("api_opening_atack");
            if (openingattack != null) {
                JsonArray openingattack_fdam = (JsonArray) openingattack.get("api_fdam");
                JsonArray openingattack_edam = (JsonArray) openingattack.get("api_edam");
                for (int i = 1; i < openingattack_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    aftercbhps[f_idx] -= cnv(openingattack_fdam.get(i));
                    afterhps[e_idx] -= cnv(openingattack_edam.get(i));
                }
            }

            // 포격전
            for (int n = 1; n <= 3; n++) {
                JsonObject hougeki = (JsonObject) api_data.get(String.format("api_hougeki%d", n));
                if (hougeki != null) {
                    JsonArray df_list = (JsonArray) hougeki.get("api_df_list");
                    JsonArray df_damage = (JsonArray) hougeki.get("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        JsonArray target = (JsonArray) df_list.get(i);
                        JsonArray target_dmg = (JsonArray) df_damage.get(i);
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
            JsonObject raigeki = (JsonObject) api_data.get("api_raigeki");
            if (raigeki != null) {
                JsonArray openingattack_fdam = (JsonArray) raigeki.get("api_fdam");
                JsonArray openingattack_edam = (JsonArray) raigeki.get("api_edam");
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
			 * JsonObject battleResultInfo = new JsonObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_COMBINED_GOBACKPORT)) {
            JsonObject battleResultInfo = new JsonObject();
            Bundle bundle = new Bundle();
            bundle.putString("url", KCA_API_NOTI_GOBAKCPORT);
            bundle.putString("data", battleResultInfo.toString());
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

            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            JsonArray maxcbhpsData = (JsonArray) api_data.get("api_maxhps_combined");
            JsonArray nowcbhpsData = (JsonArray) api_data.get("api_nowhps_combined");

            JsonArray escapeIdx = (JsonArray) api_data.get("api_escape_idx");
            JsonArray escapeIdxCb = (JsonArray) api_data.get("api_escape_idx_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
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
            JsonObject inj_kouku = (JsonObject) api_data.get("api_injection_kouku");
            if (inj_kouku != null) { // Check Null for old data
                JsonObject inj_kouku_stage3 = (JsonObject) inj_kouku.get("api_stage3");
                if (inj_kouku_stage3 != null) {
                    JsonArray inj_kouku_fdam = (JsonArray) inj_kouku_stage3.get("api_fdam");
                    JsonArray inj_kouku_edam = (JsonArray) inj_kouku_stage3.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(inj_kouku_fdam.get(i));
                        afterhps[e_idx] -= cnv(inj_kouku_edam.get(i));
                    }
                }
                JsonObject inj_kouku_stage3_combined = (JsonObject) inj_kouku.get("api_stage3_combined");
                if (inj_kouku_stage3_combined != null) {
                    JsonArray inj_kouku_fdam_combined = (JsonArray) inj_kouku_stage3_combined.get("api_fdam");
                    JsonArray inj_kouku_edam_combined = (JsonArray) inj_kouku_stage3_combined.get("api_edam");
                    for (int i = 1; i < inj_kouku_fdam_combined.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        aftercbhps[f_idx] -= cnv(inj_kouku_fdam_combined.get(i));
                        aftercbhps[e_idx] -= cnv(inj_kouku_edam_combined.get(i));
                    }
                }
            }

            // 기지항공대 Stage 3
            JsonArray airbase_attack = (JsonArray) api_data.get("api_air_base_attack");
            if (airbase_attack != null) {
                for (int i = 0; i < airbase_attack.size(); i++) {
                    JsonObject airbase_attack_info = (JsonObject) airbase_attack.get(i);
                    JsonObject airbase_attack_stage3 = (JsonObject) airbase_attack_info.get("api_stage3");
                    if(airbase_attack_stage3 != null) {
                        JsonArray airbase_attack_edam = (JsonArray) airbase_attack_stage3.get("api_edam");
                        for (int j = 1; j < airbase_attack_edam.size(); j++) {
                            int e_idx = getEnemyIdx(j);
                            afterhps[e_idx] -= cnv(airbase_attack_edam.get(j));
                        }
                    }
                    JsonObject airbase_attack_stage3_combined = (JsonObject) airbase_attack_info.get("api_stage3_combined");
                    if(airbase_attack_stage3_combined != null) {
                        JsonArray airbase_attack_edam_combined = (JsonArray) airbase_attack_stage3_combined.get("api_edam");
                        for (int j = 1; j < airbase_attack_edam_combined.size(); j++) {
                            int e_idx = getEnemyIdx(j);
                            aftercbhps[e_idx] -= cnv(airbase_attack_edam_combined.get(j));
                        }
                    }
                }
            }

            // 항공전 Stage 3
            JsonObject kouku = (JsonObject) api_data.get("api_kouku");
            JsonObject kouku_stage3 = (JsonObject) kouku.get("api_stage3");
            if (kouku_stage3 != null) {
                JsonArray kouku_fdam = (JsonArray) kouku_stage3.get("api_fdam");
                JsonArray kouku_edam = (JsonArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }
            JsonObject kouku_stage3_combined = (JsonObject) kouku.get("api_stage3_combined");
            if (kouku_stage3_combined != null) {
                JsonArray kouku_fdam_combined = (JsonArray) kouku_stage3_combined.get("api_fdam");
                JsonArray kouku_edam_combined = (JsonArray) kouku_stage3_combined.get("api_edam");
                for (int i = 1; i < kouku_fdam_combined.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    aftercbhps[f_idx] -= cnv(kouku_fdam_combined.get(i));
                    aftercbhps[e_idx] -= cnv(kouku_edam_combined.get(i));
                }
            }

            // 지원함대
            JsonObject support_info = (JsonObject) api_data.get("api_support_info");
            if (support_info != null) {
                JsonObject support_airattack = (JsonObject) support_info.get("api_support_airatack");
                JsonObject support_hourai = (JsonObject) support_info.get("api_support_hourai");

                JsonArray damage = new JsonArray();
                if (support_airattack != null) {
                    // 항공지원
                    damage = (JsonArray) ((JsonObject) support_airattack.get("api_stage3")).get("api_edam");
                } else if (support_hourai != null) {
                    damage = (JsonArray) support_hourai.get("api_damage");
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
            JsonObject opening_taisen = (JsonObject) api_data.get("api_opening_taisen");
            if (opening_taisen != null) {
                JsonArray df_list = (JsonArray) opening_taisen.get("api_df_list");
                JsonArray df_damage = (JsonArray) opening_taisen.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JsonArray target = (JsonArray) df_list.get(i);
                    JsonArray target_dmg = (JsonArray) df_damage.get(i);
                    for (int t = 0; t < target.size(); t++) {
                        aftercbhps[cnv(target.get(t))] -= cnv(target_dmg.get(t));
                    }
                }
            }

            // 개막뇌격
            JsonObject openingattack = (JsonObject) api_data.get("api_opening_atack");
            if (openingattack != null) {
                JsonArray openingattack_fdam = (JsonArray) openingattack.get("api_fdam");
                JsonArray openingattack_edam = (JsonArray) openingattack.get("api_edam");
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
                JsonObject hougeki = (JsonObject) api_data.get(String.format("api_hougeki%d", n));
                if (hougeki != null) {
                    JsonArray at_eflag = (JsonArray) hougeki.get("api_at_eflag");
                    JsonArray df_list = (JsonArray) hougeki.get("api_df_list");
                    JsonArray df_damage = (JsonArray) hougeki.get("api_damage");
                    for (int i = 1; i < df_list.size(); i++) {
                        int eflag = at_eflag.get(i).getAsInt();
                        JsonArray target = (JsonArray) df_list.get(i);
                        JsonArray target_dmg = (JsonArray) df_damage.get(i);
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
            JsonObject raigeki = (JsonObject) api_data.get("api_raigeki");
            if (raigeki != null) {
                JsonArray raigeki_fdam = (JsonArray) raigeki.get("api_fdam");
                JsonArray raigeki_edam = (JsonArray) raigeki.get("api_edam");

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
			 * JsonObject battleResultInfo = new JsonObject();
			 * battleResultInfo.put("msg", hpInfo);
			 *
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 *
			 * sHandler.sendMessage(sMsg);
			 */
        }

        // 아웃레인지, 공습
        if (url.equals(API_REQ_COMBINED_AIRBATTLE) || url.equals(API_REQ_COMBINED_LDAIRBATTLE)) {
            cleanData();
            cleanCbData();

            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            JsonArray maxcbhpsData = (JsonArray) api_data.get("api_maxhps_combined");
            JsonArray nowcbhpsData = (JsonArray) api_data.get("api_nowhps_combined");

            JsonArray escapeIdx = (JsonArray) api_data.get("api_escape_idx");
            JsonArray escapeIdxCb = (JsonArray) api_data.get("api_escape_idx_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
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
            JsonObject kouku = (JsonObject) api_data.get("api_kouku");
            JsonObject kouku_stage3 = (JsonObject) kouku.get("api_stage3");
            JsonObject kouku_stage3_combined = (JsonObject) kouku.get("api_stage3_combined");

            if (kouku_stage3 != null) {
                JsonArray kouku_fdam = (JsonArray) kouku_stage3.get("api_fdam");
                JsonArray kouku_edam = (JsonArray) kouku_stage3.get("api_edam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    int e_idx = getEnemyIdx(i);
                    afterhps[f_idx] -= cnv(kouku_fdam.get(i));
                    afterhps[e_idx] -= cnv(kouku_edam.get(i));
                }
            }
            if (kouku_stage3_combined != null) {
                JsonArray kouku_fdam = (JsonArray) kouku_stage3_combined.get("api_fdam");
                for (int i = 1; i < kouku_fdam.size(); i++) {
                    int f_idx = getFriendIdx(i);
                    aftercbhps[f_idx] -= cnv(kouku_fdam.get(i));
                }
            }

            if (url.equals(API_REQ_COMBINED_AIRBATTLE)) {
                // 제2항공전 Stage 3
                JsonObject kouku2 = (JsonObject) api_data.get("api_kouku2");
                JsonObject kouku2_stage3 = (JsonObject) kouku2.get("api_stage3");
                JsonObject kouku2_stage3_combined = (JsonObject) kouku2.get("api_stage3_combined");
                if (kouku_stage3 != null) {
                    JsonArray kouku2_fdam = (JsonArray) kouku2_stage3.get("api_fdam");
                    JsonArray kouku2_edam = (JsonArray) kouku2_stage3.get("api_edam");
                    for (int i = 1; i < kouku2_fdam.size(); i++) {
                        int f_idx = getFriendIdx(i);
                        int e_idx = getEnemyIdx(i);
                        afterhps[f_idx] -= cnv(kouku2_fdam.get(i));
                        afterhps[e_idx] -= cnv(kouku2_edam.get(i));
                    }
                }

                if (kouku2_stage3_combined != null) {
                    JsonArray kouku_fdam = (JsonArray) kouku2_stage3_combined.get("api_fdam");
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

            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            JsonArray maxcbhpsData = (JsonArray) api_data.get("api_maxhps_combined");
            JsonArray nowcbhpsData = (JsonArray) api_data.get("api_nowhps_combined");

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
            }

            JsonObject hougeki = (JsonObject) api_data.get("api_hougeki");
            if (hougeki != null) {
                JsonArray df_list = (JsonArray) hougeki.get("api_df_list");
                JsonArray df_damage = (JsonArray) hougeki.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JsonArray target = (JsonArray) df_list.get(i);
                    JsonArray target_dmg = (JsonArray) df_damage.get(i);
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
			 * JsonObject battleResultInfo = new JsonObject();
			 * battleResultInfo.put("msg", hpInfo);
			 * 
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 * 
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_COMBINED_BATTLE_MIDNIGHT_EC)) {
            JsonArray maxhpsData = api_data.get("api_maxhps").getAsJsonArray();
            JsonArray nowhpsData = api_data.get("api_nowhps").getAsJsonArray();

            JsonArray maxcbhpsData = (JsonArray) api_data.get("api_maxhps_combined");
            JsonArray nowcbhpsData = (JsonArray) api_data.get("api_nowhps_combined");

            JsonArray activeDeckData = (JsonArray) api_data.get("api_active_deck");
            int[] activedeck = {0, 0};
            for (int i = 0; i < activeDeckData.size(); i++) {
                activedeck[i] = cnv(activeDeckData.get(i));
            }

            for (int i = 0; i < maxhpsData.size(); i++) {
                maxhps[i] = maxhpsData.get(i).getAsInt();
                nowhps[i] = nowhpsData.get(i).getAsInt();
                afterhps[i] = nowhpsData.get(i).getAsInt();
            }

            for (int i = 0; i < maxcbhpsData.size(); i++) {
                maxcbhps[i] = maxcbhpsData.get(i).getAsInt();
                nowcbhps[i] = nowcbhpsData.get(i).getAsInt();
                aftercbhps[i] = nowcbhpsData.get(i).getAsInt();
            }

            JsonObject hougeki = (JsonObject) api_data.get("api_hougeki");
            if (hougeki != null) {
                JsonArray df_list = (JsonArray) hougeki.get("api_df_list");
                JsonArray df_damage = (JsonArray) hougeki.get("api_damage");
                for (int i = 1; i < df_list.size(); i++) {
                    JsonArray target = (JsonArray) df_list.get(i);
                    JsonArray target_dmg = (JsonArray) df_damage.get(i);
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
			 * JsonObject battleResultInfo = new JsonObject();
			 * battleResultInfo.put("msg", hpInfo);
			 * 
			 * Bundle bundle = new Bundle(); bundle.putString("url",
			 * KCA_API_NOTI_BATTLE_INFO); bundle.putString("data",
			 * battleResultInfo.toString()); Message sMsg =
			 * sHandler.obtainMessage(); sMsg.setData(bundle);
			 * 
			 * sHandler.sendMessage(sMsg);
			 */
        }

        if (url.equals(API_REQ_COMBINED_BATTLERESULT)) {
            JsonObject battleResultInfo = new JsonObject();
            Log.e("KCA", "CheckHeavyDamaged " + String.valueOf(checkCombinedHeavyDamagedExist()));
            if (checkCombinedHeavyDamagedExist()) {
                Bundle bundle = new Bundle();
                bundle.putString("url", KCA_API_NOTI_HEAVY_DMG);
                bundle.putString("data", battleResultInfo.toString());
                Message sMsg = sHandler.obtainMessage();
                sMsg.setData(bundle);

                sHandler.sendMessage(sMsg);
            }
        }

    }
}
