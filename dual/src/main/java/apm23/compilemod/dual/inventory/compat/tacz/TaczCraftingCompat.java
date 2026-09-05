package com.anjas.custominventory;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Optional TACZ gun-smith-table compatibility for the paged inventory. */
public final class TaczCraftingCompat {
    private static final String TACZ_MOD_ID = "tacz";
    private static final int[][] SCAN_ORDERS = buildScanOrders();
    private static final ConcurrentHashMap<Class<?>, Method> GET_RECIPE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, RecipeAccess> RECIPE_ACCESS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, InputAccess> INPUT_ACCESS = new ConcurrentHashMap<>();
    private static volatile RefreshAccess refreshAccess;
    private static volatile boolean refreshResolved;

    private TaczCraftingCompat() {}

    public static boolean handleCraft(Object menu, Identifier recipeId, Player player) {
        if (!FabricLoader.getInstance().isModLoaded(TACZ_MOD_ID) || !(player instanceof ServerPlayer serverPlayer)) return false;
        try {
            Method getRecipe = GET_RECIPE.computeIfAbsent(menu.getClass(), TaczCraftingCompat::resolveGetRecipe);
            Object recipe = getRecipe.invoke(menu, recipeId);
            if (recipe == null) return true;

            RecipeAccess recipeAccess = RECIPE_ACCESS.computeIfAbsent(recipe.getClass(), TaczCraftingCompat::resolveRecipeAccess);
            @SuppressWarnings("unchecked") List<Object> inputs = (List<Object>) recipeAccess.getInputs.invoke(recipe);
            ItemStack output = ((ItemStack) recipeAccess.getOutput.invoke(recipe)).copy();
            if (output.isEmpty()) return true;

            PagedView view = new PagedView(serverPlayer);
            if (!serverPlayer.isCreative() && !reserveAndConsume(view, inputs)) return true;

            ItemStack leftover = insert(view, output);
            view.commit();
            InventoryStorage.sync(serverPlayer);
            CustomHotbarInventory.sendHiddenRecipeState(serverPlayer);

            if (!leftover.isEmpty()) {
                ItemEntity entity = new ItemEntity(serverPlayer.level(), serverPlayer.getX(), serverPlayer.getY() + 0.5, serverPlayer.getZ(), leftover);
                entity.setPickUpDelay(0);
                serverPlayer.level().addFreshEntity(entity);
            }

            serverPlayer.inventoryMenu.broadcastFullState();
            sendCraftRefresh(menu, serverPlayer);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            CustomHotbarInventory.LOGGER.warn("TACZ workbench compatibility failed; falling back to TACZ's native crafting path", e);
            return false;
        }
    }

    private static Method resolveGetRecipe(Class<?> type) {
        try {
            Method method = type.getDeclaredMethod("getRecipe", Identifier.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new ReflectionResolutionException(e);
        }
    }

    private static RecipeAccess resolveRecipeAccess(Class<?> type) {
        try {
            return new RecipeAccess(type.getMethod("getInputs"), type.getMethod("getOutput"));
        } catch (ReflectiveOperationException e) {
            throw new ReflectionResolutionException(e);
        }
    }

    private static InputAccess resolveInputAccess(Class<?> type) {
        try {
            return new InputAccess(type.getMethod("getIngredient"), type.getMethod("getCount"));
        } catch (ReflectiveOperationException e) {
            throw new ReflectionResolutionException(e);
        }
    }

    private static boolean reserveAndConsume(PagedView view, List<Object> inputs) throws ReflectiveOperationException {
        int[] reserved = new int[view.size()];
        for (Object input : inputs) {
            InputAccess access;
            try {
                access = INPUT_ACCESS.computeIfAbsent(input.getClass(), TaczCraftingCompat::resolveInputAccess);
            } catch (ReflectionResolutionException e) {
                throw new ReflectiveOperationException(e.getCause());
            }
            Ingredient ingredient = (Ingredient) access.getIngredient.invoke(input);
            int needed = Math.max(0, ((Number) access.getCount.invoke(input)).intValue());
            if (ingredient == null) return false;

            int remaining = needed;
            for (int slot : view.scanOrder()) {
                if (remaining <= 0) break;
                ItemStack stack = view.get(slot);
                if (stack.isEmpty() || !ingredient.test(stack)) continue;
                int available = Math.max(0, stack.getCount() - reserved[slot]);
                int take = Math.min(available, remaining);
                if (take > 0) { reserved[slot] += take; remaining -= take; }
            }
            if (remaining > 0) return false;
        }

        for (int slot = 0; slot < reserved.length; slot++) {
            int take = reserved[slot];
            if (take <= 0) continue;
            ItemStack stack = view.get(slot);
            stack.shrink(take);
            if (stack.isEmpty()) view.set(slot, ItemStack.EMPTY);
        }
        return true;
    }

    private static ItemStack insert(PagedView view, ItemStack input) {
        ItemStack remaining = input.copy();
        for (int slot : view.scanOrder()) {
            if (remaining.isEmpty()) break;
            ItemStack existing = view.get(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
        }
        for (int slot : view.scanOrder()) {
            if (remaining.isEmpty()) break;
            if (!view.get(slot).isEmpty()) continue;
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack placed = remaining.copy();
            placed.setCount(moved);
            view.set(slot, placed);
            remaining.shrink(moved);
        }
        return remaining;
    }

    private static void sendCraftRefresh(Object menu, ServerPlayer player) {
        try {
            RefreshAccess access = resolveRefreshAccess();
            if (access == null) return;
            int id = access.containerId.getInt(menu);
            Object message = access.messageCtor.newInstance(id);
            if (message instanceof CustomPacketPayload payload) access.send.invoke(null, payload, player);
        } catch (ReflectiveOperationException | LinkageError e) {
            CustomHotbarInventory.LOGGER.debug("Could not send TACZ workbench refresh packet", e);
        }
    }

    private static RefreshAccess resolveRefreshAccess() {
        if (refreshResolved) return refreshAccess;
        synchronized (TaczCraftingCompat.class) {
            if (refreshResolved) return refreshAccess;
            try {
                Field containerId = net.minecraft.world.inventory.AbstractContainerMenu.class.getField("containerId");
                Constructor<?> ctor = Class.forName("com.tacz.guns.network.message.ServerMessageCraft").getConstructor(int.class);
                Method send = Class.forName("com.tacz.guns.network.NetworkHandler").getMethod("sendToClientPlayer", CustomPacketPayload.class, ServerPlayer.class);
                refreshAccess = new RefreshAccess(containerId, ctor, send);
            } catch (ReflectiveOperationException | LinkageError e) {
                refreshAccess = null;
            }
            refreshResolved = true;
            return refreshAccess;
        }
    }

    private static int[][] buildScanOrders() {
        int total = 9 + InventoryStorage.PAGE_COUNT * InventoryStorage.PAGE_SIZE;
        int[][] orders = new int[InventoryStorage.PAGE_COUNT][total];
        for (int active = 0; active < InventoryStorage.PAGE_COUNT; active++) {
            int cursor = 0;
            for (int hotbar = 0; hotbar < 9; hotbar++) orders[active][cursor++] = hotbar;
            for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) orders[active][cursor++] = 9 + active * InventoryStorage.PAGE_SIZE + i;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                if (page == active) continue;
                for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) orders[active][cursor++] = 9 + page * InventoryStorage.PAGE_SIZE + i;
            }
        }
        return orders;
    }

    private static final class PagedView {
        private final ServerPlayer player;
        private final int activePage;
        private final List<List<ItemStack>> pages = new ArrayList<>(InventoryStorage.PAGE_COUNT);
        private final int[] scanOrder;

        private PagedView(ServerPlayer player) {
            this.player = player;
            InventoryStorage.snapshotLive(player);
            this.activePage = InventoryStorage.active(player);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) pages.add(page == activePage ? List.of() : new ArrayList<>(InventoryStorage.read(player, page)));
            this.scanOrder = SCAN_ORDERS[activePage];
        }

        private int size() { return 9 + InventoryStorage.PAGE_COUNT * InventoryStorage.PAGE_SIZE; }
        private int[] scanOrder() { return scanOrder; }

        private ItemStack get(int slot) {
            if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
            if (slot < 9) return player.getInventory().getItem(slot);
            int linear = slot - 9, page = linear / InventoryStorage.PAGE_SIZE, index = linear % InventoryStorage.PAGE_SIZE;
            if (page == activePage) return player.getInventory().getItem(InventoryStorage.MAIN_START + index);
            return pages.get(page).get(index);
        }

        private void set(int slot, ItemStack stack) {
            if (slot < 0 || slot >= size()) return;
            if (slot < 9) { player.getInventory().setItem(slot, stack); return; }
            int linear = slot - 9, page = linear / InventoryStorage.PAGE_SIZE, index = linear % InventoryStorage.PAGE_SIZE;
            if (page == activePage) player.getInventory().setItem(InventoryStorage.MAIN_START + index, stack);
            else pages.get(page).set(index, stack);
        }

        private void commit() {
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) if (page != activePage) InventoryStorage.write(player, page, pages.get(page));
            InventoryStorage.snapshotLive(player);
            player.getInventory().setChanged();
        }
    }

    private record RecipeAccess(Method getInputs, Method getOutput) {}
    private record InputAccess(Method getIngredient, Method getCount) {}
    private record RefreshAccess(Field containerId, Constructor<?> messageCtor, Method send) {}
    private static final class ReflectionResolutionException extends RuntimeException {
        private ReflectionResolutionException(Throwable cause) { super(cause); }
    }
}
