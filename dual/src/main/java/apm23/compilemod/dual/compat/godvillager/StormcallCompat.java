package com.anjas.godvillagers;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

public final class StormcallCompat {
    private static final String BOOK_KEY = "godvillagers_stormcall_book";
    private static final String ITEM_KEY = "godvillagers_stormcall";
    private static final String VISUAL_KEY = "godvillagers_stormcall_visual";
    private StormcallCompat() {}

    private static boolean flag(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty()) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(key).orElse(false);
    }

    public static boolean isStormcallBook(ItemStack stack) { return flag(stack, BOOK_KEY); }
    public static boolean hasStormcall(ItemStack stack) { return flag(stack, ITEM_KEY); }

    public static void applyStormcall(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(ITEM_KEY, true);
        tag.putBoolean(VISUAL_KEY, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("⚡ Stormcall I"))));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }
}
