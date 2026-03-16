package me.fireballs.brady.tools

import me.fireballs.brady.core.cc
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.send
import me.fireballs.brady.core.url
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VersionIntercept : Listener, KoinComponent {
    private val tools by inject<Tools>()

    init {
        tools.registerEvents(this)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onVersionCommandInvoke(event: PlayerCommandPreprocessEvent) {
        if (!event.message.startsWith("/version", true)) return
        event.player.send(
            "&9&nRunning brady ${tools.description.version}".cc()
                .url("https://github.com/PGMFFEnthusiasts/brady/tree/${tools.description.version}")
        )
    }
}
