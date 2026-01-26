package me.fireballs.brady.tools

import kotlinx.coroutines.future.await
import me.fireballs.brady.core.Retrieval
import me.fireballs.brady.core.boolGet
import me.fireballs.brady.core.boolGetCached
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.registerEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.event.NameDecorationChangeEvent
import tc.oc.pgm.events.PlayerParticipationStartEvent
import tc.oc.pgm.util.named.NameDecorationProvider
import java.util.*

private val banishedSet = mutableSetOf<UUID>()

class Benched : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val pgm by inject<PGM>()

    private class NDP(
        val upstream: NameDecorationProvider
    ) : NameDecorationProvider {
        private val banishedComponent = "&c∅".cc()

        override fun getPrefix(uuid: UUID?): String? {
            return upstream.getPrefix(uuid)
        }

        override fun getSuffix(uuid: UUID?): String? {
            return upstream.getSuffix(uuid)
        }

        override fun getPrefixComponent(uuid: UUID?): Component? {
            if (banishedSet.contains(uuid)) return banishedComponent
            return upstream.getPrefixComponent(uuid)
        }

        override fun getSuffixComponent(uuid: UUID?): Component? {
            return upstream.getSuffixComponent(uuid)
        }

        override fun getColor(uuid: UUID?): TextColor? {
            if (banishedSet.contains(uuid)) return NamedTextColor.GRAY
            return upstream.getColor(uuid)
        }
    }

    init {
        tools.registerEvents(this)

        val ndr = pgm.nameDecorationRegistry
        ndr.setProvider(NDP(ndr.provider))
    }

    private val benchedPerm = "benched"
    private val benchedReason = "&cYou've been benched! You may not join matches.".cc()

    private suspend fun Player.isBanished() =
        boolGet(benchedPerm, false, Retrieval.CACHE_THEN_FRESH).await()

    private fun Player.isBanishedCached() =
        boolGetCached(benchedPerm, false)

    @EventHandler(ignoreCancelled = true)
    private fun onMatchJoin(event: PlayerParticipationStartEvent) {
        val p = event.player.bukkit
        if (!p.isBanishedCached()) return
        event.cancel(benchedReason)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private suspend fun onJoin(event: PlayerJoinEvent) {
        if (event.player.isBanished()) {
            banishedSet.add(event.player.uniqueId)
            tools.server.pluginManager.callEvent(NameDecorationChangeEvent(event.player.uniqueId))
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onQuit(event: PlayerQuitEvent) {
        banishedSet.remove(event.player.uniqueId)
    }
}
