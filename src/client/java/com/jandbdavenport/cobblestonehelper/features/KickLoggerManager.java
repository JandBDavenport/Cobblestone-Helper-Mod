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
 * Secondary mechanism: NetworkConnectionMixin intercepts Netty-level exceptions
 * (timeout, compression error, generic error) before vanilla's disconnect handler.
 *
 * Fallback mechanism: ClientPlayConnectionEvents.DISCONNECT event for raw TCP drops
 * (e.g. server crash or network timeout).
 *
 * In multi-server proxy systems (BungeeCord/Velocity), a kick sends DisconnectS2CPacket
 * from the backend server, then the proxy reconnects the player to hub. The mixin fires
 * before any reconnection, so the kick reason and game state are both valid.
 */
public class KickLoggerManager {

	/**
	 * Categorizes the type of disconnect that occurred.
	 * Used to distinguish intentional kicks from network errors/timeouts.
	 */
	public enum DisconnectType {
		NONE,                  // No disconnect yet
		INTENTIONAL_KICK,      // DisconnectS2CPacket arrived from server
		TIMEOUT,               // ReadTimeoutException from Netty
		COMPRESSION_ERROR,     // DecoderException — compression/inflation error
		GENERIC_ERROR,         // Other network exception (not timeout, not compression)
		RAW_TCP_CLOSE          // channelInactive without prior exception or packet
	}

	/**
	 * State tracking for detecting which type of disconnect occurred.
	 * Set by whichever mixin fires first (packet, exception, or channel close).
	 * Prevents double-logging in the normal disconnect flow.
	 */
	public static volatile DisconnectType lastDisconnectType = DisconnectType.NONE;
	public static volatile String lastErrorDetail = null;

	public static void init() {
		// Fallback listener for hard disconnects (raw TCP close, server crash, etc)
		// The mixins (packet + network) are the primary detection mechanisms
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			onDisconnect(handler, client);
		});

		System.out.println("[KickLogger] ✓ Initialized - packet + network mixins are primary, DISCONNECT event is fallback");
	}

	/**
	 * Called by DisconnectPacketMixin when a DisconnectS2CPacket arrives from the server.
	 * This is the ground-truth kick event at the network level.
	 * All game state (player, world, position) is still valid at this point.
	 *
	 * @param reason The plain-text kick reason from the DisconnectS2CPacket
	 */
	public static void notifyKickPacketReceived(String reason) {
		lastDisconnectType = DisconnectType.INTENTIONAL_KICK;
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
			// Log latency
			if (client.getNetworkHandler() != null) {
				var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
				if (entry != null) {
					System.out.println("[KickLogger] Last Known Latency: " + entry.getLatency() + "ms");
				}
			}
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
	 * Called by NetworkConnectionMixin when a Netty exception occurs before vanilla's disconnect handler.
	 * Detects timeout, compression/decoder errors, and generic network errors.
	 *
	 * @param cause The exception from Netty's exception handler
	 */
	public static void notifyNetworkException(Throwable cause) {
		if (cause instanceof io.netty.handler.timeout.TimeoutException) {
			lastDisconnectType = DisconnectType.TIMEOUT;
			lastErrorDetail = "ReadTimeout (no data received for 30 seconds)";
			System.out.println("[KickLogger] ========== NETWORK TIMEOUT DETECTED ==========");
			System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
			System.out.println("[KickLogger] Type: Network timeout (Netty ReadTimeoutHandler, 30s threshold)");
			System.out.println("[KickLogger] ========== END TIMEOUT LOG ==========");
		} else if (cause instanceof io.netty.handler.codec.DecoderException) {
			lastDisconnectType = DisconnectType.COMPRESSION_ERROR;
			Throwable inner = cause.getCause();
			boolean isZlib = inner instanceof java.util.zip.DataFormatException;
			lastErrorDetail = "DecoderException" + (isZlib ? " (zlib DataFormatException)" : "") + ": " + cause.getMessage();
			System.out.println("[KickLogger] ========== COMPRESSION/DECODER ERROR DETECTED ==========");
			System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
			System.out.println("[KickLogger] Error Class: " + cause.getClass().getName());
			System.out.println("[KickLogger] Error Message: " + cause.getMessage());
			if (inner != null) {
				System.out.println("[KickLogger] Caused By: " + inner.getClass().getName() + ": " + inner.getMessage());
			}
			System.out.println("[KickLogger] ========== END COMPRESSION ERROR LOG ==========");
		} else {
			// Other network exceptions (not timeout, not compression/decoder)
			lastDisconnectType = DisconnectType.GENERIC_ERROR;
			lastErrorDetail = cause.getClass().getName() + ": " + cause.getMessage();
			System.out.println("[KickLogger] ========== GENERIC NETWORK ERROR DETECTED ==========");
			System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
			System.out.println("[KickLogger] Error Class: " + cause.getClass().getName());
			System.out.println("[KickLogger] Error Message: " + cause.getMessage());
			// Log up to 3 levels of cause chain
			Throwable t = cause.getCause();
			int depth = 0;
			while (t != null && depth < 3) {
				System.out.println("[KickLogger] Caused By (" + (depth + 1) + "): " + t.getClass().getName() + ": " + t.getMessage());
				t = t.getCause();
				depth++;
			}
			System.out.println("[KickLogger] ========== END GENERIC ERROR LOG ==========");
		}
	}

	/**
	 * Called by NetworkConnectionMixin when the Netty channel closes.
	 * Detects raw TCP closes — channel closing without prior exception or disconnect packet.
	 * The state flag prevents double-logging in normal disconnect flows (packet or error already fired).
	 */
	public static void notifyChannelInactive() {
		if (lastDisconnectType == DisconnectType.NONE) {
			// channelInactive fired with no prior exception or disconnect packet
			lastDisconnectType = DisconnectType.RAW_TCP_CLOSE;
			System.out.println("[KickLogger] ========== RAW TCP CLOSE DETECTED ==========");
			System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());
			System.out.println("[KickLogger] Type: Channel closed with no prior disconnect packet or network exception");
			System.out.println("[KickLogger] Note: Possible causes: server process killed, network cable pulled, firewall drop");
			System.out.println("[KickLogger] ========== END RAW TCP CLOSE LOG ==========");
		}
		// If flag is already set, the channel closing is a side-effect of the disconnect — don't log again
	}

	/**
	 * Fallback handler for hard disconnects without a prior DisconnectS2CPacket.
	 * Fires when ClientPlayConnectionEvents.DISCONNECT is triggered (raw TCP close, etc).
	 * Logs the disconnect type (set by whichever mixin fired first) and any error details.
	 */
	private static void onDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		System.out.println("[KickLogger] ========== DISCONNECT EVENT (type: " + lastDisconnectType + ") ==========");
		System.out.println("[KickLogger] Timestamp: " + System.currentTimeMillis());

		// Log error detail if present
		if (lastErrorDetail != null) {
			System.out.println("[KickLogger] Error Detail: " + lastErrorDetail);
		}

		// Server info
		if (handler != null && handler.getServerInfo() != null) {
			System.out.println("[KickLogger] Server: " + handler.getServerInfo().name);
			System.out.println("[KickLogger] Address: " + handler.getServerInfo().address);
		}

		// Player info
		if (client.player != null) {
			System.out.println("[KickLogger] Player: " + client.player.getDisplayName().getString());
			System.out.println("[KickLogger] Health: " + client.player.getHealth() + "/" + client.player.getMaxHealth());
			// Log latency (must read before world is cleared)
			if (handler != null) {
				var entry = handler.getPlayerListEntry(client.player.getUuid());
				if (entry != null) {
					System.out.println("[KickLogger] Last Known Latency: " + entry.getLatency() + "ms");
				}
			}
		}

		// World and position
		if (client.world != null && client.player != null) {
			System.out.println("[KickLogger] Dimension: " + client.world.getRegistryKey().getValue());
			System.out.println("[KickLogger] Position: X=" + (int)client.player.getX()
				+ " Y=" + (int)client.player.getY()
				+ " Z=" + (int)client.player.getZ());
		}

		System.out.println("[KickLogger] ========== END DISCONNECT EVENT ==========");

		// Reset for next connection
		lastDisconnectType = DisconnectType.NONE;
		lastErrorDetail = null;
	}
}
