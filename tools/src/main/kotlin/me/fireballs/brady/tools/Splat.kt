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
    private var splatStatus = true

    init {
        tools.registerEvents(this)

        command("splat", permission = "brady.splat") {
            executor {
                sender.send("&7/splat [&aon&7 | &coff&7] (current ".cc() + (if (splatStatus) "&aON" else "&cOFF") + "&7)")
            }

            subcommand("on") {
                executor {
                    sender.send("&aON".cc())
                    okay.play(sender)
                    splatStatus = true
                }
            }

            subcommand("off") {
                executor {
                    sender.send("&cOFF".cc())
                    uhOh.play(sender)
                    splatStatus = false
                }
            }
        }
    }

    private val thud = soundbox()
        .add(Sound.DIG_GRASS, 1.75f)
        .add(Sound.DIG_SNOW, 1.75f)
        .add(Sound.NOTE_BASS, 0.5f)

    private var ignoreNextHit = false

    @EventHandler
    private fun onSplat(event: ProjectileHitEvent) {
        if (!splatStatus) return
        if (event.entityType != EntityType.SNOWBALL) return
//        val hitLocation = event.entity.location.toVector()
//            .add(event.entity.velocity)
//            .toLocation(event.world)
//        val solid = hitLocation.block.type.isSolid
//        log("snowball-hits", "solid = $solid")
        log("snowball-hits", "ignoreNextHit = $ignoreNextHit")
        if (!ignoreNextHit) thud.broadcast(event.world)
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
