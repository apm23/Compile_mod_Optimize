package com.anjas.forceanvil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ForceAnvilBlock extends AnvilBlock {
    private static final Component TITLE = Component.translatable("container.forceanvil.force_anvil");

    public ForceAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, ignored) -> new ForceAnvilMenu(syncId, inventory, ContainerLevelAccess.create(level, pos)),
                TITLE
            ));
        }
        return InteractionResult.SUCCESS;
    }
}
