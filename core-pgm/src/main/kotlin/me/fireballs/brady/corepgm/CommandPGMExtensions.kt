package me.fireballs.brady.corepgm

import me.fireballs.brady.core.CommandExecution

fun CommandExecution.match() = CorePGM.matchManager.getMatch(player().world) ?: err("Match not found")
