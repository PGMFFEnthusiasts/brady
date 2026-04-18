package me.fireballs.brady.core.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class BradyClickMitigateEvent(
    val player: Player,
) : Event() {
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        @JvmStatic
        private val handlers: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }
}