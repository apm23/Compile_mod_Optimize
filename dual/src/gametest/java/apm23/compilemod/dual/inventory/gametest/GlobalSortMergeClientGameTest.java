package apm23.compilemod.dual.inventory.gametest;

import com.anjas.custominventory.InventoryStorage;
import com.anjas.custominventory.client.CustomHotbarInventoryClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class GlobalSortMergeClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            prepareEightPageMerge(singleplayer);
            context.waitTicks(4);
            trigger(singleplayer, context, false);
            context.waitTicks(6);
            verifyMergedAcrossAllEightPages(singleplayer);

            prepareEightPageSort(singleplayer);
            context.waitTicks(4);
            trigger(singleplayer, context, true);
            context.waitTicks(6);
            verifySortedAcrossAllEightPages(singleplayer);

            prepareOverflowLikeActiveLatePage(singleplayer);
            context.waitTicks(4);
            trigger(singleplayer, context, true);
            context.waitTicks(6);
            verifyOverflowLikeSortReturnsToCompactedPage(singleplayer);
        }
    }

    private static void trigger(TestSingleplayerContext singleplayer, ClientGameTestContext context, boolean sort) {
        context.runOnClient(client -> {
            if (client.player == null) throw new AssertionError("client player missing");
            InventoryScreen screen = new InventoryScreen(client.player);
            boolean handled = CustomHotbarInventoryClient.testHandleActiveGuiInput(screen, sort);
            if (!handled) throw new AssertionError((sort ? "sort" : "merge") + " key was not handled by active GUI input path");
        });
    }

    private static void prepareEightPageMerge(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.containerMenu = player.inventoryMenu;
            InventoryStorage.setBrowsing(player, true);
            InventoryStorage.switchPage(player, 0);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                ArrayList<ItemStack> stacks = emptyPage();
                stacks.set(0, new ItemStack(Items.STONE, 8));
                InventoryStorage.write(player, page, stacks);
                if (page == 0) InventoryStorage.loadLive(player, stacks);
            }
            InventoryStorage.sync(player);
        });
    }

    private static void verifyMergedAcrossAllEightPages(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int totalStone = 0;
            int stoneStacks = 0;
            int nonEmptyOutsidePage0 = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) {
                    if (stack.is(Items.STONE)) { totalStone += stack.getCount(); stoneStacks++; }
                    if (page > 0 && !stack.isEmpty()) nonEmptyOutsidePage0++;
                }
            }
            assertTrue(totalStone == 64, "global merge changed total stone count: " + totalStone);
            assertTrue(stoneStacks == 1, "global merge did not merge stacks from all 8 pages: stacks=" + stoneStacks);
            assertTrue(nonEmptyOutsidePage0 == 0, "global merge left items stranded outside page 1: " + nonEmptyOutsidePage0);
            assertTrue(InventoryStorage.active(player) == 0, "global merge did not return active page to page 1");
        });
    }

    private static void prepareEightPageSort(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.containerMenu = player.inventoryMenu;
            InventoryStorage.setBrowsing(player, true);
            InventoryStorage.switchPage(player, 0);
            var items = List.of(Items.ROTTEN_FLESH,Items.REDSTONE,Items.DIAMOND,Items.IRON_PICKAXE,Items.BREAD,Items.BONE,Items.APPLE,Items.EMERALD);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                ArrayList<ItemStack> stacks = emptyPage();
                stacks.set(26, new ItemStack(items.get(page), 1));
                InventoryStorage.write(player, page, stacks);
                if (page == 0) InventoryStorage.loadLive(player, stacks);
            }
            InventoryStorage.sync(player);
        });
    }

    private static void verifySortedAcrossAllEightPages(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int totalItems = 0;
            int nonEmptyOutsidePage0 = 0;
            boolean breadPresent = false;
            boolean applePresent = false;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) {
                    if (!stack.isEmpty()) {
                        totalItems += stack.getCount();
                        if (page > 0) nonEmptyOutsidePage0++;
                        if (stack.is(Items.BREAD)) breadPresent = true;
                        if (stack.is(Items.APPLE)) applePresent = true;
                    }
                }
            }
            ItemStack first = InventoryStorage.read(player, 0).get(0);
            assertTrue(totalItems == 8, "global sort changed item count: " + totalItems);
            assertTrue(nonEmptyOutsidePage0 == 0, "global sort did not compact all 8 pages into global order: " + nonEmptyOutsidePage0);
            assertTrue(breadPresent && applePresent, "global sort lost food items from remote pages");
            assertTrue(first.is(Items.APPLE), "global category/id order incorrect after 8-page sort; first=" + first);
            assertTrue(InventoryStorage.active(player) == 0, "global sort did not return active page to page 1");
        });
    }

    private static void prepareOverflowLikeActiveLatePage(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.containerMenu = player.inventoryMenu;
            InventoryStorage.setBrowsing(player, true);
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                ArrayList<ItemStack> stacks = emptyPage();
                if (page == 0) {
                    stacks.set(0, new ItemStack(Items.COBBLESTONE, 64));
                    stacks.set(1, new ItemStack(Items.DIRT, 32));
                }
                if (page == 7) {
                    stacks.set(0, new ItemStack(Items.DIAMOND, 7));
                    stacks.set(1, new ItemStack(Items.BREAD, 5));
                }
                InventoryStorage.write(player, page, stacks);
            }
            InventoryStorage.switchPage(player, 7);
            InventoryStorage.sync(player);
        });
    }

    private static void verifyOverflowLikeSortReturnsToCompactedPage(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            InventoryStorage.snapshotLive(player);
            int cobble = 0, dirt = 0, diamonds = 0, bread = 0;
            for (int page = 0; page < InventoryStorage.PAGE_COUNT; page++) {
                for (ItemStack stack : InventoryStorage.read(player, page)) {
                    if (stack.is(Items.COBBLESTONE)) cobble += stack.getCount();
                    if (stack.is(Items.DIRT)) dirt += stack.getCount();
                    if (stack.is(Items.DIAMOND)) diamonds += stack.getCount();
                    if (stack.is(Items.BREAD)) bread += stack.getCount();
                }
            }
            assertTrue(cobble == 64 && dirt == 32 && diamonds == 7 && bread == 5,
                    "overflow-like sort changed counts: cobble=" + cobble + " dirt=" + dirt + " diamonds=" + diamonds + " bread=" + bread);
            assertTrue(InventoryStorage.active(player) == 0,
                    "sort compacted items to early pages but left player viewing late page " + InventoryStorage.active(player));
            boolean liveHasItems = InventoryStorage.liveCopy(player).stream().anyMatch(stack -> !stack.isEmpty());
            assertTrue(liveHasItems, "active live page was empty after sort even though compacted inventory contains items");
        });
    }

    private static ArrayList<ItemStack> emptyPage() {
        ArrayList<ItemStack> stacks = new ArrayList<>(InventoryStorage.PAGE_SIZE);
        for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) stacks.add(ItemStack.EMPTY);
        return stacks;
    }

    private static void assertTrue(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
