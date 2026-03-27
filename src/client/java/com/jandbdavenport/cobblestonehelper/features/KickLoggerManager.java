package com.jandbdavenport.cobblestonehelper.features;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/**
 * Diagnostic tool for logging player disconnects and kicks.
 *
 * Primary mechanism: DisconnectPacketMixin intercepts DisconnectS2CPacket at the exact moment
 * it arrives from the server, capturing the kick reason and full game state.
 *
 * Fallback mechanism: ClientPlayConnectionEvents.DISCONNECT event for raw TCP drops
 * (e.g. server crash or network timeout).
 *
 * In multi-server proxy systems (BungeeCord/Velocity), a kick sends DisconnectS2CPacket
 * from the backend server, then the proxy reconnects the player to hub. The mixin fires
 * before any reconnection, so the kick reason and game state are both valid.
 */
public class KickLoggerManager {

	public static void init() {
		// Fallback listener for hard disconnects (raw TCP close, server crash, etc)
		// The mixin is the primary detection mechanism
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			onDisconnect(handler, client);
		});

		System.out.println("[KickLogger] ✓ Initialized - packet mixin is primary, DISCONNECT event is fallback");
	}

	/**
	 * Called by DisconnectPacketMixin when a DisconnectS2CPacket arrives from the server.
	 * This is the ground-truth kick event at the network level.
	 * All game state (player, world, position) is still valid at this point.
	 *
	 * @param reason The plain-text kick reason from the DisconnectS2CPacket
	 */
	public static void notifyKickPacketReceived(String reason) {
		MinecraftClient client = MinecraftClient.getInstance();

		System.out.println("[KickLogger] ========== DISCONNECT PACKET RECEIVED ==========");
		System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
		System.out.println("[KickLogger] Kick Reason: " + reason);

		// Server info
		if (client.getCurrentServerEntry() != null) {
			System.out.println("[KickLogger] Server: " + client.getCurrentServerEntry().name);
			System.out.println("[KickLogger] Address: " + client.getCurrentServerEntry().address);
		} else {
			System.out.println("[KickLogger] Server: Unknown");
		}

		// Player identity and health
		if (client.player != null) {
			System.out.println("[KickLogger] Player: " + client.player.getDisplayName().getString());
			System.out.println("[KickLogger] UUID: " + client.player.getUuid());
			System.out.println("[KickLogger] Health: " + client.player.getHealth() + "/" + client.player.getMaxHealth());
		} else {
			System.out.println("[KickLogger] Player: Unknown");
		}

		// World, dimension, and position
		if (client.world != null && client.player != null) {
			System.out.println("[KickLogger] Dimension: " + client.world.getRegistryKey().getValue());
			System.out.println("[KickLogger] Position: X=" + (int)client.player.getX()
				+ " Y=" + (int)client.player.getY()
				+ " Z=" + (int)client.player.getZ());
			// Best-effort biome lookup
			client.world.getBiome(client.player.getBlockPos()).getKey()
				.ifPresentOrElse(
					key -> System.out.println("[KickLogger] Biome: " + key.getValue()),
					()  -> System.out.println("[KickLogger] Biome: Unknown")
				);
		} else {
			System.out.println("[KickLogger] World/Position: Unknown");
		}

		// Open screen (GUI) at moment of packet
		if (client.currentScreen != null) {
			System.out.println("[KickLogger] Open Screen: " + client.currentScreen.getClass().getSimpleName());
		} else {
			System.out.println("[KickLogger] Open Screen: None");
		}

		System.out.println("[KickLogger] ========== END DISCONNECT PACKET LOG ==========");
	}

	/**
	 * Fallback handler for hard disconnects without a prior DisconnectS2CPacket.
	 * Fires when ClientPlayConnectionEvents.DISCONNECT is triggered (raw TCP close, etc).
	 */
	private static void onDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		System.out.println("[KickLogger] ========== HARD DISCONNECT (fallback) ==========");
		System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());

		// Server info
		if (handler != null && handler.getServerInfo() != null) {
			System.out.println("[KickLogger] Server: " + handler.getServerInfo().name);
			System.out.println("[KickLogger] Address: " + handler.getServerInfo().address);
		}

		// Player info
		if (client.player != null) {
			System.out.println("[KickLogger] Player: " + client.player.getDisplayName().getString());
			System.out.println("[KickLogger] Health: " + client.player.getHealth() + "/" + client.player.getMaxHealth());
		}

		// World and position
		if (client.world != null && client.player != null) {
			System.out.println("[KickLogger] Dimension: " + client.world.getRegistryKey().getValue());
			System.out.println("[KickLogger] Position: X=" + (int)client.player.getX()
				+ " Y=" + (int)client.player.getY()
				+ " Z=" + (int)client.player.getZ());
		}

		System.out.println("[KickLogger] ========== END HARD DISCONNECT ==========");
	}
}
