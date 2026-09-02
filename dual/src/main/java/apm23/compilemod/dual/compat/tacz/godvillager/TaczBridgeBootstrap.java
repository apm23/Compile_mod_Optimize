package com.anjas.godvillagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Optional alpha.85 TACZ bridge bootstrap; TACZ itself remains external. */
public final class TaczBridgeBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!FabricLoader.getInstance().isModLoaded("tacz")) return;
        TaczDirectEventRuntime.registerEvents();
    }
}
