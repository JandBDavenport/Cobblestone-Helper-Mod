package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) return;

		Vec3d cameraPos = client.player.getEyePos();
		Vec3d entityCenter = new Vec3d(entity.getX() + 0.5, entity.getY() + entity.getHeight() / 2.0, entity.getZ() + 0.5);

		BlockHitResult hit = client.world.raycast(new RaycastContext(
			cameraPos, entityCenter,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			entity
		));

		if (hit.getType() == HitResult.Type.MISS) {
			state.outlineColor = ModConfig.itemGlowColor;
		}
		// If blocked by terrain, leave state.outlineColor = 0 (no outline)
	}
}
