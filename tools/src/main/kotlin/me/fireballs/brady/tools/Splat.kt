package me.fireballs.brady.tools

import me.fireballs.brady.core.*
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.event.ActionNodeTriggerEvent

class Splat : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val settings by inject<ToolsSettings>()
    private var splatStatus = FeatureFlagBool("splatEnabled", true)

    init {
        tools.registerEvents(this)
    }

    private val thud = soundbox()
        .add(Sound.DIG_GRASS, 1.75f)
        .add(Sound.DIG_SNOW, 1.75f)
        .add(Sound.NOTE_BASS, 0.5f)

    private var ignoreNextHit = false

    @EventHandler
    private fun onSplat(event: ProjectileHitEvent) {
        if (!splatStatus.state) return
        if (event.entityType != EntityType.SNOWBALL) return
//        val hitLocation = event.entity.location.toVector()
//            .add(event.entity.velocity)
//            .toLocation(event.world)
//        val solid = hitLocation.block.type.isSolid
//        log("snowball-hits", "solid = $solid")
        log("snowball-hits", "ignoreNextHit = $ignoreNextHit")
        if (!ignoreNextHit) {
            for (player in event.world.players) {
                if (settings.splatSetting.retrieveValue(player)) thud.play(player)
            }
        }
        ignoreNextHit = false
    }

    @EventHandler
    private fun onFlagReset(event: ActionNodeTriggerEvent) {
        if (event.nodeId != "reset-flag") return
        log("snowball-hits", "resetting to assume not player hit")
        ignoreNextHit = false
    }

    @EventHandler(ignoreCancelled = false)
    private fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Snowball) return

        ignoreNextHit = true
        log("snowball-hits", "damagedBySnowball (ignore next hit event)")
    }
}
