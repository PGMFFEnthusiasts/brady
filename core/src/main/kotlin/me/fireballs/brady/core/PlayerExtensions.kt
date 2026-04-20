package me.fireballs.brady.core

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.text.Component
import net.minecraft.server.v1_8_R3.EntityPlayer
import net.minecraft.server.v1_8_R3.Packet
import net.minecraft.server.v1_8_R3.WorldServer
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

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

val Player.handle: EntityPlayer
    get() = (this as CraftPlayer).handle

fun Entity.trackedPlayers(): Iterable<EntityPlayer> =
    ((this as CraftEntity).handle.world as WorldServer).tracker.trackedEntities.get(handle.id)?.trackedPlayers
        ?: emptySet()

fun Entity.sendTracked(packet: Packet<*>) = trackedPlayers().forEach { it.playerConnection.sendPacket(packet) }
