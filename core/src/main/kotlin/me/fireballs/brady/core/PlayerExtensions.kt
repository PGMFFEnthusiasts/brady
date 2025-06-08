package me.fireballs.brady.core

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import tc.oc.pgm.util.player.PlayerComponent.player

fun Player.sendPacket(packetWrapper: PacketWrapper<*>) =
    PacketEvents.getAPI().playerManager.sendPacket(this, packetWrapper)

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

fun Player.component(): Component = player(this)

