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
	private static final int CHECKS_PER_TICK = 100;
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

				checkItemVisibilityMultiRaycast(client, entity);
			}

			lastItems = items;
		});
	}

	/**
	 * Check if an item is visible by raycasting from player eye to 8 corners of the item's bounding box.
	 * The raycast targets are nudged 0.1 blocks toward the camera.
	 * An item is considered visible if ANY raycast returns MISS (not all blocked).
	 * This catches cases where the item center is blocked but edges are visible.
	 */
	private static void checkItemVisibilityMultiRaycast(MinecraftClient client, ItemEntity entity) {
		Vec3d cameraPos = client.player.getEyePos();
		Box boundingBox = entity.getBoundingBox();

		// 8 corners of the bounding box
		Vec3d[] corners = new Vec3d[]{
			new Vec3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
			new Vec3d(boundingBox.maxX, boundingBox.minY, boundingBox.minZ),
			new Vec3d(boundingBox.minX, boundingBox.maxY, boundingBox.minZ),
			new Vec3d(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ),
			new Vec3d(boundingBox.minX, boundingBox.minY, boundingBox.maxZ),
			new Vec3d(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ),
			new Vec3d(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ),
			new Vec3d(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
		};

		// If ANY corner is visible (raycast returns MISS), entity is visible
		for (Vec3d corner : corners) {
			Vec3d direction = cameraPos.subtract(corner);
			if (direction.length() > 0.0001) {
				direction = direction.normalize();
			}
			Vec3d nudgedTarget = corner.add(direction.multiply(NUDGE_DISTANCE));

			BlockHitResult hit = client.world.raycast(new RaycastContext(
				cameraPos,
				nudgedTarget,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				entity
			));

			if (hit.getType() == HitResult.Type.MISS) {
				// Found at least one visible corner
				visibilityCache.put(entity.getUuid(), true);
				return;
			}
		}

		// All corners are blocked
		visibilityCache.put(entity.getUuid(), false);
	}

	/**
	 * Get the cached visibility status of an item.
	 * Returns true by default for unseen items (they glow immediately).
	 */
	public static boolean getVisibility(UUID uuid) {
		return visibilityCache.getOrDefault(uuid, true);
	}
}
