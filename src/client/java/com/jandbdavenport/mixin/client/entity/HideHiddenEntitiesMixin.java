package com.jandbdavenport.mixin.client.entity;

import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces invisibility for hidden players and pets during render state creation.
 * This is the primary point where invisible entities are suppressed from rendering.
 */
@Mixin(EntityRenderer.class)
public class HideHiddenEntitiesMixin {
	@Inject(method = "updateRenderState", at = @At("TAIL"))
	private void forceInvisibilityInRenderState(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
		if (!PlayerHidingManager.hideOtherPlayers) {
			return;
		}

		boolean shouldHide = false;

		// Check if this is a non-local player
		if (entity instanceof AbstractClientPlayerEntity
				&& entity != MinecraftClient.getInstance().player) {
			shouldHide = true;
		}

		// Check if this is an armor stand (potential pet carrier)
		if (entity instanceof ArmorStandEntity) {
			shouldHide = true;
		}

		// Check if this is a display entity (text displays, item displays)
		if (entity instanceof DisplayEntity) {
			shouldHide = true;
		}

		// Check if entity name contains level indicator
		Text customName = entity.getCustomName();
		if (customName != null) {
			String nameStr = customName.getString();
			if (nameStr.contains("Lv.") && nameStr.contains("[")) {
				shouldHide = true;
			}
		}

		// If we should hide this entity, set invisible and clear the display name
		if (shouldHide) {
			state.invisible = true;
			// Also null out the display name to prevent nameplate rendering
			state.displayName = null;
		}
	}
}
