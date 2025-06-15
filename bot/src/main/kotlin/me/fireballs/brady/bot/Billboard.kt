package me.fireballs.brady.bot

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.edit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.fireballs.brady.bot.utils.InfoBoard
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Billboard : KoinComponent {
    private val bot by inject<Bot>()
    private val jda by inject<JDA>()
    private val infoBoard by inject<InfoBoard>()

    private var currentJob: Job? = null

    init {
        jda.listener<ReadyEvent> {
            currentJob?.cancel()
            currentJob = bot.launch {
                while (true) {
                    delay(15_000L)
                    try {
                        tick()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun tick() {
        val generateInfoBoard = infoBoard.generateInfoBoard() ?: return

        jda.guilds.forEach { guild ->
            val channel = guild.channels.find { it.name == "billboard" } ?: return@forEach
            if (channel !is TextChannel) return@forEach

            var pinnedBillboard = channel.retrievePinnedMessages().await()
                .find { it.author.idLong == jda.selfUser.idLong }

            val count = Bukkit.getOnlinePlayers().size
            val basePart = "There ${if (count == 1) "is `1` Brady'er" else "are `$count` Brady'ers"} online"
            val timestamp = System.currentTimeMillis() / 1000
            val billboardContent = "$basePart\n$generateInfoBoard\n-# Updated <t:$timestamp:R>"

            if (pinnedBillboard == null) {
                pinnedBillboard = channel.sendMessage(billboardContent).setSuppressedNotifications(true).await()
                pinnedBillboard.pin().await()
                return@forEach
            }

            pinnedBillboard.edit(billboardContent).await()
        }
    }
}
