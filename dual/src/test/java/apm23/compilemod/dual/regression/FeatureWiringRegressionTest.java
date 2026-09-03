package apm23.compilemod.dual.regression;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

final class FeatureWiringRegressionTest {
    @Test
    void consolidatedEntrypointsIncludeRestoredRuntimes() throws IOException {
        String mod = resource("/fabric.mod.json");
        assertTrue(mod.contains("apm23.compilemod.dual.functional.godvillager.GodVillagerRegistry"));
        assertTrue(mod.contains("com.anjas.forceanvil.ForceAnvilMod"));
        assertTrue(mod.contains("com.anjas.custominventory.CustomHotbarInventory"));
        assertTrue(mod.contains("com.anjas.linkedshulker.LinkedShulkerMod"));
    }

    @Test
    void inventoryServerMixinsArePackaged() throws IOException {
        String mixins = resource("/custom_hotbar_inventory.mixins.json");
        assertTrue(mixins.contains("MerchantMenuMixin"));
        assertTrue(mixins.contains("ServerPlaceRecipeMixin"));
        assertTrue(mixins.contains("TaczGunSmithTableMenuMixin"));
    }

    @Test
    void forceAnvilIsInVanillaAnvilTagAndMixinIsEnabled() throws IOException {
        String tag = resource("/data/minecraft/tags/block/anvil.json");
        assertTrue(tag.contains("forceanvil:force_anvil"));
        String mixins = resource("/forceanvil.mixins.json");
        assertTrue(mixins.contains("ForceAnvilMenuMixin"));
    }

    @Test
    void stormcallAndGodHorseRuntimeClassesArePresent() throws Exception {
        assertNotNull(Class.forName("com.anjas.godvillagers.StormcallRuntime"));
        assertNotNull(Class.forName("com.anjas.godvillagers.GodHorseRuntime"));
        assertNotNull(Class.forName("apm23.compilemod.dual.functional.godvillager.GodVillagerRegistry"));
    }

    @Test
    void linkedShulkerSoundAndParticleCodeArePresent() throws Exception {
        Class<?> be = Class.forName("com.anjas.linkedshulker.LinkedShulkerBlockEntity");
        assertNotNull(be.getDeclaredMethod("startOpen", net.minecraft.world.entity.ContainerUser.class));
        assertNotNull(be.getDeclaredMethod("serverTick", net.minecraft.world.level.Level.class,
            net.minecraft.core.BlockPos.class,
            net.minecraft.world.level.block.state.BlockState.class,
            be));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = FeatureWiringRegressionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
