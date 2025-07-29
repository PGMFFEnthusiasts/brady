package me.fireballs.brady.tools

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame

class TabListResize : PacketListenerAbstract() {
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.JOIN_GAME) return
        WrapperPlayServerJoinGame(event).maxPlayers = 80
    }
}
