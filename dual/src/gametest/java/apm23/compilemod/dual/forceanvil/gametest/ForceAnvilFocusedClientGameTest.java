package apm23.compilemod.dual.forceanvil.gametest;

import com.anjas.forceanvil.ForceAnvilMenu;
import com.anjas.forceanvil.ForceAnvilMod;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@SuppressWarnings("UnstableApiUsage")
public final class ForceAnvilFocusedClientGameTest implements FabricClientGameTest {
    private static final BlockPos ANVIL_POS = new BlockPos(2, 100, 2);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();

                assertTrue(BuiltInRegistries.BLOCK.getValue(ForceAnvilMod.FORCE_ANVIL_ID) == ForceAnvilMod.FORCE_ANVIL,
                        "force anvil block registry entry is not live");
                assertTrue(BuiltInRegistries.ITEM.getValue(ForceAnvilMod.FORCE_ANVIL_ID) == ForceAnvilMod.FORCE_ANVIL_ITEM,
                        "force anvil item registry entry is not live");

                level.setBlockAndUpdate(ANVIL_POS, ForceAnvilMod.FORCE_ANVIL.defaultBlockState());
                assertTrue(level.getBlockState(ANVIL_POS).is(ForceAnvilMod.FORCE_ANVIL),
                        "force anvil could not be placed");

                var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var sharpness = enchantmentRegistry.getOrThrow(Enchantments.SHARPNESS);

                ItemStack forcedBase = new ItemStack(Items.STICK);
                ItemStack enchantSource = new ItemStack(Items.BOOK);
                EnchantmentHelper.updateEnchantments(enchantSource, mutable -> mutable.set(sharpness, 5));

                ForceAnvilMenu menu = new ForceAnvilMenu(
                        77,
                        player.getInventory(),
                        ContainerLevelAccess.create(level, ANVIL_POS));
                menu.getSlot(0).set(forcedBase);
                menu.getSlot(1).set(enchantSource);

                ItemStack result = menu.getSlot(2).getItem();
                assertTrue(result.is(Items.STICK), "force anvil changed the base item: " + result);
                ItemEnchantments resultEnchantments = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                assertTrue(resultEnchantments.getLevel(sharpness) == 5,
                        "force anvil did not force Sharpness V onto incompatible stick: " + resultEnchantments);
                assertTrue(menu.getCost() == 1, "force anvil XP cost drifted from 1: " + menu.getCost());
                menu.removed(player);
            });
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
