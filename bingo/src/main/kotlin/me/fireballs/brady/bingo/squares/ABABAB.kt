package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.log
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.player.MatchPlayer

class ABABAB(squareIndex: Int) : Square("ABABAB", squareIndex) {
    private var count = 0
    private var couple = mutableSetOf<MatchPlayer>()

    @EventHandler
    private fun onPassing(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.PASS) return
        val newCouple = mutableSetOf(event.thrower, event.catcher)
        if (couple != newCouple) {
            count = 1
            log("bingo", "(ABABAB) new couple $count")
            couple = newCouple
            return
        }

        count += 1
        log("bingo", "(ABABAB) couple $count")
        if (count == 6) complete(couple.map { it.bukkit })
    }

    @EventHandler
    private fun onPick(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.BALL_PICKUP) return
        count = 0
        couple.clear()
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        count = 0
        couple.clear()
    }

    override fun matchesDomainRestriction() = playerFloorOf(2)
}
