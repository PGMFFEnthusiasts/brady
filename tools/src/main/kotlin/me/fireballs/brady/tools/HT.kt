package me.fireballs.brady.tools

import me.fireballs.brady.core.ItemBox
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.command
import me.fireballs.brady.core.registerEvents
import net.minecraft.server.v1_8_R3.Items
import org.bukkit.World
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
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

    init {
        command("wife") {
            executor {
                val p = player()
                p.inventory.addItem(retrieveStack(p.world))
            }
        }

        tools.registerEvents(this)
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        memoized = null
    }
}
