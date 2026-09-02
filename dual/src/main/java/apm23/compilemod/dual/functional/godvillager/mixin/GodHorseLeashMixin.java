package com.anjas.godvillagers.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class GodHorseLeashMixin {
    private static final String GOD_HORSE_TAG = "godvillagers_god_horse";

    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void godvillagers$allowGodHorseLeash(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self instanceof SkeletonHorse horse && horse.entityTags().contains(GOD_HORSE_TAG)) {
            cir.setReturnValue(true);
        }
    }
}
