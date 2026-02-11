package me.fireballs.brady.tools.pvpfx

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.registerPacketEvents
import me.fireballs.brady.tools.Tools
import me.fireballs.brady.tools.ToolsSettings
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import kotlin.getValue

enum class ProjectileSkins {
    SNOWBALL,
    ENDER_PEARL,
    EGG;

    override fun toString() = name.replace('_', ' ')
}

class ProjectileSkin : PacketListenerAbstract(), Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val settings by inject<ToolsSettings>()

    init {
        tools.registerPacketEvents(this)
        tools.registerEvents(this)
    }

    private val skinnedProjectileMap = mutableMapOf<Int, ProjectileSkins>()

    @EventHandler
    private fun onLaunch(event: ProjectileLaunchEvent) {
        if (event.entity !is Snowball) return
        val p = event.actor
        if (p !is Player) return
        val skin = settings.projectileSkin.retrieveValue(p)
        if (skin == ProjectileSkins.SNOWBALL) return
        skinnedProjectileMap[event.entity.entityId] = skin
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        skinnedProjectileMap.clear()
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.SPAWN_ENTITY) return
        val p = WrapperPlayServerSpawnEntity(event)
        val skin = skinnedProjectileMap[p.entityId] ?: return
        if (skin == ProjectileSkins.ENDER_PEARL) p.entityType = EntityTypes.ENDER_PEARL
        if (skin == ProjectileSkins.EGG) p.entityType = EntityTypes.EGG
        event.markForReEncode(true)
    }
}
