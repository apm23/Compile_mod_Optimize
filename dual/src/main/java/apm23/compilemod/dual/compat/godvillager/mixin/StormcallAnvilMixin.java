package com.anjas.godvillagers.mixin;

import com.anjas.godvillagers.StormcallCompat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class StormcallAnvilMixin {
    @Shadow private int repairItemCountCost;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void godvillagers$prepareStormcallResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack base = menu.getSlot(0).getItem();
        ItemStack book = menu.getSlot(1).getItem();
        if (base.isEmpty() || book.isEmpty()) return;
        if (!StormcallCompat.isStormcallBook(book) || StormcallCompat.hasStormcall(base)) return;

        ItemStack result = base.copy();
        StormcallCompat.applyStormcall(result);
        menu.getSlot(2).set(result);
        menu.setData(0, 1);
        repairItemCountCost = 1;
        menu.broadcastChanges();
    }
}
