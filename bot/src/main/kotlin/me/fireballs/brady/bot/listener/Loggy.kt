package me.fireballs.brady.bot.listener

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import io.nats.client.Nats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.fireballs.brady.bot.Bot
import me.fireballs.brady.core.ansify
import me.fireballs.brady.core.*
import me.fireballs.brady.core.event.BradyShareEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.event.ChannelMessageEvent
import tc.oc.pgm.channels.AdminChannel
import tc.oc.pgm.channels.GlobalChannel
import tc.oc.pgm.channels.TeamChannel
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

class Loggy : Listener, KoinComponent {
    private val bot by inject<Bot>()

    private val queue = ConcurrentLinkedQueue<String>()
    private val prefix = ansify("&8(${System.getenv("BRADY_SERVER")}) ".cc())

    init {
        bot.registerEvents(this)

        bot.launch(bot.asyncDispatcher) {
            while (true) {
                delay(100L)
                logExceptions { flush() }
            }
        }
    }

    private suspend fun flush() {
        val linesToFlush = mutableListOf<String>()
        var line: String?

        while (queue.poll().also { line = it } != null) linesToFlush.add(prefix + line!!)
        if (linesToFlush.isEmpty()) return

        withContext(Dispatchers.IO) {
            runCatching {
                Nats.connect().use {
                    it.publish("loggy", linesToFlush.joinToString("\n").encodeToByteArray())
                    it.flush(Duration.ofSeconds(5L))
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun channelMessageEvent(event: ChannelMessageEvent<*>) {
        val channel = event.channel
        val sender = event.sender
        val componentOut = when (channel) {
            is AdminChannel -> "&f[&6A&f] ".cc() + sender.name + ": " + event.component
            is TeamChannel -> sender.party.chatPrefix + sender.name + ": " + event.component
            is GlobalChannel -> "<".cc() + sender.name + "> " + event.component
            // disabled for privacy concerns
            // is PrivateMessageChannel -> sender.name + " &7→ " + (event.target as MatchPlayer).name + ": " + event.component
            else -> null
        } ?: return
        queue.offer(
            ansify(componentOut)
                .replace("```", "`\u00AD`\u00AD`")
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onJoin(event: PlayerJoinEvent) {
        queue.offer(ansify(event.player.component() + " joined the game"))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onQuit(event: PlayerQuitEvent) {
        queue.offer(ansify(event.player.component() + " left the game"))
    }

    @EventHandler
    private fun onShare(event: BradyShareEvent) {
        // yes, I know it's not clickable. deal with it!
        queue.offer(ansify("&f» &6${event.prefix}: &9&n${event.link}".cc()))
    }
}
