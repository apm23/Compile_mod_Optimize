package com.anjas.forceanvil;

import com.anjas.forceanvil.mixin.BlockEntityTypeAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ForceAnvilMod implements ModInitializer {
    public static final String MOD_ID = "forceanvil";

    public static final Identifier FORCE_ANVIL_ID = Identifier.fromNamespaceAndPath(MOD_ID, "force_anvil");
    public static final ResourceKey<Block> FORCE_ANVIL_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, FORCE_ANVIL_ID);
    public static final ResourceKey<Item> FORCE_ANVIL_ITEM_KEY = ResourceKey.create(Registries.ITEM, FORCE_ANVIL_ID);

    public static final Identifier VELOCITY_HOPPER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "velocity_hopper");
    public static final ResourceKey<Block> VELOCITY_HOPPER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, VELOCITY_HOPPER_ID);
    public static final ResourceKey<Item> VELOCITY_HOPPER_ITEM_KEY = ResourceKey.create(Registries.ITEM, VELOCITY_HOPPER_ID);

    public static final Block FORCE_ANVIL = new ForceAnvilBlock(
        BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).setId(FORCE_ANVIL_BLOCK_KEY)
    );
    public static final Item FORCE_ANVIL_ITEM = new BlockItem(
        FORCE_ANVIL,
        new Item.Properties().setId(FORCE_ANVIL_ITEM_KEY).useBlockDescriptionPrefix()
    );

    public static final Block VELOCITY_HOPPER = new VelocityHopperBlock(
        BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).setId(VELOCITY_HOPPER_BLOCK_KEY)
    );
    public static final Item VELOCITY_HOPPER_ITEM = new BlockItem(
        VELOCITY_HOPPER,
        new Item.Properties().setId(VELOCITY_HOPPER_ITEM_KEY).useBlockDescriptionPrefix()
    );

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, FORCE_ANVIL_BLOCK_KEY, FORCE_ANVIL);
        Registry.register(BuiltInRegistries.ITEM, FORCE_ANVIL_ITEM_KEY, FORCE_ANVIL_ITEM);
        Registry.register(BuiltInRegistries.BLOCK, VELOCITY_HOPPER_BLOCK_KEY, VELOCITY_HOPPER);
        Registry.register(BuiltInRegistries.ITEM, VELOCITY_HOPPER_ITEM_KEY, VELOCITY_HOPPER_ITEM);

        var hopperType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("hopper"));
        ((BlockEntityTypeAccessor)(Object) hopperType).forceanvil$getValidBlocks().add(VELOCITY_HOPPER);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
            output.accept(FORCE_ANVIL_ITEM);
            output.accept(VELOCITY_HOPPER_ITEM);
        });
    }
}
