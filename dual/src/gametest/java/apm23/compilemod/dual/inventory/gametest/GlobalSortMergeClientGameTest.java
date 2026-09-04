package apm23.compilemod.dual.inventory.gametest;

import com.anjas.custominventory.InventoryStorage;
import com.anjas.custominventory.client.CustomHotbarInventoryClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
            context.runOnClient(client -> CustomHotbarInventoryClient.dispatchMergeAction());
            context.waitTicks(6);
            verifyMergedAcrossAllEightPages(singleplayer);

            prepareEightPageSort(singleplayer);
            context.waitTicks(4);
            context.runOnClient(client -> CustomHotbarInventoryClient.dispatchSortAction());
            context.waitTicks(6);
            verifySortedAcrossAllEightPages(singleplayer);
        }
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
                    if (stack.is(Items.STONE)) {
                        totalStone += stack.getCount();
                        stoneStacks++;
                    }
                    if (page > 0 && !stack.isEmpty()) nonEmptyOutsidePage0++;
                }
            }
            assertTrue(totalStone == 64, "global merge changed total stone count: " + totalStone);
            assertTrue(stoneStacks == 1, "global merge did not merge stacks from all 8 pages: stacks=" + stoneStacks);
            assertTrue(nonEmptyOutsidePage0 == 0, "global merge left items stranded outside page 1: " + nonEmptyOutsidePage0);
        });
    }

    private static void prepareEightPageSort(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.containerMenu = player.inventoryMenu;
            InventoryStorage.switchPage(player, 0);
            var items = List.of(
                    Items.ROTTEN_FLESH,
                    Items.REDSTONE,
                    Items.DIAMOND,
                    Items.IRON_PICKAXE,
                    Items.BREAD,
                    Items.BONE,
                    Items.APPLE,
                    Items.EMERALD
            );
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
        });
    }

    private static ArrayList<ItemStack> emptyPage() {
        ArrayList<ItemStack> stacks = new ArrayList<>(InventoryStorage.PAGE_SIZE);
        for (int i = 0; i < InventoryStorage.PAGE_SIZE; i++) stacks.add(ItemStack.EMPTY);
        return stacks;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
