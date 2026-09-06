package com.anjas.custominventory;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczCraftingScanOrderTest {
    @Test
    void everyActivePageCoversHotbarAndAllEightPagesExactlyOnce() throws Exception {
        Method method = TaczCraftingCompat.class.getDeclaredMethod("buildScanOrders");
        method.setAccessible(true);
        int[][] orders = (int[][]) method.invoke(null);

        int expectedSize = 9 + InventoryStorage.PAGE_COUNT * InventoryStorage.PAGE_SIZE;
        assertEquals(InventoryStorage.PAGE_COUNT, orders.length);

        for (int active = 0; active < orders.length; active++) {
            int[] order = orders[active];
            assertEquals(expectedSize, order.length);

            Set<Integer> unique = new HashSet<>();
            for (int slot : order) {
                assertTrue(slot >= 0 && slot < expectedSize, "slot out of range: " + slot);
                assertTrue(unique.add(slot), "duplicate slot in scan order: " + slot);
            }
            assertEquals(expectedSize, unique.size());

            for (int hotbar = 0; hotbar < 9; hotbar++) {
                assertEquals(hotbar, order[hotbar], "hotbar priority drifted");
            }

            int activeStart = 9 + active * InventoryStorage.PAGE_SIZE;
            for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) {
                assertEquals(activeStart + i, order[9 + i], "active page priority drifted");
            }
        }
    }

    @Test
    void hiddenPagesFollowActivePageWithoutDroppingAnyPage() throws Exception {
        Method method = TaczCraftingCompat.class.getDeclaredMethod("buildScanOrders");
        method.setAccessible(true);
        int[][] orders = (int[][]) method.invoke(null);

        for (int active = 0; active < orders.length; active++) {
            int[] order = orders[active];
            Set<Integer> pageIds = new HashSet<>();
            for (int i = 9; i < order.length; i++) {
                pageIds.add((order[i] - 9) / InventoryStorage.PAGE_SIZE);
            }
            assertEquals(InventoryStorage.PAGE_COUNT, pageIds.size(),
                    "not all inventory pages are represented for active page " + active + ": " + Arrays.toString(order));
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) assertTrue(pageIds.contains(page));
        }
    }
}
