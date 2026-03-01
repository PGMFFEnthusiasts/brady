package me.fireballs.brady.bingo.squares

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.log
import me.fireballs.brady.core.plus
import me.fireballs.brady.corepgm.currentMatch
import me.fireballs.brady.corepgm.forWhom
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchFinishEvent
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.flag.FlagMatchModule
import tc.oc.pgm.flag.state.Carried

class HotPotato(squareIndex: Int) : Square("Hot Potato", squareIndex) {
    private val hotPotatoMap = mutableMapOf<MatchPlayer, Int>()

    init {
        bingo.launch {
            while (bingo.isEnabled) {
                delay(1.ticks)
                poll()
            }
        }
    }

    private fun poll() {
        val match = matchManager.currentMatch() ?: return
        if (!match.isRunning) return
        val flagModule = match.getModule(FlagMatchModule::class.java) ?: return
        val flag = flagModule.flags.firstOrNull() ?: return
        val state = flag.state
        if (state !is Carried) return
        val carrier = state.carrier
        val nextCount = (hotPotatoMap[carrier] ?: 0) + 1
        hotPotatoMap[carrier] = nextCount
        if (nextCount == 20 * 5) log("bingo", "(hot) ".cc() + carrier.name.forWhom())
    }

    @EventHandler
    private fun onMatchFinish(event: MatchFinishEvent) {
        complete(hotPotatoMap.filter { it.value <= 20 * 5 }.map { it.key.bukkit })
        hotPotatoMap.clear()
    }

    // realistically not possible without ≥2 players
    override fun matchesDomainRestriction() = playerFloorOf(2)
}
