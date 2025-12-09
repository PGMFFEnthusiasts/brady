package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import net.kyori.adventure.text.Component
import net.minecraft.server.v1_8_R3.DamageSource
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.util.Vector
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.event.MatchFinishEvent
import tc.oc.pgm.api.party.Competitor
import tc.oc.pgm.api.party.VictoryCondition
import tc.oc.pgm.damagehistory.DamageHistoryMatchModule
import tc.oc.pgm.score.ScoreCause
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent
import tc.oc.pgm.timelimit.TimeLimit
import tc.oc.pgm.timelimit.TimeLimitMatchModule

class Pause : Listener, KoinComponent {
    private val tools: Tools by inject<Tools>()

    private data class PausedState(
        val match: Match,
        val restoredTime: TimeLimit?,
        val victoryConditions: List<VictoryCondition>,
        val capturedScores: Map<Competitor, Double>?,

        // so we saying bad words in the codebase now huh
        val pauseJob: Job,
    )

    private var pauseState: PausedState? = null

    private val pauseSound = uhOh
    private val unpauseSound = soundbox()
        .add(Sound.NOTE_PIANO, 1.25f)
        .add(Sound.ORB_PICKUP, 1.25f)
        .add(2, Sound.NOTE_PIANO, 1.4f)
        .add(Sound.ORB_PICKUP, 1.4f)

    init {
        command("pause", permission = "brady.pause") {
            executor {
                val m = match()
                if (!m.isRunning) err("Match is not running")

                if (pauseState != null) {
                    unpause()
                    unpauseSound.broadcast(m.world)
                    m.sendActionBar("&d&l။ UNPAUSED".cc())
                    m.sendMessage(Component.empty())
                    m.sendMessage("&d&l။ ".cc() + sender.component() + "&d has unpaused the game.".cc())
                    m.sendMessage(Component.empty())
                    return@executor
                }

                val timeLimit = getRestoredTimeAndCancel(m)
                val victoryConditions = m.victoryConditions.toList()

                victoryConditions.forEach { m.removeVictoryCondition(it) }
                val scores = scoreCapture(m)

                val job = tools.launch {
                    val pausedText = "‣ PAUSED"
                    var tick = 0

                    while (true) {
                        val sb = StringBuilder(pausedText)
                        val even = (tick / pausedText.length) % 2 == 0
                        sb.insert(tick % pausedText.length, if (even) "&d&l" else "&5&l")
                        sb.insert(0, if (even) "&5&l" else "&d&l")
                        m.sendActionBar(sb.toString().cc())

                        delay(2.ticks)
                        if (pauseState == null) break
                        ++tick
                    }
                }

                pauseState = PausedState(m, timeLimit, victoryConditions, scores, job)

                // this should be a pretty obvious sign
                // was bouta put a title on but too much
                m.players
                    .filter { it.isParticipating }
                    .forEach { it.bukkit.velocity = Vector() }

                pauseSound.broadcast(m.world)
                m.sendMessage(Component.empty())
                m.sendMessage("&d&l‣ &f".cc() + sender.component() + "&d has paused the game.".cc())
                m.sendMessage(Component.empty())
            }
        }

        tools.registerEvents(this)
    }

    private fun getRestoredTimeAndCancel(match: Match): TimeLimit? {
        val tl = match.getModule(TimeLimitMatchModule::class.java) ?: return null
        val oldTl = tl.timeLimit ?: return null
        val remainingTime = tl.finalRemaining ?: return null
        tl.cancel()
        return TimeLimit(oldTl, remainingTime)
    }

    private fun restoreTime(match: Match, timeLimit: TimeLimit?) {
        val tl = match.getModule(TimeLimitMatchModule::class.java) ?: return
        tl.timeLimit = timeLimit
        tl.start()
    }

    private fun scoreCapture(match: Match): Map<Competitor, Double>? {
        val sm = match.getModule(ScoreMatchModule::class.java) ?: return null
        return sm.scores.toMap()
    }

    private fun unpause() {
        val state = pauseState ?: return
        pauseState = null

        restoreTime(state.match, state.restoredTime)
        state.pauseJob.cancel()

        // todo: find some way to decrement deaths?
        val dt = state.match.getModule(DamageHistoryMatchModule::class.java)
        val sm = state.match.getModule(ScoreMatchModule::class.java)

        state.match.players
            .filter { it.isParticipating }
            .forEach {
                dt?.onPlayerDespawn(ParticipantDespawnEvent(it, it.bukkit.location))
                (it.bukkit as CraftPlayer).handle.damageEntity(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE)
            }

        state.capturedScores?.forEach { (k, v) -> sm?.setScore(k, v, ScoreCause.FLAG_CAPTURE) }
        state.victoryConditions.forEach { state.match.addVictoryCondition(it) }

        state.match.calculateVictory()
    }

    @EventHandler
    private fun onFinish(event: MatchFinishEvent) {
        unpause()
    }
}
