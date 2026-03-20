package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.ShadySummonerManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects Shady Summoner HUD rendering into the game's HUD render cycle.
 */
@Mixin(InGameHud.class)
public class ShadySummonerHudMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void renderShadySummonerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		ShadySummonerManager.renderHud(context);
	}
}
