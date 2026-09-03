package apm23.compilemod.dual.inventory.gametest;

import com.anjas.custominventory.InventoryStorage;
import com.anjas.custominventory.ModPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("UnstableApiUsage")
public final class InventoryPageClientGameTest implements FabricClientGameTest {
    private static final int INVENTORY_IMAGE_WIDTH = 176;
    private static final int INVENTORY_IMAGE_HEIGHT = 166;
    private static final int SOURCE_POSITION = 9;
    private static final int TARGET_PAGE = 1;
    private static final int COUNT = 7;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            prepareServer(singleplayer);
            context.waitTicks(4);

            openInventory(context);
            clickInventorySlot(context, SOURCE_POSITION);
            assertCarriedClient(context, "after pickup");
            assertCarriedServer(singleplayer, "after pickup");

            context.runOnClient(client -> ClientPlayNetworking.send(new ModPayloads.DirectPage.P2()));
            context.waitTicks(6);

            int active = singleplayer.getServer().computeOnServer(server ->
                    InventoryStorage.active(server.getPlayerList().getPlayers().getFirst()));
            assertTrue(active == TARGET_PAGE, "page switch did not reach page 2; active=" + active);
            assertCarriedClient(context, "after page switch");
            assertCarriedServer(singleplayer, "after page switch");

            clickInventorySlot(context, SOURCE_POSITION);
            context.waitTicks(4);

            ItemStack clientCarried = context.computeOnClient(client -> client.player.containerMenu.getCarried().copy());
            assertTrue(clientCarried.isEmpty(), "client cursor did not empty after placing item on page 2: " + clientCarried);

            ItemStack serverCarried = singleplayer.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().containerMenu.getCarried().copy());
            assertTrue(serverCarried.isEmpty(), "server cursor did not empty after placing item on page 2: " + serverCarried);

            ItemStack placed = singleplayer.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().getInventory().getItem(SOURCE_POSITION).copy());
            assertTrue(placed.is(Items.DIAMOND) && placed.getCount() == COUNT,
                    "item was not placed on page 2; found " + placed);

            ItemStack sourcePageSlot = singleplayer.getServer().computeOnServer(server ->
                    InventoryStorage.read(server.getPlayerList().getPlayers().getFirst(), 0).get(0));
            assertTrue(sourcePageSlot.isEmpty(), "source page still contains the moved item: " + sourcePageSlot);
        }
    }

    private static void prepareServer(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.setBrowsing(player, true);
            InventoryStorage.switchPage(player, 0);
            InventoryStorage.write(player, TARGET_PAGE, java.util.List.of());
            player.getInventory().setItem(SOURCE_POSITION, new ItemStack(Items.DIAMOND, COUNT));
            player.containerMenu.setCarried(ItemStack.EMPTY);
            InventoryStorage.snapshotLive(player);
            InventoryStorage.sync(player);
        });
    }

    private static void openInventory(ClientGameTestContext context) {
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(3);
    }

    private static void clickInventorySlot(ClientGameTestContext context, int position) {
        double[] pos = context.computeOnClient(client -> {
            Slot slot = findSlot(client, position);
            double scale = client.getWindow().getGuiScale();
            int left = (client.gui.screen().width - INVENTORY_IMAGE_WIDTH) / 2;
            int top = (client.gui.screen().height - INVENTORY_IMAGE_HEIGHT) / 2;
            return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTicks(2);
        context.getInput().pressMouse(0);
        context.waitTicks(4);
    }

    private static Slot findSlot(Minecraft client, int position) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == client.player.getInventory() && slot.getContainerSlot() == position) return slot;
        }
        throw new AssertionError("no menu slot exposes inventory position " + position);
    }

    private static void assertCarriedClient(ClientGameTestContext context, String stage) {
        ItemStack stack = context.computeOnClient(client -> client.player.containerMenu.getCarried().copy());
        assertTrue(stack.is(Items.DIAMOND) && stack.getCount() == COUNT,
                "client carried item mismatch " + stage + ": " + stack);
    }

    private static void assertCarriedServer(TestSingleplayerContext singleplayer, String stage) {
        ItemStack stack = singleplayer.getServer().computeOnServer(server ->
                server.getPlayerList().getPlayers().getFirst().containerMenu.getCarried().copy());
        assertTrue(stack.is(Items.DIAMOND) && stack.getCount() == COUNT,
                "server carried item mismatch " + stage + ": " + stack);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
