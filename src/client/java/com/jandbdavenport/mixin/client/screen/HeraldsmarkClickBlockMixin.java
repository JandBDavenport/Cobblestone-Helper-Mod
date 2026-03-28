package com.jandbdavenport.mixin.client.screen;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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
		if (!stack.isEmpty() && isHeraldsmark(stack)) {
			System.out.println("[HeraldsmarkClickBlockMixin] Blocked cursor stack set for heraldsmark");
			ci.cancel();
		}
	}

	private static boolean isHeraldsmark(ItemStack stack) {
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
