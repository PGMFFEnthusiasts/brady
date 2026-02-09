package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.event.MatchFinishEvent
import tc.oc.pgm.api.party.Competitor
import tc.oc.pgm.api.party.VictoryCondition
import tc.oc.pgm.damagehistory.DamageHistoryMatchModule
import tc.oc.pgm.events.PlayerPartyChangeEvent
import tc.oc.pgm.score.ScoreCause
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.spawns.SpawnMatchModule
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent
import tc.oc.pgm.timelimit.TimeLimit
import tc.oc.pgm.timelimit.TimeLimitMatchModule
import tc.oc.pgm.util.bukkit.Sounds
import kotlin.collections.component1
import kotlin.collections.component2

private fun stripe(s: String, tick: Int): String {
    val sb = StringBuilder(s)
    val even = (tick / s.length) % 2 == 0
    sb.insert(tick % s.length, if (even) "&d&l" else "&5&l")
    sb.insert(0, if (even) "&5&l" else "&d&l")
    return sb.toString()
}

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
                        m.sendActionBar(stripe(pausedText, tick).cc())
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
                    .forEach { it.isFrozen = true }

                pauseSound.broadcast(m.world)
                m.sendMessage(Component.empty())
                m.sendMessage("&d&l‣ &f".cc() + sender.component() + "&d has paused the game.".cc())
                m.sendMessage(Component.empty())
            }
        }

        command("reset", permission = "brady.pause") {
            executor {
                val m = match()
                if (!m.isRunning) err("Match is not running")
                if (pauseState != null) err("The match is paused, unpause it first")

                m.sendMessage(Component.empty())
                m.sendMessage("&d&l။ ".cc() + sender.component() + "&d has reset the game.".cc())
                m.sendMessage(Component.empty())
                resetAllPlayers(m)
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

    private val pauseTicks = 60
    private suspend fun unpause(instant: Boolean = false) {
        val state = pauseState ?: return
        pauseState = null

        state.pauseJob.cancel()

        if (!instant) {
            repeat(pauseTicks) {
                val tick = pauseTicks - it
                if (pauseState != null) return
                delay(1.ticks)
                state.match.sendActionBar(stripe("။ UNPAUSING ${tick / 20}.${tick % 20}s", it).cc())
                state.match.playSound(Sounds.DEATH_OWN)
            }
        }

        restoreTime(state.match, state.restoredTime)
        resetAllPlayers(state.match)

        val sm = state.match.getModule(ScoreMatchModule::class.java)
        state.capturedScores?.forEach { (k, v) -> sm?.setScore(k, v, ScoreCause.FLAG_CAPTURE) }
        state.victoryConditions.forEach { state.match.addVictoryCondition(it) }

        state.match.calculateVictory()
    }

    private fun resetAllPlayers(match: Match) {
        val dt = match.getModule(DamageHistoryMatchModule::class.java)
        val spawnModule = match.moduleRequire(SpawnMatchModule::class.java)

        match.players
            .filter { it.isParticipating }
            .forEach {
                it.isFrozen = false
                val spawn = spawnModule.chooseSpawn(it)
                val spawnLocation = spawn?.getSpawn(it) ?: return@forEach
                it.reset()
                it.bukkit.teleport(spawnLocation)
                spawn.applyKit(it)
                dt?.onPlayerDespawn(ParticipantDespawnEvent(it, it.bukkit.location))
            }
    }

    @EventHandler
    private suspend fun onFinish(event: MatchFinishEvent) {
        unpause(true)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onMatchJoin(event: PlayerPartyChangeEvent) {
        if (pauseState == null) return
        val party = event.newParty ?: return
        if (party.isObserving) return
        event.player.isFrozen = true
    }
}
