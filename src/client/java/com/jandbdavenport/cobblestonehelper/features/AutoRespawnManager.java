package com.jandbdavenport.cobblestonehelper.features;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;

public class AutoRespawnManager {
	private static long respawnTriggerMs = 0;
	private static boolean waitingToRespawn = false;

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ModConfig.autoRespawnEnabled) {
				waitingToRespawn = false;
				return;
			}

			if (client.currentScreen instanceof DeathScreen) {
				if (!waitingToRespawn) {
					// Death screen just appeared, start countdown
					waitingToRespawn = true;
					respawnTriggerMs = System.currentTimeMillis()
						+ (long)(ModConfig.autoRespawnDelaySeconds * 1000);
				} else if (System.currentTimeMillis() >= respawnTriggerMs) {
					// Time to respawn
					waitingToRespawn = false;
					var handler = client.getNetworkHandler();
					if (handler != null) {
						handler.sendPacket(new ClientStatusC2SPacket(
							ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
					}
				}
			} else {
				// Death screen is no longer active
				waitingToRespawn = false;
			}
		});
	}

	public static boolean isWaiting() {
		return waitingToRespawn;
	}

	public static float getRemainingSeconds() {
		if (!waitingToRespawn) return 0;
		long remaining = respawnTriggerMs - System.currentTimeMillis();
		return Math.max(0, remaining / 1000.0f);
	}
}
