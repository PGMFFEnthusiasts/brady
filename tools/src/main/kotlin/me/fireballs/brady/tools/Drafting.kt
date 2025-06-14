package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.fireballs.brady.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchPhase
import tc.oc.pgm.api.match.event.MatchLoadEvent
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.cycle.CycleMatchModule
import tc.oc.pgm.join.JoinMatchModule
import tc.oc.pgm.join.JoinRequest
import tc.oc.pgm.start.StartMatchModule
import tc.oc.pgm.teams.Team
import tc.oc.pgm.teams.TeamMatchModule
import tc.oc.pgm.timelimit.TimeLimitMatchModule
import tc.oc.pgm.variables.VariablesMatchModule
import tc.oc.pgm.variables.types.DummyVariable
import tc.oc.pgm.variables.types.TeamVariableAdapter
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val draftBeginSound = soundbox()
    .add(Sound.NOTE_PIANO, 1.25f)
    .add(Sound.ORB_PICKUP, 1.25f)
    .add(2, Sound.NOTE_PIANO, 1.4f)
    .add(Sound.ORB_PICKUP, 1.4f)

private val draftEndSound = soundbox()
    .add(Sound.ENDERDRAGON_GROWL)

private val tickSound = soundbox()
    .add(Sound.NOTE_STICKS)

class Drafting : Listener, KoinComponent {
    private val plugin by inject<Tools>()
    private val mapOrder by inject<MapOrder>()

    private var draftActive = false
    private var draftJob: Job? = null
    private var draftEnrolledPlayers = mutableSetOf<Player>()
    private var draftEnrolledUnpickedPlayers = mutableSetOf<Player>()
    private var draftMatch: Match? = null
    private var draftCoinFlipMatchHappening = false

    init {
        plugin.registerEvents(this)
        command("draft", "begin a draft", "brady.draft") {
            executor {
                val player = player()
                if (draftActive) err(
                    "A draft has already begun. To cancel it, use ".cc() + "&9&n/draft cancel".cc()
                        .fill("/draft cancel")
                )
                val match = match()
                if (match.phase != MatchPhase.IDLE && match.phase != MatchPhase.STARTING) err("Match must be idle or starting")
                if (match.phase == MatchPhase.STARTING) match.countdown.cancelAll()

                val joinMatchModule = match.needModule(JoinMatchModule::class.java)
                val teamModule = match.needModule(TeamMatchModule::class.java)
                if (teamModule.teams.size != 2) err("This match isn't #2teams")
                teamModule.teams.forEach { team ->
                    team.players.forEach { joinMatchModule.leave(it, JoinRequest.force()) }
                    team.setMaxSize(0, 0)
                }

                clearDraft()
                draftActive = true
                draftMatch = match

                match.sendMessage(("&6⚠ ".cc() + player.component() + " has started a draft!".c()))
                draftBeginSound.broadcast(match.world)

                draftJob = plugin.launch(plugin.asyncDispatcher) {
                    draftingSequenceWrapper(match)
                }
            }

            subcommand("cancel") {
                executor {
                    if (!draftActive) err("There is no active draft to cancel")
                    clearDraft(true)

                    val player = player()
                    val match = match()

                    match.sendMessage(("&6⚠ ".cc() + player.component() + " cancelled the draft!".cc()))
                    draftEndSound.broadcast(match.world)
                }
            }
        }

        command("enlist", "enlist in the draft", aliases = arrayOf("enroll", "participate")) {
            executor {
                val player = player()

                if (!draftActive) err("There is no active draft to enlist in")
                if (draftEnrolledPlayers.contains(player)) err(
                    "You're already enlisted! Use ".cc() + "&9&n/unenlist".cc().command("/unenlist") + " to unenlist."
                ) else {
                    playerEnrolled(player)
                    okay.play(sender)
                }
            }
        }

        command("unenlist", "unenlist in the draft", aliases = arrayOf("unenroll")) {
            executor {
                val player = player()

                if (!draftActive) err("There is no active draft to unenlist in")
                if (draftEnrolledPlayers.contains(player)) {
                    playerUnenrolled(player)
                    okay.play(sender)
                } else err("You aren't enlisted in the draft currently")
            }
        }
    }

    private fun clearDraft(reset: Boolean = false) {
        draftActive = false
        draftEnrolledPlayers.clear()
        draftEnrolledUnpickedPlayers.clear()
        draftJob?.cancel()
        val oldMatch = draftMatch
        draftMatch = null
        draftCoinFlipMatchHappening = false

        if (reset && oldMatch != null) plugin.launch {
            delay(20.ticks)
            mapOrder.nextMap = oldMatch.map
            oldMatch.needModule(CycleMatchModule::class.java).cycleNow()
        }
    }

    private suspend fun draftingSequenceWrapper(
        match: Match,
    ) {
        try {
            draftingSequence(match)
        } catch (e: Exception) {
            // oh no
            clearDraft(true)
            return
        }

        clearDraft()
    }

    private val preDraftTime = 30
    private suspend fun draftingSequence(
        match: Match,
    ) {
        repeat(preDraftTime) { t ->
            if (t % 5 == 0 || preDraftTime - t < 5) {
                val unenlisted = match.world.players
                    .filter { !draftEnrolledPlayers.contains(it) }
                unenlisted.forEach { it.send() }
                match.sendMessage("&6⚄ &fTeam captains will be chosen in &b${preDraftTime - t}s&f.".cc())
                unenlisted.forEach {
                    it.send(
                        "&6⚄ &fYou're not enlisted! Use ".cc() + "&b&n/participate".cc()
                            .command("/participate") + " to participate in the draft."
                    )
                    it.send()
                }
                tickSound.broadcast(match.world)
            }

            delay(1000L)
        }

        if (draftEnrolledPlayers.size < 2) {
            match.sendMessage("&6⚄ &fFailed to find enough players participating in the draft!".cc())
            clearDraft(true)
            return
        }

        match.sendMessage("&6⚄ &fPicking team captains...".cc())

        suspend fun rollCaptain(team: Team): Player {
            repeat(30) { t ->
                val pitch = lerp(t / 30f, 2f, 0.5f)
                soundbox()
                    .add(Sound.NOTE_STICKS, pitch)
                    .broadcast(match.world)

                val chosen = draftEnrolledUnpickedPlayers.random()
                match.forEachAudience {
                    it.showTitle(
                        Title.title(
                            "&7» ".cc() + chosen.component() + " &7«",
                            "&7picking the ".cc() + team.name + " &7captain",
                            titleTimes(0.seconds, 5.seconds, 2.seconds)
                        )
                    )
                }
                delay(2.ticks)
            }

            val chosen = draftEnrolledUnpickedPlayers.random()
            draftEnrolledUnpickedPlayers.remove(chosen)
            soundbox()
                .add(Sound.EXPLODE, 1.5f)
                .add(Sound.LEVEL_UP, 0.5f)
                .broadcast(match.world)

            team.setMaxSize(1, 0)
            match.needModule(JoinMatchModule::class.java)
                .forceJoin(match.getPlayer(chosen), team)

            match.showTitle(
                Title.title(
                    "&a» ".cc() + chosen.component() + " &a«",
                    "&7is the ".cc() + team.name + " &7captain",
                    titleTimes(0.seconds, 3.seconds, 1.seconds)
                )
            )

            delay((3 * 20).ticks)
            return chosen
        }

        val teamModule = match.needModule(TeamMatchModule::class.java)
        val teamList = teamModule.teams.toList()

        val startModule = match.needModule(StartMatchModule::class.java)
        startModule.setAutoStart(false)

        val capOne = rollCaptain(teamList.find { it.id == "team-one" }!!)
        val capTwo = rollCaptain(teamList.find { it.id == "team-two" }!!)

        val capOneMP = match.getPlayer(capOne)
        val capTwoMP = match.getPlayer(capTwo)

        match.sendMessage("&6⚄ &fStarting match coin flip...".cc())

        startModule.forceStartCountdown(5.seconds.toJavaDuration(), Duration.ZERO)
        draftCoinFlipMatchHappening = true

        match.getModule(TimeLimitMatchModule::class.java)?.timeLimit = null

        val variableModule = match.needModule(VariablesMatchModule::class.java)
        val variableMap = variableModule.variables.map { it.key to it.value }.toList().toMap()

        val stateVariable = variableMap["player_state"] as DummyVariable<*>
        val teamOneScore = variableMap["team_one_score"] as TeamVariableAdapter
        val teamTwoScore = variableMap["team_two_score"] as TeamVariableAdapter

        delay(5.seconds + 0.5.seconds)

        match.sendMessage(Component.empty())
        match.sendMessage("&6⚄ &fMatch coin flip".cc())
        match.sendMessage("&6⚄ &fTo win a match coin flip, a team captain should catch any ball.".cc())
        match.sendMessage(Component.empty())

        suspend fun checkDisqualification(
            variable: TeamVariableAdapter,
            opposingTeamVariable: TeamVariableAdapter,
            captain: MatchPlayer
        ) {
            if (variable.getValue(match).roundToInt() == 0) return
            variable.setValue(match, 0.0)
            opposingTeamVariable.setValue(match, 1.0)
            match.finish()
            match.sendMessage("&6⚄ &fShame to ".cc() + captain.name + "&f! You were &c&lNOT &fsupposed to score!!! &c&lYOU LOSE!")
            delay(3.seconds)
            throw IllegalStateException()
        }

        class ScoreTracker(
            val captain: MatchPlayer,
            val variable: TeamVariableAdapter,
            var previousScore: Int = 0,
        )

        val stateOne = ScoreTracker(capOneMP!!, teamOneScore)
        val stateTwo = ScoreTracker(capTwoMP!!, teamTwoScore)

        suspend fun checkWin(tracker: ScoreTracker) {
            val currentState = stateVariable.getValue(tracker.captain).roundToInt()
            if (tracker.previousScore == 2 && currentState == 1) {
                tracker.variable.setValue(match, 1.0)
                match.finish()
                match.sendMessage("&6⚄ &fThe player ".cc() + tracker.captain.name + " has won the coin toss!")
                delay(3.seconds)
                throw IllegalStateException()
            }
            tracker.previousScore = currentState
        }

        withContext(plugin.minecraftDispatcher) {
            while (match.phase == MatchPhase.RUNNING) {
                delay(1.ticks)

                if (!capOne.isOnline || !capTwo.isOnline) {
                    match.sendMessage("&6⚄ &fDraft has been cancelled due to a team captain quitting.".cc())
                    clearDraft(true)
                    return@withContext
                }

                checkDisqualification(teamOneScore, teamTwoScore, capOneMP)
                checkDisqualification(teamTwoScore, teamOneScore, capTwoMP)

                checkWin(stateOne)
                checkWin(stateTwo)
            }
        }
    }

    private fun playerEnrolled(player: Player) {
        if (draftEnrolledPlayers.add(player) && draftEnrolledUnpickedPlayers.add(player)) draftMatch?.sendMessage(
            "&6⚄ ".cc() + player.component() + " has enlisted in the match &7(${draftEnrolledPlayers.size})"
        )
    }

    private fun playerUnenrolled(player: Player) {
        if (draftEnrolledPlayers.remove(player)) draftMatch?.sendMessage(
            "&6⚄ ".cc() + player.component() + " has unenlisted in the match &7(${draftEnrolledPlayers.size})"
        )
        draftEnrolledUnpickedPlayers.remove(player)
    }

    @EventHandler
    private fun onLeave(event: PlayerQuitEvent) {
        playerUnenrolled(event.player)
    }

    @EventHandler
    private fun onLoad(event: MatchLoadEvent) {
        if (!draftActive) return
        Core.adventure.all().sendMessage("&6⚄ &fDraft has been cancelled due to cycle.".cc())
        clearDraft()
    }
}
