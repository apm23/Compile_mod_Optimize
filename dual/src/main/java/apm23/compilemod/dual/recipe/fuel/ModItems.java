package com.anjas.fuelrecipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ModItems {
    public static final ResourceKey<Item> COPPERCHARGED_BAMBOO_KEY = key("coppercharged_bamboo");
    public static final ResourceKey<Item> COPPERCHARGED_LAVA_BUCKET_KEY = key("coppercharged_lava_bucket");

    public static final Item COPPERCHARGED_BAMBOO = register(COPPERCHARGED_BAMBOO_KEY, new Item.Properties());
    public static final Item COPPERCHARGED_LAVA_BUCKET = register(
        COPPERCHARGED_LAVA_BUCKET_KEY,
        new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).fireResistant()
    );

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FuelRecipeMod.MOD_ID, path));
    }

    private static Item register(ResourceKey<Item> key, Item.Properties properties) {
        Item item = new Item(properties.useItemDescriptionPrefix().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {}
    private ModItems() {}
}
