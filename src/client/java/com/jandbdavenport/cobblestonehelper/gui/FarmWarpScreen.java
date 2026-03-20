package com.jandbdavenport.cobblestonehelper.gui;

import com.jandbdavenport.cobblestonehelper.CobblestoneHelper;
import com.jandbdavenport.cobblestonehelper.data.FarmData;
import com.jandbdavenport.cobblestonehelper.features.BazaarManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class FarmWarpScreen extends Screen {
	private static final int SLOT_SIZE = 18;
	private static final int GRID_COLS = 9;
	private static final int GRID_START_Y = 30;

	// Color palette (ARGB format)
	private static final int COLOR_ORANGE       = 0xFFFF8C00;
	private static final int COLOR_ORANGE_MID   = 0x80FF8C00;
	private static final int COLOR_ORANGE_DIM   = 0x40FF8C00;
	private static final int COLOR_BG_OUTER     = 0xF2111111;
	private static final int COLOR_BG_INNER     = 0xFF1A1A1A;
	private static final int COLOR_SLOT_BG      = 0xFF222222;
	private static final int COLOR_SLOT_BORDER  = 0xFF383838;
	private static final int COLOR_TEXT_WHITE   = 0xFFFFFFFF;

	private List<CropItemWidget> cropWidgets = new ArrayList<>();
	private List<CategoryHeader> categoryHeaders = new ArrayList<>();
	private List<Integer> separatorYs = new ArrayList<>();
	private int gridStartX = 0;
	private int contentHeight = 0;
	private int panelTop = 0;

	private record CategoryHeader(String name, int y) {}

	public FarmWarpScreen() {
		super(Text.literal("Farm Warps"));
	}

	@Override
	protected void init() {
		this.cropWidgets.clear();
		this.categoryHeaders.clear();
		this.separatorYs.clear();

		// Calculate grid positioning
		int rowWidth = GRID_COLS * (SLOT_SIZE + 2) - 2;
		this.gridStartX = (this.width - rowWidth) / 2;

		// First pass: calculate total content height to determine panel size
		int tempY = GRID_START_Y;
		FarmData.FarmCategory[] categories = FarmData.LIST.toArray(new FarmData.FarmCategory[0]);
		for (FarmData.FarmCategory category : categories) {
			tempY += 15;  // Space for category label

			int col = 0;
			for (FarmData.FarmEntry entry : category.crops()) {
				col++;
				if (col >= GRID_COLS) {
					col = 0;
					tempY += SLOT_SIZE + 2;
				}
			}

			if (col > 0) {
				tempY += SLOT_SIZE + 2;
			}
			tempY += 8;
		}

		// Calculate panel top to center vertically
		// Title section is 32px (title bar + divider), add 6px top padding
		int titleSectionHeight = 32;
		int contentRelativeHeight = tempY - GRID_START_Y;
		int panelHeight = 6 + titleSectionHeight + contentRelativeHeight + 6;  // top padding + title + content + bottom padding
		this.panelTop = (this.height - panelHeight) / 2;
		int adjustedGridStartY = this.panelTop + 6 + titleSectionHeight;  // Start after title section

		// Second pass: create widgets with adjusted positions
		int y = adjustedGridStartY;

		for (int catIndex = 0; catIndex < categories.length; catIndex++) {
			FarmData.FarmCategory category = categories[catIndex];
			categoryHeaders.add(new CategoryHeader(category.name(), y + 2));
			y += 15;  // Space for category label

			int col = 0;
			int x = this.gridStartX;

			for (FarmData.FarmEntry entry : category.crops()) {
				final String commandArg = entry.commandArg();
				final String displayName = entry.displayName();

				// Create button for click area
				ButtonWidget cropButton = ButtonWidget.builder(Text.empty(), button -> {
					this.close();
					executeCommand(commandArg);
				})
				.dimensions(x, y, SLOT_SIZE, SLOT_SIZE)
				.build();

				this.addDrawableChild(cropButton);
				boolean isBestCrop = displayName.equals(BazaarManager.getBestCrop());
				cropWidgets.add(new CropItemWidget(x, y, entry.itemId(), displayName, isBestCrop));

				col++;
				if (col >= GRID_COLS) {
					col = 0;
					x = this.gridStartX;
					y += SLOT_SIZE + 2;
				} else {
					x += SLOT_SIZE + 2;
				}
			}

			if (col > 0) {
				y += SLOT_SIZE + 2;
			}

			// Store separator Y if not the last category
			if (catIndex < categories.length - 1) {
				separatorYs.add(y);
			}

			y += 8;
		}

		this.contentHeight = y;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		// Calculate grid positioning for background
		int rowWidth = GRID_COLS * (SLOT_SIZE + 2) - 2;
		int panelLeft = this.gridStartX - 6;
		int panelRight = this.gridStartX + rowWidth + 6;
		int panelBottom = this.contentHeight;

		// 1. Draw orange border (1px thick as 4 fill strips for rounded corners)
		context.fill(panelLeft, this.panelTop, panelRight, this.panelTop + 1, COLOR_ORANGE); // top
		context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, COLOR_ORANGE); // bottom
		context.fill(panelLeft, this.panelTop + 1, panelLeft + 1, panelBottom - 1, COLOR_ORANGE); // left
		context.fill(panelRight - 1, this.panelTop + 1, panelRight, panelBottom - 1, COLOR_ORANGE); // right

		// 2. Draw rounded panel interior using staircase pattern for smooth corners
		int panelW = panelRight - panelLeft;
		int panelH = panelBottom - this.panelTop;
		fillRounded(context, panelLeft + 1, this.panelTop + 1, panelW - 2, panelH - 2, COLOR_BG_OUTER);

		// 3. Draw title bar strip (slightly lighter bg, 30px tall)
		context.fill(panelLeft + 1, this.panelTop + 1, panelRight - 1, this.panelTop + 31, COLOR_BG_INNER);

		// 4. Draw orange divider line under title bar
		context.fill(panelLeft + 1, this.panelTop + 31, panelRight - 1, this.panelTop + 32, COLOR_ORANGE);

		// 5. Draw gradient in content area (much more visible: light gray to very dark)
		context.fillGradient(panelLeft + 1, this.panelTop + 32, panelRight - 1, panelBottom - 1, 0xFF2A2A2A, 0xFF0A0A0A);

		// 6. Draw title text (white, centered)
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.panelTop + 10, COLOR_TEXT_WHITE);

		// 7. Draw category headers with pulsing accent bar animation
		long now = Util.getMeasuringTimeMs();
		float pulse = (float)((Math.sin(now / 2500.0 * 2 * Math.PI) + 1) / 2); // 0→1 over 2.5s cycle
		int accentAlpha = (int)(0xCC + 0x33 * pulse); // 0xCC (80%) → 0xFF (100%)
		int accentColor = (accentAlpha << 24) | 0x00FF8C00;

		for (CategoryHeader header : categoryHeaders) {
			int headerX = this.gridStartX;
			int headerY = header.y();

			// Left accent bar (3px wide × 9px tall) with pulsing animation
			context.fill(headerX - 7, headerY - 2, headerX - 4, headerY + 7, accentColor);

			// Category name in orange
			context.drawTextWithShadow(this.textRenderer, Text.literal(header.name()), headerX, headerY, COLOR_ORANGE);
		}

		// 8. Render crop item widgets (they handle their own hover states)
		for (CropItemWidget widget : cropWidgets) {
			widget.render(context, mouseX, mouseY, delta);
		}

		// 9. Draw separator lines between categories
		for (Integer separatorY : separatorYs) {
			context.fill(this.gridStartX, separatorY, this.gridStartX + rowWidth, separatorY + 1, COLOR_ORANGE_MID);
		}
	}

	/**
	 * Draw a rounded rectangle with 3px radius for large UI elements.
	 * Uses staircase fill pattern: 2-step corners → imperceptibly different from circular.
	 */
	private void fillRounded(DrawContext ctx, int x, int y, int w, int h, int color) {
		// Top row (3px inset)
		ctx.fill(x + 3, y,     x + w - 3, y + 1,     color);
		// Top transition (2 rows, 1px inset)
		ctx.fill(x + 1, y + 1, x + w - 1, y + 3,     color);
		// Middle body (full width)
		ctx.fill(x,     y + 3, x + w,     y + h - 3, color);
		// Bottom transition (2 rows, 1px inset)
		ctx.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, color);
		// Bottom row (3px inset)
		ctx.fill(x + 3, y + h - 1, x + w - 3, y + h,    color);
	}

	/**
	 * Draw a rounded rectangle with 2px radius for 18×18 item slots.
	 * Static version for use in inner classes.
	 */
	private static void fillRoundedSlotStatic(DrawContext ctx, int x, int y, int w, int h, int color) {
		// Top row (2px inset)
		ctx.fill(x + 2, y,     x + w - 2, y + 1,     color);
		// Middle rows (1px inset on sides)
		ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
		// Bottom row (2px inset)
		ctx.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
	}

	private void executeCommand(String commandArg) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler != null) {
			networkHandler.sendChatCommand("warpfarm " + commandArg);
		}
	}

	@Override
	public void close() {
		this.client.setScreen(null);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput key) {
		// Close on farm warps keybind press
		if (CobblestoneHelper.getFarmWarpsKey().matchesKey(key)) {
			this.close();
			return true;
		}
		return super.keyPressed(key);
	}

	// Simple drawable for crop items with hover animations
	private static class CropItemWidget implements Drawable {
		private final int x;
		private final int y;
		private final ItemStack stack;
		private final String displayName;
		private final boolean isBestCrop;

		private long hoverEnterTime = Long.MIN_VALUE;
		private long hoverExitTime = Long.MIN_VALUE;
		private boolean wasHovering = false;

		CropItemWidget(int x, int y, String itemId, String displayName) {
			this(x, y, itemId, displayName, false);
		}

		CropItemWidget(int x, int y, String itemId, String displayName, boolean isBestCrop) {
			this.x = x;
			this.y = y;
			this.displayName = displayName;
			this.isBestCrop = isBestCrop;

			ItemStack tempStack;
			try {
				tempStack = new ItemStack(Registries.ITEM.get(Identifier.of(itemId)));
			} catch (Exception e) {
				tempStack = ItemStack.EMPTY;
			}
			this.stack = tempStack;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			// Check if hovering
			boolean isHovering = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;

			// Track hover state transitions
			long now = Util.getMeasuringTimeMs();

			if (isHovering && !wasHovering) {
				hoverEnterTime = now;
			}
			if (!isHovering && wasHovering) {
				hoverExitTime = now;
			}
			wasHovering = isHovering;

			// Calculate smooth hover progress with easing [0, 1]
			float hoverProgress = 0f;
			if (isHovering) {
				// Fade in over 100ms (ease-out) — only animate if hover was just triggered
				if (hoverEnterTime > Long.MIN_VALUE + 1000000) {
					hoverProgress = MathHelper.clamp((now - hoverEnterTime) / 100f, 0f, 1f);
				}
			} else {
				// Fade out over 200ms (ease-out) — only animate if hover was just ended
				if (hoverExitTime > Long.MIN_VALUE + 1000000) {
					hoverProgress = 1f - MathHelper.clamp((now - hoverExitTime) / 200f, 0f, 1f);
				}
			}
			// Apply ease-out easing: t = 1-(1-t)²
			hoverProgress = 1f - (1f - hoverProgress) * (1f - hoverProgress);

			// Animate border color: lerp from dark (0x383838) to orange (0xFF8C00)
			int bR = (int)MathHelper.lerp(hoverProgress, 0x38, 0xFF);
			int bG = (int)MathHelper.lerp(hoverProgress, 0x38, 0x8C);
			int bB = (int)MathHelper.lerp(hoverProgress, 0x38, 0x00);
			int borderColor = 0xFF000000 | (bR << 16) | (bG << 8) | bB;

			// Animate overlay alpha: 0 → 0x40 (25% opacity of orange)
			int overlayAlpha = (int)(hoverProgress * 0x40);
			int overlayColor = (overlayAlpha << 24) | 0x00FF8C00;

			// If this is the best crop, render glowing yellow outline with fancy decoration
			if (isBestCrop) {
				// Gentle pulsing animation: 0.5 → 1.0 over 1.5s cycle
				float glowPulse = (float)((Math.sin(now / 1500.0 * 2 * Math.PI) + 1) / 2) * 0.5f + 0.5f;
				int glowAlpha = (int)(0xFF * glowPulse);
				int glowColor = (glowAlpha << 24) | 0x00FFFF00; // Yellow with pulsing alpha

				// Draw outer glow layer (thinner, more transparent)
				int outerGlowAlpha = (int)(0x80 * glowPulse);
				int outerGlowColor = (outerGlowAlpha << 24) | 0x00FFFF00;
				context.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, outerGlowColor); // top
				context.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, outerGlowColor); // bottom
				context.fill(x - 1, y, x, y + SLOT_SIZE, outerGlowColor); // left
				context.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + 1, y + SLOT_SIZE, outerGlowColor); // right

				// Draw inner bright outline (1px)
				context.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y - 1, glowColor); // top
				context.fill(x - 1, y + SLOT_SIZE + 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, glowColor); // bottom (adjusted)
				context.fill(x - 1, y, x - 1, y + SLOT_SIZE, glowColor); // left (adjusted to be just top border)
			}

			// Draw simple square slot border
			context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);

			// Draw square slot background
			context.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, COLOR_SLOT_BG);

			// Draw animated overlay (only if visible)
			if (overlayAlpha > 0) {
				context.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, overlayColor);
			}

			// Draw item
			if (!stack.isEmpty()) {
				context.drawItem(stack, x + 1, y + 1);
			}

			// Draw tokens character on top (after item is drawn)
			if (isBestCrop) {
				context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal("⛁"), x + SLOT_SIZE - 4, y - 4, 0xFFFFFF00);
			}

			// Draw tooltip on hover
			if (isHovering) {
				if (!stack.isEmpty()) {
					context.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.literal(displayName), mouseX, mouseY);
				}
			}
		}
	}
}
