package com.anjas.linkedshulker;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class LinkedShulkerBlock extends BaseEntityBlock {
    public static final int MAX_OPEN_FRAME = 7;
    public static final IntegerProperty OPEN_FRAME = IntegerProperty.create("open", 0, MAX_OPEN_FRAME);

    public LinkedShulkerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN_FRAME, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN_FRAME);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(LinkedShulkerBlock::new); }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LinkedShulkerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LINKED_SHULKER, LinkedShulkerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LinkedShulkerBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof LinkedShulkerBlockEntity be) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            be.setChannel(custom == null ? "default" : custom.getString());
        }
        if (level instanceof ServerLevel server) {
            refreshChunkAnchor(server, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level instanceof ServerLevel server) {
            refreshChunkAnchor(server, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative() && level.getBlockEntity(pos) instanceof LinkedShulkerBlockEntity be) {
            ItemStack drop = new ItemStack(this);
            drop.set(DataComponents.CUSTOM_NAME, Component.literal(be.channelLabel()));
            popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        refreshChunkAnchorExcluding(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    private static boolean hasAdjacentHopper(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof HopperBlockEntity) {
                return true;
            }
        }
        return false;
    }

    private static boolean chunkNeedsAnchor(ServerLevel level, BlockPos excludedPos) {
        return level.getChunkAt(excludedPos).getBlockEntities().entrySet().stream()
            .filter(entry -> !entry.getKey().equals(excludedPos))
            .filter(entry -> entry.getValue() instanceof LinkedShulkerBlockEntity)
            .anyMatch(entry -> hasAdjacentHopper(level, entry.getKey()));
    }

    private static void refreshChunkAnchor(ServerLevel level, BlockPos pos) {
        boolean shouldForce = hasAdjacentHopper(level, pos) || chunkNeedsAnchor(level, pos);
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, shouldForce);
    }

    private static void refreshChunkAnchorExcluding(ServerLevel level, BlockPos removedPos) {
        boolean shouldForce = chunkNeedsAnchor(level, removedPos);
        level.setChunkForced(removedPos.getX() >> 4, removedPos.getZ() >> 4, shouldForce);
    }
}
