package com.anjas.forceanvil.mixin;

import com.anjas.forceanvil.ForceAnvilMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public abstract class VelocityHopperSpeedMixin {
    @Shadow private int cooldownTime;

    @Inject(method = "pushItemsTick", at = @At("HEAD"))
    private static void forceanvil$velocityHopperOneTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper, CallbackInfo ci) {
        if (state.is(ForceAnvilMod.VELOCITY_HOPPER) && ((VelocityHopperSpeedMixin)(Object)hopper).cooldownTime > 1) {
            ((VelocityHopperSpeedMixin)(Object)hopper).cooldownTime = 1;
        }
    }
}
