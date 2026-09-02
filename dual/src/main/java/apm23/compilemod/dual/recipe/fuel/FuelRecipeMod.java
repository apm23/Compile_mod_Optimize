package com.anjas.fuelrecipe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.FuelValueEvents;

public final class FuelRecipeMod implements ModInitializer {
    public static final String MOD_ID = "fuelrecipe";
    public static final int COPPERCHARGED_BAMBOO_BURN_TICKS = 800;
    public static final int COPPERCHARGED_LAVA_BURN_TICKS = 20_000;

    @Override
    public void onInitialize() {
        ModItems.initialize();
        FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.COPPERCHARGED_BAMBOO, COPPERCHARGED_BAMBOO_BURN_TICKS);
            builder.add(ModItems.COPPERCHARGED_LAVA_BUCKET, COPPERCHARGED_LAVA_BURN_TICKS);
        });
    }
}
