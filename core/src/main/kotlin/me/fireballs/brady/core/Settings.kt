package me.fireballs.brady.core

import com.google.common.collect.MapMaker
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentMap

abstract class SettingValue<V>(
    val key: String,
    val name: Component,
    val description: List<Component>,
    val baseItem: ItemBox,
) {
    val valueCache: ConcurrentMap<UUID, V> = MapMaker().makeMap<UUID, V>()
    val valueChangeHandlers = mutableListOf<(Player, V) -> Unit>()

    abstract fun allValues(): Iterable<V>
    abstract suspend fun load(player: UUID)
    abstract fun retrieveValue(player: UUID): V
    fun retrieveValue(player: Player) = retrieveValue(player.uniqueId)
    abstract fun nextValue(v: V): V
    abstract fun prevValue(v: V): V

    internal fun setNextOrPreviousBasedOnTheBoolean(forward: Boolean, player: Player) {
        val retrieved = retrieveValue(player)
        setValue(player, if (forward) nextValue(retrieved) else prevValue(retrieved))
    }

    fun renderItem(player: Player): ItemStack {
        val v = valueCache[player.uniqueId] ?: retrieveValue(player)
        val d = defaultValue()
        return baseItem
            .name(name)
            .loreComponentLines(description + "".c() + allValues().map {
                (if (v == it) "&a» " else "&7» ").cc() + stringify(it) + (if (it == d) "&e ＊" else "")
            })
            .build()
    }

    protected fun valueChanged(player: Player, v: V) {
        valueChangeHandlers.forEach { it.invoke(player, v) }
    }

    abstract fun setValue(player: Player, v: V)
    open fun stringify(v: V) = v.toString()

    open fun defaultValue() = allValues().first()

    fun onSettingChange(block: (Player, V) -> Unit) {
        valueChangeHandlers.add(block)
    }
}

class BooleanSettingValue(
    key: String,
    val defaultValue: Boolean,
    name: Component,
    description: List<Component>,
    baseItem: ItemBox,
) : SettingValue<Boolean>(key, name, description, baseItem) {
    override fun allValues(): Iterable<Boolean> {
        return listOf(true, false)
    }

    override suspend fun load(player: UUID) {
        logExceptions {
            valueCache[player] =
                player.boolGet(key, defaultValue, Retrieval.FRESH_ONLY)
                    .await()
        }
    }

    override fun retrieveValue(player: UUID): Boolean {
        return valueCache[player] ?: player.boolGetCached(key, defaultValue)
    }

    override fun nextValue(v: Boolean) = !v
    override fun prevValue(v: Boolean) = !v

    override fun setValue(player: Player, v: Boolean) {
        valueCache[player.uniqueId] = v
        player.boolSet(key, v)
        valueChanged(player, v)
    }

    override fun stringify(v: Boolean) = if (v) "ON" else "OFF"

    override fun defaultValue() = defaultValue
}

class EnumSettingsValue<E>(
    key: String,
    val enumValues: Array<E>,
    name: Component,
    description: List<Component>,
    baseItem: ItemBox,
) : SettingValue<E>(key, name, description, baseItem) where E : Enum<E> {
    override fun allValues(): Iterable<E> = enumValues.toList()

    override suspend fun load(player: UUID) {
        logExceptions {
            val key = player.stringGet(key, Retrieval.FRESH_ONLY).await()
                ?: defaultValue().name
            valueCache[player] = enumValues.find { it.name.equals(key, true) }
        }
    }

    override fun retrieveValue(player: UUID): E {
        val key = valueCache[player]?.name ?: player.stringGetCached(key) ?: defaultValue().name
        return enumValues.find { it.name.equals(key, true) } ?: return defaultValue()
    }

    override fun nextValue(v: E) = enumValues[(enumValues.indexOf(v) + 1).mod(enumValues.size)]
    override fun prevValue(v: E) = enumValues[(enumValues.indexOf(v) - 1).mod(enumValues.size)]

    override fun setValue(player: Player, v: E) {
        valueCache[player.uniqueId] = v
        player.stringSet(key, v.name)
        valueChanged(player, v)
    }
}

inline fun <reified E> createEnumSetting(key: String, name: Component, description: List<Component>, baseItem: ItemBox)
        where E : Enum<E> = EnumSettingsValue(key, enumValues<E>(), name, description, baseItem)

private val greyPane = itembox(Material.STAINED_GLASS_PANE)
    .setDamage(7)
    .name(" ")
    .build()

private val closeItem = itembox(Material.BARRIER)
    .name("&cClose".cc())
    .build()

private val nukeItem = itembox(Material.TNT)
    .name("&cReset".cc())
    .lore("&7Resets all settings".cc())
    .build()

private val switchSound = soundbox()
    .add(Sound.IRONGOLEM_THROW)

private val switchBackSound = soundbox()
    .add(Sound.IRONGOLEM_THROW, 0.75f)

private val nukeSound = soundbox()
    .add(Sound.EXPLODE, 1.25f)

private val openSound = soundbox()
    .add(Sound.DOOR_OPEN, 1.25f)

private val closeSound = soundbox()
    .add(Sound.DOOR_CLOSE, 0.75f)

val settingSlotPositions = (9..45).filter { it % 9 != 0 && it % 8 != 0 }

class Settings : Listener, KoinComponent {
    private val core by inject<Core>()
    val settingsList = mutableListOf<SettingValue<*>>()

    fun addAll(vararg settings: SettingValue<*>) {
        settings.forEach { settingsList.add(it) }
    }

    init {
        command("prefs", aliases = arrayOf("preferences")) {
            executor {
                openSettingsPage(player())
            }
        }

        core.registerEvents(this)
    }

    @EventHandler(ignoreCancelled = true)
    private suspend fun preJoin(event: AsyncPlayerPreLoginEvent) {
        settingsList.forEach { it.load(event.uniqueId) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    private fun postJoin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        settingsList.forEach { it.valueCache.remove(event.uniqueId) }
    }

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        settingsList.forEach { it.valueCache.remove(event.player.uniqueId) }
    }

    fun openSettingsPage(player: Player) {
        val settingCount = settingsList.size
        val rows = settingCount / 7
        menubox((4 + rows) * 9) {
            repeat(slots) { addItem(it, greyPane) }
            settingsList.forEachIndexed { index, value ->
                if (index > settingSlotPositions.lastIndex) return@forEachIndexed
                addClickItem(settingSlotPositions[index], value.renderItem(player)) {
                    val left = it.action == InventoryAction.PICKUP_ALL
                    val right = it.action == InventoryAction.PICKUP_HALF
                    if (!left && !right) return@addClickItem
                    value.setNextOrPreviousBasedOnTheBoolean(left, player)
                    it.clickedInventory.setItem(it.slot, value.renderItem(player))
                    if (left) switchSound.play(player)
                    else switchBackSound.play(player)
                    with(value) {
                        player.send(
                            "&aSet ".cc() + name.hover(
                                Component.join(
                                    JoinConfiguration.newlines(),
                                    description
                                )
                            ) + "&a to ${stringify(retrieveValue(player.uniqueId))}."
                        )
                    }
                }
            }

            addClickItem(slots - 5, closeItem) { pop() }
            addClickItem(slots - 1, nukeItem) { it ->
                settingsList.forEach { with(it) { setValue(player, defaultValue()) } }
                settingsList.forEachIndexed { index, value ->
                    if (index > settingSlotPositions.lastIndex) return@forEachIndexed
                    it.clickedInventory.setItem(settingSlotPositions[index], value.renderItem(player))
                }
                nukeSound.play(player)
                player.send("&cSuccessfully reset all preferences.".cc())
            }
            cancelClicks()
            title = "Preferences"

            addCloseHandler { closeSound.play(viewer) }
        }.open(player)
        openSound.play(player)
    }
}
