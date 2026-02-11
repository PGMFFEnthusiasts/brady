package me.fireballs.brady.tools.pvpfx

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.ConnectionState
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.fireballs.brady.core.registerPacketEvents
import me.fireballs.brady.core.sendPacket
import me.fireballs.brady.tools.Tools
import me.fireballs.brady.tools.ToolsSettings
import org.bukkit.Material
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

@Suppress("DEPRECATION")
class HideArmor : PacketListenerAbstract(), KoinComponent {
    private val tools by inject<Tools>()
    private val settings by inject<ToolsSettings>()

    init {
        tools.registerPacketEvents(this)
        settings.hideArmor.onSettingChange { player, newState ->
            val visiblePlayers = player.world.players.filter { player.canSee(it) }
            if (newState) {
                visiblePlayers.forEach { tp ->
                    val equipmentList = if (tp.equipment?.helmet?.type == Material.BANNER) mutableListOf(
                        Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(tp.equipment.helmet)),
                        Equipment(EquipmentSlot.CHEST_PLATE, ItemStack.EMPTY),
                        Equipment(EquipmentSlot.LEGGINGS, ItemStack.EMPTY),
                        Equipment(EquipmentSlot.BOOTS, ItemStack.EMPTY),
                    ) else mutableListOf(
                        Equipment(EquipmentSlot.HELMET, ItemStack.EMPTY),
                        Equipment(EquipmentSlot.CHEST_PLATE, ItemStack.EMPTY),
                        Equipment(EquipmentSlot.LEGGINGS, ItemStack.EMPTY),
                        Equipment(EquipmentSlot.BOOTS, ItemStack.EMPTY),
                    )
                    player.sendPacket(WrapperPlayServerEntityEquipment(tp.entityId, equipmentList))
                }

                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 5, ItemStack.EMPTY))
                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 6, ItemStack.EMPTY))
                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 7, ItemStack.EMPTY))
                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 8, ItemStack.EMPTY))
            } else {
                visiblePlayers.forEach { tp ->
                    player.sendPacket(WrapperPlayServerEntityEquipment(tp.entityId, mutableListOf(
                        Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(tp.equipment.helmet)),
                        Equipment(EquipmentSlot.CHEST_PLATE, SpigotConversionUtil.fromBukkitItemStack(tp.equipment.chestplate)),
                        Equipment(EquipmentSlot.LEGGINGS, SpigotConversionUtil.fromBukkitItemStack(tp.equipment.leggings)),
                        Equipment(EquipmentSlot.BOOTS, SpigotConversionUtil.fromBukkitItemStack(tp.equipment.boots)),
                    )))
                }

                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 5, SpigotConversionUtil.fromBukkitItemStack(player.equipment.helmet)))
                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 6, SpigotConversionUtil.fromBukkitItemStack(player.equipment.chestplate)))
                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 7, SpigotConversionUtil.fromBukkitItemStack(player.equipment.leggings)))
                player.sendPacket(WrapperPlayServerSetSlot(0, 0, 8, SpigotConversionUtil.fromBukkitItemStack(player.equipment.boots)))
            }
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.connectionState != ConnectionState.PLAY) return
        val uuid = event.user?.uuid ?: return
        if (!settings.hideArmor.retrieveValue(uuid)) return

        if (event.packetType == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            val p = WrapperPlayServerEntityEquipment(event)
            p.equipment = p.equipment.map {
                if (it.slot != EquipmentSlot.MAIN_HAND)
                    Equipment(it.slot, if (it.item.type.getId(ClientVersion.V_1_8) == Material.BANNER.id) it.item else ItemStack.EMPTY)
                else it
            }
            event.markForReEncode(true)
            return
        }

        if (event.packetType == PacketType.Play.Server.SET_SLOT) {
            val p = WrapperPlayServerSetSlot(event)
            if (p.windowId != 0) return
            if (p.item.type.getId(ClientVersion.V_1_8) == Material.BANNER.id) return
            if (p.slot !in 5..8) return
            p.item = ItemStack.EMPTY
            event.markForReEncode(true)
            return
        }

        if (event.packetType == PacketType.Play.Server.WINDOW_ITEMS) {
            val p = WrapperPlayServerWindowItems(event)
            if (p.windowId != 0) return
            for (i in 0 until p.items.size) {
                if (i !in 5..8) continue
                val item = p.items[i]
                if (item.type.getId(ClientVersion.V_1_8) == Material.BANNER.id) continue
                p.items[i] = ItemStack.EMPTY
                event.markForReEncode(true)
            }
            return
        }
    }
}