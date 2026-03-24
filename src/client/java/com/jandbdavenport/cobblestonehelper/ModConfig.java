package com.jandbdavenport.cobblestonehelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized configuration system for all mod features.
 * Handles reading/writing to config/cobblestonehelper.json with proper merging.
 */
public class ModConfig {
	// Feature toggles
	public static boolean shadySummonerEnabled = true;
	public static boolean bazaarEnabled = true;
	public static boolean guildQuestsEnabled = true;

	// Plant Hitbox toggles (individual plants)
	public static boolean plantHitboxBrownMushroom = false;
	public static boolean plantHitboxRedMushroom = false;
	public static boolean plantHitboxCrimsonFungus = false;
	public static boolean plantHitboxWarpedFungus = false;
	public static boolean plantHitboxCactusFlower = false;
	public static boolean plantHitboxTallDryGrass = false;
	public static boolean plantHitboxWildflowers = false;

	// Farm Warps display toggles
	public static boolean farmWarpsGuildQuestHighlight = true;
	public static boolean farmWarpsShowBazaarHighlight = true;

	// Auto Respawn
	public static boolean autoRespawnEnabled = false;
	public static float autoRespawnDelaySeconds = 3.0f;

	// Shady Summoner colors (ARGB ints) + scale
	public static int ssColorLabel   = 0xFFBB77FF;
	public static int ssColorActive  = 0xFF00CC00;
	public static int ssColorWarning = 0xFFFF4444;
	public static int ssColorNormal  = 0xFFFFCC00;
	public static int ssColorUnknown = 0xFF888888;
	public static float ssScale = 1.0f;

	// Bazaar colors + scale
	public static int bzColorLoading  = 0xFFFFCC00;
	public static int bzColorComplete = 0xFF00CC00;
	public static float bzScale = 1.0f;

	// Guild Quests colors + scale
	public static int gqColorHeader  = 0xFF55FF55;
	public static int gqColorLabel   = 0xFF55FF55;
	public static int gqColorTarget  = 0xFFFFFFFF;
	public static int gqColorUnknown = 0xFFFFFF00;
	public static float gqScale = 1.0f;

	// Config Menu UI colors (used for config screen styling)
	public static int fwColorBackground = 0xF2111111;
	public static int fwColorAccent     = 0xFFFF8C00;
	public static int fwColorSlot       = 0xFF222222;

	// Farm Warps Menu colors (separate from config menu)
	public static int fwMenuColorBackground = 0xF2111111;
	public static int fwMenuColorAccent     = 0xFFFF8C00;
	public static int fwMenuColorSlot       = 0xFF222222;

	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "cobblestonehelper.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void init() {
		System.out.println("[ModConfig] Initializing ModConfig...");
		loadConfig();
		System.out.println("[ModConfig] ✓ ModConfig initialized");
	}

	public static void loadConfig() {
		try {
			Path configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
			if (!Files.exists(configPath)) {
				System.out.println("[ModConfig] Config file not found, using defaults");
				return;
			}

			try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
				if (json == null) {
					System.out.println("[ModConfig] Config file is empty, using defaults");
					return;
				}

				// Load feature toggles
				shadySummonerEnabled = getBoolean(json, "shadySummonerEnabled", true);
				bazaarEnabled = getBoolean(json, "bazaarEnabled", true);
				guildQuestsEnabled = getBoolean(json, "guildQuestsEnabled", true);

				// Load Plant Hitbox toggles (default to false/disabled)
				plantHitboxBrownMushroom = getBoolean(json, "plantHitboxBrownMushroom", false);
				plantHitboxRedMushroom = getBoolean(json, "plantHitboxRedMushroom", false);
				plantHitboxCrimsonFungus = getBoolean(json, "plantHitboxCrimsonFungus", false);
				plantHitboxWarpedFungus = getBoolean(json, "plantHitboxWarpedFungus", false);
				plantHitboxCactusFlower = getBoolean(json, "plantHitboxCactusFlower", false);
				plantHitboxTallDryGrass = getBoolean(json, "plantHitboxTallDryGrass", false);
				plantHitboxWildflowers = getBoolean(json, "plantHitboxWildflowers", false);

				// Load Farm Warps toggles
				farmWarpsGuildQuestHighlight = getBoolean(json, "farmWarpsGuildQuestHighlight", true);
				farmWarpsShowBazaarHighlight = getBoolean(json, "farmWarpsShowBazaarHighlight", true);

				// Load Auto Respawn
				autoRespawnEnabled = getBoolean(json, "autoRespawnEnabled", false);
				autoRespawnDelaySeconds = getFloat(json, "autoRespawnDelaySeconds", 3.0f);

				// Load Shady Summoner colors + scale
				ssColorLabel   = getArgbColor(json, "ssColorLabel", 0xFFBB77FF);
				ssColorActive  = getArgbColor(json, "ssColorActive", 0xFF00CC00);
				ssColorWarning = getArgbColor(json, "ssColorWarning", 0xFFFF4444);
				ssColorNormal  = getArgbColor(json, "ssColorNormal", 0xFFFFCC00);
				ssColorUnknown = getArgbColor(json, "ssColorUnknown", 0xFF888888);
				ssScale = getFloat(json, "ssScale", 1.0f);

				// Load Bazaar colors + scale
				bzColorLoading  = getArgbColor(json, "bzColorLoading", 0xFFFFCC00);
				bzColorComplete = getArgbColor(json, "bzColorComplete", 0xFF00CC00);
				bzScale = getFloat(json, "bzScale", 1.0f);

				// Load Guild Quests colors + scale
				gqColorHeader  = getArgbColor(json, "gqColorHeader", 0xFF55FF55);
				gqColorLabel   = getArgbColor(json, "gqColorLabel", 0xFF55FF55);
				gqColorTarget  = getArgbColor(json, "gqColorTarget", 0xFFFFFFFF);
				gqColorUnknown = getArgbColor(json, "gqColorUnknown", 0xFFFFFF00);
				gqScale = getFloat(json, "gqScale", 1.0f);

				// Load Farm Warps colors (config menu)
				fwColorBackground = getArgbColor(json, "fwColorBackground", 0xF2111111);
				fwColorAccent     = getArgbColor(json, "fwColorAccent", 0xFFFF8C00);
				fwColorSlot       = getArgbColor(json, "fwColorSlot", 0xFF222222);

				// Load Farm Warps Menu colors (farm warp screen)
				fwMenuColorBackground = getArgbColor(json, "fwMenuColorBackground", 0xF2111111);
				fwMenuColorAccent     = getArgbColor(json, "fwMenuColorAccent", 0xFFFF8C00);
				fwMenuColorSlot       = getArgbColor(json, "fwMenuColorSlot", 0xFF222222);

				System.out.println("[ModConfig] ✓ Config loaded successfully");
			}
		} catch (Exception e) {
			System.out.println("[ModConfig] ✗ Error loading config: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void saveConfig() {
		try {
			Path configDir = Paths.get(CONFIG_DIR);
			Files.createDirectories(configDir);

			Path configPath = configDir.resolve(CONFIG_FILE);

			// Read existing JSON or create empty object
			JsonObject json = new JsonObject();
			if (Files.exists(configPath)) {
				try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
					JsonObject existing = GSON.fromJson(reader, JsonObject.class);
					if (existing != null) {
						json = existing;
					}
				}
			}

			// Update only ModConfig's keys (preserve other managers' keys)
			json.addProperty("shadySummonerEnabled", shadySummonerEnabled);
			json.addProperty("bazaarEnabled", bazaarEnabled);
			json.addProperty("guildQuestsEnabled", guildQuestsEnabled);

			// Plant Hitbox toggles
			json.addProperty("plantHitboxBrownMushroom", plantHitboxBrownMushroom);
			json.addProperty("plantHitboxRedMushroom", plantHitboxRedMushroom);
			json.addProperty("plantHitboxCrimsonFungus", plantHitboxCrimsonFungus);
			json.addProperty("plantHitboxWarpedFungus", plantHitboxWarpedFungus);
			json.addProperty("plantHitboxCactusFlower", plantHitboxCactusFlower);
			json.addProperty("plantHitboxTallDryGrass", plantHitboxTallDryGrass);
			json.addProperty("plantHitboxWildflowers", plantHitboxWildflowers);

			json.addProperty("farmWarpsGuildQuestHighlight", farmWarpsGuildQuestHighlight);
			json.addProperty("farmWarpsShowBazaarHighlight", farmWarpsShowBazaarHighlight);

			// Auto Respawn
			json.addProperty("autoRespawnEnabled", autoRespawnEnabled);
			json.addProperty("autoRespawnDelaySeconds", autoRespawnDelaySeconds);

			// Shady Summoner
			json.addProperty("ssColorLabel", String.format("%08X", ssColorLabel));
			json.addProperty("ssColorActive", String.format("%08X", ssColorActive));
			json.addProperty("ssColorWarning", String.format("%08X", ssColorWarning));
			json.addProperty("ssColorNormal", String.format("%08X", ssColorNormal));
			json.addProperty("ssColorUnknown", String.format("%08X", ssColorUnknown));
			json.addProperty("ssScale", ssScale);

			// Bazaar
			json.addProperty("bzColorLoading", String.format("%08X", bzColorLoading));
			json.addProperty("bzColorComplete", String.format("%08X", bzColorComplete));
			json.addProperty("bzScale", bzScale);

			// Guild Quests
			json.addProperty("gqColorHeader", String.format("%08X", gqColorHeader));
			json.addProperty("gqColorLabel", String.format("%08X", gqColorLabel));
			json.addProperty("gqColorTarget", String.format("%08X", gqColorTarget));
			json.addProperty("gqColorUnknown", String.format("%08X", gqColorUnknown));
			json.addProperty("gqScale", gqScale);

			// Farm Warps (config menu colors)
			json.addProperty("fwColorBackground", String.format("%08X", fwColorBackground));
			json.addProperty("fwColorAccent", String.format("%08X", fwColorAccent));
			json.addProperty("fwColorSlot", String.format("%08X", fwColorSlot));

			// Farm Warps Menu colors
			json.addProperty("fwMenuColorBackground", String.format("%08X", fwMenuColorBackground));
			json.addProperty("fwMenuColorAccent", String.format("%08X", fwMenuColorAccent));
			json.addProperty("fwMenuColorSlot", String.format("%08X", fwMenuColorSlot));

			// Write back to file
			Files.writeString(configPath, GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (Exception e) {
			System.out.println("[ModConfig] ✗ Error saving config: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
		return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
	}

	private static float getFloat(JsonObject json, String key, float defaultValue) {
		return json.has(key) ? json.get(key).getAsFloat() : defaultValue;
	}

	private static int getArgbColor(JsonObject json, String key, int defaultValue) {
		if (!json.has(key)) {
			return defaultValue;
		}
		try {
			String hexString = json.get(key).getAsString();
			return (int) Long.parseLong(hexString, 16);
		} catch (Exception e) {
			System.out.println("[ModConfig] ✗ Error parsing color " + key + ": " + e.getMessage());
			return defaultValue;
		}
	}
}
