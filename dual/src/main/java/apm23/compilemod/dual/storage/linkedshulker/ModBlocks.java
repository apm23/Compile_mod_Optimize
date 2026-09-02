package com.anjas.linkedshulker;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final BlockItemId LINKED_SHULKER_ID = BlockItemId.create(
        Identifier.fromNamespaceAndPath(LinkedShulkerMod.MOD_ID, "linked_shulker_box"),
        Identifier.fromNamespaceAndPath(LinkedShulkerMod.MOD_ID, "linked_shulker_box")
    );

    public static final Block LINKED_SHULKER = register(
        LINKED_SHULKER_ID,
        LinkedShulkerBlock::new,
        BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.STONE).pushReaction(PushReaction.BLOCK)
    );

    private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        Block block = factory.apply(properties.setId(id.block()));
        Registry.register(BuiltInRegistries.BLOCK, id.block(), block);
        BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
        Registry.register(BuiltInRegistries.ITEM, id.item(), item);
        return block;
    }

    public static void initialize() {}
    private ModBlocks() {}
}
