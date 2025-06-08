package me.fireballs.brady.core

import com.github.retrooper.packetevents.protocol.potion.PotionTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect
import net.minecraft.server.v1_8_R3.ChatMessage
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player


val fakeGameMode = WrapperPlayServerChangeGameState(
    WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
    -1f
)

fun Player.sendFakeGameMode() {
    sendPacket(fakeGameMode)
}

val adventureGameMode = WrapperPlayServerChangeGameState(
    WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
    2f,
)

fun Player.sendFakeAdventureGameMode() {
    sendPacket(adventureGameMode)
}

fun Player.sendFakeMiningFatigue() {
    val fakeMiningFatigue = WrapperPlayServerEntityEffect(
        entityId,
        PotionTypes.MINING_FATIGUE,
        -1,
        32767,
        1.toByte()
    )

    sendPacket(fakeMiningFatigue)
}

fun Player.sendInventoryTitleChange(title: String) {
    val entityPlayer = (player as CraftPlayer).handle
    val packet = PacketPlayOutOpenWindow(
        entityPlayer.activeContainer.windowId,
        "minecraft:chest",
        ChatMessage(title),
        player.openInventory.topInventory.size
    )
    entityPlayer.playerConnection.sendPacket(packet)
    entityPlayer.updateInventory(entityPlayer.activeContainer)
}
