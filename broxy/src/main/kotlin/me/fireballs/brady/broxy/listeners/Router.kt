package me.fireballs.brady.broxy.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import me.fireballs.brady.broxy.Broxy

class Router(
    private val plugin: Broxy,
) {
    @Subscribe
    fun onRoute(event: PlayerChooseInitialServerEvent) {
//        if (plugin.server.allServers.all { it.playersConnected.isEmpty() }) return
        event.setInitialServer(
            plugin.server.allServers
                // primary -> secondary -> tertiary
                .sortedBy { it.serverInfo.name }
//                .reversed()
                .maxByOrNull { it.playersConnected.size }
        )
    }
}
