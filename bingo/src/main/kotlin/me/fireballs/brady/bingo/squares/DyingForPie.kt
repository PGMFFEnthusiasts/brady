package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.bingo.mechanics.gapplePie
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerItemConsumeEvent

class DyingForPie(squareIndex: Int) : Square("Dying for Pie", squareIndex) {
    @EventHandler(ignoreCancelled = true)
    private fun onEat(event: PlayerItemConsumeEvent) {
        if (event.item?.isSimilar(gapplePie) == false) return
        complete(event.player)
    }

    override fun matchesDomainRestriction() = true to "⸬ or ▦"
}
