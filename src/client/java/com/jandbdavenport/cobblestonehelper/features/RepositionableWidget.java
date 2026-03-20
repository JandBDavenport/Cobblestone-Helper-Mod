package com.jandbdavenport.cobblestonehelper.features;

import net.minecraft.client.gui.DrawContext;

/**
 * Interface for HUD widgets that can be repositioned on screen.
 * Implemented by managers that provide draggable widget overlays.
 */
public interface RepositionableWidget {
	/**
	 * Get the current HUD X position.
	 */
	int getHudX();

	/**
	 * Get the current HUD Y position.
	 */
	int getHudY();

	/**
	 * Set the HUD position.
	 */
	void setHudPosition(int x, int y);

	/**
	 * Get the width of the widget for bounds checking.
	 */
	int getWidgetWidth();

	/**
	 * Get the height of the widget for bounds checking.
	 */
	int getWidgetHeight();

	/**
	 * Set whether HUD rendering should be disabled (e.g., when positioning screen is open).
	 */
	void setHudRenderingDisabled(boolean disabled);

	/**
	 * Render a preview of the widget at its current position.
	 * Called by HudPositionScreen to show a live preview.
	 */
	void renderWidgetPreview(DrawContext context);

	/**
	 * Save the current configuration (position, etc.) to disk.
	 */
	void saveConfig();
}
