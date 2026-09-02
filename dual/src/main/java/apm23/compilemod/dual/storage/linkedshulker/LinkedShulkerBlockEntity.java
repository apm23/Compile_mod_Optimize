package com.anjas.linkedshulker;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
    private int viewers = 0;
    private int animationFrame = 0;

    public LinkedShulkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINKED_SHULKER, pos, state);
    }

    public void setChannel(String rawName) {
        this.channelLabel = ChannelStorageData.displayName(rawName);
        this.channel = ChannelStorageData.normalize(rawName);
        setChanged();
    }

    public String channel() { return channel; }
    public String channelLabel() { return channelLabel; }

    private NonNullList<ItemStack> items() {
        if (level instanceof ServerLevel serverLevel) {
            return ChannelStorageData.get(serverLevel.getServer()).inventory(channel);
        }
        return fallback;
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            ChannelStorageData.get(serverLevel.getServer()).setDirty();
        }
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
        viewers++;
    }

    @Override
    public void stopOpen(ContainerUser user) {
        viewers = Math.max(0, viewers - 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LinkedShulkerBlockEntity be) {
        if (level.isClientSide()) return;
        int target = be.viewers > 0 ? LinkedShulkerBlock.MAX_OPEN_FRAME : 0;
        if (be.animationFrame == target) return;
        be.animationFrame += be.animationFrame < target ? 1 : -1;
        if (state.hasProperty(LinkedShulkerBlock.OPEN_FRAME)) {
            level.setBlock(pos, state.setValue(LinkedShulkerBlock.OPEN_FRAME, be.animationFrame), 3);
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
        channel = ChannelStorageData.normalize(savedChannel);
        channelLabel = ChannelStorageData.displayName(savedLabel);
    }
}
