package me.fireballs.brady.corepgm

import org.bukkit.Bukkit
import tc.oc.pgm.api.match.MatchManager

fun MatchManager.currentMatch() = getMatch(Bukkit.getConsoleSender())
