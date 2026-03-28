package com.jandbdavenport.mixin.client.screen;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks clicks on heraldsmark talismans to prevent kicks when moving them.
 *
 * Heraldsmark talismans cause the server to kick the player when moved due to
 * problematic components. This mixin prevents the client from sending the click
 * packet to the server in the first place.
 */
@Mixin(HandledScreen.class)
public class HeraldsmarkClickBlockMixin {

	@Inject(
		method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void blockHeraldsmarkClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		if (slot != null && isHeraldsmark(slot.getStack())) {
			System.out.println("[HeraldsmarkClickBlockMixin] Blocked click on heraldsmark");
			ci.cancel();
		}
	}

	private static boolean isHeraldsmark(net.minecraft.item.ItemStack stack) {
		if (stack.isEmpty()) return false;
		NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (nbtComp == null) return false;
		try {
			var nbt = nbtComp.copyNbt();
			if (nbt.contains("id")) {
				var idOpt = nbt.getString("id");
				return idOpt.isPresent() && "heraldsmark".equals(idOpt.get());
			}
		} catch (Exception ignored) {
		}
		return false;
	}
}
