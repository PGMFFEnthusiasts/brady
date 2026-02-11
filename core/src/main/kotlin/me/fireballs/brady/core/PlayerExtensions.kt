package me.fireballs.brady.core

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.text.Component
import net.minecraft.server.v1_8_R3.EntityPlayer
import net.minecraft.server.v1_8_R3.Packet
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import tc.oc.pgm.util.player.PlayerComponent.player

fun Player.sendPacket(packetWrapper: PacketWrapper<*>) =
    PacketEvents.getAPI().playerManager.sendPacket(this, packetWrapper)

fun Player.sendPacket(packet: Packet<*>) =
    handle.playerConnection.sendPacket(packet)

fun PacketWrapper<*>.send(player: Player) = player.sendPacket(this)
fun PacketWrapper<*>.send(players: List<Player>) = players.forEach { it.sendPacket(this) }

fun CommandSender.send(component: Component) {
    if (this is Player) Core.adventure.player(this).sendMessage(component)
    else Core.adventure.sender(this).sendMessage(component)
}

fun CommandSender.send() {
    Core.adventure.sender(this).sendMessage(Component.empty())
}

fun Player.actionBar(message: Component) {
    Core.adventure.player(this).sendActionBar(message)
}

fun CommandSender.component(): Component = player(this)

val Player.handle: EntityPlayer
    get() = (this as CraftPlayer).handle
