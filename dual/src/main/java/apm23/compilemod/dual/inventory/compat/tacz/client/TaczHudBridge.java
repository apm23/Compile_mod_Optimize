package com.anjas.custominventory.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/** Client-only optional TACZ bridge for reserve-ammo HUD counting. */
public final class TaczHudBridge {
    private static final int HUD_MAX = 9999;
    private static volatile Access access;
    private static volatile boolean attempted;

    private static ItemStack cachedGunRef = ItemStack.EMPTY;
    private static long cachedRevision = Long.MIN_VALUE;
    private static int cachedHiddenAmmo;

    private TaczHudBridge() {}

    public static int countHiddenAmmo(ItemStack gun) {
        Access a = access();
        if (a == null || gun == null || gun.isEmpty()) return 0;

        long revision = HiddenRecipeContentsClient.revision();
        if (gun == cachedGunRef && revision == cachedRevision) return cachedHiddenAmmo;

        int total = 0;
        try {
            for (ItemStack stack : HiddenRecipeContentsClient.view()) {
                if (stack == null || stack.isEmpty()) continue;
                Object item = stack.getItem();
                if (a.ammoClass.isInstance(item)
                        && Boolean.TRUE.equals(a.ammoMatches.invoke(item, gun, stack))) {
                    total += stack.getCount();
                } else if (a.ammoBoxClass.isInstance(item)
                        && Boolean.TRUE.equals(a.boxMatches.invoke(item, gun, stack))) {
                    if (Boolean.TRUE.equals(a.boxCreative.invoke(item, stack))
                            || Boolean.TRUE.equals(a.boxAllCreative.invoke(item, stack))) {
                        return cache(gun, revision, HUD_MAX);
                    }
                    total += Math.max(0, ((Number) a.boxCount.invoke(item, stack)).intValue());
                }
                if (total >= HUD_MAX) return cache(gun, revision, HUD_MAX);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return cache(gun, revision, 0);
        }
        return cache(gun, revision, Math.min(total, HUD_MAX));
    }

    private static int cache(ItemStack gun, long revision, int value) {
        cachedGunRef = gun;
        cachedRevision = revision;
        cachedHiddenAmmo = value;
        return value;
    }

    private static Access access() {
        if (attempted) return access;
        synchronized (TaczHudBridge.class) {
            if (attempted) return access;
            attempted = true;
            if (!FabricLoader.getInstance().isModLoaded("tacz")) return null;
            try {
                Class<?> ammo = Class.forName("com.tacz.guns.api.item.IAmmo");
                Class<?> box = Class.forName("com.tacz.guns.api.item.IAmmoBox");
                access = new Access(
                        ammo,
                        box,
                        ammo.getMethod("isAmmoOfGun", ItemStack.class, ItemStack.class),
                        box.getMethod("isAmmoBoxOfGun", ItemStack.class, ItemStack.class),
                        box.getMethod("getAmmoCount", ItemStack.class),
                        box.getMethod("isCreative", ItemStack.class),
                        box.getMethod("isAllTypeCreative", ItemStack.class)
                );
            } catch (ReflectiveOperationException | LinkageError ignored) {
                access = null;
            }
            return access;
        }
    }

    private record Access(Class<?> ammoClass, Class<?> ammoBoxClass, Method ammoMatches,
                          Method boxMatches, Method boxCount, Method boxCreative,
                          Method boxAllCreative) {}
}
