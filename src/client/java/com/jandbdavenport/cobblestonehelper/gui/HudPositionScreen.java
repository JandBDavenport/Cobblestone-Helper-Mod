package com.jandbdavenport.cobblestonehelper.gui;

import com.jandbdavenport.cobblestonehelper.features.RepositionableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Generic screen for repositioning any HUD widget that implements RepositionableWidget.
 * Replaces BazaarOverlayPositionScreen and ShadySummonerHudPositionScreen.
 * Shows a live preview and allows dragging to reposition.
 */
public class HudPositionScreen extends Screen {
	private final RepositionableWidget widget;
	private final Screen parentScreen; // Screen to restore when closing (null if opened standalone)
	private int previewX;
	private int previewY;

	// Drag state tracking
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private boolean isDragging = false;

	public HudPositionScreen(RepositionableWidget widget) {
		this(widget, null);
	}

	public HudPositionScreen(RepositionableWidget widget, Screen parentScreen) {
		super(Text.literal("HUD Position"));
		this.widget = widget;
		this.parentScreen = parentScreen;
		this.previewX = widget.getHudX();
		this.previewY = widget.getHudY();
	}

	@Override
	protected void init() {
		// Sync preview position when screen initializes
		this.previewX = widget.getHudX();
		this.previewY = widget.getHudY();

		// Disable the actual HUD rendering so it doesn't show behind this menu
		widget.setHudRenderingDisabled(true);

		// Add restore to default button
		ButtonWidget restoreButton = ButtonWidget.builder(
			Text.literal("Restore to Default Position"),
			button -> {
				this.previewX = 5;
				this.previewY = 5;
			}
		)
		.dimensions(this.width / 2 - 100, this.height - 40, 200, 20)
		.build();

		this.addDrawableChild(restoreButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Always render standard background (don't try to render parent screen)
		this.renderBackground(context, mouseX, mouseY, delta);

		// Draw instructions at top
		String instructions = "Drag the widget to reposition. Press Escape to save.";
		context.drawCenteredTextWithShadow(
			this.textRenderer,
			Text.literal(instructions),
			this.width / 2,
			10,
			0xFFFFFFFF
		);

		// Handle mouse input for dragging
		handleMouseInput(mouseX, mouseY);

		// Temporarily disable rendering, apply preview position, render, then re-disable
		widget.setHudRenderingDisabled(false);
		int savedX = widget.getHudX();
		int savedY = widget.getHudY();
		widget.setHudPosition(previewX, previewY);
		widget.renderWidgetPreview(context);
		widget.setHudPosition(savedX, savedY);
		widget.setHudRenderingDisabled(true);

		// Draw border around the preview widget
		drawPreviewBorder(context);

		// Draw buttons and other UI on top
		super.render(context, mouseX, mouseY, delta);
	}

	/**
	 * Draw a border around the preview widget.
	 */
	private void drawPreviewBorder(DrawContext context) {
		int width = widget.getWidgetWidth();
		int height = widget.getWidgetHeight();
		if (width > 0 && height > 0) {
			// 1-pixel white border
			context.fill(previewX - 1, previewY - 1, previewX + width + 1, previewY, 0xFFFFFFFF); // Top
			context.fill(previewX - 1, previewY + height, previewX + width + 1, previewY + height + 1, 0xFFFFFFFF); // Bottom
			context.fill(previewX - 1, previewY - 1, previewX, previewY + height + 1, 0xFFFFFFFF); // Left
			context.fill(previewX + width, previewY - 1, previewX + width + 1, previewY + height + 1, 0xFFFFFFFF); // Right
		}
	}

	/**
	 * Handle mouse input for dragging the widget.
	 */
	private void handleMouseInput(int mouseX, int mouseY) {
		// Check if left mouse button is pressed
		boolean leftButtonPressed = this.client != null &&
			GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

		// Check if click is within widget bounds
		int widgetWidth = widget.getWidgetWidth();
		int widgetHeight = widget.getWidgetHeight();
		boolean withinBounds = mouseX >= previewX && mouseX < previewX + widgetWidth &&
			mouseY >= previewY && mouseY < previewY + widgetHeight;

		// Start dragging if button pressed and within bounds
		if (leftButtonPressed && withinBounds && !isDragging) {
			isDragging = true;
			dragOffsetX = mouseX - previewX;
			dragOffsetY = mouseY - previewY;
		}

		// Stop dragging if button released
		if (!leftButtonPressed && isDragging) {
			isDragging = false;
		}

		// Update position while dragging
		if (isDragging && leftButtonPressed) {
			int newX = mouseX - dragOffsetX;
			int newY = mouseY - dragOffsetY;

			// Clamp to keep widget fully on screen
			newX = MathHelper.clamp(newX, 0, this.width - widgetWidth);
			newY = MathHelper.clamp(newY, 0, this.height - widgetHeight);

			previewX = newX;
			previewY = newY;
		}
	}

	@Override
	public void close() {
		// Save the new position
		widget.setHudPosition(previewX, previewY);
		widget.saveConfig();

		// Re-enable the actual HUD rendering
		widget.setHudRenderingDisabled(false);

		// Restore parent screen if one was provided, otherwise close normally
		if (parentScreen != null && this.client != null) {
			this.client.setScreen(parentScreen);
		} else {
			super.close();
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
}
