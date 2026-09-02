package com.anjas.godvillagers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaczEnchantRuntimeTest {
    private static final float EPS = 0.0001F;

    @Test
    void lifeStealRatiosMatchBalanceContract() {
        assertEquals(0.50F, TaczEnchantRuntime.healingForDamage(10.0F, 1), EPS);
        assertEquals(0.75F, TaczEnchantRuntime.healingForDamage(10.0F, 2), EPS);
        assertEquals(1.00F, TaczEnchantRuntime.healingForDamage(10.0F, 3), EPS);
    }

    @Test
    void lifeStealClampsAboveMaximumLevel() {
        assertEquals(1.00F, TaczEnchantRuntime.healingForDamage(10.0F, 99), EPS);
    }

    @Test
    void invalidDamageOrLevelCannotHeal() {
        assertEquals(0.0F, TaczEnchantRuntime.healingForDamage(0.0F, 3), EPS);
        assertEquals(0.0F, TaczEnchantRuntime.healingForDamage(-5.0F, 3), EPS);
        assertEquals(0.0F, TaczEnchantRuntime.healingForDamage(10.0F, 0), EPS);
    }

    @Test
    void actualDamageUsesVictimHealthDeltaAndCapsLethalOverkill() {
        assertEquals(3.0F, TaczEnchantRuntime.actualDamage(10.0F, 7.0F), EPS);
        assertEquals(4.0F, TaczEnchantRuntime.actualDamage(4.0F, 0.0F), EPS);
        assertEquals(4.0F, TaczEnchantRuntime.actualDamage(4.0F, -20.0F), EPS);
        assertEquals(0.0F, TaczEnchantRuntime.actualDamage(4.0F, 5.0F), EPS);
    }

    @Test
    void invalidHealthSnapshotsCannotCreateHealingBudget() {
        assertEquals(0.0F, TaczEnchantRuntime.actualDamage(Float.NaN, 0.0F), EPS);
        assertEquals(0.0F, TaczEnchantRuntime.actualDamage(10.0F, Float.POSITIVE_INFINITY), EPS);
    }

    @Test
    void overflowConvertsToHalfAbsorptionBudget() {
        assertEquals(0.50F, TaczEnchantRuntime.absorptionForOverflow(1.0F), EPS);
        assertEquals(0.0F, TaczEnchantRuntime.absorptionForOverflow(-1.0F), EPS);
    }
}
