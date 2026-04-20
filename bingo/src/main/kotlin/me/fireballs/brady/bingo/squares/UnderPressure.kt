package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.log
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.event.FootballLifecycleAction
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.teams.TeamMatchModule

class UnderPressure(squareIndex: Int) : Square("Under Pressure", squareIndex) {
    private data class PressuredCatcher(
        val player: MatchPlayer,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private var pressuredCatcher: PressuredCatcher? = null

    @EventHandler
    private fun onCatch(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.CATCH) return
        val mp = event.catcher
        val teamModule = mp.match.getModule(TeamMatchModule::class.java) ?: return
        val otherTeams = teamModule.participatingTeams
            .filter { !it.players.contains(mp) }
        val pressurers = otherTeams.sumOf {
            it.players.count { enemy ->
                enemy.player?.bukkit?.location?.toVector()?.distanceSquared(mp.player?.bukkit?.location?.toVector())
                    ?.let { d -> d <= 3.5 * 3.5 } == true
            }
        }
        log("bingo", "there are $pressurers")
        if (pressurers < 2) return
        pressuredCatcher = PressuredCatcher(mp)
    }

    @EventHandler
    private fun onTD(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.TOUCHDOWN) return
        val pc = pressuredCatcher ?: return
        if (System.currentTimeMillis() - pc.timestamp > 5_000) return
        pressuredCatcher = null
        complete(pc.player.bukkit)
    }

    @EventHandler
    private fun onPick(event: BradyFootballLifecycleEvent) {
        if (event.action != FootballLifecycleAction.BALL_PICKUP) return
        pressuredCatcher = null
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        pressuredCatcher = null
    }

    override fun matchesDomainRestriction() = playerFloorOf(3)
}
