package me.fireballs.share.football;

import tc.oc.pgm.api.player.MatchPlayer;

public interface FootballListener {
    default void onPassPossessionCompletion(CompletedFootballThrow completedThrow) {}
    default void onThrow(MatchPlayer thrower) {}
    default void onCatch(MatchPlayer catcher) {}
    default void onPass(MatchPlayer thrower, MatchPlayer catcher) {}
    default void onCarrierChange(MatchPlayer newCarrier) {}
    default void onTouchdown(MatchPlayer player) {}
    default void onTouchdownPass(MatchPlayer thrower) {}
    default void onBallPickup(MatchPlayer thrower) {}
    default void onBallSteal(MatchPlayer stealer) {}
}