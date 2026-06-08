package me.fireballs.brady.corepgm

import me.fireballs.brady.core.plainText
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import tc.oc.pgm.api.map.MapInfo
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.flag.FlagMatchModule
import tc.oc.pgm.flag.state.Carried
import tc.oc.pgm.regions.CuboidRegion
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

fun CuboidRegion.center(): Vector {
    val min = bounds.min
    val max = bounds.max

    return Vector(
        (min.x + max.x) / 2.0,
        (min.y + max.y) / 2.0,
        (min.z + max.z) / 2.0
    )
}

fun MatchPlayer.isFlagCarrier(): Boolean =
    match.getModule(FlagMatchModule::class.java)
        ?.flags
        ?.asSequence()
        ?.map { it.state }
        ?.filterIsInstance<Carried>()
        ?.any { it.carrier == this }
        ?: false
