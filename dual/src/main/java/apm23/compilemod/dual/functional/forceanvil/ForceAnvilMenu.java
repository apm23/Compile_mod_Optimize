package com.anjas.forceanvil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class ForceAnvilMenu extends AnvilMenu {
    public ForceAnvilMenu(int syncId, Inventory inventory, ContainerLevelAccess access) {
        super(syncId, inventory, access);
    }
}
