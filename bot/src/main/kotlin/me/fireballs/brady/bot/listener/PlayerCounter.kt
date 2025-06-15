package me.fireballs.brady.bot.listener

import me.fireballs.brady.bot.Bot
import me.fireballs.brady.core.registerEvents
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayerCounter : Listener, KoinComponent {
    private val plugin by inject<Bot>()
    private val jda by inject<JDA>()

    init {
        plugin.registerEvents(this)
        onPlayerCountChange()
    }

    @EventHandler
    private fun onJoin(event: PlayerJoinEvent) {
        onPlayerCountChange()
    }

    @EventHandler
    private fun onLeave(event: PlayerQuitEvent) {
        onPlayerCountChange()
    }

    private fun onPlayerCountChange() {
        val counter = Bukkit.getOnlinePlayers().size
        // that moment when NO abstraction
        jda.presence.activity = Activity.of(
            Activity.ActivityType.PLAYING,
            "TB | ${if (counter > 0) "$counter online" else "Nobody :("}",
        )
    }
}