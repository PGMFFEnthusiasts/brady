package me.fireballs.share.manager

import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.Dispatchers
import me.fireballs.share.SharePlugin
import me.fireballs.share.storage.Database
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.match.event.MatchUnloadEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class MatchIdManager(
    private val plugin: SharePlugin,
    private val database: Database?
) : Listener {

    private val preallocatedMatchIds = ConcurrentHashMap<String, Int>()
    private val finalizedMatchIds = ConcurrentHashMap.newKeySet<String>()

    @EventHandler(priority = EventPriority.LOW)
    fun onMatchLoad(event: MatchAfterLoadEvent) {
        if (database == null) return

        val pgmMatchId = event.match.id

        plugin.launch(Dispatchers.IO) {
            try {
                val matchId = database.preallocateMatchId()
                if (matchId > 0) {
                    preallocatedMatchIds[pgmMatchId] = matchId
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Failed to preallocate match ID for match $pgmMatchId", e)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMatchUnload(event: MatchUnloadEvent) {
        val pgmMatchId = event.match.id
        val matchId = preallocatedMatchIds.remove(pgmMatchId)

        if (matchId != null && !finalizedMatchIds.contains(pgmMatchId)) {
            plugin.launch(Dispatchers.IO) {
                try {
                    database?.deletePlaceholderMatch(matchId)
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Failed to clean up placeholder match ID $matchId", e)
                }
            }
        }

        finalizedMatchIds.remove(pgmMatchId)
    }

    fun markFinalized(pgmMatchId: String) {
        finalizedMatchIds.add(pgmMatchId)
    }

    fun getPreallocatedMatchId(pgmMatchId: String): Int =
        preallocatedMatchIds.getOrDefault(pgmMatchId, -1)

    fun hasPreallocatedMatchId(pgmMatchId: String): Boolean =
        preallocatedMatchIds.containsKey(pgmMatchId)
}
