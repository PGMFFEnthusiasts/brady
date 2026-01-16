package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import com.google.gson.Gson
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import me.fireballs.brady.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.event.MatchLoadEvent
import tc.oc.pgm.cycle.CycleMatchModule
import tc.oc.pgm.join.JoinMatchModule
import tc.oc.pgm.start.StartMatchModule
import tc.oc.pgm.teams.TeamMatchModule
import tc.oc.pgm.timelimit.TimeLimit
import tc.oc.pgm.timelimit.TimeLimitMatchModule
import tc.oc.pgm.util.StringUtils
import java.time.Duration
import java.util.UUID
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

// <-- not shared via deps; copy-paste changes
class Person(
    var name: String,
    val uuid: String,
)

class TournamentStateTeam(
    val name: String,
    val players: ArrayList<Person>,
)

class TournamentState(
    var teams: ArrayList<TournamentStateTeam>,
    var map: String?,
    var timeLimit: Int,
    var time: Long,
)

class TournamentMatch(
    var teams: ArrayList<TournamentStateTeam>,
    val server: String,
    val map: String,
    val timeLimit: Int,
)

class PeopleList(
    val players: ArrayList<Person>,
)
// <--

private fun b(c: Component) {
    PGM.get().matchManager.currentMatch()?.sendMessage("&c⚔ ".cc() + c)
}

class Tournaments : Listener, KoinComponent {
    private val plugin by inject<Tools>()
    private val pgm by inject<PGM>()
    private val mapOrder by inject<MapOrder>()

    private fun allMaps() = pgm.mapLibrary.maps.asSequence()
        .sortedBy { it.name }
        .toList()

    private val serverId = System.getenv("BRADY_SERVER") ?: "unknown"

    private val natsClient = runCatching {
        Nats.connect(System.getenv("BRADY_NATS") ?: Options.DEFAULT_URL)
    }.getOrNull()

    private var matchLoadDeferred: CompletableDeferred<Match>? = null
    private val gson = Gson()

    private var currentState: TournamentState? = null
    private var currentPlayers = mutableListOf<Person>()

    private fun CommandExecution.requireState() =
        currentState ?: err("State needs to be synchronized from proxy")

    private val usernameRegex = "[A-Za-z0-9_]{3,16}".toRegex()

    private suspend fun findPlayer(query: String): Person? {
        val fromCache = currentPlayers.find {
            it.name.equals(query, true) || it.uuid == query || it.uuid.replace("-", "") == query
        }

        if (fromCache != null) return fromCache

        if (query.length > 16) {
            val uniqueId = runCatching { UUID.fromString(query) }.getOrNull() ?: return null
            val username: String? = LuckPermsProvider.get().userManager.lookupUsername(uniqueId).await()
            if (username != null) return Person(username, uniqueId.toString())
        }

        if (query.length < 3 || !query.matches(usernameRegex)) return null

        val uniqueId = LuckPermsProvider.get().userManager.lookupUniqueId(query).await() ?: return null
        val normalizedName = LuckPermsProvider.get().userManager.lookupUsername(uniqueId).await() ?: return null

        return Person(normalizedName, uniqueId.toString())
    }

    private fun pushBack(state: TournamentState) {
        state.time = System.currentTimeMillis()
        plugin.launch(Dispatchers.IO) {
            natsClient?.publish("tournament.state", gson.toJson(state).toByteArray())
        }
    }

    private fun broadcast(message: Component) {
        plugin.launch(Dispatchers.IO) {
            natsClient?.publish("tournament.broadcast", ("&c⚔ ".cc() + message).coloredText().toByteArray())
        }
    }

    init {
        command("tournament", aliases = arrayOf("tour"), permission = "brady.tournament") {
            subcommand("show", " -&f shows the current state of the tournament") {
                executor {
                    val state = requireState()
                    sender.send("&7Teams:".cc())
                    for (team in state.teams) {
                        sender.send(
                            "&c${team.name}&7 (${team.players.size}): ".cc() +
                                    Component
                                        .join(JoinConfiguration.commas(true), team.players.map { it.name.c() })
                                        .color(NamedTextColor.GRAY)
                        )
                    }
                    sender.send("&7Map: &c${state.map ?: "Not set!"}".cc())
                    sender.send("&7Timelimit: &c${if (state.timeLimit <= 0) "None" else state.timeLimit.toString()}".cc())

                    okay.play(sender)
                }
            }

            subcommand("clear", " -&f reset all tournament state", aliases = arrayOf("reset")) {
                executor {
                    uhOh.play(sender)

                    pushBack(TournamentState(ArrayList(), null, 0, 0))
                    broadcast(sender.component().forWhom() + "&f reset the tournament state.")
                }
            }

            subcommand("create", "<&cteamName&7> -&f creates a team with the given name") {
                executor {
                    val state = requireState()
                    val name = capture("Provide a team name") { subArgs[0] }

                    val oops = state.teams.any { it.name.equals(name, true) }
                    if (oops) err("A team with this name already exists!")

                    state.teams.add(TournamentStateTeam(name, ArrayList()))

                    okay.play(sender)

                    pushBack(state)
                    broadcast(sender.component().forWhom() + "&f created the team &c$name&f.")
                }
            }

            subcommand("delete", "<&cteamName&7> -&f deletes an existing team") {
                executor {
                    val state = requireState()
                    val name = capture("Provide a team name") { subArgs[0] }

                    val resolved = state.teams.find { it.name.equals(name, true) }
                        ?: err("Team does not exist!")

                    state.teams.remove(resolved)

                    okay.play(sender)

                    pushBack(state)
                    broadcast(sender.component().forWhom() + "&f deleted the team &c$name&f.")
                }

                tabCompleter = {
                    when (subArgs.size) {
                        0, 1 -> currentState?.teams?.map { it.name } ?: listOf()
                        else -> listOf()
                    }
                }
            }

            subcommand("add", "<&cteamName&7> <&cplayerNameOrUUID&7> ... -&f adds a player to a team") {
                executor {
                    val state = requireState()
                    val teamName = capture("Provide a team name") { subArgs[0] }
                    val resolvedTeam = state.teams.find { it.name.equals(teamName, true) }
                        ?: err("Team does not exist!")

                    val playerArgs = subArgs.drop(1)
                    if (playerArgs.isEmpty()) err("Provide player(s) to add!")

                    val addedPlayers = mutableListOf<String>()
                    for (player in playerArgs) {
                        val resolvedPlayer = findPlayer(player)
                        if (resolvedPlayer == null) {
                            sender.send("&7Player &c\"$player\" &7not found!".cc())
                            continue
                        }

                        if (resolvedTeam.players.any { it.uuid == resolvedPlayer.uuid }) {
                            sender.send("&7Player &c${resolvedPlayer.name} &7was already in the team, skipping...".cc())
                            continue
                        }

                        resolvedTeam.players.add(resolvedPlayer)
                        addedPlayers.add(resolvedPlayer.name)
                    }

                    if (addedPlayers.isEmpty()) err("No players were able to be added")
                    okay.play(sender)

                    pushBack(state)
                    addedPlayers.forEach {
                        broadcast(
                            sender.component().forWhom() + "&f added &c${it}&f to &c${resolvedTeam.name}&f."
                        )
                    }
                }

                tabCompleter {
                    when (subArgs.size) {
                        0, 1 -> currentState?.teams?.map { it.name } ?: listOf()
                        2 -> currentPlayers.map { it.name }
                        else -> listOf()
                    }
                }
            }

            subcommand("remove", "<&cplayerNameOrUUID&7> -&f removes a player from a team") {
                executor {
                    val state = requireState()
                    val player = capture("Provide a player name or UUID") { subArgs[0] }

                    val resolvedTeam = state.teams.find {
                        it.players.any { p ->
                            p.name.equals(player, true) || p.uuid == player
                                    || p.uuid.replace("-", "") == player
                        }
                    } ?: err("Team does not exist!")

                    val resolvedPlayer = resolvedTeam.players.find {
                        it.name.equals(player, true) || it.uuid == player
                                || it.uuid.replace("-", "") == player
                    } ?: err("The player was not in the team anyway!")

                    resolvedTeam.players.remove(resolvedPlayer)

                    okay.play(sender)

                    pushBack(state)
                    broadcast(
                        sender.component()
                            .forWhom() + "&f removed &c${resolvedPlayer.name}&f from &c${resolvedTeam.name}&f."
                    )
                }

                tabCompleter {
                    when (subArgs.size) {
                        0, 1 -> currentState?.teams?.map { it.name } ?: listOf()
                        2 -> currentState?.teams
                            ?.firstOrNull { it.name.equals(subArgs[0], true) }
                            ?.players?.map { it.name } ?: listOf()

                        else -> listOf()
                    }
                }
            }

            subcommand("map", "<&cmapName&7> -&f sets the map") {
                executor {
                    val state = requireState()
                    val query = StringUtils.suggestionToText(subArgs.joinToString(" "))
                    val map = StringUtils.bestFuzzyMatch(query, allMaps()) { it.name }
                    if (map == null) err("Map not found!")

                    state.map = map.normalizedName

                    okay.play(sender)

                    pushBack(state)
                    broadcast(
                        sender.component()
                            .forWhom() + "&f set the map to &c${map.normalizedName}&f."
                    )
                }

                tabCompleter {
                    StringUtils.complete(
                        StringUtils.textToSuggestion(subArgs.joinToString(" ")),
                        allMaps().map { StringUtils.textToSuggestion(it.name) }
                    )
                }
            }

            subcommand(
                "timelimit",
                "<&ctimeSeconds&7> -&f sets the timelimit (<=0 for none)",
                aliases = arrayOf("tl")
            ) {
                executor {
                    val state = requireState()
                    val time = capture("Provide a valid time in seconds") { subArgs[0].toInt() }
                    state.timeLimit = time

                    okay.play(sender)

                    pushBack(state)
                    broadcast(sender.component().forWhom() + "&f set the timelimit to &c${time}s&f.")
                }
            }

            subcommand("match", "<&cserver&7> <&cteamA&7> <&cteamB&7> -&f execute upon the creation of the match") {
                executor {
                    val state = requireState()
                    val server = capture("Provide a server") { subArgs[0] }
                    val teamA = capture("Provide a teamA") { subArgs[1] }
                    val teamB = capture("Provide a teamB") { subArgs[2] }

                    val teams = state.teams.filter {
                        it.name.equals(teamA, true) || it.name.equals(teamB, true)
                    }

                    if (teams.size != 2) err("Could not locate both teams")

                    val map = state.map ?: err("Map not set!")

                    val matchState = TournamentMatch(
                        ArrayList(teams),
                        server,
                        map,
                        state.timeLimit,
                    )

                    okay.play(sender)

                    plugin.launch(Dispatchers.IO) {
                        natsClient?.publish("tournament.match", gson.toJson(matchState).toByteArray())
                    }

                    broadcast(
                        sender.component()
                            .forWhom() + "&f matched &c${teams[0].name}&f vs &c${teams[1].name}&f on&c ${server}&f."
                    )
                }

                tabCompleter = {
                    when (subArgs.size) {
                        0, 1 -> listOf("1", "2", "3", "4")
                        2 -> currentState?.teams?.map { it.name } ?: listOf()
                        3 -> currentState?.teams?.filter { !it.name.equals(subArgs[1], true) }?.map { it.name }
                            ?: listOf()

                        else -> listOf()
                    }
                }
            }

            executor {
                subcommands.forEach {
                    sender.send(
                        ("&7/tour &c" + it.name + " &7" + it.description).cc().fill("/tour ${it.name} ")
                    )
                }
            }
        }

        plugin.registerEvents(this)

        plugin.launch(Dispatchers.IO) {
            val matchSub = natsClient?.subscribe("tournament.match") ?: return@launch
            val pushSub = natsClient.subscribe("tournament.state")
            val requestSub = natsClient.subscribe("tournament.request")
            val playersSub = natsClient.subscribe("tournament.players")

            natsClient.flush(Duration.ofSeconds(5))

            plugin.launch(Dispatchers.IO) {
                while (true) {
                    val msg = matchSub.nextMessage(0).data.toString(Charsets.UTF_8)
                    val state = gson.fromJson(msg, TournamentMatch::class.java)
                    plugin.launch { handleMatch(state) }
                }
            }

            plugin.launch(Dispatchers.IO) {
                while (true) {
                    val msg = pushSub.nextMessage(0).data.toString(Charsets.UTF_8)
                    val state = gson.fromJson(msg, TournamentState::class.java)
                    val oldState = currentState
                    if (oldState != null) {
                        if (oldState.time <= state.time) currentState = state
                    } else currentState = state
                }
            }

            plugin.launch(Dispatchers.IO) {
                while (true) {
                    requestSub.nextMessage(0).data.toString(Charsets.UTF_8)
                    val currentState = currentState ?: continue
                    plugin.launch(Dispatchers.IO) {
                        natsClient.publish("tournament.state", gson.toJson(currentState).toByteArray())
                    }
                }
            }

            plugin.launch(Dispatchers.IO) {
                while (true) {
                    val msg = playersSub.nextMessage(0).data.toString(Charsets.UTF_8)
                    val state = gson.fromJson(msg, PeopleList::class.java)
                    currentPlayers = state.players
                }
            }

            natsClient.publish("tournament.request", serverId.toByteArray())
        }
    }

    private suspend fun handleMatch(data: TournamentMatch) {
        if (data.server != serverId) return
        b("&cReceived match request".cc())

        val targetMap = pgm.mapLibrary.getMap(data.map)
        if (targetMap != null) {
            b("&cCycling to ${targetMap.name}".cc())
            mapOrder.nextMap = targetMap

            matchLoadDeferred = CompletableDeferred()
            pgm.matchManager.currentMatch()?.needModule(CycleMatchModule::class.java)?.cycleNow()

            val newMatch = withTimeoutOrNull(10.seconds) { matchLoadDeferred?.await() }
            if (newMatch == null) {
                b("&cFailed to load map".cc())
                return
            }

            delay(20.ticks)
        } else {
            b("&cMap not found!".cc())
        }

        val match = pgm.matchManager.currentMatch() ?: return
        val teamModule = match.getModule(TeamMatchModule::class.java)
        val joinModule = match.getModule(JoinMatchModule::class.java)

        if (teamModule == null || joinModule == null) {
            b("&cMatch doesn't support teams".cc())
            return
        }

        val teamOne = teamModule.teams.find { it.id == "team-one" }
        val teamTwo = teamModule.teams.find { it.id == "team-two" }

        if (teamOne == null || teamTwo == null) {
            b("&cTeams not found".cc())
            return
        }

        if (data.timeLimit > 0) {
            val duration = Duration.ofSeconds(data.timeLimit.toLong())
            val timeLimitModule = match.getModule(TimeLimitMatchModule::class.java)
            if (timeLimitModule != null) {
                timeLimitModule.timeLimit = TimeLimit(null, duration, null, null, null, null, true)
                timeLimitModule.start()
                b("&cTime limit set!".cc())
            }
        }

        delay(4.seconds)

        val teamOneSize = data.teams.first().players.size
        val teamTwoSize = data.teams.last().players.size
        val teamSizes = max(teamOneSize, teamTwoSize).coerceAtLeast(1)

        teamOne.setMaxSize(teamSizes, 0)
        teamTwo.setMaxSize(teamSizes, 0)

        teamOne.setName(data.teams.first().name)
        teamTwo.setName(data.teams.last().name)

        for ((teamData, team) in data.teams.zip(arrayOf(teamOne, teamTwo))) {
            for (playerData in teamData.players) {
                val uuid = runCatching { UUID.fromString(playerData.uuid) }.getOrNull() ?: continue
                val player = Bukkit.getPlayer(uuid) ?: continue
                val matchPlayer = match.getPlayer(player) ?: continue
                joinModule.forceJoin(matchPlayer, team)
            }
        }

        val startModule = match.getModule(StartMatchModule::class.java)
        if (startModule != null) {
            startModule.isAutoStart = false
            match.countdown.cancelAll()
        }

        b("&cTeams assigned!".cc())
        b(teamOne.name + "&7 vs ".cc() + teamTwo.name)

        // we big serious now. no more fun.
        attemptToSetFlag("wifeEnabled", "false")
    }

    @EventHandler
    private fun onMatchLoad(event: MatchLoadEvent) {
        matchLoadDeferred?.complete(event.match)
        matchLoadDeferred = null
    }
}
