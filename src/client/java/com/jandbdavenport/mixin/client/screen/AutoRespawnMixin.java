package com.jandbdavenport.mixin.client.screen;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import com.jandbdavenport.cobblestonehelper.features.AutoRespawnManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class AutoRespawnMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void renderCountdown(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ModConfig.autoRespawnEnabled || !AutoRespawnManager.isWaiting()) return;

		int seconds = AutoRespawnManager.getRemainingSeconds();
		String text = "Respawning in " + seconds + "...";

		var client = net.minecraft.client.MinecraftClient.getInstance();
		int x = client.getWindow().getScaledWidth() / 2;
		int y = client.getWindow().getScaledHeight() / 2 + 40;

		context.drawCenteredTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
	}
}
