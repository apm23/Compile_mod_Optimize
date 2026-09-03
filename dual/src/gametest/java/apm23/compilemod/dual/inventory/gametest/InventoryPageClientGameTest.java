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

import java.util.ArrayList;
import java.util.List;

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

            runRepeatedSortMerge(context, singleplayer);
        }
    }

    private static void runRepeatedSortMerge(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.switchPage(player, 0);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) InventoryStorage.write(player, page, emptyPage());
            player.getInventory().setItem(9, new ItemStack(Items.STONE, 30));
            player.getInventory().setItem(10, new ItemStack(Items.DIRT, 5));
            player.getInventory().setItem(11, new ItemStack(Items.BREAD, 2));
            InventoryStorage.snapshotLive(player);
            List<ItemStack> page2 = emptyPage();
            page2.set(0, new ItemStack(Items.STONE, 40));
            page2.set(1, new ItemStack(Items.DIAMOND, 3));
            InventoryStorage.write(player, 1, page2);
            InventoryStorage.sync(player);
        });
        context.waitTicks(4);

        sendAndWait(context, new ModPayloads.MergeAll());
        assertItemTotal(singleplayer, Items.STONE, 70, "first merge");
        assertStackCount(singleplayer, Items.STONE, 2, "first merge");

        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(35, new ItemStack(Items.STONE, 10));
            InventoryStorage.snapshotLive(player);
            InventoryStorage.sync(player);
        });
        context.waitTicks(3);

        sendAndWait(context, new ModPayloads.MergeAll());
        assertItemTotal(singleplayer, Items.STONE, 80, "second consecutive merge");
        assertStackCount(singleplayer, Items.STONE, 2, "second consecutive merge");

        sendAndWait(context, new ModPayloads.SortAll());
        ItemStack firstAfterSort = storedSlot(singleplayer, 0, 0);
        assertTrue(firstAfterSort.is(Items.BREAD), "first sort did not put food first; found " + firstAfterSort);

        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            List<ItemStack> page3 = InventoryStorage.read(player, 2);
            page3.set(26, new ItemStack(Items.APPLE, 1));
            InventoryStorage.write(player, 2, page3);
            InventoryStorage.loadLive(player, InventoryStorage.read(player, InventoryStorage.active(player)));
            InventoryStorage.sync(player);
        });
        context.waitTicks(3);

        sendAndWait(context, new ModPayloads.SortAll());
        ItemStack firstAfterSecondSort = storedSlot(singleplayer, 0, 0);
        assertTrue(firstAfterSecondSort.is(Items.APPLE),
                "second consecutive sort was ignored; expected apple first, found " + firstAfterSecondSort);
        assertItemTotal(singleplayer, Items.STONE, 80, "second consecutive sort conservation");
        assertItemTotal(singleplayer, Items.APPLE, 1, "second consecutive sort apple conservation");
    }

    private static void sendAndWait(ClientGameTestContext context, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        context.runOnClient(client -> ClientPlayNetworking.send(payload));
        context.waitTicks(6);
    }

    private static List<ItemStack> emptyPage() {
        ArrayList<ItemStack> page = new ArrayList<>(InventoryStorage.PAGE_SIZE);
        for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) page.add(ItemStack.EMPTY);
        return page;
    }

    private static ItemStack storedSlot(TestSingleplayerContext singleplayer, int page, int slot) {
        return singleplayer.getServer().computeOnServer(server ->
                InventoryStorage.read(server.getPlayerList().getPlayers().getFirst(), page).get(slot).copy());
    }

    private static void assertItemTotal(TestSingleplayerContext singleplayer, net.minecraft.world.item.Item item, int expected, String stage) {
        int total = singleplayer.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int sum = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++)
                for (ItemStack stack : InventoryStorage.read(player, page)) if (stack.is(item)) sum += stack.getCount();
            return sum;
        });
        assertTrue(total == expected, stage + " item total mismatch: expected=" + expected + ", actual=" + total);
    }

    private static void assertStackCount(TestSingleplayerContext singleplayer, net.minecraft.world.item.Item item, int expected, String stage) {
        int stacks = singleplayer.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int count = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++)
                for (ItemStack stack : InventoryStorage.read(player, page)) if (stack.is(item)) count++;
            return count;
        });
        assertTrue(stacks == expected, stage + " stack count mismatch: expected=" + expected + ", actual=" + stacks);
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
