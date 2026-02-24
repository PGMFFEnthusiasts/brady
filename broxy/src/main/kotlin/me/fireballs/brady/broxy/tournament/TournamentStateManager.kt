package me.fireballs.brady.broxy.tournament

import com.github.shynixn.mccoroutine.velocity.launch
import com.google.gson.Gson
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import io.valkey.JedisPubSub
import me.fireballs.brady.broxy.Broxy
import me.fireballs.brady.broxy.utils.cc
import me.fireballs.brady.broxy.utils.newValkeyClient
import me.fireballs.brady.broxy.utils.newValkeyPooledClient
import me.fireballs.brady.broxy.utils.valkeyUrlOrLocalDefault
import net.kyori.adventure.text.Component
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
    private val redisUrl = valkeyUrlOrLocalDefault()
    private val redisClient = runCatching {
        newValkeyPooledClient(redisUrl)
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
            while (true) {
                runCatching {
                    newValkeyClient(redisUrl).use { jedis ->
                        jedis.subscribe(object : JedisPubSub() {
                            override fun onMessage(channel: String?, message: String?) {
                                if (channel == null || message == null) return

                                when (channel) {
                                    "tournament.broadcast" -> b(message.cc(false))

                                    "tournament.request" -> plugin.pluginContainer.launch(Dispatchers.IO) {
                                        redisClient?.publish("tournament.players", gson.toJson(peopleList))
                                        redisClient?.publish("tournament.state", gson.toJson(trueState))
                                    }

                                    "tournament.state" -> {
                                        val state = gson.fromJson(message, TournamentState::class.java)
                                        val oldState = trueState
                                        if (oldState.time <= state.time) trueState = state
                                    }

                                    "tournament.match" -> plugin.pluginContainer.launch(Dispatchers.IO) {
                                        val state = gson.fromJson(message, TournamentMatch::class.java)

                                        val targetServer = plugin.server.getServer(state.server).orElse(null)
                                        if (targetServer == null) {
                                            b("&c⚔ &fServer &c${state.server}&f not found!".cc())
                                            return@launch
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
                            }
                        }, "tournament.broadcast", "tournament.request", "tournament.state", "tournament.match")
                    }
                }.onFailure {
                    it.printStackTrace()
                    delay(1000L)
                }
            }
        }

        plugin.pluginContainer.launch(Dispatchers.IO) {
            redisClient?.publish("tournament.request", "broxy")
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
            redisClient?.publish("tournament.players", gson.toJson(peopleList))
        }
    }
}
