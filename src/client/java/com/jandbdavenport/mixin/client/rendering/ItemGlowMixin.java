package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import com.jandbdavenport.cobblestonehelper.features.ItemGlowManager;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemGlowMixin {
	@Inject(
		method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
		at = @At("TAIL")
	)
	private void setItemGlowIfVisible(ItemEntity entity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
		if (!ModConfig.itemGlowEnabled) return;

		if (ItemGlowManager.getVisibility(entity.getUuid())) {
			state.outlineColor = ModConfig.itemGlowColor;
		}
		// If blocked by terrain, leave state.outlineColor = 0 (no outline)
	}
}
