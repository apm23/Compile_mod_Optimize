package com.anjas.fuelrecipe.mixin;

import com.anjas.fuelrecipe.ModItems;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Shadow private int cookingTimer;
    @Shadow private int cookingTotalTime;
    @Unique private boolean fuelrecipe$turboActive;

    @Inject(method = "getBurnDuration", at = @At("HEAD"))
    private void fuelrecipe$rememberFuel(FuelValues values, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        fuelrecipe$turboActive = stack.is(ModItems.COPPERCHARGED_BAMBOO)
            || stack.is(ModItems.COPPERCHARGED_LAVA_BUCKET);
    }

    @Redirect(method = "serverTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingTimer:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private static int fuelrecipe$accelerateCooking(AbstractFurnaceBlockEntity furnace) {
        AbstractFurnaceBlockEntityMixin self = (AbstractFurnaceBlockEntityMixin) (Object) furnace;
        int current = self.cookingTimer;
        if (!self.fuelrecipe$turboActive || self.cookingTotalTime <= 0) return current;
        return Math.min(current + 39, self.cookingTotalTime - 1);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void fuelrecipe$saveTurboState(ValueOutput output, CallbackInfo ci) {
        output.store("fuelrecipe_turbo", Codec.BOOL, fuelrecipe$turboActive);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void fuelrecipe$loadTurboState(ValueInput input, CallbackInfo ci) {
        fuelrecipe$turboActive = input.read("fuelrecipe_turbo", Codec.BOOL).orElse(false);
    }
}
