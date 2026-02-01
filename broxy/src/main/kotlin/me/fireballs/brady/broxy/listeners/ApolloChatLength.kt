package me.fireballs.brady.broxy.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier

class ApolloChatLength {

    @Subscribe
    fun onLogin(event: PostLoginEvent) {
        event.player.sendPluginMessage(MinecraftChannelIdentifier.from("apollo:json"), APOLLO_JSON)
    }

    companion object {
        private val APOLLO_JSON = """
            [
                {
                    "apollo_module": "server_rule",
                    "enable": true,
                    "properties": {
                        "override-max-chat-length": true,
                        "max-chat-length": 256
                    }
                }
            ]
            """.toByteArray(Charsets.UTF_8)
    }
}
