package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GlobalPickaxeStateTest {
    @Test
    void normalPickaxeTurnsOffOnFourthClick() {
        UUID player = UUID.randomUUID();
        assertEquals(GlobalPickaxeState.LEVEL_1, GlobalPickaxeState.cycle(player, "diamond"));
        assertEquals(GlobalPickaxeState.LEVEL_2, GlobalPickaxeState.cycle(player, "diamond"));
        assertEquals(GlobalPickaxeState.LEVEL_3, GlobalPickaxeState.cycle(player, "diamond"));
        assertEquals(GlobalPickaxeState.OFF, GlobalPickaxeState.cycle(player, "diamond"));
    }

    @Test
    void emeraldAndDebrisReachLevelFourOnFourthClick() {
        UUID emeraldPlayer = UUID.randomUUID();
        assertEquals(GlobalPickaxeState.LEVEL_1, GlobalPickaxeState.cycle(emeraldPlayer, "emerald"));
        assertEquals(GlobalPickaxeState.LEVEL_2, GlobalPickaxeState.cycle(emeraldPlayer, "emerald"));
        assertEquals(GlobalPickaxeState.LEVEL_3, GlobalPickaxeState.cycle(emeraldPlayer, "emerald"));
        assertEquals(GlobalPickaxeState.LEVEL_4, GlobalPickaxeState.cycle(emeraldPlayer, "emerald"));
        assertEquals(GlobalPickaxeState.OFF, GlobalPickaxeState.cycle(emeraldPlayer, "emerald"));
        UUID debrisPlayer = UUID.randomUUID();
        assertEquals(GlobalPickaxeState.LEVEL_1, GlobalPickaxeState.cycle(debrisPlayer, "debris"));
        assertEquals(GlobalPickaxeState.LEVEL_2, GlobalPickaxeState.cycle(debrisPlayer, "debris"));
        assertEquals(GlobalPickaxeState.LEVEL_3, GlobalPickaxeState.cycle(debrisPlayer, "debris"));
        assertEquals(GlobalPickaxeState.LEVEL_4, GlobalPickaxeState.cycle(debrisPlayer, "debris"));
    }

    @Test
    void levelFourOnlyExtendsEmeraldAndDebris() {
        assertEquals(128, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_4, "emerald"));
        assertEquals(128, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_4, "debris"));
        assertEquals(64, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_4, "diamond"));
        assertEquals(64, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_4, "iron"));
        assertEquals(64, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_4, "amethyst"));
    }

    @Test
    void lowerLevelsRemainGlobalForEveryType() {
        assertEquals(8, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_1, "diamond"));
        assertEquals(16, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_2, "emerald"));
        assertEquals(64, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.LEVEL_3, "debris"));
        assertEquals(0, GlobalPickaxeState.sideForLevel(GlobalPickaxeState.OFF, "emerald"));
    }
}
