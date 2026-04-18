package me.fireballs.brady.broxy.listeners

import com.github.shynixn.mccoroutine.velocity.launch
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import io.valkey.args.ExpiryOption
import io.valkey.params.HSetExParams
import kotlinx.coroutines.Dispatchers
import me.fireballs.brady.broxy.Broxy
import me.fireballs.brady.broxy.utils.newValkeyClient

class NameCache(
    private val plugin: Broxy,
) {
    private val valkey = newValkeyClient()

    @Subscribe
    private fun loginEvent(event: LoginEvent) {
        val name = event.player.username
        val lower = name.lowercase()
        val uuid = event.player.uniqueId
        val uuidString = uuid.toString()

        plugin.pluginContainer.launch(Dispatchers.IO) {
            // begin expiring pre-existing
            val existingName = valkey.hget("nameCache", uuidString)
            if (existingName != null && existingName != lower)
                valkey.hexpire("uuidCache", 60 * 60 * 24 * 7, ExpiryOption.LT, existingName)

            // real setting
            valkey.hset("nameCache", uuidString, name)
            valkey.hset("uuidCache", lower, uuidString)
        }
    }
}
