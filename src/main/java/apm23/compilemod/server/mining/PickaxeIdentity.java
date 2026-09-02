package com.apm23.custompickaxe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class PickaxeIdentity {
    private static final String ROOT_KEY = "custom_pickaxe";

    private PickaxeIdentity() {
    }

    public static boolean isRemotePickaxe(ItemStack stack) {
        return stack.is(Items.IRON_PICKAXE) && !type(stack).isEmpty();
    }

    public static String type(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = customData.copyTag();
        CompoundTag marker = root.getCompoundOrEmpty(ROOT_KEY);
        return marker.getStringOr("type", "");
    }

    public static boolean isEnabled(ItemStack stack) {
        if (!isRemotePickaxe(stack)) {
            return false;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag marker = customData.copyTag().getCompoundOrEmpty(ROOT_KEY);
        return marker.getBooleanOr("enabled", false);
    }

    public static boolean toggleEnabled(ItemStack stack) {
        if (!isRemotePickaxe(stack)) {
            return false;
        }

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = customData.copyTag();
        CompoundTag marker = root.getCompoundOrEmpty(ROOT_KEY);
        boolean enabled = !marker.getBooleanOr("enabled", false);
        marker.putBoolean("enabled", enabled);
        root.put(ROOT_KEY, marker);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return enabled;
    }
}
