package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears the heraldsmark fallback cursor when an inventory screen closes.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkScreenCloseMixin {

	@Inject(method = "close", at = @At("HEAD"))
	private void clearFallbackOnClose(CallbackInfo ci) {
		if (!HeraldsmarkCursorState.getFallbackCursor().isEmpty()) {
			HeraldsmarkCursorState.clearFallbackCursor();
		}
	}
}
