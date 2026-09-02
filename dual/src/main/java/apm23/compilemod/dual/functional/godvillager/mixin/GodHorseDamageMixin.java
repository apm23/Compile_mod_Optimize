package com.anjas.godvillagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GodHorseDamageMixin {
    private static final String GOD_TAG = "godvillagers_god_horse";

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void godvillagers$protectGodHorse(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof SkeletonHorse horse) || !horse.entityTags().contains(GOD_TAG)) return;

        if (source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.CAMPFIRE)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.SULFUR_CUBE_HOT)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.BAD_RESPAWN_POINT)
                || source.is(DamageTypes.FIREWORKS)
                || source.is(DamageTypes.FALL)) {
            cir.setReturnValue(true);
        }
    }
}
