package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.ProgressSquare
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import java.util.*

class Mogged(squareIndex: Int) : ProgressSquare("Mogged", squareIndex, 7) {
    private val mogTracker = mutableMapOf<UUID, MutableMap<UUID, Long>>()

    @EventHandler
    private fun onSneak(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return
        val mp = player(event.player) ?: return
        if (!mp.isParticipating) return
        val victim = mp.match.players
            .filter { it != mp }
            .filter { it.isParticipating && it.world == mp.bukkit.world && it.isDead }
            .firstOrNull { it.location.distance(mp.location) <= 2.5 } ?: return
        val mogMap = mogTracker.getOrPut(mp.bukkit.uniqueId) { mutableMapOf() }
        val now = System.currentTimeMillis()
        if ((mogMap[victim.bukkit.uniqueId] ?: 0L) + 5000L > now) return
        mogMap[victim.bukkit.uniqueId] = now
        increment(mp.bukkit)
    }

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        mogTracker.remove(event.player.uniqueId)
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        mogTracker.clear()
    }

    override fun matchesDomainRestriction() = playerFloorOf(2)
}
