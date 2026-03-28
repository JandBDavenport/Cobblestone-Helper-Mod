package com.jandbdavenport.mixin.client.network;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/**
 * Strips minecraft:swing_animation and minecraft:use_effects from heraldsmark
 * talisman items received from the server.
 *
 * In 1.21.11 the server sends these two extra components on heraldsmark
 * talismans. Moving such an item (hotkey or cursor) causes the server to kick
 * the player. Stripping them client-side before vanilla processes the packet
 * prevents this without affecting any other items.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class HeraldsmarkComponentStripMixin {

	@Inject(
		method = "onScreenHandlerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;)V",
		at = @At("HEAD")
	)
	private void stripSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
		stripIfHeraldsmark(packet.getStack());
	}

	@Inject(
		method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V",
		at = @At("HEAD")
	)
	private void stripInventory(InventoryS2CPacket packet, CallbackInfo ci) {
		for (ItemStack stack : packet.contents()) {
			stripIfHeraldsmark(stack);
		}
		stripIfHeraldsmark(packet.cursorStack());
	}

	private static void stripIfHeraldsmark(ItemStack stack) {
		if (stack.isEmpty()) return;
		NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (nbtComp == null) return;
		try {
			var nbt = nbtComp.copyNbt();
			if (nbt.contains("id") && "heraldsmark".equals(nbt.getString("id"))) {
				System.out.println("[HeraldsmarkComponentStrip] Found heraldsmark, stripping components");
				System.out.println("[HeraldsmarkComponentStrip] Has SWING_ANIMATION before: " + (stack.get(DataComponentTypes.SWING_ANIMATION) != null));
				System.out.println("[HeraldsmarkComponentStrip] Has USE_EFFECTS before: " + (stack.get(DataComponentTypes.USE_EFFECTS) != null));
				stack.remove(DataComponentTypes.SWING_ANIMATION);
				stack.remove(DataComponentTypes.USE_EFFECTS);
				System.out.println("[HeraldsmarkComponentStrip] Has SWING_ANIMATION after: " + (stack.get(DataComponentTypes.SWING_ANIMATION) != null));
				System.out.println("[HeraldsmarkComponentStrip] Has USE_EFFECTS after: " + (stack.get(DataComponentTypes.USE_EFFECTS) != null));
			}
		} catch (Exception e) {
			System.out.println("[HeraldsmarkComponentStrip] Error processing stack: " + e);
			e.printStackTrace();
		}
	}
}
