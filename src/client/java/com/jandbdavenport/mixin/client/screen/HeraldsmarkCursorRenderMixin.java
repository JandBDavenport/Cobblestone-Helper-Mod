package com.jandbdavenport.mixin.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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

	private static ItemStack fallbackCursorStack = ItemStack.EMPTY;

	public static void setFallbackCursor(ItemStack stack) {
		fallbackCursorStack = stack.copy();
	}

	public static void clearFallbackCursor() {
		fallbackCursorStack = ItemStack.EMPTY;
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void renderHeraldsmarkCursor(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!fallbackCursorStack.isEmpty()) {
			MinecraftClient client = MinecraftClient.getInstance();
			// Render the item at cursor position
			context.drawItem(fallbackCursorStack, mouseX - 8, mouseY - 8);
		}
	}

	public static boolean isHeraldsmark(ItemStack stack) {
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
