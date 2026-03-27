package com.jandbdavenport.mixin.client.network;

import com.jandbdavenport.cobblestonehelper.features.KickLoggerManager;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts DisconnectS2CPacket at the moment it arrives from the server.
 *
 * In a proxy-based multi-server setup (BungeeCord/Velocity), when a backend server
 * kicks a player, it sends a DisconnectS2CPacket with a reason string. The proxy then
 * immediately reconnects the player to the hub. This mixin fires at HEAD of onDisconnect,
 * before the vanilla handler tears down the connection — ensuring the kick reason and
 * full game state (player, world, position) are both available simultaneously.
 */
@Mixin(ClientCommonNetworkHandler.class)
public class DisconnectPacketMixin {

	@Inject(
		method = "onDisconnect(Lnet/minecraft/network/packet/s2c/common/DisconnectS2CPacket;)V",
		at = @At("HEAD")
	)
	private void onDisconnectPacket(DisconnectS2CPacket packet, CallbackInfo ci) {
		KickLoggerManager.notifyKickPacketReceived(packet.reason().getString());
	}
}
