package me.fireballs.brady.bingo.mechanics

import me.fireballs.brady.bingo.Bingo
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.itembox
import me.fireballs.brady.core.registerEvents
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

val chicken = itembox(Material.COOKED_CHICKEN)
    .name("&cSlightly Burnt Chicken".cc())
    .shiny()
    .build()

val uncookedPie = itembox(Material.PUMPKIN_PIE)
    .name("&cUncooked Pie".cc())
    .lore("&7Bake in oven at 425F for 35-45 mins".cc(), "&7...or crisp in the fire for a few ticks".cc())
    .build()

val gapplePie = itembox(Material.PUMPKIN_PIE)
    .name("&eGapple Pie".cc())
    .shiny()
    .lore("&7Delightful and decadent".cc())
    .build()

class Cooking : Listener, KoinComponent {
    private val bingo by inject<Bingo>()

    init {
        bingo.registerEvents(this)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onHotbarSlotSwitch(event: PlayerItemHeldEvent) {
        val currentSlot = event.player.inventory.getItem(event.newSlot)
        if (currentSlot?.type == Material.COOKED_CHICKEN || currentSlot?.isSimilar(gapplePie) == true) {
            event.player.foodLevel = 16
            return
        }
        if (event.player.foodLevel < 20) event.player.foodLevel = 20
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun itemPickupEvent(event: PlayerPickupItemEvent) {
        if (event.player.itemInHand?.type == Material.COOKED_CHICKEN || event.player.itemInHand?.isSimilar(gapplePie) == true)
            event.player.foodLevel = 16
    }

    @EventHandler(ignoreCancelled = true)
    private fun onEat(event: PlayerItemConsumeEvent) {
        if (event.item?.type != Material.COOKED_CHICKEN && event.item?.isSimilar(gapplePie) == false) return
        event.player.foodLevel = 20
    }

//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    private fun foodLevelChange(event: FoodLevelChangeEvent) {
//        if (!event.isCancelled) return
//        val player = event.entity
//        if (player !is Player) return
//        event.isCancelled = player.foodLevel >= event.foodLevel
//    }

    @EventHandler
    private fun onItemDamage(event: EntityDamageEvent) {
        val item = event.entity
        if (item !is Item) return

        if (event.cause != EntityDamageEvent.DamageCause.FIRE
            && event.cause != EntityDamageEvent.DamageCause.FIRE_TICK
            && event.cause != EntityDamageEvent.DamageCause.LAVA
        ) return

        if (item.itemStack?.type == Material.COOKED_CHICKEN || item.itemStack?.isSimilar(gapplePie) == true) {
            event.isCancelled = true
            return
        }

        if (item.itemStack?.type == Material.RAW_CHICKEN) {
            item.itemStack = chicken.clone()
            event.isCancelled = true
            return
        }

        if (item.itemStack?.isSimilar(uncookedPie) == true) {
            item.itemStack = gapplePie.clone()
            event.isCancelled = true
            return
        }
    }
}
