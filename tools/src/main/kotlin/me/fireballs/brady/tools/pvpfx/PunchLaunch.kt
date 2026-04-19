package me.fireballs.brady.tools.pvpfx

import me.fireballs.brady.core.handle
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.sendPacket
import me.fireballs.brady.tools.Tools
import me.fireballs.brady.tools.ToolsSettings
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager

class PunchLaunch : KoinComponent, Listener {
    private val tools by inject<Tools>()
    private val settings by inject<ToolsSettings>()
    private val matchManager by inject<MatchManager>()

    init {
        tools.registerEvents(this)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onLaunch(event: ProjectileLaunchEvent) {
        if (event.entity !is Snowball) return
        val player = event.actor
        if (player !is Player) return
        if (matchManager.getPlayer(player)?.isParticipating != true) return
        if (!settings.punchLaunch.retrieveValue(player)) return
        player.sendPacket(PacketPlayOutAnimation(player.handle, 0))
    }
}
