package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears the heraldsmark fallback cursor when a slot is clicked.
 *
 * When the player clicks a slot to place a heraldsmark item (which is on the
 * server-side cursor but not the client-side), we need to clear the fallback
 * since the server is processing the placement.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkSlotClickMixin {

	@Inject(method = "onMouseClick", at = @At("HEAD"))
	private void clearFallbackOnSlotClick(CallbackInfo ci) {
		if (!HeraldsmarkCursorState.getFallbackCursor().isEmpty()) {
			System.out.println("[HeraldsmarkSlotClickMixin] Slot clicked, clearing fallback");
			HeraldsmarkCursorState.clearFallbackCursor();
		}
	}
}
