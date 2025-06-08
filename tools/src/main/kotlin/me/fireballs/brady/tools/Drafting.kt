package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchPhase
import tc.oc.pgm.cycle.CycleMatchModule
import tc.oc.pgm.join.JoinMatchModule
import tc.oc.pgm.join.JoinRequest
import tc.oc.pgm.teams.Team
import tc.oc.pgm.teams.TeamMatchModule
import kotlin.time.Duration.Companion.seconds

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

    init {
        plugin.registerEvents(this)
        command("draft", "begin a draft", "brady.draft") {
            executor = {
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
                executor = {
                    if (!draftActive) err("There is no active draft to cancel")
                    clearDraft(true)

                    val player = player()
                    val match = match()

                    match.sendMessage(("&6⚠ ".cc() + player.component() + " cancelled the draft!".cc()))
                    draftEndSound.broadcast(match.world)
                }
            }
        }

        command("enlist", "enlist in the draft", aliases = arrayOf("enroll")) {
            executor = {
                val player = player()

                if (!draftActive) err("There is no active draft to enlist in")
                if (draftEnrolledPlayers.contains(player)) err(
                    "You're already enrolled! Use ".cc() + "&9&n/unenlist".cc().command("/unenlist") + " to unenlist."
                ) else {
                    playerEnrolled(player)
                    okay.play(sender)
                }
            }
        }

        command("unenlist", "unenlist in the draft", aliases = arrayOf("unenroll")) {
            executor = {
                val player = player()

                if (!draftActive) err("There is no active draft to unenlist in")
                if (draftEnrolledPlayers.contains(player)) {
                    playerUnenrolled(player)
                    okay.play(sender)
                } else err("You aren't enrolled in the draft currently")
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

    private val preDraftTime = 10
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
                        "&6⚄ &fYou're not enlisted! Use ".cc() + "&b&n/enlist".cc()
                            .command("/enlist") + " to participate in the draft."
                    )
                    it.send()
                }
                tickSound.broadcast(match.world)
            }

            delay(1000L)
        }

        if (draftEnrolledPlayers.size < 2) {
            match.sendMessage("&6⚄ &fFailed to find enough players participating in the draft!".cc())
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
            match.needModule(TeamMatchModule::class.java).balanceTeams()

            match.forEachAudience {
                it.showTitle(
                    Title.title(
                        "&a» ".cc() + chosen.component() + " &a«",
                        "&7is the ".cc() + team.name + " &7captain",
                        titleTimes(0.seconds, 3.seconds, 1.seconds)
                    )
                )
            }

            delay((3 * 20).ticks)
            return chosen
        }

        val teamModule = match.needModule(TeamMatchModule::class.java)
        val teamList = teamModule.teams.toList()

        rollCaptain(teamList[0])
        rollCaptain(teamList[1])
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
//
//    @EventHandler
//    private fun onLeave(event: PlayerQuitEvent) {
//        playerUnenrolled(event.player)
//    }
//
//    @EventHandler
//    private fun onCycle(event: MatchAfterLoadEvent) {
//
//    }
//
//    @EventHandler
//    private fun onFlagPickup(event: FlagPickupEvent) {
//    }
//
//    @EventHandler
//    private fun onFlagCapture(event: FlagCaptureEvent) {
//        event.carrier.name
//    }
//
//    @EventHandler
//    private fun onFlagWhat(event: FlagStateChangeEvent) {
//
//    }
}