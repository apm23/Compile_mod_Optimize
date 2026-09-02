package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RewardMathTest {
    @Test
    void stackSplittingPreservesExactRewardCount() {
        int[] totals = {1, 2, 63, 64, 65, 127, 128, 129, 500, 4096, 262144};
        int[] stackSizes = {1, 16, 64};
        for (int total : totals) {
            for (int maxStack : stackSizes) {
                int remaining = total;
                int accounted = 0;
                int entities = 0;
                while (remaining > 0) {
                    int amount = RewardMath.nextStackSize(remaining, maxStack);
                    assertTrue(amount > 0 && amount <= maxStack);
                    accounted += amount;
                    remaining -= amount;
                    entities++;
                }
                assertEquals(total, accounted);
                assertEquals((total + maxStack - 1) / maxStack, entities);
            }
        }
    }

    @Test
    void invalidInputsProduceNoStack() {
        assertEquals(0, RewardMath.nextStackSize(0, 64));
        assertEquals(0, RewardMath.nextStackSize(-1, 64));
        assertEquals(0, RewardMath.nextStackSize(10, 0));
    }
}
