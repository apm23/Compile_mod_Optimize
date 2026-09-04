package apm23.compilemod.dual.linkedshulker.gametest;

import com.anjas.linkedshulker.LinkedShulkerBlock;
import com.anjas.linkedshulker.LinkedShulkerBlockEntity;
import com.anjas.linkedshulker.ModBlocks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("UnstableApiUsage")
public final class LinkedShulkerClientGameTest implements FabricClientGameTest {
    private static final BlockPos FIRST = new BlockPos(2, 100, 2);
    private static final BlockPos SECOND = new BlockPos(4, 100, 2);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runOnServer(server -> {
                var level = server.overworld();
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
                first.stopOpen(player); // Executes the real close path, including close sound cue.
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
