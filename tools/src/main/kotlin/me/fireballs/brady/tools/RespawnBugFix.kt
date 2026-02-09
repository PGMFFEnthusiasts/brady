package me.fireballs.brady.tools

import me.fireballs.brady.core.*
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import tc.oc.pgm.spawns.SpawnMatchModule

class RespawnBugFix : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val pgm by inject<PGM>()

    private val enabled = FeatureFlagBool("respawnBugFix", true)

    init {
        tools.registerEvents(this)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onDeath(event: PlayerDeathEvent) {
        if (!enabled.state) return
        if ((event.entity as CraftPlayer).health > 0.0) return
        event.entity.spigot().respawn()
        val mp = pgm.matchManager.getPlayer(event.entity) ?: return
        val spawn = mp.match.getModule(SpawnMatchModule::class.java)?.chooseSpawn(mp)
        val spawnLocation = spawn?.getSpawn(mp) ?: return
        mp.reset()
        mp.bukkit.teleport(spawnLocation)
        spawn.applyKit(mp)

        val deathVariantMessage = "&c⨷ ".cc() + event.entity.component() + "&c got fully onDeath corrected.".cc()
        pgm.chatManager.adminChannel.getViewers(null)
            .forEach { it.sendMessage(deathVariantMessage) }
    }
}