package com.anjas.forceanvil.mixin;

import com.anjas.forceanvil.ForceAnvilMenu;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class ForceAnvilMenuMixin {
    @Shadow private int repairItemCountCost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void forceanvil$createForcedEnchantResult(CallbackInfo ci) {
        if (!((Object) this instanceof ForceAnvilMenu)) return;

        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack base = menu.getSlot(0).getItem();
        ItemStack addition = menu.getSlot(1).getItem();
        if (base.isEmpty() || addition.isEmpty()) return;

        ItemEnchantments incoming = addition.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (incoming.isEmpty()) incoming = addition.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (incoming.isEmpty()) incoming = EnchantmentHelper.getEnchantmentsForCrafting(addition);
        if (incoming.isEmpty()) return;

        ItemStack result = base.copy();
        ItemEnchantments finalIncoming = incoming;
        EnchantmentHelper.updateEnchantments(result, mutable -> {
            for (Holder<Enchantment> enchantment : finalIncoming.keySet()) {
                int incomingLevel = finalIncoming.getLevel(enchantment);
                int currentLevel = mutable.getLevel(enchantment);
                if (incomingLevel > currentLevel) mutable.set(enchantment, incomingLevel);
            }
        });

        menu.getSlot(2).set(result);
        menu.setData(0, 1);
        repairItemCountCost = 1;
        menu.broadcastChanges();
        ci.cancel();
    }
}
