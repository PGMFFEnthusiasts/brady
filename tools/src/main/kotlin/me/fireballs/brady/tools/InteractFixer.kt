package me.fireballs.brady.tools

import me.fireballs.brady.core.registerEvents
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

// i would love to contribute back to pgm
// but im not sure if there is a true universal
// solution out there to catch everything
// so a hard coded fix has to do for now

class InteractFixer : Listener, KoinComponent {
    private val tools by inject<Tools>()

    init {
        tools.registerEvents(this)
    }

    private val problematicBlocks = EnumSet.of(
        Material.WORKBENCH,
        Material.BEACON,
        Material.FENCE,
        Material.ACACIA_FENCE,
        Material.JUNGLE_FENCE,
        Material.NETHER_FENCE,
        Material.BIRCH_FENCE,
        Material.DARK_OAK_FENCE,
        Material.IRON_FENCE,
        Material.SPRUCE_FENCE,
        Material.TRAP_DOOR,
        Material.IRON_TRAPDOOR,
    )

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.item?.type != Material.SNOW_BALL) return
        if (!problematicBlocks.contains(event.clickedBlock?.type ?: return)) return
        event.setUseItemInHand(Event.Result.DENY)
    }
}
