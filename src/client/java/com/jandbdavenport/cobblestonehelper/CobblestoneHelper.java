package com.jandbdavenport.cobblestonehelper;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import com.jandbdavenport.cobblestonehelper.features.AutoRespawnManager;
import com.jandbdavenport.cobblestonehelper.features.BazaarManager;
import com.jandbdavenport.cobblestonehelper.features.GuildQuestsManager;
import com.jandbdavenport.cobblestonehelper.features.ItemGlowManager;
import com.jandbdavenport.cobblestonehelper.features.PlayerHidingManager;
import com.jandbdavenport.cobblestonehelper.features.ShadySummonerManager;
import com.jandbdavenport.cobblestonehelper.gui.ConfigScreen;
import com.jandbdavenport.cobblestonehelper.gui.FarmWarpScreen;
import com.jandbdavenport.cobblestonehelper.gui.HudPositionScreen;
import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CobblestoneHelper implements ClientModInitializer {
	public static final String MOD_ID = "cobblestonehelper";

	private static KeyBinding hidePlayersKey;
	private static KeyBinding farmWarpsKey;
	private static KeyBinding.Category customCategory;

	@Override
	public void onInitializeClient() {
		System.out.println("[CobblestoneHelper] ========== MOD INITIALIZATION START ==========");
		System.out.println("[CobblestoneHelper] Version: " + MOD_ID);
		System.out.println("[CobblestoneHelper] Initializing client features...");

		// Initialize config first
		ModConfig.init();

		try {
			// Create custom keybinding category
			customCategory = new KeyBinding.Category(Identifier.of(MOD_ID, "category"));
			System.out.println("[CobblestoneHelper] ✓ Created keybinding category");

			// Register keybinds
			hidePlayersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.cobblestonehelper.hideplayers",
				GLFW.GLFW_KEY_H,
				customCategory
			));
			System.out.println("[CobblestoneHelper] ✓ Registered 'Hide Players' keybind (H)");

			farmWarpsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.cobblestonehelper.farmwarps",
				GLFW.GLFW_KEY_R,
				customCategory
			));
			System.out.println("[CobblestoneHelper] ✓ Registered 'Farm Warps' keybind (R)");

			// Initialize player hiding feature with keybind
			System.out.println("[CobblestoneHelper] Initializing PlayerHidingManager...");
			PlayerHidingManager.init(hidePlayersKey);
			System.out.println("[CobblestoneHelper] ✓ PlayerHidingManager initialized");

			// Initialize Shady Summoner manager
			System.out.println("[CobblestoneHelper] Initializing ShadySummonerManager...");
			ShadySummonerManager.init();
			System.out.println("[CobblestoneHelper] ✓ ShadySummonerManager initialized");

			// Initialize Bazaar manager
			System.out.println("[CobblestoneHelper] Initializing BazaarManager...");
			BazaarManager.init();
			System.out.println("[CobblestoneHelper] ✓ BazaarManager initialized");

			// Initialize Guild Quests manager
			System.out.println("[CobblestoneHelper] Initializing GuildQuestsManager...");
			GuildQuestsManager.init();
			System.out.println("[CobblestoneHelper] ✓ GuildQuestsManager initialized");

			// Initialize Auto Respawn manager
			System.out.println("[CobblestoneHelper] Initializing AutoRespawnManager...");
			AutoRespawnManager.init();
			System.out.println("[CobblestoneHelper] ✓ AutoRespawnManager initialized");

			// Initialize Item Glow manager
			System.out.println("[CobblestoneHelper] Initializing ItemGlowManager...");
			ItemGlowManager.init();
			System.out.println("[CobblestoneHelper] ✓ ItemGlowManager initialized");
		} catch (Exception e) {
			System.out.println("[CobblestoneHelper] ✗ Error during feature initialization!");
			e.printStackTrace();
		}

		// Register farm warps keybind handler (toggle open/close)
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (farmWarpsKey.wasPressed()) {
				if (client.currentScreen instanceof FarmWarpScreen) {
					// Farm warps screen is open, close it
					client.setScreen(null);
				} else {
					// Farm warps screen is closed, open it
					client.setScreen(new FarmWarpScreen());
				}
			}
		});
		System.out.println("[CobblestoneHelper] ✓ Registered farm warps tick event");

		// Register /wf command
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(literal("wf").executes(ctx -> {
				MinecraftClient.getInstance().send(() ->
					MinecraftClient.getInstance().setScreen(new FarmWarpScreen()));
				return Command.SINGLE_SUCCESS;
			}))
		);
		System.out.println("[CobblestoneHelper] ✓ Registered command: /wf");


		// Register /chconfig command
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(literal("chconfig").executes(ctx -> {
				MinecraftClient.getInstance().send(() ->
					MinecraftClient.getInstance().setScreen(new ConfigScreen(MinecraftClient.getInstance().currentScreen)));
				return Command.SINGLE_SUCCESS;
			}))
		);
		System.out.println("[CobblestoneHelper] ✓ Registered command: /chconfig");

		System.out.println("[CobblestoneHelper] ========== MOD INITIALIZATION COMPLETE ==========");
	}

	public static KeyBinding getFarmWarpsKey() {
		return farmWarpsKey;
	}
}
