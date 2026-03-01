package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerItemConsumeEvent
import tc.oc.pgm.api.match.event.MatchFinishEvent
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.teams.Team

class WWCD(squareIndex: Int) : Square("W.W.C.D.", squareIndex) {
    private val eatenSet = mutableSetOf<MatchPlayer>()

    @EventHandler
    private fun onEat(event: PlayerItemConsumeEvent) {
        if (event.item?.type != Material.COOKED_CHICKEN) return
        val mp = player(event.player) ?: return
        if (mp.isObserving) return
        eatenSet.add(mp)
    }

    private fun checkWin(event: MatchFinishEvent) {
        if (event.winners.size != 1) return
        val soleWinner = event.winners.first()
        if (soleWinner !is Team) return
        val winners = soleWinner.players.toSet().intersect(eatenSet)
        complete(winners.map { it.bukkit }.toList())
    }

    @EventHandler
    private fun onMatchFinish(event: MatchFinishEvent) {
        checkWin(event)
        eatenSet.clear()
    }
}
