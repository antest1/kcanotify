package com.antest1.kcanotify;

import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KcaDeckInfo {
    // Formula 33 (2016.12.26)
    // Reference: http://ja.kancolle.wikia.com/wiki/%E3%83%9E%E3%83%83%E3%83%97%E7%B4%A2%E6%95%B5
    //            http://kancolle-calc.net/deckbuilder.html
    public static double getSeekValue(JSONArray deckPortData, int deckid, boolean isCombined, int Cn) {
        double pureTotalSeek = 0.0;
        double totalSeek = 0.0;

        double totalShipSeek = 0.0;
        double totalEquipSeek = 0.0;
        double hqPenalty = 0.0;
        double noShipBonus = 0;

        int userLevel = KcaApiData.getLevel();
        hqPenalty = Math.ceil(0.4*userLevel);

        int noShipCount = 6;
        List<JSONObject> shipDataList = new ArrayList<JSONObject>();
        JSONArray deckShipIdList = (JSONArray) ((JSONObject) deckPortData.get(deckid)).get("api_ship");
        for(int i=0; i<deckShipIdList.size(); i++) {
            int shipId = intv(deckShipIdList.get(i));
            if (shipId != -1) {
                noShipCount -= 1;
                JSONObject shipData = KcaApiData.getUserShipDataById(shipId, "slot,sakuteki");
                int shipSeek = intv(((JSONArray) shipData.get("sakuteki")).get(0));
                pureTotalSeek += shipSeek;

                JSONArray shipItem = (JSONArray) shipData.get("slot");
                for (int j = 0; j < shipItem.size(); j++) {
                    int item_id = intv(shipItem.get(j));
                    if (item_id != -1) {
                        JSONObject itemData = KcaApiData.getUserItemStatusById (item_id, "level,alv", "name,type,saku");
                        String itemName = (String)itemData.get("name");
                        int itemLevel = intv(itemData.get("level"));
                        int itemType = intv(((JSONArray) itemData.get("type")).get(2));
                        int itemSeek = intv(itemData.get("saku"));
                        shipSeek -= itemSeek;

                        switch (itemType) {
                            case KcaApiData.T2_TORPEDO_BOMBER:
                                totalEquipSeek += 0.8 * itemSeek;
                                break;
                            case KcaApiData.T2_SCOUT:
                                totalEquipSeek += 1 * itemSeek;
                                break;
                            case KcaApiData.T2_SCOUT_II:
                                totalEquipSeek += 1 * itemSeek;
                                break;
                            case KcaApiData.T2_SEA_SCOUT:
                                totalEquipSeek += 1.2 * (itemSeek + 1.2 * Math.sqrt(itemLevel));
                                break;
                            case KcaApiData.T2_SEA_BOMBER:
                                totalEquipSeek += 1.1 * itemSeek;
                                break;
                            case KcaApiData.T2_RADAR_LARGE:
                                totalEquipSeek += 0.6 * (itemSeek + 1.4 * Math.sqrt(itemLevel));
                                break;
                            case KcaApiData.T2_RADAR_SMALL:
                                totalEquipSeek += 0.6 * (itemSeek + 1.25 * Math.sqrt(itemLevel));
                                break;
                            default:
                                totalEquipSeek += 0.6 * itemSeek;
                                break;
                        }
                        Log.e("KCA", itemName + " " + String.valueOf(itemLevel) + " " + String.valueOf(itemType) + " " + String.valueOf(itemSeek));
                    }
                }
                totalShipSeek += Math.sqrt(shipSeek);
            }
        }
        noShipBonus =  2*noShipCount;
        totalSeek = totalShipSeek + Cn * totalEquipSeek - hqPenalty + noShipBonus;
        return totalSeek;
    }

    private static Integer intv(Object o) {
        return ((Long) o).intValue();
    }

}
