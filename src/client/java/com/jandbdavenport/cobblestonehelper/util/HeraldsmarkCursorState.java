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
		System.out.println("[HeraldsmarkCursorState] Set fallback: " + stack.getItem().getName().getString());
	}

	public static ItemStack getFallbackCursor() {
		return fallbackCursorStack;
	}

	public static void clearFallbackCursor() {
		System.out.println("[HeraldsmarkCursorState] Cleared fallback");
		fallbackCursorStack = ItemStack.EMPTY;
	}
}
