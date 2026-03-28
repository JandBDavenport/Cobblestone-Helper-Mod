package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the heraldsmark fallback item on inventory screens.
 *
 * Injects into Screen.render at every RETURN point. On HandledScreen,
 * one of these RETURN points fires after all inventory rendering is complete,
 * ensuring the fallback renders on top of overlays.
 */
@Mixin(Screen.class)
public class HeraldsmarkCursorRenderHandledScreenMixin {

	private static int renderCount = 0;

	@Inject(method = "render", at = @At("RETURN"))
	private void renderHeraldsmarkCursor(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		// Only render on inventory screens
		if (!(((Screen)(Object)this) instanceof HandledScreen)) {
			return;
		}
		ItemStack fallbackStack = HeraldsmarkCursorState.getFallbackCursor();
		// Log every 100 renders to see if method is being called
		if (renderCount++ % 100 == 0) {
			System.out.println("[HeraldsmarkCursorRenderHandledScreenMixin] Render #" + renderCount + ", fallback stack empty: " + fallbackStack.isEmpty());
		}
		if (!fallbackStack.isEmpty()) {
			System.out.println("[HeraldsmarkCursorRenderHandledScreenMixin] Rendering fallback at (" + mouseX + ", " + mouseY + ")");
			// Render the item at cursor position
			context.drawItem(fallbackStack, mouseX - 8, mouseY - 8);
		}
	}
}
