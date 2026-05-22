package me.fireballs.brady.corepgm

import me.fireballs.brady.core.plainText
import org.bukkit.Bukkit
import tc.oc.pgm.api.map.MapInfo
import tc.oc.pgm.api.match.MatchManager

fun MatchManager.currentMatch() = getMatch(Bukkit.getConsoleSender())

fun MapInfo.isTouchdown(): Boolean = gamemode?.plainText() == "Touchdown"
