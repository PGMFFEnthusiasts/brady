package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.player.MatchPlayer

class ThatWorked(squareIndex: Int) : Square("That Worked?", squareIndex) {
    private data class Attempt(
        val player: MatchPlayer,
        val time: Long = System.currentTimeMillis(),
    )

    private var currentAttempt: Attempt? = null

    @EventHandler
    private fun onSelfPass(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.PASS) return
        if (event.thrower != event.catcher) {
            currentAttempt = null
            return
        }
        currentAttempt = Attempt(event.thrower)
    }

    @EventHandler
    private fun onTouchdown(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.TOUCHDOWN) return
        val attempt = currentAttempt ?: return
        if (event.actor != attempt.player) return
        if (System.currentTimeMillis() - attempt.time > 15_000L) {
            currentAttempt = null
            return
        }
        currentAttempt = null
        complete(attempt.player.bukkit)
    }

    @EventHandler
    private fun onPick(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.BALL_PICKUP) return
        currentAttempt = null
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        currentAttempt = null
    }
}
