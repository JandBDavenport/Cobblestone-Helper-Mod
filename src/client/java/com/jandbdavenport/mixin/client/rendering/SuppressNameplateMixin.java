package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses nameplate rendering for invisible entities.
 */
@Mixin(LivingEntityRenderer.class)
public class SuppressNameplateMixin {
	@Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
	private void suppressNameplateForHiddenEntities(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
		// Don't show nameplate for invisible entities when hideOtherPlayers is enabled
		if (PlayerHidingManager.hideOtherPlayers && state.invisible) {
			cir.setReturnValue(false);
		}
	}
}
