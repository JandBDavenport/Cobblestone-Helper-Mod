package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses glow outline rendering for hidden players.
 */
@Mixin(MinecraftClient.class)
public class SuppressGlowOutlineMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void suppressOutlineForHiddenPlayers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (PlayerHidingManager.hideOtherPlayers
				&& entity instanceof AbstractClientPlayerEntity
				&& entity != MinecraftClient.getInstance().player) {
			cir.setReturnValue(false);
		}
	}
}
