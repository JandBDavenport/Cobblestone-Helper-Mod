package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses nameplate rendering for hidden players.
 */
@Mixin(LivingEntityRenderer.class)
public class SuppressNameplateMixin {
	@Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
	private void suppressNameplateForHiddenEntities(LivingEntity entity, double cameraDistance, CallbackInfoReturnable<Boolean> cir) {
		if (PlayerHidingManager.hideOtherPlayers
				&& entity instanceof AbstractClientPlayerEntity
				&& entity != MinecraftClient.getInstance().player) {
			cir.setReturnValue(false);
		}
	}
}
