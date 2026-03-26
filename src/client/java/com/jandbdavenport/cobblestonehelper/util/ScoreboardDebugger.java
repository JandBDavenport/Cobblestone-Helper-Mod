package com.jandbdavenport.cobblestonehelper.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generic debug utility to dump all scoreboard data to a markdown file.
 * Dumps every objective and entry to help diagnose where the actual sidebar text is stored.
 */
public class ScoreboardDebugger {
	/**
	 * Initialize the scoreboard debugger by registering tick event handler.
	 * Listens for the configured keybind press.
	 *
	 * @param debugKey The KeyBinding that specifies which key to listen for
	 */
	public static void init(KeyBinding debugKey) {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null) return;

			while (debugKey.wasPressed()) {
				System.out.println("[ScoreboardDebugger] Debug key pressed! Dumping scoreboard...");
				dumpScoreboard();
			}
		});
	}

	/**
	 * Dump ALL scoreboard objectives and entries to help find the actual sidebar text.
	 */
	public static void dumpScoreboard() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			System.out.println("[ScoreboardDebugger] Not in a world!");
			return;
		}

		try {
			Scoreboard scoreboard = client.world.getScoreboard();

			// Create logs/scoreboard-debug directory
			Path debugPath = Paths.get("logs", "scoreboard-debug");
			Files.createDirectories(debugPath);

			// Generate filename with timestamp
			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			String filename = "scoreboard_" + timestamp + ".md";
			Path outputPath = debugPath.resolve(filename);

			// Build markdown content
			StringBuilder content = new StringBuilder();
			content.append("# Scoreboard Debug Dump - All Objectives\n\n");
			content.append("**Timestamp:** ").append(new Date()).append("\n\n");

			// Player team info
			if (client.player != null) {
				var team = client.player.getScoreboardTeam();
				content.append("## Player Team Info\n");
				if (team != null) {
					content.append("- **Team Name:** `").append(team.getName()).append("`\n");
					content.append("- **Team Display Name:** `").append(team.getDisplayName().getString()).append("`\n");
					content.append("- **Team Color:** `").append(team.getColor()).append("`\n");
					if (team.getColor() != null) {
						var slot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
						content.append("- **Mapped Display Slot:** `").append(slot).append("`\n");
					}
				} else {
					content.append("- **Team:** None\n");
				}
				content.append("\n");
			}

			// List all available objectives
			content.append("## All Objectives\n\n");
			var allObjectives = scoreboard.getObjectives();
			content.append("**Total Objectives:** ").append(allObjectives.size()).append("\n\n");

			for (ScoreboardObjective objective : allObjectives) {
				content.append("### Objective: ").append(objective.getName()).append("\n\n");
				content.append("- **Display Name:** `").append(objective.getDisplayName().getString()).append("`\n");
				content.append("- **Criterion:** `").append(objective.getCriterion().getName()).append("`\n");
				content.append("- **Render Type:** `").append(objective.getRenderType()).append("`\n");

				// Check which slots this objective occupies
				content.append("- **Display Slots:** ");
				var slots = new java.util.ArrayList<String>();
				for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
					if (scoreboard.getObjectiveForSlot(slot) == objective) {
						slots.add(slot.name());
					}
				}
				if (slots.isEmpty()) {
					content.append("None (not displayed)");
				} else {
					content.append(String.join(", ", slots));
				}
				content.append("\n\n");

				// Show entries for this objective
				var entries = scoreboard.getScoreboardEntries(objective);
				content.append("**Entries (").append(entries.size()).append("):**\n\n");

				int entryIndex = 0;
				for (ScoreboardEntry entry : entries) {
					content.append("#### Entry #").append(entryIndex).append("\n");
					content.append("- **owner():** `").append(entry.owner()).append("`\n");
					content.append("- **name():** `").append(entry.name().getString()).append("`\n");
					content.append("- **value():** `").append(entry.value()).append("`\n");

					var display = entry.display();
					if (display != null) {
						content.append("- **display():** `").append(display.getString()).append("`\n");
					} else {
						content.append("- **display():** null\n");
					}

					Team team = scoreboard.getScoreHolderTeam(entry.owner());
					if (team != null) {
						content.append("- **team.getName():** `").append(team.getName()).append("`\n");
						content.append("- **team.getPrefix():** `").append(team.getPrefix().getString()).append("`\n");
						content.append("- **team.getSuffix():** `").append(team.getSuffix().getString()).append("`\n");
						content.append("- **decorated:** `").append(Team.decorateName(team, entry.name()).getString()).append("`\n");
					}
					content.append("\n");

					entryIndex++;
				}

				content.append("\n");
			}

			// Write to file
			Files.writeString(outputPath, content.toString(), StandardCharsets.UTF_8);
			System.out.println("[ScoreboardDebugger] Saved scoreboard dump to: " + outputPath);
		} catch (IOException e) {
			System.out.println("[ScoreboardDebugger] Error dumping scoreboard: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
