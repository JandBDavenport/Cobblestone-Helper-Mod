package com.jandbdavenport.cobblestonehelper.features;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

/**
 * Diagnostic tool for logging player disconnects and kicks.
 * Helps identify why players are being kicked from the server.
 */
public class KickLoggerManager {

	public static void init() {
		// Listen to disconnect events
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			onDisconnect(handler, client);
		});

		System.out.println("[KickLogger] ✓ Initialized - monitoring disconnect events");
	}

	private static void onDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		// Log basic disconnect info
		System.out.println("[KickLogger] ========== PLAYER DISCONNECTED ==========");
		System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());

		// Log server info
		if (handler != null && handler.getServerInfo() != null) {
			System.out.println("[KickLogger] Server: " + handler.getServerInfo().name);
			System.out.println("[KickLogger] Server Address: " + handler.getServerInfo().address);
		} else {
			System.out.println("[KickLogger] Server: Unknown (null handler)");
		}

		// Log player info
		if (client.player != null) {
			System.out.println("[KickLogger] Player: " + client.player.getDisplayName().getString());
			System.out.println("[KickLogger] Player UUID: " + client.player.getUuid());
			System.out.println("[KickLogger] Health: " + client.player.getHealth() + "/" + client.player.getMaxHealth());
		} else {
			System.out.println("[KickLogger] Player: Unknown (null player)");
		}

		// Log world/dimension info
		if (client.world != null) {
			System.out.println("[KickLogger] Dimension: " + client.world.getRegistryKey().getValue());
			System.out.println("[KickLogger] Position: X=" + (int)client.player.getX() + " Y=" + (int)client.player.getY() + " Z=" + (int)client.player.getZ());
			System.out.println("[KickLogger] Biome: " + client.world.getBiome(client.player.getBlockPos()).getKey().orElse(null));
		} else {
			System.out.println("[KickLogger] Dimension: Unknown (null world)");
		}

		// Log network state info
		System.out.println("[KickLogger] Connection State: " + (handler != null ? "Active" : "Null"));

		// Log disconnect reason if available (from screen)
		if (client.currentScreen != null && client.currentScreen.getTitle() != null) {
			String screenTitle = client.currentScreen.getTitle().getString();
			System.out.println("[KickLogger] Current Screen: " + client.currentScreen.getClass().getSimpleName());
			System.out.println("[KickLogger] Screen Title: " + screenTitle);

			// If it's a disconnect screen, log the reason
			if (client.currentScreen.getClass().getSimpleName().contains("Disconnect")) {
				try {
					// Try to extract disconnect reason from screen
					java.lang.reflect.Field field = client.currentScreen.getClass().getDeclaredField("reason");
					field.setAccessible(true);
					Text reason = (Text) field.get(client.currentScreen);
					if (reason != null) {
						System.out.println("[KickLogger] Disconnect Reason: " + reason.getString());
					}
				} catch (Exception e) {
					System.out.println("[KickLogger] Could not extract disconnect reason (reflection failed)");
				}
			}
		}

		// Log recent chat messages for context
		System.out.println("[KickLogger] ========== RECENT CHAT CONTEXT ==========");
		if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
			try {
				// Try to access chat messages
				java.lang.reflect.Field field = client.inGameHud.getChatHud().getClass().getDeclaredField("messages");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				java.util.List<net.minecraft.client.gui.hud.ChatHudLine> messages =
					(java.util.List<net.minecraft.client.gui.hud.ChatHudLine>) field.get(client.inGameHud.getChatHud());

				if (messages != null && !messages.isEmpty()) {
					System.out.println("[KickLogger] Last 10 chat messages:");
					int count = 0;
					for (int i = messages.size() - 1; i >= 0 && count < 10; i--, count++) {
						net.minecraft.client.gui.hud.ChatHudLine line = messages.get(i);
						System.out.println("[KickLogger]   " + line.content().getString());
					}
				} else {
					System.out.println("[KickLogger] No chat messages available");
				}
			} catch (Exception e) {
				System.out.println("[KickLogger] Could not retrieve chat messages: " + e.getMessage());
			}
		} else {
			System.out.println("[KickLogger] Chat HUD not available");
		}

		// Log open containers for context
		System.out.println("[KickLogger] ========== CONTEXT INFO ==========");
		if (client.currentScreen != null) {
			System.out.println("[KickLogger] Current Screen Class: " + client.currentScreen.getClass().getName());
		} else {
			System.out.println("[KickLogger] No current screen");
		}

		// Log player inventory state
		if (client.player != null && client.player.currentScreenHandler != null) {
			System.out.println("[KickLogger] Container Open: " + client.player.currentScreenHandler.getClass().getSimpleName());
		}

		System.out.println("[KickLogger] ========== END DISCONNECT LOG ==========");
	}
}
