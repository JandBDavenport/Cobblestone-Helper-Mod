package com.jandbdavenport.cobblestonehelper.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
		System.out.println("[HeraldsmarkCursorState] Set fallback: " + stack.getItem().getName().getString());
	}

	public static ItemStack getFallbackCursor() {
		return fallbackCursorStack;
	}

	public static void clearFallbackCursor() {
		System.out.println("[HeraldsmarkCursorState] Cleared fallback");
		fallbackCursorStack = ItemStack.EMPTY;
		shouldClearOnNextTick = false;
	}

	public static void clearFallbackCursorOnNextTick() {
		System.out.println("[HeraldsmarkCursorState] Scheduled fallback clear for next tick");
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
}
