package com.jandbdavenport.cobblestonehelper.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

/**
 * Shared state for tracking the fallback cursor item when heraldsmark talismans
 * are blocked from entering the actual cursor.
 */
public class HeraldsmarkCursorState {
	private static ItemStack fallbackCursorStack = ItemStack.EMPTY;
	private static boolean shouldClearOnNextTick = false;

	public static void setFallbackCursor(ItemStack stack) {
		fallbackCursorStack = stack.copy();
		shouldClearOnNextTick = false;
	}

	public static ItemStack getFallbackCursor() {
		return fallbackCursorStack;
	}

	public static void clearFallbackCursor() {
		fallbackCursorStack = ItemStack.EMPTY;
		shouldClearOnNextTick = false;
	}

	public static void clearFallbackCursorOnNextTick() {
		shouldClearOnNextTick = true;
	}

	public static void tickClear() {
		if (shouldClearOnNextTick) {
			clearFallbackCursor();
		}
	}

	public static void init() {
		ClientTickEvents.START_CLIENT_TICK.register(client -> tickClear());
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
