package com.antest1.kcanotify;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.junit.Test;

public class KcaDeckInfoShipListTest {
    private static JsonArray ships(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    @Test
    public void sameWhenTheShipsAndOrderMatch() {
        assertTrue(KcaDeckInfo.isSameShipList(ships("[10,20,30,-1,-1,-1]"), ships("[10,20,30,-1,-1,-1]")));
    }

    @Test
    public void sameWhenOnlyTrailingEmptySlotsDiffer() {
        assertTrue(KcaDeckInfo.isSameShipList(ships("[10,20,-1,-1,-1,-1]"), ships("[10,20,-1,-1,-1,-1,-1]")));
    }

    @Test
    public void differentWhenAShipIsAddedOrRemoved() {
        assertFalse(KcaDeckInfo.isSameShipList(ships("[10,20,30,-1,-1,-1]"), ships("[10,20,-1,-1,-1,-1]")));
        assertFalse(KcaDeckInfo.isSameShipList(ships("[10,20,-1,-1,-1,-1]"), ships("[10,20,30,-1,-1,-1]")));
    }

    @Test
    public void differentWhenTheOrderChanges() {
        assertFalse(KcaDeckInfo.isSameShipList(ships("[10,20,30,-1,-1,-1]"), ships("[20,10,30,-1,-1,-1]")));
    }
}
