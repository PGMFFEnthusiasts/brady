package me.fireballs.brady.bingo.squares

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.log
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.player.MatchPlayer

class DynamicDuo(squareIndex: Int) : Square("Dynamic Duo", squareIndex) {
    private var count = 0
    private var couple = mutableSetOf<MatchPlayer>()

    @EventHandler
    private fun onPassingTD(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.TOUCHDOWN_PASS) return
        val newCouple = mutableSetOf(event.thrower, event.catcher)
        if (couple != newCouple) {
            count = 1
            log("bingo", "(duo) new couple $count")
            couple = newCouple
            return
        }

        count += 1
        log("bingo", "(duo) couple $count")
        if (count == 5) bingo.launch {
            delay(20.ticks)
            complete(couple.map { it.bukkit })
        }
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        count = 0
        couple.clear()
    }

    override fun matchesDomainRestriction() = playerFloorOf(2, "Requires ≥2 BFFs playing")
}
