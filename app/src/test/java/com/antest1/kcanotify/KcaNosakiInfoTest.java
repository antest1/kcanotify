package com.antest1.kcanotify;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KcaNosakiInfoTest {
    // nowHp, maxHp, cond, fuel, fuelMax, bull, bullMax, onExpedition, inDock
    @Test
    public void effectiveWhenAllConditionsAreMet() {
        assertTrue(KcaNosakiInfo.isNosakiEffective(40, 40, 49, 15, 15, 20, 20, false, false));
        assertTrue(KcaNosakiInfo.isNosakiEffective(31, 40, 30, 15, 15, 20, 20, false, false));
    }

    @Test
    public void notEffectiveWhenLightlyDamagedOrWorse() {
        // 30/40 is exactly 75%: lightly damaged
        assertFalse(KcaNosakiInfo.isNosakiEffective(30, 40, 49, 15, 15, 20, 20, false, false));
    }

    @Test
    public void notEffectiveWithMoraleBelow30() {
        assertFalse(KcaNosakiInfo.isNosakiEffective(40, 40, 29, 15, 15, 20, 20, false, false));
    }

    @Test
    public void notEffectiveWhenNotFullySupplied() {
        assertFalse(KcaNosakiInfo.isNosakiEffective(40, 40, 49, 14, 15, 20, 20, false, false));
        assertFalse(KcaNosakiInfo.isNosakiEffective(40, 40, 49, 15, 15, 19, 20, false, false));
    }

    @Test
    public void notEffectiveOnExpeditionOrInDocking() {
        assertFalse(KcaNosakiInfo.isNosakiEffective(40, 40, 49, 15, 15, 20, 20, true, false));
        assertFalse(KcaNosakiInfo.isNosakiEffective(40, 40, 49, 15, 15, 20, 20, false, true));
    }
}
