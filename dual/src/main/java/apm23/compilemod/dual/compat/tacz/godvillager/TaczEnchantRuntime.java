package com.anjas.godvillagers;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Locale;

/** Optional TACZ enchant runtime with strict per-shooter ownership. */
public final class TaczEnchantRuntime {
    public static final String MAGNET_ID = "godvillagers:magnet";
    public static final String LIFE_STEAL_ID = "godvillagers:life_steal";
    public static final int MAX_LIFE_STEAL_LEVEL = 3;
    private static final float[] LIFE_STEAL_RATIOS = {0.0F, 0.05F, 0.075F, 0.10F};
    public static final float ABSORPTION_RATIO = 0.50F;
    public static final float MAX_ABSORPTION_HEALTH = 4.0F;

    private TaczEnchantRuntime() {}

    public static float healingForDamage(float damage, int level) {
        if (damage <= 0.0F || level <= 0) return 0.0F;
        return damage * LIFE_STEAL_RATIOS[Math.min(level, MAX_LIFE_STEAL_LEVEL)];
    }

    public static float actualDamage(float healthBefore, float healthAfter) {
        if (!Float.isFinite(healthBefore) || !Float.isFinite(healthAfter)) return 0.0F;
        return Math.max(0.0F, healthBefore - Math.max(0.0F, healthAfter));
    }

    public static float absorptionForOverflow(float unusedHealing) {
        return Math.max(0.0F, unusedHealing) * ABSORPTION_RATIO;
    }

    public static void afterSuccessfulDamage(LivingEntity victim, DamageSource source, float actualDamage) {
        if (victim == null || source == null || actualDamage <= 0.0F) return;
        ServerPlayer shooter = exactShooter(source);
        if (shooter == null) return;
        ItemStack gun = shooter.getMainHandItem();
        if (!looksLikeTaczGun(gun)) return;
        int lifeSteal = enchantLevel(gun, LIFE_STEAL_ID);
        if (lifeSteal > 0) applyLifeSteal(shooter, actualDamage, lifeSteal);
    }

    public static void applyLifeSteal(ServerPlayer shooter, float bulletDamage, int level) {
        float budget = healingForDamage(bulletDamage, level);
        if (budget <= 0.0F) return;
        float missing = Math.max(0.0F, shooter.getMaxHealth() - shooter.getHealth());
        float directHeal = Math.min(missing, budget);
        if (directHeal > 0.0F) shooter.heal(directHeal);
        float overflow = budget - directHeal;
        if (overflow <= 0.0F) return;
        shooter.setAbsorptionAmount(Math.min(MAX_ABSORPTION_HEALTH, shooter.getAbsorptionAmount() + absorptionForOverflow(overflow)));
    }

    public static ServerPlayer exactShooter(DamageSource source) {
        Entity owner = source.getEntity();
        if (owner instanceof ServerPlayer player) return player;
        Entity direct = source.getDirectEntity();
        return direct instanceof ServerPlayer player ? player : null;
    }

    public static boolean looksLikeTaczGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        return id.startsWith("tacz:") || id.contains(":tacz_") || id.contains("tacz");
    }

    public static int enchantLevel(ItemStack stack, String wantedId) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            var key = holder.unwrapKey();
            if (key.isPresent() && key.get().identifier().toString().equals(wantedId)) return enchantments.getLevel(holder);
        }
        return 0;
    }

    public static boolean sharedBossReward(LivingEntity victim) {
        String id = victim.getType().toString().toLowerCase(Locale.ROOT);
        return id.contains("ender_dragon") || id.contains("wither");
    }
}
