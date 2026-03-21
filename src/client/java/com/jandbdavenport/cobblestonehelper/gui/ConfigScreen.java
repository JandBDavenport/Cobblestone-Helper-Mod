package com.jandbdavenport.cobblestonehelper.gui;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Configuration screen for Cobblestone Helper mod settings.
 * Displays feature toggles and allows enabling/disabling them.
 */
public class ConfigScreen extends Screen {
	private final Screen parentScreen;

	public ConfigScreen(Screen parentScreen) {
		super(Text.literal("Cobblestone Helper Config"));
		this.parentScreen = parentScreen;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = 40;
		int buttonHeight = 20;
		int spacing = 28;

		// Title
		this.addDrawableChild(ButtonWidget.builder(
			Text.literal(ModConfig.shadySummonerEnabled ? "✓ Shady Summoner: ON" : "✗ Shady Summoner: OFF"),
			button -> {
				ModConfig.shadySummonerEnabled = !ModConfig.shadySummonerEnabled;
				ModConfig.saveConfig();
				button.setMessage(Text.literal(ModConfig.shadySummonerEnabled ? "✓ Shady Summoner: ON" : "✗ Shady Summoner: OFF"));
			}
		).dimensions(centerX - 100, startY, 200, buttonHeight).build());

		this.addDrawableChild(ButtonWidget.builder(
			Text.literal(ModConfig.bazaarEnabled ? "✓ Bazaar Overlay: ON" : "✗ Bazaar Overlay: OFF"),
			button -> {
				ModConfig.bazaarEnabled = !ModConfig.bazaarEnabled;
				ModConfig.saveConfig();
				button.setMessage(Text.literal(ModConfig.bazaarEnabled ? "✓ Bazaar Overlay: ON" : "✗ Bazaar Overlay: OFF"));
			}
		).dimensions(centerX - 100, startY + spacing, 200, buttonHeight).build());

		this.addDrawableChild(ButtonWidget.builder(
			Text.literal(ModConfig.guildQuestsEnabled ? "✓ Guild Quests: ON" : "✗ Guild Quests: OFF"),
			button -> {
				ModConfig.guildQuestsEnabled = !ModConfig.guildQuestsEnabled;
				ModConfig.saveConfig();
				button.setMessage(Text.literal(ModConfig.guildQuestsEnabled ? "✓ Guild Quests: ON" : "✗ Guild Quests: OFF"));
			}
		).dimensions(centerX - 100, startY + spacing * 2, 200, buttonHeight).build());

		this.addDrawableChild(ButtonWidget.builder(
			Text.literal(ModConfig.plantHitboxEnabled ? "✓ Plant Hitbox: ON" : "✗ Plant Hitbox: OFF"),
			button -> {
				ModConfig.plantHitboxEnabled = !ModConfig.plantHitboxEnabled;
				ModConfig.saveConfig();
				button.setMessage(Text.literal(ModConfig.plantHitboxEnabled ? "✓ Plant Hitbox: ON" : "✗ Plant Hitbox: OFF"));
			}
		).dimensions(centerX - 100, startY + spacing * 3, 200, buttonHeight).build());

		// Farm Warps display toggles
		this.addDrawableChild(ButtonWidget.builder(
			Text.literal(ModConfig.farmWarpsGuildQuestHighlight ? "✓ Guild Quest Highlight: ON" : "✗ Guild Quest Highlight: OFF"),
			button -> {
				ModConfig.farmWarpsGuildQuestHighlight = !ModConfig.farmWarpsGuildQuestHighlight;
				ModConfig.saveConfig();
				button.setMessage(Text.literal(ModConfig.farmWarpsGuildQuestHighlight ? "✓ Guild Quest Highlight: ON" : "✗ Guild Quest Highlight: OFF"));
			}
		).dimensions(centerX - 100, startY + spacing * 4, 200, buttonHeight).build());

		this.addDrawableChild(ButtonWidget.builder(
			Text.literal(ModConfig.farmWarpsShowBazaarHighlight ? "✓ Bazaar Highlight: ON" : "✗ Bazaar Highlight: OFF"),
			button -> {
				ModConfig.farmWarpsShowBazaarHighlight = !ModConfig.farmWarpsShowBazaarHighlight;
				ModConfig.saveConfig();
				button.setMessage(Text.literal(ModConfig.farmWarpsShowBazaarHighlight ? "✓ Bazaar Highlight: ON" : "✗ Bazaar Highlight: OFF"));
			}
		).dimensions(centerX - 100, startY + spacing * 5, 200, buttonHeight).build());

		// Close button
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
			.dimensions(centerX - 50, this.height - 30, 100, buttonHeight)
			.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);

		// Draw title
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

		// Draw instruction text
		String hint = "Click buttons to toggle features. Changes saved automatically.";
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hint), this.width / 2, 25, 0xAAAAAA);

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parentScreen);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
}
