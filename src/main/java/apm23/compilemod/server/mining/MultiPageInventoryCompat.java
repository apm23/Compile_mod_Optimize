package com.apm23.custompickaxe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class MultiPageInventoryCompat {
    private static final String STORAGE_CLASS = "com.anjas.custominventory.InventoryStorage";
    private static volatile boolean resolved;
    private static volatile Access access;

    private MultiPageInventoryCompat() {}

    static void insertOverflow(ServerPlayer player, ItemStack remaining) {
        if (remaining.isEmpty()) return;
        Access api = resolve();
        if (api == null) return;
        try {
            api.snapshotLive.invoke(null, player);
            int activePage = (Integer) api.active.invoke(null, player);
            for (int page = 0; page < api.pageCount && !remaining.isEmpty(); page++) {
                if (page == activePage) continue;
                List<ItemStack> slots = mutableCopy(api.read.invoke(null, player, page), api.pageSize);
                insertInto(slots, remaining);
                api.write.invoke(null, player, page, slots);
            }
            if (!remaining.isEmpty()) {
                List<ItemStack> hotbar = mutableCopy(api.readAltHotbar.invoke(null, player), 9);
                insertInto(hotbar, remaining);
                api.writeAltHotbar.invoke(null, player, hotbar);
            }
            api.sync.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException error) {
            CustomPickaxeMod.LOGGER.warn("Could not route mining overflow into multi-page inventory; falling back to world drop", error);
        }
    }

    static void insertInto(List<ItemStack> slots, ItemStack remaining) {
        insertGeneric(slots, remaining, new SlotOps<>() {
            public boolean empty(ItemStack s) { return s == null || s.isEmpty(); }
            public boolean same(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }
            public int count(ItemStack s) { return s.getCount(); }
            public int max(ItemStack s) { return s.getMaxStackSize(); }
            public void grow(ItemStack s, int n) { s.grow(n); }
            public void shrink(ItemStack s, int n) { s.shrink(n); }
            public ItemStack copyWithCount(ItemStack s, int n) { ItemStack c = s.copy(); c.setCount(n); return c; }
        });
    }

    static <T> void insertGeneric(List<T> slots, T remaining, SlotOps<T> ops) {
        if (ops.empty(remaining)) return;
        for (T existing : slots) {
            if (ops.empty(remaining)) return;
            if (ops.empty(existing) || !ops.same(existing, remaining)) continue;
            int room = ops.max(existing) - ops.count(existing);
            if (room <= 0) continue;
            int moved = Math.min(room, ops.count(remaining));
            ops.grow(existing, moved);
            ops.shrink(remaining, moved);
        }
        for (int i = 0; i < slots.size() && !ops.empty(remaining); i++) {
            if (!ops.empty(slots.get(i))) continue;
            int moved = Math.min(ops.count(remaining), ops.max(remaining));
            slots.set(i, ops.copyWithCount(remaining, moved));
            ops.shrink(remaining, moved);
        }
    }

    interface SlotOps<T> {
        boolean empty(T stack);
        boolean same(T a, T b);
        int count(T stack);
        int max(T stack);
        void grow(T stack, int amount);
        void shrink(T stack, int amount);
        T copyWithCount(T stack, int amount);
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> mutableCopy(Object raw, int size) {
        List<ItemStack> source = raw instanceof List<?> list ? (List<ItemStack>) list : List.of();
        ArrayList<ItemStack> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = i < source.size() && source.get(i) != null ? source.get(i) : ItemStack.EMPTY;
            out.add(stack.copy());
        }
        return out;
    }

    private static Access resolve() {
        if (resolved) return access;
        synchronized (MultiPageInventoryCompat.class) {
            if (resolved) return access;
            try {
                Class<?> type = Class.forName(STORAGE_CLASS);
                access = new Access(type.getField("PAGE_COUNT").getInt(null), type.getField("PAGE_SIZE").getInt(null),
                        type.getMethod("snapshotLive", ServerPlayer.class), type.getMethod("active", ServerPlayer.class),
                        type.getMethod("read", ServerPlayer.class, int.class), type.getMethod("write", ServerPlayer.class, int.class, List.class),
                        type.getMethod("readAltHotbar", ServerPlayer.class), type.getMethod("writeAltHotbar", ServerPlayer.class, List.class),
                        type.getMethod("sync", ServerPlayer.class));
                CustomPickaxeMod.LOGGER.info("Multi-page inventory compatibility enabled");
            } catch (ClassNotFoundException ignored) {
                access = null;
            } catch (ReflectiveOperationException error) {
                CustomPickaxeMod.LOGGER.warn("Multi-page inventory detected but compatibility API could not be resolved", error);
                access = null;
            }
            resolved = true;
            return access;
        }
    }

    private record Access(int pageCount, int pageSize, Method snapshotLive, Method active, Method read, Method write,
                          Method readAltHotbar, Method writeAltHotbar, Method sync) {}
}
