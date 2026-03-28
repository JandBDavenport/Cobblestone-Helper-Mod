package com.jandbdavenport.cobblestonehelper.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Strips minecraft:swing_animation and minecraft:use_effects from heraldsmark
 * talisman items found in the player's inventory.
 *
 * In 1.21.11 the server sends these two extra components on heraldsmark
 * talismans. Moving such an item (hotkey or cursor) causes the server to kick
 * the player. Stripping them client-side prevents this without affecting any
 * other items.
 */
public class HeraldsmarkComponentStripper {

	public static void init() {
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player.getInventory() != null) {
				// Strip from player inventory
				Inventory inv = client.player.getInventory();
				for (int i = 0; i < inv.size(); i++) {
					stripIfHeraldsmark(inv.getStack(i));
				}
			}
		});
	}

	private static void stripIfHeraldsmark(ItemStack stack) {
		if (stack.isEmpty()) return;
		NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (nbtComp == null) return;
		try {
			var nbt = nbtComp.copyNbt();
			if (nbt.contains("id")) {
				var idOpt = nbt.getString("id");
				String id = idOpt.isPresent() ? idOpt.get() : "";
				if ("heraldsmark".equals(id)) {
					stack.remove(DataComponentTypes.SWING_ANIMATION);
					stack.remove(DataComponentTypes.USE_EFFECTS);
				}
			}
		} catch (Exception ignored) {
		}
	}
}
