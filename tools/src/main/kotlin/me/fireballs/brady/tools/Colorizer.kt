package me.fireballs.brady.tools

import me.fireballs.brady.core.cc
import me.fireballs.brady.core.plainText
import me.fireballs.brady.core.registerEvents
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.event.ChannelMessageEvent

class Colorizer : Listener, KoinComponent {
    private val tools by inject<Tools>()

    init {
        tools.registerEvents(this)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onChat(event: ChannelMessageEvent<*>) {
        if (!event.sender.bukkit.hasPermission("brady.color")) return
        val component = event.component
        if (component !is TextComponent || component.children().isNotEmpty() || component.clickEvent() != null) return
        event.component = component.plainText().cc()
    }
}
