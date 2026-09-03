package apm23.compilemod.dual.inventory.storage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Compatibility facade only. The single authoritative implementation is the proven
 * com.anjas.custominventory.InventoryStorage class. Keeping this facade prevents responsibility-
 * grouped adapters from creating a second set of attachment state.
 */
public final class InventoryStorage {
    public static final int PAGE_COUNT = com.anjas.custominventory.InventoryStorage.PAGE_COUNT;
    public static final int PAGE_SIZE = com.anjas.custominventory.InventoryStorage.PAGE_SIZE;
    public static final int MAIN_START = com.anjas.custominventory.InventoryStorage.MAIN_START;

    private InventoryStorage() {}

    public static int active(ServerPlayer player) { return com.anjas.custominventory.InventoryStorage.active(player); }
    public static List<ItemStack> read(ServerPlayer player, int page) { return com.anjas.custominventory.InventoryStorage.read(player, page); }
    public static void write(ServerPlayer player, int page, List<ItemStack> stacks) { com.anjas.custominventory.InventoryStorage.write(player, page, stacks); }
    public static void snapshotLive(ServerPlayer player) { com.anjas.custominventory.InventoryStorage.snapshotLive(player); }
    public static void loadLive(ServerPlayer player, List<ItemStack> page) { com.anjas.custominventory.InventoryStorage.loadLive(player, page); }
    public static void sync(ServerPlayer player) { com.anjas.custominventory.InventoryStorage.sync(player); }
    public static List<ItemStack> readAltHotbar(ServerPlayer player) { return com.anjas.custominventory.InventoryStorage.readAltHotbar(player); }
    public static void writeAltHotbar(ServerPlayer player, List<ItemStack> stacks) { com.anjas.custominventory.InventoryStorage.writeAltHotbar(player, stacks); }
}
