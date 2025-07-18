package me.fireballs.brady.core.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class BradyShareEvent(
    val prefix: String,
    val link: String,
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
