package me.fireballs.share.listener.pgm

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.fireballs.share.SharePlugin
import me.fireballs.share.manager.MatchIdManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.match.event.MatchFinishEvent
import tc.oc.pgm.api.match.event.MatchUnloadEvent
import tc.oc.pgm.scoreboard.SidebarMatchModule
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class ScoreboardListener(
    private val plugin: SharePlugin,
    private val matchIdManager: MatchIdManager
) : Listener {

    companion object {
        private const val CHECK_INTERVAL_TICKS = 10
    }

    private val updateTasks = ConcurrentHashMap<String, Job>()
    private val displayedMatchIds = ConcurrentHashMap<String, Int>()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMatchLoad(event: MatchAfterLoadEvent) {
        val pgmMatchId = event.match.id
        val match = event.match

        val sidebarModule = match.getModule(SidebarMatchModule::class.java)
        if (sidebarModule == null) {
            plugin.logger.log(Level.FINE, "SidebarMatchModule not available, cannot display match ID in scoreboard")
            return
        }

        updateTasks.remove(pgmMatchId)?.cancel()
        updateTasks[pgmMatchId] = plugin.launch {
            while (isActive) {
                if (match.isFinished || !match.isLoaded) break

                val matchId = matchIdManager.getPreallocatedMatchId(pgmMatchId)
                if (matchId > 0) {
                    displayedMatchIds[pgmMatchId] = matchId
                    sidebarModule.dynamicFooter = Component.text("#$matchId", NamedTextColor.GRAY)
                    break
                }

                delay(CHECK_INTERVAL_TICKS.ticks)
            }

            updateTasks.remove(pgmMatchId)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMatchFinish(event: MatchFinishEvent) {
        val pgmMatchId = event.match.id

        updateTasks.remove(pgmMatchId)?.cancel()

        val matchId = displayedMatchIds[pgmMatchId]
        if (matchId != null) {
            val sidebarModule = event.match.getModule(SidebarMatchModule::class.java)
            sidebarModule?.dynamicFooter = Component.text("#$matchId", NamedTextColor.GRAY)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMatchUnload(event: MatchUnloadEvent) {
        val pgmMatchId = event.match.id

        updateTasks.remove(pgmMatchId)?.cancel()
        displayedMatchIds.remove(pgmMatchId)

        val sidebarModule = event.match.getModule(SidebarMatchModule::class.java)
        sidebarModule?.dynamicFooter = null
    }
}
