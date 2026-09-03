package com.anjas.custominventory.mixin;

import com.anjas.custominventory.CustomHotbarInventory;
import com.anjas.custominventory.InventoryStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces vanilla's incremental merchant autofill on the logical server with one transaction
 * across hotbar + all eight inventory pages. The full operation is simulated first so a missing
 * second cost can never partially consume/move the first cost. Once validated, payment slots are
 * filled like vanilla (up to one compatible stack), preserving post-trade change in the slot.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    @Inject(method = "tryMoveItems", at = @At("HEAD"), cancellable = true)
    private void custominventory$atomicPagedTradeFill(int newTradeIndex, CallbackInfo ci) {
        if (!(this.trader.getTradingPlayer() instanceof ServerPlayer player)) return;
        ci.cancel();

        if (newTradeIndex < 0 || newTradeIndex >= this.trader.getOffers().size()) return;

        InventoryStorage.snapshotLive(player);
        int activePage = InventoryStorage.active(player);
        List<ItemStack> working = custominventory$snapshotVirtualInventory(player);

        if (!custominventory$insertFully(working, this.tradeContainer.getItem(0).copy())) return;
        if (!custominventory$insertFully(working, this.tradeContainer.getItem(1).copy())) return;

        MerchantOffer offer = this.trader.getOffers().get(newTradeIndex);
        int requiredA = offer.getCostA().getCount();
        int requiredB = offer.getCostB().getCount();

        List<ItemStack> preflight = custominventory$copyStacks(working);
        if (custominventory$extractExact(preflight, offer.getItemCostA(), requiredA) == null) return;
        if (offer.getItemCostB().isPresent()
                && custominventory$extractExact(preflight, offer.getItemCostB().get(), requiredB) == null) return;

        ItemStack paymentA = custominventory$extractPaymentStack(
                working, offer.getItemCostA(), requiredA);
        if (paymentA == null) return;

        ItemStack paymentB = ItemStack.EMPTY;
        if (offer.getItemCostB().isPresent()) {
            paymentB = custominventory$extractPaymentStack(
                    working, offer.getItemCostB().get(), requiredB);
            if (paymentB == null) return;
        }

        custominventory$commitVirtualInventory(player, activePage, working);
        this.tradeContainer.setItem(0, paymentA);
        this.tradeContainer.setItem(1, paymentB);
        InventoryStorage.sync(player);
        CustomHotbarInventory.sendHiddenRecipeState(player);
    }

    /**
     * Vanilla MerchantMenu only returns remaining payment-slot items to the materialized inventory
     * when the menu closes. If that page is full it drops the change, even when another virtual
     * page still has room. Pre-empt that close path with the same merge-first virtual insertion
     * used by paged trading, then clear the payment slots so vanilla has nothing left to drop.
     */
    @Inject(method = "removed", at = @At("HEAD"))
    private void custominventory$returnChangeAcrossPages(Player closingPlayer, CallbackInfo ci) {
        if (!(closingPlayer instanceof ServerPlayer player)) return;

        ItemStack first = this.tradeContainer.getItem(0).copy();
        ItemStack second = this.tradeContainer.getItem(1).copy();
        if (first.isEmpty() && second.isEmpty()) return;

        InventoryStorage.snapshotLive(player);
        int activePage = InventoryStorage.active(player);
        List<ItemStack> working = custominventory$snapshotVirtualInventory(player);

        // Atomic close: either both payment/change stacks fit somewhere in the virtual inventory,
        // or leave the menu untouched and let vanilla use its normal fallback behavior.
        if (!custominventory$insertFully(working, first.copy())) return;
        if (!custominventory$insertFully(working, second.copy())) return;

        custominventory$commitVirtualInventory(player, activePage, working);
        this.tradeContainer.setItem(0, ItemStack.EMPTY);
        this.tradeContainer.setItem(1, ItemStack.EMPTY);
        InventoryStorage.sync(player);
        CustomHotbarInventory.sendHiddenRecipeState(player);
    }

    /** hotbar (9) followed by page 1..8 (8 x 27). */
    @Unique
    private static List<ItemStack> custominventory$snapshotVirtualInventory(ServerPlayer player) {
        ArrayList<ItemStack> out = new ArrayList<>(9 + InventoryStorage.PAGE_COUNT * InventoryStorage.PAGE_SIZE);
        for (int slot = 0; slot < 9; slot++) out.add(player.getInventory().getItem(slot).copy());
        for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
            for (ItemStack stack : InventoryStorage.read(player, page)) out.add(stack.copy());
        }
        return out;
    }

    @Unique
    private static List<ItemStack> custominventory$copyStacks(List<ItemStack> source) {
        ArrayList<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack stack : source) copy.add(stack.copy());
        return copy;
    }

    @Unique
    private static void custominventory$commitVirtualInventory(ServerPlayer player, int activePage, List<ItemStack> working) {
        for (int slot = 0; slot < 9; slot++) player.getInventory().setItem(slot, working.get(slot).copy());

        int offset = 9;
        for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
            ArrayList<ItemStack> pageStacks = new ArrayList<>(InventoryStorage.PAGE_SIZE);
            for (int index = 0; index < InventoryStorage.PAGE_SIZE; index++) {
                pageStacks.add(working.get(offset + page * InventoryStorage.PAGE_SIZE + index).copy());
            }
            if (page == activePage) InventoryStorage.loadLive(player, pageStacks);
            else InventoryStorage.write(player, page, pageStacks);
        }
        InventoryStorage.snapshotLive(player);
    }

    /** Simulates Inventory.placeItemBackInInventory semantics without touching live state. */
    @Unique
    private static boolean custominventory$insertFully(List<ItemStack> slots, ItemStack incoming) {
        if (incoming.isEmpty()) return true;

        for (ItemStack existing : slots) {
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, incoming)) continue;
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) continue;
            int moved = Math.min(space, incoming.getCount());
            existing.grow(moved);
            incoming.shrink(moved);
            if (incoming.isEmpty()) return true;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).isEmpty()) continue;
            int moved = Math.min(incoming.getMaxStackSize(), incoming.getCount());
            slots.set(i, incoming.copyWithCount(moved));
            incoming.shrink(moved);
            if (incoming.isEmpty()) return true;
        }
        return false;
    }

    /** Extract exactly the required amount from one component-compatible variant for preflight. */
    @Unique
    private static ItemStack custominventory$extractExact(List<ItemStack> slots, ItemCost cost, int required) {
        if (required <= 0) return ItemStack.EMPTY;
        ItemStack representative = custominventory$findSatisfyingVariant(slots, cost, required);
        if (representative == null) return null;
        return custominventory$extractVariant(slots, cost, representative, required);
    }

    @Unique
    private static ItemStack custominventory$extractPaymentStack(List<ItemStack> slots, ItemCost cost, int required) {
        if (required <= 0) return ItemStack.EMPTY;
        ItemStack representative = custominventory$findSatisfyingVariant(slots, cost, required);
        if (representative == null) return null;

        int available = custominventory$countVariant(slots, cost, representative);
        int move = Math.min(representative.getMaxStackSize(), available);
        if (move < required) return null;
        return custominventory$extractVariant(slots, cost, representative, move);
    }

    @Unique
    private static ItemStack custominventory$findSatisfyingVariant(List<ItemStack> slots, ItemCost cost, int required) {
        for (ItemStack candidate : slots) {
            if (candidate.isEmpty() || !cost.test(candidate)) continue;
            if (custominventory$countVariant(slots, cost, candidate) >= required) return candidate.copy();
        }
        return null;
    }

    @Unique
    private static int custominventory$countVariant(List<ItemStack> slots, ItemCost cost, ItemStack representative) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && cost.test(stack)
                    && ItemStack.isSameItemSameComponents(representative, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Unique
    private static ItemStack custominventory$extractVariant(
            List<ItemStack> slots, ItemCost cost, ItemStack representative, int amount) {
        int remaining = amount;
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty() || !cost.test(stack)
                    || !ItemStack.isSameItemSameComponents(representative, stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            if (stack.isEmpty()) slots.set(i, ItemStack.EMPTY);
        }
        return remaining == 0 ? representative.copyWithCount(amount) : null;
    }
}
