package com.jandbdavenport.cobblestonehelper.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for common container screen operations.
 * Provides helpers for tooltip scanning and navigation button detection.
 */
public final class ContainerScreenUtils {
	private static final Pattern LORE_NUMBER_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");

	private ContainerScreenUtils() {
		// Static utility class
	}

	/**
	 * Check if an item stack is a navigation button (contains itemsadder NBT data).
	 */
	public static boolean isNavButton(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (nbtComp == null) {
			return false;
		}

		try {
			var nbt = nbtComp.copyNbt();
			return nbt.contains("itemsadder");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Get the lore lines from an item stack.
	 * Returns an empty list if no lore is present.
	 */
	public static List<Text> getLoreLines(ItemStack stack) {
		var loreComp = stack.get(DataComponentTypes.LORE);
		if (loreComp == null) {
			return new ArrayList<>();
		}

		return new ArrayList<>(loreComp.lines());
	}

	/**
	 * Check if there's an active (non-gray) next-page button in the inventory.
	 * Returns true if a non-gray curved_arrow_right is found at slot >= minSlot.
	 */
	public static boolean hasActiveNextPageButton(Inventory inventory, int minSlot) {
		try {
			for (int i = minSlot; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}

				if (isNavButton(stack)) {
					NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
					if (nbtComp != null) {
						try {
							var nbt = nbtComp.copyNbt();
							String itemsadderStr = nbt.get("itemsadder").toString();
							// Only match non-gray curved_arrow_right
							if (itemsadderStr.contains("curved_arrow_right") && !itemsadderStr.contains("gray")) {
								return true;
							}
						} catch (Exception ignored) {}
					}
				}
			}
		} catch (Exception ignored) {}

		return false;
	}

	/**
	 * Extract the first decimal number after a line prefix from any lore line.
	 * For example, extracting "12.5" from "Sell: ⛁12.5 per unit"
	 * Returns null if the prefix is not found or the number cannot be parsed.
	 */
	public static Double extractLoreNumber(ItemStack stack, String linePrefix) {
		List<Text> loreLines = getLoreLines(stack);

		for (Text line : loreLines) {
			String lineText = line.getString();

			if (lineText.contains(linePrefix)) {
				// Find the part after the prefix
				int prefixIndex = lineText.indexOf(linePrefix);
				String afterPrefix = lineText.substring(prefixIndex + linePrefix.length());

				// Extract the first number
				var matcher = LORE_NUMBER_PATTERN.matcher(afterPrefix);
				if (matcher.find()) {
					try {
						return Double.parseDouble(matcher.group(1));
					} catch (NumberFormatException e) {
						return null;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Compute a hash of crop item names in the stable probe slots (10, 11, 12).
	 * These slots are always in the middle crop columns, never fillers or nav buttons.
	 * Used to detect page changes reliably. Returns a consistent hash even if some
	 * slots are empty (empty slots contribute "EMPTY" to the hash).
	 */
	public static int getCropContentHash(Inventory inventory) {
		int[] probeSlots = {10, 11, 12};
		StringBuilder sb = new StringBuilder();
		try {
			for (int slot : probeSlots) {
				if (slot >= inventory.size()) {
					sb.append("OOB");
					continue;
				}
				ItemStack stack = inventory.getStack(slot);
				if (stack.isEmpty()) {
					sb.append("EMPTY");
				} else {
					sb.append(stack.getName().getString());
				}
				sb.append("|");
			}
		} catch (Exception ignored) {}
		return sb.toString().hashCode();
	}

	/**
	 * Compute a hash of the first itemsadder NBT string found in the inventory.
	 * Used for detecting page changes. Returns -1 if no itemsadder button is found.
	 *
	 * @deprecated Use {@link #getCropContentHash(Inventory)} instead. This method
	 * hashes a filler item that is identical on all pages, so page changes are not
	 * reliably detected.
	 */
	@Deprecated
	public static int navButtonHash(Inventory inventory) {
		try {
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}

				if (isNavButton(stack)) {
					NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
					if (nbtComp != null) {
						try {
							var nbt = nbtComp.copyNbt();
							return nbt.toString().hashCode();
						} catch (Exception ignored) {}
					}
				}
			}
		} catch (Exception ignored) {}

		return -1;
	}
}
