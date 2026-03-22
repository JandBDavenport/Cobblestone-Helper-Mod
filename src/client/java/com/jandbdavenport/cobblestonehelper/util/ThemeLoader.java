package com.jandbdavenport.cobblestonehelper.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.minecraft.util.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Theme system that loads themes from JSON files.
 *
 * Supports both forward and backward compatibility:
 * - Old themes with missing keys load safely (unknown keys skipped, current values untouched)
 * - New themes always contain all registered keys
 * - Adding a new themeable field only requires one registry entry
 */
public class ThemeLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String THEMES_DIR = "config/cobblestonehelper/themes";

	public record LoadedTheme(String name, Map<String, Integer> colors) {}

	/**
	 * Registry: key name → lambda to apply color to ModConfig.
	 * Adding a new themeable field: add one reg(...) call to the static initializer.
	 */
	public static final LinkedHashMap<String, Consumer<Integer>> APPLIERS = new LinkedHashMap<>();

	/**
	 * Registry: key name → lambda to read color from ModConfig.
	 * Used by saveTheme() to collect all current values.
	 */
	public static final LinkedHashMap<String, Supplier<Integer>> READERS = new LinkedHashMap<>();

	static {
		// Shady Summoner colors
		reg("ssColorLabel",      c -> ModConfig.ssColorLabel = c,      () -> ModConfig.ssColorLabel);
		reg("ssColorActive",     c -> ModConfig.ssColorActive = c,     () -> ModConfig.ssColorActive);
		reg("ssColorWarning",    c -> ModConfig.ssColorWarning = c,    () -> ModConfig.ssColorWarning);
		reg("ssColorNormal",     c -> ModConfig.ssColorNormal = c,     () -> ModConfig.ssColorNormal);
		reg("ssColorUnknown",    c -> ModConfig.ssColorUnknown = c,    () -> ModConfig.ssColorUnknown);

		// Bazaar colors
		reg("bzColorLoading",    c -> ModConfig.bzColorLoading = c,    () -> ModConfig.bzColorLoading);
		reg("bzColorComplete",   c -> ModConfig.bzColorComplete = c,   () -> ModConfig.bzColorComplete);

		// Guild Quests colors
		reg("gqColorHeader",     c -> ModConfig.gqColorHeader = c,     () -> ModConfig.gqColorHeader);
		reg("gqColorLabel",      c -> ModConfig.gqColorLabel = c,      () -> ModConfig.gqColorLabel);
		reg("gqColorTarget",     c -> ModConfig.gqColorTarget = c,     () -> ModConfig.gqColorTarget);
		reg("gqColorUnknown",    c -> ModConfig.gqColorUnknown = c,    () -> ModConfig.gqColorUnknown);

		// Farm Warps colors
		reg("fwColorBackground", c -> ModConfig.fwColorBackground = c, () -> ModConfig.fwColorBackground);
		reg("fwColorAccent",     c -> ModConfig.fwColorAccent = c,     () -> ModConfig.fwColorAccent);
		reg("fwColorSlot",       c -> ModConfig.fwColorSlot = c,       () -> ModConfig.fwColorSlot);
	}

	/**
	 * Register a themeable color field.
	 */
	private static void reg(String key, Consumer<Integer> applier, Supplier<Integer> reader) {
		APPLIERS.put(key, applier);
		READERS.put(key, reader);
	}

	/**
	 * Load all themes from config/cobblestonehelper/themes/ directory.
	 * Creates the directory and seeds 5 default themes if it doesn't exist.
	 */
	public static List<LoadedTheme> loadThemes() {
		Path themesPath = Paths.get(THEMES_DIR);

		try {
			// Create directory if it doesn't exist
			if (!Files.exists(themesPath)) {
				Files.createDirectories(themesPath);
				// Seed default themes
				seedDefaultThemes(themesPath);
			}

			// Scan and load all .json files
			return Files.list(themesPath)
				.filter(p -> p.toString().endsWith(".json"))
				.map(ThemeLoader::loadThemeFromFile)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		} catch (Exception e) {
			System.err.println("[ThemeLoader] Error loading themes: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Load a single theme from a JSON file.
	 */
	private static LoadedTheme loadThemeFromFile(Path file) {
		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			JsonObject obj = GSON.fromJson(json, JsonObject.class);

			String name = obj.has("name") ? obj.get("name").getAsString() : file.getFileName().toString();
			Map<String, Integer> colors = new HashMap<>();

			if (obj.has("colors")) {
				JsonObject colorsObj = obj.getAsJsonObject("colors");
				for (String key : colorsObj.keySet()) {
					String hexStr = colorsObj.get(key).getAsString();
					try {
						int color = (int) Long.parseLong(hexStr, 16);
						colors.put(key, color);
					} catch (NumberFormatException e) {
						System.err.println("[ThemeLoader] Invalid color value for " + key + ": " + hexStr);
					}
				}
			}

			return new LoadedTheme(name, colors);
		} catch (Exception e) {
			System.err.println("[ThemeLoader] Error loading theme from " + file + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Save the current ModConfig colors as a new theme file.
	 */
	public static void saveTheme(String name) {
		if (name == null || name.trim().isEmpty()) {
			System.err.println("[ThemeLoader] Theme name cannot be empty");
			return;
		}

		// Sanitize filename
		String filename = name.replaceAll("[/\\:*?\"<>|]", "_") + ".json";
		Path themesPath = Paths.get(THEMES_DIR);
		Path themePath = themesPath.resolve(filename);

		try {
			// Create directory if needed
			Files.createDirectories(themesPath);

			// Build colors map from all registered readers
			Map<String, String> colors = new LinkedHashMap<>();
			for (Map.Entry<String, Supplier<Integer>> entry : READERS.entrySet()) {
				int color = entry.getValue().get();
				String hexStr = String.format("%08X", color);
				colors.put(entry.getKey(), hexStr);
			}

			// Build JSON object
			JsonObject themeObj = new JsonObject();
			themeObj.addProperty("name", name);
			JsonObject colorsObj = new JsonObject();
			for (Map.Entry<String, String> entry : colors.entrySet()) {
				colorsObj.addProperty(entry.getKey(), entry.getValue());
			}
			themeObj.add("colors", colorsObj);

			// Write to file
			String json = GSON.toJson(themeObj);
			Files.writeString(themePath, json, StandardCharsets.UTF_8);
			System.out.println("[ThemeLoader] Saved theme: " + themePath);
		} catch (Exception e) {
			System.err.println("[ThemeLoader] Error saving theme: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Seed 5 default themes to the themes directory.
	 */
	private static void seedDefaultThemes(Path themesPath) {
		Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

		defaults.put("Default", Map.ofEntries(
			Map.entry("ssColorLabel", "FFBB77FF"),
			Map.entry("ssColorActive", "FF00CC00"),
			Map.entry("ssColorWarning", "FFFF4444"),
			Map.entry("ssColorNormal", "FFFFCC00"),
			Map.entry("ssColorUnknown", "FF888888"),
			Map.entry("bzColorLoading", "FFFFCC00"),
			Map.entry("bzColorComplete", "FF00CC00"),
			Map.entry("gqColorHeader", "FF55FF55"),
			Map.entry("gqColorLabel", "FF55FF55"),
			Map.entry("gqColorTarget", "FFFFFFFF"),
			Map.entry("gqColorUnknown", "FFFFFF00"),
			Map.entry("fwColorBackground", "F2111111"),
			Map.entry("fwColorAccent", "FFFF8C00"),
			Map.entry("fwColorSlot", "FF222222")
		));

		defaults.put("Purple", Map.ofEntries(
			Map.entry("ssColorLabel", "FFBB77FF"),
			Map.entry("ssColorActive", "FF00CC00"),
			Map.entry("ssColorWarning", "FFFF4444"),
			Map.entry("ssColorNormal", "FFFFCC00"),
			Map.entry("ssColorUnknown", "FF888888"),
			Map.entry("bzColorLoading", "FFBB77FF"),
			Map.entry("bzColorComplete", "FF00CC00"),
			Map.entry("gqColorHeader", "FFBB77FF"),
			Map.entry("gqColorLabel", "FFBB77FF"),
			Map.entry("gqColorTarget", "FFFFFFFF"),
			Map.entry("gqColorUnknown", "FFFF4444"),
			Map.entry("fwColorBackground", "F2111111"),
			Map.entry("fwColorAccent", "FF7B5EA7"),
			Map.entry("fwColorSlot", "FF222222")
		));

		defaults.put("Green", Map.ofEntries(
			Map.entry("ssColorLabel", "FF55FF55"),
			Map.entry("ssColorActive", "FF00FF00"),
			Map.entry("ssColorWarning", "FFFF0000"),
			Map.entry("ssColorNormal", "FFFFFF00"),
			Map.entry("ssColorUnknown", "FF777777"),
			Map.entry("bzColorLoading", "FF00FF00"),
			Map.entry("bzColorComplete", "FF00AA00"),
			Map.entry("gqColorHeader", "FF55FF55"),
			Map.entry("gqColorLabel", "FF55FF55"),
			Map.entry("gqColorTarget", "FFFFFFFF"),
			Map.entry("gqColorUnknown", "FF00AA00"),
			Map.entry("fwColorBackground", "F2111111"),
			Map.entry("fwColorAccent", "FF00CC00"),
			Map.entry("fwColorSlot", "FF222222")
		));

		defaults.put("Blue", Map.ofEntries(
			Map.entry("ssColorLabel", "FF5599FF"),
			Map.entry("ssColorActive", "FF0099FF"),
			Map.entry("ssColorWarning", "FFFF6600"),
			Map.entry("ssColorNormal", "FFFFFF99"),
			Map.entry("ssColorUnknown", "FF666666"),
			Map.entry("bzColorLoading", "FF0099FF"),
			Map.entry("bzColorComplete", "FF00CCFF"),
			Map.entry("gqColorHeader", "FF5599FF"),
			Map.entry("gqColorLabel", "FF5599FF"),
			Map.entry("gqColorTarget", "FFFFFFFF"),
			Map.entry("gqColorUnknown", "FF00CCFF"),
			Map.entry("fwColorBackground", "F2111111"),
			Map.entry("fwColorAccent", "FF0099FF"),
			Map.entry("fwColorSlot", "FF222222")
		));

		defaults.put("Warm", Map.ofEntries(
			Map.entry("ssColorLabel", "FFFF8844"),
			Map.entry("ssColorActive", "FFFFCC00"),
			Map.entry("ssColorWarning", "FFFF0000"),
			Map.entry("ssColorNormal", "FFFFDD00"),
			Map.entry("ssColorUnknown", "FFCC6600"),
			Map.entry("bzColorLoading", "FFFF6600"),
			Map.entry("bzColorComplete", "FFFF3300"),
			Map.entry("gqColorHeader", "FFFFDD00"),
			Map.entry("gqColorLabel", "FFFFDD00"),
			Map.entry("gqColorTarget", "FFFFFFFF"),
			Map.entry("gqColorUnknown", "FFFF6600"),
			Map.entry("fwColorBackground", "F2111111"),
			Map.entry("fwColorAccent", "FFFF6600"),
			Map.entry("fwColorSlot", "FF222222")
		));

		for (Map.Entry<String, Map<String, String>> entry : defaults.entrySet()) {
			String themeName = entry.getKey();
			Map<String, String> colors = entry.getValue();

			try {
				JsonObject themeObj = new JsonObject();
				themeObj.addProperty("name", themeName);
				JsonObject colorsObj = new JsonObject();
				for (Map.Entry<String, String> colorEntry : colors.entrySet()) {
					colorsObj.addProperty(colorEntry.getKey(), colorEntry.getValue());
				}
				themeObj.add("colors", colorsObj);

				Path themePath = themesPath.resolve(themeName + ".json");
				String json = GSON.toJson(themeObj);
				Files.writeString(themePath, json, StandardCharsets.UTF_8);
			} catch (Exception e) {
				System.err.println("[ThemeLoader] Error seeding theme " + themeName + ": " + e.getMessage());
			}
		}
	}
}
