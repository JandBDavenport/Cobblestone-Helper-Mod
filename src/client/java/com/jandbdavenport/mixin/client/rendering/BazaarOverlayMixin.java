package com.jandbdavenport.mixin.client.rendering;

import com.jandbdavenport.cobblestonehelper.features.BazaarManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the Bazaar overlay on top of the Bazaar: Crops Market container.
 */
@Mixin(GenericContainerScreen.class)
public class BazaarOverlayMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void renderBazaarOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		GenericContainerScreen screen = (GenericContainerScreen) (Object) this;
		String titleStr = screen.getTitle().getString();

		// Only render overlay if this is the bazaar screen
		if (titleStr.contains("Bazaar: Crops Market")) {
			BazaarManager.renderBazaarOverlay(context);
		}
	}
}
