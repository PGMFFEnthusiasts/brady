package me.fireballs.brady.core

import org.bukkit.Bukkit
import tc.oc.pgm.api.match.MatchManager

fun MatchManager.currentMatch() = getMatch(Bukkit.getConsoleSender())!!
