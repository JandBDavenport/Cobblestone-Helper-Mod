package com.jandbdavenport.mixin.client.rendering;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses feature rendering (armor, held items, etc.) for invisible players.
 */
@Mixin(PlayerEntityRenderer.class)
public class SuppressPlayerFeaturesMixin {
	@Inject(method = "shouldRenderFeatures", at = @At("HEAD"), cancellable = true)
	private void suppressFeaturesIfInvisible(PlayerEntityRenderState state,
											  CallbackInfoReturnable<Boolean> cir) {
		// Only suppress features if the entity is invisible
		if (state.invisible) {
			cir.setReturnValue(false);
		}
	}
}
