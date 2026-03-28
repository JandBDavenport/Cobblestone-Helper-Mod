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
 * Renders a visual fallback item on the cursor for heraldsmark talismans.
 *
 * When a heraldsmark item would be placed on the cursor (which is blocked by
 * HeraldsmarkClickBlockMixin), we render it visually at the cursor position
 * so the player sees they're moving it, even though the server doesn't.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkCursorRenderMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void renderHeraldsmarkCursor(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		ItemStack fallbackStack = HeraldsmarkCursorState.getFallbackCursor();
		if (!fallbackStack.isEmpty()) {
			// Render the item at cursor position
			context.drawItem(fallbackStack, mouseX - 8, mouseY - 8);
		}
	}
}
