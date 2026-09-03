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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
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
            testMerchantCancelPickup(singleplayer);
            testMerchantCloseReturnAcrossPages(singleplayer);

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

            testRepeatedSortMerge(context, singleplayer);
        }
    }

    private static void testMerchantCancelPickup(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            Villager villager = new Villager(EntityType.VILLAGER, player.level());
            villager.setTradingPlayer(player);
            MerchantMenu menu = new MerchantMenu(41, player.getInventory(), villager);
            player.containerMenu = menu;
            menu.getSlot(0).set(new ItemStack(Items.EMERALD, 17));
            menu.setCarried(ItemStack.EMPTY);

            menu.clicked(0, 0, ClickType.PICKUP, player);

            ItemStack carried = menu.getCarried().copy();
            assertTrue(carried.is(Items.EMERALD) && carried.getCount() == 17,
                    "merchant cancel pickup lost payment; carried=" + carried);
            assertTrue(menu.getSlot(0).getItem().isEmpty(),
                    "merchant cancel pickup left payment duplicated in slot: " + menu.getSlot(0).getItem());
            menu.setCarried(ItemStack.EMPTY);
            player.containerMenu = player.inventoryMenu;
            villager.setTradingPlayer(null);
        });
    }

    private static void testMerchantCloseReturnAcrossPages(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.setBrowsing(player, false);
            InventoryStorage.switchPage(player, 0);

            ArrayList<ItemStack> full = new ArrayList<>(InventoryStorage.PAGE_SIZE);
            for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) full.add(new ItemStack(Items.COBBLESTONE, 64));
            InventoryStorage.write(player, 0, full);
            InventoryStorage.loadLive(player, full);
            InventoryStorage.write(player, 1, List.of());
            for (int i = 0; i < 9; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            InventoryStorage.snapshotLive(player);

            Villager villager = new Villager(EntityType.VILLAGER, player.level());
            villager.setTradingPlayer(player);
            MerchantMenu menu = new MerchantMenu(42, player.getInventory(), villager);
            player.containerMenu = menu;
            menu.getSlot(0).set(new ItemStack(Items.EMERALD, 23));
            menu.getSlot(1).set(ItemStack.EMPTY);

            menu.removed(player);
            player.containerMenu = player.inventoryMenu;

            int virtualEmeralds = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) {
                    if (stack.is(Items.EMERALD)) virtualEmeralds += stack.getCount();
                }
            }
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.EMERALD)) virtualEmeralds += stack.getCount();
            }
            assertTrue(virtualEmeralds == 23,
                    "merchant close did not return change into virtual inventory; emeralds=" + virtualEmeralds);
            assertTrue(menu.getSlot(0).getItem().isEmpty(),
                    "merchant close left payment in merchant slot: " + menu.getSlot(0).getItem());
        });
    }

    private static void testRepeatedSortMerge(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        context.runOnClient(client -> client.setScreen(null));
        context.waitTicks(3);
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.setBrowsing(player, true);
            InventoryStorage.switchPage(player, 0);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) InventoryStorage.write(player, page, List.of());
            player.getInventory().setItem(9, new ItemStack(Items.STONE, 30));
            player.getInventory().setItem(10, new ItemStack(Items.STONE, 40));
            player.getInventory().setItem(11, new ItemStack(Items.ROTTEN_FLESH, 5));
            player.getInventory().setItem(12, new ItemStack(Items.BREAD, 2));
            player.containerMenu = player.inventoryMenu;
            InventoryStorage.snapshotLive(player);
            InventoryStorage.sync(player);
        });
        context.waitTicks(3);
        context.runOnClient(client -> ClientPlayNetworking.send(new ModPayloads.MergeAll()));
        context.waitTicks(5);
        assertVirtualCount(singleplayer, Items.STONE, 70, "first merge");
        assertVirtualStackCount(singleplayer, Items.STONE, 2, "first merge");

        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(20, new ItemStack(Items.STONE, 10));
            InventoryStorage.snapshotLive(player);
        });
        context.runOnClient(client -> ClientPlayNetworking.send(new ModPayloads.MergeAll()));
        context.waitTicks(5);
        assertVirtualCount(singleplayer, Items.STONE, 80, "second merge");
        assertVirtualStackCount(singleplayer, Items.STONE, 2, "second merge");

        context.runOnClient(client -> ClientPlayNetworking.send(new ModPayloads.SortAll()));
        context.waitTicks(5);
        assertFirstVirtualItem(singleplayer, Items.BREAD, "first sort");
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(25, new ItemStack(Items.APPLE, 1));
            InventoryStorage.snapshotLive(player);
        });
        context.runOnClient(client -> ClientPlayNetworking.send(new ModPayloads.SortAll()));
        context.waitTicks(5);
        assertFirstVirtualItem(singleplayer, Items.APPLE, "second sort");
    }

    private static void assertVirtualCount(TestSingleplayerContext singleplayer, net.minecraft.world.item.Item item, int expected, String stage) {
        int actual = singleplayer.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int total = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) if (stack.is(item)) total += stack.getCount();
            }
            return total;
        });
        assertTrue(actual == expected, stage + " count mismatch: expected=" + expected + " actual=" + actual);
    }

    private static void assertVirtualStackCount(TestSingleplayerContext singleplayer, net.minecraft.world.item.Item item, int expected, String stage) {
        int actual = singleplayer.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int total = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) if (stack.is(item)) total++;
            }
            return total;
        });
        assertTrue(actual == expected, stage + " stack-count mismatch: expected=" + expected + " actual=" + actual);
    }

    private static void assertFirstVirtualItem(TestSingleplayerContext singleplayer, net.minecraft.world.item.Item expected, String stage) {
        ItemStack first = singleplayer.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) if (!stack.isEmpty()) return stack.copy();
            }
            return ItemStack.EMPTY;
        });
        assertTrue(first.is(expected), stage + " did not execute/order correctly; first=" + first);
    }

    private static void prepareServer(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.containerMenu = player.inventoryMenu;
            InventoryStorage.setBrowsing(player, true);
            InventoryStorage.switchPage(player, 0);
            InventoryStorage.write(player, TARGET_PAGE, java.util.List.of());
            for (int i = 0; i < 9; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
            for (int i = 9; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
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
