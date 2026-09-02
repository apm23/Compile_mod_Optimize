package apm23.compilemod.server.xp.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true)
    private void compileMod$repairAllMendingItems(ServerPlayer player, int amount, CallbackInfoReturnable<Integer> cir) {
        if (amount <= 0) {
            cir.setReturnValue(amount);
            return;
        }

        List<ItemStack> damagedMending = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.isDamaged()
                    && EnchantmentHelper.has(stack, EnchantmentEffectComponents.REPAIR_WITH_XP)) {
                damagedMending.add(stack);
            }
        }

        if (damagedMending.isEmpty()) {
            cir.setReturnValue(amount);
            return;
        }

        int remainingXp = amount;
        int cursor = 0;
        while (remainingXp > 0 && !damagedMending.isEmpty()) {
            if (cursor >= damagedMending.size()) cursor = 0;
            ItemStack stack = damagedMending.get(cursor);
            if (!stack.isDamaged()) {
                damagedMending.remove(cursor);
                continue;
            }

            int repair = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), stack, 1);
            repair = Math.min(repair, stack.getDamageValue());
            if (repair <= 0) {
                damagedMending.remove(cursor);
                continue;
            }

            stack.setDamageValue(stack.getDamageValue() - repair);
            remainingXp--;
            cursor++;
        }

        cir.setReturnValue(remainingXp);
    }
}
