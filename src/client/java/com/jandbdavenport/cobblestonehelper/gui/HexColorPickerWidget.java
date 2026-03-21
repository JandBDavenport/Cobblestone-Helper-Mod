package com.jandbdavenport.cobblestonehelper.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/**
 * A hex color picker widget: 12×12 preview square + 80px TextFieldWidget for 6-char RRGGBB hex input.
 *
 * The TextFieldWidget is accessed via getField() and should be added to the Screen via addDrawableChild().
 * The preview square is drawn separately via renderPreview().
 *
 * When the hex field changes to a valid 6-char value, the onChange callback fires with the ARGB color (0xFF + RGB).
 */
public class HexColorPickerWidget {
	private final TextFieldWidget field;
	private int currentColor; // ARGB (0xFFRRGGBB)

	/**
	 * Create a hex color picker at the specified position.
	 *
	 * @param tr TextRenderer for the field
	 * @param x X position (of the preview box left edge)
	 * @param y Y position
	 * @param currentArgbColor Initial color in ARGB format (0xFFRRGGBB)
	 * @param onChange Callback fired when valid hex is entered; receives full ARGB int
	 */
	public HexColorPickerWidget(TextRenderer tr, int x, int y, int currentArgbColor, Consumer<Integer> onChange) {
		this.currentColor = currentArgbColor;

		// TextFieldWidget positioned 16px to the right of the preview box (14px box + 2px gap)
		this.field = new TextFieldWidget(tr, x + 16, y, 80, 12, Text.empty());
		field.setMaxLength(6);
		field.setText(String.format("%06X", currentArgbColor & 0x00FFFFFF));
		field.setDrawsBackground(true);

		// On text change: if 6 chars and valid hex, parse and fire onChange
		field.setChangedListener(text -> {
			if (text.length() == 6) {
				try {
					int rgb = (int) Long.parseLong(text, 16);
					currentColor = 0xFF000000 | rgb;
					onChange.accept(currentColor);
				} catch (NumberFormatException ignored) {
					// Invalid hex, don't update
				}
			}
		});
	}

	/**
	 * Get the underlying TextFieldWidget.
	 * Must be added to the Screen via screen.addDrawableChild(widget.getField()).
	 */
	public TextFieldWidget getField() {
		return field;
	}

	/**
	 * Get the currently selected color in ARGB format.
	 */
	public int getCurrentColor() {
		return currentColor;
	}

	/**
	 * Set the color and update the text field to display the new hex value.
	 */
	public void setColor(int argb) {
		currentColor = argb;
		field.setText(String.format("%06X", argb & 0x00FFFFFF));
	}

	/**
	 * Render the preview color square.
	 * Called from ConfigScreen.render() to draw the preview box left of the text field.
	 *
	 * The preview box is positioned 14px to the left of the text field (12px box + 2px gap).
	 */
	public void renderPreview(DrawContext context) {
		int x = field.getX() - 14;  // 12px box + 2px gap before field
		int y = field.getY();

		// Fill the color square
		context.fill(x, y, x + 12, y + 12, currentColor);

		// Draw 1px dark border around preview
		context.fill(x - 1, y - 1, x + 13, y, 0xFF4A4A5A);        // top
		context.fill(x - 1, y + 12, x + 13, y + 13, 0xFF4A4A5A);  // bottom
		context.fill(x - 1, y, x, y + 12, 0xFF4A4A5A);            // left
		context.fill(x + 12, y, x + 13, y + 12, 0xFF4A4A5A);      // right
	}
}
