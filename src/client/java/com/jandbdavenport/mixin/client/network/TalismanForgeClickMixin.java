package com.jandbdavenport.mixin.client.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Workaround for Talisman Forge container causing kicks when items are clicked.
 *
 * The issue: Clicking an item in the Talisman Forge puts it on the player's cursor.
 * When the server initiates a CONFIG transition, the cursor item state becomes corrupted,
 * causing the player to be kicked.
 *
 * Solution: Prevent the cursor stack from being set when Talisman Forge is open.
 */
@Mixin(ScreenHandler.class)
public class TalismanForgeClickMixin {

	@Inject(
		method = "setCursorStack(Lnet/minecraft/item/ItemStack;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void onSetCursorStack(ItemStack stack, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();

		// If Talisman Forge is open and trying to set cursor to non-empty, block it
		if (!stack.isEmpty() && client.currentScreen instanceof GenericContainerScreen) {
			GenericContainerScreen screen = (GenericContainerScreen) client.currentScreen;
			String title = screen.getTitle().getString();

			if (title.contains("Talisman Forge")) {
				ci.cancel();
			}
		}
	}
}
