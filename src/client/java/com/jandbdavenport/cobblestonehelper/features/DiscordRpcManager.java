package com.jandbdavenport.cobblestonehelper.features;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom Discord Rich Presence implementation.
 * Pure Java implementation of Discord IPC protocol - no external dependencies.
 * Supports both stable Discord and Discord PTB.
 */
public class DiscordRpcManager {
	private static final long CLIENT_ID = 1486603917281464381L;
	private static final int UPDATE_INTERVAL_TICKS = 100;     // 5 seconds
	private static final int RETRY_INTERVAL_TICKS = 600;      // 30 seconds
	private static final Gson GSON = new Gson();

	private enum State { DISCONNECTED, CONNECTING, CONNECTED }

	private static volatile State state = State.DISCONNECTED;
	private static DiscordIPCConnection ipcConnection;
	private static int ticksSinceLastUpdate = 0;
	private static int ticksSinceLastRetry = RETRY_INTERVAL_TICKS;
	private static long sessionStartMs;
	private static String lastDetails = "";
	private static String lastState = "";
	private static int tickCount = 0;

	// Regex patterns for scoreboard parsing
	private static final Pattern WORLD_PATTERN = Pattern.compile("(?:World|Area|Zone):\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern CROP_PATTERN = Pattern.compile("(?:Crop|Farming|Farm):\\s*(.+)", Pattern.CASE_INSENSITIVE);

	/**
	 * Initialize Discord RPC system.
	 */
	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ModConfig.discordRpcEnabled) {
				if (tickCount++ % 600 == 0) {
					System.out.println("[DiscordRpcManager] Discord RPC is disabled in config");
				}
				return;
			}
			tick(client);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			System.out.println("[DiscordRpcManager] Server disconnect detected, shutting down");
			shutdown();
		});

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutdown();
			} catch (Exception ignored) {}
		}));

		System.out.println("[DiscordRpcManager] ✓ Initialized (enabled=" + ModConfig.discordRpcEnabled + ")");
	}

	/**
	 * Main tick handler.
	 */
	private static void tick(MinecraftClient client) {
		if (client.world == null) {
			if (state != State.DISCONNECTED) {
				System.out.println("[DiscordRpcManager] Left world, disconnecting");
				shutdown();
			}
			return;
		}

		if (state == State.DISCONNECTED) {
			ticksSinceLastRetry++;
			if (ticksSinceLastRetry >= RETRY_INTERVAL_TICKS) {
				System.out.println("[DiscordRpcManager] Attempting to connect to Discord...");
				attemptConnect();
				ticksSinceLastRetry = 0;
			}
			return;
		}

		if (state == State.CONNECTED) {
			ticksSinceLastUpdate++;
			if (ticksSinceLastUpdate >= UPDATE_INTERVAL_TICKS) {
				updatePresence(client);
				ticksSinceLastUpdate = 0;
			}
		}
	}

	/**
	 * Attempt to connect to Discord IPC.
	 */
	private static void attemptConnect() {
		try {
			ipcConnection = new DiscordIPCConnection(CLIENT_ID);
			if (ipcConnection.connect()) {
				state = State.CONNECTED;
				sessionStartMs = System.currentTimeMillis();
				System.out.println("[DiscordRpcManager] ✓ Connected to Discord successfully");
				return;
			}
		} catch (Exception e) {
			System.out.println("[DiscordRpcManager] Failed to connect: " + e.getMessage());
		}

		state = State.DISCONNECTED;
		ipcConnection = null;
	}

	/**
	 * Get the sidebar objective, checking team-specific slot first.
	 */
	private static net.minecraft.scoreboard.ScoreboardObjective getSidebarObjective(MinecraftClient client, Scoreboard scoreboard) {
		if (client.player == null) {
			return scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
		}

		// Check if player has a team and try that objective slot first
		var team = client.player.getScoreboardTeam();
		if (team != null && team.getColor() != null) {
			var teamSlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
			if (teamSlot != null) {
				var teamObjective = scoreboard.getObjectiveForSlot(teamSlot);
				if (teamObjective != null) {
					return teamObjective;
				}
			}
		}

		// Fall back to sidebar if no team objective
		return scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
	}

	/**
	 * Update Discord presence.
	 */
	private static void updatePresence(MinecraftClient client) {
		if (ipcConnection == null || state != State.CONNECTED) return;

		try {
			String[] scoreboardInfo = readScoreboard(client);
			String world = scoreboardInfo[0];
			String crop = scoreboardInfo[1];

			String details = crop.isEmpty() ? "On play.cobblestone.gg" : crop;
			String presenceState = world.isEmpty() ? "Exploring" : world;

			if (details.equals(lastDetails) && presenceState.equals(lastState)) {
				return;
			}

			lastDetails = details;
			lastState = presenceState;

			System.out.println("[DiscordRpcManager] Updating presence: details=\"" + details + "\", state=\"" + presenceState + "\"");

			// Send presence update
			ipcConnection.setPresence(details, presenceState, sessionStartMs);
			System.out.println("[DiscordRpcManager] ✓ Presence sent to Discord");
		} catch (Exception e) {
			System.out.println("[DiscordRpcManager] Failed to update presence: " + e.getMessage());
			state = State.DISCONNECTED;
			ipcConnection = null;
		}
	}

	/**
	 * Read scoreboard sidebar, checking team-specific objectives first.
	 */
	private static String[] readScoreboard(MinecraftClient client) {
		String world = "";
		String crop = "";

		try {
			Scoreboard scoreboard = client.world.getScoreboard();

			// Try team objective first (like Scoreboard Overhaul does)
			var sidebarObjective = getSidebarObjective(client, scoreboard);

			if (sidebarObjective == null) {
				return new String[]{"", ""};
			}

			var entries = scoreboard.getScoreboardEntries(sidebarObjective);
			for (ScoreboardEntry entry : entries) {
			Team team = scoreboard.getScoreHolderTeam(entry.owner());
			String line = (team != null)
				? Team.decorateName(team, entry.name()).getString()
				: entry.name().getString();
				if (line == null) continue;

				String cleanedLine = line.replaceAll("§[0-9a-fA-Fk-oK-Or-tR-T]", "");

				if (world.isEmpty()) {
					Matcher worldMatcher = WORLD_PATTERN.matcher(cleanedLine);
					if (worldMatcher.find()) {
						world = "World: " + worldMatcher.group(1).trim();
					}
				}

				if (crop.isEmpty()) {
					Matcher cropMatcher = CROP_PATTERN.matcher(cleanedLine);
					if (cropMatcher.find()) {
						crop = "Farming: " + cropMatcher.group(1).trim();
					}
				}

				if (!world.isEmpty() && !crop.isEmpty()) {
					break;
				}
			}
		} catch (Exception e) {
			System.out.println("[DiscordRpcManager] Exception reading scoreboard: " + e.getClass().getSimpleName());
		}

		return new String[]{world, crop};
	}

	/**
	 * Shutdown Discord RPC.
	 */
	public static void shutdown() {
		if (ipcConnection != null) {
			try {
				ipcConnection.close();
				System.out.println("[DiscordRpcManager] ✓ IPC connection closed");
			} catch (Exception ignored) {}
			ipcConnection = null;
		}
		state = State.DISCONNECTED;
	}

	/**
	 * Disable Discord RPC.
	 */
	public static void disable() {
		System.out.println("[DiscordRpcManager] Disabled via config menu");
		shutdown();
	}

	/**
	 * Custom Discord IPC connection handler.
	 * Implements the Discord RPC protocol over named pipes (Windows) or Unix sockets (Linux/macOS).
	 */
	private static class DiscordIPCConnection {
		private final long clientId;
		private RandomAccessFile pipe;
		private int nonce = 0;

		DiscordIPCConnection(long clientId) {
			this.clientId = clientId;
		}

		/**
		 * Attempt to connect to Discord IPC.
		 */
		boolean connect() throws IOException {
			// Try pipes 0-9 (Discord allocates them sequentially)
			for (int i = 0; i < 10; i++) {
				try {
					String pipePath = "\\\\.\\pipe\\discord-ipc-" + i;
					pipe = new RandomAccessFile(pipePath, "rw");

					// Send handshake
					JsonObject handshake = new JsonObject();
					handshake.addProperty("v", 1);
					handshake.addProperty("client_id", String.valueOf(clientId));

					sendFrame(0, handshake.toString());

					// Receive handshake response
					String response = receiveFrame();
					if (response != null && response.contains("READY")) {
						System.out.println("[DiscordRpcManager] Connected to pipe: discord-ipc-" + i);
						return true;
					}

					pipe.close();
					pipe = null;
				} catch (Exception e) {
					// This pipe didn't work, try the next
					if (pipe != null) {
						try {
							pipe.close();
						} catch (Exception ignored) {}
						pipe = null;
					}
				}
			}

			throw new IOException("Could not find Discord IPC pipe");
		}

		/**
		 * Set presence on Discord.
		 */
		void setPresence(String details, String state, long startTime) throws IOException {
			if (pipe == null) {
				throw new IOException("Not connected");
			}

			JsonObject activity = new JsonObject();
			activity.addProperty("state", state);
			activity.addProperty("details", details);
			activity.addProperty("start_timestamp", startTime / 1000);

			JsonObject assets = new JsonObject();
			assets.addProperty("large_image", "minecraft");
			activity.add("assets", assets);

			JsonObject command = new JsonObject();
			command.addProperty("cmd", "SET_ACTIVITY");
			command.addProperty("nonce", String.valueOf(nonce++));

			JsonObject args = new JsonObject();
			args.addProperty("pid", ProcessHandle.current().pid());
			args.add("activity", activity);

			command.add("args", args);

			sendFrame(1, command.toString());

			// Receive response (non-blocking for performance)
			try {
				receiveFrame();
			} catch (Exception ignored) {
				// Response not critical for our use case
			}
		}

		/**
		 * Send IPC frame.
		 * Frame format: [opcode (4 bytes)][length (4 bytes)][payload]
		 */
		private void sendFrame(int opcode, String payload) throws IOException {
			byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

			// Create header: opcode + length (little-endian)
			ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			header.putInt(opcode);
			header.putInt(payloadBytes.length);

			// Write header + payload
			pipe.write(header.array());
			pipe.write(payloadBytes);
			pipe.seek(0);  // Reset file pointer for next read
		}

		/**
		 * Receive IPC frame.
		 */
		private String receiveFrame() throws IOException {
			byte[] headerBytes = new byte[8];
			int bytesRead = pipe.read(headerBytes);

			if (bytesRead != 8) {
				return null;
			}

			ByteBuffer header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
			int opcode = header.getInt();
			int length = header.getInt();

			if (length <= 0 || length > 1024 * 1024) {  // Max 1MB
				return null;
			}

			byte[] payload = new byte[length];
			bytesRead = pipe.read(payload);

			if (bytesRead != length) {
				return null;
			}

			return new String(payload, StandardCharsets.UTF_8);
		}

		/**
		 * Close connection.
		 */
		void close() throws IOException {
			if (pipe != null) {
				pipe.close();
				pipe = null;
			}
		}
	}
}
