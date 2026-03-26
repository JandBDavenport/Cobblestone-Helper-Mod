package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
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
	@Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
	private void forceInvisibilityBeforeRender(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue renderQueue, CameraRenderState cameraState, CallbackInfo ci) {
		if (PlayerHidingManager.hideOtherPlayers && state.invisible) {
			state.invisible = true;
		}
	}
}
