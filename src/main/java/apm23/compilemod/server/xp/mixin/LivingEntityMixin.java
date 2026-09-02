package apm23.compilemod.server.xp.mixin;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final ThreadLocal<Context> COMPILE_MOD_CONTEXT = new ThreadLocal<>();

    @Inject(method = "dropExperience", at = @At("HEAD"))
    private void compileMod$captureContext(ServerLevel level, Entity killer, CallbackInfo ci) {
        COMPILE_MOD_CONTEXT.set(new Context((LivingEntity) (Object) this, level, killer));
    }

    @ModifyArg(
            method = "dropExperience",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
            index = 2
    )
    private int compileMod$boostTaczXp(int original) {
        Context context = COMPILE_MOD_CONTEXT.get();
        if (context == null || original <= 0 || context.level.getDifficulty() != Difficulty.HARD) return original;
        if (!compileMod$isTaczKill(context.victim, context.killer)) return original;

        double multiplier = compileMod$isBoss(context.victim) ? 1.5D : 2.5D;
        return Math.max(original, (int) Math.round(original * multiplier));
    }

    @Inject(method = "dropExperience", at = @At("RETURN"))
    private void compileMod$clearContext(ServerLevel level, Entity killer, CallbackInfo ci) {
        COMPILE_MOD_CONTEXT.remove();
    }

    private static boolean compileMod$isTaczKill(LivingEntity victim, Entity killer) {
        DamageSource source = victim.getLastDamageSource();
        if (source == null) return false;
        Entity direct = source.getDirectEntity();
        if (compileMod$isTaczEntity(direct)) return true;
        if (compileMod$isTaczEntity(source.getEntity())) return true;
        if (killer != null && compileMod$isTaczEntity(killer)) return true;
        return source.toString().toLowerCase(Locale.ROOT).contains("tacz");
    }

    private static boolean compileMod$isTaczEntity(Entity entity) {
        if (entity == null) return false;
        return entity.getClass().getName().startsWith("com.tacz.");
    }

    private static boolean compileMod$isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    private record Context(LivingEntity victim, ServerLevel level, Entity killer) {}
}
