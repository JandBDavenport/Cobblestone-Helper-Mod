package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the heraldsmark fallback item at the same depth as the vanilla cursor item.
 *
 * Injects into HandledScreen.renderCursorStack, which is called after all slot items
 * and overlays (including the white slot-hover highlight) have been drawn.
 * renderCursorStack calls createNewRootLayer() before drawing, placing items on top
 * of everything else. By injecting here, our fallback gets identical visual depth.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkCursorRenderHandledScreenMixin {

	@Shadow protected TextRenderer textRenderer;

	@Inject(
		method = "renderCursorStack(Lnet/minecraft/client/gui/DrawContext;II)V",
		at = @At("TAIL")
	)
	private void renderHeraldsmarkFallbackCursor(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
		ItemStack fallbackStack = HeraldsmarkCursorState.getFallbackCursor();
		if (fallbackStack.isEmpty()) return;
		System.out.println("[HeraldsmarkCursorRenderHandledScreenMixin] Rendering fallback at (" + mouseX + ", " + mouseY + ")");
		// createNewRootLayer() ensures we render on top of all prior draws,
		// same as vanilla does for the real cursor item
		context.createNewRootLayer();
		context.drawItem(fallbackStack, mouseX - 8, mouseY - 8);
		context.drawStackOverlay(textRenderer, fallbackStack, mouseX - 8, mouseY - 8);
	}
}
