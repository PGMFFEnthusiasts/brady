package me.fireballs.brady.bot.listener

import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fireballs.brady.bot.Bot
import me.fireballs.brady.core.generateInGameInfoBoard
import me.fireballs.brady.core.registerEvents
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent
import tc.oc.pgm.events.PlayerJoinMatchEvent
import tc.oc.pgm.events.PlayerLeaveMatchEvent
import java.time.Duration

class StatusPush : Listener, KoinComponent {
    private val bot by inject<Bot>()

    private val natsClient = runCatching {
        Nats.connect(System.getenv("BRADY_NATS") ?: Options.DEFAULT_URL)
    }.getOrNull()

    init {
        bot.registerEvents(this)
    }

    private suspend fun publishStatus() {
        withContext(Dispatchers.IO) {
            runCatching {
                natsClient?.publish(
                    "status.${System.getenv("BRADY_SERVER") ?: "unknown"}",
                    generateInGameInfoBoard().encodeToByteArray()
                )
                natsClient?.flush(Duration.ofSeconds(5L))
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private suspend fun onJoin(event: PlayerJoinEvent) {
        publishStatus()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private suspend fun onQuit(event: PlayerQuitEvent) {
        publishStatus()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private suspend fun onMatchJoin(event: PlayerJoinMatchEvent) {
        publishStatus()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private suspend fun onMatchLeave(event: PlayerLeaveMatchEvent) {
        publishStatus()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private suspend fun onMatchStatusChange(event: MatchPhaseChangeEvent) {
        publishStatus()
    }
}
