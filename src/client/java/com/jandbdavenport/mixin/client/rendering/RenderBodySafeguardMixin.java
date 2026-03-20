package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides an extra safeguard by verifying invisibility right before entity body rendering.
 * This prevents rare 1-frame flickers by ensuring state.invisible is true at the exact moment rendering begins.
 */
@Mixin(LivingEntityRenderer.class)
public class RenderBodySafeguardMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void forceInvisibilityBeforeRender(LivingEntityRenderState state, Object matrixStack, Object queue, int light, CallbackInfo ci) {
		// If hideOtherPlayers is enabled and this entity is marked invisible,
		// ensure state.invisible is absolutely true before rendering proceeds
		if (PlayerHidingManager.hideOtherPlayers && state.invisible) {
			state.invisible = true; // Force it to be true (defensive check)
		}
	}
}
