package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.MetaNode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

enum class Retrieval {
    CACHE_THEN_FRESH,
    FRESH_ONLY,
}

fun UUID.stringGetCached(key: String) = LuckPermsProvider
    .get()
    .userManager
    .getUser(this)
    ?.cachedData
    ?.metaData
    ?.getMetaValue(key)

fun UUID.stringGet(key: String, retrieval: Retrieval): CompletableFuture<String?> {
    if (retrieval == Retrieval.CACHE_THEN_FRESH) {
        val retrieved = stringGetCached(key)
        if (retrieved != null) return CompletableFuture.completedFuture(retrieved)
    }

    return LuckPermsProvider
        .get()
        .userManager
        .loadUser(this)
        .exceptionally { null }
        .thenApply { it?.cachedData?.metaData?.getMetaValue(key) }
}

fun UUID.stringSet(key: String, value: String) {
    val user = LuckPermsProvider
        .get().userManager
        .getUser(this) ?: return
    user.data().clear(NodeType.META.predicate { metaNode -> metaNode.metaKey == key })
    user.data().add(MetaNode.builder(key, value).build())
    Core.kavyManager.queueSaving(user)
}

fun UUID.boolGetCached(key: String, defaultValue: Boolean) =
    stringGetCached(key)?.toBooleanStrictOrNull() ?: defaultValue

fun UUID.boolGet(key: String, defaultValue: Boolean, retrieval: Retrieval): CompletableFuture<Boolean> =
    stringGet(key, retrieval).thenApply { it?.toBooleanStrictOrNull() ?: defaultValue }

fun UUID.boolSet(key: String, value: Boolean) =
    stringSet(key, if (value) "true" else "false")

fun Player.stringGetCached(key: String) = uniqueId.stringGetCached(key)
fun Player.stringGet(key: String, retrieval: Retrieval) = uniqueId.stringGet(key, retrieval)
fun Player.stringSet(key: String, value: String) = uniqueId.stringSet(key, value)

fun Player.boolGetCached(key: String, defaultValue: Boolean = false) = uniqueId.boolGetCached(key, defaultValue)
fun Player.boolGet(key: String, defaultValue: Boolean, retrieval: Retrieval) = uniqueId.boolGet(key, defaultValue, retrieval)
fun Player.boolSet(key: String, value: Boolean) = uniqueId.boolSet(key, value)

class KavyManager : Listener, KoinComponent {
    private val tools by inject<Core>()
    private var syncJob: Job? = null

    init {
        tools.registerEvents(this)
        syncJob = tools.launch(tools.asyncDispatcher) {
            while (true) {
                delay(20.ticks)
                sync()
            }
        }
    }

    private val toSync = ConcurrentLinkedQueue<User>()
    internal fun queueSaving(user: User) {
        toSync.add(user)
    }

    private fun sync() {
        val users = mutableSetOf<User>()
        while (true) {
            val user = toSync.poll() ?: break
            users.add(user)
        }

        val lp = LuckPermsProvider.get()
        for (user in users) {
            lp.userManager.saveUser(user)
            lp.messagingService.ifPresent { it.pushUserUpdate(user) }
        }
    }

    internal fun onDisable() {
        syncJob?.cancel()
        sync()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onQuit(event: PlayerQuitEvent) {
        val user = toSync.firstOrNull { it.uniqueId == event.player.uniqueId } ?: return
        val lp = LuckPermsProvider.get()
        lp.userManager.saveUser(user).thenRun {
            lp.messagingService.ifPresent { it.pushUserUpdate(user) }
        }
    }
}

