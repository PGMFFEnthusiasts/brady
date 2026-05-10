package me.fireballs.brady.corepgm

import me.fireballs.brady.core.plainText
import org.bukkit.Bukkit
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchManager

fun MatchManager.currentMatch() = getMatch(Bukkit.getConsoleSender())

fun Match.isTouchdown() = map.gamemode?.plainText() in setOf("Flag Football", "Touchdown")
