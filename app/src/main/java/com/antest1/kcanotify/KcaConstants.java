package com.antest1.kcanotify;

import java.util.Arrays;
import java.util.List;

public final class KcaConstants {
    public final static String KC_PACKAGE_NAME = "com.dmm.dmmlabo.kancolle";

    public static final String KCA_VERSION = "/kca/version.json";
    public static final String KCANOTIFY_S2 = "/kcanotify/kca_api_start2.php";
    public static final String KCANOTIFY_S2_CACHE_FILENAME = "kca_api_start2";
    public static final String KCANOTIFY_USERITEM_CACHE_FILENAME = "kca_userslotitem";

    public static final int FAIRY_TOTAL_COUNT = 20;
    public static final int[] FAIRY_REVERSE_LIST = {0, 1, 2, 8, 9, 11, 12, 15, 19};

    public static final String API_PORT = "/api_port/port";
    public static final String API_WORLD_GET_ID = "/api_world/get_id";
    public static final String API_START2 = "/api_start2";
    public static final String API_REQ_MEMBER_GET_INCENTIVE = "/api_req_member/get_incentive";
    public static final String API_GET_MEMBER_REQUIRED_INFO = "/api_get_member/require_info";
    public static final String API_REQ_MEMBER_PRESET_DECK = "/api_get_member/preset_deck";
    public static final String API_GET_MEMBER_DECK = "/api_get_member/deck";
    public static final String API_GET_MEMBER_SHIP_DECK = "/api_get_member/ship_deck";
    public static final String API_GET_MEMBER_SLOT_ITEM = "/api_get_member/slot_item";
    public static final String API_REQ_MISSION_RETURN = "/api_req_mission/return_instruction";
    public static final String API_REQ_MISSION_RESULT = "/api_req_mission/result";


    public static final String API_REQ_HENSEI_CHANGE = "/api_req_hensei/change";
    public static final String API_REQ_HENSEI_PRESET = "/api_req_hensei/preset_select";
    public static final String API_REQ_HENSEI_COMBINED = "/api_req_hensei/combined";

    public static final String API_GET_MEMBER_SHIP3 = "/api_get_member/ship3";

    public static final String API_REQ_KAISOU_SLOTSET = "/api_req_kaisou/slotset";
    public static final String API_REQ_KAISOU_SLOTSET_EX = "/api_req_kaisou/slotset_ex";
    public static final String API_REQ_KAISOU_UNSLOTSET_ALL = "/api_req_kaisou/unsetslot_all";
    public static final String API_REQ_KAISOU_SLOT_EXCHANGE = "/api_req_kaisou/slot_exchange_index";
    public static final String API_REQ_KAISOU_SLOT_DEPRIVE = "/api_req_kaisou/slot_deprive";
    public static final String API_REQ_KAISOU_POWERUP = "/api_req_kaisou/powerup";

    public static final String API_GET_MEMBER_NDOCK = "/api_get_member/ndock";
    public static final String API_REQ_NYUKYO_SPEEDCHAGNE = "/api_req_nyukyo/speedchange";

    public static final String API_REQ_KOUSYOU_CREATETIEM = "/api_req_kousyou/createitem";
    public static final String API_REQ_KOUSYOU_DESTROYITEM = "/api_req_kousyou/destroyitem2";
    public static final String API_REQ_KOUSYOU_GETSHIP = "/api_req_kousyou/getship";
    public static final String API_REQ_KOUSYOU_DESTROYSHIP = "/api_req_kousyou/destroyship";
    public static final String API_REQ_KOUSYOU_REMOEL_SLOT = "/api_req_kousyou/remodel_slot";

    public static final String API_REQ_KOUSYOU_CREATESHIP = "/api_req_kousyou/createship";
    public static final String API_GET_MEMBER_KDOCK = "/api_get_member/kdock";

    public static final String API_GET_MEMBER_QUESTLIST = "/api_get_member/questlist";

    public static final String API_GET_MEMBER_MAPINFO = "/api_get_member/mapinfo";
    public static final String API_GET_MEMBER_MISSION = "/api_get_member/mission";
    public static final String API_REQ_MAP_START = "/api_req_map/start";
    public static final String API_REQ_MAP_NEXT = "/api_req_map/next";
    public static final String API_REQ_SORTIE_BATTLE = "/api_req_sortie/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT = "/api_req_battle_midnight/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT_SP = "/api_req_battle_midnight/sp_midnight";
    public static final String API_REQ_SORTIE_AIRBATTLE = "/api_req_sortie/airbattle";
    public static final String API_REQ_SORTIE_LDAIRBATTLE = "/api_req_sortie/ld_airbattle";
    public static final String API_REQ_SORTIE_BATTLE_RESULT = "/api_req_sortie/battleresult";

    public static final String API_REQ_MAP_SELECT_EVENTMAP_RANK = "/api_req_map/select_eventmap_rank";

    public static final String API_REQ_COMBINED_BATTLE = "/api_req_combined_battle/battle"; // 기동
    public static final String API_REQ_COMBINED_BATTLE_WATER = "/api_req_combined_battle/battle_water"; // 수상
    public static final String API_REQ_COMBINED_BATTLE_EC = "/api_req_combined_battle/ec_battle"; // 단일-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH = "/api_req_combined_battle/each_battle"; // 기동-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH_WATER = "/api_req_combined_battle/each_battle_water"; // 수상-연합

    public static final String API_REQ_COMBINED_AIRBATTLE = "/api_req_combined_battle/airbattle"; // 아웃레인지
    public static final String API_REQ_COMBINED_LDAIRBATTLE = "/api_req_combined_battle/ld_airbattle"; // 공습

    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT = "/api_req_combined_battle/midnight_battle";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_SP = "/api_req_combined_battle/sp_midnight";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_EC = "/api_req_combined_battle/ec_midnight_battle"; // 단대연 야전

    public static final String API_REQ_COMBINED_BATTLERESULT = "/api_req_combined_battle/battleresult";
    public static final String API_REQ_COMBINED_GOBACKPORT = "/api_req_combined_battle/goback_port"; // 퇴피

    public static final String API_REQ_MEMBER_GET_PRACTICE_ENEMYINFO = "/api_req_member/get_practice_enemyinfo";
    public static final String API_REQ_PRACTICE_BATTLE = "/api_req_practice/battle";
    public static final String API_REQ_PRACTICE_MIDNIGHT_BATTLE = "/api_req_practice/midnight_battle";
    public static final String API_REQ_PRACTICE_BATTLE_RESULT = "/api_req_practice/battle_result";

    public static final String[] API_BATTLE_REQ_LIST = new String[]{
            API_REQ_MAP_START,
            API_REQ_MAP_NEXT,
            API_REQ_SORTIE_BATTLE,
            API_REQ_SORTIE_BATTLE_MIDNIGHT,
            API_REQ_SORTIE_BATTLE_MIDNIGHT_SP,
            API_REQ_SORTIE_AIRBATTLE,
            API_REQ_SORTIE_LDAIRBATTLE,
            API_REQ_SORTIE_BATTLE_RESULT,
            API_REQ_COMBINED_BATTLE,
            API_REQ_COMBINED_BATTLE_WATER,
            API_REQ_COMBINED_AIRBATTLE,
            API_REQ_COMBINED_LDAIRBATTLE,
            API_REQ_COMBINED_BATTLE_MIDNIGHT,
            API_REQ_COMBINED_BATTLE_MIDNIGHT_SP,
            API_REQ_COMBINED_BATTLE_EC,
            API_REQ_COMBINED_BATTLE_EACH,
            API_REQ_COMBINED_BATTLE_EACH_WATER,
            API_REQ_COMBINED_BATTLE_MIDNIGHT_EC,
            API_REQ_COMBINED_BATTLERESULT,
            API_REQ_COMBINED_GOBACKPORT,

            API_REQ_PRACTICE_BATTLE,
            API_REQ_PRACTICE_MIDNIGHT_BATTLE,
            API_REQ_PRACTICE_BATTLE_RESULT
    };

    public static final int API_NODE_EVENT_ID_OBTAIN = 2;
    public static final int API_NODE_EVENT_ID_LOSS = 3;
    public static final int API_NODE_EVENT_ID_NORMAL = 4;
    public static final int API_NODE_EVENT_ID_BOSS = 5;
    public static final int API_NODE_EVENT_ID_NOEVENT = 6;
    public static final int API_NODE_EVENT_ID_AIR = 7;
    public static final int API_NODE_EVENT_ID_SENDAN = 8;
    public static final int API_NODE_EVENT_ID_TPOINT = 9;
    public static final int API_NODE_EVENT_ID_LDAIRBATTLE = 10;

    public static final int API_NODE_EVENT_KIND_NOBATTLE = 0;
    public static final int API_NODE_EVENT_KIND_BATTLE = 1;
    public static final int API_NODE_EVENT_KIND_NIGHTBATTLE = 2;
    public static final int API_NODE_EVENT_KIND_NIGHTDAYBATTLE = 3;
    public static final int API_NODE_EVENT_KIND_AIRBATTLE = 4;
    public static final int API_NODE_EVENT_KIND_ECBATTLE = 5;
    public static final int API_NODE_EVENT_KIND_LDAIRBATTLE = 6;

    public static final int API_NODE_EVENT_KIND_SELECTABLE = 2;

    public static final int API_NODE_EVENT_KIND_AIRSEARCH = 0;

    public static final String KCA_API_DATA_LOADED = "/kca_api/data_loaded";
    public static final String KCA_API_NOTI_EXP_LEFT = "/kca_api/noti_exp_left";
    public static final String KCA_API_NOTI_EXP_FIN = "/kca_api/noti_exp_fin";
    public static final String KCA_API_NOTI_EXP_CANCELED = "/kca_api/noti_exp_canceled";
    public static final String KCA_API_NOTI_HEAVY_DMG = "/kca_api/noti_heavy_dmg";
    public static final String KCA_API_NOTI_BATTLE_NODE = "/kca_api/noti_battle_node";
    public static final String KCA_API_NOTI_BATTLE_INFO = "/kca_api/noti_battle_info";
    public static final String KCA_API_NOTI_GOBACKPORT = "/kca_api/noti_gobackport";
    public static final String KCA_API_NOTI_BATTLE_DROPINFO = "/kca_api/noti_battle_dropinfo";
    public static final String KCA_API_NOTI_DOCK_FIN = "/kca_api/noti_dock_fin";
    public static final String KCA_API_PREF_CN_CHANGED = "/kca_api/pref_cn_changed";
    public static final String KCA_API_PREF_EXPVIEW_CHANGED = "/kca_api/pref_expview_changed";
    public static final String KCA_API_PREF_FAIRY_CHANGED = "/kca_api/pref_fairy_changed";
    public static final String KCA_API_PREF_LANGUAGE_CHANGED = "/kca_api/pref_language_changed";

    public static final String KCA_MSG_FAIRY_CHANGED = "com.antest1.kcanotify.KcaFairySelectActivity.KCA_MSG_FAIRY_CHANGED";
    public static final String KCA_MSG_BATTLE_NODE = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_NODE";
    public static final String KCA_MSG_BATTLE_INFO = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_INFO";
    public static final String KCA_MSG_BATTLE_HDMG = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_HDMG";
    public static final String KCA_MSG_QUEST_LIST = "com.antest1.kcanotify.KcaService.KCA_MSG_QUEST_LIST";
    public static final String KCA_MSG_BATTLE_VIEW_REFRESH = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_VIEW_REFRESH";
    public static final String KCA_MSG_BATTLE_VIEW_HDMG = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_VIEW_HDMG";
    public static final String KCA_MSG_QUEST_VIEW_LIST = "com.antest1.kcanotify.KcaService.KCA_MSG_QUEST_VIEW_LIST";
    public static final String KCA_MSG_DATA = "com.antest1.kcanotify.KcaService.KCA_MSG_DATA";

    public static final String KCA_MSG_BUTTON_TOP = "com.antest1.kcanotify.KcaBattleViewService.KCA_MSG_BUTTON_TOP";
    public static final String KCA_MSG_VIEW_HIDE = "com.antest1.kcanotify.KcaViewButtonService.KCA_MSG_VIEW_HIDE";

    public static final String KCA_API_PROCESS_BATTLE_FAILED = "/kca_api/process_battle_failed";
    public static final String KCA_API_OPENDB_FAILED = "/kca_api/opendb_failed";

    public static final String KCA_API_VPN_DATA_ERROR = "/kca_api/vpn_data_error";

    public static final int SEEK_PURE = 0;
    public static final int SEEK_33CN1 = 1;
    public static final int SEEK_33CN3 = 3;
    public static final int SEEK_33CN4 = 4;


    public static final int NOTI_FRONT = 0;
    public static final int NOTI_EXP = 1;
    public static final int NOTI_DOCK = 2;

    public static final int FRONT_NONE = 0;
    public static final int FRONT_EXP_SET = 2;

    public static final int COMBINED_A = 1;
    public static final int COMBINED_W = 2;

    public static final int HD_NONE = 0;
    public static final int HD_DAMECON = 1;
    public static final int HD_DANGER = 2;

    public static final List<String> API_BATTLE_REQS = Arrays.asList(API_BATTLE_REQ_LIST);

    public static final int VIEW_HP_MAX = 100;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_LIGHTDMG = 1;
    public static final int STATE_MODERATEDMG = 2;
    public static final int STATE_HEAVYDMG = 3;

    public static final int FORMATION_LAH = 1;
    public static final int FORMATION_DLN = 2;
    public static final int FORMATION_DIA = 3;
    public static final int FORMATION_ECH = 4;
    public static final int FORMATION_LAB = 5;
    public static final int FORMATION_C1 = 11;
    public static final int FORMATION_C2 = 12;
    public static final int FORMATION_C3 = 13;
    public static final int FORMATION_C4 = 14;

    public static final int ENGAGE_PARL = 1;
    public static final int ENGAGE_HDON = 2;
    public static final int ENGAGE_TADV = 3;
    public static final int ENGAGE_TDIS = 4;

    public static final int AIR_PARITY = 0;
    public static final int AIR_SUPERMACY = 1;
    public static final int AIR_SUPERIORITY = 2;
    public static final int AIR_DENIAL = 3;
    public static final int AIR_INCAPABILITY = 4;

    public static final int RAID_LOST_TYPE_1 = 1;
    public static final int RAID_LOST_TYPE_2 = 2;
    public static final int RAID_LOST_TYPE_3 = 3;
    public static final int RAID_LOST_TYPE_4 = 4;

    public static final int JUDGE_E = 0;
    public static final int JUDGE_D = 1;
    public static final int JUDGE_C = 2;
    public static final int JUDGE_B = 3;
    public static final int JUDGE_A = 4;
    public static final int JUDGE_S = 5;
    public static final int JUDGE_SS = 6;

    public static final int ITEM_FUEL = 1;
    public static final int ITEM_AMMO = 2;
    public static final int ITEM_STEL = 3;
    public static final int ITEM_BAUX = 4;
    public static final int ITEM_BRNR = 5;
    public static final int ITEM_BGTZ = 6;
    public static final int ITEM_MMAT = 7;
    public static final int ITEM_KMAT = 8;
    public static final int ITEM_BOXS = 10;
    public static final int ITEM_BOXM = 11;
    public static final int ITEM_BOXL = 12;

    public static final String PREF_VPN_ENABLED = "enabled";
    public static final String PREF_SVC_ENABLED = "svcenabled";

    public static final String PREF_KCA_VERSION = "kca_version";
    public static final String PREF_KCA_LANGUAGE = "kca_language";
    public static final String PREF_KCA_SEEK_CN = "kca_seek_cn";
    public static final String PREF_OPENDB_API_USE = "opendb_api_use";
    public static final String PREF_KCA_EXP_VIEW = "expedition_view";
    public static final String PREF_KCA_BATTLEVIEW_USE = "battleview_use";
    public static final String PREF_KCA_QUESTVIEW_USE = "questview_use";
    public static final String PREF_OVERLAY_SETTING = "overlay_setting";
    public static final String PREF_SHOWDROP_SETTING = "showdrop_setting";
    public static final String PREF_KCA_DOWNLOAD_DATA = "download_data";
    public static final String PREF_CHECK_UPDATE = "check_update";
    public static final String PREF_KCA_NOTI_SOUND_KIND = "notify_sound_kind";
    public static final String PREF_KCA_NOTI_RINGTONE = "notify_ringtone";
    public static final String PREF_KCA_NOTI_EXP = "notify_expedition";
    public static final String PREF_KCA_NOTI_DOCK = "notify_docking";
    public static final String PREF_KCA_NOTI_V_HD = "notify_vibrate_heavydamaged";
    public static final String PREF_KCA_NOTI_V_NS = "notify_vibrate_notsupplied";
    public static final String PREF_AKASHI_STAR_CHECKED = "akashi_star_checked";
    public static final String PREF_AKASHI_STARLIST = "akashi_starlist";
    public static final String PREF_AKASHI_FILTERLIST = "akashi_filterlist";
    public static final String PREF_FAIRY_ICON = "fairy_icon";
    public static final String PREF_APK_DOWNLOAD_SITE = "apk_download_site";
    public static final String PREF_VPN_BYPASS_ADDRESS = "bypass_address";

    public static final String[] PREF_ARRAY = {
            PREF_CHECK_UPDATE,
            PREF_KCA_VERSION,
            PREF_KCA_LANGUAGE,
            PREF_KCA_SEEK_CN,
            PREF_OPENDB_API_USE,
            PREF_KCA_EXP_VIEW,
            PREF_KCA_BATTLEVIEW_USE,
            PREF_KCA_QUESTVIEW_USE,
            PREF_OVERLAY_SETTING,
            PREF_SHOWDROP_SETTING,
            PREF_KCA_DOWNLOAD_DATA,
            PREF_KCA_NOTI_SOUND_KIND,
            PREF_KCA_NOTI_RINGTONE,
            PREF_KCA_NOTI_EXP,
            PREF_KCA_NOTI_DOCK,
            PREF_KCA_NOTI_V_HD,
            PREF_KCA_NOTI_V_NS,
            PREF_AKASHI_STAR_CHECKED,
            PREF_AKASHI_STARLIST,
            PREF_AKASHI_FILTERLIST,
            PREF_FAIRY_ICON,
            PREF_APK_DOWNLOAD_SITE,
            PREF_VPN_BYPASS_ADDRESS
    };

    public static final List<String> PREFS_LIST = Arrays.asList(PREF_ARRAY);

}
