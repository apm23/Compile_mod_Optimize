package com.anjas.custominventory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

final class InventoryAlgorithmsRegressionTest {
    @Test
    void mergeCombinesStacksWithoutLoss() {
        List<ItemStack> out = InventoryAlgorithms.merge(List.of(
            new ItemStack(Items.STONE, 30),
            new ItemStack(Items.STONE, 40),
            new ItemStack(Items.DIRT, 11)
        ));

        assertEquals(81, out.stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(70, out.stream().filter(s -> s.is(Items.STONE)).mapToInt(ItemStack::getCount).sum());
        assertTrue(out.stream().allMatch(s -> s.getCount() <= s.getMaxStackSize()));
    }

    @Test
    void mergeDoesNotCollapseDifferentComponents() {
        ItemStack pristine = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(1);

        List<ItemStack> out = InventoryAlgorithms.merge(List.of(pristine, damaged));
        assertEquals(2, out.size());
        assertFalse(ItemStack.isSameItemSameComponents(out.get(0), out.get(1)));
    }

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

    @Test
    void conservationGuardRejectsLoss() {
        assertThrows(IllegalStateException.class, () -> InventoryAlgorithms.verifyConservation(
            List.of(new ItemStack(Items.IRON_INGOT, 10)),
            List.of(new ItemStack(Items.IRON_INGOT, 9)),
            "regression"
        ));
    }
}
