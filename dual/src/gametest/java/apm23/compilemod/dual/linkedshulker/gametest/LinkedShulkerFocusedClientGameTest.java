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
public final class LinkedShulkerFocusedClientGameTest implements FabricClientGameTest {
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

                first.setChannel("Focused Smoke");
                second.setChannel("Focused Smoke");
                first.setItem(0, new ItemStack(Items.DIAMOND, 11));

                ItemStack mirrored = second.getItem(0);
                assertTrue(mirrored.is(Items.DIAMOND) && mirrored.getCount() == 11,
                        "same-channel storage did not mirror exactly: " + mirrored);

                second.setChannel("focused smoke");
                assertTrue(second.getItem(0).isEmpty(),
                        "channel names stopped being case-sensitive: " + second.getItem(0));
                second.setChannel("Focused Smoke");
                assertTrue(second.getItem(0).is(Items.DIAMOND) && second.getItem(0).getCount() == 11,
                        "returning to original channel did not restore shared contents");

                var player = server.getPlayerList().getPlayers().getFirst();
                first.startOpen(player);
            });

            context.waitTicks(12);
            int openFrame = singleplayer.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(FIRST).getValue(LinkedShulkerBlock.OPEN_FRAME));
            assertTrue(openFrame == LinkedShulkerBlock.MAX_OPEN_FRAME,
                    "open animation did not reach max frame: " + openFrame);

            singleplayer.getServer().runOnServer(server -> {
                LinkedShulkerBlockEntity first = requireLinked(server.overworld().getBlockEntity(FIRST), "first-close");
                var player = server.getPlayerList().getPlayers().getFirst();
                first.stopOpen(player);
            });

            context.waitTicks(12);
            int closedFrame = singleplayer.getServer().computeOnServer(server ->
                    server.overworld().getBlockState(FIRST).getValue(LinkedShulkerBlock.OPEN_FRAME));
            assertTrue(closedFrame == 0, "close animation did not return to frame 0: " + closedFrame);

            ItemStack persisted = singleplayer.getServer().computeOnServer(server -> {
                LinkedShulkerBlockEntity second = requireLinked(server.overworld().getBlockEntity(SECOND), "second-final");
                return second.getItem(0).copy();
            });
            assertTrue(persisted.is(Items.DIAMOND) && persisted.getCount() == 11,
                    "shared inventory changed during animation: " + persisted);
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
