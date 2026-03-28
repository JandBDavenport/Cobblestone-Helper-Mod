package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks cursor stack updates for heraldsmark talismans to prevent kicks.
 *
 * Heraldsmark talismans cause the server to kick the player when the item is
 * placed on the cursor (either by clicking or hotkey). This mixin prevents
 * the cursor from being set to a heraldsmark item, similar to the
 * TalismanForgeClickMixin but applying to all heraldsmark items.
 */
@Mixin(ScreenHandler.class)
public class HeraldsmarkClickBlockMixin {

	@Inject(
		method = "setCursorStack(Lnet/minecraft/item/ItemStack;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void blockHeraldsmarkCursorStack(ItemStack stack, CallbackInfo ci) {
		if (stack.isEmpty()) {
			// Cursor is being cleared, clear the fallback too
			HeraldsmarkCursorState.clearFallbackCursor();
		} else if (HeraldsmarkCursorState.isHeraldsmark(stack)) {
			// Block the actual cursor stack update
			ci.cancel();
			// But show a visual fallback on the cursor
			HeraldsmarkCursorState.setFallbackCursor(stack);
		} else {
			// A different item is being placed on cursor, clear the fallback
			HeraldsmarkCursorState.clearFallbackCursor();
		}
	}

}
