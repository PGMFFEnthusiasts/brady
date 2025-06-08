package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun menubox(
    slots: Int,
    builder: MenuBox.() -> Unit,
) = MenuBoxContainer(slots, builder)

// this is here so you can reuse the same MenuBox with
// different states--a pretty common use case if I do say
// so myself :)
class MenuBoxContainer(
    val slots: Int,
    val builder: MenuBox.() -> Unit,
) {
    fun open(player: Player): Menu {
        val menuBox = MenuBox(slots, player)
        builder(menuBox)
        return menuBox.openFor(player)
    }
}

data class MenuItem(
    val stack: ItemStack,
    val handler: suspend Menu.(InventoryClickEvent) -> Unit = {},
)

class MenuBox(
    val slots: Int,
    val player: Player,
) : KoinComponent {
    var title = "Menu"
    private val menuManager by inject<MenuManager>()
    private val items = mutableMapOf<Int, MenuItem>()
    private val clickHandlers = mutableListOf<suspend Menu.(InventoryClickEvent) -> Unit>()
    private val tickHandlers = mutableListOf<suspend Menu.() -> Unit>()
    private val closeHandlers = mutableListOf<suspend Menu.() -> Unit>()

    fun addClickItem(
        slot: Int,
        item: ItemStack,
        interaction: suspend Menu.(InventoryClickEvent) -> Unit,
    ) {
        items[slot] = MenuItem(item.clone(), interaction)
    }

    fun addItem(
        slot: Int,
        item: ItemStack,
    ) {
        items[slot] = MenuItem(item.clone())
    }

    fun addClickHandler(
        handler: suspend Menu.(InventoryClickEvent) -> Unit,
    ) {
        clickHandlers.add(handler)
    }

    fun addTickHandler(
        handler: suspend Menu.() -> Unit,
    ) {
        tickHandlers.add(handler)
    }

    fun addCloseHandler(
        handler: suspend Menu.() -> Unit,
    ) {
        closeHandlers.add(handler)
    }

    fun cancelClicks() {
        addClickHandler { it.isCancelled = true }
    }

    fun openFor(
        player: Player,
    ): Menu {
        val inventory = Bukkit.createInventory(player, slots, title)
        val menu = Menu(
            player,
            inventory,

            HashMap(items),
            ArrayList(clickHandlers),
            ArrayList(tickHandlers),
            ArrayList(closeHandlers),
        )

        items.entries.forEach {
            inventory.setItem(it.key, it.value.stack)
        }

        player.openInventory(inventory)
        menuManager.playerMenus[player] = menu
        return menu
    }
}

class Menu(
    val viewer: Player,
    val inventory: Inventory,

    val items: MutableMap<Int, MenuItem>,
    val clickHandlers: List<suspend Menu.(InventoryClickEvent) -> Unit>,
    val tickHandlers: List<suspend Menu.() -> Unit>,
    val closeHandlers: List<suspend Menu.() -> Unit>,
) : KoinComponent {
    private val plugin by inject<Core>()

    val isActive
        get() = viewer.isOnline && viewer.openInventory.topInventory == inventory

    @Suppress("DEPRECATION")
    var currentTitle = inventory.title
        set(value) {
            if (isActive) viewer.sendInventoryTitleChange(value)
            field = value
        }

    fun close(): Boolean {
        if (isActive) viewer.closeInventory()
        plugin.launch {
            closeHandlers.forEach {
                it.invoke(this@Menu)
            }
        }

        return true
    }

    internal suspend fun onInteract(event: InventoryClickEvent) {
        items[event.slot]?.handler?.invoke(this, event)
        clickHandlers.forEach {
            it.invoke(this@Menu, event)
        }
    }

    fun setClickItem(
        slot: Int,
        item: ItemStack,
        interaction: suspend Menu.(InventoryClickEvent) -> Unit,
    ) {
        items[slot] = MenuItem(item.clone(), interaction)
    }

    fun setItem(
        slot: Int,
        item: ItemStack,
    ) {
        items[slot] = MenuItem(item.clone())
    }

    fun removeItem(
        slot: Int,
    ) {
        items.remove(slot)
        inventory.clear(slot)
    }
}

class MenuManager : Listener, KoinComponent {
    private val plugin by inject<Core>()

    init {
        plugin.registerEvents(this)
        plugin.launch(plugin.minecraftDispatcher) { garbageCollect() }
        plugin.launch(plugin.minecraftDispatcher) { renderTick() }
    }

    private suspend fun garbageCollect() {
        while (true) {
            playerMenus.entries.removeIf { !it.value.isActive && it.value.close() }
            delay(20.ticks)
        }
    }

    private suspend fun renderTick() {
        while (true) {
            playerMenus.values.forEach { menu -> menu.tickHandlers.forEach { it.invoke(menu) } }
            delay(1.ticks)
        }
    }

    internal val playerMenus = mutableMapOf<Player, Menu>()

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        playerMenus.remove(event.player)?.close()
    }

    @EventHandler
    private fun onInventoryClose(event: InventoryCloseEvent) {
        playerMenus.remove(event.player)?.close()
    }

    @EventHandler
    private suspend fun onInventoryClick(event: InventoryClickEvent) {
        playerMenus[event.actor]?.onInteract(event)
    }
}
