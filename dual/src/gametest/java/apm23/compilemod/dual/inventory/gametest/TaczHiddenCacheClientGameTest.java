package apm23.compilemod.dual.inventory.gametest;

import com.anjas.custominventory.client.HiddenRecipeContentsClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class TaczHiddenCacheClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext ignored = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                HiddenRecipeContentsClient.clear();
                long before = HiddenRecipeContentsClient.revision();

                ItemStack diamonds = new ItemStack(Items.DIAMOND, 37);
                ItemStack arrows = new ItemStack(Items.ARROW, 12);
                HiddenRecipeContentsClient.replace(List.of(ItemStack.EMPTY, diamonds, arrows));

                List<ItemStack> view = HiddenRecipeContentsClient.view();
                assertTrue(view.size() == 2, "hidden cache retained empty entries: size=" + view.size());
                assertTrue(view.get(0).is(Items.DIAMOND) && view.get(0).getCount() == 37,
                        "diamond stack changed in hidden cache: " + view.get(0));
                assertTrue(view.get(1).is(Items.ARROW) && view.get(1).getCount() == 12,
                        "arrow stack changed in hidden cache: " + view.get(1));
                assertTrue(HiddenRecipeContentsClient.revision() == before + 1,
                        "replace did not advance revision exactly once");

                ItemStack source = new ItemStack(Items.IRON_INGOT, 23);
                HiddenRecipeContentsClient.replace(List.of(source));
                source.setCount(1);
                assertTrue(HiddenRecipeContentsClient.view().getFirst().getCount() == 23,
                        "replace retained caller-owned mutable stack");

                ItemStack snapshot = HiddenRecipeContentsClient.snapshot().getFirst();
                assertTrue(snapshot != HiddenRecipeContentsClient.view().getFirst(),
                        "snapshot returned live cached stack reference");
                snapshot.setCount(2);
                assertTrue(HiddenRecipeContentsClient.view().getFirst().getCount() == 23,
                        "snapshot mutation leaked into hidden cache");

                long clearBefore = HiddenRecipeContentsClient.revision();
                HiddenRecipeContentsClient.clear();
                assertTrue(HiddenRecipeContentsClient.view().isEmpty(), "clear did not empty hidden cache");
                assertTrue(HiddenRecipeContentsClient.revision() == clearBefore + 1,
                        "clear did not advance revision exactly once");
            });
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
