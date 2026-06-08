package me.fireballs.brady.corepgm

import me.fireballs.brady.core.plainText
import org.bukkit.Bukkit
import tc.oc.pgm.api.map.MapInfo
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.variables.Variable
import tc.oc.pgm.variables.VariablesMatchModule

fun MatchManager.currentMatch() = getMatch(Bukkit.getConsoleSender())

fun MapInfo.isTouchdown(): Boolean = gamemode?.plainText() == "Touchdown"

@Suppress("UNCHECKED_CAST")
fun <T : Variable<*>> Match.getVariable(id: String): T? {
    val module = this.getModule(VariablesMatchModule::class.java) ?: return null

    return module.variables
        .filter { it.key == id }
        .map { it.value }
        .findFirst()
        .orElse(null) as? T
}
