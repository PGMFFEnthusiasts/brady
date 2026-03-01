package me.fireballs.brady.bingo.mechanics

import me.fireballs.brady.bingo.Bingo
import me.fireballs.brady.core.registerEvents
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ShapelessRecipe
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import kotlin.getValue

class PieCooking : Listener, KoinComponent {
    private val bingo by inject<Bingo>()

    init {
        bingo.registerEvents(this)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onMatchLoad(event: MatchAfterLoadEvent) {
        event.world.addRecipe(
            ShapelessRecipe(uncookedPie)
                .addIngredient(Material.GOLDEN_APPLE)
                .addIngredient(Material.GOLDEN_APPLE)
                .addIngredient(Material.GOLDEN_APPLE)
        )
    }
}
