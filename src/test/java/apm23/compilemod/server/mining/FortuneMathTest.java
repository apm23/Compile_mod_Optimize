package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class FortuneMathTest {
    @Test
    void noFortuneAlwaysDropsOne() {
        assertEquals(1, FortuneMath.multiplierForRoll(0, 0));
        assertEquals(1, FortuneMath.multiplierForRoll(0, 99));
    }

    @Test
    void fortuneUsesVanillaStyleMultiplierCurve() {
        assertEquals(1, FortuneMath.multiplierForRoll(1, 0));
        assertEquals(1, FortuneMath.multiplierForRoll(1, 1));
        assertEquals(2, FortuneMath.multiplierForRoll(1, 2));
        assertEquals(1, FortuneMath.multiplierForRoll(3, 0));
        assertEquals(1, FortuneMath.multiplierForRoll(3, 1));
        assertEquals(2, FortuneMath.multiplierForRoll(3, 2));
        assertEquals(3, FortuneMath.multiplierForRoll(3, 3));
        assertEquals(4, FortuneMath.multiplierForRoll(3, 4));
    }

    @Test
    void outOfRangeRollsAreSafelyClamped() {
        assertEquals(1, FortuneMath.multiplierForRoll(3, -10));
        assertEquals(4, FortuneMath.multiplierForRoll(3, 999));
    }
}
