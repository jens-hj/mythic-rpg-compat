package dev.jensh.mythicrpgcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jensh.mythicrpgcompat.MythicParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.trique.mythicupgrades.handler.NecoiumShareHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scales the particles of Mythic Upgrades' Necoium share effect ({@link MythicParticles}). */
@Mixin(NecoiumShareHandler.class)
public abstract class NecoiumParticlesMixin {
	@WrapOperation(method = "spawnParticlesBetween", at = @At(value = "INVOKE", target = MythicParticles.SEND_PARTICLES))
	private static int mythicRpgCompat$scaleNecoium(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
													 double dx, double dy, double dz, double speed, Operation<Integer> original) {
		return MythicParticles.send(MythicParticles.Group.NECOIUM, level, particle, x, y, z, count, dx, dy, dz, speed, original);
	}
}
