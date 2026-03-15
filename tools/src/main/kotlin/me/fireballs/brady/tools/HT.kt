package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import com.google.common.collect.MapMaker
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import me.fireballs.brady.corepgm.FeatureFlagBool
import net.minecraft.server.v1_8_R3.Items
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem
import org.bukkit.entity.Item
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
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

class HTMR(val image: BufferedImage) : MapRenderer() {
    private var previousView: MapView? = null
    override fun render(p0: MapView, p1: MapCanvas, p2: Player) {
        if (previousView == p0) return
        previousView = p0
        p1.drawImage(0, 0, image)
    }
}

class HT : Listener, KoinComponent {
    private val tools by inject<Tools>()

    private val memoizedMaps = MapMaker()
        .makeMap<Int, ItemStack>()
    private val currentlyViewing = MapMaker()
        .weakKeys()
        .weakValues()
        .makeMap<UUID, Player>()

    private val imageList = mutableListOf<BufferedImage>()
    private val bouncer = Bouncer(
        { e -> e is Item && e.itemStack.specialData() == "ht" },
        { e -> currentlyViewing[e.uniqueId]?.let { listOf(it) } ?: listOf() }
    )

    init {
        val inputStream = this::class.java.classLoader.getResourceAsStream("map_norm.bin")
        if (inputStream != null) {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    imageList += ImageIO.read(bytes.inputStream())
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        tools.launch {
            while (tools.isEnabled) {
                delay(1.ticks)
                if (enabled.state) bouncer.tick()
            }
        }
    }

    private fun retrieveStack(world: World, n: Int): ItemStack {
        if (!imageList.indices.contains(n)) return ItemStack(Material.AIR)
        val memoized = memoizedMaps[n]
        if (memoized != null) return memoized

        val stack = net.minecraft.server.v1_8_R3.ItemStack(Items.FILLED_MAP, 1, -1)
        val map = Items.FILLED_MAP.getSavedMap(stack, (world as CraftWorld).handle)
        val view = map.mapView
        view.scale = MapView.Scale.FARTHEST
        view.renderers.forEach { view.removeRenderer(it) }
        view.addRenderer(HTMR(imageList[n]))
        val finalStack = ItemBox(stack.asBukkitMirror())
            .name("&cMotivation #$n".cc())
            .lore("&cHu Tao&7 is the 77th director".cc(), "&7of the Wangsheng funeral parlor".cc())
            .shiny()
            .itemMeta { addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS) }
            .specialData("ht")
            .build()
        memoizedMaps[n] = finalStack

        return finalStack
    }

    private val enabled = FeatureFlagBool("wifeEnabled", true)

    init {
        command("wife") {
            executor {
                val p = player()
                if (enabled.state) p.inventory.addItem(retrieveStack(p.world, imageList.indices.random()).clone())
                else err("This feature is disabled")
            }
        }

        tools.registerEvents(this)
    }

    @EventHandler
    private fun onCycle(event: MatchAfterLoadEvent) {
        memoizedMaps.clear()
        currentlyViewing.clear()
    }

    @EventHandler
    private fun onDrop(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.specialData() != "ht") return
        val item = event.itemDrop
        currentlyViewing[item.uniqueId] = event.player
        item.velocity = item.velocity.multiply(3.0)
        item.fireTicks = 9999
        (item as CraftItem).handle.noDamageTicks = 9999

        tools.launch {
            delay(80.ticks)
            item.remove()
            currentlyViewing.remove(item.uniqueId, event.player)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onPickup(event: PlayerPickupItemEvent) {
        if (event.item.itemStack.specialData() != "ht") return
        event.isCancelled = true
        event.item.remove()
    }
}
