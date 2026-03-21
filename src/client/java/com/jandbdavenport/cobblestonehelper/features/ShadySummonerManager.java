package com.jandbdavenport.cobblestonehelper.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the Shady Summoner HUD widget and state tracking.
 * Tracks when the NPC appears/disappears and shows a countdown timer.
 */
public class ShadySummonerManager {
	// RepositionableWidget implementation for HudPositionScreen
	public static final RepositionableWidget WIDGET = new RepositionableWidget() {
		@Override
		public int getHudX() {
			return hudX;
		}

		@Override
		public int getHudY() {
			return hudY;
		}

		@Override
		public void setHudPosition(int x, int y) {
			hudX = x;
			hudY = y;
		}

		@Override
		public int getWidgetWidth() {
			return (int)(WIDGET_WIDTH * ModConfig.ssScale);
		}

		@Override
		public int getWidgetHeight() {
			return (int)(WIDGET_HEIGHT * ModConfig.ssScale);
		}

		@Override
		public void setHudRenderingDisabled(boolean disabled) {
			hudRenderingDisabled = disabled;
		}

		@Override
		public void renderWidgetPreview(DrawContext context) {
			renderHud(context);
		}

		@Override
		public void saveConfig() {
			ShadySummonerManager.saveConfig();
		}
	};

	private enum State {
		UNKNOWN, ACTIVE, COUNTDOWN, SOON
	}

	private static State state = State.UNKNOWN;
	private static String activeWorld = null;
	private static long countdownEndMs = 0;
	private static long activeSpawnTimeMs = 0; // Track when summoner spawned (for 2-minute timer)
	private static long soonStateStartMs = 0; // Track when we entered SOON state
	private static boolean warningSoundPlayed60s = false;
	private static boolean warningSoundPlayed30s = false;
	private static long bellSequenceStartMs = 0; // Track when the 30s bell sequence started
	private static int bellsPlayed = 0; // Track how many bells have been played in sequence
	private static long bellSequence5xStartMs = 0; // Track when the 5x bell sequence started
	private static int bells5xPlayed = 0; // Track how many bells have been played in 5x sequence

	// HUD position (saved to config)
	private static int hudX = 5;
	private static int hudY = 5;

	// HUD widget dimensions (approximate for text-based display)
	private static final int WIDGET_WIDTH = 160; // ~"Shady Summoner: countdown"
	private static final int WIDGET_HEIGHT = 10;  // Single line of text

	// Flag to disable HUD rendering when positioning screen is open
	private static boolean hudRenderingDisabled = false;

	// Offline tracking for persistent timer
	private static long lastSaveTimeMs = 0; // When we last saved state to config
	private static long offlineCountdownRemaining = 0; // Countdown time remaining when player went offline
	private static boolean isEstimationMode = false; // True if timer is estimated based on offline calculation

	// Colors (ARGB format)
	private static final int COLOR_ORANGE = 0xFFFF8C00;
	private static final int COLOR_DARK_BG = 0xCC111111;
	private static final int COLOR_TEXT_GREY = 0xFF888888;
	private static final int COLOR_TEXT_GREEN = 0xFF00CC00;
	private static final int COLOR_TEXT_YELLOW = 0xFFFFCC00;
	private static final int COLOR_TEXT_RED = 0xFFFF4444;
	private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
	private static final int COLOR_TEXT_PURPLE = 0xFFBB77FF; // Light purple

	// Config file path
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "cobblestonehelper.json";

	public static void init() {
		// Load config from disk
		loadConfig();

		// Register chat message listener for spawn/despawn events
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return; // Only process game messages, not chat
			String text = message.getString().toLowerCase();

			// Check for spawn message
			if (text.contains("shady summoner has appeared in")) {
				String world = extractWorldName(message.getString());
				if (world != null) {
					setState(State.ACTIVE, world);
				}
			}
			// Check for despawn message
			else if (text.contains("shady summoner has disappeared")) {
				setState(State.COUNTDOWN, null);
			}
		});

		// Register tick handler for countdown and warning sound
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long now = System.currentTimeMillis();

			if (state == State.COUNTDOWN) {
				// Check if countdown has ended
				if (now >= countdownEndMs) {
					setState(State.SOON, null);
					soonStateStartMs = now;
					playWarningBellSequence5x(); // Play 5 bells when timer hits 0
				}
				// Check if we should play warning sounds
				else {
					long remainingMs = countdownEndMs - now;
					long remainingSeconds = remainingMs / 1000;

					// Warning at 60 seconds remaining
					if (remainingSeconds == 60 && !warningSoundPlayed60s) {
						playWarningSound(client);
						warningSoundPlayed60s = true;
					}

					// Warning at 30 seconds remaining (play 3 times in succession)
					if (remainingSeconds == 30 && !warningSoundPlayed30s) {
						playWarningBellSequence();
						warningSoundPlayed30s = true;
					}
				}
			}

			// Handle bell sequence playback (3 bells with 250ms delays)
			if (bellsPlayed < 3 && bellSequenceStartMs > 0) {
				long elapsedMs = now - bellSequenceStartMs;

				// Play bells at 0ms, 250ms, and 500ms
				if (bellsPlayed == 0 && elapsedMs >= 0) {
					playWarningSound(client);
					bellsPlayed++;
				} else if (bellsPlayed == 1 && elapsedMs >= 250) {
					playWarningSound(client);
					bellsPlayed++;
				} else if (bellsPlayed == 2 && elapsedMs >= 500) {
					playWarningSound(client);
					bellsPlayed++;
				}
			}

			// Reset bell sequence after all bells have played
			if (bellsPlayed >= 3) {
				bellSequenceStartMs = 0;
				bellsPlayed = 0;
			}

			// Handle 5x bell sequence playback (5 bells with 250ms delays for 0 second alert)
			if (bells5xPlayed < 5 && bellSequence5xStartMs > 0) {
				long elapsedMs = now - bellSequence5xStartMs;

				// Play bells at 0ms, 250ms, 500ms, 750ms, and 1000ms
				if (bells5xPlayed == 0 && elapsedMs >= 0) {
					playWarningSound(client);
					bells5xPlayed++;
				} else if (bells5xPlayed == 1 && elapsedMs >= 250) {
					playWarningSound(client);
					bells5xPlayed++;
				} else if (bells5xPlayed == 2 && elapsedMs >= 500) {
					playWarningSound(client);
					bells5xPlayed++;
				} else if (bells5xPlayed == 3 && elapsedMs >= 750) {
					playWarningSound(client);
					bells5xPlayed++;
				} else if (bells5xPlayed == 4 && elapsedMs >= 1000) {
					playWarningSound(client);
					bells5xPlayed++;
				}
			}

			// Reset 5x bell sequence after all bells have played
			if (bells5xPlayed >= 5) {
				bellSequence5xStartMs = 0;
				bells5xPlayed = 0;
			}

			// SOON state no longer times out - will persist until actual spawn message received
		});
	}

	/**
	 * Extract the world name from a spawn message.
	 * Handles both "in the [World]" and "in [World]" formats.
	 */
	private static String extractWorldName(String message) {
		String lowerMsg = message.toLowerCase();

		// Try "in the " first
		int startIdx = lowerMsg.indexOf("in the ");
		int prefixLen = 7; // "in the ".length()

		// Fall back to "in " if "in the " not found
		if (startIdx == -1) {
			startIdx = lowerMsg.indexOf("in ");
			prefixLen = 3; // "in ".length()
		}

		if (startIdx == -1) return null;

		startIdx += prefixLen;
		int endIdx = message.indexOf(".", startIdx);
		if (endIdx == -1) endIdx = message.length();

		return message.substring(startIdx, endIdx).trim();
	}

	/**
	 * Update the state machine.
	 */
	private static void setState(State newState, String world) {
		state = newState;
		warningSoundPlayed60s = false;
		warningSoundPlayed30s = false;
		soonStateStartMs = 0;
		bellSequenceStartMs = 0; // Reset bell sequence on state change
		bellsPlayed = 0;
		bellSequence5xStartMs = 0; // Reset 5x bell sequence on state change
		bells5xPlayed = 0;
		isEstimationMode = false; // Reset estimation mode when receiving server message

		switch (newState) {
			case UNKNOWN:
				activeWorld = null;
				countdownEndMs = 0;
				activeSpawnTimeMs = 0;
				break;
			case ACTIVE:
				activeWorld = world;
				countdownEndMs = 0;
				activeSpawnTimeMs = System.currentTimeMillis(); // Start 2-minute timer
				break;
			case COUNTDOWN:
				activeWorld = null;
				countdownEndMs = System.currentTimeMillis() + (30 * 60 * 1000); // 30 minutes
				activeSpawnTimeMs = 0;
				break;
			case SOON:
				activeWorld = null;
				activeSpawnTimeMs = 0;
				// Don't reset countdownEndMs, we use it to track the spawn time
				break;
		}

		saveConfig();
	}

	/**
	 * Play the note block pling warning sound at the player's position (pitched 1 octave up).
	 */
	private static void playWarningSound(MinecraftClient client) {
		if (client.player != null) {
			client.player.playSound(
				SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
				1.0f,  // volume
				2.0f   // pitch (2.0 = 1 octave up)
			);
		}
	}

	/**
	 * Start the bell warning sound sequence for the 30-second alert (plays 3 times with delays).
	 */
	private static void playWarningBellSequence() {
		bellSequenceStartMs = System.currentTimeMillis();
		bellsPlayed = 0;
	}

	/**
	 * Start the bell warning sound sequence for 0 seconds (plays 5 times with delays).
	 */
	private static void playWarningBellSequence5x() {
		bellSequence5xStartMs = System.currentTimeMillis();
		bells5xPlayed = 0;
	}

	/**
	 * Render the HUD widget at the configured position.
	 */
	public static void renderHud(DrawContext context) {
		// Don't render if disabled or feature disabled
		if (hudRenderingDisabled || !ModConfig.shadySummonerEnabled) {
			return;
		}

	MinecraftClient client = MinecraftClient.getInstance();
	String stateText = getStateText();
	int stateColor = getStateColor();

	// Draw label
	context.drawTextWithShadow(client.textRenderer, Text.literal("Shady Summoner: "), hudX, hudY, ModConfig.ssColorLabel);

	// Draw state in appropriate color right after the label
	int stateX = hudX + client.textRenderer.getWidth("Shady Summoner: ");
	context.drawTextWithShadow(client.textRenderer, Text.literal(stateText), stateX, hudY, stateColor);
}

	/**
	 * Get the text representation of the current state.
	 */
	private static String getStateText() {
		switch (state) {
			case UNKNOWN:
				return "Unknown";

			case ACTIVE:
				// Show world name with 2-minute spawn window timer
				String worldDisplay = activeWorld.length() > 12 ? activeWorld.substring(0, 12) : activeWorld;

				// Calculate remaining time in 2-minute spawn window
				if (activeSpawnTimeMs > 0) {
					long now = System.currentTimeMillis();
					long elapsedMs = now - activeSpawnTimeMs;
					long remainingMs = (2 * 60 * 1000) - elapsedMs; // 2 minutes
					long remainingSeconds = Math.max(0, remainingMs / 1000);
					long minutes = remainingSeconds / 60;
					long seconds = remainingSeconds % 60;

					String timerText = String.format("%d:%02d", minutes, seconds);
					return worldDisplay + " " + timerText;
				}

				return worldDisplay;

			case COUNTDOWN:
				long now = System.currentTimeMillis();
				long remainingMs = countdownEndMs - now;
				long remainingSeconds = Math.max(0, remainingMs / 1000);
				long minutes = remainingSeconds / 60;
				long seconds = remainingSeconds % 60;
				String timerText = String.format("%d:%02d", minutes, seconds);
				// Append "?" if in estimation mode (offline timer)
				if (isEstimationMode) {
					timerText += "?";
				}
				return timerText;

			case SOON:
				return "Soon!";

			default:
				return "Unknown";
		}
	}

	/**
	 * Get the color for the current state.
	 */
	private static int getStateColor() {
		switch (state) {
			case UNKNOWN:
				return ModConfig.ssColorUnknown;
			case ACTIVE:
				return ModConfig.ssColorActive;
			case COUNTDOWN:
				long now = System.currentTimeMillis();
				long remainingMs = countdownEndMs - now;
				long remainingSeconds = Math.max(0, remainingMs / 1000);
				return remainingSeconds < 90 ? ModConfig.ssColorWarning : ModConfig.ssColorNormal;
			case SOON:
				return ModConfig.ssColorNormal;
			default:
				return ModConfig.ssColorUnknown;
		}
	}

	/**
	 * Load HUD position and offline timer state from config file.
	 */
	private static void loadConfig() {
		try {
			Path configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
			File configFile = configPath.toFile();

			if (configFile.exists()) {
				String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
				JsonObject json = new Gson().fromJson(content, JsonObject.class);

				if (json.has("shadySummonerHudX")) {
					hudX = json.get("shadySummonerHudX").getAsInt();
				}
				if (json.has("shadySummonerHudY")) {
					hudY = json.get("shadySummonerHudY").getAsInt();
				}

				// Load offline timer data
				if (json.has("offlineCountdownRemaining") && json.has("lastSaveTimeMs")) {
					long savedOfflineRemaining = json.get("offlineCountdownRemaining").getAsLong();
					long savedTime = json.get("lastSaveTimeMs").getAsLong();

					if (savedOfflineRemaining > 0) {
						// Calculate how much time has passed
						long now = System.currentTimeMillis();
						long elapsedMs = now - savedTime;

						// Adjust the remaining time
						long newRemaining = savedOfflineRemaining - elapsedMs;

						if (newRemaining > 0) {
							// Timer still has time remaining - restore it
							state = State.COUNTDOWN;
							countdownEndMs = now + newRemaining;
							isEstimationMode = true;
							lastSaveTimeMs = now;
						} else if (newRemaining > -120000) {
							// Timer ended less than 2 minutes ago - in spawn window, set to SOON
							state = State.SOON;
							soonStateStartMs = now;
							isEstimationMode = true;
						} else {
							// More than 2 minutes have passed - calculate position in cycle
							// Spawning cycle: 2 min spawn window + 30 min countdown = 32 min total
							long timeSinceDespawn = -newRemaining - 120000; // Time after 2-min spawn window
							long cycleLength = 32 * 60 * 1000; // 32 minutes in milliseconds
							long positionInCycle = timeSinceDespawn % cycleLength;

							if (positionInCycle < (30 * 60 * 1000)) {
								// In countdown phase of current cycle
								long newCountdownRemaining = (30 * 60 * 1000) - positionInCycle;
								state = State.COUNTDOWN;
								countdownEndMs = now + newCountdownRemaining;
								isEstimationMode = true;
								lastSaveTimeMs = now;
							} else {
								// In spawn window (position >= 30 min means we're in the 2-min spawn window)
								state = State.SOON;
								soonStateStartMs = now;
								isEstimationMode = true;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// If loading fails, use defaults
		}
	}

	/**
	 * Save HUD position and offline timer state to config file.
	 * Uses read-merge-write pattern to preserve other config data.
	 */
	public static void saveConfig() {
		try {
			File configDir = new File(CONFIG_DIR);
			if (!configDir.exists()) {
				configDir.mkdirs();
			}

			// Read existing config or create new object
			JsonObject json = new JsonObject();
			Path configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
			if (Files.exists(configPath)) {
				try {
					String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
					JsonObject existing = new Gson().fromJson(content, JsonObject.class);
					if (existing != null) {
						// Copy all existing entries to preserve other config data
						for (var entry : existing.entrySet()) {
							json.add(entry.getKey(), entry.getValue());
						}
					}
				} catch (Exception ignored) {
					// If read fails, start with empty object
				}
			}

			// Update Shady Summoner specific properties
			json.addProperty("shadySummonerHudX", hudX);
			json.addProperty("shadySummonerHudY", hudY);

			// Save offline timer data
			if (state == State.COUNTDOWN && countdownEndMs > 0) {
				long now = System.currentTimeMillis();
				long remainingMs = countdownEndMs - now;
				if (remainingMs > 0) {
					json.addProperty("offlineCountdownRemaining", remainingMs);
					json.addProperty("lastSaveTimeMs", now);
				}
			}

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String content = gson.toJson(json);

			Files.write(configPath, content.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			// If saving fails, silently ignore
		}
	}

	// Getters for HUD positioning screen
	public static int getHudX() {
		return hudX;
	}

	public static int getHudY() {
		return hudY;
	}

	public static void setHudPosition(int x, int y) {
		hudX = x;
		hudY = y;
	}

	public static int getWidgetWidth() {
		return WIDGET_WIDTH;
	}

	public static int getWidgetHeight() {
		return (int)(WIDGET_HEIGHT * ModConfig.ssScale);
	}

	// Debug methods for testing
	public static void debugSimulateSpawn(String worldName) {
		setState(State.ACTIVE, worldName);
	}

	public static void debugSimulateDespawn() {
		setState(State.COUNTDOWN, null);
	}

	public static void debugSetCountdownTimer(long seconds) {
		state = State.COUNTDOWN;
		activeWorld = null;
		countdownEndMs = System.currentTimeMillis() + (seconds * 1000);
		warningSoundPlayed60s = false;
		warningSoundPlayed30s = false;
		saveConfig();
	}

	// Methods to control HUD rendering visibility
	public static void setHudRenderingDisabled(boolean disabled) {
		hudRenderingDisabled = disabled;
	}
}
