package me.fireballs.brady.broxy.listeners

import com.github.shynixn.mccoroutine.velocity.launch
import com.google.common.collect.MapMaker
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.fireballs.brady.broxy.Broxy
import me.fireballs.brady.broxy.utils.c
import me.fireballs.brady.broxy.utils.cc
import me.fireballs.brady.broxy.utils.plus
import me.fireballs.brady.broxy.utils.url
import java.time.Duration
import java.util.concurrent.ConcurrentMap
import kotlin.jvm.optionals.getOrNull

class StatusPull(
    private val plugin: Broxy,
    val statusMap: ConcurrentMap<String, String> = MapMaker()
        .concurrencyLevel(2)
        .makeMap()
) {
    init {
        plugin.pluginContainer.launch {
            withContext(Dispatchers.IO) {
                val natsClient = runCatching {
                    Nats.connect(System.getenv("BRADY_NATS") ?: Options.DEFAULT_URL)
                }.getOrNull() ?: return@withContext

                val sub = natsClient.subscribe("status.*")
                natsClient.flush(Duration.ofSeconds(5))

                while (true) {
                    val msg = sub.nextMessage(0)
                    val key = msg.subject
                    val content = msg.data.decodeToString()
                    statusMap[key] = content
                }
            }
        }
    }

    @Subscribe
    fun onPostConnect(event: ServerPostConnectEvent) {
        plugin.pluginContainer.launch {
            delay(500)
            plugin.server.allServers
                .sortedBy { it.serverInfo.name }
                .forEach {
                    statusMap["status.${it.serverInfo.name}"]?.let { status ->
                        event.player.sendMessage("&6» &6&n${it.serverInfo.name.uppercase()}&7 (${it.playersConnected.size} online)".cc())
                        event.player.sendMessage("".c())
                        event.player.sendMessage(status.cc(false))
                        event.player.sendMessage("".c())
                    }
                }

            event.player.sendMessage(
                "&6» You are currently on &l${event.player.currentServer.getOrNull()?.serverInfo?.name}&6.".cc() + " &7&o(psst... check out our ".cc() + "&9&nDISCORD".cc()
                    .url("https://tombrady.fireballs.me/discord") + "&7&o)"
            )
        }
    }
}