package com.jandbdavenport.cobblestonehelper.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Stateful composition object for in-place drag repositioning of HUD widgets.
 * Manages the active state and dragging logic for repositioning mode.
 */
public class HudRepositionHelper {
	private boolean active = false;
	private boolean dragging = false;
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;

	/**
	 * Check if repositioning mode is currently active.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Start repositioning mode.
	 */
	public void start() {
		active = true;
		dragging = false;
	}

	/**
	 * Exit repositioning mode and run the save callback.
	 */
	public void exit(Runnable onSave) {
		if (onSave != null) {
			onSave.run();
		}
		active = false;
		dragging = false;
	}

	/**
	 * Handle input for dragging. Returns the new {x, y} position.
	 * Polls GLFW for mouse/keyboard input and calls exit() if Escape is pressed.
	 *
	 * @param client Minecraft client instance
	 * @param mouseX Current mouse X coordinate
	 * @param mouseY Current mouse Y coordinate
	 * @param hudX Current HUD X position
	 * @param hudY Current HUD Y position
	 * @param widgetWidth Widget width for bounds checking
	 * @param widgetHeight Widget height for bounds checking
	 * @param onExit Callback to run when exiting (via Escape)
	 * @return Array containing new {x, y} position, or null if no change
	 */
	public int[] handleInput(MinecraftClient client, int mouseX, int mouseY, int hudX, int hudY,
	                         int widgetWidth, int widgetHeight, Runnable onExit) {
		if (!active) {
			return null;
		}

		// Check for Escape key to exit repositioning mode
		if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
			exit(onExit);
			return null;
		}

		boolean leftButtonPressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean withinBounds = mouseX >= hudX && mouseX < hudX + widgetWidth &&
			mouseY >= hudY && mouseY < hudY + widgetHeight;

		// Start dragging if button pressed and within bounds
		if (leftButtonPressed && withinBounds && !dragging) {
			dragging = true;
			dragOffsetX = mouseX - hudX;
			dragOffsetY = mouseY - hudY;
		}

		// Stop dragging if button released
		if (!leftButtonPressed && dragging) {
			dragging = false;
		}

		// Update position while dragging
		if (dragging && leftButtonPressed) {
			int newX = mouseX - dragOffsetX;
			int newY = mouseY - dragOffsetY;
			newX = MathHelper.clamp(newX, 0, client.getWindow().getScaledWidth() - widgetWidth);
			newY = MathHelper.clamp(newY, 0, client.getWindow().getScaledHeight() - widgetHeight);
			return new int[]{newX, newY};
		}

		return null;
	}

	/**
	 * Draw a 1-pixel white border around the widget at the specified position.
	 */
	public void drawBorder(DrawContext context, int x, int y, int width, int height) {
		// 1-pixel white border
		context.fill(x - 1, y - 1, x + width + 1, y, 0xFFFFFFFF); // Top
		context.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFFFFFFF); // Bottom
		context.fill(x - 1, y - 1, x, y + height + 1, 0xFFFFFFFF); // Left
		context.fill(x + width, y - 1, x + width + 1, y + height + 1, 0xFFFFFFFF); // Right
	}
}
