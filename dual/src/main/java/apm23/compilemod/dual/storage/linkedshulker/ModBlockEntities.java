package com.anjas.linkedshulker;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<LinkedShulkerBlockEntity> LINKED_SHULKER = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(LinkedShulkerMod.MOD_ID, "linked_shulker_box"),
        FabricBlockEntityTypeBuilder.create(LinkedShulkerBlockEntity::new, ModBlocks.LINKED_SHULKER).build()
    );

    public static void initialize() {}
    private ModBlockEntities() {}
}
