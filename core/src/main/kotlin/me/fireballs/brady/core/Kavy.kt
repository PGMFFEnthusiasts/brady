package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import com.google.common.collect.MapMaker
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
import java.util.concurrent.ConcurrentMap

enum class Retrieval {
    CACHE_THEN_FRESH,
    FRESH_ONLY,
}

private object KavyValkey {
    private const val namespace = "kavy"
    private val client = runCatching { newValkeyPooledClient() }.getOrNull()

    private val cache: ConcurrentMap<UUID, ConcurrentMap<String, String>> = MapMaker()
        .concurrencyLevel(4)
        .makeMap()

    val enabled = client != null

    private fun redisKey(uuid: UUID) = "$namespace:${uuid.toString().lowercase()}"

    fun getCached(uuid: UUID, key: String) = cache[uuid]?.get(key)

    fun setCached(uuid: UUID, key: String, value: String) {
        cache.computeIfAbsent(uuid) {
            MapMaker().concurrencyLevel(2).makeMap()
        }[key] = value
    }

    fun clearCached(uuid: UUID) {
        cache.remove(uuid)
    }

    fun getFresh(uuid: UUID, key: String): String? {
        val value = runCatching {
            client?.hget(redisKey(uuid), key)
        }.getOrNull() ?: return null

        setCached(uuid, key, value)
        return value
    }

    fun flush(entries: Map<UUID, Map<String, String>>) {
        val pooled = client ?: return
        for ((uuid, values) in entries) {
            val redisKey = redisKey(uuid)
            for ((key, value) in values) {
                runCatching {
                    pooled.hset(redisKey, key, value)
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
}

private fun UUID.legacyStringGetCached(key: String) = LuckPermsProvider
    .get()
    .userManager
    .getUser(this)
    ?.cachedData
    ?.metaData
    ?.getMetaValue(key)

private fun UUID.legacyStringGet(key: String, retrieval: Retrieval): CompletableFuture<String?> {
    if (retrieval == Retrieval.CACHE_THEN_FRESH) {
        val retrieved = legacyStringGetCached(key)
        if (retrieved != null) return CompletableFuture.completedFuture(retrieved)
    }

    return LuckPermsProvider
        .get()
        .userManager
        .loadUser(this)
        .exceptionally { null }
        .thenApply { it?.cachedData?.metaData?.getMetaValue(key) }
}

private fun UUID.legacyStringSet(key: String, value: String) {
    val user = LuckPermsProvider
        .get().userManager
        .getUser(this) ?: return

    user.data().clear(NodeType.META.predicate { metaNode -> metaNode.metaKey == key })
    user.data().add(MetaNode.builder(key, value).build())
    Core.kavyManager.queueLegacySaving(user)
}

fun UUID.stringGetCached(key: String): String? {
    if (!KavyValkey.enabled) return legacyStringGetCached(key)

    val cached = KavyValkey.getCached(this, key)
    if (cached != null) return cached

    val migrated = legacyStringGetCached(key) ?: return null
    KavyValkey.setCached(this, key, migrated)
    return migrated
}

fun UUID.stringGet(key: String, retrieval: Retrieval): CompletableFuture<String?> {
    if (!KavyValkey.enabled) return legacyStringGet(key, retrieval)

    if (retrieval == Retrieval.CACHE_THEN_FRESH) {
        val cached = KavyValkey.getCached(this, key)
        if (cached != null) return CompletableFuture.completedFuture(cached)
    }

    val uuid = this
    return CompletableFuture
        .supplyAsync { KavyValkey.getFresh(uuid, key) }
        .thenCompose { fromValkey ->
            if (fromValkey != null) return@thenCompose CompletableFuture.completedFuture(fromValkey)

            uuid.legacyStringGet(key, retrieval).thenApply { fromLegacy ->
                if (fromLegacy != null) {
                    KavyValkey.setCached(uuid, key, fromLegacy)
                    Core.kavyManager.queueValkeySaving(uuid, key, fromLegacy)
                }

                fromLegacy
            }
        }
}

fun UUID.stringSet(key: String, value: String) {
    if (!KavyValkey.enabled) {
        legacyStringSet(key, value)
        return
    }

    KavyValkey.setCached(this, key, value)
    Core.kavyManager.queueValkeySaving(this, key, value)
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
    private data class ValkeySave(
        val uuid: UUID,
        val key: String,
        val value: String,
    )

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

    private val toSyncLegacy = ConcurrentLinkedQueue<User>()
    private val toSyncValkey = ConcurrentLinkedQueue<ValkeySave>()

    internal fun queueLegacySaving(user: User) {
        toSyncLegacy.add(user)
    }

    internal fun queueValkeySaving(uuid: UUID, key: String, value: String) {
        if (!KavyValkey.enabled) return
        toSyncValkey.add(ValkeySave(uuid, key, value))
    }

    private fun sync() {
        val valkeyData = mutableMapOf<UUID, MutableMap<String, String>>()
        while (true) {
            val save = toSyncValkey.poll() ?: break
            valkeyData
                .computeIfAbsent(save.uuid) { mutableMapOf() }[save.key] = save.value
        }
        KavyValkey.flush(valkeyData)

        val users = mutableSetOf<User>()
        while (true) {
            val user = toSyncLegacy.poll() ?: break
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
        KavyValkey.clearCached(event.player.uniqueId)

        val user = toSyncLegacy.firstOrNull { it.uniqueId == event.player.uniqueId } ?: return
        val lp = LuckPermsProvider.get()
        lp.userManager.saveUser(user).thenRun {
            lp.messagingService.ifPresent { it.pushUserUpdate(user) }
        }
    }
}

