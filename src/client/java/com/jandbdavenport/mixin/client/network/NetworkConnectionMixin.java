package com.jandbdavenport.mixin.client.network;

import com.jandbdavenport.cobblestonehelper.features.KickLoggerManager;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts Netty-level error and close events on ClientConnection.
 *
 * exceptionCaught: fires when a Netty exception occurs (timeout, compression error, generic error)
 *   before vanilla's own handling converts it to disconnect text.
 *
 * channelInactive: fires when the TCP channel closes. Used to detect raw TCP drops —
 *   those where no exception and no DisconnectS2CPacket preceded the close.
 *   The state flag in KickLoggerManager prevents double-logging for normal kick flows.
 */
@Mixin(ClientConnection.class)
public class NetworkConnectionMixin {

	@Inject(
		method = "exceptionCaught(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Throwable;)V",
		at = @At("HEAD")
	)
	private void onExceptionCaught(ChannelHandlerContext ctx, Throwable cause, CallbackInfo ci) {
		KickLoggerManager.notifyNetworkException(cause);
	}

	@Inject(
		method = "channelInactive(Lio/netty/channel/ChannelHandlerContext;)V",
		at = @At("HEAD")
	)
	private void onChannelInactive(ChannelHandlerContext ctx, CallbackInfo ci) {
		KickLoggerManager.notifyChannelInactive();
	}
}
