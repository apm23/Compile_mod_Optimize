package com.anjas.godvillagers;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Proven Stormcall combat behavior: a Stormcall main-hand hit summons lightning at the victim. */
public final class StormcallRuntime {
    private StormcallRuntime() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            Entity attacker = source.getEntity();
            if (!(attacker instanceof Player player)) return true;
            ItemStack weapon = player.getMainHandItem();
            if (weapon.isEmpty() || !StormcallCompat.hasStormcall(weapon)) return true;
            if (!(victim.level() instanceof ServerLevel level)) return true;
            String command = "summon minecraft:lightning_bolt " + victim.getX() + " " + victim.getY() + " " + victim.getZ();
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withSuppressedOutput(), command);
            return true;
        });
    }
}
