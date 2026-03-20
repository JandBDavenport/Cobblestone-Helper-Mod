package com.jandbdavenport.mixin.client.particles;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses particle spawning from hidden players and pets.
 * This includes walking particles and custom pet particles.
 */
@Mixin(ParticleManager.class)
public class SuppressHiddenEntityParticlesMixin {
	@Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
	private void suppressParticlesFromHiddenPlayers(ParticleEffect effect, double x, double y, double z,
													  double velocityX, double velocityY, double velocityZ,
													  CallbackInfoReturnable<?> cir) {
		if (!PlayerHidingManager.hideOtherPlayers) {
			return;
		}

		// Suppress particles from hidden players and pets
		if (MinecraftClient.getInstance().world != null) {
			for (Entity entity : MinecraftClient.getInstance().world.getEntities()) {
				// Check if this is a hidden player
				if (entity instanceof AbstractClientPlayerEntity
						&& entity != MinecraftClient.getInstance().player
						&& entity.isInvisible()) {
					// Check if particle is close to the player position (walking particles)
					double distance = Math.sqrt(Math.pow(x - entity.getX(), 2) +
											   Math.pow(z - entity.getZ(), 2));
					if (distance < 2.0) { // Walking particles spawn within ~2 blocks of player
						cir.setReturnValue(null);
						return;
					}
				}

				// Check if this is a hidden armor stand (pet) with particles
				if (entity instanceof ArmorStandEntity
						&& entity.isInvisible()) {
					// Check if particle is close to armor stand (pet particles)
					double distance = Math.sqrt(Math.pow(x - entity.getX(), 2) +
											   Math.pow(y - entity.getY(), 2) +
											   Math.pow(z - entity.getZ(), 2));
					if (distance < 3.0) { // Pet particles spawn within ~3 blocks
						cir.setReturnValue(null);
						return;
					}
				}
			}
		}
	}

	@Inject(method = "addEmitter", at = @At("HEAD"), cancellable = true)
	private void suppressEmitterForHiddenPlayers(Entity entity, ParticleEffect effect, CallbackInfo ci) {
		// Suppress particle emitters (walking particles) for hidden entities
		if (!PlayerHidingManager.hideOtherPlayers) {
			return;
		}

		if (entity instanceof AbstractClientPlayerEntity
				&& entity != MinecraftClient.getInstance().player
				&& entity.isInvisible()) {
			ci.cancel();
			return;
		}

		if (entity instanceof ArmorStandEntity && entity.isInvisible()) {
			ci.cancel();
		}
	}
}
