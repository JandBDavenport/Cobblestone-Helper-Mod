package com.jandbdavenport.cobblestonehelper.gui;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import com.jandbdavenport.cobblestonehelper.features.BazaarManager;
import com.jandbdavenport.cobblestonehelper.features.GuildQuestsManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Comprehensive configuration screen for Cobblestone Helper mod.
 * Features collapsible sections, color pickers, scale buttons, and theme presets.
 *
 * Uses modern widget styling with flat design, borders, and color-coded elements.
 */
public class ConfigScreen extends Screen {
	// ============================================================================
	// Color Palette Constants
	// ============================================================================

	private static final int PANEL_BG = 0xFF1A1A1A;        // Dark background
	private static final int PANEL_INNER = 0xFF242424;     // Slightly lighter panel interior
	private static final int PANEL_HEADER_BG = 0xFF0D0D0D; // Very dark header strip
	private static final int ACCENT = 0xFF7B5EA7;          // Purple accent for section dividers

	private static final int TEXT_PRIMARY = 0xFFFFFFFF;    // White text
	private static final int TEXT_SECONDARY = 0xFFAAAAAA;  // Gray text for hints
	private static final int TEXT_DISABLED = 0xFF666666;   // Darker gray for disabled text

	private static final int BTN_BG = 0xFF2A2A2A;          // Button background
	private static final int BTN_BORDER = 0xFF444444;      // Button border
	private static final int BTN_HOVER_BG = 0xFF333333;    // Hovered button background
	private static final int BTN_TEXT = 0xFFFFFFFF;        // Button text

	private static final int TOGGLE_ON_BG = 0xFF00AA00;    // Green for enabled
	private static final int TOGGLE_OFF_BG = 0xFFAA0000;   // Red for disabled
	private static final int TOGGLE_TEXT = 0xFFFFFFFF;     // White text on toggles

	private static final int SCROLL_TRACK = 0x4D333333;    // Semi-transparent scrollbar track
	private static final int SCROLL_THUMB = 0xFF6699FF;    // Modern blue scrollbar thumb
	private static final int SCROLL_THUMB_LIGHT = 0xFF8ABBFF;   // Light accent on thumb
	private static final int SCROLL_THUMB_DARK = 0xFF4477DD;    // Dark accent on thumb

	private final Screen parentScreen;
	private int scrollOffset = 0;
	private int maxScroll = 0;
	private static final int CONTENT_WIDTH = 300;
	private static final int SCROLL_AREA_HEIGHT = 350;

	// Layout constants
	private static final int TITLE_START_Y = 10;
	private static final int TITLE_HEIGHT = 35;  // Title goes from y=10 to y=45
	private static final int TITLE_BOTTOM_Y = 45;
	private static final int TITLE_GAP = 10;     // Gap between title and content
	private static final int CONTENT_START_Y = TITLE_BOTTOM_Y + TITLE_GAP;  // y=55
	private static final int CONTENT_BOTTOM_MARGIN = 40;

	// Section collapse state
	private static final Map<String, Boolean> sectionExpanded = new HashMap<>();
	static {
		sectionExpanded.put("shadySummoner", false);
		sectionExpanded.put("bazaar", false);
		sectionExpanded.put("guildQuests", false);
		sectionExpanded.put("farmWarps", false);
		sectionExpanded.put("plantHitbox", false);
	}

	// Section Y positions for rendering background cards
	private int shadySummonerSectionStart = 0;
	private int shadySummonerSectionEnd = 0;
	private int bazaarSectionStart = 0;
	private int bazaarSectionEnd = 0;
	private int guildQuestsSectionStart = 0;
	private int guildQuestsSectionEnd = 0;
	private int farmWarpsSectionStart = 0;
	private int farmWarpsSectionEnd = 0;
	private int plantHitboxSectionStart = 0;
	private int plantHitboxSectionEnd = 0;

	// Color picker widgets (indexed by feature)
	private final Map<String, HexColorPickerWidget> colorPickers = new HashMap<>();

	// Record for pairing labels with color picker widgets
	record ColorPickerEntry(String label, HexColorPickerWidget picker) {}
	private final List<ColorPickerEntry> colorPickerEntries = new ArrayList<>();

	// Custom button types for styling
	private static final int BUTTON_TYPE_NORMAL = 0;
	private static final int BUTTON_TYPE_TOGGLE = 1;
	private static final int BUTTON_TYPE_HEADER = 2;
	private static final int BUTTON_TYPE_SCALE = 3;

	// Store button styling info
	private final Map<ButtonWidget, Integer> buttonTypes = new HashMap<>();
	private final Map<ButtonWidget, Boolean> buttonToggleStates = new HashMap<>();
	private final Map<ButtonWidget, Boolean> buttonScaleSelected = new HashMap<>();

	public ConfigScreen(Screen parentScreen) {
		super(Text.literal("Cobblestone Helper Config"));
		this.parentScreen = parentScreen;
	}

	@Override
	protected void init() {
		clearChildren();
		colorPickers.clear();
		colorPickerEntries.clear();
		buttonTypes.clear();
		buttonToggleStates.clear();
		buttonScaleSelected.clear();

		// Estimate content height for scroll calculation
		int estimatedHeight = 0;
		estimatedHeight += 50; // Shady Summoner section
		estimatedHeight += sectionExpanded.get("shadySummoner") ? 200 : 0;
		estimatedHeight += 50; // Bazaar section
		estimatedHeight += sectionExpanded.get("bazaar") ? 200 : 0;
		estimatedHeight += 50; // Guild Quests section
		estimatedHeight += sectionExpanded.get("guildQuests") ? 200 : 0;
		estimatedHeight += 50; // Farm Warps section
		estimatedHeight += sectionExpanded.get("farmWarps") ? 150 : 0;
		estimatedHeight += 50; // Plant Hitbox section
		estimatedHeight += sectionExpanded.get("plantHitbox") ? 100 : 0;

		// Content box height is fixed; content scrolls inside it
		int contentBottomY = this.height - CONTENT_BOTTOM_MARGIN;
		int contentBoxHeight = contentBottomY - CONTENT_START_Y;
		maxScroll = Math.max(0, estimatedHeight - contentBoxHeight);

		int centerX = this.width / 2;

		// SHADY SUMMONER SECTION
		int yPos = CONTENT_START_Y + 5 - scrollOffset;  // 5px padding inside box
		shadySummonerSectionStart = yPos;
		yPos = buildShadySummonerSection(centerX, yPos);
		shadySummonerSectionEnd = yPos;

		// BAZAAR SECTION
		bazaarSectionStart = yPos;
		yPos = buildBazaarSection(centerX, yPos);
		bazaarSectionEnd = yPos;

		// GUILD QUESTS SECTION
		guildQuestsSectionStart = yPos;
		yPos = buildGuildQuestsSection(centerX, yPos);
		guildQuestsSectionEnd = yPos;

		// FARM WARPS SECTION
		farmWarpsSectionStart = yPos;
		yPos = buildFarmWarpsSection(centerX, yPos);
		farmWarpsSectionEnd = yPos;

		// PLANT HITBOX SECTION
		plantHitboxSectionStart = yPos;
		yPos = buildPlantHitboxSection(centerX, yPos);
		plantHitboxSectionEnd = yPos;

		// CLOSE BUTTON (fixed at bottom of content box, not scrollable)
		ButtonWidget closeBtn = ButtonWidget.builder(Text.literal("Close"), button -> this.close())
			.dimensions(centerX - 50, contentBottomY - 25, 100, 20)
			.build();
		buttonTypes.put(closeBtn, BUTTON_TYPE_NORMAL);
		this.addDrawableChild(closeBtn);

		// Add all color picker fields as drawable children
		for (HexColorPickerWidget picker : colorPickers.values()) {
			this.addDrawableChild(picker.getField());
		}
	}

	/**
	 * Create a styled button widget and track its type
	 */
	private ButtonWidget addStyledButton(int x, int y, int width, int height, Text text,
			ButtonWidget.PressAction onPress, int buttonType) {
		ButtonWidget btn = ButtonWidget.builder(text, onPress)
			.dimensions(x, y, width, height)
			.build();
		this.addDrawableChild(btn);
		buttonTypes.put(btn, buttonType);
		return btn;
	}

	/**
	 * Create a toggle button and track its state
	 */
	private ButtonWidget addToggleButton(int x, int y, int width, int height, Text text,
			boolean isOn, ButtonWidget.PressAction onPress) {
		ButtonWidget btn = addStyledButton(x, y, width, height, text, onPress, BUTTON_TYPE_TOGGLE);
		buttonToggleStates.put(btn, isOn);
		return btn;
	}

	/**
	 * Create a header button
	 */
	private ButtonWidget addHeaderButton(int x, int y, int width, int height, Text text,
			ButtonWidget.PressAction onPress) {
		return addStyledButton(x, y, width, height, text, onPress, BUTTON_TYPE_HEADER);
	}

	/**
	 * Create a scale button and track if it's selected
	 */
	private ButtonWidget addScaleButton(int x, int y, int width, int height, Text text,
			boolean isSelected, ButtonWidget.PressAction onPress) {
		ButtonWidget btn = addStyledButton(x, y, width, height, text, onPress, BUTTON_TYPE_SCALE);
		buttonScaleSelected.put(btn, isSelected);
		return btn;
	}

	/**
	 * Build Shady Summoner section with modern widgets
	 */
	private int buildShadySummonerSection(int centerX, int yPos) {
		// Section header (collapsible)
		boolean expanded = sectionExpanded.get("shadySummoner");
		String headerText = (expanded ? "▼" : "▶") + " Shady Summoner";
		addHeaderButton(centerX - 140, yPos, 280, 20, Text.literal(headerText), button -> {
			sectionExpanded.put("shadySummoner", !sectionExpanded.get("shadySummoner"));
			this.init();
		});
		yPos += 25;

		if (!expanded) {
			return yPos;
		}

		// Enable toggle
		addToggleButton(centerX - 140, yPos, 280, 20,
			Text.literal(ModConfig.shadySummonerEnabled ? "✓ Enabled" : "✗ Disabled"),
			ModConfig.shadySummonerEnabled, button -> {
				ModConfig.shadySummonerEnabled = !ModConfig.shadySummonerEnabled;
				ModConfig.saveConfig();
				this.init();
			});
		yPos += 25;

		// Set Position button
		addStyledButton(centerX - 140, yPos, 280, 20, Text.literal("Set Position..."), button ->
			this.client.setScreen(new HudPositionScreen(com.jandbdavenport.cobblestonehelper.features.ShadySummonerManager.WIDGET, this)),
			BUTTON_TYPE_NORMAL);
		yPos += 25;

		// Scale buttons
		yPos = drawScaleButtons(centerX, yPos, "Shady Summoner Scale:", ModConfig.ssScale, newScale -> {
			ModConfig.ssScale = newScale;
			ModConfig.saveConfig();
			this.init();
		});

		// Color pickers
		yPos = drawColorPicker(centerX, yPos, "Label Color:", "ssColorLabel", ModConfig.ssColorLabel, color -> {
			ModConfig.ssColorLabel = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Active Color:", "ssColorActive", ModConfig.ssColorActive, color -> {
			ModConfig.ssColorActive = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Warning Color:", "ssColorWarning", ModConfig.ssColorWarning, color -> {
			ModConfig.ssColorWarning = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Normal Color:", "ssColorNormal", ModConfig.ssColorNormal, color -> {
			ModConfig.ssColorNormal = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Unknown Color:", "ssColorUnknown", ModConfig.ssColorUnknown, color -> {
			ModConfig.ssColorUnknown = color;
			ModConfig.saveConfig();
		});

		// Theme presets
		yPos = drawThemePresets(centerX, yPos, "Shady Summoner Theme:", "shadySummoner");

		return yPos;
	}

	/**
	 * Build Bazaar section with modern widgets
	 */
	private int buildBazaarSection(int centerX, int yPos) {
		boolean expanded = sectionExpanded.get("bazaar");
		String headerText = (expanded ? "▼" : "▶") + " Bazaar";
		addHeaderButton(centerX - 140, yPos, 280, 20, Text.literal(headerText), button -> {
			sectionExpanded.put("bazaar", !sectionExpanded.get("bazaar"));
			this.init();
		});
		yPos += 25;

		if (!expanded) {
			return yPos;
		}

		// Enable toggle
		addToggleButton(centerX - 140, yPos, 280, 20,
			Text.literal(ModConfig.bazaarEnabled ? "✓ Enabled" : "✗ Disabled"),
			ModConfig.bazaarEnabled, button -> {
				ModConfig.bazaarEnabled = !ModConfig.bazaarEnabled;
				ModConfig.saveConfig();
				this.init();
			});
		yPos += 25;

		// Set Position button
		addStyledButton(centerX - 140, yPos, 280, 20, Text.literal("Set Position..."), button ->
			this.client.setScreen(new HudPositionScreen(BazaarManager.WIDGET, this)),
			BUTTON_TYPE_NORMAL);
		yPos += 25;

		// Scale buttons
		yPos = drawScaleButtons(centerX, yPos, "Bazaar Scale:", ModConfig.bzScale, newScale -> {
			ModConfig.bzScale = newScale;
			ModConfig.saveConfig();
			this.init();
		});

		// Color pickers
		yPos = drawColorPicker(centerX, yPos, "Loading Color:", "bzColorLoading", ModConfig.bzColorLoading, color -> {
			ModConfig.bzColorLoading = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Complete Color:", "bzColorComplete", ModConfig.bzColorComplete, color -> {
			ModConfig.bzColorComplete = color;
			ModConfig.saveConfig();
		});

		// Clear Cache button
		addStyledButton(centerX - 140, yPos, 280, 20, Text.literal("Clear Bazaar Cache"), button -> {
			BazaarManager.clearCache();
			ModConfig.saveConfig();
		}, BUTTON_TYPE_NORMAL);
		yPos += 25;

		// Theme presets
		yPos = drawThemePresets(centerX, yPos, "Bazaar Theme:", "bazaar");

		return yPos;
	}

	/**
	 * Build Guild Quests section with modern widgets
	 */
	private int buildGuildQuestsSection(int centerX, int yPos) {
		boolean expanded = sectionExpanded.get("guildQuests");
		String headerText = (expanded ? "▼" : "▶") + " Guild Quests";
		addHeaderButton(centerX - 140, yPos, 280, 20, Text.literal(headerText), button -> {
			sectionExpanded.put("guildQuests", !sectionExpanded.get("guildQuests"));
			this.init();
		});
		yPos += 25;

		if (!expanded) {
			return yPos;
		}

		// Enable toggle
		addToggleButton(centerX - 140, yPos, 280, 20,
			Text.literal(ModConfig.guildQuestsEnabled ? "✓ Enabled" : "✗ Disabled"),
			ModConfig.guildQuestsEnabled, button -> {
				ModConfig.guildQuestsEnabled = !ModConfig.guildQuestsEnabled;
				ModConfig.saveConfig();
				this.init();
			});
		yPos += 25;

		// Scale buttons
		yPos = drawScaleButtons(centerX, yPos, "Guild Quests Scale:", ModConfig.gqScale, newScale -> {
			ModConfig.gqScale = newScale;
			ModConfig.saveConfig();
			this.init();
		});

		// Color pickers
		yPos = drawColorPicker(centerX, yPos, "Header Color:", "gqColorHeader", ModConfig.gqColorHeader, color -> {
			ModConfig.gqColorHeader = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Label Color:", "gqColorLabel", ModConfig.gqColorLabel, color -> {
			ModConfig.gqColorLabel = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Target Color:", "gqColorTarget", ModConfig.gqColorTarget, color -> {
			ModConfig.gqColorTarget = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Unknown Color:", "gqColorUnknown", ModConfig.gqColorUnknown, color -> {
			ModConfig.gqColorUnknown = color;
			ModConfig.saveConfig();
		});

		// Clear Cache button
		addStyledButton(centerX - 140, yPos, 280, 20, Text.literal("Clear Guild Quests Cache"), button -> {
			GuildQuestsManager.clearCache();
			ModConfig.saveConfig();
		}, BUTTON_TYPE_NORMAL);
		yPos += 25;

		// Theme presets
		yPos = drawThemePresets(centerX, yPos, "Guild Quests Theme:", "guildQuests");

		return yPos;
	}

	/**
	 * Build Farm Warps section with modern widgets
	 */
	private int buildFarmWarpsSection(int centerX, int yPos) {
		boolean expanded = sectionExpanded.get("farmWarps");
		String headerText = (expanded ? "▼" : "▶") + " Farm Warps";
		addHeaderButton(centerX - 140, yPos, 280, 20, Text.literal(headerText), button -> {
			sectionExpanded.put("farmWarps", !sectionExpanded.get("farmWarps"));
			this.init();
		});
		yPos += 25;

		if (!expanded) {
			return yPos;
		}

		// Guild Quest highlight toggle
		addToggleButton(centerX - 140, yPos, 135, 20,
			Text.literal(ModConfig.farmWarpsGuildQuestHighlight ? "✓ GQ Highlight" : "✗ GQ Highlight"),
			ModConfig.farmWarpsGuildQuestHighlight, button -> {
				ModConfig.farmWarpsGuildQuestHighlight = !ModConfig.farmWarpsGuildQuestHighlight;
				ModConfig.saveConfig();
				this.init();
			});

		// Bazaar highlight toggle
		addToggleButton(centerX - 5, yPos, 135, 20,
			Text.literal(ModConfig.farmWarpsShowBazaarHighlight ? "✓ Bazaar Highlight" : "✗ Bazaar Highlight"),
			ModConfig.farmWarpsShowBazaarHighlight, button -> {
				ModConfig.farmWarpsShowBazaarHighlight = !ModConfig.farmWarpsShowBazaarHighlight;
				ModConfig.saveConfig();
				this.init();
			});
		yPos += 25;

		// Color pickers
		yPos = drawColorPicker(centerX, yPos, "Background:", "fwColorBackground", ModConfig.fwColorBackground, color -> {
			ModConfig.fwColorBackground = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Accent Color:", "fwColorAccent", ModConfig.fwColorAccent, color -> {
			ModConfig.fwColorAccent = color;
			ModConfig.saveConfig();
		});

		yPos = drawColorPicker(centerX, yPos, "Slot Color:", "fwColorSlot", ModConfig.fwColorSlot, color -> {
			ModConfig.fwColorSlot = color;
			ModConfig.saveConfig();
		});

		return yPos;
	}

	/**
	 * Build Plant Hitbox section with modern widgets
	 */
	private int buildPlantHitboxSection(int centerX, int yPos) {
		boolean expanded = sectionExpanded.get("plantHitbox");
		String headerText = (expanded ? "▼" : "▶") + " Plant Hitbox";
		addHeaderButton(centerX - 140, yPos, 280, 20, Text.literal(headerText), button -> {
			sectionExpanded.put("plantHitbox", !sectionExpanded.get("plantHitbox"));
			this.init();
		});
		yPos += 25;

		if (!expanded) {
			return yPos;
		}

		// Enable toggle
		addToggleButton(centerX - 140, yPos, 280, 20,
			Text.literal(ModConfig.plantHitboxEnabled ? "✓ Enabled" : "✗ Disabled"),
			ModConfig.plantHitboxEnabled, button -> {
				ModConfig.plantHitboxEnabled = !ModConfig.plantHitboxEnabled;
				ModConfig.saveConfig();
				this.init();
			});
		yPos += 25;

		return yPos;
	}

	/**
	 * Draw scale buttons (0.75x, 1.0x, 1.25x, 1.5x) with modern styling
	 */
	private int drawScaleButtons(int centerX, int yPos, String label, float currentScale, Consumer<Float> onChanged) {
		yPos += 5;

		// Normalize scale for comparison (avoid floating point issues)
		float normalized = Math.round(currentScale * 100) / 100.0f;

		// 0.75x
		addScaleButton(centerX - 140, yPos, 65, 20,
			Text.literal(normalized == 0.75f ? "●0.75x" : "○0.75x"),
			normalized == 0.75f,
			button -> onChanged.accept(0.75f));

		// 1.0x
		addScaleButton(centerX - 70, yPos, 65, 20,
			Text.literal(normalized == 1.0f ? "●1.0x" : "○1.0x"),
			normalized == 1.0f,
			button -> onChanged.accept(1.0f));

		// 1.25x
		addScaleButton(centerX, yPos, 65, 20,
			Text.literal(normalized == 1.25f ? "●1.25x" : "○1.25x"),
			normalized == 1.25f,
			button -> onChanged.accept(1.25f));

		// 1.5x
		addScaleButton(centerX + 70, yPos, 65, 20,
			Text.literal(normalized == 1.5f ? "●1.5x" : "○1.5x"),
			normalized == 1.5f,
			button -> onChanged.accept(1.5f));

		return yPos + 25;
	}

	/**
	 * Draw a color picker (label + hex field + preview box) with modern styling
	 */
	private int drawColorPicker(int centerX, int yPos, String label, String key, int currentColor, Consumer<Integer> onChanged) {
		// Create and register color picker
		HexColorPickerWidget picker = new HexColorPickerWidget(
			this.textRenderer,
			centerX + 10,
			yPos,
			currentColor,
			onChanged
		);
		colorPickers.put(key, picker);
		colorPickerEntries.add(new ColorPickerEntry(label, picker));

		// Draw preview will happen in render()
		return yPos + 20;
	}

	/**
	 * Draw theme preset buttons with modern styling
	 */
	private int drawThemePresets(int centerX, int yPos, String label, String section) {
		yPos += 10;

		// Define themes
		Map<String, Map<String, Integer>> themes = new HashMap<>();

		if ("shadySummoner".equals(section)) {
			// Shady Summoner themes
			themes.put("Purple", Map.of(
				"ssColorLabel", 0xFFBB77FF,
				"ssColorActive", 0xFF00CC00,
				"ssColorWarning", 0xFFFF4444,
				"ssColorNormal", 0xFFFFCC00,
				"ssColorUnknown", 0xFF888888
			));
			themes.put("Green", Map.of(
				"ssColorLabel", 0xFF55FF55,
				"ssColorActive", 0xFF00FF00,
				"ssColorWarning", 0xFFFF0000,
				"ssColorNormal", 0xFFFFFF00,
				"ssColorUnknown", 0xFF777777
			));
			themes.put("Blue", Map.of(
				"ssColorLabel", 0xFF5599FF,
				"ssColorActive", 0xFF0099FF,
				"ssColorWarning", 0xFFFF6600,
				"ssColorNormal", 0xFFFFFF99,
				"ssColorUnknown", 0xFF666666
			));
			themes.put("Pastel", Map.of(
				"ssColorLabel", 0xFFD4A5D4,
				"ssColorActive", 0xFFA5D4A5,
				"ssColorWarning", 0xFFFFB3B3,
				"ssColorNormal", 0xFFFFF5A5,
				"ssColorUnknown", 0xFFC0C0C0
			));
			themes.put("Dark", Map.of(
				"ssColorLabel", 0xFF999999,
				"ssColorActive", 0xFF33CC33,
				"ssColorWarning", 0xFFCC3333,
				"ssColorNormal", 0xFFCCCC00,
				"ssColorUnknown", 0xFF555555
			));
		} else if ("bazaar".equals(section)) {
			themes.put("Default", Map.of(
				"bzColorLoading", 0xFFFFCC00,
				"bzColorComplete", 0xFF00CC00
			));
			themes.put("Sunset", Map.of(
				"bzColorLoading", 0xFFFF6600,
				"bzColorComplete", 0xFFFF3300
			));
			themes.put("Ocean", Map.of(
				"bzColorLoading", 0xFF0099FF,
				"bzColorComplete", 0xFF00CCFF
			));
			themes.put("Forest", Map.of(
				"bzColorLoading", 0xFF00AA00,
				"bzColorComplete", 0xFF00FF00
			));
			themes.put("Neon", Map.of(
				"bzColorLoading", 0xFFFF00FF,
				"bzColorComplete", 0xFF00FFFF
			));
		} else if ("guildQuests".equals(section)) {
			themes.put("Default", Map.of(
				"gqColorHeader", 0xFF55FF55,
				"gqColorLabel", 0xFF55FF55,
				"gqColorTarget", 0xFFFFFFFF,
				"gqColorUnknown", 0xFFFFFF00
			));
			themes.put("Gold", Map.of(
				"gqColorHeader", 0xFFFFDD00,
				"gqColorLabel", 0xFFFFDD00,
				"gqColorTarget", 0xFFFFFFFF,
				"gqColorUnknown", 0xFFFF9900
			));
			themes.put("Cyan", Map.of(
				"gqColorHeader", 0xFF00FFFF,
				"gqColorLabel", 0xFF00FFFF,
				"gqColorTarget", 0xFFFFFFFF,
				"gqColorUnknown", 0xFF00CCFF
			));
			themes.put("Red", Map.of(
				"gqColorHeader", 0xFFFF3333,
				"gqColorLabel", 0xFFFF3333,
				"gqColorTarget", 0xFFFFFFFF,
				"gqColorUnknown", 0xFFFF0000
			));
			themes.put("Monochrome", Map.of(
				"gqColorHeader", 0xFFAAAAAA,
				"gqColorLabel", 0xFFAAAAAA,
				"gqColorTarget", 0xFFFFFFFF,
				"gqColorUnknown", 0xFF666666
			));
		}

		// Draw theme buttons (2 per row)
		int buttonWidth = 65;
		int buttonSpacing = 70;
		int themeIndex = 0;
		for (String themeName : themes.keySet()) {
			int xOffset = (themeIndex % 2) * buttonSpacing;
			int yOffset = (themeIndex / 2) * 25;

			final Map<String, Integer> themeColors = themes.get(themeName);
			addStyledButton(centerX - 140 + xOffset, yPos + yOffset, buttonWidth, 20,
				Text.literal(themeName), button -> {
					for (Map.Entry<String, Integer> entry : themeColors.entrySet()) {
						String colorKey = entry.getKey();
						int color = entry.getValue();

						if ("shadySummoner".equals(section)) {
							if ("ssColorLabel".equals(colorKey)) ModConfig.ssColorLabel = color;
							if ("ssColorActive".equals(colorKey)) ModConfig.ssColorActive = color;
							if ("ssColorWarning".equals(colorKey)) ModConfig.ssColorWarning = color;
							if ("ssColorNormal".equals(colorKey)) ModConfig.ssColorNormal = color;
							if ("ssColorUnknown".equals(colorKey)) ModConfig.ssColorUnknown = color;
						} else if ("bazaar".equals(section)) {
							if ("bzColorLoading".equals(colorKey)) ModConfig.bzColorLoading = color;
							if ("bzColorComplete".equals(colorKey)) ModConfig.bzColorComplete = color;
						} else if ("guildQuests".equals(section)) {
							if ("gqColorHeader".equals(colorKey)) ModConfig.gqColorHeader = color;
							if ("gqColorLabel".equals(colorKey)) ModConfig.gqColorLabel = color;
							if ("gqColorTarget".equals(colorKey)) ModConfig.gqColorTarget = color;
							if ("gqColorUnknown".equals(colorKey)) ModConfig.gqColorUnknown = color;
						}
					}
					ModConfig.saveConfig();
					this.init();
				},
				BUTTON_TYPE_NORMAL);

			themeIndex++;
		}

		return yPos + ((themeIndex / 2 + 1) * 25);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		int contentLeft = this.width / 2 - CONTENT_WIDTH / 2;
		int contentRight = this.width / 2 + CONTENT_WIDTH / 2;
		int contentBottomY = this.height - CONTENT_BOTTOM_MARGIN;

		// STEP 1: Draw title background with gradient effect
		context.fill(contentLeft, TITLE_START_Y, contentRight, TITLE_BOTTOM_Y, PANEL_HEADER_BG);
		// Top edge accent (subtle lighter line)
		context.fill(contentLeft, TITLE_START_Y, contentRight, TITLE_START_Y + 1, 0xFF1A1A1A);
		// Bottom separator (purple accent)
		context.fill(contentLeft, TITLE_BOTTOM_Y - 1, contentRight, TITLE_BOTTOM_Y, ACCENT);

		// STEP 2: Draw content box border (1px accent) - draw first
		context.fill(contentLeft - 1, CONTENT_START_Y - 1, contentRight + 1, contentBottomY + 1, ACCENT);

		// STEP 3: Draw content box background (dark gray) - on top of border
		context.fill(contentLeft, CONTENT_START_Y, contentRight, contentBottomY, PANEL_BG);

		// STEP 4: Enable scissor BEFORE super.render() to clip all content
		context.enableScissor(contentLeft, CONTENT_START_Y, contentRight, contentBottomY);

		// STEP 5: Call super.render() to draw blur + all button children (now clipped)
		super.render(context, mouseX, mouseY, delta);

		// STEP 6: Render buttons with custom styling
		renderCustomButtons(context, mouseX, mouseY);

		// STEP 7: Draw color picker previews and labels
		drawColorPickerPreviews(context);

		// STEP 8: Disable scissor
		context.disableScissor();

		// STEP 9: Draw title area text (outside scissor, won't be clipped)
		drawTitleArea(context, contentLeft, contentRight);

		// STEP 10: Draw scrollbar inside the panel (as right edge)
		drawScrollbar(context, contentLeft, contentRight);
	}

	/**
	 * Render buttons with custom styling on top of default rendering
	 */
	private void renderCustomButtons(DrawContext context, int mouseX, int mouseY) {
		for (ButtonWidget btn : this.children().stream()
				.filter(w -> w instanceof ButtonWidget)
				.map(w -> (ButtonWidget) w)
				.toList()) {
			Integer type = buttonTypes.get(btn);
			if (type == null) continue;

			boolean hovered = btn.isMouseOver(mouseX, mouseY);

			switch (type) {
				case BUTTON_TYPE_HEADER -> renderHeaderButton(context, btn, hovered);
				case BUTTON_TYPE_TOGGLE -> renderToggleButton(context, btn, hovered);
				case BUTTON_TYPE_SCALE -> renderScaleButton(context, btn, hovered);
				default -> renderNormalButton(context, btn, hovered);
			}
		}
	}

	/**
	 * Render a normal button with flat style
	 */
	private void renderNormalButton(DrawContext context, ButtonWidget btn, boolean hovered) {
		int bgColor = hovered ? BTN_HOVER_BG : BTN_BG;

		// Draw background
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + btn.getHeight(), bgColor);

		// Draw border
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + 1, BTN_BORDER); // Top
		context.fill(btn.getX(), btn.getY() + btn.getHeight() - 1,
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Bottom
		context.fill(btn.getX(), btn.getY(), btn.getX() + 1,
				btn.getY() + btn.getHeight(), BTN_BORDER); // Left
		context.fill(btn.getX() + btn.getWidth() - 1, btn.getY(),
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Right

		// Draw centered text
		context.drawCenteredTextWithShadow(this.textRenderer, btn.getMessage(),
				btn.getX() + btn.getWidth() / 2, btn.getY() + (btn.getHeight() - 8) / 2,
				BTN_TEXT);
	}

	/**
	 * Render a toggle button (green/red)
	 */
	private void renderToggleButton(DrawContext context, ButtonWidget btn, boolean hovered) {
		Boolean isOn = buttonToggleStates.get(btn);
		if (isOn == null) isOn = false;

		int bgColor = isOn ? TOGGLE_ON_BG : TOGGLE_OFF_BG;
		if (hovered) {
			bgColor = adjustBrightness(bgColor, 1.1f);
		}

		// Draw background
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + btn.getHeight(), bgColor);

		// Draw border
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + 1, BTN_BORDER); // Top
		context.fill(btn.getX(), btn.getY() + btn.getHeight() - 1,
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Bottom
		context.fill(btn.getX(), btn.getY(), btn.getX() + 1,
				btn.getY() + btn.getHeight(), BTN_BORDER); // Left
		context.fill(btn.getX() + btn.getWidth() - 1, btn.getY(),
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Right

		// Draw text
		context.drawCenteredTextWithShadow(this.textRenderer, btn.getMessage(),
				btn.getX() + btn.getWidth() / 2, btn.getY() + (btn.getHeight() - 8) / 2,
				TOGGLE_TEXT);
	}

	/**
	 * Render a header button with left accent bar
	 */
	private void renderHeaderButton(DrawContext context, ButtonWidget btn, boolean hovered) {
		int bgColor = hovered ? BTN_HOVER_BG : BTN_BG;

		// Draw background
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + btn.getHeight(), bgColor);

		// Draw left accent bar
		context.fill(btn.getX(), btn.getY(), btn.getX() + 3,
				btn.getY() + btn.getHeight(), ACCENT);

		// Draw border
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + 1, BTN_BORDER); // Top
		context.fill(btn.getX(), btn.getY() + btn.getHeight() - 1,
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Bottom
		context.fill(btn.getX() + btn.getWidth() - 1, btn.getY(),
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Right

		// Draw left-aligned text (with padding from accent bar)
		context.drawTextWithShadow(this.textRenderer, btn.getMessage(),
				btn.getX() + 8, btn.getY() + (btn.getHeight() - 8) / 2,
				BTN_TEXT);
	}

	/**
	 * Render a scale button (purple when selected)
	 */
	private void renderScaleButton(DrawContext context, ButtonWidget btn, boolean hovered) {
		Boolean isSelected = buttonScaleSelected.get(btn);
		if (isSelected == null) isSelected = false;

		int bgColor = isSelected ? 0xFF5A4A7A : BTN_BG;
		if (hovered) {
			bgColor = 0xFF3A2A5A;
		}

		// Draw background
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + btn.getHeight(), bgColor);

		// Draw border
		context.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(),
				btn.getY() + 1, BTN_BORDER); // Top
		context.fill(btn.getX(), btn.getY() + btn.getHeight() - 1,
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Bottom
		context.fill(btn.getX(), btn.getY(), btn.getX() + 1,
				btn.getY() + btn.getHeight(), BTN_BORDER); // Left
		context.fill(btn.getX() + btn.getWidth() - 1, btn.getY(),
				btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), BTN_BORDER); // Right

		// Draw centered text
		context.drawCenteredTextWithShadow(this.textRenderer, btn.getMessage(),
				btn.getX() + btn.getWidth() / 2, btn.getY() + (btn.getHeight() - 8) / 2,
				BTN_TEXT);
	}

	/**
	 * Adjust color brightness
	 */
	private int adjustBrightness(int color, float factor) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int) ((color & 0xFF) * factor));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Draw section background cards with accent bars
	 * (No longer used - section backgrounds are now part of the static content box)
	 */
	private void drawSectionBackgrounds(DrawContext context, int contentLeft, int contentRight) {
		// Individual section backgrounds are no longer needed
		// All content is now within a single static content box
	}

	/**
	 * Draw a single section card with background and accent bar
	 * (No longer used - deprecated)
	 */
	private void drawSectionCard(DrawContext context, int left, int right, int top, int bottom) {
		// Deprecated - use static content box instead
	}

	/**
	 * Draw title area text
	 */
	private void drawTitleArea(DrawContext context, int contentLeft, int contentRight) {
		int centerX = this.width / 2;

		// Draw title text
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, TITLE_START_Y + 12, TEXT_PRIMARY);

		// Draw hint text
		String hint = "Click buttons to toggle features. Changes saved automatically.";
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hint), centerX, TITLE_START_Y + 27, TEXT_SECONDARY);
	}

	/**
	 * Draw color picker previews and labels
	 */
	private void drawColorPickerPreviews(DrawContext context) {
		int centerX = this.width / 2;

		for (ColorPickerEntry entry : colorPickerEntries) {
			HexColorPickerWidget picker = entry.picker();
			int y = picker.getField().getY();

			// Draw label to the left
			context.drawTextWithShadow(this.textRenderer, entry.label(),
					centerX - 140, y + 1, TEXT_SECONDARY);

			// Draw preview
			picker.renderPreview(context);
		}
	}

	/**
	 * Draw scrollbar as part of the content box (right edge)
	 */
	private void drawScrollbar(DrawContext context, int contentLeft, int contentRight) {
		int contentBottomY = this.height - CONTENT_BOTTOM_MARGIN;
		int scrollbarX = contentRight - 8;  // Inside the panel, as right edge
		int scrollbarY = CONTENT_START_Y;
		int scrollbarEndY = contentBottomY;
		int scrollbarH = scrollbarEndY - scrollbarY;
		int scrollbarW = 8;

		// Draw scrollbar track (subtle line)
		context.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarW, scrollbarEndY, 0xFF3A3A4A);

		// Draw scrollbar thumb
		if (maxScroll > 0) {
			int thumbHeight = Math.max(15, (scrollbarH * scrollbarH) / (scrollbarH + maxScroll));
			int thumbY = scrollbarY + (scrollOffset * (scrollbarH - thumbHeight)) / maxScroll;
			int thumbEnd = Math.min(thumbY + thumbHeight, scrollbarEndY);

			// Main color (match accent purple)
			context.fill(scrollbarX + 1, thumbY + 1, scrollbarX + scrollbarW - 1, thumbEnd - 1, ACCENT);
			// Light accent on left
			context.fill(scrollbarX + 1, thumbY + 1, scrollbarX + 2, thumbEnd - 1, 0xFF9977DD);
			// Dark accent on right
			context.fill(scrollbarX + scrollbarW - 1, thumbY + 1, scrollbarX + scrollbarW, thumbEnd - 1, 0xFF5A3D88);
			// Light bevel on top
			context.fill(scrollbarX + 1, thumbY, scrollbarX + scrollbarW - 1, thumbY + 1, 0xFF9977DD);
			// Dark bevel on bottom
			context.fill(scrollbarX + 1, thumbEnd - 1, scrollbarX + scrollbarW - 1, thumbEnd, 0xFF5A3D88);
		} else {
			// Full scrollbar when nothing to scroll
			context.fill(scrollbarX + 1, scrollbarY + 1, scrollbarX + scrollbarW - 1, scrollbarEndY - 1, ACCENT);
			context.fill(scrollbarX + 1, scrollbarY + 1, scrollbarX + 2, scrollbarEndY - 1, 0xFF9977DD);
			context.fill(scrollbarX + scrollbarW - 1, scrollbarY + 1, scrollbarX + scrollbarW, scrollbarEndY - 1, 0xFF5A3D88);
			// Light bevel on top
			context.fill(scrollbarX + 1, scrollbarY, scrollbarX + scrollbarW - 1, scrollbarY + 1, 0xFF9977DD);
			// Dark bevel on bottom
			context.fill(scrollbarX + 1, scrollbarEndY - 1, scrollbarX + scrollbarW - 1, scrollbarEndY, 0xFF5A3D88);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double scrollAmount) {
		int scrollDelta = (int) (-scrollAmount * 10);
		scrollOffset = MathHelper.clamp(scrollOffset + scrollDelta, 0, maxScroll);
		this.init();
		return true;
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parentScreen);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
}
