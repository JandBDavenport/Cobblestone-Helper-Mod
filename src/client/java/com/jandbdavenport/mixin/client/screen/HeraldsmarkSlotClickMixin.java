package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clears the heraldsmark fallback cursor when a mouse button is clicked.
 *
 * When the player clicks to place a heraldsmark item (which is on the
 * server-side cursor but not the client-side), we need to clear the fallback
 * since the server is processing the placement.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkSlotClickMixin {

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void clearFallbackOnClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (!HeraldsmarkCursorState.getFallbackCursor().isEmpty()) {
			System.out.println("[HeraldsmarkSlotClickMixin] Mouse clicked, clearing fallback");
			HeraldsmarkCursorState.clearFallbackCursor();
		}
	}
}
