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
        ClassLoader loader = FeatureWiringRegressionTest.class.getClassLoader();
        assertNotNull(Class.forName("com.anjas.godvillagers.StormcallRuntime", false, loader));
        assertNotNull(Class.forName("com.anjas.godvillagers.GodHorseRuntime", false, loader));
        assertNotNull(Class.forName("apm23.compilemod.dual.functional.godvillager.GodVillagerRegistry", false, loader));
    }

    @Test
    void linkedShulkerSoundAndParticleCodeArePresent() throws Exception {
        ClassLoader loader = FeatureWiringRegressionTest.class.getClassLoader();
        Class<?> be = Class.forName("com.anjas.linkedshulker.LinkedShulkerBlockEntity", false, loader);
        Class<?> user = Class.forName("net.minecraft.world.entity.ContainerUser", false, loader);
        Class<?> level = Class.forName("net.minecraft.world.level.Level", false, loader);
        Class<?> pos = Class.forName("net.minecraft.core.BlockPos", false, loader);
        Class<?> state = Class.forName("net.minecraft.world.level.block.state.BlockState", false, loader);
        assertNotNull(be.getDeclaredMethod("startOpen", user));
        assertNotNull(be.getDeclaredMethod("serverTick", level, pos, state, be));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = FeatureWiringRegressionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
