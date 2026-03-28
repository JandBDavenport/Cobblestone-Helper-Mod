package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.util.HeraldsmarkCursorState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
		at = @At("HEAD"),
		cancellable = true
	)
	private void onHeraldsmarkSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		// Clear fallback on any slot click
		if (slot != null && !HeraldsmarkCursorState.getFallbackCursor().isEmpty()) {
			HeraldsmarkCursorState.clearFallbackCursorOnNextTick();
		}

		// Block QUICK_MOVE (shift-click) and SWAP (number keys) on heraldsmark items
		if (slot != null && (actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.SWAP)) {
			if (HeraldsmarkCursorState.isHeraldsmark(slot.getStack())) {
				ci.cancel();
				MinecraftClient client = MinecraftClient.getInstance();
				if (client.player != null) {
					client.player.sendMessage(
						Text.translatable("cobblestonehelper.heraldsmark.blocked")
							.formatted(Formatting.RED),
						false  // chat message
					);
					client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
					client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0f, 1.0f);
				}
			}
		}
	}
}
