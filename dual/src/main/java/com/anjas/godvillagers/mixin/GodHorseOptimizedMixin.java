package com.anjas.godvillagers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonHorse.class)
public abstract class GodHorseOptimizedMixin {
    @Unique private static final String GOD_TAG = "godvillagers_god_horse";
    @Unique private static final String INIT_TAG = "godvillagers_god_horse_initialized_v78";
    @Unique private boolean godvillagers$fluidLocked;
    @Unique private boolean godvillagers$boostedOnFluid;

    @Unique private SkeletonHorse godvillagers$self() { return (SkeletonHorse) (Object) this; }
    @Unique private boolean godvillagers$isGodHorse(SkeletonHorse horse) { return horse.entityTags().contains(GOD_TAG); }
    @Unique private static boolean godvillagers$isSupportedFluid(FluidState state) { return state.is(FluidTags.WATER) || state.is(FluidTags.LAVA); }

    @Unique private static void godvillagers$setBase(AttributeInstance attribute, double value) {
        if (attribute != null && attribute.getBaseValue() != value) attribute.setBaseValue(value);
    }

    @Unique private void godvillagers$initializeOnce(SkeletonHorse horse) {
        if (horse.entityTags().contains(INIT_TAG)) return;
        godvillagers$setBase(horse.getAttribute(Attributes.MAX_HEALTH), 80.0D);
        godvillagers$setBase(horse.getAttribute(Attributes.MOVEMENT_SPEED), 0.45D);
        godvillagers$setBase(horse.getAttribute(Attributes.JUMP_STRENGTH), 1.8D);
        godvillagers$setBase(horse.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), 0.0D);
        godvillagers$setBase(horse.getAttribute(Attributes.SAFE_FALL_DISTANCE), 1024.0D);
        godvillagers$setBase(horse.getAttribute(Attributes.MOVEMENT_EFFICIENCY), 1.0D);
        godvillagers$setBase(horse.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY), 1.0D);
        godvillagers$setBase(horse.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), 1.0D);
        if (horse.getHealth() < 80.0F) horse.setHealth(80.0F);
        horse.addTag(INIT_TAG);
    }

    @Unique private double godvillagers$fluidSurfaceY(SkeletonHorse horse) {
        Level level = horse.level();
        int x = (int)Math.floor(horse.getX());
        int z = (int)Math.floor(horse.getZ());
        int y = (int)Math.floor(horse.getY());
        int fluidY = Integer.MIN_VALUE;
        for (int dy = 1; dy >= -2; dy--) {
            int py = y + dy;
            if (godvillagers$isSupportedFluid(level.getFluidState(new BlockPos(x, py, z)))) { fluidY = py; break; }
        }
        if (fluidY == Integer.MIN_VALUE && godvillagers$fluidLocked) {
            int py = y - 3;
            if (godvillagers$isSupportedFluid(level.getFluidState(new BlockPos(x, py, z)))) fluidY = py;
        }
        if (fluidY == Integer.MIN_VALUE) return Double.NaN;
        int top = fluidY;
        for (int i = 0; i < 32; i++, top++) {
            if (!godvillagers$isSupportedFluid(level.getFluidState(new BlockPos(x, top, z)))) return (double)top;
        }
        return Double.NaN;
    }

    @Unique private void godvillagers$setFluidSpeed(SkeletonHorse horse, boolean onFluid) {
        if (godvillagers$boostedOnFluid == onFluid) return;
        godvillagers$boostedOnFluid = onFluid;
        godvillagers$setBase(horse.getAttribute(Attributes.MOVEMENT_SPEED), onFluid ? 1.80D : 0.45D);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void godvillagers$optimizedGodHorseTick(CallbackInfo ci) {
        SkeletonHorse horse = godvillagers$self();
        if (!godvillagers$isGodHorse(horse)) return;
        godvillagers$initializeOnce(horse);
        horse.clearFire();
        double surface = godvillagers$fluidSurfaceY(horse);
        if (Double.isNaN(surface)) {
            if (godvillagers$fluidLocked) { godvillagers$fluidLocked = false; horse.setNoGravity(false); }
            godvillagers$setFluidSpeed(horse, false);
            return;
        }
        godvillagers$fluidLocked = true;
        godvillagers$setFluidSpeed(horse, horse.isVehicle());
        Vec3 velocity = horse.getDeltaMovement();
        horse.setPos(horse.getX(), surface, horse.getZ());
        horse.setDeltaMovement(velocity.x, 0.0D, velocity.z);
        horse.setNoGravity(true);
        horse.setOnGround(true);
    }
}
