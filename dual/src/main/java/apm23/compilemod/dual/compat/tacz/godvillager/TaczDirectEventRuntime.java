package com.anjas.godvillagers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Reflection-only bridge to TACZ Fabric events; no hard TACZ dependency is linked. */
public final class TaczDirectEventRuntime {
    private static final double MAGNET_RADIUS = 3.0D;
    private static final int FRESH_ENTITY_TICKS = 8;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private TaczDirectEventRuntime() {}

    public static void registerEvents() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        try {
            register("com.tacz.guns.api.event.common.EntityHurtByGunEvent", "POST",
                    "com.tacz.guns.api.event.common.EntityHurtByGunEvent$PostCallBack", TaczDirectEventRuntime::onGunHurtPost);
            register("com.tacz.guns.api.event.common.EntityKillByGunEvent", "CALLBACK",
                    "com.tacz.guns.api.event.common.EntityKillByGunEvent$Callback", TaczDirectEventRuntime::onGunKill);
            System.out.println("[God Villagers] TACZ direct hit/kill event bridge registered");
        } catch (ReflectiveOperationException | RuntimeException e) {
            REGISTERED.set(false);
            System.err.println("[God Villagers] Failed to register TACZ direct event bridge: " + e);
        }
    }

    private static void register(String eventClassName, String fieldName, String callbackClassName, Consumer<Object> handler)
            throws ReflectiveOperationException {
        ClassLoader loader = TaczDirectEventRuntime.class.getClassLoader();
        Class<?> eventClass = Class.forName(eventClassName, true, loader);
        Class<?> callbackClass = Class.forName(callbackClassName, true, loader);
        Field field = eventClass.getField(fieldName);
        Object fabricEvent = field.get(null);
        Object callback = Proxy.newProxyInstance(loader, new Class<?>[]{callbackClass}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "GodVillagersTaczCallback[" + callbackClass.getSimpleName() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> null;
                };
            }
            if (args != null && args.length > 0) handler.accept(args[0]);
            return null;
        });
        Class<?> fabricEventInterface = Class.forName("net.fabricmc.fabric.api.event.Event", true, loader);
        Method register = fabricEventInterface.getMethod("register", Object.class);
        register.invoke(fabricEvent, callback);
    }

    public static void onGunHurtPost(Object event) {
        ServerPlayer shooter = serverPlayer(invoke(event, "getAttacker"));
        if (shooter == null) return;
        ItemStack gun = shooter.getMainHandItem();
        int level = TaczEnchantRuntime.enchantLevel(gun, TaczEnchantRuntime.LIFE_STEAL_ID);
        if (level <= 0) return;
        float damage = number(invoke(event, "getAmount"));
        if (damage <= 0.0F) {
            damage = number(invoke(event, "getBaseAmount"));
            float headshot = number(invoke(event, "getHeadshotMultiplier"));
            if (headshot > 0.0F) damage *= headshot;
        }
        if (damage > 0.0F) TaczEnchantRuntime.applyLifeSteal(shooter, damage, level);
    }

    public static void onGunKill(Object event) {
        ServerPlayer shooter = serverPlayer(invoke(event, "getAttacker"));
        Object killed = invoke(event, "getKilledEntity");
        if (shooter == null || !(killed instanceof LivingEntity victim)) return;

        ItemStack gun = shooter.getMainHandItem();
        int lifeSteal = TaczEnchantRuntime.enchantLevel(gun, TaczEnchantRuntime.LIFE_STEAL_ID);
        if (lifeSteal > 0) {
            float damage = number(invoke(event, "getBaseDamage"));
            float headshot = number(invoke(event, "getHeadshotMultiplier"));
            if (headshot > 0.0F) damage *= headshot;
            if (damage > 0.0F) TaczEnchantRuntime.applyLifeSteal(shooter, damage, lifeSteal);
        }

        if (TaczEnchantRuntime.sharedBossReward(victim)) return;
        if (TaczEnchantRuntime.enchantLevel(gun, TaczEnchantRuntime.MAGNET_ID) <= 0) return;
        if (!(victim.level() instanceof ServerLevel level) || shooter.level() != level) return;

        final double x = victim.getX(), y = victim.getY(), z = victim.getZ();
        level.getServer().execute(() -> deliverFreshRewards(level, shooter, x, y, z));
    }

    private static void deliverFreshRewards(ServerLevel level, ServerPlayer shooter, double x, double y, double z) {
        if (shooter.level() != level) return;
        AABB box = new AABB(x - MAGNET_RADIUS, y - MAGNET_RADIUS, z - MAGNET_RADIUS,
                x + MAGNET_RADIUS, y + MAGNET_RADIUS, z + MAGNET_RADIUS);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.tickCount > FRESH_ENTITY_TICKS) continue;
            ItemStack remainder = entity.getItem().copy();
            if (remainder.isEmpty()) { entity.discard(); continue; }
            shooter.getInventory().add(remainder);
            if (remainder.isEmpty()) entity.discard();
            else {
                entity.setItem(remainder);
                entity.teleportTo(shooter.getX(), shooter.getY(), shooter.getZ());
                entity.setPickUpDelay(0);
            }
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            if (orb.tickCount > FRESH_ENTITY_TICKS) continue;
            shooter.giveExperiencePoints(orb.getValue());
            orb.discard();
        }
    }

    private static ServerPlayer serverPlayer(Object value) {
        return value instanceof ServerPlayer player ? player : null;
    }

    private static float number(Object value) {
        return value instanceof Number n ? n.floatValue() : 0.0F;
    }

    private static Object invoke(Object target, String name) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
