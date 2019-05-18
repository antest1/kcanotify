package com.antest1.kcanotify;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public final class KcaConstants {
    public final static String KC_PACKAGE_NAME = "com.dmm.dmmlabo.kancolle";
    public final static String DMMLOGIN_PACKAGE_NAME = "com.dmm.app.store";
    public final static String MAINACTIVITY_NAME = "com.antest1.kcanotify.MainActivity";
    public final static String GOTO_PACKAGE_NAME = "com.antest1.gotobrowser";

    public static final String AUTHORITY = "com.antest1.kcasniffer.contentprovider";
    public static final String PATH  = "/request";

    public static final Uri CONTENT_URI = Uri.parse("content://".concat(AUTHORITY).concat(PATH));
    public static final String BROADCAST_ACTION = "com.antest1.kcasniffer.broadcast";

    public static final String KCA_VERSION = "/kca/version.json";
    public static final String KCANOTIFY_S2 = "/kcanotify/kca_api_start2.php";
    public static final String KCANOTIFY_USERITEM_CACHE_FILENAME = "kca_userslotitem";
    public static final int KCANOTIFY_DB_VERSION = 5;
    public static final int KCANOTIFY_QTDB_VERSION = 3;
    public static final int KCANOTIFY_DROPLOG_VERSION = 1;
    public static final int KCANOTIFY_RESOURCELOG_VERSION = 1;
    public static final int KCANOTIFY_PACKETLOG_VERSION = 2;

    public static final String DB_KEY_STARTDATA = "key_startdata";
    public static final String DB_KEY_DECKPORT = "key_deckport";
    public static final String DB_KEY_USEITEMS = "key_useitems";
    public static final String DB_KEY_MAPEDGES = "key_mapedges";
    public static final String DB_KEY_MAPSUBDT = "key_mapsubdt";
    public static final String DB_KEY_EXPDINFO = "key_expdinfo";
    public static final String DB_KEY_QUESTTRACK = "key_questtrack";
    public static final String DB_KEY_BATTLEINFO = "key_battleinfo";
    public static final String DB_KEY_BATTLENODE = "key_battlenode";
    public static final String DB_KEY_QTRACKINFO = "key_qtrackinfo";
    public static final String DB_KEY_APIMAPINFO = "key_apimapinfo";
    public static final String DB_KEY_NDOCKDATA = "key_ndockdata";
    public static final String DB_KEY_KDOCKDATA = "key_kdockdata";
    public static final String DB_KEY_EXPTDAY = "key_exptday";
    public static final String DB_KEY_EXPCRNT = "key_expcrnt";
    public static final String DB_KEY_EXPTIME = "key_exptime";
    public static final String DB_KEY_FAIRYLOC = "key_fairyloc";
    public static final String DB_KEY_LABSIFNO = "key_labsinfo";
    public static final String DB_KEY_SHIPIFNO = "key_shipinfo";
    public static final String DB_KEY_EXPSHIP = "key_expship";
    public static final String DB_KEY_EXPSORTIE = "key_expsortie";
    public static final String DB_KEY_BASICIFNO = "key_basicinfo";
    public static final String DB_KEY_LATESTDEV = "key_latestdev";
    public static final String DB_KEY_EXPCALTRK = "key_expcaltrk";
    public static final String DB_KEY_QUESTNCHK = "key_questnchk";
    public static final String DB_KEY_MATERIALS = "key_materials";
    public static final String DB_KEY_KCMAINTNC = "key_kcmaintnc";

    public static final String[] DB_KEY_ARRAY = {
            DB_KEY_STARTDATA,
            DB_KEY_DECKPORT,
            DB_KEY_USEITEMS,
            DB_KEY_MAPEDGES,
            DB_KEY_EXPDINFO,
            DB_KEY_QUESTTRACK,
            DB_KEY_BATTLEINFO,
            DB_KEY_BATTLENODE,
            DB_KEY_QTRACKINFO,
            DB_KEY_APIMAPINFO,
            DB_KEY_NDOCKDATA,
            DB_KEY_KDOCKDATA,
            DB_KEY_EXPTDAY,
            DB_KEY_EXPCRNT,
            DB_KEY_EXPTIME,
            DB_KEY_FAIRYLOC,
            DB_KEY_LABSIFNO,
            DB_KEY_SHIPIFNO,
            DB_KEY_EXPSHIP,
            DB_KEY_EXPSORTIE,
            DB_KEY_BASICIFNO,
            DB_KEY_LATESTDEV,
            DB_KEY_EXPCALTRK,
            DB_KEY_QUESTNCHK,
            DB_KEY_MATERIALS
    };

    public static final String ERROR_TYPE_SERVICE = "S";
    public static final String ERROR_TYPE_BATTLE = "B";
    public static final String ERROR_TYPE_BATTLEVIEW = "BV";
    public static final String ERROR_TYPE_QUESTVIEW = "Q";
    public static final String ERROR_TYPE_QUESTTRACK = "QT";
    public static final String ERROR_TYPE_FLEETVIEW = "F";
    public static final String ERROR_TYPE_NOTI = "N";
    public static final String ERROR_TYPE_VPN = "V";
    public static final String ERROR_TYPE_OPENDB = "O";
    public static final String ERROR_TYPE_DB = "D";
    public static final String ERROR_TYPE_MAIN = "AM";
    public static final String ERROR_TYPE_SETTING = "AS";
    public static final String ERROR_TYPE_RESLOG = "RL";
    public static final String ERROR_TYPE_DATALOAD = "DL";

    public static final String API_PORT = "/api_port/port";
    public static final String API_WORLD_GET_WORLDINFO = "/api_world/get_worldinfo";
    public static final String API_WORLD_GET_ID = "/api_world/get_id";
    public static final String API_START2 = "/api_start2";
    public static final String API_START2_NEW = "/api_start2/getData";
    public static final String API_REQ_MEMBER_GET_INCENTIVE = "/api_req_member/get_incentive";
    public static final String API_GET_MEMBER_REQUIRED_INFO = "/api_get_member/require_info";
    public static final String API_GET_MEMBER_RECORD = "/api_get_member/record";
    public static final String API_GET_MEMBER_USEITEM = "/api_get_member/useitem";
    public static final String API_REQ_MEMBER_PRESET_DECK = "/api_get_member/preset_deck";
    public static final String API_REQ_MEMBER_ITEMUSE_COND = "/api_req_member/itemuse_cond";
    public static final String API_GET_MEMBER_DECK = "/api_get_member/deck";
    public static final String API_GET_MEMBER_SHIP_DECK = "/api_get_member/ship_deck";
    public static final String API_GET_MEMBER_SLOT_ITEM = "/api_get_member/slot_item";
    public static final String API_REQ_MISSION_RETURN = "/api_req_mission/return_instruction";
    public static final String API_REQ_MISSION_RESULT = "/api_req_mission/result";
    public static final String API_GET_MEMBER_MATERIAL = "/api_get_member/material";

    public static final String API_REQ_HENSEI_CHANGE = "/api_req_hensei/change";
    public static final String API_REQ_HENSEI_PRESET = "/api_req_hensei/preset_select";
    public static final String API_REQ_HENSEI_COMBINED = "/api_req_hensei/combined";

    public static final String API_GET_MEMBER_SHIP2 = "/api_get_member/ship2";
    public static final String API_GET_MEMBER_SHIP3 = "/api_get_member/ship3";

    public static final String API_REQ_KAISOU_SLOTSET = "/api_req_kaisou/slotset";
    public static final String API_REQ_KAISOU_SLOTSET_EX = "/api_req_kaisou/slotset_ex";
    public static final String API_REQ_KAISOU_UNSLOTSET_ALL = "/api_req_kaisou/unsetslot_all";
    public static final String API_REQ_KAISOU_SLOT_EXCHANGE = "/api_req_kaisou/slot_exchange_index";
    public static final String API_REQ_KAISOU_SLOT_DEPRIVE = "/api_req_kaisou/slot_deprive";
    public static final String API_REQ_KAISOU_POWERUP = "/api_req_kaisou/powerup";

    public static final String API_GET_MEMBER_NDOCK = "/api_get_member/ndock";
    public static final String API_REQ_NYUKYO_START = "/api_req_nyukyo/start";
    public static final String API_REQ_NYUKYO_SPEEDCHAGNE = "/api_req_nyukyo/speedchange";
    public static final String API_REQ_HOKYU_CHARGE = "/api_req_hokyu/charge";

    public static final String API_REQ_KOUSYOU_CREATEITEM = "/api_req_kousyou/createitem";
    public static final String API_REQ_KOUSYOU_DESTROYITEM = "/api_req_kousyou/destroyitem2";
    public static final String API_REQ_KOUSYOU_GETSHIP = "/api_req_kousyou/getship";
    public static final String API_REQ_KOUSYOU_DESTROYSHIP = "/api_req_kousyou/destroyship";
    public static final String API_REQ_KOUSYOU_REMOEL_SLOT = "/api_req_kousyou/remodel_slot";

    public static final String API_REQ_KOUSYOU_CREATESHIP = "/api_req_kousyou/createship";
    public static final String API_GET_MEMBER_KDOCK = "/api_get_member/kdock";
    public static final String API_REQ_KOUSYOU_CREATESHIP_SPEEDCHANGE = "/api_req_kousyou/createship_speedchange";

    public static final String[] API_WIDGET_TIMERUPDATE_LIST = new String[]{
            API_PORT,
            API_GET_MEMBER_NDOCK,
            API_REQ_NYUKYO_START,
            API_GET_MEMBER_DECK,
            API_GET_MEMBER_KDOCK,
            API_REQ_KOUSYOU_GETSHIP,
            API_REQ_KOUSYOU_CREATESHIP_SPEEDCHANGE
    };

    public static final String API_GET_MEMBER_QUESTLIST = "/api_get_member/questlist";
    public static final String API_REQ_QUEST_START = "/api_req_quest/start";
    public static final String API_REQ_QUEST_STOP = "/api_req_quest/stop";
    public static final String API_REQ_QUEST_CLEARITEMGET = "/api_req_quest/clearitemget";

    public static final String[] API_QUEST_REQ_LIST = new String[]{
            API_GET_MEMBER_QUESTLIST,
            API_REQ_QUEST_START,
            API_REQ_QUEST_STOP,
            API_REQ_QUEST_CLEARITEMGET
    };

    public static final String API_GET_MEMBER_BASEAIRCORPS = "/api_get_member/base_air_corps";
    public static final String API_REQ_AIR_CORPS_SETPLANE = "/api_req_air_corps/set_plane";
    public static final String API_REQ_AIR_CORPS_CHANGENAME = "/api_req_air_corps/change_name";
    public static final String API_REQ_AIR_CORPS_SETACTION = "/api_req_air_corps/set_action";
    public static final String API_REQ_AIR_CORPS_SUPPLY = "/api_req_air_corps/supply";
    public static final String API_REQ_AIR_CORPS_EXPANDBASE = "/api_req_air_corps/expand_base";

    public static final String API_GET_MEMBER_MAPINFO = "/api_get_member/mapinfo";
    public static final String API_GET_MEMBER_PRACTICE = "/api_get_member/practice";
    public static final String API_GET_MEMBER_MISSION = "/api_get_member/mission";
    public static final String API_REQ_MAP_START = "/api_req_map/start";
    public static final String API_REQ_MAP_NEXT = "/api_req_map/next";
    public static final String API_REQ_SORTIE_BATTLE = "/api_req_sortie/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT = "/api_req_battle_midnight/battle";
    public static final String API_REQ_SORTIE_BATTLE_MIDNIGHT_SP = "/api_req_battle_midnight/sp_midnight";
    public static final String API_REQ_SORTIE_AIRBATTLE = "/api_req_sortie/airbattle";
    public static final String API_REQ_SORTIE_LDAIRBATTLE = "/api_req_sortie/ld_airbattle";
    public static final String API_REQ_SORTIE_BATTLE_RESULT = "/api_req_sortie/battleresult";
    public static final String API_REQ_SORTIE_NIGHTTODAY = "/api_req_sortie/night_to_day";
    public static final String API_REQ_SORTIE_LDSHOOTING = "/api_req_sortie/ld_shooting"; // 레이더사격
    public static final String API_REQ_SORTIE_GOBACKPORT = "/api_req_sortie/goback_port"; // 단함퇴피

    public static final String API_REQ_MAP_SELECT_EVENTMAP_RANK = "/api_req_map/select_eventmap_rank";

    public static final String API_REQ_COMBINED_BATTLE = "/api_req_combined_battle/battle"; // 기동
    public static final String API_REQ_COMBINED_BATTLE_WATER = "/api_req_combined_battle/battle_water"; // 수상
    public static final String API_REQ_COMBINED_BATTLE_EC = "/api_req_combined_battle/ec_battle"; // 단일-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH = "/api_req_combined_battle/each_battle"; // 기동-연합
    public static final String API_REQ_COMBINED_BATTLE_EACH_WATER = "/api_req_combined_battle/each_battle_water"; // 수상-연합

    public static final String API_REQ_COMBINED_AIRBATTLE = "/api_req_combined_battle/airbattle"; // 아웃레인지
    public static final String API_REQ_COMBINED_LDAIRBATTLE = "/api_req_combined_battle/ld_airbattle"; // 공습
    public static final String API_REQ_COMBINED_LDSHOOTING = "/api_req_combined_battle/ld_shooting"; // 레이더사격?
    public static final String API_REQ_COMBINED_BATTLE_EC_NIGHTTODAY = "/api_req_combined_battle/ec_night_to_day"; // 야간->주간
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT = "/api_req_combined_battle/midnight_battle";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_SP = "/api_req_combined_battle/sp_midnight";
    public static final String API_REQ_COMBINED_BATTLE_MIDNIGHT_EC = "/api_req_combined_battle/ec_midnight_battle"; // 단대연 야전

    public static final String API_REQ_COMBINED_BATTLERESULT = "/api_req_combined_battle/battleresult";
    public static final String API_REQ_COMBINED_GOBACKPORT = "/api_req_combined_battle/goback_port"; // 연합 퇴피

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
            API_REQ_SORTIE_NIGHTTODAY,
            API_REQ_SORTIE_AIRBATTLE,
            API_REQ_SORTIE_LDAIRBATTLE,
            API_REQ_SORTIE_BATTLE_RESULT,
            API_REQ_SORTIE_GOBACKPORT,

            API_REQ_COMBINED_BATTLE,
            API_REQ_COMBINED_BATTLE_WATER,
            API_REQ_COMBINED_AIRBATTLE,
            API_REQ_COMBINED_LDAIRBATTLE,
            API_REQ_SORTIE_LDSHOOTING,
            API_REQ_COMBINED_BATTLE_MIDNIGHT,
            API_REQ_COMBINED_BATTLE_MIDNIGHT_SP,
            API_REQ_COMBINED_BATTLE_EC,
            API_REQ_COMBINED_BATTLE_EACH,
            API_REQ_COMBINED_BATTLE_EACH_WATER,
            API_REQ_COMBINED_BATTLE_MIDNIGHT_EC,
            API_REQ_COMBINED_BATTLE_EC_NIGHTTODAY,
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
    public static final int API_NODE_EVENT_KIND_NIGHTDAYBATTLE_EC = 7;
    public static final int API_NODE_EVENT_KIND_LDSHOOTING = 8;

    public static final int API_NODE_EVENT_KIND_SELECTABLE = 2;

    public static final int API_NODE_EVENT_KIND_AIRSEARCH = 0;

    public static final String KCA_API_DATA_LOADED = "/kca_api/data_loaded";
    public static final String KCA_API_UPDATE_FRONTVIEW = "/kca_api/update_frontview";
    public static final String KCA_API_FAIRY_HIDDEN = "/kca_api/fairy_hidden";
    public static final String KCA_API_FAIRY_RETURN = "/kca_api/fairy_return";
    public static final String KCA_API_FAIRY_CHECKED = "/kca_api/fairy_checked";
    public static final String KCA_API_NOTI_EXP_LEFT = "/kca_api/noti_exp_left";
    public static final String KCA_API_NOTI_EXP_FIN = "/kca_api/noti_exp_fin";
    public static final String KCA_API_NOTI_EXP_CANCELED = "/kca_api/noti_exp_canceled";
    public static final String KCA_API_NOTI_HEAVY_DMG = "/kca_api/noti_heavy_dmg";
    public static final String KCA_API_NOTI_BATTLE_NODE = "/kca_api/noti_battle_node";
    public static final String KCA_API_NOTI_BATTLE_INFO = "/kca_api/noti_battle_info";
    public static final String KCA_API_NOTI_GOBACKPORT = "/kca_api/noti_gobackport";
    public static final String KCA_API_NOTI_BATTLE_DROPINFO = "/kca_api/noti_battle_dropinfo";
    public static final String KCA_API_NOTI_DOCK_FIN = "/kca_api/noti_dock_fin";
    public static final String KCA_API_NOTI_QUEST_STAT = "/kca_api/noti_quest_stat";
    public static final String KCA_API_PREF_CN_CHANGED = "/kca_api/pref_cn_changed";
    public static final String KCA_API_PREF_EXPVIEW_CHANGED = "/kca_api/pref_expview_changed";
    public static final String KCA_API_PREF_FAIRY_CHANGED = "/kca_api/pref_fairy_changed";
    public static final String KCA_API_PREF_PRIORITY_CHANGED = "/kca_api/pref_priority_changed";
    public static final String KCA_API_PREF_NOTICOUNT_CHANGED = "/kca_api/pref_noticount_changed";
    public static final String KCA_API_PREF_LANGUAGE_CHANGED = "/kca_api/pref_language_changed";
    public static final String KCA_API_PREF_ALARMDELAY_CHANGED = "/kca_api/pref_alarmdelay_changed";

    public static final String KCA_MSG_FAIRY_CHANGED = "com.antest1.kcanotify.KcaFairySelectActivity.KCA_MSG_FAIRY_CHANGED";
    public static final String KCA_MSG_BATTLE_NODE = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_NODE";
    public static final String KCA_MSG_BATTLE_INFO = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_INFO";
    public static final String KCA_MSG_BATTLE_HDMG = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_HDMG";
    public static final String KCA_MSG_QUEST_LIST = "com.antest1.kcanotify.KcaService.KCA_MSG_QUEST_LIST";
    public static final String KCA_MSG_QUEST_COMPLETE = "com.antest1.kcanotify.KcaViewButtonService.KCA_MSG_QUEST_COMPLETE";

    public static final String KCA_MSG_BATTLE_VIEW_REFRESH = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_VIEW_REFRESH";
    public static final String KCA_MSG_BATTLE_VIEW_HDMG = "com.antest1.kcanotify.KcaService.KCA_MSG_BATTLE_VIEW_HDMG";
    public static final String KCA_MSG_QUEST_VIEW_LIST = "com.antest1.kcanotify.KcaService.KCA_MSG_QUEST_VIEW_LIST";
    public static final String KCA_MSG_DATA = "com.antest1.kcanotify.KcaService.KCA_MSG_DATA";

    public static final String KCA_API_PROCESS_BATTLE_FAILED = "/kca_api/process_battle_failed";
    public static final String KCA_API_PROCESS_BATTLEVIEW_FAILED = "/kca_api/process_battleview_failed";
    public static final String KCA_API_PROCESS_QUESTVIEW_FAILED = "/kca_api/process_questview_failed";
    public static final String KCA_API_OPENDB_FAILED = "/kca_api/opendb_failed";
    public static final String KCA_API_POIDB_FAILED = "/kca_api/poidb_failed";

    public static final String KCA_API_VPN_DATA_ERROR = "/kca_api/vpn_data_error";
    public static final String KCA_API_RESOURCE_URL = "/kca_api/resource_url";

    public static final int SEEK_PURE = 0;
    public static final int SEEK_33CN1 = 1;
    public static final int SEEK_33CN3 = 3;
    public static final int SEEK_33CN4 = 4;

    public static final int NOTI_FRONT = 0;
    public static final int NOTI_EXP = 1;
    public static final int NOTI_DOCK = 2;
    public static final int NOTI_UPDATE = 3;
    public static final int NOTI_MORALE = 4;
    public static final int NOTI_AKASHI = 5;

    public static final int FRONT_NONE = 0;
    public static final int FRONT_EXP_SET = 2;

    public static final int PHASE_1 = 1;
    public static final int PHASE_2 = 2;
    public static final int PHASE_3 = 3;

    public static final int COMBINED_A = 1;
    public static final int COMBINED_W = 2;

    public static final int HD_NONE = 0;
    public static final int HD_DAMECON = 1;
    public static final int HD_DANGER = 2;

    public static final List<String> API_BATTLE_REQS = Arrays.asList(API_BATTLE_REQ_LIST);
    public static final List<String> API_QUEST_REQS = Arrays.asList(API_QUEST_REQ_LIST);
    public static final List<String> API_WIDGET_TU_REQS = Arrays.asList(API_WIDGET_TIMERUPDATE_LIST);

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
    public static final int FORMATION_DEF = 6; // Temporal: may change after api available
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

    public static final int LAB_STATUS_STANDBY = 0;
    public static final int LAB_STATUS_SORTIE = 1;
    public static final int LAB_STATUS_DEFENSE = 2;
    public static final int LAB_STATUS_RETREAT = 3;
    public static final int LAB_STATUS_REST = 4;

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
    public static final String VPN_STOP_REASON = "vpn_stop_from_main";

    public static final int SNIFFER_ACTIVE = 0;
    public static final int SNIFFER_PASSIVE = 1;

    public static final String PREF_KCA_VERSION = "kca_version";
    public static final String PREF_KCARESOURCE_VERSION = "kcadata_version";
    public static final String PREF_KCA_LANGUAGE = "kca_language";
    public static final String PREF_SNIFFER_MODE = "kca_sniffer_mode";
    public static final String PREF_KCA_SEEK_CN = "kca_seek_cn";
    public static final String PREF_OPENDB_API_USE = "opendb_api_use";
    public static final String PREF_POIDB_API_USE = "poidb_api_use";
    public static final String PREF_KCA_EXP_VIEW = "expedition_view";
    public static final String PREF_KCA_EXP_TYPE = "expedition_type";
    public static final String PREF_KCA_SET_PRIORITY = "set_priority";
    public static final String PREF_KCA_BATTLENODE_USE = "battlenode_use";
    public static final String PREF_KCA_BATTLEVIEW_USE = "battleview_use";
    public static final String PREF_KCA_QUESTVIEW_USE = "questview_use";
    public static final String PREF_OVERLAY_SETTING = "overlay_setting";
    public static final String PREF_SHOWDROP_SETTING = "showdrop_setting";
    public static final String PREF_KCA_DOWNLOAD_DATA = "download_data";
    public static final String PREF_CHECK_UPDATE = "check_update";
    public static final String PREF_KCA_NOTI_NOTIFYATSVCOFF = "notify_at_svc_off";
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
    public static final String PREF_FAIRY_AUTOHIDE = "fairy_auto_hide";
    public static final String PREF_APK_DOWNLOAD_SITE = "apk_download_site";
    public static final String PREF_VPN_BYPASS_ADDRESS = "bypass_address";
    public static final String PREF_FULLMORALE_SETTING = "fullmorale_setting";
    public static final String PREF_KCA_DATA_VERSION = "kca_data_version";
    public static final String PREF_FAIRY_NOTI_LONGCLICK = "notify_fairy_longclick";
    public static final String PREF_DISABLE_CUSTOMTOAST = "disable_customtoast";
    public static final String PREF_KCA_NOTI_QUEST_FAIRY_GLOW = "noti_quest_fairy_glow";
    public static final String PREF_UPDATE_SERVER = "check_update_server";
    public static final String PREF_KCA_NOTI_MORALE = "notify_morale";
    public static final String PREF_KCA_MORALE_MIN = "morale_min";
    public static final String PREF_KCA_NOTI_AKASHI = "notify_akashi";
    public static final String PREF_SHIPINFO_SORTKEY = "shipinfo_sortkey";
    public static final String PREF_SHIPINFO_FILTCOND = "shipinfo_filtcond";
    public static final String PREF_SHIPINFO_SPEQUIPS = "shipinfo_spequips";
    public static final String PREF_EQUIPINFO_SORTKEY = "equipinfo_sortkey";
    public static final String PREF_EQUIPINFO_FILTCOND = "equipinfo_filtcond";
    public static final String PREF_ALARM_DELAY = "alarm_delay";
    public static final String PREF_KCA_ACTIVATE_DROPLOG = "activate_droplog";
    public static final String PREF_KCA_ACTIVATE_RESLOG = "activate_resourcelog";
    public static final String PREF_FV_MENU_ORDER = "fleetview_menu_order";
    public static final String PREF_SHOW_CONSTRSHIP_NAME = "show_constrship_name";
    public static final String PREF_VIEW_YLOC = "view_yloc";
    public static final String PREF_LAST_UPDATE_CHECK = "last_update_check";
    public static final String PREF_TIMER_WIDGET_STATE = "timer_widget_state";
    public static final String PREF_DATALOAD_ERROR_FLAG = "dataload_error_flag";
    public static final String PREF_FIX_VIEW_LOC = "fix_view_loc";
    public static final String PREF_SCREEN_ADV_NETWORK = "adv_network_setting";
    public static final String PREF_LAST_QUEST_CHECK = "last_quest_check";
    public static final String PREF_KCAQSYNC_USE = "kcaqsync_use";
    public static final String PREF_KCAQSYNC_PASS = "kcaqsync_pass";
    public static final String PREF_PACKET_LOG = "packet_log";
    public static final String PREF_CHECK_UPDATE_START = "check_update_start";
    public static final String PREF_FAIRY_DOWN_FLAG = "fairy_downloaded_flag";
    public static final String PREF_RES_USELOCAL = "res_uselocal";
    public static final String PREF_FAIRY_REV = "fairy_rev";
    public static final String PREF_PACKAGE_ALLOW = "package_allow";
    public static final String PREF_ALLOW_EXTFILTER = "allow_external_filter";
    public static final String PREF_DNS_NAMESERVERS = "dns_nameservers";
    public static final String PREF_KC_PACKAGE = "kc_package";
    public static final String PREF_HDNOTI_LOCKED = "hdnoti_locked";
    public static final String PREF_HDNOTI_MINLEVEL = "hdnoti_minlevel";


    public static final String[] PREF_ARRAY = {
            PREF_CHECK_UPDATE,
            PREF_KCA_VERSION,
            PREF_KCARESOURCE_VERSION,
            PREF_CHECK_UPDATE_START,
            PREF_RES_USELOCAL,
            PREF_KCA_LANGUAGE,
            PREF_SNIFFER_MODE,
            PREF_KCA_SEEK_CN,
            PREF_OPENDB_API_USE,
            PREF_POIDB_API_USE,
            PREF_KCA_EXP_VIEW,
            PREF_KCA_EXP_TYPE,
            PREF_KCA_BATTLEVIEW_USE,
            PREF_KCA_QUESTVIEW_USE,
            PREF_OVERLAY_SETTING,
            PREF_SHOWDROP_SETTING,
            PREF_KCA_DOWNLOAD_DATA,
            PREF_KCA_NOTI_NOTIFYATSVCOFF,
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
            PREF_FAIRY_AUTOHIDE,
            PREF_FAIRY_DOWN_FLAG,
            PREF_APK_DOWNLOAD_SITE,
            PREF_SCREEN_ADV_NETWORK,
            PREF_VPN_BYPASS_ADDRESS,
            PREF_FULLMORALE_SETTING,
            PREF_KCA_DATA_VERSION,
            PREF_FAIRY_NOTI_LONGCLICK,
            PREF_DISABLE_CUSTOMTOAST,
            PREF_KCA_NOTI_QUEST_FAIRY_GLOW,
            PREF_UPDATE_SERVER,
            PREF_KCA_NOTI_MORALE,
            PREF_KCA_MORALE_MIN,
            PREF_KCA_NOTI_AKASHI,
            PREF_SHIPINFO_SORTKEY,
            PREF_SHIPINFO_FILTCOND,
            PREF_SHIPINFO_SPEQUIPS,
            PREF_EQUIPINFO_SORTKEY,
            PREF_EQUIPINFO_FILTCOND,
            PREF_KCA_ACTIVATE_DROPLOG,
            PREF_KCA_ACTIVATE_RESLOG,
            PREF_FV_MENU_ORDER,
            PREF_SHOW_CONSTRSHIP_NAME,
            PREF_VIEW_YLOC,
            PREF_LAST_UPDATE_CHECK,
            PREF_TIMER_WIDGET_STATE,
            PREF_DATALOAD_ERROR_FLAG,
            PREF_FIX_VIEW_LOC,
            PREF_LAST_QUEST_CHECK,
            PREF_KCAQSYNC_USE,
            PREF_KCAQSYNC_PASS,
            PREF_FAIRY_REV,
            PREF_PACKAGE_ALLOW,
            PREF_ALLOW_EXTFILTER,
            PREF_DNS_NAMESERVERS,
            PREF_KC_PACKAGE,
            PREF_HDNOTI_LOCKED,
            PREF_HDNOTI_MINLEVEL
    };

    public static final String[] PREF_BOOLEAN_ARRAY = {
            PREF_CHECK_UPDATE_START,
            PREF_RES_USELOCAL,
            PREF_OPENDB_API_USE,
            PREF_POIDB_API_USE,
            PREF_AKASHI_STAR_CHECKED,
            PREF_KCA_SET_PRIORITY,
            PREF_DISABLE_CUSTOMTOAST,
            PREF_KCA_EXP_VIEW,
            PREF_KCA_NOTI_NOTIFYATSVCOFF,
            PREF_KCA_NOTI_DOCK,
            PREF_KCA_NOTI_EXP,
            PREF_KCA_BATTLEVIEW_USE,
            PREF_KCA_BATTLENODE_USE,
            PREF_KCA_QUESTVIEW_USE,
            PREF_KCA_NOTI_V_HD,
            PREF_KCA_NOTI_V_NS,
            PREF_FAIRY_DOWN_FLAG,
            PREF_SHOWDROP_SETTING,
            PREF_FAIRY_NOTI_LONGCLICK,
            PREF_FAIRY_AUTOHIDE,
            PREF_KCA_NOTI_QUEST_FAIRY_GLOW,
            PREF_KCA_NOTI_MORALE,
            PREF_KCA_NOTI_AKASHI,
            PREF_KCA_ACTIVATE_DROPLOG,
            PREF_KCA_ACTIVATE_RESLOG,
            PREF_SHOW_CONSTRSHIP_NAME,
            PREF_DATALOAD_ERROR_FLAG,
            PREF_FIX_VIEW_LOC,
            PREF_KCAQSYNC_USE,
            PREF_ALLOW_EXTFILTER,
            PREF_HDNOTI_LOCKED
    };

    public static final List<String> PREFS_LIST = Arrays.asList(PREF_ARRAY);
    public static final List<String> PREFS_BOOLEAN_LIST = Arrays.asList(PREF_BOOLEAN_ARRAY);
}
