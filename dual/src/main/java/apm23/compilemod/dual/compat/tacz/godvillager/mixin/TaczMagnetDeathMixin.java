package com.anjas.godvillagers.mixin;

import com.anjas.godvillagers.TaczEnchantRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Event-only Magnet capture anchored at the killed mob, never at shooter range. */
@Mixin(LivingEntity.class)
public abstract class TaczMagnetDeathMixin {
    private static final int MAGNET_CAPTURE_TICKS = 4;
    private static final double REWARD_CAPTURE_RADIUS = 3.0D;

    private UUID godvillagers$magnetShooter;
    private Set<Integer> godvillagers$itemsBefore;
    private Set<Integer> godvillagers$xpBefore;
    private int godvillagers$magnetTicksLeft;
    private AABB godvillagers$rewardBox;

    @Inject(method = "die", at = @At("HEAD"), require = 0)
    private void godvillagers$resolveMagnetOwner(DamageSource source, CallbackInfo ci) {
        godvillagers$clearCapture();
        LivingEntity victim = (LivingEntity)(Object)this;
        if (!(victim.level() instanceof ServerLevel level) || TaczEnchantRuntime.sharedBossReward(victim)) return;
        ServerPlayer shooter = TaczEnchantRuntime.exactShooter(source);
        if (shooter == null || shooter.level() != level) return;
        ItemStack gun = shooter.getMainHandItem();
        if (!TaczEnchantRuntime.looksLikeTaczGun(gun) || TaczEnchantRuntime.enchantLevel(gun, TaczEnchantRuntime.MAGNET_ID) <= 0) return;

        godvillagers$magnetShooter = shooter.getUUID();
        godvillagers$magnetTicksLeft = MAGNET_CAPTURE_TICKS;
        double x = victim.getX(), y = victim.getY(), z = victim.getZ();
        godvillagers$rewardBox = new AABB(x - REWARD_CAPTURE_RADIUS, y - REWARD_CAPTURE_RADIUS, z - REWARD_CAPTURE_RADIUS,
                x + REWARD_CAPTURE_RADIUS, y + REWARD_CAPTURE_RADIUS, z + REWARD_CAPTURE_RADIUS);
        godvillagers$itemsBefore = new HashSet<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, godvillagers$rewardBox)) godvillagers$itemsBefore.add(entity.getId());
        godvillagers$xpBefore = new HashSet<>();
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, godvillagers$rewardBox)) godvillagers$xpBefore.add(orb.getId());
    }

    @Inject(method = "die", at = @At("RETURN"), require = 0)
    private void godvillagers$deliverImmediateMagnetReward(DamageSource source, CallbackInfo ci) {
        godvillagers$deliverNewRewards();
    }

    @Inject(method = "tickDeath", at = @At("HEAD"), require = 0)
    private void godvillagers$deliverDelayedMagnetReward(CallbackInfo ci) {
        if (godvillagers$magnetShooter == null || godvillagers$magnetTicksLeft <= 0) return;
        godvillagers$deliverNewRewards();
        if (--godvillagers$magnetTicksLeft <= 0) godvillagers$clearCapture();
    }

    private void godvillagers$deliverNewRewards() {
        LivingEntity victim = (LivingEntity)(Object)this;
        UUID shooterId = godvillagers$magnetShooter;
        AABB box = godvillagers$rewardBox;
        if (shooterId == null || box == null || !(victim.level() instanceof ServerLevel level)) return;
        ServerPlayer shooter = level.getServer().getPlayerList().getPlayer(shooterId);
        if (shooter == null || shooter.level() != level) {
            godvillagers$clearCapture();
            return;
        }

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (godvillagers$itemsBefore == null || godvillagers$itemsBefore.contains(entity.getId())) continue;
            godvillagers$itemsBefore.add(entity.getId());
            ItemStack reward = entity.getItem().copy();
            if (reward.isEmpty()) {
                entity.discard();
                continue;
            }
            shooter.getInventory().add(reward);
            if (reward.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(reward);
                entity.teleportTo(shooter.getX(), shooter.getY(), shooter.getZ());
                entity.setPickUpDelay(0);
            }
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            if (godvillagers$xpBefore == null || godvillagers$xpBefore.contains(orb.getId())) continue;
            godvillagers$xpBefore.add(orb.getId());
            shooter.giveExperiencePoints(orb.getValue());
            orb.discard();
        }
    }

    private void godvillagers$clearCapture() {
        godvillagers$magnetShooter = null;
        godvillagers$itemsBefore = null;
        godvillagers$xpBefore = null;
        godvillagers$magnetTicksLeft = 0;
        godvillagers$rewardBox = null;
    }
}
