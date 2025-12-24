package me.fireballs.share.football;

import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;

public record CompletedFootballPass(
    MatchPlayer thrower, Location throwLocation,
    MatchPlayer catcher, Location catchLocation, Location lossOfControlLocation
) { }
