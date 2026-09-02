package com.apm23.custompickaxe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RemoteMiningManager {
    private static final int POSITIONS_PER_TICK = 2048;
    private static final int BLOCKS_PER_TICK = 64;

    private static final Map<String, TargetSpec> TARGETS = Map.of(
            "iron", TargetSpec.sameReward(Set.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE), Items.RAW_IRON),
            "copper", TargetSpec.sameReward(Set.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE), Items.RAW_COPPER),
            "gold", TargetSpec.sameReward(Set.of(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE), Items.RAW_GOLD),
            "diamond", TargetSpec.sameReward(Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE), Items.DIAMOND),
            "emerald", TargetSpec.sameReward(Set.of(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE), Items.EMERALD),
            "coal", TargetSpec.sameReward(Set.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE), Items.COAL),
            "lapis", TargetSpec.sameReward(Set.of(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE), Items.LAPIS_LAZULI),
            "redstone", TargetSpec.sameReward(Set.of(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE), Items.REDSTONE),
            "debris", TargetSpec.sameReward(Set.of(Blocks.ANCIENT_DEBRIS), Items.ANCIENT_DEBRIS),
            "amethyst", new TargetSpec(Map.of(
                    Blocks.AMETHYST_BLOCK, Blocks.AMETHYST_BLOCK.asItem(),
                    Blocks.BUDDING_AMETHYST, Blocks.BUDDING_AMETHYST.asItem(),
                    Blocks.SMALL_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD.asItem(),
                    Blocks.MEDIUM_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD.asItem(),
                    Blocks.LARGE_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD.asItem(),
                    Blocks.AMETHYST_CLUSTER, Blocks.AMETHYST_CLUSTER.asItem()
            ), Items.AMETHYST_SHARD)
    );

    private static final Map<UUID, ScanTask> TASKS = new HashMap<>();

    private RemoteMiningManager() {}

    public static boolean isSupportedType(String type) {
        return TARGETS.containsKey(type);
    }

    public static void start(ServerPlayer player, BlockPos origin, String type, int side) {
        TargetSpec target = TARGETS.get(type);
        if (target == null || !ScanLayout.isSupportedSide(side)) return;

        if (player.level() instanceof ServerLevel level) breakNaturalMask(level, player, origin);

        var enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.FORTUNE), player.getMainHandItem());

        ScanTask previous = TASKS.put(player.getUUID(), new ScanTask(
                player, origin.getX(), origin.getY(), origin.getZ(), player.level().dimension(), target, side, fortuneLevel));
        if (previous != null) previous.preserveCollected();
    }

    public static void tick(ServerLevel level) {
        TASKS.values().removeIf(task -> task.tick(level));
    }

    private static void breakNaturalMask(ServerLevel level, ServerPlayer player, BlockPos origin) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int wanted = random.nextInt(2, 6);
        int broken = 0;
        int attempts = 0;
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();

        while (broken < wanted && attempts++ < 24) {
            BlockPos candidate = origin
                    .relative(forward, random.nextInt(0, 3))
                    .relative(side, random.nextInt(-1, 2))
                    .above(random.nextInt(-1, 2));

            if (candidate.equals(origin) || !level.isInWorldBounds(candidate)
                    || !level.hasChunkAt(candidate) || level.getBlockEntity(candidate) != null) continue;

            BlockState state = level.getBlockState(candidate);
            if (!isNaturalMaskTerrain(state)) continue;
            if (level.destroyBlock(candidate, true, player)) broken++;
        }
    }

    private static boolean isNaturalMaskTerrain(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BLACKSTONE) || state.is(Blocks.BASALT) || state.is(Blocks.SMOOTH_BASALT);
    }

    private static final class ScanTask {
        private final ServerPlayer player;
        private final int originX, originY, originZ;
        private final ResourceKey<Level> dimension;
        private final TargetSpec target;
        private final int side;
        private final int totalPositions;
        private final int fortuneLevel;
        private final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        private final Map<Item, Integer> collected = new HashMap<>();
        private int cursor;

        private ScanTask(ServerPlayer player, int originX, int originY, int originZ,
                         ResourceKey<Level> dimension, TargetSpec target, int side, int fortuneLevel) {
            this.player = player;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.dimension = dimension;
            this.target = target;
            this.side = side;
            this.totalPositions = ScanLayout.totalPositions(side);
            this.fortuneLevel = Math.max(0, fortuneLevel);
        }

        private boolean tick(ServerLevel level) {
            if (level.dimension() != dimension) return false;
            if (player.isRemoved() || player.hasDisconnected() || player.level() != level) {
                preserveCollected();
                return true;
            }

            scanAndBreak(level);
            if (cursor >= totalPositions) {
                dropCollected(level);
                collected.clear();
                return true;
            }
            return false;
        }

        private void scanAndBreak(ServerLevel level) {
            int scanned = 0;
            int broken = 0;
            while (cursor < totalPositions && scanned < POSITIONS_PER_TICK && broken < BLOCKS_PER_TICK) {
                int index = cursor++;
                scanned++;
                scanPos.set(originX + ScanLayout.offsetX(index, side), originY + ScanLayout.offsetY(index, side),
                        originZ + ScanLayout.offsetZ(index, side));

                if (!level.isInWorldBounds(scanPos) || !level.hasChunkAt(scanPos)) continue;
                BlockState state = level.getBlockState(scanPos);
                Item reward = target.rewardFor(state.getBlock());
                if (reward == null) continue;
                if (level.destroyBlock(scanPos, false, player)) {
                    int amount = FortuneMath.randomMultiplier(fortuneLevel);
                    collected.merge(reward, amount, Integer::sum);
                    broken++;
                }
            }
        }

        private void preserveCollected() {
            if (collected.isEmpty()) return;
            for (var entry : collected.entrySet()) {
                Item reward = entry.getKey();
                int remaining = entry.getValue();
                int maxStack = reward.getDefaultMaxStackSize();
                while (remaining > 0) {
                    int amount = RewardMath.nextStackSize(remaining, maxStack);
                    ItemStack stack = new ItemStack(reward, amount);
                    player.getInventory().add(stack);
                    if (!stack.isEmpty()) MultiPageInventoryCompat.insertOverflow(player, stack);
                    if (!stack.isEmpty() && player.level() instanceof ServerLevel dropLevel) {
                        ItemEntity entity = new ItemEntity(dropLevel, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy());
                        entity.setDefaultPickUpDelay();
                        dropLevel.addFreshEntity(entity);
                    }
                    remaining -= amount;
                }
            }
            collected.clear();
        }

        private void dropCollected(ServerLevel level) {
            if (collected.isEmpty()) return;
            var look = player.getLookAngle();
            double x = player.getX() + look.x;
            double y = player.getY() + 0.5;
            double z = player.getZ() + look.z;

            for (var entry : collected.entrySet()) {
                Item reward = entry.getKey();
                int remaining = entry.getValue();
                int maxStack = reward.getDefaultMaxStackSize();
                while (remaining > 0) {
                    int amount = RewardMath.nextStackSize(remaining, maxStack);
                    ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(reward, amount));
                    entity.setDefaultPickUpDelay();
                    level.addFreshEntity(entity);
                    remaining -= amount;
                }
            }
        }
    }

    private record TargetSpec(Map<Block, Item> rewards, Item fallbackReward) {
        static TargetSpec sameReward(Set<Block> blocks, Item reward) {
            Map<Block, Item> rewards = new HashMap<>();
            for (Block block : blocks) rewards.put(block, reward);
            return new TargetSpec(Map.copyOf(rewards), reward);
        }

        Item rewardFor(Block block) {
            Item reward = rewards.get(block);
            if (reward == null) return null;
            return reward == Items.AIR ? fallbackReward : reward;
        }
    }
}

final class ScanLayout {
    private ScanLayout() {}

    static boolean isSupportedSide(int side) {
        return side == 8 || side == 16 || side == 64 || side == 128;
    }

    static int totalPositions(int side) {
        return side * side * side;
    }

    static int offsetX(int index, int side) {
        return (index % side) - side / 2;
    }

    static int offsetZ(int index, int side) {
        return ((index / side) % side) - side / 2;
    }

    static int offsetY(int index, int side) {
        return (index / (side * side)) - side / 2;
    }
}

final class RewardMath {
    private RewardMath() {}
    static int nextStackSize(int remaining, int maxStack) {
        if (remaining <= 0 || maxStack <= 0) return 0;
        return Math.min(remaining, maxStack);
    }
}

final class FortuneMath {
    private FortuneMath() {}

    static int randomMultiplier(int fortuneLevel) {
        if (fortuneLevel <= 0) return 1;
        int roll = ThreadLocalRandom.current().nextInt(fortuneLevel + 2);
        return multiplierForRoll(fortuneLevel, roll);
    }

    static int multiplierForRoll(int fortuneLevel, int roll) {
        if (fortuneLevel <= 0) return 1;
        int clampedRoll = Math.max(0, Math.min(roll, fortuneLevel + 1));
        return Math.max(0, clampedRoll - 1) + 1;
    }
}
