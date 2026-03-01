package me.fireballs.brady.bingo.squares

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.bingo.Square
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.player.MatchPlayer

class Ax2bxc(squareIndex: Int) : Square("ax^2+bx+c", squareIndex) {
    private var playerSprintingEveryTick: MatchPlayer? = null

    @EventHandler
    private fun onThrow(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.THROW) return
        playerSprintingEveryTick = event.actor
    }

    @EventHandler
    private fun onCatch(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.CATCH) return
        if (event.thrower != event.catcher) return
        if (event.catcher != playerSprintingEveryTick) return
        playerSprintingEveryTick = null
        complete(event.thrower.bukkit)
    }

    init {
        bingo.launch {
            while (bingo.isEnabled) {
                delay(1.ticks)
                val target = playerSprintingEveryTick ?: continue
                if (!target.bukkit.isSprinting) playerSprintingEveryTick = null
            }
        }
    }
}
