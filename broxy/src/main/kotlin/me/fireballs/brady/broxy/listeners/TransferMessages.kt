package me.fireballs.brady.broxy.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import me.fireballs.brady.broxy.utils.newValkeyClient

class TransferMessages {
    private val valkey = newValkeyClient()

    @Subscribe
    private fun onPreConnect(event: ServerPreConnectEvent) {
        val from = event.previousServer
        val to = event.originalServer
        if (from == null) return
        valkey.publish(
            "live-transfers",
            "${event.player.uniqueId}\t${from.serverInfo.name}\t${to.serverInfo.name}",
        )
    }
}
