package com.jandbdavenport.cobblestonehelper.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

/**
 * Manages the player hiding feature.
 * Handles toggling, state management, and synchronization of player visibility.
 */
public class PlayerHidingManager {
	public static boolean hideOtherPlayers = false;

	/**
	 * Initialize the player hiding feature with tick-based visibility updates.
	 */
	public static void init(KeyBinding hidePlayersKey) {
		// Initialize entity tracker to catch newly added players
		EntityTracker.init();

		// Update visibility at START of tick (before rendering)
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			updatePlayerVisibility(client);
		});

		// Also update at END of tick to catch any late server updates
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Check if keybind was pressed
			while (hidePlayersKey.wasPressed()) {
				toggleHidePlayers(client);
			}
			// Update visibility after keybind processing
			updatePlayerVisibility(client);
		});
	}

	/**
	 * Update the visibility state of all non-local players in the world.
	 */
	private static void updatePlayerVisibility(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null) return;

		for (Entity entity : world.getEntities()) {
			if (entity instanceof AbstractClientPlayerEntity && entity != client.player) {
				AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
				player.setInvisible(hideOtherPlayers);
			}
		}
	}

	/**
	 * Toggle player hiding on or off and show a message to the player.
	 */
	public static void toggleHidePlayers(MinecraftClient client) {
		hideOtherPlayers = !hideOtherPlayers;
		if (client.inGameHud != null) {
			client.inGameHud.setOverlayMessage(
				Text.literal(hideOtherPlayers ? "Players Hidden" : "Players Visible"),
				false
			);
		}
	}
}
