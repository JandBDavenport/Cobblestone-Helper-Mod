package com.jandbdavenport.cobblestonehelper.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Diagnostic tool for logging player disconnects and kicks.
 * Tracks both hard disconnects (to multiplayer menu) and soft kicks (transfers to hub server).
 * In multi-server systems, soft kicks happen without explicit disconnect - just world/server changes.
 */
public class KickLoggerManager {
	private static String lastServerName = null;
	private static String lastDimensionName = null;
	private static BlockPos lastPlayerPos = null;
	private static boolean wasContainerOpen = false;
	private static int ticksSinceContainerClosed = Integer.MAX_VALUE; // Track intentional teleports via containers
	private static final int CONTAINER_TELEPORT_GRACE_PERIOD = 20; // 1 second (20 ticks) after container close to ignore dimension changes

	public static void init() {
		// Listen to disconnect events (hard disconnect to multiplayer menu)
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			onDisconnect(handler, client);
		});

		// Listen to chat messages for kick indicators (soft kicks to hub)
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			onChatMessage(message);
		});

		// Monitor for world changes (soft kick transfers)
		// Also track container open/close for intentional teleports
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			checkForContainerChange(client);
			checkForWorldChange(client);
		});

		System.out.println("[KickLogger] ✓ Initialized - monitoring disconnects, transfers, and kick messages");
		System.out.println("[KickLogger] Note: Dimension changes within 1 second of closing a container are ignored (legitimate teleports)");
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

	private static void checkForContainerChange(MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		// Check if player has an open container (GUI that's not the normal inventory)
		boolean containerOpen = client.player.currentScreenHandler != null &&
			!(client.player.currentScreenHandler instanceof net.minecraft.screen.PlayerScreenHandler);

		// If container was open and is now closed, start grace period for teleport
		if (wasContainerOpen && !containerOpen) {
			ticksSinceContainerClosed = 0; // Reset grace period counter
		}

		// If we're in grace period, increment counter
		if (ticksSinceContainerClosed < CONTAINER_TELEPORT_GRACE_PERIOD) {
			ticksSinceContainerClosed++;
		}

		wasContainerOpen = containerOpen;
	}

	private static void checkForWorldChange(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			return;
		}

		String currentServer = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().name : "Unknown";
		String currentDimension = client.world.getRegistryKey().getValue().toString();
		BlockPos currentPos = client.player.getBlockPos();

		// Detect dimension/world change (soft kick indicator in multi-server systems)
		if (lastDimensionName != null && !lastDimensionName.equals(currentDimension)) {
			// Skip logging if this is likely a legitimate teleport via container
			if (ticksSinceContainerClosed < CONTAINER_TELEPORT_GRACE_PERIOD) {
				System.out.println("[KickLogger] Dimension change detected within grace period - treating as legitimate teleport");
				ticksSinceContainerClosed = Integer.MAX_VALUE; // Reset grace period
			} else {
				logWorldChange(currentServer, lastDimensionName, currentDimension, currentPos);
			}
		}

		// Detect server change
		if (lastServerName != null && !lastServerName.equals(currentServer)) {
			logServerChange(lastServerName, currentServer);
		}

		lastServerName = currentServer;
		lastDimensionName = currentDimension;
		lastPlayerPos = currentPos;
	}

	private static void logWorldChange(String currentServer, String oldDimension, String newDimension, BlockPos newPos) {
		System.out.println("[KickLogger] ========== WORLD/DIMENSION CHANGE DETECTED (Possible Soft Kick) ==========");
		System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
		System.out.println("[KickLogger] Server: " + currentServer);
		System.out.println("[KickLogger] Old Dimension: " + oldDimension);
		System.out.println("[KickLogger] New Dimension: " + newDimension);
		System.out.println("[KickLogger] New Position: X=" + newPos.getX() + " Y=" + newPos.getY() + " Z=" + newPos.getZ());
		System.out.println("[KickLogger] Note: World change without disconnect suggests transfer to hub/another server");
		System.out.println("[KickLogger] ========== END WORLD CHANGE LOG ==========");
	}

	private static void logServerChange(String oldServer, String newServer) {
		System.out.println("[KickLogger] ========== SERVER CHANGE DETECTED ==========");
		System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
		System.out.println("[KickLogger] Old Server: " + oldServer);
		System.out.println("[KickLogger] New Server: " + newServer);
		System.out.println("[KickLogger] ========== END SERVER CHANGE LOG ==========");
	}

	private static void onChatMessage(Text message) {
		String messageText = message.getString();

		// Detect kick-related messages (case-insensitive)
		String lowerMessage = messageText.toLowerCase();
		if (lowerMessage.contains("kicked") || lowerMessage.contains("disconnected") ||
			lowerMessage.contains("removed from") || lowerMessage.contains("banned") ||
			lowerMessage.contains("connection closed") || lowerMessage.contains("you were") ||
			lowerMessage.contains("transferred")) {

			System.out.println("[KickLogger] ========== KICK/TRANSFER MESSAGE DETECTED ==========");
			System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
			System.out.println("[KickLogger] Message: " + messageText);
			System.out.println("[KickLogger] Raw (with formatting): " + message);
			System.out.println("[KickLogger] ========== END KICK MESSAGE LOG ==========");
		}
	}
}
