package apm23.compilemod.server.xp.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

public class GlobalMendingGameTest {
    @GameTest
    public void repairsMultipleMendingItemsAndKeepsRemainder(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(mending, 1);
        pickaxe.setDamageValue(8);
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chestplate.enchant(mending, 1);
        chestplate.setDamageValue(8);
        player.getInventory().setItem(0, pickaxe);
        player.getInventory().setItem(1, chestplate);

        int pickaxeBefore = pickaxe.getDamageValue();
        int chestplateBefore = chestplate.getDamageValue();
        int xpBefore = player.totalExperience;
        new ExperienceOrb(player.level(), player.getX(), player.getY(), player.getZ(), 4).playerTouch(player);
        helper.assertTrue(pickaxe.getDamageValue() < pickaxeBefore, "Global Mending did not repair first item");
        helper.assertTrue(chestplate.getDamageValue() < chestplateBefore, "Global Mending did not repair second item");

        pickaxe.setDamageValue(1);
        chestplate.setDamageValue(1);
        new ExperienceOrb(player.level(), player.getX(), player.getY(), player.getZ(), 10).playerTouch(player);
        helper.assertTrue(pickaxe.getDamageValue() == 0, "First Mending item was not fully repaired");
        helper.assertTrue(chestplate.getDamageValue() == 0, "Second Mending item was not fully repaired");
        helper.assertTrue(player.totalExperience > xpBefore, "XP remainder did not reach player");
        helper.succeed();
    }
}
