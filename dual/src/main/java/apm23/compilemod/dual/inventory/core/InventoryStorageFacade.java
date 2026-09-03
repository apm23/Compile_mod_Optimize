package com.anjas.custominventory;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Golden API surface retained for the proven custom-inventory implementation.
 * The implementation remains physically grouped under inventory/storage for maintainability.
 */
public final class InventoryStorage {
    public static final int PAGE_COUNT = apm23.compilemod.dual.inventory.storage.InventoryStorage.PAGE_COUNT;
    public static final int PAGE_SIZE = apm23.compilemod.dual.inventory.storage.InventoryStorage.PAGE_SIZE;
    public static final int MAIN_START = apm23.compilemod.dual.inventory.storage.InventoryStorage.MAIN_START;
    public static AttachmentType<List<ItemStack>> ALT_HOTBAR;

    private InventoryStorage() {}

    public static void register() {
        apm23.compilemod.dual.inventory.storage.InventoryStorage.register();
        ALT_HOTBAR = apm23.compilemod.dual.inventory.storage.InventoryStorage.ALT_HOTBAR;
    }
    public static void migrateLegacy(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.migrateLegacy(p); }
    public static int active(ServerPlayer p) { return apm23.compilemod.dual.inventory.storage.InventoryStorage.active(p); }
    public static boolean isBrowsing(ServerPlayer p) { return apm23.compilemod.dual.inventory.storage.InventoryStorage.isBrowsing(p); }
    public static void setBrowsing(ServerPlayer p, boolean v) { apm23.compilemod.dual.inventory.storage.InventoryStorage.setBrowsing(p, v); }
    public static List<ItemStack> read(ServerPlayer p, int page) { return apm23.compilemod.dual.inventory.storage.InventoryStorage.read(p, page); }
    public static void write(ServerPlayer p, int page, List<ItemStack> stacks) { apm23.compilemod.dual.inventory.storage.InventoryStorage.write(p, page, stacks); }
    public static List<ItemStack> readAltHotbar(ServerPlayer p) { return apm23.compilemod.dual.inventory.storage.InventoryStorage.readAltHotbar(p); }
    public static void writeAltHotbar(ServerPlayer p, List<ItemStack> stacks) { apm23.compilemod.dual.inventory.storage.InventoryStorage.writeAltHotbar(p, stacks); }
    public static void snapshotLive(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.snapshotLive(p); }
    public static List<ItemStack> liveCopy(ServerPlayer p) { return apm23.compilemod.dual.inventory.storage.InventoryStorage.liveCopy(p); }
    public static void switchPage(ServerPlayer p, int page) { apm23.compilemod.dual.inventory.storage.InventoryStorage.switchPage(p, page); }
    public static void cycle(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.cycle(p); }
    public static void routeOverflow(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.routeOverflow(p); }
    public static boolean hasEmptySlot(List<ItemStack> page) { return apm23.compilemod.dual.inventory.storage.InventoryStorage.hasEmptySlot(page); }
    public static void loadLive(ServerPlayer p, List<ItemStack> page) { apm23.compilemod.dual.inventory.storage.InventoryStorage.loadLive(p, page); }
    public static void copyState(ServerPlayer a, ServerPlayer b) { apm23.compilemod.dual.inventory.storage.InventoryStorage.copyState(a, b); }
    public static void dropHiddenOnDeath(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.dropHiddenOnDeath(p); }
    public static void clearStoredPages(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.clearStoredPages(p); }
    public static void sync(ServerPlayer p) { apm23.compilemod.dual.inventory.storage.InventoryStorage.sync(p); }
}
