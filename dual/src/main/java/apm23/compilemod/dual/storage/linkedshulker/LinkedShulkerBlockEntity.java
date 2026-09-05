package com.anjas.linkedshulker;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class LinkedShulkerBlockEntity extends BlockEntity implements Container, MenuProvider {
    private String channel = "default";
    private String channelLabel = "default";
    private final NonNullList<ItemStack> fallback = NonNullList.withSize(ChannelStorageData.SIZE, ItemStack.EMPTY);
    private @Nullable NonNullList<ItemStack> serverItemsCache;
    private int viewers = 0;
    private int animationFrame = 0;

    public LinkedShulkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINKED_SHULKER, pos, state);
        if (state.hasProperty(LinkedShulkerBlock.OPEN_FRAME)) animationFrame = state.getValue(LinkedShulkerBlock.OPEN_FRAME);
    }

    public void setChannel(String rawName) {
        this.channelLabel = ChannelStorageData.displayName(rawName);
        this.channel = ChannelStorageData.normalize(rawName);
        this.serverItemsCache = null;
        setChanged();
    }

    public String channel() { return channel; }
    public String channelLabel() { return channelLabel; }

    private NonNullList<ItemStack> items() {
        if (level instanceof ServerLevel serverLevel) {
            NonNullList<ItemStack> cached = serverItemsCache;
            if (cached == null) {
                cached = ChannelStorageData.get(serverLevel.getServer()).inventory(channel);
                serverItemsCache = cached;
            }
            return cached;
        }
        return fallback;
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) ChannelStorageData.get(serverLevel.getServer()).setDirty();
    }

    @Override public int getContainerSize() { return ChannelStorageData.SIZE; }
    @Override public boolean isEmpty() { return items().stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items().get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items(), slot, amount);
        if (!result.isEmpty()) changed();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items(), slot);
        if (!result.isEmpty()) changed();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items().set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        changed();
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items().clear(); changed(); }

    @Override
    public void startOpen(ContainerUser user) {
        if (viewers == 0 && level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.SHULKER_SHOOT, SoundSource.BLOCKS, 0.28F, 0.72F);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.24F, 1.62F);
        }
        viewers++;
    }

    @Override
    public void stopOpen(ContainerUser user) {
        int previousViewers = viewers;
        viewers = Math.max(0, viewers - 1);
        if (previousViewers > 0 && viewers == 0 && level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.SHULKER_CLOSE, SoundSource.BLOCKS, 0.30F, 0.86F);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.16F, 1.20F);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LinkedShulkerBlockEntity be) {
        if (level.isClientSide()) return;
        int target = be.viewers > 0 ? LinkedShulkerBlock.MAX_OPEN_FRAME : 0;
        if (be.animationFrame == target) return;
        boolean opening = target > be.animationFrame;
        be.animationFrame = target;

        if (opening && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5D, pos.getY() + 0.62D, pos.getZ() + 0.5D,
                6, 0.28D, 0.08D, 0.28D, 0.015D);
        }

        if (state.hasProperty(LinkedShulkerBlock.OPEN_FRAME)) {
            level.setBlock(pos, state.setValue(LinkedShulkerBlock.OPEN_FRAME, target), 3);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Shared channel storage must never be dropped from a single linked block.
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Linked Shulker [" + channelLabel + "]");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("channel", Codec.STRING, channel);
        output.store("channel_label", Codec.STRING, channelLabel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String savedChannel = input.read("channel", Codec.STRING).orElse("default");
        String savedLabel = input.read("channel_label", Codec.STRING).orElse(savedChannel);
        channelLabel = ChannelStorageData.displayName(savedLabel);
        channel = ChannelStorageData.normalize(channelLabel);
        serverItemsCache = null;
    }
}
