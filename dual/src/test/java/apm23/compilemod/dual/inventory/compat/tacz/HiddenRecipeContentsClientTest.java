package com.anjas.custominventory.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiddenRecipeContentsClientTest {
    @Test
    void replaceDropsOnlyEmptyEntriesAndPreservesExactCounts() {
        HiddenRecipeContentsClient.clear();
        long before = HiddenRecipeContentsClient.revision();

        ItemStack diamonds = new ItemStack(Items.DIAMOND, 37);
        ItemStack arrows = new ItemStack(Items.ARROW, 12);
        HiddenRecipeContentsClient.replace(List.of(ItemStack.EMPTY, diamonds, arrows));

        List<ItemStack> view = HiddenRecipeContentsClient.view();
        assertEquals(2, view.size());
        assertEquals(Items.DIAMOND, view.get(0).getItem());
        assertEquals(37, view.get(0).getCount());
        assertEquals(Items.ARROW, view.get(1).getItem());
        assertEquals(12, view.get(1).getCount());
        assertEquals(before + 1, HiddenRecipeContentsClient.revision());
    }

    @Test
    void replaceCopiesInputsAndSnapshotIsDefensive() {
        HiddenRecipeContentsClient.clear();
        ItemStack source = new ItemStack(Items.IRON_INGOT, 23);
        HiddenRecipeContentsClient.replace(List.of(source));

        source.setCount(1);
        assertEquals(23, HiddenRecipeContentsClient.view().getFirst().getCount());

        ItemStack snapshot = HiddenRecipeContentsClient.snapshot().getFirst();
        assertNotSame(HiddenRecipeContentsClient.view().getFirst(), snapshot);
        snapshot.setCount(2);
        assertEquals(23, HiddenRecipeContentsClient.view().getFirst().getCount());
    }

    @Test
    void clearEmptiesCacheAndAdvancesRevision() {
        HiddenRecipeContentsClient.replace(List.of(new ItemStack(Items.GOLD_INGOT, 5)));
        long before = HiddenRecipeContentsClient.revision();
        HiddenRecipeContentsClient.clear();
        assertTrue(HiddenRecipeContentsClient.view().isEmpty());
        assertEquals(before + 1, HiddenRecipeContentsClient.revision());
    }
}
