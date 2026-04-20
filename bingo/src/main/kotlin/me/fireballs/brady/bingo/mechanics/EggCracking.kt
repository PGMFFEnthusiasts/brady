package me.fireballs.brady.bingo.mechanics

import me.fireballs.brady.bingo.Bingo
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.itembox
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.tools.ToolsSettings
import me.fireballs.brady.tools.pvpfx.ProjectileSkins
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Chicken
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.event.ActionNodeTriggerEvent

class EggCracking : Listener, KoinComponent {
    private val bingo by inject<Bingo>()
    private val toolsSettings by inject<ToolsSettings>()

    init {
        bingo.registerEvents(this)
    }

    private var ignoreNextHit = false

    @EventHandler
    private fun onFlagReset(event: ActionNodeTriggerEvent) {
        if (event.nodeId != "reset-flag") return
        ignoreNextHit = false
    }

    @EventHandler(ignoreCancelled = false)
    private fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Snowball) return
        ignoreNextHit = true
    }

    private fun checkSpawn(player: Player, spawn: Location) {
        if (player.world != spawn.world) return
        if (player.location.distance(spawn) > 6) return
        val skin = toolsSettings.projectileSkin.retrieveValue(player)
        if (skin != ProjectileSkins.EGG) return
        val offspring = spawn.world.spawn(spawn.clone().add(0.0, 0.5, 0.0), Chicken::class.java)
        offspring.setBaby()
        offspring.ageLock = true
    }

    @EventHandler
    private fun onCrack(event: ProjectileHitEvent) {
        if (event.entityType != EntityType.SNOWBALL) return
        val thrower = event.entity.shooter
        if (thrower !is Player) return
        val egg = event.entity
        if (!ignoreNextHit) checkSpawn(thrower, egg.location)
        ignoreNextHit = false
    }

    @EventHandler
    private fun onFeedChicken(event: PlayerInteractEntityEvent) {
        val chicken = event.rightClicked
        if (chicken !is Chicken) return
        if (chicken.isAdult) return
        val item = event.player.itemInHand
        if (item.type != Material.GOLDEN_APPLE) return
        item.amount -= 1
        event.player.itemInHand = if (item.amount == 0) null else item
        chicken.setAdult()
        chicken.world.playSound(chicken.location, Sound.BURP, 1f, 1f)
    }
}
