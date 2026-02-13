package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import me.fireballs.brady.corepgm.FeatureFlagBool
import net.minecraft.server.v1_8_R3.Items
import org.bukkit.World
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent
import javax.imageio.ImageIO

class HTMR : MapRenderer() {
    private val image = ImageIO.read(this::class.java.classLoader.getResource("map_norm.bin"))

    private var previousView: MapView? = null
    override fun render(p0: MapView, p1: MapCanvas, p2: Player) {
        if (previousView == p0) return
        previousView = p0
        p1.drawImage(0, 0, image)
    }
}

class HT : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val renderer = HTMR()

    private data class MemoizedMap(
        val world: World,
        val stack: ItemStack,
    )

    private var memoized: MemoizedMap? = null

    private fun retrieveStack(world: World): ItemStack {
        val m = memoized
        if (m != null && m.world == world) return m.stack
        memoized = null

        val stack = net.minecraft.server.v1_8_R3.ItemStack(Items.FILLED_MAP, 1, -1)
        val map = Items.FILLED_MAP.getSavedMap(stack, (world as CraftWorld).handle)
        val view = map.mapView
        view.scale = MapView.Scale.FARTHEST
        view.renderers.forEach { view.removeRenderer(it) }
        view.addRenderer(renderer)
        val finalStack = ItemBox(stack.asBukkitMirror())
            .name("&cMotivation".cc())
            .lore("&cHu Tao&7 is the 77th director".cc(), "&7of the Wangsheng funeral parlor".cc())
            .shiny()
            .itemMeta { addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS) }
            .build()
        memoized = MemoizedMap(world, finalStack)

        return finalStack
    }

    private val enabled = FeatureFlagBool("wifeEnabled", true)

    init {
        command("wife") {
            executor {
                val p = player()
                if (enabled.state) p.inventory.addItem(retrieveStack(p.world).clone())
                else err("This feature is disabled")
            }
        }

        tools.registerEvents(this)
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        memoized = null
    }

    @EventHandler
    private fun onDrop(event: PlayerDropItemEvent) {
        val memo = memoized?.stack ?: return
        if (!event.itemDrop.itemStack.isSimilar(memo)) return
        val item = event.itemDrop
        item.velocity = item.velocity.multiply(4.0)
        item.fireTicks = 9999
        (item as CraftItem).handle.noDamageTicks = 9999

        tools.launch {
            delay(40.ticks)
            item.remove()
        }
    }

    @EventHandler
    private fun onPickup(event: PlayerPickupItemEvent) {
        val memo = memoized?.stack ?: return
        if (!event.item.itemStack.isSimilar(memo)) return
        event.isCancelled = true
    }
}
