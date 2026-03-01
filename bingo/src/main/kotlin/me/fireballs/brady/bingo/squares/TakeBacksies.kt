package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.c
import me.fireballs.brady.core.log
import me.fireballs.brady.core.plus
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import me.fireballs.brady.corepgm.forWhom
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.player.MatchPlayer

class TakeBacksies(squareIndex: Int) : Square("Take Backsies", squareIndex) {
    private var carrierChanger: Pair<MatchPlayer, Long>? = null

    @EventHandler
    private fun onCarrierChange(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.PASS) return
        carrierChanger = event.thrower to System.currentTimeMillis()
        log("bingo", "(take backsies) waiting for ".c() + event.thrower.name.forWhom() + " to take back")
    }

    @EventHandler
    private fun onStealBack(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.BALL_STEAL) return
        val cc = carrierChanger ?: return
        if (event.actor != cc.first) return
        if (System.currentTimeMillis() - cc.second > 10_000) {
            log("bingo", "(take backsies) not complete in time")
            carrierChanger = null
            return
        }
        complete(event.actor.bukkit)
    }

    @EventHandler
    private fun onPick(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.BALL_PICKUP) return
        carrierChanger = null
        log("bingo", "(take backsies) erm. i'm resetting ts")
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        carrierChanger = null
    }

    override fun matchesDomainRestriction() = playerFloorOf(2)
}
