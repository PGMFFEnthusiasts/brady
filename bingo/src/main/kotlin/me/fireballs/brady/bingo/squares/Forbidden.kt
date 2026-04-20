package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import tc.oc.pgm.api.event.ChannelMessageEvent

class Forbidden(squareIndex: Int) : Square("###@###", squareIndex) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    private fun onAttemptedSwear(event: ChannelMessageEvent<*>) {
        if (!event.message.contains("mineplex", true)
            && !event.message.contains("stratus", true)
        ) return
        complete(event.sender.bukkit)
    }
}
