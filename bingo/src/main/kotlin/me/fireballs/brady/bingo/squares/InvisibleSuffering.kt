package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.ProgressSquare
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerDropItemEvent

class InvisibleSuffering(squareIndex: Int) : ProgressSquare("Invisible Suffering", squareIndex, 5) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onDrop(event: PlayerDropItemEvent) {
        val stack = event.itemDrop.itemStack
        if (stack.type != Material.WOOD_SWORD &&
            stack.type != Material.STONE_SWORD &&
            stack.type != Material.GOLD_SWORD &&
            stack.type != Material.IRON_SWORD &&
            stack.type != Material.DIAMOND_SWORD
        ) return
        val mp = player(event.player) ?: return
        if (mp.isObserving) return
        increment(mp.bukkit)
    }
}
