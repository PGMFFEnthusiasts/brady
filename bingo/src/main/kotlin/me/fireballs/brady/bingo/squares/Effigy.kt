package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent
import tc.oc.pgm.tracker.info.FireInfo

class Effigy(squareIndex: Int) : Square("Effigy", squareIndex) {
    @EventHandler
    private fun death(event: MatchPlayerDeathEvent) {
        if (event.damageInfo !is FireInfo) return
        if (!event.player.isParticipating) return
        complete(event.player.bukkit)
    }
}
