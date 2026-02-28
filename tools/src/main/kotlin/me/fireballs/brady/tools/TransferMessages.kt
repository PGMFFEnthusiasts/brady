package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.common.collect.MapMaker
import io.valkey.JedisPubSub
import kotlinx.coroutines.Dispatchers
import me.fireballs.brady.core.*
import me.fireballs.brady.corepgm.component
import me.fireballs.brady.corepgm.currentMatch
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchManager
import java.util.*

class TransferMessages : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val matchManager by inject<MatchManager>()
    private val serverId = System.getenv("BRADY_SERVER") ?: "unknown"

    private data class TransferIntent(
        val otherServer: String,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        // transfers should be valid only for 5 seconds otherwise
        // something obviously went wrong and we should just ignore
        fun valid() = createdAt + 5000L >= System.currentTimeMillis()
    }

    // this is a memory leak and that's okay
    private val outgoingTransfers = MapMaker().makeMap<UUID, TransferIntent>()
    private val incomingTransfers = MapMaker().makeMap<UUID, TransferIntent>()

    init {
        tools.launch(Dispatchers.IO) { startSubscribing() }
        tools.registerEvents(this)
    }

    private fun startSubscribing() {
        val vk = newValkeyClient() ?: return
        vk.subscribe(object : JedisPubSub() {
            override fun onMessage(channel: String?, message: String) {
                val split = message.split("\t")
                if (split.size != 3) return
                val target = UUID.fromString(split[0])
                val from = split[1]
                val to = split[2]
                if (from == to) return
                if (from == serverId) outgoingTransfers[target] = TransferIntent(to)
                if (to == serverId) incomingTransfers[target] = TransferIntent(from)
            }
        }, "live-transfers")
    }

    private fun resolve(player: Player, map: MutableMap<UUID, TransferIntent>, direction: String): Boolean? {
        val transfer = map[player.uniqueId] ?: return null
        map.remove(player.uniqueId, transfer)
        if (!transfer.valid()) return null
        val match = matchManager.currentMatch() ?: return null
        val other = transfer.otherServer
        match.sendMessage(
            player.component() + "&e transferred $direction $other".cc().command("/server $other")
                .hover("&eClick to transfer to $other".cc())
        )
        return true
    }

    @EventHandler
    private fun onJoin(event: PlayerJoinEvent) {
        resolve(event.player, incomingTransfers, "from") ?: return
        event.joinMessage = null
    }

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        resolve(event.player, outgoingTransfers, "to") ?: return
        event.quitMessage = null
    }
}
