package com.anjas.custominventory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Standalone JUnit coverage must not instantiate Minecraft Item/ItemStack registries.
 * The merge/component/conservation probes are already executed by
 * InventoryAlgorithms.runStartupSelfTests() after SERVER_STARTED and therefore run
 * inside the real Minecraft bootstrap during the dedicated-server smoke.
 */
final class InventoryAlgorithmsRegressionTest {
    @Test
    void verticalSortMappingCoversEverySlotExactlyOnce() {
        boolean[] seen = new boolean[27];
        for (int logical = 0; logical < 27; logical++) {
            int slot = InventoryAlgorithms.verticalSlot(logical);
            assertTrue(slot >= 0 && slot < 27);
            assertFalse(seen[slot], "duplicate physical slot " + slot);
            seen[slot] = true;
        }
        for (boolean value : seen) assertTrue(value);
    }
}
