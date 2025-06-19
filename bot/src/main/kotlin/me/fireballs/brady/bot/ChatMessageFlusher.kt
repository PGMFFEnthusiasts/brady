package me.fireballs.brady.bot

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.delay
import me.fireballs.brady.bot.utils.ansify
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.component
import me.fireballs.brady.core.event.BradyShareEvent
import me.fireballs.brady.core.plus
import me.fireballs.brady.core.registerEvents
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.IncomingWebhookClient
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.WebhookClient
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
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.seconds

class ChatMessageFlusher : Listener, KoinComponent {
    private val bot by inject<Bot>()
    private val jda by inject<JDA>()

    private val queue = ConcurrentLinkedQueue<String>()
    private var client: IncomingWebhookClient? = null

    init {
        bot.registerEvents(this)
        val webhookUrl = System.getenv("LOGGING_WEBHOOK")
        if (!webhookUrl.isNullOrEmpty()) client = WebhookClient.createClient(jda, webhookUrl)
        bot.launch {
            while (true) {
                delay(3.seconds)
                try {
                    flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun flush() {
        val linesToFlush = mutableListOf<String>()
        var line: String?

        while (queue.poll().also { line = it } != null) linesToFlush.add(line!!)
        if (linesToFlush.isEmpty()) return

        client?.sendMessage("```ansi\n${linesToFlush.joinToString("\n")}\n```")
            ?.setAvatarUrl("https://i.kunet.dev/log.png")
            ?.setUsername("Loggy")
            ?.setSuppressedNotifications(true)
            ?.setAllowedMentions(emptySet<Message.MentionType>())
            ?.await()
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
        queue.offer(ansify("&f» &6Match stats: &9&n${event.link}".cc()))
    }
}
