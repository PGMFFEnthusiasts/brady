package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import me.fireballs.brady.core.Retrieval
import me.fireballs.brady.core.currentMatch
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.stringGet
import me.fireballs.brady.core.stringGetCached
import me.fireballs.brady.core.stringSet
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM

class ACR : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val pgm by inject<PGM>()

    private val kvKey = "pgm.channel"
    private val serializableChannels = setOf("global", "team", "admin")

    init {
        tools.registerEvents(this)

        tools.launch {
            while (true) {
                delay(1.ticks)
                pgm.matchManager.currentMatch()
                    ?.players?.forEach {
                        val channelName = pgm.chatManager.getSelectedChannel(it).displayName
                        val currentChannel = it.bukkit.stringGetCached(kvKey)

                        if (serializableChannels.contains(channelName)) {
                            if (currentChannel == channelName) return@forEach
                            it.bukkit.stringSet(kvKey, channelName)
                        }
                    }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onJoin(event: PlayerJoinEvent) {
        tools.launch {
            val channel = event.player.uniqueId
                .stringGet(kvKey, Retrieval.CACHE_THEN_FRESH).await() ?: return@launch

            val selectedChannel = when (channel) {
                "global" -> pgm.chatManager.globalChannel
                "team" -> pgm.chatManager.teamChannel
                "admin" -> pgm.chatManager.adminChannel
                else -> return@launch
            }

            val mp = pgm.matchManager.getPlayer(event.player) ?: return@launch
            pgm.chatManager.setChannel(mp, selectedChannel)
        }
    }
}
