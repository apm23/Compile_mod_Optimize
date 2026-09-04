package apm23.compilemod.dual.linkedshulker.gametest;

import apm23.compilemod.dual.functional.godvillager.GodVillagerRegistry;
import com.anjas.forceanvil.ForceAnvilMod;
import com.anjas.godvillagers.SpecialistSpawnEggItem;
import com.anjas.linkedshulker.LinkedShulkerBlock;
import com.anjas.linkedshulker.LinkedShulkerBlockEntity;
import com.anjas.linkedshulker.ModBlocks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

@SuppressWarnings("UnstableApiUsage")
public final class LinkedShulkerClientGameTest implements FabricClientGameTest {
    private static final BlockPos FIRST = new BlockPos(2, 100, 2);
    private static final BlockPos SECOND = new BlockPos(4, 100, 2);
    private static final BlockPos FORCE_ANVIL = new BlockPos(6, 100, 2);
    private static final BlockPos VELOCITY_HOPPER = new BlockPos(8, 100, 2);
    private static final BlockPos VILLAGER_POS = new BlockPos(10, 100, 2);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runOnServer(server -> {
                var level = server.overworld();

                // Force Anvil + Velocity Hopper runtime registration/placement smoke.
                assertTrue(BuiltInRegistries.BLOCK.getValue(ForceAnvilMod.FORCE_ANVIL_ID) == ForceAnvilMod.FORCE_ANVIL,
                        "force anvil registry entry is not live");
                assertTrue(BuiltInRegistries.BLOCK.getValue(ForceAnvilMod.VELOCITY_HOPPER_ID) == ForceAnvilMod.VELOCITY_HOPPER,
                        "velocity hopper registry entry is not live");
                level.setBlockAndUpdate(FORCE_ANVIL, ForceAnvilMod.FORCE_ANVIL.defaultBlockState());
                level.setBlockAndUpdate(VELOCITY_HOPPER, ForceAnvilMod.VELOCITY_HOPPER.defaultBlockState());
                assertTrue(level.getBlockState(FORCE_ANVIL).is(ForceAnvilMod.FORCE_ANVIL), "force anvil could not be placed");
                assertTrue(level.getBlockState(VELOCITY_HOPPER).is(ForceAnvilMod.VELOCITY_HOPPER), "velocity hopper could not be placed");
                assertTrue(level.getBlockEntity(VELOCITY_HOPPER) != null, "velocity hopper did not create a hopper block entity");

                // God Villager enhanced summon/trade smoke: verify the final egg payload, execute it,
                // and require a real villager with offers to exist afterward.
                assertTrue(GodVillagerRegistry.GOD_TOOLS_EGG instanceof SpecialistSpawnEggItem,
                        "God Tools specialist egg is not registered as the runtime spawn item");
                String toolsSummon = summonSuffix((SpecialistSpawnEggItem) GodVillagerRegistry.GOD_TOOLS_EGG);
                assertTrue(toolsSummon.contains("God Fishing Rod"), "God Fishing Rod trade is missing from Tool Sage");
                assertTrue(toolsSummon.contains("luck_of_the_sea:25,lure:5,unbreaking:3,mending:2"),
                        "God Fishing Rod enchant payload drifted: " + toolsSummon);
                assertTrue(toolsSummon.contains("buy:{id:emerald,count:28},buyB:{id:book,count:12}"),
                        "God Fishing Rod 20 percent discounted price drifted: " + toolsSummon);

                level.setBlockAndUpdate(VILLAGER_POS.below(), Blocks.STONE.defaultBlockState());
                int split = toolsSummon.indexOf(' ');
                String entity = split < 0 ? toolsSummon : toolsSummon.substring(0, split);
                String nbt = split < 0 ? "" : toolsSummon.substring(split + 1);
                String command = "summon " + entity + " " + VILLAGER_POS.getX() + " " + VILLAGER_POS.getY() + " "
                        + VILLAGER_POS.getZ() + (nbt.isEmpty() ? "" : " " + nbt);
                int summonResult = server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(VILLAGER_POS)).withSuppressedOutput(),
                        command);
                assertTrue(summonResult > 0, "God Tools villager summon command failed");
                var villagers = level.getEntitiesOfClass(Villager.class,
                        new AABB(VILLAGER_POS.getX() - 2, VILLAGER_POS.getY() - 2, VILLAGER_POS.getZ() - 2,
                                VILLAGER_POS.getX() + 3, VILLAGER_POS.getY() + 3, VILLAGER_POS.getZ() + 3));
                assertTrue(!villagers.isEmpty(), "God Tools villager did not spawn");
                assertTrue(villagers.getFirst().getOffers().size() >= 6, "God Tools villager offers were not loaded from NBT");

                // Linked Shulker functional smoke.
                level.setBlockAndUpdate(FIRST, ModBlocks.LINKED_SHULKER.defaultBlockState());
                level.setBlockAndUpdate(SECOND, ModBlocks.LINKED_SHULKER.defaultBlockState());

                LinkedShulkerBlockEntity first = requireLinked(level.getBlockEntity(FIRST), "first");
                LinkedShulkerBlockEntity second = requireLinked(level.getBlockEntity(SECOND), "second");
                first.setChannel("Functional Smoke");
                second.setChannel("Functional Smoke");
                first.setItem(0, new ItemStack(Items.DIAMOND, 11));

                ItemStack linked = second.getItem(0);
                assertTrue(linked.is(Items.DIAMOND) && linked.getCount() == 11,
                        "same-name linked shulkers did not share storage: " + linked);

                second.setChannel("Functional smoke");
                assertTrue(second.getItem(0).isEmpty(),
                        "case-sensitive channel separation regressed: " + second.getItem(0));
                second.setChannel("Functional Smoke");

                var player = server.getPlayerList().getPlayers().getFirst();
                first.startOpen(player);
            });

            context.waitTicks(12);
            int fullOpen = singleplayer.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(FIRST).getValue(LinkedShulkerBlock.OPEN_FRAME));
            assertTrue(fullOpen == LinkedShulkerBlock.MAX_OPEN_FRAME,
                    "linked shulker did not reach full-open frame; frame=" + fullOpen);

            singleplayer.getServer().runOnServer(server -> {
                LinkedShulkerBlockEntity first = requireLinked(server.overworld().getBlockEntity(FIRST), "first-close");
                var player = server.getPlayerList().getPlayers().getFirst();
                first.stopOpen(player);
            });

            context.waitTicks(12);
            int closed = singleplayer.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(FIRST).getValue(LinkedShulkerBlock.OPEN_FRAME));
            assertTrue(closed == 0, "linked shulker did not return to closed frame; frame=" + closed);

            ItemStack persisted = singleplayer.getServer().computeOnServer(server -> {
                LinkedShulkerBlockEntity second = requireLinked(server.overworld().getBlockEntity(SECOND), "second-final");
                return second.getItem(0).copy();
            });
            assertTrue(persisted.is(Items.DIAMOND) && persisted.getCount() == 11,
                    "linked inventory changed during open/close animation: " + persisted);
        }
    }

    private static String summonSuffix(SpecialistSpawnEggItem item) {
        try {
            Field field = SpecialistSpawnEggItem.class.getDeclaredField("summonSuffix");
            field.setAccessible(true);
            return (String) field.get(item);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not inspect specialist summon payload", exception);
        }
    }

    private static LinkedShulkerBlockEntity requireLinked(Object blockEntity, String stage) {
        if (!(blockEntity instanceof LinkedShulkerBlockEntity linked)) {
            throw new AssertionError("missing linked shulker block entity at " + stage + ": " + blockEntity);
        }
        return linked;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
