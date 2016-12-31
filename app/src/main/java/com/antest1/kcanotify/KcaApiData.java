package com.antest1.kcanotify;

import android.content.res.AssetManager;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class KcaApiData {
	public static JsonObject kcGameData = null;
	public static Map<Integer, JsonObject> kcShipData = new HashMap<Integer, JsonObject>();
	public static Map<Integer, JsonObject> kcItemData = new HashMap<Integer, JsonObject>();
	public static Map<Integer, JsonObject> userShipData = new HashMap<Integer, JsonObject>();
	public static Map<Integer, JsonObject> userItemData = new HashMap<Integer, JsonObject>();

	public static int level = 0;
	public static Integer experience = 0;

	public static int maxShipSize = 0;
	public static int maxItemSize = 0;
	public static int getShipCountInBattle = 0;

	public static JsonObject mapEdgeInfo = new JsonObject();
	public static int[] eventMapDifficulty = new int[10];

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

	public static final int[] T2LIST_FIGHT_AIRCRAFTS = {T2_FIGHTER, T2_BOMBER, T2_TORPEDO_BOMBER, T2_SEA_BOMBER,
			T2_SEA_FIGHTER, T2_LBA_AIRCRAFT, T2_ITCP_FIGHTER, T2_JET_FIGHTER, T2_JET_BOMBER, T2_JET_TORPEDO_BOMBER};

	public static final int[] BASIC_MASTERY_MIN_BONUS = {0, 10, 25, 40, 55, 70, 85, 100};
	public static final int[] BASIC_MASTERY_MAX_BONUS = {9, 24, 39, 54, 69, 84, 99, 120};

	public static final int[] FIGHTER_MASTERY_BONUS = {0, 0, 2, 5, 9, 14, 14, 22, 0, 0, 0};
	public static final int[] SEA_BOMBER_MASTERY_BONUS = {0, 0, 1, 1, 1, 3, 3, 6, 0, 0, 0};

	public static final int SPEED_FAST = 10;
	public static final int SPEED_SLOW = 5;
	public static final int SPEED_NONE = 0;
	public static final int SPEED_MIXED = 15;

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

	public static int getKcGameData(JsonObject api_data) {
		//Log.e("KCA", "getKcGameData Called");
		kcGameData = api_data;
		if (kcGameData.has("api_mst_ship")) {
			JsonArray shipStatusArray = (JsonArray) kcGameData.get("api_mst_ship");
			JsonElement temp;
			for (Iterator<JsonElement> itr = shipStatusArray.iterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
				kcShipData.put(api_id, temp.getAsJsonObject());
			}
		}
		if (kcGameData.has("api_mst_slotitem")) {
			JsonArray itemStatusArray = (JsonArray) kcGameData.get("api_mst_slotitem");
			JsonElement temp;
			for (Iterator<JsonElement> itr = itemStatusArray.iterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
				kcItemData.put(api_id, temp.getAsJsonObject());
			}
		}
		Log.e("KCA", "ship: ".concat(String.valueOf(kcShipData.size())));
		Log.e("KCA", "item: ".concat(String.valueOf(kcItemData.size())));
		return kcGameData.entrySet().size();
	}

	public static boolean isGameDataLoaded() {
		return kcGameData != null;
	}

	public static boolean isUserItemDataLoaded() {
		return userItemData != null;
	}

	public static int getLevel() {
		return level;
	}

	public static Integer getExperience() {
		return experience;
	}

	public static void addShipCountInBattle() { getShipCountInBattle += 1; }
	public static void resetShipCountInBattle() { getShipCountInBattle = 0; }

	public static boolean checkUserShipMax() { return maxShipSize == (userShipData.size() + getShipCountInBattle); }
	public static boolean checkUserItemMax() { return maxItemSize == userItemData.size(); }
	public static boolean checkUserPortEnough() { return !(checkUserShipMax() || checkUserItemMax()); }

	public static int getEventMapDifficulty(int no) {
		return eventMapDifficulty[no];
	}

	public static void setEventMapDifficulty(int no, int diff) {
		eventMapDifficulty[no] = diff;
	}

	public static int getShipSize() {
		return userShipData.size();
	}

	public static void loadMapEdgeInfo(JsonObject data) {
		mapEdgeInfo = data;
	}

	public static String getCurrentNodeAlphabet(int maparea, int mapno, int no) {
		String currentMapString = String.format("%d-%d", maparea, mapno);
		if (mapEdgeInfo != null && mapEdgeInfo.has(currentMapString)) {
			JsonArray nodeInfo = mapEdgeInfo.getAsJsonObject(currentMapString)
											.getAsJsonArray(String.valueOf(no));
			return nodeInfo.get(1).getAsString();
		}
		else {
			return String.valueOf(no);
		}
	}

	public static int getPortData(JsonObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userShipData.keySet());
		if (api_data.has("api_basic")) {
			JsonObject basicInfo = (JsonObject) api_data.get("api_basic");
			level = basicInfo.get("api_level").getAsInt();
			experience = basicInfo.get("api_experience").getAsInt();
		}

		if (api_data.has("api_ship")) {
			JsonArray shipDataArray = (JsonArray) api_data.get("api_ship");
			JsonElement temp;
			for (Iterator<JsonElement> itr = shipDataArray.iterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
				if(!prevItemIds.contains(api_id)) {
					userShipData.put(api_id, temp.getAsJsonObject());
				} else if(!userShipData.get(api_id).equals(temp)) {
					userShipData.put(api_id, temp.getAsJsonObject());
				}
				prevItemIds.remove(api_id);
			}
		}
		for (Integer i: prevItemIds) {
			userShipData.remove(i);
		}
		return userShipData.size();
	}

	public static int updatePortDataOnBattle(JsonObject api_data) {
		if (api_data.has("api_ship_data")) {
			JsonArray shipDataArray = (JsonArray) api_data.get("api_ship_data");
			JsonElement temp;
			for (Iterator<JsonElement> itr = shipDataArray.iterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
				userShipData.put(api_id, temp.getAsJsonObject());
			}
			return shipDataArray.size();
		} else {
			return -1;
		}
	}

	public static int updateSlotItemData(JsonArray api_data) {
		JsonElement temp;
		if (api_data != null) {
			for (Iterator<JsonElement> itr = api_data.iterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
				if(!userItemData.containsKey(api_id)) {
					Log.e("KCA", String.valueOf(api_id) + " added");
				}
				userItemData.put(api_id, temp.getAsJsonObject());
			}
			return api_data.size();
		} else {
			return -1;
		}
	}

	public static Integer getUserId(JsonObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userItemData.keySet());
		if (api_data.has("api_basic")) {
			JsonObject basic = api_data.get("api_basic").getAsJsonObject();
			return basic.get("api_member_id").getAsInt();
		}
		return -1;
	}

	public static int getSlotItemData(JsonObject api_data) {
		Set<Integer> prevItemIds = new HashSet<Integer>(userItemData.keySet());
		if (api_data.has("api_slot_item")) {
			JsonArray slotItemApiData = (JsonArray) api_data.get("api_slot_item");
			JsonElement temp;
			for (Iterator<JsonElement> itr = slotItemApiData.iterator(); itr.hasNext();) {
				temp = itr.next();
				Integer api_id = temp.getAsJsonObject().get("api_id").getAsInt();
				if(!prevItemIds.contains(api_id)) {
					userItemData.put(api_id, temp.getAsJsonObject());
				} else if(!userItemData.get(api_id).equals(temp.getAsJsonObject())) {
					userItemData.put(api_id, temp.getAsJsonObject());
				}
				prevItemIds.remove(api_id);
			}
		}
		for (Integer i: prevItemIds) {
			userItemData.remove(i);
		}
		return userItemData.size();
	}

	public static JsonObject getKcShipDataById(int id, String list) {
		if (kcGameData == null) return null;
		JsonObject temp = new JsonObject();
		if(kcShipData.containsKey(id)) {
			if (list.equals("all")) {
				return kcShipData.get(id);
			} else {
				String[] requestList = list.split(",");
				for (int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					temp.add(orig_api_item, kcShipData.get(id).get(api_item));
				}
				return temp;
			}
		} else {
			Log.e("KCA", String.valueOf(id) + " not in list");
			return null;
		}
	}

	public static JsonObject getUserShipDataById(int id, String list) {
		JsonObject temp = new JsonObject();
		if(userShipData.containsKey(id)) {
			if (list.equals("all")) {
				return userShipData.get(id);
			} else {
				String[] requestList = list.split(",");
				for (int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					temp.add(orig_api_item, userShipData.get(id).get(api_item));
				}
				return temp;
			}
		} else {
			return null;
		}
	}

	public static JsonObject getUserItemStatusById(int id, String list, String kclist) {
		if (kcGameData == null) return null;
		if(userItemData.containsKey(id)) {
			int kc_item_id = userItemData.get(id).get("api_slotitem_id").getAsInt();
			JsonObject kcData = getKcItemStatusById(kc_item_id, kclist);
			JsonObject userData = userItemData.get(id);

			if (list.equals("all")) {
				for(Map.Entry<String, JsonElement> k: userData.entrySet()) {
					kcData.add(k.getKey(), userData.get(k.getKey()));
				}
			} else {
				String[] requestList = list.split(",");
				for(int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					if(userData.has(api_item)) {
						kcData.add(orig_api_item, userData.get(api_item));
					}
				}
			}
			return kcData;
		} else {
			return null;
		}
	}

	public static JsonObject getKcItemStatusById(int id, String list) {
		if (kcGameData == null) return null;
		JsonObject temp = new JsonObject();
		if(kcItemData.containsKey(id)) {
			if (list.equals("all")) {
				return kcItemData.get(id);
			} else {
				String[] requestList = list.split(",");
				for (int i=0; i<requestList.length; i++) {
					String orig_api_item = requestList[i];
					String api_item = orig_api_item;
					if(!api_item.startsWith("api_")) {
						api_item = "api_" + api_item;
					}
					temp.add(orig_api_item, kcItemData.get(id).get(api_item));
				}
				return temp.getAsJsonObject();
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

	public static void addUserShip(JsonObject api_data) {
		if (kcGameData == null) return;
		if (api_data.has("api_id")) {
			int shipId = api_data.get("api_id").getAsInt();
			JsonObject shipData = (JsonObject) api_data.get("api_ship");
			userShipData.put(shipId, shipData);
			int shipKcId = api_data.get("api_ship_id").getAsInt();
			String shipName = getKcShipDataById(shipKcId, "name").get("name").getAsString();
			Log.e("KCA", String.format("add ship %d (%s)", shipId, shipName));

			JsonArray shipSlotItemData = (JsonArray) api_data.get("api_slotitem");
			for (int i=0; i<shipSlotItemData.size(); i++) {
				addUserItem((JsonObject) shipSlotItemData.get(i));
			}
		}
	}

	public static void updateUserShip(JsonObject api_data) {
		if (kcGameData == null) return;
		if (api_data.has("api_id")) {
			int shipId = api_data.get("api_id").getAsInt();
			userShipData.put(shipId, api_data);
			int shipKcId = api_data.get("api_ship_id").getAsInt();
			String shipName = getKcShipDataById(shipKcId, "name").get("name").getAsString();
			Log.e("KCA", String.format("update ship %d (%s)", shipId, shipName));
		}
	}

	public static void updateUserShipSlot(int shipId, JsonObject api_data) {
		if (kcGameData == null) return;
		if (api_data.has("api_slot")) {
			JsonObject shipData = userShipData.get(shipId).getAsJsonObject();
			shipData.add("api_slot", api_data.getAsJsonArray("api_slot"));
			userShipData.put(shipId, shipData);
		}
	}

	public static void deleteUserShip(String api_ship_id) {
		if (kcGameData == null) return;
		int shipId = Integer.valueOf(api_ship_id);
		JsonObject shipKcData = getUserShipDataById(shipId,"ship_id,slot");

		int shipKcId = shipKcData.get("ship_id").getAsInt();
		JsonArray shipSlotItem = (JsonArray) shipKcData.get("slot");
		List<String> shipSlotItemList = new ArrayList<String>();
		for (int i=0; i<shipSlotItem.size(); i++) {
			int item = shipSlotItem.get(i).getAsInt();
			if (item != -1) {
				shipSlotItemList.add(String.valueOf(item));
			}
		}
		deleteUserItem(joinStr(shipSlotItemList, ","));
		userShipData.remove(shipId);

		String shipName = getKcShipDataById(shipKcId, "name").get("name").getAsString();
		Log.e("KCA", String.format("remove ship %d (%s)",shipId, shipName));
	}

	public static int addUserItem(JsonObject api_data) {
		if (kcGameData == null) return -1;
		JsonObject item = null;
		if (api_data.has("api_create_flag") && api_data.get("api_create_flag").getAsInt() == 1) {
			item = (JsonObject) api_data.get("api_slot_item");
		} else if(api_data.has("api_slotitem_id")) {
			item = api_data;
		}
		if (item != null) {
			int item_id = item.get("api_id").getAsInt();
			int kc_item_id = item.get("api_slotitem_id").getAsInt();
			int itemType = getKcItemStatusById(kc_item_id, "type").get("type").getAsJsonArray().get(2).getAsInt();
			item.addProperty("api_locked", 0);
			item.addProperty("api_level", 0);
			if(isItemAircraft(itemType)) {
				item.addProperty("api_alv", 0);
			}
			userItemData.put(item_id, item);
			String itemName = getKcItemStatusById(kc_item_id, "name").get("name").getAsString();
			Log.e("KCA", String.format("add item %d (%s)",item_id, itemName));
			return kc_item_id;
		} else {
			return 0;
		}
	}

	public static void deleteUserItem(String list) {
		if (kcGameData == null) return;
		Log.e("KCA", list);
		String[] requestList = list.split(",");
		for (int i=0; i<requestList.length; i++) {
			int itemId = Integer.valueOf(requestList[i]);
			String itemName = getUserItemStatusById(itemId, "id", "name").get("name").getAsString();
			userItemData.remove(Integer.valueOf(requestList[i]));
			Log.e("KCA", String.format("remove item %d (%s)",itemId, itemName));
		}
	}

}
