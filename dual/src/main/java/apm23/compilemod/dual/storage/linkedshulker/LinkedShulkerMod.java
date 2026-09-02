package com.anjas.linkedshulker;

import net.fabricmc.api.ModInitializer;

public final class LinkedShulkerMod implements ModInitializer {
    public static final String MOD_ID = "linkedshulker";

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntities.initialize();
    }
}
