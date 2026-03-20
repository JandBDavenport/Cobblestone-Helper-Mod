package com.jandbdavenport.cobblestonehelper.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;

/**
 * Tracks entity loading events and applies hiding rules to newly spawned entities.
 * This ensures hidden players and pets are marked as invisible immediately upon spawning.
 */
public class EntityTracker {
	public static void init() {
		// When an entity is added to the world, immediately set invisible if needed
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!PlayerHidingManager.hideOtherPlayers) {
				return;
			}

			// Hide non-local players
			if (entity instanceof AbstractClientPlayerEntity
					&& entity != MinecraftClient.getInstance().player) {
				((AbstractClientPlayerEntity) entity).setInvisible(true);
				return;
			}

			// Hide armor stands (potential pet carriers)
			if (entity instanceof ArmorStandEntity) {
				entity.setInvisible(true);
				return;
			}

			// Hide display entities (text displays, item displays - often used for nameplate data)
			if (entity instanceof DisplayEntity) {
				entity.setInvisible(true);
				return;
			}

			// Hide pet entities by checking their custom name
			Text customName = entity.getCustomName();
			if (customName != null) {
				String nameStr = customName.getString();
				// Pet names contain "Lv." - check for this pattern
				if (nameStr.contains("Lv.")) {
					entity.setInvisible(true);
				}
			}
		});
	}
}
