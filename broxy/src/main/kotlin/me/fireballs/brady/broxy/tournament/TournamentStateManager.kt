package me.fireballs.brady.broxy.tournament

import com.github.shynixn.mccoroutine.velocity.launch
import com.google.gson.Gson
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.Dispatchers
import me.fireballs.brady.broxy.Broxy
import me.fireballs.brady.broxy.utils.c
import me.fireballs.brady.broxy.utils.cc
import net.kyori.adventure.text.Component
import java.time.Duration
import java.util.UUID

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

class TournamentStateManager(
    private val plugin: Broxy,
) {
    private val natsClient = runCatching {
        Nats.connect(System.getenv("BRADY_NATS") ?: Options.DEFAULT_URL)
    }.getOrNull()

    private val truePersonLookaside = mutableMapOf<UUID, Person>()
    private val truePersonList = ArrayList<Person>()
    private val peopleList = PeopleList(truePersonList)

    private var trueState = TournamentState(ArrayList(), null, 0, 0)

    private val gson = Gson()

    private fun b(c: Component) {
        plugin.server.allPlayers
            .filter { it.hasPermission("brady.tournament") }
            .forEach { it.sendMessage(c) }
        plugin.server.consoleCommandSource.sendMessage(c)
    }

    init {
        plugin.pluginContainer.launch(Dispatchers.IO) {
            val broadcasts = natsClient?.subscribe("tournament.broadcast") ?: return@launch
            val dataRequests = natsClient.subscribe("tournament.request")
            val pushSub = natsClient.subscribe("tournament.state")
            val matchSub = natsClient.subscribe("tournament.match")
            natsClient.flush(Duration.ofSeconds(5L))

            plugin.pluginContainer.launch(Dispatchers.IO) {
                while (true) {
                    val msg = broadcasts.nextMessage(0).data.toString(Charsets.UTF_8)
                    b(msg.cc(false))
                }
            }

            plugin.pluginContainer.launch(Dispatchers.IO) {
                while (true) {
                    dataRequests.nextMessage(0).data.toString(Charsets.UTF_8)
                    plugin.pluginContainer.launch(Dispatchers.IO) {
                        natsClient.publish("tournament.players", gson.toJson(peopleList).toByteArray())
                        natsClient.publish("tournament.state", gson.toJson(trueState).toByteArray())
                    }
                }
            }

            plugin.pluginContainer.launch(Dispatchers.IO) {
                while (true) {
                    val msg = pushSub.nextMessage(0).data.toString(Charsets.UTF_8)
                    val state = gson.fromJson(msg, TournamentState::class.java)
                    val oldState = trueState
                    if (oldState.time <= state.time) trueState = state
                }
            }

            plugin.pluginContainer.launch(Dispatchers.IO) {
                while (true) {
                    val msg = matchSub.nextMessage(0).data.toString(Charsets.UTF_8)
                    val state = gson.fromJson(msg, TournamentMatch::class.java)

                    val targetServer = plugin.server.getServer(state.server).orElse(null)
                    if (targetServer == null) {
                        b("&c⚔ &fServer &c${state.server}&f not found!".cc())
                        continue
                    }

                    b("&c⚔ &fPulling players into &c${state.server}&f...".cc())

                    state.teams
                        .flatMap { it.players }
                        .map { it.uuid }
                        .forEach { uuid ->
                        plugin.server.getPlayer(UUID.fromString(uuid)).ifPresent { player ->
                            player.createConnectionRequest(targetServer).fireAndForget()
                        }
                    }
                }
            }

            natsClient.publish("tournament.request", "broxy".toByteArray())
        }
    }

    @Subscribe
    fun onStateUpdate(event: ServerPostConnectEvent) {
        if (event.previousServer != null) return

        // this makes it O(1*) and not O(n) thank goodness
        truePersonLookaside[event.player.uniqueId]?.let {
            it.name = event.player.username
            return
        }

        val person = Person(event.player.username, event.player.uniqueId.toString())
        truePersonList.add(person)
        truePersonLookaside[event.player.uniqueId] = person

        plugin.pluginContainer.launch(Dispatchers.IO) {
            natsClient?.publish("tournament.players", gson.toJson(peopleList).toByteArray())
        }
    }
}
