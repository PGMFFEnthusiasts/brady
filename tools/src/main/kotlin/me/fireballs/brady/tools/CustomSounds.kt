package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.log
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.corepgm.currentMatch
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.event.ActionNodeTriggerEvent
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.match.event.MatchStartEvent
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent
import tc.oc.pgm.flag.event.FlagCaptureEvent
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.teams.TeamMatchModule
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

// adventure is BUSTED and IDK WHY!
// ↪ actually, I know why but the masses don't want to hear it.
private fun soundOf(s: String): (players: Collection<Player>) -> Unit = { players ->
    log("sounds", s)
    for (player in players) {
        player.playSound(
            player.location.add(0.0, 1000.0, 0.0),
            "brady:$s",
            100000f,
            1f,
        )
    }
}

private val roundStartSound = soundOf("round.start")
private val roundWinSound = soundOf("round.win")
private val roundLossSound = soundOf("round.loss")

private val gameStartSound = soundOf("game.start")
private val gameWinSound = soundOf("game.win")
private val gameLossSound = soundOf("game.loss")

private val killSound = soundOf("player.kill")
private val deathSound = soundOf("player.death")

class CustomSounds : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val matchManager by inject<MatchManager>()

    init {
        tools.registerEvents(this)
    }

    private var inCage = false

    @EventHandler
    private fun onAction(event: ActionNodeTriggerEvent) {
        log("sounds-verbose", event.nodeId)
        val match = matchManager.currentMatch() ?: return
        if (event.nodeId == "reset-flag" && inCage) {
            inCage = false
            roundStartSound(match.world.players)
            return
        }
    }

    @EventHandler
    private fun onStart(event: MatchStartEvent) {
        inCage = false
        gameStartSound(event.match.players.map { it.bukkit })
    }

    @EventHandler
    private suspend fun onScore(event: FlagCaptureEvent) {
        delay(1.ticks) // hack
        val teams = event.match.getModule(TeamMatchModule::class.java) ?: return
        val targetTeam = teams.teams.find { it.players.contains(event.carrier) } ?: return
        val scores = event.match.getModule(ScoreMatchModule::class.java) ?: return
        if (!scores.hasScoreLimit()) return
        inCage = true
        log("sounds-verbose", "score: ${scores.getScore(targetTeam)} limit: ${scores.scoreLimit}")
        val gameWinning = scores.getScore(targetTeam).toInt() >= scores.scoreLimit
        log("sounds-verbose", "gameWinning: $gameWinning")
        val winnerAudience = targetTeam.players.map { it.bukkit }
        val loserAudience = teams.teams.filter { it != targetTeam }.flatMap { it.players }.map { it.bukkit }
        (if (gameWinning) gameWinSound else roundWinSound)(winnerAudience)
        (if (gameWinning) gameLossSound else roundLossSound)(loserAudience)
    }

    @EventHandler
    private fun onKillDeath(event: MatchPlayerDeathEvent) {
        event.killer?.player?.getOrNull()?.let { killSound(listOf(it.bukkit)) }
        deathSound(listOf(event.player.bukkit))
    }
}
