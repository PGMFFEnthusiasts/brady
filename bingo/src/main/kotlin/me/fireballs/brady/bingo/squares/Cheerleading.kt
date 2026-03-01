package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.log
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import tc.oc.pgm.api.event.ChannelMessageEvent
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.channels.GlobalChannel
import tc.oc.pgm.channels.TeamChannel

class Cheerleading(squareIndex: Int) : Square("Cheerleading", squareIndex) {
    private data class Goats(val goatSet: Set<String>, val goatStamp: Long = System.currentTimeMillis())
    private var currentGoats: Goats? = null

    @EventHandler
    private fun onTD(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.TOUCHDOWN_PASS) return
        val t = event.thrower?.nameLegacy?.lowercase() ?: return
        val c = event.catcher?.nameLegacy?.lowercase() ?: return
        log("bingo", "goats: $t $c")
        currentGoats = Goats(setOf(t, c))
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onChat(event: ChannelMessageEvent<*>) {
        if (event.channel !is TeamChannel && event.channel !is GlobalChannel) return
        val g = currentGoats ?: return
        if (System.currentTimeMillis() - g.goatStamp > 10_000L) {
            log("bingo", "goats but it expired so resetting ts")
            currentGoats = null
            return
        }
        for (name in g.goatSet) {
            if (!event.message.lowercase().contains(name)) continue
            complete(event.sender.bukkit)
        }
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        currentGoats = null
    }

    override fun matchesDomainRestriction() = playerFloorOf(2)
}
