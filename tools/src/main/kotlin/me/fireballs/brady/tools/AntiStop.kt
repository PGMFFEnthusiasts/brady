package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.send
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class AntiStop : Listener, KoinComponent {
    private val tools by inject<Tools>()

    init {
        tools.registerEvents(this)
    }

    private val confirmingOperators = mutableSetOf<UUID>()
    private val msg = "&c&lYou are requesting to shutdown the server, do it again to confirm.".cc()

    @EventHandler
    private fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!event.message.startsWith("/stop", true)) return
        if (confirmingOperators.contains(event.player.uniqueId)) return

        tools.launch {
            delay(200.ticks)
            confirmingOperators.remove(event.player.uniqueId)
        }

        event.player.send(msg)
        event.isCancelled = true
    }
}
