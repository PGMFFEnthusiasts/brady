package me.fireballs.brady.broxy.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import me.fireballs.brady.broxy.Broxy
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import kotlin.jvm.optionals.getOrNull

class Status(
    private val plugin: Broxy,
    private val jda: JDA,
) {
    init {
        update()
    }

    private data class ServerStatus(var count: Int, var name: String) {}

    private fun update() {
        val servers = plugin.server.allServers?.mapNotNull { server ->
            val name = server?.serverInfo?.name ?: return@mapNotNull null
            ServerStatus(0, name)
        }?.sortedBy { it.name } ?: listOf()

        if (servers.isEmpty()) return

        plugin.server.allPlayers.forEach { player ->
            servers.find { it.name == player.currentServer.getOrNull()?.serverInfo?.name }?.count++
        }

        jda.presence.activity = Activity.of(
            Activity.ActivityType.PLAYING,
            "TB | ${servers.joinToString(separator = " : ") { status -> status.count.toString() }}",
        )
    }

    @Subscribe
    fun onStateUpdate(event: ServerPostConnectEvent) {
        update()
    }

    @Subscribe
    fun onStateUpdateButLeave(event: DisconnectEvent) {
        update()
    }
}
