package com.antest1.kcanotify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KcaAkashiRepairTest {
    // {equipped, total, usable} as KcaDeckInfo gives them; usable is 0 when moderately damaged or worse
    private static int[] akashi(int equipped) { return new int[]{equipped, equipped + 2, 1}; }
    private static int[] asahiKai(int equipped) { return new int[]{equipped, equipped, 1}; }
    private static int[] damaged(int[] ship) { return new int[]{ship[0], ship[1], 0}; }

    private static final double DELTA = 1e-9;

    @Test
    public void oneRepairShipRepairsAsManyShipsAsFacilities() {
        // Akashi: 2 built in + equipped
        assertEquals(2, KcaAkashiRepairInfo.getRepairReach(akashi(0), null));
        assertEquals(3, KcaAkashiRepairInfo.getRepairReach(akashi(1), null));
        assertEquals(6, KcaAkashiRepairInfo.getRepairReach(akashi(4), null));
        // Asahi Kai: equipped only, up to herself and the 2nd and 3rd ships
        assertEquals(1, KcaAkashiRepairInfo.getRepairReach(asahiKai(1), null));
        assertEquals(3, KcaAkashiRepairInfo.getRepairReach(asahiKai(3), null));
        assertEquals(1.0, KcaAkashiRepairInfo.getRepairModifier(akashi(1), null), DELTA);
    }

    @Test
    public void noRepairWithoutFacilityOrRepairShipFlagship() {
        assertEquals(0, KcaAkashiRepairInfo.getRepairReach(asahiKai(0), null));
        // a repair ship only as the 2nd ship does not repair
        assertEquals(0, KcaAkashiRepairInfo.getRepairReach(null, akashi(1)));
        // a moderately damaged flagship does not repair
        assertEquals(0, KcaAkashiRepairInfo.getRepairReach(damaged(akashi(2)), null));
    }

    @Test
    public void twoRepairShipsRepair2PlusEquippedFacilities() {
        // Akashi + Asahi Kai, no facility needed: the base 2
        assertTrue(KcaAkashiRepairInfo.isTwoRepairShips(akashi(0), asahiKai(0)));
        assertEquals(2, KcaAkashiRepairInfo.getRepairReach(akashi(0), asahiKai(0)));
        assertEquals(5.0 / 6.0, KcaAkashiRepairInfo.getRepairModifier(akashi(0), asahiKai(0)), DELTA);
        // either one can be the flagship; facilities on both count: 4 in total for the 6th ship
        assertEquals(6, KcaAkashiRepairInfo.getRepairReach(asahiKai(2), akashi(2)));
        assertEquals(6, KcaAkashiRepairInfo.getRepairReach(akashi(1), asahiKai(3)));
        // 5 in total for the 7th ship of a striking force, and no more
        assertEquals(7, KcaAkashiRepairInfo.getRepairReach(akashi(2), asahiKai(3)));
        assertEquals(7, KcaAkashiRepairInfo.getRepairReach(akashi(4), asahiKai(3)));
    }

    @Test
    public void damagedSecondRepairShipLeavesOneRepairShip() {
        assertFalse(KcaAkashiRepairInfo.isTwoRepairShips(asahiKai(1), damaged(akashi(0))));
        assertEquals(1, KcaAkashiRepairInfo.getRepairReach(asahiKai(1), damaged(akashi(0))));
        assertEquals(1.0, KcaAkashiRepairInfo.getRepairModifier(asahiKai(1), damaged(akashi(0))), DELTA);
    }

    @Test
    public void twoRepairShipsShortenEachRepair() {
        // level 1 destroyer: 10 seconds per HP, 25/3 seconds with two repair ships
        assertEquals(130, KcaDocking.getRepairedHp(1, KcaApiData.STYPE_DD, 1.0, 1300));
        assertEquals(156, KcaDocking.getRepairedHp(1, KcaApiData.STYPE_DD, 5.0 / 6.0, 1300));
        assertEquals(10, KcaDocking.getNextRepair(1, KcaApiData.STYPE_DD, 1.0, 1300));
        assertEquals(9, KcaDocking.getNextRepair(1, KcaApiData.STYPE_DD, 5.0 / 6.0, 1300));
    }

    @Test
    public void showsExpectedRepairBefore20Minutes() {
        // level 1 destroyer: 10 seconds per HP, applied all at once after 20 minutes
        assertEquals(10, KcaDocking.getRepairedHp(1, KcaApiData.STYPE_DD, 1.0, 100));
        assertEquals(10, KcaDocking.getNextRepair(1, KcaApiData.STYPE_DD, 1.0, 100));
        assertEquals(119, KcaDocking.getRepairedHp(1, KcaApiData.STYPE_DD, 1.0, 1199));
        assertEquals(120, KcaDocking.getRepairedHp(1, KcaApiData.STYPE_DD, 1.0, 1200));
    }

    @Test
    public void atLeastOneHpFrom20Minutes() {
        // level 99 battleship: 1270 seconds per HP, rounded up to 1 HP at 20 minutes
        assertEquals(0, KcaDocking.getRepairedHp(99, KcaApiData.STYPE_BB, 1.0, 100));
        assertEquals(1200 - 100, KcaDocking.getNextRepair(99, KcaApiData.STYPE_BB, 1.0, 100));
        assertEquals(1, KcaDocking.getRepairedHp(99, KcaApiData.STYPE_BB, 1.0, 1200));
        assertEquals(2540 - 1200, KcaDocking.getNextRepair(99, KcaApiData.STYPE_BB, 1.0, 1200));
        assertEquals(2, KcaDocking.getRepairedHp(99, KcaApiData.STYPE_BB, 1.0, 2540));
    }
}
