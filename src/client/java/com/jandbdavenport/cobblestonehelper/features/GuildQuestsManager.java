package com.jandbdavenport.cobblestonehelper.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jandbdavenport.cobblestonehelper.ModConfig;
import com.jandbdavenport.cobblestonehelper.gui.HudPositionScreen;
import com.jandbdavenport.cobblestonehelper.util.ContainerScreenUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the Guild Quests HUD overlay and state tracking.
 * Tracks quest targets, completion status, and remaining timer.
 */
public class GuildQuestsManager {
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
			return (int)(WIDGET_WIDTH * ModConfig.gqScale);
		}

		@Override
		public int getWidgetHeight() {
			return (int)(WIDGET_HEIGHT * ModConfig.gqScale);
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
	public int getDefaultHudY() {
		return 60;
	}

	@Override
	public void saveConfig() {
		GuildQuestsManager.saveConfig();
	}
};

	private enum DataState {
		NO_DATA, KNOWN
	}

	// State machine
	private static DataState dataState = DataState.NO_DATA;

	// Quest data (targets are the extracted names, actionLines are the full lore lines)
	private static String boss1Target = "";
	private static String boss2Target = "";
	private static String crop3kTarget = "";
	private static String crop7500Target = "";

	private static String boss1ActionLine = "";
	private static String boss2ActionLine = "";
	private static String crop3kActionLine = "";
	private static String crop7500ActionLine = "";

	// Track previous action lines to detect quest changes
	private static String prevBoss1ActionLine = "";
	private static String prevBoss2ActionLine = "";
	private static String prevCrop3kActionLine = "";
	private static String prevCrop7500ActionLine = "";

	private static boolean boss1Complete = false;
	private static boolean boss2Complete = false;
	private static boolean crop3kComplete = false;
	private static boolean crop7500Complete = false;

	// Timer
	private static long questResetMs = 0;
	private static long lastTimerSyncMs = 0; // Track when we last synced the timer

	// Screen detection
	private static boolean isGuildQuestsScreenOpen = false;
	private static GenericContainerScreen currentScreen = null;
	private static boolean scraped = false;
	private static long screenOpenedMs = 0;

	// HUD position (saved to config)
	private static int hudX = 5;
	private static int hudY = 60; // Below Shady Summoner (which is at y=5)

	// HUD widget dimensions
	private static final int WIDGET_WIDTH = 160;
	private static final int WIDGET_HEIGHT = 60; // 5 lines × ~10px + padding

	// Flag to disable HUD rendering when positioning screen is open
	private static boolean hudRenderingDisabled = false;

	// Config file path
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "cobblestonehelper.json";

	/**
	 * Get the quest highlight type for a given crop/boss name.
	 * Returns: "crop-green", "boss-red", or empty string if no match.
	 */
	public static String getQuestHighlightType(String name) {
		// Check crop quests
		if (!crop3kComplete && !crop3kTarget.isEmpty() && crop3kTarget.equalsIgnoreCase(name)) {
			return "crop-green";
		}
		if (!crop7500Complete && !crop7500Target.isEmpty() && crop7500Target.equalsIgnoreCase(name)) {
			return "crop-green";
		}

		// Check boss quests - need to map boss names to their crops
		if (!boss1Complete && !boss1Target.isEmpty()) {
			String bossTarget = getBossCropName(boss1Target);
			if (!bossTarget.isEmpty() && bossTarget.equalsIgnoreCase(name)) {
				return "boss-red";
			}
		}
		if (!boss2Complete && !boss2Target.isEmpty()) {
			String bossTarget = getBossCropName(boss2Target);
			if (!bossTarget.isEmpty() && bossTarget.equalsIgnoreCase(name)) {
				return "boss-red";
			}
		}

		return "";
	}

	/**
	 * Map boss names to their crop names for the farm warps menu.
	 */
	private static String getBossCropName(String bossName) {
		return switch (bossName.toLowerCase()) {
			case "potato monster", "potato monsters" -> "Potato";
			case "secret cat", "secret cats" -> "Carrot";
			case "overworld cadaver", "overworld cadavers" -> "Beetroot";
			case "sun queen", "sun queens" -> "Pitcher Plant";
			default -> "";
		};
	}

	/**
	 * Clear all guild quest cache and reset to NO_DATA state.
	 * Used for testing and manual cache reset.
	 */
	public static void clearCache() {
		dataState = DataState.NO_DATA;
		questResetMs = 0;
		lastTimerSyncMs = 0;
		boss1Target = "";
		boss2Target = "";
		crop3kTarget = "";
		crop7500Target = "";
		boss1ActionLine = "";
		boss2ActionLine = "";
		crop3kActionLine = "";
		crop7500ActionLine = "";
		boss1Complete = false;
		boss2Complete = false;
		crop3kComplete = false;
		crop7500Complete = false;
		saveConfig();
	}

	/**
	 * Reset quest state when quests refresh on the server.
	 * Called when "New guild quests have become available!" message is detected.
	 */
	private static void resetQuestState() {
		dataState = DataState.NO_DATA;
		questResetMs = 0;
		lastTimerSyncMs = 0;
		boss1Target = "";
		boss2Target = "";
		crop3kTarget = "";
		crop7500Target = "";
		boss1ActionLine = "";
		boss2ActionLine = "";
		crop3kActionLine = "";
		crop7500ActionLine = "";
		prevBoss1ActionLine = "";
		prevBoss2ActionLine = "";
		prevCrop3kActionLine = "";
		prevCrop7500ActionLine = "";
		boss1Complete = false;
		boss2Complete = false;
		crop3kComplete = false;
		crop7500Complete = false;
		scraped = false;  // Reset scrape flag so we re-scrape when GUI opens
		saveConfig();
	}

	/**
	 * Add buttons to the Guild Quests screen.
	 */
	private static void addGuildQuestsButtons(Screen screen) {
		if (!(screen instanceof GenericContainerScreen)) {
			return;
		}

		// Create "Clear Cache" button (left button, top right area)
		ButtonWidget clearButton = ButtonWidget.builder(Text.literal("C"), button -> {
			MinecraftClient.getInstance().send(() -> {
				clearCache();
				MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§6Guild quests cache cleared!"));
			});
		})
		.dimensions(screen.width - 105, 5, 20, 20)
		.build();

		// Create "Reposition" button (right button, top right area)
		ButtonWidget settingsButton = ButtonWidget.builder(Text.literal("S"), button -> {
			MinecraftClient.getInstance().send(() ->
				MinecraftClient.getInstance().setScreen(new HudPositionScreen(WIDGET)));
		})
		.dimensions(screen.width - 80, 5, 20, 20)
		.build();

		Screens.getButtons(screen).add(clearButton);
		Screens.getButtons(screen).add(settingsButton);
	}

	public static void init() {
		// Load config from disk
		loadConfig();

		// Register screen open/close detection
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof GenericContainerScreen containerScreen) {
				String title = screen.getTitle().getString();
				if (title.contains("Guild Quests")) {
					isGuildQuestsScreenOpen = true;
					currentScreen = containerScreen;
					scraped = false;
					screenOpenedMs = System.currentTimeMillis();
					lastTimerSyncMs = 0; // Force timer sync on next tick
					addGuildQuestsButtons(screen); // Add buttons to the screen

					// Sync timer immediately
					syncTimerFromScreen();
				}
			}
		});

		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (isGuildQuestsScreenOpen) {
				isGuildQuestsScreenOpen = false;
				currentScreen = null;
				scraped = false;
				// Don't clear questResetMs - let the timer continue to tick in the HUD
			}
		});

		// Register tick handler for scraping and timer sync
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (isGuildQuestsScreenOpen && currentScreen != null) {
				// Sync timer from screen only every 5 seconds to avoid constant resetting
				long now = System.currentTimeMillis();
				if (now - lastTimerSyncMs >= 5000) {
					syncTimerFromScreen();
					lastTimerSyncMs = now;
				}

				// Scrape quest data on first open (with 1-second delay)
				if (!scraped) {
					long elapsedMs = now - screenOpenedMs;
					if (elapsedMs >= 1000) {
						scrapeQuestData();
					}
				}
			}
		});

		// Register chat message listener for quest completion and reset
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				String text = message.getString();
				// Check for quest reset message
				if (text.contains("GUILDS") && text.contains("New guild quests have become available")) {
					resetQuestState();
				} else {
					// Check for quest completion message
					handleQuestCompleteMessage(message);
				}
			}
		});
	}

	/**
	 * Scrape quest data from the open container screen.
	 */
	private static void scrapeQuestData() {
		if (currentScreen == null) {
			return;
		}

		// Reset action lines before scraping to detect changes
		String newBoss1ActionLine = "";
		String newBoss2ActionLine = "";
		String newCrop3kActionLine = "";
		String newCrop7500ActionLine = "";

		Inventory inventory = currentScreen.getScreenHandler().getInventory();
		int questItemsFound = 0;

		try {
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}

				String displayName = stack.getName().getString();
				List<Text> lore = ContainerScreenUtils.getLoreLines(stack);

				// Parse boss quest #1
				if (displayName.contains("BOSS QUEST #1")) {
					if (lore.size() > 2) {
						String actionLine = lore.get(2).getString();
						newBoss1ActionLine = actionLine.trim().replaceAll("^\\|\\s*", ""); // Strip leading pipe
						boss1Target = extractBossTarget(newBoss1ActionLine);
					}
					if (lore.size() > 3) {
						// Always check current item state to determine completion
						boss1Complete = isQuestItemEnchanted(stack) || parseProgressLine(lore.get(3).getString());
					}
					questItemsFound++;
				}
				// Parse boss quest #2
				else if (displayName.contains("BOSS QUEST #2")) {
					if (lore.size() > 2) {
						String actionLine = lore.get(2).getString();
						newBoss2ActionLine = actionLine.trim().replaceAll("^\\|\\s*", ""); // Strip leading pipe
						boss2Target = extractBossTarget(newBoss2ActionLine);
					}
					if (lore.size() > 3) {
						// Always check current item state to determine completion
						boss2Complete = isQuestItemEnchanted(stack) || parseProgressLine(lore.get(3).getString());
					}
					questItemsFound++;
				}
				// Parse farming quest #1
				else if (displayName.contains("FARMING QUEST #1")) {
					if (lore.size() > 2) {
						String actionLine = lore.get(2).getString();
						newCrop3kActionLine = actionLine.trim().replaceAll("^\\|\\s*", ""); // Strip leading pipe
						String cropName = extractCropName(newCrop3kActionLine);
						crop3kTarget = cropName;
					}
					if (lore.size() > 3) {
						// Always check current item state to determine completion
						String progressLine = lore.get(3).getString();
						crop3kComplete = isQuestItemEnchanted(stack) || parseProgressLine(progressLine);
					}
					questItemsFound++;
				}
				// Parse farming quest #2
				else if (displayName.contains("FARMING QUEST #2")) {
					if (lore.size() > 2) {
						String actionLine = lore.get(2).getString();
						newCrop7500ActionLine = actionLine.trim().replaceAll("^\\|\\s*", ""); // Strip leading pipe
						String cropName = extractCropName(newCrop7500ActionLine);
						crop7500Target = cropName;
					}
					if (lore.size() > 3) {
						// Always check current item state to determine completion
						String progressLine = lore.get(3).getString();
						crop7500Complete = isQuestItemEnchanted(stack) || parseProgressLine(progressLine);
					}
					questItemsFound++;
				}
			}

			// If we found at least 2 quest items, mark scrape as complete
			if (questItemsFound >= 2) {
				scraped = true;
				dataState = DataState.KNOWN;

				// Update the current action lines and save previous ones for change detection
				prevBoss1ActionLine = boss1ActionLine;
				prevBoss2ActionLine = boss2ActionLine;
				prevCrop3kActionLine = crop3kActionLine;
				prevCrop7500ActionLine = crop7500ActionLine;

				boss1ActionLine = newBoss1ActionLine;
				boss2ActionLine = newBoss2ActionLine;
				crop3kActionLine = newCrop3kActionLine;
				crop7500ActionLine = newCrop7500ActionLine;

				saveConfig();
			}
		} catch (Exception e) {
			// Silent fail on scrape errors
		}
	}

	/**
	 * Check if an item has the enchanted glint (indicating a completed quest).
	 */
	private static boolean isQuestItemEnchanted(ItemStack stack) {
		try {
			var glintOverride = stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
			return glintOverride != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Parse the progress line to determine if a quest is complete.
	 * Format: "Progress: X/Y" or " | Progress: X,XXX/X,XXX" (with commas)
	 */
	private static boolean parseProgressLine(String line) {
		// Pattern that matches numbers with optional commas: \d+(?:,\d{3})*
		Pattern pattern = Pattern.compile("(\\d+(?:,\\d{3})*)[/\\s]+(\\d+(?:,\\d{3})*)");
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			// Remove commas and parse
			int current = Integer.parseInt(matcher.group(1).replace(",", ""));
			int target = Integer.parseInt(matcher.group(2).replace(",", ""));
			return current >= target;
		}
		return false;
	}

	/**
	 * Extract boss target name from action line.
	 * Examples: " | Kill 3 Overworld Cadaver" -> "Overworld Cadaver"
	 * Format: [Pipe] [Action] [Number] [BossName...]
	 */
	private static String extractBossTarget(String actionLine) {
		// Pattern: word (Kill) followed by a number, then the boss name
		// Handles formats like " | Kill 3 Overworld Cadaver"
		Pattern pattern = Pattern.compile("[A-Za-z]+\\s+\\d+\\s+(.+)");
		Matcher matcher = pattern.matcher(actionLine);
		if (matcher.find()) {
			return matcher.group(1).trim(); // Return everything after the number
		}
		return "Unknown";
	}

	/**
	 * Extract crop name from action line.
	 * Examples: " | Farm 3,000 Rose" -> "Rose", " | Farm 7,500 Firefly Bush" -> "Firefly Bush"
	 * Format: [Pipe] [Action] [Number with commas] [CropName...]
	 */
	private static String extractCropName(String actionLine) {
		// Pattern: word(s) followed by a number (with optional commas), then the crop name
		// Handles formats like " | Farm 3,000 Rose" or "Farm 3000 Rose"
		Pattern pattern = Pattern.compile("[A-Za-z]+\\s+[\\d,]+\\s+(.+)");
		Matcher matcher = pattern.matcher(actionLine);
		if (matcher.find()) {
			return matcher.group(1).trim(); // Return everything after the number
		}
		return "Unknown";
	}

	/**
	 * Sync the timer from the quest screen's clock item.
	 */
	private static void syncTimerFromScreen() {
		if (currentScreen == null) {
			return;
		}

		try {
			Inventory inventory = currentScreen.getScreenHandler().getInventory();
			// Slot 4 is the middle of the top row (0-8 in a 9-wide container)
			ItemStack clockStack = inventory.getStack(4);

			if (!clockStack.isEmpty()) {
				List<Text> lore = ContainerScreenUtils.getLoreLines(clockStack);
				for (Text line : lore) {
					Long remainingMs = parseTimeRemaining(line.getString());
					if (remainingMs != null) {
						questResetMs = System.currentTimeMillis() + remainingMs;
						saveConfig(); // Save the updated timer
						return;
					}
				}

				// Log first failure for debugging
				if (questResetMs == 0 && !lore.isEmpty()) {
					System.out.println("[GuildQuestsManager] Could not parse clock tooltip. Lore lines:");
					for (Text line : lore) {
						System.out.println("  - " + line.getString());
					}
				}
			}
		} catch (Exception e) {
			// Silent fail on timer sync
		}
	}

	/**
	 * Parse a time remaining from a string.
	 * Supports: "mm:ss", "hh:mm:ss", "X minutes", "Xm Ys", "Xm", "Xh"
	 */
	private static Long parseTimeRemaining(String text) {
		// Try "Xm Ys" format (e.g., "55m 30s!" or "55m 30s")
		Pattern msFormatPattern = Pattern.compile("(\\d+)\\s*m\\s+(\\d+)\\s*s");
		Matcher msMatcher = msFormatPattern.matcher(text);
		if (msMatcher.find()) {
			try {
				int m = Integer.parseInt(msMatcher.group(1));
				int s = Integer.parseInt(msMatcher.group(2));
				return (long) (m * 60 + s) * 1000;
			} catch (NumberFormatException ignored) {
			}
		}

		// Try "Xm" format (e.g., "55m!" or "55m")
		Pattern mFormatPattern = Pattern.compile("(\\d+)\\s*m");
		Matcher mFormatMatcher = mFormatPattern.matcher(text);
		if (mFormatMatcher.find()) {
			try {
				int m = Integer.parseInt(mFormatMatcher.group(1));
				return (long) m * 60000;
			} catch (NumberFormatException ignored) {
			}
		}

		// Try "Xs" format (e.g., "30s!" or "30s") - for seconds only when less than 1 minute
		Pattern sFormatPattern = Pattern.compile("^\\s*(\\d+)\\s*s");
		Matcher sFormatMatcher = sFormatPattern.matcher(text);
		if (sFormatMatcher.find()) {
			try {
				int s = Integer.parseInt(sFormatMatcher.group(1));
				return (long) s * 1000;
			} catch (NumberFormatException ignored) {
			}
		}

		// Try "Xh" format (e.g., "2h")
		Pattern hFormatPattern = Pattern.compile("(\\d+)\\s*h");
		Matcher hFormatMatcher = hFormatPattern.matcher(text);
		if (hFormatMatcher.find()) {
			try {
				int h = Integer.parseInt(hFormatMatcher.group(1));
				return (long) h * 3600000;
			} catch (NumberFormatException ignored) {
			}
		}

		// Try mm:ss format
		Pattern mmssPattern = Pattern.compile("(\\d+):(\\d+)");
		Matcher mmssMatcher = mmssPattern.matcher(text);
		if (mmssMatcher.find() && mmssMatcher.groupCount() >= 2) {
			try {
				int m = Integer.parseInt(mmssMatcher.group(1));
				int s = Integer.parseInt(mmssMatcher.group(2));
				// Check if this looks like mm:ss (m < 60 and s < 60)
				if (m < 60 && s < 60) {
					return (long) (m * 60 + s) * 1000;
				}
			} catch (NumberFormatException ignored) {
			}
		}

		// Try hh:mm:ss format
		Pattern hhmmssPattern = Pattern.compile("(\\d+):(\\d+):(\\d+)");
		Matcher hhmmssMatcher = hhmmssPattern.matcher(text);
		if (hhmmssMatcher.find() && hhmmssMatcher.groupCount() >= 3) {
			try {
				int h = Integer.parseInt(hhmmssMatcher.group(1));
				int m = Integer.parseInt(hhmmssMatcher.group(2));
				int s = Integer.parseInt(hhmmssMatcher.group(3));
				return (long) (h * 3600 + m * 60 + s) * 1000;
			} catch (NumberFormatException ignored) {
			}
		}

		// Try "X minutes" format
		Pattern minutesPattern = Pattern.compile("(\\d+)\\s*minutes?");
		Matcher minutesMatcher = minutesPattern.matcher(text);
		if (minutesMatcher.find()) {
			try {
				int m = Integer.parseInt(minutesMatcher.group(1));
				return (long) m * 60000;
			} catch (NumberFormatException ignored) {
			}
		}

		return null;
	}

	/**
	 * Handle quest completion messages from chat.
	 */
	private static void handleQuestCompleteMessage(Text message) {
		String text = message.getString();

		// Guard: must contain both "GUILDS" and "Guild quest complete:"
		if (!text.contains("GUILDS") || !text.contains("Guild quest complete:")) {
			return;
		}

		// Extract the quest description
		int idx = text.indexOf("Guild quest complete: ");
		if (idx == -1) {
			return;
		}

		String description = text.substring(idx + "Guild quest complete: ".length()).trim();

		// Try to match against stored quest action lines (most specific first)
		// Match the exact action line from the quest item
		if (boss1ActionLine.length() > 0 && description.contains(boss1ActionLine.trim())) {
			boss1Complete = true;
			saveConfig();
		}
		if (boss2ActionLine.length() > 0 && description.contains(boss2ActionLine.trim())) {
			boss2Complete = true;
			saveConfig();
		}
		if (crop3kActionLine.length() > 0 && description.contains(crop3kActionLine.trim())) {
			crop3kComplete = true;
			saveConfig();
		}
		if (crop7500ActionLine.length() > 0 && description.contains(crop7500ActionLine.trim())) {
			crop7500Complete = true;
			saveConfig();
		}
	}

	/**
	 * Render the HUD widget at the configured position.
	 */
	public static void renderHud(DrawContext context) {
		// Don't render if disabled or feature disabled
		if (hudRenderingDisabled || !ModConfig.guildQuestsEnabled) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();

		// Apply scaling transformation
		context.getMatrices().pushMatrix();
		context.getMatrices().translate((float)hudX, (float)hudY);
		context.getMatrices().scale(ModConfig.gqScale, ModConfig.gqScale);

		int y = 0;
		int lineHeight = 10;

		// Header: "Guild Quests: MM:SS"
		String header = "Guild Quests: " + formatTimer();
		context.drawTextWithShadow(client.textRenderer, Text.literal(header), 0, y, ModConfig.gqColorHeader);
		y += lineHeight;

		if (dataState == DataState.NO_DATA) {
			// Prompt user to open guild quests
			context.drawTextWithShadow(client.textRenderer, Text.literal("Open /guild to load"), 0, y, ModConfig.gqColorUnknown);
		} else {
			// Show each incomplete quest
			if (!boss1Complete && !boss1ActionLine.isEmpty()) {
				context.drawTextWithShadow(client.textRenderer, Text.literal(boss1ActionLine), 0, y, ModConfig.gqColorLabel);
				y += lineHeight;
			}
			if (!boss2Complete && !boss2ActionLine.isEmpty()) {
				context.drawTextWithShadow(client.textRenderer, Text.literal(boss2ActionLine), 0, y, ModConfig.gqColorLabel);
				y += lineHeight;
			}
			if (!crop3kComplete && !crop3kActionLine.isEmpty()) {
				context.drawTextWithShadow(client.textRenderer, Text.literal(crop3kActionLine), 0, y, ModConfig.gqColorLabel);
				y += lineHeight;
			}
			if (!crop7500Complete && !crop7500ActionLine.isEmpty()) {
				context.drawTextWithShadow(client.textRenderer, Text.literal(crop7500ActionLine), 0, y, ModConfig.gqColorLabel);
				y += lineHeight;
			}
			// If ALL quests complete, show completed message
			if (boss1Complete && boss2Complete && crop3kComplete && crop7500Complete) {
				context.drawTextWithShadow(client.textRenderer, Text.literal("All quests done!"), 0, y, ModConfig.gqColorTarget);
			}
		}

		context.getMatrices().popMatrix();
	}

	/**
	 * Format the remaining time as mm:ss or --:-- if unknown.
	 */
	private static String formatTimer() {
		if (questResetMs == 0) {
			return "--:--";
		}

		long remainingMs = questResetMs - System.currentTimeMillis();
		if (remainingMs < 0) {
			return "--:--";
		}

		long remainingSeconds = remainingMs / 1000;
		long minutes = remainingSeconds / 60;
		long seconds = remainingSeconds % 60;

		return String.format("%d:%02d", minutes, seconds);
	}

	/**
	 * Load configuration from disk.
	 */
	private static void loadConfig() {
		try {
			Path configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
			File configFile = configPath.toFile();

			if (configFile.exists()) {
				String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
				JsonObject json = new Gson().fromJson(content, JsonObject.class);

				if (json.has("guildQuestHudX")) {
					hudX = json.get("guildQuestHudX").getAsInt();
				}
				if (json.has("guildQuestHudY")) {
					hudY = json.get("guildQuestHudY").getAsInt();
				}

				if (json.has("questResetMs")) {
					questResetMs = json.get("questResetMs").getAsLong();
				}

				// Load quest data
				if (json.has("questData")) {
					JsonObject questData = json.getAsJsonObject("questData");
					if (questData.has("boss1Target")) {
						boss1Target = questData.get("boss1Target").getAsString();
					}
					if (questData.has("boss2Target")) {
						boss2Target = questData.get("boss2Target").getAsString();
					}
					if (questData.has("crop3kTarget")) {
						crop3kTarget = questData.get("crop3kTarget").getAsString();
					}
					if (questData.has("crop7500Target")) {
						crop7500Target = questData.get("crop7500Target").getAsString();
					}
					if (questData.has("boss1ActionLine")) {
						boss1ActionLine = questData.get("boss1ActionLine").getAsString();
						prevBoss1ActionLine = boss1ActionLine;  // Initialize prev for change detection
					}
					if (questData.has("boss2ActionLine")) {
						boss2ActionLine = questData.get("boss2ActionLine").getAsString();
						prevBoss2ActionLine = boss2ActionLine;  // Initialize prev for change detection
					}
					if (questData.has("crop3kActionLine")) {
						crop3kActionLine = questData.get("crop3kActionLine").getAsString();
						prevCrop3kActionLine = crop3kActionLine;  // Initialize prev for change detection
					}
					if (questData.has("crop7500ActionLine")) {
						crop7500ActionLine = questData.get("crop7500ActionLine").getAsString();
						prevCrop7500ActionLine = crop7500ActionLine;  // Initialize prev for change detection
					}
					if (questData.has("boss1Complete")) {
						boss1Complete = questData.get("boss1Complete").getAsBoolean();
					}
					if (questData.has("boss2Complete")) {
						boss2Complete = questData.get("boss2Complete").getAsBoolean();
					}
					if (questData.has("crop3kComplete")) {
						crop3kComplete = questData.get("crop3kComplete").getAsBoolean();
					}
					if (questData.has("crop7500Complete")) {
						crop7500Complete = questData.get("crop7500Complete").getAsBoolean();
					}

					// If we loaded quest data, mark state as KNOWN
					if (!boss1Target.isEmpty() || !boss2Target.isEmpty() || !crop3kTarget.isEmpty() || !crop7500Target.isEmpty()) {
						dataState = DataState.KNOWN;
					}
				}
			}
		} catch (Exception e) {
			// If loading fails, use defaults
		}
	}

	/**
	 * Save configuration to disk.
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

			// Update Guild Quests specific properties
			json.addProperty("guildQuestHudX", hudX);
			json.addProperty("guildQuestHudY", hudY);
			json.addProperty("questResetMs", questResetMs);

			// Save quest data
			JsonObject questData = new JsonObject();
			questData.addProperty("boss1Target", boss1Target);
			questData.addProperty("boss2Target", boss2Target);
			questData.addProperty("crop3kTarget", crop3kTarget);
			questData.addProperty("crop7500Target", crop7500Target);
			questData.addProperty("boss1ActionLine", boss1ActionLine);
			questData.addProperty("boss2ActionLine", boss2ActionLine);
			questData.addProperty("crop3kActionLine", crop3kActionLine);
			questData.addProperty("crop7500ActionLine", crop7500ActionLine);
			questData.addProperty("boss1Complete", boss1Complete);
			questData.addProperty("boss2Complete", boss2Complete);
			questData.addProperty("crop3kComplete", crop3kComplete);
			questData.addProperty("crop7500Complete", crop7500Complete);
			json.add("questData", questData);

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String content = gson.toJson(json);

			Files.write(configPath, content.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			// If saving fails, silently ignore
		}
	}
}
