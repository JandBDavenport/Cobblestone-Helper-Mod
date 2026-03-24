package com.jandbdavenport.cobblestonehelper.features;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemGlowManager {
	private static final int CHECKS_PER_TICK = 25;
	private static final double NUDGE_DISTANCE = 0.1; // blocks toward camera
	private static final int RENDER_DISTANCE = 64; // blocks

	private static final ConcurrentHashMap<UUID, Boolean> visibilityCache = new ConcurrentHashMap<>();
	private static int checkIndex = 0;
	private static List<ItemEntity> lastItems = new ArrayList<>();

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ModConfig.itemGlowEnabled || client.world == null || client.player == null) {
				return;
			}

			// Collect all ItemEntities near the player
			Box searchBox = client.player.getBoundingBox().expand(RENDER_DISTANCE);
			List<ItemEntity> items = client.world.getEntitiesByType(EntityType.ITEM, searchBox, e -> true);

			// Remove stale cache entries for entities that no longer exist
			visibilityCache.keySet().retainAll(items.stream()
				.map(ItemEntity::getUuid)
				.toList());

			// Process CHECKS_PER_TICK items in a round-robin fashion
			int checksThisTick = Math.min(CHECKS_PER_TICK, items.size());
			for (int i = 0; i < checksThisTick; i++) {
				if (items.isEmpty()) break;
				checkIndex = checkIndex % items.size();
				ItemEntity entity = items.get(checkIndex);
				checkIndex++;

				checkItemVisibility(client, entity);
			}

			lastItems = items;
		});
	}

	/**
	 * Check if an item is visible by raycasting from player eye to item center.
	 * The raycast target is nudged 0.1 blocks toward the camera to handle edge cases
	 * where items are flush against block edges but still visually exposed.
	 */
	private static void checkItemVisibility(MinecraftClient client, ItemEntity entity) {
		Vec3d cameraPos = client.player.getEyePos();
		Vec3d itemCenter = new Vec3d(
			entity.getX(),
			entity.getY() + entity.getHeight() / 2.0,
			entity.getZ()
		);

		// Nudge the target toward the camera to avoid hitting block edges
		Vec3d direction = cameraPos.subtract(itemCenter);
		if (direction.length() > 0.0001) {
			direction = direction.normalize();
		}
		Vec3d nudgedTarget = itemCenter.add(direction.multiply(NUDGE_DISTANCE));

		BlockHitResult hit = client.world.raycast(new RaycastContext(
			cameraPos,
			nudgedTarget,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			entity
		));

		boolean isVisible = hit.getType() == HitResult.Type.MISS;
		visibilityCache.put(entity.getUuid(), isVisible);
	}

	/**
	 * Get the cached visibility status of an item.
	 * Returns true by default for unseen items (they glow immediately).
	 */
	public static boolean getVisibility(UUID uuid) {
		return visibilityCache.getOrDefault(uuid, true);
	}
}
