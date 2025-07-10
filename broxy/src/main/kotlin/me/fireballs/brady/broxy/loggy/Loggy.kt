package me.fireballs.brady.broxy.loggy

import com.github.shynixn.mccoroutine.velocity.launch
import dev.minn.jda.ktx.coroutines.await
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.fireballs.brady.broxy.Broxy
import me.fireballs.brady.broxy.utils.logExceptions
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.IncomingWebhookClient
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.WebhookClient
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

class Loggy(
    private val plugin: Broxy,
    private val jda: JDA,
) {
    private val queue = ConcurrentLinkedQueue<String>()
    private var client: IncomingWebhookClient? = null

    init {
        val webhookUrl = System.getenv("BRADY_BOT_LOGGING_WEBHOOK")
        if (!webhookUrl.isNullOrEmpty()) client = WebhookClient.createClient(jda, webhookUrl)

        plugin.pluginContainer.launch {
            while (true) {
                delay(100L)
                logExceptions { flush() }
            }
        }

        plugin.pluginContainer.launch {
            withContext(Dispatchers.IO) {
                logExceptions {
                    Nats.connect(
                        Options.builder()
                            .server(Options.DEFAULT_URL)
                            .reconnectWait(Duration.ofSeconds(10))
                            .maxReconnects(-1)
                            .build()
                    ).use {
                        val sub = it.subscribe("loggy")
                        it.flush(Duration.ofSeconds(5))

                        while (true) {
                            val msg = sub.nextMessage(0)
                            val content = msg.data.decodeToString()
                            queue.offer(content)
                        }
                    }
                }
            }
        }
    }

    private suspend fun flush() {
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
}
