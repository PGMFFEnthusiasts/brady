package me.fireballs.share.util;

public record PlayerFootballStats(
    int team, int kills, int deaths, int assists, int killstreak, double damageDealt, double damageTaken, int pickups,
    int throwz, int passes, int catches, int strips, int touchdowns, int touchdownPasses, int totalPassingBlocks,
    int totalReceivingBlocks
) {}