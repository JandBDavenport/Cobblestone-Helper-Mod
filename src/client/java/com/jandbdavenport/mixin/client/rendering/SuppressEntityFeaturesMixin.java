package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses feature rendering (armor, equipment, etc.) for invisible living entities.
 */
@Mixin(LivingEntityRenderer.class)
public class SuppressEntityFeaturesMixin {
	@Inject(method = "shouldRenderFeatures", at = @At("HEAD"), cancellable = true)
	private void suppressArmorStandFeaturesIfHidden(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
		// If this is an invisible entity being rendered by LivingEntityRenderer, don't render features
		// This covers invisible armor stands (pets) that have equipment
		if (PlayerHidingManager.hideOtherPlayers && state.invisible) {
			cir.setReturnValue(false);
		}
	}
}
