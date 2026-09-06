package apm23.compilemod.dual.godvillager.gametest;

import com.anjas.godvillagers.GodHorseRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

@SuppressWarnings("UnstableApiUsage")
public final class GodHorseFocusedClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getServer().runOnServer(server -> {
                var level = server.overworld();
                BlockPos pos = new BlockPos(8, 100, 8);

                var source = server.createCommandSourceStack()
                        .withLevel(level)
                        .withPosition(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D))
                        .withSuppressedOutput();
                int result = server.getCommands().performPrefixedCommand(source,
                        "summon minecraft:skeleton_horse 8 100 8 {Tame:1b,Temper:100,PersistenceRequired:1b,Tags:[\"godvillagers_god_horse\"]}");
                assertTrue(result > 0, "God Horse summon command failed");

                SkeletonHorse horse = level.getEntitiesOfClass(
                                SkeletonHorse.class,
                                new AABB(7, 99, 7, 10, 103, 10),
                                h -> h.entityTags().contains("godvillagers_god_horse"))
                        .stream().findFirst()
                        .orElseThrow(() -> new AssertionError("God Horse entity was not spawned"));

                invoke("promote", new Class<?>[]{SkeletonHorse.class}, horse);

                assertNear(base(horse, Attributes.MAX_HEALTH), 80.0D, 0.01D, "max health drifted");
                assertNear(base(horse, Attributes.MOVEMENT_SPEED), 0.45D, 0.01D, "movement speed drifted");
                assertNear(base(horse, Attributes.JUMP_STRENGTH), 1.8D, 0.01D, "jump strength drifted");
                assertNear(base(horse, Attributes.FALL_DAMAGE_MULTIPLIER), 0.0D, 0.01D, "fall multiplier drifted");
                assertTrue(horse.getHealth() >= 80.0F, "God Horse health was not promoted to 80");
                assertTrue(horse.entityTags().contains("godvillagers_god_horse_initialized_clean"),
                        "God Horse initialization tag missing");

                horse.igniteForSeconds(5.0F);
                invoke("tickHorse", new Class<?>[]{SkeletonHorse.class}, horse);
                assertTrue(!horse.isOnFire(), "God Horse tick no longer clears fire");

                // Damage policy is frozen: fall, fire, and explosion are blocked for recognized God Horses.
                boolean fallAllowed = invokeBoolean("allowDamage",
                        new Class<?>[]{net.minecraft.world.entity.LivingEntity.class, net.minecraft.world.damagesource.DamageSource.class, float.class},
                        horse, level.damageSources().fall(), 10.0F);
                assertTrue(!fallAllowed, "God Horse fall damage protection drifted");

                boolean fireAllowed = invokeBoolean("allowDamage",
                        new Class<?>[]{net.minecraft.world.entity.LivingEntity.class, net.minecraft.world.damagesource.DamageSource.class, float.class},
                        horse, level.damageSources().onFire(), 10.0F);
                assertTrue(!fireAllowed, "God Horse fire damage protection drifted");
            });
        }
    }

    private static double base(SkeletonHorse horse, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        var instance = horse.getAttribute(attribute);
        if (instance == null) throw new AssertionError("Missing horse attribute: " + attribute);
        return instance.getBaseValue();
    }

    private static void invoke(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = GodHorseRuntime.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not invoke frozen God Horse runtime method: " + name, e);
        }
    }

    private static boolean invokeBoolean(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = GodHorseRuntime.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (boolean) method.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not invoke frozen God Horse runtime method: " + name, e);
        }
    }

    private static void assertNear(double actual, double expected, double epsilon, String message) {
        if (Math.abs(actual - expected) > epsilon) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
