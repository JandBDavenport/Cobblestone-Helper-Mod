package com.jandbdavenport.cobblestonehelper.data;

import java.util.ArrayList;
import java.util.List;

public class FarmData {
	public record FarmEntry(String displayName, String itemId, String commandArg) {}
	public record FarmCategory(String name, List<FarmEntry> crops) {}

	public static final List<FarmCategory> LIST = new ArrayList<>();

	static {
		// Overworld
		List<FarmEntry> overworld = new ArrayList<>();
		overworld.add(new FarmEntry("Wheat", "minecraft:wheat", "wheat"));
		overworld.add(new FarmEntry("Carrot", "minecraft:carrot", "carrot"));
		overworld.add(new FarmEntry("Potato", "minecraft:potato", "potato"));
		overworld.add(new FarmEntry("Beetroot", "minecraft:beetroot", "beetroot"));
		overworld.add(new FarmEntry("Pumpkin", "minecraft:pumpkin", "pumpkin"));
		LIST.add(new FarmCategory("Overworld", overworld));

		// Floating Isles
		List<FarmEntry> floatingIsles = new ArrayList<>();
		floatingIsles.add(new FarmEntry("Rose", "minecraft:rose_bush", "rose"));
		floatingIsles.add(new FarmEntry("Pitcher Plant", "minecraft:pitcher_plant", "pitcherplant"));
		floatingIsles.add(new FarmEntry("Peony", "minecraft:peony", "peony"));
		floatingIsles.add(new FarmEntry("Lilac", "minecraft:lilac", "lilac"));
		floatingIsles.add(new FarmEntry("Azalea", "minecraft:azalea_leaves", "azalea"));
		floatingIsles.add(new FarmEntry("Firefly Bush", "minecraft:firefly_bush", "fireflybush"));
		LIST.add(new FarmCategory("Floating Isles", floatingIsles));

		// Deep Volcano
		List<FarmEntry> deepVolcano = new ArrayList<>();
		deepVolcano.add(new FarmEntry("Basalt", "minecraft:basalt", "basalt"));
		deepVolcano.add(new FarmEntry("Calcite", "minecraft:calcite", "calcite"));
		deepVolcano.add(new FarmEntry("Magma", "minecraft:magma_block", "magma"));
		deepVolcano.add(new FarmEntry("Netherite", "minecraft:netherite_ingot", "netherite"));
		deepVolcano.add(new FarmEntry("Dripstone", "minecraft:pointed_dripstone", "dripstone"));
		LIST.add(new FarmCategory("Deep Volcano", deepVolcano));

		// Icy Outpost
		List<FarmEntry> icyOutpost = new ArrayList<>();
		icyOutpost.add(new FarmEntry("Ice", "minecraft:ice", "ice"));
		icyOutpost.add(new FarmEntry("Snow", "minecraft:snowball", "snow"));
		icyOutpost.add(new FarmEntry("Diamond", "minecraft:diamond", "diamond"));
		icyOutpost.add(new FarmEntry("Quartz", "minecraft:quartz", "quartz"));
		icyOutpost.add(new FarmEntry("Prismarine", "minecraft:dark_prismarine", "prismarine"));
		icyOutpost.add(new FarmEntry("Bone", "minecraft:bone", "bone"));
		icyOutpost.add(new FarmEntry("Dream", "minecraft:amethyst_shard", "dream"));
		LIST.add(new FarmCategory("Icy Outpost", icyOutpost));

		// Elysium
		List<FarmEntry> elysium = new ArrayList<>();
		elysium.add(new FarmEntry("Lightwood", "minecraft:stripped_pale_oak_wood", "lightwood"));
		elysium.add(new FarmEntry("Gold", "minecraft:gold_ingot", "gold"));
		elysium.add(new FarmEntry("Resin", "minecraft:resin_clump", "resin"));
		elysium.add(new FarmEntry("Glowstone", "minecraft:glowstone_dust", "glowstone"));
		elysium.add(new FarmEntry("Seraphic Rose", "minecraft:cactus_flower", "seraphicrose"));
		elysium.add(new FarmEntry("Godgrass", "minecraft:tall_dry_grass", "godgrass"));
		elysium.add(new FarmEntry("Godflowers", "minecraft:wildflowers", "godflowers"));
		elysium.add(new FarmEntry("Hallowed Heart", "minecraft:creaking_heart", "hallowedheart"));
		LIST.add(new FarmCategory("Elysium", elysium));
	}
}
