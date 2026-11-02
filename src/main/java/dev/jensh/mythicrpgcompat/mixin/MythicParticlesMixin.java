package dev.jensh.mythicrpgcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jensh.mythicrpgcompat.MythicParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales the particles of Mythic Upgrades' gem effects ({@link MythicParticles}). Mythic spawns
 * them from static helper methods its LivingEntity mixin merges in, one or more per gem; this
 * mixin applies after it (higher priority) and wraps their {@code sendParticles} calls.
 */
@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class MythicParticlesMixin {
	@WrapOperation(method = {"emitArcaneAuraWaveRing"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleAmetrine(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.AMETRINE, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}

	@WrapOperation(method = {"emitAquamarineHitParticles", "emitIceBombWaveRing", "emitIceFreezeOrbitParticles", "emitIceShieldStreamParticles"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleAquamarine(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.AQUAMARINE, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}

	@WrapOperation(method = {"emitCitrineChainParticles", "emitStaticFieldParticles"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleCitrine(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.CITRINE, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}

	@WrapOperation(method = {"applyJadeTrail", "emitJadeAuraAmbientParticles", "emitJadeLingerParticles"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleJade(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.JADE, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}

	@WrapOperation(method = {"emitLethalIncubationWaveRing", "emitMiasmaWaveRing"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scalePeridot(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.PERIDOT, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}

	@WrapOperation(method = {"emitBloodThirstParticles", "emitLifestealParticles"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleRuby(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.RUBY, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}

	@WrapOperation(method = {"emitTopazWaveRing"}, at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleTopaz(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.TOPAZ, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}
}
