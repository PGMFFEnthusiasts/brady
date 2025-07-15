package me.fireballs.share.football;

import tc.oc.pgm.api.player.MatchPlayer;

public interface FootballListener {
    default void onPassPossessionCompletion(CompletedFootballThrow completedThrow) {}
    default void onPass(MatchPlayer thrower, MatchPlayer catcher) {}
    default void onCarrierChange(MatchPlayer newCarrier) {}
}
