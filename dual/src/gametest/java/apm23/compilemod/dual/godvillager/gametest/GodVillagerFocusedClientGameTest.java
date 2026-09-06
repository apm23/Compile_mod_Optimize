package apm23.compilemod.dual.godvillager.gametest;

import apm23.compilemod.dual.functional.godvillager.GodVillagerRegistry;
import com.anjas.godvillagers.SpecialistSpawnEggItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;

@SuppressWarnings("UnstableApiUsage")
public final class GodVillagerFocusedClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runOnServer(server -> {
                assertTrue(GodVillagerRegistry.GOD_TOOLS_EGG instanceof SpecialistSpawnEggItem,
                        "God Tools specialist egg is not live");
                assertTrue(GodVillagerRegistry.GOD_CLERK_EGG instanceof SpecialistSpawnEggItem,
                        "Grand Clerk specialist egg is not live");
                assertTrue(BuiltInRegistries.ITEM.getValue(
                                Identifier.fromNamespaceAndPath("godvillagers", "god_tools_villager_spawn_egg"))
                                == GodVillagerRegistry.GOD_TOOLS_EGG,
                        "God Tools egg registry entry drifted");

                String toolsNbt = summonNbt((SpecialistSpawnEggItem) GodVillagerRegistry.GOD_TOOLS_EGG);
                assertContains(toolsNbt, "God Fishing Rod", "God Fishing Rod trade missing");
                assertContains(toolsNbt, "luck_of_the_sea:25,lure:5,unbreaking:3,mending:2",
                        "God Fishing Rod enchant payload drifted");
                assertContains(toolsNbt, "buy:{id:emerald,count:28},buyB:{id:book,count:12}",
                        "God Fishing Rod discounted price drifted");

                String clerkNbt = summonNbt((SpecialistSpawnEggItem) GodVillagerRegistry.GOD_CLERK_EGG);
                assertContains(clerkNbt, "buy:{id:string,count:1},sell:{id:emerald,count:1}",
                        "Grand Clerk string trade missing or changed");
                assertContains(clerkNbt, "buy:{id:arrow,count:1},sell:{id:emerald,count:1}",
                        "Grand Clerk arrow trade missing or changed");
                assertContains(clerkNbt, "buy:{id:coal,count:2},sell:{id:gunpowder,count:1}",
                        "Grand Clerk coal-to-gunpowder trade missing or changed");
                assertContains(clerkNbt, "buy:{id:copper_ingot,count:3},sell:{id:gunpowder,count:1}",
                        "Grand Clerk copper-to-gunpowder trade missing or changed");
            });
        }
    }

    private static String summonNbt(SpecialistSpawnEggItem item) {
        try {
            Field field = SpecialistSpawnEggItem.class.getDeclaredField("summonNbt");
            field.setAccessible(true);
            return (String) field.get(item);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect specialist summon payload", e);
        }
    }

    private static void assertContains(String text, String needle, String message) {
        if (!text.contains(needle)) throw new AssertionError(message + ": " + needle);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
