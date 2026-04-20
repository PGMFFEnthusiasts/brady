package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.log
import me.fireballs.brady.core.plus
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import me.fireballs.brady.corepgm.forWhom
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.player.MatchPlayer

class Explosion(squareIndex: Int) : Square("Explosion", squareIndex) {
    private class TimedPass(
        val thrower: MatchPlayer,
        val catcher: MatchPlayer,
        val time: Long = System.currentTimeMillis()
    )

    private var currentPass: TimedPass? = null

    @EventHandler
    private fun onPassComplete(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.PASS) return
        if (event.thrower == event.catcher) return
        currentPass = TimedPass(event.thrower ?: return, event.catcher ?: return)
        log("bingo", "(explosion) marking ".cc() + event.thrower.name.forWhom())
    }

    @EventHandler
    private fun onTDPass(event: BradyFootballLifecycleEvent) {
        val current = currentPass ?: return
        if (current.time + 5_000L < System.currentTimeMillis()) {
            log("bingo", "(explosion) invalidly timed pass lol")
            currentPass = null
            return
        }

        if (event.action != FootballLifecycleAction.TOUCHDOWN_PASS) return
        val thrower = event.thrower
        val catcher = event.catcher
        if (current.thrower != thrower || current.catcher != catcher) return

        val delta = catcher.bukkit.location.subtract(thrower.bukkit.location).toVector().normalize()
        val look = thrower.bukkit.location.direction
        val dot = delta.dot(look)
        log("bingo", "(explosion) dot: $dot")
        if (dot > -0.75) return

        complete(current.thrower.bukkit)
    }

    @EventHandler
    private fun onPick(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.BALL_PICKUP) return
        currentPass = null
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        currentPass = null
    }

    override fun matchesDomainRestriction() = playerFloorOf(2)
}
