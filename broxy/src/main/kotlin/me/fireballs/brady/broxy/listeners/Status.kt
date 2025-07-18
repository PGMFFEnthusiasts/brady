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

    private fun update() {
        var primary = 0
        var secondary = 0

        plugin.server.allPlayers.forEach {
            when (it.currentServer.getOrNull()?.serverInfo?.name) {
                "primary" -> ++primary
                "secondary" -> ++secondary
            }
        }

        jda.presence.activity = Activity.of(
            Activity.ActivityType.PLAYING,
            "TB | $primary : $secondary",
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
