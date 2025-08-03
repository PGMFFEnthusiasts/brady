package me.fireballs.brady.broxy.listeners

import com.github.shynixn.mccoroutine.velocity.launch
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import me.fireballs.brady.broxy.Broxy
import me.fireballs.brady.broxy.utils.Constants.PRIMARY_SERVER_ID
import me.fireballs.brady.broxy.utils.Constants.SECONDARY_SERVER_ID
import me.fireballs.brady.broxy.utils.cc
import me.fireballs.brady.broxy.utils.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.newline
import kotlin.jvm.optionals.getOrNull

class BetterMOTD(
    private val plugin: Broxy,
) {
    init {
        plugin.pluginContainer.launch {
            while (true) {
                updateServerStatuses()
                delay(5000L)
            }
        }
    }

    private var primaryStatus = "&4No data.".cc() as Component
    private var secondaryStatus = "&4No data.".cc() as Component

    private suspend fun updateServerStatuses() {
        plugin.server.allServers
            .map { it.serverInfo.name to runCatching { it.ping().await() } }
            .forEach { (server, result) ->
                val description = result.getOrNull()?.descriptionComponent ?: "&4No data.".cc() as Component
                if (server == PRIMARY_SERVER_ID) primaryStatus = description
                if (server == SECONDARY_SERVER_ID) secondaryStatus = description
            }
    }

    private fun tabulateCounts() =
        plugin.server.allPlayers
            .mapNotNull { it.currentServer.getOrNull()?.serverInfo?.name }
            .count { it == "primary" } to plugin.server.allPlayers
            .mapNotNull { it.currentServer.getOrNull()?.serverInfo?.name }
            .count { it == "secondary" }

    @Subscribe
    fun messageOfTheDayRequest(event: ProxyPingEvent) {
        val (primary, secondary) = tabulateCounts()
        val description =
            "&8➀ &7($primary) ".cc() + primaryStatus + newline() + "&8➁ &7($secondary) " + secondaryStatus

        event.ping = ServerPing(
            ServerPing.Version(event.ping.version.protocol, "Brady"),
            event.ping.players.getOrNull()?.let {
                ServerPing.Players(it.online, plugin.server.allServers.size * 50, it.sample)
            },
            description,
            event.ping.favicon.getOrNull(),
        )
    }
}
