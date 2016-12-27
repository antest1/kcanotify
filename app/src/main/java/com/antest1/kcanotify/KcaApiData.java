package com.antest1.kcanotify;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

	public static int level = 0;
	public static long experience = 0;

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


	public static final int[] T2LIST_AIRCRAFTS = {T2_FIGHTER, T2_BOMBER, T2_TORPEDO_BOMBER, T2_SCOUT, T2_SEA_SCOUT, T2_SEA_BOMBER, T2_FLYING_BOAT,
			T2_SEA_FIGHTER, T2_LBA_AIRCRAFT, T2_ITCP_FIGHTER, T2_JET_FIGHTER, T2_JET_BOMBER, T2_JET_TORPEDO_BOMBER, T2_JET_SCOUT};

	public static final int[] BASIC_MASTERY_MIN_BONUS = {0, 10, 25, 40, 55, 70, 85, 100};
	public static final int[] BASIC_MASTERY_MAX_BONUS = {9, 24, 39, 54, 69, 84, 99, 120};

	public static final int[] FIGHTER_MASTERY_BONUS = {0, 0, 2, 5, 9, 14, 14, 22, 0, 0, 0};
	public static final int[] SEA_BOMBER_MASTERY_BONUS = {0, 0, 1, 1, 1, 3, 3, 6, 0, 0, 0};

	public static final int SPEED_FAST = 10;
	public static final int SPEED_SLOW = 5;
	public static final int SPEED_NONE = 0;
	public static final int SPEED_MIXED = 15;

	private static Integer intv(Object o) {
		return ((Long) o).intValue();
	}
	private static Long longv(Object o) { return (Long) o; }
	private static String joinStr(List<String> list, String delim) {
		String resultStr = "";
		int i;
		for (i = 0; i < list.size() - 1; i++) {
			resultStr = resultStr.concat(list.get(i));
			resultStr = resultStr.concat(delim);
		}
		resultStr = resultStr.concat(list.get(i));
		return resultStr;
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


	public static int getLevel() {
		return level;
	}

	public static long getExperience() {
		return experience;
	}

	public static int getPortData(JSONObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userShipData.keySet());
		if (api_data.containsKey("api_basic")) {
			JSONObject basicInfo = (JSONObject) api_data.get("api_basic");
			level = intv(basicInfo.get("api_level"));
			experience = longv(basicInfo.get("api_experience"));
		}

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

	public static int updatePortDataOnBattle(JSONObject api_data) {
		if (api_data.containsKey("api_ship_data")) {
			JSONArray shipDataArray = (JSONArray) api_data.get("api_ship_data");
			JSONObject temp;
			for (ListIterator<JSONObject> itr = shipDataArray.listIterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = ((Long) temp.get("api_id")).intValue();
				userShipData.put(api_id, temp);
			}
			return shipDataArray.size();
		} else {
			return -1;
		}
	}

	public static int updateSlotItemData(JSONArray api_data) {
		JSONObject temp;
		if (api_data != null) {
			for (ListIterator<JSONObject> itr = api_data.listIterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = ((Long) temp.get("api_id")).intValue();
				userShipData.put(api_id, temp);
			}
			return api_data.size();
		} else {
			return -1;
		}
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

	public static JSONObject getUserItemStatusById(int id, String list, String kclist) {
		if(userItemData.containsKey(id)) {
			int kc_item_id = intv(userItemData.get(id).get("api_slotitem_id"));
			JSONObject kcData = getKcItemStatusById(kc_item_id, kclist);
			JSONObject userData = userItemData.get(id);

			if (list == "all") {
				for(Object k: userData.keySet()) {
					kcData.put(k, userData.get(k));
				}
			} else {
				String[] requestList = list.split(",");
				for(int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					if(userData.containsKey(api_item)) {
						kcData.put(orig_api_item, userData.get(api_item));
					}
				}
			}
			return kcData;
		} else {
			return null;
		}
	}

	public static JSONObject getKcItemStatusById(int id, String list) {
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

	public static boolean isItemAircraft(int id) {
		int result = Arrays.binarySearch(T2LIST_AIRCRAFTS, id);
		if (result > 0) return true;
		else return false;
	}

	public static void addUserShip(JSONObject api_data) {
		if (api_data.containsKey("api_id")) {
			int shipId = intv(api_data.get("api_id"));
			JSONObject shipData = (JSONObject) api_data.get("api_ship");
			userShipData.put(shipId, shipData);
			int shipKcId = intv(api_data.get("api_ship_id"));
			String shipName = (String) getKcShipDataById(shipKcId, "name").get("name");
			Log.e("KCA", String.format("add ship %d (%s)", shipId, shipName));

			JSONArray shipSlotItemData = (JSONArray) api_data.get("api_slotitem");
			for (int i=0; i<shipSlotItemData.size(); i++) {
				addUserItem((JSONObject) shipSlotItemData.get(i));
			}
		}
	}

	public static void deleteUserShip(String api_ship_id) {
		int shipId = Integer.valueOf(api_ship_id);
		JSONObject shipKcData = getUserShipDataById(shipId,"ship_id,slot");

		int shipKcId = intv(shipKcData.get("ship_id"));
		JSONArray shipSlotItem = (JSONArray) shipKcData.get("slot");
		List<String> shipSlotItemList = new ArrayList<String>();
		for (int i=0; i<shipSlotItem.size(); i++) {
			int item = intv(shipSlotItem.get(i));
			if (item != -1) {
				shipSlotItemList.add(String.valueOf(item));
			}
		}
		deleteUserItem(joinStr(shipSlotItemList, ","));
		userShipData.remove(shipId);

		String shipName = (String) getKcShipDataById(shipKcId, "name").get("name");
		Log.e("KCA", String.format("remove ship %d (%s)",shipId, shipName));
	}

	public static void addUserItem(JSONObject api_data) {
		JSONObject item = null;
		if (api_data.containsKey("api_create_flag") && intv(api_data.get("api_create_flag")) == 1) {
			item = (JSONObject) api_data.get("api_slot_item");
		} else if(api_data.containsKey("api_slotitem_id")) {
			item = api_data;
		}
		if (item != null) {
			int item_id = intv(item.get("api_id"));
			int kc_item_id = intv(item.get("api_slotitem_id"));
			int itemType = intv(((JSONArray) getKcItemStatusById(kc_item_id, "type").get("type")).get(2));
			item.put("api_locked", 0);
			item.put("api_level", 0);
			if(isItemAircraft(itemType)) {
				item.put("api_alv", 0);
			}
			userItemData.put(item_id, item);
			String itemName = (String) getKcItemStatusById(kc_item_id, "name").get("name");
			Log.e("KCA", String.format("add item %d (%s)",item_id, itemName));
		}
	}

	public static void deleteUserItem(String list) {
		Log.e("KCA", list);
		String[] requestList = list.split(",");
		for (int i=0; i<requestList.length; i++) {
			int itemId = Integer.valueOf(requestList[i]);
			String itemName = (String) getUserItemStatusById(itemId, "id", "name").get("name");
			userItemData.remove(Integer.valueOf(requestList[i]));
			Log.e("KCA", String.format("remove item %d (%s)",itemId, itemName));
		}
	}
}
