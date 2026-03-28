package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the heraldsmark fallback item on top of inventory screen overlays.
 *
 * Injects into drawMouseoverTooltip which is called at the end of HandledScreen's
 * rendering pipeline, ensuring the fallback renders after all slots, highlights,
 * and white hover overlays.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkCursorRenderHandledScreenMixin {

	private static int renderCount = 0;

	@Inject(method = "drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("TAIL"))
	private void renderHeraldsmarkCursor(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
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
