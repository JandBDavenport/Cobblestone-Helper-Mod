package com.jandbdavenport.cobblestonehelper.util;

import net.minecraft.item.ItemStack;

/**
 * Shared state for tracking the fallback cursor item when heraldsmark talismans
 * are blocked from entering the actual cursor.
 */
public class HeraldsmarkCursorState {
	private static ItemStack fallbackCursorStack = ItemStack.EMPTY;

	public static void setFallbackCursor(ItemStack stack) {
		fallbackCursorStack = stack.copy();
	}

	public static ItemStack getFallbackCursor() {
		return fallbackCursorStack;
	}

	public static void clearFallbackCursor() {
		fallbackCursorStack = ItemStack.EMPTY;
	}
}
