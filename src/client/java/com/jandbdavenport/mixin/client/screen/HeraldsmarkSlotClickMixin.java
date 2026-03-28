package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears the heraldsmark fallback cursor when a slot is clicked.
 *
 * When the player clicks a slot to place a heraldsmark item (which is on the
 * server-side cursor but blocked from the client-side cursor), the server
 * processes the click but does NOT call setCursorStack on the client. This
 * mixin detects the slot click and clears the fallback, ensuring the visual
 * feedback disappears when the item is actually placed.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkSlotClickMixin {

	@Inject(
		method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
		at = @At("HEAD")
	)
	private void clearFallbackOnSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		if (slot != null && !HeraldsmarkCursorState.getFallbackCursor().isEmpty()) {
			System.out.println("[HeraldsmarkSlotClickMixin] Slot " + slotId + " clicked (" + actionType + "), clearing fallback");
			HeraldsmarkCursorState.clearFallbackCursor();
		}
	}
}
