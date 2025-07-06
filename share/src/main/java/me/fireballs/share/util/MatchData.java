package me.fireballs.share.util;

public record MatchData(
    String server, long startTime, int duration, int winner, int teamOneScore, int teamTwoScore, String map,
    boolean isTourney
) { }