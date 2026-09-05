package apm23.compilemod.dual.forceanvil.gametest;

import com.anjas.forceanvil.ForceAnvilMod;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

public final class VelocityHopperFocusedClientGameTest implements ClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runOnServer(server -> {
                var level = server.overworld();
                BlockPos hopperPos = new BlockPos(0, 100, 0);
                BlockPos chestPos = hopperPos.below();

                level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(hopperPos, ForceAnvilMod.VELOCITY_HOPPER.defaultBlockState());

                if (!(level.getBlockEntity(hopperPos) instanceof HopperBlockEntity hopper)) {
                    throw new AssertionError("Velocity Hopper did not create a HopperBlockEntity");
                }
                if (!(level.getBlockEntity(chestPos) instanceof ContainerBlockEntity chest)) {
                    throw new AssertionError("Destination chest block entity missing");
                }

                hopper.setItem(0, new ItemStack(Items.DIAMOND, 3));
                hopper.setChanged();

                // Let the real server ticker + VelocityHopperSpeedMixin perform transfer.
                for (int i = 0; i < 6; i++) {
                    level.tickBlockEntities();
                }

                int moved = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (stack.is(Items.DIAMOND)) moved += stack.getCount();
                }
                if (moved <= 0) {
                    throw new AssertionError("Velocity Hopper did not transfer an item through the real hopper path");
                }
                if (hopper.getItem(0).getCount() >= 3) {
                    throw new AssertionError("Velocity Hopper source inventory was not consumed");
                }
            });
        }
    }
}