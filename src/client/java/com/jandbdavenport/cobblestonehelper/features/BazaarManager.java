package com.jandbdavenport.cobblestonehelper.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jandbdavenport.cobblestonehelper.ModConfig;
import com.jandbdavenport.cobblestonehelper.util.ContainerScreenUtils;
import com.jandbdavenport.cobblestonehelper.util.HudRepositionHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the Bazaar Best Crop Overlay.
 * Scrapes crop prices from the Bazaar: Crops Market GUI across multiple pages
 * and displays the most profitable crop (highest sell price/unit).
 */
public class BazaarManager {
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
			return (int)(160 * ModConfig.bzScale);
		}

		@Override
		public int getWidgetHeight() {
			return (int)(10 * ModConfig.bzScale);
		}

		@Override
		public void setHudRenderingDisabled(boolean disabled) {
			hudRenderingDisabled = disabled;
		}

		@Override
		public void renderWidgetPreview(DrawContext context) {
			previewMode = true;
		renderBazaarOverlay(context);
		previewMode = false;
		}

		@Override
		public void saveConfig() {
			BazaarManager.saveConfig();
		}
	};

	private enum CollectionState {
		NO_DATA, COLLECTING, COMPLETE
	}

	private static CollectionState collectionState = CollectionState.NO_DATA;
	private static Map<String, Double> cropSellPrices = new HashMap<>();
	private static String bestCrop = "";
	private static double bestSellPrice = 0.0;
	private static long lastCompleteMs = System.currentTimeMillis(); // Start fresh (not stale)
	private static boolean scrapedThisPage = false;
	private static boolean finishedThisCollection = false; // Prevent finishCollection from being called multiple times

	// Tick-based scraping and page detection
	private static int ticksUntilScrape = 0;        // Countdown before scraping allowed (replaces wall-clock INITIAL_WAIT_MS)
	private static int noNextPageTicks = 0;         // Consecutive ticks where next-page button is absent (debounce)
	private static int lastCropContentHash = -1;    // Hash of crop items for page-change detection
	private static final int INITIAL_WAIT_TICKS = 20;      // ~1 second at 20 ticks/sec
	private static final int NO_NEXT_PAGE_THRESHOLD = 3;   // ~150 ms debounce

	// HUD position (saved to config)
	private static int hudX = 5;
	private static int hudY = 5;

	// Flag to disable HUD rendering when positioning screen is open
	private static boolean hudRenderingDisabled = false;

	// Flag to indicate preview mode (for HudPositionScreen)
	public static boolean previewMode = false;

	// Track if bazaar screen is currently open
	private static boolean isBazaarScreenOpen = false;
	private static GenericContainerScreen currentBazaarScreen = null;

	// Colors (ARGB format)
	private static final int COLOR_ORANGE = 0xFFFF8C00;
	private static final int COLOR_DARK_BG = 0xCC111111;
	private static final int COLOR_TEXT_GREY = 0xFF888888;
	private static final int COLOR_TEXT_GREEN = 0xFF00CC00;
	private static final int COLOR_TEXT_YELLOW = 0xFFFFCC00;

	// In-place repositioning
	private static final HudRepositionHelper repositionHelper = new HudRepositionHelper();
	private static ButtonWidget restoreButton = null;

	// Config constants
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "cobblestonehelper.json";
	private static final long CACHE_DURATION_MS = 2 * 60 * 1000; // 2 minutes

	public static void init() {
		System.out.println("[BazaarManager] Loading config...");
		loadConfig();
		System.out.println("[BazaarManager] ✓ Config loaded");

		// Register screen event to detect bazaar container
		System.out.println("[BazaarManager] Registering AFTER_INIT screen event...");
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof GenericContainerScreen)) {
				isBazaarScreenOpen = false;
				return;
			}

			GenericContainerScreen containerScreen = (GenericContainerScreen) screen;
			Text title = containerScreen.getTitle();
			String titleStr = title.getString();

			// Check if this is the Bazaar: Crops Market container
			if (!titleStr.contains("Bazaar: Crops Market")) {
				isBazaarScreenOpen = false;
				return;
			}

			isBazaarScreenOpen = true;
			currentBazaarScreen = containerScreen;


			// Check if we should start fresh collection
			// Only clear if: NO_DATA state (never collected) OR cache is stale AND we've completed a collection
			long now = System.currentTimeMillis();
			boolean isCacheStale = (now - lastCompleteMs) > CACHE_DURATION_MS;

			if (collectionState == CollectionState.NO_DATA) {
				// Starting fresh collection
				System.out.println("[BazaarManager] Starting fresh collection");
				collectionState = CollectionState.COLLECTING;
				cropSellPrices.clear();
				bestCrop = "";
				bestSellPrice = 0.0;
				finishedThisCollection = false;
			isFreshBazaarOpen = true;
			} else if (collectionState == CollectionState.COMPLETE && isCacheStale) {
				// Cache expired - clear and start over
				System.out.println("[BazaarManager] Cache expired, starting fresh collection");
				collectionState = CollectionState.COLLECTING;
				cropSellPrices.clear();
				bestCrop = "";
				bestSellPrice = 0.0;
				finishedThisCollection = false;
			isFreshBazaarOpen = true;
		} else if (collectionState == CollectionState.COMPLETE && !isCacheStale && finishedThisCollection) {
			// Cache is fresh AND fully collected - keep showing result, don't scrape again
			System.out.println("[BazaarManager] Using cached best crop: " + bestCrop + " at ⛁" + bestSellPrice);
			addPositionButton(screen);
			return; // Don't scrape, just use cached data
		} else if (collectionState == CollectionState.COMPLETE && (!isCacheStale || !finishedThisCollection)) {
			// Cache expired OR previous collection was incomplete - clear and start over
			if (!finishedThisCollection) {
				System.out.println("[BazaarManager] Previous collection incomplete, restarting from scratch");
			} else {
				System.out.println("[BazaarManager] Cache expired, starting fresh collection");
			}
			collectionState = CollectionState.COLLECTING;
			cropSellPrices.clear();
			bestCrop = "";
			bestSellPrice = 0.0;
			finishedThisCollection = false;
			isFreshBazaarOpen = true;
		} else if (collectionState == CollectionState.COLLECTING && !finishedThisCollection && bazaarOpenedTimeMs == 0) {
			// Incomplete collection session AND fresh bazaar open - bazaar was closed before finishing, restart fresh
			System.out.println("[BazaarManager] Incomplete previous session, restarting from scratch");
			cropSellPrices.clear();
			bestCrop = "";
			bestSellPrice = 0.0;
			finishedThisCollection = false;
			isFreshBazaarOpen = true;
		} else if (collectionState == CollectionState.COLLECTING && !finishedThisCollection && bazaarOpenedTimeMs != 0) {
			// Page change during ongoing collection - continue collecting without resetting
			System.out.println("[BazaarManager] Page change detected, continuing collection...");
			isFreshBazaarOpen = false;
		} else if (collectionState == CollectionState.COLLECTING && finishedThisCollection) {
			// Already collecting and was completed - continue accumulating (don't clear!)
			System.out.println("[BazaarManager] Continuing collection...");
			isFreshBazaarOpen = false;
		}

			// Reset page scrape flag so we scrape this page (whether new collection or continuation)
			scrapedThisPage = false;
			initialWaitScrapeScheduled = false;
			// Record when bazaar was opened for initial wait period
			if (bazaarOpenedTimeMs == 0) {
				bazaarOpenedTimeMs = System.currentTimeMillis();
				System.out.println("[BazaarManager] Bazaar opened, waiting 1 second for prices to load...");
			}

			// Add gear button for positioning
			addPositionButton(screen);
		});

		// Track when bazaar screen closes
		System.out.println("[BazaarManager] Registering BEFORE_INIT screen event...");
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			// When a new screen is about to init, clear bazaar state
			// (unless the new screen is also the bazaar)
			if (screen instanceof GenericContainerScreen) {
				GenericContainerScreen containerScreen = (GenericContainerScreen) screen;
				String titleStr = containerScreen.getTitle().getString();
				if (!titleStr.contains("Bazaar: Crops Market")) {
					// New screen is NOT bazaar - clear bazaar state and exit repositioning mode
					removeRestoreButton(currentBazaarScreen);
					isBazaarScreenOpen = false;
					currentBazaarScreen = null;
					bazaarOpenedTimeMs = 0;
					initialWaitScrapeScheduled = false;
					repositionHelper.exit(null); // Exit repositioning mode
				}
			} else {
				// New screen is not a container at all - clear bazaar state and exit repositioning mode
				removeRestoreButton(currentBazaarScreen);
				isBazaarScreenOpen = false;
				currentBazaarScreen = null;
			bazaarOpenedTimeMs = 0;
			initialWaitScrapeScheduled = false;
			repositionHelper.exit(null); // Exit repositioning mode
		}
		});
		System.out.println("[BazaarManager] ✓ BazaarManager initialization complete");
	}

	/**
	 * Scrape crop prices from the current screen's visible items.
	 */
	private static void scrapeItemsFromScreen(GenericContainerScreen screen) {
		if (scrapedThisPage) {
			return;
		}
		// Don't set scrapedThisPage = true yet - only set it if we actually find items or give up

		try {
			Inventory inventory = currentBazaarScreen.getScreenHandler().getInventory();
			int itemsProcessed = 0;
			int pricesFound = 0;

			// Iterate through all slots in the container
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}

				itemsProcessed++;
				String itemName = stack.getName().getString();

				// Skip navigation buttons (contain itemsadder data)
				NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
				if (nbtComp != null) {
					try {
						NbtCompound nbt = nbtComp.copyNbt();
						if (nbt.contains("itemsadder")) {
							continue; // Skip nav buttons
						}
					} catch (Exception ignored) {}
				}

				// Skip items with hidden tooltips
				var tooltipDisplay = stack.get(DataComponentTypes.TOOLTIP_DISPLAY);
				if (tooltipDisplay != null && tooltipDisplay.hideTooltip()) {
					continue;
				}

				// Try to extract price from item name or NBT
				Double price = parseItemForPrice(stack, itemName);

				if (price != null && price > 0) {
					// Update map with max price seen for this crop
					cropSellPrices.put(itemName, Math.max(cropSellPrices.getOrDefault(itemName, 0.0), price));
					pricesFound++;
				}
			}

			// Mark this page as scraped only if we found items
			if (pricesFound > 0) {
				scrapedThisPage = true;
			}

			// Log results
			System.out.println("[BazaarManager] Scraped page: " + itemsProcessed + " items, " + pricesFound + " prices found, total crops: " + cropSellPrices.size());

			// Check for next-page button
			boolean hasNextPage = checkForNextPageButton(screen);

			if (!hasNextPage) {
				// No next page - collection complete
				scrapedThisPage = true; // Mark as done even if we found nothing
				finishCollection();
			}
		} catch (Exception e) {
			System.out.println("[BazaarManager] Error scraping: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Parse item for sell price from the lore component.
	 * Looks for "Sell: ⛁X.XXX" pattern in the item lore.
	 * Returns the extracted price or null if not found.
	 */
	private static Double parseItemForPrice(ItemStack stack, String itemName) {
		Double price = ContainerScreenUtils.extractLoreNumber(stack, "Sell:");
		if (price != null) {
			System.out.println("[BazaarManager] Extracted price: " + price + " for " + itemName);
		}
		return price;
	}

	/**
	 * Check if a next-page button exists and is not grayed out.
	 */
	private static boolean checkForNextPageButton(GenericContainerScreen screen) {
		Inventory inventory = screen.getScreenHandler().getInventory();
		return ContainerScreenUtils.hasActiveNextPageButton(inventory, 45);
	}

	/**
	 * Mark collection as complete and compute best crop.
	 * Only runs once per collection cycle.
	 */
	private static void finishCollection() {
		// Prevent multiple calls per collection
		if (finishedThisCollection) {
			return;
		}

		if (cropSellPrices.isEmpty()) {
			// Still collecting data, just not complete yet
			return;
		}

		finishedThisCollection = true;

		// Find the best crop (highest sell price)
		bestCrop = "";
		bestSellPrice = 0.0;

		System.out.println("[BazaarManager] Finding best crop from " + cropSellPrices.size() + " crops");
		for (Map.Entry<String, Double> entry : cropSellPrices.entrySet()) {
			System.out.println("[BazaarManager] Comparing: " + entry.getKey() + " = " + entry.getValue() + " vs current best " + bestCrop + " = " + bestSellPrice);
			if (entry.getValue() > bestSellPrice) {
				bestSellPrice = entry.getValue();
				bestCrop = entry.getKey();
				System.out.println("[BazaarManager] New best: " + bestCrop + " = " + bestSellPrice);
			}
		}

		System.out.println("[BazaarManager] Collection complete! Best crop: " + bestCrop + " at ⛁" + bestSellPrice);
		collectionState = CollectionState.COMPLETE;
		lastCompleteMs = System.currentTimeMillis();
		saveConfig();
	}

	/**
	 * Add a gear button to the screen for positioning the overlay.
	 */
	private static void addPositionButton(Screen screen) {
		if (!ModConfig.bazaarEnabled) {
			return;
		}

		// Create clear cache button at top-right
		ButtonWidget clearCacheButton = ButtonWidget.builder(Text.literal("R"), button -> {
			clearCache();
		})
		.dimensions(screen.width - 60, 10, 20, 20)
		.tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Reset Cache\nRecalculate prices")))
		.build();

		Screens.getButtons(screen).add(clearCacheButton);

		// Create settings button at top-right (to the right of clear cache button)
		ButtonWidget settingsButton = ButtonWidget.builder(Text.literal("S"), button -> {
			repositionHelper.start();
			// Add restore button when entering repositioning mode
			addRestoreButton(screen);
		})
		.dimensions(screen.width - 30, 10, 20, 20)
		.tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Reposition Overlay\nDrag to move")))
		.build();

		Screens.getButtons(screen).add(settingsButton);
	}

	/**
	 * Add the restore to default button when in repositioning mode.
	 */
	private static void addRestoreButton(Screen screen) {
		if (restoreButton != null) {
			return; // Button already added
		}

		restoreButton = ButtonWidget.builder(Text.literal("Restore to Default Position"), button -> {
			// Reset to default position (5, 5)
			hudX = 5;
			hudY = 5;
		})
		.dimensions(screen.width / 2 - 100, screen.height - 40, 200, 20)
		.build();

		Screens.getButtons(screen).add(restoreButton);
	}

	/**
	 * Remove the restore button when exiting repositioning mode.
	 */
	private static void removeRestoreButton(Screen screen) {
		if (restoreButton != null && screen != null) {
			Screens.getButtons(screen).remove(restoreButton);
			restoreButton = null;
		}
	}

	/**
	 * Render the bazaar overlay on top of the bazaar container screen.
	 * Called from BazaarOverlayMixin when rendering the GenericContainerScreen.
	 */
	public static void renderBazaarOverlay(DrawContext context) {
		// Don't render if feature disabled
		if (!ModConfig.bazaarEnabled) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		Screen currentScreen = client.currentScreen;

		// In preview mode, skip screen checks; otherwise verify we're in the correct bazaar screen
		if (!previewMode) {
			// Only render if current screen is the exact same bazaar screen we opened
			if (currentScreen != currentBazaarScreen) {
				isBazaarScreenOpen = false;
				currentBazaarScreen = null;
				return;
			}

			// Double-check it's still a bazaar
			if (!(currentScreen instanceof GenericContainerScreen)) {
				isBazaarScreenOpen = false;
				currentBazaarScreen = null;
				return;
			}

			GenericContainerScreen screen = (GenericContainerScreen) currentScreen;
			String titleStr = screen.getTitle().getString();
			if (!titleStr.contains("Bazaar: Crops Market")) {
				isBazaarScreenOpen = false;
				currentBazaarScreen = null;
				return;
			}
		}

		// Don't render if disabled or no data
		if (hudRenderingDisabled || collectionState == CollectionState.NO_DATA) {
			return;
		}

		// Per-render page-change detection via nav button hash
		if (!previewMode && currentBazaarScreen != null) {
			Inventory inventory = currentBazaarScreen.getScreenHandler().getInventory();
			int currentHash = ContainerScreenUtils.navButtonHash(inventory);
			if (currentHash != -1) {
				if (lastNavButtonHash != -1 && currentHash != lastNavButtonHash) {
					scrapedThisPage = false;
					System.out.println("[BazaarManager] Page change detected, will scrape new page");
				}
				lastNavButtonHash = currentHash;
			}
		}

		// Determine value text
		String valueText = "";
		int valueColor = COLOR_TEXT_GREY;

		long now = System.currentTimeMillis();
		long timeSinceBazaarOpened = now - bazaarOpenedTimeMs;
		// Only show initial wait message if bazaar was JUST opened (not on page changes)
		if (bazaarOpenedTimeMs > 0 && timeSinceBazaarOpened < INITIAL_WAIT_MS && isFreshBazaarOpen) {
			long remainingMs = INITIAL_WAIT_MS - timeSinceBazaarOpened;
			valueText = "Loading prices... (" + (remainingMs / 1000 + 1) + "s)";
			valueColor = ModConfig.bzColorLoading;
		} else if (bazaarOpenedTimeMs > 0 && !initialWaitScrapeScheduled && collectionState == CollectionState.COLLECTING) {
			initialWaitScrapeScheduled = true;
			System.out.println("[BazaarManager] 1-second wait complete, starting price scrape");
			scrapeItemsFromScreen((GenericContainerScreen) currentScreen);
			valueText = "Scanning page 1... Found " + cropSellPrices.size() + " crops";
			valueColor = ModConfig.bzColorLoading;
		} else if (!scrapedThisPage && collectionState == CollectionState.COLLECTING && initialWaitScrapeScheduled) {
			System.out.println("[BazaarManager] Scraping new page");
			scrapeItemsFromScreen((GenericContainerScreen) currentScreen);
			valueText = "Scanning pages... Found " + cropSellPrices.size() + " crops total";
			valueColor = ModConfig.bzColorLoading;
		} else if (collectionState == CollectionState.COLLECTING) {
			valueText = "Scanning bazaar... Found " + cropSellPrices.size() + " crops (turn the page →)";
			valueColor = ModConfig.bzColorLoading;
		} else if (collectionState == CollectionState.COMPLETE) {
			String priceStr = String.format("%.3f", bestSellPrice).replaceAll("0+$", "").replaceAll("\\.$", "");
			valueText = "✓ Best: " + bestCrop + " ⛁" + priceStr + "/unit";
			valueColor = ModConfig.bzColorComplete;
		}

	if (valueText.isEmpty()) {
		return;
	}

	// Draw value text with scaling transformation
	context.getMatrices().pushMatrix();
	context.getMatrices().translate((float)hudX, (float)hudY);
	context.getMatrices().scale(ModConfig.bzScale, ModConfig.bzScale);
	context.drawTextWithShadow(client.textRenderer, Text.literal(valueText), 0, 0, valueColor);
	context.getMatrices().popMatrix();


		// Handle in-place repositioning mode
		if (repositionHelper.isActive()) {
			int mouseX = (int)(client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
			int mouseY = (int)(client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());

			int[] newPos = repositionHelper.handleInput(client, mouseX, mouseY, hudX, hudY, 160, 10,
				() -> {
					BazaarManager.saveConfig();
					// Exit repositioning and remove button
					if (currentBazaarScreen != null) {
						removeRestoreButton(currentBazaarScreen);
					}
				});
			if (newPos != null) {
				hudX = newPos[0];
				hudY = newPos[1];
			}

			// Draw repositioning border
			repositionHelper.drawBorder(context, hudX, hudY, 160, 10);

			// Draw instructions at top
			String instructions = "Drag the widget to reposition. Press Escape to save.";
			context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(instructions), client.getWindow().getScaledWidth() / 2, 10, 0xFFFFFFFF);
		}
	}

	/**
	 * Render the bazaar overlay in the HUD.
	 * Called from ShadySummonerHudMixin for HUD rendering.
	 *
	 * NOTE: Bazaar overlay only renders in the bazaar container screen (via BazaarOverlayMixin).
	 * This method does nothing - all bazaar overlay rendering happens through the screen mixin.
	 */
	public static void renderOverlay(DrawContext context) {
		// Bazaar overlay only renders in the bazaar container (via BazaarOverlayMixin)
		// Do not render in HUD
		return;
	}

	/**
	 * Load HUD position and cache data from config file.
	 */
	private static void loadConfig() {
		try {
			Path configPath = java.nio.file.Paths.get(CONFIG_DIR, CONFIG_FILE);
			if (Files.exists(configPath)) {
				try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
					JsonObject json = new Gson().fromJson(reader, JsonObject.class);
					if (json != null) {
						if (json.has("bazaarOverlayX")) {
							hudX = json.get("bazaarOverlayX").getAsInt();
						}
						if (json.has("bazaarOverlayY")) {
							hudY = json.get("bazaarOverlayY").getAsInt();
						}
						if (json.has("bazaarLastUpdate")) {
							lastCompleteMs = json.get("bazaarLastUpdate").getAsLong();
						}
						if (json.has("bazaarPrices")) {
							JsonObject pricesObj = json.getAsJsonObject("bazaarPrices");
							for (String key : pricesObj.keySet()) {
								try {
									cropSellPrices.put(key, pricesObj.get(key).getAsDouble());
								} catch (Exception ignored) {}
							}
						}

						// If we loaded prices, compute best crop and set state to COMPLETE
						if (!cropSellPrices.isEmpty()) {
							bestCrop = "";
							bestSellPrice = 0.0;
							for (Map.Entry<String, Double> entry : cropSellPrices.entrySet()) {
								if (entry.getValue() > bestSellPrice) {
									bestSellPrice = entry.getValue();
									bestCrop = entry.getKey();
								}
							}
							if (!bestCrop.isEmpty()) {
								collectionState = CollectionState.COMPLETE;
								System.out.println("[BazaarManager] Loaded cached best crop: " + bestCrop + " at ⛁" + bestSellPrice);
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
	 * Save HUD position and cache data to config file.
	 * Uses read-merge-write pattern to preserve other config data.
	 */
	public static void saveConfig() {
		try {
			java.nio.file.Paths.get(CONFIG_DIR).toFile().mkdirs();

			// Read existing config or create new object
			JsonObject json = new JsonObject();
			Path configPath = java.nio.file.Paths.get(CONFIG_DIR, CONFIG_FILE);
			if (Files.exists(configPath)) {
				try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
					JsonObject existing = new Gson().fromJson(reader, JsonObject.class);
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

			// Update Bazaar specific properties
			json.addProperty("bazaarOverlayX", hudX);
			json.addProperty("bazaarOverlayY", hudY);
			json.addProperty("bazaarLastUpdate", lastCompleteMs);

			// Save prices as sub-object
			JsonObject pricesObj = new JsonObject();
			for (Map.Entry<String, Double> entry : cropSellPrices.entrySet()) {
				pricesObj.addProperty(entry.getKey(), entry.getValue());
			}
			json.add("bazaarPrices", pricesObj);

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String content = gson.toJson(json);

			Files.write(configPath, content.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			// If saving fails, silently ignore
		}
	}

	// Getters
	public static String getBestCrop() {
		// Only return bestCrop if collection is complete and cache is fresh
		if (collectionState != CollectionState.COMPLETE || !finishedThisCollection) {
			return ""; // Collection incomplete or in progress
		}
		long timeSinceComplete = System.currentTimeMillis() - lastCompleteMs;
		if (timeSinceComplete > CACHE_DURATION_MS) {
			return ""; // Cache is stale
		}
		return bestCrop;
	}

	/**
	 * Clear the bazaar cache, forcing recalculation on current page.
	 */
	public static void clearCache() {
		cropSellPrices.clear();
		bestCrop = "";
		bestSellPrice = 0.0;
		scrapedThisPage = false;
		finishedThisCollection = false;
		initialWaitScrapeScheduled = false;
		lastNavButtonHash = -1;
		// Set to COLLECTING and skip the initial wait - start scanning immediately
		collectionState = CollectionState.COLLECTING;
		// Set bazaarOpenedTimeMs to past so initial wait is skipped
		bazaarOpenedTimeMs = System.currentTimeMillis() - INITIAL_WAIT_MS - 1;
		System.out.println("[BazaarManager] Cache cleared! Starting fresh scan on current page...");
	}

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
		return 160; // Approximate width for overlay
	}

	public static int getWidgetHeight() {
		return 20; // Height of overlay
	}

	public static void setHudRenderingDisabled(boolean disabled) {
		hudRenderingDisabled = disabled;
	}

	/**
	 * Force preview state for positioning screen (temporarily set to COLLECTING if NO_DATA).
	 */
	public static void setPreviewMode(boolean enabled) {
		if (enabled && collectionState == CollectionState.NO_DATA) {
			collectionState = CollectionState.COLLECTING;
			if (cropSellPrices.isEmpty()) {
				cropSellPrices.put("Wheat", 12.5);
				cropSellPrices.put("Carrot", 10.0);
				bestCrop = "Wheat";
				bestSellPrice = 12.5;
			}
		}
	}
}
