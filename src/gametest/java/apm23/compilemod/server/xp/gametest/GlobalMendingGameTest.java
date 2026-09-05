package apm23.compilemod.server.xp.gametest;

import com.apm23.custompickaxe.RemoteMiningManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

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

    @GameTest
    public void multiplayerPlayersRepairIndependently(GameTestHelper helper) {
        ServerPlayer playerA = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ServerPlayer playerB = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var enchantments = playerA.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);

        ItemStack toolA = new ItemStack(Items.DIAMOND_PICKAXE);
        toolA.enchant(mending, 1);
        toolA.setDamageValue(8);
        ItemStack toolB = new ItemStack(Items.DIAMOND_AXE);
        toolB.enchant(mending, 1);
        toolB.setDamageValue(8);
        playerA.getInventory().setItem(0, toolA);
        playerB.getInventory().setItem(0, toolB);

        int bBefore = toolB.getDamageValue();
        new ExperienceOrb(playerA.level(), playerA.getX(), playerA.getY(), playerA.getZ(), 4).playerTouch(playerA);
        helper.assertTrue(toolA.getDamageValue() < 8, "Player A did not receive Global Mending repair");
        helper.assertTrue(toolB.getDamageValue() == bBefore, "Player A XP incorrectly repaired Player B item");

        int aAfterOwnXp = toolA.getDamageValue();
        new ExperienceOrb(playerB.level(), playerB.getX(), playerB.getY(), playerB.getZ(), 4).playerTouch(playerB);
        helper.assertTrue(toolB.getDamageValue() < bBefore, "Player B did not receive Global Mending repair");
        helper.assertTrue(toolA.getDamageValue() == aAfterOwnXp, "Player B XP incorrectly repaired Player A item");
        helper.succeed();
    }

    @GameTest
    public void remoteMiningBreaksTargetOreAndDropsReward(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        level.setBlockAndUpdate(origin, Blocks.DIAMOND_ORE.defaultBlockState());

        helper.assertTrue(RemoteMiningManager.isSupportedType("diamond"), "diamond remote-mining target is not registered");
        RemoteMiningManager.start(player, origin, "diamond", 8);
        RemoteMiningManager.tick(level.getServer());

        helper.assertTrue(level.getBlockState(origin).isAir(), "remote mining did not break target diamond ore");
        boolean diamondReward = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0D)).stream()
                .anyMatch(entity -> entity.getItem().is(Items.DIAMOND) && entity.getItem().getCount() >= 1);
        helper.assertTrue(diamondReward, "remote mining did not emit diamond reward");
        helper.succeed();
    }
}
