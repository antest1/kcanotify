package com.antest1.kcanotify;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class KcaApiData {
	public static JSONObject kcGameData = null;
	public static Map<Integer, JSONObject> kcShipData = new HashMap<Integer, JSONObject>();
	public static Map<Integer, JSONObject> kcItemData = new HashMap<Integer, JSONObject>();
	public static Map<Integer, JSONObject> userShipData = new HashMap<Integer, JSONObject>();
	public static Map<Integer, JSONObject> userItemData = new HashMap<Integer, JSONObject>();

	// Equipment Constants (api_type 2)
	public static final int T2_GUN_SMALL = 1;
	public static final int T2_GUN_MEDIUM = 2;
	public static final int T2_GUN_LARGE = 3;
	public static final int T2_SUB_GUN = 4;
	public static final int T2_TORPEDO = 5;
	public static final int T2_FIGHTER = 6;
	public static final int T2_BOMBER = 7;
	public static final int T2_TORPEDO_BOMBER = 8;
	public static final int T2_SCOUT = 9;
	public static final int T2_SEA_SCOUT = 10;
	public static final int T2_SEA_BOMBER = 11;
	public static final int T2_RADAR_SMALL = 12;
	public static final int T2_RADAR_LARGE = 13;
	public static final int T2_SONAR = 14;
	public static final int T2_DEPTH_CHARGE = 15; // 폭뢰
	public static final int T2_EXT_ARMOR = 16; // NOT USED
	public static final int T2_TURBINE = 17; // 기관부강화
	public static final int T2_SANSHIKIDAN = 18;
	public static final int T2_AP_SHELL = 19;
	public static final int T2_VT_FUZE = 20; // NOT USED
	public static final int T2_MACHINE_GUN = 21;
	public static final int T2_KOHYOTEKI = 22;
	public static final int T2_DAMECON = 23;
	public static final int T2_LANDING_CRAFT= 24;
	public static final int T2_AUTOGYRO = 25;
	public static final int T2_ANTISUB_PATROL = 26;
	public static final int T2_EXT_ARMOR_M = 27;
	public static final int T2_EXT_ARMOR_L = 28;
	public static final int T2_SEARCHLIGHT = 29;
	public static final int T2_DRUM_CAN = 30;
	public static final int T2_REPAIR_INFRA = 31;
	public static final int T2_SS_TORPEDO = 32;
	public static final int T2_STAR_SHELL = 33;
	public static final int T2_COMMAND_FAC = 34;
	public static final int T2_AVI_PERSONNEL = 35;
	public static final int T2_ANTI_AIR_DEVICE = 36;
	public static final int T2_ANTI_GROUND_EQIP = 37;
	public static final int T2_GUN_LARGE_II = 38;
	public static final int T2_SHIP_PERSONNEL = 39;
	public static final int T2_SONAR_LARGE = 40;
	public static final int T2_FLYING_BOAT = 41;
	public static final int T2_SEARCHLIGHT_LARGE = 42;
	public static final int T2_COMBAT_FOOD = 43;
	public static final int T2_SUPPLIES = 44;
	public static final int T2_SEA_FIGHTER = 45;
	public static final int T2_AMP_TANK = 46;
	public static final int T2_LBA_AIRCRAFT = 47;
	public static final int T2_ITCP_FIGHTER = 48;

	public static final int T2_JET_FIGHTER = 56;
	public static final int T2_JET_BOMBER = 57;
	public static final int T2_JET_TORPEDO_BOMBER = 58;
	public static final int T2_JET_SCOUT = 59;

	public static final int T2_RADER_LARGE_II = 93;
	public static final int T2_SCOUT_II = 94;

	private static Integer intv(Object o) {
		return ((Long) o).intValue();
	}

	public static int getKcGameData(JSONObject api_data) {
		//Log.e("KCA", "getKcGameData Called");
		kcGameData = api_data;
		if (kcGameData.containsKey("api_mst_ship")) {
			JSONArray shipStatusArray = (JSONArray) kcGameData.get("api_mst_ship");
			JSONObject temp;
			for (ListIterator<JSONObject> itr = shipStatusArray.listIterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = ((Long) temp.get("api_id")).intValue();
				kcShipData.put(api_id, temp);
			}
		}
		if (kcGameData.containsKey("api_mst_slotitem")) {
			JSONArray itemStatusArray = (JSONArray) kcGameData.get("api_mst_slotitem");
			JSONObject temp;
			for (ListIterator<JSONObject> itr = itemStatusArray.listIterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = ((Long) temp.get("api_id")).intValue();
				kcItemData.put(api_id, temp);
			}
		}
		return kcGameData.size();
	}

	public static boolean isGameDataLoaded() {
		return kcGameData != null;
	}

	public static int getPortData(JSONObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userShipData.keySet());
		if (api_data.containsKey("api_ship")) {
			JSONArray shipDataArray = (JSONArray) api_data.get("api_ship");
			JSONObject temp;
			for (ListIterator<JSONObject> itr = shipDataArray.listIterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = ((Long) temp.get("api_id")).intValue();
				if(!prevItemIds.contains(api_id)) {
					userShipData.put(api_id, temp);
				} else if(!userShipData.get(api_id).equals(temp)) {
					userShipData.put(api_id, temp);
				}
				prevItemIds.remove(api_id);
			}
		}
		for (Integer i: prevItemIds) {
			userShipData.remove(i);
		}
		return userShipData.size();
	}

	public static Integer getUserId(JSONObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userItemData.keySet());
		if (api_data.containsKey("api_basic")) {
			JSONObject basic = (JSONObject) api_data.get("api_basic");
			return intv(basic.get("api_member_id"));
		}
		return -1;
	}

	public static int getSlotItemData(JSONObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userItemData.keySet());
		if (api_data.containsKey("api_slot_item")) {
			JSONArray slotItemApiData = (JSONArray) api_data.get("api_slot_item");
			JSONObject temp;
			for (ListIterator<JSONObject> itr = slotItemApiData.listIterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = ((Long) temp.get("api_id")).intValue();
				if(!prevItemIds.contains(api_id)) {
					userItemData.put(api_id, temp);
				} else if(!userItemData.get(api_id).equals(temp)) {
					userItemData.put(api_id, temp);
				}
				prevItemIds.remove(api_id);
			}
		}
		for (Integer i: prevItemIds) {
			userItemData.remove(i);
		}
		return userItemData.size();
	}

	public static JSONObject getKcShipDataById(int id, String list) {
		JSONObject temp = new JSONObject();
		if(kcShipData.containsKey(id)) {
			if (list == "all") {
				return kcShipData.get(id);
			} else {
				String[] requestList = list.split(",");
				for (int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					temp.put(orig_api_item, kcShipData.get(id).get(api_item));
				}
				return temp;
			}
		} else {
			Log.e("KCA", String.valueOf(id) + " not in list");
			return null;
		}
	}

	public static JSONObject getUserShipDataById(int id, String list) {
		JSONObject temp = new JSONObject();
		if(userShipData.containsKey(id)) {
			if (list == "all") {
				return userShipData.get(id);
			} else {
				String[] requestList = list.split(",");
				for (int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					temp.put(orig_api_item, userShipData.get(id).get(api_item));
				}
				return temp;
			}
		} else {
			return null;
		}
	}

	public static JSONObject getItemStatusById(int id, String list) {
		JSONObject temp = new JSONObject();
		if(kcItemData.containsKey(id)) {
			if (list == "all") {
				return kcItemData.get(id);
			} else {
				String[] requestList = list.split(",");
				for (int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					temp.put(orig_api_item, kcItemData.get(id).get(api_item));
				}
				return temp;
			}
		} else {
			return null;
		}
		
	}
}
