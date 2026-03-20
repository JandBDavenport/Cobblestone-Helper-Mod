package com.jandbdavenport.mixin.client.network;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents IllegalStateException when the server sends team removal packets for teams
 * the client doesn't know about. This occurs on servers with custom glow color systems
 * (e.g. play.cobblestone.gg) when a player's glow changes and the server sends a
 * "remove player from team" packet, but the client is out of sync and the player isn't
 * actually on that team.
 */
@Mixin(Scoreboard.class)
public class ScoreboardTeamSyncMixin {

    @Inject(
        method = "removeScoreHolderFromTeam(Ljava/lang/String;Lnet/minecraft/scoreboard/Team;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void preventTeamSyncCrash(String playerName, Team team, CallbackInfo ci) {
        // Check if the player is actually on this team before attempting removal
        if (!team.getPlayerList().contains(playerName)) {
            System.out.println("[CobblestoneHelper] Team sync: suppressed removal of '"
                + playerName + "' from team '" + team.getName() + "' (player not on team)");
            ci.cancel();
        }
    }
}
